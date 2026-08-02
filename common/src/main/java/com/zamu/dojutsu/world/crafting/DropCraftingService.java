package com.zamu.dojutsu.world.crafting;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.domain.crafting.DropRecipe;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/**
 * <b>Drop-crafting</b> {@code [S23]} (F9/DJT-206): combina items soltados juntos en el suelo. Porta
 * {@code main/internal/crafting.mcfunction} y sus 16 funciones de resultado.
 *
 * <h2>Que hace y que no</h2>
 * Busca entidades item proximas entre si, casa el <b>conjunto</b> contra el recetario
 * ({@link DropCraftingRegistry}), consume las entradas, lanza particulas {@code crit} y suelta el resultado. Lo
 * que <b>no</b> hace es cubrir todo el crafteo: lo que cabe en una mesa 3x3 son recetas nativas
 * ({@code data/dojutsu/recipe/}), y aqui solo queda lo que no cabe.
 *
 * <h2>RB-2: el radio de 0.3 del datapack es una trampa</h2>
 * El datapack exige {@code distance=0..0.3}. Un item recien soltado <b>rebota</b>, y con dos jugadores o un
 * terreno irregular las pilas acaban a medio bloque unas de otras: la receta simplemente no salta y el jugador
 * no sabe por que. Aqui el radio es {@value #DROP_RADIUS} bloques, que es lo que ocupa un puñado de items
 * soltados de golpe, y el emparejamiento es <b>por conjunto</b> (una entidad distinta por ingrediente), no por
 * pares encadenados.
 *
 * <p>Lo que <b>no</b> se afloja es la <b>cantidad de pila</b>: es lo unico que distingue las tres mascaras ANBU
 * entre si ({@link DropRecipe}).
 *
 * <h2>Coste</h2>
 * El datapack corre esto <b>cada tick</b> sobre <b>todas</b> las entidades item del mundo. Aqui se barre cada
 * {@value #SCAN_INTERVAL_TICKS} ticks y solo alrededor de los jugadores ({@value #PLAYER_SCAN_RADIUS} bloques):
 * items en un chunk cargado sin nadie cerca no craftean, que es justo lo que nadie va a notar.
 */
public final class DropCraftingService {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Radio de emparejamiento, en bloques (datapack {@code 0..0.3}; ampliado por RB-2). */
    public static final double DROP_RADIUS = 1.5D;

    /** Cada cuantos ticks se barre (datapack: cada tick). */
    public static final int SCAN_INTERVAL_TICKS = 5;

    /** Radio alrededor de cada jugador donde se buscan items, en bloques. */
    public static final double PLAYER_SCAN_RADIUS = 16.0D;

    /** Particulas del crafteo ({@code particle crit ~ ~0.2 ~ .2 .5 .2 0 20 force}). */
    private static final int PARTICLE_COUNT = 20;

    private static long ticks;

    private DropCraftingService() {
    }

    /** Barrido periodico. Registrado en {@code TickEvent.SERVER_POST}. */
    public static void tick(MinecraftServer server) {
        if (++ticks % SCAN_INTERVAL_TICKS != 0 || DropCraftingRegistry.size() == 0) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (!level.players().isEmpty()) {
                scan(level);
            }
        }
    }

    /** Reinicia el contador entre partidas (cada mundo tiene su propio tiempo). */
    public static void reset() {
        ticks = 0;
    }

    private static void scan(ServerLevel level) {
        List<ItemEntity> candidates = gather(level);
        if (candidates.size() < 2) {
            return;
        }
        Set<ItemEntity> consumed = new HashSet<>();
        for (ItemEntity anchor : candidates) {
            if (consumed.contains(anchor)) {
                continue;
            }
            List<ItemEntity> cluster = clusterAround(anchor, candidates, consumed);
            if (cluster.size() < 2) {
                continue;
            }
            craft(level, anchor, cluster).ifPresent(consumed::addAll);
        }
    }

    /** Items alrededor de los jugadores, sin repetir (dos jugadores cerca ven los mismos). */
    private static List<ItemEntity> gather(ServerLevel level) {
        Map<Integer, ItemEntity> unique = new LinkedHashMap<>();
        for (ServerPlayer player : level.players()) {
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class,
                    player.getBoundingBox().inflate(PLAYER_SCAN_RADIUS), ItemEntity::isAlive)) {
                unique.putIfAbsent(item.getId(), item);
            }
        }
        return new ArrayList<>(unique.values());
    }

    /** El ancla y todo lo que tenga a {@value #DROP_RADIUS} bloques, sin lo ya consumido en este barrido. */
    private static List<ItemEntity> clusterAround(ItemEntity anchor, List<ItemEntity> candidates,
            Set<ItemEntity> consumed) {
        List<ItemEntity> cluster = new ArrayList<>();
        cluster.add(anchor);
        double radiusSq = DROP_RADIUS * DROP_RADIUS;
        for (ItemEntity other : candidates) {
            if (other != anchor && !consumed.contains(other) && other.distanceToSqr(anchor) <= radiusSq) {
                cluster.add(other);
            }
        }
        return cluster;
    }

    /**
     * Prueba el recetario contra un grupo de items. Las recetas vienen ordenadas de mas especifica a menos, asi
     * que la primera que casa es la buena.
     *
     * @return las entidades consumidas, o vacio si no casa ninguna receta
     */
    private static Optional<List<ItemEntity>> craft(ServerLevel level, ItemEntity anchor, List<ItemEntity> cluster) {
        List<DropRecipe.Stack> stacks = cluster.stream()
                .map(entity -> new DropRecipe.Stack(idOf(entity.getItem()), entity.getItem().getCount()))
                .toList();
        for (DropCraftingRegistry.Entry entry : DropCraftingRegistry.all()) {
            Optional<Item> result = BuiltInRegistries.ITEM
                    .getOptional(ResourceLocation.parse(entry.recipe().result()));
            if (result.isEmpty()) {
                continue; // receta declarada cuyo item aun no existe (los 9 huevos de bestia)
            }
            Optional<List<Integer>> matched = entry.recipe().match(stacks);
            if (matched.isEmpty()) {
                continue;
            }
            List<ItemEntity> used = matched.get().stream().map(cluster::get).toList();
            used.forEach(ItemEntity::discard); // el kill @e del datapack
            level.sendParticles(ParticleTypes.CRIT, anchor.getX(), anchor.getY() + 0.2D, anchor.getZ(),
                    PARTICLE_COUNT, 0.2D, 0.5D, 0.2D, 0.0D);

            ItemStack output = new ItemStack(result.get(), entry.recipe().resultCount());
            ItemEntity spawned = new ItemEntity(level, anchor.getX(), anchor.getY(), anchor.getZ(), output,
                    0.0D, 0.0D, 0.0D);
            spawned.setDefaultPickUpDelay(); // el summon item del datapack cae y se recoge, no se teletransporta al jugador
            level.addFreshEntity(spawned);

            LOGGER.debug("[{}] Drop-crafting: {} -> {}", DojutsuMod.MOD_NAME, entry.id(), entry.recipe().result());
            return Optional.of(used);
        }
        return Optional.empty();
    }

    /** Id de registro de la pila ({@code "minecraft:oak_planks"}). */
    private static String idOf(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
