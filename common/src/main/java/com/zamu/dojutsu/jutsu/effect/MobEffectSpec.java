package com.zamu.dojutsu.jutsu.effect;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

/**
 * Efecto de pocion parametrizado desde JSON (F4/DJT-076), compartido por las familias buff/area/
 * heal: {@code {"id":"minecraft:slow_falling","seconds":6,"amplifier":8}} — los mismos numeros
 * que los {@code effect give} del datapack. Un id desconocido se descarta con aviso (contenido
 * roto no crashea el cast).
 *
 * @param id        id del efecto vanilla
 * @param seconds   duracion en segundos (como el comando /effect)
 * @param amplifier nivel-1 (amplifier 0 = nivel I)
 */
public record MobEffectSpec(String id, int seconds, int amplifier) {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Parsea la lista {@code "effects": [...]} de unos effectParams (vacia si no esta). */
    public static List<MobEffectSpec> parseList(JsonObject params, String field) {
        List<MobEffectSpec> specs = new ArrayList<>();
        JsonArray array = GsonHelper.getAsJsonArray(params, field, new JsonArray());
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            specs.add(new MobEffectSpec(
                    GsonHelper.getAsString(obj, "id"),
                    GsonHelper.getAsInt(obj, "seconds", 1),
                    GsonHelper.getAsInt(obj, "amplifier", 0)));
        }
        return List.copyOf(specs);
    }

    /** Aplica el efecto al objetivo (ambient=true, como el {@code true} de los effect give del datapack). */
    public void applyTo(LivingEntity target) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        Holder<MobEffect> effect = rl == null ? null
                : BuiltInRegistries.MOB_EFFECT.getHolder(rl).orElse(null);
        if (effect == null) {
            LOGGER.warn("[{}] Efecto de pocion desconocido en effectParams: {} (se ignora).",
                    DojutsuMod.MOD_NAME, id);
            return;
        }
        target.addEffect(new MobEffectInstance(effect, seconds * 20, amplifier, true, true));
    }
}
