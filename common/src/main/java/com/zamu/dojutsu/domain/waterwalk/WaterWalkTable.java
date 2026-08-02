package com.zamu.dojutsu.domain.waterwalk;

/**
 * Reglas de <b>caminar sobre el agua</b> {@code [S22]} (F9/DJT-205). Dominio puro, verificado contra
 * {@code main/internal/water_walking/{main,walk,cost,end,hit}} y el predicado {@code can_walk_on_water}.
 *
 * <h2>No es un flag: es un umbral de progresion</h2>
 * La ficha pide "confirmar el flag {@code can_walk_on_water}, por defecto true". <b>No existe tal flag.</b>
 * {@code can_walk_on_water} es un <b>predicado</b> con dos condiciones de puntuacion:
 * {@code d.max_chakra >= }{@value #REQUIRED_MAX_CHAKRA} y {@code d.chakra >= }{@value #REQUIRED_CHAKRA}. O sea
 * que no se activa ni se desactiva por configuracion: <b>se desbloquea creciendo</b>, y se pierde al quedarte
 * sin chakra. Modelarlo como un booleano de config habria borrado la progresion entera.
 *
 * <h2>Cuesta poco, pero constantemente</h2>
 * {@value #CHAKRA_COST} de chakra cada {@value #COST_INTERVAL_TICKS} ticks ({@code walk} sube
 * {@code d.waterwalk_time} y {@code cost} cobra al llegar a 2, reiniciandolo). Es un goteo, no un peaje: el
 * limite real es que al bajar de {@value #REQUIRED_CHAKRA} de chakra dejas de cumplir el predicado y te hundes.
 *
 * <h2>La levitacion no puede sostener por si sola</h2>
 * {@code walk} aplica {@code levitation 1 255 true} cada tick. Lo unico que hace falta de ella es que su rama en
 * {@code LivingEntity.travel} <b>sustituye a la gravedad</b>; el empuje que aplica no sirve para flotar:
 * <ul>
 *   <li>La formula es {@code vy + (0.05·(amp+1) − vy)·0.2}, o sea que tiende a {@code 0.05·(amp+1)}.</li>
 *   <li>{@code MobEffectInstance} hace <b>{@code Mth.clamp(amplifier, 0, 255)}</b> en el constructor
 *       (verificado en el bytecode de 1.21.1), asi que <b>no hay amplificador negativo</b>: el minimo es 0 y su
 *       objetivo es {@code +0.05} — sube despacio, no se queda quieto.</li>
 * </ul>
 * Por eso el sostenimiento no se delega al efecto: la levitacion solo <b>apaga la gravedad</b>
 * ({@value #HOVER_AMPLIFIER}) y quien fija la altura es el servicio, anclando al jugador a la superficie. El
 * empujon hacia abajo de {@code hit} si es un amplificador alto de verdad ({@value #SINK_AMPLIFIER}).
 *
 * <p><b>Correccion:</b> una version anterior de este fichero afirmaba que 255 se leia como -1 por
 * desbordamiento de byte y que eso congelaba la caida. Es falso —el clamp ocurre al construir el efecto— y
 * producia el sintoma de "caminar un poco y hundirse".
 */
public final class WaterWalkTable {

    /** Chakra maximo minimo para poder caminar sobre el agua ({@code can_walk_on_water}). */
    public static final int REQUIRED_MAX_CHAKRA = 300;

    /** Chakra actual minimo; por debajo, te hundes. */
    public static final int REQUIRED_CHAKRA = 2;

    /** Chakra que cuesta cada cobro ({@code cost}: {@code chakra -= 1}). */
    public static final int CHAKRA_COST = 1;

    /** Ticks entre cobros ({@code waterwalk_time >= 2}, contando desde 0: se cobra al tercero). */
    public static final int COST_INTERVAL_TICKS = 3;

    /** Profundidad a la que se busca el agua bajo los pies ({@code block ~ ~-0.35 ~}). */
    public static final double SURFACE_PROBE = -0.35;

    /**
     * Amplificador de la levitacion mientras se camina. Su unico papel es <b>apagar la gravedad</b>: el minimo
     * posible (el clamp impide negativos) para que su empuje residual sea el menor posible.
     */
    public static final int HOVER_AMPLIFIER = 0;

    /** Amplificador que <b>hunde</b> al recibir un golpe ({@code hit}: {@code levitation 1 128}). */
    public static final int SINK_AMPLIFIER = 128;

    /**
     * Margen vertical, en bloques, dentro del cual se considera que el ninja sigue "sobre" la superficie y se le
     * ancla a ella. Cubre el empuje residual de la levitacion y el paso de una ola sin soltarlo.
     */
    public static final double SURFACE_TOLERANCE = 0.6;

    /**
     * Desvio a partir del cual el servidor corrige la altura. Es una <b>red de seguridad</b>, no el mecanismo de
     * sostenimiento: quien mantiene la altura es el propio cliente ({@code WaterWalkClient}), que conoce el
     * estado. Corregir con un umbral fino convertia cada tick en un tiron — ese fue el sintoma reportado.
     */
    public static final double SNAP_EPSILON = 0.25;

    private WaterWalkTable() {
    }

    /**
     * Indica si el ninja tiene chakra suficiente para sostenerse ({@code can_walk_on_water}). Es la unica
     * "llave" del sistema: ni config, ni item, ni habilidad aprendida.
     */
    public static boolean canWalk(int maxChakra, int chakra) {
        return maxChakra >= REQUIRED_MAX_CHAKRA && chakra >= REQUIRED_CHAKRA;
    }

    /** Indica si con esos ticks acumulados toca cobrar. */
    public static boolean shouldCharge(int ticksSinceCharge) {
        return ticksSinceCharge >= COST_INTERVAL_TICKS - 1;
    }

    /**
     * Indica si unos pies a {@code feetY} siguen "sobre" una superficie a {@code surfaceY}: se admite
     * {@value #SURFACE_TOLERANCE} por arriba (empuje residual, oleaje) y lo mismo por abajo (haber empezado a
     * hundirse pero aun recuperable). Fuera de esa banda, se suelta.
     */
    public static boolean nearSurface(double feetY, double surfaceY) {
        return Math.abs(feetY - surfaceY) <= SURFACE_TOLERANCE;
    }

    /** Indica si la desviacion respecto a la superficie justifica corregir la altura. */
    public static boolean shouldSnap(double feetY, double surfaceY) {
        return Math.abs(feetY - surfaceY) > SNAP_EPSILON;
    }
}
