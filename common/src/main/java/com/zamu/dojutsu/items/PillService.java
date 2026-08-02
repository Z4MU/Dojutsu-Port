package com.zamu.dojutsu.items;

import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.core.PlayerTickSystem;
import com.zamu.dojutsu.core.TickPhase;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.domain.items.PillTable;
import com.zamu.dojutsu.network.NinjaSync;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Pildoras de Racion Militar {@code [S14]} (F8/DJT-170): chakra ahora, factura despues.
 *
 * <h2>Diez segundos de ventaja y diez de castigo</h2>
 * Comer devuelve <b>un quinto del chakra maximo</b> al instante; la resaca —debilidad, lentitud y fatiga
 * minera— no llega hasta {@value PillTable#HANGOVER_DELAY_TICKS} ticks despues. En combate eso significa que
 * decides con la ventaja y pagas cuando ya no puedes elegir.
 *
 * <h2>Encadenar pildoras no encadena resacas</h2>
 * {@code eat} solo encola la resaca {@code unless ration_pill_queued}: comerte las cinco seguidas te da las
 * cinco recargas y <b>una sola</b> resaca. Es un detalle facil de perder al portar, y es lo que hace que
 * atiborrarse sea una jugada valida en vez de un suicidio.
 *
 * <p>El datapack lo resuelve con un {@code schedule} de 10 s y una bandera; aqui es un temporizador por jugador
 * en el tick que ya existe — sin {@code schedule} global y sin polling de selectores.
 */
public final class PillService implements PlayerTickSystem {

    /** Ticks que faltan para la resaca de cada jugador; ausente = no hay ninguna encolada. */
    private static final Map<UUID, Integer> HANGOVERS = new ConcurrentHashMap<>();

    /** Veto entre mordiscos ({@code d.pill_delay}). */
    private static final Map<UUID, Integer> EAT_DELAYS = new ConcurrentHashMap<>();

    @Override
    public TickPhase phase() {
        return TickPhase.CHAKRA;
    }

    @Override
    public void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        Integer delay = EAT_DELAYS.get(id);
        if (delay != null) {
            if (delay <= 1) {
                EAT_DELAYS.remove(id);
            } else {
                EAT_DELAYS.put(id, delay - 1);
            }
        }
        Integer pending = HANGOVERS.get(id);
        if (pending == null) {
            return; // caso comun: nadie con resaca pendiente
        }
        if (pending > 1) {
            HANGOVERS.put(id, pending - 1);
            return;
        }
        HANGOVERS.remove(id);
        applyHangover(player);
    }

    /**
     * Come una pildora: devuelve chakra, deja el veto entre mordiscos y <b>encola la resaca solo si no habia
     * ninguna</b>.
     *
     * @return {@code true} si se comio (y por tanto el bote debe gastar una carga)
     */
    public static boolean eat(ServerPlayer player, int pillsLeft) {
        UUID id = player.getUUID();
        if (!PillTable.hasPillsLeft(pillsLeft) || EAT_DELAYS.containsKey(id)) {
            return false;
        }
        PlayerNinjaData data = NinjaData.get(player);
        int restored = PillTable.chakraRestored(data.chakra().maxChakra());
        ChakraService.set(data, Math.min(data.chakra().chakra() + restored, data.chakra().maxChakra()));
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 20, 1));
        player.playNotifySound(SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.3f, 0.7f);
        EAT_DELAYS.put(id, PillTable.EAT_DELAY_TICKS);
        HANGOVERS.putIfAbsent(id, PillTable.HANGOVER_DELAY_TICKS); // putIfAbsent = "unless ration_pill_queued"
        NinjaSync.sendDelta(player);
        return true;
    }

    /** Indica si al jugador le queda una resaca por llegar (para el comando de QA). */
    public static boolean hangoverPending(UUID playerId) {
        return HANGOVERS.containsKey(playerId);
    }

    /** Olvida los temporizadores (al desconectar): la resaca no persigue entre sesiones. */
    public static void forget(UUID playerId) {
        HANGOVERS.remove(playerId);
        EAT_DELAYS.remove(playerId);
    }

    /** La factura ({@code ration_pills/effects}). */
    private static void applyHangover(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                PillTable.HANGOVER_WEAKNESS_TICKS, PillTable.HANGOVER_AMPLIFIER));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                PillTable.HANGOVER_WEAKNESS_TICKS, PillTable.HANGOVER_AMPLIFIER));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, PillTable.HANGOVER_FATIGUE_TICKS, 0));
        player.sendSystemMessage(Component.translatable("pill.dojutsu.hangover")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
