package com.zamu.dojutsu.world.godtree;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.core.config.DojutsuConfig;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.domain.godtree.GodTreeTable;
import com.zamu.dojutsu.dojutsu.AwakeningService;
import com.zamu.dojutsu.dojutsu.EyeSounds;
import com.zamu.dojutsu.registry.ModSounds;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.world.godtree.GodTreeRegistry.GrowingTree;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;

/**
 * El <b>Arbol Divino</b> {@code [S21]} (F9/DJT-215): plantarlo, verlo crecer y florecer en el Rinne Sharingan.
 * Es el nodo final de la migracion.
 *
 * <h2>No hay semilla</h2>
 * La ficha pide un {@code GodTreeSapling}. No existe en el datapack: se planta desde la <b>forma del Diez
 * Colas</b> (slot 8 + sneak) pagando {@value GodTreeTable#PLANT_CHAKRA} de chakra. Lo que se coloca es una
 * estructura, no un item que lleves encima.
 *
 * <h2>Cinco etapas, y el premio va al que lo planto</h2>
 * Cada {@value GodTreeTable#TICKS_PER_STAGE} ticks sube una etapa y coloca la estructura siguiente. Al llegar a
 * la {@value GodTreeTable#MAX_STAGE}, {@code find_ply} busca al jugador <b>vinculado</b> por UUID y le despierta
 * el ojo. El vinculo es del datapack ({@code d.tree_ply} ↔ {@code d.tree_as}) y aqui es el {@code owner} del
 * registro: plantar un arbol y que se lo lleve otro seria un cambio de diseño.
 *
 * <p><b>Fidelidad con un limite (RM-7):</b> colocar la estructura <b>arrasa</b> lo que hubiera, igual que el
 * {@code structure_block} con {@code integrity 1.0}. Con {@code GodTreeProtectsBuilds=true} el arbol <b>se niega
 * a plantarse</b> si en su huella hay algun block entity (cofres, hornos, carteles): no protege todo, pero evita
 * lo que de verdad duele perder, y avisa en vez de destruir en silencio.
 */
public final class GodTreeService {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Lado de la huella de la estructura ({@code sizeX}/{@code sizeZ} del structure block). */
    private static final int FOOTPRINT = 17;

    /** Alto de la huella ({@code sizeY}). */
    private static final int FOOTPRINT_HEIGHT = 10;

    private GodTreeService() {
    }

