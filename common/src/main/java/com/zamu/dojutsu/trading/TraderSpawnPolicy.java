package com.zamu.dojutsu.trading;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.core.config.DojutsuConfig;
import com.zamu.dojutsu.domain.trading.TradingTable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

/**
 * Ritmo propio de aparicion del mercader errante {@code [S18]} (F8/DJT-190). Persistido como {@link SavedData}
 * del overworld: cuantos intentos se han hecho hoy es propiedad de la <b>partida</b>, no de nadie.
 *
 * <h2>La config dice oportunidades por dia, no intervalo</h2>
 * {@code trader/try} programa el siguiente intento a {@value TradingTable#DAY_TICKS}{@code / rate} ticks. La tabla
 * de 24 lineas del datapack es esa division escrita a mano, valor a valor; aqui es la division.
 *
 * <h2>Y la probabilidad va al reves</h2>
 * {@code traderspawnchance} tira en {@code [chance, 10]} y gana el 10, asi que {@code chance = 10} es
 * <b>certeza</b> y {@code chance = 1} es una de cada diez. Portarlo del reves seria un error silencioso, asi que
 * la direccion vive en {@link TradingTable#rollSpawn} con test propio.
 *
 * <p><b>Solo actua si {@code useTraderSpawnRate} esta activo</b>; si no, manda el ritmo de Minecraft y este
 * servicio no hace nada — que es exactamente lo que declara la config.
 */
public final class TraderSpawnPolicy extends SavedData {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String FILE_ID = DojutsuMod.MOD_ID + "_trader_policy";

    /** Intentos hechos en el dia en curso ({@code times d.trader_tmp}). */
    private int attemptsToday;

    /** Tick de mundo del ultimo intento; con el se sabe cuando toca el siguiente. */
    private long lastAttemptTick;

    private TraderSpawnPolicy() {
    }

    /** Factory de {@link SavedData} para un mundo nuevo. */
    public static TraderSpawnPolicy create() {
        return new TraderSpawnPolicy();
    }

    /** Factory de {@link SavedData} al cargar de NBT. */
    public static TraderSpawnPolicy load(CompoundTag tag, HolderLookup.Provider registries) {
        TraderSpawnPolicy policy = new TraderSpawnPolicy();
        policy.attemptsToday = tag.getInt("attemptsToday");
        policy.lastAttemptTick = tag.getLong("lastAttemptTick");
        return policy;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("attemptsToday", attemptsToday);
        tag.putLong("lastAttemptTick", lastAttemptTick);
        return tag;
    }

    /** La politica de la partida, creandola si es la primera vez. */
    public static TraderSpawnPolicy of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TraderSpawnPolicy::create, TraderSpawnPolicy::load, null), FILE_ID);
    }

    /**
     * Comprueba si toca intentar la aparicion y, si toca, tira los dados.
     *
     * @return {@code true} si el mercader debe aparecer <b>ahora</b>
     */
    public boolean tryAttempt(ServerLevel level) {
        DojutsuConfig config = DojutsuConfig.get();
        if (!config.useTraderSpawnRate()) {
            return false; // sin ritmo propio: manda Minecraft
        }
        int interval = TradingTable.attemptIntervalTicks(config.traderSpawnRate());
        if (interval <= 0) {
            return false;
        }
        long now = level.getGameTime();
        long day = now / TradingTable.DAY_TICKS;
        if (day != lastAttemptTick / TradingTable.DAY_TICKS) {
            attemptsToday = 0; // dia nuevo: el contador vuelve a cero (trader/reset)
        }
        if (now - lastAttemptTick < interval) {
            return false;
        }
        lastAttemptTick = now;
        attemptsToday++;
        setDirty();
        int drawn = level.getRandom().nextInt(TradingTable.spawnRollSpan(config.traderSpawnChance()));
        boolean spawn = TradingTable.isSpawnRoll(config.traderSpawnChance(), drawn);
        if (spawn) {
            LOGGER.debug("[{}] La politica de mercader dice que aparezca (intento {} del dia).",
                    DojutsuMod.MOD_NAME, attemptsToday);
        }
        return spawn;
    }

    /** Intentos hechos hoy (comando de QA). */
    public int attemptsToday() {
        return attemptsToday;
    }

    /** Tick del ultimo intento (comando de QA). */
    public long lastAttemptTick() {
        return lastAttemptTick;
    }
}
