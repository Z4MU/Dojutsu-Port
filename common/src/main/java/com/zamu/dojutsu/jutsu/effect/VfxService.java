package com.zamu.dojutsu.jutsu.effect;

import com.zamu.dojutsu.jutsu.JutsuEntry;
import dev.architectury.event.events.common.TickEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Dibuja el ancla visual de un jutsu y la retira sola (F11/DJT-239).
 *
 * <p>Dos capas, las dos opcionales y declaradas en el JSON de la tecnica ({@link EffectVfx}): particulas, que
 * funcionan hoy sin arte, y un <b>modelo</b> mostrado con un {@link Display.ItemDisplay} de vanilla.
 *
 * <p><b>Por que un ItemDisplay y no una entidad propia.</b> La regla 5 del repo obliga a dar renderer en
 * cliente a toda {@code EntityType} que el mod registre. El {@code ItemDisplay} es de vanilla: ya tiene
 * renderer, ya sincroniza, ya soporta escala y rotacion, y muestra <b>cualquier</b> modelo de item, incluidos
 * los {@code custom_model_data} por tecnica que lleguen despues. El mod no registra ninguna entidad nueva.
 *
 * <p><b>La cola de retirada es propia y no usa {@code ModScheduler}</b> a proposito: el scheduler se inyecta
 * en el dispatcher y no tiene acceso global, y darselo por una via estatica solo para esto habria roto ese
 * diseno. La cola es pequena, esta ordenada por vencimiento y en la inmensa mayoria de los ticks esta vacia.
 */
public final class VfxService {

    /** Un modelo en escena y el tick en que se retira. */
    private record Pending(Display.ItemDisplay display, long expiresAtTick) {
    }

    /**
     * Cola por vencimiento. Es una {@link ArrayDeque} y no una cola de prioridad porque las vidas se
     * programan casi siempre con la misma duracion, asi que llegan ya ordenadas; si alguna se colara fuera de
     * orden, lo unico que pasa es que se retira un poco mas tarde.
     */
    private static final Deque<Pending> PENDING = new ArrayDeque<>();

    /** Tope de modelos vivos a la vez. Un servidor lleno no puede llenarse de entidades por un efecto. */
    static final int MAX_PENDING = 256;

    private VfxService() {
    }

    /** Engancha la retirada al tick del servidor. Llamar una vez en la inicializacion del mod. */
    public static void register() {
        TickEvent.SERVER_POST.register(server -> {
            long now = server.getTickCount();
            while (!PENDING.isEmpty() && PENDING.peekFirst().expiresAtTick() <= now) {
                PENDING.pollFirst().display().discard();
            }
        });
    }

    /**
     * Dibuja el visual de un jutsu en un punto.
     *
     * @param radius radio del efecto, para dispersar las particulas a su tamano cuando la tecnica no declara
     *               uno propio: un jutsu de area de 10 bloques no puede verse igual que uno de 2
     */
    public static void play(ServerLevel level, JutsuEntry jutsu, Vec3 at, double radius) {
        EffectVfx vfx = EffectVfx.parse(jutsu.effectParams().getAsJsonObject());
        double spread = vfx.spreadOr(radius);
        level.sendParticles(vfx.particleFor(jutsu.definition()), at.x, at.y + 0.5D, at.z,
                vfx.count(), spread / 2.0D, 0.5D, spread / 2.0D, vfx.speed());
        vfx.model().ifPresent(model -> spawnModel(level, vfx, model, at));
    }

    /** Atajo para el caso comun: el visual sale del propio caster. */
    public static void playAt(ServerLevel level, JutsuEntry jutsu, net.minecraft.world.entity.Entity caster,
            double radius) {
        play(level, jutsu, caster.position(), radius);
    }

    /**
     * Coloca el modelo y lo apunta a la cola de retirada. Un item desconocido se ignora en silencio.
     *
     * <p>Se construye <b>por NBT</b>, que es el camino que usa {@code /summon}: {@code ItemDisplay} no expone
     * setters publicos para su item ni para su transformacion en Mojmap, y esta via no depende de detalles
     * internos que cambian entre versiones.
     */
    private static void spawnModel(ServerLevel level, EffectVfx vfx, String itemId, Vec3 at) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null || PENDING.size() >= MAX_PENDING) {
            return;
        }
        ItemStack stack = BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(ItemStack.EMPTY);
        if (stack.isEmpty()) {
            return; // item inexistente: sin modelo, pero las particulas ya salieron
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:item_display");
        tag.put("item", stack.save(level.registryAccess()));
        tag.put("transformation", scaleTag(vfx.modelScale()));
        Entity spawned = EntityType.loadEntityRecursive(tag, level, entity -> {
            entity.moveTo(at.x, at.y + vfx.modelOffsetY(), at.z, 0.0F, 0.0F);
            return entity;
        });
        if (!(spawned instanceof Display.ItemDisplay display) || !level.addFreshEntity(display)) {
            return;
        }
        PENDING.addLast(new Pending(display, level.getServer().getTickCount() + vfx.lifetimeTicks()));
    }

    /** Transformacion de escala uniforme, en el formato NBT de vanilla; sin rotacion ni desplazamiento. */
    private static CompoundTag scaleTag(float scale) {
        CompoundTag transformation = new CompoundTag();
        transformation.put("translation", floats(0F, 0F, 0F));
        transformation.put("scale", floats(scale, scale, scale));
        transformation.put("left_rotation", floats(0F, 0F, 0F, 1F));
        transformation.put("right_rotation", floats(0F, 0F, 0F, 1F));
        return transformation;
    }

    private static ListTag floats(float... values) {
        ListTag list = new ListTag();
        for (float value : values) {
            list.add(FloatTag.valueOf(value));
        }
        return list;
    }

    /** Retira todo lo que quede en escena (cambio de mundo, cierre del servidor). Visible para tests. */
    static void clear() {
        PENDING.forEach(pending -> pending.display().discard());
        PENDING.clear();
    }

    /** Cuantos modelos hay esperando retirada. Visible para tests. */
    static int pendingCount() {
        return PENDING.size();
    }
}
