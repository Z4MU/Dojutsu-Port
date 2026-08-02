package com.zamu.dojutsu.network;

import com.zamu.dojutsu.data.state.ProgressionState;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.progression.NinjaRank;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Proyeccion <b>observable</b> de la progresion que viaja al cliente (HUD/menu): skill points, XP ninja y el
 * nivel de cada habilidad (en el orden de {@link Skill#values()}).
 *
 * <p>Solo lo que el cliente necesita mostrar: los desbloqueos y la XP por habilidad no cruzan la red. El
 * <b>rango</b> no se transmite: se deriva en cliente con {@link #rank()} ({@code NinjaRank} es dominio comun).
 *
 * @param skillPoints puntos disponibles
 * @param ninjaXp     XP ninja global (deriva el rango)
 * @param skillLevels nivel por habilidad, indexado por {@link Skill#ordinal()}
 */
public record ProgressionSnapshot(int skillPoints, int ninjaXp, List<Integer> skillLevels) {

    /** Snapshot vacio (antes del primer sync). */
    public static final ProgressionSnapshot EMPTY = new ProgressionSnapshot(0, 0, List.of());

    public static final StreamCodec<ByteBuf, ProgressionSnapshot> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ProgressionSnapshot::skillPoints,
            ByteBufCodecs.VAR_INT, ProgressionSnapshot::ninjaXp,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), ProgressionSnapshot::skillLevels,
            ProgressionSnapshot::new);

    public ProgressionSnapshot {
        skillLevels = List.copyOf(skillLevels);
    }

    /** Construye la proyeccion observable desde el estado del servidor. */
    public static ProgressionSnapshot of(ProgressionState state) {
        List<Integer> levels = new ArrayList<>(Skill.values().length);
        for (Skill skill : Skill.values()) {
            levels.add(state.skill(skill).level());
        }
        return new ProgressionSnapshot(state.skillPoints(), state.ninjaXp(), levels);
    }

    /** Nivel observado de una habilidad (0 si aun no hay datos). */
    public int levelOf(Skill skill) {
        return skill.ordinal() < skillLevels.size() ? skillLevels.get(skill.ordinal()) : 0;
    }

    /** Rango derivado de la XP ninja (mismo calculo que en servidor; no se transmite). */
    public NinjaRank rank() {
        return NinjaRank.fromXp(ninjaXp);
    }
}
