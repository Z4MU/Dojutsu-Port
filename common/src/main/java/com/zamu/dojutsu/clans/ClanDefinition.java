package com.zamu.dojutsu.clans;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.domain.resource.RegenFactor;
import com.zamu.dojutsu.natures.EnumCodecs;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;

/**
 * Definicion data-driven de un clan ({@code clans/join_<clan>} del datapack, {@code [S04]}): colapsa las 14
 * funciones copy-paste en una tabla de datos. Se carga desde {@code data/dojutsu/clans/*.json}
 * (ver {@link ClanReloadListener}); la consume {@code ClanService.join} (DJT-057).
 *
 * <p><b>Extraida funcion a funcion</b> (RA-4): la tabla-resumen de {@code [S04]} omite varios bonus (kenjutsu
 * +10 de Hatake y Kaguya, kenjutsu +5 de Sarutobi, fuinjutsu +10 de Namikaze) — manda el datapack.
 *
 * <p><b>Correccion S04-R3 (DJT-057), resuelta EN LOS DATOS:</b> el bonus de Namikaze dependia de un roll sobre
 * el temporal global {@code d.tmp} no garantizado (wind o lightning +25 al azar); aqui Namikaze declara
 * {@code affinityBonus: lightning 25} <b>determinista</b>. El 50% de Wind de Sarutobi, que si es azar
 * intencionado, se conserva via {@link RandomNature}. La velocidad 0.15 de Namikaze (FTG) requiere el puerto
 * {@code AttributeService}, aun diferido.
 *
 * <p><b>La ficha situaba esta clase en {@code domain/clan/}</b>, pero el dominio no conoce {@code Codec} (regla
 * dura de EP-DOMAIN): como {@code KekkeiGenkaiDefinition}, vive en el modulo que la carga. El clan no es un
 * enum del dominio a proposito: es <b>contenido</b> (un pack puede anadir clanes).
 *
 * @param id               id data-driven del clan ({@code "uchiha"}); debe coincidir con el nombre del archivo
 * @param natures          naturalezas otorgadas al unirse (cada una con su ninja_xp y learner propios)
 * @param kekkeiGenkai     KG otorgados al unirse (Kaguya: shikotsumyaku; Namikaze: swift)
 * @param affinityBonus    niveles de afinidad de regalo ({@code d.aff_<nat> += N})
 * @param skillBonus       niveles de habilidad de regalo ({@code d.<skill> += N})
 * @param maxChakra        chakra maximo del clan ({@code d.max_chakra}; el join rellena el chakra al maximo)
 * @param ninjaXp          ninja_xp explicita del join (sin contar la que dan sus naturalezas/KG)
 * @param startingJutsu    ids de jutsus iniciales (la entrega es F4; se admiten repetidos)
 * @param startingItems    ids de items iniciales (la entrega es F8; Sarutobi lleva 2 chakra_blade)
 * @param potentialDojutsu ids de dojutsus que el clan puede despertar (F5); Otsutsuki tiene dos (50/50)
 * @param randomNature     naturaleza extra con probabilidad {@code 1/oneIn} al unirse (Sarutobi: wind, 1 de 2)
 * @param regenFactor      factor de regeneracion de chakra que activa pertenecer al clan (Otsutsuki/Uzumaki)
 * @param jutsuUnlockTicks desbloqueo progresivo de jutsus de clan ({@code d.<clan>_time}): cada N ticks de
 *                         juego se desbloquea el siguiente (Nara/Kaguya/Akimichi/Aburame: 78000 = 65 min).
 *                         Vacio = el clan no tiene desbloqueo progresivo
 * @param teamColor        color del team/nametag del clan ({@code create_teams} de {@code main/load});
 *                         vacio = sin color (blanco vanilla)
 * @param skipsAffinity    el onboarding salta la seleccion de afinidad <b>y</b> el roll de KG para este clan
 *                         (Lee: {@code join_main} y {@code join_finish} condicionan ambas ramas a
 *                         {@code is_lee}). Antes era un predicate hardcodeado; aqui es dato
 */
