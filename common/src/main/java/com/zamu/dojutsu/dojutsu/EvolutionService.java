package com.zamu.dojutsu.dojutsu;

import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.core.DomainEvents;
import com.zamu.dojutsu.core.PlayerTickSystem;
import com.zamu.dojutsu.core.TickPhase;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.EyeState;
import com.zamu.dojutsu.domain.event.EyeEvolvedEvent;
import com.zamu.dojutsu.domain.eye.EyeEvolution;
import com.zamu.dojutsu.domain.eye.EyeFamily;
import com.zamu.dojutsu.domain.eye.EyeRequirement;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.natures.KekkeiGenkaiService;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.progression.XpApi;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import net.minecraft.server.level.ServerPlayer;

/**
 * Evoluciona el ojo activo por <b>uso</b> recorriendo el grafo data-driven de {@link EyeDefinition} {@code [S05]}
 * (el bloque de evolucion de {@code dojutsu/main}, DJT-115). Sistema por tick en {@link TickPhase#DOJUTSU}.
 *
 * <p><b>Dos partes fieles al datapack:</b>
 * <ol>
 *   <li><b>Acumulacion pasiva del uso</b> (+1/tick) de la familia del ojo activo mientras <b>alguna</b> arista
 *       de evolucion dependa del uso ({@code d.<eye>_use += 1} solo {@code if wearing_<eye>}). Los ojos
 *       terminales (Rinnegan/Tenseigan) no acumulan — el datapack tampoco (RA-3, sin trabajo por tick inutil).</li>
 *   <li><b>Transicion por grafo</b>: la primera arista cuyos {@code requirements} se cumplen (uso, clan, flag)
 *       cambia el ojo activo, marca la familia destino como despertada, aplica el bonus del destino
 *       ({@code max_chakra}/{@code ninja_xp}, p. ej. Mangekyo +20/+80, Rinnegan +100/+150) y emite
 *       {@link EyeEvolvedEvent}. Las <b>rutas divergentes</b> (Uchiha/Otsutsuki vs. implante) son datos, no
 *       ramas.</li>
 * </ol>
 *
 * <p><b>El EMS (forma eterna) NO se maneja aqui:</b> en el datapack es una <b>accion manual</b>
 * ({@code eternal/try_give}, +60/+150, exige el Mangekyo activo y notificacion), no una auto-evolucion por uso.
 * Corresponde al menu/accion ocular (DJT-117+). Los tomoe 1/2/3 del Sharingan son nivel <b>derivado</b> del uso
 * (no ojos aparte, DJT-113), no una transicion de grafo.
 *
 * <p><b>Servicio puro + glue</b>: {@link #evaluate} decide sin Minecraft; devuelve la <b>misma instancia</b> de
 * estado cuando no hay cambio (el glue evita escrituras inutiles).
 */
public final class EvolutionService implements PlayerTickSystem {

    /**
     * Azar <b>compartido</b> del glue (F10/DJT-222). La variante de Mangekyō ({@code mangekyo_rng}) se tira una
     * vez en la vida de un ninja; construir un {@code new Random()} por jugador y tick pagaba un
     * {@code System.nanoTime()} y un CAS sobre el uniquificador estatico de la JVM para no usarlo casi nunca.
     * {@link ThreadLocalRandom} no asigna nada y es un {@link RandomGenerator}, que es lo que pide
     * {@link #evaluate} — el parametro sigue inyectado para que los tests le pasen una semilla fija.
     */
    private static final RandomGenerator RANDOM = ThreadLocalRandom.current();

    /** El ojo activo evoluciono de {@code from} a {@code to} (para el glue: bonus + evento). */
    record Transition(String from, String to) {
    }

    /** Resultado puro de un tick de evolucion: el nuevo estado y, si hubo, la transicion. */
    record EvolveResult(EyeState state, Optional<Transition> transition) {
    }

    @Override
    public TickPhase phase() {
        return TickPhase.DOJUTSU;
    }

