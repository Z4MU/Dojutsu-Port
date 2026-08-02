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
 * Carga las secuencias de aprendizaje desde {@code data/dojutsu/learner_sequences/*.json} (una
 * por elemento, el nombre del archivo debe coincidir con el {@code element}) al
 * {@link LearnSequenceRegistry}. Se ejecuta al cargar el mundo y en {@code /reload}.
 *
 * <p>Mismo aislamiento por archivo que el resto de contenido (DJT-052/056/073): un archivo
 * invalido se descarta con error en el log y el resto sigue cargando.
 */
public final class LearnSequenceReloadListener extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    public LearnSequenceReloadListener() {
        super(GSON, "learner_sequences");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, LearnSequence> loaded = new TreeMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            parse(entry.getKey(), entry.getValue()).ifPresent(sequence -> {
                LearnSequence previous = loaded.put(sequence.element(), sequence);
                if (previous != null) {
                    LOGGER.warn("[{}] secuencia duplicada: '{}' redefinida por {}.",
                            DojutsuMod.MOD_NAME, sequence.element(), entry.getKey());
                }
            });
        }
        LearnSequenceRegistry.load(loaded);
        LOGGER.info("[{}] Secuencias de aprendizaje cargadas: {}.", DojutsuMod.MOD_NAME, loaded.size());
    }

    /** Parseo de un archivo, puro y testeable: codec estricto + element == nombre del archivo. */
    static Optional<LearnSequence> parse(ResourceLocation file, JsonElement json) {
        Optional<LearnSequence> parsed;
        try {
            parsed = LearnSequence.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> LOGGER.error("[{}] secuencia {} invalida (se ignora): {}",
                            DojutsuMod.MOD_NAME, file, error));
        } catch (RuntimeException e) {
            LOGGER.error("[{}] secuencia {} invalida (se ignora): {}", DojutsuMod.MOD_NAME, file, e.getMessage());
            return Optional.empty();
        }
        return parsed.filter(sequence -> {
            if (!sequence.element().equals(file.getPath())) {
                LOGGER.error("[{}] secuencia {}: el elemento '{}' no coincide con el nombre del archivo (se ignora).",
                        DojutsuMod.MOD_NAME, file, sequence.element());
                return false;
            }
            return true;
        });
    }
}
