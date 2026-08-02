package com.zamu.dojutsu.bijuu.ai;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.bijuu.entity.TailedBeastEntity;
import com.zamu.dojutsu.domain.bijuu.BeastAiPlan;
import com.zamu.dojutsu.domain.bijuu.BeastStats;
import com.zamu.dojutsu.domain.bijuu.TailedBeast;
import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

/**
 * Goal de <b>tecnicas</b> de la Bestia con Cola {@code [S09]} (F7/DJT-161): el temporizador, la tirada y el
 * lanzado. Traduce el bloque {@code # ai timer} de {@code tailed_beasts/main} + {@code ai/get_move}.
 *
 * <h2>Solo pelea si hay alguien delante</h2>
 * El datapack condiciona <b>todo</b> el bloque de IA a {@code if entity @p[distance=0..32]}: sin nadie cerca, el
 * temporizador ni siquiera baja. Aqui es el {@link #canUse()} del goal, que es exactamente la misma idea
 * expresada donde corresponde — y ademas evita que una bestia en un chunk cargado y vacio gaste tick alguno.
 *
 * <h2>La decision es dominio puro</h2>
 * El goal no sabe que tecnica toca: eso lo decide {@link BeastAiPlan}, que se testea sin arrancar Minecraft.
 * Aqui solo queda lo que necesita el mundo real — quien esta cerca, donde mira la bestia y resolver el id contra
 * el {@link BeastMoveRegistry}.
 *
 * <p>Una tecnica <b>aun no implementada</b> simplemente no se lanza y la bestia pierde el turno, en vez de
 * romper el tick: mientras el catalogo se completa, las bestias pelean con lo que tengan.
 */
public class TailedBeastMoveGoal extends Goal {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final TailedBeastEntity beast;

    /** Ticks hasta la proxima decision ({@code d.tbai_timer}). */
    private int timer;

    public TailedBeastMoveGoal(TailedBeastEntity beast) {
        this.beast = beast;
        this.setFlags(EnumSet.noneOf(Flag.class)); // no bloquea moverse ni mirar: la bestia dispara andando
    }

    @Override
    public boolean canUse() {
        return nearestPlayer() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.timer = BeastStats.aiIntervalTicks(beast.beast());
    }

    @Override
    public void tick() {
        if (timer > 0) {
            timer--;
            return;
        }
        timer = BeastStats.aiIntervalTicks(beast.beast());
        castTurn();
    }

    /** Un turno de la bestia: tirada, eleccion y —si toca algo— lanzado. */
    private void castTurn() {
        TailedBeast identity = beast.beast();
        int roll = BeastAiPlan.ROLL_MIN + beast.getRandom().nextInt(BeastAiPlan.ROLL_OUTCOMES);
        Optional<String> chosen = BeastAiPlan.choose(identity, roll, targetInMelee(), beast.tenpenchiiReady());
        if (chosen.isEmpty()) {
            return; // la mitad de los turnos no hacen nada: es del datapack, no un fallo
        }
        BeastMoveRegistry.get(chosen.get()).ifPresentOrElse(
                move -> {
                    faceTarget();
                    move.execute(new BeastMoveContext(beast, identity));
                },
                () -> LOGGER.debug("[{}] {} quiso usar {}, aun sin implementar.",
                        DojutsuMod.MOD_NAME, identity.id(), chosen.get()));
    }

    /** Jugador vivo mas cercano dentro del radio que despierta la IA, o {@code null}. */
    private Player nearestPlayer() {
        if (!(beast.level() instanceof ServerLevel level)) {
            return null;
        }
        return level.getNearestPlayer(beast, BeastStats.AI_ACTIVATION_RADIUS);
    }

    /** Indica si hay alguien lo bastante encima como para justificar la Bola Bestia con Cola. */
    private boolean targetInMelee() {
        Player nearest = nearestPlayer();
        return nearest != null && nearest.distanceToSqr(beast) <= BeastAiPlan.MELEE_RANGE * BeastAiPlan.MELEE_RANGE;
    }

    /** Encara al objetivo antes de disparar: las tecnicas salen hacia donde mira. */
    private void faceTarget() {
        LivingEntity target = beast.getTarget() != null ? beast.getTarget() : nearestPlayer();
        if (target != null) {
            beast.getLookControl().setLookAt(target, 30F, 30F);
            beast.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        }
    }
}
