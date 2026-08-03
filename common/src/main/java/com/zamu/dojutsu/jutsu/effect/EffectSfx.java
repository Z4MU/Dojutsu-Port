package com.zamu.dojutsu.jutsu.effect;

import com.google.gson.JsonObject;
import com.zamu.dojutsu.core.NinjaSfx;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * El {@code playsound} de un jutsu, declarado en su JSON (F10/DJT-227).
 *
 * <h2>Por que es un parametro y no un {@code switch} por id</h2>
 * El datapack pone el sonido en el {@code .mcfunction} de <b>cada</b> tecnica y los valores <b>no son
 * derivables de la familia</b>: el Chidori y el Corta-Relampagos comparten sonido pero no tono (1 y
 * <b>0.9</b>); la madera suena a <b>volumen 5 en 15 bloques</b> y el golpe cargado a <b>0.6 en 10</b>. Ni el
 * volumen, ni el tono, ni el alcance se pueden sacar del tipo de efecto.
 *
 * <p>Cablearlo por id habria dejado una lista de nombres dentro del codigo y habria obligado a tocar la clase
 * cada vez que se portara una tecnica mas. Declarado aqui, la tecnica trae su sonido igual que trae su daño.
 *
 * <h2>Solo suena lo que el datapack hacia sonar</h2>
 * Los jutsus <b>sin</b> {@code sound} siguen mudos a proposito: de las nueve estructuras de madera solo tres
 * tocaban {@code jutsu.wood}, y de las once tecnicas de golpe cargado solo cinco tenian {@code playsound}.
 * Ausencia de parametro = ausencia de sonido en el original, no un olvido.
 *
 * @param sound  id del evento; se acepta cualquiera registrado, tambien de vanilla
 * @param source canal ({@code master} o {@code player} en el original)
 * @param volume volumen literal del datapack
 * @param pitch  tono literal del datapack
 * @param range  alcance del selector {@code @a[distance=0..N]}, en bloques
 */
record EffectSfx(Optional<ResourceLocation> sound, SoundSource source, float volume, float pitch, double range) {

    /**
     * Lee {@code sound}, {@code soundSource}, {@code soundVolume}, {@code soundPitch} y {@code soundRange}.
     * Sin {@code sound} el jutsu es mudo y el resto se ignora.
     */
    static EffectSfx parse(JsonObject json) {
        Optional<ResourceLocation> id = Optional
                .ofNullable(net.minecraft.util.GsonHelper.getAsString(json, "sound", null))
                .map(ResourceLocation::tryParse);
        SoundSource source = "player".equalsIgnoreCase(
                net.minecraft.util.GsonHelper.getAsString(json, "soundSource", "master"))
                        ? SoundSource.PLAYERS
                        : SoundSource.MASTER;
        return new EffectSfx(id, source,
                net.minecraft.util.GsonHelper.getAsFloat(json, "soundVolume", 1.0F),
                net.minecraft.util.GsonHelper.getAsFloat(json, "soundPitch", 1.0F),
                net.minecraft.util.GsonHelper.getAsDouble(json, "soundRange", NinjaSfx.RANGE_10));
    }

    /**
     * Lo toca donde se indique. Un id que no exista se ignora <b>en silencio</b> en vez de tumbar el cast: es
     * el mismo criterio tolerante con el que el mod carga sus registros.
     */
    void play(ServerLevel level, double x, double y, double z) {
        resolve().ifPresent(event -> NinjaSfx.at(level, x, y, z, event, source, volume, pitch, range));
    }

    /** El evento registrado, si el id apunta a alguno. */
    Optional<SoundEvent> resolve() {
        return sound.flatMap(BuiltInRegistries.SOUND_EVENT::getOptional);
    }
}
