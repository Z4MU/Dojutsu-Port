package com.zamu.dojutsu.bijuu.jinchuriki;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.core.DomainEvents;
import com.zamu.dojutsu.core.config.DojutsuConfig;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.JinchurikiState;
import com.zamu.dojutsu.domain.bijuu.SealingTable;
import com.zamu.dojutsu.domain.bijuu.TailedBeast;
import com.zamu.dojutsu.domain.event.BeastSealedEvent;
import com.zamu.dojutsu.natures.TemporaryGrantService;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.progression.XpApi;
import com.zamu.dojutsu.sixpaths.SixPathsService;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;

/**
 * Sellado y extraccion de Bestias con Cola {@code [S09]} (F7/DJT-165): convertir a un jugador en
 * <b>jinchuriki</b>, y deshacerlo sin dejar rastro.
 *
 * <h2>Dos exclusividades, no una</h2>
 * <ul>
 *   <li><b>Un jugador, una bestia</b>: {@code seal} veta si ya llevas alguna ("You already have a Tailed Beast
 *       sealed inside you!"). Vive en el estado del jugador.</li>
 *   <li><b>Una bestia, un jinchuriki</b> —si {@code allowMultJinchuriki} esta desactivado—: eso <b>no</b> se
 *       puede responder mirando al jugador que tienes delante, porque el portador puede estar desconectado. De
 *       ahi el {@link TailedBeastRegistry} global (S09-R4).</li>
 * </ul>
 *
 * <h2>Todo lo que presta, lo devuelve</h2>
 * Sellar sube el chakra maximo <b>1000 por cola</b> y presta naturalezas y a veces un Kekkei Genkai. El riesgo
 * {@code [RA-2]} no es el tamaño del numero, es no poder deshacerlo:
 * <ul>
 *   <li>El chakra prestado se <b>guarda</b> en {@link JinchurikiState#grantedMaxChakra()} y se devuelve exacto,
 *       en vez de restar la constante de la tabla como hace {@code removal/*} (que se rompe si la tabla cambia).</li>
 *   <li>Las naturalezas y el KG van por {@link TemporaryGrantService} con la bestia como {@code source}, que ya
 *       sabe <b>no pisar lo que ya era tuyo</b> ni llevarselo al retirarse. Escrito en F3/DJT-053 anticipando
 *       este momento; hoy encuentra su consumidor real.</li>
 * </ul>
 *
 * <p><b>Lo que aun no esta</b> (por dependencia, no por olvido): el disparador real es el <b>item de chakra</b>
 * que suelta la bestia al morir, y exige tener activo el jutsu <b>Sello de los Cuatro Simbolos</b>
 * ({@code fss_timer}). Los items son F8; hoy el disparador es el comando de QA. El caso especial del
 * <b>Diez Colas</b> ({@code ten_tails/seal}, que salta la comprobacion de "ya llevas una") converge en F9 con el
 * resto de su nodo.
 */
