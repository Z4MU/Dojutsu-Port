package com.zamu.dojutsu.natures;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.core.DomainEvents;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.domain.event.DomainEventBus;
import com.zamu.dojutsu.domain.event.NatureGrantedEvent;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.domain.progression.LeveledStat;
import com.zamu.dojutsu.domain.progression.XpCurve;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.progression.LevelingService;
import com.zamu.dojutsu.progression.XpApi;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Naturalezas de chakra del ninja ({@code [S03]}): otorgar, revocar y consultar. Reemplaza las 8 funciones
 * {@code natures/give/<nat>} y sus 8 {@code natures/take/<nat>} (copy-paste) por una tabla + un servicio.
 *
 * <p>SRP y patron de F2: {@link #grant(PlayerNinjaData, UUID, NatureType, DomainEventBus)} y
 * {@link #revoke(PlayerNinjaData, UUID, NatureType, DomainEventBus)} son <b>puros</b> (mutan
 * {@link PlayerNinjaData}, testeables sin Minecraft); las sobrecargas con {@link ServerPlayer} son el glue
 * (resuelven estado/identidad, sincronizan y loguean).
 *
 * <p><b>Fidelidad al datapack — lo que grant SI hace y lo que NO:</b>
 * <ul>
 *   <li><b>SI:</b> marca la naturaleza, suma {@link NatureType#ninjaXp()} (25/20/30/100 segun la naturaleza) y
 *       emite {@link NatureGrantedEvent}.</li>
 *   <li><b>NO fija la afinidad a 50.</b> La ficha del ticket lo pedia, pero es una mala lectura de
 *       {@code [S03]}: {@code natures/give/fire} <b>no</b> toca {@code d.aff_fire} (solo lo sanea si fuese
 *       negativo). El <b>50 es del onboarding</b> ({@code main/internal/nature_select_main} y
 *       {@code random_aff} lo fijan <i>antes</i> de llamar al give) -> lo aplicara DJT-059. Prueba
 *       definitiva: {@code clans/join_uchiha} hace {@code give/fire} y luego {@code aff_fire += 10}; si el
 *       grant fijase 50, un Uchiha acabaria con 60 en vez de 10 y Wood pondria earth/water a 50.</li>
 *   <li>El saneo {@code unless aff_<nat> matches 0.. -> set 0} del datapack <b>no se porta</b>: aqui la
 *       afinidad es un {@code LeveledStat}, que no admite valores negativos por construccion. La rama era un
 *       parche contra scoreboards corruptos.</li>
 * </ul>
 *
 * <p><b>Bug {@code [S03-R1]} (Yang) corregido por construccion:</b> {@code get_random_kg} llama a
 * {@code natures/give/kg/yang} y consulta el predicate {@code natures/kekkei_genkai/has_yang}, y <b>ninguno de
 * los dos existe</b> -> la tirada {@code kg_rng 1..2} no hacia nada. Aqui Yang no es un KG: es un
 * {@link NatureType} mas, y {@code grant(YANG)} es una llamada tipada, asi que la referencia rota es
 * <b>imposible de escribir</b>. Mapear la tirada 1..2 -> Yang es cosa de DJT-054 (la tabla de RNG).
 */
public final class NatureService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private NatureService() {
    }

    // --- Servicio puro (sobre el estado) ---

    /** Indica si el jugador posee la naturaleza. */
    public static boolean has(PlayerNinjaData data, NatureType nature) {
        return data.natures().has(nature);
    }

    /**
     * Otorga una naturaleza: la marca, suma su {@link NatureType#ninjaXp()} y emite {@link NatureGrantedEvent}.
     *
     * <p><b>Idempotente</b> ({@code unless has_<nat>} del datapack): si ya la tenia no repite XP, ni evento, ni
     * toca la afinidad (re-otorgar no debe resetear el progreso).
     *
     * <p>La XP pasa por {@link LevelingService} — el motor detras de la unica puerta {@link XpApi} — para que
     * la emision de {@code NinjaXpChangedEvent} (y por tanto el rango) ocurra en un solo sitio.
     *
     * @return {@code true} si se otorgo; {@code false} si ya la tenia (sin efecto)
     */
    public static boolean grant(PlayerNinjaData data, UUID player, NatureType nature, DomainEventBus bus) {
        if (has(data, nature)) {
            return false;
        }
        data.setNatures(data.natures().withNature(nature));
        LevelingService.addNinjaXp(data, player, nature.ninjaXp(), bus);
        bus.publish(new NatureGrantedEvent(player, id(nature)));
        return true;
    }

    /**
     * Revoca una naturaleza: la desmarca y resta la misma {@link NatureType#ninjaXp()} que dio el grant (los
     * {@code natures/take/<nat>} son exactamente simetricos; verificado uno a uno).
     *
     * <p><b>No borra la afinidad</b>: el {@code take/*} del datapack tampoco resetea {@code d.aff_<nat>}, y
     * conservarla evita perder el progreso al revocar y re-otorgar.
     *
     * <p><b>Learner ({@code [S03-R2]}):</b> el datapack no revoca el "Skill Learner" al hacer take (residuo);
     * la politica del mod es retirarlo. Lo hace la sobrecarga de runtime via
     * {@code LearnerService.removeStaleLearners} (aqui no: el inventario es de {@code ServerPlayer}).
     *
     * @return {@code true} si se revoco; {@code false} si no la tenia (sin efecto)
     */
    public static boolean revoke(PlayerNinjaData data, UUID player, NatureType nature, DomainEventBus bus) {
        if (!has(data, nature)) {
            return false;
        }
        data.setNatures(data.natures().withoutNature(nature));
        LevelingService.removeNinjaXp(data, player, nature.ninjaXp(), bus);
        return true;
    }

    /**
     * Sube {@code levels} el <b>nivel</b> de una afinidad, conservando su XP ({@code scoreboard players add @s
     * d.aff_<nat> N} del datapack: bonus de KG e {@code ice}/{@code blaze}, y de clan en DJT-057).
     *
     * <p><b>Clampa al cap</b> ({@link XpCurve#MAX_LEVEL}) en vez de rechazar: un bonus no debe fallar porque la
     * afinidad ya este al maximo (a diferencia de {@code SkillPointService.spend}, que si rechaza — alli el
     * jugador <b>paga</b> puntos y perderlos seria injusto; aqui el bonus es un regalo).
     *
     * @param levels niveles a sumar, positivos (se ignoran los no positivos)
     * @throws IllegalArgumentException si la naturaleza no almacena afinidad ({@code YIN_YANG}: es derivada)
     */
    public static void addAffinityLevels(PlayerNinjaData data, NatureType nature, int levels) {
        if (levels <= 0) {
            return;
        }
        LeveledStat current = data.natures().affinity(nature);
        int raised = Math.min(XpCurve.MAX_LEVEL, current.level() + levels);
        if (raised == current.level()) {
            return; // ya estaba al cap
        }
        data.setNatures(data.natures().withAffinity(nature, new LeveledStat(raised, current.xp())));
    }

    /**
     * Fija el <b>nivel</b> de una afinidad a un valor absoluto, conservando su XP ({@code scoreboard players
     * set @s d.aff_<nat> 50} del onboarding). A diferencia de {@link #addAffinityLevels}, esto <b>sobrescribe</b>:
     * en el datapack, un Uchiha (fire +10 del clan) que elige Fuego acaba con 50, no con 60.
     *
     * @throws IllegalArgumentException si la naturaleza no almacena afinidad ({@code YIN_YANG}: es derivada)
     */
    public static void setAffinityLevel(PlayerNinjaData data, NatureType nature, int level) {
        int clamped = Math.max(0, Math.min(XpCurve.MAX_LEVEL, level));
        LeveledStat current = data.natures().affinity(nature);
        data.setNatures(data.natures().withAffinity(nature, new LeveledStat(clamped, current.xp())));
    }

    /**
     * Suma XP a una afinidad y <b>auto-nivela</b> ({@code scoreboard players add @s d.<nat>_xp 1} que hace cada
     * jutsu del elemento al lanzarse, {@code [S06]}). Es la puerta de la XP de afinidad (F4/DJT-078); la fachada
     * de runtime {@link XpApi#addAffinityXp} delega aqui, manteniendo el perfil elemental bajo una sola puerta.
     *
     * <p>Comparte curva y semantica con las habilidades ({@link LeveledStat#withXpAdded}: multi-nivel con
     * arrastre, cap 500). Ignora XP no positiva y {@code YIN_YANG} (afinidad derivada, no almacena XP).
     *
     * @param xp XP a sumar, positiva (se ignoran las no positivas)
     */
    public static void addAffinityXp(PlayerNinjaData data, NatureType nature, int xp) {
        if (xp <= 0 || !nature.storesAffinity()) {
            return;
        }
        LeveledStat current = data.natures().affinity(nature);
        LeveledStat updated = current.withXpAdded(xp); // copia nueva: multi-nivel con arrastre, cap 500
        if (updated.equals(current)) {
            return; // en el cap: sin cambio
        }
        data.setNatures(data.natures().withAffinity(nature, updated));
    }

    /**
     * Glue de runtime de {@link #addAffinityXp(PlayerNinjaData, NatureType, int)}: resuelve el estado del jugador
     * y sincroniza (la afinidad no es observable hoy, pero el sync es barato e inofensivo). Lo llama el emisor de
     * la XP de afinidad ({@code jutsu.JutsuXpHandler}) al lanzar un jutsu del elemento.
     */
    public static void addAffinityXp(ServerPlayer player, NatureType nature, int xp) {
        addAffinityXp(NinjaData.get(player), nature, xp);
        NinjaSync.sendDelta(player);
    }

    // --- Glue con Minecraft ---

    /**
     * Otorga una naturaleza al jugador y sincroniza. Devuelve {@code false} si ya la tenia.
     *
     * <p>Solo <b>delega</b> en el servicio puro y anade lo que necesita Minecraft (sync + log): la regla de
     * negocio vive en un unico sitio. Resolver el estado y el bus aqui es exactamente lo que hace
     * {@link XpApi} por dentro, asi que la XP sigue pasando por la misma puerta.
     */
    public static boolean grant(ServerPlayer player, NatureType nature) {
        if (!grant(NinjaData.get(player), player.getUUID(), nature, DomainEvents.bus())) {
            return false;
        }
        NinjaSync.sendDelta(player); // la ninja_xp es observable (HUD de rango): empuja el cambio
        LOGGER.info("[{}] {} obtuvo la naturaleza {} (+{} ninja_xp).",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName(), id(nature), nature.ninjaXp());
        return true;
    }

    /** Revoca una naturaleza al jugador, retira su learner ({@code [S03-R2]}) y sincroniza. */
    public static boolean revoke(ServerPlayer player, NatureType nature) {
        if (!revoke(NinjaData.get(player), player.getUUID(), nature, DomainEvents.bus())) {
            return false;
        }
        LearnerService.removeStaleLearners(player); // politica del mod: sin elemento, sin learner
        NinjaSync.sendDelta(player); // la ninja_xp es observable (HUD de rango): empuja el cambio
        LOGGER.info("[{}] {} perdio la naturaleza {} (-{} ninja_xp).",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName(), id(nature), nature.ninjaXp());
        return true;
    }

    /** Id data-driven de la naturaleza (minusculas, estable): el que viaja en {@link NatureGrantedEvent}. */
    public static String id(NatureType nature) {
        return nature.name().toLowerCase(Locale.ROOT);
    }
}
