package com.zamu.dojutsu.data;

/**
 * Campos de {@link PlayerNinjaData} <b>observables por el cliente</b> (HUD/GUI), susceptibles de sync.
 *
 * <p>Solo estos campos cruzan la red; el estado interno no observable nunca se sincroniza. Cada fase anade
 * los suyos: F1 el onboarding; F2 el chakra (HUD) y la progresion (menu/HUD: puntos, ninja_xp, niveles);
 * F4 los cooldowns de jutsu (HUD de cd, DJT-081).
 */
public enum ObservableField {
    ONBOARDING,
    CHAKRA,
    PROGRESSION,
    JUTSU_COOLDOWNS
}
