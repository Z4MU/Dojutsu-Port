package com.zamu.dojutsu.jutsu;

import com.zamu.dojutsu.core.DomainEvents;
import com.zamu.dojutsu.domain.event.JutsuCastEvent;
import dev.architectury.utils.GameInstance;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Registro del <b>último cast observable</b> (F4/DJT-079): base de la copia del Sharingan (F5).
 * Suscriptor de {@link JutsuCastEvent}; al lanzarse un jutsu guarda quién, qué, dónde y hasta
 * cuándo es "copiable". El Sharingan (F5) lo consulta por proximidad —{@link #recentCastsNear}—,
 * sin acoplarse al emisor (el pipeline no conoce el Sharingan).
 *
 * <p><b>Corrige S06-R6 (estado global):</b> el datapack marcaba a los observadores con
 * {@code d.sharingan_seen} sin caducidad — el jutsu "visto" se recordaba para siempre. Aquí cada
 * registro <b>expira</b> ({@link #DEFAULT_LIFETIME_TICKS}) y está <b>acotado por dimensión y
 * distancia</b>: nada global, nada eterno. Se poda con perezÍa al escribir y al consultar (sin
 * tarea por tick).
 *
 * <p><b>Un registro por caster</b> (su último cast): un cast nuevo del mismo caster reemplaza al
 * anterior, como el {@code d.sharingan_seen} que solo guardaba el último id. El caster puede ser
 * jugador o entidad IA (S17-R3): se resuelve por UUID en cualquier nivel del servidor.
 */
public final class LastCastRegistry {

    /** Ventana de copia por defecto: 200 ticks (10 s). F5 puede ajustar el radio al consultar. */
    public static final int DEFAULT_LIFETIME_TICKS = 200;

    /**
     * Un cast observable: qué técnica, quién, en qué dimensión, dónde y hasta qué tick de mundo
     * sigue siendo copiable. {@link Vec3}/{@link ResourceKey} no necesitan bootstrap de Minecraft,
     * así que la lógica de candidatura es <b>testeable sin arrancar MC</b>.
     */
    public record CastRecord(String jutsuId, UUID caster, ResourceKey<Level> dimension,
            Vec3 position, long expiresAtTick) {

        public CastRecord {
            Objects.requireNonNull(jutsuId, "jutsuId");
            Objects.requireNonNull(caster, "caster");
            Objects.requireNonNull(dimension, "dimension");
            Objects.requireNonNull(position, "position");
        }

        /** Indica si el registro sigue vivo al tick dado (no ha expirado). */
        public boolean isLiveAt(long nowTick) {
            return nowTick < expiresAtTick;
        }

        /**
         * Indica si este cast es candidato a copia por un observador: vivo, en la misma dimensión,
         * dentro del radio, y no es el propio observador (no te copias a ti mismo).
         */
        public boolean isCandidateFor(ResourceKey<Level> observerDim, Vec3 observerPos,
                double radius, long nowTick, UUID observer) {
            return isLiveAt(nowTick)
                    && dimension == observerDim
                    && !caster.equals(observer)
                    && position.distanceToSqr(observerPos) <= radius * radius;
        }
    }

    /** Último cast por caster (UUID → registro). Acotado por nº de casters y podado por expiración. */
    private static final Map<UUID, CastRecord> RECENT = new ConcurrentHashMap<>();

    private LastCastRegistry() {
    }

    /** Suscribe el registro al bus de dominio. Llamar una vez en el arranque. */
    public static void register() {
        DomainEvents.bus().subscribe(JutsuCastEvent.class, LastCastRegistry::onCast);
    }

    /** Vacía el registro (al parar el servidor / cambiar de mundo en cliente integrado). */
    public static void clear() {
        RECENT.clear();
    }

    private static void onCast(JutsuCastEvent event) {
        MinecraftServer server = GameInstance.getServer();
        if (server == null) {
            return;
        }
        Entity caster = resolveEntity(server, event.caster());
        if (caster == null) {
            return; // el caster ya no está (desconectó / murió): nada que registrar
        }
        long now = caster.level().getGameTime();
        pruneExpired(now);
        RECENT.put(event.caster(), new CastRecord(event.jutsuId(), event.caster(),
                caster.level().dimension(), caster.position(), now + DEFAULT_LIFETIME_TICKS));
    }

    /**
     * Casts recientes copiables por un observador: vivos, en su dimensión, dentro del radio y de
     * otros casters. Para el Sharingan de F5 (el datapack usaba radio 20). Poda de paso los vencidos.
     *
     * @param level    nivel del observador (aporta dimensión y tick de mundo)
     * @param center   posición del observador
     * @param radius   radio de visión (bloques)
     * @param observer UUID del observador (se excluye a sí mismo)
     */
    public static List<CastRecord> recentCastsNear(ServerLevel level, Vec3 center, double radius, UUID observer) {
        long now = level.getGameTime();
        pruneExpired(now);
        return RECENT.values().stream()
                .filter(rec -> rec.isCandidateFor(level.dimension(), center, radius, now, observer))
                .toList();
    }

    /** El último cast de un caster concreto, si sigue vivo (consulta directa, sin proximidad). */
    public static CastRecord lastCastOf(UUID caster, long nowTick) {
        CastRecord record = RECENT.get(caster);
        return record != null && record.isLiveAt(nowTick) ? record : null;
    }

    /** Elimina los registros vencidos (poda perezosa; sin barrido por tick). */
    private static void pruneExpired(long nowTick) {
        RECENT.values().removeIf(record -> !record.isLiveAt(nowTick));
    }

    /** Resuelve el caster por UUID: jugador conectado o entidad viva en cualquier nivel. */
    private static Entity resolveEntity(MinecraftServer server, UUID casterId) {
        Entity player = server.getPlayerList().getPlayer(casterId);
        if (player != null) {
            return player;
        }
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(casterId);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }
}
