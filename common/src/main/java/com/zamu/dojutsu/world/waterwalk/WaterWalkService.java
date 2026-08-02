package com.zamu.dojutsu.world.waterwalk;

import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.core.PlayerTickSystem;
import com.zamu.dojutsu.core.TickPhase;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.VisionState;
import com.zamu.dojutsu.client.ClientWaterWalk;
import com.zamu.dojutsu.domain.waterwalk.WaterWalkTable;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.network.ToggleWaterWalkPayload;
import com.zamu.dojutsu.network.WaterWalkPayload;
import com.zamu.dojutsu.platform.NetworkService;
import com.zamu.dojutsu.platform.PlatformServices;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Caminar sobre el agua {@code [S22]} (F9/DJT-205). Sistema por tick en la fase {@link TickPhase#WATER_WALK},
 * que estaba declarada desde F1 esperando justo esto.
 *
 * <h2>La llave no es un flag, es tu chakra</h2>
 * {@code can_walk_on_water} no es configuracion: exige {@value WaterWalkTable#REQUIRED_MAX_CHAKRA} de chakra
 * <b>maximo</b> y {@value WaterWalkTable#REQUIRED_CHAKRA} de chakra actual. Se desbloquea creciendo y se pierde
 * al agotarte — hundirse a mitad de un lago es parte del diseño, no un fallo.
 *
 * <h2>Cuatro condiciones para sostenerse</h2>
 * {@code main} las encadena y todas importan: agua justo bajo los pies, <b>no</b> agua a la altura del cuerpo ni
 * de la cabeza (si te sumerges, se acabo) y el bloque donde estas debe ser atravesable. Y agacharse te hunde a
 * proposito, que es como se baja al agua.
 *
 * <h2>Bounded por construccion</h2>
 * Solo hace trabajo mientras hay agua debajo: fuera del agua el guard sale en la primera condicion. El estado
 * transitorio (ticks hacia el proximo cobro) vive en memoria, no en el {@code PlayerNinjaData}: es de ejecucion
 * y no debe sobrevivir a una reconexion, igual que los modos del arnes.
 *
 * <h2>RB-1 (jitter): el cliente tiene que saberlo</h2>
 * El movimiento del jugador es <b>autoritativo en cliente</b>, y la levitacion la aplican los dos lados. Si solo
 * el servidor sabe que te sostienes, el cliente sube con el empuje residual del efecto y el servidor lo baja de
 * un tiron cada tick: <b>eso son los tirones</b>. Por eso el estado se sincroniza ({@code WaterWalkPayload}, dos
 * paquetes por travesia, no uno por tick) y el cliente anula su propia velocidad vertical: llega a la misma
 * conclusion por su cuenta y no queda nada que corregir.
 *
 * <p>El anclaje del servidor sobrevive como <b>red de seguridad</b> con un umbral grueso
 * ({@link WaterWalkTable#SNAP_EPSILON}), para desvios que el cliente no pueda explicar por si mismo.
 *
 * <p><b>Historial</b>: dos intentos fallidos antes de este. Confiar el sostenimiento al amplificador del efecto
 * no funciona —{@code Mth.clamp(0, 255)} impide el valor que haria falta— y corregir posicion por tick tampoco:
 * arregla la altura y rompe la sensacion.
 */
public final class WaterWalkService implements PlayerTickSystem {

    /** Ticks acumulados hacia el proximo cobro, por jugador. Transitorio. */
    private static final Map<UUID, Integer> CHARGE_TIMER = new ConcurrentHashMap<>();

    @Override
    public TickPhase phase() {
        return TickPhase.WATER_WALK;
    }

    @Override
    public void tick(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        PlayerNinjaData data = NinjaData.get(player);
        OptionalDouble surface = findSurface(player, level, data);
        if (surface.isEmpty()) {
            stop(player);
            return;
        }
        if (CHARGE_TIMER.putIfAbsent(player.getUUID(), 0) == null) {
            // Empieza a sostenerse: el cliente tiene que saberlo para no pelearse con nosotros.
            PlatformServices.NETWORK.sendToPlayer(player, new WaterWalkPayload(true));
        }
        hover(player, level, surface.getAsDouble());
        charge(player, data);
    }

    /**
     * Busca la superficie que pisar, o vacio si no procede sostenerse.
     *
     * <p>Se comprueba, en el orden de {@code main}: chakra suficiente, no agachado, la cabeza fuera del agua y
     * el bloque del cuerpo atravesable. La diferencia con el datapack es que <b>la superficie se busca en una
     * banda</b> ({@link WaterWalkTable#SURFACE_TOLERANCE}) en vez de exigir agua exactamente a -0.35: asi un
     * empujon residual o una ola no sueltan al ninja, que era justo el fallo de "camina un poco y se hunde".
     */
    private static OptionalDouble findSurface(ServerPlayer player, ServerLevel level, PlayerNinjaData data) {
        if (data.vision().waterWalkOff()) {
            return OptionalDouble.empty(); // desactivada a proposito por el jugador (F10, tecla U)
        }
        if (!WaterWalkTable.canWalk(data.chakra().maxChakra(), data.chakra().chakra())) {
            return OptionalDouble.empty();
        }
        if (player.isShiftKeyDown()) {
            return OptionalDouble.empty(); // agacharse es como se baja al agua a proposito
        }
        Vec3 pos = player.position();
        if (isWater(level, pos.add(0, 1, 0))) {
            return OptionalDouble.empty(); // con la cabeza dentro se nada, no se camina
        }
        // Superficie candidata: el techo del bloque de agua que hay justo debajo de los pies.
        BlockPos below = BlockPos.containing(pos.add(0, WaterWalkTable.SURFACE_PROBE, 0));
        if (!isWater(level, Vec3.atLowerCornerOf(below))) {
            return OptionalDouble.empty(); // sin agua debajo no hay nada que pisar
        }
        double surfaceY = below.getY() + 1.0;
        if (!WaterWalkTable.nearSurface(pos.y(), surfaceY) || !isPassable(level, pos)) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(surfaceY);
    }

    /** {@code #dojutsu:waters}: agua y las plantas acuaticas que cuentan como superficie. */
    private static boolean isWater(ServerLevel level, Vec3 pos) {
        BlockState state = level.getBlockState(BlockPos.containing(pos));
        return state.is(Blocks.WATER) || state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS)
                || state.is(Blocks.SEA_PICKLE);
    }

    /**
     * {@code #dojutsu:voids}: el bloque donde esta el jugador debe poder atravesarse. En vez de repetir la lista
     * de 22 entradas del tag, se pregunta por la propiedad que todas comparten — no tener colision.
     */
    private static boolean isPassable(ServerLevel level, Vec3 pos) {
        BlockPos block = BlockPos.containing(pos);
        return level.getBlockState(block).getCollisionShape(level, block).isEmpty();
    }

    /**
     * Sostiene al ninja sobre la superficie y salpica, como {@code walk}.
     *
     * <p><b>Quien fija la altura es esto, no el efecto.</b> La levitacion solo esta para que
     * {@code LivingEntity.travel} no aplique gravedad; su empuje minimo ({@code +0.05}) haria subir al jugador
     * si se le dejara. Asi que ademas se anula la velocidad vertical y se ancla la Y a la superficie —
     * <b>solo si la desviacion lo merece</b> ({@link WaterWalkTable#SNAP_EPSILON}), para no reenviar posicion
     * cada tick, que es de donde sale el jitter de {@code [RB-1]}.
     */
    private static void hover(ServerPlayer player, ServerLevel level, double surfaceY) {
        player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 1, WaterWalkTable.HOVER_AMPLIFIER,
                true, false, false));
        Vec3 motion = player.getDeltaMovement();
        if (motion.y() != 0) {
            player.setDeltaMovement(motion.x(), 0, motion.z()); // ni cae ni la levitacion la empuja
        }
        if (WaterWalkTable.shouldSnap(player.getY(), surfaceY)) {
            player.teleportTo(player.getX(), surfaceY, player.getZ());
        }
        player.resetFallDistance(); // pisar agua no puede matarte al llegar a la orilla
        level.sendParticles(ParticleTypes.RAIN, player.getX(), player.getY(), player.getZ(),
                5, 0.2, 0, 0.2, 0);
    }

    /** Cobra {@value WaterWalkTable#CHAKRA_COST} cada {@value WaterWalkTable#COST_INTERVAL_TICKS} ticks. */
    private static void charge(ServerPlayer player, PlayerNinjaData data) {
        int ticks = CHARGE_TIMER.getOrDefault(player.getUUID(), 0);
        if (WaterWalkTable.shouldCharge(ticks)) {
            ChakraService.consume(data, WaterWalkTable.CHAKRA_COST);
            CHARGE_TIMER.put(player.getUUID(), 0);
        } else {
            CHARGE_TIMER.put(player.getUUID(), ticks + 1);
        }
    }

    /**
     * {@code end}: deja de sostenerse y olvida el contador.
     *
     * <p>No recoloca al jugador: soltarlo y dejar que la fisica normal siga es lo que hace limpia la salida a
     * tierra. Y solo retira la levitacion <b>si la puso este servicio</b> — quitarla a ciegas se llevaria por
     * delante la de Chibaku Tensei o cualquier otra fuente.
     */
    private static void stop(ServerPlayer player) {
        if (CHARGE_TIMER.remove(player.getUUID()) == null) {
            return;
        }
        MobEffectInstance current = player.getEffect(MobEffects.LEVITATION);
        if (current != null && current.getAmplifier() == WaterWalkTable.HOVER_AMPLIFIER) {
            player.removeEffect(MobEffects.LEVITATION);
        }
        PlatformServices.NETWORK.sendToPlayer(player, new WaterWalkPayload(false));
    }

    /** Suscribe el hook de daño y el payload S2C de estado. Llamar una vez en el arranque. */
    public static void register() {
        EntityEvent.LIVING_HURT.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player && amount > 0) {
                onHurt(player);
            }
            return EventResult.pass();
        });
        PlatformServices.NETWORK.register(NetworkService.Side.S2C, WaterWalkPayload.TYPE, WaterWalkPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientWaterWalk.set(payload.active())));
        PlatformServices.NETWORK.register(NetworkService.Side.C2S, ToggleWaterWalkPayload.TYPE,
                ToggleWaterWalkPayload.CODEC, (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        toggle(player);
                    }
                }));
    }

    /**
     * Alterna la caminata sobre el agua del jugador y se lo dice (F10, peticion de la verificacion en partida).
     *
     * <p>Al desactivarla se corta el sostenimiento en el acto: si estabas de pie sobre el agua, caes. Es lo
     * esperado — la tecla existe justo para eso.
     *
     * @return {@code true} si queda <b>activada</b>
     */
    public static boolean toggle(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        VisionState next = data.vision().toggledWaterWalk();
        data.setVision(next);
        boolean active = !next.waterWalkOff();
        if (!active) {
            stop(player); // deja de sostenerse ya, sin esperar al siguiente tick
        }
        player.sendSystemMessage(Component.translatable(
                active ? "waterwalk.dojutsu.on" : "waterwalk.dojutsu.off").withStyle(ChatFormatting.AQUA));
        NinjaSync.sendDelta(player);
        return active;
    }

    /**
     * Un golpe te tira al agua ({@code water_walking/hit}, disparado por el advancement
     * {@code entity_hurt_player}). El datapack usa una levitacion de amplificador -128 <b>y un armor stand
     * auxiliar</b> al que te teletransporta para recolocarte; aqui basta con el empujon y cortar el
     * sostenimiento — desaparecen la entidad auxiliar, su funcion {@code fix} y el {@code advancement revoke}.
     */
    public static void onHurt(ServerPlayer player) {
        if (CHARGE_TIMER.containsKey(player.getUUID())) {
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 1, WaterWalkTable.SINK_AMPLIFIER,
                    true, false, false));
            stop(player);
        }
    }

    /** Olvida el estado transitorio de un jugador (al desconectar). */
    public static void forget(UUID playerId) {
        CHARGE_TIMER.remove(playerId);
    }

    /** {@code true} si ese jugador se esta sosteniendo ahora mismo (comando de QA). */
    public static boolean isWalking(UUID playerId) {
        return CHARGE_TIMER.containsKey(playerId);
    }
}
