package com.zamu.dojutsu.data.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Sub-estado de conocimiento de jutsus del ninja ({@code [S06]}, F4/DJT-075).
 *
 * <p>Reemplaza tres familias de scoreboards del datapack:
 * <ul>
 *   <li>{@link #known}: que jutsus ha aprendido (en el datapack era implicito en "tener el item
 *       carrot"; aqui se persiste de verdad — perder el item no des-aprende, y el cast (DJT-080)
 *       valida contra esto, no contra el inventario).</li>
 *   <li>{@link #learnProgress}: los contadores {@code d.lrn_<elem>} del aprendizaje <b>secuencial</b>
 *       por elemento (cuantos jutsus de la secuencia de ese elemento ha entregado ya su learner).
 *       Recibir un learner nuevo lo resetea a 0 (fiel a {@code give/learner/<elem>}).</li>
 *   <li>{@link #cooldowns}: cooldowns <b>por-instancia</b> (F4/DJT-077) — {@code jutsuId → tick de
 *       mundo en el que vuelve a estar disponible}. Reemplaza los ~80 timers {@code d.<jutsu>_timer}
 *       que el datapack decrementaba <b>cada uno cada tick</b> (S06-R3): aqui no hay barrido, solo se
 *       consulta el jutsu que se castea. Tick <b>absoluto de mundo</b> ({@code Level.getGameTime}, que
 *       persiste en el save): sobrevive a relogin/reinicio sin recalcular.</li>
 * </ul>
 *
 * <p>Los jutsus y elementos se identifican por <b>id data-driven</b> (nunca el indice numerico
 * {@code JutsuId} del datapack). Ids que ya no existan en el registry simplemente no se pueden
 * castear (codec de estado <b>tolerante</b>: nunca bloquea la carga del save).
 *
 * <p>Value object <b>inmutable</b> (patron de sub-estado enchufable de
 * {@link com.zamu.dojutsu.data.PlayerNinjaData}): {@link #DEFAULT} + {@link #CODEC} + withers.
 *
 * @param known         ids de los jutsus aprendidos (orden estable)
 * @param learnProgress progreso secuencial del learner por elemento ({@code d.lrn_<elem>})
 * @param cooldowns     {@code jutsuId → tick de mundo disponible} (por-instancia, DJT-077)
 */
public record JutsuKnowledge(Set<String> known, Map<String, Integer> learnProgress,
        Map<String, Long> cooldowns) {

    /** Sin jutsus (el estado de un jugador nuevo). */
    public static final JutsuKnowledge DEFAULT = new JutsuKnowledge(Set.of(), Map.of(), Map.of());

    /** Codec del sub-estado (compound tolerante: campos ausentes → sin jutsus). */
    public static final Codec<JutsuKnowledge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("known", List.of())
                    .forGetter(k -> List.copyOf(k.known())),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("learnProgress", Map.of())
                    .forGetter(JutsuKnowledge::learnProgress),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("cooldowns", Map.of())
                    .forGetter(JutsuKnowledge::cooldowns)
    ).apply(instance, (known, progress, cooldowns) -> new JutsuKnowledge(new TreeSet<>(known), progress, cooldowns)));

    /**
     * Copias defensivas inmutables con <b>orden estable</b> (TreeSet/TreeMap: {@code Set.copyOf}
     * perderia el orden en listados y en el NBT) y saneado de NBT corrupto (progresos negativos → 0).
     */
    public JutsuKnowledge {
        known = Collections.unmodifiableSet(new TreeSet<>(Objects.requireNonNull(known, "known")));
        TreeMap<String, Integer> sane = new TreeMap<>();
        Objects.requireNonNull(learnProgress, "learnProgress")
                .forEach((element, count) -> sane.put(element, Math.max(0, count)));
        learnProgress = Collections.unmodifiableMap(sane);
        cooldowns = Collections.unmodifiableMap(new TreeMap<>(Objects.requireNonNull(cooldowns, "cooldowns")));
    }

    /** Indica si el ninja conoce un jutsu. */
    public boolean knows(String jutsuId) {
        return known.contains(jutsuId);
    }

    /** Progreso secuencial del learner de un elemento ({@code d.lrn_<elem>}; 0 si nunca uso uno). */
    public int progressOf(String element) {
        return learnProgress.getOrDefault(element, 0);
    }

    /** Indica si el jutsu esta en cooldown al tick de mundo dado (sin cooldown registrado → no). */
    public boolean isOnCooldown(String jutsuId, long nowTick) {
        Long availableAt = cooldowns.get(jutsuId);
        return availableAt != null && nowTick < availableAt;
    }

    /** Ticks restantes de cooldown al tick dado (0 si esta disponible). Para el HUD de DJT-081. */
    public long remainingCooldown(String jutsuId, long nowTick) {
        Long availableAt = cooldowns.get(jutsuId);
        return availableAt == null ? 0L : Math.max(0L, availableAt - nowTick);
    }

    /** Copia conociendo ademas un jutsu (idempotente). */
    public JutsuKnowledge withKnown(String jutsuId) {
        Objects.requireNonNull(jutsuId, "jutsuId");
        TreeSet<String> next = new TreeSet<>(known);
        next.add(jutsuId);
        return new JutsuKnowledge(next, learnProgress, cooldowns);
    }

    /** Copia con el progreso secuencial de un elemento fijado ({@code >= 0}). */
    public JutsuKnowledge withProgress(String element, int count) {
        Objects.requireNonNull(element, "element");
        if (count < 0) {
            throw new IllegalArgumentException("el progreso no puede ser negativo: " + count);
        }
        TreeMap<String, Integer> next = new TreeMap<>(learnProgress);
        next.put(element, count);
        return new JutsuKnowledge(known, next, cooldowns);
    }

    /** Copia con el progreso del elemento a 0 (recibir un learner nuevo, {@code give/learner/<elem>}). */
    public JutsuKnowledge withProgressReset(String element) {
        return withProgress(element, 0);
    }

    /**
     * Copia poniendo un jutsu en cooldown durante {@code cooldownTicks} desde {@code nowTick}. De
     * paso <b>poda</b> los cooldowns ya vencidos a {@code nowTick} (el mapa no crece sin limite; sin
     * barrido por tick). Un {@code cooldownTicks <= 0} no anade cooldown pero si poda los vencidos.
     */
    public JutsuKnowledge withCooldown(String jutsuId, long nowTick, int cooldownTicks) {
        Objects.requireNonNull(jutsuId, "jutsuId");
        TreeMap<String, Long> next = new TreeMap<>();
        cooldowns.forEach((id, availableAt) -> {
            if (availableAt > nowTick) {
                next.put(id, availableAt); // conserva solo los vigentes (poda perezosa)
            }
        });
        if (cooldownTicks > 0) {
            next.put(jutsuId, nowTick + cooldownTicks);
        }
        return new JutsuKnowledge(known, learnProgress, next);
    }
}
