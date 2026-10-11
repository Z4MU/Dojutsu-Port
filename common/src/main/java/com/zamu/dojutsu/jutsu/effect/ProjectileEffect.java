package com.zamu.dojutsu.jutsu.effect;

import com.google.gson.JsonObject;
import com.zamu.dojutsu.entity.NinjaProjectile;
import com.zamu.dojutsu.entity.ProjectileTrail;
import com.zamu.dojutsu.entity.ProjectileTrailFx;
import com.zamu.dojutsu.jutsu.JutsuEntry;
import com.zamu.dojutsu.registry.ModItems;
import com.zamu.dojutsu.items.JutsuModelData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

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
 * {@code damage} (al impactar una entidad, def. 0) · {@code displayItem} (def. bola de nieve) ·
 * {@code displayModel3d} (F11/DJT-240, def. false) · {@code hitboxSize} (F11/DJT-241, lado en bloques, def. 0 =
 * la caja de 0.25 de siempre) · {@code trail} (F11/DJT-241, estela y estallido: {@code "fire"}, def. sin estela) ·
 * {@code casterEffects} (F11/DJT-241, pociones para quien lanza, como {@link MobEffectSpec}; def. ninguna).
 *
 * <p><b>Proteccion del lanzador (DJT-241).</b> Los cinco jutsus de fuego con proyectil hacian en su
 * {@code cast.mcfunction} {@code effect give @s fire_resistance 2 255 true}: el proyectil sale de la cara de quien
 * lo lanza y prende el suelo desde el primer tick. El port lo habia perdido, y al activar {@code igniteGround} el
 * lanzador ardia con su propio fuego. Lo recupera {@code casterEffects}.
 *
 * <p><b>Hitbox y estela (DJT-241).</b> Un modelo 3D de 0.8 bloques con la caja de una bola de nieve atravesaba a
 * los mobs sin tocarlos: {@code hitboxSize} la ajusta al diametro de la bola del modelo ({@code Jutsu3dFitTest}
 * comprueba que coincidan). {@code trail} recupera las particulas que el datapack soltaba en cada tick de vuelo, y
 * anade un estallido con sonido al impactar, que el original no tenia.
 *
 * <p><b>Modelo 3D en vuelo (DJT-240).</b> Con {@code displayModel3d: true} el proyectil se dibuja con el item del
 * mod {@code dojutsu:jutsu} y el {@code CustomModelData} 3D de la tecnica ({@link JutsuModelData#worldModelOf}),
 * en vez de con un item de vanilla. Lo dibuja {@code NinjaProjectileRenderer}, orientado segun el vuelo, con brillo
 * maximo y una animacion de giro y latido. Es opt-in por tecnica, porque
 * un numero 3D sin modelo detras mostraria el de otro jutsu (ver {@link JutsuModelData#worldModelOf}).
 */
public final class ProjectileEffect implements JutsuEffect {

    /** Parametros de la familia, parseados de {@code effectParams} (puro y testeable). */
    public record Params(double speed, double gravity, int lifetimeTicks, double radius,
            int fireTicks, boolean igniteGround, float damage, String displayItem, boolean displayModel3d,
            float hitboxSize, ProjectileTrail trail, java.util.List<MobEffectSpec> casterEffects, EffectSfx sfx) {

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
                    GsonHelper.getAsBoolean(json, "displayModel3d", false),
                    GsonHelper.getAsFloat(json, "hitboxSize", 0.0F),
                    ProjectileTrail.byName(GsonHelper.getAsString(json, "trail", "")),
                    MobEffectSpec.parseList(json, "casterEffects"),
                    EffectSfx.parse(json));
        }
    }

    @Override
    public void apply(ServerLevel level, LivingEntity caster, JutsuEntry jutsu) {
        Params params = Params.parse(jutsu.effectParams().getAsJsonObject());
        // Suena al lanzar, no al impactar: el playsound del Rasenshuriken esta en throw.mcfunction, la primera
        // linea (F10/DJT-227). Es el unico proyectil con sonido propio en el datapack.
        params.sfx().play(level, caster.getX(), caster.getY(), caster.getZ());
        // Antes de lanzar: el proyectil prende el suelo desde el primer tick, junto a quien lo lanza.
        params.casterEffects().forEach(effect -> effect.applyTo(caster));
        NinjaProjectile projectile = NinjaProjectile.create(level, caster)
                .withGravity(params.gravity())
                .withLifetime(params.lifetimeTicks())
                .withDisplayItem(params.displayModel3d()
                        ? worldModelStack(jutsu.id(), params.displayItem())
                        : displayStack(params.displayItem()))
                .withTrail(params.trail())
                .withFlightTick(p -> flightAura(level, caster, p, params))
                .withImpactCallback((p, hit) -> {
                    if (params.damage() > 0
                            && hit instanceof net.minecraft.world.phys.EntityHitResult entityHit) {
                        entityHit.getEntity().hurt(level.damageSources().indirectMagic(p, caster), params.damage());
                    }
                    // Contra una entidad, el estallido va en la bola (justo delante del objetivo), no en sus pies.
                    ProjectileTrailFx.burst(level, params.trail(), hit.getType() == HitResult.Type.ENTITY
                            ? p.getBoundingBox().getCenter() : hit.getLocation());
                });
        if (params.hitboxSize() > 0) {
            projectile.withHitboxSize(params.hitboxSize());
        }
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

    /**
     * El item del mod con el modelo 3D de la tecnica. Si la tecnica no tiene numero de modelo, cae al
     * {@code displayItem} de vanilla en vez de dibujar un pergamino generico volando.
     */
    private static ItemStack worldModelStack(String jutsuId, String fallbackItem) {
        int modelData = JutsuModelData.worldModelOf(jutsuId);
        if (modelData == 0) {
            return displayStack(fallbackItem);
        }
        ItemStack stack = new ItemStack(ModItems.JUTSU.get());
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(modelData));
        return stack;
    }

    private static ItemStack displayStack(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        Item item = id == null ? Items.SNOWBALL : BuiltInRegistries.ITEM.getOptional(id).orElse(Items.SNOWBALL);
        return new ItemStack(item);
    }
}
