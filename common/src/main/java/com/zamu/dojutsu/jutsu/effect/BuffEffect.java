package com.zamu.dojutsu.jutsu.effect;

import com.google.gson.JsonObject;
import com.zamu.dojutsu.jutsu.JutsuEntry;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Familia BUFF (F4/DJT-076): efectos de pocion temporales sobre el <b>caster</b>. Piloto:
 * {@code fireproof} (fire_resistance 1s — literal del datapack: {@code effect give @s
 * fire_resistance 1 0 true}).
 *
 * <p>Parametros: {@code effects} — lista de {@link MobEffectSpec}.
 */
public final class BuffEffect implements JutsuEffect {

    /** Parametros de la familia (puro y testeable). */
    public record Params(List<MobEffectSpec> effects) {

        public static Params parse(JsonObject json) {
            return new Params(MobEffectSpec.parseList(json, "effects"));
        }
    }

    @Override
    public void apply(ServerLevel level, LivingEntity caster, JutsuEntry jutsu) {
        for (MobEffectSpec spec : Params.parse(jutsu.effectParams().getAsJsonObject()).effects()) {
            spec.applyTo(caster);
        }
    }
}
