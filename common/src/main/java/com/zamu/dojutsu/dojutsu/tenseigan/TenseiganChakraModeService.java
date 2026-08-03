package com.zamu.dojutsu.dojutsu.tenseigan;

import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.core.NinjaSfx;
import com.zamu.dojutsu.core.PlayerTickSystem;
import com.zamu.dojutsu.core.TickPhase;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.domain.event.ModeToggledEvent;
import com.zamu.dojutsu.domain.tenseigan.TenseiganChakraMode;
import com.zamu.dojutsu.domain.tenseigan.TenseiganChakraModeContext;
import com.zamu.dojutsu.empowerment.ToggleableModeService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Servicio del Tenseigan Chakra Mode {@code [S05]} (cierre de flecos de F5): la glue del
 * {@link TenseiganChakraMode} — un modo por jugador, tickeado en {@link TickPhase#DOJUTSU}, que traduce los hooks
 * a Minecraft (chakra, buffs, vuelo). Mismo montaje que {@code SusanooService}, sin nivel ni avatar.
 *
 * <p><b>Se enciende desde el menu ocular + tecla R</b> ({@code TenseiganChakraModeAbility}), como el Susanoo, sin
 * tecla nueva: {@code tenseigan_chakra_mode} ya es una habilidad declarada de {@code tenseigan.json}. El estado
 * de encendido es de ejecucion (no se guarda); el vuelo se retira al apagar, al caer por chakra y al desconectar.
 *
 * <h2>Sobre el arnes compartido (F10/DJT-226)</h2>
 * Este servicio decia: <i>"este es el segundo servicio que replica el bucle tick el modo → si cae, avisa y
 * limpia. Cuando F6 traiga sage/sellos/puertas/SOAH conviene extraer un arnes compartido"</i>. F6 lo extrajo
 * ({@link ToggleableModeService}, DJT-130) y lo adoptaron los cinco modos de F6/F7, pero estos dos de F5 se
 * quedaron fuera. Ya no: el hospedaje —modo por jugador, tick, caida, evento y sync— vive en el arnes, y aqui
 * queda solo lo propio del Tenseigan (el contexto y el vuelo).
 *
 * <p><b>Un cambio de comportamiento, y es una correccion:</b> antes, cuando el modo <b>caia</b> por chakra
 * agotado, se avisaba al jugador pero <b>no se emitia</b> {@code ModeToggledEvent} — el Susanoo si lo emitia en
 * el mismo caso. Ahora lo emite el arnes, igual para los siete modos. Hoy es inerte (nadie se suscribe al
 * evento todavia), pero deja de ser una diferencia arbitraria entre modos.
 */
public final class TenseiganChakraModeService implements PlayerTickSystem {

    /** Id del modo para {@link ModeToggledEvent}. */
    public static final String MODE_ID = "tenseigan_chakra_mode";

    /** Pulso por jugador (ticks hasta el proximo cobro); ejecucion, no guardado. */
    private static final Map<UUID, Integer> PULSE_TIMERS = new ConcurrentHashMap<>();

    /** El arnes compartido: hospeda el modo, lo tickea y limpia el vuelo cuando cae. */
    private static final ToggleableModeService<TenseiganChakraMode, TenseiganChakraModeContext> MODES =
            new ToggleableModeService<>(MODE_ID, TenseiganChakraMode::new, PlayerContext::new,
                    (player, context) -> onFall(context));

    @Override
    public TickPhase phase() {
        return TickPhase.DOJUTSU;
    }

    @Override
    public void tick(ServerPlayer player) {
        MODES.tick(player);
    }

    /**
     * Enciende o apaga el chakra mode del jugador (lo llama {@code TenseiganChakraModeAbility}).
     *
     * @return {@code true} si quedo encendido
     */
    public static boolean toggle(ServerPlayer player) {
        return MODES.toggle(player);
    }

    /** Indica si el jugador tiene el chakra mode activo. */
    public static boolean isActive(UUID playerId) {
        return MODES.isActive(playerId);
    }

    /** Olvida el modo y su pulso, y retira el vuelo si estaba activo (al desconectar). */
    public static void forget(ServerPlayer player) {
        TenseiganChakraMode mode = MODES.forget(player.getUUID());
        PULSE_TIMERS.remove(player.getUUID());
        if (mode != null && mode.isActive()) {
            new PlayerContext(player).setFlight(false);
        }
    }

    /** Al caer por chakra agotado (el apagado no paso por {@code onDeactivate}): retira el vuelo y avisa. */
    private static void onFall(TenseiganChakraModeContext context) {
        context.setFlight(false);
        context.notifyToggled(false);
    }

    /** Contexto del modo sobre un jugador real: la unica pieza que conoce a la vez el modo y Minecraft. */
    private record PlayerContext(ServerPlayer player) implements TenseiganChakraModeContext {

        @Override
        public int chakra() {
            return NinjaData.get(player).chakra().chakra();
        }

        @Override
        public boolean consumeChakra(int amount) {
            return amount <= 0 || ChakraService.consume(NinjaData.get(player), amount);
        }

        @Override
        public int pulseTimer() {
            return PULSE_TIMERS.getOrDefault(player.getUUID(), 0);
        }

        @Override
        public void setPulseTimer(int ticks) {
            PULSE_TIMERS.put(player.getUUID(), Math.max(ticks, 0));
        }

        @Override
        public void applyAura() {
            // duracion 2 ticks (no 1) para que no parpadee entre ticks del pipeline; verificados del datapack
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2, 1, true, false));      // Fuerza II
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2, 1, true, false));    // Velocidad II
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 2, 3, true, false));              // Salto IV
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2, 1, true, false)); // Resistencia II
            setFlight(true); // se re-asegura por si otra causa lo quito
        }

        @Override
        public void setFlight(boolean allowed) {
            if (player.isCreative() || player.isSpectator()) {
                return; // ya vuelan por su cuenta; no tocamos sus habilidades
            }
            if (player.getAbilities().mayfly == allowed) {
                return;
            }
            player.getAbilities().mayfly = allowed;
            if (!allowed) {
                player.getAbilities().flying = false;
            }
            player.onUpdateAbilities(); // sincroniza al cliente
        }

        @Override
        public void notifyToggled(boolean active) {
            player.sendSystemMessage(Component.translatable(
                    active ? "tenseigan.dojutsu.chakra_mode.on" : "tenseigan.dojutsu.chakra_mode.off"));
            // tenseigan/toggle: los dos a 0.5 y tono 2 en 5 bloques. El tono de encendido estaba en 1.0
            // (F10/DJT-227) — el par sonaba desparejo. El modo usa sonidos de vanilla a proposito: el
            // dojutsu.tenseigan.activate del pack es del OJO al despertar, y lo toca EyeSounds.
            NinjaSfx.at(player, active ? SoundEvents.BEACON_POWER_SELECT : SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS, 0.5F, 2.0F, 5.0D);
        }
    }
}