public record ClanDefinition(String id, List<NatureType> natures, List<KekkeiGenkai> kekkeiGenkai,
        Map<NatureType, Integer> affinityBonus, Map<Skill, Integer> skillBonus, int maxChakra, int ninjaXp,
        List<String> startingJutsu, List<String> startingItems, List<String> potentialDojutsu,
        Optional<RandomNature> randomNature, Optional<RegenFactor> regenFactor,
        Optional<Integer> jutsuUnlockTicks, Optional<ChatFormatting> teamColor, boolean skipsAffinity) {

    /** Codec estricto de un color de chat vanilla por nombre ({@code "dark_red"}); un color falso falla. */
    private static final Codec<ChatFormatting> COLOR_CODEC = Codec.STRING.comapFlatMap(name -> {
        ChatFormatting color = ChatFormatting.getByName(name);
        return color != null && color.isColor()
                ? DataResult.success(color)
                : DataResult.error(() -> "color de team desconocido: " + name);
    }, formatting -> formatting.getName().toLowerCase(Locale.ROOT));

    public static final Codec<ClanDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(ClanDefinition::id),
            EnumCodecs.NATURE.listOf().optionalFieldOf("natures", List.of()).forGetter(ClanDefinition::natures),
            EnumCodecs.KEKKEI_GENKAI.listOf().optionalFieldOf("kekkeiGenkai", List.of())
                    .forGetter(ClanDefinition::kekkeiGenkai),
            Codec.unboundedMap(EnumCodecs.NATURE, Codec.INT).optionalFieldOf("affinityBonus", Map.of())
                    .forGetter(ClanDefinition::affinityBonus),
            Codec.unboundedMap(EnumCodecs.SKILL, Codec.INT).optionalFieldOf("skillBonus", Map.of())
                    .forGetter(ClanDefinition::skillBonus),
            Codec.INT.fieldOf("maxChakra").forGetter(ClanDefinition::maxChakra),
            Codec.INT.optionalFieldOf("ninjaXp", 0).forGetter(ClanDefinition::ninjaXp),
            Codec.STRING.listOf().optionalFieldOf("startingJutsu", List.of()).forGetter(ClanDefinition::startingJutsu),
            Codec.STRING.listOf().optionalFieldOf("startingItems", List.of()).forGetter(ClanDefinition::startingItems),
            Codec.STRING.listOf().optionalFieldOf("potentialDojutsu", List.of())
                    .forGetter(ClanDefinition::potentialDojutsu),
            RandomNature.CODEC.optionalFieldOf("randomNature").forGetter(ClanDefinition::randomNature),
            EnumCodecs.REGEN_FACTOR.optionalFieldOf("regenFactor").forGetter(ClanDefinition::regenFactor),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("jutsuUnlockTicks")
                    .forGetter(ClanDefinition::jutsuUnlockTicks),
            COLOR_CODEC.optionalFieldOf("teamColor").forGetter(ClanDefinition::teamColor),
            Codec.BOOL.optionalFieldOf("skipsAffinity", false).forGetter(ClanDefinition::skipsAffinity)
    ).apply(instance, ClanDefinition::new));
    // Nota: el group() de RecordCodecBuilder admite 16 campos y este lleva 15. Si un ticket futuro necesita
    // otro, agrupar campos relacionados en un sub-record (como RandomNature) en vez de crecer en plano.

    public ClanDefinition {
        natures = List.copyOf(natures);
        kekkeiGenkai = List.copyOf(kekkeiGenkai);
        affinityBonus = Map.copyOf(affinityBonus);
        skillBonus = Map.copyOf(skillBonus);
        startingJutsu = List.copyOf(startingJutsu);
        startingItems = List.copyOf(startingItems);
        potentialDojutsu = List.copyOf(potentialDojutsu);
    }

    /**
     * Naturaleza extra con probabilidad {@code 1/oneIn} al unirse ({@code join_sarutobi}: roll de
     * {@code random_aff_namikazi} 1..2, actua solo el 1 -> Wind el 50% de las veces).
     *
     * @param nature naturaleza a otorgar si la tirada acierta
     * @param oneIn  probabilidad {@code 1/oneIn} (2 = 50%)
     */
    public record RandomNature(NatureType nature, int oneIn) {

        public static final Codec<RandomNature> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EnumCodecs.NATURE.fieldOf("nature").forGetter(RandomNature::nature),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("oneIn").forGetter(RandomNature::oneIn)
        ).apply(instance, RandomNature::new));
    }
}
