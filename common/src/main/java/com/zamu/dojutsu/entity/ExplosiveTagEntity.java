package com.zamu.dojutsu.entity;

import com.zamu.dojutsu.domain.items.ExplosiveTagTable;
import com.zamu.dojutsu.registry.ModEntities;
import com.zamu.dojutsu.registry.ModItems;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * <b>Sello explosivo colocado</b> ({@code [S14]}, F10/DJT-225): la mina que el datapack representa con un
 * {@code armor_stand} invisible con el papel en la cabeza ({@code items/explosive_tag/place}).
 *
 * <h2>Por que es una entidad propia y no un {@link AbilityAnchor}</h2>
 * El ancla de habilidad encaja en todo salvo en lo que aqui importa: {@link AbilityAnchor#shouldBeSaved()}
 * devuelve {@code false}. Un sello colocado es una <b>trampa</b>: se deja puesta y se vuelve luego, quiza tras
 * reiniciar el servidor. Con un ancla, todas las minas del mundo desaparecerian al descargar el chunk, y nadie
 * lo notaria hasta pisar donde ya no hay nada. El {@code armor_stand} del datapack persiste; esta tambien.
 *
 * <p>Lo demas del ancla si se hereda en espiritu: tick propio, aislamiento y un solo camino de retirada.
 *
 * <h2>Ciclo</h2>
 * Se coloca desarmada con {@value ExplosiveTagTable#ARM_TICKS} ticks de cuenta atras. Mientras se arma no la
 * dispara nadie. Al llegar a 0 queda cebada, y detona en cuanto entra algo que no sea decorado en
 * {@value ExplosiveTagTable#TRIGGER_RADIUS} bloques.
 */
public class ExplosiveTagEntity extends Entity implements ItemSupplier {

    /**
     * Ticks que le quedan para armarse. <b>Sincronizado</b> porque el cliente puede querer pintarla distinta
     * cuando este cebada (F10/DJT-224); hoy nadie lo lee en cliente, pero el dato es barato y el renderer no
     * tendria de donde sacarlo.
     */
    private static final EntityDataAccessor<Integer> DATA_ARMING =
            SynchedEntityData.defineId(ExplosiveTagEntity.class, EntityDataSerializers.INT);

    public ExplosiveTagEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true; // NoGravity:1b — se queda donde se coloco
    }

    /**
     * Coloca un sello en el punto indicado, desarmado.
     *
     * @return la entidad ya añadida al mundo, o {@code null} si el mundo la rechazo
     */
    public static ExplosiveTagEntity place(ServerLevel level, Vec3 pos) {
        ExplosiveTagEntity tag = ModEntities.EXPLOSIVE_TAG.get().create(level);
        if (tag == null) {
            return null;
        }
        tag.setPos(pos.x, pos.y, pos.z);
        tag.setArmingTicksLeft(ExplosiveTagTable.ARM_TICKS);
        level.addFreshEntity(tag);
        return tag;
    }

    /** Ticks que le quedan para quedar cebada; 0 = armada. */
    public int armingTicksLeft() {
        return this.entityData.get(DATA_ARMING);
    }

    private void setArmingTicksLeft(int ticks) {
        this.entityData.set(DATA_ARMING, Math.max(ticks, 0));
    }

    /** Indica si ya esta cebada ({@code d.primed}). */
    public boolean isArmed() {
        return ExplosiveTagTable.isArmed(armingTicksLeft());
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level) || this.isRemoved()) {
            return;
        }
        if (!isArmed()) {
            setArmingTicksLeft(armingTicksLeft() - 1);
            return;
        }
        if (findTrigger() != null) {
            detonate(level);
        }
    }

    /**
     * Primera entidad en el radio que dispara el sello. El selector del datapack excluye los demas sellos, los
     * cuadros, los marcos, los soportes de armadura y los items sueltos: todo lo que <b>no se mueve por su
     * cuenta</b>.
     */
    private Entity findTrigger() {
        AABB box = this.getBoundingBox().inflate(ExplosiveTagTable.TRIGGER_RADIUS);
        List<Entity> nearby = this.level().getEntities(this, box, ExplosiveTagEntity::triggersTag);
        return nearby.isEmpty() ? null : nearby.get(0);
    }

    /** {@code tag=!d.explosive_tag,type=!painting,type=!item_frame,type=!armor_stand,type=!item}. */
    private static boolean triggersTag(Entity entity) {
        return entity.isAlive()
                && !(entity instanceof ExplosiveTagEntity)
                && !(entity instanceof ItemEntity)
                && !(entity instanceof ArmorStand)
                && !(entity instanceof HangingEntity); // cuadros y marcos comparten padre
    }

    /**
     * Explosion + golpe magico. Son <b>dos</b> daños distintos: la potencia del {@code tnt_minecart} y un
     * {@code instant_damage} que la armadura no para y que la distancia no atenua.
     */
    private void detonate(ServerLevel level) {
        for (Entity entity : level.getEntities(this,
                this.getBoundingBox().inflate(ExplosiveTagTable.TRIGGER_RADIUS),
                e -> e instanceof LivingEntity && e.isAlive())) {
            ((LivingEntity) entity).addEffect(new MobEffectInstance(MobEffects.HARM,
                    ExplosiveTagTable.HARM_DURATION_TICKS, ExplosiveTagTable.HARM_AMPLIFIER));
        }
        level.explode(this, this.getX(), this.getY(), this.getZ(),
                ExplosiveTagTable.EXPLOSION_POWER, Level.ExplosionInteraction.TNT);
        this.discard();
    }

    // ------------------------------------------------------------------
    // Contrato de Entity
    // ------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ARMING, ExplosiveTagTable.ARM_TICKS);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setArmingTicksLeft(tag.getInt("ArmingTicks"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("ArmingTicks", armingTicksLeft());
    }

    /** Persistente: es una trampa colocada, no un efecto de una habilidad. */
    @Override
    public boolean shouldBeSaved() {
        return true;
    }

    /** Invulnerable, como el {@code armor_stand} del datapack: solo se retira detonando. */
    @Override
    public boolean isPickable() {
        return false;
    }

    /**
     * El item que dibuja el renderer: el papel del sello, igual que el {@code ArmorItems} del
     * {@code armor_stand} del datapack. Contrato de {@code ThrownItemRenderer}.
     */
    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.EXPLOSIVE_TAG.get());
    }
}
