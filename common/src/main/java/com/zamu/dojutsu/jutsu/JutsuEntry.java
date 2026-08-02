package com.zamu.dojutsu.jutsu;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.jutsu.AffinityRequirement;
import com.zamu.dojutsu.domain.jutsu.JutsuDefinition;
import com.zamu.dojutsu.domain.jutsu.KekkeiGenkaiRequirement;
import com.zamu.dojutsu.domain.jutsu.SkillRequirement;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.natures.EnumCodecs;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.util.ExtraCodecs;

/**
 * Entrada data-driven de un jutsu (F4/DJT-073): el {@link JutsuDefinition} de dominio (DJT-013)
 * mas los <b>parametros del efecto</b> en crudo. El codec vive aqui y no en el dominio (regla
 * dura de EP-DOMAIN: el dominio no conoce {@code Codec}) — misma desviacion justificada que
 * {@code KekkeiGenkaiDefinition} (DJT-052) y {@code ClanDefinition} (DJT-056).
 *
 * <p><b>Codec estricto</b> (contenido, no estado): la afinidad requerida referencia una
 * {@link NatureType} por nombre via {@link EnumCodecs#NATURE} — una naturaleza desconocida
 * <b>falla el archivo</b> con error en el log, aislado del resto. El {@code effect} se valida
 * como no-vacio; la validacion estricta contra el resolver de familias llega con el pipeline
 * (DJT-074/076), cuando ese registro exista.
 *
 * <p><b>{@code effectParams} es passthrough:</b> el esquema de los parametros lo define cada
 * familia de efecto (DJT-076) con su propio codec; aqui se conserva el JSON tal cual para no
 * inventar un esquema antes de tiempo ni reescribir los archivos despues.
 *
 * @param definition   contrato de dominio de la tecnica (id, coste, cd, afinidad, categoria, efecto)
 * @param effectParams parametros del efecto en crudo (objeto vacio si el JSON no los trae)
 */
public record JutsuEntry(JutsuDefinition definition, JsonElement effectParams) {

    /** Codec del requisito de afinidad: naturaleza estricta por nombre + nivel minimo. */
    private static final Codec<AffinityRequirement> AFFINITY_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    EnumCodecs.NATURE.fieldOf("nature").forGetter(JutsuEntry::natureOf),
                    Codec.intRange(0, Integer.MAX_VALUE).fieldOf("amount").forGetter(AffinityRequirement::amount)
            ).apply(instance, (nature, amount) ->
                    new AffinityRequirement(nature.name().toLowerCase(Locale.ROOT), amount)));

    /**
     * Codec de <b>afinidad multiple</b> (F4/DJT-091): acepta un objeto suelto {@code "affinity": {...}}
     * (retrocompat: las tecnicas de una sola afinidad) <b>o</b> un array {@code "affinity": [{...},{...}]}
     * (jutsus KG con varias naturalezas componentes). Reserializa como objeto si hay exactamente una.
     */
    private static final Codec<List<AffinityRequirement>> AFFINITIES_CODEC =
            Codec.either(AFFINITY_CODEC.listOf(), AFFINITY_CODEC).xmap(
                    either -> either.map(list -> list, one -> List.of(one)),
                    list -> list.size() == 1 ? Either.right(list.get(0)) : Either.left(list));

    /** Codec del requisito de posesion de KG: Kekkei Genkai estricto por nombre (F4/DJT-091). */
    private static final Codec<KekkeiGenkaiRequirement> KEKKEI_GENKAI_CODEC =
            EnumCodecs.KEKKEI_GENKAI.xmap(
                    kg -> new KekkeiGenkaiRequirement(kg.name().toLowerCase(Locale.ROOT)),
                    JutsuEntry::kekkeiGenkaiOf);

    /** Codec del requisito de habilidad: habilidad estricta por nombre + nivel minimo (F4/DJT-083). */
    private static final Codec<SkillRequirement> SKILL_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    EnumCodecs.SKILL.fieldOf("skill").forGetter(JutsuEntry::skillOf),
                    Codec.intRange(0, Integer.MAX_VALUE).fieldOf("amount").forGetter(SkillRequirement::amount)
            ).apply(instance, (skill, amount) ->
                    new SkillRequirement(skill.name().toLowerCase(Locale.ROOT), amount)));

    public static final Codec<JutsuEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.NON_EMPTY_STRING.fieldOf("id").forGetter(e -> e.definition().id()),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("chakraCost").forGetter(e -> e.definition().chakraCost()),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("cooldownTicks").forGetter(e -> e.definition().cooldownTicks()),
            AFFINITIES_CODEC.optionalFieldOf("affinity", List.of())
                    .forGetter(e -> e.definition().affinityRequirements()),
            KEKKEI_GENKAI_CODEC.optionalFieldOf("kekkeiGenkai")
                    .forGetter(e -> e.definition().kekkeiGenkaiRequirement()),
            SKILL_CODEC.optionalFieldOf("skill").forGetter(e -> e.definition().skillRequirement()),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("levelRequirement", 0)
                    .forGetter(e -> e.definition().levelRequirement()),
            ExtraCodecs.NON_EMPTY_STRING.fieldOf("category").forGetter(e -> e.definition().category()),
            ExtraCodecs.NON_EMPTY_STRING.fieldOf("effect").forGetter(e -> e.definition().effectId()),
            // Optional + instancia fresca por decode: un default compartido seria un JsonObject
            // mutable envenenable para todas las entradas (la leccion de DJT-063 / regla 10b).
            ExtraCodecs.JSON.optionalFieldOf("effectParams")
                    .forGetter(e -> java.util.Optional.of(e.effectParams()))
    ).apply(instance, (id, cost, cd, affinities, kekkeiGenkai, skill, level, category, effect, params) ->
            new JutsuEntry(new JutsuDefinition(id, cost, cd, affinities, kekkeiGenkai, skill, level,
                    category, effect), params.orElseGet(JsonObject::new))));

    public JutsuEntry {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(effectParams, "effectParams");
    }

    /** Id de la tecnica (atajo de {@code definition().id()}). */
    public String id() {
        return definition.id();
    }

    /** La naturaleza del requisito como enum (para codificar; el dominio guarda el id String). */
    private static NatureType natureOf(AffinityRequirement requirement) {
        return NatureType.valueOf(requirement.natureId().toUpperCase(Locale.ROOT));
    }

    /** La habilidad del requisito como enum (para codificar; el dominio guarda el id String). */
    private static Skill skillOf(SkillRequirement requirement) {
        return Skill.valueOf(requirement.skillId().toUpperCase(Locale.ROOT));
    }

    /** El KG del requisito como enum (para codificar; el dominio guarda el id String). */
    private static KekkeiGenkai kekkeiGenkaiOf(KekkeiGenkaiRequirement requirement) {
        return KekkeiGenkai.valueOf(requirement.kekkeiGenkaiId().toUpperCase(Locale.ROOT));
    }
}
