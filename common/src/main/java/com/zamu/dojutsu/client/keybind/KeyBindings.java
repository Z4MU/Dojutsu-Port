package com.zamu.dojutsu.client.keybind;

import com.zamu.dojutsu.client.screen.JutsuWheelScreen;
import com.zamu.dojutsu.client.screen.NinjaMenuScreen;
import com.zamu.dojutsu.network.CastJutsuPayload;
import com.zamu.dojutsu.network.CastSlotPayload;
import com.zamu.dojutsu.network.EightGatesPayload;
import com.zamu.dojutsu.network.FtgTeleportPayload;
import com.zamu.dojutsu.network.JinchurikiFormPayload;
import com.zamu.dojutsu.network.SoahSealPayload;
import com.zamu.dojutsu.network.ToggleCursedSealPayload;
import com.zamu.dojutsu.network.ToggleEyePayload;
import com.zamu.dojutsu.network.ToggleSagePayload;
import com.zamu.dojutsu.network.ToggleVisionPayload;
import com.zamu.dojutsu.network.ToggleWaterWalkPayload;
import com.zamu.dojutsu.network.UseEyeAbilityPayload;
import com.zamu.dojutsu.platform.PlatformServices;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Keybinds del mod (input real, remapeable; reemplaza el polling de slot/estadisticas del datapack, RM-1).
 * <b>Solo cliente:</b> se invoca desde {@code DojutsuMod.initClient()}; no debe cargarse en servidor dedicado.
 *
 * <ul>
 *   <li>{@link #OPEN_MENU} ({@code M}) abre el {@link NinjaMenuScreen}.</li>
 *   <li>{@link #CAST_JUTSU} ({@code V}) lanza el jutsu del item en la mano principal — envia
 *       {@link CastJutsuPayload}; el servidor lee el jutsu seleccionado del estado y valida
 *       (DJT-080, reapuntado al selector en F11/DJT-231).</li>
 *   <li>Las habilidades oculares y el libro de jutsus <b>ya no tienen tecla</b>: son pestañas del menu
 *       ninja desde F11/DJT-236, que libero {@code N} y {@code P}.</li>
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

    /**
     * Encender/apagar el dojutsu. Por defecto {@code X}; remapeable (F11/DJT-230).
     *
     * <p>No es {@code K} ni {@code B} —las dos estaban ya cogidas por la forma de jinchuriki y el sello
     * maldito—, y {@code X} esta libre tanto aqui como en los controles de vanilla.
     */
    public static final KeyMapping TOGGLE_EYE = new KeyMapping(
            "key.dojutsu.toggle_eye", GLFW.GLFW_KEY_X, "key.categories.dojutsu");

    /**
     * Rueda de jutsus. Por defecto {@code Z}; remapeable (F11/DJT-234).
     *
     * <p>El plan proponia {@code B}, pero {@code B} ya es el sello maldito. {@code Z} esta libre en el mod y
     * en los controles de vanilla, y cae bajo la mano izquierda sin soltar el movimiento, que es lo que pide
     * un gesto de mantener.
     *
     * <p>Se usa <b>manteniendola</b>: abre la rueda y al soltar elige el sector apuntado. Un toque corto
     * tambien vale: la rueda se queda abierta y se elige con el raton.
     */
    public static final KeyMapping JUTSU_WHEEL = new KeyMapping(
            "key.dojutsu.jutsu_wheel", GLFW.GLFW_KEY_Z, "key.categories.dojutsu");

    /**
     * Modificador de los huecos rapidos. Por defecto {@code ALT izquierdo}; remapeable (F11/DJT-234).
     *
     * <p>Mantenerlo y pulsar {@code 1}-{@code 8} lanza el jutsu de ese hueco <b>sin abrir nada</b>: es lo que
     * se usa en combate de verdad, mientras que la rueda sirve para cambiar de set entre peleas.
     *
     * <p>Va con modificador y no directamente sobre {@code 1}-{@code 8} porque esas teclas son la hotbar
     * vanilla, y robarlas dejaria al jugador sin poder cambiar de objeto. Con {@code ALT} pulsado la hotbar
     * <b>no</b> cambia de slot: la pulsacion se consume antes (ver {@link #onRawKey}).
     */
    public static final KeyMapping QUICK_CAST_MODIFIER = new KeyMapping(
            "key.dojutsu.quick_cast_modifier", GLFW.GLFW_KEY_LEFT_ALT, "key.categories.dojutsu");

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

    /**
     * Indica si la tecla de la rueda sigue fisicamente pulsada.
     *
     * <p>Se pregunta a GLFW y no a {@link KeyMapping#isDown()} a proposito: mientras hay una pantalla
     * abierta, vanilla deja de alimentar el estado de los KeyMapping —estan pensados para el juego, no para
     * las GUI—, asi que {@code isDown()} contestaria {@code false} nada mas abrirse la rueda y el gesto de
     * mantener no funcionaria. Respeta el remapeo porque el codigo de tecla sale del propio KeyMapping.
     *
     * <p>Si el jugador la remapea a un boton del raton, esto devuelve {@code false} y la rueda se queda
     * abierta hasta que elija o cancele: peor gesto, pero nunca una rueda que no se puede cerrar.
     */
    public static boolean isWheelKeyHeld() {
        return isHeld(JUTSU_WHEEL);
    }

    /**
     * Sondeo real del teclado para un keybind. Ver {@link #isWheelKeyHeld()} para el porque de no usar
     * {@link KeyMapping#isDown()}: mientras hay una pantalla abierta vanilla deja de alimentar ese estado.
     */
    private static boolean isHeld(KeyMapping mapping) {
        // saveString() es el nombre canonico de la tecla ligada ahora mismo ("key.keyboard.z"); no hay
        // accesor publico a la Key en Mojmap, y esto respeta el remapeo igual.
        InputConstants.Key key = InputConstants.getKey(mapping.saveString());
        if (key.getType() != InputConstants.Type.KEYSYM || key.getValue() == GLFW.GLFW_KEY_UNKNOWN) {
            return false;
        }
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key.getValue());
    }

    /**
     * Huecos rapidos: {@code ALT} + {@code 1}-{@code 8} lanza el jutsu de ese hueco (F11/DJT-234).
     *
     * <p>Se engancha al <b>input crudo</b> y no al sondeo de keybinds porque hay que <b>consumir</b> la
     * pulsacion antes de que vanilla la lea: si no, la hotbar cambiaria de slot a la vez que se lanza el
     * jutsu, y el jugador acabaria con otro objeto en la mano en mitad de un combate. Devolver
     * {@code interruptFalse()} corta ahi el procesado.
     *
     * <p>Sin el modificador pulsado no se toca nada: la hotbar sigue siendo la hotbar.
     */
    private static EventResult onRawKey(Minecraft minecraft, int keyCode, int scanCode, int action,
            int modifiers) {
        if (action != GLFW.GLFW_PRESS || minecraft.player == null || minecraft.screen != null) {
            return EventResult.pass();
        }
        if (!isHeld(QUICK_CAST_MODIFIER)) {
            return EventResult.pass();
        }
        int slot = slotOf(keyCode);
        if (slot < 0) {
            return EventResult.pass();
        }
        // Solo la intencion (la POSICION pulsada); el servidor mira que hay en ese hueco y valida.
        PlatformServices.NETWORK.sendToServer(new CastSlotPayload(slot));
        return EventResult.interruptFalse(); // consumida: la hotbar no cambia de slot
    }

    /**
     * Hueco (0-7) de una tecla de la fila numerica o del teclado numerico; {@code -1} si no es ninguna.
     *
     * <p>La tecla {@code 9} queda fuera <b>a proposito</b>: la hotbar vanilla tiene nueve slots y la rueda
     * ocho, asi que {@code ALT+9} no es un hueco de jutsu y debe seguir siendo la hotbar de siempre.
     *
     * <p>Visible para tests.
     */
    static int slotOf(int keyCode) {
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_8) {
            return keyCode - GLFW.GLFW_KEY_1;
        }
        if (keyCode >= GLFW.GLFW_KEY_KP_1 && keyCode <= GLFW.GLFW_KEY_KP_8) {
            return keyCode - GLFW.GLFW_KEY_KP_1;
        }
        return -1;
    }

    /** Registra los keybinds y el sondeo de pulsacion. Llamar una vez en la inicializacion de cliente. */
    public static void register() {
        KeyMappingRegistry.register(OPEN_MENU);
        KeyMappingRegistry.register(CAST_JUTSU);
        KeyMappingRegistry.register(USE_EYE_ABILITY);
        KeyMappingRegistry.register(TOGGLE_EYE);
        KeyMappingRegistry.register(JUTSU_WHEEL);
        KeyMappingRegistry.register(QUICK_CAST_MODIFIER);
        KeyMappingRegistry.register(TOGGLE_VISION);
        KeyMappingRegistry.register(TOGGLE_SAGE);
        KeyMappingRegistry.register(TOGGLE_CURSED_SEAL);
        KeyMappingRegistry.register(EIGHT_GATES);
        KeyMappingRegistry.register(SOAH_SEAL);
        KeyMappingRegistry.register(JINCHURIKI_FORM);
        KeyMappingRegistry.register(FTG_TELEPORT);
        KeyMappingRegistry.register(TOGGLE_WATER_WALK);
        ClientTickEvent.CLIENT_POST.register(KeyBindings::onClientTick);
        ClientRawInputEvent.KEY_PRESSED.register(KeyBindings::onRawKey);
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
                // Solo la intencion; el servidor lee el jutsu seleccionado del estado y valida (DJT-231).
                PlatformServices.NETWORK.sendToServer(new CastJutsuPayload());
            }
        }
        while (JUTSU_WHEEL.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                // La rueda dibuja lo ya sincronizado; no pide nada al servidor al abrirse (F11/DJT-234).
                minecraft.setScreen(new JutsuWheelScreen());
            }
        }
        while (USE_EYE_ABILITY.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                // Solo la intencion; el servidor lee la habilidad seleccionada del estado y valida (DJT-118).
                PlatformServices.NETWORK.sendToServer(new UseEyeAbilityPayload());
            }
        }
        while (TOGGLE_EYE.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                // Solo la intencion; el servidor valida ojo y fatiga, y decide el estado (F11/DJT-230).
                PlatformServices.NETWORK.sendToServer(new ToggleEyePayload());
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
