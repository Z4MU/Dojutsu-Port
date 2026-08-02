package com.zamu.dojutsu.summoning;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.core.PlayerTickSystem;
import com.zamu.dojutsu.core.TickPhase;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.summoning.SummonContractDef;
import com.zamu.dojutsu.domain.summoning.SummonTable;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

/**
 * Firma de contratos de invocacion {@code [S15]} (F8/DJT-175): el pacto con la especie.
 *
 * <h2>Se firma golpeando, y la criatura muere en el pacto</h2>
 * {@code attempt_form_contract} se dispara al <b>golpear</b> a un bicho llevando un contrato en blanco, y al
 * cerrarse el pacto hace {@code kill @s}: la criatura con la que firmas <b>no sobrevive</b>. Suena brutal y lo es,
 * pero es lo que hay en el datapack y cambia como se siente el sistema — no coleccionas amigos, sellas un
 * linaje.
 *
 * <h2>Firmar y usar son dos puertas distintas</h2>
 * Firmar exige rango <b>Chunin</b> para cualquier tier, y fuinjutsu solo en mid/high. Usar exige fuinjutsu
 * <b>tambien</b> en el tier bajo. Puedes acabar con un contrato firmado que todavia no puedes usar (ver
 * {@code SummonTier}).
 *
 * <p>Este servicio ademas <b>tickea el veto</b> de {@link SummonService}: ya recorre a los jugadores, asi que no
 * hace falta un segundo sistema por tick solo para restar uno.
 */
public final class ContractService implements PlayerTickSystem {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public TickPhase phase() {
        return TickPhase.CHAKRA;
    }

    @Override
    public void tick(ServerPlayer player) {
        SummonService.tickCooldown(player.getUUID());
    }

    /** Por que no se pudo firmar. */
    public enum FormOutcome {
        /** Contrato firmado (y la criatura consumida en el pacto). */
        FORMED,
        /** Rango por debajo de Chunin. */
        LOW_RANK,
        /** Fuinjutsu insuficiente para el tier de esa especie. */
        LOW_FUINJUTSU,
        /** Esa especie no admite contrato. */
        NOT_CONTRACTABLE
    }

    /**
     * Intenta firmar un contrato con la criatura golpeada.
     *
     * @return el resultado; solo {@link FormOutcome#FORMED} consume la criatura y firma el contrato
     */
    public static FormOutcome form(ServerPlayer player, LivingEntity target) {
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        Optional<SummonContractDef> contract = ContractRegistry.forEntity(entityId);
        if (contract.isEmpty()) {
            reject(player, "contract.dojutsu.not_contractable");
            return FormOutcome.NOT_CONTRACTABLE;
        }
        PlayerNinjaData data = NinjaData.get(player);
        int rank = data.progression().rank().ordinal();
        if (rank < SummonTable.CONTRACT_MIN_RANK) {
            reject(player, "contract.dojutsu.low_rank");
            return FormOutcome.LOW_RANK;
        }
        SummonContractDef def = contract.get();
        int fuinjutsu = data.progression().skill(Skill.FUINJUTSU).level();
        if (!def.tier().canForm(fuinjutsu, rank)) {
            reject(player, "contract.dojutsu.low_fuinjutsu");
            return FormOutcome.LOW_FUINJUTSU;
        }

        player.sendSystemMessage(Component.translatable("contract.dojutsu.formed",
                target.getType().getDescription()).withStyle(ChatFormatting.GOLD));
        if (target.level() instanceof ServerLevel level) {
            level.playSound(null, target.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL,
                    SoundSource.PLAYERS, 0.5f, 0.8f);
        }
        target.discard(); // el kill @s del datapack, sin soltar botin: el pacto se la lleva
        LOGGER.info("[{}] {} firmo el contrato de {}.",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName(), def.id());
        return FormOutcome.FORMED;
    }

    /** Rechazo por requisito: aviso y yunque, como el datapack. */
    private static void reject(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
        player.playNotifySound(SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.3f, 1f);
    }
}
