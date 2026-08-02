package com.zamu.dojutsu.registry;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.platform.AttachmentHandle;
import com.zamu.dojutsu.platform.PlatformServices;

/**
 * Registro de Data Attachments del mod, via el puerto
 * {@link com.zamu.dojutsu.platform.AttachmentService}.
 *
 * <p>Declara el attachment real {@code player_ninja_data} ({@link PlayerNinjaData}), persistente y con
 * copia en muerte (el estado ninja sobrevive a los respawns). Reemplaza el attachment dummy de F0.
 */
public final class ModAttachments {

    /**
     * Estado ninja por jugador. Persistente (via {@link NinjaData#PERSISTENT_CODEC}, que migra al cargar)
     * y con {@code copyOnDeath} para conservarse entre respawns. Acceso via {@link NinjaData#get}.
     */
    public static final AttachmentHandle<PlayerNinjaData> PLAYER_NINJA_DATA =
            PlatformServices.ATTACHMENTS.register(
                    DojutsuMod.id("player_ninja_data"),
                    PlayerNinjaData::new,
                    NinjaData.PERSISTENT_CODEC,
                    true);

    private ModAttachments() {
    }

    /**
     * Fuerza la inicializacion de esta clase (y por tanto el registro del attachment)
     * durante la inicializacion del mod.
     */
    public static void register() {
        // El registro ocurre al inicializar el campo estatico PLAYER_NINJA_DATA.
    }
}
