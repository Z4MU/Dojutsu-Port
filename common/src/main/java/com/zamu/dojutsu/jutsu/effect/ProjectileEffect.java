package com.zamu.dojutsu.jutsu.effect;

import com.google.gson.JsonObject;
import com.zamu.dojutsu.entity.NinjaProjectile;
import com.zamu.dojutsu.jutsu.JutsuEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Familia PROYECTIL (F4/DJT-076): lanza un {@link NinjaProjectile} en la direccion de la mirada.
 * Reproduce el patron de los jutsu-proyectil de fuego del datapack (fire_bullet/great_fireball):
 * el proyectil lleva un <b>aura durante el vuelo</b> que incendia lo cercano ({@code Fire:100} en
 * radio 3 <b>cada tick</b>) y prende el suelo bajo su trayectoria; impactar contra un bloque lo
 * detiene. El dano directo es opcional ({@code damage}) para las tecnicas que golpean al impacto.
 *
 * <p>Parametros ({@code effectParams}): {@code speed} (bloques/tick, def. 1.0) · {@code gravity}
 * (def. 0) · {@code lifetimeTicks} (def. 100) · {@code radius} (aura, def. 0 = sin aura) ·
 * {@code fireTicks} (fuego del aura/impacto, def. 0) · {@code igniteGround} (def. false) ·
 * {@code damage} (al impactar una entidad, def. 0) · {@code displayItem} (def. bola de nieve).
 */
public final class ProjectileEffect implements JutsuEffect {

    /** Parametros de la familia, parseados de {@code effectParams} (puro y testeable). */
    public record Params(double speed, double gravity, int lifetimeTicks, double radius,
            int fireTicks, boolean igniteGround, float damage, String displayItem, EffectSfx sfx) {

        public static Params parse(JsonObject json) {
            return new Params(
                    GsonHelper.getAsDouble(json, "speed", 1.0D),
                    GsonHelper.getAsDouble(json, "gravity", 0.0D),
                    GsonHelper.getAsInt(json, "lifetimeTicks", NinjaProjectile.DEFAULT_LIFETIME_TICKS),
                    GsonHelper.getAsDouble(json, "radius", 0.0D),
                    GsonHelper.getAsInt(json, "fireTicks", 0),
                    GsonHelper.getAsBoolean(json, "igniteGround", false),
                    GsonHelper.getAsFloat(json, "damage", 0.0F),
                    GsonHelper.getAsString(json, "displayItem", "minecraft:snowball"),
                    EffectSfx.parse(json));
        }
    }

    @Override
    public void apply(ServerLevel level, LivingEntity caster, JutsuEntry jutsu) {
        Params params = Params.parse(jutsu.effectParams().getAsJsonObject());
        // Suena al lanzar, no al impactar: el playsound del Rasenshuriken esta en throw.mcfunction, la primera
        // linea (F10/DJT-227). Es el unico proyectil con sonido propio en el datapack.
        params.sfx().play(level, caster.getX(), caster.getY(), caster.getZ());
        NinjaProjectile projectile = NinjaProjectile.create(level, caster)
                .withGravity(params.gravity())
                .withLifetime(params.lifetimeTicks())
                .withDisplayItem(displayStack(params.displayItem()))
                .withFlightTick(p -> flightAura(level, caster, p, params))
                .withImpactCallback((p, hit) -> {
                    if (params.damage() > 0
                            && hit instanceof net.minecraft.world.phys.EntityHitResult entityHit) {
                        entityHit.getEntity().hurt(level.damageSources().indirectMagic(p, caster), params.damage());
                    }
                });
        projectile.launch(caster.getLookAngle(), (float) params.speed());
        level.addFreshEntity(projectile);
    }

    /**
     * El aura de vuelo del datapack: entidades cercanas arden ({@code Fire set value N} cada
     * tick) y el suelo inflamable bajo el proyectil prende (cruz de 1 bloque, como el original).
     */
    private static void flightAura(ServerLevel level, LivingEntity caster, NinjaProjectile projectile,
            Params params) {
        if (params.radius() <= 0) {
            return;
        }
        if (params.fireTicks() > 0) {
            for (Entity entity : level.getEntities(projectile,
                    projectile.getBoundingBox().inflate(params.radius()),
                    e -> e instanceof LivingEntity && e != caster)) {
                entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), params.fireTicks()));
            }
        }
        if (params.igniteGround()) {
            BlockPos base = projectile.blockPosition();
            igniteIfBurnable(level, base);
            igniteIfBurnable(level, base.north());
            igniteIfBurnable(level, base.south());
            igniteIfBurnable(level, base.east());
            igniteIfBurnable(level, base.west());
        }
    }

    /** Prende un bloque de aire si el fuego puede sobrevivir ahi (el {@code burnable_void} del datapack). */
    private static void igniteIfBurnable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() && Blocks.FIRE.defaultBlockState().canSurvive(level, pos)) {
            level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
        }
    }

    private static ItemStack displayStack(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        Item item = id == null ? Items.SNOWBALL : BuiltInRegistries.ITEM.getOptional(id).orElse(Items.SNOWBALL);
        return new ItemStack(item);
    }
}
