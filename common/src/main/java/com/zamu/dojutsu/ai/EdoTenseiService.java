package com.zamu.dojutsu.ai;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.domain.ai.ThreatLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * <b>Edo Tensei</b> — Reanimacion del Mundo Impuro {@code [S17]} (F8/DJT-185): traer de vuelta a un shinobi
 * muerto para que pelee por ti.
 *
 * <h2>Lo que hay y lo que no</h2>
 * En el datapack la reanimacion <b>ya existe como resultado</b> —los cuatro {@code spawn/undead_*} invocan
 * "Undead Shinobi" con su nivel de amenaza— pero <b>no hay tecnica que la dispare</b>: se obtienen con un
 * <b>huevo de invocacion</b> ({@code give/undead_shinobi}). O sea que el Edo Tensei del datapack es, literalmente,
 * un item de creativo.
 *
 * <p>Aqui se separa lo que ya estaba de lo que falta: este servicio es la <b>puerta de reanimacion</b>, con los
 * cuatro niveles que el datapack declara, y el <b>disparador</b> queda para quien lo necesite — el huevo de
 * invocacion (items) o el jutsu narrativo (F9). Modelarlo como servicio y no como item evita que el dia que
 * llegue el jutsu haya que reimplementarlo.
 *
 * <h2>Los cuatro que el datapack invoca</h2>
 * {@code undead_shinobi} es <b>Genin</b>, {@code undead_chunin} Chunin, {@code undead_jonin} Jonin y
 * {@code undead_kage} Kage. Los niveles Academico y Tokubetsu Jonin existen en la tabla de amenaza pero
 * <b>ninguna funcion los invoca</b>: son huecos del datapack, no de este port, y estan disponibles igual.
 */
public final class EdoTenseiService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private EdoTenseiService() {
    }

    /**
     * Reanima un shinobi del nivel dado.
     *
     * @return el shinobi reanimado, o {@code null} si el nivel rechazo la entidad
     */
    public static ShinobiEntity reanimate(ServerLevel level, ThreatLevel threat, Vec3 at) {
        ShinobiEntity shinobi = ShinobiEntity.summon(level, threat, at.x, at.y, at.z);
        if (shinobi == null) {
            return null;
        }
        // el ritual: humo blanco y el chasquido de una tumba abriendose
        level.sendParticles(ParticleTypes.ASH, at.x, at.y + 1, at.z, 80, 0.6, 1.2, 0.6, 0.02);
        level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 0.5, at.z, 30, 0.5, 0.5, 0.5, 0.05);
        level.playSound(null, shinobi.blockPosition(), SoundEvents.SOUL_ESCAPE.value(),
                SoundSource.HOSTILE, 1f, 0.5f);
        level.playSound(null, shinobi.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED,
                SoundSource.HOSTILE, 0.7f, 0.6f);
        LOGGER.info("[{}] Edo Tensei: reanimado un shinobi de amenaza {}.", DojutsuMod.MOD_NAME, threat.id());
        return shinobi;
    }
}
