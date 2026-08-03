package com.zamu.dojutsu.api;

import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.domain.progression.NinjaRank;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * <b>Foto inmutable</b> del estado ninja de un jugador (F10/DJT-220). Lo que la API entrega a un tercero cuando
 * pregunta «¿cómo está este jugador?».
 *
 * <h2>Por qué una copia y no el agregado</h2>
 * Devolver el {@link PlayerNinjaData} real sería más barato y sería un error: sus sub-estados son inmutables,
 * pero el agregado tiene <b>puertas de escritura</b> ({@code setChakra}, {@code setEye}…) y entregarlo permitiría
 * a cualquiera saltarse los servicios que existen precisamente para que el chakra no se toque por dos sitios
 * (reglas 10 y 10b). Una vista se lee y ya está; para escribir hay que pasar por {@link DojutsuApi}.
 *
 * <p>Es una <b>foto</b>, no una ventana: refleja el instante en que se pidió. Quien necesite reaccionar a los
 * cambios debe suscribirse a los eventos, no sondear esto.
 *
 * <h2>Qué NO trae</h2>
 * Deliberadamente no expone los temporizadores de ejecución (pulsos de modo, cooldowns internos, contadores de
 * despertar): son detalle de implementación y cambiarían el contrato cada vez que se afinara un sistema. Si algo
 * de eso hace falta de verdad, se añade con su ticket y su motivo.
 *
 * @param rank        rango derivado del ninja_xp (cosmético: no gatea nada)
 * @param ninjaXp     XP ninja global, el sumidero que deriva el rango
 * @param chakra      chakra actual
 * @param maxChakra   chakra máximo
 * @param skillPoints puntos de habilidad sin gastar
 * @param skills      nivel de cada habilidad (0 si nunca se subió)
 * @param affinities  nivel de cada afinidad elemental
 * @param clan        id del clan, o vacío si aún no eligió
 * @param activeEye   id del ojo activo, o vacío
 * @param eternalEye  el ojo activo está en su forma eterna (EMS)
 * @param eyeStrain   fatiga ocular 0..100
 * @param knownJutsus ids de las técnicas que conoce
 * @param sealedBeast id de la Bestia con Cola sellada, o vacío si no es jinchūriki
 * @param sage        es sabio
 */
public record NinjaView(
        NinjaRank rank,
        int ninjaXp,
        int chakra,
        int maxChakra,
        int skillPoints,
        Map<Skill, Integer> skills,
        Map<NatureType, Integer> affinities,
        Optional<String> clan,
        Optional<String> activeEye,
        boolean eternalEye,
        int eyeStrain,
        Set<String> knownJutsus,
        Optional<String> sealedBeast,
        boolean sage) {

    public NinjaView {
        skills = Collections.unmodifiableMap(new EnumMap<>(skills));
        affinities = Collections.unmodifiableMap(new EnumMap<>(affinities));
        knownJutsus = Set.copyOf(knownJutsus);
    }

    /** Nivel de una habilidad (0 si nunca se subió). */
    public int skill(Skill skill) {
        return skills.getOrDefault(skill, 0);
    }

    /** Nivel de una afinidad elemental (0 si no la tiene). */
    public int affinity(NatureType nature) {
        return affinities.getOrDefault(nature, 0);
    }

    /** Indica si lleva una Bestia con Cola sellada. */
    public boolean isJinchuriki() {
        return sealedBeast.isPresent();
    }

    /** Indica si conoce esa técnica. */
    public boolean knows(String jutsuId) {
        return knownJutsus.contains(jutsuId);
    }

    /**
     * Construye la vista desde el estado real. <b>Uso interno</b> del mod: los terceros llaman a
     * {@link DojutsuApi#view}.
     */
    static NinjaView of(PlayerNinjaData data) {
        Map<Skill, Integer> skills = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) {
            skills.put(skill, data.progression().skill(skill).level());
        }
        Map<NatureType, Integer> affinities = new EnumMap<>(NatureType.class);
        for (NatureType nature : NatureType.values()) {
            affinities.put(nature, data.natures().affinity(nature).level());
        }
        return new NinjaView(
                NinjaRank.fromXp(data.progression().ninjaXp()),
                data.progression().ninjaXp(),
                data.chakra().chakra(),
                data.chakra().maxChakra(),
                data.progression().skillPoints(),
                skills,
                affinities,
                data.clan().clan(),
                data.eye().activeEye(),
                data.eye().eternal(),
                data.eye().eyeStrain(),
                data.jutsu().known(),
                data.jinchuriki().sealedBeast(),
                data.sage().sage());
    }
}
