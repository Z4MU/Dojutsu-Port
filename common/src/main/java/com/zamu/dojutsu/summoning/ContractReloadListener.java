package com.zamu.dojutsu.summoning;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.domain.summoning.SummonContractDef;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

/**
 * Carga los contratos de invocacion desde {@code data/dojutsu/contracts/*.json} (uno por especie) al
 * {@link ContractRegistry}. Se ejecuta al cargar el mundo y en {@code /reload}, en ambos loaders.
 *
 * <p><b>Validacion:</b> un archivo con un tier desconocido se descarta con error en el log y el resto sigue
 * cargando; el {@code id} debe coincidir con el nombre del archivo (mismo criterio que clanes y ojos).
 */
public final class ContractReloadListener extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    public ContractReloadListener() {
        super(GSON, "contracts");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager,
            ProfilerFiller profiler) {
        Map<String, SummonContractDef> loaded = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            SummonContractDef.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(error -> LOGGER.error("[{}] contrato {} invalido (se ignora): {}",
                            DojutsuMod.MOD_NAME, entry.getKey(), error))
                    .ifPresent(def -> {
                        if (!def.id().equals(entry.getKey().getPath())) {
                            LOGGER.error("[{}] contrato {}: el id '{}' no coincide con el archivo (se ignora).",
                                    DojutsuMod.MOD_NAME, entry.getKey(), def.id());
                            return;
                        }
                        loaded.put(def.id(), def);
                    });
        }
        ContractRegistry.load(loaded);
        LOGGER.info("[{}] Contratos de invocacion cargados: {}.", DojutsuMod.MOD_NAME, loaded.size());
    }
}
