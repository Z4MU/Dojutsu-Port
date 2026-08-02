package com.zamu.dojutsu.data.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.domain.progression.LeveledStat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

/** Tests del perfil elemental (DJT-050): defaults, afinidad derivada, idempotencia y round-trip NBT. */
class NatureProfileTest {

    private static NatureProfile decode(Tag tag) {
        return NatureProfile.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }

    private static Tag encode(NatureProfile profile) {
        return NatureProfile.CODEC.encodeStart(NbtOps.INSTANCE, profile).getOrThrow();
    }

    @Test
    void defaultEstaVacioConAfinidadesANivel0() {
        NatureProfile d = NatureProfile.DEFAULT;
        assertTrue(d.natures().isEmpty());
        assertTrue(d.kekkeiGenkai().isEmpty());
        assertFalse(d.hashiramaDna());
        assertFalse(d.allBasicsBoost());
        assertFalse(d.allNaturesBoost());
        assertEquals(NatureType.withAffinity().size(), d.affinities().size());
        for (NatureType nature : NatureType.withAffinity()) {
            assertEquals(new LeveledStat(0, 0), d.affinity(nature));
        }
    }

    @Test
    void laAfinidadUsaLaMismaEscalaQueLasHabilidades() {
        // [S13-R1]: afinidad y skill comparten eje (LeveledStat + XpCurve), no son dos escalas distintas.
        // Misma XP sobre el mismo nivel -> mismo resultado en una afinidad y en una habilidad.
        // Banda 0-10: 10 XP por nivel -> sube a 6 sin sobrante.
        LeveledStat afinidad = new LeveledStat(5, 5).withXpAdded(5);
        LeveledStat habilidad = new LeveledStat(5, 5).withXpAdded(5);

        NatureProfile perfil = NatureProfile.DEFAULT.withAffinity(NatureType.FIRE, afinidad);
        ProgressionState progresion = ProgressionState.DEFAULT.withSkill(Skill.TAIJUTSU, habilidad);

        assertEquals(6, perfil.affinity(NatureType.FIRE).level());
        assertEquals(progresion.skill(Skill.TAIJUTSU), perfil.affinity(NatureType.FIRE));
    }

    @Test
    void yinYangDerivaSuAfinidadDeYinYYang() {
        // menu/leveling/affinity/update_yinyang: aff_yinyang = (aff_yin + aff_yang) / 2 (division entera).
        NatureProfile p = NatureProfile.DEFAULT
                .withAffinity(NatureType.YIN, new LeveledStat(50, 0))
                .withAffinity(NatureType.YANG, new LeveledStat(31, 0));
        assertEquals(40, p.affinity(NatureType.YIN_YANG).level());
        assertEquals(0, p.affinity(NatureType.YIN_YANG).xp());
    }

    @Test
    void laAfinidadDerivadaNoSePuedeEscribir() {
        assertThrows(IllegalArgumentException.class,
                () -> NatureProfile.DEFAULT.withAffinity(NatureType.YIN_YANG, new LeveledStat(10, 0)));
    }

    @Test
    void anadirNaturalezaEsIdempotente() {
        NatureProfile conFuego = NatureProfile.DEFAULT.withNature(NatureType.FIRE);
        assertTrue(conFuego.has(NatureType.FIRE));
        assertSame(conFuego, conFuego.withNature(NatureType.FIRE));
    }

    @Test
    void quitarNaturalezaNoBorraSuAfinidad() {
        // El take/* del datapack no resetea d.aff_<nat>; conservarla evita perder progreso al revocar.
        NatureProfile p = NatureProfile.DEFAULT
                .withNature(NatureType.WATER)
                .withAffinity(NatureType.WATER, new LeveledStat(50, 0))
                .withoutNature(NatureType.WATER);
        assertFalse(p.has(NatureType.WATER));
        assertEquals(50, p.affinity(NatureType.WATER).level());
        assertSame(p, p.withoutNature(NatureType.WATER));
    }

