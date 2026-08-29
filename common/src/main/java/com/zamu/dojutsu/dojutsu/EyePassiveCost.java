package com.zamu.dojutsu.dojutsu;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Coste pasivo de chakra por llevar un ojo encendido (F11/DJT-238). Salda la deuda que el javadoc de
 * {@link EyeDefinition} declaraba desde DJT-113: aquel ticket modelo el grafo y el estado, DJT-116 porto la
 * <b>fatiga</b>, y el <b>drenaje</b> se quedo sin portar.
 *
 * <p><b>Es una lista ordenada de reglas, y eso es deliberado.</b> En el datapack cada ojo tiene un
 * {@code cost.mcfunction} que es literalmente una cadena de {@code execute if predicate ... run remove}
 * evaluada de arriba abajo. Modelarlo como reglas en orden —<b>la primera que encaja manda</b>— hace que el
 * JSON se pueda leer al lado del mcfunction original y ver que dicen lo mismo. Un esquema con campos fijos
 * (despierto/implantado) no llegaba: el Sharingan cruza <b>tres</b> niveles de clan con <b>tres</b> tomoe y
 * con si tienes el Mangekyo, o sea doce numeros.
 *
 * @param intervalTicks cada cuantos ticks se cobra. El datapack cuenta hasta 40 (2 segundos) y reinicia
 * @param rules         reglas en orden de evaluacion; la primera que encaja fija el coste
 * @param fallback      coste si ninguna regla encaja. Vacio = no cobrar (y se avisa por log: un ojo con
 *                      coste declarado que no sabe cobrarle a alguien es casi seguro un dato incompleto)
 */
public record EyePassiveCost(int intervalTicks, List<Rule> rules, Optional<Integer> fallback) {

    /** Intervalo del datapack: 40 ticks = 2 segundos. */
    public static final int DEFAULT_INTERVAL_TICKS = 40;

    /**
     * Una linea del {@code cost.mcfunction}: condiciones que deben cumplirse <b>todas</b>, y el coste.
     *
     * <p>Todas las condiciones son opcionales; una regla sin ninguna encaja siempre (el {@code else} final).
     *
     * @param clan         id de clan requerido; admite los compuestos del grafo ({@code otsutsuki_uchiha})
     * @param notClan      ids de clan que <b>excluyen</b> la regla (el {@code unless predicate} del datapack)
     * @param awakened     exige que la familia del ojo este (o no este) despertada
     * @param eyeMatch     exige que el ojo que llevas sea (o no sea) la variante que tu despertaste
     * @param hasMangekyo  exige tener (o no tener) el Mangekyo despierto; solo lo usa el Sharingan
     * @param minUse       uso minimo de la familia, inclusive (los tomoe del Sharingan salen de aqui)
     * @param maxUse       uso maximo de la familia, exclusive
     * @param cost         chakra que se cobra en el intervalo
     */
    public record Rule(Optional<String> clan, List<String> notClan, Optional<Boolean> awakened,
            Optional<Boolean> eyeMatch, Optional<Boolean> hasMangekyo, Optional<Integer> minUse,
            Optional<Integer> maxUse, int cost) {

        public static final Codec<Rule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("clan").forGetter(Rule::clan),
                Codec.STRING.listOf().optionalFieldOf("notClan", List.of()).forGetter(Rule::notClan),
                Codec.BOOL.optionalFieldOf("awakened").forGetter(Rule::awakened),
                Codec.BOOL.optionalFieldOf("eyeMatch").forGetter(Rule::eyeMatch),
                Codec.BOOL.optionalFieldOf("hasMangekyo").forGetter(Rule::hasMangekyo),
                Codec.INT.optionalFieldOf("minUse").forGetter(Rule::minUse),
                Codec.INT.optionalFieldOf("maxUse").forGetter(Rule::maxUse),
                Codec.INT.fieldOf("cost").forGetter(Rule::cost)
        ).apply(instance, Rule::new));

        public Rule {
            Objects.requireNonNull(clan, "clan");
            notClan = List.copyOf(Objects.requireNonNull(notClan, "notClan"));
            Objects.requireNonNull(awakened, "awakened");
            Objects.requireNonNull(eyeMatch, "eyeMatch");
            Objects.requireNonNull(hasMangekyo, "hasMangekyo");
            Objects.requireNonNull(minUse, "minUse");
            Objects.requireNonNull(maxUse, "maxUse");
            if (cost < 0) {
                throw new IllegalArgumentException("el coste pasivo no puede ser negativo: " + cost);
            }
        }
    }

    public static final Codec<EyePassiveCost> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("intervalTicks", DEFAULT_INTERVAL_TICKS)
                    .forGetter(EyePassiveCost::intervalTicks),
            Rule.CODEC.listOf().optionalFieldOf("rules", List.of()).forGetter(EyePassiveCost::rules),
            Codec.INT.optionalFieldOf("fallback").forGetter(EyePassiveCost::fallback)
    ).apply(instance, EyePassiveCost::new));

    public EyePassiveCost {
        if (intervalTicks <= 0) {
            throw new IllegalArgumentException("el intervalo debe ser positivo: " + intervalTicks);
        }
        rules = List.copyOf(Objects.requireNonNull(rules, "rules"));
        Objects.requireNonNull(fallback, "fallback");
    }
}
