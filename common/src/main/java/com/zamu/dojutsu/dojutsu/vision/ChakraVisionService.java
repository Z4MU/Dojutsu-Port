package com.zamu.dojutsu.dojutsu.vision;

import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.core.PlayerTickSystem;
import com.zamu.dojutsu.core.TickPhase;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.EyeState;
import com.zamu.dojutsu.domain.vision.ChakraVisionRelations;
import com.zamu.dojutsu.domain.vision.EntityRelationship;
import com.zamu.dojutsu.dojutsu.EyeDefinition;
import com.zamu.dojutsu.dojutsu.EyeRegistry;
import com.zamu.dojutsu.network.ToggleVisionPayload;
import com.zamu.dojutsu.platform.NetworkService;
import com.zamu.dojutsu.platform.PlatformServices;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Servicio de la chakra vision {@code [S25]} (DJT-125): resalta las entidades cercanas con el <b>glow nativo</b>
 * de Minecraft, coloreado por su relacion con el observador. Es un {@link PlayerTickSystem} en la fase
 * {@link TickPhase#DOJUTSU}.
 *
 * <p><b>Glow nativo + equipos, no particulas por tick ni renderer propio (RB-3):</b> el datapack corria
 * {@code execute at @e[type=#tag] run particle} cada tick y por categoria. Aqui, cada cierto intervalo (no cada
 * tick), un jugador con vision <b>marca</b> las entidades de su entorno con el efecto {@link MobEffects#GLOWING}
 * y las mete en un equipo de scoreboard cuyo color fija el del contorno. El contorno lo dibuja el cliente por
 * su cuenta (vanilla), sin selector por tick ni codigo de render del mod. La clasificacion es <b>estatica</b>
 * ({@link ChakraVisionRelations}, de los cuatro {@code chakra_highlights/*} del datapack).
 *
 * <p><b>Encendida por defecto, se apaga a voluntad</b> (fiel a {@code d.no_cv}): quien lleva un ojo que da vision
 * la tiene activa; la tecla la silencia ({@link #toggle}). Un ojo que no la da (Ketsuryugan; Sharingan base) no
 * la enciende — {@link EyeDefinition#grantsChakraVision(int)}.
 *
 * <p><b>El glow es global</b> (a diferencia del {@code particle ... force @s} del datapack, que solo lo veia el
 * observador): con el contorno nativo, cualquiera ve resaltada a la entidad marcada. Es el precio de no usar un
 * selector por tick; se asume como parte de RB-3.
 */
public final class ChakraVisionService implements PlayerTickSystem {

    /** Radio de deteccion ({@code distance=0..30} del datapack). */
    static final double RADIUS = 30.0;

    /** Cada cuantos ticks un jugador con vision reescanea su entorno (no cada tick: RB-3). */
    static final int REFRESH_INTERVAL_TICKS = 10;

    /** Cuanto dura un resaltado sin renovarse: al salir de rango o apagar la vision, se desvanece en ~TTL. */
    static final int HIGHLIGHT_TTL_TICKS = 30;

    /** Entidades resaltadas ahora mismo: hasta que tick siguen marcadas si nadie las renueva. */
    private static final Map<UUID, Long> HIGHLIGHT_EXPIRY = new ConcurrentHashMap<>();

    /** Relacion ya resuelta por tipo de entidad (F10/DJT-222). Ver {@link #relationshipOf}. */
    private static final Map<EntityType<?>, Optional<EntityRelationship>> RELATIONSHIP_CACHE =
            new ConcurrentHashMap<>();

    /** Ultimo tick en el que se barrieron los resaltados vencidos (para no barrer varias veces por tick). */
    private static long lastSweepTick = Long.MIN_VALUE;

    @Override
    public TickPhase phase() {
        return TickPhase.DOJUTSU;
    }

    @Override
    public void tick(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        sweepExpired(level, now); // limpia lo que ya no renueva nadie (una vez por tick de mundo)
        if (now % REFRESH_INTERVAL_TICKS != 0) {
            return; // reescaneo periodico, no cada tick
        }
        PlayerNinjaData data = NinjaData.get(player);
        if (!hasVisionActive(data)) {
            return; // sin ojo que la de, o apagada, o sin chakra: no marca nada (lo suyo caduca por TTL)
        }
        highlightAround(player, level, now);
    }

    /** Decision pura: la vision resalta si el ojo la da con su uso actual, no esta silenciada y hay chakra. */
    static boolean shouldHighlight(boolean grantsVision, boolean visionOff, boolean hasChakra) {
        return grantsVision && !visionOff && hasChakra;
    }

    private static boolean hasVisionActive(PlayerNinjaData data) {
        EyeState eye = data.eye();
        boolean grants = eye.activeEye()
                .flatMap(EyeRegistry::get)
                .map(def -> def.grantsChakraVision(eye.useLevel(def.family())))
                .orElse(false);
        return shouldHighlight(grants, data.vision().chakraVisionOff(), ChakraService.has(data, 1));
    }

    private static void highlightAround(ServerPlayer player, ServerLevel level, long now) {
        AABB box = player.getBoundingBox().inflate(RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
            relationshipOf(target.getType()).ifPresent(rel -> mark(level, target, rel, now));
        }
    }

    /**
     * Relacion de un tipo de entidad, <b>memoizada por tipo</b> (F10/DJT-222).
     *
     * <p>La tabla de {@link ChakraVisionRelations} vive en {@code domain} y por tanto no puede conocer
     * {@code EntityType}: su clave es el id como texto. Traducirlo dentro del bucle construia un {@code String}
     * <b>por entidad y por barrido</b> ({@code EntityType.getKey(...).toString()}), y este bucle recorre un cubo
     * de {@value #RADIUS} bloques de radio cada {@value #REFRESH_INTERVAL_TICKS} ticks por <b>cada</b> jugador
     * con la vision encendida — en una aldea o una granja son cientos de entidades. Ahora el id se construye una
     * vez por <b>tipo</b> y lo demas es una consulta de mapa.
     *
     * <p>El cache esta acotado por el registro de tipos de entidad (unos cientos, fijos tras el arranque): no
     * crece con la partida. Y la tabla de dominio sigue siendo pura y testeable sin Minecraft, que era el motivo
     * de que la clave fuera texto.
     */
    private static Optional<EntityRelationship> relationshipOf(EntityType<?> type) {
        return RELATIONSHIP_CACHE.computeIfAbsent(type,
                key -> ChakraVisionRelations.of(EntityType.getKey(key).toString()));
    }

    /** Marca una entidad: glow + equipo de color, y renueva su caducidad. */
    private static void mark(ServerLevel level, LivingEntity target, EntityRelationship relationship, long now) {
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, HIGHLIGHT_TTL_TICKS, 0, false, false));
        assignTeam(level.getScoreboard(), target, relationship);
        HIGHLIGHT_EXPIRY.put(target.getUUID(), now + HIGHLIGHT_TTL_TICKS);
    }

    /** Retira las marcas que ya nadie renueva (les quita el equipo; el glow caduca solo). */
    private static void sweepExpired(ServerLevel level, long now) {
        if (now == lastSweepTick) {
            return;
        }
        lastSweepTick = now;
        HIGHLIGHT_EXPIRY.entrySet().removeIf(entry -> {
            if (now < entry.getValue()) {
                return false;
            }
            removeFromTeams(level, entry.getKey());
            return true;
        });
    }

    /** Mete a la entidad en el equipo de su relacion (creandolo con su color si hace falta). */
    private static void assignTeam(Scoreboard scoreboard, LivingEntity target, EntityRelationship relationship) {
        PlayerTeam team = scoreboard.getPlayerTeam(relationship.teamName());
        if (team == null) {
            team = scoreboard.addPlayerTeam(relationship.teamName());
            ChatFormatting color = ChatFormatting.getByName(relationship.teamColorName());
            if (color != null) {
                team.setColor(color);
            }
            team.setSeeFriendlyInvisibles(false);
        }
        String name = target.getScoreboardName();
        if (scoreboard.getPlayersTeam(name) != team) {
            scoreboard.addPlayerToTeam(name, team);
        }
    }

    /** Saca a la entidad del equipo de vision en el que estuviera (si es uno nuestro). */
    private static void removeFromTeams(ServerLevel level, UUID entityId) {
        var entity = level.getServer().getPlayerList().getPlayer(entityId);
        String name = entity != null ? entity.getScoreboardName() : null;
        if (name == null) {
            // no es un jugador: resolver por UUID en cualquier nivel
            for (ServerLevel any : level.getServer().getAllLevels()) {
                var found = any.getEntity(entityId);
                if (found != null) {
                    name = found.getScoreboardName();
                    break;
                }
            }
        }
        if (name == null) {
            return; // la entidad ya no esta; su equipo se fue con ella
        }
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayersTeam(name);
        if (team != null && team.getName().startsWith("dojutsu.cv.")) {
            scoreboard.removePlayerFromTeam(name, team);
        }
    }

    /**
     * Alterna la chakra vision de un jugador (lo llama el handler de {@link ToggleVisionPayload}) y le avisa por
     * chat, como el {@code toggle_chakra_highlights} del datapack.
     */
    public static void toggle(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        data.setVision(data.vision().toggledChakraVision());
        boolean off = data.vision().chakraVisionOff();
        player.sendSystemMessage(Component.translatable(
                off ? "vision.dojutsu.chakra_vision.off" : "vision.dojutsu.chakra_vision.on"));
    }

    /** Registra el payload C2S de alternar la vision. Llamar en la inicializacion del mod. */
    public static void register() {
        PlatformServices.NETWORK.register(NetworkService.Side.C2S, ToggleVisionPayload.TYPE,
                ToggleVisionPayload.CODEC, ChakraVisionService::onToggle);
    }

    private static void onToggle(ToggleVisionPayload payload, NetworkService.Context context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                toggle(player);
            }
        });
    }

    /** Vacia el registro de resaltados (al parar el servidor). Los equipos viven en el scoreboard del mundo. */
    public static void clear() {
        HIGHLIGHT_EXPIRY.clear();
        lastSweepTick = Long.MIN_VALUE;
    }
}
