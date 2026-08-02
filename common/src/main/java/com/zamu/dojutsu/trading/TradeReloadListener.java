package com.zamu.dojutsu.trading;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.domain.trading.TraderCatalogue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

/**
 * Carga los catalogos de comerciante desde {@code data/dojutsu/trades/*.json} al {@link TradeRegistry}.
 *
 * <h2>Informa de la mercancia que aun no existe, en vez de callarla</h2>
 * El catalogo declara el <b>surtido completo del datapack</b>, incluida la mercancia cuyo item todavia no se ha
 * implementado (bombas de humo, pergaminos, ojos como item). Al cargar, se cuenta cuantas ofertas apuntan a un
 * item inexistente y <b>se dice en el log</b>. Esas ofertas simplemente no se ofrecen; el dia que el item se
 * registre, <b>aparecen solas</b> sin tocar codigo.
 *
 * <p>Es la alternativa honesta a las dos malas: ni recortar el catalogo (y perder la referencia de lo que falta)
 * ni fallar la carga (y quedarse sin comerciantes por mercancia futura).
 */
public final class TradeReloadListener extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    public TradeReloadListener() {
        super(GSON, "trades");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager,
            ProfilerFiller profiler) {
        List<TraderCatalogue> loaded = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            TraderCatalogue.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(error -> LOGGER.error("[{}] catalogo {} invalido (se ignora): {}",
                            DojutsuMod.MOD_NAME, entry.getKey(), error))
                    .ifPresent(loaded::add);
        }
        TradeRegistry.load(loaded);

        int total = loaded.stream().mapToInt(catalogue -> catalogue.offers().size()).sum();
        int pending = (int) loaded.stream()
                .flatMap(catalogue -> catalogue.offers().stream())
                .filter(offer -> BuiltInRegistries.ITEM
                        .getOptional(ResourceLocation.parse(offer.item())).isEmpty())
                .count();
        LOGGER.info("[{}] Catalogos de comercio cargados: {} ({} ofertas, {} pendientes de su item).",
                DojutsuMod.MOD_NAME, loaded.size(), total, pending);
    }
}
