package com.zamu.dojutsu.progression;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.zamu.dojutsu.DojutsuMod;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

/**
 * Carga las definiciones de desbloqueo desde {@code data/dojutsu/unlocks/*.json} (cada archivo, un array de
 * hitos) al {@link UnlockRegistry}. Se ejecuta al cargar el mundo y en {@code /reload}, en ambos loaders.
 */
public final class UnlockReloadListener extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    public UnlockReloadListener() {
        super(GSON, "unlocks");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<UnlockDefinition> loaded = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            UnlockDefinition.LIST_CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(error -> LOGGER.error("[{}] unlocks {} invalido: {}",
                            DojutsuMod.MOD_NAME, entry.getKey(), error))
                    .ifPresent(loaded::addAll);
        }
        UnlockRegistry.load(loaded);
        LOGGER.info("[{}] Desbloqueos cargados: {} hito(s).", DojutsuMod.MOD_NAME, loaded.size());
    }
}
