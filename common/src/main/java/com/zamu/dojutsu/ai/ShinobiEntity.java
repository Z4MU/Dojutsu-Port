package com.zamu.dojutsu.ai;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.domain.ai.ThreatLevel;
import com.zamu.dojutsu.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

/**
 * Shinobi NPC {@code [S17]} (F8/DJT-185): el enemigo humano del mod, en seis niveles de amenaza.
 *
 * <h2>Entidad propia, no un zombi con nombre</h2>
 * El datapack invoca un {@code zombie} con {@code CustomName:"Undead Shinobi"} y le cuelga un <b>armor stand
 * flotante</b> encima para mostrar {@code Threat: Chūnin}. Aqui el nivel de amenaza es estado <b>sincronizado</b>
 * de la entidad y el titulo es su propio nombre: ni armor stand ni una entidad extra que se pueda quedar
 * huerfana — el datapack de hecho necesita un barrido por tick ({@code spawn/internal/main}) solo para
 * teletransportar la etiqueta y matarla si su shinobi desaparecio.
 *
 * <h2>No suelta nada</h2>
 * {@code DeathLootTable} vacio en las cuatro variantes: un shinobi reanimado no deja carne podrida. Portado.
 *
 * <p>La vida sale de {@link ThreatLevel}, que es tabla verificada; la <b>IA</b> es diseño nuevo (ver
 * {@link com.zamu.dojutsu.domain.ai.ShinobiCombatPlan}).
 */
public class ShinobiEntity extends Monster {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Nivel de amenaza (1..6), sincronizado: fija el nombre y el color que ve el cliente. */
    private static final EntityDataAccessor<Integer> DATA_THREAT =
            SynchedEntityData.defineId(ShinobiEntity.class, EntityDataSerializers.INT);

    private static final String TAG_THREAT = "ThreatLevel";

    public ShinobiEntity(EntityType<? extends ShinobiEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 0;
    }

    /**
     * Atributos base. La <b>vida</b> la fija el nivel de amenaza al invocarlo ({@link #setThreat}), porque va de
     * 120 a 700; aqui solo el minimo comun.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, ThreatLevel.ACADEMY.maxHealth())
                .add(Attributes.FOLLOW_RANGE, ThreatLevel.FOLLOW_RANGE)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ARMOR, 4.0);
    }

    /**
     * Invoca un shinobi de ese nivel de amenaza, con su vida y sin botin.
     *
     * @return el shinobi, o {@code null} si el nivel rechazo la entidad
     */
    public static ShinobiEntity summon(ServerLevel level, ThreatLevel threat, double x, double y, double z) {
        ShinobiEntity shinobi = ModEntities.SHINOBI.get().create(level);
        if (shinobi == null) {
            return null;
        }
        shinobi.setThreat(threat);
        shinobi.moveTo(x, y, z, level.random.nextFloat() * 360F, 0F);
        shinobi.setHealth(shinobi.getMaxHealth());
        level.addFreshEntity(shinobi);
        LOGGER.info("[{}] Shinobi de amenaza {} invocado en {} {} {}.",
                DojutsuMod.MOD_NAME, threat.id(), (int) x, (int) y, (int) z);
        return shinobi;
    }

    /** Nivel de amenaza; cae a {@link ThreatLevel#GENIN} si el NBT trae basura (el default del datapack). */
    public ThreatLevel threat() {
        return ThreatLevel.byDatapackId(this.entityData.get(DATA_THREAT)).orElse(ThreatLevel.GENIN);
    }

    /** Fija el nivel de amenaza y con el la vida maxima. */
    public void setThreat(ThreatLevel threat) {
        this.entityData.set(DATA_THREAT, threat.datapackId());
        var attribute = this.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) {
            attribute.setBaseValue(threat.maxHealth());
        }
        this.setCustomName(threatName(threat));
        this.setCustomNameVisible(true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ShinobiCombatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, (float) ThreatLevel.FOLLOW_RANGE));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_THREAT, ThreatLevel.GENIN.datapackId());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt(TAG_THREAT, this.entityData.get(DATA_THREAT));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains(TAG_THREAT)) {
            ThreatLevel.byDatapackId(compound.getInt(TAG_THREAT)).ifPresent(this::setThreat);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false; // PersistenceRequired: un shinobi invocado no se esfuma
    }

    /** Nombre con el titulo y su color ({@code assign_threat}), sin el armor stand flotante del datapack. */
    private static Component threatName(ThreatLevel threat) {
        return Component.translatable("entity.dojutsu.shinobi.threat",
                Component.translatable("entity.dojutsu.shinobi.rank." + threat.id())
                        .withStyle(style -> style.withColor(threat.titleColor())));
    }
}
