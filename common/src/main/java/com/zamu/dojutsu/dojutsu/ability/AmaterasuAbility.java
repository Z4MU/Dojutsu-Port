package com.zamu.dojutsu.dojutsu.ability;

import com.zamu.dojutsu.entity.AbilityAnchor;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Amaterasu (Mangekyo Sharingan): habilidad piloto de {@code EP-EYE-ABILITIES} (DJT-118), plantilla del resto
 * del catalogo. Fuego negro <b>anclado</b> donde apunta el lanzador: coloca un {@link AbilityAnchor} (nunca un
 * armor stand — RA-3) que incendia y dana a las entidades cercanas durante su vida. {@code [S05]}.
 *
 * <p>Valores del datapack ({@code sharingan/mangekyo/sasuke/amaterasu}): coste <b>40 chakra</b> y <b>+10 eye
 * strain</b> (exento con el ojo eterno — lo aplica el pipeline). El dano por tick se acota solo con las
 * <b>invuln-frames</b> vanilla de {@code hurt} (~cada 10 ticks), sin necesidad de un temporizador propio. El
 * ajuste fino del dano/duracion queda para el balance (F10); aqui basta con reproducir la mecanica anclada.
 */
public final class AmaterasuAbility implements EyeAbility {

    private static final int CHAKRA_COST = 40;
    private static final int STRAIN_COST = 10;
    private static final int LIFETIME_TICKS = 200;   // 10 s de fuego negro anclado
    private static final double AIM_RANGE = 20.0;    // alcance del apuntado (anchored eyes del datapack)
    private static final double BURN_RADIUS = 3.0;   // radio de quemado (distance 0..3 del datapack)
    private static final int FIRE_TICKS = 100;       // fuego renovado cada tick mientras esta cerca (5 s)
    private static final float DAMAGE = 2.0f;

    /** Nucleo de la llama: {@code dust 0.05 0.05 0.05 0.9} del datapack. */
    private static final DustParticleOptions CORE =
            new DustParticleOptions(new Vector3f(0.05F, 0.05F, 0.05F), 0.9F);

    /** Halo superior, mas fino y con un punto de azul: {@code dust 0.02 0.03 0.03 0.5}. */
    private static final DustParticleOptions HALO =
            new DustParticleOptions(new Vector3f(0.02F, 0.03F, 0.03F), 0.5F);

    @Override
    public int chakraCost() {
        return CHAKRA_COST;
    }

    @Override
    public int strainCost() {
        return STRAIN_COST;
    }

    @Override
    public void execute(EyeAbilityContext context) {
        ServerPlayer caster = context.caster();
        ServerLevel level = context.level();
        Vec3 target = caster.pick(AIM_RANGE, 1.0f, false).getLocation(); // a donde mira (raycast de bloque)
        AbilityAnchor anchor = AbilityAnchor.create(level, caster, LIFETIME_TICKS)
                .at(target)
                .withTickCallback(a -> burnNearby(a, caster));
        level.addFreshEntity(anchor);
    }

    /** Incendia y dana a las entidades cercanas al ancla cada tick (el dueno queda exento). */
    private static void burnNearby(AbilityAnchor anchor, ServerPlayer owner) {
        if (!(anchor.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 pos = anchor.position();
        blackFlames(level, pos);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                anchor.getBoundingBox().inflate(BURN_RADIUS),
                e -> e != owner && e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand))) {
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), FIRE_TICKS));
            // hurt lo acotan las invuln-frames vanilla: un impacto efectivo ~cada 10 ticks.
            target.hurt(level.damageSources().indirectMagic(owner, owner), DAMAGE);
        }
    }

    /**
     * <b>El fuego del Amaterasu es negro</b> (F10, arreglo de la verificacion en partida). Se dibujaba con
     * {@code ParticleTypes.FLAME}, que es naranja: la llama mas famosa de la serie salia del color equivocado.
     *
     * <p>El datapack no usa ninguna particula de fuego — usa {@code dust} con RGB casi cero, en dos capas
     * ({@code sasuke/hit_particles}): una densa y grande a ras de suelo
     * ({@code dust 0.05 0.05 0.05 0.9}) y otra mas fina y azulada por encima
     * ({@code dust 0.02 0.03 0.03 0.5}). El negro puro no se ve contra un bloque oscuro; ese casi-negro con un
     * punto de azul si, y es lo que le da el aspecto de llama.
     */
    private static void blackFlames(ServerLevel level, Vec3 pos) {
        level.sendParticles(CORE, pos.x, pos.y + 0.5, pos.z, 6, 0.5, 0.5, 0.5, 0.0);
        level.sendParticles(HALO, pos.x, pos.y + 1.0, pos.z, 4, 0.5, 0.6, 0.5, 0.0);
    }
}
