package com.zamu.dojutsu.clans;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.zamu.dojutsu.DojutsuMod;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

/**
 * Carga las definiciones de clan desde {@code data/dojutsu/clans/*.json} (un clan por archivo) al
 * {@link ClanRegistry}. Se ejecuta al cargar el mundo y en {@code /reload}, en ambos loaders.
 *
 * <p><b>Validacion:</b> un archivo con una naturaleza/KG/habilidad desconocida <b>se descarta con error en el
 * log</b> (codec estricto de {@code EnumCodecs}); el resto sigue cargando. El {@code id} del JSON debe
 * coincidir con el nombre del archivo (evita confusiones al sobrescribir por datapack).
 */
public final class ClanReloadListener extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    public ClanReloadListener() {
        super(GSON, "clans");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, ClanDefinition> loaded = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            ClanDefinition.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(error -> LOGGER.error("[{}] clan {} invalido (se ignora): {}",
                            DojutsuMod.MOD_NAME, entry.getKey(), error))
                    .ifPresent(def -> {
                        if (!def.id().equals(entry.getKey().getPath())) {
                            LOGGER.error("[{}] clan {}: el id '{}' no coincide con el archivo (se ignora).",
                                    DojutsuMod.MOD_NAME, entry.getKey(), def.id());
                            return;
                        }
                        loaded.put(def.id(), def);
                    });
        }
        ClanRegistry.load(loaded);
        LOGGER.info("[{}] Clanes cargados: {}.", DojutsuMod.MOD_NAME, loaded.size());
        if (loaded.isEmpty()) {
            LOGGER.warn("[{}] Ningun clan cargado: el onboarding no tendra opciones.", DojutsuMod.MOD_NAME);
        }
    }
}
