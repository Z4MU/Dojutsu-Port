package com.zamu.dojutsu.items;

import com.zamu.dojutsu.bijuu.jinchuriki.FormService;
import com.zamu.dojutsu.bijuu.jinchuriki.SealingService;
import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.SageState;
import com.zamu.dojutsu.domain.items.ScrollType;
import com.zamu.dojutsu.domain.sage.NaturalEnergyTable;
import com.zamu.dojutsu.network.NinjaSync;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Uso de los tres <b>pergaminos</b> ({@code [S14]}, F10/DJT-225). Porta
 * {@code items/medical_scroll/restore}, {@code items/sage_scroll/restore} y {@code jinchuriki/removal}.
 *
 * <h2>Quien decide si el pergamino se gasta</h2>
 * El datapack gasta el pergamino <b>dentro</b> de la funcion de efecto, y solo en la rama que tuvo exito
 * ({@code execute if predicate ... run clear}). Aqui el servicio devuelve {@code true} exactamente cuando el
 * datapack habria hecho ese {@code clear}, y el {@link ScrollItem} lo consume: un pergamino de sabio usado por
 * quien no es sabio <b>no se pierde</b>.
 *
 * <h2>El pergamino medico hace dos cosas</h2>
 * Llena el chakra <b>y</b> pone la fatiga ocular a 0 — es la unica via, aparte de dormir, para bajarla de golpe.
 * Es tambien el unico de los tres que <b>nunca falla</b>: su filtro esta en el crafteo (medicina 75), no en el
 * uso.
 *
 * <h2>El de retirada apaga lo que la extraccion no apagaria</h2>
 * {@code jinchuriki/removal} desactiva el Modo Baryon y la forma de jinchuriki <b>antes</b> de soltar la bestia:
 * si no, quedaria un modo encendido sin bestia que lo sostenga. {@code SealingService.extract} devuelve lo
 * prestado pero no toca los modos (su cliente hasta hoy era el comando de QA, que se usa fuera de forma), asi
 * que el apagado se hace aqui, en el mismo orden que el datapack.
 */
public final class ScrollService {

    private ScrollService() {
    }

    /**
     * Usa un pergamino.
     *
     * @return {@code true} si el pergamino surtio efecto y debe consumirse
     */
    public static boolean use(ServerPlayer player, ScrollType type) {
        return switch (type) {
            case MEDICAL -> useMedical(player);
            case REMOVAL -> useRemoval(player);
            case SAGE -> useSage(player);
        };
    }

    /** {@code items/medical_scroll/restore}: chakra al maximo + fatiga ocular a 0. Nunca falla. */
    private static boolean useMedical(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        ChakraService.restoreFull(data);
        data.setEye(data.eye().withEyeStrain(0));
        NinjaSync.sendDelta(player);
        return true;
    }

    /** {@code jinchuriki/removal}: apaga los modos y extrae la bestia. Falla si no eres jinchuriki. */
    private static boolean useRemoval(ServerPlayer player) {
        if (SealingService.beastOf(player).isEmpty()) {
            player.sendSystemMessage(Component.translatable("jinchuriki.dojutsu.not_jinchuriki")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        if (FormService.baryonActive(player.getUUID())) {
            FormService.toggleBaryon(player);
        }
        if (FormService.isActive(player.getUUID())) {
            FormService.toggle(player);
        }
        return SealingService.extract(player).isPresent();
    }

    /** {@code items/sage_scroll/restore}: energia natural al maximo. Falla si no eres sabio. */
    private static boolean useSage(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        SageState sage = data.sage();
        if (!sage.sage()) {
            player.sendSystemMessage(Component.translatable("scroll.dojutsu.not_sage")
                    .withStyle(ChatFormatting.DARK_RED));
            player.playNotifySound(SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.3F, 1.0F);
            return false;
        }
        data.setSage(sage.withCharge(NaturalEnergyTable.MAX_CHARGE));
        NinjaSync.sendDelta(player);
        return true;
    }
}
