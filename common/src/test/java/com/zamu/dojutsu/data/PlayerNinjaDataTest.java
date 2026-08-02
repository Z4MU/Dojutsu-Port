package com.zamu.dojutsu.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.state.ChakraState;
import com.zamu.dojutsu.data.state.ClanMembership;
import com.zamu.dojutsu.data.state.NatureProfile;
import com.zamu.dojutsu.data.state.OnboardingState;
import com.zamu.dojutsu.data.state.ProgressionState;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.domain.progression.LeveledStat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

/** Tests del agregado raiz y su round-trip NBT (DJT-017). */
class PlayerNinjaDataTest {

    private static PlayerNinjaData decode(Tag tag) {
        return PlayerNinjaData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }

    private static Tag encode(PlayerNinjaData data) {
        return PlayerNinjaData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
    }

    @Test
    void jugadorNuevoTieneDefaults() {
        PlayerNinjaData d = new PlayerNinjaData();
        assertEquals(PlayerNinjaData.CURRENT_DATA_VERSION, d.dataVersion());
        assertEquals(OnboardingState.DEFAULT, d.onboarding());
        assertEquals(OnboardingState.Phase.NOT_STARTED, d.onboarding().phase());
        assertFalse(d.onboarding().completed());
        assertEquals(ChakraState.DEFAULT, d.chakra());
    }

    @Test
    void chakraPersisteEnElAgregado() {
        PlayerNinjaData original = new PlayerNinjaData();
        original.setChakra(original.chakra().withChakra(73));

        PlayerNinjaData decoded = decode(encode(original));

        assertEquals(73, decoded.chakra().chakra());
        assertEquals(100, decoded.chakra().maxChakra());
    }

    @Test
    void saveSinChakraTomaElDefault() {
        // Save legado (F1) sin la rama 'chakra': la tolerancia de optionalFieldOf da el default de S02.
        CompoundTag tag = new CompoundTag();
        tag.putInt("dataVersion", PlayerNinjaData.CURRENT_DATA_VERSION);
        assertEquals(ChakraState.DEFAULT, decode(tag).chakra());
    }

    @Test
    void progresionPersisteEnElAgregado() {
        PlayerNinjaData original = new PlayerNinjaData();
        original.setProgression(original.progression()
                .withSkillPoints(4)
                .withSkill(Skill.FUINJUTSU, new LeveledStat(3, 5)));

        PlayerNinjaData decoded = decode(encode(original));

        assertEquals(4, decoded.progression().skillPoints());
        assertEquals(new LeveledStat(3, 5), decoded.progression().skill(Skill.FUINJUTSU));
    }

    @Test
    void saveSinProgresionTomaElDefault() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("dataVersion", PlayerNinjaData.CURRENT_DATA_VERSION);
        assertEquals(ProgressionState.DEFAULT, decode(tag).progression());
    }

    @Test
    void perfilElementalPersisteEnElAgregado() {
        PlayerNinjaData original = new PlayerNinjaData();
        original.setNatures(original.natures()
                .withNature(NatureType.FIRE)
                .withAffinity(NatureType.FIRE, new LeveledStat(50, 0))
                .withKekkeiGenkai(KekkeiGenkai.WOOD));

        PlayerNinjaData decoded = decode(encode(original));

        assertTrue(decoded.natures().has(NatureType.FIRE));
        assertTrue(decoded.natures().has(KekkeiGenkai.WOOD));
        assertEquals(50, decoded.natures().affinity(NatureType.FIRE).level());
    }

    @Test
    void saveSinPerfilElementalTomaElDefault() {
        // Save de F1/F2 sin la rama 'natures': la tolerancia de optionalFieldOf da el default de S03.
        CompoundTag tag = new CompoundTag();
        tag.putInt("dataVersion", PlayerNinjaData.CURRENT_DATA_VERSION);
        assertEquals(NatureProfile.DEFAULT, decode(tag).natures());
    }

    @Test
    void laPertenenciaAClanPersisteEnElAgregado() {
        PlayerNinjaData original = new PlayerNinjaData();
        original.setClan(original.clan().withClan("uchiha"));

        PlayerNinjaData decoded = decode(encode(original));

        assertTrue(decoded.clan().isIn("uchiha"));
        assertTrue(decoded.clan().initialized());
    }

    @Test
    void saveSinClanTomaElDefault() {
        // Save de F1/F2 sin la rama 'clan': la tolerancia de optionalFieldOf da "sin clan".
        CompoundTag tag = new CompoundTag();
        tag.putInt("dataVersion", PlayerNinjaData.CURRENT_DATA_VERSION);
        assertEquals(ClanMembership.DEFAULT, decode(tag).clan());
    }

    @Test
    void roundTripNbtNoPierdeEstado() {
        PlayerNinjaData original = new PlayerNinjaData();
        original.setOnboarding(new OnboardingState(OnboardingState.Phase.COMPLETED));

        PlayerNinjaData decoded = decode(encode(original));

        assertEquals(original.dataVersion(), decoded.dataVersion());
        assertEquals(original.onboarding(), decoded.onboarding());
        assertTrue(decoded.onboarding().completed());
    }

    @Test
    void nbtVacioSeLeeComoLegadoConDefaults() {
        // Sin dataVersion -> INITIAL (save previo al versionado, a migrar en DJT-018); onboarding -> default.
        PlayerNinjaData decoded = decode(new CompoundTag());
        assertEquals(PlayerNinjaData.INITIAL_DATA_VERSION, decoded.dataVersion());
        assertEquals(OnboardingState.DEFAULT, decoded.onboarding());
    }

    @Test
    void campoFuturoDesconocidoNoRompe() {
        CompoundTag tag = new CompoundTag();
        tag.putString("campoDelFuturo", "algo");
        tag.putInt("dataVersion", PlayerNinjaData.CURRENT_DATA_VERSION);
        PlayerNinjaData decoded = decode(tag);
        assertEquals(PlayerNinjaData.CURRENT_DATA_VERSION, decoded.dataVersion());
        assertEquals(OnboardingState.DEFAULT, decoded.onboarding());
    }

    @Test
    void faseCorruptaCaeADefault() {
        CompoundTag onboarding = new CompoundTag();
        onboarding.putString("phase", "NO_EXISTE");
        CompoundTag root = new CompoundTag();
        root.put("onboarding", onboarding);
        PlayerNinjaData decoded = decode(root);
        assertEquals(OnboardingState.Phase.NOT_STARTED, decoded.onboarding().phase());
    }

    @Test
    void laPuertaDeEscrituraActualizaOnboarding() {
        PlayerNinjaData d = new PlayerNinjaData();
        d.setOnboarding(d.onboarding().withPhase(OnboardingState.Phase.SELECTING_CLAN));
        assertEquals(OnboardingState.Phase.SELECTING_CLAN, d.onboarding().phase());
        assertThrows(NullPointerException.class, () -> d.setOnboarding(null));
    }
}
