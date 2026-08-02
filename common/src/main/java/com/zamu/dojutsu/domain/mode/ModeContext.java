package com.zamu.dojutsu.domain.mode;

/**
 * Contexto de ejecucion de un {@link ToggleableMode} en una invocacion (activar/tick/desactivar).
 *
 * <p>Es una costura (interfaz marcador) que mantiene el framework agnostico de <em>quien</em> lleva el
 * modo y de <em>que</em> recursos consume. Cada consumidor concreto (sage S08, sellos S10, puertas S11,
 * SOAH S12, susanoo S24, formas S09) provee su propia implementacion cargando lo que sus hooks necesitan
 * (entidad, chakra, strain...). No asume jugador: un NPC con IA tambien podria portar un modo.
 *
 * <p>Dominio puro: sin imports de Minecraft.
 */
public interface ModeContext {
}
