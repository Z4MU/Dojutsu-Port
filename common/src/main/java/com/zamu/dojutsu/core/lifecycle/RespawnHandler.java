package com.zamu.dojutsu.core.lifecycle;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.empowerment.cursedseal.CursedSealService;
import com.zamu.dojutsu.empowerment.eightgates.EightGatesService;
import com.zamu.dojutsu.bijuu.jinchuriki.FormService;
import com.zamu.dojutsu.empowerment.soah.SoahSealService;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.sage.SageService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

/**
 * Restablece al jugador tras respawnear ({@code main/respawn} del datapack): desactivar todos los modos
 * activos, recalcular bonos y sincronizar.
 *
 * <p>Se suscribe a {@code PlayerEvent.PLAYER_RESPAWN} desde {@link com.zamu.dojutsu.core.DojutsuLifecycle}.
 * Nacio en F1 como <b>hook</b> vacio; desde F6/DJT-145 apaga los tres modos de potenciacion que ya existen
 * (puertas, modo sabio, sello maldito). Quedan las formas de jinchuriki (F7), la velocidad base del namikaze y
 * el bonus de taijutsu. El estado ninja en si persiste al respawn (copyOnDeath, DJT-019).
 */
public final class RespawnHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private RespawnHandler() {
    }

    /**
     * Handler de {@code PlayerEvent.PLAYER_RESPAWN}.
     *
     * @param newPlayer    la nueva entidad del jugador tras respawnear
     * @param conqueredEnd {@code true} si el respawn es por salir del End (no por muerte)
     * @param reason       motivo de retirada de la entidad anterior
     */
    public static void onRespawn(ServerPlayer newPlayer, boolean conqueredEnd, Entity.RemovalReason reason) {
        // main/respawn: los modos activos se apagan al reaparecer. Los tres de F6 ya existen; las puertas usan
        // el cierre SEGURO (safe_deactivate): sin coste de cierre, porque la fatiga ya se limpio al morir.
        // TODO F7+: formas de jinchuriki, velocidad base (namikaze) y bonus de taijutsu.
        EightGatesService.closeSafely(newPlayer);       // F6/DJT-145
        SageService.forget(newPlayer.getUUID());        // F6/DJT-136
        CursedSealService.forget(newPlayer.getUUID());  // F6/DJT-140
        SoahSealService.forget(newPlayer.getUUID());    // F6/DJT-150 (la reserva del sello sobrevive)
        FormService.forget(newPlayer.getUUID());        // F7/DJT-166 (la bestia sellada sobrevive)
        LOGGER.info("[{}] Respawn de {}: modos de potenciacion apagados.",
                DojutsuMod.MOD_NAME, newPlayer.getGameProfile().getName());
        NinjaSync.sendFull(newPlayer); // re-sync completo tras respawnear
    }
}
