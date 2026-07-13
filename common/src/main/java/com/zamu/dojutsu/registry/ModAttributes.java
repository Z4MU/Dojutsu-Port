package com.zamu.dojutsu.registry;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.platform.PlatformServices;
import com.zamu.dojutsu.platform.Registrar;
import com.zamu.dojutsu.platform.RegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/**
 * Registro de atributos del mod. En F0 solo declara un atributo placeholder
 * ({@code max_chakra}).
 *
 * <p>Nota: registrar el atributo lo crea en el registro, pero <b>adjuntarlo</b> a la
 * entidad jugador requiere un evento por loader (p. ej. NeoForge
 * {@code EntityAttributeModificationEvent}); se hara con el futuro puerto
 * {@code AttributeService} cuando el modulo de chakra use el atributo.
 */
public final class ModAttributes {

    private static final Registrar<Attribute> ATTRIBUTES =
            PlatformServices.REGISTRAR.create(Registries.ATTRIBUTE);

    /** Chakra maximo (placeholder). */
    public static final RegistryEntry<Attribute> MAX_CHAKRA =
            ATTRIBUTES.register(DojutsuMod.id("max_chakra"),
                    () -> new RangedAttribute("attribute.dojutsu.max_chakra", 100.0D, 0.0D, 1_000_000.0D)
                            .setSyncable(true));

    private ModAttributes() {
    }

    /** Confirma el registro. Llamar durante la inicializacion del mod. */
    public static void register() {
        ATTRIBUTES.commit();
    }
}
