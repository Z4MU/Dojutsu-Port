package com.zamu.dojutsu.items;

import java.util.Map;

/**
 * Mapa de <b>presentacion</b> elemento → {@code CustomModelData} del Skill Learner (F4/DJT-085c). Igual que
 * {@link JutsuModelData}: el item {@code dojutsu:learner} es uno solo; su modelo (una textura por elemento) se
 * elige por el data component {@code minecraft:custom_model_data} que {@link LearnerService#createLearner} fija,
 * y el modelo base ({@code models/item/learner.json}) lo mapea. Numeros autenticos del datapack
 * ({@code give/learner/*}, CMD 1000-1023).
 */
public final class LearnerModelData {

    /** {@code elemento → CustomModelData} (numeros autenticos del datapack). 0 = modelo por defecto. */
    private static final Map<String, Integer> MODEL_DATA = Map.ofEntries(
            Map.entry("fire", 1000), Map.entry("wind", 1001), Map.entry("lightning", 1002),
            Map.entry("earth", 1003), Map.entry("water", 1004), Map.entry("yin", 1005),
            Map.entry("yang", 1006), Map.entry("wood", 1007), Map.entry("blaze", 1008),
            Map.entry("ice", 1009), Map.entry("lava", 1010), Map.entry("storm", 1011),
            Map.entry("boil", 1012), Map.entry("dust", 1013), Map.entry("magnet", 1014),
            Map.entry("explosion", 1015), Map.entry("scorch", 1016), Map.entry("yin_yang", 1017),
            Map.entry("steel", 1018), Map.entry("dark", 1019), Map.entry("swift", 1020),
            Map.entry("crystal", 1021), Map.entry("typhoon", 1022), Map.entry("mud", 1023));

    private LearnerModelData() {
    }

    /** {@code CustomModelData} del learner de un elemento, o 0 si no tiene textura portada. */
    public static int of(String element) {
        return MODEL_DATA.getOrDefault(element, 0);
    }
}
