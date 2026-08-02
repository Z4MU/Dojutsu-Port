package com.zamu.dojutsu.data.state;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

/** Tests del sub-estado de chakra: defaults de {@code [S02]}, invariante e integridad NBT (DJT-030). */
class ChakraStateTest {

    private static ChakraState decode(Tag tag) {
        return ChakraState.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }

    private static Tag encode(ChakraState state) {
        return ChakraState.CODEC.encodeStart(NbtOps.INSTANCE, state).getOrThrow();
    }

    @Test
    void defaultSigueS02() {
        ChakraState d = ChakraState.DEFAULT;
        assertEquals(0, d.chakra());
        assertEquals(100, d.maxChakra());
        assertEquals(0, d.naturalChakra());
        assertEquals(100, d.maxNaturalChakra());
        assertEquals(0, d.chakraTimer());
        assertEquals(0, d.jugoTimer());
    }

    @Test
    void roundTripNbtNoPierdeEstado() {
        ChakraState original = new ChakraState(42, 120, 7, 100, 30, 1);
        ChakraState decoded = decode(encode(original));
        assertEquals(original, decoded);
    }

    @Test
    void nbtVacioCaeADefault() {
        assertEquals(ChakraState.DEFAULT, decode(new CompoundTag()));
    }

    @Test
    void campoAusenteTomaSuDefault() {
        // Solo trae 'chakra'; el resto (incluido maxChakra) toma el default de S02.
        CompoundTag tag = new CompoundTag();
        tag.putInt("chakra", 50);
        ChakraState decoded = decode(tag);
        assertEquals(50, decoded.chakra());
        assertEquals(100, decoded.maxChakra());
    }

    @Test
    void invarianteSaneaValoresCorruptos() {
        // max negativo -> 0; actual por encima del max -> clampa; timer negativo -> 0.
        ChakraState s = new ChakraState(999, -5, -3, 100, -1, 0);
        assertEquals(0, s.maxChakra());
        assertEquals(0, s.chakra());       // clampado a [0, max=0]
        assertEquals(0, s.naturalChakra()); // negativo -> 0
        assertEquals(0, s.chakraTimer());
    }

    @Test
    void withChakraClampaAlMaximo() {
        ChakraState s = ChakraState.DEFAULT.withChakra(500);
        assertEquals(100, s.chakra()); // clampado a maxChakra
        assertEquals(0, ChakraState.DEFAULT.withChakra(-10).chakra());
    }
}