    @Test
    void kekkeiGenkaiIdempotente() {
        NatureProfile p = NatureProfile.DEFAULT.withKekkeiGenkai(KekkeiGenkai.WOOD);
        assertTrue(p.has(KekkeiGenkai.WOOD));
        assertFalse(p.has(KekkeiGenkai.ICE));
        assertSame(p, p.withKekkeiGenkai(KekkeiGenkai.WOOD));
        assertFalse(p.withoutKekkeiGenkai(KekkeiGenkai.WOOD).has(KekkeiGenkai.WOOD));
    }

    @Test
    void hasAllBasicsExigeLasCinco() {
        NatureProfile p = NatureProfile.DEFAULT;
        for (NatureType nature : NatureType.basics()) {
            assertFalse(p.hasAllBasics());
            p = p.withNature(nature);
        }
        assertTrue(p.hasAllBasics());
        assertFalse(p.hasAllNatures()); // faltan yin y yang
    }

    @Test
    void hasAllNaturesNoExigeYinYang() {
        NatureProfile p = NatureProfile.DEFAULT;
        for (NatureType nature : NatureType.withAffinity()) {
            p = p.withNature(nature);
        }
        assertTrue(p.hasAllNatures());
        assertFalse(p.has(NatureType.YIN_YANG));
    }

    @Test
    void roundTripNbtNoPierdeEstado() {
        NatureProfile original = NatureProfile.DEFAULT
                .withNature(NatureType.FIRE)
                .withNature(NatureType.YIN)
                .withAffinity(NatureType.FIRE, new LeveledStat(50, 3))
                .withKekkeiGenkai(KekkeiGenkai.WOOD)
                .withKekkeiGenkai(KekkeiGenkai.JUGO_CLAN)
                .withHashiramaDna(true)
                .withAllBasicsBoost(true);

        NatureProfile decoded = decode(encode(original));

        assertEquals(original, decoded);
        assertTrue(decoded.has(NatureType.FIRE));
        assertTrue(decoded.has(KekkeiGenkai.JUGO_CLAN));
        assertTrue(decoded.hashiramaDna());
        assertTrue(decoded.allBasicsBoost());
        assertFalse(decoded.allNaturesBoost());
        assertEquals(new LeveledStat(50, 3), decoded.affinity(NatureType.FIRE));
    }

    @Test
    void nbtVacioDaElDefault() {
        assertEquals(NatureProfile.DEFAULT, decode(new CompoundTag()));
    }

    @Test
    void nombresDesconocidosSeDescartanSinRomperLaCarga() {
        CompoundTag tag = new CompoundTag();
        ListTag natures = new ListTag();
        natures.add(StringTag.valueOf("FIRE"));
        natures.add(StringTag.valueOf("PLASMA")); // no existe: NBT corrupto o de una version futura
        tag.put("natures", natures);

        NatureProfile decoded = decode(tag);

        assertTrue(decoded.has(NatureType.FIRE));
        assertEquals(1, decoded.natures().size());
    }

    @Test
    void nivelCorruptoSeClampaAlCap() {
        CompoundTag fire = new CompoundTag();
        fire.putInt("level", 99_999);
        fire.putInt("xp", -5);
        CompoundTag tag = new CompoundTag();
        tag.put("fire", fire);

        NatureProfile decoded = decode(tag);

        assertEquals(500, decoded.affinity(NatureType.FIRE).level());
        assertEquals(0, decoded.affinity(NatureType.FIRE).xp());
    }

    @Test
    void elDefaultNoSeCorrompeAlSubirUnaAfinidad() {
        // DJT-063: LeveledStat es inmutable, asi que operar sobre el DEFAULT estatico no puede envenenarlo.
        NatureProfile.DEFAULT.affinity(NatureType.FIRE).withXpAdded(500);
        assertEquals(new LeveledStat(0, 0), NatureProfile.DEFAULT.affinity(NatureType.FIRE));
    }

    @Test
    void losSetsExpuestosSonInmutables() {
        NatureProfile p = NatureProfile.DEFAULT.withNature(NatureType.FIRE);
        assertThrows(UnsupportedOperationException.class, () -> p.natures().add(NatureType.WATER));
        assertThrows(UnsupportedOperationException.class, () -> p.kekkeiGenkai().add(KekkeiGenkai.ICE));
        assertThrows(UnsupportedOperationException.class,
                () -> p.affinities().put(NatureType.FIRE, new LeveledStat()));
    }
}
