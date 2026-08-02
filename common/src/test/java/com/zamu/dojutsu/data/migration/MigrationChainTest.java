package com.zamu.dojutsu.data.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.zamu.dojutsu.data.DataVersion;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.OnboardingState;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;

/** Tests de la cadena de migracion idempotente (DJT-018). */
class MigrationChainTest {

    /** NBT "legado" v0: onboarding presente, sin clave de version. */
    private static CompoundTag legacyV0() {
        CompoundTag onboarding = new CompoundTag();
        onboarding.putString("phase", OnboardingState.Phase.COMPLETED.name());
        CompoundTag tag = new CompoundTag();
        tag.put("onboarding", onboarding);
        return tag;
    }

    @Test
    void migraElNbtLegadoALaVersionActual() {
        CompoundTag migrated = MigrationChain.forCurrentSchema().migrate(legacyV0());
        assertEquals(DataVersion.CURRENT, DataVersion.read(migrated));
    }

    @Test
    void esIdempotente() {
        MigrationChain chain = MigrationChain.forCurrentSchema();
        CompoundTag once = chain.migrate(legacyV0());
        CompoundTag twice = chain.migrate(once);
        assertEquals(once, twice, "re-migrar un NBT ya actual no lo cambia");
    }

    @Test
    void noMutaElInput() {
        CompoundTag input = legacyV0();
        MigrationChain.forCurrentSchema().migrate(input);
        assertFalse(input.contains(DataVersion.KEY), "el input original no se toca");
    }

    @Test
    void versionFuturaNoSeMigra() {
        CompoundTag futuro = new CompoundTag();
        futuro.putInt(DataVersion.KEY, 99);
        futuro.putString("marca", "x");
        CompoundTag result = MigrationChain.forCurrentSchema().migrate(futuro);
        assertEquals(99, DataVersion.read(result), "no se toca la version futura");
        assertEquals("x", result.getString("marca"));
    }

    @Test
    void faltaDePasoIntermedioLanza() {
        // currentVersion=2 pero solo esta registrado v0->v1: al llegar a v1 falta v1->v2.
        MigrationChain rota = new MigrationChain(2, List.of(Migration.of(0, t -> t)));
        assertThrows(IllegalStateException.class, () -> rota.migrate(legacyV0()));
    }

    @Test
    void dosPasosDesdeLaMismaVersionSonRechazados() {
        assertThrows(IllegalArgumentException.class, () -> new MigrationChain(
                2, List.of(Migration.of(0, t -> t), Migration.of(0, t -> t))));
    }

    @Test
    void trasMigrarElCodecDecodificaEnLaVersionActual() {
        // Integracion con DJT-017: NBT legado -> migrar -> decodificar.
        CompoundTag migrated = MigrationChain.forCurrentSchema().migrate(legacyV0());
        PlayerNinjaData data = PlayerNinjaData.CODEC.parse(NbtOps.INSTANCE, migrated).getOrThrow();
        assertEquals(PlayerNinjaData.CURRENT_DATA_VERSION, data.dataVersion());
        assertEquals(OnboardingState.Phase.COMPLETED, data.onboarding().phase());
    }
}
