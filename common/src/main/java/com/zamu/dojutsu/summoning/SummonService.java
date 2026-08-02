package com.zamu.dojutsu.summoning;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.summoning.SummonContractDef;
import com.zamu.dojutsu.domain.summoning.SummonTable;
import com.zamu.dojutsu.entity.AbilityAnchor;
import com.zamu.dojutsu.progression.XpApi;
import com.zamu.dojutsu.registry.ModSounds;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Jutsu de Invocacion {@code [S15]} (F8/DJT-175): traer a la criatura de un contrato firmado.
 *
 * <h2>S15-R3 resuelto reutilizando el ancla de F4</h2>
 * La invocacion vive {@value SummonTable#LIFETIME_TICKS} ticks. El datapack lo consigue con <b>dos selectores
 * globales por tick</b> —uno que suma {@code d.summon_time} a cada invocacion viva y otro que mata a las
 * vencidas— y eso corre <b>siempre</b>, haya invocaciones o no.
 *
 * <p>Aqui cada invocacion nace con un {@link AbilityAnchor} pegado, que <b>ya tiene caducidad por construccion</b>
 * desde F4/DJT-071: la vida es parametro obligatorio y al expirar corre su callback. La criatura desaparece
 * porque su ancla venció, no porque alguien barra el mundo buscándola. Cero selectores, y con cero invocaciones
 * el coste es literalmente cero.
 *
 * <p>El ancla se escribio para las habilidades oculares de F5 y resulta que era exactamente la primitiva que
 * pedia el riesgo de este ticket, tres fases despues.
 *
 * <h2>Invocar no cuesta chakra: cuesta tiempo</h2>
 * No hay coste de chakra en {@code cast} (ver {@link SummonTable}). El veto baja de 600 a 200 ticks segun el
 * fuinjutsu: la maestria se paga a si misma.
 */
public final class SummonService {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Veto entre invocaciones por jugador ({@code d.summon_cd}); ejecucion, no guardado. */
    private static final Map<UUID, Integer> COOLDOWNS = new ConcurrentHashMap<>();

    private SummonService() {
    }

    /** Por que no se pudo invocar. */
    public enum SummonOutcome {
        /** Invocada. */
        SUMMONED,
        /** Aun corre el veto entre invocaciones. */
        COOLDOWN,
        /** Fuinjutsu por debajo del suelo del cast. */
        LOW_FUINJUTSU,
        /** El tier del contrato pide mas fuinjutsu o mas rango. */
        TIER_LOCKED,
        /** El tipo de entidad del contrato no existe (datapack ajeno roto). */
        UNKNOWN_ENTITY
    }

    /**
     * Invoca la criatura de un contrato.
     *
     * @return el resultado; solo {@link SummonOutcome#SUMMONED} crea algo
     */
    public static SummonOutcome summon(ServerPlayer player, SummonContractDef contract) {
        UUID id = player.getUUID();
        if (COOLDOWNS.getOrDefault(id, 0) > 0) {
            player.displayClientMessage(Component.translatable("summon.dojutsu.cooldown")
                    .withStyle(ChatFormatting.RED), true);
            return SummonOutcome.COOLDOWN;
        }
        PlayerNinjaData data = NinjaData.get(player);
        int fuinjutsu = data.progression().skill(Skill.FUINJUTSU).level();
        int rank = data.progression().rank().ordinal();
        if (!SummonTable.canCast(fuinjutsu)) {
            reject(player, "summon.dojutsu.low_fuinjutsu");
            return SummonOutcome.LOW_FUINJUTSU;
        }
        if (!contract.tier().canSummon(fuinjutsu, rank)) {
            reject(player, "summon.dojutsu.tier_locked");
            return SummonOutcome.TIER_LOCKED;
        }
        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE
                .getOptional(ResourceLocation.parse(contract.entityType()));
        if (type.isEmpty()) {
            LOGGER.error("[{}] el contrato {} declara un tipo de entidad desconocido: {}",
                    DojutsuMod.MOD_NAME, contract.id(), contract.entityType());
            return SummonOutcome.UNKNOWN_ENTITY;
        }
        ServerLevel level = (ServerLevel) player.level();
        Vec3 spot = player.position().add(player.getLookAngle().scale(2.0));
        Entity summoned = type.get().create(level);
        if (summoned == null) {
            return SummonOutcome.UNKNOWN_ENTITY;
        }
        summoned.moveTo(spot.x, spot.y, spot.z, player.getYRot(), 0F);
        outfit(summoned, player, contract);
        level.addFreshEntity(summoned);
        bindLifetime(level, summoned);

        COOLDOWNS.put(id, SummonTable.cooldownTicks(fuinjutsu));
        XpApi.addSkillXp(player, Skill.FUINJUTSU, SummonTable.SUMMON_FUINJUTSU_XP);
        effects(level, spot);
        player.displayClientMessage(Component.translatable("summon.dojutsu.summoned",
                summoned.getType().getDescription()).withStyle(ChatFormatting.GOLD), true);
        return SummonOutcome.SUMMONED;
    }

    /**
     * Pega la caducidad a la invocacion: un ancla que vive lo mismo que ella y, al expirar, la descarta.
     *
     * <p>Se descarta y no se mata: una invocacion que se desvanece no debe soltar botin ni contar como kill
     * (el datapack usa {@code kill @s}, que sí los dispara — divergencia consciente y a favor del jugador).
     */
    private static void bindLifetime(ServerLevel level, Entity summoned) {
        AbilityAnchor anchor = AbilityAnchor.create(level, summoned, SummonTable.LIFETIME_TICKS)
                .withTickCallback(a -> {
                    if (!summoned.isAlive()) {
                        a.discard(); // murio antes de expirar: el ancla no tiene nada que vigilar
                    }
                })
                .withExpireCallback(a -> {
                    if (summoned.isAlive() && summoned.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.POOF, summoned.getX(),
                                summoned.getY() + 0.5, summoned.getZ(), 20, 0.4, 0.4, 0.4, 0.05);
                        summoned.discard();
                    }
                });
        anchor.setPos(summoned.getX(), summoned.getY(), summoned.getZ());
        level.addFreshEntity(anchor);
    }

    /** Aplica lo que declare el contrato: domada, con silla, con cofres ({@code do_summon} + {@code tame}). */
    private static void outfit(Entity summoned, ServerPlayer owner, SummonContractDef contract) {
        if (contract.tame()) {
            if (summoned instanceof TamableAnimal tamable) {
                tamable.tame(owner);
            } else if (summoned instanceof AbstractHorse horse) {
                horse.setTamed(true);
                horse.setOwnerUUID(owner.getUUID());
            }
        }
        if (contract.saddle() && summoned instanceof AbstractHorse horse) {
            horse.equipSaddle(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE), null);
        }
        if (contract.chest() && summoned instanceof AbstractChestedHorse chested) {
            chested.setChest(true);
        }
        if (summoned instanceof Mob mob) {
            mob.setPersistenceRequired(); // no despawnea antes de que su ancla venza
        }
    }

    /**
     * Humo y sonido del circulo de invocacion ({@code do_summon}). El sonido es el del resource pack original
     * (F10/DJT-224), que sustituye al {@code ILLUSIONER_CAST_SPELL} provisional; el datapack lo lanza en
     * {@code master}, no en {@code player}, y con ese mismo volumen 0.4.
     */
    private static void effects(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.5, at.z, 60, 0.5, 0.5, 0.5, 0.05);
        level.playSound(null, at.x, at.y, at.z, ModSounds.JUTSU_SUMMONING.get(), SoundSource.MASTER, 0.4f, 1f);
    }

    /** Tickea los vetos. La llama el {@code ContractService}, que ya recorre a los jugadores. */
    static void tickCooldown(UUID playerId) {
        int value = COOLDOWNS.getOrDefault(playerId, 0);
        if (value > 0) {
            COOLDOWNS.put(playerId, value - 1);
        }
    }

    /** Ticks de veto que le quedan (comando de QA). */
    public static int cooldownOf(UUID playerId) {
        return COOLDOWNS.getOrDefault(playerId, 0);
    }

    /** Limpia el veto. Solo lo usa el comando de QA. */
    public static void clearCooldown(UUID playerId) {
        COOLDOWNS.remove(playerId);
    }

    /** Olvida el veto (al desconectar). */
    public static void forget(UUID playerId) {
        COOLDOWNS.remove(playerId);
    }

    /** Rechazo por requisito: aviso y yunque, como el datapack. */
    private static void reject(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
        player.playNotifySound(SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.3f, 1f);
    }
}
