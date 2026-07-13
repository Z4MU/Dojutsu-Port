package com.zamu.dojutsu.registry;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.platform.PlatformServices;
import com.zamu.dojutsu.platform.Registrar;
import com.zamu.dojutsu.platform.RegistryEntry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

/**
 * Registro de particulas del mod. En F0 solo declara un tipo de particula placeholder
 * ({@code chakra_debug}). El proveedor de render se registra en cliente cuando exista.
 */
public final class ModParticles {

    private static final Registrar<net.minecraft.core.particles.ParticleType<?>> PARTICLES =
            PlatformServices.REGISTRAR.create(Registries.PARTICLE_TYPE);

    /** Particula de depuracion (placeholder; sin proveedor de render aun). */
    public static final RegistryEntry<SimpleParticleType> CHAKRA_DEBUG =
            PARTICLES.register(DojutsuMod.id("chakra_debug"), () -> new SimpleParticleType(false) {
            });

    private ModParticles() {
    }

    /** Confirma el registro. Llamar durante la inicializacion del mod. */
    public static void register() {
        PARTICLES.commit();
    }
}
