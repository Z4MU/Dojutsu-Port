package com.zamu.dojutsu.domain.trading;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Catalogo de ofertas de un tipo de comerciante {@code [S18]} (F8/DJT-190): un JSON por comerciante en
 * {@code data/dojutsu/trades/}.
 *
 * @param kind   a que comerciante pertenece
 * @param offers sus ofertas, en el orden en que se presentan
 */
public record TraderCatalogue(Kind kind, List<TraderOffer> offers) {

    /** Los dos comerciantes que el datapack reskinea. */
    public enum Kind {
        /** Aldeano convertido: mercancia de consumo (kunai, sellos, pildoras). */
        SHINOBI_VILLAGER,
        /** Mercader errante: la mercancia rara (ojos, ADN, contratos). */
        WANDERING_TRADER;

        /** Id data-driven en minusculas. */
        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** Resuelve un tipo por su id; vacio si no existe. */
        public static Optional<Kind> byId(String id) {
            for (Kind kind : values()) {
                if (kind.id().equals(id)) {
                    return Optional.of(kind);
                }
            }
            return Optional.empty();
        }
    }

    /** Tipo por su id; un tipo desconocido invalida el archivo (codec estricto, como los clanes). */
    private static final Codec<Kind> KIND_CODEC = Codec.STRING.comapFlatMap(
            raw -> Kind.byId(raw)
                    .map(com.mojang.serialization.DataResult::success)
                    .orElseGet(() -> com.mojang.serialization.DataResult.error(
                            () -> "comerciante desconocido: " + raw)),
            Kind::id);

    public static final Codec<TraderCatalogue> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            KIND_CODEC.fieldOf("kind").forGetter(TraderCatalogue::kind),
            TraderOffer.CODEC.listOf().fieldOf("offers").forGetter(TraderCatalogue::offers)
    ).apply(instance, TraderCatalogue::new));
}
