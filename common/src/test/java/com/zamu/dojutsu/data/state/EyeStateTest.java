package com.zamu.dojutsu.data.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.domain.eye.EyeFamily;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

/** Tests del sub-estado de ojos (DJT-113): defaults, withers, clamp de strain, tolerancia y round-trip NBT. */
class EyeStateTest {

    private static EyeState decode(Tag tag) {
        return EyeState.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }

    private static Tag encode(EyeState state) {
        return EyeState.CODEC.encodeStart(NbtOps.INSTANCE, state).getOrThrow();
    }

    private static EyeState roundTrip(EyeState state) {
        return decode(encode(state));
    }

    @Test
    void defaultEstaVacio() {
        EyeState d = EyeState.DEFAULT;
        assertTrue(d.activeEye().isEmpty());
        assertFalse(d.eternal());
        assertTrue(d.useLevels().isEmpty());
        assertTrue(d.awakened().isEmpty());
        assertEquals(0, d.eyeStrain());
        assertTrue(d.selectedAbility().isEmpty());
    }

    @Test
    void withersRoundTripPorNbt() {
        EyeState state = EyeState.DEFAULT
                .withActiveEye("mangekyo_itachi")
                .withEternal(true)
                .withUseLevel(EyeFamily.SHARINGAN, 360000)
                .withAwakened(EyeFamily.SHARINGAN)
                .withTimer("shar_waketime", 42)
                .withEyeStrain(75)
                .withSelectedAbility("amaterasu");

        EyeState back = roundTrip(state);

        assertEquals(state, back);
        assertEquals("mangekyo_itachi", back.activeEye().orElseThrow());
        assertTrue(back.eternal());
        assertEquals(360000, back.useLevel(EyeFamily.SHARINGAN));
        assertTrue(back.isAwakened(EyeFamily.SHARINGAN));
        assertEquals(42, back.timer("shar_waketime"));
        assertEquals(75, back.eyeStrain());
        assertEquals("amaterasu", back.selectedAbility().orElseThrow());
    }

    @Test
    void strainSeClampaAlRango() {
        assertEquals(100, EyeState.DEFAULT.withEyeStrain(150).eyeStrain());
        assertEquals(0, EyeState.DEFAULT.withEyeStrain(-5).eyeStrain());
        // el clamp tambien se aplica al reconstruir desde NBT (dato corrupto/manipulado)
        CompoundTag tag = new CompoundTag();
        tag.putInt("eyeStrain", 999);
        assertEquals(100, decode(tag).eyeStrain());
    }

    @Test
    void desactivarLimpiaEternalYSeleccion() {
        EyeState activo = EyeState.DEFAULT
                .withActiveEye("mangekyo_itachi").withEternal(true).withSelectedAbility("amaterasu")
                .withUseLevel(EyeFamily.SHARINGAN, 360000);

        EyeState off = activo.withoutActiveEye();

        assertTrue(off.activeEye().isEmpty());
        assertFalse(off.eternal());
        assertTrue(off.selectedAbility().isEmpty());
        // el uso acumulado NO se pierde al desactivar (el datapack conserva d.<eye>_use)
        assertEquals(360000, off.useLevel(EyeFamily.SHARINGAN));
    }

    @Test
    void withersSonIdempotentes() {
        EyeState state = EyeState.DEFAULT.withActiveEye("sharingan").withAwakened(EyeFamily.SHARINGAN);
        assertSame(state, state.withActiveEye("sharingan"));
        assertSame(state, state.withAwakened(EyeFamily.SHARINGAN));
        assertSame(state, state.withEternal(false));
        assertSame(EyeState.DEFAULT, EyeState.DEFAULT.withoutActiveEye());
        assertSame(EyeState.DEFAULT, EyeState.DEFAULT.withoutSelectedAbility());
    }

    @Test
    void toleraFamiliasDesconocidasEnElNbt() {
        // Un NBT de una version futura con una familia que este build no conoce no debe romper la carga:
        // se descarta la clave desconocida y se conserva el resto (codec tolerante de data.state).
        CompoundTag tag = new CompoundTag();
        CompoundTag useLevels = new CompoundTag();
        useLevels.putInt("sharingan", 100000);
        useLevels.putInt("ojo_del_futuro", 50000);
        tag.put("useLevels", useLevels);
        ListTag awakened = new ListTag();
        awakened.add(StringTag.valueOf("sharingan"));
        awakened.add(StringTag.valueOf("ojo_del_futuro"));
        tag.put("awakened", awakened);

        EyeState state = decode(tag);

        assertEquals(100000, state.useLevel(EyeFamily.SHARINGAN));
        assertEquals(1, state.useLevels().size());
        assertTrue(state.isAwakened(EyeFamily.SHARINGAN));
        assertEquals(1, state.awakened().size());
    }

    // ------------------------------------------------------------------
    // Origen del Amenominaka (F10/DJT-226)
    // ------------------------------------------------------------------

    @Test
    void unNinjaNuevoNoTieneOrigenDeAmenominaka() {
        assertTrue(EyeState.DEFAULT.amenominakaOrigin().isEmpty());
    }

    @Test
    void elOrigenDelAmenominakaSobreviveAlRoundTrip() {
        GlobalPos origin = GlobalPos.of(Level.OVERWORLD, new BlockPos(120, 64, -340));
        EyeState state = roundTrip(EyeState.DEFAULT.withAmenominakaOrigin(origin));

        assertEquals(Optional.of(origin), state.amenominakaOrigin());
    }

    @Test
    void olvidarElOrigenEsIdempotenteYNoTocaLoDemas() {
        assertSame(EyeState.DEFAULT, EyeState.DEFAULT.withoutAmenominakaOrigin());

        EyeState conOjo = EyeState.DEFAULT.withActiveEye("rinne_sharingan")
                .withAmenominakaOrigin(GlobalPos.of(Level.NETHER, BlockPos.ZERO));
        EyeState sinOrigen = conOjo.withoutAmenominakaOrigin();

        assertTrue(sinOrigen.amenominakaOrigin().isEmpty());
        assertEquals(conOjo.activeEye(), sinOrigen.activeEye()); // el resto del estado no se toca
    }

    @Test
    void losWithersDeSiempreConservanElOrigen() {
        // el campo nuevo se enhebra por todos los withers: si alguno lo perdiera, volver a casa se rompe en
        // silencio y solo se notaria al intentar regresar
        GlobalPos origin = GlobalPos.of(Level.END, new BlockPos(1, 2, 3));
        EyeState base = EyeState.DEFAULT.withAmenominakaOrigin(origin);

        assertEquals(Optional.of(origin), base.withEyeStrain(50).amenominakaOrigin());
        assertEquals(Optional.of(origin), base.withEternal(true).amenominakaOrigin());
        assertEquals(Optional.of(origin), base.withActiveEye("rinnegan").amenominakaOrigin());
        assertEquals(Optional.of(origin), base.withoutActiveEye().amenominakaOrigin());
        assertEquals(Optional.of(origin), base.withSelectedAbility("shinra_tensei").amenominakaOrigin());
        assertEquals(Optional.of(origin), base.withTimer("t", 5).amenominakaOrigin());
        assertEquals(Optional.of(origin), base.withUseLevel(EyeFamily.RINNEGAN, 10).amenominakaOrigin());
        assertEquals(Optional.of(origin), base.withAwakened(EyeFamily.RINNEGAN).amenominakaOrigin());
    }
}
