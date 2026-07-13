package com.zamu.dojutsu.neoforge.platform;

import com.mojang.serialization.Codec;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.platform.AttachmentHandle;
import com.zamu.dojutsu.platform.AttachmentService;
import dev.architectury.platform.hooks.EventBusesHooks;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Implementacion NeoForge del puerto {@link AttachmentService}. Registra los tipos de
 * attachment con un {@code DeferredRegister} sobre {@code ATTACHMENT_TYPES}, enganchado
 * al bus del mod la primera vez que se registra un attachment (durante la construccion
 * del mod, antes de que se dispare el evento de registro).
 */
public final class NeoForgeAttachmentService implements AttachmentService {

    private final DeferredRegister<AttachmentType<?>> attachmentTypes =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, DojutsuMod.MOD_ID);
    private boolean boundToBus;

    @Override
    public <T> AttachmentHandle<T> register(ResourceLocation id, Supplier<T> defaultValue, Codec<T> codec,
                                            boolean copyOnDeath) {
        DeferredHolder<AttachmentType<?>, AttachmentType<T>> holder =
                attachmentTypes.register(id.getPath(), () -> build(defaultValue, codec, copyOnDeath));
        ensureBoundToBus();
        return new NeoForgeAttachmentHandle<>(holder);
    }

    private static <T> AttachmentType<T> build(Supplier<T> defaultValue, Codec<T> codec, boolean copyOnDeath) {
        AttachmentType.Builder<T> builder = AttachmentType.builder(defaultValue).serialize(codec);
        if (copyOnDeath) {
            builder.copyOnDeath();
        }
        return builder.build();
    }

    private void ensureBoundToBus() {
        if (boundToBus) {
            return;
        }
        IEventBus modEventBus = EventBusesHooks.getModEventBus(DojutsuMod.MOD_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Bus de eventos del mod no disponible para " + DojutsuMod.MOD_ID));
        attachmentTypes.register(modEventBus);
        boundToBus = true;
    }
}
