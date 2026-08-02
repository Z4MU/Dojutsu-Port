package com.zamu.dojutsu.core.lifecycle;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.dojutsu.EyeStrainService;
import com.zamu.dojutsu.empowerment.eightgates.EightGatesService;
import dev.architectury.event.EventResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import org.slf4j.Logger;

/**
 * Aplica la penalizacion de muerte del jugador ({@code main/death} del datapack): en supervivencia,
 * {@code chakra = maxChakra/2} y reset de strain (eye/eight-gates).
 *
 * <p>Se suscribe a {@code EntityEvent.LIVING_DEATH} desde {@link com.zamu.dojutsu.core.DojutsuLifecycle} y
 * filtra a jugadores. La penalizacion de chakra ({@code chakra = max/2}) se aplica sobre el jugador que muere
 * y se propaga al respawn via {@code copyOnDeath} (DJT-019); {@code RespawnHandler} sincroniza al cliente. El
 * reset de fatiga (eye/eight-gates strain) queda como hook hasta que existan esos sub-estados (F5/F6).
 */
public final class DeathHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private DeathHandler() {
    }

    /**
     * Indica si la penalizacion de muerte aplica en un modo de juego dado: solo cuando el jugador recibe
     * dano de gameplay (SURVIVAL o ADVENTURE), nunca en CREATIVE ni SPECTATOR.
     */
    public static boolean appliesPenalty(GameType gameMode) {
        return gameMode == GameType.SURVIVAL || gameMode == GameType.ADVENTURE;
    }

    /** Handler de {@code EntityEvent.LIVING_DEATH}. Solo actua sobre jugadores en modo penalizable. */
    public static EventResult onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof ServerPlayer player)) {
            return EventResult.pass(); // ignora mobs
        }
        if (!appliesPenalty(player.gameMode.getGameModeForPlayer())) {
            return EventResult.pass(); // creativo/espectador: sin penalizacion
        }

        // main/death: chakra = max/2 (mitad del MAXIMO, no del actual). Se propaga al respawn via copyOnDeath.
        PlayerNinjaData data = NinjaData.get(player);
        ChakraService.set(data, data.chakra().maxChakra() / 2);
        // main/death: las dos fatigas se ponen a 0 al morir. Deuda de F5/F6 saldada en DJT-145, cuando existen
        // los dos sub-estados. Falta aun limpiar mrpills (items, F8).
        EyeStrainService.resetStrain(data);
        EightGatesService.resetStrain(data);
        LOGGER.info("[{}] Muerte de {}: chakra reducido a la mitad del maximo.",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName());
        return EventResult.pass();
    }
}
