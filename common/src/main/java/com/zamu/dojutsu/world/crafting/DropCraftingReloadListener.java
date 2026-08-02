package com.zamu.dojutsu.world.crafting;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.domain.crafting.DropRecipe;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

/**
 * Carga las combinaciones de drop-crafting desde {@code data/dojutsu/drop_crafting/*.json} al
 * {@link DropCraftingRegistry}.
 *
 * <h2>Declara tambien lo que aun no tiene item, y lo dice</h2>
 * Igual que el catalogo de comercio: las <b>9 combinaciones de huevos de bestia con cola</b> se declaran aunque
 * el item no exista todavia (F7 los dio por comando de debug, F8 no los creo). Al cargar se cuentan y
 * <b>se avisa en el log</b>; esas recetas no emparejan, y el dia que el item se registre <b>funcionan solas</b>.
 *
 * <p>Es la alternativa honesta a recortar el recetario —y perder la referencia de lo que falta— o a fallar la
 * carga y quedarse sin drop-crafting por contenido futuro.
 */
public final class DropCraftingReloadListener extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    public DropCraftingReloadListener() {
        super(GSON, "drop_crafting");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager,
            ProfilerFiller profiler) {
        Map<String, DropRecipe> loaded = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            DropRecipe.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(error -> LOGGER.error("[{}] receta de drop-crafting {} invalida (se ignora): {}",
                            DojutsuMod.MOD_NAME, entry.getKey(), error))
                    .ifPresent(recipe -> loaded.put(entry.getKey().toString(), recipe));
        }
        DropCraftingRegistry.load(loaded);

        int pending = (int) loaded.values().stream()
                .filter(recipe -> BuiltInRegistries.ITEM
                        .getOptional(ResourceLocation.parse(recipe.result())).isEmpty())
                .count();
        LOGGER.info("[{}] Drop-crafting cargado: {} combinaciones ({} pendientes de su item).",
                DojutsuMod.MOD_NAME, loaded.size(), pending);
    }
}
