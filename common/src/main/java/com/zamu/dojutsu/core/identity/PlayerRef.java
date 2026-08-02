package com.zamu.dojutsu.core.identity;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Identidad estable de un jugador (o dueño de entidad) por su {@link UUID} nativo.
 *
 * <p>Reemplaza el {@code player_id 1..10} del datapack ({@code assign_plys}, {@code reset_players},
 * {@code player_cnt}) y su limite duro de 10 jugadores. Guarda <b>solo el UUID</b> (nunca una referencia
 * dura al {@code Player}, para no fugar memoria); la resolucion a un jugador conectado devuelve un
 * {@link Optional} vacio si esta desconectado, sin asumir que esta online.
 *
 * <p>Capa {@code core}: puede usar tipos de Minecraft. Sera la base del "owner" de entidades en F4.
 *
 * @param uuid identificador unico del jugador
 */
public record PlayerRef(UUID uuid) {

    public PlayerRef {
        Objects.requireNonNull(uuid, "uuid");
    }

    /** Crea una referencia a partir de un UUID. */
    public static PlayerRef of(UUID uuid) {
        return new PlayerRef(uuid);
    }

    /** Crea una referencia a partir de un jugador (toma su UUID; no retiene la instancia). */
    public static PlayerRef of(Player player) {
        return new PlayerRef(player.getUUID());
    }

    /**
     * Resuelve la referencia con una funcion de busqueda por UUID. Devuelve vacio si la funcion no
     * encuentra nada (p. ej. jugador desconectado). Util para testear sin un servidor real.
     */
    public <T> Optional<T> resolve(Function<UUID, T> lookup) {
        Objects.requireNonNull(lookup, "lookup");
        return Optional.ofNullable(lookup.apply(uuid));
    }

    /** Resuelve al {@link ServerPlayer} conectado, o vacio si no esta online. */
    public Optional<ServerPlayer> resolve(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return Optional.ofNullable(server.getPlayerList().getPlayer(uuid));
    }

    /** Indica si el jugador esta conectado en el servidor dado. */
    public boolean isOnline(MinecraftServer server) {
        return resolve(server).isPresent();
    }
}
