package com.zamu.dojutsu.entity;

import com.mojang.logging.LogUtils;
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
import org.slf4j.Logger;

/**
 * Ancla posicional de una habilidad activa (F4/DJT-071). Base de las habilidades ancladas de
 * F5 (oculares) y F6 (sage): una entidad invisible que ejecuta la logica de su habilidad en
 * su propio tick y <b>expira garantizado</b>. Reemplaza los armor stands "de servicio" del
 * datapack que quedaban colgados si su funcion de limpieza no corria.
 *
 * <p>Contrato:
 * <ul>
 *   <li><b>Expiracion por construccion:</b> la vida maxima es parametro obligatorio de
 *       {@link #create} — no se puede crear un ancla sin caducidad (el riesgo de la ficha son
 *       las anclas colgadas). Al expirar corre el callback de limpieza (si lo hay) y se
 *       descarta. Un sistema que necesite un ancla "mientras el modo este activo" crea el
 *       ancla con la vida del modo o la descarta el mismo al desactivar.</li>
 *   <li><b>Tick propio</b> ({@link #withTickCallback}, solo servidor): la habilidad enchufa su
 *       logica sin heredar de la entidad. Un callback que lanza una excepcion <b>descarta el
 *       ancla</b> (aislamiento de fallos: mejor perder el ancla que spamear el log cada tick).</li>
 *   <li><b>Dueno por UUID</b> (DJT-020): nunca referencia dura. Si el dueno se desconecta, el
 *       ancla sigue su ciclo de vida y su callback decide (huerfana ≠ colgada: expira igual).</li>
 *   <li><b>Transitoria:</b> {@link #shouldBeSaved()} es {@code false}; recargar el chunk la
 *       elimina. Si un sistema futuro necesitara anclas persistentes, debera disenar su propia
 *       persistencia (anotado; hoy YAGNI).</li>
 * </ul>
 */
public class AbilityAnchor extends Entity {

    /** Logica por tick de la habilidad anclada (server-side). */
    @FunctionalInterface
    public interface AnchorTick {

        /** Invocado cada tick de servidor mientras el ancla vive. */
        void onTick(AbilityAnchor anchor);
    }

    /** Limpieza al expirar la vida maxima (server-side). No corre si el ancla se descarta a mano. */
    @FunctionalInterface
    public interface AnchorExpire {

        /** Invocado una vez, justo antes del descarte por expiracion. */
        void onExpire(AbilityAnchor anchor);
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    private int maxLifetimeTicks = 1; // create() lo fija siempre; > 0 garantizado
    private AnchorTick tickCallback;
    private AnchorExpire expireCallback;
    private UUID ownerUuid;

    public AbilityAnchor(EntityType<?> type, Level level) {
        super(type, level);
    }

    /**
     * Crea un ancla en la posicion del dueno con vida maxima obligatoria, lista para
     * configurar (withers) y anadir al mundo con {@code level.addFreshEntity(...)}.
     *
     * @param lifetimeTicks vida maxima en ticks ({@code > 0}); al agotarse expira y se descarta
     */
    public static AbilityAnchor create(ServerLevel level, Entity owner, int lifetimeTicks) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(owner, "owner");
        ProjectileMath.requireValidLifetime(lifetimeTicks);
        AbilityAnchor anchor = new AbilityAnchor(ModEntities.ABILITY_ANCHOR.get(), level);
        anchor.ownerUuid = owner.getUUID();
        anchor.maxLifetimeTicks = lifetimeTicks;
        anchor.setPos(owner.getX(), owner.getY(), owner.getZ());
        return anchor;
    }

    // ------------------------------------------------------------------
    // Configuracion fluida (server-side, antes de spawnear)
    // ------------------------------------------------------------------

    /** Ancla en otra posicion (p. ej. habilidad sobre un punto del suelo, no sobre el dueno). */
    public AbilityAnchor at(Vec3 pos) {
        Objects.requireNonNull(pos, "pos");
        this.setPos(pos.x, pos.y, pos.z);
        return this;
    }

    /** Logica por tick de la habilidad (server-side). */
    public AbilityAnchor withTickCallback(AnchorTick callback) {
        this.tickCallback = Objects.requireNonNull(callback, "callback");
        return this;
    }

    /** Limpieza al expirar (server-side). */
    public AbilityAnchor withExpireCallback(AnchorExpire callback) {
        this.expireCallback = Objects.requireNonNull(callback, "callback");
        return this;
    }

    // ------------------------------------------------------------------
    // Dueno por UUID
    // ------------------------------------------------------------------

    /** UUID del dueno (jugador o entidad), disponible aunque este offline/descargado. */
    public Optional<UUID> ownerUuid() {
        return Optional.ofNullable(this.ownerUuid);
    }

    /** El dueno como referencia de jugador, para resolverlo con seguridad si esta online. */
    public Optional<PlayerRef> ownerRef() {
        return ownerUuid().map(PlayerRef::of);
    }

    /** Vida maxima en ticks; la edad actual es {@code tickCount} (para tiempo restante). */
    public int maxLifetimeTicks() {
        return this.maxLifetimeTicks;
    }

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || this.isRemoved()) {
            return;
        }

        if (ProjectileMath.isExpired(this.tickCount, this.maxLifetimeTicks)) {
            if (this.expireCallback != null) {
                try {
                    this.expireCallback.onExpire(this);
                } catch (RuntimeException e) {
                    LOGGER.error("[Dojutsu] Fallo el callback de expiracion del ancla {}", this.getId(), e);
                }
            }
            this.discard();
            return;
        }

        if (this.tickCallback != null) {
            try {
                this.tickCallback.onTick(this);
            } catch (RuntimeException e) {
                // Aislamiento de fallos: un callback roto correria (y fallaria) cada tick.
                LOGGER.error("[Dojutsu] Fallo el tick del ancla {}; se descarta", this.getId(), e);
                this.discard();
            }
        }
    }

    /** Transitoria: nunca se persiste con el chunk. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // Sin datos sincronizados: el ancla es logica de servidor (renderer Noop).
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
