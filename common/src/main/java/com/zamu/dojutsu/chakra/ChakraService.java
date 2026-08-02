package com.zamu.dojutsu.chakra;

import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.ChakraState;
import com.zamu.dojutsu.domain.resource.RegenFactor;
import com.zamu.dojutsu.domain.resource.RegenPolicy;
import com.zamu.dojutsu.domain.resource.Resource;
import java.util.Objects;
import java.util.Set;

/**
 * Unica puerta de escritura del <b>chakra normal</b> de un jugador ({@code [S02]}): comprueba, consume y
 * repone, aplicando siempre el clamp {@code [0, maxChakra]}. Ningun otro codigo debe modificar el chakra
 * directamente (todo poder que lo consuma pasa por aqui), para que el cap y el marcado observable sean
 * imposibles de olvidar.
 *
 * <p>Opera sobre el agregado {@link PlayerNinjaData} (capa {@code data}, sin {@code ServerPlayer}): es
 * testeable sin arrancar Minecraft. El clamp se delega en el dominio puro {@link Resource} (DRY, ya probado);
 * la escritura se hace por la puerta {@link PlayerNinjaData#setChakra(ChakraState)}, que marca el campo como
 * observable-sucio. El disparo del sync (delta al cliente) es responsabilidad del llamador.
 *
 * <p>El chakra natural (senjutsu/Jugo) tiene su propia mecanica y llega con la regeneracion (DJT-031) y el
 * Modo Sabio (F6); este servicio cubre solo el chakra normal.
 */
public final class ChakraService {

    private ChakraService() {
    }

    /** Indica si el jugador tiene chakra suficiente para pagar un coste (no muta nada). */
    public static boolean has(PlayerNinjaData data, int cost) {
        return resourceOf(data).has(cost);
    }

    /**
     * Consume chakra de forma "todo o nada": si no hay suficiente, no cambia nada.
     *
     * @return {@code true} si se consumio; {@code false} si no habia suficiente
     */
    public static boolean consume(PlayerNinjaData data, int cost) {
        Resource resource = resourceOf(data);
        if (!resource.consume(cost)) {
            return false;
        }
        write(data, resource);
        return true;
    }

    /** Repone chakra (regen, comida...). El overshoot se clampa al maximo. */
    public static void restore(PlayerNinjaData data, int amount) {
        Resource resource = resourceOf(data);
        resource.add(amount);
        write(data, resource);
    }

    /** Deja el chakra al maximo ({@code chakra/restore} del datapack). */
    public static void restoreFull(PlayerNinjaData data) {
        ChakraState state = state(data);
        data.setChakra(state.withChakra(state.maxChakra()));
    }

    /**
     * Fija el chakra normal a un valor absoluto, con clamp a {@code [0, maxChakra]}. Puerta de escritura para
     * asignaciones directas del datapack (p. ej. la penalizacion de muerte {@code chakra = max/2}).
     */
    public static void set(PlayerNinjaData data, int value) {
        data.setChakra(state(data).withChakra(value)); // withChakra clampa via el invariante de ChakraState
    }

    /**
     * Fija el chakra <b>maximo</b> ({@code d.max_chakra}: lo asigna el clan al unirse, DJT-057) y, si
     * {@code fill}, rellena el chakra al nuevo maximo ({@code d.chakra = d.max_chakra} del join). Si el nuevo
     * maximo es menor que el chakra actual, este se clampa (invariante de {@code ChakraState}; caso Lee: 50).
     */
    public static void setMax(PlayerNinjaData data, int newMax, boolean fill) {
        ChakraState resized = state(data).withMaxChakra(newMax);
        data.setChakra(fill ? resized.withChakra(resized.maxChakra()) : resized);
    }

    /**
     * Avanza un tick la regeneracion de <b>chakra normal</b> segun los factores activos (patron timer-&gt;pulso
     * de {@code [S02]}, tabla en {@link RegenPolicy}). Acumula el timer persistido ({@code chakraTimer}); al
     * alcanzar el umbral suma el incremento (clamp al maximo) y reinicia el timer.
     *
     * @return {@code true} si hubo un pulso que cambio el chakra (para que el llamador sincronice el HUD)
     */
    public static boolean advanceRegen(PlayerNinjaData data, Set<RegenFactor> factors) {
        ChakraState s = state(data);
        RegenPolicy policy = RegenPolicy.resolve(factors);
        int timer = s.chakraTimer() + 1;
        if (timer < policy.thresholdTicks()) {
            data.setChakra(s.withChakraTimer(timer)); // solo timer (no observable): no marca sucio
            return false;
        }
        Resource resource = new Resource(s.chakra(), s.maxChakra());
        resource.add(policy.increment());
        ChakraState next = s.withChakraTimer(0).withChakra(resource.current());
        boolean changed = next.chakra() != s.chakra(); // false si ya estaba al maximo
        data.setChakra(next);
        return changed;
    }

    /**
     * Avanza un tick la regeneracion pasiva de <b>chakra natural</b> del kekkei genkai Jugo ({@code [S02]}):
     * +1 cada 2 ticks (via {@code jugoTimer}) hasta el 99% del maximo; el resto lo cubre el Modo Sabio (S08).
     * No es observable (sin HUD todavia), asi que no genera sync.
     *
     * @param active si el jugador tiene el clan Jugo activo (falso hasta que exista {@code natures}, F3)
     */
    public static void advanceNaturalRegen(PlayerNinjaData data, boolean active) {
        if (!active) {
            return;
        }
        ChakraState s = state(data);
        int cap99 = s.maxNaturalChakra() * 99 / 100;
        if (s.naturalChakra() >= cap99) {
            return; // ya al 99%: no regenera mas pasivamente
        }
        if (s.jugoTimer() == 0) {
            data.setChakra(s.withJugoTimer(1)); // tick de espera (regen cada 2 ticks)
        } else {
            Resource resource = new Resource(s.naturalChakra(), s.maxNaturalChakra());
            resource.add(1);
            data.setChakra(s.withNaturalChakra(resource.current()).withJugoTimer(0));
        }
    }

    private static Resource resourceOf(PlayerNinjaData data) {
        ChakraState state = state(data);
        return new Resource(state.chakra(), state.maxChakra());
    }

    private static void write(PlayerNinjaData data, Resource resource) {
        data.setChakra(state(data).withChakra(resource.current()));
    }

    private static ChakraState state(PlayerNinjaData data) {
        return Objects.requireNonNull(data, "data").chakra();
    }
}
