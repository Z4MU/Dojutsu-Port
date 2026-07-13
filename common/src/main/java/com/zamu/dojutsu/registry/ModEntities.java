package com.zamu.dojutsu.registry;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.entity.DebugMarkerEntity;
import com.zamu.dojutsu.platform.PlatformServices;
import com.zamu.dojutsu.platform.Registrar;
import com.zamu.dojutsu.platform.RegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Registro de tipos de entidad del mod. En F0 solo declara una entidad marcador
 * placeholder ({@code debug_marker}) para validar el registro de {@code EntityType}.
 */
public final class ModEntities {

    private static final Registrar<EntityType<?>> ENTITIES =
            PlatformServices.REGISTRAR.create(Registries.ENTITY_TYPE);

    /** Entidad marcador de depuracion (sin renderer; no spawnear en cliente aun). */
    public static final RegistryEntry<EntityType<DebugMarkerEntity>> DEBUG_MARKER =
            ENTITIES.register(DojutsuMod.id("debug_marker"),
                    () -> EntityType.Builder.of(DebugMarkerEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .build("debug_marker"));

    private ModEntities() {
    }

    /** Confirma el registro. Llamar durante la inicializacion del mod. */
    public static void register() {
        ENTITIES.commit();
    }
}
