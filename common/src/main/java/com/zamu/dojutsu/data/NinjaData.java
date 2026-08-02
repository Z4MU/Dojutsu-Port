package com.zamu.dojutsu.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.zamu.dojutsu.data.migration.MigrationChain;
import com.zamu.dojutsu.registry.ModAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

/**
 * Fachada de acceso al estado ninja por jugador ({@link PlayerNinjaData}).
 *
 * <p>Expone {@link #get(Player)} (getOrCreate: crea defaults en el primer acceso) y el
 * {@link #PERSISTENT_CODEC} con el que {@link ModAttachments} registra el attachment real. La migracion
 * se aplica <b>al decodificar</b> (antes del {@code PlayerNinjaData.CODEC}), de forma transparente en ambos
 * loaders.
 */
public final class NinjaData {

    private static final MigrationChain MIGRATIONS = MigrationChain.forCurrentSchema();

    /**
     * Codec de persistencia: al decodificar, migra el NBT a la version actual y luego lo decodifica; al
     * codificar, delega en {@link PlayerNinjaData#CODEC} (escribe la version actual). Es NBT-especifico
     * (convierte cualquier ops a NBT) porque la migracion opera sobre {@code CompoundTag}.
     */
    public static final Codec<PlayerNinjaData> PERSISTENT_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<PlayerNinjaData, T>> decode(DynamicOps<T> ops, T input) {
            Tag tag = ops.convertTo(NbtOps.INSTANCE, input);
            CompoundTag compound = tag instanceof CompoundTag c ? c : new CompoundTag();
            CompoundTag migrated = MIGRATIONS.migrate(compound);
            return PlayerNinjaData.CODEC.parse(NbtOps.INSTANCE, migrated).map(data -> Pair.of(data, input));
        }

        @Override
        public <T> DataResult<T> encode(PlayerNinjaData input, DynamicOps<T> ops, T prefix) {
            return PlayerNinjaData.CODEC.encode(input, ops, prefix);
        }

        @Override
        public String toString() {
            return "PlayerNinjaData.PERSISTENT_CODEC";
        }
    };

    private NinjaData() {
    }

    /**
     * Devuelve el estado ninja del jugador, creando los defaults en el primer acceso. El estado cargado ya
     * viene migrado a la version actual (via {@link #PERSISTENT_CODEC}).
     */
    public static PlayerNinjaData get(Player player) {
        return ModAttachments.PLAYER_NINJA_DATA.get(player);
    }

    /**
     * Resetea el estado ninja del jugador reemplazandolo por una <b>instancia nueva</b> con defaults (reset
     * total, sin reset campo-a-campo: es imposible olvidar un campo o quedar desincronizado). Corrige S01-R2.
     */
    public static void reset(Player player) {
        ModAttachments.PLAYER_NINJA_DATA.set(player, new PlayerNinjaData());
    }
}
