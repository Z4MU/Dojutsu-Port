package com.zamu.dojutsu.natures;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.zamu.dojutsu.DojutsuMod;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

/**
 * Carga la tabla de asignacion aleatoria desde {@code data/dojutsu/rng/nature_roll.json} al
 * {@link NatureRollRegistry}. Se ejecuta al cargar el mundo y en {@code /reload}, en ambos loaders.
 *
 * <p><b>Valida la tabla</b> ({@link NatureRollTable#validate()}) antes de aceptarla: unos tramos solapados o
 * fuera de rango alterarian las probabilidades canonicas <b>en silencio</b> ({@code [S03-R4]}). Si no cuadra, se
 * rechaza con un error en el log y el registro queda vacio (no se otorga nada) en vez de repartir premios con
 * una distribucion equivocada.
 */
public final class NatureRollReloadListener extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    /** Id del unico archivo que se espera en la carpeta: {@code dojutsu:nature_roll}. */
    private static final ResourceLocation TABLE_ID = DojutsuMod.id("nature_roll");

    public NatureRollReloadListener() {
        super(GSON, "rng");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        JsonElement json = files.get(TABLE_ID);
        if (json == null) {
            LOGGER.error("[{}] Falta {}: la asignacion aleatoria quedara inactiva.", DojutsuMod.MOD_NAME, TABLE_ID);
            NatureRollRegistry.load(NatureRollTable.EMPTY);
            return;
        }
        NatureRollTable parsed = NatureRollTable.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> LOGGER.error("[{}] {} invalido: {}", DojutsuMod.MOD_NAME, TABLE_ID, error))
                .orElse(NatureRollTable.EMPTY);
        Optional<String> problem = parsed.validate();
        if (problem.isPresent()) {
            // Sin tabla no se otorga nada; con una tabla incoherente se repartirian premios con probabilidades
            // equivocadas y en silencio ([S03-R4]). Se prefiere lo primero.
            LOGGER.error("[{}] {} incoherente ({}): se ignora la tabla.",
                    DojutsuMod.MOD_NAME, TABLE_ID, problem.get());
            NatureRollRegistry.load(NatureRollTable.EMPTY);
            return;
        }
        NatureRollRegistry.load(parsed);
        LOGGER.info("[{}] Tabla de asignacion aleatoria cargada: rango 1..{}, {} tramo(s).",
                DojutsuMod.MOD_NAME, parsed.range(), parsed.entries().size());
    }
}
