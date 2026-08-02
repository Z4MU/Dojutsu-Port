package com.zamu.dojutsu.entity;

import com.zamu.dojutsu.core.identity.PlayerRef;
import com.zamu.dojutsu.registry.ModEntities;
import com.zamu.dojutsu.util.ProjectileMath;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Marcador generico transitorio (F4/DJT-071): una posicion con caducidad y dueno opcional,
 * sin logica propia — quien necesite comportamiento por tick usa un {@link AbilityAnchor}.
 * Reemplaza los armor stands "chincheta" del datapack (puntos de referencia de teleports,
 * areas y estructuras) que exigian barridos de limpieza.
 *
 * <p>Como el {@code Marker} vanilla, no viaja al cliente ({@code clientTrackingRange 0}):
 * es puro estado de servidor. La vida maxima es obligatoria en {@link #create} (sin
 * marcadores eternos por construccion) y {@link #shouldBeSaved()} es {@code false} (nunca
 * persiste con el chunk).
 */
public class MarkerEntity extends Entity {

    private int maxLifetimeTicks = 1; // create() lo fija siempre; > 0 garantizado
    private UUID ownerUuid;

    public MarkerEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /**
     * Crea un marcador en una posicion con vida maxima obligatoria, listo para anadir al
     * mundo con {@code level.addFreshEntity(...)}.
     *
     * @param lifetimeTicks vida maxima en ticks ({@code > 0}); al agotarse se descarta
     */
    public static MarkerEntity create(ServerLevel level, Vec3 pos, int lifetimeTicks) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        ProjectileMath.requireValidLifetime(lifetimeTicks);
        MarkerEntity marker = new MarkerEntity(ModEntities.MARKER.get(), level);
        marker.maxLifetimeTicks = lifetimeTicks;
        marker.setPos(pos.x, pos.y, pos.z);
        return marker;
    }

    /** Dueno opcional por UUID (nunca referencia dura). */
    public MarkerEntity withOwner(Entity owner) {
        this.ownerUuid = Objects.requireNonNull(owner, "owner").getUUID();
        return this;
    }

    /** UUID del dueno, si lo tiene. */
    public Optional<UUID> ownerUuid() {
        return Optional.ofNullable(this.ownerUuid);
    }

    /** El dueno como referencia de jugador, para resolverlo con seguridad si esta online. */
    public Optional<PlayerRef> ownerRef() {
        return ownerUuid().map(PlayerRef::of);
    }

    /** Vida maxima en ticks; la edad actual es {@code tickCount}. */
    public int maxLifetimeTicks() {
        return this.maxLifetimeTicks;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && ProjectileMath.isExpired(this.tickCount, this.maxLifetimeTicks)) {
            this.discard();
        }
    }

    /** Transitoria: nunca se persiste con el chunk. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // Sin datos sincronizados: el marcador no viaja al cliente (trackingRange 0).
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        // Sin estado persistente (shouldBeSaved = false).
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        // Sin estado persistente (shouldBeSaved = false).
    }
}
