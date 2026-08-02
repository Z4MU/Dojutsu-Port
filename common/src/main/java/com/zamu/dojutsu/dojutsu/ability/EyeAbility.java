package com.zamu.dojutsu.dojutsu.ability;

/**
 * Contrato de una <b>habilidad ocular</b> (dojutsu ability) del datapack {@code [S05]} (DJT-118, abre
 * `EP-EYE-ABILITIES`). Es la plantilla que clona el resto del catalogo (Kamui, Shinra Tensei, Tsukuyomi…):
 * declara su coste de chakra y de fatiga ocular, y ejecuta su efecto sobre un {@link EyeAbilityContext}.
 *
 * <p><b>El pipeline de cast</b> ({@code EyeAbilityService}) valida ojo activo + seleccion + chakra, consume
 * chakra y fatiga, y solo entonces llama a {@link #execute}. La habilidad concreta <b>no</b> valida recursos ni
 * toca la red: solo produce su efecto (RA-3: las anclas son {@code AbilityAnchor}, nunca armor stands).
 */
public interface EyeAbility {

    /** Chakra que cuesta lanzarla ({@code d.chakra}); el pipeline lo consume todo-o-nada. */
    int chakraCost();

    /** Fatiga ocular que anade al lanzarla ({@code d.eye_strain}); el pipeline la aplica (0 si el ojo es eterno). */
    int strainCost();

    /**
     * Ticks de reutilizacion tras lanzarla ({@code d.<ability>_cd} del datapack). 0 = sin cooldown: la habilidad
     * se autolimita por su coste de chakra, que es el caso de la mayoria del catalogo.
     *
     * <p>Existe porque hay habilidades que el datapack gatea <b>solo</b> por cooldown y no cobran chakra
     * (el {@code genjutsu_stun} del Sharingan: {@code scoreboard players set @s d.sharstun_cd 200}). Sin esto,
     * una habilidad de coste 0 seria spameable.
     */
    default int cooldownTicks() {
        return 0;
    }

    /**
     * Precondicion de "hay algo que hacer": {@code false} aborta el cast <b>sin cobrar</b> chakra ni fatiga
     * ("Nothing nearby"). Por defecto {@code true} (AoE, auto-buff, proyectil y punto de mira siempre aplican).
     *
     * <p>Fiel al {@code no_cast_nby} del datapack: se comprueba <b>antes</b> del cobro, solo en las habilidades
     * de objetivo unico que pueden fallar (Ketsuryugan {@code blood_eruption}/{@code exploding_human}, Sharingan
     * {@code genjutsu_stun}). El pipeline la evalua tras el cooldown y antes de {@link #chakraCost()}.
     */
    default boolean canApply(EyeAbilityContext context) {
        return true;
    }

    /** Produce el efecto de la habilidad (server-side). El chakra/fatiga ya fueron cobrados por el pipeline. */
    void execute(EyeAbilityContext context);
}
