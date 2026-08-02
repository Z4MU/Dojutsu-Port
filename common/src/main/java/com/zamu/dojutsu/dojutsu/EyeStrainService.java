package com.zamu.dojutsu.dojutsu;

import com.zamu.dojutsu.chakra.SleepChakraHandler;
import com.zamu.dojutsu.core.PlayerTickSystem;
import com.zamu.dojutsu.core.TickPhase;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.EyeState;
import com.zamu.dojutsu.domain.strain.StrainWarnings;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Fatiga ocular (eye strain) del dojutsu {@code [S05]}: {@code eye_strain/main} del datapack (DJT-116). Sistema
 * por tick en {@link TickPhase#STRAIN}. La fatiga sube al <b>usar</b> habilidades oculares (la puerta
 * {@link #addStrain} la llaman las habilidades de DJT-118+; hoy no hay emisor, como pasaba con
 * {@code addAffinityXp} antes de F4) y baja pasivamente; al 100% apaga el ojo y ciega; dormir la resetea.
 *
 * <p><b>Fiel al datapack:</b>
 * <ul>
 *   <li><b>Regen</b>: cada pulso de {@value #REGEN_PULSE_TICKS} ticks ({@code d.eyes_timer}) la fatiga baja
 *       {@value #REGEN_AMOUNT} (o {@value #REGEN_AMOUNT_HASHIRAMA} con ADN de Hashirama). Solo si hay fatiga.</li>
 *   <li><b>Efectos por umbral</b> (sobre la fatiga ya regenerada): {@value #WEAKNESS_AT} → weakness (todos los
 *       ojos), {@value #BLINDNESS_AT} → blindness, {@value #FORCE_DEACTIVATE_AT} → apaga el ojo. El
 *       <b>Rinne Sharingan</b> ({@value #STRAINLESS_EYE}) esta <b>exento</b> de ceguera y apagado (pero no de
 *       weakness), como en {@code eye_strain/main} (unless {@code wearing_eye_10}).</li>
 *   <li><b>Reset al dormir</b> ({@code chakra/sleep/wake_up}: {@code eye_strain=0}) via {@link #resetStrain},
 *       que llama {@code SleepChakraHandler} (DJT-032).</li>
 * </ul>
 *
 * <p>El {@code eyeStrain} se persiste como {@code int} en {@link EyeState} (no un {@code StrainMeter} mutable);
 * la logica de umbrales aqui reproduce los cortes de {@code [S05]}. Los <b>avisos de chat</b> (25/50/75/90/99,
 * un color por banda, con anti-spam {@code esw_*}: cada banda avisa <b>una vez</b> al entrar) los emite
 * {@link #warnStrain}; el estado "ya avisado" es de ejecucion (se olvida al desconectar y se limpia sin ojo, como
 * el {@code no_dojutsu} del datapack). {@link #regen}, {@link #effectsFor} y {@link #strainWarningFloor} son
 * puros (testeables sin Minecraft).
 */
public final class EyeStrainService implements PlayerTickSystem {

    /** Clave del temporizador de regen en {@link EyeState#timer(String)} ({@code d.eyes_timer}). */
    static final String REGEN_TIMER_KEY = "eye_strain_regen";

    /** Suelos de aviso por banda ({@code esw_25/50/75/90/99}) ya emitidos por jugador (ejecucion, no guardado). */
    private static final java.util.Map<UUID, Set<Integer>> WARNED = new ConcurrentHashMap<>();

    private static final int REGEN_PULSE_TICKS = 200;
    private static final int REGEN_AMOUNT = 1;
    private static final int REGEN_AMOUNT_HASHIRAMA = 3;

    private static final int WEAKNESS_AT = 95;
    private static final int BLINDNESS_AT = 98;
    private static final int FORCE_DEACTIVATE_AT = 100;

    /** Ojo exento de ceguera y apagado por fatiga (Rinne Sharingan, {@code eye:10}). */
    static final String STRAINLESS_EYE = "rinne_sharingan";

    /** Que efectos aplica un nivel de fatiga a un ojo (decision pura). */
    record StrainEffects(boolean weakness, boolean blindness, boolean forceDeactivate) {
    }

    @Override
    public TickPhase phase() {
        return TickPhase.STRAIN;
    }

    @Override
    public void tick(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        EyeState eyeState = data.eye();
        if (eyeState.eyeStrain() == 0 && eyeState.activeEye().isEmpty()) {
            return; // sin fatiga ni ojo activo: nada que hacer (caso comun, sin trabajo por tick)
        }
        EyeState afterRegen = regen(eyeState, data.natures().hashiramaDna());
        EyeState finalState = applyEffects(player, afterRegen);
        if (finalState != eyeState) {
            data.setEye(finalState);
        }
        warnStrain(player, finalState.eyeStrain(), finalState.activeEye().isPresent());
    }

    /**
     * Regen puro: mientras el temporizador cuenta, decrementa; al llegar a 0 baja la fatiga (1, o 3 con ADN de
     * Hashirama) y reinicia el temporizador. Solo actua si hay fatiga (sin fatiga no hay trabajo).
     */
    static EyeState regen(EyeState eyeState, boolean hashiramaDna) {
        if (eyeState.eyeStrain() <= 0) {
            return eyeState;
        }
        int timer = eyeState.timer(REGEN_TIMER_KEY);
        if (timer >= 1) {
            return eyeState.withTimer(REGEN_TIMER_KEY, timer - 1);
        }
        int amount = hashiramaDna ? REGEN_AMOUNT_HASHIRAMA : REGEN_AMOUNT;
        return eyeState.withEyeStrain(eyeState.eyeStrain() - amount).withTimer(REGEN_TIMER_KEY, REGEN_PULSE_TICKS);
    }

    /**
     * Decide (puro) que efectos corresponden a un nivel de fatiga. El {@code strainlessEye} (Rinne Sharingan)
     * queda exento de ceguera y apagado, pero <b>no</b> de weakness (fiel a {@code eye_strain/main}).
     */
    static StrainEffects effectsFor(int strain, boolean strainlessEye) {
        boolean weakness = strain >= WEAKNESS_AT;
        boolean blindness = !strainlessEye && strain >= BLINDNESS_AT;
        boolean forceDeactivate = !strainlessEye && strain >= FORCE_DEACTIVATE_AT;
        return new StrainEffects(weakness, blindness, forceDeactivate);
    }

    /** Aplica los efectos por umbral al jugador (glue) y devuelve el estado (apagado forzado al 100%). */
    private static EyeState applyEffects(ServerPlayer player, EyeState eyeState) {
        if (eyeState.activeEye().isEmpty()) {
            return eyeState; // sin ojo activo no hay penalizacion; la fatiga solo regena
        }
        StrainEffects effects = effectsFor(eyeState.eyeStrain(), STRAINLESS_EYE.equals(eyeState.activeEye().get()));
        if (effects.weakness()) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20, 0, false, false)); // 1s, refrescado
        }
        if (effects.blindness()) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 2400, 0, false, false)); // 120s, refrescado
        }
        if (effects.forceDeactivate()) {
            // force_deactivate del datapack: dos lineas (aviso + porcentaje actual)
            player.displayClientMessage(
                    Component.translatable("eye_strain.dojutsu.too_strained").withStyle(ChatFormatting.DARK_RED),
                    false);
            player.displayClientMessage(
                    Component.translatable("eye_strain.dojutsu.at_percent", eyeState.eyeStrain())
                            .withStyle(ChatFormatting.RED),
                    false);
            return eyeState.withoutActiveEye(); // el ojo se apaga a la fuerza (force_deactivate)
        }
        return eyeState;
    }

    /**
     * Banda de aviso a la que pertenece la fatiga, expresada por su <b>suelo</b> ({@code esw_*}): 25/50/75/90/99;
     * {@code 0} = sin banda. Delega en {@link StrainWarnings}, compartido con la fatiga corporal de las Ocho
     * Puertas desde F6/DJT-145 (los cortes son identicos: es el riesgo S11-R1 saldado).
     */
    static int strainWarningFloor(int strain) {
        return StrainWarnings.floorFor(strain);
    }

    /**
     * Emite el aviso {@code Eye Strain: N%} <b>una vez</b> al entrar en una banda (anti-spam {@code esw_*}), con el
     * color del datapack por banda y un {@code player.hurt} tenue. Sin ojo activo ({@code no_dojutsu}) olvida los
     * avisos para que reequipar vuelva a avisar; los avisos de banda se limpian al bajar de su suelo.
     */
    private static void warnStrain(ServerPlayer player, int strain, boolean eyeActive) {
        Set<Integer> warned = WARNED.computeIfAbsent(player.getUUID(), id -> ConcurrentHashMap.newKeySet());
        if (!eyeActive) {
            warned.clear();
            return;
        }
        warned.removeIf(floor -> strain < floor); // esw_N se limpia al caer por debajo de N
        int floor = strainWarningFloor(strain);
        if (floor > 0 && warned.add(floor)) { // add=true la primera vez en esta banda: avisa una sola vez
            player.sendSystemMessage(strainMessage(strain, floor));
            player.playNotifySound(SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.1f, 0.65f);
        }
    }

    /** Mensaje coloreado {@code Eye Strain: N%} (color del numero por banda, como el datapack). */
    private static Component strainMessage(int strain, int floor) {
        return Component.empty()
                .append(Component.translatable("eye_strain.dojutsu.label")
                        .withStyle(s -> s.withColor(ChatFormatting.RED).withBold(true)))
                .append(Component.literal(": ").withStyle(s -> s.withColor(ChatFormatting.DARK_RED).withBold(false)))
                .append(Component.literal(strain + "%").withStyle(s -> s.withColor(colorFor(floor)).withBold(false)));
    }

    /** Color del datapack por banda: 25 verde, 50 amarillo, 75 oro, 90 rojo, 99 rojo oscuro. */
    private static ChatFormatting colorFor(int floor) {
        return switch (floor) {
            case 25 -> ChatFormatting.GREEN;
            case 50 -> ChatFormatting.YELLOW;
            case 75 -> ChatFormatting.GOLD;
            case 90 -> ChatFormatting.RED;
            default -> ChatFormatting.DARK_RED; // 99
        };
    }

    /** Olvida los avisos de fatiga de un jugador (al desconectar): el "ya avisado" no sobrevive a la sesion. */
    public static void forgetWarnings(UUID playerId) {
        WARNED.remove(playerId);
    }

    /**
     * Aumenta la fatiga ocular. <b>Puerta unica de subida</b>, la llaman las habilidades oculares (DJT-118+;
     * cada habilidad declara su coste de strain en {@code [S05]}). Ignora cantidades no positivas.
     */
    public static void addStrain(PlayerNinjaData data, int amount) {
        if (amount <= 0) {
            return;
        }
        EyeState eyeState = data.eye();
        data.setEye(eyeState.withEyeStrain(eyeState.eyeStrain() + amount));
    }

    /**
     * Engancha el reset de fatiga al despertar (dependencia descendente {@code dojutsu -> chakra}, patron de los
     * proveedores de {@code RegenService}). La llama {@code DojutsuLifecycle} una vez en el arranque.
     */
    public static void registerSleepReset() {
        SleepChakraHandler.registerWakeHandler(EyeStrainService::resetStrain);
    }

    /**
     * Resetea la fatiga ocular a 0 (dormir la noche completa: {@code chakra/sleep/wake_up}). La invoca el handler
     * de despertar de {@code SleepChakraHandler} (DJT-032). Idempotente.
     */
    public static void resetStrain(PlayerNinjaData data) {
        EyeState eyeState = data.eye();
        if (eyeState.eyeStrain() != 0) {
            data.setEye(eyeState.withEyeStrain(0));
        }
    }
}
