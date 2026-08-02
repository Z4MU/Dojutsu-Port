package com.zamu.dojutsu.natures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import java.util.List;
import java.util.Map;

/**
 * Definicion data-driven de un Kekkei Genkai ({@code natures/give/kg/<kg>} del datapack, {@code [S03]}): que
 * naturalezas <b>implica</b>, cuanta ninja_xp otorga y que bonus de afinidad regala.
 *
 * <p>Se carga desde {@code data/dojutsu/kekkei_genkai/*.json} (ver {@link KekkeiGenkaiReloadListener}). Los
 * componentes viven en JSON y <b>no</b> en el enum a proposito: hardcodearlos era el error que la ficha del
 * ticket pedia evitar, y asi un pack puede redefinir la composicion sin tocar el codigo.
 *
 * <p><b>Ninguno de estos numeros es uniforme</b> (RA-4, auditados uno a uno): la ninja_xp va de <b>8 a 20</b>
 * ({@code [S03]} dice "KG = 10" y es <b>falso</b>), los componentes son 0, 1, 2 o 3, y dos KG regalan afinidad
 * ({@code ice} -> water +10; {@code blaze} -> fire +20).
 *
 * @param kekkeiGenkai   KG que define
 * @param components     naturalezas que implica (pueden ser 0: {@code shikotsumyaku}, {@code jugo_clan})
 * @param ninjaXp        ninja_xp que otorga obtenerlo
 * @param affinityBonus  niveles de afinidad de regalo por naturaleza (normalmente vacio)
 */
public record KekkeiGenkaiDefinition(KekkeiGenkai kekkeiGenkai, List<NatureType> components, int ninjaXp,
        Map<NatureType, Integer> affinityBonus) {

    /** Codecs por nombre, <b>estrictos</b> (un id desconocido falla y valida las refs del JSON): {@link EnumCodecs}. */
    public static final Codec<KekkeiGenkaiDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EnumCodecs.KEKKEI_GENKAI.fieldOf("id").forGetter(KekkeiGenkaiDefinition::kekkeiGenkai),
            EnumCodecs.NATURE.listOf().optionalFieldOf("components", List.of())
                    .forGetter(KekkeiGenkaiDefinition::components),
            Codec.INT.fieldOf("ninjaXp").forGetter(KekkeiGenkaiDefinition::ninjaXp),
            Codec.unboundedMap(EnumCodecs.NATURE, Codec.INT).optionalFieldOf("affinityBonus", Map.of())
                    .forGetter(KekkeiGenkaiDefinition::affinityBonus)
    ).apply(instance, KekkeiGenkaiDefinition::new));

    public KekkeiGenkaiDefinition {
        components = List.copyOf(components);
        affinityBonus = Map.copyOf(affinityBonus);
    }
}
