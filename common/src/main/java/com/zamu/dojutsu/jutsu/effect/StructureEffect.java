package com.zamu.dojutsu.jutsu.effect;

import com.google.gson.JsonObject;
import com.zamu.dojutsu.jutsu.JutsuEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Familia ESTRUCTURA (F4/DJT-076): coloca un muro de bloques delante del caster. Piloto:
 * {@code earth_wall} — muro de dirt de 3×4 a 2 bloques, orientado a la mirada horizontal,
 * con semantica {@code setblock ... destroy} (rompe y dropea lo que hubiera, incluida una
 * construccion del jugador — fiel al original; las variantes 5/7 de ancho del datapack se
 * parametrizan con {@code width} al portarlas).
 *
 * <p>Parametros: {@code block} (def. dirt) · {@code width} (impar, def. 3) · {@code height}
 * (def. 4) · {@code forward} (bloques por delante, def. 2).
 */
public final class StructureEffect implements JutsuEffect {

    /** Parametros de la familia (puro y testeable). */
    public record Params(String block, int width, int height, int forward) {

        public static Params parse(JsonObject json) {
            return new Params(
                    GsonHelper.getAsString(json, "block", "minecraft:dirt"),
                    GsonHelper.getAsInt(json, "width", 3),
                    GsonHelper.getAsInt(json, "height", 4),
                    GsonHelper.getAsInt(json, "forward", 2));
        }
    }

    @Override
    public void apply(ServerLevel level, LivingEntity caster, JutsuEntry jutsu) {
        Params params = Params.parse(jutsu.effectParams().getAsJsonObject());
        ResourceLocation id = ResourceLocation.tryParse(params.block());
        Block block = id == null ? Blocks.DIRT : BuiltInRegistries.BLOCK.getOptional(id).orElse(Blocks.DIRT);

        Direction facing = caster.getDirection(); // mirada horizontal (el muro es vertical)
        Direction right = facing.getClockWise();
        BlockPos center = caster.blockPosition().relative(facing, params.forward());
        int half = params.width() / 2;
        for (int w = -half; w <= half; w++) {
            for (int h = 1; h <= params.height(); h++) {
                BlockPos pos = center.relative(right, w).above(h);
                // setblock ... destroy: rompe (y dropea) lo que hubiera, como el datapack.
                if (!level.getBlockState(pos).isAir()) {
                    level.destroyBlock(pos, true);
                }
                level.setBlockAndUpdate(pos, block.defaultBlockState());
            }
        }
    }
}
