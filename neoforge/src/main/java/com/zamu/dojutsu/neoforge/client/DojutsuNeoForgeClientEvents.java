package com.zamu.dojutsu.neoforge.client;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.registry.ModEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
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
        // Toda entidad registrada necesita un renderer; la marcador usa uno invisible.
        event.registerEntityRenderer(ModEntities.DEBUG_MARKER.get(), NoopRenderer::new);
    }
}
