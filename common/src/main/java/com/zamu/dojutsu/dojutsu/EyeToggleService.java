package com.zamu.dojutsu.dojutsu;

import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.EyeState;
import com.zamu.dojutsu.network.ToggleEyePayload;
import com.zamu.dojutsu.platform.NetworkService;
import com.zamu.dojutsu.platform.PlatformServices;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Encendido y apagado del dojutsu (F11/DJT-230): la tecla que activa y desactiva el ojo, con la textura y los
 * poderes que lleva detras.
 *
 * <h2>Esto es contenido nuevo, no un port</h2>
 * El datapack <b>no tiene interruptor</b>: alli el ojo esta puesto o no lo esta, porque es un item en el slot
 * de casco, y "apagarlo" es quitarselo ({@code give/check} lo devuelve al inventario). Como el mod colapsa ese
 * item en estado (R4), hacia falta una forma explicita de decir "lo tengo pero ahora mismo no lo uso".
 *
 * <h2>Tener el ojo y tenerlo encendido son cosas distintas</h2>
 * {@link EyeState#activeEye()} responde "de que ojo soy dueno" y {@link EyeState#poweredEye()} responde "que
 * poderes me funcionan ahora". Apagar <b>conserva</b> el ojo y su forma eterna; lo unico que se pierde
 * mientras esta apagado son los poderes (habilidades, chakra vision) y las penalizaciones de fatiga.
 *
 * <h2>La fatiga manda sobre el interruptor</h2>
 * Con la fatiga al maximo no se puede encender: {@code eye_strain/force_deactivate} apaga el ojo al llegar al
 * 100%, y sin esta guarda bastaria volver a pulsar la tecla para saltarse esa penalizacion. El Rinne Sharingan
 * queda exento, igual que lo esta del apagado forzado ({@code unless wearing_eye_10}).
 *
 * <h2>Rate-limit</h2>
 * El estado del ojo lo ven los demas jugadores, asi que alternar sin freno haria parpadear la textura en todos
 * los clientes que rastreen al jugador. {@link #COOLDOWN_TICKS} es el minimo entre pulsaciones aceptadas; el
 * exceso se descarta en silencio (no es un error del jugador, es la tecla repitiendo).
 */
public final class EyeToggleService {

    /** Minimo entre alternancias aceptadas, en ticks (medio segundo). */
    static final int COOLDOWN_TICKS = 10;

    /** Ultimo tick de mundo en que cada jugador alterno con exito. */
    private static final Map<UUID, Long> LAST_TOGGLE = new ConcurrentHashMap<>();

    private EyeToggleService() {
    }

    /**
     * Alterna el dojutsu del jugador.
     *
     * @return {@code true} si el ojo quedo encendido
     */
    public static boolean toggle(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        EyeState eye = data.eye();

        Optional<String> owned = eye.activeEye();
        if (owned.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("dojutsu.dojutsu.toggle.no_eye").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        if (!consumeCooldown(player)) {
            return eye.enabled(); // la tecla repitiendo: se ignora sin ruido
        }
        boolean turningOn = !eye.enabled();
        if (turningOn && blockedByStrain(eye)) {
            player.displayClientMessage(
                    Component.translatable("eye_strain.dojutsu.too_strained").withStyle(ChatFormatting.DARK_RED),
                    false);
            player.displayClientMessage(
                    Component.translatable("eye_strain.dojutsu.at_percent", eye.eyeStrain())
                            .withStyle(ChatFormatting.RED),
                    false);
            return false;
        }

        data.setEye(eye.withEnabled(turningOn));
        announce(player, owned.get(), turningOn);
        return turningOn;
    }

    /**
     * Decision pura: la fatiga impide encender el ojo. Al maximo de fatiga el ojo se apaga solo, asi que
     * permitir encenderlo dejaria la penalizacion en nada. El ojo exento del apagado forzado tampoco se bloquea
     * aqui, para que las dos reglas digan lo mismo.
     */
    static boolean blockedByStrain(EyeState eye) {
        Optional<String> owned = eye.activeEye();
        if (owned.isEmpty() || owned.filter(EyeStrainService.STRAINLESS_EYE::equals).isPresent()) {
            return false; // sin ojo no hay nada que vetar; el exento del apagado forzado tampoco se veta
        }
        return eye.eyeStrain() >= EyeStrainService.FORCE_DEACTIVATE_AT;
    }

    /** Olvida el rate-limit del jugador (al desconectar): no debe sobrevivir a la sesion. */
    public static void forget(UUID playerId) {
        LAST_TOGGLE.remove(playerId);
    }

    /** Registra el payload C2S de alternar el ojo. Llamar en la inicializacion del mod. */
    public static void register() {
        PlatformServices.NETWORK.register(
                NetworkService.Side.C2S,
                ToggleEyePayload.TYPE,
                ToggleEyePayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        toggle(player);
                    }
                }));
    }

    /**
     * Consume el rate-limit: {@code true} si ha pasado el enfriamiento (y lo reinicia), {@code false} si la
     * pulsacion llega demasiado pronto.
     */
    private static boolean consumeCooldown(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        Long last = LAST_TOGGLE.get(player.getUUID());
        if (last != null && now - last < COOLDOWN_TICKS) {
            return false;
        }
        LAST_TOGGLE.put(player.getUUID(), now);
        return true;
    }

    /** Aviso en la barra de accion + sonido, para que el cambio se note sin abrir ningun menu. */
    private static void announce(ServerPlayer player, String eyeId, boolean on) {
        Component name = Component.translatable("eye.dojutsu." + eyeId);
        player.displayClientMessage(
                Component.translatable(on ? "dojutsu.dojutsu.toggle.on" : "dojutsu.dojutsu.toggle.off", name)
                        .withStyle(on ? ChatFormatting.AQUA : ChatFormatting.GRAY),
                true);
        // Al encender suena el propio ojo (el mismo del despertar); al apagar, un clic neutro y tenue.
        if (on) {
            EyeRegistry.get(eyeId).ifPresent(def -> EyeSounds.playAwaken(player, def.family()));
        } else {
            player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.25F, 0.8F);
        }
    }
}
