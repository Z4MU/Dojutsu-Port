package com.zamu.dojutsu.domain.susanoo;

/**
 * Una fila de la tabla de Susanoo {@code [S24]}: que cuesta sostener un nivel y que otorga. Dominio puro (sin
 * imports de Minecraft): los amplificadores son numeros, no {@code MobEffect}s — el glue los traduce.
 *
 * <p><b>Los niveles altos son mas baratos, no mas caros:</b> es la curva del datapack (el dominio del Susanoo
 * mejora con la maestria), verificada en {@code susanoo/main.mcfunction}. Ver {@link SusanooCostTable}.
 *
 * @param level                  nivel 1..6
 * @param name                   nombre de la etapa ({@code Ribcage}…{@code Perfect}), del {@code level/set_N}
 * @param chakraPerPulse         chakra que cuesta cada pulso de mantenimiento
 * @param strainPerPulse         fatiga ocular que anade cada pulso
 * @param pulseTicks             cada cuantos ticks se cobra ({@code d.susanoo_time})
 * @param resistanceAmplifier    amplificador de {@code resistance} mientras esta activo
 * @param fireResistanceAmplifier amplificador de {@code fire_resistance}
 * @param slowness               si el nivel lastra al portador ({@code slowness} amp 1)
 * @param levelUpXp              ninja_xp que da alcanzar este nivel ({@code level/set_N})
 */
public record SusanooTier(int level, String name, int chakraPerPulse, int strainPerPulse, int pulseTicks,
        int resistanceAmplifier, int fireResistanceAmplifier, boolean slowness, int levelUpXp) {
}
