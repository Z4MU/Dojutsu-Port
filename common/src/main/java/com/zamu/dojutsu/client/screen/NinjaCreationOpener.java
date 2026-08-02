package com.zamu.dojutsu.client.screen;

import com.zamu.dojutsu.client.ClientNinjaData;
import com.zamu.dojutsu.data.state.OnboardingState.Phase;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;

/**
 * Abre (y <b>reabre</b>) la {@link NinjaCreationScreen} mientras la fase sincronizada sea de seleccion
 * (DJT-060, riesgo de la ficha: "que el jugador cierre la pantalla sin elegir"). <b>Solo cliente.</b>
 *
 * <p>No se puede saltar la creacion: si el jugador cierra la pantalla (Esc, morir...), esta vuelve a abrirse
 * tras {@value #REOPEN_DELAY_TICKS} ticks — margen suficiente para abrir el menu de pausa y guardar/salir,
 * asi que la insistencia nunca bloquea el cliente. En cuanto el servidor marca {@code COMPLETED}, deja de
 * actuar. Reemplaza la insistencia por titulos de {@code join_main} (times {@code 0 9000 0}).
 */
public final class NinjaCreationOpener {

    /** 2 s entre cierre manual y reapertura: deja llegar al menu de pausa sin poder saltarse la creacion. */
    private static final int REOPEN_DELAY_TICKS = 40;

    private static int cooldown;

    private NinjaCreationOpener() {
    }

    /** Suscribe el tick de cliente. Llamar una vez en la inicializacion de cliente. */
    public static void register() {
        ClientTickEvent.CLIENT_POST.register(NinjaCreationOpener::onClientTick);
    }

    /** Retrasa la proxima reapertura (lo llama la pantalla al cerrarse). */
    static void delayReopen() {
        cooldown = REOPEN_DELAY_TICKS;
    }

    private static void onClientTick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.screen != null) {
            return; // sin mundo, o hay otra pantalla (pausa, muerte, inventario...): no interrumpir
        }
        Phase phase = ClientNinjaData.onboarding().orElse(null);
        if (phase != Phase.SELECTING_CLAN && phase != Phase.SELECTING_AFFINITY) {
            cooldown = 0; // fuera de la creacion no hay nada que reabrir
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        minecraft.setScreen(new NinjaCreationScreen());
    }
}
