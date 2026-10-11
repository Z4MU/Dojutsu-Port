package com.zamu.dojutsu.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Las particulas de cada {@link ProjectileTrail} (F11/DJT-241).
 *
 * <ul>
 *   <li><b>Estela:</b> la emite <b>cada cliente</b> desde el tick de la entidad. El datapack lo hacia en el
 *       servidor con {@code particle ... force}, que en un mod seria un paquete por tick y proyectil.</li>
 *   <li><b>Estallido:</b> lo emite el <b>servidor</b> al impactar, porque el cliente no se entera del impacto:
 *       solo ve que la entidad desaparece.</li>
 * </ul>
 */
public final class ProjectileTrailFx {

    // Colores del datapack (fire_bullet/run.mcfunction). Se crean al usarse, no en un inicializador estatico.
    private static final Vector3f FIRE_DEEP = new Vector3f(1.0F, 0.4F, 0.08F);
    private static final Vector3f FIRE_LIGHT = new Vector3f(1.0F, 0.6F, 0.2F);

    private ProjectileTrailFx() {
    }

    /**
     * Rastro de un tick de vuelo, repartido a lo largo del segmento recorrido para que salga continuo aunque
     * el proyectil avance un bloque por tick. Solo pinta algo en el cliente; en el servidor no hace nada.
     *
     * @param from centro del proyectil al empezar el tick
     * @param to   centro al acabarlo
     */
    public static void emitTrail(Level level, ProjectileTrail trail, Vec3 from, Vec3 to, RandomSource random) {
        if (!level.isClientSide || trail == ProjectileTrail.NONE) {
            return;
        }
        if (trail == ProjectileTrail.FIRE) {
            // El datapack soltaba 120 particulas por tick (20 + 100). Aqui bastan menos porque se reparten por
            // el segmento en vez de amontonarse en un punto.
            scatter(level, new DustParticleOptions(FIRE_DEEP, 1.0F), from, to, 6, 0.25D, random);
            scatter(level, new DustParticleOptions(FIRE_LIGHT, 0.5F), from, to, 10, 0.35D, random);
            scatter(level, ParticleTypes.FLAME, from, to, 2, 0.15D, random);
            // El humo sale de la cola: lo que queda detras cuando la bola ya ha pasado.
            level.addParticle(ParticleTypes.SMOKE, from.x, from.y, from.z, 0.0D, 0.02D, 0.0D);
        }
    }

    /** Estallido al impactar, con su sonido. Lo llama el servidor; los clientes lo reciben como siempre. */
    public static void burst(ServerLevel level, ProjectileTrail trail, Vec3 at) {
        if (trail == ProjectileTrail.FIRE) {
            level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 40, 0.35D, 0.35D, 0.35D, 0.08D);
            level.sendParticles(new DustParticleOptions(FIRE_DEEP, 1.5F), at.x, at.y, at.z, 20, 0.5D, 0.5D, 0.5D, 0.0D);
            level.sendParticles(ParticleTypes.LAVA, at.x, at.y, at.z, 6, 0.2D, 0.2D, 0.2D, 0.0D);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 10, 0.3D, 0.3D, 0.3D, 0.02D);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS,
                    1.0F, 0.9F + level.random.nextFloat() * 0.2F);
        }
    }

    private static void scatter(Level level, ParticleOptions particle, Vec3 from, Vec3 to, int count,
            double spread, RandomSource random) {
        for (int i = 0; i < count; i++) {
            double t = random.nextDouble();
            level.addParticle(particle,
                    from.x + (to.x - from.x) * t + random.nextGaussian() * spread,
                    from.y + (to.y - from.y) * t + random.nextGaussian() * spread,
                    from.z + (to.z - from.z) * t + random.nextGaussian() * spread,
                    0.0D, 0.0D, 0.0D);
        }
    }
}
