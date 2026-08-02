package com.zamu.dojutsu.bijuu.ai.moves;

import com.zamu.dojutsu.bijuu.ai.BeastMove;
import com.zamu.dojutsu.bijuu.ai.BeastMoveContext;
import com.zamu.dojutsu.domain.bijuu.BeastSignatureTable;
import com.zamu.dojutsu.domain.bijuu.TailedBeast;
import com.zamu.dojutsu.entity.NinjaProjectile;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * <b>Técnica insignia</b> de una Bestia con Cola {@code [S09]} (F10/DJT-226): la andanada de corto alcance que
 * cada una de las nueve primeras dispara en los slots 1, 3 y 4 de su plan de IA.
 *
 * <h2>Una clase, nueve técnicas</h2>
 * El datapack escribe nueve carpetas de funciones —{@code sand_buckshot}, {@code cat_flame_roaring_fire},
 * {@code great_water_mass_bullet}, {@code monkey_flame_arson}, {@code unrivalled_strength},
 * {@code soap_bubble_barrage}, {@code scale_powder}, {@code tentacle_barrage}, {@code chakra_arms_barrage}— y las
 * nueve son, <b>valor a valor</b>, la misma técnica: mismo alcance, misma velocidad, mismo radio, mismo daño.
 * Sólo cambian las partículas. Así que aquí hay una implementación parametrizada por bestia (RA-1: «una clase
 * por familia, nunca por técnica»), y la diferencia visible —que es la única diferencia real— vive en
 * {@link #trail(TailedBeast)}.
 *
 * <p>Los números están en {@link BeastSignatureTable}, incluida la excepción del Nueve Colas, cuyo disparo vive
 * el doble y por tanto llega el doble de lejos.
 *
 * <p>Comparte forma con {@link TailedBeastBallMove} —proyectil con carga de tres ticks y daño en área al
 * impactar— pero es su opuesto en escala: la Bola vuela {@code 2,2} bloques/tick durante 100 ticks y detona con
 * {@code HARM IV} más explosión; esto vuela {@code 1,2} durante 10 y hace {@code HARM II} sin explosión. Una es
 * el ataque definitivo; esto es el jab.
 */
public final class SignatureBarrageMove implements BeastMove {

    /** Bloques por delante del lanzador donde nace el disparo ({@code positioned ^ ^1.25 ^1}). */
    private static final double SPAWN_FORWARD = 1.0D;

    /** Altura sobre los ojos a la que nace ({@code ~1.25} del {@code tp} de {@code summon}). */
    private static final double SPAWN_UP = 0.25D;

    private final TailedBeast beast;

    public SignatureBarrageMove(TailedBeast beast) {
        this.beast = beast;
    }

    @Override
    public String id() {
        return beast.signatureMove();
    }

    @Override
    public int chakraCost() {
        return BeastSignatureTable.CHAKRA_COST;
    }

    @Override
    public int cooldownTicks() {
        return BeastSignatureTable.COOLDOWN_TICKS;
    }

    @Override
    public void execute(BeastMoveContext context) {
        LivingEntity caster = context.caster();
        ServerLevel level = context.level();
        Vec3 look = caster.getLookAngle();
        Vec3 spawn = caster.getEyePosition().add(look.scale(SPAWN_FORWARD)).add(0.0D, SPAWN_UP, 0.0D);

        AtomicInteger charge = new AtomicInteger(BeastSignatureTable.CHARGE_TICKS);
        NinjaProjectile shot = NinjaProjectile.create(level, caster)
                .withLifetime(BeastSignatureTable.flightTicks(beast))
                .withDisplayItem(new ItemStack(Items.CLAY_BALL))
                .withFlightTick(projectile -> {
                    if (charge.get() > 0) {
                        // cuenta atras d.hit1/2/3: el disparo se forma quieto antes de salir
                        charge.decrementAndGet();
                        projectile.setDeltaMovement(Vec3.ZERO);
                    }
                    trailTick(projectile, beast);
                })
                .withImpactCallback((projectile, hit) -> burst(projectile.level(), projectile.position(), caster));
        shot.setPos(spawn.x, spawn.y, spawn.z);
        level.addFreshEntity(shot);
        shot.launch(look, BeastSignatureTable.SPEED_BLOCKS_PER_TICK);

        sound(beast).ifPresent(event ->
                level.playSound(null, caster.blockPosition(), event, SoundSource.HOSTILE, 0.2F, 1.0F));
    }

    /** Estela durante el vuelo: lo único que distingue de verdad a las nueve. */
    private static void trailTick(NinjaProjectile projectile, TailedBeast beast) {
        if (projectile.level() instanceof ServerLevel level) {
            Vec3 pos = projectile.position();
            level.sendParticles(trail(beast), pos.x, pos.y, pos.z, 12, 0.3D, 0.3D, 0.3D, 0.0D);
        }
    }

    /**
     * Partícula de cada bestia, verificada en el {@code run.mcfunction} de su técnica. Los {@code dust} llevan
     * el color exacto del datapack; donde este usaba una partícula con nombre propio se usa la misma.
     */
    private static ParticleOptions trail(TailedBeast beast) {
        return switch (beast) {
            // particle dust 0.9 0.8 0.55 — arena
            case ONE_TAIL -> dust(0.9F, 0.8F, 0.55F);
            // particle soul_fire_flame — llama azul de Matatabi
            case TWO_TAILS -> ParticleTypes.SOUL_FIRE_FLAME;
            // particle rain — masa de agua
            case THREE_TAILS -> ParticleTypes.RAIN;
            // particle dust 0.58 0.74 0.71
            case FOUR_TAILS -> dust(0.58F, 0.74F, 0.71F);
            // particle dust 0.8 0.79 0.8
            case FIVE_TAILS -> dust(0.8F, 0.79F, 0.8F);
            // particle bubble — la babosa
            case SIX_TAILS -> ParticleTypes.BUBBLE;
            // particle cloud — polvo de escamas
            case SEVEN_TAILS -> ParticleTypes.CLOUD;
            // particle block magenta_terracotta — los tentaculos
            case EIGHT_TAILS -> new BlockParticleOption(ParticleTypes.BLOCK,
                    Blocks.MAGENTA_TERRACOTTA.defaultBlockState());
            // particle dust 0.4 0.04 0.01 — chakra rojo de Kurama
            case NINE_TAILS -> dust(0.4F, 0.04F, 0.01F);
            // el Diez Colas no tiene andanada: su insignia es el Tenpenchii (area de rayos)
            case TEN_TAILS -> ParticleTypes.SOUL_FIRE_FLAME;
        };
    }

    /** Sonido propio, si la técnica lo tenía: sólo el Tres Colas ({@code entity.player.splash.high_speed}). */
    private static Optional<SoundEvent> sound(TailedBeast beast) {
        return beast == TailedBeast.THREE_TAILS
                ? Optional.of(SoundEvents.PLAYER_SPLASH_HIGH_SPEED)
                : Optional.empty();
    }

    private static DustParticleOptions dust(float r, float g, float b) {
        return new DustParticleOptions(new Vector3f(r, g, b), 1.0F);
    }

    /**
     * Impacto: {@code instant_damage} amplificador {@value BeastSignatureTable#DAMAGE_AMPLIFIER} en
     * {@value BeastSignatureTable#BLAST_RADIUS} bloques, menos al lanzador. Sin explosión — a diferencia de la
     * Bola Bestia con Cola, estas no rompen bloques. El {@code HARM} se invierte solo en los no-muertos, como la
     * rama {@code #dojutsu:undead} del datapack.
     */
    private static void burst(Level level, Vec3 at, LivingEntity caster) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                new AABB(at, at).inflate(BeastSignatureTable.BLAST_RADIUS))) {
            if (victim != caster) {
                victim.addEffect(new MobEffectInstance(MobEffects.HARM, 1, BeastSignatureTable.DAMAGE_AMPLIFIER));
            }
        }
    }
}
