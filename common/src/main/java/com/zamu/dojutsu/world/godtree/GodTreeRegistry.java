package com.zamu.dojutsu.world.godtree;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.domain.godtree.GodTreeTable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Registro <b>persistente</b> de los Arboles Divinos que estan creciendo {@code [S21]} (F9/DJT-215).
 *
 * <h2>Por que no vale {@code AbilityAnchor}</h2>
 * La tentacion es reusar el ancla de F4, que ya sabe vivir N ticks y ejecutar callbacks. No sirve:
 * {@code AbilityAnchor.shouldBeSaved()} devuelve <b>false</b> —es transitoria por diseño, y sus callbacks son
 * lambdas que no se pueden serializar—. Un arbol tarda {@value GodTreeTable#TOTAL_TICKS} ticks (25 minutos) en
 * florecer: si el servidor se reinicia a mitad, el jugador perderia el premio y los 1000 de chakra que pago. El
 * armor stand del datapack <b>si</b> persiste, asi que perder eso al portar seria una regresion silenciosa.
 *
 * <p>Como {@code TailedBeastRegistry} y {@code TraderSpawnPolicy}, esto es una propiedad de la <b>partida</b>:
 * {@link SavedData} en el overworld.
 *
 * <h2>Crece donde esta, no en el vacio</h2>
 * El datapack tiquea el marcador con {@code as @e[...]}, o sea <b>solo si su chunk esta cargado</b>: alejarse
 * congela el arbol. Se porta igual —{@link GodTreeService} solo avanza los que estan en chunk cargado— porque
 * colocar bloques exige el chunk de todas formas, y cambiarlo regalaria un crecimiento que el original no da.
 * Lo que si se arregla es la <b>persistencia</b>, que es el defecto real.
 *
 * @param pos    donde se planto, con su dimension
 * @param owner  jugador vinculado ({@code d.tree_ply} ↔ {@code d.tree_as}), el unico que recibe el premio
 * @param stage  etapa actual 0..{@value GodTreeTable#MAX_STAGE}
 * @param timer  ticks acumulados hacia la siguiente etapa
 */
public final class GodTreeRegistry extends SavedData {

    /** Un arbol creciendo. Inmutable: crecer produce otro. */
    public record GrowingTree(ResourceKey<Level> dimension, BlockPos pos, UUID owner, int stage, int timer) {

        /** Copia con un tick mas acumulado. */
        public GrowingTree ticked() {
            return new GrowingTree(dimension, pos, owner, stage, timer + 1);
        }

        /** Copia una etapa mas arriba, con el contador reiniciado ({@code grow}: {@code godtree_time = 0}). */
        public GrowingTree grown() {
            return new GrowingTree(dimension, pos, owner, stage + 1, 0);
        }
    }

    private static final String FILE_ID = DojutsuMod.MOD_ID + "_god_trees";
    private static final String KEY_TREES = "trees";
    private static final String KEY_DIMENSION = "dimension";
    private static final String KEY_POS = "pos";
    private static final String KEY_OWNER = "owner";
    private static final String KEY_STAGE = "stage";
    private static final String KEY_TIMER = "timer";

    private final List<GrowingTree> trees = new ArrayList<>();

    private GodTreeRegistry() {
    }

    /** Factory de {@link SavedData} para un mundo nuevo. */
    public static GodTreeRegistry create() {
        return new GodTreeRegistry();
    }

    /** Factory al cargar de NBT; entradas ilegibles se descartan en vez de romper la carga (tolerante). */
    public static GodTreeRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        GodTreeRegistry registry = new GodTreeRegistry();
        ListTag list = tag.getList(KEY_TREES, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ResourceLocation dim = ResourceLocation.tryParse(entry.getString(KEY_DIMENSION));
            if (dim == null || !entry.hasUUID(KEY_OWNER)) {
                continue;
            }
            registry.trees.add(new GrowingTree(
                    ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dim),
                    NbtUtils.readBlockPos(entry, KEY_POS).orElse(BlockPos.ZERO),
                    entry.getUUID(KEY_OWNER),
                    entry.getInt(KEY_STAGE),
                    entry.getInt(KEY_TIMER)));
        }
        return registry;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (GrowingTree tree : trees) {
            CompoundTag entry = new CompoundTag();
            entry.putString(KEY_DIMENSION, tree.dimension().location().toString());
            entry.put(KEY_POS, NbtUtils.writeBlockPos(tree.pos()));
            entry.putUUID(KEY_OWNER, tree.owner());
            entry.putInt(KEY_STAGE, tree.stage());
            entry.putInt(KEY_TIMER, tree.timer());
            list.add(entry);
        }
        tag.put(KEY_TREES, list);
        return tag;
    }

    /** El registro de la partida, creandolo si es la primera vez. */
    public static GodTreeRegistry of(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(GodTreeRegistry::create, GodTreeRegistry::load, null), FILE_ID);
    }

    /** Los arboles creciendo, en copia: mutar la lista solo pasa por esta clase. */
    public List<GrowingTree> trees() {
        return List.copyOf(trees);
    }

    /** Anota un arbol recien plantado. */
    public void plant(GrowingTree tree) {
        trees.add(tree);
        setDirty();
    }

    /** Reemplaza un arbol por su version avanzada (crecido o con un tick mas). */
    public void replace(GrowingTree before, GrowingTree after) {
        int index = trees.indexOf(before);
        if (index >= 0) {
            trees.set(index, after);
            setDirty();
        }
    }

    /** Retira un arbol (florecio y dio su premio, como el {@code kill @s} del marcador). */
    public void remove(GrowingTree tree) {
        if (trees.remove(tree)) {
            setDirty();
        }
    }

    /** El arbol que planto ese jugador, si tiene alguno creciendo ({@code d.tt_planted}). */
    public Optional<GrowingTree> treeOf(UUID owner) {
        return trees.stream().filter(tree -> tree.owner().equals(owner)).findFirst();
    }
}
