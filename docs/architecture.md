# Arquitectura — Dojutsu Port (estado: F0–F4 completadas; siguiente F5 ojos)

> Este documento describe la arquitectura **implementada** hasta hoy. El diseño completo (todos los
> módulos del mod) vive en el ADD externo: `D:\Projectos claude\Dojutsu reverse\DOCS\arquitectura\`.
> Aquí se refleja solo lo que existe en el código.

## 1. Estilo arquitectónico

**Clean/Hexagonal Architecture** sobre **Architectury multiloader**:

- Núcleo/lógica en `common` (loader-agnóstico).
- Adaptadores por loader en `neoforge` y `fabric`.
- Diferencias de loader **solo** tras un **puerto** (`platform`), resuelto por `ServiceLoader`.

## 2. Subproyectos Gradle

```
Dojutsu Port/
├── common/     # lógica loader-agnóstica (paquetes bajo com.zamu.dojutsu)
├── neoforge/   # adaptador NeoForge (loom.platform=neoforge)
├── fabric/     # adaptador Fabric
├── gradle/ · build.gradle · settings.gradle · gradle.properties
└── .github/workflows/ci.yml
```

## 3. Módulos existentes (paquetes bajo `com.zamu.dojutsu`) y responsabilidades

| Módulo (paquete) | Responsabilidad | Estado |
|------------------|-----------------|--------|
| raíz `DojutsuMod` | `MOD_ID`, `init()`/`initClient()`, helper `id(path)`, orquestación del registro | permanente |
| `platform` | **Puertos** (`RegistrarService`, `AttachmentService`, `NetworkService`) + fachada `PlatformServices`; tipos de apoyo (`Registrar`, `RegistryEntry`, `SimpleRegistryEntry`, `AttachmentHandle`) | permanente |
| `registry` | Clases `Mod*`: declaran contenido vía los puertos. `ModDataComponents` (F3/DJT-055): primer registro sobre `Registries.DATA_COMPONENT_TYPE` | permanente |
| `core` | `DataReloadListener` (datos de servidor) | permanente |
| `chakra` | `ChakraService` (única puerta del chakra: has/consume/restore/setMax + regen, clamp vía `Resource`) + `RegenService` (`PlayerTickSystem`, fase `CHAKRA`, tabla de `[S02]`; **factores por proveedores** registrados por otros módulos — clans hoy, sage en F6) + `SleepChakraHandler` (dormir→chakra; +**registro de handlers de despertar** por proveedor, F5/DJT-116: el reset de fatiga ocular se engancha aquí sin que `chakra` dependa de `dojutsu`) + `FoodChakraHandler` (comer→chakra) — F2/DJT-030,031,032 | permanente |
| `progression` | `SkillPointService` (kills→puntos, gasto), `XpApi`/`LevelingService` (única puerta de XP, auto-nivelación reactiva, add/remove de ninja_xp), `RankService` (rango), `UnlockService`+`UnlockRegistry`+`UnlockReloadListener` (desbloqueos data-driven) — F2/DJT-034…038 | permanente |
| `natures` | `NatureService` (única puerta del perfil elemental: grant/revoke/has + `addAffinityLevels` + `setAffinityLevel` (SET absoluto del onboarding, DJT-059); ninja_xp exacta por naturaleza, idempotente, emite `NatureGrantedEvent`; corrige el bug Yang `[S03-R1]`) — F3/DJT-051 · `KekkeiGenkaiService`+`KekkeiGenkaiRegistry`+`KekkeiGenkaiDefinition`+`KekkeiGenkaiReloadListener` (18 KG **data-driven** con componentes/XP/bonus; otorgar un KG implica sus naturalezas) — F3/DJT-052 · `TemporaryGrantService` (naturalezas/KG **prestados por una fuente**; retirar la fuente no toca lo propio ni lo de otras. Sin temporizador: el préstamo es procedencia. Consumidor real: F7) — F3/DJT-053 · `NatureRoll`+`NatureRollTable`+`NatureRollRegistry`+`NatureRollReloadListener` (asignación aleatoria con los pesos canónicos de las loot tables `*_rng`, data-driven y **validada al cargar**; solo tira, no aplica) + `EnumCodecs` (codecs estrictos compartidos) — F3/DJT-054 | permanente |
| `core.DomainEvents` | Holder del `DomainEventBus` compartido (server-side): emisores publican, `RankService`/`UnlockService` se suscriben en el arranque | permanente |
| `core.onboarding` | `OnboardingController` (máquina de estados de la creación del ninja `[S01]`: `NOT_STARTED→SELECTING_CLAN→SELECTING_AFFINITY→COMPLETED`, "finish" como transición con los rolls de `get_random_kg`; **el avance es solo del servidor**, payload de fase incorrecta = no-op; `refresh` en login arranca/reanuda/re-embarca) — F3/DJT-059 | permanente |
| `domain` | Dominio **puro** (sin MC): `resource`, `strain`, `mode`, `jutsu` (contrato de técnica + **`List<AffinityRequirement>`** (afinidad múltiple, F4/DJT-091) + **`KekkeiGenkaiRequirement`** (posesión de KG, `check_kekkei_genkai`, F4/DJT-091) + **`SkillRequirement`** F4/DJT-083: gate kenjutsu/medical), `progression`, `rng`, `event`, `nature` (`NatureType`, `KekkeiGenkai` — F3/DJT-050), **`eye`** (`EyeFamily`, `EyeRequirement`, `EyeEvolution` — nodos y aristas del grafo de ojos, F5/DJT-113), **`susanoo`** (`SusanooCostTable`+`SusanooTier` — tabla verificada de niveles 1..6, **coste decreciente** con la maestría y tarifa plana para el EMS; `SusanooMode` — el **primer** `ToggleableMode` con clientes; `SusanooContext` — costura que deja el ciclo testeable sin MC. F5/DJT-120), **`vision`** (`EntityRelationship` + `ChakraVisionRelations` — clasificación estática de la chakra vision por relación, de los 4 tags `chakra_highlights/*`. F5/DJT-125). Eventos: los 8 de F1 + `KekkeiGenkaiGrantedEvent` (F3/DJT-055) | permanente |
| `data` | `PlayerNinjaData` (agregado raíz) + sub-estados enchufables (`OnboardingState`, `ChakraState`, `ProgressionState`, `NatureProfile` con `TemporaryGrants` anidado, `ClanMembership`, **`JutsuKnowledge`** — known + progreso secuencial del learner (F4/DJT-075) + **cooldowns por-instancia** `jutsuId→tick de mundo`, poda perezosa, sin barrido por tick (F4/DJT-077), **`EyeState`** — ojo activo + flag `eternal` + `useLevels` por familia + `awakened` + eye strain (int, no `StrainMeter`) + habilidad seleccionada; F5/DJT-113) + `LeveledStats` (codec de `LeveledStat` compartido por habilidades y afinidades) + versionado/migración + `DirtyTracker`/`ObservableField` | permanente |
| `network` | `SyncNinjaDataPayload` (S2C, full/delta) + `ProgressionSnapshot` + `NinjaSync`; `MenuActionPayload` (C2S) + `handler/MenuActionHandler` (validación server-side); `OnboardingChoicePayload` (C2S, intenciones de la creación del ninja) + `handler/OnboardingChoiceHandler` — F3/DJT-059; `OnboardingOptionsPayload`+`OnboardingSync` (S2C, opciones de clan `(id, color)` — solo presentación, el servidor re-valida) — F3/DJT-060; **`CastJutsuPayload`** (C2S, tecla de cast: lleva **solo la mano**, el servidor lee el `JutsuComponent` del ítem real y valida `known` — el cliente no decide qué jutsu es) + `handler/CastJutsuHandler` — F4/DJT-080; **menú ocular (F5/DJT-117): `OpenEyeMenuPayload`+`SelectEyeAbilityPayload` (C2S) + `EyeMenuPayload` (S2C, vista) + `handler/EyeMenuHandler`** (request/response autocontenido: el servidor construye la vista desde `EyeState`+`EyeRegistry` y re-valida la selección; el cliente no decide); **`UseEyeAbilityPayload` (C2S, F5/DJT-118)** — lanzar la habilidad ocular seleccionada (el servidor lee la `selectedAbility` real) | permanente |
| `client` | **Solo cliente**: `ClientNinjaData` (+cooldowns observables, F4/DJT-081) + `ClientOnboardingOptions` + **`ClientEyeMenu`** (F5/DJT-117) (caches read-only), `keybind/KeyBindings` (+`CAST_JUTSU` V F4/DJT-080, +`OPEN_EYE_MENU` N F5/DJT-117), `screen/{NinjaMenuScreen, NinjaCreationScreen + NinjaCreationOpener, **AbilitySelectScreen** (menú de habilidad ocular: pide la vista al abrir, se reconstruye al refrescar — F5/DJT-117)}`, keybinds **N** (menú ocular) y **R** (usar habilidad, F5/DJT-118), `hud/*` (+`AbilityBarOverlay`: cd del jutsu equipado — F4/DJT-081) | permanente |
| `items` | `LearnerItem` + `LearnerComponent` (data component tipado del Skill Learner) — F3/DJT-055 · **`JutsuItem` + `JutsuComponent`** (la técnica como ítem; reemplaza `carrot_on_a_stick{JutsuItem,JutsuId}`; el cast por keybind es DJT-080) — F4/DJT-075 · **`WeaponItem` + `WeaponComponent`** (armas ninja: el daño va en el `ItemAttributeModifiers` **vanilla** del stack — reversible por construcción, corrige S07-R2; escala con kenjutsu) — F4/DJT-082; el resto del módulo llega en F8 | permanente |
| `clans` | `ClanDefinition`+`ClanRegistry`+`ClanReloadListener` (14 clanes **data-driven** con `randomNature` y `regenFactor`) — F3/DJT-056 · `ClanService.join` (bonus **una sola vez**, Namikaze determinista S04-R3, azar inyectado; registra el proveedor del regenFactor del clan) — F3/DJT-057 · `ClanService.reset` (defaults atómicos; corrige S04-R2) — F3/DJT-058 · `ClanJutsuUnlockService` (`PlayerTickSystem` en `UNLOCKS`: desbloqueo progresivo `d.<clan>_time`, umbral 78000 en datos, **una vez por tick** — corrige la doble ejecución de `clans/jutsu/main`; entrega en F4) — F3/DJT-061 · `ClanTeamService` (team/nametag por clan: `dojutsu_<clan>` namespaceado, color/sufijo de `create_teams` en datos, **no pisa teams del servidor**; `sync` en join/reset/login) — F3/DJT-062 | permanente |
| `entity` | **`NinjaProjectile`** (F4/DJT-070): proyectil reutilizable para 6 sistemas (jutsu/dojutsu/sage/throwing/summon/bijuu). Física propia **sin fricción** (fiel a los armor stands de velocidad constante del datapack; base `ThrowableItemProjectile` — el ctor de `Projectile` es package-private en el jar común), impacto por **raycast de segmento** (sin tunneling), dueño por **UUID** (`PlayerRef`), callback de impacto enchufable (las familias de DJT-076 se conectan aquí), gravedad **sincronizada**, vida máxima, **no persistente** · **`AbilityAnchor`** (F4/DJT-071): ancla de habilidad activa con tick-callback propio y **expiración garantizada por construcción** (vida máxima obligatoria; callback roto → descarte, no spam); base de F5/F6 · **`MarkerEntity`** (F4/DJT-071): marcador transitorio server-only (trackingRange 0, como el Marker vanilla). **EP-ENTITY completa** (el placeholder `DebugMarkerEntity` de F0 se retiró en DJT-072) | permanente |
| `util` | `ProjectileMath` (física pura del proyectil, testeable sin MC) + `RaycastHelper` (**costura de colisión**: los sistemas dependen del helper, no de `ProjectileUtil` vanilla — un cambio de semántica se hace en un punto) — F4/DJT-070 | permanente |
| `jutsu` | **`JutsuEntry`** (codec **estricto** que construye el `JutsuDefinition` de dominio (DJT-013) + `effectParams` en crudo — el esquema de parámetros lo define cada familia en DJT-076) + **`JutsuRegistry`** (id→entrada, por nombre — el índice numérico de `run_jutsu` no se porta) + **`JutsuReloadListener`** (`data/dojutsu/jutsu/**` con subcarpetas por naturaleza; id==nombre de archivo; aislamiento por archivo; un archivo roto jamás tumba el `/reload`) — F4/DJT-073 · **`LearnSequence{,Registry,ReloadListener}`** (secuencias de aprendizaje por elemento, `data/dojutsu/learner_sequences/`) + **`JutsuLearningService`** (aprendizaje **secuencial** fiel a `jutsu/learner/*`: posesión del elemento → "todos aprendidos" → cd 14t por jugador → entrega el siguiente y el **último consume el learner**; decisión pura `evaluate` testeable; contenido incremental: jutsu sin definición no avanza la secuencia; **dirección `items → jutsu → natures`, sin ciclos**) — F4/DJT-075 · **`JutsuCastPipeline`** (la secuencia canónica de todo cast: validar chakra→cd→**posesión de la naturaleza**→nivel de afinidad — la posesión es un hallazgo de `check_affinity`, regla 16 — → consumir **todo-o-nada** (incluida la disponibilidad del efecto: familia no implementada = sin coste) → cooldown (almacenamiento en DJT-077) → efecto con **aislamiento de fallos** → `JutsuCastEvent`. **Caster genérico**: decisión por `CastContext` y pago por `ChakraSink` — la IA de F8 enchufa los suyos. Único camino de cast del mod) + **`PlayerCastContext`** (adapter puro sobre `PlayerNinjaData`) + **`effect/*`**: las **6 familias del ADD** (una clase por familia, cero por técnica — RA-1): `ProjectileEffect` (primer cliente real de `NinjaProjectile`, con **aura de vuelo** que incendia y prende el suelo), `AreaEffect`, `BuffEffect`, `TeleportEffect`, `StructureEffect`, `HealEffect` (+`MobEffectSpec` compartido) — parametrizadas desde `effectParams` (records `Params` con parseo puro testeable) y registradas en código (`JutsuEffectRegistry`, extensible por F5+ sin tocar el pipeline). `canApply` valida "Nothing nearby" **antes de cobrar** (`NO_TARGET`). Familia **`primed_strike`** (golpe cargado del rayo — lightning_bolt/chidori/lightning_cutter/drilling: `PrimedStrikeEffect` arma al castear, `PrimedStrikeManager` descarga rayo+daño al golpear vía `LIVING_HURT`; F4/DJT-085d) · **`JutsuXpHandler`** (suscriptor de `JutsuCastEvent`: cada cast con afinidad da +1 XP a esa afinidad — bucle maestro usar→XP→nivel; la puerta es `NatureService.addAffinityXp`, **no** `XpApi`, para no cerrar el ciclo `progression↔natures`) — F4/DJT-078 · **`LastCastRegistry`** (último cast observable por caster: `CastRecord{jutsuId, dimensión, posición, expiración}`; suscriptor de `JutsuCastEvent`, consultado por proximidad — la copia del Sharingan de F5; **corrige S06-R6** el estado global del `d.sharingan_seen`: acotado por dimensión/distancia/tiempo, poda perezosa, `clear()` en `SERVER_STOPPING`) — F4/DJT-079. Kenjutsu (082+) llega en los siguientes tickets | permanente |
| `dojutsu` | **Sistema de ojos (F5).** `EyeDefinition`+`EyeRegistry`+`EyeReloadListener` (grafo de evolución **data-driven** desde `data/dojutsu/eyes/*.json`: familia, bonus de despertar, `awakenTicks`, habilidades, `evolutions` con `requirements` typados —`use_level`/`clan`/`flag`— y `eternalRequirements`; codec **estricto**, aislamiento por archivo) — F5/DJT-113 · **`AwakeningService`** (`PlayerTickSystem` en `TickPhase.DOJUTSU`: despierta el dojutsu del clan leyendo `potentialDojutsu` —no predicates `is_*`—, temporizador→umbral, 50/50 Otsutsuki con azar inyectado, bonus max_chakra/XP + `EyeAwakenedEvent`; decisión pura + glue) — F5/DJT-114 · **`EvolutionService`** (`PlayerTickSystem` en `TickPhase.DOJUTSU`: acumula el uso del ojo activo —solo si alguna arista lo exige; terminales no— y transiciona por la **primera arista** satisfecha —`USE_LEVEL` inclusivo, `CLAN` compuesto `otsutsuki_uchiha`, `FLAG` `hashirama_dna`—, bonus del destino + `EyeEvolvedEvent`; el EMS/eternal es acción manual, va al menú) — F5/DJT-115 · **`EyeStrainService`** (`PlayerTickSystem` en `TickPhase.STRAIN`: regen por pulso de 200t —1, o 3 con ADN Hashirama—; efectos 95→weakness/98→blindness/100→apaga, el Rinne Sharingan exento de ceguera/apagado; `addStrain` puerta de subida para las habilidades de DJT-118+; reset al dormir vía `SleepChakraHandler`; `regen`/`effectsFor` puros) — F5/DJT-116 · **`EyeMenuService`** (menú de habilidades: construye la vista `EyeState`+`EyeRegistry` y valida la selección contra el ojo activo real; puro) — F5/DJT-117 · **`ability/*`** (`EyeAbility` contrato + `EyeAbilityRegistry` en código + `EyeAbilityService` pipeline de cast —valida ojo/selección/cooldown → **`canApply`** (precondición "sin objetivo → Nothing nearby, sin cobrar", fiel al `no_cast_nby`; solo la overridean blood_eruption/exploding_human/genjutsu_stun) → consume chakra+fatiga → ejecuta— + piloto **`AmaterasuAbility`**: raycast → `AbilityAnchor` de fuego negro, 40 chakra/+10 strain, exento con ojo eterno; disparo por tecla R vía `UseEyeAbilityPayload`) — F5/DJT-118, abre EP-EYE-ABILITIES · **lotes 1-6 (F5/DJT-119, 19 habilidades)**: velocidad (`VacuumPalm`/`ShinraTensei`/`BanshoTenin`), Ketsuryugan (`BloodEruption`/`IronBloodFist` auto-buff/`RisingBloodSpear`/`ExplodingHuman`), genjutsu/teleport/melé (`Tsukuyomi`/`Amenotejikara`/`SixtyFourPalms`/`HundredTwentyEightPalms`), gravedad vertical (`ChibakuTensei` levitación) , proyectil/defensa/Tenseigan (`AsuraPath` misil `NinjaProjectile`/`PretaPath` absorción+resistencia/`TruthSeekingOrbs` HARM en el punto de mira) y Sharingan/canalización (`GenjutsuStun` stun duro slowness 255 gateado **solo por cooldown** —el `sharingan/cost` es goteo pasivo, no coste de habilidad—/`CopyJutsu` **aprende el jutsu visto** consumiendo `LastCastRegistry` [F4/DJT-079], único puente F5→F4, corrige S06-R6 porque el registro caduca/`KinrinTenseiBaku` canalizada con auto-raíz slowness+jump_boost 255) + `EyeAbility.cooldownTicks()` (default 0) con puerta de cooldown por (jugador, habilidad) en el pipeline + helper `EyeAbilityEffects` (objetivos/impulso [`hurtMarked` para valer sobre jugadores]/daño/`nearestLiving`; `MobEffects.HARM` invierte para no-muertos; costes y strain verificados). El estado del ojo es `data.state.EyeState` · **`susanoo/SusanooService`** (F5/DJT-120, **primer consumidor de `ToggleableMode`**: `PlayerTickSystem` en `TickPhase.DOJUTSU` que tickea un modo por jugador y traduce sus hooks a MC —chakra, fatiga, aura de `resistance`/`fire_resistance` amp `nivel-1` y `slowness` en 3-4—; lleva la maestría por uso con XP; `SusanooAbility` lo enciende desde el menú + R sin tecla nueva, coste 0 porque el chakra se paga **por pulsos**; el encendido es de ejecución, la maestría vive en `data.state.SusanooState`) · **`susanoo/SusanooAvatar`** (F5/DJT-121: la coraza visible es un **`AbilityAnchor`** —no un tipo de entidad nuevo: el ancla de F4/DJT-071 ya contemplaba el caso "mientras el modo esté activo"— que sigue al dueño, dibuja su silueta según la **etapa** y se descarta al apagar, al caer por chakra agotado y al desconectar; **corrige RA-3**, el datapack usaba un `armor_stand` `d.narr` con `knowledge_book{CustomModelData}` que quedaba colgado. **Etapa ≠ nivel**: la etapa dimensiona la silueta —escalera `advance` con tope en la maestría, decisión pura `decide`—, el nivel fija coste y protección. Los 7 modelos 3D por personaje siguen siendo trabajo de assets) · **`vision/ChakraVisionService`** (F5/DJT-125: chakra vision con **glow nativo + equipos de scoreboard**, no renderer propio ni selector por tick —RB-3—; `PlayerTickSystem` que reescanea cada 10t y marca las entidades con `GLOWING` + equipo de color; clasificación **estática** por relación; disponibilidad data-driven —`EyeDefinition.chakraVision`/`chakraVisionUseLevel`, Sharingan solo maduro uso≥216000, Ketsuryugan no—; tecla C → `ToggleVisionPayload` invierte `data.state.VisionState.chakraVisionOff`, persistente y activa por defecto) · **`tenseigan/TenseiganChakraModeService`** (F5 fleco: **2º consumidor de `ToggleableMode`** —el chakra mode del Tenseigan—: buffs [Fuerza II/Velocidad II/Salto IV/Resistencia II] + **vuelo** mientras se sostiene, pulso 10 chakra/200t, cae si chakra<19; se enciende desde el menú + R vía `TenseiganChakraModeAbility`, la 21ª y última del catálogo declarado. F6/EP-TOGGLE-FRAMEWORK unificará el bucle de tick de los modos) · **`EternalAwakeningService`** (F5 fleco: despertar del **EMS** como acción manual del menú ocular —botón "Awaken Eternal Mangekyo" → `AwakenEternalPayload`, server-side— que reemplaza las herramientas quirúrgicas del datapack; decisión pura `evaluate`: ojo eternalizable + no-eterno + clan `otsutsuki_uchiha` + uso≥864000 + medical≥150 → enciende `EyeState.eternal` + bonos +60 max_chakra/+150 xp/+78000 susanoo_use. El uso-gate lo acumula `EvolutionService` para ojos con `hasUseGatedEternal`) | permanente |
| `debug` | Comandos de depuración (`/dojutsuninja`, `/dojutsusched`, `/dojutsuchakra`, `/dojutsuprogression`, `/dojutsunatures`, `/dojutsuclans`, `/dojutsuonboarding`, `/dojutsuprojectile`, `/dojutsuanchor`, `/dojutsujutsu`, `/dojutsuweapon`, **`/dojutsudojutsu`** F5: activar ojo / fijar uso / strain / eternal para QA) | **temporal** |

### Adaptadores por loader

| NeoForge (`com.zamu.dojutsu.neoforge`) | Fabric (`com.zamu.dojutsu.fabric`) |
|----------------------------------------|-------------------------------------|
| `DojutsuNeoForge` (`@Mod`) | `DojutsuFabric` (main), `DojutsuFabricClient` (client) |
| `DojutsuNeoForgeServerEvents` (eventos nativos server: `PlayerWakeUpEvent`, `LivingEntityUseItemEvent.Finish`) | `DojutsuFabricServerEvents` (fabric-api: `EntitySleepEvents.STOP_SLEEPING`) + `mixin.LivingEntityMixin` (`completeUsingItem`, comer) |
| `client.DojutsuNeoForgeClientEvents` (renderers) | — (renderers en el client initializer) |
| `platform.NeoForge{Registrar,Attachment,Network}Service` (+ handles/registrar) | `platform.Fabric{Registrar,Attachment,Network}Service` (+ handles/registrar, `FabricClientNetworking`) |

## 4. Dependencias entre capas

```
        registry ──► platform ◄── core
           │            ▲            │
           └────────────┼───────────┘
                        │
                   DojutsuMod (orquesta init)
                        │
        ┌───────────────┴────────────────┐
        ▼                                 ▼
   neoforge (impl puertos)          fabric (impl puertos)
```

Regla: `common` **nunca** importa clases de loader; solo depende de los **puertos**. Los puertos se
implementan en `neoforge`/`fabric` y se resuelven por `ServiceLoader`.

## 5. Los puertos de plataforma

| Puerto | API (resumen) | NeoForge | Fabric |
|--------|---------------|----------|--------|
| `RegistrarService` | `create(registryKey)` → `Registrar<T>` (`register(id, factory)`, `commit()`) | `DeferredRegister` + bus (`EventBusesHooks`) | `Registry.register` inmediato |
| `AttachmentService` | `register(id, default, Codec, copyOnDeath)` → `AttachmentHandle<T>` (`get`/`set`) | `AttachmentType` + `DeferredRegister` sobre `ATTACHMENT_TYPES` | `AttachmentRegistry.create`; acceso via cast a `AttachmentTarget` |
| `NetworkService` | `register(Side, type, codec, handler)`, `sendToServer`, `sendToPlayer` | `RegisterPayloadHandlersEvent` + `PayloadRegistrar` + `PacketDistributor` | `PayloadTypeRegistry` + `ServerPlayNetworking`/`ClientPlayNetworking` (client aislado) |

`PlatformServices` (common) expone: `REGISTRAR`, `ATTACHMENTS`, `NETWORK`
(los dos primeros por `ServiceLoader.load(...)`; `NETWORK` también por `ServiceLoader`).

## 6. Flujo del sistema (ciclo de vida actual)

```
Arranque del mod
  DojutsuMod.init()  [common, ambos loaders]
    ├─ Mod*.register()  → usan PlatformServices.REGISTRAR / ATTACHMENTS
    ├─ DojutsuLifecycle.init()
    │     ├─ config + payloads (NinjaSync S2C, MenuActionHandler C2S)
    │     ├─ ServerTickDispatcher(pipeline, scheduler).register()
    │     │     └─ pipeline.register(new RegenService())   [fase CHAKRA = primero]
    │     ├─ reload listeners: DataReloadListener, UnlockReloadListener
    │     ├─ suscriptores del bus de dominio: RankService, UnlockService
    │     └─ hooks: join/quit, dimensión, LIVING_DEATH (DeathHandler + SkillPointService), respawn, level load
    ├─ AdminCommands + comandos de debug (temporales)
    └─ [adaptador] Dojutsu<Loader>ServerEvents.register()  → eventos nativos (despertar, comer)

  DojutsuMod.initClient()  [solo cliente]
    ├─ ClientNinjaData.clear() al salir del mundo
    ├─ KeyBindings.register()   → M abre NinjaMenuScreen
    └─ HudOverlays.register()   → barra de chakra + rango

Tick del servidor (por jugador)
  RegenService (fase CHAKRA) → ChakraService.advanceRegen → [pulso] restore + NinjaSync.sendDelta

Eventos discretos (reactivo, sin polling)
  kill      → SkillPointService.onKill → award + sendDelta
  comer     → FoodChakraHandler  → ChakraService.restore + sendDelta
  despertar → SleepChakraHandler → ChakraService.restoreFull + sendDelta
  muerte    → DeathHandler → chakra = max/2 (copyOnDeath → respawn → RespawnHandler.sendFull)
  XP        → XpApi → LevelingService → [multi-nivel] SkillLevelChangedEvent → UnlockService
                                     └→ NinjaXpChangedEvent → RankService (aviso de rango)

Cliente → Servidor (autoridad del servidor)
  NinjaMenuScreen "+" → MenuActionPayload (C2S)
     → MenuActionHandler: valida (emisor, skill por nombre, cantidad, rate-limit, puntos/cap)
        → SkillPointService.spend → XpApi.addNinjaXp → NinjaSync.sendDelta → ClientNinjaData → UI/HUD
```

## 7. Qué falta por construir (según el ADD, aún no en código)

**Completados:** F0 (`platform`, `registry`), F1 (`domain`, `data`, `core`, `network`/`client` base),
F2 (`chakra`, `progression`, `client` GUI/HUD, C2S validado), **F3** (`natures`, `items` learner, `clans`,
`core.onboarding` + `client` creación — ver `docs/handoff_phase3.md`): **EP-NATURES** (DJT-050…055: estado,
servicios, 18 KG data-driven, préstamos, RNG canónico y Skill Learner), **EP-CLANS** (DJT-056/057/058/061/062:
14 clanes data-driven, join con bonus una vez, reset atómico, desbloqueo progresivo por tick y team/nametag;
factores de regen de F2 enchufados) y **EP-ONBOARDING** (DJT-059/060: máquina de estados del servidor +
`NinjaCreationScreen` con clan/afinidad aleatorios).

**En curso (F4):** **EP-ENTITY completa** (DJT-070/071/072); en EP-JUTSU-ENGINE existen la carga data-driven
(DJT-073), el aprendizaje (DJT-075), el pipeline (DJT-074) y las 6 familias de efecto (DJT-076) , los
cooldowns por-instancia (077), la XP de afinidad (078), el registro de último cast (079), la tecla de cast
validada (080) y el HUD de cd (081) → **EP-JUTSU-ENGINE completa** (la familia `primed_strike` se implementó en DJT-085d). En
**EP-KENJUTSU completa**: daño reversible por arma (`WeaponAttributeService` sobre `ItemAttributeModifiers`
vanilla, DJT-082), técnicas de espada (jutsus categoría kenjutsu con `SkillRequirement`, DJT-083) y la Samehada
(`SamehadaService`, absorción de chakra en `LIVING_HURT`, DJT-084). **El motor de combate de F4 está cerrado**;
solo queda el **contenido** (EP-JUTSU-CONTENT): **Fuego y Rayo volcados** (DJT-085, 24 jutsus totales, RA-1
validado a escala solo con datos) con **assets portados** (DJT-085b/c: jutsus, armas y learner vía
`custom_model_data`) y la familia **`primed_strike`** implementada (DJT-085d); faltan el resto de naturalezas
(DJT-086+) y los cloaks (empowerment→F6). El puerto
`AttributeService` **sigue diferido**: DJT-082 optó por el component vanilla (reversible por construcción).
**Aún no existen:** `throwing` (F4 tardía); `dojutsu`, `sage`, `empowerment`, `bijuu`, `summoning`, `ai`,
`trading`, `story`, `world` (F5–F9); `api`. El módulo `items` **nace en F3** (DJT-055, solo el ítem learner)
pese a estar situado en F5–F9 en el ADD; el resto de `items` sigue en F8.

El puerto **`AttributeService` se difiere** (F2 no lo exigió: `maxChakra` vive como campo de `ChakraState`)
hasta que un sistema necesite **modificadores de atributo vanilla** (previsiblemente kenjutsu/puertas, F4/F6).

## 8. Patrones establecidos (reutilizar en F3+)

| Patrón | Dónde se estableció | Cómo se reutiliza |
|--------|---------------------|-------------------|
| **Sub-estado enchufable** | `OnboardingState` (F1), `ChakraState`/`ProgressionState` (F2), `NatureProfile` (F3) | Value object `DEFAULT`+`CODEC` → campo + getter + **puerta de escritura** + 1 línea de codec `optionalFieldOf`. Sin bumpear `dataVersion`. Ya aplicado 4 veces; F3 lo repite con `TemporaryGrants` (DJT-053) y `ClanMembership` (DJT-056) |
| **Derivado, nunca cacheado** | `ProgressionState.rank()` (F2), `NatureProfile.affinity(YIN_YANG)` (F3) | Si un valor se puede calcular de otro estado, **no se persiste ni se sincroniza**: se deriva al leerlo (y su puerta de escritura lanza). Imposible de desincronizar |
| **Servicio puro + glue MC** | `ChakraService`/`RegenService`, `SkillPointService`/`onKill`, `LevelingService`/`XpApi` | La mutación es pura sobre `PlayerNinjaData` (testeable sin MC); una clase aparte conoce `ServerPlayer`/eventos |
| **Única puerta de escritura** | `ChakraService` (chakra), `XpApi` (XP) | Clamp, invariantes y eventos en un solo sitio; nadie escribe el campo directamente |
| **Reactivo por bus de dominio** | `DomainEvents` + `RankService`/`UnlockService` (F2) | El emisor publica; los reactivos se suscriben en el arranque. **Nunca polling por tick** |
| **Carga data-driven** | `UnlockReloadListener` + `UnlockRegistry` + JSON (F2); `KekkeiGenkaiReloadListener` + `KekkeiGenkaiRegistry` + 18 JSON (F3) | `SimpleJsonResourceReloadListener` + Codec + registry. **Codec estricto** en contenido (una ref rota falla y se ve en el log, aislada a su archivo) frente al codec **tolerante** de `data.state` (un save corrupto no debe impedir jugar). F3/F4/F5: clanes, jutsus, ojos |
| **Evento loader-nativo** | `Dojutsu<Loader>ServerEvents` (F2) | Si Architectury no lo expone: el **adaptador** suscribe y delega en un handler común ("push"). Fabric tiene mixins montados |
| **Acción de cliente validada** | `MenuActionPayload` + `MenuActionHandler` (F2) | El cliente envía **intención**; el servidor valida (emisor, id **por nombre**, rango, rate-limit) y sincroniza |
| **Sync observable** | `ObservableField` + `DirtyTracker` + `SyncNinjaDataPayload` | Solo lo observable cruza la red; lo derivable se calcula en cliente (p. ej. el rango) |
