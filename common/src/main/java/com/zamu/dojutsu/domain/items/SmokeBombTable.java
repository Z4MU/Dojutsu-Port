package com.zamu.dojutsu.domain.items;

/**
 * Constantes de la <b>Bomba de Humo</b> {@code [S14]} (F10/DJT-225). Verificadas en
 * {@code items/smoke_bomb/use.mcfunction} y {@code items/smoke_bomb/smoke.mcfunction}.
 *
 * <h2>La bomba de humo no ciega a nadie</h2>
 * Al portarla uno espera {@code blindness}: no la hay. El datapack no aplica <b>ningun</b> efecto de estado — solo
 * suelta {@value #PARTICLE_COUNT} particulas {@code large_smoke} por tick durante
 * {@value #DURATION_TICKS} ticks y suena un {@code redstone_torch.burnout}. La cortina es literal: <b>tapa la
 * vista</b> de quien mire a traves, y a quien no le tape no le hace nada. Portarla con un {@code MobEffect}
 * habria sido mas comodo y habria cambiado el juego.
 *
 * <p>Se usa <b>soltandola</b> ({@code items/main}: {@code @e[type=item,predicate=items/smoke_bomb]}, y el
 * predicate exige {@code OnGround:1b}), no con clic derecho.
 */
public final class SmokeBombTable {

    /** Duracion de la cortina en ticks ({@code d.smokebombtime 120} = 6 s). */
    public static final int DURATION_TICKS = 120;

    /** Particulas {@code large_smoke} por tick ({@code particle large_smoke ... 100 force}). */
    public static final int PARTICLE_COUNT = 100;

    /** Radio de la nube en bloques, por eje ({@code ... 2 2 2 ...}). */
    public static final double PARTICLE_SPREAD = 2.0D;

    /** Velocidad de las particulas ({@code ... 0.05 ...}). */
    public static final double PARTICLE_SPEED = 0.05D;

    /** Altura del centro de la nube sobre la bomba ({@code particle ... ~ ~1 ~}). */
    public static final double PARTICLE_HEIGHT = 1.0D;

    /** Altura a la que aparece el marcador de humo sobre el item soltado ({@code summon marker ~ ~.5 ~}). */
    public static final double SPAWN_HEIGHT = 0.5D;

    /** Radio en bloques donde se oye el chisporroteo ({@code @a[distance=0..10]}). */
    public static final double SOUND_RADIUS = 10.0D;

    /** Volumen del {@code redstone_torch.burnout}. */
    public static final float SOUND_VOLUME = 0.2F;

    /** Tono del {@code redstone_torch.burnout}. */
    public static final float SOUND_PITCH = 1.5F;

    private SmokeBombTable() {
    }
}
