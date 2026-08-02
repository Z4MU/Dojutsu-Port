package com.zamu.dojutsu.domain.sage;

import java.util.Locale;
import java.util.Optional;

/**
 * Tipo de modo sabio {@code [S08]}: el linaje que se despierta al reunir energia natural, determinado por
 * <b>donde</b> se medita. Verificado en {@code sage/become_sage.mcfunction} ({@code d.sage_type} 1..3).
 *
 * <p>El <b>Seis Caminos</b> (Six Paths) del datapack <b>no</b> se modela aqui: no se obtiene meditando sino que
 * <b>converge en F9</b> (narrativa/Seis Caminos), y la ficha de diseño lo excluye explicitamente de F6. Dominio
 * puro: los biomas se expresan como ids (el glue los resuelve contra el registro de Minecraft).
 */
public enum SageType {

    /** Sapo: se despierta meditando en un <b>pantano</b> ({@code in_swamp}/{@code in_swamp_hills}). */
    TOAD(1),
    /** Serpiente: en una <b>fortaleza</b> ({@code in_stronghold}). */
    SNAKE(2),
    /** Babosa: en un <b>bosque oscuro</b> ({@code in_dark_forest}/{@code in_dark_forest_hills}). */
    SLUG(3);

    private final int datapackId;

    SageType(int datapackId) {
        this.datapackId = datapackId;
    }

    /** Valor de {@code d.sage_type} en el datapack (1=toad, 2=snake, 3=slug); solo para trazabilidad. */
    public int datapackId() {
        return datapackId;
    }

    /** Id data-driven en minusculas ({@code "toad"}, {@code "snake"}, {@code "slug"}). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Resuelve un tipo por su id data-driven; vacio si no existe (estado tolerante). */
    public static Optional<SageType> byId(String id) {
        for (SageType type : values()) {
            if (type.id().equals(id)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
