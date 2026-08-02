package com.zamu.dojutsu.natures;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.core.DomainEvents;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.domain.event.DomainEventBus;
import com.zamu.dojutsu.domain.event.KekkeiGenkaiGrantedEvent;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.progression.LevelingService;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Kekkei Genkai del ninja ({@code [S03]}): otorgarlos <b>implicando sus naturalezas-componente</b>. Reemplaza
 * las 18 funciones {@code natures/give/kg/<kg>} (copy-paste) por una tabla data-driven
 * ({@link KekkeiGenkaiRegistry}) + este servicio.
 *
 * <p>Orden de {@code give/kg/<kg>}, replicado aqui: (1) si ya lo tenia, no hacer nada; (2) otorgar cada
 * naturaleza-componente que falte (por {@link NatureService}, con su propia ninja_xp y su evento); (3) marcar
 * el KG; (4) aplicar el bonus de afinidad si lo hay; (5) sumar la ninja_xp del KG.
 *
 * <p>SRP y patron de F2: {@link #grant(PlayerNinjaData, UUID, KekkeiGenkai, DomainEventBus)} es <b>puro</b>
 * (testeable sin Minecraft); la sobrecarga con {@link ServerPlayer} solo delega y anade sync + log.
 *
 * <p>El learner del KG (DJT-055) llega por evento: {@code grant} publica {@link KekkeiGenkaiGrantedEvent} y
 * {@code LearnerService} entrega el item ({@code give/kg/<kg>} -> {@code give/learner/<kg>} del datapack).
 *
 * <p><b>Hooks pendientes</b> (partes de {@code give/kg/*} que no son de F3):
 * <ul>
 *   <li><b>DJT-061/F4:</b> {@code give/kg/shikotsumyaku} entrega ademas los 3 jutsus del clan Kaguya
 *       ({@code dance_of_the_camellia/larch/seedling_fern}) si el jugador tiene clan; los jutsus no existen aun.</li>
 *   <li><b>Deuda:</b> {@code natures/give/kg/hashirama_cells} <b>no es un KG</b> (escribe {@code d.has_hsdna},
 *       +10 medical, +30 medical_xp, +30 ninja_xp) -> no se carga aqui. El flag existe
 *       ({@code NatureProfile.hashiramaDna}) pero nadie lo escribe: lo otorgan items/ADN (S14, F8).</li>
 * </ul>
 */
public final class KekkeiGenkaiService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private KekkeiGenkaiService() {
    }

    // --- Servicio puro (sobre el estado) ---

    /** Indica si el jugador posee el Kekkei Genkai. */
    public static boolean has(PlayerNinjaData data, KekkeiGenkai kg) {
        return data.natures().has(kg);
    }

    /**
     * Otorga un Kekkei Genkai: implica sus naturalezas-componente, lo marca, aplica su bonus de afinidad y suma
     * su ninja_xp. Los componentes se otorgan por {@link NatureService#grant} (idempotente), asi que cada uno
     * aporta <b>su propia</b> ninja_xp y emite su evento — igual que en el datapack, donde {@code give/kg/wood}
     * llama a {@code give/earth} y {@code give/water}.
     *
     * <p><b>Idempotente</b> ({@code unless has_<kg>}): si ya lo tenia, no repite nada (ni componentes, ni XP,
     * ni bonus de afinidad).
     *
     * @return {@code true} si se otorgo; {@code false} si ya lo tenia o si <b>no hay definicion</b> cargada
     */
    public static boolean grant(PlayerNinjaData data, UUID player, KekkeiGenkai kg, DomainEventBus bus) {
        if (has(data, kg)) {
            return false;
        }
        Optional<KekkeiGenkaiDefinition> definition = KekkeiGenkaiRegistry.get(kg);
        if (definition.isEmpty()) {
            LOGGER.error("[{}] Kekkei Genkai sin definicion cargada: {} (no se otorga).", DojutsuMod.MOD_NAME, kg);
            return false; // sin datos no se puede otorgar: mejor no hacer nada que otorgarlo a medias
        }
        KekkeiGenkaiDefinition def = definition.get();
        for (NatureType component : def.components()) {
            NatureService.grant(data, player, component, bus); // idempotente: no repite si ya la tenia
        }
        data.setNatures(data.natures().withKekkeiGenkai(kg));
        for (Map.Entry<NatureType, Integer> bonus : def.affinityBonus().entrySet()) {
            NatureService.addAffinityLevels(data, bonus.getKey(), bonus.getValue());
        }
        LevelingService.addNinjaXp(data, player, def.ninjaXp(), bus);
        bus.publish(new KekkeiGenkaiGrantedEvent(player, id(kg))); // lo consume LearnerService (DJT-055)
        return true;
    }

    /**
     * Revoca un Kekkei Genkai: lo desmarca y resta su ninja_xp.
     *
     * <p>Fiel a {@code natures/take/kg/<kg>}: <b>no</b> revoca las naturalezas-componente (quien obtuvo Wood
     * conserva Earth y Water) ni retira el bonus de afinidad.
     *
     * @return {@code true} si se revoco; {@code false} si no lo tenia o si no hay definicion cargada
     */
    public static boolean revoke(PlayerNinjaData data, UUID player, KekkeiGenkai kg, DomainEventBus bus) {
        if (!has(data, kg)) {
            return false;
        }
        Optional<KekkeiGenkaiDefinition> definition = KekkeiGenkaiRegistry.get(kg);
        if (definition.isEmpty()) {
            LOGGER.error("[{}] Kekkei Genkai sin definicion cargada: {} (no se revoca).", DojutsuMod.MOD_NAME, kg);
            return false;
        }
        data.setNatures(data.natures().withoutKekkeiGenkai(kg));
        LevelingService.removeNinjaXp(data, player, definition.get().ninjaXp(), bus);
        return true;
    }

    // --- Glue con Minecraft ---

    /** Otorga un Kekkei Genkai al jugador (con sus componentes) y sincroniza. */
    public static boolean grant(ServerPlayer player, KekkeiGenkai kg) {
        if (!grant(NinjaData.get(player), player.getUUID(), kg, DomainEvents.bus())) {
            return false;
        }
        NinjaSync.sendDelta(player); // la ninja_xp es observable (HUD de rango)
        LOGGER.info("[{}] {} obtuvo el Kekkei Genkai {}.",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName(), id(kg));
        return true;
    }

    /** Revoca un Kekkei Genkai al jugador, retira su learner ({@code [S03-R2]}) y sincroniza. */
    public static boolean revoke(ServerPlayer player, KekkeiGenkai kg) {
        if (!revoke(NinjaData.get(player), player.getUUID(), kg, DomainEvents.bus())) {
            return false;
        }
        LearnerService.removeStaleLearners(player); // politica del mod: sin elemento, sin learner
        NinjaSync.sendDelta(player);
        LOGGER.info("[{}] {} perdio el Kekkei Genkai {}.",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName(), id(kg));
        return true;
    }

    /** Id data-driven del KG (minusculas): el que usan los JSON de {@code data/dojutsu/kekkei_genkai/}. */
    public static String id(KekkeiGenkai kg) {
        return kg.name().toLowerCase(Locale.ROOT);
    }
}
