package com.zamu.dojutsu.fabric;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;

/**
 * Entrypoint {@code client} de Fabric. Fabric solo carga este entrypoint en el
 * entorno de cliente, por lo que el servidor dedicado nunca ejecuta
 * {@link DojutsuMod#initClient()} ni el registro de renderers.
 */
public final class DojutsuFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        DojutsuMod.initClient();
        // Toda entidad registrada necesita un renderer; la marcador usa uno invisible.
        EntityRendererRegistry.register(ModEntities.DEBUG_MARKER.get(), NoopRenderer::new);
    }
}
