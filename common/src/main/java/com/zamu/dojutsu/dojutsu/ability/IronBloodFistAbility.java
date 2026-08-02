package com.zamu.dojutsu.dojutsu.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Iron Blood Fist (Ketsuryugan): endurece la sangre en un <b>auto-buff</b> de fuerza. Fiel a
 * {@code ketsuryugan/iron_blood_fist/main} ({@code effect give @s strength 1 amp 1} mientras dura). Es la
 * habilidad de <b>buff</b> del lote (frente al empuje/atracción y el daño). Clona la plantilla de DJT-118.
 *
 * <p>Coste <b>80 chakra / +4 eye strain</b> (verificado: {@code iron_blood_fist/cost}). El datapack mantiene el
 * buff con un temporizador ({@code ibf_usetime}); aquí se aplica una duración fija (pilotable, ajuste → F10).
 */
public final class IronBloodFistAbility implements EyeAbility {

    private static final int CHAKRA_COST = 80;
    private static final int STRAIN_COST = 4;
    private static final int DURATION_TICKS = 200; // 10 s (el datapack usa un timer; duración fija de piloto)
    private static final int STRENGTH_AMPLIFIER = 1; // strength amp 1 (Fuerza II)

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
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, DURATION_TICKS, STRENGTH_AMPLIFIER,
                false, true)); // strength II visible
    }
}
