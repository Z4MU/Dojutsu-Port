package com.zamu.dojutsu.dojutsu;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zamu.dojutsu.domain.eye.EyeEvolution;
import com.zamu.dojutsu.domain.eye.EyeFamily;
import com.zamu.dojutsu.domain.eye.EyeRequirement;
import com.zamu.dojutsu.natures.EnumCodecs;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Definicion data-driven de un ojo (dojutsu) del datapack {@code [S05]}: un nodo del grafo de evolucion. Colapsa
 * las 372 funciones {@code dojutsu/**} en una tabla de datos. Se carga desde {@code data/dojutsu/eyes/*.json}
 * (ver {@link EyeReloadListener}) al {@link EyeRegistry}; la consumiran los servicios de despertar/evolucion
 * (DJT-114/115), el strain (DJT-116) y el menu ocular (DJT-117).
 *
 * <p><b>El grafo es el modelo, no las ramas hardcodeadas</b> (RA-3): las evoluciones y sus requisitos son datos
 * ({@link EyeEvolution} + {@link EyeRequirement}), asi las <b>rutas divergentes</b> (Uchiha/Otsutsuki vs.
 * implante, R6) se expresan como aristas distintas y no como {@code if} en el codigo.
 *
 * <p><b>Colapso 33 -> logicos (R4):</b> los 9 indices Eternal Mangekyo no son ojos aparte, son el flag
 * {@code eternal} de {@link com.zamu.dojutsu.data.state.EyeState}. Como un ojo puede volverse eterno se declara
 * en {@link #eternalRequirements} (vacio = no eternalizable). El {@code Eye:18} (placeholder "naka") se modela
 * con {@code implemented=false}: se carga sin romper, pero los servicios lo tratan como no jugable (R7).
 *
 * <p><b>Vive en el modulo que la carga, no en {@code domain}</b> (misma regla que {@link ClanDefinition} y
 * {@code JutsuEntry}): el dominio no conoce {@code Codec}. Los tipos del grafo ({@link EyeFamily},
 * {@link EyeEvolution}, {@link EyeRequirement}) si son dominio puro.
 *
 * <p><b>Coste pasivo y eye strain: DJT-116.</b> Este ticket (DJT-113) modela el grafo y el estado; el drenaje
 * de chakra por llevar el ojo y la curva de fatiga se anaden como campos tolerantes cuando llegue su servicio.
 *
 * @param id                    id data-driven del ojo ({@code "sharingan"}, {@code "mangekyo_itachi"}); debe
 *                              coincidir con el nombre del archivo
 * @param family                familia/linaje del ojo (agrupa el {@code useLevels} del estado)
 * @param implemented           {@code false} para el placeholder ({@code Eye:18}); se carga pero no es jugable
 * @param awakenMaxChakraBonus  {@code d.max_chakra += N} al despertar el ojo (Byakugan/Sharingan 20, Rinnegan 100)
 * @param awakenNinjaXpBonus    {@code d.ninja_xp += N} al despertar (Byakugan/Sharingan 20, Rinnegan 150)
 * @param awakenTicks           ticks de pertenencia a un clan con este dojutsu para que despierte
 *                              ({@code <eye>_waketime}: Byakugan/Sharingan 78000, Ketsuryugan 24000). Vacio = el
 *                              ojo <b>no</b> se despierta por clan (es un destino de evolucion: Mangekyo/Rinnegan/
 *                              Tenseigan). Lo consume {@code AwakeningService} (F5/DJT-114)
 * @param awakenInitialUse      uso de la familia que se fija <b>al despertar</b> el ojo ({@code sharingan/awaken}
 *                              hace {@code d.sharingan_use = 78000}: nace en tomoe 1). Vacio = arranca en 0
 *                              (Byakugan/Ketsuryugan no fijan uso inicial). Lo consume {@code AwakeningService}
 * @param abilities             ids de habilidades oculares del menu (DJT-117/118+); solo datos en este ticket
 * @param evolutions            aristas salientes del grafo (a que otros ojos evoluciona y con que requisitos)
 * @param eternalRequirements   requisitos para que el ojo pase a su forma eterna (EMS); vacio = no eternalizable
 * @param chakraVision          el ojo otorga chakra vision {@code [S25]} (DJT-125); {@code false} = no la da
 * @param chakraVisionUseLevel  uso minimo de la familia para que el ojo de vision ({@code sharingan}: solo el
 *                              maduro de 3 tomoe, {@code Eye:4}, uso >= 216000; 0 = desde que se lleva)
 * @param passiveCost           coste pasivo de chakra por llevarlo encendido (F11/DJT-238), o vacio si el ojo
 *                              no cobra nada. Salda la deuda que declara el parrafo de arriba: DJT-116 porto
 *                              la fatiga y dejo el drenaje sin portar
 * @param awakenKekkeiGenkai    id del kekkei genkai que se concede <b>al despertar/alcanzar</b> este ojo (el
 *                              {@code natures/give/kg/blaze} que el Mangekyō de Sasuke otorga en
 *                              {@code mangekyo/awaken}); vacio = no concede ninguno
 */
public record EyeDefinition(String id, EyeFamily family, boolean implemented, int awakenMaxChakraBonus,
        int awakenNinjaXpBonus, Optional<Integer> awakenTicks, Optional<Integer> awakenInitialUse,
        List<String> abilities, List<EyeEvolution> evolutions, List<EyeRequirement> eternalRequirements,
        boolean chakraVision, int chakraVisionUseLevel, Optional<String> awakenKekkeiGenkai,
        Optional<EyePassiveCost> passiveCost) {

    /** Codec del tipo de requisito por su nombre en minusculas ({@code "use_level"}, {@code "clan"}). */
    private static final Codec<EyeRequirement.Type> REQUIREMENT_TYPE = Codec.STRING.comapFlatMap(name -> {
        for (EyeRequirement.Type type : EyeRequirement.Type.values()) {
            if (type.name().toLowerCase(Locale.ROOT).equals(name)) {
                return DataResult.success(type);
            }
        }
        return DataResult.error(() -> "tipo de requisito de ojo desconocido: " + name);
    }, type -> type.name().toLowerCase(Locale.ROOT));

    /** Codec de un requisito de arista. {@code min/max/value} son opcionales segun el tipo. */
    private static final Codec<EyeRequirement> REQUIREMENT = RecordCodecBuilder.create(instance -> instance.group(
            REQUIREMENT_TYPE.fieldOf("type").forGetter(EyeRequirement::type),
            Codec.INT.optionalFieldOf("min", 0).forGetter(EyeRequirement::min),
            Codec.INT.optionalFieldOf("max", Integer.MAX_VALUE).forGetter(EyeRequirement::max),
            Codec.STRING.optionalFieldOf("value", "").forGetter(EyeRequirement::value)
    ).apply(instance, EyeRequirement::new));

    /** Codec de una arista de evolucion (destino + requisitos AND + pool aleatorio opcional de variantes). */
    private static final Codec<EyeEvolution> EVOLUTION = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("to").forGetter(EyeEvolution::to),
            REQUIREMENT.listOf().optionalFieldOf("requirements", List.of()).forGetter(EyeEvolution::requirements),
            Codec.STRING.listOf().optionalFieldOf("variants", List.of()).forGetter(EyeEvolution::variants)
    ).apply(instance, EyeEvolution::new));

    public static final Codec<EyeDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(EyeDefinition::id),
            EnumCodecs.EYE_FAMILY.fieldOf("family").forGetter(EyeDefinition::family),
            Codec.BOOL.optionalFieldOf("implemented", true).forGetter(EyeDefinition::implemented),
            Codec.INT.optionalFieldOf("awakenMaxChakraBonus", 0).forGetter(EyeDefinition::awakenMaxChakraBonus),
            Codec.INT.optionalFieldOf("awakenNinjaXpBonus", 0).forGetter(EyeDefinition::awakenNinjaXpBonus),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("awakenTicks").forGetter(EyeDefinition::awakenTicks),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("awakenInitialUse")
                    .forGetter(EyeDefinition::awakenInitialUse),
            Codec.STRING.listOf().optionalFieldOf("abilities", List.of()).forGetter(EyeDefinition::abilities),
            EVOLUTION.listOf().optionalFieldOf("evolutions", List.of()).forGetter(EyeDefinition::evolutions),
            REQUIREMENT.listOf().optionalFieldOf("eternalRequirements", List.of())
                    .forGetter(EyeDefinition::eternalRequirements),
            Codec.BOOL.optionalFieldOf("chakraVision", false).forGetter(EyeDefinition::chakraVision),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("chakraVisionUseLevel", 0)
                    .forGetter(EyeDefinition::chakraVisionUseLevel),
            Codec.STRING.optionalFieldOf("awakenKekkeiGenkai").forGetter(EyeDefinition::awakenKekkeiGenkai),
            EyePassiveCost.CODEC.optionalFieldOf("passiveCost").forGetter(EyeDefinition::passiveCost)
    ).apply(instance, EyeDefinition::new));

    public EyeDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("el id del ojo no puede estar vacio");
        }
        if (family == null) {
            throw new IllegalArgumentException("el ojo '" + id + "' necesita family");
        }
        abilities = List.copyOf(abilities);
        evolutions = List.copyOf(evolutions);
        eternalRequirements = List.copyOf(eternalRequirements);
    }

    /** {@code true} si el ojo tiene al menos una forma eterna (EMS) alcanzable. */
    public boolean isEternalizable() {
        return !eternalRequirements.isEmpty();
    }

    /**
     * {@code true} si el ojo otorga chakra vision con el uso actual de su familia. El {@code sharingan} solo la
     * da maduro ({@code chakraVisionUseLevel}=216000, el 3-tomoe {@code Eye:4}); el resto que la declaran, desde
     * que se llevan ({@code chakraVisionUseLevel}=0). Decision pura {@code [S25]} (DJT-125).
     *
     * @param familyUseLevel uso acumulado de la familia del ojo ({@code d.<eye>_use})
     */
    public boolean grantsChakraVision(int familyUseLevel) {
        return chakraVision && familyUseLevel >= chakraVisionUseLevel;
    }
}