public final class SealingService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private SealingService() {
    }

    /** Por que no se pudo sellar. */
    public enum SealOutcome {
        /** Sellada con exito. */
        SEALED,
        /** El jugador ya lleva una bestia dentro. */
        ALREADY_JINCHURIKI,
        /** Otro jugador ya porta esa bestia y la config no permite duplicados. */
        BEAST_TAKEN
    }

    /**
     * Sella una bestia en el jugador: exclusividades, chakra prestado, XP, elementos prestados y evento.
     *
     * @return el resultado; solo {@link SealOutcome#SEALED} cambia algo
     */
    public static SealOutcome seal(ServerPlayer player, TailedBeast beast) {
        PlayerNinjaData data = NinjaData.get(player);
        if (data.jinchuriki().isJinchuriki()) {
            player.sendSystemMessage(Component.translatable("jinchuriki.dojutsu.already")
                    .withStyle(ChatFormatting.RED));
            return SealOutcome.ALREADY_JINCHURIKI;
        }
        TailedBeastRegistry registry = registryOf(player);
        if (!DojutsuConfig.get().allowMultJinchuriki() && registry.isTaken(beast)) {
            player.sendSystemMessage(Component.translatable("jinchuriki.dojutsu.beast_taken")
                    .withStyle(ChatFormatting.RED));
            return SealOutcome.BEAST_TAKEN;
        }

        int granted = SealingTable.maxChakraBonus(beast);
        ChakraService.setMax(data, data.chakra().maxChakra() + granted, false);
        ChakraService.set(data, Math.min(data.chakra().chakra() + granted, data.chakra().maxChakra()));
        data.setJinchuriki(data.jinchuriki().withSealed(beast.id(), SealingTable.INITIAL_LEVEL, granted));
        XpApi.addNinjaXp(player, SealingTable.ninjaXp(beast));
        lendElements(player, beast);
        registry.claim(beast, player.getUUID());

        DomainEvents.bus().publish(new BeastSealedEvent(player.getUUID(), beast.id()));
        player.sendSystemMessage(Component.translatable("jinchuriki.dojutsu.sealed",
                Component.translatable("entity.dojutsu.tailed_beast." + beast.id())
                        .withStyle(s -> s.withColor(beast.chakraColor()))).withStyle(ChatFormatting.GOLD));
        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS,
                    1F, 0.6F);
        }
        NinjaSync.sendDelta(player);
        LOGGER.info("[{}] {} es ahora el jinchuriki de {} (+{} de chakra maximo).",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName(), beast.id(), granted);
        return SealOutcome.SEALED;
    }

    /**
     * Extrae la bestia del jugador y <b>devuelve todo lo prestado</b> ({@code jinchuriki/removal/*}): el chakra
     * maximo exacto que se presto, la XP, los elementos y el slot global.
     *
     * @return la bestia extraida, o vacio si no llevaba ninguna
     */
    public static Optional<TailedBeast> extract(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        JinchurikiState state = data.jinchuriki();
        Optional<TailedBeast> sealed = state.sealedBeast().flatMap(TailedBeast::byId);
        if (sealed.isEmpty()) {
            player.sendSystemMessage(Component.translatable("jinchuriki.dojutsu.not_jinchuriki")
                    .withStyle(ChatFormatting.RED));
            return Optional.empty();
        }
        TailedBeast beast = sealed.get();

        // se devuelve lo que se guardo, no lo que diga hoy la tabla: la extraccion es exacta por construccion
        int granted = state.grantedMaxChakra();
        ChakraService.setMax(data, Math.max(data.chakra().maxChakra() - granted, 0), false);
        ChakraService.set(data, Math.min(data.chakra().chakra(), data.chakra().maxChakra()));
        data.setJinchuriki(state.withoutBeast());
        TemporaryGrantService.revokeAllFrom(player, beast.id());
        SixPathsService.revokeFrom(player, beast.id()); // remove_tmp barre tmp_sp_chakra junto a lo demas
        registryOf(player).release(beast);

        player.sendSystemMessage(Component.translatable("jinchuriki.dojutsu.extracted",
                Component.translatable("entity.dojutsu.tailed_beast." + beast.id())
                        .withStyle(s -> s.withColor(beast.chakraColor()))).withStyle(ChatFormatting.RED));
        NinjaSync.sendDelta(player);
        LOGGER.info("[{}] {} deja de ser el jinchuriki de {} (-{} de chakra maximo).",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName(), beast.id(), granted);
        return sealed;
    }

    /** Bestia que lleva ese jugador, o vacio. */
    public static Optional<TailedBeast> beastOf(ServerPlayer player) {
        return NinjaData.get(player).jinchuriki().sealedBeast().flatMap(TailedBeast::byId);
    }

    /** El registro global de la partida. */
    public static TailedBeastRegistry registryOf(ServerPlayer player) {
        return TailedBeastRegistry.of(player.getServer());
    }

    /**
     * Presta los elementos de la bestia ({@code sealing/*}: primero las naturalezas componente, luego el KG, en
     * ese orden). {@link TemporaryGrantService} ya garantiza que no pisa lo que el jugador tuviera de antes.
     */
    private static void lendElements(ServerPlayer player, TailedBeast beast) {
        SealingTable.naturesOf(beast)
                .forEach(nature -> TemporaryGrantService.grantNature(player, nature, beast.id()));
        SealingTable.kekkeiGenkaiOf(beast)
                .ifPresent(kg -> TemporaryGrantService.grantKekkeiGenkai(player, kg, beast.id()));
        if (beast.isTenTails()) {
            // sealing/ten_tails: el Diez Colas presta ademas el chakra de los Seis Caminos (tmp_sp_chakra), con
            // la misma vida que sus elementos. Es la tercera via del nodo de convergencia (F9/DJT-210) y la
            // unica que lo presta en vez de concederlo.
            SixPathsService.lend(player, beast.id());
        }
    }
}
