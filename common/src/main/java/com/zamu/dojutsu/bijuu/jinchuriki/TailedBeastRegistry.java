package com.zamu.dojutsu.bijuu.jinchuriki;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.domain.bijuu.TailedBeast;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Registro <b>global</b> de quien porta cada Bestia con Cola {@code [S09]} (F7/DJT-165). Persistido como
 * {@link SavedData} del mundo, no en el estado de cada jugador: es una propiedad de la <b>partida</b>, no de
 * nadie en particular.
 *
 * <h2>Por que no basta con el estado del jugador</h2>
 * Es el riesgo <b>S09-R4</b>. El datapack lleva diez <i>slots</i> globales ({@code d.jin_tmp one_tail..ten_tails},
 * cada uno 0 o 1) para saber si <b>alguien</b> ya lleva esa bestia, porque con {@code AllowMultJinchuriki=false}
 * solo puede haber un jinchuriki por bestia en todo el servidor. Esa pregunta —"¿existe ya un jinchuriki del
 * Nueve Colas?"— no se puede responder mirando al jugador que tienes delante, ni recorriendo a los conectados:
 * el portador puede estar desconectado.
 *
 * <p>Se guarda <b>el UUID del portador</b> y no un simple 0/1 como el datapack. Cuesta lo mismo y responde a la
 * pregunta util: no solo <i>si</i> la bestia esta tomada, sino <b>por quien</b> — necesario para el dia en que la
 * extraccion la dispare otro jugador, y suficiente para reconstruir el estado si algo se desincroniza.
 *
 * <p>Los slots viven en el <b>overworld</b> ({@code ServerLevel.OVERWORLD}) porque un {@code SavedData} pertenece
 * a una dimension y el registro debe ser uno solo para toda la partida.
 */
public final class TailedBeastRegistry extends SavedData {

    /** Nombre del fichero dentro de {@code data/} del mundo. */
    private static final String FILE_ID = DojutsuMod.MOD_ID + "_tailed_beasts";

    /** Portador de cada bestia; ausente = nadie la lleva. */
    private final Map<TailedBeast, UUID> carriers = new EnumMap<>(TailedBeast.class);

    private TailedBeastRegistry() {
    }

    /** Factory de {@link SavedData} para un mundo nuevo. */
    public static TailedBeastRegistry create() {
        return new TailedBeastRegistry();
    }

    /** Factory de {@link SavedData} al cargar de NBT; ids desconocidos se descartan (estado tolerante). */
    public static TailedBeastRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        TailedBeastRegistry registry = new TailedBeastRegistry();
        for (TailedBeast beast : TailedBeast.values()) {
            if (tag.hasUUID(beast.id())) {
                registry.carriers.put(beast, tag.getUUID(beast.id()));
            }
        }
        return registry;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        carriers.forEach((beast, carrier) -> tag.putUUID(beast.id(), carrier));
        return tag;
    }

    /** El registro de la partida, creandolo si es la primera vez. */
    public static TailedBeastRegistry of(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TailedBeastRegistry::create, TailedBeastRegistry::load, null), FILE_ID);
    }

    /** Portador actual de esa bestia, o vacio si esta libre. */
    public Optional<UUID> carrierOf(TailedBeast beast) {
        return Optional.ofNullable(carriers.get(beast));
    }

    /** Indica si alguien ya lleva esa bestia. */
    public boolean isTaken(TailedBeast beast) {
        return carriers.containsKey(beast);
    }

    /** Bestia que lleva ese jugador, o vacio. Barrido corto: como mucho hay diez entradas. */
    public Optional<TailedBeast> beastOf(UUID player) {
        for (Map.Entry<TailedBeast, UUID> entry : carriers.entrySet()) {
            if (entry.getValue().equals(player)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    /** Apunta que ese jugador pasa a portar la bestia. */
    public void claim(TailedBeast beast, UUID player) {
        carriers.put(beast, player);
        setDirty();
    }

    /** Libera la bestia (extraccion). Idempotente. */
    public void release(TailedBeast beast) {
        if (carriers.remove(beast) != null) {
            setDirty();
        }
    }

    /** Numero de bestias con portador (para el comando de QA). */
    public int taken() {
        return carriers.size();
    }
}
