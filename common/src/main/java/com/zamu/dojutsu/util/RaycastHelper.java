package com.zamu.dojutsu.util;

import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.HitResult;

/**
 * Raycast del proyectil ninja (F4/DJT-070). Encuentra el primer impacto (bloque o entidad,
 * el mas cercano al origen) a lo largo del movimiento de este tick, trazando el segmento
 * posicion → posicion + velocidad. Al ser por segmento y no por muestreo, no hay tunneling
 * a ninguna velocidad, y si hay dos entidades en la trayectoria gana la mas cercana.
 *
 * <p>Es la costura de colision del mod: los sistemas de F4+ (jutsu/dojutsu/sage/throwing/
 * summon/bijuu) dependen de este helper y no de la utilidad vanilla, de modo que un cambio
 * de semantica (p. ej. atravesar fluidos o piercing) se haga en un unico punto. Hoy delega
 * en {@link ProjectileUtil} (bloques con forma de colision; los fluidos no detienen el
 * proyectil, igual que no detenian a los armor stands del datapack).
 *
 * <p>Glue fino sobre la API vanilla: se valida en runtime (como los teams de DJT-062), no
 * con tests unitarios — construir un {@code Level} exige bootstrap de Minecraft.
 */
public final class RaycastHelper {

    private RaycastHelper() {
    }

    /**
     * Primer impacto del movimiento de este tick, o un resultado de tipo {@code MISS} si el
     * segmento esta libre.
     *
     * @param projectile proyectil en vuelo (aporta posicion, velocidad y nivel)
     * @param canHit     filtro de entidades golpeables (tipicamente {@code Projectile#canHitEntity}:
     *                   excluye al dueno hasta abandonarlo, espectadores y entidades no seleccionables)
     */
    public static HitResult firstHitAlongMotion(Projectile projectile, Predicate<Entity> canHit) {
        return ProjectileUtil.getHitResultOnMoveVector(projectile, canHit);
    }
}
