package com.zamu.dojutsu.debug;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Raiz comun de los comandos de <b>depuracion</b> (F10).
 *
 * <h2>Por que existe</h2>
 * Los veinte comandos de QA se registraban <b>sin ninguna puerta de permisos</b>: cualquier jugador de un
 * servidor podia invocarse un Nueve Colas, sellarselo, ponerse la maestria del Susanoo al maximo o fijarse la
 * fatiga ocular. Comodo en una sesion de pruebas en solitario, inaceptable en cuanto haya alguien mas conectado
 * — y este mod esta pensado para servidores.
 *
 * <p>Ahora todos pasan por {@link #root(String)}, que aplica el <b>mismo nivel de operador</b> que
 * {@code /dojutsu reset_ninja} ({@code AdminCommands}). Un comando nuevo que se registre por aqui hereda la
 * puerta sin que nadie tenga que acordarse; ese es justo el punto de tener una raiz comun en vez de veinte
 * {@code requires} copiados.
 *
 * <p><b>Nota:</b> en un mundo de un jugador el anfitrion suele tener permiso de operador, asi que la sesion de
 * QA no cambia. Lo que cambia es lo que pasa cuando alguien mas entra.
 */
public final class DebugCommands {

    /**
     * Nivel de operador exigido, el mismo que los comandos de administracion reales del mod. Nivel 2 es el de
     * los comandos de trampa de vanilla ({@code /give}, {@code /summon}), que es exactamente lo que hacen
     * estos.
     */
    public static final int OP_LEVEL = 2;

    private DebugCommands() {
    }

    /**
     * Crea la raiz de un comando de depuracion, ya con la puerta de operador puesta.
     *
     * @param name nombre del comando sin barra ({@code "dojutsubeast"})
     */
    public static LiteralArgumentBuilder<CommandSourceStack> root(String name) {
        return Commands.literal(name).requires(source -> source.hasPermission(OP_LEVEL));
    }
}
