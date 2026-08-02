package com.zamu.dojutsu.client;

import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Mantiene quieto al jugador local mientras camina sobre el agua (F9/DJT-205). <b>Solo cliente.</b>
 *
 * <h2>Por que hace falta codigo de cliente para esto</h2>
 * El movimiento del jugador es <b>autoritativo en cliente</b>: el servidor puede corregirlo, pero cada
 * correccion es un tiron. Y la levitacion que apaga la gravedad la aplican <b>los dos</b>, con un empuje minimo
 * de {@code +0.05} que hace subir al jugador. Resultado de la primera version: el cliente subia, el servidor lo
 * bajaba cada tick, y se veian tirones al andar.
 *
 * <p>Aqui el cliente anula su propia velocidad vertical, asi que llega a la misma conclusion que el servidor por
 * su cuenta y este no tiene nada que corregir. Es la respuesta a {@code [RB-1]}: no se suaviza el temblor, se
 * elimina su causa.
 *
 * <p>No decide nada de gameplay ({@code [ADD/07]}): solo actua cuando el <b>servidor</b> ha dicho que el jugador
 * se sostiene ({@link ClientWaterWalk}).
 */
public final class WaterWalkClient {

    private WaterWalkClient() {
    }

    /** Suscribe el tick de cliente. Llamar una vez en la inicializacion de cliente. */
    public static void register() {
        ClientTickEvent.CLIENT_POST.register(WaterWalkClient::onClientTick);
    }

    private static void onClientTick(Minecraft minecraft) {
        if (minecraft.player == null || !ClientWaterWalk.active()) {
            return;
        }
        Vec3 motion = minecraft.player.getDeltaMovement();
        if (motion.y() != 0) {
            // Ni cae (la levitacion apaga la gravedad) ni sube (su empuje residual se anula aqui).
            minecraft.player.setDeltaMovement(motion.x(), 0, motion.z());
        }
    }
}
