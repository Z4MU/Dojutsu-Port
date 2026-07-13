package com.zamu.dojutsu.registry;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.platform.PlatformServices;
import com.zamu.dojutsu.platform.Registrar;
import com.zamu.dojutsu.platform.RegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

/**
 * Registro de items del mod. En F0 solo declara un item placeholder ({@code ninja_debug})
 * para validar el puerto {@link com.zamu.dojutsu.platform.RegistrarService} end-to-end.
 */
public final class ModItems {

    private static final Registrar<Item> ITEMS = PlatformServices.REGISTRAR.create(Registries.ITEM);

    /** Item de depuracion, obtenible con {@code /give @s dojutsu:ninja_debug}. */
    public static final RegistryEntry<Item> NINJA_DEBUG =
            ITEMS.register(DojutsuMod.id("ninja_debug"), () -> new Item(new Item.Properties()));

    private ModItems() {
    }

    /** Confirma el registro. Llamar durante la inicializacion del mod. */
    public static void register() {
        ITEMS.commit();
    }
}
