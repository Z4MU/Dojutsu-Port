package com.zamu.dojutsu.registry;

import com.mojang.serialization.Codec;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.platform.AttachmentHandle;
import com.zamu.dojutsu.platform.PlatformServices;

/**
 * Registro de Data Attachments del mod. En F0 solo declara un attachment placeholder
 * ({@code placeholder}) via el puerto {@link com.zamu.dojutsu.platform.AttachmentService}.
 * Aqui vivira {@code PlayerNinjaData} cuando exista el modulo {@code data}.
 */
public final class ModAttachments {

    /** Attachment placeholder (entero persistente por entidad). */
    public static final AttachmentHandle<Integer> PLACEHOLDER =
            PlatformServices.ATTACHMENTS.register(DojutsuMod.id("placeholder"), () -> 0, Codec.INT, false);

    private ModAttachments() {
    }

    /**
     * Fuerza la inicializacion de esta clase (y por tanto el registro del attachment)
     * durante la inicializacion del mod.
     */
    public static void register() {
        // El registro ocurre al inicializar el campo estatico PLACEHOLDER.
    }
}
