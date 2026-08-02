package com.zamu.dojutsu.natures;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.core.DomainEvents;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.TemporaryGrants;
import com.zamu.dojutsu.domain.event.DomainEventBus;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.network.NinjaSync;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Naturalezas y Kekkei Genkai <b>prestados</b> por una fuente ({@code d.tmp_<nat>} / {@code d.tkg_<kg>} y
 * {@code natures/remove_tmp} del datapack, {@code [S03]}).
 *
 * <p><b>"Temporal" = PRESTADO, no "con caducidad".</b> En el datapack, quien marca {@code d.tmp_*} es
 * <b>solo</b> {@code jinchuriki/sealing/*} (la bestia con cola presta sus naturalezas) y quien lo limpia es
 * <b>solo</b> {@code jinchuriki/removal/*} via {@code remove_tmp}. <b>No existe ningun temporizador</b>: por eso
 * aqui no hay timestamps ni ningun {@code PlayerTickSystem} que sondee vencimientos (seria polling, prohibido).
 * Ver {@link TemporaryGrants} y la entrada de DJT-053 en {@code docs/development_log.md}.
 *
 * <p>Vive en una clase aparte (y no en {@link NatureService}/{@link KekkeiGenkaiService}) porque la semantica de
 * prestamo abarca <b>ambos</b>: ponerla en cualquiera de los dos crearia un ciclo entre ellos.
 *
 * <p><b>Consumidor real: F7 (bijuu).</b> El sellado llamara a {@link #grantNature}/{@link #grantKekkeiGenkai}
 * por cada elemento que preste la bestia (en el mismo orden que {@code sealing/*}: primero las naturalezas
 * componente, luego el KG) y la extraccion a {@link #revokeAllFrom} con el id de la bestia. Hoy solo lo ejercita
 * el comando de depuracion.
 */
public final class TemporaryGrantService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private TemporaryGrantService() {
    }

    // --- Servicio puro (sobre el estado) ---

    /**
     * Presta una naturaleza al jugador en nombre de {@code source} (p. ej. {@code "one_tail"}): la otorga y la
     * marca como prestada, para poder retirarla cuando esa fuente desaparezca.
     *
     * <p><b>Si ya la tenia, no hace nada</b> ({@code unless has_<nat>} del datapack): ni la marca como prestada
     * ni repite XP. Es lo que garantiza que <b>una fuente nunca "pisa" lo que es tuyo</b> y que al retirarse no
     * se lleve por delante una naturaleza permanente — el riesgo principal del ticket.
     *
     * @return {@code true} si se presto; {@code false} si el jugador ya la tenia (propia o prestada)
     */
    public static boolean grantNature(PlayerNinjaData data, UUID player, NatureType nature, String source,
            DomainEventBus bus) {
        if (!NatureService.grant(data, player, nature, bus)) {
            return false; // ya la tenia: se queda como estaba (propia o prestada por otra fuente)
        }
        data.setNatures(data.natures().withTemporary(data.natures().temporary().withNature(nature, source)));
        return true;
    }

    /**
     * Presta un Kekkei Genkai al jugador en nombre de {@code source}. Como {@code give/kg/<kg>}, implica sus
     * naturalezas-componente; las que el jugador no tuviera se otorgan como <b>permanentes</b>, igual que en el
     * datapack: es el sellado quien presta antes cada componente (ver {@code sealing/ten_tails}, que marca
     * {@code tmp_earth}/{@code tmp_water} <i>antes</i> de {@code give/kg/wood}). F7 debe replicar ese orden.
     *
     * @return {@code true} si se presto; {@code false} si ya lo tenia o no hay definicion cargada
     */
    public static boolean grantKekkeiGenkai(PlayerNinjaData data, UUID player, KekkeiGenkai kg, String source,
            DomainEventBus bus) {
        if (!KekkeiGenkaiService.grant(data, player, kg, bus)) {
            return false;
        }
        data.setNatures(data.natures().withTemporary(data.natures().temporary().withKekkeiGenkai(kg, source)));
        return true;
    }

    /**
     * Retira <b>todo</b> lo prestado por {@code source} y solo eso ({@code natures/remove_tmp}, llamado desde
     * {@code jinchuriki/removal/*}): revoca cada naturaleza/KG prestado por esa fuente (restando su ninja_xp) y
     * borra sus marcas. Lo permanente y lo prestado por <b>otras</b> fuentes queda intacto.
     *
     * <p>Sigue el orden del datapack: primero las naturalezas, luego los KG.
     *
     * @return numero de elementos retirados
     */
    public static int revokeAllFrom(PlayerNinjaData data, UUID player, String source, DomainEventBus bus) {
        TemporaryGrants grants = data.natures().temporary();
        Set<NatureType> natures = grants.naturesFrom(source);
        Set<KekkeiGenkai> kekkeiGenkai = grants.kekkeiGenkaiFrom(source);
        int removed = 0;
        for (NatureType nature : natures) {
            NatureService.revoke(data, player, nature, bus);
            removed++;
        }
        for (KekkeiGenkai kg : kekkeiGenkai) {
            KekkeiGenkaiService.revoke(data, player, kg, bus);
            removed++;
        }
        TemporaryGrants next = data.natures().temporary();
        for (NatureType nature : natures) {
            next = next.withoutNature(nature);
        }
        for (KekkeiGenkai kg : kekkeiGenkai) {
            next = next.withoutKekkeiGenkai(kg);
        }
        data.setNatures(data.natures().withTemporary(next));
        return removed;
    }

    // --- Glue con Minecraft ---

    /** Presta una naturaleza al jugador en nombre de {@code source} y sincroniza. */
    public static boolean grantNature(ServerPlayer player, NatureType nature, String source) {
        if (!grantNature(NinjaData.get(player), player.getUUID(), nature, source, DomainEvents.bus())) {
            return false;
        }
        NinjaSync.sendDelta(player);
        LOGGER.info("[{}] {} recibio prestada la naturaleza {} de {}.",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName(), NatureService.id(nature), source);
        return true;
    }

    /** Presta un Kekkei Genkai al jugador en nombre de {@code source} y sincroniza. */
    public static boolean grantKekkeiGenkai(ServerPlayer player, KekkeiGenkai kg, String source) {
        if (!grantKekkeiGenkai(NinjaData.get(player), player.getUUID(), kg, source, DomainEvents.bus())) {
            return false;
        }
        NinjaSync.sendDelta(player);
        LOGGER.info("[{}] {} recibio prestado el Kekkei Genkai {} de {}.",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName(), KekkeiGenkaiService.id(kg), source);
        return true;
    }

    /**
     * Retira todo lo prestado por {@code source} al jugador, retira los learners de los elementos perdidos
     * ({@code [S03-R2]}) y sincroniza. Devuelve cuantos elementos retiro.
     */
    public static int revokeAllFrom(ServerPlayer player, String source) {
        int removed = revokeAllFrom(NinjaData.get(player), player.getUUID(), source, DomainEvents.bus());
        if (removed > 0) {
            LearnerService.removeStaleLearners(player); // politica del mod: sin elemento, sin learner
            NinjaSync.sendDelta(player);
            LOGGER.info("[{}] {} perdio {} elemento(s) prestado(s) por {}.",
                    DojutsuMod.MOD_NAME, player.getGameProfile().getName(), removed, source);
        }
        return removed;
    }
}
