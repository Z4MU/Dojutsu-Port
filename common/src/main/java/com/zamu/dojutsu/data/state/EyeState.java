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
 * <p><b>Tener un ojo y tenerlo encendido son cosas distintas (F11/DJT-230).</b> {@link #activeEye} dice
 * <i>cual</i> ojo posee el jugador (equivale a llevar el item en el slot de casco); {@link #enabled} dice si
 * ahora mismo esta <i>encendido</i>. Los consumidores que representan "los poderes del ojo estan funcionando"
 * (habilidades, chakra vision, penalizaciones de fatiga) deben usar {@link #poweredEye()}, no
 * {@link #activeEye()}; los que representan "de que ojo soy dueno" (menu ocular, evolucion, despertar del EMS)
 * siguen usando {@link #activeEye()} — si no, el menu para volver a encenderlo desapareceria al apagarlo.
 *
 * <p>El datapack no tenia apagado: alli "apagar" era quitarse el item, y {@code eye_strain/force_deactivate}
 * devolvia el ojo al inventario con {@code give/check} para poder reequiparlo. Como aqui no hay item, apagar
 * <b>conserva</b> el ojo y su forma eterna.
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
 * @param activeEye        id del ojo que posee el jugador, o vacio si no lleva ninguno. <b>Poseerlo no
 *                         implica tenerlo encendido</b>: ver {@link #enabled} y {@link #poweredEye()}
 * @param enabled          el ojo esta encendido. {@code true} por defecto (quien ya tenia un ojo antes de
 *                         F11/DJT-230 sigue con el encendido al cargar la partida)
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
public record EyeState(Optional<String> activeEye, boolean enabled, boolean eternal,
        Map<EyeFamily, Integer> useLevels, Set<EyeFamily> awakened, Map<String, Integer> timers, int eyeStrain,
        Optional<String> selectedAbility, Optional<GlobalPos> amenominakaOrigin,
        Map<EyeFamily, String> awakenedEyes) {

    /** Ninja nuevo: sin ojo, sin uso, sin despertares, sin fatiga. */
    public static final EyeState DEFAULT =
            new EyeState(Optional.empty(), true, false, Map.of(), Set.of(), Map.of(), 0, Optional.empty(),
                    Optional.empty(), Map.of());

    /**
     * Codec del sub-estado. {@code useLevels}/{@code awakened} viajan por <b>nombre</b> (minusculas, ordenado)
     * y toleran familias desconocidas (se descartan). {@code activeEye}/{@code selectedAbility} son strings
     * opcionales. {@code eyeStrain} se clampa a {@code [0,100]} en el constructor.
     */
    public static final Codec<EyeState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("activeEye").forGetter(EyeState::activeEye),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(EyeState::enabled),
            Codec.BOOL.optionalFieldOf("eternal", false).forGetter(EyeState::eternal),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("useLevels", Map.of())
                    .forGetter(s -> nameKeys(s.useLevels())),
            Codec.STRING.listOf().optionalFieldOf("awakened", List.of()).forGetter(s -> names(s.awakened())),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("timers", Map.of())
                    .forGetter(EyeState::timers),
            Codec.INT.optionalFieldOf("eyeStrain", 0).forGetter(EyeState::eyeStrain),
            Codec.STRING.optionalFieldOf("selectedAbility").forGetter(EyeState::selectedAbility),
            GlobalPos.CODEC.optionalFieldOf("amenominakaOrigin").forGetter(EyeState::amenominakaOrigin),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("awakenedEyes", Map.of())
                    .forGetter(s -> awakenedEyeNames(s.awakenedEyes()))
    ).apply(instance, EyeState::fromCodec));

    /** Normaliza: colecciones inmutables, clamp del strain. */
    public EyeState {
        Objects.requireNonNull(activeEye, "activeEye");
        Objects.requireNonNull(selectedAbility, "selectedAbility");
        useLevels = immutableUseLevels(useLevels);
        awakened = immutableFamilies(awakened);
        timers = Collections.unmodifiableMap(new TreeMap<>(timers));
        eyeStrain = clampStrain(eyeStrain);
        awakenedEyes = immutableAwakenedEyes(awakenedEyes);
    }

    /** Copia inmutable con orden estable de los ojos despertados por familia. */
    private static Map<EyeFamily, String> immutableAwakenedEyes(Map<EyeFamily, String> value) {
        Objects.requireNonNull(value, "awakenedEyes");
        EnumMap<EyeFamily, String> copy = new EnumMap<>(EyeFamily.class);
        copy.putAll(value);
        return Collections.unmodifiableMap(copy);
    }

    /** Uso acumulado de una familia (0 si nunca se uso). */
    public int useLevel(EyeFamily family) {
        return useLevels.getOrDefault(family, 0);
    }

    /**
     * Que ojo <b>concreto</b> desperto el jugador en esa familia, o vacio si no consta (F11/DJT-238).
     *
     * <p>{@link #awakened} guarda la familia; esto guarda la <b>variante</b>. La diferencia importa para el
     * coste pasivo: el datapack cobra menos por llevar el Mangekyo que tu despertaste que por llevar el de
     * otro Uchiha, y con solo la familia esas dos situaciones eran indistinguibles.
     *
     * <p>Vacio en partidas anteriores a DJT-238. Quien lo consulte debe tratar "no consta" con benevolencia
     * -como si casara-, para no cobrarle de mas a quien ya tenia el ojo antes de que esto existiera.
     */
    public Optional<String> awakenedEye(EyeFamily family) {
        return Optional.ofNullable(awakenedEyes.get(family));
    }

    /** Indica si la familia ya desperto ({@code d.<eye>_wake}). */
    public boolean isAwakened(EyeFamily family) {
        return awakened.contains(family);
    }

    /** Valor de un temporizador por clave (0 si no existe). */
    public int timer(String key) {
        return timers.getOrDefault(key, 0);
    }

    /**
     * Ojo cuyos poderes estan <b>funcionando</b> ahora mismo: el ojo activo si esta encendido, vacio si no hay
     * ojo o si esta apagado (F11/DJT-230).
     *
     * <p>Es el metodo que deben usar los consumidores que preguntan "puede hacer esto el jugador": habilidades
     * oculares, chakra vision, penalizaciones de fatiga y el coste pasivo de chakra. Los que preguntan "de que
     * ojo es dueno" (menu, evolucion, EMS) usan {@link #activeEye()}.
     */
    public Optional<String> poweredEye() {
        return enabled ? activeEye : Optional.empty();
    }

    /**
     * Copia encendiendo o apagando el ojo (idempotente). No toca {@link #activeEye} ni {@link #eternal}: apagar
     * <b>no</b> es perder el ojo. Apagar tambien suelta la habilidad seleccionada, porque el menu ocular no
     * aplica con el ojo apagado.
     */
    public EyeState withEnabled(boolean value) {
        if (enabled == value) {
            return this;
        }
        Optional<String> ability = value ? selectedAbility : Optional.empty();
        return new EyeState(activeEye, value, eternal, useLevels, awakened, timers, eyeStrain, ability,
                amenominakaOrigin, awakenedEyes);
    }

    /** Copia con otro ojo activo (idempotente). Equipar/despertar un ojo lo deja <b>encendido</b>. */
    public EyeState withActiveEye(String eyeId) {
        Objects.requireNonNull(eyeId, "eyeId");
        if (enabled && activeEye.filter(eyeId::equals).isPresent()) {
            return this;
        }
        return new EyeState(Optional.of(eyeId), true, eternal, useLevels, awakened, timers, eyeStrain,
                selectedAbility, amenominakaOrigin, awakenedEyes);
    }

    /** Copia sin ojo activo (desactiva; tambien limpia el flag eterno y la seleccion del menu). */
    public EyeState withoutActiveEye() {
        if (activeEye.isEmpty() && enabled && !eternal && selectedAbility.isEmpty()) {
            return this;
        }
        return new EyeState(Optional.empty(), true, false, useLevels, awakened, timers, eyeStrain,
                Optional.empty(), amenominakaOrigin, awakenedEyes);
    }

    /** Copia con el flag de forma eterna (EMS). */
    public EyeState withEternal(boolean value) {
        if (eternal == value) {
            return this;
        }
        return new EyeState(activeEye, enabled, value, useLevels, awakened, timers, eyeStrain, selectedAbility,
                amenominakaOrigin, awakenedEyes);
    }

    /** Copia fijando (absoluto) el uso de una familia ({@code d.<eye>_use = N}). */
    public EyeState withUseLevel(EyeFamily family, int level) {
        Objects.requireNonNull(family, "family");
        EnumMap<EyeFamily, Integer> next = new EnumMap<>(EyeFamily.class);
        next.putAll(useLevels);
        next.put(family, level);
        return new EyeState(activeEye, enabled, eternal, next, awakened, timers, eyeStrain, selectedAbility,
                amenominakaOrigin, awakenedEyes);
    }

    /** Copia marcando una familia como despertada (idempotente). */
    /**
     * Copia marcando la familia como despierta <b>y recordando que ojo concreto</b> fue (F11/DJT-238).
     *
     * <p>Es el wither que deben usar los servicios de despertar y evolucion. El de una sola familia se
     * conserva para quien solo necesite el flag, pero deja {@link #awakenedEyes} sin rellenar, y entonces el
     * coste pasivo no puede distinguir tu ojo del que le robaste a otro.
     */
    public EyeState withAwakenedEye(EyeFamily family, String eyeId) {
        Objects.requireNonNull(family, "family");
        Objects.requireNonNull(eyeId, "eyeId");
        if (awakened.contains(family) && eyeId.equals(awakenedEyes.get(family))) {
            return this;
        }
        EnumSet<EyeFamily> families = EnumSet.noneOf(EyeFamily.class);
        families.addAll(awakened);
        families.add(family);
        EnumMap<EyeFamily, String> eyes = new EnumMap<>(EyeFamily.class);
        eyes.putAll(awakenedEyes);
        eyes.put(family, eyeId);
        return new EyeState(activeEye, enabled, eternal, useLevels, families, timers, eyeStrain,
                selectedAbility, amenominakaOrigin, eyes);
    }

    public EyeState withAwakened(EyeFamily family) {
        Objects.requireNonNull(family, "family");
        if (awakened.contains(family)) {
            return this;
        }
        EnumSet<EyeFamily> next = EnumSet.noneOf(EyeFamily.class);
        next.addAll(awakened);
        next.add(family);
        return new EyeState(activeEye, enabled, eternal, useLevels, next, timers, eyeStrain, selectedAbility,
                amenominakaOrigin, awakenedEyes);
    }

    /** Copia fijando (absoluto) un temporizador por clave. */
    public EyeState withTimer(String key, int value) {
        Objects.requireNonNull(key, "key");
        Map<String, Integer> next = new HashMap<>(timers);
        next.put(key, value);
        return new EyeState(activeEye, enabled, eternal, useLevels, awakened, next, eyeStrain, selectedAbility,
                amenominakaOrigin, awakenedEyes);
    }

    /** Copia con otra fatiga ocular (clampada a 0..100 por el constructor). */
    public EyeState withEyeStrain(int value) {
        if (eyeStrain == clampStrain(value)) {
            return this;
        }
        return new EyeState(activeEye, enabled, eternal, useLevels, awakened, timers, value, selectedAbility,
                amenominakaOrigin, awakenedEyes);
    }

    /** Copia con otra habilidad seleccionada en el menu ocular. */
    public EyeState withSelectedAbility(String abilityId) {
        Objects.requireNonNull(abilityId, "abilityId");
        return new EyeState(activeEye, enabled, eternal, useLevels, awakened, timers, eyeStrain,
                Optional.of(abilityId), amenominakaOrigin, awakenedEyes);
    }

    /** Copia sin habilidad seleccionada. */
    public EyeState withoutSelectedAbility() {
        if (selectedAbility.isEmpty()) {
            return this;
        }
        return new EyeState(activeEye, enabled, eternal, useLevels, awakened, timers, eyeStrain,
                Optional.empty(), amenominakaOrigin, awakenedEyes);
    }

    /** Copia recordando desde donde se salio del Overworld con el Amenominaka ({@code set_original_pos}). */
    public EyeState withAmenominakaOrigin(GlobalPos origin) {
        Objects.requireNonNull(origin, "origin");
        return new EyeState(activeEye, enabled, eternal, useLevels, awakened, timers, eyeStrain, selectedAbility,
                Optional.of(origin), awakenedEyes);
    }

    /** Copia olvidando la posicion original ({@code remove_pos}: al volver, la marca se consume). */
    public EyeState withoutAmenominakaOrigin() {
        if (amenominakaOrigin.isEmpty()) {
            return this;
        }
        return new EyeState(activeEye, enabled, eternal, useLevels, awakened, timers, eyeStrain, selectedAbility,
                Optional.empty(), awakenedEyes);
    }

    private static EyeState fromCodec(Optional<String> activeEye, boolean enabled, boolean eternal,
            Map<String, Integer> useLevels, List<String> awakened, Map<String, Integer> timers, int eyeStrain,
            Optional<String> selectedAbility, Optional<GlobalPos> amenominakaOrigin,
            Map<String, String> awakenedEyes) {
        return new EyeState(activeEye, enabled, eternal, parseUseLevels(useLevels), parseFamilies(awakened),
                timers, eyeStrain, selectedAbility, amenominakaOrigin, parseAwakenedEyes(awakenedEyes));
    }

    /** Serializa el mapa de uso con claves por nombre en minusculas, ordenadas (salida determinista). */
    private static Map<String, Integer> nameKeys(Map<EyeFamily, Integer> levels) {
        Map<String, Integer> out = new TreeMap<>();
        levels.forEach((family, level) -> out.put(family.name().toLowerCase(Locale.ROOT), level));
        return out;
    }

    /** Serializa el mapa de ojos despertados con claves por nombre en minusculas, ordenadas. */
    private static Map<String, String> awakenedEyeNames(Map<EyeFamily, String> eyes) {
        Map<String, String> out = new TreeMap<>();
        eyes.forEach((family, eyeId) -> out.put(family.name().toLowerCase(Locale.ROOT), eyeId));
        return out;
    }

    /** Deserializa el mapa de ojos despertados descartando familias desconocidas (tolerancia). */
    private static Map<EyeFamily, String> parseAwakenedEyes(Map<String, String> raw) {
        EnumMap<EyeFamily, String> out = new EnumMap<>(EyeFamily.class);
        raw.forEach((name, eyeId) -> family(name).ifPresent(f -> out.put(f, eyeId)));
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
