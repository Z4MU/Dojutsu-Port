package com.zamu.dojutsu.client;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.data.state.OnboardingState;
import com.zamu.dojutsu.network.JutsuSnapshot;
import com.zamu.dojutsu.network.ProgressionSnapshot;
import java.util.List;
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
    private static List<String> known = List.of();          // jutsus aprendidos, en orden estable
    private static Optional<String> selectedJutsu = Optional.empty();
    private static List<String> quickSlots = List.of();     // reparto de la rueda, 8 entradas ("" = libre)
    private static boolean hasFull;

    private ClientNinjaData() {
    }

    /** Aplica un snapshot completo (habilita la cache). */
    public static void applyFull(OnboardingState.Phase phase, int chakraValue, int maxChakraValue,
            ProgressionSnapshot progressionValue, JutsuSnapshot jutsuValue) {
        onboarding = phase;
        chakra = chakraValue;
        maxChakra = maxChakraValue;
        progression = progressionValue;
        cooldowns = Map.copyOf(jutsuValue.cooldowns());
        // El full siempre trae la lista; si llegara sin ella, mejor vaciarla que arrastrar la de otra partida.
        known = jutsuValue.known().map(List::copyOf).orElse(List.of());
        selectedJutsu = jutsuValue.selected();
        quickSlots = jutsuValue.slots().map(List::copyOf).orElse(List.of());
        hasFull = true;
        LOGGER.info("[{}] Cache cliente (full): onboarding={} chakra={}/{} puntos={} jutsus={}.",
                DojutsuMod.MOD_NAME, phase, chakraValue, maxChakraValue, progressionValue.skillPoints(),
                known.size());
    }

    /** Aplica un delta; se ignora si aun no llego un full (evita estado incoherente). */
    public static void applyDelta(OnboardingState.Phase phase, int chakraValue, int maxChakraValue,
            ProgressionSnapshot progressionValue, JutsuSnapshot jutsuValue) {
        if (!hasFull) {
            LOGGER.warn("[{}] Delta recibido antes del primer full; ignorado.", DojutsuMod.MOD_NAME);
            return;
        }
        onboarding = phase;
        chakra = chakraValue;
        maxChakra = maxChakraValue;
        progression = progressionValue;
        cooldowns = Map.copyOf(jutsuValue.cooldowns());
        // Lista ausente = "no ha cambiado": se CONSERVA la que ya habia. Si aqui se asignara List.of() cuando
        // viene vacia, el selector de jutsu se vaciaria en el siguiente pulso de regeneracion de chakra.
        jutsuValue.known().ifPresent(list -> known = List.copyOf(list));
        // La seleccion SI viaja en todos los deltas (es un id), asi que aqui vacio significa "ninguna".
        selectedJutsu = jutsuValue.selected();
        jutsuValue.slots().ifPresent(list -> quickSlots = List.copyOf(list)); // ausente = sin cambios
    }

    /** Limpia la cache (al cambiar de mundo / desconectar). */
    public static void clear() {
        onboarding = null;
        chakra = 0;
        maxChakra = 0;
        progression = ProgressionSnapshot.EMPTY;
        cooldowns = Map.of();
        known = List.of();
        selectedJutsu = Optional.empty();
        quickSlots = List.of();
        hasFull = false;
    }

    /** Progresion observada (puntos, ninja_xp, niveles; rango derivable). Vacia si aun no hay sync. */
    public static ProgressionSnapshot progression() {
        return progression;
    }

    /**
     * Jutsus aprendidos, en orden estable (F11/DJT-231): lo que el selector de jutsu dibuja. Vacia si aun no
     * hay sync.
     *
     * <p>Es una vista <b>de presentacion</b>: sirve para decidir que se muestra, nunca para decidir si un cast
     * procede. Eso lo valida el servidor en {@code JutsuCastPipeline} (regla 12).
     */
    public static List<String> known() {
        return known;
    }

    /** Indica si el jugador tiene aprendido un jutsu (solo para pintar la interfaz, no para autorizar nada). */
    public static boolean knows(String jutsuId) {
        return known.contains(jutsuId);
    }

    /**
     * Jutsu seleccionado para la tecla de cast (F11/DJT-231), observado del servidor. Vacio si no hay ninguno.
     *
     * <p>El cliente lo <b>muestra</b>; no lo decide. Al elegir en el selector se manda la intencion y la UI se
     * refresca cuando vuelve el sync, igual que el resto de acciones (regla 12).
     */
    public static Optional<String> selectedJutsu() {
        return selectedJutsu;
    }

    /**
     * Reparto observado de los huecos de la rueda (F11/DJT-234): 8 entradas, {@code ""} = hueco libre. Vacia
     * si aun no hay sync.
     *
     * <p>Como todo en esta cache, es <b>presentacion</b>: la rueda dibuja esto, pero quien decide que se
     * puede lanzar sigue siendo el servidor.
     */
    public static List<String> quickSlots() {
        return quickSlots;
    }

    /** Jutsu del hueco dado, o vacio si esta libre o el indice cae fuera. */
    public static Optional<String> slot(int index) {
        if (index < 0 || index >= quickSlots.size()) {
            return Optional.empty();
        }
        String id = quickSlots.get(index);
        return id.isEmpty() ? Optional.empty() : Optional.of(id);
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
