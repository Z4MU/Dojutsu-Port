package com.zamu.dojutsu.progression;

import java.util.ArrayList;
import java.util.List;

/**
 * Registro de los hitos de desbloqueo ({@link UnlockDefinition}) cargados desde datos ({@code [S20]}). Lo
 * puebla {@code UnlockReloadListener} en cada carga/{@code /reload}; lo consulta {@code UnlockService}.
 *
 * <p>Reemplaza el polling {@code unlocks} por tick del datapack: en vez de revisar todos los hitos cada tick,
 * se consulta O(hitos) solo cuando una habilidad cambia de nivel (evento).
 */
public final class UnlockRegistry {

    private static volatile List<UnlockDefinition> definitions = List.of();

    private UnlockRegistry() {
    }

    /** Reemplaza el conjunto de hitos (llamado por el reload listener). */
    public static void load(List<UnlockDefinition> loaded) {
        definitions = List.copyOf(loaded);
    }

    /**
     * Ids de los hitos que se <b>cruzan</b> al subir de {@code oldLevel} a {@code newLevel} en {@code skillId}
     * (es decir, {@code oldLevel < requiredLevel <= newLevel}). Vacio si no se cruza ninguno (incluido bajar de
     * nivel: no se revoca nada).
     */
    public static List<UnlockDefinition> unlocksCrossing(String skillId, int oldLevel, int newLevel) {
        List<UnlockDefinition> result = new ArrayList<>();
        for (UnlockDefinition def : definitions) {
            if (def.skillId().equals(skillId) && oldLevel < def.requiredLevel() && def.requiredLevel() <= newLevel) {
                result.add(def);
            }
        }
        return result;
    }

    /**
     * Hitos de {@code skillId} <b>ya alcanzados</b> con ese nivel (F10/DJT-221). Lo usa el barrido de puesta al
     * dia del {@code UnlockService}.
     *
     * <p><b>Por que hace falta:</b> {@link #unlocksCrossing} solo ve el <b>instante</b> en que se sube de nivel,
     * y hay cuatro hitos que exigen <b>ademas</b> chakra maximo. Si el chakra llega despues de la habilidad, ese
     * instante ya paso y el hito no se otorgaria jamas. El datapack no tenia el problema porque revisaba los
     * hitos <b>cada tick</b>; aqui se recupera esa garantia con un barrido al entrar, que es O(hitos) una vez por
     * sesion en vez de por tick.
     */
    public static List<UnlockDefinition> unlocksReached(String skillId, int level) {
        List<UnlockDefinition> result = new ArrayList<>();
        for (UnlockDefinition def : definitions) {
            if (def.skillId().equals(skillId) && def.requiredLevel() <= level) {
                result.add(def);
            }
        }
        return result;
    }

    /** Vista de solo lectura de los hitos cargados (inspeccion/tests). */
    public static List<UnlockDefinition> all() {
        return definitions;
    }
}
