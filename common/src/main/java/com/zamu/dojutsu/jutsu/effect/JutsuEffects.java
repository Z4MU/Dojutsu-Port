package com.zamu.dojutsu.jutsu.effect;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import org.slf4j.Logger;

/**
 * Punto de registro de las familias de efecto en el arranque (F4/DJT-074/076).
 *
 * <p>Las <b>seis familias del ADD</b> (una clase por familia, nunca por tecnica — RA-1):
 * proyectil, area, buff, teleport, estructura y curacion, parametrizadas desde
 * {@code effectParams}. F5+ registra familias nuevas aqui (o desde su propio modulo) sin tocar
 * el pipeline. + la familia <b>primed_strike</b> (golpe cargado del rayo: lightning_bolt, chidori,
 * lightning_cutter, drilling — arma al castear, descarga al golpear; DJT-085d).
 */
public final class JutsuEffects {

    private static final Logger LOGGER = LogUtils.getLogger();

    private JutsuEffects() {
    }

    /** Registra las familias. Llamar una vez desde el arranque ({@code DojutsuLifecycle}). */
    public static void register() {
        JutsuEffectRegistry.register("projectile", new ProjectileEffect());
        JutsuEffectRegistry.register("area", new AreaEffect());
        JutsuEffectRegistry.register("buff", new BuffEffect());
        JutsuEffectRegistry.register("teleport", new TeleportEffect());
        JutsuEffectRegistry.register("structure", new StructureEffect());
        JutsuEffectRegistry.register("heal", new HealEffect());
        JutsuEffectRegistry.register("primed_strike", new PrimedStrikeEffect());
        LOGGER.info("[{}] Familias de efecto registradas: projectile, area, buff, teleport, structure, heal, primed_strike.",
                DojutsuMod.MOD_NAME);
    }
}
