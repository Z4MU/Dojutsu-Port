package com.zamu.dojutsu.bijuu.entity;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.bijuu.ai.TailedBeastMoveGoal;
import com.zamu.dojutsu.domain.bijuu.BeastStats;
import com.zamu.dojutsu.domain.bijuu.TailedBeast;
import com.zamu.dojutsu.progression.XpApi;
import com.zamu.dojutsu.registry.ModEntities;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
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
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

/**
 * Bestia con Cola {@code [S09]} (F7/DJT-160): entidad jefe propia del mod.
 *
 * <h2>Entidad propia, no un ravager disfrazado</h2>
 * El datapack invoca un {@code ravager} con tags ({@code d.tailed_beast} + {@code d.tb_N}) y le cuelga un armor
 * stand con un item de modelo — el propio autor lo comenta en {@code tailed_beasts/main}: <i>"esto seguramente
 * habria que rehacerlo, pero como no hay modelos, ni idea"</i>. Aqui es una entidad registrada con su tipo, su
 * identidad sincronizada y su ciclo de vida; el <b>arte</b> sigue siendo deuda (F10), la <b>estructura</b> ya no.
 *
 * <h2>La barra de jefe es nativa</h2>
 * Esto salda el riesgo <b>S09-R6</b>. El datapack mantiene diez {@code bossbar} manuales y cada tick lee el
 * {@code Health} por NBT de cada bestia viva para copiarlo a su barra, mas otra pasada por jugador para
 * añadirlos y quitarlos del rango. Aqui es un {@link ServerBossEvent}: se actualiza solo con la vida y la
 * visibilidad se resuelve por proximidad una vez por tick sobre la lista de jugadores ya cargada. Cero NBT, cero
 * barras huerfanas si la bestia muere de forma rara.
 *
 * <h2>Matarla reparte</h2>
 * {@code tailed_beasts/death} no premia al que da el golpe final: reparte ninja_xp y experiencia a <b>todos</b>
 * los jugadores a {@value BeastStats#REWARD_RADIUS} bloques. Se porta igual. El <b>item de chakra</b> que suelta
 * (y que al usarse sella la bestia) llega con {@code SealingService} en DJT-165.
 */
public class TailedBeastEntity extends Monster {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Numero de colas, sincronizado al cliente: es lo que fija identidad, nombre y color de la barra. */
    private static final EntityDataAccessor<Integer> DATA_TAILS =
            SynchedEntityData.defineId(TailedBeastEntity.class, EntityDataSerializers.INT);

    /** Clave NBT de la identidad; sin ella una bestia recargada seria un Shukaku por defecto. */
    private static final String TAG_TAILS = "Tails";

    /** Altura de los ojos como fraccion del alto: lo bastante arriba para que mire por encima de su cuerpo. */
    private static final float EYE_HEIGHT_RATIO = 0.8F;

