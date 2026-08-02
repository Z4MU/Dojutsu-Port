package com.zamu.dojutsu.jutsu;

import com.zamu.dojutsu.core.DomainEvents;
import com.zamu.dojutsu.domain.event.JutsuCastEvent;
import com.zamu.dojutsu.domain.jutsu.AffinityRequirement;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.natures.NatureService;
import dev.architectury.utils.GameInstance;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Emisor de la <b>XP de afinidad</b> (F4/DJT-078): suscriptor de {@link JutsuCastEvent} que suma
 * XP a la afinidad del jutsu lanzado. Cierra el bucle de progresion maestro — usar un jutsu →
 * sube la afinidad → sube de nivel → habilita jutsus mas fuertes (cast: {@code aff >= N}).
 *
 * <p><b>Fidelidad al datapack ({@code [S06]}, regla 16):</b> cada cast del elemento hace
 * {@code scoreboard players add @s d.<nat>_xp 1} — cantidad <b>uniforme +1</b>, verificada en los
 * {@code cast.mcfunction} (great_fireball/fire_bullet/fireproof → fire_xp; vacuum_wave → wind_xp;
 * chidori → ligh_xp...). Resuelve el {@code TBD_datapack_per_jutsu} de {@code xp_sources.json}.
 *
 * <p><b>Solo en cast exitoso</b> (el pipeline emite el evento solo tras consumir) y <b>solo para
 * jutsus con afinidad</b>: los universales (substitution, healing) no dan XP de afinidad — el
 * datapack no hace {@code add d.<nat>_xp} en ellos. El caster no-jugador (IA, F8) se ignora: la
 * XP de afinidad es progresion de jugador.
 */
public final class JutsuXpHandler {

    /** XP de afinidad por cast del elemento, uniforme en el datapack ({@code add d.<nat>_xp 1}). */
    public static final int AFFINITY_XP_PER_CAST = 1;

    private JutsuXpHandler() {
    }

    /** Suscribe el handler al bus de dominio. Llamar una vez en el arranque. */
    public static void register() {
        DomainEvents.bus().subscribe(JutsuCastEvent.class, JutsuXpHandler::onCast);
    }

    /**
     * Decision pura (testeable): <b>todas</b> las naturalezas cuya afinidad sube este jutsu (vacia si es
     * universal). Los jutsus KG dan XP a sus <b>varias</b> naturalezas componentes — verificado en el
     * datapack: castear Madera hace {@code add d.earth_xp 1} <b>y</b> {@code add d.water_xp 1} (F4/DJT-092).
     */
    public static List<NatureType> affinityNaturesOf(String jutsuId) {
        return JutsuRegistry.get(jutsuId)
                .map(entry -> entry.definition().affinityRequirements().stream()
                        .map(AffinityRequirement::natureId)
                        .map(JutsuXpHandler::natureOf)
                        .flatMap(Optional::stream)
                        .toList())
                .orElseGet(List::of);
    }

    /** La primera naturaleza de afinidad (o vacio). Atajo para consumidores de una sola naturaleza. */
    public static Optional<NatureType> affinityNatureOf(String jutsuId) {
        return affinityNaturesOf(jutsuId).stream().findFirst();
    }

    private static void onCast(JutsuCastEvent event) {
        List<NatureType> natures = affinityNaturesOf(event.jutsuId());
        if (natures.isEmpty()) {
            return;
        }
        ServerPlayer player = resolvePlayer(event.caster());
        if (player == null) {
            return;
        }
        for (NatureType nature : natures) {
            NatureService.addAffinityXp(player, nature, AFFINITY_XP_PER_CAST);
        }
    }

    private static Optional<NatureType> natureOf(String natureId) {
        for (NatureType nature : NatureType.values()) {
            if (nature.name().toLowerCase(Locale.ROOT).equals(natureId)) {
                return Optional.of(nature);
            }
        }
        return Optional.empty();
    }

    private static ServerPlayer resolvePlayer(UUID casterId) {
        MinecraftServer server = GameInstance.getServer();
        return server == null ? null : server.getPlayerList().getPlayer(casterId);
    }
}
