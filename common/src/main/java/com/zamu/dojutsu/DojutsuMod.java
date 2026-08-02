package com.zamu.dojutsu;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.client.ClientDialogue;
import com.zamu.dojutsu.client.ClientEyeMenu;
import com.zamu.dojutsu.client.ClientNinjaData;
import com.zamu.dojutsu.client.ClientOnboardingOptions;
import com.zamu.dojutsu.client.ClientWaterWalk;
import com.zamu.dojutsu.client.WaterWalkClient;
import com.zamu.dojutsu.client.hud.HudOverlays;
import com.zamu.dojutsu.client.keybind.KeyBindings;
import com.zamu.dojutsu.client.screen.DialogueOpener;
import com.zamu.dojutsu.client.screen.NinjaCreationOpener;
import com.zamu.dojutsu.debug.AnchorDebugCommand;
import com.zamu.dojutsu.debug.ChakraDebugCommand;
import com.zamu.dojutsu.debug.EyeDebugCommand;
import com.zamu.dojutsu.debug.GodTreeDebugCommand;
import com.zamu.dojutsu.debug.BeastDebugCommand;
import com.zamu.dojutsu.debug.CursedSealDebugCommand;
import com.zamu.dojutsu.debug.EightGatesDebugCommand;
import com.zamu.dojutsu.debug.ItemDebugCommand;
import com.zamu.dojutsu.debug.SageDebugCommand;
import com.zamu.dojutsu.debug.ShinobiDebugCommand;
import com.zamu.dojutsu.debug.SoahDebugCommand;
import com.zamu.dojutsu.debug.JutsuDebugCommand;
import com.zamu.dojutsu.debug.ClanDebugCommand;
import com.zamu.dojutsu.debug.NatureDebugCommand;
import com.zamu.dojutsu.debug.NinjaDataDebugCommand;
import com.zamu.dojutsu.debug.OnboardingDebugCommand;
import com.zamu.dojutsu.debug.ProgressionDebugCommand;
import com.zamu.dojutsu.debug.StoryDebugCommand;
import com.zamu.dojutsu.debug.WeaponDebugCommand;
import com.zamu.dojutsu.debug.SchedulerDebugCommand;
import com.zamu.dojutsu.core.DojutsuLifecycle;
import com.zamu.dojutsu.core.command.AdminCommands;
import dev.architectury.event.events.client.ClientPlayerEvent;
import com.zamu.dojutsu.registry.ModAttachments;
import com.zamu.dojutsu.registry.ModDataComponents;
import com.zamu.dojutsu.registry.ModAttributes;
import com.zamu.dojutsu.registry.ModEntities;
import com.zamu.dojutsu.registry.ModItems;
import com.zamu.dojutsu.registry.ModParticles;
import com.zamu.dojutsu.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
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
        ModDataComponents.register();
        ModAttachments.register();

        // Ciclo de vida: suscribe los listeners del loader y el setup de arranque (config, tick, reload,
        // join/quit, muerte/respawn, cambio de dimension, payloads de sync). Centraliza lo estatico de main/load.
        DojutsuLifecycle.init();

        // Comandos de administracion (/dojutsu reset_ninja + stubs).
        AdminCommands.register();

        // Comandos de depuracion (temporales; se retiran cuando el HUD de F2 permita observar el estado).
        NinjaDataDebugCommand.register();
        SchedulerDebugCommand.register();
        ChakraDebugCommand.register();
        ProgressionDebugCommand.register();
        NatureDebugCommand.register();
        ClanDebugCommand.register();
        OnboardingDebugCommand.register();
        AnchorDebugCommand.register();
        JutsuDebugCommand.register();
        WeaponDebugCommand.register();
        EyeDebugCommand.register();
        SageDebugCommand.register(); // andamiaje F6: energia natural/modo sabio
        CursedSealDebugCommand.register(); // andamiaje F6: sello maldito (el Injector es item de F8)
        EightGatesDebugCommand.register(); // andamiaje F6: Ocho Puertas (fatiga y vetos son muy time-gated)
        SoahDebugCommand.register(); // andamiaje F6: SOAH (requisitos altisimos; el item del sello es de F8)
        BeastDebugCommand.register(); // andamiaje F7: bestias (los huevos de invocacion son items de F8)
        ItemDebugCommand.register(); // andamiaje F8: ADN, mascaras y pildoras (medicina y azar muy time-gated)
        ShinobiDebugCommand.register(); // andamiaje F8: shinobi NPC (el huevo de invocacion no tiene ticket)
        StoryDebugCommand.register(); // andamiaje F9: Sabios (rango Kage + cola de 1 h + tirada del 5%)
        GodTreeDebugCommand.register(); // andamiaje F9: Arbol Divino (25 min de crecimiento + forma del Diez Colas)
        LOGGER.info("[{}] Inicializacion comun completada.", MOD_NAME);
    }

    /** Inicializacion exclusiva de cliente (keybinds, HUD, renders, pantallas). */
    public static void initClient() {
        // Limpia las caches read-only (estado ninja y opciones de clan) al salir del mundo / desconectar.
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
            ClientNinjaData.clear();
            ClientOnboardingOptions.clear();
            ClientEyeMenu.clear();
            ClientDialogue.clear();
            ClientWaterWalk.clear();
        });

        // Keybinds del mod (abrir menu ninja; input real remapeable).
        KeyBindings.register();

        // Creacion del ninja: abre (y reabre) la Screen mientras la fase sincronizada sea de seleccion.
        NinjaCreationOpener.register();

        // Dialogo del Sabio: abre la Screen cuando el servidor anuncia un encuentro (F9/DJT-200).
        DialogueOpener.register();

        // Caminar sobre el agua: el cliente se sostiene solo para no pelearse con el servidor (F9/DJT-205).
        WaterWalkClient.register();

        // Overlays del HUD (chakra y rango), leidos de la cache read-only.
        HudOverlays.register();
        LOGGER.info("[{}] Inicializacion de cliente completada.", MOD_NAME);
    }
}
