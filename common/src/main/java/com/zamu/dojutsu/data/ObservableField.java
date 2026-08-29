package com.zamu.dojutsu.data;

/**
 * Campos de {@link PlayerNinjaData} <b>observables por el cliente</b> (HUD/GUI), susceptibles de sync.
 *
 * <p>Solo estos campos cruzan la red; el estado interno no observable nunca se sincroniza. Cada fase anade
 * los suyos: F1 el onboarding; F2 el chakra (HUD) y la progresion (menu/HUD: puntos, ninja_xp, niveles);
 * F4 los cooldowns de jutsu (HUD de cd, DJT-081); F11 los jutsus aprendidos (selector de jutsu, DJT-231).
 */
public enum ObservableField {
    ONBOARDING,
    CHAKRA,
    PROGRESSION,
    JUTSU_COOLDOWNS,

    /**
     * Ids de los jutsus aprendidos. Cambia <b>muy pocas veces</b> en una partida (solo al aprender uno), pero
     * es la lista mas grande que cruza la red, asi que el sync la envia <b>solo cuando cambia</b> y no en cada
     * delta. Ver {@code SyncNinjaDataPayload}.
     */
    JUTSU_KNOWN,

    /**
     * Jutsu seleccionado para lanzar con la tecla (F11/DJT-231). Es un solo id opcional, asi que —al reves que
     * {@link #JUTSU_KNOWN}— viaja en todos los deltas sin coste apreciable.
     */
    JUTSU_SELECTED,

    /**
     * Reparto de los ocho huecos rapidos (F11/DJT-234). Va aparte de {@link #JUTSU_KNOWN} porque cambia por
     * motivos distintos: asignar un hueco no ensena ningun jutsu nuevo.
     */
    JUTSU_SLOTS
}
