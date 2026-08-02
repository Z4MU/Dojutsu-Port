package com.zamu.dojutsu.progression;

import com.zamu.dojutsu.core.DomainEvents;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.state.Skill;
import net.minecraft.server.level.ServerPlayer;

/**
 * <b>Unica puerta de escritura de XP</b> del runtime ({@code [ADD/03 §2.7]}, {@code [ADD/10 progression]}).
 * Todo sistema que otorgue XP (uso de jutsu, gasto de puntos, hitos, ...) pasa por aqui: garantiza la
 * auto-nivelacion y la emision de eventos ({@code SkillLevelChangedEvent}/{@code NinjaXpChangedEvent}) en un
 * unico sitio, evitando escrituras de XP dispersas.
 *
 * <p>Fachada de {@link LevelingService} (el motor puro): resuelve el estado y la identidad del jugador y usa el
 * bus compartido {@link DomainEvents#bus()}.
 *
 * <p><b>Afinidades (F4/DJT-078):</b> la XP de afinidad NO pasa por {@link XpApi} sino por su propia puerta,
 * {@code NatureService.addAffinityXp} — el perfil elemental solo lo escribe {@code NatureService} (regla 10), y
 * meter {@code progression → natures} aqui cerraria un ciclo entre modulos ({@code natures} ya depende de
 * {@code progression} para la ninja_xp del grant). En el datapack la XP de afinidad se gana usando jutsus (cada
 * cast del elemento suma {@code d.<nat>_xp 1}); su emisor es {@code jutsu.JutsuXpHandler}. Otorgar una naturaleza
 * (DJT-051) no da XP de afinidad, solo ninja_xp.
 */
public final class XpApi {

    private XpApi() {
    }

    /** Anade XP a una habilidad del jugador (auto-nivela; emite evento si sube de nivel). */
    public static void addSkillXp(ServerPlayer player, Skill skill, int xp) {
        LevelingService.addSkillXp(NinjaData.get(player), player.getUUID(), skill, xp, DomainEvents.bus());
    }

    /** Anade XP ninja global al jugador (emite {@code NinjaXpChangedEvent} para el rango). */
    public static void addNinjaXp(ServerPlayer player, int xp) {
        LevelingService.addNinjaXp(NinjaData.get(player), player.getUUID(), xp, DomainEvents.bus());
    }

    /**
     * Resta XP ninja global al jugador (suelo en 0; emite {@code NinjaXpChangedEvent}). La usa el revoke de
     * naturalezas ({@code natures/take/<nat>}); tambien pasa por esta puerta unica.
     */
    public static void removeNinjaXp(ServerPlayer player, int amount) {
        LevelingService.removeNinjaXp(NinjaData.get(player), player.getUUID(), amount, DomainEvents.bus());
    }
}
