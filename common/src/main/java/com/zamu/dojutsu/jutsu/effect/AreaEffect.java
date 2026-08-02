package com.zamu.dojutsu.jutsu.effect;

import com.google.gson.JsonObject;
import com.zamu.dojutsu.jutsu.JutsuEntry;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;

/**
 * Familia AREA (F4/DJT-076): actua sobre todas las entidades vivas alrededor del caster.
 * Piloto: {@code vacuum_wave} (radio 10; cada objetivo es lanzado 8 bloques hacia arriba y
 * recibe slow_falling 6s nivel 8 — el "dano" del datapack era la caida).
 *
 * <p>Parametros: {@code radius} (def. 5) · {@code launchY} (teleport vertical, def. 0) ·
 * {@code damage} (dano directo, def. 0) · {@code effects} (lista de {@link MobEffectSpec}).
 * El caster nunca se incluye. Requiere al menos un objetivo ({@code canApply} — el
 * "Nothing nearby" del datapack se valida antes de cobrar).
 */
public final class AreaEffect implements JutsuEffect {

    /** Parametros de la familia (puro y testeable). */
    public record Params(double radius, double launchY, float damage, List<MobEffectSpec> effects) {

        public static Params parse(JsonObject json) {
            return new Params(
                    GsonHelper.getAsDouble(json, "radius", 5.0D),
                    GsonHelper.getAsDouble(json, "launchY", 0.0D),
                    GsonHelper.getAsFloat(json, "damage", 0.0F),
                    MobEffectSpec.parseList(json, "effects"));
        }
    }

    @Override
    public boolean canApply(ServerLevel level, LivingEntity caster, JutsuEntry jutsu) {
        Params params = Params.parse(jutsu.effectParams().getAsJsonObject());
        return !targets(level, caster, params).isEmpty();
    }

    @Override
    public void apply(ServerLevel level, LivingEntity caster, JutsuEntry jutsu) {
        Params params = Params.parse(jutsu.effectParams().getAsJsonObject());
        for (LivingEntity target : targets(level, caster, params)) {
            if (params.launchY() > 0) {
                target.teleportTo(target.getX(), target.getY() + params.launchY(), target.getZ());
            }
            if (params.damage() > 0) {
                target.hurt(level.damageSources().indirectMagic(caster, caster), params.damage());
            }
            for (MobEffectSpec spec : params.effects()) {
                spec.applyTo(target);
            }
        }
    }

    private static List<LivingEntity> targets(ServerLevel level, LivingEntity caster, Params params) {
        return level.getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(params.radius()),
                // ArmorStand ES LivingEntity: el datapack los excluia via #dojutsu:ignore.
                e -> e != caster && e.isAlive() && !e.isSpectator()
                        && !(e instanceof net.minecraft.world.entity.decoration.ArmorStand));
    }
}
