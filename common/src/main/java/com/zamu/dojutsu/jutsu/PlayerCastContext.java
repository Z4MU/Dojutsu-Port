package com.zamu.dojutsu.jutsu;

import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.domain.jutsu.CastContext;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import java.util.Locale;
import java.util.Objects;

/**
 * {@link CastContext} de un jugador (F4/DJT-074): adapta {@code PlayerNinjaData} al contrato de
 * precondiciones del dominio. Puro y testeable (no toca {@code ServerPlayer}).
 *
 * <p>Un id de naturaleza desconocido resuelve a "no la posee / afinidad 0" (nunca crashea: los
 * ids vienen de JSON de contenido). El cooldown (DJT-077) se consulta por-instancia contra
 * {@code JutsuKnowledge.cooldowns} al {@code nowTick} de mundo pasado al construir el contexto.
 *
 * @param data    estado del jugador
 * @param nowTick tick de mundo actual ({@code Level.getGameTime}) para evaluar cooldowns
 */
public record PlayerCastContext(PlayerNinjaData data, long nowTick) implements CastContext {

    public PlayerCastContext {
        Objects.requireNonNull(data, "data");
    }

    @Override
    public int availableChakra() {
        return data.chakra().chakra();
    }

    @Override
    public boolean isOnCooldown(String jutsuId) {
        return data.jutsu().isOnCooldown(jutsuId, nowTick);
    }

    @Override
    public boolean hasNature(String natureId) {
        NatureType nature = natureOf(natureId);
        return nature != null && data.natures().has(nature);
    }

    @Override
    public int affinity(String natureId) {
        NatureType nature = natureOf(natureId);
        return nature == null ? 0 : data.natures().affinity(nature).level();
    }

    @Override
    public int skillLevel(String skillId) {
        com.zamu.dojutsu.data.state.Skill skill = skillOf(skillId);
        return skill == null ? 0 : data.progression().skill(skill).level();
    }

    @Override
    public boolean hasKekkeiGenkai(String kekkeiGenkaiId) {
        KekkeiGenkai kg = kekkeiGenkaiOf(kekkeiGenkaiId);
        return kg != null && data.natures().has(kg);
    }

    /** Resuelve el id data-driven a la naturaleza, o null si no existe. */
    private static NatureType natureOf(String natureId) {
        for (NatureType nature : NatureType.values()) {
            if (nature.name().toLowerCase(Locale.ROOT).equals(natureId)) {
                return nature;
            }
        }
        return null;
    }

    /** Resuelve el id data-driven a la habilidad, o null si no existe. */
    private static com.zamu.dojutsu.data.state.Skill skillOf(String skillId) {
        for (com.zamu.dojutsu.data.state.Skill skill : com.zamu.dojutsu.data.state.Skill.values()) {
            if (skill.name().toLowerCase(Locale.ROOT).equals(skillId)) {
                return skill;
            }
        }
        return null;
    }

    /** Resuelve el id data-driven al Kekkei Genkai, o null si no existe. */
    private static KekkeiGenkai kekkeiGenkaiOf(String kekkeiGenkaiId) {
        for (KekkeiGenkai kg : KekkeiGenkai.values()) {
            if (kg.name().toLowerCase(Locale.ROOT).equals(kekkeiGenkaiId)) {
                return kg;
            }
        }
        return null;
    }
}
