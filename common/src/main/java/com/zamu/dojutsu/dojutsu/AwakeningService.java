package com.zamu.dojutsu.dojutsu;

import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.clans.ClanDefinition;
import com.zamu.dojutsu.clans.ClanRegistry;
import com.zamu.dojutsu.core.DomainEvents;
import com.zamu.dojutsu.core.PlayerTickSystem;
import com.zamu.dojutsu.core.TickPhase;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.EyeState;
import com.zamu.dojutsu.domain.event.EyeAwakenedEvent;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.progression.XpApi;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import net.minecraft.server.level.ServerPlayer;

/**
 * Despierta el dojutsu de un jugador segun su clan {@code [S05]}: el bloque de despertares de
 * {@code dojutsu/main} (DJT-114). Sistema por tick en la fase {@link TickPhase#DOJUTSU}; solo hace trabajo
 * mientras el jugador pertenece a un clan con dojutsu potencial y aun no lo ha despertado — una vez despierto,
 * el guard lo deja inactivo (bounded, como el temporizador {@code d.<eye>_waketime} del datapack).
 *
 * <p><b>Lee {@code potentialDojutsu} del clan (DJT-113/checklist), no predicates {@code is_hyuga}:</b> el clan
 * declara en datos que ojos puede despertar (Hyuga: byakugan; Uchiha: sharingan; Chinoike: ketsuryugan;
 * Otsutsuki: byakugan <b>o</b> sharingan al 50/50 — la loot table {@code 50_50} del datapack). El umbral de
 * despertar es el {@code awakenTicks} del ojo ({@code <eye>_waketime}: 78000 / Ketsuryugan 24000). El despertar
 * ocurre <b>una vez por clan</b>: si ya despierto cualquiera de sus ojos potenciales, no vuelve a disparar.
 *
 * <p><b>Servicio puro + glue</b> (patron del proyecto): {@link #evaluate} decide sin Minecraft (testeable con
 * azar sembrado); {@link #tick} y {@link #applyAwakening} conocen al {@link ServerPlayer} y aplican el efecto
 * (chakra maximo via {@link ChakraService}, XP via {@link XpApi}, evento y sync). La XP/chakra los <b>lee</b> de
 * la {@link EyeDefinition} (DJT-113), no los hardcodea.
 */
public final class AwakeningService implements PlayerTickSystem {

    /**
     * Azar <b>compartido</b> del glue (F10/DJT-222). El 50/50 de Otsutsuki se tira una vez en la vida de un
     * ninja; construir un {@code new Random()} por jugador y tick pagaba un {@code System.nanoTime()} y un CAS
     * sobre el uniquificador estatico de la JVM para no usarlo casi nunca. {@link ThreadLocalRandom} no asigna
     * nada y es un {@link RandomGenerator} — el parametro de {@link #evaluate} sigue inyectado para que los
     * tests le pasen una semilla fija.
     */
    private static final RandomGenerator RANDOM = ThreadLocalRandom.current();

    /** Resultado puro de un tick de despertar: el nuevo estado y, si desperto, el id del ojo (para el glue). */
    record AwakenResult(EyeState state, Optional<String> awakenedEyeId) {
    }

    @Override
    public TickPhase phase() {
        return TickPhase.DOJUTSU;
    }

    @Override
    public void tick(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        Optional<String> clanId = data.clan().clan();
        if (clanId.isEmpty()) {
            return; // sin clan (onboarding no completado) no hay despertar por clan
        }
        Optional<ClanDefinition> clan = ClanRegistry.get(clanId.get());
        if (clan.isEmpty() || clan.get().potentialDojutsu().isEmpty()) {
            return; // clan sin dojutsu potencial (Senju, etc.)
        }

        AwakenResult result = evaluate(data.eye(), clanId.get(), clan.get(), EyeRegistry::get, RANDOM);
        if (result.state() == data.eye()) {
            return; // sin cambios: ya despierto o nada que acumular (evaluate devuelve la MISMA instancia)
        }
        data.setEye(result.state()); // solo subio el timer (no observable) o desperto; el glue sincroniza si toca
        result.awakenedEyeId().ifPresent(eyeId -> applyAwakening(player, data, eyeId));
    }

