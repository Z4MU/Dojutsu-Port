package com.zamu.dojutsu.data.migration;

import com.zamu.dojutsu.data.DataVersion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;

/**
 * Aplica en orden las migraciones de {@link com.zamu.dojutsu.data.PlayerNinjaData} hasta la version
 * actual, de forma <b>idempotente</b>.
 *
 * <p>Lee la version del NBT y aplica cada {@link Migration} desde esa version hasta {@code currentVersion},
 * escribiendo la nueva version tras cada paso. La idempotencia (S20-R4) sale del <em>version-gating</em>:
 * una vez el NBT esta en la version actual, volver a migrar no hace nada. El input no se muta (se copia).
 *
 * <ul>
 *   <li><b>Version ausente</b> -&gt; {@link DataVersion#INITIAL} (se migra desde la mas antigua).</li>
 *   <li><b>Version futura</b> (&gt; actual) -&gt; no se migra; se avisa y se devuelve tal cual.</li>
 *   <li><b>Falta un paso intermedio</b> -&gt; error (no se salta ninguna migracion).</li>
 * </ul>
 */
public final class MigrationChain {

    private static final System.Logger LOGGER = System.getLogger("dojutsu.data.migration");

    private final int currentVersion;
    private final Map<Integer, Migration> stepsByFromVersion;

    /**
     * @param currentVersion version objetivo a la que se migra
     * @param migrations     pasos disponibles (un unico paso por {@code fromVersion})
     * @throws IllegalArgumentException si hay dos pasos con el mismo {@code fromVersion}
     */
    public MigrationChain(int currentVersion, List<Migration> migrations) {
        this.currentVersion = currentVersion;
        this.stepsByFromVersion = new HashMap<>();
        for (Migration migration : Objects.requireNonNull(migrations, "migrations")) {
            if (stepsByFromVersion.put(migration.fromVersion(), migration) != null) {
                throw new IllegalArgumentException(
                        "Dos migraciones desde la version " + migration.fromVersion());
            }
        }
    }

    /** Cadena con todas las migraciones conocidas del esquema actual. */
    public static MigrationChain forCurrentSchema() {
        return new MigrationChain(DataVersion.CURRENT, List.of(
                // v0 (esquema previo al versionado) -> v1: placeholder, sin cambios estructurales.
                Migration.of(0, UnaryIdentity.INSTANCE)
        ));
    }

    /**
     * Devuelve una copia del NBT migrada a la version actual. No modifica el argumento.
     *
     * @throws IllegalStateException si falta un paso de migracion intermedio
     */
    public CompoundTag migrate(CompoundTag input) {
        CompoundTag tag = input.copy();
        int version = DataVersion.read(tag);
        if (version > currentVersion) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "PlayerNinjaData con version futura {0} (actual {1}); no se migra",
                    version, currentVersion);
            return tag;
        }
        while (version < currentVersion) {
            Migration step = stepsByFromVersion.get(version);
            if (step == null) {
                throw new IllegalStateException("Falta migracion desde la version " + version);
            }
            tag = step.migrate(tag);
            version++;
            tag.putInt(DataVersion.KEY, version);
        }
        return tag;
    }

    /** Identidad reutilizable para pasos placeholder (evita crear lambdas equivalentes). */
    private enum UnaryIdentity implements java.util.function.UnaryOperator<CompoundTag> {
        INSTANCE;

        @Override
        public CompoundTag apply(CompoundTag tag) {
            return tag;
        }
    }
}
