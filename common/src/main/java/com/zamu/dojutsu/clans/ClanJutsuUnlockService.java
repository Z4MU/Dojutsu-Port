package com.zamu.dojutsu.clans;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.core.PlayerTickSystem;
import com.zamu.dojutsu.core.TickPhase;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.ClanMembership;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Desbloqueo progresivo de jutsus de clan ({@code clans/jutsu/main} §"Unlocking", {@code [S04]}): los clanes
 * con jutsus ocultos (Nara/Kaguya/Akimichi/Aburame) acumulan {@code d.<clan>_time} por tick de juego y, al
 * llegar al umbral ({@code jutsuUnlockTicks} de la {@link ClanDefinition}: 78000 = 65 min), desbloquean el
 * siguiente jutsu de su secuencia.
 *
 * <p><b>Correccion de la doble ejecucion (el objetivo del ticket):</b> en el datapack, {@code clans/jutsu/main}
 * estaba registrado en {@code minecraft/tags/functions/tick.json} <b>y</b> ademas lo llamaba
 * {@code player_functions} → corria <b>dos veces por tick</b> (los contadores subian al doble, los timers de
 * jutsu se decrementaban 2/tick...). Aqui el sistema se registra <b>una sola vez</b> en el
 * {@code PlayerTickPipeline} ({@link TickPhase#UNLOCKS}), el punto unico de registro por construccion.
 *
 * <p><b>Stub de F3:</b> el contador avanza y se <b>capa en el umbral</b> (no se pierde progreso ni desborda);
 * la <b>entrega</b> del jutsu (secuencias por clan: Nara → shadow_clutch → shadow_sewing, etc., con reset del
 * contador al entregar) es <b>TODO F4/DJT-075</b>, cuando existan los jutsus. La mecanica de los jutsus de
 * clan en si (Shadow Paralysis/Sewing/Clutch, dances, Butterfly Mode...) tambien es F4 — <b>nota de
 * rendimiento S04-R6 para F4</b>: Shadow Clutch recorre y arrastra entidades cada tick; disenar con presupuesto
 * (area acotada, no {@code @e} global).
 *
 * <p>Casos borde (de la ficha): sin clan o clan sin desbloqueo progresivo (Uchiha, Lee...) → no acumula nada.
 * El contador persiste en {@link ClanMembership#jutsuTime()} y el reset de clan lo limpia (defaults atomicos).
 */
public final class ClanJutsuUnlockService implements PlayerTickSystem {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public TickPhase phase() {
        return TickPhase.UNLOCKS;
    }

    @Override
    public void tick(ServerPlayer player) {
        if (player.isSpectator() || !player.isAlive()) {
            return; // como el resto de sistemas por tick: sin progreso para espectador ni muerto
        }
        PlayerNinjaData data = NinjaData.get(player);
        if (advance(data) && reachedThreshold(data)) {
            // TODO F4/DJT-075: entregar el siguiente jutsu de la secuencia del clan y resetear el contador.
            LOGGER.info("[{}] {} alcanzo el umbral de desbloqueo de jutsu de clan ({}): entrega pendiente de F4.",
                    DojutsuMod.MOD_NAME, player.getGameProfile().getName(), data.clan().clan().orElse("?"));
        }
    }

    /**
     * Avanza un tick el contador de desbloqueo, <b>capado al umbral del clan</b> (llegar al tope no pierde
     * progreso: F4 entregara y reseteara). <b>Puro</b> (testeable sin MC).
     *
     * @return {@code true} si el contador cambio (sin clan, clan sin desbloqueo o ya en el tope: {@code false})
     */
    public static boolean advance(PlayerNinjaData data) {
        ClanMembership membership = data.clan();
        Optional<Integer> threshold = unlockTicksOf(membership);
        if (threshold.isEmpty() || membership.jutsuTime() >= threshold.get()) {
            return false; // sin clan, clan sin desbloqueo progresivo, o ya listo (la entrega es F4)
        }
        data.setClan(membership.withJutsuTime(membership.jutsuTime() + 1));
        return true;
    }

    /** Indica si el contador esta en el umbral del clan (listo para el siguiente jutsu). Puro. */
    public static boolean reachedThreshold(PlayerNinjaData data) {
        return unlockTicksOf(data.clan()).map(t -> data.clan().jutsuTime() >= t).orElse(false);
    }

    private static Optional<Integer> unlockTicksOf(ClanMembership membership) {
        return membership.clan().flatMap(ClanRegistry::get).flatMap(ClanDefinition::jutsuUnlockTicks);
    }
}
