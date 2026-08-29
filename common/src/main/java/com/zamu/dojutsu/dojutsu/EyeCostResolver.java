package com.zamu.dojutsu.dojutsu;

import com.zamu.dojutsu.data.state.EyeState;
import com.zamu.dojutsu.domain.eye.EyeFamily;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Decide cuanto chakra cuesta llevar un ojo encendido (F11/DJT-238). Es la parte <b>pura</b> del drenaje:
 * sin jugador, sin mundo y sin efectos, para que los doce numeros del Sharingan y las ramas de
 * {@code eyematch} se puedan fijar con tests en vez de a ojo en partida.
 *
 * <p>Evalua las reglas <b>en orden</b> y devuelve la primera que encaja, igual que el {@code cost.mcfunction}
 * del datapack se ejecuta de arriba abajo.
 */
public final class EyeCostResolver {

    /**
     * Lo que el resolutor necesita saber del jugador. Se pasa aplanado a proposito: mantiene la decision pura
     * y hace explicito que este calculo NO mira nada mas.
     *
     * @param clanId    clan del jugador, o vacio si no tiene
     * @param awakened  si la familia del ojo activo esta despertada
     * @param eyeMatch  si el ojo que lleva es la variante que el mismo desperto (ver {@link EyeState#awakenedEye})
     * @param mangekyo  si tiene el Mangekyo despierto (el Sharingan cobra menos cuando lo tienes)
     * @param useLevel  uso acumulado de la familia del ojo activo (de aqui salen los tomoe)
     */
    public record Context(Optional<String> clanId, boolean awakened, boolean eyeMatch, boolean mangekyo,
            int useLevel) {
    }

    private EyeCostResolver() {
    }

    /**
     * Coste del intervalo para este ojo y este jugador, o vacio si no se le cobra nada.
     *
     * <p>Vacio significa <b>de verdad</b> "no cobrar": lo devuelve tanto un ojo sin coste declarado como uno
     * cuyas reglas no cubren este caso y no tiene {@code fallback}. Quien llama decide si eso es normal o
     * merece un aviso.
     */
    public static OptionalInt costFor(EyeDefinition definition, Context context) {
        EyePassiveCost cost = definition.passiveCost().orElse(null);
        if (cost == null) {
            return OptionalInt.empty();
        }
        for (EyePassiveCost.Rule rule : cost.rules()) {
            if (matches(rule, context)) {
                return OptionalInt.of(rule.cost());
            }
        }
        return cost.fallback().map(OptionalInt::of).orElseGet(OptionalInt::empty);
    }

    /** Todas las condiciones declaradas de la regla se cumplen (las no declaradas no restringen). */
    static boolean matches(EyePassiveCost.Rule rule, Context context) {
        if (rule.clan().isPresent() && !clanIs(context.clanId(), rule.clan().get())) {
            return false;
        }
        for (String excluded : rule.notClan()) {
            if (clanIs(context.clanId(), excluded)) {
                return false;
            }
        }
        if (rule.awakened().isPresent() && rule.awakened().get() != context.awakened()) {
            return false;
        }
        if (rule.eyeMatch().isPresent() && rule.eyeMatch().get() != context.eyeMatch()) {
            return false;
        }
        if (rule.hasMangekyo().isPresent() && rule.hasMangekyo().get() != context.mangekyo()) {
            return false;
        }
        if (rule.minUse().isPresent() && context.useLevel() < rule.minUse().get()) {
            return false;
        }
        return rule.maxUse().isEmpty() || context.useLevel() < rule.maxUse().get();
    }

    /** Pertenencia a un clan, admitiendo los compuestos del grafo ({@code otsutsuki_uchiha}). */
    private static boolean clanIs(Optional<String> clanId, String required) {
        return clanId.filter(id -> EvolutionService.clanMatches(id, required)).isPresent();
    }

    /**
     * Construye el contexto desde el estado real del jugador.
     *
     * <p>{@code eyeMatch} es "el ojo que llevas es la variante que TU despertaste". El datapack lo resuelve
     * comparando el ojo del slot con el que despertaste, y acepta ademas su forma <b>eterna</b>: quien
     * despierta el EMS de su propio Mangekyo sigue llevando el suyo.
     */
    public static Context contextOf(EyeState state, EyeDefinition definition, Optional<String> clanId) {
        EyeFamily family = definition.family();
        boolean match = state.activeEye()
                .map(active -> state.awakenedEye(family).filter(active::equals).isPresent())
                .orElse(false);
        return new Context(clanId, state.isAwakened(family), match,
                state.isAwakened(EyeFamily.MANGEKYO), state.useLevel(family));
    }
}
