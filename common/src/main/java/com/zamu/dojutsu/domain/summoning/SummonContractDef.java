package com.zamu.dojutsu.domain.summoning;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Definicion <b>data-driven</b> de un contrato de invocacion {@code [S15]} (F8/DJT-175): con que especie se firma
 * y en que tier cae. Un JSON por contrato en {@code data/dojutsu/contracts/}.
 *
 * <h2>27 especies eran 27 lineas repetidas cuatro veces</h2>
 * El datapack las multiplexa a mano en <b>cuatro</b> sitios distintos: {@code contract_main} (dar el contrato),
 * {@code summon_main} (etiquetar), {@code jutsu/summoning/setup} (pasar la etiqueta al circulo) y
 * {@code do_summon} (invocar de verdad). Son ~108 lineas casi identicas, y anadir una especie obliga a tocar las
 * cuatro. Aqui es un archivo por especie y <b>cero codigo</b>: un pack puede anadir contratos sin recompilar
 * ({@code [RA-3]}).
 *
 * <p><b>{@code tame} y {@code saddle}</b> salen de {@code do_summon}: las monturas se invocan ya domadas y con
 * silla ({@code Tame:1, SaddleItem}), y los burros y mulas ademas con cofres. Son datos de la especie, no ramas.
 *
 * @param id          id del contrato, igual al nombre del archivo ({@code "wolf"})
 * @param entityType  id del tipo de entidad a invocar ({@code "minecraft:wolf"})
 * @param tier        tier al que pertenece; fija los requisitos
 * @param tame        se invoca ya domada y perteneciente al invocador
 * @param saddle      se invoca con silla (monturas)
 * @param chest       se invoca con cofres (burro y mula)
 */
public record SummonContractDef(String id, String entityType, SummonTier tier, boolean tame, boolean saddle,
        boolean chest) {

    /** Tier por su id; un tier desconocido invalida el archivo entero (codec estricto, como los clanes). */
    private static final Codec<SummonTier> TIER_CODEC = Codec.STRING.comapFlatMap(
            raw -> SummonTier.byId(raw)
                    .map(com.mojang.serialization.DataResult::success)
                    .orElseGet(() -> com.mojang.serialization.DataResult.error(() -> "tier desconocido: " + raw)),
            SummonTier::id);

    /** Codec del contrato; los tres flags son opcionales y caen a {@code false}. */
    public static final Codec<SummonContractDef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(SummonContractDef::id),
            Codec.STRING.fieldOf("entityType").forGetter(SummonContractDef::entityType),
            TIER_CODEC.fieldOf("tier").forGetter(SummonContractDef::tier),
            Codec.BOOL.optionalFieldOf("tame", false).forGetter(SummonContractDef::tame),
            Codec.BOOL.optionalFieldOf("saddle", false).forGetter(SummonContractDef::saddle),
            Codec.BOOL.optionalFieldOf("chest", false).forGetter(SummonContractDef::chest)
    ).apply(instance, SummonContractDef::new));
}
