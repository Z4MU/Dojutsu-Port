package com.zamu.dojutsu.fabric.platform;

import com.zamu.dojutsu.platform.AttachmentHandle;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.Entity;

/**
 * Manejador Fabric de un attachment. Las entidades implementan {@link AttachmentTarget}
 * (via mixin de Fabric API), por lo que se accede al valor con un cast a esa interfaz.
 *
 * @param <T> tipo del valor adjunto
 */
public final class FabricAttachmentHandle<T> implements AttachmentHandle<T> {

    private final AttachmentType<T> type;

    FabricAttachmentHandle(AttachmentType<T> type) {
        this.type = type;
    }

    @Override
    public T get(Entity holder) {
        return ((AttachmentTarget) holder).getAttachedOrCreate(type);
    }

    @Override
    public void set(Entity holder, T value) {
        ((AttachmentTarget) holder).setAttached(type, value);
    }
}
