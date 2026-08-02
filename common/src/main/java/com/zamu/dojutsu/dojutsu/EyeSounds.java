package com.zamu.dojutsu.dojutsu;

import com.zamu.dojutsu.domain.eye.EyeFamily;
import com.zamu.dojutsu.registry.ModSounds;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Sonido de activacion de cada familia de ojo (F10/DJT-224). Traduce los {@code playsound} que el datapack
 * lanza en el {@code awaken} y en el {@code evolve} de cada ojo.
 *
 * <h2>Sharingan y Mangekyo comparten sonido, y no es un descuido</h2>
 * {@code dojutsu/sharingan/awaken} toca <b>{@code dojutsu.sharingan.mangekyo.activate}</b>, el mismo que el
 * Mangekyo. No existe un "sharingan.activate" en el resource pack: son nueve familias y ocho sonidos porque
 * dos de ellas usan el mismo.
 *
 * <h2>El volumen es parte del dato</h2>
 * No es uniforme y no se puede promediar: el Tenseigan despierta a <b>1.0</b> y el Sharingan a <b>0.1</b>, un
 * factor diez. El Byakugan ademas <b>baja</b> al evolucionar (0.3 → 0.1), asi que awaken y evolve se guardan
 * por separado. Todos van con {@code SoundSource.PLAYERS} y tono 1, como el datapack.
 *
 * <h2>Tres familias despiertan en silencio, a proposito</h2>
 * Jougan e Isshiki tienen sonido propio, pero el datapack solo lo toca en el <b>coste de sus habilidades</b>
 * ({@code jougan/bodily_restriction/cost}, {@code isshiki/sukunahikona/cast}), nunca al despertar. Inventarles
 * un sonido de despertar seria contenido nuevo. El Rinne Sharingan tampoco aparece aqui porque no se despierta
 * como los demas: lo concede el Arbol Divino, y su sonido lo toca {@code GodTreeService} donde el datapack lo
 * tocaba ({@code god_tree/awaken_rinne_sharingan}, volumen 0.8).
 */
public final class EyeSounds {

    /** Volumen del {@code playsound} del Arbol Divino al florecer ({@code god_tree/awaken_rinne_sharingan}). */
    public static final float RINNE_SHARINGAN_VOLUME = 0.8F;

    /** Alcance del {@code @a[distance=0..10]} del datapack, en bloques. */
    private static final double RANGE = 10.0D;

    /**
     * Sonido de una familia con sus dos volumenes.
     *
     * @param sound  evento de sonido
     * @param awaken volumen al despertar el ojo
     * @param evolve volumen al evolucionar a el
     */
    private record EyeSound(SoundEvent sound, float awaken, float evolve) {
    }

    private static final Map<EyeFamily, EyeSound> BY_FAMILY = new EnumMap<>(EyeFamily.class);

    private EyeSounds() {
    }

    private static Map<EyeFamily, EyeSound> table() {
        if (BY_FAMILY.isEmpty()) { // perezoso: los RegistryEntry no estan resueltos al cargar la clase
            BY_FAMILY.put(EyeFamily.BYAKUGAN,
                    new EyeSound(ModSounds.BYAKUGAN_ACTIVATE.get(), 0.3F, 0.1F));
            BY_FAMILY.put(EyeFamily.SHARINGAN,
                    new EyeSound(ModSounds.MANGEKYO_ACTIVATE.get(), 0.1F, 0.1F));
            BY_FAMILY.put(EyeFamily.MANGEKYO,
                    new EyeSound(ModSounds.MANGEKYO_ACTIVATE.get(), 0.1F, 0.1F));
            BY_FAMILY.put(EyeFamily.TENSEIGAN,
                    new EyeSound(ModSounds.TENSEIGAN_ACTIVATE.get(), 1.0F, 1.0F));
            BY_FAMILY.put(EyeFamily.KETSURYUGAN,
                    new EyeSound(ModSounds.KETSURYUGAN_ACTIVATE.get(), 0.1F, 0.1F));
            BY_FAMILY.put(EyeFamily.RINNEGAN,
                    new EyeSound(ModSounds.RINNEGAN_ACTIVATE.get(), 0.5F, 0.5F));
        }
        return BY_FAMILY;
    }

    /** Toca el sonido de despertar de esa familia, si el datapack tocaba alguno. */
    public static void playAwaken(ServerPlayer player, EyeFamily family) {
        play(player, family, true);
    }

    /** Toca el sonido de evolucion a esa familia, si el datapack tocaba alguno. */
    public static void playEvolve(ServerPlayer player, EyeFamily family) {
        play(player, family, false);
    }

    private static void play(ServerPlayer player, EyeFamily family, boolean awaken) {
        Optional.ofNullable(table().get(family)).ifPresent(entry ->
                at(player, entry.sound(), awaken ? entry.awaken() : entry.evolve()));
    }

    /** {@code playsound <s> player @a[distance=0..10] ~ ~ ~ <vol> 1} sobre la posicion del jugador. */
    public static void at(ServerPlayer player, SoundEvent sound, float volume) {
        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), sound,
                    SoundSource.PLAYERS, volume, 1.0F);
        }
    }

    /** Alcance del selector original; documenta de donde sale el volumen bajo de casi todos. */
    public static double range() {
        return RANGE;
    }
}
