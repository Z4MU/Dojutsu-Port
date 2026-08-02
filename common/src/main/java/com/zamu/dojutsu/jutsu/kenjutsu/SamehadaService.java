package com.zamu.dojutsu.jutsu.kenjutsu;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.items.WeaponComponent;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.registry.ModDataComponents;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/**
 * Samehada (F4/DJT-084, {@code [S07]}): la espada viva que <b>absorbe chakra</b> al golpear. Comportamiento
 * especial sobre el framework de armas (DJT-082) + un hook propio en el evento de daño.
 *
 * <p><b>Fidelidad al datapack</b> ({@code jutsu/kenjutsu/samehada/absorb} + {@code absorb_ply}, auditado):
 * <ul>
 *   <li>Golpear un <b>mob</b> con la Samehada → el portador gana <b>+100 chakra</b> (fijo).</li>
 *   <li>Golpear un <b>jugador</b> → <b>drena</b> hasta 100 de su chakra y lo <b>transfiere</b> al portador
 *       (el que roba gana exactamente lo que el objetivo pierde; si el objetivo tiene menos de 100, roba lo
 *       que haya).</li>
 * </ul>
 * En ambos casos el chakra del portador se clampa a su maximo ({@code ChakraService}). Solo actua si el arma
 * en mano es la Samehada y el kenjutsu del portador alcanza su requisito ({@code hold_check kenjutsu_1000p},
 * reescalado ÷4 = 250 en la escala del mod; misma politica que DJT-082/083).
 *
 * <p>La absorcion <b>no cancela</b> el daño del golpe (el evento se deja pasar): la Samehada hace daño normal
 * (base 6) y ademas roba chakra.
 */
public final class SamehadaService {

    /** Id del arma Samehada (en su {@link WeaponComponent}). */
    public static final String WEAPON_TYPE = "samehada";

    /** Chakra maximo absorbido por golpe ({@code absorb}: +100 de mob; drena hasta 100 de jugador). */
    public static final int ABSORB_PER_HIT = 100;

    /** Nivel de kenjutsu (escala del mod, 1000 datapack ÷4) para que la Samehada sea efectiva. */
    public static final int KENJUTSU_REQ = 250;

    private static final Logger LOGGER = LogUtils.getLogger();

    private SamehadaService() {
    }

    /** Suscribe el hook de daño. Llamar una vez en el arranque. */
    public static void register() {
        EntityEvent.LIVING_HURT.register(SamehadaService::onLivingHurt);
    }

    /**
     * Cuanto chakra se drena de un objetivo <b>jugador</b> con {@code targetChakra} disponible: hasta
     * {@link #ABSORB_PER_HIT}, o lo que tenga si es menos. Puro y testeable.
     */
    public static int drainFromPlayer(int targetChakra) {
        return Math.max(0, Math.min(ABSORB_PER_HIT, targetChakra));
    }

    /** Indica si el portador puede usar la Samehada (arma correcta en mano + kenjutsu suficiente). */
    public static boolean canAbsorb(ItemStack heldWeapon, int kenjutsuLevel) {
        WeaponComponent weapon = heldWeapon.get(ModDataComponents.WEAPON.get());
        return weapon != null && WEAPON_TYPE.equals(weapon.weaponType()) && kenjutsuLevel >= KENJUTSU_REQ;
    }

    private static EventResult onLivingHurt(LivingEntity target, DamageSource source, float amount) {
        // Solo nos interesa el golpe cuerpo a cuerpo de un jugador (el atacante directo).
        if (source.getDirectEntity() instanceof ServerPlayer attacker && attacker != target) {
            tryAbsorb(attacker, target);
        }
        return EventResult.pass(); // nunca cancelamos el daño: la Samehada golpea y ademas roba
    }

    private static void tryAbsorb(ServerPlayer attacker, LivingEntity target) {
        PlayerNinjaData attackerData = NinjaData.get(attacker);
        int kenjutsu = attackerData.progression().skill(Skill.KENJUTSU).level();
        if (!canAbsorb(attacker.getMainHandItem(), kenjutsu)) {
            return; // no lleva la Samehada o no tiene el nivel: golpe normal, sin absorcion
        }
        if (target instanceof ServerPlayer victim) {
            PlayerNinjaData victimData = NinjaData.get(victim);
            int drained = drainFromPlayer(victimData.chakra().chakra());
            if (drained <= 0) {
                return; // el objetivo no tenia chakra que robar
            }
            ChakraService.consume(victimData, drained); // exacto: drained <= chakra del objetivo
            ChakraService.restore(attackerData, drained); // el portador gana lo drenado (clamp al maximo)
            NinjaSync.sendDelta(victim);
            NinjaSync.sendDelta(attacker);
            LOGGER.debug("[{}] Samehada: {} drena {} de chakra a {}.",
                    DojutsuMod.MOD_NAME, attacker.getGameProfile().getName(), drained, victim.getGameProfile().getName());
        } else {
            ChakraService.restore(attackerData, ABSORB_PER_HIT); // +100 fijo de un mob (clamp al maximo)
            NinjaSync.sendDelta(attacker);
        }
    }
}
