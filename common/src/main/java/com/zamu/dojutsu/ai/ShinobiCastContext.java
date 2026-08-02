package com.zamu.dojutsu.ai;

import com.zamu.dojutsu.domain.ai.ThreatLevel;
import com.zamu.dojutsu.domain.jutsu.CastContext;

/**
 * {@link CastContext} de un <b>shinobi NPC</b> (F8/DJT-185): <b>S17-R3 realizado</b>.
 *
 * <h2>La costura ya existia desde F4</h2>
 * El javadoc de {@link CastContext} decia, cuatro fases antes de que existiera ningun NPC:
 * <i>"No asume jugador (S17-R3): un NPC con IA implementa esta interfaz igual que un Player"</i>. Esta clase es
 * esa frase hecha codigo — <b>no hubo que tocar el pipeline de casteo</b>, ni el dominio, ni una sola precondicion.
 *
 * <h2>El shinobi es deliberadamente simple</h2>
 * El datapack le da al aparecer {@value ThreatLevel#CHAKRA} de chakra y {@value ThreatLevel#FIRE_AFFINITY} de
 * afinidad de fuego, y nada mas: sin habilidades, sin Kekkei Genkai y sin cooldowns propios. Aqui se refleja tal
 * cual, asi que un shinobi <b>no puede</b> lanzar un jutsu que exija un KG o una habilidad — que es exactamente
 * lo que queremos: el catalogo entero sigue gateado por las mismas puertas para todos.
 *
 * <p>El cooldown lo lleva el goal de la entidad ({@link ShinobiCombatGoal}), no el estado: un NPC no persiste.
 *
 * @param level nivel de amenaza del shinobi; fija que jutsus tiene declarados
 */
public record ShinobiCastContext(ThreatLevel level) implements CastContext {

    @Override
    public int availableChakra() {
        return ThreatLevel.CHAKRA;
    }

    @Override
    public boolean isOnCooldown(String jutsuId) {
        return false; // el ritmo lo marca el temporizador de la IA, no un cooldown por jutsu
    }

    @Override
    public boolean hasNature(String natureId) {
        return "fire".equals(natureId); // assign_threat solo le da fuego
    }

    @Override
    public int affinity(String natureId) {
        return hasNature(natureId) ? ThreatLevel.FIRE_AFFINITY : 0;
    }
}
