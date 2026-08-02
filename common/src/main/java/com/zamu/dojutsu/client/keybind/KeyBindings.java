package com.zamu.dojutsu.client.keybind;

import com.zamu.dojutsu.client.screen.AbilitySelectScreen;
import com.zamu.dojutsu.client.screen.NinjaMenuScreen;
import com.zamu.dojutsu.network.CastJutsuPayload;
import com.zamu.dojutsu.network.EightGatesPayload;
import com.zamu.dojutsu.network.FtgTeleportPayload;
import com.zamu.dojutsu.network.JinchurikiFormPayload;
import com.zamu.dojutsu.network.SoahSealPayload;
import com.zamu.dojutsu.network.ToggleCursedSealPayload;
import com.zamu.dojutsu.network.ToggleSagePayload;
import com.zamu.dojutsu.network.ToggleVisionPayload;
import com.zamu.dojutsu.network.ToggleWaterWalkPayload;
import com.zamu.dojutsu.network.UseEyeAbilityPayload;
import com.zamu.dojutsu.platform.PlatformServices;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import org.lwjgl.glfw.GLFW;

/**
 * Keybinds del mod (input real, remapeable; reemplaza el polling de slot/estadisticas del datapack, RM-1).
 * <b>Solo cliente:</b> se invoca desde {@code DojutsuMod.initClient()}; no debe cargarse en servidor dedicado.
 *
 * <ul>
 *   <li>{@link #OPEN_MENU} ({@code M}) abre el {@link NinjaMenuScreen}.</li>
 *   <li>{@link #CAST_JUTSU} ({@code V}) lanza el jutsu del item en la mano principal — envia
 *       {@link CastJutsuPayload}; el servidor lee el item real y valida (DJT-080).</li>
 *   <li>{@link #OPEN_EYE_MENU} ({@code N}) abre el menu de habilidades oculares ({@link AbilitySelectScreen});
 *       reemplaza el hotbar-as-selector del datapack (DJT-117).</li>
 *   <li>{@link #USE_EYE_ABILITY} ({@code R}) lanza la habilidad ocular seleccionada — envia
 *       {@link UseEyeAbilityPayload}; el servidor lee la {@code selectedAbility} y valida (DJT-118).</li>
 * </ul>
 * El mapa de teclas es contextual y ampliable (habilidades, chakra vision...) sin rehacer este registro.
 */
public final class KeyBindings {

    /** Abrir el menu ninja. Por defecto {@code M}; remapeable en Controles. */
    public static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.dojutsu.open_menu", GLFW.GLFW_KEY_M, "key.categories.dojutsu");

    /** Lanzar el jutsu equipado en la mano principal. Por defecto {@code V}; remapeable. */
    public static final KeyMapping CAST_JUTSU = new KeyMapping(
            "key.dojutsu.cast_jutsu", GLFW.GLFW_KEY_V, "key.categories.dojutsu");

    /** Abrir el menu de habilidades oculares. Por defecto {@code N}; remapeable. */
    public static final KeyMapping OPEN_EYE_MENU = new KeyMapping(
            "key.dojutsu.open_eye_menu", GLFW.GLFW_KEY_N, "key.categories.dojutsu");

    /** Lanzar la habilidad ocular seleccionada. Por defecto {@code R}; remapeable. */
    public static final KeyMapping USE_EYE_ABILITY = new KeyMapping(
            "key.dojutsu.use_eye_ability", GLFW.GLFW_KEY_R, "key.categories.dojutsu");

    /** Alternar el modo sabio. Por defecto {@code G}; remapeable (F6/DJT-136). */
    public static final KeyMapping TOGGLE_SAGE = new KeyMapping(
            "key.dojutsu.toggle_sage", GLFW.GLFW_KEY_G, "key.categories.dojutsu");

    /** Alternar el sello maldito. Por defecto {@code B}; remapeable (F6/DJT-140). */
    public static final KeyMapping TOGGLE_CURSED_SEAL = new KeyMapping(
            "key.dojutsu.toggle_cursed_seal", GLFW.GLFW_KEY_B, "key.categories.dojutsu");

    /**
     * Ocho Puertas. Por defecto {@code H}; remapeable (F6/DJT-145). Pulsar abre la <b>siguiente</b> puerta;
     * <b>agachado</b>, las cierra todas — las dos intenciones del clic del item del datapack en una sola tecla.
     */
    public static final KeyMapping EIGHT_GATES = new KeyMapping(
            "key.dojutsu.eight_gates", GLFW.GLFW_KEY_H, "key.categories.dojutsu");

    /**
     * Sello de la Fuerza de un Centenar. Por defecto {@code J}; remapeable (F6/DJT-150). Pulsar lo alterna;
     * <b>agachado</b> con la reserva vacia, lo <b>recarga</b> — los dos gestos del item del datapack.
     */
    public static final KeyMapping SOAH_SEAL = new KeyMapping(
            "key.dojutsu.soah_seal", GLFW.GLFW_KEY_J, "key.categories.dojutsu");

    /**
     * Forma de jinchuriki. Por defecto {@code K}; remapeable (F7/DJT-166). Pulsar transforma; <b>agachado</b>
     * lanza la tecnica de la bestia; <b>corriendo</b> alterna el Modo Baryon.
     */
    public static final KeyMapping JINCHURIKI_FORM = new KeyMapping(
            "key.dojutsu.jinchuriki_form", GLFW.GLFW_KEY_K, "key.categories.dojutsu");

