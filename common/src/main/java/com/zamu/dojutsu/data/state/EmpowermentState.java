package com.zamu.dojutsu.data.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

/**
 * Sub-estado de los <b>modos de potenciación</b> de F6 (DJT-130): sello maldito {@code [S10]}, Ocho Puertas
 * {@code [S11]} y SOAH {@code [S12]}. Sub-estado enchufable de {@link com.zamu.dojutsu.data.PlayerNinjaData},
 * siguiendo la receta que documenta el propio agregado.
 *
 * <p><b>Solo persiste lo que sería injusto perder</b> (mismo criterio que {@link SusanooState}): el sello que
 * llevas y su nivel, la fatiga corporal acumulada y la carga del SOAH. El <b>encendido</b> de cada modo es
 * estado de ejecución del arnés ({@code ToggleableModeService}), no se guarda — reconectar no debe dejarte con
 * las Ocho Puertas abiertas. Y lo que se puede <b>derivar</b> tampoco se guarda: la puerta máxima y la maestría
 * salen del taijutsu (F6/DJT-145), como en el datapack.
 *
 * <p>Campos verificados contra los scoreboards del datapack ({@code d.cseal_id}, {@code d.cseal_lvl},
 * {@code d.cseal_deaths}, {@code d.eg_strain}, {@code d.soah_chakra}, {@code d.soah_gave}). Value object
 * <b>inmutable</b> (regla 10b): se reemplaza con withers.
 *
 * @param cursedSeal      id del sello maldito que porta ({@code "heaven"}, {@code "earth"}, {@code "jirobo"},
 *                        {@code "kidomaru"}, {@code "sukon_ukon"}, {@code "tayuya"}); vacio = ninguno
 * @param cursedSealLevel nivel del sello 1..2 ({@code d.cseal_lvl}); 0 si no porta ninguno
 * @param cursedSealDeaths muertes por <b>agotamiento</b> sosteniendo el sello ({@code d.cseal_deaths}); a las
 *                        10 en nivel 2 el sello degrada ({@code cursed_seal/punish})
 * @param gateStrain      fatiga corporal de las puertas ({@code d.eg_strain}); se resetea al dormir y al morir.
 *                        La puerta maxima ({@code d.eg_max}) y la maestria ({@code d.eg_mastery}) <b>no</b> se
 *                        guardan: el datapack las recalcula cada tick desde el taijutsu, y aqui son funciones
 *                        puras ({@code EightGatesTable.maxGate/hasMastery}) que no pueden desincronizarse
 * @param soahCharge      carga del sello SOAH ({@code d.soah_chakra}); habilita los tiers de autocuracion
 * @param soahGranted     ya recibio el sello SOAH ({@code d.soah_gave})
 */
public record EmpowermentState(Optional<String> cursedSeal, int cursedSealLevel, int cursedSealDeaths,
        int gateStrain, int soahCharge, boolean soahGranted) {

    /** Ninja sin potenciaciones: sin sello, sin puertas dominadas, sin SOAH. */
    public static final EmpowermentState DEFAULT =
            new EmpowermentState(Optional.empty(), 0, 0, 0, 0, false);

    /** Puertas totales {@code [S11]}: la octava es la de la Muerte. */
    public static final int MAX_GATES = 8;

    /** Tope de la fatiga corporal ({@code main}: {@code eg_strain_101p → set 100}). */
    public static final int MAX_GATE_STRAIN = 100;

    /** Nivel maximo del sello maldito {@code [S10]}. */
    public static final int MAX_SEAL_LEVEL = 2;

    /** Codec del sub-estado; todos los campos son opcionales y caen a {@link #DEFAULT}. */
    public static final Codec<EmpowermentState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("cursedSeal").forGetter(EmpowermentState::cursedSeal),
            Codec.INT.optionalFieldOf("cursedSealLevel", 0).forGetter(EmpowermentState::cursedSealLevel),
            Codec.INT.optionalFieldOf("cursedSealDeaths", 0).forGetter(EmpowermentState::cursedSealDeaths),
            Codec.INT.optionalFieldOf("gateStrain", 0).forGetter(EmpowermentState::gateStrain),
            Codec.INT.optionalFieldOf("soahCharge", 0).forGetter(EmpowermentState::soahCharge),
            Codec.BOOL.optionalFieldOf("soahGranted", false).forGetter(EmpowermentState::soahGranted)
    ).apply(instance, EmpowermentState::new));

    /** Normaliza: niveles y contadores dentro de rango (NBT corrupto no rompe el tick). */
    public EmpowermentState {
        cursedSealLevel = Math.min(Math.max(cursedSealLevel, 0), MAX_SEAL_LEVEL);
        cursedSealDeaths = Math.max(cursedSealDeaths, 0);
        gateStrain = Math.min(Math.max(gateStrain, 0), MAX_GATE_STRAIN);
        soahCharge = Math.max(soahCharge, 0);
    }

    /** Indica si porta un sello maldito. */
    public boolean hasCursedSeal() {
        return cursedSeal.isPresent();
    }

    /** Copia con un sello maldito recien recibido (nivel 1). */
    public EmpowermentState withCursedSeal(String sealId) {
        return new EmpowermentState(Optional.of(sealId), 1, cursedSealDeaths, gateStrain,
                soahCharge, soahGranted);
    }

    /** Copia sin sello maldito (nivel a 0). */
    public EmpowermentState withoutCursedSeal() {
        return new EmpowermentState(Optional.empty(), 0, cursedSealDeaths, gateStrain,
                soahCharge, soahGranted);
    }

    /** Copia con otro nivel de sello (clampado a 1..2 por el constructor). */
    public EmpowermentState withCursedSealLevel(int level) {
        return new EmpowermentState(cursedSeal, level, cursedSealDeaths, gateStrain,
                soahCharge, soahGranted);
    }

    /** Copia sumando una muerte por inyeccion fallida. */
    public EmpowermentState withCursedSealDeath() {
        return new EmpowermentState(cursedSeal, cursedSealLevel, cursedSealDeaths + 1, gateStrain,
                soahCharge, soahGranted);
    }

    /** Copia con otra fatiga corporal (clampada a 0..100 por {@link #MAX_GATE_STRAIN}). */
    public EmpowermentState withGateStrain(int strain) {
        return new EmpowermentState(cursedSeal, cursedSealLevel, cursedSealDeaths, strain,
                soahCharge, soahGranted);
    }

    /** Copia con otra carga de SOAH (no negativa). */
    public EmpowermentState withSoahCharge(int charge) {
        return new EmpowermentState(cursedSeal, cursedSealLevel, cursedSealDeaths, gateStrain,
                charge, soahGranted);
    }

    /** Copia marcando que ya recibio el sello SOAH. */
    public EmpowermentState withSoahGranted(boolean granted) {
        return new EmpowermentState(cursedSeal, cursedSealLevel, cursedSealDeaths, gateStrain,
                soahCharge, granted);
    }
}
