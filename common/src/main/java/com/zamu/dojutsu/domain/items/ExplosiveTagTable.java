package com.zamu.dojutsu.domain.items;

/**
 * Constantes del <b>Sello Explosivo</b> suelto {@code [S14]} (F10/DJT-225). Verificadas en
 * {@code items/main.mcfunction} (bloque "Explosive Tag"), {@code items/explosive_tag/place.mcfunction},
 * {@code items/explosive_tag/main.mcfunction} y los predicates {@code items/extagtimer{,_0}}.
 *
 * <h2>Es una mina, no una granada</h2>
 * El sello no explota al soltarlo: se <b>coloca</b> y tarda {@value #ARM_TICKS} ticks (10 s) en armarse
 * ({@code d.extagtimer}). Solo cuando el contador llega a 0 se marca {@code d.primed}, y a partir de ahi detona
 * en cuanto <b>cualquier</b> entidad que no sea decorado entre en {@value #TRIGGER_RADIUS} bloques. Los diez
 * segundos son la ventana para alejarse: quien lo suelte a sus pies y se quede, lo detona el mismo.
 *
 * <h2>Lo que no lo dispara</h2>
 * El selector del datapack excluye {@code painting}, {@code item_frame}, {@code armor_stand}, {@code item} y los
 * demas sellos. Es decir: lo dispara <b>todo lo que se mueve</b>, incluidos los mobs pacificos, pero no lo
 * disparan ni el cuadro de la pared ni el propio item que lo dejo caer.
 *
 * <h2>Dos daños, no uno</h2>
 * La explosion la produce un {@code tnt_minecart} con {@code TNTFuse:0} y sin velocidad — potencia exacta
 * {@value #EXPLOSION_POWER}, la misma que el kunai con sello. <b>Ademas</b> reparte {@code instant_damage}
 * amplificador {@value #HARM_AMPLIFIER} (Daño II) en el mismo radio: es un golpe magico que la armadura no para
 * y que el radio de la explosion no atenua.
 */
public final class ExplosiveTagTable {

    /** Ticks que tarda en armarse una vez colocado ({@code scoreboard players set @s d.extagtimer 200}). */
    public static final int ARM_TICKS = 200;

    /** Radio de deteccion y de reparto del daño magico, en bloques ({@code distance=0..5}). */
    public static final double TRIGGER_RADIUS = 5.0D;

    /** Potencia de la explosion ({@code summon tnt_minecart {TNTFuse:0}} parado = 4.0 exacto). */
    public static final float EXPLOSION_POWER = 4.0F;

    /** Amplificador del {@code instant_damage} ({@code effect give ... instant_damage 1 1}) = Daño II. */
    public static final int HARM_AMPLIFIER = 1;

    /** Duracion del {@code instant_damage} en ticks (el {@code 1} del comando son segundos). */
    public static final int HARM_DURATION_TICKS = 20;

    private ExplosiveTagTable() {
    }

    /**
     * Indica si un sello con ese contador ya esta armado. El datapack lo parte en dos predicates
     * ({@code extagtimer} = {@code min:1}, {@code extagtimer_0} = exactamente 0); aqui es una comparacion.
     *
     * @param armingTicksLeft ticks que le quedan para armarse
     */
    public static boolean isArmed(int armingTicksLeft) {
        return armingTicksLeft <= 0;
    }
}
