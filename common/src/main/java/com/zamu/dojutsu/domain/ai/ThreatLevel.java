package com.zamu.dojutsu.domain.ai;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Los seis <b>niveles de amenaza</b> de un shinobi NPC {@code [S17]} (F8/DJT-185). Verificados en
 * {@code shinobi/spawn/internal/assign_threat} (vida y titulo) y en los cuatro {@code spawn/undead_*}.
 *
 * <h2>Esto si estaba completo en el datapack</h2>
 * A diferencia de la IA —que es un WIP con {@code say} de depuracion, ver {@link ShinobiCombatPlan}— la tabla de
 * amenaza esta terminada y es exacta: la vida crece de <b>120 en 120</b> del Academico al Jonin y el <b>Kage
 * rompe el patron</b> con 700 en vez de 720. Se guarda como tabla y no como formula precisamente por eso.
 *
 * <h2>El titulo no es decorativo</h2>
 * El datapack lo muestra sobre la cabeza ({@code Threat: Chūnin}) con su color, y esa etiqueta <b>es</b> la
 * interfaz de dificultad: le dice al jugador si puede con el antes de acercarse. Se conserva, pero con el
 * nombre nativo de la entidad en vez del armor stand flotante del original.
 *
 * <p>El <b>roster de jutsus</b> por nivel es <b>diseño nuevo</b> (el datapack no lo tenia cableado): se declara
 * aqui con jutsus que ya existen en el catalogo de F4, en la afinidad que el propio datapack le da al shinobi al
 * aparecer ({@code aff_fire 100}).
 *
 * @param datapackId  valor de {@code d.ai_lvl} (1..6); solo trazabilidad
 * @param maxHealth   vida base ({@code assign_threat})
 * @param titleColor  color del titulo en el datapack, en hex
 * @param jutsus      jutsus que sabe, del mas debil al mas fuerte (<b>diseño nuevo</b>)
 */
public enum ThreatLevel {

    /** Academico: el mas debil; ni siquiera lanza jutsus. */
    ACADEMY(1, 120.0, 0xFFFFFF, List.of()),
    /** Genin: el que sale del huevo de invocacion por defecto ({@code spawn/undead_shinobi}). */
    GENIN(2, 240.0, 0xFFFFFF, List.of("great_fireball")),
    /** Chunin ({@code spawn/undead_chunin}). */
    CHUNIN(3, 360.0, 0xFFFF55, List.of("great_fireball", "fire_bullet")),
    /** Tokubetsu Jonin. */
    TOKUBETSU_JONIN(4, 480.0, 0xFFAA00, List.of("great_fireball", "fire_bullet", "fire_dragon_flame_bullet")),
    /** Jonin ({@code spawn/undead_jonin}). */
    JONIN(5, 600.0, 0xE05D00,
            List.of("great_fireball", "fire_bullet", "fire_dragon_flame_bullet", "exploding_dragon_strike")),
    /** Kage: el techo, y el unico que rompe la progresion de vida ({@code spawn/undead_kage}). */
    KAGE(6, 700.0, 0x9E2235,
            List.of("great_fireball", "fire_bullet", "fire_dragon_flame_bullet", "exploding_dragon_strike",
                    "great_fire_annihilation"));

    /** Alcance con el que ve objetivos ({@code follow_range base set 64}), igual para los seis. */
    public static final double FOLLOW_RANGE = 64.0;

    /** Chakra con el que aparece ({@code assign_threat}: {@code d.chakra 10000}), igual para los seis. */
    public static final int CHAKRA = 10_000;

    /** Afinidad de fuego con la que aparece ({@code assign_threat}: {@code d.aff_fire 100}). */
    public static final int FIRE_AFFINITY = 100;

    private final int datapackId;
    private final double maxHealth;
    private final int titleColor;
    private final List<String> jutsus;

    ThreatLevel(int datapackId, double maxHealth, int titleColor, List<String> jutsus) {
        this.datapackId = datapackId;
        this.maxHealth = maxHealth;
        this.titleColor = titleColor;
        this.jutsus = jutsus;
    }

    /** Valor de {@code d.ai_lvl} (1..6). */
    public int datapackId() {
        return datapackId;
    }

    /** Id data-driven en minusculas ({@code "genin"}, {@code "kage"}). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Vida base de este nivel de amenaza. */
    public double maxHealth() {
        return maxHealth;
    }

    /** Color del titulo (hex RGB). */
    public int titleColor() {
        return titleColor;
    }

    /** Jutsus que sabe, del mas debil al mas fuerte. Vacio = solo pelea de cerca y con kunai. */
    public List<String> jutsus() {
        return jutsus;
    }

    /** El jutsu mas debil que sabe, o vacio si no sabe ninguno. */
    public Optional<String> weakJutsu() {
        return jutsus.isEmpty() ? Optional.empty() : Optional.of(jutsus.get(0));
    }

    /** El jutsu mas fuerte que sabe, o vacio si no sabe ninguno. */
    public Optional<String> strongJutsu() {
        return jutsus.isEmpty() ? Optional.empty() : Optional.of(jutsus.get(jutsus.size() - 1));
    }

    /** Resuelve un nivel por su id; vacio si no existe (estado tolerante). */
    public static Optional<ThreatLevel> byId(String id) {
        for (ThreatLevel level : values()) {
            if (level.id().equals(id)) {
                return Optional.of(level);
            }
        }
        return Optional.empty();
    }

    /** Resuelve un nivel por su valor 1..6 del datapack. */
    public static Optional<ThreatLevel> byDatapackId(int id) {
        for (ThreatLevel level : values()) {
            if (level.datapackId == id) {
                return Optional.of(level);
            }
        }
        return Optional.empty();
    }
}
