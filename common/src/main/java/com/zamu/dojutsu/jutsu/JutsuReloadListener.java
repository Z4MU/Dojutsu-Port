package com.zamu.dojutsu.jutsu;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.zamu.dojutsu.DojutsuMod;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

/**
 * Carga los jutsus desde {@code data/dojutsu/jutsu/**.json} (un jutsu por archivo, organizados
 * en subcarpetas por naturaleza: {@code jutsu/fire/great_fireball.json}) al
 * {@link JutsuRegistry}. Se ejecuta al cargar el mundo y en {@code /reload}, en ambos loaders.
 *
 * <p><b>Validacion con aislamiento por archivo</b> (patron de DJT-052/056): un archivo con una
 * naturaleza desconocida, un campo invalido o cuyo <b>id no coincide con el nombre del
 * archivo</b> se descarta con un error en el log; el resto sigue cargando. Un id duplicado
 * (mismo nombre en dos subcarpetas) se avisa y gana el ultimo.
 */
public final class JutsuReloadListener extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    public JutsuReloadListener() {
        super(GSON, "jutsu");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, JutsuEntry> loaded = new TreeMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            parse(entry.getKey(), entry.getValue()).ifPresent(jutsu -> {
                JutsuEntry previous = loaded.put(jutsu.id(), jutsu);
                if (previous != null) {
                    LOGGER.warn("[{}] jutsu duplicado: '{}' redefinido por {}.",
                            DojutsuMod.MOD_NAME, jutsu.id(), entry.getKey());
                }
            });
        }
        JutsuRegistry.load(loaded);
        LOGGER.info("[{}] Jutsus cargados: {}.", DojutsuMod.MOD_NAME, loaded.size());
    }

    /**
     * Parseo de un archivo, puro y testeable: codec estricto + el id del JSON debe coincidir
     * con el nombre del archivo (sin la subcarpeta). Invalido → vacio, con error en el log.
     * El try/catch cubre las validaciones del constructor de dominio que lanzan <b>dentro</b>
     * del apply del codec (p. ej. id de solo espacios): un archivo roto jamas tumba el /reload.
     */
    static Optional<JutsuEntry> parse(ResourceLocation file, JsonElement json) {
        Optional<JutsuEntry> parsed;
        try {
            parsed = JutsuEntry.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> LOGGER.error("[{}] jutsu {} invalido (se ignora): {}",
                            DojutsuMod.MOD_NAME, file, error));
        } catch (RuntimeException e) {
            LOGGER.error("[{}] jutsu {} invalido (se ignora): {}", DojutsuMod.MOD_NAME, file, e.getMessage());
            return Optional.empty();
        }
        return parsed
                .filter(jutsu -> {
                    String fileName = fileBaseName(file);
                    if (!jutsu.id().equals(fileName)) {
                        LOGGER.error("[{}] jutsu {}: el id '{}' no coincide con el nombre del archivo '{}' (se ignora).",
                                DojutsuMod.MOD_NAME, file, jutsu.id(), fileName);
                        return false;
                    }
                    return true;
                });
    }

    /** Nombre base del archivo sin subcarpetas (el listener ya quita el prefijo y el {@code .json}). */
    static String fileBaseName(ResourceLocation file) {
        String path = file.getPath();
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }
}
