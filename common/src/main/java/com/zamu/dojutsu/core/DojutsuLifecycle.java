package com.zamu.dojutsu.core;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.chakra.RegenService;
import com.zamu.dojutsu.clans.ClanJutsuUnlockService;
import com.zamu.dojutsu.clans.ClanReloadListener;
import com.zamu.dojutsu.clans.ClanService;
import com.zamu.dojutsu.clans.ClanTeamService;
import com.zamu.dojutsu.core.config.DojutsuConfig;
import com.zamu.dojutsu.dojutsu.AwakeningService;
import com.zamu.dojutsu.dojutsu.EternalAwakeningService;
import com.zamu.dojutsu.dojutsu.EvolutionService;
import com.zamu.dojutsu.dojutsu.EyeReloadListener;
import com.zamu.dojutsu.dojutsu.EyeStrainService;
import com.zamu.dojutsu.dojutsu.ability.EyeAbilityService;
import com.zamu.dojutsu.dojutsu.susanoo.SusanooService;
import com.zamu.dojutsu.dojutsu.tenseigan.TenseiganChakraModeService;
import com.zamu.dojutsu.dojutsu.vision.ChakraVisionService;
import com.zamu.dojutsu.sage.NaturalEnergyChargingService;
import com.zamu.dojutsu.bijuu.ai.BeastMoveRegistry;
import com.zamu.dojutsu.bijuu.ai.moves.SignatureBarrageMove;
import com.zamu.dojutsu.bijuu.ai.moves.TailedBeastBallMove;
import com.zamu.dojutsu.domain.bijuu.BeastSignatureTable;
import com.zamu.dojutsu.domain.bijuu.TailedBeast;
import com.zamu.dojutsu.bijuu.jinchuriki.FormService;
import com.zamu.dojutsu.items.MaskService;
import com.zamu.dojutsu.summoning.ContractReloadListener;
import com.zamu.dojutsu.story.DialogueController;
import com.zamu.dojutsu.story.SageEncounterService;
import com.zamu.dojutsu.throwing.FtgMarkerService;
import com.zamu.dojutsu.items.DropUseService;
import com.zamu.dojutsu.trading.ShinobiVillagerService;
import com.zamu.dojutsu.world.crafting.CraftingGateService;
import com.zamu.dojutsu.world.crafting.DropCraftingReloadListener;
import com.zamu.dojutsu.world.crafting.DropCraftingService;
import com.zamu.dojutsu.world.godtree.GodTreeService;
import com.zamu.dojutsu.world.waterwalk.WaterWalkService;
import com.zamu.dojutsu.trading.TradeReloadListener;
import com.zamu.dojutsu.summoning.ContractService;
import com.zamu.dojutsu.summoning.SummonService;
import com.zamu.dojutsu.items.PillService;
import com.zamu.dojutsu.empowerment.cursedseal.CursedSealService;
import com.zamu.dojutsu.empowerment.eightgates.EightGatesService;
import com.zamu.dojutsu.empowerment.soah.SoahSealService;
import com.zamu.dojutsu.sage.SageService;
import com.zamu.dojutsu.progression.RankService;
import com.zamu.dojutsu.progression.SkillPointService;
import com.zamu.dojutsu.jutsu.JutsuLearningService;
import com.zamu.dojutsu.jutsu.JutsuReloadListener;
import com.zamu.dojutsu.jutsu.JutsuXpHandler;
import com.zamu.dojutsu.jutsu.LastCastRegistry;
import com.zamu.dojutsu.jutsu.kenjutsu.SamehadaService;
import com.zamu.dojutsu.jutsu.kenjutsu.WeaponAttributeService;
import com.zamu.dojutsu.jutsu.effect.JutsuEffects;
import com.zamu.dojutsu.jutsu.effect.PrimedStrikeManager;
import com.zamu.dojutsu.jutsu.LearnSequenceReloadListener;
import com.zamu.dojutsu.natures.KekkeiGenkaiReloadListener;
import com.zamu.dojutsu.natures.LearnerService;
import com.zamu.dojutsu.natures.NatureRollReloadListener;
import com.zamu.dojutsu.progression.UnlockReloadListener;
import com.zamu.dojutsu.progression.UnlockService;
import com.zamu.dojutsu.core.lifecycle.DeathHandler;
import com.zamu.dojutsu.core.lifecycle.RespawnHandler;
import com.zamu.dojutsu.core.onboarding.OnboardingController;
import com.zamu.dojutsu.core.scheduler.ModScheduler;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.network.OnboardingSync;
import com.zamu.dojutsu.network.handler.CastJutsuHandler;
import com.zamu.dojutsu.network.handler.EyeMenuHandler;
import com.zamu.dojutsu.network.handler.MenuActionHandler;
import com.zamu.dojutsu.network.handler.OnboardingChoiceHandler;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

