package com.zamu.dojutsu.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Reproduccion de sonido del mod (F10/DJT-227): el equivalente del {@code playsound} del datapack, con su
 * <b>alcance</b> y su <b>puerta anti-spam</b>.
 *
 * <h2>Por que no vale {@code Level.playSound}</h2>
 * Es la razon de que esta clase exista. El {@code playsound} del datapack tiene <b>dos</b> mandos que deciden
 * quien oye algo: el volumen y el selector ({@code @a[distance=0..N]}). En vanilla solo hay uno — el radio de
 * difusion es {@code max(16, 16 x volumen)} y {@code Level.playSound} no deja tocarlo—, asi que los sonidos
 * fuertes se irian mucho mas lejos de lo que el datapack queria: {@code medical_ninjutsu.healing} suena a
 * <b>volumen 10 en 5 bloques</b>, y por la via vanilla se oiria a <b>160</b>. Un factor treinta y dos.
 *
 * <p>Por eso se envia el {@link ClientboundSoundPacket} a mano a los jugadores dentro del radio: el volumen
 * queda para lo que es —cuanto suena— y el alcance lo pone el selector, como en el original.
 *
 * <h2>La puerta {@code d.sfx}</h2>
 * El datapack lleva un contador <b>por jugador</b> ({@code d.sfx}, {@code main/tick} le resta 1 por tick) y
 * catorce llamadas se saltan si sigue vivo. No es un cooldown de la tecnica: es un cooldown <b>del sonido</b>,
 * para que castear en rafaga no solape treinta copias del mismo audio. Es <b>uno solo y compartido</b>, no uno
 * por sonido — encadenar dos habilidades distintas dentro de la ventana deja muda a la segunda, y asi era.
 *
 * <p>Aqui se guarda el <b>tick en el que vuelve a estar permitido</b> en vez de un contador que baje solo:
 * misma semantica y cero trabajo por tick (regla 11), el mismo idioma que los cooldowns de
 * {@code JutsuKnowledge}.
 */
public final class NinjaSfx {

    /**
     * Ventana por defecto de la puerta, en ticks. Es la que usan las catorce llamadas gateadas del datapack
     * ({@code scoreboard players set @s d.sfx 20}); las de sabio y las de jutsu con su propio ritmo declaran la
     * suya (8, 5, 3, 2).
     */
    public static final int GATE_TICKS = 20;

    /** Alcance del selector mas frecuente, {@code @a[distance=0..10]}. */
    public static final double RANGE_10 = 10.0D;

    /** Tick de mundo en el que cada jugador puede volver a disparar un sonido gateado. */
    private static final Map<UUID, Long> READY_AT_TICK = new ConcurrentHashMap<>();

    private NinjaSfx() {
    }

    /**
     * {@code playsound <sonido> <fuente> @a[distance=0..<alcance>] <x> <y> <z> <volumen> <tono>}.
     *
     * @param range alcance del selector original, en bloques
     */
    public static void at(ServerLevel level, double x, double y, double z, SoundEvent sound, SoundSource source,
            float volume, float pitch, double range) {
        Holder<SoundEvent> holder = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
        long seed = level.getRandom().nextLong();
        double rangeSqr = range * range;
        ClientboundSoundPacket packet =
                new ClientboundSoundPacket(holder, source, x, y, z, volume, pitch, seed);
        for (ServerPlayer listener : level.players()) {
            if (listener.distanceToSqr(x, y, z) <= rangeSqr) {
                listener.connection.send(packet);
            }
        }
    }

    /** El caso mas comun: en la posicion del jugador que lo provoca. */
    public static void at(ServerPlayer player, SoundEvent sound, SoundSource source, float volume, float pitch,
            double range) {
        if (player.level() instanceof ServerLevel level) {
            at(level, player.getX(), player.getY(), player.getZ(), sound, source, volume, pitch, range);
        }
    }

    /**
     * El {@code @s} del datapack: <b>solo lo oye ese jugador</b>, en su propia posicion. Lo usan el pitido de
     * oidos de la Tecnica de la Ira Blanca (lo sufre quien la recibe, no quien la lanza) y los avisos de ojo.
     */
    public static void toPlayer(ServerPlayer player, SoundEvent sound, SoundSource source, float volume,
            float pitch) {
        Holder<SoundEvent> holder = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
        long seed = player.level().getRandom().nextLong();
        player.connection.send(new ClientboundSoundPacket(holder, source, player.getX(), player.getY(),
                player.getZ(), volume, pitch, seed));
    }

    /**
     * Consulta y arma la puerta compartida: {@code unless predicate dojutsu:sfx_delay run ... ; run scoreboard
     * players set @s d.sfx <ticks>}.
     *
     * @return {@code true} si toca sonar (y deja la puerta armada); {@code false} si sigue cerrada
     */
    public static boolean pass(ServerPlayer player, int gateTicks) {
        long now = player.serverLevel().getGameTime();
        Long readyAt = READY_AT_TICK.get(player.getUUID());
        if (readyAt != null && now < readyAt) {
            return false;
        }
        READY_AT_TICK.put(player.getUUID(), now + gateTicks);
        return true;
    }

    /** Igual que {@link #pass(ServerPlayer, int)} con la ventana por defecto de 20 ticks. */
    public static boolean pass(ServerPlayer player) {
        return pass(player, GATE_TICKS);
    }

    /** Decision pura de la puerta, para poder probarla sin mundo. */
    public static boolean isOpen(long readyAtTick, long nowTick) {
        return nowTick >= readyAtTick;
    }

    /** Olvida la puerta de un jugador (al desconectar). */
    public static void forget(UUID playerId) {
        READY_AT_TICK.remove(playerId);
    }

    /** Olvida todas las puertas: los ticks son del mundo que se cierra y no cruzan partidas. */
    public static void clear() {
        READY_AT_TICK.clear();
    }
}
