package com.zamu.dojutsu.fabric;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.client.render.ShinobiRenderer;
import com.zamu.dojutsu.client.render.TailedBeastRenderer;
import com.zamu.dojutsu.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/**
 * Entrypoint {@code client} de Fabric. Fabric solo carga este entrypoint en el
 * entorno de cliente, por lo que el servidor dedicado nunca ejecuta
 * {@link DojutsuMod#initClient()} ni el registro de renderers.
 */
public final class DojutsuFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        DojutsuMod.initClient();
        // Toda entidad registrada necesita un renderer (regla 5).
        // El proyectil ninja se dibuja como item lanzado (su display item, sin assets propios).
        EntityRendererRegistry.register(ModEntities.NINJA_PROJECTILE.get(), ThrownItemRenderer::new);
        // Ancla y marcador son invisibles (logica de servidor).
        EntityRendererRegistry.register(ModEntities.ABILITY_ANCHOR.get(), NoopRenderer::new);
        EntityRendererRegistry.register(ModEntities.MARKER.get(), NoopRenderer::new);
        // La bestia toma prestada la malla del ravager vanilla; modelos propios en F10 (F7/DJT-160).
        EntityRendererRegistry.register(ModEntities.TAILED_BEAST.get(), TailedBeastRenderer::new);
        // El shinobi toma prestado el humanoide del zombi; skins por aldea en F10 (F8/DJT-185).
        EntityRendererRegistry.register(ModEntities.SHINOBI.get(), ShinobiRenderer::new);
        // El sello explosivo colocado se dibuja como el papel que lleva encima (F10/DJT-225).
        EntityRendererRegistry.register(ModEntities.EXPLOSIVE_TAG.get(), ThrownItemRenderer::new);
    }
}
