package com.zamu.dojutsu.domain.story;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Los <b>dos Sabios</b> que se aparecen al ninja {@code [S19]} (F9/DJT-200). Verificados en
 * {@code main/internal/hagoromo/*} y {@code main/internal/hamura/*}.
 *
 * <h2>La ficha dice "Sage of Six Paths"; el datapack tiene dos hermanos</h2>
 * {@code SageOfSixPathsEventService} suena a un solo evento. Son <b>dos</b>, con carpetas paralelas y —lo que de
 * verdad importa— <b>clanes y recompensas distintas</b>:
 * <ul>
 *   <li><b>Hagoromo</b> se aparece a <b>Uzumaki, Uchiha y Senju</b> —sus tres descendencias— y su camino Yin
 *       despierta el <b>Rinnegan</b>.</li>
 *   <li><b>Hamura</b> se aparece <b>solo a los Hyuga</b>, y su camino Yin despierta el <b>Tenseigan</b>.</li>
 * </ul>
 * El camino <b>Yang</b>, en cambio, es <b>idéntico</b> en los dos: naturaleza Yin-Yang y Modo Sabio de los Seis
 * Caminos. Modelarlo como un solo evento habría borrado justo la diferencia que le da sentido a tener dos.
 *
 * <h2>Un solo sabio por linaje</h2>
 * Los conjuntos de clanes son <b>disjuntos</b>: ningun ninja puede encontrarse con los dos. Tu linaje decide cual
 * de los hermanos te habla, y eso no se elige.
 *
 * @param clans     clanes a los que se aparece ({@code try}); disjuntos entre los dos sabios
 * @param yinEye    id del ojo que despierta su camino Yin
 * @param nameColor color de su nombre en el dialogo, en hex (el mismo para los dos, verificado en sus tellraw)
 */
public enum SageEncounter {

    /** Hagoromo Otsutsuki, el Sabio de los Seis Caminos. Camino Yin → Rinnegan. */
    HAGOROMO(Set.of("uzumaki", "uchiha", "senju"), "rinnegan", 0xAEA2C1),
    /** Hamura Otsutsuki, su hermano. Solo a los Hyuga; camino Yin → Tenseigan. */
    HAMURA(Set.of("hyuga"), "tenseigan", 0xAEA2C1);

    private final Set<String> clans;
    private final String yinEye;
    private final int nameColor;

    SageEncounter(Set<String> clans, String yinEye, int nameColor) {
        this.clans = clans;
        this.yinEye = yinEye;
        this.nameColor = nameColor;
    }

    /** Id data-driven en minusculas ({@code "hagoromo"}, {@code "hamura"}). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Clanes a los que se aparece este sabio. */
    public Set<String> clans() {
        return clans;
    }

    /** Ojo que despierta su camino Yin. */
    public String yinEye() {
        return yinEye;
    }

    /** Color de su nombre en el dialogo (hex RGB). */
    public int nameColor() {
        return nameColor;
    }

    /** Indica si este sabio se aparece a ese clan. */
    public boolean appearsTo(String clanId) {
        return clanId != null && clans.contains(clanId);
    }

    /**
     * El sabio que le corresponde a un clan, o vacio si ese linaje no desciende de ninguno. Los conjuntos son
     * disjuntos, asi que como mucho hay uno.
     */
    public static Optional<SageEncounter> forClan(String clanId) {
        for (SageEncounter sage : values()) {
            if (sage.appearsTo(clanId)) {
                return Optional.of(sage);
            }
        }
        return Optional.empty();
    }

    /** Resuelve un sabio por su id; vacio si no existe (estado tolerante). */
    public static Optional<SageEncounter> byId(String id) {
        for (SageEncounter sage : values()) {
            if (sage.id().equals(id)) {
                return Optional.of(sage);
            }
        }
        return Optional.empty();
    }

    /** Todos los clanes que pueden tener un encuentro, de los dos sabios juntos. */
    public static List<String> allClans() {
        return java.util.Arrays.stream(values())
                .flatMap(sage -> sage.clans.stream())
                .sorted()
                .toList();
    }
}