    private final ServerBossEvent bossEvent =
            new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);

    public TailedBeastEntity(EntityType<? extends TailedBeastEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired(); // una bestia jefe no despawnea por distancia
        this.xpReward = 0; // la recompensa se reparte a mano en death (no la suelta el golpe final)
    }

    /**
     * Goals de combate (F7/DJT-161). El datapack deja al ravager su IA vanilla —persigue y golpea— y le añade
     * <b>encima</b> el bloque de tecnicas; aqui es lo mismo, expresado como goals: nadar, el goal de tecnicas
     * (que no bloquea el movimiento, la bestia dispara andando), perseguir a melee, deambular y mirar.
     *
     * <p>El goal de tecnicas va con <b>prioridad 1</b>, por encima del cuerpo a cuerpo: cuando toca turno de
     * tecnica, se lanza aunque este persiguiendo.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TailedBeastMoveGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, (float) BeastStats.FOLLOW_RANGE));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * Indica si el Tenpenchii del Diez Colas <b>no</b> esta en cuenta atras ({@code d.tt_tentimer}). Para las
     * otras nueve siempre es cierto: no tienen esa tecnica.
     *
     * <p>Hoy siempre devuelve {@code true} porque el Tenpenchii es contenido del Diez Colas y llega en <b>F9</b>
     * con el resto de su nodo (S09-R7). La <b>decision</b> ya esta modelada en {@code BeastAiPlan} y testeada;
     * lo unico que falta es que algo ponga este temporizador. Una linea a cambiar.
     */
    public boolean tenpenchiiReady() {
        return true;
    }

    /** Atributos base de cualquier bestia; las diez comparten cuerpo ({@code spawn_tb_entity}). */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, BeastStats.MAX_HEALTH)
                .add(Attributes.ARMOR, BeastStats.ARMOR)
                .add(Attributes.FOLLOW_RANGE, BeastStats.FOLLOW_RANGE)
                .add(Attributes.MOVEMENT_SPEED, BeastStats.MOVEMENT_SPEED)
                .add(Attributes.ATTACK_DAMAGE, BeastStats.ATTACK_DAMAGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, BeastStats.KNOCKBACK_RESISTANCE);
    }

    /**
     * Invoca una bestia ya identificada y con la vida al maximo.
     *
     * @return la bestia invocada, o {@code null} si el nivel rechazo la entidad
     */
    public static TailedBeastEntity summon(ServerLevel level, TailedBeast beast, double x, double y, double z) {
        TailedBeastEntity entity = ModEntities.TAILED_BEAST.get().create(level);
        if (entity == null) {
            return null;
        }
        entity.setBeast(beast);
        entity.moveTo(x, y, z, level.random.nextFloat() * 360F, 0F);
        entity.setHealth(entity.getMaxHealth());
        level.addFreshEntity(entity);
        LOGGER.info("[{}] Invocada la bestia {} en {} {} {}.",
                DojutsuMod.MOD_NAME, beast.id(), (int) x, (int) y, (int) z);
        return entity;
    }

    /** Bestia que es esta entidad; cae a {@link TailedBeast#ONE_TAIL} si el NBT trae basura. */
    public TailedBeast beast() {
        return TailedBeast.byTails(this.entityData.get(DATA_TAILS)).orElse(TailedBeast.ONE_TAIL);
    }

    /** Fija la identidad, refresca el nombre de la barra de jefe y <b>ajusta la caja</b> al tamaño de la bestia. */
    public void setBeast(TailedBeast beast) {
        this.entityData.set(DATA_TAILS, beast.tails());
        this.bossEvent.setName(beastName(beast));
        refreshDimensions(); // la caja crece con la bestia (ver getDimensions)
    }

    /**
     * Caja de colision <b>a la escala de la bestia</b> (F10, arreglo de la verificacion en partida).
     *
     * <p>El {@code sized(...)} del {@code EntityType} es un tamaño fijo para las diez, pero el renderer dibuja
     * cada bestia mas grande segun sus colas. El resultado era que un Nueve Colas se veia enorme y arrastraba una
     * caja de dos bloques pegada a los pies: los golpes atravesaban el cuerpo sin tocar nada y la bestia
     * <b>parecia inmune</b>.
     *
     * <p>Ahora la caja sale de {@link BeastStats#renderScale}, <b>la misma funcion</b> que usa el renderer, asi
     * que lo que se ve y lo que se puede golpear no pueden volver a separarse.
     */
    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        TailedBeast beast = beast();
        return EntityDimensions.scalable(BeastStats.width(beast), BeastStats.height(beast))
                .withEyeHeight(BeastStats.height(beast) * EYE_HEIGHT_RATIO);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TAILS, TailedBeast.ONE_TAIL.tails());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt(TAG_TAILS, this.entityData.get(DATA_TAILS));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains(TAG_TAILS)) {
            TailedBeast.byTails(compound.getInt(TAG_TAILS)).ifPresent(this::setBeast);
        }
    }

    @Override
    public boolean fireImmune() {
        return true; // spawn_tb_entity: fire_resistance permanente
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false; // PersistenceRequired del datapack
    }

    @Override
    public Component getDisplayName() {
        return beastName(beast());
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel serverLevel) {
            updateBossBar(serverLevel);
        }
    }

    /**
     * Mantiene la barra: progreso por vida y espectadores por proximidad. Sustituye las dos pasadas por tick del
     * datapack ({@code store result bossbar ... run data get entity @s Health} y {@code add_me_to_tb_bb}).
     */
    private void updateBossBar(ServerLevel level) {
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        double radiusSq = BeastStats.BOSS_BAR_RADIUS * BeastStats.BOSS_BAR_RADIUS;
        for (ServerPlayer player : level.players()) {
            boolean near = player.distanceToSqr(this) <= radiusSq;
            if (near) {
                this.bossEvent.addPlayer(player);
            } else {
                this.bossEvent.removePlayer(player);
            }
        }
    }

    @Override
    public void die(DamageSource cause) {
        if (this.level() instanceof ServerLevel serverLevel) {
            rewardNearbyPlayers(serverLevel);
        }
        this.bossEvent.removeAllPlayers(); // la barra no sobrevive a su bestia (el datapack lo hacia a mano)
        super.die(cause);
    }

    @Override
    public void remove(RemovalReason reason) {
        this.bossEvent.removeAllPlayers(); // tambien si desaparece sin morir (comando, chunk, /kill)
        super.remove(reason);
    }

    /** {@code death}: reparte ninja_xp y experiencia a todos los jugadores cercanos, no solo al que remata. */
    private void rewardNearbyPlayers(ServerLevel level) {
        TailedBeast beast = beast();
        int ninjaXp = BeastStats.ninjaXpReward(beast);
        int experience = BeastStats.experienceReward(beast);
        List<ServerPlayer> nearby = level.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(BeastStats.REWARD_RADIUS));
        for (ServerPlayer player : nearby) {
            XpApi.addNinjaXp(player, ninjaXp);
            player.giveExperiencePoints(experience);
        }
        if (!nearby.isEmpty()) {
            LOGGER.info("[{}] {} derrotada: {} ninja_xp y {} de experiencia a {} jugador(es).",
                    DojutsuMod.MOD_NAME, beast.id(), ninjaXp, experience, nearby.size());
        }
    }

    /** Nombre traducible de la bestia ({@code entity.dojutsu.tailed_beast.nine_tails}). */
    private static Component beastName(TailedBeast beast) {
        return Component.translatable("entity.dojutsu.tailed_beast." + beast.id());
    }
}
