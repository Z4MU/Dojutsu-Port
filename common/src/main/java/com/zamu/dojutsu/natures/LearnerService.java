package com.zamu.dojutsu.natures;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.core.DomainEvents;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.domain.event.KekkeiGenkaiGrantedEvent;
import com.zamu.dojutsu.domain.event.NatureGrantedEvent;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.items.LearnerComponent;
import com.zamu.dojutsu.registry.ModDataComponents;
import com.zamu.dojutsu.registry.ModItems;
import dev.architectury.utils.GameInstance;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/**
 * El "Skill Learner" ({@code [S03]}/{@code [S06]}): entrega y uso del item que ensena los jutsus de un
 * elemento. Reemplaza las 24 funciones {@code give/learner/<elem>} y el arbol {@code jutsu/learner/*}.
 *
 * <p><b>Entrega reactiva</b> ({@code [ADD/05 §2]}): se suscribe a {@link NatureGrantedEvent} y
 * {@link KekkeiGenkaiGrantedEvent} — igual que en el datapack cada {@code give/<elem>} llamaba a
 * {@code give/learner/<elem>}. Asi la entrega cubre <b>todos</b> los caminos de grant (comando, KG que implica
 * componentes, prestamo de bijuu, onboarding, clan) sin repetirla en cada llamador. Con inventario lleno, el
 * item se dropea (como el {@code give} vanilla).
 *
 * <p><b>Sin learner en el datapack:</b> {@code shikotsumyaku} y {@code jugo_clan} (sus "jutsus" van por otro
 * camino). Verificado: {@code give/learner/} tiene 24 archivos = 8 naturalezas + 16 KG.
 *
 * <p><b>Uso:</b> desde F4/DJT-075 el click derecho lo atiende {@code jutsu.JutsuLearningService} (posesion,
 * secuencia, cooldown y entrega del jutsu). Este servicio conserva la entrega/retirada de learners y el helper
 * de posesion; la <b>entrega resetea el progreso</b> del elemento (fiel a {@code give/learner/<elem>}, que
 * hacia {@code set d.lrn_<elem> 0}). La dependencia va {@code jutsu → natures} (nunca al reves): asi no se
 * repite el ciclo entre modulos que DJT-057 tuvo que romper.
 *
 * <p><b>Politica de revoke (decision del mod, {@code [S03-R2]}, fijada en DJT-051):</b> el datapack dejaba el
 * learner residual al perder el elemento; aqui {@link #removeStaleLearners} barre del inventario los learners
 * de elementos que ya no se poseen. Lo llaman los revoke de {@code NatureService}/{@code KekkeiGenkaiService}/
 * {@code TemporaryGrantService}.
 */
public final class LearnerService {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** KG cuyo {@code give/kg/<kg>} no entrega learner (verificado en el datapack). */
    private static final Set<KekkeiGenkai> WITHOUT_LEARNER =
            EnumSet.of(KekkeiGenkai.SHIKOTSUMYAKU, KekkeiGenkai.JUGO_CLAN);

    private LearnerService() {
    }

    /** Suscribe la entrega reactiva al bus de dominio. Llamar una vez en el arranque. */
    public static void register() {
        DomainEvents.bus().subscribe(NatureGrantedEvent.class, e -> deliver(e.player(), e.natureId()));
        DomainEvents.bus().subscribe(KekkeiGenkaiGrantedEvent.class, e -> deliver(e.player(), e.kekkeiGenkaiId()));
    }

    /** Indica si el elemento tiene learner (todas las naturalezas; los KG salvo shikotsumyaku/jugo_clan). */
    public static boolean hasLearner(LearnerComponent element) {
        if (element.nature().isPresent()) {
            return true;
        }
        return element.kekkeiGenkai().map(kg -> !WITHOUT_LEARNER.contains(kg)).orElse(false);
    }

    /** Crea el ItemStack del learner de un elemento (item + componente + nombre localizado + modelo). */
    public static ItemStack createLearner(String element) {
        ItemStack stack = new ItemStack(ModItems.LEARNER.get());
        stack.set(ModDataComponents.LEARNER.get(), new LearnerComponent(element));
        stack.set(DataComponents.ITEM_NAME,
                Component.translatable("item.dojutsu.learner.of", Component.translatable("element.dojutsu." + element)));
        // Modelo por elemento: custom_model_data (predicado vanilla) -> el modelo base lo mapea (DJT-085c).
        int modelData = com.zamu.dojutsu.items.LearnerModelData.of(element);
        if (modelData > 0) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA,
                    new net.minecraft.world.item.component.CustomModelData(modelData));
        }
        return stack;
    }

    /** Entrega el learner de un elemento al jugador; si el inventario esta lleno, lo dropea (como el give vanilla). */
    public static void giveLearner(ServerPlayer player, String element) {
        ItemStack stack = createLearner(element);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        LOGGER.info("[{}] {} recibio el Skill Learner de {}.",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName(), element);
    }

    /**
     * Retira del inventario los learners de elementos que el jugador <b>ya no posee</b> (politica de revoke del
     * mod, {@code [S03-R2]}). Idempotente; no toca learners de elementos vigentes.
     *
     * @return cuantos items retiro
     */
    public static int removeStaleLearners(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        int removed = player.getInventory().clearOrCountMatchingItems(
                stack -> {
                    LearnerComponent component = stack.get(ModDataComponents.LEARNER.get());
                    return component != null && !possesses(data, component);
                }, -1, player.inventoryMenu.getCraftSlots());
        if (removed > 0) {
            player.containerMenu.broadcastChanges();
            player.inventoryMenu.slotsChanged(player.getInventory());
            LOGGER.info("[{}] {} perdio {} Skill Learner(s) de elementos revocados.",
                    DojutsuMod.MOD_NAME, player.getGameProfile().getName(), removed);
        }
        return removed;
    }

    /**
     * Indica si el jugador posee el elemento del learner (naturaleza o KG). Es la comprobacion de
     * {@code jutsu/learner/check}; la consume {@code jutsu.JutsuLearningService} (DJT-075).
     */
    public static boolean possesses(PlayerNinjaData data, LearnerComponent component) {
        return component.nature().map(n -> data.natures().has(n))
                .or(() -> component.kekkeiGenkai().map(kg -> data.natures().has(kg)))
                .orElse(false);
    }

    /** Entrega reactiva: busca al jugador online (los grants ocurren con el jugador conectado). */
    private static void deliver(UUID playerId, String element) {
        LearnerComponent component = new LearnerComponent(element);
        if (!hasLearner(component)) {
            return; // shikotsumyaku/jugo_clan: sin learner en el datapack
        }
        MinecraftServer server = GameInstance.getServer();
        if (server == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return; // el jugador ya no esta conectado
        }
        // Fiel a give/learner/<elem>: recibir un learner nuevo reinicia el progreso del elemento.
        PlayerNinjaData data = NinjaData.get(player);
        data.setJutsu(data.jutsu().withProgressReset(component.element()));
        giveLearner(player, component.element());
    }
}
