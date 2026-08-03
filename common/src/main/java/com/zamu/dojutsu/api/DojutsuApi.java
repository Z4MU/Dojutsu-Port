package com.zamu.dojutsu.api;

import com.zamu.dojutsu.bijuu.ai.BeastMove;
import com.zamu.dojutsu.bijuu.ai.BeastMoveRegistry;
import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.core.DomainEvents;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.domain.event.DomainEvent;
import com.zamu.dojutsu.dojutsu.ability.EyeAbility;
import com.zamu.dojutsu.dojutsu.ability.EyeAbilityRegistry;
import com.zamu.dojutsu.jutsu.JutsuGrantService;
import com.zamu.dojutsu.jutsu.effect.JutsuEffect;
import com.zamu.dojutsu.jutsu.effect.JutsuEffectRegistry;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.progression.XpApi;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;

/**
 * <b>Fachada pública del mod Dojutsu</b> (F10/DJT-220). El punto de entrada para otro mod.
 *
 * <h2>Qué se puede hacer</h2>
 * Leer el estado de un ninja ({@link #view}), escribirlo por las puertas del propio mod, aportar contenido de
 * código en el arranque y escuchar lo que pasa. El contenido de <b>datos</b> —técnicas, ojos, clanes,
 * contratos— va por {@link DojutsuRegistryEvents}, porque se recarga con {@code /reload}.
 *
 * <h2>Las escrituras no tienen atajo</h2>
 * Cada método de escritura delega en el <b>mismo servicio</b> que usa el mod: el chakra sólo por
 * {@code ChakraService}, la XP sólo por {@code XpApi}, las técnicas sólo por {@code JutsuGrantService}. No es
 * ceremonia: esas puertas hacen el clamp, emiten los eventos de dominio y sincronizan al cliente. Una escritura
 * por fuera dejaría el HUD mintiendo y los desbloqueos sin disparar.
 *
 * <h2>Versionado</h2>
 * {@link #API_VERSION} sigue <i>semver</i> sobre <b>esta</b> superficie, no sobre la versión del mod. Sube la
 * mayor si algo de aquí deja de compilar o cambia de significado; la menor al añadir. Comprobarla al arrancar es
 * barato y evita fallos raros:
 *
 * <pre>{@code
 * if (DojutsuApi.majorVersion() != 1) {
 *     throw new IllegalStateException("Dojutsu API incompatible: " + DojutsuApi.API_VERSION);
 * }
 * }</pre>
 */
public final class DojutsuApi {

    /**
     * Versión de <b>esta API</b> (semver), independiente de la del mod. {@code 1.0.0} es la primera superficie
     * congelada, al cerrar F10.
     */
    public static final String API_VERSION = "1.0.0";

    private DojutsuApi() {
    }

    /** Componente mayor de {@link #API_VERSION}: cambia cuando algo de aquí rompe. */
    public static int majorVersion() {
        return Integer.parseInt(API_VERSION.substring(0, API_VERSION.indexOf('.')));
    }

    // ------------------------------------------------------------------
    // Leer
    // ------------------------------------------------------------------

    /**
     * Foto inmutable del estado ninja de un jugador. Es una <b>copia</b>: leerla no permite escribir, y no se
     * actualiza sola.
     */
    public static NinjaView view(ServerPlayer player) {
        return NinjaView.of(NinjaData.get(player));
    }

    // ------------------------------------------------------------------
    // Escribir (por las puertas del mod)
    // ------------------------------------------------------------------

    /** Repone chakra, con clamp al máximo. Sincroniza al cliente. */
    public static void restoreChakra(ServerPlayer player, int amount) {
        PlayerNinjaData data = NinjaData.get(player);
        ChakraService.restore(data, amount);
        NinjaSync.sendDelta(player);
    }

    /**
     * Consume chakra <b>todo o nada</b>: si no llega, no descuenta nada.
     *
     * @return {@code true} si se cobró
     */
    public static boolean consumeChakra(ServerPlayer player, int amount) {
        PlayerNinjaData data = NinjaData.get(player);
        boolean paid = ChakraService.consume(data, amount);
        if (paid) {
            NinjaSync.sendDelta(player);
        }
        return paid;
    }

    /**
     * Suma XP ninja. Sube el rango solo si cruza umbral y dispara los desbloqueos que dependan de ella — por eso
     * va por {@code XpApi} y no escribiendo el campo.
     */
    public static void addNinjaXp(ServerPlayer player, int xp) {
        XpApi.addNinjaXp(player, xp);
    }

    /**
     * Entrega una técnica: la marca como conocida y pone el ítem en el inventario. Idempotente, y tolerante con
     * un id que no exista (se salta con aviso).
     *
     * @return {@code true} si se entregó ahora
     */
    public static boolean grantJutsu(ServerPlayer player, String jutsuId) {
        return JutsuGrantService.grant(player, jutsuId);
    }

    // ------------------------------------------------------------------
    // Aportar contenido de codigo (en el arranque)
    // ------------------------------------------------------------------

    /**
     * Registra una <b>familia de efecto</b> de jutsu: lo que un JSON de técnica puede poner en su campo
     * {@code effect}. Las siete del mod (proyectil, área, buff, teleport, estructura, curación, golpe cargado)
     * se registran así mismo.
     *
     * <p>Llamar <b>una vez</b> durante la inicialización del mod, antes de que se carguen los datos.
     */
    public static void registerJutsuEffect(String familyId, JutsuEffect effect) {
        JutsuEffectRegistry.register(familyId, effect);
    }

    /** Registra una <b>habilidad ocular</b>, que un ojo puede declarar en su lista de {@code abilities}. */
    public static void registerEyeAbility(String abilityId, EyeAbility ability) {
        EyeAbilityRegistry.register(abilityId, ability);
    }

    /**
     * Registra una <b>técnica de Bestia con Cola</b>. La misma instancia la usan la IA de la bestia y el
     * jinchūriki transformado: no hay dos versiones que puedan divergir.
     */
    public static void registerBeastMove(BeastMove move) {
        BeastMoveRegistry.register(move);
    }

    // ------------------------------------------------------------------
    // Escuchar
    // ------------------------------------------------------------------

    /**
     * Se suscribe a un evento de dominio del mod: despertar y evolución de ojo, cast de jutsu, sellado de
     * bestia, cambio de nivel o de ninja_xp, concesión de naturaleza o Kekkei Genkai, y encendido de modo.
     *
     * <p>Los eventos son <b>de servidor</b> y se emiten dentro del tick: el suscriptor debe ser corto y no
     * bloquear. Ver {@code com.zamu.dojutsu.domain.event} para los tipos.
     *
     * @param type     clase del evento
     * @param listener qué hacer cuando ocurra
     */
    public static <E extends DomainEvent> void subscribe(Class<E> type, Consumer<E> listener) {
        DomainEvents.bus().subscribe(type, listener);
    }
}
