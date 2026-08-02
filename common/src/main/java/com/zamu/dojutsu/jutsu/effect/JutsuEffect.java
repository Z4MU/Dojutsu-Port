package com.zamu.dojutsu.jutsu.effect;

import com.zamu.dojutsu.jutsu.JutsuEntry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Efecto ejecutable de una tecnica (F4/DJT-074). Una implementacion por <b>familia</b> de efecto
 * (proyectil, area, buff, teleport, estructura, curacion — DJT-076), nunca por tecnica (RA-1):
 * cada familia se parametriza desde {@link JutsuEntry#effectParams()} con su propio codec.
 *
 * <p>El caster es un {@link LivingEntity} <b>generico</b> (S17-R3): un jugador hoy, un shinobi IA
 * en F8. Corre <b>solo en servidor</b>, invocado por el pipeline <b>despues</b> de validar y
 * consumir (el efecto no comprueba ni cobra nada — eso es del pipeline).
 */
@FunctionalInterface
public interface JutsuEffect {

    /**
     * Ejecuta el efecto de la tecnica.
     *
     * @param level  nivel del servidor donde ocurre el cast
     * @param caster quien lanza (posicion/mirada de origen del efecto)
     * @param jutsu  la entrada completa (definicion + parametros del efecto)
     */
    void apply(ServerLevel level, LivingEntity caster, JutsuEntry jutsu);

    /**
     * Precondicion del efecto, evaluada por el pipeline <b>antes de consumir chakra</b> (F4/DJT-076):
     * el datapack validaba "Nothing nearby" antes de cobrar (healing, vacuum_wave). Devuelve
     * {@code false} para abortar el cast sin coste ({@code NO_TARGET}).
     */
    default boolean canApply(ServerLevel level, LivingEntity caster, JutsuEntry jutsu) {
        return true;
    }
}
