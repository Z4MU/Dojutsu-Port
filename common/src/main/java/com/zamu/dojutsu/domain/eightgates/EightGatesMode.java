package com.zamu.dojutsu.domain.eightgates;

import com.zamu.dojutsu.domain.mode.ToggleableMode;

/**
 * Ocho Puertas {@code [S11]} (F6/DJT-145) como {@link ToggleableMode}: <b>no es un interruptor, es una
 * escalera</b>. La misma tecla abre la siguiente puerta cada vez; agachado, las cierra todas de golpe.
 *
 * <p>Quinto consumidor del framework y el que mejor explica por que el ciclo se dejo abierto: aqui
 * {@code activate} no significa "encender", significa "abrir la <b>primera</b> puerta". Subir de la 1 a la 8 no
 * pasa por el ciclo del framework —el modo ya esta activo— sino por {@link #openNext}, que reutiliza las mismas
 * comprobaciones. El framework sigue automatizando lo unico que le corresponde: cuando el coste (aqui, la
 * fatiga) se vuelve impagable, se apaga solo.
 *
 * <p>Traduce {@code eight_gates/activate} + {@code run} + {@code deactivate}:
 * <ul>
 *   <li>{@code onActivate}: veta por taijutsu, por veto de cierre o por no tener ninguna puerta desbloqueada;
 *       si pasa, abre la 1ª.</li>
 *   <li>{@code onTick}: aplica los efectos de la puerta abierta y suma fatiga cada
 *       {@value EightGatesTable#STRAIN_PULSE_TICKS} ticks, mas rapido cuanto mas arriba estas.</li>
 *   <li>{@code onCost}: <b>no cuesta chakra</b> — el coste es la fatiga. Devuelve {@code false} al llegar a
 *       {@value EightGatesTable#FORCE_CLOSE_AT}, y el framework cierra.</li>
 *   <li>{@code onDeactivate}: cierra todas, cobra el <b>coste de cerrar</b> (que duele si la fatiga estaba
 *       alta) y deja el veto de {@value EightGatesTable#CLOSE_COOLDOWN_TICKS} ticks.</li>
 * </ul>
 *
 * <p><b>La 8ª puerta no mata</b> en el datapack, al contrario de lo que dice la ficha de {@code [S11]}: no hay
 * ningun {@code kill} en {@code eight_gates/}, ni en el Night Guy que exige tenerla abierta. Lo que hace es
 * acumular fatiga al cuadruple, y lo que castiga de verdad es <b>pasarse de 95</b>. Se porta como esta (regla
 * 16).
 *
 * <p>Dominio puro: todo lo del jugador pasa por {@link EightGatesModeContext}.
 */
public final class EightGatesMode extends ToggleableMode<EightGatesModeContext> {

    /** Por que no se pudo abrir la siguiente puerta. */
    public enum BlockReason {
        /** Taijutsu por debajo de {@link EightGatesTable#ACTIVATION_TAIJUTSU} ({@code activate}). */
        LOW_TAIJUTSU,
        /** Aun corre el veto tras haber cerrado ({@code d.eg_cd}). */
        CLOSE_COOLDOWN,
        /** Aun corre el veto entre aperturas ({@code d.eg_opencd}). */
        OPEN_COOLDOWN,
        /** Ya esta abierta la ultima puerta que su taijutsu desbloquea. */
        NO_MORE_GATES
    }

    /** Puerta abierta actualmente ({@code d.eg_stage}); 0 = todas cerradas. */
    private int stage;

    /** Marca de "este cierre no cobra" (la pone {@link #closeSafely} y la consume {@link #onDeactivate}). */
    private boolean safeClose;

    /** Puerta abierta ahora mismo (1..8), o 0 si estan cerradas. */
    public int stage() {
        return stage;
    }

    /**
     * Abre la <b>siguiente</b> puerta: la 1ª si estaban cerradas, o una mas si ya hay alguna abierta. Es la
     * unica via de subida; el {@code activate} del framework solo cubre el primer escalon.
     *
     * @return {@code true} si se abrio una puerta en esta llamada
     */
    public boolean openNext(EightGatesModeContext ctx) {
        if (!isActive()) {
            return activate(ctx); // el framework llama a onActivate, que abre la 1ª
        }
        return tryOpen(ctx, stage + 1);
    }

    /**
     * Cierra las puertas <b>sin cobrar el coste de cerrar</b> ({@code safe_deactivate}): al respawnear o al
     * resetear el ninja no tiene sentido castigar por una fatiga que ya se limpia aparte.
     */
    public void closeSafely(EightGatesModeContext ctx) {
        if (!isActive()) {
            return;
        }
        safeClose = true;
        deactivate(ctx);
    }

    @Override
    protected boolean onActivate(EightGatesModeContext ctx) {
        return tryOpen(ctx, 1);
    }

    @Override
    protected void onTick(EightGatesModeContext ctx) {
        Gate gate = Gate.forStage(stage).orElse(null);
        if (gate == null) {
            return; // defensivo: activo sin etapa valida no deberia ocurrir
        }
        ctx.applyGateEffects(gate);
        int timer = ctx.strainTimer();
        if (timer > 0) {
            ctx.setStrainTimer(timer - 1);
            return;
        }
        ctx.addStrain(gate.strainGain(EightGatesTable.hasMastery(ctx.taijutsuLevel())));
        ctx.setStrainTimer(EightGatesTable.STRAIN_PULSE_TICKS);
    }

    @Override
    protected boolean onCost(EightGatesModeContext ctx) {
        return !EightGatesTable.forcesClose(ctx.strain()); // el coste es la fatiga, no el chakra
    }

    @Override
    protected void onDeactivate(EightGatesModeContext ctx) {
        stage = 0;
        if (safeClose) {
            safeClose = false;
            return; // safe_deactivate: sin coste de cierre ni veto
        }
        ctx.setCloseCooldown(EightGatesTable.CLOSE_COOLDOWN_TICKS);
        ctx.notifyClosed(ctx.strain());
    }

    /** Comprobaciones de {@code activate} y apertura efectiva de la puerta pedida. */
    private boolean tryOpen(EightGatesModeContext ctx, int target) {
        int taijutsu = ctx.taijutsuLevel();
        if (taijutsu < EightGatesTable.ACTIVATION_TAIJUTSU) {
            ctx.notifyBlocked(BlockReason.LOW_TAIJUTSU);
            return false;
        }
        if (ctx.closeCooldown() > 0) {
            ctx.notifyBlocked(BlockReason.CLOSE_COOLDOWN);
            return false;
        }
        if (ctx.openCooldown() > 0) {
            ctx.notifyBlocked(BlockReason.OPEN_COOLDOWN);
            return false;
        }
        int maxGate = EightGatesTable.maxGate(taijutsu);
        if (target > maxGate) {
            ctx.notifyBlocked(BlockReason.NO_MORE_GATES);
            return false;
        }
        Gate gate = Gate.forStage(target).orElse(null);
        if (gate == null) {
            ctx.notifyBlocked(BlockReason.NO_MORE_GATES);
            return false;
        }
        stage = target;
        boolean mastered = EightGatesTable.hasMastery(taijutsu);
        ctx.setOpenCooldown(EightGatesTable.openCooldownTicks(maxGate, mastered));
        ctx.setStrainTimer(EightGatesTable.firstStrainTickTicks(maxGate, mastered));
        ctx.notifyOpened(gate);
        return true;
    }
}
