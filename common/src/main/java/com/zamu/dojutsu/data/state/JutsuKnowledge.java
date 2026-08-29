package com.zamu.dojutsu.data.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
 * <p><b>Invariante (F11/DJT-231):</b> {@link #selectedJutsu} solo puede contener un jutsu que este en
 * {@link #known}. Se garantiza en el constructor compacto, no en las puertas de escritura: si dependiera de
 * que cada llamante se acuerde, bastaria un reset o un olvido para dejar seleccionado un jutsu que ya no se
 * conoce, y el selector mostraria algo que no se puede lanzar.
 *
 * @param known         ids de los jutsus aprendidos (orden estable)
 * @param learnProgress progreso secuencial del learner por elemento ({@code d.lrn_<elem>})
 * @param cooldowns     {@code jutsuId → tick de mundo disponible} (por-instancia, DJT-077)
 * @param selectedJutsu jutsu elegido para lanzar con la tecla (F11/DJT-231), o vacio. Vive en el servidor
 *                      —igual que {@code selectedAbility} del ojo— para que sobreviva a la reconexion y para
 *                      que el cast no dependa de que el cliente diga la verdad
 * @param quickSlots    los ocho huecos rapidos que dibuja la rueda (F11/DJT-234), en orden. Siempre tiene
 *                      exactamente {@link #SLOT_COUNT} entradas; {@code ""} es un hueco libre. Vive en el
 *                      servidor por la misma razon que {@link #selectedJutsu}: las teclas 1-8 lanzaran desde
 *                      aqui, y el cliente no puede ser quien diga que hay en cada hueco
 */
public record JutsuKnowledge(Set<String> known, Map<String, Integer> learnProgress,
        Map<String, Long> cooldowns, Optional<String> selectedJutsu, List<String> quickSlots) {

    /** Huecos de la rueda. Ocho porque es lo que cabe legible en un anillo y lo que pidio el diseno. */
    public static final int SLOT_COUNT = 8;

    /** Marca de hueco libre. Cadena vacia y no {@code null} para que la lista sea serializable tal cual. */
    public static final String EMPTY_SLOT = "";

    private static final List<String> NO_SLOTS = Collections.nCopies(SLOT_COUNT, EMPTY_SLOT);

    /** Sin jutsus (el estado de un jugador nuevo). */
    public static final JutsuKnowledge DEFAULT =
            new JutsuKnowledge(Set.of(), Map.of(), Map.of(), Optional.empty(), NO_SLOTS);

    /** Codec del sub-estado (compound tolerante: campos ausentes → sin jutsus). */
    public static final Codec<JutsuKnowledge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("known", List.of())
                    .forGetter(k -> List.copyOf(k.known())),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("learnProgress", Map.of())
                    .forGetter(JutsuKnowledge::learnProgress),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("cooldowns", Map.of())
                    .forGetter(JutsuKnowledge::cooldowns),
            Codec.STRING.optionalFieldOf("selectedJutsu").forGetter(JutsuKnowledge::selectedJutsu),
            Codec.STRING.listOf().optionalFieldOf("quickSlots", NO_SLOTS).forGetter(JutsuKnowledge::quickSlots)
    ).apply(instance, (known, progress, cooldowns, selected, slots) ->
            new JutsuKnowledge(new TreeSet<>(known), progress, cooldowns, selected, slots)));

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
        // Invariante: no se puede tener seleccionado lo que no se conoce. Cae aqui tambien el NBT de una
        // partida en la que el jutsu seleccionado ya no exista en el registry.
        Objects.requireNonNull(selectedJutsu, "selectedJutsu");
        selectedJutsu = selectedJutsu.filter(known::contains);
        quickSlots = normalizeSlots(quickSlots, known);
    }

    /**
     * Deja la lista de huecos en exactamente {@link #SLOT_COUNT} entradas y vacia las que ya no se conocen.
     *
     * <p>Recorta o rellena en vez de rechazar porque esto tambien recibe el NBT de partidas viejas -sin el
     * campo, o con otro numero de huecos si algun dia cambia- y el codec de estado no debe poder bloquear la
     * carga de un save. Misma politica de tolerancia que el resto del sub-estado.
     */
    private static List<String> normalizeSlots(List<String> slots, Set<String> known) {
        Objects.requireNonNull(slots, "quickSlots");
        List<String> sane = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            String id = i < slots.size() ? slots.get(i) : EMPTY_SLOT;
            // Un hueco con un jutsu que no conoces se vacia: la rueda no debe ofrecer lo que no se puede usar.
            sane.add(id != null && known.contains(id) ? id : EMPTY_SLOT);
        }
        return Collections.unmodifiableList(sane);
    }

    /**
     * Jutsu seleccionado, ya validado contra los conocidos por el constructor. Es lo que lanza la tecla de
     * cast; el servidor sigue comprobando chakra, enfriamiento y afinidad en {@code JutsuCastPipeline}.
     */
    public Optional<String> selectedJutsu() {
        return selectedJutsu;
    }

    /** Jutsu del hueco dado, o vacio si el hueco esta libre o el indice cae fuera. */
    public Optional<String> slot(int index) {
        if (index < 0 || index >= SLOT_COUNT) {
            return Optional.empty();
        }
        String id = quickSlots.get(index);
        return id.isEmpty() ? Optional.empty() : Optional.of(id);
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

    /**
     * Copia conociendo ademas un jutsu (idempotente). De paso lo <b>coloca en el primer hueco libre</b> de la
     * rueda, si queda alguno.
     *
     * <p>El autorelleno vive aqui y no en los servicios que llaman a este metodo porque los tres
     * (aprendizaje con learner, entrega por comando/clan, y la copia del Sharingan) significan lo mismo
     * -acabas de aprender esto- y bastaria que uno se olvidara para que a ese jugador la rueda le saliera
     * vacia sin motivo aparente. Reordenar sigue siendo cosa del jugador; esto solo evita la rueda en blanco.
     */
    public JutsuKnowledge withKnown(String jutsuId) {
        Objects.requireNonNull(jutsuId, "jutsuId");
        TreeSet<String> next = new TreeSet<>(known);
        next.add(jutsuId);
        return new JutsuKnowledge(next, learnProgress, cooldowns, selectedJutsu, quickSlots)
                .withSlotAutoFill(jutsuId);
    }

    /**
     * Copia seleccionando un jutsu. Si no esta aprendido, la seleccion se descarta (invariante del
     * constructor): el servidor no acepta que el cliente elija algo que no tiene.
     */
    public JutsuKnowledge withSelected(String jutsuId) {
        Objects.requireNonNull(jutsuId, "jutsuId");
        if (selectedJutsu.filter(jutsuId::equals).isPresent()) {
            return this;
        }
        return new JutsuKnowledge(known, learnProgress, cooldowns, Optional.of(jutsuId), quickSlots);
    }

    /** Copia sin jutsu seleccionado. */
    public JutsuKnowledge withoutSelected() {
        if (selectedJutsu.isEmpty()) {
            return this;
        }
        return new JutsuKnowledge(known, learnProgress, cooldowns, Optional.empty(), quickSlots);
    }

    /**
     * Copia con un jutsu puesto en un hueco. Si no esta aprendido el hueco queda libre (lo sanea el
     * constructor). Si el jutsu ya ocupaba OTRO hueco, ese se vacia: el mismo jutsu dos veces en la rueda es
     * un hueco desperdiciado, y en partida se lee como un fallo.
     */
    public JutsuKnowledge withSlot(int index, String jutsuId) {
        Objects.requireNonNull(jutsuId, "jutsuId");
        if (index < 0 || index >= SLOT_COUNT) {
            return this;
        }
        List<String> next = new ArrayList<>(quickSlots);
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (i != index && jutsuId.equals(next.get(i))) {
                next.set(i, EMPTY_SLOT);
            }
        }
        next.set(index, jutsuId);
        return new JutsuKnowledge(known, learnProgress, cooldowns, selectedJutsu, next);
    }

    /** Copia con un hueco vaciado. */
    public JutsuKnowledge withoutSlot(int index) {
        if (index < 0 || index >= SLOT_COUNT || quickSlots.get(index).isEmpty()) {
            return this;
        }
        List<String> next = new ArrayList<>(quickSlots);
        next.set(index, EMPTY_SLOT);
        return new JutsuKnowledge(known, learnProgress, cooldowns, selectedJutsu, next);
    }

    /**
     * Copia con el jutsu colocado en el primer hueco libre, o {@code this} si ya esta en la rueda o no queda
     * sitio. Es lo que hace que la rueda no nazca vacia: al aprender un jutsu se coloca solo, y el jugador
     * reordena despues.
     */
    public JutsuKnowledge withSlotAutoFill(String jutsuId) {
        Objects.requireNonNull(jutsuId, "jutsuId");
        if (!known.contains(jutsuId) || quickSlots.contains(jutsuId)) {
            return this;
        }
        int free = quickSlots.indexOf(EMPTY_SLOT);
        return free < 0 ? this : withSlot(free, jutsuId);
    }

    /** Copia con el progreso secuencial de un elemento fijado ({@code >= 0}). */
    public JutsuKnowledge withProgress(String element, int count) {
        Objects.requireNonNull(element, "element");
        if (count < 0) {
            throw new IllegalArgumentException("el progreso no puede ser negativo: " + count);
        }
        TreeMap<String, Integer> next = new TreeMap<>(learnProgress);
        next.put(element, count);
        return new JutsuKnowledge(known, next, cooldowns, selectedJutsu, quickSlots);
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
        return new JutsuKnowledge(known, learnProgress, next, selectedJutsu, quickSlots);
    }
}
