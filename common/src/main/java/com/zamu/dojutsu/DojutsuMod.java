package com.zamu.dojutsu;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.debug.DummyAttachment;
import com.zamu.dojutsu.debug.DummyAttachmentCommand;
import com.zamu.dojutsu.debug.PingPongClientDebug;
import com.zamu.dojutsu.debug.PingPongDebug;
import com.zamu.dojutsu.core.DataReloadListener;
import com.zamu.dojutsu.registry.ModAttachments;
import com.zamu.dojutsu.registry.ModAttributes;
import com.zamu.dojutsu.registry.ModEntities;
import com.zamu.dojutsu.registry.ModItems;
import com.zamu.dojutsu.registry.ModParticles;
import com.zamu.dojutsu.registry.ModSounds;
import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;

/**
 * Entrypoint comun del mod, independiente del loader.
 *
 * <p>Los adaptadores de cada plataforma (NeoForge y Fabric) delegan aqui:
 * {@link #init()} se ejecuta en cliente y servidor; {@link #initClient()} solo
 * en el entorno de cliente. Esta separacion garantiza que el servidor dedicado
 * nunca cargue codigo de cliente (regla dura de aislamiento cliente/servidor).
 *
 * <p>En esta fase (DJT-002) ambos metodos solo registran un log de arranque; el
 * registro real de contenido, config, red y eventos se anadira en fases posteriores
 * (ver {@code 04_CICLO_DE_VIDA.md} fases 2-4).
 */
public final class DojutsuMod {

    /** Identificador unico del mod, compartido por los tres subproyectos. */
    public static final String MOD_ID = "dojutsu";

    /** Nombre legible del mod, usado en logs y metadatos. */
    public static final String MOD_NAME = "Dojutsu";

    private static final Logger LOGGER = LogUtils.getLogger();

    private DojutsuMod() {
    }

    /**
     * Construye un {@link ResourceLocation} en el namespace del mod.
     *
     * @param path ruta del identificador
     * @return {@code dojutsu:<path>}
     */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Inicializacion comun a cliente y servidor (registro, config, red, eventos). */
    public static void init() {
        // Registro base de contenido (una entrada placeholder por tipo).
        ModItems.register();
        ModEntities.register();
        ModAttributes.register();
        ModSounds.register();
        ModParticles.register();
        ModAttachments.register();

        // Reload listener del datapack del mod (datos de servidor).
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new DataReloadListener(), id("data_reload"));

        // Andamiaje de validacion de plataforma (F0): attachment dummy + comando + red ping-pong.
        DummyAttachment.register();
        DummyAttachmentCommand.register();
        PingPongDebug.registerReceivers();
        LOGGER.info("[{}] Inicializacion comun completada.", MOD_NAME);
    }

    /** Inicializacion exclusiva de cliente (keybinds, HUD, renders, pantallas). */
    public static void initClient() {
        // Andamiaje de validacion de plataforma (F0): disparador de cliente del ping-pong.
        PingPongClientDebug.registerClientTrigger();
        LOGGER.info("[{}] Inicializacion de cliente completada.", MOD_NAME);
    }
}
