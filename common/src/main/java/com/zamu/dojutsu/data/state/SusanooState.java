package com.zamu.dojutsu.data.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zamu.dojutsu.domain.susanoo.SusanooCostTable;

/**
 * Sub-estado del Susanoo {@code [S24]} (DJT-120): la <b>maestria</b> del portador — que nivel domina y cuanto
 * lleva usado hacia el siguiente. Sub-estado propio (no un campo de {@link EyeState}) porque S24 es un sistema
 * aparte de S05, siguiendo la receta de sub-estado enchufable de
 * {@link com.zamu.dojutsu.data.PlayerNinjaData}.
 *
 * <p><b>Solo persiste la maestria</b>, no si esta encendido: el modo arranca <b>apagado</b> en cada sesion (el
 * datapack tambien lo apaga al perder el ojo, {@code check_deactivate}), asi que el encendido y su temporizador
 * de pulso son estado de ejecucion del servicio, no del guardado. Lo que seria injusto perder al reconectar es
 * el nivel ganado, y eso si vive aqui.
 *
 * <p>Value object <b>inmutable</b> (regla 10b): para cambiar algo se reemplaza con un wither.
 *
 * @param level nivel dominado 1..6 ({@code d.susanoo_lvl}); clampado por el constructor
 * @param use   uso acumulado hacia el siguiente nivel ({@code d.susanoo_use}); nunca negativo
 */
public record SusanooState(int level, int use) {

    /** Portador que aun no ha manifestado nunca su Susanoo: nivel 1 (Ribcage), sin uso. */
    public static final SusanooState DEFAULT = new SusanooState(1, 0);

    /** Codec del sub-estado; ambos campos son opcionales y caen al valor de {@link #DEFAULT}. */
    public static final Codec<SusanooState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("level", DEFAULT.level()).forGetter(SusanooState::level),
            Codec.INT.optionalFieldOf("use", DEFAULT.use()).forGetter(SusanooState::use)
    ).apply(instance, SusanooState::new));

    /** Normaliza: nivel dentro de {@code [1, MAX_LEVEL]} y uso no negativo (NBT corrupto no rompe el tick). */
    public SusanooState {
        level = Math.min(Math.max(level, 1), SusanooCostTable.MAX_LEVEL);
        use = Math.max(use, 0);
    }

    /** Copia con otro uso acumulado. */
    public SusanooState withUse(int value) {
        return use == Math.max(value, 0) ? this : new SusanooState(level, value);
    }

    /** Copia subiendo un nivel y reiniciando el uso ({@code level/set_N}: {@code susanoo_use = 0}). */
    public SusanooState leveledUp() {
        return level >= SusanooCostTable.MAX_LEVEL ? this : new SusanooState(level + 1, 0);
    }

    /**
     * Copia sumando uso en crudo y aplicando las subidas de nivel que provoque (el {@code susanoo_use += N} del
     * datapack, p. ej. el bono de +78000 al despertar el EMS). En el nivel maximo el uso extra se descarta.
     */
    public SusanooState plusUse(int extra) {
        if (extra <= 0) {
            return this;
        }
        int lvl = level;
        int acc = use + extra;
        while (lvl < SusanooCostTable.MAX_LEVEL && acc >= SusanooCostTable.USE_PER_LEVEL) {
            acc -= SusanooCostTable.USE_PER_LEVEL;
            lvl++;
        }
        int carry = lvl >= SusanooCostTable.MAX_LEVEL ? 0 : acc; // en el tope no se guarda uso sobrante
        return new SusanooState(lvl, carry);
    }
}
