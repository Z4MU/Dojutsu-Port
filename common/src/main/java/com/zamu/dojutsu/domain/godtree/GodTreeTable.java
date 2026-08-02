package com.zamu.dojutsu.domain.godtree;

import java.util.List;

/**
 * Reglas del <b>Arbol Divino</b> {@code [S21]} (F9/DJT-215). Dominio puro, verificado contra
 * {@code god_tree/{main,grow,find_ply,awaken_rinne_sharingan}} y
 * {@code jinchuriki/form/ten_tails/plant_god_tree/*}.
 *
 * <h2>No hay semilla que plantar</h2>
 * La ficha pide un {@code GodTreeSapling} —un item o bloque—. <b>No existe</b>: el arbol se planta desde la
 * <b>forma del Diez Colas</b> (slot 8 + sneak), pagando {@value #PLANT_CHAKRA} de chakra. La "semilla" del
 * datapack es la primera estructura que se coloca, no un objeto del inventario.
 *
 * <h2>Cinco etapas de cinco minutos</h2>
 * {@code main} corre <b>por tick</b> sobre el marcador del arbol y suma {@code d.godtree_time}; al llegar a
 * {@value #TICKS_PER_STAGE} llama a {@code grow}, que sube una etapa, coloca la estructura correspondiente y
 * reinicia el contador. Son {@value #MAX_STAGE} etapas, o sea <b>{@value #TOTAL_TICKS} ticks</b> en total.
 *
 * <p><b>Anomalia documentada:</b> el mensaje al plantar dice "In 15 minutes, the flower will bloom", pero
 * {@value #MAX_STAGE} × {@value #TICKS_PER_STAGE} ticks son <b>25 minutos</b>. El texto se porta tal cual —es lo
 * que ve el jugador— pero el numero que manda es el de las etapas.
 *
 * <h2>El premio</h2>
 * Al llegar a la ultima etapa, {@code find_ply} busca al jugador <b>vinculado</b> al arbol y le despierta el
 * <b>Rinne Sharingan</b>. Los bonus (+100 de chakra maximo, +1000 de ninja_xp) no viven aqui: son
 * {@code awakenMaxChakraBonus}/{@code awakenNinjaXpBonus} de la definicion del ojo, que es donde el mod ya los
 * modela para todos los despertares.
 */
public final class GodTreeTable {

    /** Chakra que cuesta plantarlo ({@code plant_god_tree/cast}: {@code chakra >= 1000}, luego {@code -= 1000}). */
    public static final int PLANT_CHAKRA = 1000;

    /** Veto de la forma tras plantar ({@code jinform_timer = 300}). */
    public static final int PLANT_COOLDOWN_TICKS = 300;

    /** Bloques que sube al jugador al plantar, para que el arbol no lo sepulte ({@code tp @s ~ ~3 ~}). */
    public static final int PLANT_LIFT = 3;

    /** Ticks entre etapas ({@code godtree_time}: {@code min 6000}) = 5 minutos. */
    public static final int TICKS_PER_STAGE = 6000;

    /** Etapa final ({@code godtree_stage_5}); {@code godtree_stage_6p} la vuelve a clampar aqui. */
    public static final int MAX_STAGE = 5;

    /** Ticks totales de crecimiento: 25 minutos, pese a lo que diga el mensaje al plantar. */
    public static final int TOTAL_TICKS = TICKS_PER_STAGE * MAX_STAGE;

    /** Id del ojo que despierta al florecer ({@code dojutsu/give/rinne_sharingan}). */
    public static final String REWARD_EYE = "rinne_sharingan";

    /** Radio en el que se oye crecer el arbol ({@code playsound ... @a[distance=0..50]}). */
    public static final int GROW_SOUND_RADIUS = 50;

    /** Estructura que se coloca al plantar, antes de la primera etapa ({@code plant_god_tree/summon}). */
    public static final String SAPLING_STRUCTURE = "god_tree/sapling/god_sapling_0";

    /**
     * Estructura de cada etapa 1..{@value #MAX_STAGE}, en orden. La primera es el otro brote; el resto es el
     * arbol creciendo.
     */
    public static final List<String> STAGE_STRUCTURES = List.of(
            "god_tree/sapling/god_sapling_1",
            "god_tree/god_tree_0",
            "god_tree/god_tree_1",
            "god_tree/god_tree_2",
            "god_tree/god_tree_3");

    /**
     * Desplazamiento horizontal con el que se coloca cada estructura, tomado del {@code posX}/{@code posZ} del
     * structure block del datapack. El brote inicial va a -1, el segundo a -3 y el arbol a -8: la estructura
     * crece centrada en el punto donde se planto.
     */
    public static final int SAPLING_OFFSET = -1;

    /** Desplazamiento del segundo brote ({@code grow} etapa 1: {@code posX/posZ = -3}). */
    public static final int STAGE_1_OFFSET = -3;

    /** Desplazamiento del arbol ya crecido ({@code grow} etapas 2..5: {@code posX/posZ = -8}). */
    public static final int TREE_OFFSET = -8;

    private GodTreeTable() {
    }

    /** Estructura que toca colocar al entrar en esa etapa (1..{@value #MAX_STAGE}). */
    public static String structureForStage(int stage) {
        if (stage < 1 || stage > MAX_STAGE) {
            throw new IllegalArgumentException("etapa fuera de rango: " + stage);
        }
        return STAGE_STRUCTURES.get(stage - 1);
    }

    /** Desplazamiento horizontal de la estructura de esa etapa; el brote y el arbol no se colocan igual. */
    public static int offsetForStage(int stage) {
        return stage == 1 ? STAGE_1_OFFSET : TREE_OFFSET;
    }

    /** Indica si con esos ticks acumulados toca crecer una etapa. */
    public static boolean readyToGrow(int timerTicks) {
        return timerTicks >= TICKS_PER_STAGE;
    }

    /** Indica si el arbol ya florecio y toca dar el premio ({@code godtree_stage_5}). */
    public static boolean isBloomed(int stage) {
        return stage >= MAX_STAGE;
    }
}
