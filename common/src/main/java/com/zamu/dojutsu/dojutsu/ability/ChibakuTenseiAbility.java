package com.zamu.dojutsu.dojutsu.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Chibaku Tensei (Rinnegan): un núcleo de gravedad que <b>arrastra hacia arriba</b> a las entidades de la zona y
 * las daña — la técnica que forma satélites/meteoritos. Clona la plantilla de {@link AmaterasuAbility}; es un
 * efecto de gravedad <b>vertical</b>, distinto de la atracción horizontal de {@link BanshoTeninAbility}.
 * {@code [S05]}, {@code rinnegan/chibaku_tensei}.
 *
 * <p>Coste <b>600 chakra</b> (verificado: {@code chibaku_tensei/cast}; sin fatiga, como el resto del Rinnegan).
 * El datapack invoca un ancla a 20 bloques y arrastra las entidades hacia ella, dañándolas ({@code instant_damage}
 * amp 1); aquí se aplica {@link MobEffects#LEVITATION} + {@link MobEffects#HARM} en la zona. Ajuste → balance.
 */
public final class ChibakuTenseiAbility implements EyeAbility {

    private static final int CHAKRA_COST = 600;
    private static final int STRAIN_COST = 0; // el Rinnegan no añade eye_strain
    private static final double RADIUS = 7.0;
    private static final int LEVITATION_TICKS = 60; // arrastre hacia arriba
    private static final int LEVITATION_AMPLIFIER = 3;
    private static final int HARM_AMPLIFIER = 1; // instant_damage amp 1 (Harming II)

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
        Vec3 origin = caster.position();
        for (LivingEntity target : EyeAbilityEffects.livingAround(level, caster, origin, RADIUS)) {
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, LEVITATION_TICKS, LEVITATION_AMPLIFIER));
            target.addEffect(new MobEffectInstance(MobEffects.HARM, 1, HARM_AMPLIFIER)); // vanilla cura no-muertos
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, origin.x, origin.y + 3, origin.z, 60, RADIUS / 2, 2, RADIUS / 2, 0.1);
    }
}
