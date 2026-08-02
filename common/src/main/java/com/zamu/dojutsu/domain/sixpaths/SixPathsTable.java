package com.zamu.dojutsu.domain.sixpaths;

import com.zamu.dojutsu.domain.nature.NatureType;
import java.util.List;

/**
 * Reglas del <b>chakra de los Seis Caminos</b> {@code [S08/S19]} (F9/DJT-210). Dominio puro, verificado contra
 * {@code sage/six_paths/{obtain_six_paths_chakra,tmp_sp_chakra,take_sp_chakra,unlock_spsm}}.
 *
 * <h2>Un solo estado, y ya existia: {@code d.sp_chakra}</h2>
 * La ficha pedia "definir el estado unico y refactorizar las 3 vias para activarlo". El datapack <b>ya</b> lo
 * tiene resuelto asi: un unico flag y una unica funcion que lo concede. Lo que hacen las vias es <b>llamarla</b>,
 * no reimplementarla. Este ticket no inventa la convergencia — la porta.
 *
 * <h2>Permanente y prestado no son la misma concesion</h2>
 * Hay <b>dos</b> funciones, y difieren en algo mas que el nombre:
 * <ul>
 *   <li>{@code obtain_six_paths_chakra} (permanente) — chakra, XP, <b>las siete naturalezas</b>,
 *       +{@value #AFFINITY_BONUS} a cada afinidad y +{@value #SENJUTSU_LEVELS} de senjutsu.</li>
 *   <li>{@code tmp_sp_chakra} (prestado) — <b>solo</b> chakra y XP. Nada de naturalezas ni afinidades.</li>
 * </ul>
 * El Diez Colas usa la prestada y marca {@code d.tmp_sp_chakra}, que {@code natures/remove_tmp} barre junto a las
 * naturalezas y KG prestados. O sea: el chakra de los Seis Caminos <b>ya era parte del sistema de prestamos</b>
 * del datapack, el mismo que el mod modela desde F3. No hace falta maquinaria nueva.
 *
 * <p><b>Anomalia documentada:</b> {@code tmp_sp_chakra} imprime "• Obtained all Nature Affinities" pero no
 * concede ninguna. No es inconsecuente: quien sella al Diez Colas <b>si</b> recibe las naturalezas, pero se las da
 * {@code jinchuriki/sealing/ten_tails} por su cuenta (como {@code d.tmp_*}), no esta funcion. El mensaje miente
 * sobre quien lo hace, no sobre el resultado.
 *
 * <h2>Escalas</h2>
 * Chakra, ninja_xp y afinidades van <b>1:1</b>; el senjutsu es una <i>skill</i> y va en <b>escala del mod</b>
 * (datapack ÷ 4), la misma convencion calibrada en {@code SoahTable} (medical 1500 → 375) y {@code EightGatesTable}.
 */
public final class SixPathsTable {

    /** Chakra maximo y actual que concede ({@code max_chakra/chakra += 6000}). */
    public static final int CHAKRA_BONUS = 6000;

    /** Ninja_xp por obtenerlo ({@code ninja_xp += 200}); {@code take_sp_chakra} lo descuenta igual. */
    public static final int NINJA_XP = 200;

    /** Afinidad que suma a <b>cada</b> naturaleza ({@code aff_* += 100}); escala nativa, sin reescalar. */
    public static final int AFFINITY_BONUS = 100;

    /** Senjutsu que concede, en escala del mod ({@code senjutsu += 300} del datapack ÷ 4). */
    public static final int SENJUTSU_LEVELS = 75;

    /** Ninja_xp por cada naturaleza <b>elemental</b> que no tuviera ({@code has_fire/wind/ligh/earth/water}). */
    public static final int NATURE_XP_ELEMENT = 20;

    /** Ninja_xp por Yin o Yang si no los tuviera; valen mas que los elementales. */
    public static final int NATURE_XP_YIN_YANG = 30;

    /**
     * Chakra maximo extra del Modo Sabio de los Seis Caminos ({@code unlock_spsm}: {@code max_chakra += 50}).
     *
     * <p><b>Ojo:</b> {@code unlock_spsm} <b>no lo llama nadie</b> en el datapack publicado — su unico invocador
     * son lineas comentadas de {@code still_check}. Se porta el numero por fidelidad, pero la via que lo usaria
     * no existe (ver {@link #RINNEGAN_ROUTE_DISABLED}).
     */
    public static final int SIX_PATHS_SAGE_MAX_CHAKRA = 50;

    /**
     * La via del <b>Rinnegan</b> hacia los Seis Caminos esta <b>desactivada</b> en el datapack publicado.
     *
     * <p>La ficha de DJT-210 nombra tres vias: Rinnegan (S05), Diez Colas (S09) y eventos (S19). En
     * {@code still_check} las tres lineas que concederian el chakra por sabio nivel 5 + Uchiha, o por Otsutsuki
     * con el Rinnegan despierto, estan <b>comentadas</b> — junto con la que llamaria a {@code unlock_spsm}. No es
     * una via lenta ni rara: no se ejecuta. Portarla seria inventar contenido, asi que no se implementa; queda
     * anotada por si se decide reactivarla como decision de diseño.
     */
    public static final boolean RINNEGAN_ROUTE_DISABLED = true;

    /** Las siete naturalezas que concede la version permanente, en el orden del datapack. */
    public static final List<NatureType> GRANTED_NATURES = List.of(
            NatureType.FIRE, NatureType.WIND, NatureType.LIGHTNING, NatureType.EARTH, NatureType.WATER,
            NatureType.YIN, NatureType.YANG);

    private SixPathsTable() {
    }

    /**
     * Ninja_xp que da conceder esa naturaleza si el jugador aun no la tenia: {@value #NATURE_XP_YIN_YANG} para
     * Yin y Yang, {@value #NATURE_XP_ELEMENT} para las cinco elementales.
     */
    public static int natureXp(NatureType nature) {
        return nature == NatureType.YIN || nature == NatureType.YANG ? NATURE_XP_YIN_YANG : NATURE_XP_ELEMENT;
    }
}