    @Override
    public void tick(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        if (data.eye().activeEye().isEmpty()) {
            // Guarda barata (F10/DJT-222): sin ojo activo no hay nada que evolucionar, y es el caso de casi
            // todos los jugadores casi todo el rato. Antes se construia el predicado de flags y un Random
            // nuevo, y se entraba en evaluate, para que devolviera exactamente lo que le entro.
            return;
        }
        EvolveResult result =
                evaluate(data.eye(), data.clan().clan(), flagsFor(data), EyeRegistry::get, RANDOM);
        if (result.state() == data.eye()) {
            return; // sin cambios: sin ojo, ojo terminal, o uso que no alimenta evolucion (misma instancia)
        }
        data.setEye(result.state()); // uso acumulado (no observable) o evolucion; el glue sincroniza si toca
        result.transition().ifPresent(transition -> applyEvolution(player, data, transition));
    }

    /**
     * Decide la evolucion de este tick, sin Minecraft: acumula el uso (si alimenta alguna evolucion) y aplica la
     * primera arista satisfecha. Devuelve el estado <b>sin tocar</b> (misma instancia) cuando no hay nada que
     * hacer.
     *
     * @param eyeState  estado ocular actual
     * @param clanId    clan del jugador (para los requisitos {@code CLAN})
     * @param flags     resolucion de flags {@code FLAG} (p. ej. {@code hashirama_dna})
     * @param eyeLookup resolucion id de ojo -&gt; definicion ({@code EyeRegistry::get} en produccion)
     * @param random    azar para la variante de Mangekyo por personaje ({@code mangekyo_rng}; inyectado, testeable)
     */
    static EvolveResult evaluate(EyeState eyeState, Optional<String> clanId, Predicate<String> flags,
            Function<String, Optional<EyeDefinition>> eyeLookup, RandomGenerator random) {
        Optional<EyeDefinition> activeOpt = eyeState.activeEye().flatMap(eyeLookup);
        if (activeOpt.isEmpty()) {
            return new EvolveResult(eyeState, Optional.empty());
        }
        EyeDefinition active = activeOpt.get();

        // 1) Acumulacion pasiva del uso, solo si algo lo exige: una arista de evolucion, o un requisito de uso
        //    para volverse eterno (Mangekyo->EMS). Los terminales sin uso-gate no acumulan (RA-3, sin trabajo inutil).
        EyeState state = eyeState;
        if (hasUseGatedEvolution(active) || hasUseGatedEternal(active)) {
            state = eyeState.withUseLevel(active.family(), eyeState.useLevel(active.family()) + 1);
        }

        // 2) Primera arista cuyos requisitos se cumplen y cuyo destino existe (precedencia = orden de la lista).
        //    Si la arista tiene un pool de variantes (mangekyo_rng), el destino se elige al azar entre las que existen.
        for (EyeEvolution evolution : active.evolutions()) {
            if (!requirementsMet(evolution.requirements(), state, active.family(), clanId, flags)) {
                continue;
            }
            Optional<EyeDefinition> target = resolveTarget(evolution, eyeLookup, random);
            if (target.isPresent()) {
                EyeState evolved = state.withActiveEye(target.get().id())
                        .withAwakenedEye(target.get().family(), target.get().id());
                return new EvolveResult(evolved, Optional.of(new Transition(active.id(), target.get().id())));
            }
        }
        return new EvolveResult(state, Optional.empty());
    }

    /**
     * Resuelve el destino de una arista: si tiene pool de variantes ({@code mangekyo_rng}), elige <b>al azar</b>
     * entre las que existen en el registro; si no, el {@link EyeEvolution#to()} fijo. Vacio si ninguna existe.
     */
    private static Optional<EyeDefinition> resolveTarget(EyeEvolution evolution,
            Function<String, Optional<EyeDefinition>> eyeLookup, RandomGenerator random) {
        if (!evolution.isRandom()) {
            return eyeLookup.apply(evolution.to());
        }
        List<EyeDefinition> pool = evolution.variants().stream()
                .map(eyeLookup)
                .flatMap(Optional::stream)
                .toList();
        return pool.isEmpty() ? Optional.empty() : Optional.of(pool.get(random.nextInt(pool.size())));
    }

    /**
     * {@code true} si volverse eterno (EMS) exige un umbral de uso ({@code use_level} en
     * {@code eternalRequirements}): el Mangekyo, terminal sin aristas, debe acumular uso para llegar al Eternal
     * Mangekyo. Divergencia consciente del contador continuo {@code sharingan_use} del datapack: aqui el uso es de
     * la familia del ojo (Mangekyo), asi que el umbral se cuenta desde que se alcanza el Mangekyo.
     */
    static boolean hasUseGatedEternal(EyeDefinition eye) {
        return eye.eternalRequirements().stream()
                .anyMatch(req -> req.type() == EyeRequirement.Type.USE_LEVEL);
    }

