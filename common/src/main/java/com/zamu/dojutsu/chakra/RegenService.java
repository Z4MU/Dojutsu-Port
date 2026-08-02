package com.zamu.dojutsu.chakra;

import com.zamu.dojutsu.core.PlayerTickSystem;
import com.zamu.dojutsu.core.TickPhase;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.resource.RegenFactor;
import com.zamu.dojutsu.network.NinjaSync;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sistema de regeneracion de chakra por tick ({@code chakra/main} del datapack, {@code [S02]}). Corre
 * <b>primero</b> en el pipeline ({@link TickPhase#CHAKRA}), antes de cualquier consumo, reponiendo el chakra
 * a la tasa de {@link com.zamu.dojutsu.domain.resource.RegenPolicy} segun los factores activos.
 *
 * <p>Responsabilidad (SRP): conoce al jugador y al mundo (guards de espectador/muerte, disparo del sync). La
 * mutacion del estado la hace {@link ChakraService} (unica puerta) y la derivacion de factores es <b>pura</b>
 * ({@link #factorsFor}): ambas testeables sin Minecraft.
 *
 * <p><b>Factores por proveedores (OCP, DJT-057 — salda la deuda de F2):</b> cada modulo que aporte un factor
 * lo <b>registra</b> en el arranque con {@link #registerFactorProvider} — {@code clans} aporta el que declara
 * la {@code ClanDefinition} en datos (Otsutsuki/Uzumaki); el Modo Sabio (F6) registrara el suyo <b>sin tocar
 * este modulo</b>. Asi {@code chakra} no depende de {@code clans} (que ya depende de {@code chakra}: sin
 * ciclos). El ADN de Hashirama se deriva aqui mismo (es estado de {@code data.state}, capa inferior), igual
 * que el KG del clan Jugo (chakra natural). El andamiaje {@code /dojutsuchakra regen <factor>} de F2 quedo
 * retirado.
 */
public final class RegenService implements PlayerTickSystem {

    /** Proveedores de factores registrados por otros modulos (clans en F3, sage en F6). */
    private static final List<Function<PlayerNinjaData, Set<RegenFactor>>> PROVIDERS = new CopyOnWriteArrayList<>();

    @Override
    public TickPhase phase() {
        return TickPhase.CHAKRA;
    }

    @Override
    public void tick(ServerPlayer player) {
        if (player.isSpectator() || !player.isAlive()) {
            return; // sin regen para espectador ni muerto
        }
        PlayerNinjaData data = NinjaData.get(player);
        boolean pulsed = ChakraService.advanceRegen(data, factorsFor(data));
        ChakraService.advanceNaturalRegen(data, hasJugoKekkeiGenkai(data));
        if (pulsed) {
            NinjaSync.sendDelta(player); // el HUD se actualiza solo cuando el chakra cambia
        }
    }

    /**
     * Registra un proveedor de factores de regen. Llamar una vez en el arranque desde el modulo que aporta el
     * factor (p. ej. {@code ClanService.registerRegenFactors()}); los proveedores deben ser <b>puros</b>
     * (leer solo {@link PlayerNinjaData}).
     */
    public static void registerFactorProvider(Function<PlayerNinjaData, Set<RegenFactor>> provider) {
        PROVIDERS.add(provider);
    }

    /** Vacia los proveedores registrados. Solo para tests (el registro es estatico). */
    public static void clearFactorProvidersForTests() {
        PROVIDERS.clear();
    }

    /**
     * Factores de regen activos, derivados del estado real (el {@code d.diff_cr} del datapack): la union de lo
     * que aporten los proveedores registrados, mas el ADN de Hashirama ({@code NatureProfile.hashiramaDna}).
     * <b>Puro</b> (testeable sin MC).
     */
    public static Set<RegenFactor> factorsFor(PlayerNinjaData data) {
        EnumSet<RegenFactor> factors = EnumSet.noneOf(RegenFactor.class);
        for (Function<PlayerNinjaData, Set<RegenFactor>> provider : PROVIDERS) {
            factors.addAll(provider.apply(data));
        }
        if (data.natures().hashiramaDna()) {
            factors.add(RegenFactor.HASHIRAMA_DNA);
        }
        return factors;
    }

    /** El KG del clan Jugo habilita la regeneracion pasiva de chakra natural ({@code [S02]}). Puro. */
    public static boolean hasJugoKekkeiGenkai(PlayerNinjaData data) {
        return data.natures().has(KekkeiGenkai.JUGO_CLAN);
    }
}
