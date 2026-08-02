package com.zamu.dojutsu.data.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

/**
 * Sub-estado de <b>jinchuriki</b> {@code [S09]} (F7/DJT-165): la bestia que llevas dentro. Sub-estado enchufable
 * de {@link com.zamu.dojutsu.data.PlayerNinjaData}, siguiendo la receta que documenta el propio agregado.
 *
 * <h2>Se guarda lo prestado, no solo lo que tienes</h2>
 * {@code grantedMaxChakra} es la respuesta concreta al riesgo <b>{@code [RA-2]}</b>. Sellar una bestia sube el
 * chakra maximo <b>1000 por cola</b> (hasta 10 000), lo que revienta la escala normal del juego; el peligro no es
 * el numero, es no poder <b>deshacerlo</b>. El datapack resta la misma constante literal en {@code removal/*}, lo
 * que funciona solo mientras nadie toque esas cifras. Aqui se guarda <b>cuanto se prestó de verdad</b>, asi que
 * extraer la bestia devuelve exactamente eso aunque la tabla cambie entre versiones.
 *
 * <p>Las <b>naturalezas y el Kekkei Genkai</b> prestados no se repiten aqui: ya viven en {@link TemporaryGrants}
 * marcados con la bestia como {@code source} (F3/DJT-053), que es su sitio.
 *
 * <p>Campos verificados contra los scoreboards del datapack ({@code d.jinchuriki}, {@code d.jinchuriki_lvl},
 * {@code d.max_chakra} de {@code sealing/*}). Value object <b>inmutable</b> (regla 10b): se reemplaza con withers.
 *
 * @param sealedBeast       id de la bestia sellada ({@code "nine_tails"}); vacio = no eres jinchuriki
 * @param level             nivel de jinchuriki ({@code d.jinchuriki_lvl}); 0 si no llevas ninguna
 * @param grantedMaxChakra  chakra maximo que presto la bestia, para devolverlo exacto al extraerla
 * @param bondTicks         ticks de vinculo acumulados desde la ultima subida ({@code d.jin_bondtime}); el
 *                          vinculo se gana <b>portando</b> la bestia, no haciendo nada concreto
 * @param baryonUse         ticks de Modo Baryon que le quedan ({@code d.baryon_use}); al agotarse, la bestia
 *                          <b>se va</b> — no es un coste, es una despedida
 */
public record JinchurikiState(Optional<String> sealedBeast, int level, int grantedMaxChakra, int bondTicks,
        int baryonUse) {

    /** Ninja sin bestia sellada. */
    public static final JinchurikiState DEFAULT = new JinchurikiState(Optional.empty(), 0, 0, 0, 0);

    /** Codec del sub-estado; todos los campos son opcionales y caen a {@link #DEFAULT}. */
    public static final Codec<JinchurikiState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("sealedBeast").forGetter(JinchurikiState::sealedBeast),
            Codec.INT.optionalFieldOf("level", 0).forGetter(JinchurikiState::level),
            Codec.INT.optionalFieldOf("grantedMaxChakra", 0).forGetter(JinchurikiState::grantedMaxChakra),
            Codec.INT.optionalFieldOf("bondTicks", 0).forGetter(JinchurikiState::bondTicks),
            Codec.INT.optionalFieldOf("baryonUse", 0).forGetter(JinchurikiState::baryonUse)
    ).apply(instance, JinchurikiState::new));

    /** Normaliza: nivel y prestamo nunca negativos (NBT corrupto no rompe el tick). */
    public JinchurikiState {
        level = Math.max(level, 0);
        grantedMaxChakra = Math.max(grantedMaxChakra, 0);
        bondTicks = Math.max(bondTicks, 0);
        baryonUse = Math.max(baryonUse, 0);
    }

    /** Indica si el ninja lleva una bestia sellada. */
    public boolean isJinchuriki() {
        return sealedBeast.isPresent();
    }

    /** Copia con una bestia recien sellada, anotando cuanto chakra maximo presto. */
    public JinchurikiState withSealed(String beastId, int level, int grantedMaxChakra) {
        return new JinchurikiState(Optional.of(beastId), level, grantedMaxChakra, 0,
                com.zamu.dojutsu.domain.bijuu.JinchurikiFormTable.BARYON_DURATION_TICKS);
    }

    /** Copia sin bestia: extraerla lo deja todo a cero, incluido el prestamo. */
    public JinchurikiState withoutBeast() {
        return DEFAULT;
    }

    /** Copia con otro nivel de jinchuriki. */
    public JinchurikiState withLevel(int level) {
        return new JinchurikiState(sealedBeast, level, grantedMaxChakra, bondTicks, baryonUse);
    }

    /** Copia con otro vinculo acumulado. */
    public JinchurikiState withBondTicks(int bondTicks) {
        return new JinchurikiState(sealedBeast, level, grantedMaxChakra, bondTicks, baryonUse);
    }

    /** Copia subiendo un nivel de vinculo y reiniciando el contador ({@code bond_increase}). */
    public JinchurikiState withBondLevelUp() {
        return new JinchurikiState(sealedBeast, level + 1, grantedMaxChakra, 0, baryonUse);
    }

    /** Copia con otro remanente de Modo Baryon. */
    public JinchurikiState withBaryonUse(int baryonUse) {
        return new JinchurikiState(sealedBeast, level, grantedMaxChakra, bondTicks, baryonUse);
    }
}