    /**
     * Decide el despertar de este tick, sin Minecraft. Devuelve el estado resultante (timer acumulado, o el ojo
     * despierto) y, si desperto, el id del ojo. Devuelve el estado <b>sin tocar</b> (misma instancia) cuando no
     * hay nada que hacer, para que {@link #tick} evite escrituras inutiles.
     *
     * @param eyeState  estado ocular actual del jugador
     * @param clanId    id del clan (clave del temporizador de despertar)
     * @param clan      definicion del clan (aporta {@code potentialDojutsu})
     * @param eyeLookup resolucion id de ojo -&gt; {@link EyeDefinition} ({@code EyeRegistry::get} en produccion)
     * @param random    azar para el 50/50 de Otsutsuki (inyectado: testeable con semilla)
     */
    static AwakenResult evaluate(EyeState eyeState, String clanId, ClanDefinition clan,
            Function<String, Optional<EyeDefinition>> eyeLookup, RandomGenerator random) {
        List<EyeDefinition> potential = clan.potentialDojutsu().stream()
                .map(eyeLookup).flatMap(Optional::stream).toList();

        // Uno por clan: si ya desperto cualquiera de sus ojos potenciales, no se dispara de nuevo (unless _wake).
        if (potential.stream().anyMatch(def -> eyeState.isAwakened(def.family()))) {
            return new AwakenResult(eyeState, Optional.empty());
        }
        // Candidatos despertables por clan: implementados y con umbral de despertar declarado.
        List<EyeDefinition> candidates = potential.stream()
                .filter(EyeDefinition::implemented)
                .filter(def -> def.awakenTicks().isPresent())
                .toList();
        if (candidates.isEmpty()) {
            return new AwakenResult(eyeState, Optional.empty());
        }
        int threshold = candidates.stream().mapToInt(def -> def.awakenTicks().orElseThrow()).min().orElseThrow();
        int timer = eyeState.timer(clanId) + 1;
        if (timer < threshold) {
            return new AwakenResult(eyeState.withTimer(clanId, timer), Optional.empty());
        }
        EyeDefinition chosen = candidates.get(random.nextInt(candidates.size())); // 50/50 de Otsutsuki
        EyeState awakened = eyeState
                .withAwakened(chosen.family())
                .withActiveEye(chosen.id())
                .withTimer(clanId, 0);
        // Uso inicial de la familia al despertar (sharingan/awaken fija sharingan_use=78000: nace en tomoe 1).
        if (chosen.awakenInitialUse().isPresent()) {
            awakened = awakened.withUseLevel(chosen.family(), chosen.awakenInitialUse().orElseThrow());
        }
        return new AwakenResult(awakened, Optional.of(chosen.id()));
    }

    /**
     * Despierta un ojo <b>concreto</b>, saltandose el clan y el temporizador: la via de los eventos narrativos
     * (F9/DJT-200, el camino Yin del Sabio despierta Rinnegan o Tenseigan). Aplica exactamente lo mismo que el
     * despertar por clan —marca la familia, lo pone activo, fija el uso inicial y concede chakra/XP— para que un
     * ojo regalado no quede a medias respecto a uno ganado.
     *
     * @param eyeId id del ojo a despertar
     * @return {@code true} si desperto; {@code false} si el ojo no existe, no esta implementado o ya lo tenia
     */
    public static boolean grantEye(ServerPlayer player, String eyeId) {
        Optional<EyeDefinition> lookup = EyeRegistry.get(eyeId).filter(EyeDefinition::implemented);
        if (lookup.isEmpty()) {
            return false;
        }
        EyeDefinition def = lookup.get();
        PlayerNinjaData data = NinjaData.get(player);
        if (data.eye().isAwakened(def.family())) {
            return false; // ya tiene esa familia: no se re-despierta ni se repiten los bonus
        }
        EyeState awakened = data.eye().withAwakened(def.family()).withActiveEye(def.id());
        if (def.awakenInitialUse().isPresent()) {
            awakened = awakened.withUseLevel(def.family(), def.awakenInitialUse().orElseThrow());
        }
        data.setEye(awakened);
        applyAwakening(player, data, def.id());
        return true;
    }

    /** Aplica los efectos del despertar (chakra maximo, XP, evento) y sincroniza. */
    private static void applyAwakening(ServerPlayer player, PlayerNinjaData data, String eyeId) {
        EyeDefinition def = EyeRegistry.get(eyeId).orElseThrow(); // existe: evaluate lo eligio de EyeRegistry
        if (def.awakenMaxChakraBonus() > 0) {
            ChakraService.setMax(data, data.chakra().maxChakra() + def.awakenMaxChakraBonus(), false);
        }
        if (def.awakenNinjaXpBonus() > 0) {
            XpApi.addNinjaXp(player, def.awakenNinjaXpBonus()); // auto-nivela y avisa del rango
        }
        EyeSounds.playAwaken(player, def.family()); // el playsound de dojutsu/<ojo>/awaken (F10/DJT-224)
        DomainEvents.bus().publish(new EyeAwakenedEvent(player.getUUID(), eyeId));
        NinjaSync.sendDelta(player); // empuja el nuevo max de chakra (observable)
    }
}
