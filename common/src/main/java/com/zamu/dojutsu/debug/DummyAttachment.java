package com.zamu.dojutsu.debug;

import com.mojang.serialization.Codec;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.platform.AttachmentHandle;
import com.zamu.dojutsu.platform.PlatformServices;
import net.minecraft.resources.ResourceLocation;

/**
 * Attachment de prueba (F0) para validar el puerto {@link com.zamu.dojutsu.platform.AttachmentService}
 * en ambos loaders (desriesgue RM-6). Almacena un unico entero persistente por entidad.
 *
 * <p>Es andamiaje temporal: se reemplazara por {@code PlayerNinjaData} cuando exista el
 * modulo {@code data}.
 */
public final class DummyAttachment {

    private static AttachmentHandle<Integer> handle;

    private DummyAttachment() {
    }

    /** Registra el attachment dummy. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        handle = PlatformServices.ATTACHMENTS.register(
                ResourceLocation.fromNamespaceAndPath(DojutsuMod.MOD_ID, "dummy_value"),
                () -> 0,
                Codec.INT,
                true);
    }

    /** @return el manejador del attachment dummy (tras {@link #register()}). */
    public static AttachmentHandle<Integer> handle() {
        if (handle == null) {
            throw new IllegalStateException("DummyAttachment no registrado; llama a register() en init().");
        }
        return handle;
    }
}
