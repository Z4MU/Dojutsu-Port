package com.zamu.dojutsu.neoforge;

import com.zamu.dojutsu.DojutsuMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Entrypoint de NeoForge. Delega en {@link DojutsuMod}: {@link DojutsuMod#init()}
 * en todos los entornos y {@link DojutsuMod#initClient()} solo cuando el entorno
 * es {@link Dist#CLIENT}, de modo que el servidor dedicado no lo invoca.
 *
 * <p>Architectury 13.x obtiene el bus de eventos del mod directamente desde
 * {@code ModList} de NeoForge, por lo que no es necesario registrarlo a mano.
 */
@Mod(DojutsuMod.MOD_ID)
public final class DojutsuNeoForge {

    public DojutsuNeoForge() {
        DojutsuMod.init();
        DojutsuNeoForgeServerEvents.register(); // eventos nativos de NeoForge (server-side): despertar -> chakra
        if (FMLEnvironment.dist == Dist.CLIENT) {
            DojutsuMod.initClient();
        }
    }
}
