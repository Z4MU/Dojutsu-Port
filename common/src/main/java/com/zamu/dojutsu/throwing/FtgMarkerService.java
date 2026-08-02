package com.zamu.dojutsu.throwing;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.core.PlayerTickSystem;
import com.zamu.dojutsu.core.TickPhase;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.data.state.ThrowingState;
import com.zamu.dojutsu.domain.throwing.FtgTable;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.progression.XpApi;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * <b>Vuelo del Dios del Trueno</b> {@code [S16]} (F8/DJT-180): la marca de Minato y el salto hacia ella.
 *
 * <h2>La marca no depende del chunk, y eso mata el forceload</h2>
 * En el datapack la marca <b>es</b> el armor stand clavado, asi que un chunk descargado se la lleva. El parche del
 * autor es <b>{@code forceload add}</b>: cada marca <b>mantiene un chunk cargado indefinidamente</b>, y si algo
 * mata el kunai por otra via el forceload queda huerfano para siempre.
 *
 * <p>Aqui la marca son <b>coordenadas en el estado del jugador</b> ({@link GlobalPos}, en el Attachment). No
 * depende de ningun chunk, funciona <b>entre dimensiones</b> por construccion y no hay nada que liberar. El kunai
 * clavado pasa a ser la parte <i>visible</i> de la marca, no la marca.
 *
 * <h2>Una sola marca, de un solo uso</h2>
 * Marcar otra vez <b>reemplaza</b> la anterior ({@code reset} mata el kunai viejo) y saltar la <b>consume</b>
 * ({@code kill @s} tras el {@code tp}). No es una red de puntos de viaje: es una salida de emergencia que
 * preparas y gastas.
 *
 * <h2>Dominarlo lo abarata cinco veces</h2>
 * De {@value FtgTable#CHAKRA_NOVICE} de chakra por salto a {@value FtgTable#CHAKRA_MASTER} en el nivel 21. Y el
 * <b>clan Namikaze</b> —el de Minato— aprende al doble de velocidad.
 *
 * <p>Este servicio ademas tickea el veto de {@link ThrowingService}: ya recorre a los jugadores.
 */
public final class FtgMarkerService implements PlayerTickSystem {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Veto entre saltos por jugador ({@code d.ftg_cd}); ejecucion, no guardado. */
    private static final Map<UUID, Integer> COOLDOWNS = new ConcurrentHashMap<>();

    @Override
    public TickPhase phase() {
        return TickPhase.CHAKRA;
    }

    @Override
    public void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        int cooldown = COOLDOWNS.getOrDefault(id, 0);
        if (cooldown > 0) {
            COOLDOWNS.put(id, cooldown - 1);
        }
        ThrowingService.tickCooldown(id);
    }

    /** Por que no se pudo saltar. */
    public enum TeleportOutcome {
        /** Salto realizado (y la marca consumida). */
        TELEPORTED,
        /** No hay nada marcado. */
        NO_MARK,
        /** Fuinjutsu insuficiente. */
        LOW_FUINJUTSU,
        /** Chakra insuficiente para su nivel de tecnica. */
        NO_CHAKRA,
        /** Aun corre el veto. */
        COOLDOWN,
        /** La dimension de la marca ya no existe (mundo modificado). */
        LOST_DIMENSION
    }

    /**
     * Marca un punto, reemplazando la marca anterior ({@code reset} + el kunai clavado).
     *
     * <p>El {@code forceload} del datapack no tiene equivalente aqui <b>a proposito</b>: la marca vive en el
     * estado, no en el mundo.
     */
    public static void mark(ServerPlayer player, ServerLevel level, Vec3 at) {
        PlayerNinjaData data = NinjaData.get(player);
        GlobalPos mark = GlobalPos.of(level.dimension(), BlockPos.containing(at));
        data.setThrowing(data.throwing().withMark(mark));
        player.displayClientMessage(Component.translatable("ftg.dojutsu.marked",
                mark.pos().getX(), mark.pos().getY(), mark.pos().getZ()).withStyle(ChatFormatting.YELLOW), true);
        level.playSound(null, BlockPos.containing(at), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.6f, 1.6f);
    }

    /**
     * Salta a la marca: la consume y deja al jugador {@value FtgTable#TELEPORT_Y_OFFSET} bloques por encima.
     *
     * @return el resultado; solo {@link TeleportOutcome#TELEPORTED} mueve al jugador
     */
    public static TeleportOutcome teleport(ServerPlayer player) {
        UUID id = player.getUUID();
        if (COOLDOWNS.getOrDefault(id, 0) > 0) {
            return TeleportOutcome.COOLDOWN;
        }
        PlayerNinjaData data = NinjaData.get(player);
        ThrowingState state = data.throwing();
        Optional<GlobalPos> mark = state.ftgMark();
        if (mark.isEmpty()) {
            fail(player, "ftg.dojutsu.no_mark");
            return TeleportOutcome.NO_MARK;
        }
        if (!FtgTable.canTeleport(data.progression().skill(Skill.FUINJUTSU).level())) {
            fail(player, "ftg.dojutsu.low_fuinjutsu");
            return TeleportOutcome.LOW_FUINJUTSU;
        }
        int cost = FtgTable.chakraCost(state.ftgLevel());
        if (!ChakraService.consume(data, cost)) {
            fail(player, "ftg.dojutsu.no_chakra");
            return TeleportOutcome.NO_CHAKRA;
        }
        ServerLevel target = player.getServer().getLevel(mark.get().dimension());
        if (target == null) {
            fail(player, "ftg.dojutsu.lost_dimension");
            data.setThrowing(state.withoutMark()); // la marca apunta a ninguna parte: se descarta
            return TeleportOutcome.LOST_DIMENSION;
        }

        BlockPos pos = mark.get().pos();
        flash(player); // el destello se ve en el origen...
        player.teleportTo(target, pos.getX() + 0.5, pos.getY() + FtgTable.TELEPORT_Y_OFFSET, pos.getZ() + 0.5,
                player.getYRot(), player.getXRot());
        flash(player); // ...y en el destino
        data.setThrowing(state.withoutMark()); // saltar CONSUME la marca
        awardXp(player);
        COOLDOWNS.put(id, FtgTable.COOLDOWN_TICKS);
        player.displayClientMessage(Component.translatable("ftg.dojutsu.performed")
                .withStyle(ChatFormatting.GOLD), true);
        NinjaSync.sendDelta(player);
        return TeleportOutcome.TELEPORTED;
    }

    /** Marca actual del jugador, si tiene alguna (comando de QA). */
    public static Optional<GlobalPos> markOf(ServerPlayer player) {
        return NinjaData.get(player).throwing().ftgMark();
    }

    /** Registra el payload C2S del salto. Llamar en la inicializacion del mod. */
    public static void register() {
        com.zamu.dojutsu.platform.PlatformServices.NETWORK.register(
                com.zamu.dojutsu.platform.NetworkService.Side.C2S,
                com.zamu.dojutsu.network.FtgTeleportPayload.TYPE,
                com.zamu.dojutsu.network.FtgTeleportPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        teleport(player);
                    }
                }));
    }

    /** Olvida el veto (al desconectar). */
    public static void forget(UUID playerId) {
        COOLDOWNS.remove(playerId);
        ThrowingService.forget(playerId);
    }

    /** XP de la tecnica: +1 por salto, o <b>+2</b> siendo Namikaze; 10 por nivel, cap 30 ({@code xp}). */
    private static void awardXp(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        XpApi.addSkillXp(player, Skill.FUINJUTSU, 1);
        ThrowingState state = data.throwing();
        if (state.ftgLevel() >= FtgTable.MAX_LEVEL) {
            return;
        }
        int xp = state.ftgXp() + FtgTable.xpPerUse(data.clan().clan().orElse(null));
        if (!FtgTable.levelsUp(state.ftgLevel(), xp)) {
            data.setThrowing(state.withFtgXp(xp));
            return;
        }
        ThrowingState raised = state.withFtgXp(xp).withFtgLevelUp();
        data.setThrowing(raised);
        player.sendSystemMessage(Component.translatable("ftg.dojutsu.improved").withStyle(ChatFormatting.GOLD));
        LOGGER.info("[{}] El Vuelo del Dios del Trueno de {} sube a nivel {} (coste {} de chakra).",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName(), raised.ftgLevel(),
                FtgTable.chakraCost(raised.ftgLevel()));
    }

    /** Destello del salto. */
    private static void flash(ServerPlayer player) {
        if (player.level() instanceof ServerLevel level) {
            Vec3 pos = player.position();
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 1, pos.z, 30, 0.4, 0.8, 0.4, 0.2);
            level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.PLAYERS, 0.3f, 2f);
        }
    }

    /** Intento fallido: aviso y <b>el doble</b> de veto ({@code d.ftg_cd = 40}). */
    private static void fail(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_RED), true);
        player.playNotifySound(SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.3f, 1f);
        COOLDOWNS.put(player.getUUID(), FtgTable.FAILED_COOLDOWN_TICKS);
    }
}
