package com.zamu.dojutsu.dojutsu;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.domain.eye.EyeEvolution;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

/**
 * Carga las definiciones de ojo desde {@code data/dojutsu/eyes/*.json} (un ojo por archivo) al
 * {@link EyeRegistry}. Se ejecuta al cargar el mundo y en {@code /reload}, en ambos loaders {@code [S05]}.
 *
 * <p><b>Validacion con aislamiento por archivo</b> (mismo patron que {@code JutsuReloadListener}/clanes): un
 * archivo con una familia desconocida, un requisito invalido o cuyo <b>id no coincide con el nombre del
 * archivo</b> se descarta con un error en el log; el resto sigue cargando (un ojo roto jamas tumba el
 * {@code /reload}). Tras cargar, se avisa de las <b>evoluciones colgantes</b> (destino inexistente) sin
 * descartar el ojo: el contenido es incremental (un ojo puede referenciar a otro aun no volcado).
 */
public final class EyeReloadListener extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    public EyeReloadListener() {
        super(GSON, "eyes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, EyeDefinition> loaded = new TreeMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            parse(entry.getKey(), entry.getValue()).ifPresent(eye -> loaded.put(eye.id(), eye));
        }
        warnDanglingTargets(loaded);
        EyeRegistry.load(loaded);
        LOGGER.info("[{}] Ojos cargados: {}.", DojutsuMod.MOD_NAME, loaded.size());
    }

    /**
     * Parseo de un archivo, puro y testeable: codec estricto + el id del JSON debe coincidir con el nombre del
     * archivo. Invalido → vacio, con error en el log. El try/catch cubre las validaciones del constructor que
     * lanzan <b>dentro</b> del apply del codec (id vacio, familia nula, requisito mal formado).
     */
    static Optional<EyeDefinition> parse(ResourceLocation file, JsonElement json) {
        Optional<EyeDefinition> parsed;
        try {
            parsed = EyeDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> LOGGER.error("[{}] ojo {} invalido (se ignora): {}",
                            DojutsuMod.MOD_NAME, file, error));
        } catch (RuntimeException e) {
            LOGGER.error("[{}] ojo {} invalido (se ignora): {}", DojutsuMod.MOD_NAME, file, e.getMessage());
            return Optional.empty();
        }
        return parsed.filter(eye -> {
            if (!eye.id().equals(file.getPath())) {
                LOGGER.error("[{}] ojo {}: el id '{}' no coincide con el archivo (se ignora).",
                        DojutsuMod.MOD_NAME, file, eye.id());
                return false;
            }
            return true;
        });
    }

    /** Avisa (sin descartar) de las evoluciones que apuntan a un ojo que no esta cargado. */
    private static void warnDanglingTargets(Map<String, EyeDefinition> loaded) {
        for (EyeDefinition eye : loaded.values()) {
            for (EyeEvolution evolution : eye.evolutions()) {
                if (!loaded.containsKey(evolution.to())) {
                    LOGGER.warn("[{}] ojo '{}': la evolucion apunta a '{}', que no existe (aun sin volcar).",
                            DojutsuMod.MOD_NAME, eye.id(), evolution.to());
                }
            }
        }
    }
}
