package com.zamu.dojutsu.client;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.data.state.OnboardingState;
import com.zamu.dojutsu.network.ProgressionSnapshot;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;

/**
 * Cache <b>de solo lectura</b> del estado ninja observable en el <b>cliente</b>. La alimentan los payloads
 * de sync ({@code SyncNinjaDataPayload}); el HUD/GUI la consultan.
 *
 * <p>Reglas: es <b>solo cliente</b> (el servidor nunca la lee ni la importa); el cliente <b>nunca</b> decide
 * gameplay a partir de ella (es autoridad del servidor). No se persiste. Ignora deltas recibidos antes del
 * primer snapshot completo y se limpia al cambiar de mundo.
 *
 * <p>Estado estatico (un unico cliente por proceso). No depende de clases de MC-cliente, asi que es inocuo
 * aunque no se cargue en servidor dedicado.
 */
public final class ClientNinjaData {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static OnboardingState.Phase onboarding; // null hasta el primer full
    private static int chakra;    // significativo solo cuando hasFull
    private static int maxChakra; // significativo solo cuando hasFull
    private static ProgressionSnapshot progression = ProgressionSnapshot.EMPTY;
    private static Map<String, Long> cooldowns = Map.of(); // jutsuId -> tick de mundo disponible
    private static boolean hasFull;

    private ClientNinjaData() {
    }

    /** Aplica un snapshot completo (habilita la cache). */
    public static void applyFull(OnboardingState.Phase phase, int chakraValue, int maxChakraValue,
            ProgressionSnapshot progressionValue, Map<String, Long> cooldownsValue) {
        onboarding = phase;
        chakra = chakraValue;
        maxChakra = maxChakraValue;
        progression = progressionValue;
        cooldowns = Map.copyOf(cooldownsValue);
        hasFull = true;
        LOGGER.info("[{}] Cache cliente (full): onboarding={} chakra={}/{} puntos={}.",
                DojutsuMod.MOD_NAME, phase, chakraValue, maxChakraValue, progressionValue.skillPoints());
    }

    /** Aplica un delta; se ignora si aun no llego un full (evita estado incoherente). */
    public static void applyDelta(OnboardingState.Phase phase, int chakraValue, int maxChakraValue,
            ProgressionSnapshot progressionValue, Map<String, Long> cooldownsValue) {
        if (!hasFull) {
            LOGGER.warn("[{}] Delta recibido antes del primer full; ignorado.", DojutsuMod.MOD_NAME);
            return;
        }
        onboarding = phase;
        chakra = chakraValue;
        maxChakra = maxChakraValue;
        progression = progressionValue;
        cooldowns = Map.copyOf(cooldownsValue);
        LOGGER.info("[{}] Cache cliente (delta): onboarding={} chakra={}/{} puntos={}.",
                DojutsuMod.MOD_NAME, phase, chakraValue, maxChakraValue, progressionValue.skillPoints());
    }

    /** Limpia la cache (al cambiar de mundo / desconectar). */
    public static void clear() {
        onboarding = null;
        chakra = 0;
        maxChakra = 0;
        progression = ProgressionSnapshot.EMPTY;
        cooldowns = Map.of();
        hasFull = false;
    }

    /** Progresion observada (puntos, ninja_xp, niveles; rango derivable). Vacia si aun no hay sync. */
    public static ProgressionSnapshot progression() {
        return progression;
    }

    /** Indica si la cache ya recibio un snapshot completo. */
    public static boolean hasData() {
        return hasFull;
    }

    /** Fase de onboarding observable, o vacio si aun no hay sync. */
    public static Optional<OnboardingState.Phase> onboarding() {
        return Optional.ofNullable(onboarding);
    }

    /** Chakra normal actual observado (0 si aun no hay sync; comprobar {@link #hasData()}). Para el HUD. */
    public static int chakra() {
        return chakra;
    }

    /** Chakra normal maximo observado (0 si aun no hay sync; comprobar {@link #hasData()}). Para el HUD. */
    public static int maxChakra() {
        return maxChakra;
    }

    /**
     * Ticks de cooldown restantes de un jutsu al tick de mundo dado (0 si esta disponible). Para el HUD
     * de cd (DJT-081). El {@code nowTick} lo aporta el cliente ({@code level.getGameTime}, sincronizado
     * con el servidor); un cooldown ya vencido computa 0 aunque el mapa aun lo tenga.
     */
    public static long remainingCooldown(String jutsuId, long nowTick) {
        Long availableAt = cooldowns.get(jutsuId);
        return availableAt == null ? 0L : Math.max(0L, availableAt - nowTick);
    }
}
