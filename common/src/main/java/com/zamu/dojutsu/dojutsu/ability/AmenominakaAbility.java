package com.zamu.dojutsu.dojutsu.ability;

import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.EyeState;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.registry.ModSounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * <b>Amenominaka</b> {@code [S05]} (F10/DJT-226): la técnica del Rinne Sharingan que arrastra al lanzador
 * <b>y a todo el que tenga cerca</b> a otra dimensión.
 *
 * <h2>Es un traslado colectivo, no un teleport</h2>
 * Lo que la distingue del resto del catálogo ocular está en el selector del datapack:
 * {@code as @a[distance=0..10] in <dimension> run tp @s ~ ~ ~}. No te llevas a ti: te llevas a <b>todos los
 * jugadores</b> en 10 bloques, contigo, a las mismas coordenadas de otra dimensión. Es la técnica más cara del
 * juego ({@value #CHAKRA_COST} de chakra, cuatro veces el Shinra Tensei) y se entiende leyendo eso: no es
 * movilidad, es secuestrar una pelea entera.
 *
 * <h2>La vuelta a casa se gana al irse</h2>
 * {@code teleport/nether} y {@code teleport/the_end} llaman a {@code set_original_pos} <b>sólo</b>
 * {@code if Dimension == overworld}: la posición de regreso se graba al <b>salir</b> del Overworld, y volver al
 * Overworld la borra ({@code remove_pos}). Por eso «Posición original» aparece tachada hasta que te has ido de
 * verdad — el gate {@code amenominaka_req2} exige {@code d.amenom_pos == 1} además del chakra y el cooldown.
 *
 * <p><b>Sin {@code forceload}</b> (mismo arreglo que la marca del Dios del Trueno, DJT-180): el datapack invoca
 * un {@code marker} y hace {@code forceload add} de su chunk, o sea que <b>mantiene un trozo de mundo cargado</b>
 * mientras tengas un regreso pendiente. Aquí el origen es un {@link GlobalPos} en {@link EyeState}: cuesta cero
 * y sobrevive a los reinicios, que el marker no hacía si alguien limpiaba entidades.
 *
 * <h2>Cuatro entradas en el menú, una clase</h2>
 * El datapack abre una <b>subpágina</b> con cuatro destinos. El mod tiene un menú de verdad desde DJT-117, así
 * que la subpágina se disuelve en cuatro entradas de la lista, servidas por esta misma clase parametrizada por
 * {@link Destination}.
 */
public final class AmenominakaAbility implements EyeAbility {

    /** Chakra que cuesta ({@code teleport/*}: {@code if score @s d.chakra matches 8000..}). */
    public static final int CHAKRA_COST = 8000;

    /** Fatiga ocular: el datapack no añade ninguna a esta técnica. */
    public static final int STRAIN_COST = 0;

    /** Ticks de reutilización ({@code scoreboard players set @s d.amenom_cd 100}). */
    public static final int COOLDOWN_TICKS = 100;

    /** Radio en bloques del arrastre colectivo ({@code @a[distance=0..10]}). */
    public static final double PULL_RADIUS = 10.0D;

    /** Destino de un salto. */
    public enum Destination {
        /** Al Overworld; borra la posición de regreso. */
        OVERWORLD(Level.OVERWORLD, "overworld"),
        /** Al Nether; graba la posición de regreso si sales del Overworld. */
        NETHER(Level.NETHER, "nether"),
        /** Al End; graba la posición de regreso si sales del Overworld. */
        THE_END(Level.END, "the_end"),
        /** De vuelta al punto exacto desde el que saliste ({@code tp_original}). */
        ORIGINAL(null, "original");

        private final ResourceKey<Level> dimension;
        private final String id;

        Destination(ResourceKey<Level> dimension, String id) {
            this.dimension = dimension;
            this.id = id;
        }

        /** Id de la habilidad en el catálogo y en el JSON del ojo ({@code "amenominaka_nether"}). */
        public String abilityId() {
            return "amenominaka_" + id;
        }
    }

    private final Destination destination;

    public AmenominakaAbility(Destination destination) {
        this.destination = destination;
    }

    /** Destino de esta entrada del menú. */
    public Destination destination() {
        return destination;
    }

    @Override
    public int chakraCost() {
        return CHAKRA_COST;
    }

    @Override
    public int strainCost() {
        return STRAIN_COST;
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    /**
     * Aborta <b>sin cobrar</b> si el salto no tiene sentido: ir a la dimensión en la que ya estás, o pedir el
     * regreso sin tener a dónde volver. Es el equivalente exacto de las entradas tachadas del actionbar y del
     * {@code tag @s remove cast} del datapack, que tampoco cobraban.
     */
    @Override
    public boolean canApply(EyeAbilityContext context) {
        ServerPlayer caster = context.caster();
        if (destination == Destination.ORIGINAL) {
            return origin(caster).isPresent();
        }
        return caster.level().dimension() != destination.dimension;
    }

    @Override
    public void execute(EyeAbilityContext context) {
        ServerPlayer caster = context.caster();
        ServerLevel from = context.level();

        if (destination == Destination.ORIGINAL) {
            origin(caster).ifPresent(back -> {
                ServerLevel target = from.getServer().getLevel(back.dimension());
                if (target != null) {
                    shift(from, caster, target, Vec3.atCenterOf(back.pos()));
                }
                forgetOrigin(caster);
            });
            return;
        }

        ServerLevel target = from.getServer().getLevel(destination.dimension);
        if (target == null) {
            return; // dimension deshabilitada en este servidor
        }
        // set_original_pos: solo al SALIR del Overworld; volver a el borra el regreso (remove_pos)
        if (from.dimension() == Level.OVERWORLD) {
            rememberOrigin(caster, from, caster.blockPosition());
        } else if (destination == Destination.OVERWORLD) {
            forgetOrigin(caster);
        }
        shift(from, caster, target, caster.position());
    }

    /**
     * Arrastra al lanzador y a todos los jugadores en {@value #PULL_RADIUS} bloques a las mismas coordenadas de
     * la dimensión destino.
     */
    private static void shift(ServerLevel from, ServerPlayer caster, ServerLevel target, Vec3 to) {
        List<ServerPlayer> pulled = from.getEntitiesOfClass(ServerPlayer.class,
                new AABB(caster.position(), caster.position()).inflate(PULL_RADIUS));
        if (!pulled.contains(caster)) {
            pulled = new java.util.ArrayList<>(pulled);
            pulled.add(caster);
        }
        for (ServerPlayer player : pulled) {
            from.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    player.getX(), player.getY() + 1, player.getZ(), 60, 0.5D, 1.0D, 0.5D, 0.2D);
            player.teleportTo(target, to.x, to.y, to.z, player.getYRot(), player.getXRot());
            // playsound dojutsu.rinne_sharingan.amenominaka player @s ~ ~ ~ 0.5 1 — el sonido propio ya estaba
            // registrado desde DJT-224 y era uno de los que nadie tocaba
            player.level().playSound(null, player.blockPosition(), ModSounds.AMENOMINAKA.get(),
                    SoundSource.PLAYERS, 0.5F, 1.0F);
            if (player != caster) {
                player.sendSystemMessage(Component.translatable("eye.dojutsu.amenominaka.pulled",
                        caster.getDisplayName()).withStyle(ChatFormatting.DARK_PURPLE));
            }
        }
    }

    private static Optional<GlobalPos> origin(ServerPlayer player) {
        return NinjaData.get(player).eye().amenominakaOrigin();
    }

    private static void rememberOrigin(ServerPlayer player, ServerLevel level, BlockPos pos) {
        PlayerNinjaData data = NinjaData.get(player);
        data.setEye(data.eye().withAmenominakaOrigin(GlobalPos.of(level.dimension(), pos)));
        NinjaSync.sendDelta(player);
    }

    private static void forgetOrigin(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        EyeState eye = data.eye();
        if (eye.amenominakaOrigin().isPresent()) {
            data.setEye(eye.withoutAmenominakaOrigin());
            NinjaSync.sendDelta(player);
        }
    }
}
