package com.zamu.dojutsu.data.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zamu.dojutsu.domain.eye.EyeFamily;
import com.zamu.dojutsu.domain.strain.StrainMeter;
import net.minecraft.core.GlobalPos;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Sub-estado del sistema de ojos (dojutsu) del ninja {@code [S05]}: que ojo lleva activo, si esta en su forma
 * eterna, cuanto ha usado cada familia (para evolucionar), que familias ha despertado, los temporizadores de
 * despertar por clan, la fatiga ocular y la habilidad seleccionada del menu.
 *
 * <p>Value object <b>inmutable</b> (patron de sub-estado enchufable de
 * {@link com.zamu.dojutsu.data.PlayerNinjaData}, como {@link NatureProfile}): expone {@link #DEFAULT},
 * {@link #CODEC} y withers; para cambiar algo se <b>reemplaza</b>, nunca se muta (regla 10b).
 *
 * <p><b>Colapso 33 -> logicos (R4):</b> el "ojo equipado" del datapack (un item {@code spider_eye} con NBT
 * {@code Eye:N} en el slot de casco) se modela como <b>estado</b>, no como armadura. Los 9 indices Eternal
 * Mangekyo son {@code activeEye} + {@link #eternal}, no ids aparte.
 *
 * <p><b>El eye strain se persiste como {@code int}</b> (0..100), no como {@link StrainMeter}: el meter es un
 * objeto <b>mutable</b> con listeners (servicio), incompatible con un value object inmutable. El servicio de
 * strain (DJT-116) reconstruye un {@link StrainMeter} transitorio con {@link #eyeStrain()}, opera y escribe el
 * nuevo valor con {@link #withEyeStrain(int)} — mismo "servicio puro + glue" del resto del proyecto.
 *
 * <p><b>Codec tolerante</b> (como todo {@code data.state}): las familias desconocidas (NBT de una version
 * futura o corrupto) se descartan al leer en vez de romper la carga; un ojo activo desconocido se conserva como
 * string (el servicio decidira que hacer si su definicion no existe).
 *
 * @param activeEye        id del ojo activo, o vacio si no lleva ninguno
 * @param eternal          el ojo activo esta en su forma eterna (EMS)
 * @param useLevels        uso acumulado por familia ({@code d.<eye>_use}); dispara las evoluciones
 * @param awakened         familias ya despertadas ({@code d.<eye>_wake}); evita volver a despertar
 * @param timers           temporizadores por proposito del sistema de ojos, por clave (despertar por clan
 *                         {@code d.byak_waketime}/{@code d.shar_waketime}; regen de fatiga {@code d.eyes_timer})
 * @param eyeStrain        fatiga ocular actual 0..100 ({@code d.eye_strain})
 * @param selectedAbility  id de la habilidad seleccionada en el menu ocular, o vacio (DJT-117)
 * @param amenominakaOrigin desde donde se salio del Overworld con el Amenominaka, o vacio (F10/DJT-226). El
 *                          datapack lo guarda como un {@code marker} + {@code forceload} de su chunk; aqui son
 *                          coordenadas en el estado, igual que la marca del Dios del Trueno (DJT-180), asi que
 *                          <b>no se mantiene ningun chunk cargado</b> por tener un regreso pendiente
 */
public record EyeState(Optional<String> activeEye, boolean eternal, Map<EyeFamily, Integer> useLevels,
        Set<EyeFamily> awakened, Map<String, Integer> timers, int eyeStrain,
        Optional<String> selectedAbility, Optional<GlobalPos> amenominakaOrigin) {

    /** Ninja nuevo: sin ojo, sin uso, sin despertares, sin fatiga. */
    public static final EyeState DEFAULT =
            new EyeState(Optional.empty(), false, Map.of(), Set.of(), Map.of(), 0, Optional.empty(),
                    Optional.empty());

    /**
     * Codec del sub-estado. {@code useLevels}/{@code awakened} viajan por <b>nombre</b> (minusculas, ordenado)
     * y toleran familias desconocidas (se descartan). {@code activeEye}/{@code selectedAbility} son strings
     * opcionales. {@code eyeStrain} se clampa a {@code [0,100]} en el constructor.
     */
    public static final Codec<EyeState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("activeEye").forGetter(EyeState::activeEye),
            Codec.BOOL.optionalFieldOf("eternal", false).forGetter(EyeState::eternal),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("useLevels", Map.of())
                    .forGetter(s -> nameKeys(s.useLevels())),
            Codec.STRING.listOf().optionalFieldOf("awakened", List.of()).forGetter(s -> names(s.awakened())),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("timers", Map.of())
                    .forGetter(EyeState::timers),
            Codec.INT.optionalFieldOf("eyeStrain", 0).forGetter(EyeState::eyeStrain),
            Codec.STRING.optionalFieldOf("selectedAbility").forGetter(EyeState::selectedAbility),
            GlobalPos.CODEC.optionalFieldOf("amenominakaOrigin").forGetter(EyeState::amenominakaOrigin)
    ).apply(instance, EyeState::fromCodec));

    /** Normaliza: colecciones inmutables, clamp del strain. */
    public EyeState {
        Objects.requireNonNull(activeEye, "activeEye");
        Objects.requireNonNull(selectedAbility, "selectedAbility");
        useLevels = immutableUseLevels(useLevels);
        awakened = immutableFamilies(awakened);
        timers = Collections.unmodifiableMap(new TreeMap<>(timers));
        eyeStrain = clampStrain(eyeStrain);
    }

    /** Uso acumulado de una familia (0 si nunca se uso). */
    public int useLevel(EyeFamily family) {
        return useLevels.getOrDefault(family, 0);
    }

    /** Indica si la familia ya desperto ({@code d.<eye>_wake}). */
    public boolean isAwakened(EyeFamily family) {
        return awakened.contains(family);
    }

    /** Valor de un temporizador por clave (0 si no existe). */
    public int timer(String key) {
        return timers.getOrDefault(key, 0);
    }

    /** Copia con otro ojo activo (idempotente). */
    public EyeState withActiveEye(String eyeId) {
        Objects.requireNonNull(eyeId, "eyeId");
        if (activeEye.filter(eyeId::equals).isPresent()) {
            return this;
        }
        return new EyeState(Optional.of(eyeId), eternal, useLevels, awakened, timers, eyeStrain,
                selectedAbility, amenominakaOrigin);
    }

    /** Copia sin ojo activo (desactiva; tambien limpia el flag eterno y la seleccion del menu). */
    public EyeState withoutActiveEye() {
        if (activeEye.isEmpty() && !eternal && selectedAbility.isEmpty()) {
            return this;
        }
        return new EyeState(Optional.empty(), false, useLevels, awakened, timers, eyeStrain, Optional.empty(), amenominakaOrigin);
    }

    /** Copia con el flag de forma eterna (EMS). */
    public EyeState withEternal(boolean value) {
        if (eternal == value) {
            return this;
        }
        return new EyeState(activeEye, value, useLevels, awakened, timers, eyeStrain, selectedAbility, amenominakaOrigin);
    }

    /** Copia fijando (absoluto) el uso de una familia ({@code d.<eye>_use = N}). */
    public EyeState withUseLevel(EyeFamily family, int level) {
        Objects.requireNonNull(family, "family");
        EnumMap<EyeFamily, Integer> next = new EnumMap<>(EyeFamily.class);
        next.putAll(useLevels);
        next.put(family, level);
        return new EyeState(activeEye, eternal, next, awakened, timers, eyeStrain, selectedAbility, amenominakaOrigin);
    }

    /** Copia marcando una familia como despertada (idempotente). */
    public EyeState withAwakened(EyeFamily family) {
        Objects.requireNonNull(family, "family");
        if (awakened.contains(family)) {
            return this;
        }
        EnumSet<EyeFamily> next = EnumSet.noneOf(EyeFamily.class);
        next.addAll(awakened);
        next.add(family);
        return new EyeState(activeEye, eternal, useLevels, next, timers, eyeStrain, selectedAbility, amenominakaOrigin);
    }

    /** Copia fijando (absoluto) un temporizador por clave. */
    public EyeState withTimer(String key, int value) {
        Objects.requireNonNull(key, "key");
        Map<String, Integer> next = new HashMap<>(timers);
        next.put(key, value);
        return new EyeState(activeEye, eternal, useLevels, awakened, next, eyeStrain, selectedAbility, amenominakaOrigin);
    }

    /** Copia con otra fatiga ocular (clampada a 0..100 por el constructor). */
    public EyeState withEyeStrain(int value) {
        if (eyeStrain == clampStrain(value)) {
            return this;
        }
        return new EyeState(activeEye, eternal, useLevels, awakened, timers, value, selectedAbility, amenominakaOrigin);
    }

    /** Copia con otra habilidad seleccionada en el menu ocular. */
    public EyeState withSelectedAbility(String abilityId) {
        Objects.requireNonNull(abilityId, "abilityId");
        return new EyeState(activeEye, eternal, useLevels, awakened, timers, eyeStrain, Optional.of(abilityId), amenominakaOrigin);
    }

    /** Copia sin habilidad seleccionada. */
    public EyeState withoutSelectedAbility() {
        if (selectedAbility.isEmpty()) {
            return this;
        }
        return new EyeState(activeEye, eternal, useLevels, awakened, timers, eyeStrain, Optional.empty(), amenominakaOrigin);
    }

    /** Copia recordando desde donde se salio del Overworld con el Amenominaka ({@code set_original_pos}). */
    public EyeState withAmenominakaOrigin(GlobalPos origin) {
        Objects.requireNonNull(origin, "origin");
        return new EyeState(activeEye, eternal, useLevels, awakened, timers, eyeStrain, selectedAbility,
                Optional.of(origin));
    }

    /** Copia olvidando la posicion original ({@code remove_pos}: al volver, la marca se consume). */
    public EyeState withoutAmenominakaOrigin() {
        if (amenominakaOrigin.isEmpty()) {
            return this;
        }
        return new EyeState(activeEye, eternal, useLevels, awakened, timers, eyeStrain, selectedAbility,
                Optional.empty());
    }

    private static EyeState fromCodec(Optional<String> activeEye, boolean eternal, Map<String, Integer> useLevels,
            List<String> awakened, Map<String, Integer> timers, int eyeStrain,
            Optional<String> selectedAbility, Optional<GlobalPos> amenominakaOrigin) {
        return new EyeState(activeEye, eternal, parseUseLevels(useLevels), parseFamilies(awakened), timers,
                eyeStrain, selectedAbility, amenominakaOrigin);
    }

    /** Serializa el mapa de uso con claves por nombre en minusculas, ordenadas (salida determinista). */
    private static Map<String, Integer> nameKeys(Map<EyeFamily, Integer> levels) {
        Map<String, Integer> out = new TreeMap<>();
        levels.forEach((family, level) -> out.put(family.name().toLowerCase(Locale.ROOT), level));
        return out;
    }

    /** Serializa un set de familias como nombres en minusculas, ordenados. */
    private static List<String> names(Set<EyeFamily> families) {
        return families.stream().map(f -> f.name().toLowerCase(Locale.ROOT)).sorted().toList();
    }

    /** Deserializa el mapa de uso descartando familias desconocidas (tolerancia). */
    private static Map<EyeFamily, Integer> parseUseLevels(Map<String, Integer> raw) {
        EnumMap<EyeFamily, Integer> out = new EnumMap<>(EyeFamily.class);
        raw.forEach((name, level) -> family(name).ifPresent(f -> out.put(f, level)));
        return out;
    }

    /** Deserializa nombres de familia descartando los desconocidos (tolerancia). */
    private static Set<EyeFamily> parseFamilies(List<String> names) {
        EnumSet<EyeFamily> out = EnumSet.noneOf(EyeFamily.class);
        for (String name : names) {
            family(name).ifPresent(out::add);
        }
        return out;
    }

    private static Optional<EyeFamily> family(String name) {
        for (EyeFamily f : EyeFamily.values()) {
            if (f.name().toLowerCase(Locale.ROOT).equals(name)) {
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }

    private static Map<EyeFamily, Integer> immutableUseLevels(Map<EyeFamily, Integer> given) {
        // Ojo: new EnumMap<>(mapa) lanza si el mapa esta vacio y no es un EnumMap (caso DEFAULT). Se copia con
        // putAll sobre un EnumMap por-clase para evitar el gotcha.
        EnumMap<EyeFamily, Integer> copy = new EnumMap<>(EyeFamily.class);
        copy.putAll(given);
        return Collections.unmodifiableMap(copy);
    }

    private static Set<EyeFamily> immutableFamilies(Set<EyeFamily> given) {
        return Collections.unmodifiableSet(given.isEmpty() ? EnumSet.noneOf(EyeFamily.class) : EnumSet.copyOf(given));
    }

    private static int clampStrain(int value) {
        return Math.max(StrainMeter.MIN, Math.min(value, StrainMeter.MAX));
    }
}
