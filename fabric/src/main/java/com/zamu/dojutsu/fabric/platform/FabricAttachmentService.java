package com.zamu.dojutsu.fabric.platform;

import com.mojang.serialization.Codec;
import com.zamu.dojutsu.platform.AttachmentHandle;
import com.zamu.dojutsu.platform.AttachmentService;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Implementacion Fabric del puerto {@link AttachmentService} sobre el
 * {@code AttachmentRegistry} de Fabric API. El registro es inmediato (no diferido).
 */
public final class FabricAttachmentService implements AttachmentService {

    @Override
    public <T> AttachmentHandle<T> register(ResourceLocation id, Supplier<T> defaultValue, Codec<T> codec,
                                            boolean copyOnDeath) {
        AttachmentType<T> type = AttachmentRegistry.create(id, builder -> {
            builder.initializer(defaultValue::get).persistent(codec);
            if (copyOnDeath) {
                builder.copyOnDeath();
            }
        });
        return new FabricAttachmentHandle<>(type);
    }
}
