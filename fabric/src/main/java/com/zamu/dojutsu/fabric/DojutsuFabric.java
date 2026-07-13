package com.zamu.dojutsu.fabric;

import com.zamu.dojutsu.DojutsuMod;
import net.fabricmc.api.ModInitializer;

/**
 * Entrypoint {@code main} de Fabric. Se ejecuta en cliente y servidor y delega
 * la inicializacion comun en {@link DojutsuMod#init()}.
 */
public final class DojutsuFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        DojutsuMod.init();
    }
}
