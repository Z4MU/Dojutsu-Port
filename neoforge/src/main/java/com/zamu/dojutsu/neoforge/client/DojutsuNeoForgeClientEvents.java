package com.zamu.dojutsu.neoforge.client;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.client.render.ShinobiRenderer;
import com.zamu.dojutsu.client.render.TailedBeastRenderer;
import com.zamu.dojutsu.registry.ModEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import com.zamu.dojutsu.client.render.NinjaProjectileRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Eventos de cliente de NeoForge. Anotada con {@code value = Dist.CLIENT}, por lo que
 * NeoForge solo la carga en el entorno de cliente y nunca en el servidor dedicado.
 */
@EventBusSubscriber(modid = DojutsuMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DojutsuNeoForgeClientEvents {

    private DojutsuNeoForgeClientEvents() {
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Toda entidad registrada necesita un renderer (regla 5).
        // El proyectil ninja se dibuja como item lanzado (su display item, sin assets propios).
        event.registerEntityRenderer(ModEntities.NINJA_PROJECTILE.get(), NinjaProjectileRenderer::new);
        // Ancla y marcador son invisibles (logica de servidor).
        event.registerEntityRenderer(ModEntities.ABILITY_ANCHOR.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.MARKER.get(), NoopRenderer::new);
        // La bestia toma prestada la malla del ravager vanilla; modelos propios en F10 (F7/DJT-160).
        event.registerEntityRenderer(ModEntities.TAILED_BEAST.get(), TailedBeastRenderer::new);
        // El shinobi toma prestado el humanoide del zombi; skins por aldea en F10 (F8/DJT-185).
        event.registerEntityRenderer(ModEntities.SHINOBI.get(), ShinobiRenderer::new);
        // El sello explosivo colocado se dibuja como el papel que lleva encima (F10/DJT-225).
        event.registerEntityRenderer(ModEntities.EXPLOSIVE_TAG.get(), ThrownItemRenderer::new);
    }
}
