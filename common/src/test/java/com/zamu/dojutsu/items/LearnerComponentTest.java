package com.zamu.dojutsu.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.natures.LearnerService;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

/** Tests del componente del Skill Learner (DJT-055): resolucion de elemento, tolerancia y round-trip. */
class LearnerComponentTest {

    @Test
    void resuelveNaturalezasYKekkeiGenkai() {
        assertEquals(NatureType.FIRE, new LearnerComponent("fire").nature().orElseThrow());
        assertEquals(KekkeiGenkai.WOOD, new LearnerComponent("wood").kekkeiGenkai().orElseThrow());
        assertTrue(new LearnerComponent("fire").kekkeiGenkai().isEmpty()); // los espacios son disjuntos
        assertTrue(new LearnerComponent("wood").nature().isEmpty());
    }

    @Test
    void unElementoDesconocidoNoEsValidoPeroNoCrashea() {
        LearnerComponent unknown = new LearnerComponent("plasma");
        assertFalse(unknown.isValid());
        assertTrue(unknown.nature().isEmpty());
        assertTrue(unknown.kekkeiGenkai().isEmpty());
    }

    @Test
    void elElementoSeNormalizaAMinusculas() {
        assertEquals("yin_yang", new LearnerComponent("YIN_YANG").element());
        assertEquals(NatureType.YIN_YANG, new LearnerComponent("YIN_YANG").nature().orElseThrow());
    }

    @Test
    void roundTripDelCodec() {
        LearnerComponent original = new LearnerComponent("jugo_clan");
        Tag tag = LearnerComponent.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        assertEquals(original, LearnerComponent.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow());
    }

    @Test
    void todosLosElementosConLearnerDelDatapackLoTienen() {
        // give/learner/ tiene 24 archivos: las 8 naturalezas + 16 KG (todos salvo shikotsumyaku y jugo_clan).
        for (NatureType nature : NatureType.values()) {
            assertTrue(LearnerService.hasLearner(new LearnerComponent(nature.name())),
                    "deberia tener learner: " + nature);
        }
        for (KekkeiGenkai kg : KekkeiGenkai.values()) {
            boolean expected = kg != KekkeiGenkai.SHIKOTSUMYAKU && kg != KekkeiGenkai.JUGO_CLAN;
            assertEquals(expected, LearnerService.hasLearner(new LearnerComponent(kg.name())),
                    "hasLearner incorrecto en " + kg);
        }
        assertFalse(LearnerService.hasLearner(new LearnerComponent("plasma"))); // desconocido: sin learner
    }
}
