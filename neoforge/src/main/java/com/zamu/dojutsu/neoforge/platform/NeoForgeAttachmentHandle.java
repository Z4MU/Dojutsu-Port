package com.zamu.dojutsu.neoforge.platform;

import com.zamu.dojutsu.platform.AttachmentHandle;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;

import java.util.function.Supplier;

/**
 * Manejador NeoForge de un attachment, respaldado por el {@code getData}/{@code setData}
 * de la entidad. El tipo se obtiene perezosamente del holder diferido.
 *
 * @param <T> tipo del valor adjunto
 */
public final class NeoForgeAttachmentHandle<T> implements AttachmentHandle<T> {

    private final Supplier<AttachmentType<T>> type;

    NeoForgeAttachmentHandle(Supplier<AttachmentType<T>> type) {
        this.type = type;
    }

    @Override
    public T get(Entity holder) {
        return holder.getData(type.get());
    }

    @Override
    public void set(Entity holder, T value) {
        holder.setData(type.get(), value);
    }
}
