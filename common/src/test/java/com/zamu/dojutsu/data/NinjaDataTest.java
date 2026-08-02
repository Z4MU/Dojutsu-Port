package com.zamu.dojutsu.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zamu.dojutsu.data.state.OnboardingState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests del codec de persistencia (migratorio) usado por el attachment (DJT-019). El {@code get}/registro
 * del attachment se valida en runtime en ambos loaders; aqui se cubre la logica migrar->decodificar.
 */
class NinjaDataTest {

    @Test
    void elCodecMigraElNbtLegadoAlDecodificar() {
        CompoundTag onboarding = new CompoundTag();
        onboarding.putString("phase", OnboardingState.Phase.COMPLETED.name());
        CompoundTag legacy = new CompoundTag(); // sin dataVersion -> v0
        legacy.put("onboarding", onboarding);

        PlayerNinjaData data = NinjaData.PERSISTENT_CODEC.parse(NbtOps.INSTANCE, legacy).getOrThrow();

        assertEquals(PlayerNinjaData.CURRENT_DATA_VERSION, data.dataVersion(), "migrado a la version actual");
        assertEquals(OnboardingState.Phase.COMPLETED, data.onboarding().phase(), "estado conservado");
    }

    @Test
    void elCodecHaceRoundTripSinPerdida() {
        PlayerNinjaData original = new PlayerNinjaData();
        original.setOnboarding(new OnboardingState(OnboardingState.Phase.SELECTING_AFFINITY));

        Tag encoded = NinjaData.PERSISTENT_CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        PlayerNinjaData decoded = NinjaData.PERSISTENT_CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();

        assertEquals(original.dataVersion(), decoded.dataVersion());
        assertEquals(original.onboarding(), decoded.onboarding());
    }

    @Test
    void elCodecDecodificaNbtVacioComoDefaultsEnVersionActual() {
        PlayerNinjaData data = NinjaData.PERSISTENT_CODEC.parse(NbtOps.INSTANCE, new CompoundTag()).getOrThrow();
        assertEquals(PlayerNinjaData.CURRENT_DATA_VERSION, data.dataVersion());
        assertEquals(OnboardingState.DEFAULT, data.onboarding());
    }
}
