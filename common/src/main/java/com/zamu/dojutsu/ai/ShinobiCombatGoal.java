package com.zamu.dojutsu.ai;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.core.DomainEvents;
import com.zamu.dojutsu.domain.ai.ShinobiCombatPlan;
import com.zamu.dojutsu.domain.ai.ThreatLevel;
import com.zamu.dojutsu.domain.throwing.ThrowableType;
import com.zamu.dojutsu.entity.NinjaProjectile;
import com.zamu.dojutsu.jutsu.JutsuCastPipeline;
import com.zamu.dojutsu.jutsu.effect.JutsuEffectRegistry;
import com.zamu.dojutsu.jutsu.JutsuRegistry;
import java.util.EnumSet;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

/**
 * Goal de combate del shinobi NPC {@code [S17]} (F8/DJT-185): el temporizador, la tirada y la accion.
 *
 * <h2>Un NPC casteando jutsus, sin tocar el pipeline</h2>
 * Esto es <b>S17-R3</b> cerrado. El pipeline de casteo de F4 estaba preparado de antemano en <b>dos</b> sitios y
 * ninguno hubo que cambiarlo:
 * <ul>
 *   <li>{@code CastContext} declaraba en su javadoc <i>"No asume jugador (S17-R3)"</i> → {@link ShinobiCastContext}.</li>
 *   <li>{@code JutsuEffect.apply} ya recibia un {@code LivingEntity}, no un {@code ServerPlayer}.</li>
 * </ul>
 * El shinobi entra por el <b>nucleo generico</b> de {@code JutsuCastPipeline.cast(UUID, ...)}, el mismo que usa el
 * jugador tras su sobrecarga de conveniencia, asi que pasa por <b>las mismas puertas</b>: naturaleza, afinidad,
 * Kekkei Genkai y coste. Un shinobi no puede saltarse un requisito que un jugador no puede saltarse.
 *
 * <p>La decision es dominio puro ({@link ShinobiCombatPlan}); aqui solo queda lo que necesita el mundo: quien
 * esta cerca, a que distancia y como se ejecuta cada accion.
 */
public class ShinobiCombatGoal extends Goal {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final ShinobiEntity shinobi;

    /** Ticks hasta la proxima decision ({@code d.ai_timer}). */
    private int timer;

    public ShinobiCombatGoal(ShinobiEntity shinobi) {
        this.shinobi = shinobi;
        this.setFlags(EnumSet.noneOf(Flag.class)); // no bloquea moverse ni mirar: decide mientras persigue
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
        this.timer = ShinobiCombatPlan.DECISION_INTERVAL_TICKS;
    }

    @Override
    public void tick() {
        if (timer > 0) {
            timer--;
            return;
        }
        timer = ShinobiCombatPlan.DECISION_INTERVAL_TICKS;
        act();
    }

    /** Un turno: tirada, decision y ejecucion. */
    private void act() {
        Player target = nearestPlayer();
        if (target == null) {
            return;
        }
        ThreatLevel level = shinobi.threat();
        int roll = ShinobiCombatPlan.ROLL_MIN + shinobi.getRandom().nextInt(ShinobiCombatPlan.ROLL_OUTCOMES);
        ShinobiCombatPlan.Action action = ShinobiCombatPlan.choose(level, roll, shinobi.distanceTo(target));
        switch (action) {
            case IDLE -> { } // la mitad de los turnos: es del datapack, no un fallo
            case MELEE -> shinobi.setTarget(target); // el goal de melee vanilla hace el resto
            case THROW_KUNAI -> throwKunai(target);
            case WEAK_JUTSU, STRONG_JUTSU -> castJutsu(action, level, target);
        }
    }

    /** Lanza un kunai al objetivo, con el mismo {@code NinjaProjectile} que usa el jugador (F8/DJT-180). */
    private void throwKunai(Player target) {
        if (!(shinobi.level() instanceof ServerLevel level)) {
            return;
        }
        face(target);
        NinjaProjectile kunai = NinjaProjectile.create(level, shinobi)
                .withDisplayItem(new ItemStack(Items.IRON_NUGGET))
                .withImpactCallback((thrown, hit) -> hurtNearby(level, thrown));
        level.addFreshEntity(kunai);
        kunai.launch(shinobi.getLookAngle(), ThrowableType.KUNAI.speed());
        level.playSound(null, shinobi.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.HOSTILE, 0.5f, 1.2f);
    }

    /** Daño del kunai al impactar (el mismo amplificador que el del jugador). */
    private void hurtNearby(ServerLevel level, NinjaProjectile projectile) {
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                projectile.getBoundingBox().inflate(com.zamu.dojutsu.domain.throwing.ThrowingTable.HIT_RADIUS))) {
            if (victim != shinobi) {
                victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.HARM, 1,
                        com.zamu.dojutsu.domain.throwing.ThrowingTable.THROWN_DAMAGE_AMPLIFIER));
            }
        }
    }

    /**
     * Lanza un jutsu por el <b>nucleo generico</b> del pipeline: las mismas puertas que un jugador, sin
     * atajos.
     */
    private void castJutsu(ShinobiCombatPlan.Action action, ThreatLevel level, Player target) {
        String jutsuId = ShinobiCombatPlan.jutsuFor(action, level).orElse(null);
        if (jutsuId == null || !(shinobi.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        var entry = JutsuRegistry.get(jutsuId).orElse(null);
        if (entry == null) {
            LOGGER.debug("[{}] el shinobi {} declara el jutsu {}, que no esta en el catalogo.",
                    DojutsuMod.MOD_NAME, level.id(), jutsuId);
            return;
        }
        face(target);
        var effect = JutsuEffectRegistry.get(entry.definition().effectId()).orElse(null);
        Runnable effectRunner = effect == null ? null : () -> effect.apply(serverLevel, shinobi, entry);
        java.util.function.BooleanSupplier applicable = effect == null ? () -> true
                : () -> effect.canApply(serverLevel, shinobi, entry);
        JutsuCastPipeline.cast(shinobi.getUUID(), entry, new ShinobiCastContext(level),
                amount -> true, // el chakra del NPC no se persiste: assign_threat le da 10000 y ahi se queda
                effectRunner, applicable, () -> { }, DomainEvents.bus());
    }

    /** Jugador vivo mas cercano dentro del radio que despierta la IA, o {@code null}. */
    private Player nearestPlayer() {
        if (!(shinobi.level() instanceof ServerLevel level)) {
            return null;
        }
        return level.getNearestPlayer(shinobi, ShinobiCombatPlan.ACTIVATION_RADIUS);
    }

    /** Encara al objetivo: los jutsus y los kunai salen hacia donde mira. */
    private void face(Player target) {
        shinobi.getLookControl().setLookAt(target, 30F, 30F);
        shinobi.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
    }
}
