package com.zamu.dojutsu.jutsu.effect;

import com.google.gson.JsonObject;
import com.zamu.dojutsu.jutsu.JutsuEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Familia TELEPORT (F4/DJT-076). Piloto: {@code substitution} — poof de particulas,
 * invisibilidad 6s y reubicacion aleatoria a 5..10 bloques ({@code spreadplayers ~ ~ 5 10}).
 * El "tronco senuelo" del original (armor stand con oak_log) es cosmetico y queda para el
 * porte de assets (anotado).
 *
 * <p>Parametros: {@code minRange} (def. 5) · {@code maxRange} (def. 10) ·
 * {@code invisibilitySeconds} (def. 0). El destino se busca sobre la superficie (heightmap,
 * como hace spreadplayers); si tras varios intentos no hay destino valido, no se teletransporta
 * (pero el cast ya ocurrio, igual que spreadplayers podia dejarte cerca).
 */
public final class TeleportEffect implements JutsuEffect {

    private static final int ATTEMPTS = 10;

    /** Parametros de la familia (puro y testeable). */
    public record Params(double minRange, double maxRange, int invisibilitySeconds) {

        public static Params parse(JsonObject json) {
            return new Params(
                    GsonHelper.getAsDouble(json, "minRange", 5.0D),
                    GsonHelper.getAsDouble(json, "maxRange", 10.0D),
                    GsonHelper.getAsInt(json, "invisibilitySeconds", 0));
        }
    }

    @Override
    public void apply(ServerLevel level, LivingEntity caster, JutsuEntry jutsu) {
        Params params = Params.parse(jutsu.effectParams().getAsJsonObject());
        // Poof en el origen (substitution/summon: particle poof).
        level.sendParticles(ParticleTypes.POOF,
                caster.getX(), caster.getY() + 1.5D, caster.getZ(), 80, 0.8D, 1.0D, 0.8D, 0.0D);
        if (params.invisibilitySeconds() > 0) {
            caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,
                    params.invisibilitySeconds() * 20, 0, true, true));
        }
        for (int i = 0; i < ATTEMPTS; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double distance = params.minRange()
                    + level.random.nextDouble() * Math.max(0, params.maxRange() - params.minRange());
            int x = (int) Math.round(caster.getX() + Math.cos(angle) * distance);
            int z = (int) Math.round(caster.getZ() + Math.sin(angle) * distance);
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(x, 0, z));
            if (level.getBlockState(surface).isAir() && level.getBlockState(surface.above()).isAir()) {
                caster.teleportTo(x + 0.5D, surface.getY(), z + 0.5D);
                return;
            }
        }
        // Sin destino valido tras N intentos: se queda (spreadplayers tambien podia fallar cerca).
    }
}
