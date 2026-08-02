package com.zamu.dojutsu.data.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Sub-estado de la vision ocular {@code [S25]} (DJT-125): las preferencias de vision del ninja. Por ahora una:
 * si la chakra vision esta <b>silenciada</b>. Sub-estado enchufable de
 * {@link com.zamu.dojutsu.data.PlayerNinjaData}.
 *
 * <p><b>Se guarda el "off", no el "on"</b> (fiel al {@code d.no_cv} del datapack, que persiste): la chakra
 * vision esta activa <b>por defecto</b> cuando se lleva un ojo que la da — el jugador la apaga si le molesta, y
 * esa preferencia sobrevive a la sesion. De ahi el nombre {@code chakraVisionOff}: el default es {@code false}
 * (no silenciada = activa).
 *
 * <p>Value object <b>inmutable</b> (regla 10b): para cambiar algo se reemplaza con un wither.
 *
 * <p><b>Divergencia consciente (F10):</b> el {@code waterWalkOff} <b>no existe en el datapack</b>, donde caminar
 * sobre el agua es automatico y la unica forma de bajarse es agacharse. En partida eso resulto incomodo — cruzar
 * un rio nadando exige mantener el sneak todo el rato — asi que se añade un interruptor persistente, con el
 * mismo patron que el {@code d.no_cv} que si es fiel. Agacharse sigue funcionando igual: el interruptor no lo
 * sustituye, lo complementa.
 *
 * @param chakraVisionOff el jugador silencio la chakra vision ({@code d.no_cv})
 * @param waterWalkOff    el jugador desactivo la caminata sobre el agua (sin equivalente en el datapack)
 */
public record VisionState(boolean chakraVisionOff, boolean waterWalkOff) {

    /** Preferencias por defecto: nada silenciado (vision y caminata activas si se cumplen sus requisitos). */
    public static final VisionState DEFAULT = new VisionState(false, false);

    /** Codec del sub-estado; los campos son opcionales y caen al valor de {@link #DEFAULT}. */
    public static final Codec<VisionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("chakraVisionOff", DEFAULT.chakraVisionOff())
                    .forGetter(VisionState::chakraVisionOff),
            Codec.BOOL.optionalFieldOf("waterWalkOff", DEFAULT.waterWalkOff())
                    .forGetter(VisionState::waterWalkOff)
    ).apply(instance, VisionState::new));

    /** Copia silenciando o reactivando la chakra vision. */
    public VisionState withChakraVisionOff(boolean off) {
        return chakraVisionOff == off ? this : new VisionState(off, waterWalkOff);
    }

    /** Copia alternando el silencio de la chakra vision. */
    public VisionState toggledChakraVision() {
        return new VisionState(!chakraVisionOff, waterWalkOff);
    }

    /** Copia desactivando o reactivando la caminata sobre el agua. */
    public VisionState withWaterWalkOff(boolean off) {
        return waterWalkOff == off ? this : new VisionState(chakraVisionOff, off);
    }

    /** Copia alternando la caminata sobre el agua. */
    public VisionState toggledWaterWalk() {
        return new VisionState(chakraVisionOff, !waterWalkOff);
    }
}