    /** {@code true} si alguna arista de evolucion depende del contador de uso (para no acumular en vano). */
    private static boolean hasUseGatedEvolution(EyeDefinition eye) {
        return eye.evolutions().stream()
                .flatMap(evolution -> evolution.requirements().stream())
                .anyMatch(req -> req.type() == EyeRequirement.Type.USE_LEVEL);
    }

    /** Comprueba que se cumplen <b>todos</b> los requisitos de una arista (AND). */
    static boolean requirementsMet(List<EyeRequirement> requirements, EyeState state, EyeFamily sourceFamily,
            Optional<String> clanId, Predicate<String> flags) {
        for (EyeRequirement req : requirements) {
            boolean ok = switch (req.type()) {
                case USE_LEVEL -> {
                    int use = state.useLevel(sourceFamily);
                    yield use >= req.min() && use <= req.max(); // rango inclusivo, como los entity_scores del datapack
                }
                case CLAN -> clanId.map(id -> clanMatches(id, req.value())).orElse(false);
                case FLAG -> flags.test(req.value());
            };
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /**
     * Un requisito de clan acepta un predicate <b>compuesto</b> unido por {@code _} (como {@code is_*} del
     * datapack): {@code "otsutsuki_uchiha"} = Otsutsuki <b>o</b> Uchiha. Los ids de clan son tokens simples.
     */
    static boolean clanMatches(String clanId, String requirement) {
        for (String allowed : requirement.split("_")) {
            if (allowed.equals(clanId)) {
                return true;
            }
        }
        return false;
    }

    /** Resolucion de flags de requisito sobre el estado del jugador (hoy solo el ADN de Hashirama). */
    private static Predicate<String> flagsFor(PlayerNinjaData data) {
        return flag -> "hashirama_dna".equals(flag) && data.natures().hashiramaDna();
    }

    /** Aplica los efectos de la evolucion (bonus del destino, evento) y sincroniza. */
    private static void applyEvolution(ServerPlayer player, PlayerNinjaData data, Transition transition) {
        EyeRegistry.get(transition.to()).ifPresent(target -> {
            if (target.awakenMaxChakraBonus() > 0) {
                ChakraService.setMax(data, data.chakra().maxChakra() + target.awakenMaxChakraBonus(), false);
            }
            if (target.awakenNinjaXpBonus() > 0) {
                XpApi.addNinjaXp(player, target.awakenNinjaXpBonus());
            }
            grantEyeKekkeiGenkai(player, target);
            EyeSounds.playEvolve(player, target.family()); // el playsound de dojutsu/<ojo>/evolve (F10/DJT-224)
            DomainEvents.bus().publish(new EyeEvolvedEvent(player.getUUID(), transition.from(), transition.to()));
            NinjaSync.sendDelta(player);
        });
    }

    /**
     * Concede el kekkei genkai que declare el ojo alcanzado ({@code awakenKekkeiGenkai}), si lo declara: el
     * {@code natures/give/kg/blaze} que el datapack otorga al despertar el Mangekyō de Sasuke. <b>Data-driven</b>
     * (RA-3): el vinculo ojo→KG vive en el JSON del ojo, no en un {@code if} por personaje. El
     * {@code KekkeiGenkaiService} ya es idempotente y avisa por chat, asi que aqui solo se delega.
     */
    private static void grantEyeKekkeiGenkai(ServerPlayer player, EyeDefinition target) {
        target.awakenKekkeiGenkai()
                .flatMap(EvolutionService::parseKekkeiGenkai)
                .ifPresent(kg -> KekkeiGenkaiService.grant(player, kg));
    }

    /** Resuelve un id data-driven de KG ({@code "blaze"}) a su enum; vacio si no existe (JSON tolerante). */
    private static Optional<KekkeiGenkai> parseKekkeiGenkai(String id) {
        for (KekkeiGenkai kg : KekkeiGenkai.values()) {
            if (KekkeiGenkaiService.id(kg).equals(id)) {
                return Optional.of(kg);
            }
        }
        return Optional.empty();
    }
}
