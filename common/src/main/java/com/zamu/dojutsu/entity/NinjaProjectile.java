package com.zamu.dojutsu.entity;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.core.identity.PlayerRef;
import com.zamu.dojutsu.registry.ModEntities;
import com.zamu.dojutsu.util.ProjectileMath;
import com.zamu.dojutsu.util.RaycastHelper;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Proyectil ninja reutilizable (F4/DJT-070). Base compartida por los sistemas de tecnica
 * (jutsu F4, dojutsu F5, sage F6, throwing/bijuu F7+): reemplaza el patron de armor stands
 * teletransportados del datapack (S06-R5/S16-R1) por una entidad real con colision de verdad.
 *
 * <p>Contrato:
 * <ul>
 *   <li><b>Fisica configurable:</b> velocidad inicial ({@link #launch}), gravedad opcional y
 *       vida maxima ({@link #withGravity}, {@link #withLifetime}); la fisica pura vive en
 *       {@link ProjectileMath} (testeable sin Minecraft). Sin friccion: el proyectil mantiene
 *       su velocidad, fiel a los armor stands de velocidad constante del datapack (por eso
 *       {@link #tick()} implementa la fisica propia en vez de heredar la del throwable
 *       vanilla, que frena un 1% por tick y un 20% en agua).</li>
 *   <li><b>Impacto por raycast de segmento</b> ({@link RaycastHelper}): sin tunneling a alta
 *       velocidad; con dos entidades en la trayectoria gana la mas cercana; puede impactar en
 *       el mismo tick del spawn. Deteccion <b>solo en servidor</b>; el cliente solo interpola.</li>
 *   <li><b>Dueno por UUID</b> (DJT-020): nunca una referencia dura; el dueno puede ser un
 *       jugador o cualquier entidad (caster generico, S17-R3). Si el dueno se desconecta, el
 *       proyectil sigue su vuelo y el callback decide que hacer con un dueno irresoluble.
 *       El dueno nunca es golpeado por su propio proyectil (igual que en el datapack).</li>
 *   <li><b>Callback de impacto</b> ({@link #withImpactCallback}): las familias de efecto de F4
 *       (DJT-076) enchufan aqui su logica; el punto exacto es {@code hit.getLocation()}. Tras
 *       el callback el proyectil se descarta (un impacto, un efecto).</li>
 *   <li><b>Transitoria, no persistente:</b> {@link #shouldBeSaved()} es {@code false} — nunca
 *       se guarda con el chunk (recargar el mundo la elimina) y expira sola por vida maxima o
 *       al caer al vacio.</li>
 * </ul>
 *
 * <p>Base {@link ThrowableItemProjectile}: es la clase vanilla con constructor publico (el de
 * {@code Projectile} es package-private en el jar comun; NeoForge lo ensancha por AT, pero
 * {@code common} compila contra vanilla) y aporta el item de display sincronizado + el
 * contrato {@code ItemSupplier} del {@code ThrownItemRenderer}. {@link #withDisplayItem}
 * cambia el aspecto por proyectil (default: bola de nieve); los assets reales del resource
 * pack se portan por fase (ver handoff F3).
 *
 * <p>Limitacion documentada: al no ejecutar {@code Projectile.tick()} (sus flags
 * {@code hasBeenShot}/{@code leftOwner} son privados), no se emite el game event
 * {@code PROJECTILE_SHOOT} (sensores sculk). Se revisara si algun sistema lo necesita.
 */
public class NinjaProjectile extends ThrowableItemProjectile {

    /** Reaccion al impacto, enchufable por el sistema que dispara (server-side). */
    @FunctionalInterface
    public interface ImpactCallback {

        /**
         * Invocado en el servidor al primer impacto (bloque o entidad), antes del descarte.
         *
         * @param projectile el proyectil que impacto (posicion, dueno, nivel)
         * @param hit        resultado del raycast; el punto exacto es {@code hit.getLocation()}
         */
        void onImpact(NinjaProjectile projectile, HitResult hit);
    }

    /**
     * Logica por tick de vuelo (server-side), enchufable — p. ej. el aura de fuego del gran
     * fireball, que incendiaba lo cercano DURANTE el vuelo (F4/DJT-076). Mismo patron y misma
     * politica de aislamiento que el tick de {@code AbilityAnchor}: si lanza excepcion, el
     * proyectil se descarta (fallaria cada tick).
     */
    @FunctionalInterface
    public interface FlightTick {

        /** Invocado cada tick de servidor mientras el proyectil vuela (antes de moverse). */
        void onTick(NinjaProjectile projectile);
    }

    /** Vida maxima por defecto: 100 ticks (5 s), sobrescribible con {@link #withLifetime}. */
    public static final int DEFAULT_LIFETIME_TICKS = 100;

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Gravedad sincronizada: el cliente interpola con la misma fisica que el servidor, asi
     * que la gravedad debe viajar con la entidad (si fuese un campo solo-servidor, las
     * trayectorias curvas darian saltos visuales en cada sync).
     */
    private static final EntityDataAccessor<Float> DATA_GRAVITY =
            SynchedEntityData.defineId(NinjaProjectile.class, EntityDataSerializers.FLOAT);

    private int maxLifetimeTicks = DEFAULT_LIFETIME_TICKS;
    private ImpactCallback impactCallback;
    private FlightTick flightTick;
    private UUID ownerUuid;

    public NinjaProjectile(EntityType<? extends NinjaProjectile> type, Level level) {
        super(type, level);
    }

    /**
     * Crea un proyectil en la posicion de los ojos del dueno, listo para configurar
     * (withers) y lanzar ({@link #launch}). No lo anade al mundo: el llamador hace
     * {@code level.addFreshEntity(...)} tras configurarlo.
     */
    public static NinjaProjectile create(ServerLevel level, Entity owner) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(owner, "owner");
        NinjaProjectile projectile = new NinjaProjectile(ModEntities.NINJA_PROJECTILE.get(), level);
        projectile.setOwner(owner);
        Vec3 eye = owner.getEyePosition();
        projectile.setPos(eye.x, eye.y - 0.1D, eye.z);
        return projectile;
    }

    // ------------------------------------------------------------------
    // Configuracion fluida (server-side, antes de spawnear)
    // ------------------------------------------------------------------

    /** Gravedad como resta por tick sobre Y ({@code >= 0}; 0 = vuelo rectilineo, el default). */
    public NinjaProjectile withGravity(double gravityPerTick) {
        ProjectileMath.requireValidGravity(gravityPerTick);
        this.entityData.set(DATA_GRAVITY, (float) gravityPerTick);
        return this;
    }

    /** Vida maxima en ticks ({@code > 0}); al agotarse el proyectil expira sin impactar. */
    public NinjaProjectile withLifetime(int maxLifetimeTicks) {
        ProjectileMath.requireValidLifetime(maxLifetimeTicks);
        this.maxLifetimeTicks = maxLifetimeTicks;
        return this;
    }

    /** Reaccion al impacto (server-side). Sin callback, el impacto solo descarta el proyectil. */
    public NinjaProjectile withImpactCallback(ImpactCallback callback) {
        this.impactCallback = Objects.requireNonNull(callback, "callback");
        return this;
    }

    /** Logica por tick de vuelo (server-side), p. ej. un aura alrededor del proyectil. */
    public NinjaProjectile withFlightTick(FlightTick callback) {
        this.flightTick = Objects.requireNonNull(callback, "callback");
        return this;
    }

    /** Item con el que se renderiza el proyectil (default: bola de nieve). */
    public NinjaProjectile withDisplayItem(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("el item de display no puede estar vacio");
        }
        this.setItem(stack.copyWithCount(1));
        return this;
    }

    /** Lanza el proyectil en una direccion con la velocidad dada (bloques/tick), sin dispersion. */
    public void launch(Vec3 direction, float speed) {
        Objects.requireNonNull(direction, "direction");
        this.shoot(direction.x, direction.y, direction.z, speed, 0.0F);
    }

    // ------------------------------------------------------------------
    // Dueno por UUID
    // ------------------------------------------------------------------

    @Override
    public void setOwner(Entity owner) {
        super.setOwner(owner);
        this.ownerUuid = owner != null ? owner.getUUID() : null;
    }

    /** UUID del dueno (jugador o entidad), disponible aunque este offline/descargado. */
    public Optional<UUID> ownerUuid() {
        return Optional.ofNullable(this.ownerUuid);
    }

    /** El dueno como referencia de jugador, para resolverlo con seguridad si esta online. */
    public Optional<PlayerRef> ownerRef() {
        return ownerUuid().map(PlayerRef::of);
    }

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------

    /**
     * Fisica propia (sin friccion) + raycast + expiracion. Reemplaza por completo el tick del
     * throwable vanilla: {@code baseTick} conserva el mantenimiento del {@code Entity}
     * (vacio, fuego, portales); la deteccion de impacto y la expiracion corren solo en
     * servidor; el avance corre en ambos lados para que el cliente interpole suave.
     */
    @Override
    public void tick() {
        this.baseTick();
        if (this.isRemoved()) {
            return;
        }

        // Deteccion y expiracion: solo con autoridad de servidor.
        if (!this.level().isClientSide) {
            if (ProjectileMath.isExpired(this.tickCount, this.maxLifetimeTicks)) {
                this.discard();
                return;
            }
            if (this.flightTick != null) {
                try {
                    this.flightTick.onTick(this);
                } catch (RuntimeException e) {
                    // Aislamiento de fallos (politica de EP-ENTITY): un aura rota no debe fallar cada tick.
                    LOGGER.error("[Dojutsu] Fallo el tick de vuelo del proyectil {}; se descarta", this.getId(), e);
                    this.discard();
                    return;
                }
                if (this.isRemoved()) {
                    return; // el aura puede decidir descartar
                }
            }
            HitResult hit = RaycastHelper.firstHitAlongMotion(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                this.onHit(hit);
                return; // onHit descarta; no avanzar tras impactar
            }
        }

        // Avance + gravedad en ambos lados: el cliente interpola entre syncs con la misma fisica.
        Vec3 next = ProjectileMath.advance(this.position(), this.getDeltaMovement());
        this.setPos(next.x, next.y, next.z);
        this.setDeltaMovement(ProjectileMath.applyGravity(
                this.getDeltaMovement(), this.entityData.get(DATA_GRAVITY)));
        // Rumbo y cabeceo siguen a la velocidad (F11/DJT-240). Este tick sustituye entero al de vanilla y se
        // habia quedado sin esta linea, asi que los angulos solo se fijaban una vez, al lanzar. Sin ella, un
        // modelo 3D orientado por NinjaProjectileRenderer llevaria la nariz rigida mientras cae en arco.
        this.updateRotation();
        this.checkInsideBlocks();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result); // despacha onHitEntity/onHitBlock (interacciones vanilla del bloque)
        if (this.impactCallback != null) {
            try {
                this.impactCallback.onImpact(this, result);
            } catch (RuntimeException e) {
                // Aislamiento de fallos (mismo principio que DomainEventBus): un callback roto
                // no debe impedir el descarte, o el proyectil re-impactaria cada tick.
                LOGGER.error("[Dojutsu] Fallo el callback de impacto del proyectil {}", this.getId(), e);
            }
        }
        this.discard();
    }

    /** Transitoria: nunca se persiste con el chunk (DoD de la ficha: desaparece al recargar). */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    // ------------------------------------------------------------------
    // Sync / render
    // ------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); // el item de display del throwable vanilla
        builder.define(DATA_GRAVITY, 0.0F);
    }

    /** Item mostrado por el {@code ThrownItemRenderer} si nadie configura otro. */
    @Override
    protected Item getDefaultItem() {
        return Items.SNOWBALL;
    }
}
