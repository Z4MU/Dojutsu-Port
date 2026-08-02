package com.zamu.dojutsu.trading;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.core.config.DojutsuConfig;
import com.zamu.dojutsu.domain.trading.TraderCatalogue;
import com.zamu.dojutsu.domain.trading.TraderOffer;
import com.zamu.dojutsu.domain.trading.TradingTable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.slf4j.Logger;

/**
 * Convierte aldeanos y mercaderes errantes en comerciantes ninja {@code [S18]} (F8/DJT-190).
 *
 * <h2>Por evento, no por barrido</h2>
 * El datapack usa {@code setup_custom_entities}, un bucle {@code schedule 1s} que <b>recorre todos los aldeanos y
 * mercaderes del mundo cada segundo</b> preguntando si ya los ha procesado (tag {@code d.tsetup}). Funciona, pero
 * el coste es permanente y crece con la aldea.
 *
 * <p>Aqui se engancha a {@code EntityEvent.ADD} y se decide <b>una vez, al aparecer</b>. Ni barrido, ni tag de
 * "ya visto", ni coste con cero comerciantes — que es justo lo que pedia el checklist de la ficha ("conversion por
 * evento, no polling").
 *
 * <h2>Solo uno de cada diez aldeanos</h2>
 * {@value TradingTable#SHINOBI_CHANCE_PERCENT}% ({@code trades_rng} 0..19, gana 0..1). El mercader errante, en
 * cambio, se convierte <b>siempre</b>: el datapack no le tira dados, porque ya es raro de por si.
 *
 * <p>Las dos conversiones respetan su bandera de config ({@code allowShinobis} y {@code allowCustomTrader}).
 */
public final class ShinobiVillagerService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ShinobiVillagerService() {
    }

    /** Suscribe la conversion al alta de entidades. Llamar una vez en el arranque. */
    public static void register() {
        dev.architectury.event.events.common.EntityEvent.ADD.register((entity, level) -> {
            if (!level.isClientSide()) {
                onEntityAdded(entity);
            }
            return dev.architectury.event.EventResult.pass();
        });
    }

    /** Decide y convierte, una sola vez, en el momento en que la entidad entra al mundo. */
    private static void onEntityAdded(Entity entity) {
        if (entity instanceof WanderingTrader trader) {
            if (DojutsuConfig.get().allowCustomTrader()) {
                convert(trader, TraderCatalogue.Kind.WANDERING_TRADER, null);
            }
            return;
        }
        if (entity instanceof Villager villager
                && DojutsuConfig.get().allowShinobis()
                && !villager.isBaby()
                && TradingTable.isShinobiRoll(villager.getRandom().nextInt(TradingTable.SHINOBI_ROLL_OUTCOMES))) {
            convert(villager, TraderCatalogue.Kind.SHINOBI_VILLAGER,
                    Component.translatable("entity.dojutsu.shinobi_villager").withStyle(ChatFormatting.GOLD));
        }
    }

    /**
     * Aplica el catalogo a un comerciante: le pone las ofertas cuya mercancia exista y, si procede, su nombre.
     *
     * @return numero de ofertas realmente aplicadas
     */
    public static int convert(AbstractVillager merchant, TraderCatalogue.Kind kind, Component name) {
        Optional<TraderCatalogue> catalogue = TradeRegistry.get(kind);
        if (catalogue.isEmpty()) {
            return 0;
        }
        List<MerchantOffer> offers = new ArrayList<>();
        int skipped = 0;
        for (TraderOffer offer : catalogue.get().offers()) {
            ItemStack stack = resolve(offer);
            if (stack.isEmpty()) {
                skipped++;
                continue; // mercancia declarada cuyo item aun no existe: se salta, no rompe
            }
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, offer.price()), stack, offer.maxUses(), offer.xp(), 0.05F));
        }
        if (offers.isEmpty()) {
            return 0;
        }
        if (merchant instanceof Villager villager) {
            // setup_shinobi: librarian nivel 5, para que el aldeano no reasigne su profesion ni sus trades
            villager.setVillagerData(new VillagerData(villager.getVillagerData().getType(),
                    VillagerProfession.LIBRARIAN, TradingTable.SHINOBI_VILLAGER_LEVEL));
        }
        merchant.getOffers().clear();
        merchant.getOffers().addAll(offers);
        if (name != null) {
            merchant.setCustomName(name);
            merchant.setCustomNameVisible(true);
        }
        LOGGER.debug("[{}] {} convertido con {} ofertas ({} sin item aun).",
                DojutsuMod.MOD_NAME, kind.id(), offers.size(), skipped);
        return offers.size();
    }

    /** Resuelve la mercancia de una oferta; stack vacio si el item no esta registrado. */
    private static ItemStack resolve(TraderOffer offer) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(offer.item()))
                .map(item -> new ItemStack(item, offer.count()))
                .orElse(ItemStack.EMPTY);
    }
}
