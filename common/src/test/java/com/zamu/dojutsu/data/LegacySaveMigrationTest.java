package com.zamu.dojutsu.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.migration.MigrationChain;
import com.zamu.dojutsu.data.state.ChakraState;
import com.zamu.dojutsu.data.state.ClanMembership;
import com.zamu.dojutsu.data.state.EmpowermentState;
import com.zamu.dojutsu.data.state.EyeState;
import com.zamu.dojutsu.data.state.ItemFlags;
import com.zamu.dojutsu.data.state.JinchurikiState;
import com.zamu.dojutsu.data.state.JutsuKnowledge;
import com.zamu.dojutsu.data.state.NatureProfile;
import com.zamu.dojutsu.data.state.OnboardingState;
import com.zamu.dojutsu.data.state.ProgressionState;
import com.zamu.dojutsu.data.state.SageState;
import com.zamu.dojutsu.data.state.StoryState;
import com.zamu.dojutsu.data.state.SusanooState;
import com.zamu.dojutsu.data.state.ThrowingState;
import com.zamu.dojutsu.data.state.VisionState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

/**
 * <b>Migración desde saves antiguos</b> (F10/DJT-223).
 *
 * <h2>Por qué hace falta este test</h2>
 * La auditoría del 2026-07-30 señaló que a lo largo de F1–F9 se enchufaron <b>16 sub-estados</b> en
 * {@link PlayerNinjaData} sin subir nunca {@link PlayerNinjaData#CURRENT_DATA_VERSION}, y que la maquinaria de
 * migración está construida pero <b>sin estrenar</b> (su única entrada es la identidad v0→v1). Funciona porque
 * todos los codecs son {@code optionalFieldOf}, pero eso era una <b>suposición sin comprobar</b>: nadie había
 * cargado nunca un NBT de la época de F1 contra el agregado de hoy.
 *
 * <p>Aquí se comprueba. Si alguien añade un sub-estado con un campo obligatorio, este test falla y el save de
 * cualquier partida existente deja de cargar — que es exactamente el fallo que no se quiere descubrir en
 * producción.
 */
class LegacySaveMigrationTest {

    private static PlayerNinjaData decode(Tag tag) {
        return PlayerNinjaData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }

    /**
     * Un save tal y como lo escribía <b>F1</b>: sólo {@code onboarding} y {@code chakra} existían como ramas, y
     * {@code dataVersion} ni siquiera se guardaba (se lee como {@link PlayerNinjaData#INITIAL_DATA_VERSION}).
     */
    private static CompoundTag saveDeF1() {
        CompoundTag onboarding = new CompoundTag();
        onboarding.putString("phase", "COMPLETED");

        CompoundTag chakra = new CompoundTag();
        chakra.putInt("chakra", 42);
        chakra.putInt("maxChakra", 100);

        CompoundTag root = new CompoundTag();
        root.put("onboarding", onboarding);
        root.put("chakra", chakra);
        return root;
    }

    @Test
    void unSaveDeF1CargaEnElAgregadoDeHoySinPerderLoQueTenia() {
        PlayerNinjaData decoded = decode(saveDeF1());

        assertEquals(OnboardingState.Phase.COMPLETED, decoded.onboarding().phase());
        assertEquals(42, decoded.chakra().chakra());
        assertEquals(100, decoded.chakra().maxChakra());
    }

    @Test
    void losCatorceSubEstadosQueF1NoConociaCaenASuDefault() {
        // Si alguno dejara de ser opcional, este test falla ANTES que la partida de un jugador real.
        PlayerNinjaData d = decode(saveDeF1());

        assertEquals(ProgressionState.DEFAULT, d.progression());
        assertEquals(NatureProfile.DEFAULT, d.natures());
        assertEquals(ClanMembership.DEFAULT, d.clan());
        assertEquals(JutsuKnowledge.DEFAULT, d.jutsu());
        assertEquals(EyeState.DEFAULT, d.eye());
        assertEquals(SusanooState.DEFAULT, d.susanoo());
        assertEquals(VisionState.DEFAULT, d.vision());
        assertEquals(EmpowermentState.DEFAULT, d.empowerment());
        assertEquals(SageState.DEFAULT, d.sage());
        assertEquals(JinchurikiState.DEFAULT, d.jinchuriki());
        assertEquals(ItemFlags.DEFAULT, d.items());
        assertEquals(ThrowingState.DEFAULT, d.throwing());
        assertEquals(StoryState.DEFAULT, d.story());
        assertNotNull(d.sixPaths()); // anidado bajo sage: se comprueba que existe, no que sea raiz
    }

    @Test
    void unSaveDeF1SeLeeComoVersionInicialYLaCadenaLoSubeALaActual() {
        PlayerNinjaData decoded = decode(saveDeF1());
        assertEquals(PlayerNinjaData.INITIAL_DATA_VERSION, decoded.dataVersion());

        CompoundTag migrado = MigrationChain.forCurrentSchema().migrate(saveDeF1());
        assertEquals(PlayerNinjaData.CURRENT_DATA_VERSION, DataVersion.read(migrado));
    }

    @Test
    void migrarUnSaveDeF1NoDestruyeSusRamas() {
        // el paso v0->v1 es la identidad: lo que habia sigue estando, y ademas ahora lleva version
        CompoundTag migrado = MigrationChain.forCurrentSchema().migrate(saveDeF1());
        PlayerNinjaData decoded = decode(migrado);

        assertEquals(OnboardingState.Phase.COMPLETED, decoded.onboarding().phase());
        assertEquals(42, decoded.chakra().chakra());
        assertEquals(PlayerNinjaData.CURRENT_DATA_VERSION, decoded.dataVersion());
    }

    @Test
    void migrarEsIdempotente() {
        MigrationChain chain = MigrationChain.forCurrentSchema();
        CompoundTag unaVez = chain.migrate(saveDeF1());
        CompoundTag dosVeces = chain.migrate(unaVez);
        assertEquals(unaVez, dosVeces);
    }

    @Test
    void unSaveDelFuturoNoSeToca() {
        // un mundo abierto con una version mas nueva del mod y devuelto a esta: no se degrada, se deja estar
        CompoundTag futuro = saveDeF1();
        futuro.putInt(DataVersion.KEY, PlayerNinjaData.CURRENT_DATA_VERSION + 5);

        CompoundTag resultado = MigrationChain.forCurrentSchema().migrate(futuro);

        assertEquals(PlayerNinjaData.CURRENT_DATA_VERSION + 5, DataVersion.read(resultado));
        assertTrue(resultado.contains("chakra"));
    }
}
