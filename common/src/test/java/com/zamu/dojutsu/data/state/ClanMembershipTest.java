package com.zamu.dojutsu.data.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

/** Tests del sub-estado de pertenencia a clan (DJT-056): defaults, withers y round-trip NBT. */
class ClanMembershipTest {

    private static ClanMembership decode(Tag tag) {
        return ClanMembership.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }

    private static Tag encode(ClanMembership membership) {
        return ClanMembership.CODEC.encodeStart(NbtOps.INSTANCE, membership).getOrThrow();
    }

    @Test
    void defaultSinClanNiInicializar() {
        assertFalse(ClanMembership.DEFAULT.hasClan());
        assertFalse(ClanMembership.DEFAULT.initialized());
        assertFalse(ClanMembership.DEFAULT.isIn("uchiha"));
    }

    @Test
    void unirseMarcaClanEInicializado() {
        ClanMembership uchiha = ClanMembership.DEFAULT.withClan("uchiha");
        assertTrue(uchiha.hasClan());
        assertTrue(uchiha.isIn("uchiha"));
        assertFalse(uchiha.isIn("hyuga"));
        assertTrue(uchiha.initialized()); // la guarda de "bonus una sola vez" (DJT-057)
    }

    @Test
    void roundTripNbtNoPierdeEstado() {
        ClanMembership original = ClanMembership.DEFAULT.withClan("namikaze");
        assertEquals(original, decode(encode(original)));
        assertEquals(ClanMembership.DEFAULT, decode(encode(ClanMembership.DEFAULT)));
    }

    @Test
    void nbtVacioDaElDefault() {
        assertEquals(ClanMembership.DEFAULT, decode(new CompoundTag()));
    }
}
