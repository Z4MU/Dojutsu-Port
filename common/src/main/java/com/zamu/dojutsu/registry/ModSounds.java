package com.zamu.dojutsu.registry;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.platform.PlatformServices;
import com.zamu.dojutsu.platform.Registrar;
import com.zamu.dojutsu.platform.RegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

/**
 * Registro de sonidos del mod. En F0 solo declara un evento de sonido placeholder
 * ({@code ninja_debug}).
 */
public final class ModSounds {

    private static final Registrar<SoundEvent> SOUNDS = PlatformServices.REGISTRAR.create(Registries.SOUND_EVENT);

    /** Sonido de depuracion (placeholder, sin fichero de audio asociado aun). */
    public static final RegistryEntry<SoundEvent> NINJA_DEBUG =
            SOUNDS.register(DojutsuMod.id("ninja_debug"),
                    () -> SoundEvent.createVariableRangeEvent(DojutsuMod.id("ninja_debug")));

    private ModSounds() {
    }

    /** Confirma el registro. Llamar durante la inicializacion del mod. */
    public static void register() {
        SOUNDS.commit();
    }
}
