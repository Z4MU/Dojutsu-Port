package com.zamu.dojutsu.natures;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

/**
 * Carga las definiciones de Kekkei Genkai desde {@code data/dojutsu/kekkei_genkai/*.json} (un KG por archivo)
 * al {@link KekkeiGenkaiRegistry}. Se ejecuta al cargar el mundo y en {@code /reload}, en ambos loaders.
 *
 * <p><b>Validacion de referencias:</b> un archivo con una naturaleza o un KG desconocidos <b>se descarta con un
 * error en el log</b> (el codec falla) en vez de cargarse a medias; el resto de archivos siguen cargando. Si
 * falta algun KG del enum tras la carga, se avisa: seria un KG imposible de otorgar.
 */
public final class KekkeiGenkaiReloadListener extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    public KekkeiGenkaiReloadListener() {
        super(GSON, "kekkei_genkai");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        EnumMap<KekkeiGenkai, KekkeiGenkaiDefinition> loaded = new EnumMap<>(KekkeiGenkai.class);
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            KekkeiGenkaiDefinition.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(error -> LOGGER.error("[{}] kekkei_genkai {} invalido (se ignora): {}",
                            DojutsuMod.MOD_NAME, entry.getKey(), error))
                    .ifPresent(def -> {
                        KekkeiGenkaiDefinition previous = loaded.put(def.kekkeiGenkai(), def);
                        if (previous != null) {
                            LOGGER.warn("[{}] kekkei_genkai duplicado: {} redefinido por {}.",
                                    DojutsuMod.MOD_NAME, def.kekkeiGenkai(), entry.getKey());
                        }
                    });
        }
        KekkeiGenkaiRegistry.load(loaded);
        LOGGER.info("[{}] Kekkei Genkai cargados: {}/{}.",
                DojutsuMod.MOD_NAME, loaded.size(), KekkeiGenkai.values().length);
        for (KekkeiGenkai kg : KekkeiGenkai.values()) {
            if (!loaded.containsKey(kg)) {
                LOGGER.warn("[{}] Kekkei Genkai sin definicion: {} (no se podra otorgar).", DojutsuMod.MOD_NAME, kg);
            }
        }
    }
}