    /**
     * Planta el Arbol Divino bajo el jugador ({@code plant_god_tree/cast}). No cobra ni coloca nada si no
     * procede.
     *
     * @return {@code true} si se planto
     */
    public static boolean plant(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        var data = NinjaData.get(player);
        if (data.chakra().chakra() < GodTreeTable.PLANT_CHAKRA) {
            player.sendSystemMessage(Component.translatable("godtree.dojutsu.no_chakra")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        GodTreeRegistry registry = GodTreeRegistry.of(level.getServer());
        if (registry.treeOf(player.getUUID()).isPresent()) {
            player.sendSystemMessage(Component.translatable("godtree.dojutsu.already")
                    .withStyle(ChatFormatting.RED)); // d.tt_planted: uno por jinchuriki
            return false;
        }
        BlockPos pos = player.blockPosition();
        if (DojutsuConfig.get().godTreeProtectsBuilds() && hasBlockEntities(level, pos)) {
            player.sendSystemMessage(Component.translatable("godtree.dojutsu.would_destroy")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        if (!place(level, pos, GodTreeTable.SAPLING_STRUCTURE, GodTreeTable.SAPLING_OFFSET)) {
            return false; // estructura ausente: no se cobra
        }
        ChakraService.consume(data, GodTreeTable.PLANT_CHAKRA);
        registry.plant(new GrowingTree(level.dimension(), pos, player.getUUID(), 0, 0));
        player.teleportTo(player.getX(), player.getY() + GodTreeTable.PLANT_LIFT, player.getZ());
        player.sendSystemMessage(Component.translatable("godtree.dojutsu.planted")
                .withStyle(ChatFormatting.DARK_RED));
        level.playSound(null, pos, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.BLOCKS, 0.3f, 0f);
        NinjaSync.sendDelta(player);
        LOGGER.info("[{}] {} planto el Arbol Divino en {}.", DojutsuMod.MOD_NAME,
                player.getGameProfile().getName(), pos);
        return true;
    }

    /**
     * Avanza todos los arboles un tick. Lo llama el tick de servidor.
     *
     * <p>Solo avanzan los que estan en <b>chunk cargado</b>, como el {@code as @e} del datapack: colocar bloques
     * lo exige igualmente, y crecer en chunks descargados seria regalar algo que el original no da.
     */
    public static void tick(MinecraftServer server) {
        GodTreeRegistry registry = GodTreeRegistry.of(server);
        for (GrowingTree tree : registry.trees()) {
            ServerLevel level = server.getLevel(tree.dimension());
            if (level == null || !level.isLoaded(tree.pos())) {
                continue;
            }
            GrowingTree ticked = tree.ticked();
            if (!GodTreeTable.readyToGrow(ticked.timer())) {
                registry.replace(tree, ticked);
                continue;
            }
            grow(level, registry, tree);
        }
    }

    /** Sube una etapa, coloca su estructura y, si florecio, entrega el premio ({@code grow} + {@code find_ply}). */
    private static void grow(ServerLevel level, GodTreeRegistry registry, GrowingTree tree) {
        GrowingTree grown = tree.grown();
        int stage = grown.stage();
        place(level, grown.pos(), GodTreeTable.structureForStage(stage), GodTreeTable.offsetForStage(stage));
        level.playSound(null, grown.pos(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.BLOCKS, 0.3f, 0f);

        if (!GodTreeTable.isBloomed(stage)) {
            registry.replace(tree, grown);
            return;
        }
        // Florecio: el marcador muere (kill @s) tanto si el jugador esta como si no.
        registry.remove(tree);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(grown.owner());
        if (owner == null) {
            LOGGER.info("[{}] El Arbol Divino de {} florecio sin el delante; nadie recibe el ojo.",
                    DojutsuMod.MOD_NAME, grown.owner());
            return; // find_ply solo mira a los conectados: el premio se pierde, como en el datapack
        }
        awaken(owner);
    }

    /**
     * Despierta el Rinne Sharingan ({@code awaken_rinne_sharingan}). Los bonus (+100 de chakra maximo, +1000 de
     * ninja_xp) los aplica {@link AwakeningService#grantEye} desde la definicion del ojo — el mismo camino que
     * cualquier otro despertar, extraido en DJT-200 justo para esto.
     */
    public static boolean awaken(ServerPlayer player) {
        if (!AwakeningService.grantEye(player, GodTreeTable.REWARD_EYE)) {
            return false;
        }
        player.sendSystemMessage(Component.translatable("godtree.dojutsu.bloomed")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        // El sonido real del datapack, ya portado (F10/DJT-224): sustituye al BEACON_POWER_SELECT provisional.
        // El Rinne Sharingan no despierta como los demas —lo concede el arbol—, por eso no esta en EyeSounds.
        EyeSounds.at(player, ModSounds.RINNE_SHARINGAN_ACTIVATE.get(), EyeSounds.RINNE_SHARINGAN_VOLUME);
        LOGGER.info("[{}] {} desperto el Rinne Sharingan bajo el Arbol Divino.",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName());
        return true;
    }

    /**
     * Coloca una estructura del mod centrada en {@code pos} con el desplazamiento del datapack.
     *
     * @return {@code false} si la estructura no existe (no se cobra ni se registra nada)
     */
    private static boolean place(ServerLevel level, BlockPos pos, String structure, int offset) {
        ResourceLocation id = DojutsuMod.id(structure);
        Optional<StructureTemplate> template = level.getStructureManager().get(id);
        if (template.isEmpty()) {
            LOGGER.error("[{}] Falta la estructura {}.", DojutsuMod.MOD_NAME, id);
            return false;
        }
        BlockPos origin = pos.offset(offset, 0, offset);
        template.get().placeInWorld(level, origin, origin,
                new StructurePlaceSettings().setIgnoreEntities(true), level.getRandom(), Block.UPDATE_CLIENTS);
        return true;
    }

    /** {@code true} si en la huella del arbol hay algun block entity (lo que de verdad duele perder). */
    private static boolean hasBlockEntities(ServerLevel level, BlockPos pos) {
        int half = FOOTPRINT / 2;
        for (BlockPos check : BlockPos.betweenClosed(
                pos.offset(-half, 0, -half), pos.offset(half, FOOTPRINT_HEIGHT, half))) {
            if (level.getBlockEntity(check) != null) {
                return true;
            }
        }
        return false;
    }
}
