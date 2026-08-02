package com.zamu.dojutsu.data.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zamu.dojutsu.domain.sage.NaturalEnergyTable;
import com.zamu.dojutsu.domain.sage.SageType;
import java.util.Optional;

/**
 * Sub-estado del <b>modo sabio</b> {@code [S08]} (F6/DJT-135): si es sabio, de que linaje, su nivel y la energia
 * natural reunida. Sub-estado enchufable de {@link com.zamu.dojutsu.data.PlayerNinjaData}.
 *
 * <p>Campos verificados contra los scoreboards del datapack: {@code d.is_sage}, {@code d.sage_type},
 * {@code d.sage_level}, {@code d.sagechakra} (la carga), {@code d.sage_use} (uso hacia el siguiente nivel).
 *
 * <p><b>La energia natural es carga, no un recurso gastable</b>: se acumula estando quieto hasta
 * {@link NaturalEnergyTable#MAX_CHARGE} y al llenarse te vuelve sabio. No es un segundo {@code Resource} con
 * regen propia — el datapack la trata como un contador simple ({@code sagechakra += 1} por pulso).
 *
 * <p><b>El encendido del modo no vive aqui</b> (mismo criterio que {@link SusanooState}/{@link EmpowermentState}):
 * lo lleva el arnes {@code ToggleableModeService}, asi que reconectar no te deja en modo sabio. Lo que si
 * persiste es <b>haberte vuelto sabio</b> y tu progreso.
 *
 * <p>Value object <b>inmutable</b> (regla 10b): se reemplaza con withers.
 *
 * @param sage      ya es sabio ({@code d.is_sage}): puede activar el modo
 * @param type      linaje despertado segun el bioma ({@code d.sage_type}); vacio hasta volverse sabio
 * @param level     nivel de sabio 1..5 ({@code d.sage_level}); 0 si aun no lo es
 * @param charge    energia natural reunida 0..100 ({@code d.sagechakra})
 * @param stillTicks ticks acumulado quieto ({@code d.still_timer}+{@code d.still_wait}); se reinicia al moverse
 * @param use       uso acumulado del modo hacia el siguiente nivel ({@code d.sage_use})
 * @param oiled     lleva aceite de sapo: acelera la carga a la mitad ({@code is_oiled})
 * @param sixPaths  chakra de los Seis Caminos {@code [S08/S19]} (F9/DJT-210), <b>anidado</b> aqui: el agregado ya
 *                  agota los 16 componentes que admite {@code RecordCodecBuilder.group} (techo de DataFixerUpper),
 *                  y de los sub-estados existentes el sabio es el anfitrion correcto — el datapack archiva esas
 *                  funciones bajo {@code sage/six_paths/} y {@code d.is_sage 2} es estado de sabio. Se lee por el
 *                  atajo {@code PlayerNinjaData.sixPaths()}, asi que sus consumidores no ven el anidamiento
 */
public record SageState(boolean sage, Optional<SageType> type, int level, int charge, int stillTicks,
        int use, boolean oiled, SixPathsState sixPaths) {

    /** Ninja que aun no medito nunca. */
    public static final SageState DEFAULT =
            new SageState(false, Optional.empty(), 0, 0, 0, 0, false, SixPathsState.DEFAULT);

    /** Codec del sub-estado; todos los campos opcionales caen a {@link #DEFAULT}. El tipo viaja por su id. */
    public static final Codec<SageState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("sage", false).forGetter(SageState::sage),
            Codec.STRING.optionalFieldOf("type").forGetter(s -> s.type().map(SageType::id)),
            Codec.INT.optionalFieldOf("level", 0).forGetter(SageState::level),
            Codec.INT.optionalFieldOf("charge", 0).forGetter(SageState::charge),
            Codec.INT.optionalFieldOf("stillTicks", 0).forGetter(SageState::stillTicks),
            Codec.INT.optionalFieldOf("use", 0).forGetter(SageState::use),
            Codec.BOOL.optionalFieldOf("oiled", false).forGetter(SageState::oiled),
            SixPathsState.CODEC.optionalFieldOf("sixPaths", SixPathsState.DEFAULT).forGetter(SageState::sixPaths)
    ).apply(instance, SageState::fromCodec));

    /** Normaliza: nivel 0..5, carga 0..100, contadores no negativos (NBT corrupto no rompe el tick). */
    public SageState {
        level = Math.min(Math.max(level, 0), NaturalEnergyTable.MAX_SAGE_LEVEL);
        charge = Math.min(Math.max(charge, 0), NaturalEnergyTable.MAX_CHARGE);
        stillTicks = Math.max(stillTicks, 0);
        use = Math.max(use, 0);
    }

    /** Reconstruccion tolerante: un tipo desconocido se descarta en vez de romper la carga. */
    private static SageState fromCodec(boolean sage, Optional<String> typeId, int level, int charge,
            int stillTicks, int use, boolean oiled, SixPathsState sixPaths) {
        return new SageState(sage, typeId.flatMap(SageType::byId), level, charge, stillTicks, use, oiled,
                sixPaths);
    }

    /** {@code true} si la energia natural esta al maximo: toca volverse sabio ({@code is_at_max}). */
    public boolean fullyCharged() {
        return NaturalEnergyTable.isFullyCharged(charge);
    }

    /** Copia con otra energia natural reunida (clampada por el constructor). */
    public SageState withCharge(int value) {
        return new SageState(sage, type, level, value, stillTicks, use, oiled, sixPaths);
    }

    /** Copia con otro acumulado de quietud (0 al moverse). */
    public SageState withStillTicks(int value) {
        return new SageState(sage, type, level, charge, value, use, oiled, sixPaths);
    }

    /** Copia convirtiendo al ninja en sabio del linaje dado, en nivel 1 (el {@code become_sage} del datapack). */
    public SageState becomeSage(SageType sageType) {
        return new SageState(true, Optional.of(sageType), Math.max(level, 1), charge, stillTicks, use, oiled, sixPaths);
    }

    /** Copia con otro nivel de sabio (clampado a 0..5), reiniciando el uso hacia el siguiente. */
    public SageState withLevel(int value) {
        return new SageState(sage, type, value, charge, stillTicks, 0, oiled, sixPaths);
    }

    /** Copia con otro uso acumulado hacia el siguiente nivel. */
    public SageState withUse(int value) {
        return new SageState(sage, type, level, charge, stillTicks, value, oiled, sixPaths);
    }

    /** Copia con el aceite de sapo puesto o gastado. */
    public SageState withOiled(boolean value) {
        return new SageState(sage, type, level, charge, stillTicks, use, value, sixPaths);
    }

    /**
     * Copia con otro estado de los Seis Caminos. La llama {@code SixPathsService} a traves de
     * {@code PlayerNinjaData.setSixPaths}, que sigue siendo la unica puerta de escritura.
     */
    public SageState withSixPaths(SixPathsState value) {
        return new SageState(sage, type, level, charge, stillTicks, use, oiled, value);
    }
}
