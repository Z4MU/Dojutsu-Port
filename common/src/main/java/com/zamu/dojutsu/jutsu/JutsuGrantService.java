package com.zamu.dojutsu.jutsu;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.items.JutsuItem;
import com.zamu.dojutsu.network.NinjaSync;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/**
 * <b>Entrega de una tecnica</b> a un jugador (F10): marca el jutsu como conocido y le pone el item en la mano.
 *
 * <h2>Por que existe</h2>
 * Habia <b>tres</b> {@code TODO F4} distintos esperando exactamente esto —{@code UnlockService} (hitos),
 * {@code ClanService} (jutsu inicial del clan) y {@code ClanJutsuUnlockService} (desbloqueo progresivo del
 * clan)— y por eso <b>26 de los 128 jutsus no tenian forma de obtenerse en partida</b>: existian como datos,
 * tenian su umbral y su efecto, y nadie los entregaba nunca. El aprendizaje por learner (DJT-075) era la unica
 * via viva, y solo cubre las naturalezas y los Kekkei Genkai.
 *
 * <p>La entrega se escribe <b>una vez</b> aqui en vez de tres, que es lo que hacia el datapack con sus 145
 * funciones {@code give/jutsu/*}: alli cada tecnica tenia su propio {@code give} con el NBT copiado a mano.
 *
 * <h2>Tolerante con lo que aun no existe</h2>
 * Un id que no este en el {@link JutsuRegistry} <b>se salta con un aviso</b>, no rompe la entrega. Es el mismo
 * patron que el catalogo de comercio y el drop-crafting: los datos declaran el surtido completo y el codigo
 * cuenta lo que falta. Hace falta de verdad — el clan Kaguya declara {@code dance_of_the_camellia} y el Nara
 * {@code shadow_paralysis}, y ninguno de los dos jutsus esta portado todavia.
 */
public final class JutsuGrantService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private JutsuGrantService() {
    }

    /**
     * Entrega una tecnica: la marca como conocida y da el item. Idempotente — si ya la conocia no duplica el
     * item ni vuelve a avisar.
     *
     * @return {@code true} si se entrego ahora (y por tanto conviene sincronizar)
     */
    public static boolean grant(ServerPlayer player, String jutsuId) {
        if (JutsuRegistry.get(jutsuId).isEmpty()) {
            LOGGER.warn("[{}] Se pidio entregar el jutsu '{}', que no existe en el registro; se omite.",
                    DojutsuMod.MOD_NAME, jutsuId);
            return false;
        }
        PlayerNinjaData data = NinjaData.get(player);
        if (data.jutsu().knows(jutsuId)) {
            return false; // ya lo sabe: ni item duplicado ni aviso repetido
        }
        data.setJutsu(data.jutsu().withKnown(jutsuId));

        ItemStack stack = JutsuItem.createStack(jutsuId);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false); // inventario lleno: al suelo, no se pierde
        }
        player.sendSystemMessage(Component.translatable("jutsu.dojutsu.granted",
                Component.translatable("jutsu.dojutsu." + jutsuId)));
        LOGGER.info("[{}] {} recibio el jutsu {}.",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName(), jutsuId);
        return true;
    }

    /**
     * Entrega varias tecnicas y sincroniza una sola vez si alguna prendio.
     *
     * @return cuantas se entregaron de verdad
     */
    public static int grantAll(ServerPlayer player, List<String> jutsuIds) {
        int granted = 0;
        for (String jutsuId : jutsuIds) {
            if (grant(player, jutsuId)) {
                granted++;
            }
        }
        if (granted > 0) {
            NinjaSync.sendDelta(player);
        }
        return granted;
    }

    /**
     * Entrega la <b>primera</b> tecnica de la lista que el jugador aun no conozca. Es lo que hace el desbloqueo
     * progresivo de clan del datapack: {@code unless predicate has_<clan>_jutsu run give ...}, o sea una por
     * vuelta del temporizador, no todas de golpe.
     *
     * @return {@code true} si entrego alguna
     */
    public static boolean grantNextUnknown(ServerPlayer player, List<String> jutsuIds) {
        for (String jutsuId : jutsuIds) {
            if (!NinjaData.get(player).jutsu().knows(jutsuId) && grant(player, jutsuId)) {
                NinjaSync.sendDelta(player);
                return true;
            }
        }
        return false;
    }
}