/**
 * Punto unico de suscripcion de listeners del loader y setup de arranque. Reemplaza lo <b>estatico</b> de
 * {@code main/load} del datapack (el estado ya no se declara aqui; vive en {@code PlayerNinjaData}).
 *
 * <p>Se invoca una sola vez desde {@link DojutsuMod#init()} (arranque del mod), en cliente y servidor. Solo
 * suscribe hooks del <b>lado servidor</b> (tick, reload, join/quit), por lo que es seguro en servidor
 * dedicado; el codigo de cliente va aparte en {@code initClient()}. Un guard evita doble inicializacion.
 *
 * <p>Ya suscritos: tick, reload, join/quit, cambio de dimension, muerte/respawn (DJT-024), carga de nivel +
 * config con deteccion de hardcore (DJT-025), y los payloads de red del sync (DJT-027).
 */
public final class DojutsuLifecycle {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean initialized;

    private DojutsuLifecycle() {
    }

    /** Suscribe los listeners y ejecuta el setup de arranque. Idempotente (solo actua la primera vez). */
    public static void init() {
        if (initialized) {
            LOGGER.warn("[{}] DojutsuLifecycle.init() invocado dos veces; se ignora.", DojutsuMod.MOD_NAME);
            return;
        }
        initialized = true;

        loadConfig();              // TODO DJT-025
        registerNetworkPayloads(); // TODO DJT-027

        // Motor por tick: pipeline por jugador + scheduler, enganchado al tick del servidor.
        ServerTickDispatcher dispatcher = new ServerTickDispatcher(new PlayerTickPipeline(), new ModScheduler());
        dispatcher.register();

        // EP-CHAKRA: la regeneracion de chakra corre primero en el pipeline (TickPhase.CHAKRA), antes de consumos.
        dispatcher.pipeline().register(new RegenService());

        // EP-WORLD: caminar sobre el agua (TickPhase.WATER_WALK, declarada desde F1 esperando esto). La llave no
        // es config: exige 300 de chakra maximo y 2 de chakra actual (F9/DJT-205).
        dispatcher.pipeline().register(new WaterWalkService());

        // EP-CLANS: desbloqueo progresivo de jutsus de clan (TickPhase.UNLOCKS). UNA sola vez: corrige la doble
        // ejecucion del datapack (clans/jutsu/main corria en tick.json Y en player_functions). DJT-061.
        dispatcher.pipeline().register(new ClanJutsuUnlockService());

        // EP-EYE-CORE: despertar del dojutsu por clan (TickPhase.DOJUTSU). Acumula el temporizador de despertar
        // y, al llegar al umbral, despierta el ojo potencial del clan (50/50 en Otsutsuki). DJT-114.
        dispatcher.pipeline().register(new AwakeningService());

        // EP-EYE-CORE: evolucion del ojo por uso (TickPhase.DOJUTSU). Acumula el uso del ojo activo y recorre el
        // grafo (sharingan->mangekyo/rinnegan, byakugan->tenseigan) segun los requisitos data-driven. DJT-115.
        dispatcher.pipeline().register(new EvolutionService());

        // EP-EYE-CORE: fatiga ocular (TickPhase.STRAIN). Regen pasivo; al 100% apaga/ciega (salvo Rinne
        // Sharingan); la subida la producen las habilidades (DJT-118+); dormir la resetea (SleepChakraHandler).
        dispatcher.pipeline().register(new EyeStrainService());
        EyeStrainService.registerSleepReset(); // engancha el reset de fatiga al despertar (S05)

        // EP-SUSANOO: el Susanoo como ToggleableMode (TickPhase.DOJUTSU). Cobra chakra+fatiga por pulsos segun
        // el nivel y cae solo si el chakra no llega; sube de maestria con el uso. Se enciende desde el menu
        // ocular + tecla R (SusanooAbility), sin tecla nueva. DJT-120.
        dispatcher.pipeline().register(new SusanooService());

        // EP-VISION: chakra vision (TickPhase.DOJUTSU). Resalta las entidades cercanas con el glow nativo,
        // coloreado por relacion (equipos de scoreboard), reescaneando por intervalo (no cada tick, RB-3).
        // Activa por defecto con un ojo que la da; la tecla C la silencia. DJT-125.
        dispatcher.pipeline().register(new ChakraVisionService());
        ChakraVisionService.register(); // payload C2S de alternar la vision

        // F5 (fleco): Tenseigan Chakra Mode como ToggleableMode (TickPhase.DOJUTSU). Buffs + vuelo mientras se
        // sostiene; cae si el chakra baja de 19. Se enciende desde el menu ocular + R (ultima habilidad declarada).
        dispatcher.pipeline().register(new TenseiganChakraModeService());

        // F5 (fleco): despertar del EMS (accion manual del menu ocular). Valida clan/uso/medicina + Mangekyo
        // activo y aplica los bonos; reemplaza las herramientas quirurgicas del datapack.
        EternalAwakeningService.register();

        // EP-SAGE: reunion de energia natural (TickPhase.DOJUTSU). Estarse quieto —por umbral de VELOCIDAD, no
        // sneak_time (S08-R1)— acumula energia; al llenarla te vuelves sabio del linaje del bioma. F6/DJT-135.
        dispatcher.pipeline().register(new NaturalEnergyChargingService());

        // EP-SAGE: modo sabio (TickPhase.DOJUTSU) sobre el arnes compartido de DJT-130. Gasta 1 de energia
        // natural cada 100..500 ticks segun nivel; cae sin chakra o sin energia. Tecla G. F6/DJT-136.
        dispatcher.pipeline().register(new SageService());
        SageService.register(); // payload C2S de alternar el modo

        // EP-CURSED-SEAL: sello maldito (TickPhase.DOJUTSU) sobre el mismo arnes. Cobra 12/8 de chakra cada 40
        // ticks y, si no puedes pagarlo, TE MATA (unico modo con castigo real). Tecla B. F6/DJT-140.
        dispatcher.pipeline().register(new CursedSealService());
        CursedSealService.register(); // payload C2S de alternar el sello

        // EP-EIGHT-GATES: Ocho Puertas (TickPhase.STRAIN, como la fatiga ocular: comparten mecanica, S11-R1).
        // Escalera de 8 puertas con fatiga corporal que sube al abrirlas y baja siempre. Tecla H. F6/DJT-145.
        dispatcher.pipeline().register(new EightGatesService());
        EightGatesService.register(); // payload C2S (abrir/cerrar) + reset de fatiga al dormir

        // EP-SOAH: Sello de la Fuerza de un Centenar (TickPhase.DOJUTSU). Unico modo que DEVUELVE recurso: la
        // reserva se llena a mano y se gasta sola cuando el chakra baja de 5000. Tecla J. F6/DJT-150.
        dispatcher.pipeline().register(new SoahSealService());
        SoahSealService.register(); // payload C2S (alternar/recargar) + otorgamiento por hito de medicina

        // EP-BEAST-AI: catalogo de tecnicas de bestia (F7/DJT-161). El mismo registro lo consultan la IA de la
        // bestia y —desde DJT-166— el jinchuriki transformado: es lo que hace imposible que vuelvan a divergir.
        BeastMoveRegistry.register(new TailedBeastBallMove());
        // Las nueve insignias (F10/DJT-226): una sola clase parametrizada, porque en el datapack las nueve son
        // la misma tecnica con distintas particulas. El Diez Colas queda fuera: su insignia es el Tenpenchii.
        for (TailedBeast beast : TailedBeast.values()) {
            if (BeastSignatureTable.hasBarrage(beast)) {
                BeastMoveRegistry.register(new SignatureBarrageMove(beast));
            }
        }
        LOGGER.info("[{}] Tecnicas de bestia registradas: {}.", DojutsuMod.MOD_NAME, BeastMoveRegistry.size());

        // EP-JINCHURIKI: forma, vinculo y Modo Baryon (TickPhase.DOJUTSU). El vinculo sube SOLO por portar la
        // bestia; el Baryon es terminal (al agotarse, la bestia se va). Tecla K. F7/DJT-166.
        dispatcher.pipeline().register(new FormService());
        FormService.register(); // payload C2S (forma / tecnica / baryon)

        // EP-ITEMS: pildoras de racion militar (TickPhase.CHAKRA). Solo lleva los dos temporizadores: el veto
        // entre mordiscos y la resaca diferida de 10 s, que NO se encadena al comer varias. F8/DJT-170.
        dispatcher.pipeline().register(new PillService());

        // EP-SUMMONING: contratos e invocaciones (F8/DJT-175). Este tick solo resta el veto entre invocaciones;
        // la CADUCIDAD de cada invocacion la lleva su propio AbilityAnchor, no un selector global (S15-R3).
        dispatcher.pipeline().register(new ContractService());

        // EP-THROWING: arrojadizas y Vuelo del Dios del Trueno (F8/DJT-180). La marca vive en el ESTADO del
        // jugador (GlobalPos), no en un armor stand con forceload: no depende del chunk (S16).
        dispatcher.pipeline().register(new FtgMarkerService());
        FtgMarkerService.register(); // payload C2S del salto

        // EP-PROGRESSION: suscriptores reactivos del bus de dominio (server-side).
        RankService.register();    // ninja_xp -> rango (DJT-037)
        UnlockService.register();  // nivel de habilidad -> desbloqueos (DJT-038)
        LearnerService.register(); // naturaleza/KG otorgados -> entrega del Skill Learner (DJT-055)
        JutsuXpHandler.register(); // cast de jutsu -> +1 XP a la afinidad del elemento (DJT-078)
        LastCastRegistry.register(); // cast de jutsu -> registro observable para la copia Sharingan (DJT-079)
        WeaponAttributeService.register(); // subir kenjutsu -> recalcula el daño reversible de las armas (DJT-082)
        SamehadaService.register(); // golpear con la Samehada -> absorbe chakra (DJT-084)
        WaterWalkService.register(); // un golpe te tira al agua mientras caminas sobre ella (F9/DJT-205)
        PrimedStrikeManager.register(); // golpe cargado del rayo: arma al castear, descarga al golpear (DJT-085d)
        ClanService.registerRegenFactors(); // el regenFactor declarado por el clan (DJT-057, deuda de F2)
        JutsuEffects.register(); // familias de efecto del pipeline de cast (DJT-074; reales en DJT-076)
        ShinobiVillagerService.register(); // aldeano/mercader -> comerciante ninja AL APARECER, sin barrido (DJT-190)
        CraftingGateService.register(); // la puerta de habilidad de las 4 recetas gateadas (DJT-206; +3 pergaminos en DJT-225)
        DropUseService.register(); // bomba de humo y sello explosivo: se activan al aterrizar, no al soltar (DJT-225)

        // Datos de servidor del datapack del mod (se dispara al cargar mundo y en /reload).
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new DataReloadListener(), DojutsuMod.id("data_reload"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new UnlockReloadListener(), DojutsuMod.id("unlocks"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new KekkeiGenkaiReloadListener(), DojutsuMod.id("kekkei_genkai"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new NatureRollReloadListener(), DojutsuMod.id("nature_roll"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new ClanReloadListener(), DojutsuMod.id("clans"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new JutsuReloadListener(), DojutsuMod.id("jutsu"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new LearnSequenceReloadListener(), DojutsuMod.id("learner_sequences"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new EyeReloadListener(), DojutsuMod.id("eyes"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new ContractReloadListener(), DojutsuMod.id("contracts"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new TradeReloadListener(), DojutsuMod.id("trades"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new DropCraftingReloadListener(), DojutsuMod.id("drop_crafting"));

        // Hooks de jugador (server-side), suscritos una sola vez.
        PlayerEvent.PLAYER_JOIN.register(DojutsuLifecycle::onJoin);
        PlayerEvent.PLAYER_QUIT.register(DojutsuLifecycle::onQuit);
        PlayerEvent.CHANGE_DIMENSION.register(DojutsuLifecycle::onChangeDimension);
        EntityEvent.LIVING_DEATH.register(DeathHandler::onDeath);
        EntityEvent.LIVING_DEATH.register(SkillPointService::onKill); // el atacante gana skill points (DJT-035)
        PlayerEvent.PLAYER_RESPAWN.register(RespawnHandler::onRespawn);
        LifecycleEvent.SERVER_LEVEL_LOAD.register(DojutsuLifecycle::onLevelLoad);
        // Una sola cola global reparte los encuentros con los Sabios entre todos los conectados (F9/DJT-200).
        LifecycleEvent.SERVER_STARTED.register(SageEncounterService::scheduleQueue);
        // Los Arboles Divinos crecen con el mundo, no con una entidad transitoria (F9/DJT-215).
        TickEvent.SERVER_POST.register(GodTreeService::tick);
        // El drop-crafting mira entidades item del mundo, no jugadores: no cabe en el PlayerTickPipeline (F9/DJT-206).
        TickEvent.SERVER_POST.register(DropCraftingService::tick);
        // Cada mundo tiene su propio getGameTime: limpiar el registro de casts evita comparar ticks
        // de mundos distintos al cambiar de partida (DJT-079).
        LifecycleEvent.SERVER_STOPPING.register(server -> {
            LastCastRegistry.clear();
            ChakraVisionService.clear(); // olvida los resaltados (los equipos viven en el scoreboard del mundo)
            DropCraftingService.reset(); // el contador del barrido tampoco cruza partidas (F9/DJT-206)
            DropUseService.reset(); // los items vigilados son del mundo que se cierra (F10/DJT-225)
            NinjaSfx.clear(); // la puerta de sonido guarda ticks del mundo que se cierra (F10/DJT-227)
        });

        LOGGER.info("[{}] Ciclo de vida inicializado.", DojutsuMod.MOD_NAME);
    }

    private static void onJoin(ServerPlayer player) {
        LOGGER.info("[{}] Jugador {} se unio.", DojutsuMod.MOD_NAME, player.getGameProfile().getName());
        ClanTeamService.sync(player); // el team refleja el clan (cubre saves previos a DJT-062 y deriva)
        MaskService.refresh(player); // si llevaba mascara, vuelve a ocultar el nombre (F8/DJT-170)
        OnboardingController.refresh(player); // nuevos -> SELECTING_CLAN; reanudacion; re-embarque tras reset
        WeaponAttributeService.updateInventory(player); // arma con daño acorde al nivel (armas obtenidas offline)
        UnlockService.onJoin(player); // puesta al dia de hitos que el modelo reactivo no pudo ver (F10/DJT-221)
        NinjaSync.sendFull(player); // snapshot completo al entrar (incluye la fase de onboarding)
    }

    private static void onQuit(ServerPlayer player) {
        MenuActionHandler.forget(player.getUUID()); // libera el estado anti-spam del menu
        EyeMenuHandler.forget(player.getUUID()); // libera el estado anti-spam del menu ocular (DJT-117)
        EyeAbilityService.forget(player.getUUID()); // libera el estado anti-spam del uso de habilidad (DJT-118)
        NinjaSfx.forget(player.getUUID()); // libera la puerta de sonido compartida (F10/DJT-227)
        com.zamu.dojutsu.dojutsu.ability.KamuiAbility.forget(player.getUUID()); // olvida el punto de Kamui (roster)
        SusanooService.forget(player.getUUID()); // el Susanoo no sobrevive a la sesion (DJT-120)
        TenseiganChakraModeService.forget(player); // retira el vuelo del chakra mode al desconectar (fleco F5)
        EyeStrainService.forgetWarnings(player.getUUID()); // olvida el "ya avisado" de fatiga (fleco F5)
        SageService.forget(player.getUUID()); // el modo sabio no sobrevive a la sesion (F6/DJT-136)
        CursedSealService.forget(player.getUUID()); // el sello se apaga al desconectar (F6/DJT-140)
        EightGatesService.forget(player.getUUID()); // las puertas se cierran al desconectar (F6/DJT-145)
        SoahSealService.forget(player.getUUID()); // el SOAH se apaga al desconectar; la reserva persiste (DJT-150)
        FormService.forget(player.getUUID()); // la forma no sobrevive a la sesion; la bestia si (F7/DJT-166)
        PillService.forget(player.getUUID()); // la resaca no persigue entre sesiones (F8/DJT-170)
        SummonService.forget(player.getUUID()); // el veto de invocacion no sobrevive a la sesion (F8/DJT-175)
        FtgMarkerService.forget(player.getUUID()); // vetos de salto y lanzamiento; la MARCA si persiste (DJT-180)
        WaterWalkService.forget(player.getUUID()); // el sostenimiento sobre agua no sobrevive a la sesion (DJT-205)
        CraftingGateService.forget(player.getUUID()); // libera el anti-spam del aviso de crafteo (F9/DJT-206)
        CastJutsuHandler.forget(player.getUUID()); // libera el estado anti-spam de la tecla de cast (DJT-080)
        PrimedStrikeManager.forget(player.getUUID()); // libera el golpe cargado sin descargar (DJT-085d)
        JutsuLearningService.forget(player.getUUID()); // libera el cooldown transitorio del learner (DJT-075)
        LOGGER.info("[{}] Jugador {} salio.", DojutsuMod.MOD_NAME, player.getGameProfile().getName());
    }

    private static void onChangeDimension(ServerPlayer player, ResourceKey<Level> from, ResourceKey<Level> to) {
        NinjaSync.sendFull(player); // re-sync completo tras cambiar de dimension
    }

    private static void loadConfig() {
        DojutsuConfig.load();
    }

    /**
     * Carga de nivel: detecta si el mundo es Hardcore por {@code level.isHardcore()} (no destructivo,
     * reemplaza el "matar a un jugador" del datapack). Solo actua en el overworld para no repetir por dimension.
     */
    private static void onLevelLoad(ServerLevel level) {
        if (level.dimension() == Level.OVERWORLD) {
            LOGGER.info("[{}] Mundo hardcore = {} (deteccion no destructiva).",
                    DojutsuMod.MOD_NAME, level.getServer().isHardcore());
        }
    }

    private static void registerNetworkPayloads() {
        NinjaSync.register();                // S2C: estado observable (DJT-027)
        OnboardingSync.register();           // S2C: opciones de clan de la creacion del ninja (DJT-060)
        MenuActionHandler.register();        // C2S: acciones del menu, validadas en servidor (DJT-042)
        OnboardingChoiceHandler.register();  // C2S: elecciones de la creacion del ninja (DJT-059)
        CastJutsuHandler.register();         // C2S: tecla de cast, lee el item real y valida (DJT-080)
        EyeMenuHandler.register();           // C2S abrir/seleccionar + S2C vista del menu ocular (DJT-117)
        EyeAbilityService.register();        // habilidades oculares (Amaterasu) + C2S de uso (DJT-118)
        DialogueController.register();       // S2C abrir dialogo del Sabio + C2S eleccion de camino (F9/DJT-200)
    }
}