    /**
     * Saltar a la marca del Vuelo del Dios del Trueno. Por defecto {@code Y}; remapeable (F8/DJT-180). En el
     * datapack el gesto es agacharse con el kunai en la mano; aqui es una tecla, porque la marca ya no vive en
     * el item.
     */
    public static final KeyMapping FTG_TELEPORT = new KeyMapping(
            "key.dojutsu.ftg_teleport", GLFW.GLFW_KEY_Y, "key.categories.dojutsu");

    /** Alternar la chakra vision. Por defecto {@code C}; remapeable (DJT-125). */
    public static final KeyMapping TOGGLE_VISION = new KeyMapping(
            "key.dojutsu.toggle_vision", GLFW.GLFW_KEY_C, "key.categories.dojutsu");

    /**
     * Alternar la caminata sobre el agua. Por defecto {@code U}; remapeable (F10, peticion de la verificacion en
     * partida).
     *
     * <p>El datapack no tiene interruptor: caminar sobre el agua es automatico y la unica salida es agacharse.
     * En partida eso obliga a sostener el sneak durante todo un rio. Agacharse <b>sigue funcionando igual</b>;
     * esta tecla es para cuando no quieres caminar sobre el agua en un buen rato.
     */
    public static final KeyMapping TOGGLE_WATER_WALK = new KeyMapping(
            "key.dojutsu.toggle_water_walk", GLFW.GLFW_KEY_U, "key.categories.dojutsu");

    private KeyBindings() {
    }

    /** Registra los keybinds y el sondeo de pulsacion. Llamar una vez en la inicializacion de cliente. */
    public static void register() {
        KeyMappingRegistry.register(OPEN_MENU);
        KeyMappingRegistry.register(CAST_JUTSU);
        KeyMappingRegistry.register(OPEN_EYE_MENU);
        KeyMappingRegistry.register(USE_EYE_ABILITY);
        KeyMappingRegistry.register(TOGGLE_VISION);
        KeyMappingRegistry.register(TOGGLE_SAGE);
        KeyMappingRegistry.register(TOGGLE_CURSED_SEAL);
        KeyMappingRegistry.register(EIGHT_GATES);
        KeyMappingRegistry.register(SOAH_SEAL);
        KeyMappingRegistry.register(JINCHURIKI_FORM);
        KeyMappingRegistry.register(FTG_TELEPORT);
        KeyMappingRegistry.register(TOGGLE_WATER_WALK);
        ClientTickEvent.CLIENT_POST.register(KeyBindings::onClientTick);
    }

    private static void onClientTick(Minecraft minecraft) {
        // consumeClick() devuelve true una vez por pulsacion (no es polling de estado). Solo en el juego,
        // sin otra pantalla abierta (no colisiona con inventario/chat).
        while (OPEN_MENU.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                minecraft.setScreen(new NinjaMenuScreen());
            }
        }
        while (CAST_JUTSU.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                // El cliente solo envia la intencion (mano principal); el servidor lee el item y valida.
                PlatformServices.NETWORK.sendToServer(new CastJutsuPayload(InteractionHand.MAIN_HAND));
            }
        }
        while (OPEN_EYE_MENU.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                // La Screen pide la vista al servidor al abrirse (habilidades del ojo activo, data-driven).
                minecraft.setScreen(new AbilitySelectScreen());
            }
        }
        while (USE_EYE_ABILITY.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                // Solo la intencion; el servidor lee la habilidad seleccionada del estado y valida (DJT-118).
                PlatformServices.NETWORK.sendToServer(new UseEyeAbilityPayload());
            }
        }
        while (TOGGLE_VISION.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                // Solo la intencion; el servidor invierte el estado y decide si la vision aplica (DJT-125).
                PlatformServices.NETWORK.sendToServer(new ToggleVisionPayload());
            }
        }
        while (TOGGLE_WATER_WALK.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                // Solo la intencion; el servidor invierte el estado y avisa (F10).
                PlatformServices.NETWORK.sendToServer(new ToggleWaterWalkPayload());
            }
        }
        while (TOGGLE_SAGE.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                // Solo la intencion; el servidor valida sabio/sello/energia y decide (F6/DJT-136).
                PlatformServices.NETWORK.sendToServer(new ToggleSagePayload());
            }
        }
        while (TOGGLE_CURSED_SEAL.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                // Solo la intencion; el servidor valida que porte sello y lo alterna (F6/DJT-140).
                PlatformServices.NETWORK.sendToServer(new ToggleCursedSealPayload());
            }
        }
        while (EIGHT_GATES.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                // agachado = cerrar, de pie = abrir la siguiente (el clic/sneak-clic del item del datapack)
                PlatformServices.NETWORK.sendToServer(new EightGatesPayload(minecraft.player.isShiftKeyDown()));
            }
        }
        while (SOAH_SEAL.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                // agachado con la reserva vacia = recargar; en cualquier otro caso, alternar (F6/DJT-150)
                PlatformServices.NETWORK.sendToServer(new SoahSealPayload(minecraft.player.isShiftKeyDown()));
            }
        }
        while (JINCHURIKI_FORM.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                // agachado = técnica de la bestia; corriendo = Baryon; de pie = transformarse (F7/DJT-166)
                JinchurikiFormPayload.Action action = minecraft.player.isShiftKeyDown()
                        ? JinchurikiFormPayload.Action.CAST_MOVE
                        : minecraft.player.isSprinting()
                        ? JinchurikiFormPayload.Action.TOGGLE_BARYON
                        : JinchurikiFormPayload.Action.TOGGLE_FORM;
                PlatformServices.NETWORK.sendToServer(new JinchurikiFormPayload(action));
            }
        }
        while (FTG_TELEPORT.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                // solo la intencion: el SERVIDOR decide a donde te mueve (F8/DJT-180)
                PlatformServices.NETWORK.sendToServer(new FtgTeleportPayload());
            }
        }
    }
}
