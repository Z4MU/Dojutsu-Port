package com.zamu.dojutsu.core;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

/**
 * Reload listener del datapack del mod (datos de servidor). Se ejecuta al cargar el
 * mundo y en cada {@code /reload}. En fases posteriores (F4/F5) cargara aqui las
 * definiciones data-driven (jutsus, clanes, ojos, curvas...); por ahora solo registra
 * un log para validar que el hook funciona en ambos loaders.
 */
public final class DataReloadListener extends SimplePreparableReloadListener<Void> {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        // Aun no hay recursos que preparar fuera del hilo principal.
        return null;
    }

    @Override
    protected void apply(Void prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOGGER.info("[{}] Datapack del mod recargado (sin definiciones aun).", DojutsuMod.MOD_NAME);
    }
}
