# Registro de desarrollo — Dojutsu Port

> Historial técnico cronológico. Cada entrada = un ticket. F0 = andamiaje; F1 = núcleo de dominio y datos.
> Fechas reales de implementación: 2026-07-12 (DJT-001–007) y 2026-07-13 (DJT-008).

---

## F0 · DJT-001 — Estructura Gradle multiloader (Architectury)

- **Qué:** esqueleto Gradle `common`/`neoforge`/`fabric` compilando en vacío + Gradle wrapper.
- **Archivos:** `settings.gradle`, `build.gradle` (raíz), `common/build.gradle`,
  `fabric/build.gradle`, `neoforge/build.gradle`, `gradle.properties`, `neoforge/gradle.properties`,
  `common/src/main/resources/architectury.common.json`, `.gitignore`, `gradle/wrapper/*`.
- **Motivo:** base multiloader (ADR-001 del ADD): 1 lógica común + adaptadores finos por loader.
- **Decisiones técnicas:**
  - Groovy DSL (no Kotlin): es la forma más documentada de la plantilla Architectury → menor riesgo.
  - Fuente única de versiones en `gradle.properties`.
  - Mappings **Mojmap** unificados en los 3 subproyectos (prohibido mezclar con Yarn).
  - Toolchain Java 21 fijada (falla claro si el JDK no es 21).
- **Problemas y resolución (matriz de versiones, riesgo RM-6):**
  1. `Array must have size 1, but has size 0` → causas: `fabric-loader 0.19.3` demasiado nuevo para
     loom 1.7, y `architectury.common.json` con `"accessWidener": []`. **Fix:** loader → `0.16.10`,
     json → `{}`.
  2. `Could not find method neoForge()` → Loom lee la plataforma al aplicar el plugin. **Fix:**
     `neoforge/gradle.properties` con `loom.platform=neoforge` (Fabric no lo necesita).
  3. `ClassNotFound: net.minecraftforge.binarypatcher.ConsoleTool` → loom `1.6.x` demasiado viejo
     para neoforge `21.1.235`. **Fix:** loom → `1.7.435`.
- **Versiones conocidas-buenas fijadas:** loom `1.7.435`, architectury-plugin `3.4.164`,
  architectury API `13.0.8`, fabric-loader `0.16.10`, fabric-api `0.116.13+1.21.1`,
  neoforge `21.1.235`, Gradle wrapper `8.10.2`.
- **Resultado:** `./gradlew build` verde en los 3 subproyectos.

---

## F0 · DJT-002 — Entrypoints y `MOD_ID`

- **Qué:** entrypoint común `DojutsuMod` (`MOD_ID`, `init()`, `initClient()`) + adaptadores por loader.
- **Archivos:** `common/.../DojutsuMod.java`; `fabric/.../DojutsuFabric.java`, `DojutsuFabricClient.java`,
  `fabric.mod.json`; `neoforge/.../DojutsuNeoForge.java`, `META-INF/neoforge.mods.toml`.
  `gradle.properties` (maven_group/mod_author confirmados por el usuario: `com.zamu.dojutsu` / `Zamu`).
- **Motivo:** que el mod se cargue y se separe la inicialización cliente/servidor.
- **Decisiones técnicas:**
  - `initClient()` solo se invoca en cliente: Fabric via entrypoint `client`; NeoForge via
    `FMLEnvironment.dist == Dist.CLIENT`.
  - **NeoForge:** el constructor `@Mod` **no** registra el mod event bus. En Architectury 13.x la
    API pública `EventBuses.registerModEventBus` fue eliminada; Architectury obtiene el bus desde
    `ModList` (`EventBusesHooks`). (Verificado inspeccionando el jar de architectury-neoforge.)
- **Problema:** import a `dev.architectury.platform.forge.EventBuses` (inexistente en 13.0.8) →
  fallo de compilación. **Fix:** quitarlo; constructor sin argumentos.
- **Resultado:** validado en runtime — servidor dedicado de Fabric y NeoForge cargan el mod
  (`Inicializacion comun completada`) y **no** ejecutan `initClient()`.

---

## F0 · DJT-003 — Puerto `RegistrarService`

- **Qué:** puerto de registro diferido cross-loader + fachada `PlatformServices`.
- **Archivos (common):** `platform/PlatformServices.java`, `RegistrarService.java`, `Registrar.java`,
  `RegistryEntry.java`, `SimpleRegistryEntry.java`. **(neoforge/fabric):** `*RegistrarService.java`,
  `*Registrar.java` + `META-INF/services/com.zamu.dojutsu.platform.RegistrarService`.
- **Motivo:** aislar el mecanismo de registro del loader (DIP / hexagonal).
- **Decisiones técnicas:**
  - Resolución por **`ServiceLoader`** (patrón `Services` de Architectury): `PlatformServices` carga
    la impl de cada loader desde `META-INF/services/*`.
  - Impl **nativa por loader**: NeoForge usa `DeferredRegister` (+ bus via `EventBusesHooks`);
    Fabric registra inmediato con `Registry.register` sobre `BuiltInRegistries.REGISTRY.get(key.location())`.
  - `commit()`: NeoForge engancha el `DeferredRegister` al bus; Fabric es no-op.
- **Auto-revisión:** las dos clases anónimas `RegistryEntry` idénticas se colapsaron en
  `SimpleRegistryEntry` (DRY).
- **Resultado:** compila; ficheros de servicio correctamente empaquetados. Validación end-to-end
  diferida a DJT-006 (así lo indica el ticket).

---

## F0 · DJT-004 — Puerto `AttachmentService` + dummy (RM-6)

- **Qué:** puerto de Data Attachments por entidad + attachment dummy + comando de prueba.
- **Archivos (common):** `platform/AttachmentService.java`, `AttachmentHandle.java`;
  `debug/DummyAttachment.java`, `DummyAttachmentCommand.java`. **(por loader):**
  `*AttachmentService.java`, `*AttachmentHandle.java` + service files.
- **Motivo:** ticket de desriesgue clave de F0 (la API de attachments es la que más diverge).
- **Decisiones técnicas:**
  - API: `register(id, defaultSupplier, Codec, copyOnDeath)` → `AttachmentHandle<T>` (`get`/`set`).
  - NeoForge: `DeferredRegister` sobre `NeoForgeRegistries.ATTACHMENT_TYPES` +
    `AttachmentType.builder(...).serialize(codec)[.copyOnDeath()]`; acceso `entity.getData/setData`.
  - Fabric: `AttachmentRegistry.create(id, b -> b.initializer(...).persistent(codec)[.copyOnDeath()])`;
    acceso `((AttachmentTarget) entity).getAttachedOrCreate/setAttached` (cast obligatorio: la interfaz
    se inyecta por mixin).
  - Comando `/dojutsudummy get|set` registrado con `CommandRegistrationEvent` (cross-loader) de Architectury.
- **Problema/validación:** se creó un self-test temporal (`SERVER_STARTED` → set/get sobre entidad
  efímera) que dio **OK en ambos loaders**; luego se **retiró** para no ejecutarse en cada arranque.
- **Política de copia documentada:** persistencia en disco via Codec en ambos loaders; copia en
  respawn solo con `copyOnDeath`. El usuario validó save/relogin/respawn/dimensión con el comando.

---

## F0 · DJT-005 — Puerto `NetworkService` + ping-pong

- **Qué:** puerto de red C2S/S2C sobre `CustomPacketPayload` de 1.21 + ping-pong dummy.
- **Archivos (common):** `platform/NetworkService.java`; `network/PingPayload.java`, `PongPayload.java`;
  `debug/PingPongDebug.java`, `PingPongClientDebug.java`. **(por loader):** `*NetworkService.java`
  (+ `fabric/.../FabricClientNetworking.java`) + service files.
- **Motivo:** base de la sincronización cliente↔servidor.
- **Decisión técnica CLAVE (pivote):**
  - Empecé respaldando el puerto con `NetworkManager` de Architectury, pero al **probar en servidor**
    apareció un `AbstractMethodError` en Fabric (`NetworkManagerImpl$1 does not define registerS2C`) —
    **bug de architectury 13.0.8** (issue architectury-api #518, sin parche upstream).
  - **Pivote a impl nativa por loader** (ServiceLoader): NeoForge `RegisterPayloadHandlersEvent` +
    `PayloadRegistrar.playToServer/playToClient` + `PacketDistributor`; Fabric `PayloadTypeRegistry`
    + `ServerPlayNetworking` con el receptor S2C / envío C2S (client-only) aislados en
    `FabricClientNetworking` bajo comprobación de entorno.
  - Esto además cumple mejor el ticket ("Implementar por loader") y elimina la dependencia del
    networking roto de Architectury.
- **Resultado:** ambos servidores dedicados arrancan sin crash; el usuario confirmó el **round-trip
  completo** en runtime (`Enviando Ping`→`Ping recibido`→`Pong recibido`, nonce=7).

---

## F0 · DJT-006 — Registro base placeholder

- **Qué:** clases `Mod*` con un placeholder por tipo, ejercitando el `RegistrarService`.
- **Archivos (common):** `registry/ModItems.java`, `ModEntities.java`, `ModAttributes.java`,
  `ModSounds.java`, `ModParticles.java`, `ModAttachments.java`; `entity/DebugMarkerEntity.java`.
  **Modificado:** `DojutsuMod` (helper `id(path)` + wiring en `init()`).
  **(fix crash):** `fabric/.../DojutsuFabricClient.java`, `neoforge/.../client/DojutsuNeoForgeClientEvents.java`.
- **Placeholders:** item `ninja_debug`, `EntityType debug_marker`, atributo `max_chakra`
  (`RangedAttribute`), sonido/partícula debug, attachment `placeholder`.
- **Problema encontrado (crash):** al probar `/summon dojutsu:debug_marker`, el **cliente crasheó**
  (`entityRenderer is null`) y el mundo quedó inabrible (crash al recargar la entidad guardada).
  Causa: toda `EntityType` registrada necesita un renderer en cliente.
- **Resolución:** registrar un **`NoopRenderer`** (invisible, como el `Marker` vanilla) por loader:
  Fabric `EntityRendererRegistry.register(...)` en el client initializer; NeoForge clase
  `@EventBusSubscriber(..., Dist.CLIENT)` con `EntityRenderersEvent.RegisterRenderers`. Esto recuperó
  el mundo del usuario.
- **Notas:** el `ERROR: No data fixer registered for debug_marker` es **benigno** (MC lo emite para
  toda entidad modeada sin datafixer). **Deferido:** registrar el atributo no lo **adjunta** a
  `Player` — el attach cross-loader necesita un futuro puerto `AttributeService`.

---

## F0 · DJT-007 — Reload listener del datapack

- **Qué:** listener de datos de servidor que hoy solo loguea (F4/F5 cargará definiciones data-driven).
- **Archivos (common):** `core/DataReloadListener.java`; `resources/data/dojutsu/.gitkeep`.
  **Modificado:** `DojutsuMod.init()`.
- **Decisión técnica:** `DataReloadListener extends SimplePreparableReloadListener<Void>`, registrado
  con `ReloadListenerRegistry.register(PackType.SERVER_DATA, ..., id("data_reload"))` (Architectury,
  cross-loader — sus *registries* sí funcionan; solo su *networking* estaba roto).
- **Resultado:** `apply()` corre al cargar el mundo (y en `/reload`) en ambos loaders.

---

## F0 · DJT-008 — CI + repositorio (cierre de F0)

- **Qué:** pipeline de CI y publicación del proyecto en GitHub.
- **Archivos:** `.github/workflows/ci.yml`, `.gitattributes`; repo git inicializado y subido.
- **Decisiones técnicas:**
  - Workflow en `ubuntu-latest`: `setup-java@v4` (Temurin 21) + `gradle/actions/setup-gradle@v4`
    (caché) + `chmod +x gradlew` + `./gradlew build test`.
  - `.gitattributes` con `gradlew text eol=lf` (imprescindible: gradlew debe ser LF para ejecutarse
    en el runner Linux).
  - `.gitignore` excluye `build/`, `run/`, `.gradle/`, `.idea/`.
- **Repo:** https://github.com/Z4MU/Dojutsu-Port.git (`origin`, rama `main`). Commit inicial de toda
  la F0 (61 archivos versionados).
- **Resultado:** CI **verde** en runner limpio (build ~4 min). Fase 0 cerrada.

---

# Fase 1 — Núcleo de dominio y datos

---

## F1 · DJT-010 — `Resource` + `RegenPolicy` + `RegenFactor` (dominio de recurso)

- **Qué:** primer módulo de dominio puro (sin Minecraft, con TDD): recurso `current/max` con clamp y la
  política de regeneración *timer→pulso* del chakra, reproduciendo la tabla exacta de `[S02]`.
- **Archivos:** `common/.../domain/resource/Resource.java`, `RegenFactor.java`, `RegenPolicy.java`;
  tests `common/src/test/.../domain/resource/ResourceTest.java`, `RegenPolicyTest.java`.
  **Modificado (infra de tests, inexistente hasta hoy):** `common/build.gradle` (JUnit 5 vía BOM,
  `mavenCentral`, `useJUnitPlatform`), `gradle.properties` (`junit_version=5.10.2`).
- **Motivo:** raíz del grafo de dependencias de F1 (DJT-012/013 dependen de `Resource`); desriesga RA-4
  (correctitud de la fórmula de regen). `[ADD/00 §1 patrón 4]`, `[ADD/10 chakra "fórmula"]`, `[ADD/03 §2.1]`.
- **Decisiones técnicas:**
  - `Resource`: clamp `[0,max]` en cada operación; `consume` "todo o nada"; `add` clampa el overshoot
    (S02-R4: el cap se aplica siempre tras la regen); valores negativos rechazados.
  - `RegenPolicy`: umbral+incremento inmutables, timer transitorio. `resolve(Set<RegenFactor>)` mapea a
    las 12 celdas de `[S02]` (modo × clase): estándar 120/+3; acelerada 60/+{4,6,8}; sage1 60/30;
    sage2 30/15. Tasa efectiva `chakra/seg = incremento × (20/umbral)`.
  - `RegenFactor` (enum): factores acumulables (clan Otsutsuki/Uzumaki, ADN Hashirama, modo sabio 1/2).
    La derivación desde el estado real del jugador es del módulo `chakra` (F2).
  - **Decisión de diseño anotada (a confirmar):** `[S02]` no especifica precedencia cuando concurren
    varios factores → se elige "más fuerte gana" (Hashirama > Uzumaki > Otsutsuki) y "modo más alto gana"
    (Sage 2 > Sage 1), coherente con "mejor clan/DNA y mayor modo → regen más veloz".
- **Infra:** se introduce la carpeta `common/src/test` y JUnit 5. El task `test` de CI deja de ser trivial.
- **Validación:** `./gradlew :common:test` → **19 tests verdes** (11 `Resource` + 8 `RegenPolicy`), 0
  fallos. Dominio puro: sin arrancar Minecraft, sin validación por loader (los unit tests son el DoD).
- **Resultado:** contrato de recurso/regen estable y verificado con los números exactos del datapack.

---

## F1 · DJT-011 — `StrainMeter` + `StrainThreshold` (fatiga con umbrales)

- **Qué:** medidor de fatiga puro `0..100` con umbrales escalonados, compartido por eye strain `[S05]`
  y eight-gates strain `[S11]`. Notifica solo al cruzar un umbral (anti-spam) y es agnóstico del efecto.
- **Archivos:** `common/.../domain/strain/StrainThreshold.java`, `StrainListener.java`, `StrainMeter.java`;
  test `common/src/test/.../domain/strain/StrainMeterTest.java`.
- **Motivo:** patrón 3 del ADD; base de S05/S11 (y futuros S24, etc.). `[ADD/00 §1 patrón 3]`, `[S05]`, `[S11]`.
- **Decisiones técnicas:**
  - Umbrales canónicos `25/50/75/90/95/98/100` (`StrainThreshold` con `percent()`). El datapack avisa en
    25/50/75/90/99 y aplica efectos en 95/98/100; el ticket unifica el set en los 7 cortes.
  - `add`/`reduce` clampan a `[0,100]` y disparan `StrainListener.onThresholdCrossed(t, rising)` **una vez
    por cruce**, en orden ascendente al subir / descendente al bajar (rearme). Equivale a las tags anti-spam
    `d.esw_*`/`d.egsw_*`.
  - `reset()` pone a 0 **sin notificar** (dormir, `[S02]`): es un reset de estado, no un cruce.
  - Meter **agnóstico del castigo** (RM-3): weakness/blindness/apagado los aplica el consumidor via listener.
  - `[S05]/[S11]` no definen histéresis → rearme en el mismo umbral (subir a ≥T notifica; bajar a <T rearma).
- **Validación:** `./gradlew :common:test` → **11 tests** de `StrainMeter` verdes (30 en total con DJT-010).
- **Resultado:** contrato de fatiga estable; los consumidores de F5+ enganchan efectos sin tocar el meter.

---

## F1 · DJT-012 — `ToggleableMode` + `ModeContext` (modo activable con coste/castigo)

- **Qué:** framework abstracto activate→run→cost→punish→deactivate con hooks para los matices; base de
  sage (S08), sellos (S10), puertas (S11), SOAH (S12), susanoo (S24) y formas (S09). Solo el contrato.
- **Archivos:** `common/.../domain/mode/ModeContext.java`, `ToggleableMode.java`; test
  `common/src/test/.../domain/mode/ToggleableModeTest.java`.
- **Motivo:** patrón 2 del ADD; colapsa el toggle+coste+castigo compartido por 7 sistemas. `[ADD/00 §1 patrón 2]`,
  `[ADD/10 empowerment]`, `[ADD/08 RM-3]`.
- **Decisiones técnicas:**
  - **Genérico** `ToggleableMode<C extends ModeContext>`: cada modo declara su contexto → sin casts ni
    fugas entre modos (coherente con "caster genérico"). `ModeContext` es la costura (interfaz marcador)
    que el consumidor especializa; no asume jugador (un NPC podría portar un modo).
  - Métodos de ciclo `final` (`activate/deactivate/toggle/tick`), estado `active` encapsulado; los 5 hooks
    (`onActivate` con veto, `onTick`, `onCost`, `onPunish`, `onDeactivate`) por defecto no-op/gratis.
  - **Único automatismo del framework:** en `tick`, si `onCost` no puede pagar → `onPunish` + `deactivate`.
    Los demás matices (letal 8ª puerta, inyección probabilística, tiers SOAH, castigos por umbral de strain)
    los dispara el modo desde sus hooks (p. ej. `onTick` consultando su `StrainMeter`), sin acoplarlos aquí.
  - El coste consume `Resource` (DJT-010) **solo en el modo concreto/test**; el framework lo ignora.
- **Validación:** `./gradlew :common:test` → **9 tests** de `ToggleableMode` verdes (39 en total).
- **Resultado:** contrato de modo activable estable; RM-3 desriesgado (hooks suficientes para los matices).

---

## F1 · DJT-013 — `Jutsu` + `JutsuDefinition` + `CastContext` + `CastResult` (contrato de técnica)

- **Qué:** contrato de técnica (coste, cooldown, afinidad req, nivel req, categoría, ref de efecto) y la
  máquina de precondiciones fiel a `[S06]`, con caster genérico y resultado tipado. Sin efectos (eso es F4).
- **Archivos:** `common/.../domain/jutsu/`: `CastFailureReason.java`, `CastResult.java`, `CastContext.java`,
  `AffinityRequirement.java`, `JutsuDefinition.java`, `Jutsu.java`; test `.../domain/jutsu/JutsuTest.java`.
- **Motivo:** patrón 1 del ADD; contrato estable para ~200 técnicas data-driven (RA-1). `[ADD/00 §1 patrón 1]`,
  `[ADD/10 jutsu]`, `[ADD/03 §5]`.
- **Decisiones técnicas:**
  - `JutsuDefinition` es un `record` inmutable con validación; **efecto, categoría y afinidad se referencian
    por id data-driven (String)** — el efecto NO va en la definición (es `effectId`, lo resuelve F4). Así
    añadir técnicas = añadir datos, sin tocar el contrato (RA-1).
  - `CastContext` interfaz con **caster genérico** (`availableChakra`/`isOnCooldown`/`affinity(natureId)`),
    no tipa `Player` → un NPC IA la implementa (S17-R3). La afinidad se consulta por id de naturaleza (el
    módulo `natures` es F3; el dominio no lo conoce aún).
  - Máquina de precondiciones como `default` de `Jutsu.evaluatePreconditions`, en el orden del datapack
    **chakra → cooldown → afinidad**; `CastResult` con razones tipadas (`CastFailureReason`). F4 reutiliza
    el chequeo y añade el efecto en `cast()`.
  - `levelRequirement` es puerta de **aprendizaje** (S06 learner), no del cast → queda como dato de la
    definición pero **fuera** de `evaluatePreconditions` (fiel a `<jutsu>/cast`).
  - Técnica universal = `affinityRequirement` vacío (`Optional.empty()`), no amount 0.
- **Validación:** `./gradlew :common:test` → **9 tests** de `Jutsu` verdes (48 en total).
- **Resultado:** contrato de técnica estable; RA-1 desriesgado.

---

## F1 · DJT-014 — `LeveledStat` + `XpCurve` + `NinjaRank` (progresión) · #riesgo-alto RA-4

- **Qué:** curva de XP por bandas (cap 500) y rango ninja 0..11, con los números exactos de `[S13]`/`[S20]`.
  TDD obligatorio (un error de curva rompe el balance global, RA-4 alto).
- **Archivos:** `common/.../domain/progression/`: `XpCurve.java`, `LeveledStat.java`, `NinjaRank.java`;
  tests `common/src/test/.../domain/progression/`: `XpCurveTest.java`, `LeveledStatTest.java`, `NinjaRankTest.java`.
- **Motivo:** patrón 5 del ADD; reemplaza ~600 predicates de nivelación y 12 de rango. `[ADD/00 §1 patrón 5]`,
  `[ADD/10 domain/progression]`, `[ADD/03 §5]`.
- **Decisiones técnicas:**
  - `XpCurve.xpForLevel(level)` = 9 bandas de `[S13]` (0-10:10, 11-20:15, 21-40:20, 41-80:30, 81-90:50,
    91-99:60, 100-299:80, 300-499:100, 500:120), `MAX_LEVEL=500`. Todas las fronteras verificadas.
  - `LeveledStat.addXp` **elimina la guardia `d.dolvl`** (1 level-up/tick) del datapack: sube todos los
    niveles que correspondan en una llamada y **arrastra** el sobrante (el datapack ponía `xp=0`, lo que
    impedía el salto multinivel). En el cap 500 no acumula y descarta sobrante.
  - `NinjaRank` (enum de 12) con `minXp` y `title`; `fromXp` mapea por umbrales de `[S20]`; `ordinal()` =
    `d.ninja_rank`. Títulos romanizados sin macrones (Chunin/Jonin) por consistencia de codificación.
  - `XpCurve` (per-afinidad/habilidad) y `NinjaRank` (desde `ninja_xp` global) son ejes independientes.
- **Incidencia (test propio, no del código):** una expectativa de test confundió la banda 0-10 (todos
  cuestan 10) con coste 15 en el nivel 1; corregida. El `xpForLevel` era fiel a `[S13]`. Reafirma el valor
  del TDD por frontera en RA-4.
- **Validación:** `./gradlew :common:test` → **55 tests** de progresión verdes (103 en total).
- **Resultado:** curva y rango reproducen exactamente `[S13]`/`[S20]`; RA-4 desriesgado.

---

## F1 · DJT-015 — `RollTable` + `WeightedEntry` (RNG por pesos)

- **Qué:** tabla de pesos pura que reemplaza las loot-tables-como-RNG (`*_rng`) del datapack, con RNG
  inyectado y determinista (semilla fija en tests).
- **Archivos:** `common/.../domain/rng/WeightedEntry.java`, `RollTable.java`; test
  `common/src/test/.../domain/rng/RollTableTest.java`.
- **Motivo:** `[ADD/00 §1]`, `[ADD/03 §5]`, `[ADD/10 domain/rng]`. Base de S03/S04/S17/S18.
- **Decisiones técnicas:**
  - **Conflicto ficha↔arquitectura resuelto:** la ficha dice "usa `RandomSource`", pero es una clase de
    Minecraft y el dominio exige cero imports de MC. Se usa `java.util.random.RandomGenerator` (JDK),
    inyectado en `roll(...)`. Cumple la intención (inyección + determinismo, sin `Math.random()`) y mantiene
    la pureza. El consumidor (F2+) adaptará el `RandomSource` del mundo a esta interfaz.
  - `RollTable<T>` genérica e inmutable (`List.copyOf`); pesos relativos no normalizados; peso 0 valido pero
    nunca elegido; tabla sin peso total positivo lanza al tirar.
  - Selección por rango acumulado con `random.nextInt(totalWeight)`.
- **Validación:** `./gradlew :common:test` → **8 tests** de `RollTable` verdes (111 en total). Proporción
  probada de forma determinista (recorriendo `r=0..total-1`) + reproducibilidad con semilla fija.
- **Resultado:** RNG por pesos determinista y testeable disponible para las tablas del datapack.

---

## F1 · DJT-016 — `DomainEventBus` + eventos internos (cierra EP-DOMAIN)

- **Qué:** bus de eventos de dominio ligero, **síncrono y server-side**, para desacoplar emisor/observadores,
  más los 8 eventos internos base. No cruzan la red.
- **Archivos:** `common/.../domain/event/`: `DomainEvent.java`, `DomainEventBus.java` y 8 records
  (`NatureGrantedEvent`, `JutsuCastEvent`, `SkillLevelChangedEvent`, `NinjaXpChangedEvent`, `EyeAwakenedEvent`,
  `EyeEvolvedEvent`, `ModeToggledEvent`, `BeastSealedEvent`); test `.../domain/event/DomainEventBusTest.java`.
- **Motivo:** `[ADD/05 §2]`. Reemplaza acoplamientos cableados del datapack (unlocks polling, allow_copy,
  get_rank, chequeos cruzados) por emisor→observador.
- **Decisiones técnicas:**
  - `DomainEvent` marcador; eventos como records inmutables validados. Identidad por `java.util.UUID` y
    conceptos por id `String` (data-driven) → dominio puro; `PlayerRef` (DJT-020) podrá adoptarse sin
    romper el contrato.
  - `DomainEventBus`: `subscribe/unsubscribe/publish`; dispatch por tipo o supertipo (`isInstance`);
    `ConcurrentHashMap` + `CopyOnWriteArrayList` + **instantánea** de handlers en `publish` → seguro ante
    (des)suscripción y reentrancia durante el dispatch.
  - **Aislamiento de fallos:** cada handler en try/catch; el fallo se registra con `System.Logger` (JDK,
    no `LogUtils` de Mojang, para mantener la pureza) y los demás handlers siguen.
  - Síncrono y server-side por diseño; nunca asíncrono ni de red.
- **Validación:** `./gradlew :common:test` → **10 tests** del bus verdes (121 en total). `./gradlew build`
  multiloader verde.
- **Resultado:** **EP-DOMAIN completo.** Las 5 abstracciones + RNG + bus de dominio existen y están testeadas
  (cero imports de Minecraft). Listo para EP-DATA (DJT-017).

---

# Fase 1 — EP-DATA (estado por jugador)

---

## F1 · DJT-017 — `PlayerNinjaData` (agregado raíz) + `Codec` mínimo · #riesgo-alto

- **Qué:** agregado raíz del estado por jugador (reemplaza ~800 scoreboards), mínimo: `OnboardingState` +
  `dataVersion`, con `Codec`/NBT y el patrón de sub-estado enchufable.
- **Archivos:** `common/.../data/PlayerNinjaData.java`, `common/.../data/state/OnboardingState.java`;
  test `common/src/test/.../data/PlayerNinjaDataTest.java`.
- **Motivo:** contrato central (`[ADD/03 §2, §2.12, §2.14]`, `[ADD/10 data]`, `[00 §8]`). Diseñar bien el
  patrón ahora evita reescrituras en 10 fases.
- **Decisiones técnicas:**
  - Capa `data` (no `domain`): usa Mojang/Minecraft (`Codec`, NBT) legítimamente, como `AttachmentService`.
  - **Patrón de sub-estado enchufable:** cada sub-estado = value object inmutable con `DEFAULT` + `CODEC` de
    compound; el agregado tiene un campo privado por sub-estado (getter + puerta de escritura) y su `CODEC`
    los combina con `optionalFieldOf(default)` → tolerante a NBT ausente (saves viejos) y a campos futuros
    desconocidos. Añadir sub-estado = 1 campo + 1 línea de codec + getter/setter, sin reescritura.
  - **Escritura por servicio:** campos privados; puertas `setOnboarding`/`setDataVersion` (nada de campos
    públicos mutables).
  - **Versionado día 1:** `CURRENT_DATA_VERSION=1`; NBT sin `dataVersion` → `INITIAL_DATA_VERSION=0` (esquema
    legado), para que la cadena de migración (DJT-018) sepa que debe migrarlo. Reset = instancia nueva.
  - `OnboardingState` con `Phase` (NOT_STARTED/SELECTING_CLAN/SELECTING_AFFINITY/COMPLETED, fiel a `join_main`
    de `[S01]`); codec de fase tolerante (nombre desconocido → NOT_STARTED).
- **Validación:** `./gradlew :common:test` → **6 tests** verdes; round-trip NBT con `NbtOps` (sin bootstrap de
  Minecraft), tolerancia a NBT ausente/corrupto/futuro, puerta de escritura. 127 tests en total.
- **Resultado:** contrato central estable y serializable; patrón listo para que cada fase enchufe su sub-estado.

---

## F1 · DJT-018 — `DataVersion` + `MigrationChain` + `Migration` (migración idempotente)

- **Qué:** migrar `PlayerNinjaData` de esquemas antiguos al actual, en orden y de forma idempotente. Primera
  migración placeholder v0→v1.
- **Archivos:** `common/.../data/DataVersion.java`, `common/.../data/migration/Migration.java`,
  `MigrationChain.java`; test `common/src/test/.../data/migration/MigrationChainTest.java`.
- **Motivo:** `[ADD/03 §2.14]`, `[ADD/04 §4]`, `[ADD/10 data]`. Migración no idempotente corrompe estado (S20-R4).
- **Decisiones técnicas:**
  - **Migración a nivel de NBT (`CompoundTag→CompoundTag`), antes de decodificar** con el codec. Razón: el
    codec (`optionalFieldOf`) ya tolera cambios aditivos; la cadena existe para cambios estructurales, que
    a nivel de objeto ya se habrían perdido en el decode (campo desconocido → ignorado).
  - **Idempotencia por version-gating:** una vez el NBT está en `CURRENT`, re-migrar no hace nada →
    `migrate(migrate(x)) == migrate(x)`. El input **no se muta** (se copia al entrar).
  - Versión ausente → `DataVersion.INITIAL` (más antigua); versión futura → no migra + aviso; falta de paso
    intermedio → `IllegalStateException` (no se salta ninguna migración).
  - `Migration.of(fromVersion, fn)` para registro cómodo; `MigrationChain.forCurrentSchema()` registra v0→v1
    (placeholder identidad; la cadena escribe la nueva versión).
  - Logging con `System.Logger` (JDK).
- **Validación:** `./gradlew :common:test` → **7 tests** verdes: migra legado→actual, idempotencia, no-muta,
  versión futura, falta de paso, duplicado rechazado, e integración migrar→decodificar (DJT-017). 134 en total.
- **Resultado:** migración idempotente lista; DJT-019 la enganchará en el codec del attachment.

---

## F1 · DJT-019 — `NinjaData.get` sobre `AttachmentService` (attachment real)

- **Qué:** registrar el attachment real de `PlayerNinjaData` (reemplaza el dummy de DJT-004) y la fachada
  `NinjaData.get(player)` (getOrCreate + migración al cargar); política de copia en respawn.
- **Archivos:** `common/.../data/NinjaData.java`; `common/.../registry/ModAttachments.java` (registra
  `player_ninja_data`); `common/.../debug/NinjaDataDebugCommand.java` (temporal, valida runtime);
  `common/.../DojutsuMod.java` (wiring). **Eliminados:** `debug/DummyAttachment.java`,
  `debug/DummyAttachmentCommand.java`. Test: `common/src/test/.../data/NinjaDataTest.java`.
- **Motivo:** `[ADD/04 §4, §7]`, `[ADD/03 §2]`. Habilita a todos los consumidores por jugador. RM-6.
- **Decisiones técnicas:**
  - **Codec migratorio (`NinjaData.PERSISTENT_CODEC`):** al decodificar convierte el input a NBT, corre
    `MigrationChain.forCurrentSchema()` y decodifica con `PlayerNinjaData.CODEC`; al codificar delega en el
    codec normal. Así la migración es transparente y ocurre al deserializar en cualquier loader (encaja
    DJT-018 en el attachment sin hooks por loader).
  - **`copyOnDeath=true`** en el registro → el estado ninja persiste entre respawns. `get()` es getOrCreate
    en ambos loaders; la mutación in-place del objeto adjunto persiste al guardar.
  - **Retirado el andamiaje dummy** de DJT-004 (`DummyAttachment`/`DummyAttachmentCommand`); en su lugar,
    comando temporal `/dojutsuninja get|set <fase>` para validar persistencia (se retira en DJT-026).
  - `ModAttachments` registra el attachment con `NinjaData.PERSISTENT_CODEC`; `NinjaData` lo accede — sin
    ciclo de init (el codec no referencia el handle).
- **Validación:** `./gradlew :common:test` → **3 tests** del codec migratorio verdes (137 en total);
  `./gradlew build` multiloader verde. **Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric**
  (2026-07-12): `set`→relogin→persiste, muerte/respawn→persiste, cambio de dimensión→persiste.
- **Resultado:** estado real por jugador accesible y persistente en ambos loaders. **EP-DATA completo.**

---

# Fase 1 — EP-CORE (motor: identidad, tick, ciclo de vida)

---

## F1 · DJT-020 — `PlayerRef` (identidad por UUID)

- **Qué:** referenciar jugadores/dueños por UUID nativo, eliminando el `player_id 1..10` y su límite de 10.
- **Archivos:** `common/.../core/identity/PlayerRef.java`; test `.../core/identity/PlayerRefTest.java`.
- **Motivo:** `[ADD/00 §2.4]`, `[ADD/04 §4]`, `[S01]`. Base del "owner" de entidades (F4: S05/S06/S15/S16/S09).
- **Decisiones técnicas:**
  - `record PlayerRef(UUID uuid)`: **solo guarda el UUID**, nunca una referencia dura al `Player` (evita
    fugas de memoria); igualdad por UUID.
  - Resolución segura: `resolve(Function<UUID,T>)` genérico (testeable con lookup fake) y `resolve(MinecraftServer)`
    → `Optional<ServerPlayer>` vacío si offline (no asume online, no crashea). `isOnline(server)`.
  - Capa `core` (usa MC); primer paquete `core.identity` de F1.
- **Validación:** `./gradlew :common:test` → **5 tests** verdes (142 en total): guarda UUID, resuelve online,
  vacío si offline, igualdad, UUID nulo rechazado.
- **Resultado:** identidad estable por UUID; sin límite de jugadores ni rastro de player_id.

---

## F1 · DJT-021 — `PlayerTickSystem` + `PlayerTickPipeline` (motor por tick)

- **Qué:** pipeline ordenado por prioridad al que cada módulo se engancha, preservando el orden del datapack.
- **Archivos:** `common/.../core/TickPhase.java`, `PlayerTickSystem.java`, `PlayerTickPipeline.java`;
  test `.../core/PlayerTickPipelineTest.java`.
- **Motivo:** `[ADD/04 §5]`, `[ADD/10 core]`, `[S01 §4]`. Punto de acoplamiento central del motor.
- **Decisiones técnicas:**
  - **`TickPhase` (enum)** expresa el orden invariante completo de `[S01 §4]`: CHAKRA, STRAIN, WATER_WALK,
    JUTSU_TIMERS, DOJUTSU, SAGE, EMPOWERMENT, JINCHURIKI, THROWING, SUMMONING, PROGRESSION, UNLOCKS. Poblado
    desde el día 1 → añadir un sistema en F2+ elige su fase y **no toca `core`** (OCP). CHAKRA primero
    (invariante: repone antes de consumir).
  - `PlayerTickSystem` (interfaz): `phase()` + `tick(ServerPlayer)`; `core` no conoce la lógica concreta
    (inyección).
  - `PlayerTickPipeline`: ordena por fase con `List.sort` (estable → orden de registro dentro de la misma
    fase); registro dinámico respeta prioridad. **Sin reset de input** (no polling). Aislamiento por jugador
    será de DJT-022.
- **Validación:** `./gradlew :common:test` → **5 tests** verdes (147 en total): orden por fase pese al orden
  de registro, chakra primero, estabilidad intra-fase, registro dinámico, pipeline vacío. Sistemas fake (la
  prueba con chakra real llega en F2).
- **Resultado:** motor por tick ordenado y desacoplado; los módulos se enganchan sin tocar `core`.

---

## F1 · DJT-022 — `ServerTickDispatcher` + `ModScheduler` (tick del servidor + tareas diferidas)

- **Qué:** enganchar el tick del servidor y ejecutar 3 fases (jugadores / entidad-global placeholder /
  scheduler); scheduler que reemplaza `schedule 1s/3600s`.
- **Archivos:** `common/.../core/ServerTickDispatcher.java`, `common/.../core/scheduler/ModScheduler.java`;
  `debug/SchedulerDebugCommand.java` (temporal); `DojutsuMod.java` (wiring). Test:
  `common/src/test/.../core/scheduler/ModSchedulerTest.java`.
- **Motivo:** `[ADD/04 §5]`, `[ADD/05 tabla "Tick"]`.
- **Decisiones técnicas:**
  - Hook cross-loader con **`TickEvent.SERVER_POST`** de Architectury (sus eventos funcionan; solo el
    networking estaba roto) — consistente con las convenciones. Corre en el hilo del servidor; **no itera
    `@e` global** (solo `getPlayerList().getPlayers()`).
  - **3 fases:** jugador (`PlayerTickPipeline.run` por jugador, **con try/catch por jugador** para aislar
    fallos), entidad/global (placeholder), scheduler (`ModScheduler.tick`).
  - `ModScheduler`: `PriorityQueue` por (tickVencimiento, secuencia); `schedule(delay)` vence en
    `currentTick+max(0,delay)` (retardo ≤0 → próximo tick, no "en el pasado"); `tick()` drena vencidas
    aislando el fallo de cada tarea. Lógica pura, unit-testeada.
  - Engranaje accesible por `ServerTickDispatcher.get()` (pipeline + scheduler) para que los módulos de F2+
    registren sistemas y programen tareas. Wiring provisional en `init()`; se consolidará en `DojutsuLifecycle`
    (DJT-023).
  - Comando temporal `/dojutsusched count|in <ticks>` (usa `PlayerRef` para no retener al jugador).
- **Validación:** `./gradlew :common:test` → **7 tests** del scheduler verdes (154 en total); `./gradlew build`
  multiloader verde. **Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric** (2026-07-12):
  `/dojutsusched count` crece cada tick, `/dojutsusched in 100` dispara a ~5 s.
- **Resultado:** motor por tick enganchado y scheduler operativo en ambos loaders.

---

## F1 · DJT-023 — `DojutsuLifecycle` (consolida el wiring de listeners)

- **Qué:** punto único de suscripción de listeners del loader y setup de arranque; reemplaza lo estático de
  `main/load`.
- **Archivos:** `common/.../core/DojutsuLifecycle.java`; `DojutsuMod.java` (delega en él; se retira el wiring
  reubicado).
- **Motivo:** `[ADD/04 §2]`, `[ADD/05]`.
- **Decisiones técnicas:**
  - `DojutsuLifecycle.init()` (llamado una vez desde `DojutsuMod.init()`, con guard `initialized`):
    stubs `loadConfig()`/`registerNetworkPayloads()` (TODO DJT-025/027), registra el `ServerTickDispatcher`
    (pipeline+scheduler) y el reload listener (movidos desde `DojutsuMod.init()`), y suscribe `PLAYER_JOIN`
    / `PLAYER_QUIT` (Architectury, server-side).
  - **Suscripción una sola vez** (init no se re-ejecuta en `/reload`; guard defensivo). **Aislado de cliente**
    (solo hooks server-side; el código de cliente sigue en `initClient()`).
  - Muerte/respawn (DJT-024) y carga de nivel + hardcore (DJT-025) se suscribirán aquí en sus tickets; se
    evita registrar handlers vacíos ahora (correrían en cada muerte de entidad sin hacer nada).
- **Validación:** `./gradlew build` multiloader verde (154 unit tests; DJT-023 es wiring, sin nuevos unit
  tests). **Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric** (2026-07-12): logs de
  arranque (init/stubs), `Jugador X se unio`/`salio`, `/dojutsusched count` crece (tick reubicado).
- **Resultado:** wiring de ciclo de vida centralizado y verificado; base lista para enganchar muerte/respawn.

---

## F1 · DJT-024 — `DeathHandler` + `RespawnHandler` (muerte/respawn)

- **Qué:** enganchar muerte/respawn con guards correctos y dejar la estructura + hooks para la penalización
  real (chakra/strain/modos llegan en F2+).
- **Archivos:** `common/.../core/lifecycle/DeathHandler.java`, `RespawnHandler.java`;
  `core/DojutsuLifecycle.java` (suscribe `LIVING_DEATH` y `PLAYER_RESPAWN`); test
  `.../core/lifecycle/DeathHandlerTest.java`.
- **Motivo:** `[ADD/04 §6]` (`main/death`, `main/respawn`).
- **Decisiones técnicas:**
  - **Guard testeable** `DeathHandler.appliesPenalty(GameType)`: penaliza en SURVIVAL/ADVENTURE, no en
    CREATIVE/SPECTATOR (cubre el borde "muerte en creativo/espectador"). `LIVING_DEATH` filtra a
    `ServerPlayer` (no penaliza mobs).
  - **Hooks/TODO F2:** la penalización concreta (½ chakra, strain=0, limpiar mrpills, RFSS) y la
    desactivación de modos (sage/puertas/sellos/soah/susanoo/formas) + recálculos + sync (DJT-027) son
    hooks, porque esos campos/modos no existen aún. Ahora solo el guard + el enganche + logs de traza.
  - Hardcore: la muerte dispara sin lógica destructiva; el respawn no ocurre → `RespawnHandler` no corre.
  - Handlers suscritos desde `DojutsuLifecycle` (punto único, DJT-023).
- **Validación:** `./gradlew build` multiloader verde; **2 tests** del guard verdes (156 en total).
  **Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric** (2026-07-12): morir en survival
  loguea penalización + respawn; en creativo/espectador el guard no penaliza.
- **Resultado:** muerte/respawn enganchados con guards correctos; la penalización real se conectará en F2 sin
  tocar el enganche.

---

## F1 · DJT-025 — `DojutsuConfig` + detección de Hardcore

- **Qué:** config editable equivalente a `properties` y lectura del flag hardcore sin matar al jugador.
- **Archivos:** `common/.../core/config/DojutsuConfig.java`; `core/DojutsuLifecycle.java` (carga config +
  hook de carga de nivel con detección de hardcore); test `.../core/config/DojutsuConfigTest.java`.
- **Motivo:** `[ADD/04 §2]`, `[ADD/05 tabla "Carga de nivel"]`, `[S01]`.
- **Decisiones técnicas:**
  - `DojutsuConfig` (record) con las claves globales de `properties` y **defaults exactos** de `[S01]`
    (verificados contra el datapack): AllowCustomTrader/AllowShinobis/AllowMultJinchuriki=1,
    CheckHardcore/UseTraderSpawnRate=0, CustomMangekyoObtainable=1, KillMob=1/Player=3/Boss=200,
    TraderSpawnRate=1, TraderSpawnChance=1, NatureEnergyCharge=1.
  - `fromProperties(Properties)` **puro y testeable**: clave ausente → default; valor inválido (int/bool) →
    default + aviso; acepta `1/0` y `true/false`. `load()` lee/crea `config/dojutsu.properties` vía
    `Platform.getConfigFolder()`; `get()` expone la instancia.
  - **Detección de hardcore no destructiva** por `level.getServer().isHardcore()` en `SERVER_LEVEL_LOAD`
    (solo overworld), reemplazando el "matar a un jugador" del datapack.
  - Las claves de tuning de Modo Sabio (`GatherTimeLevel_*`) se difieren al módulo sage (no al config global).
- **Validación:** `./gradlew build` multiloader verde; **6 tests** de config verdes (162 en total).
  **Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric** (2026-07-13): `Config cargada` +
  `Mundo hardcore = false` en mundo normal (se crea el .properties); `= true` en hardcore sin matar a nadie.
- **Resultado:** config global cargable/editable con defaults fieles; hardcore detectado sin efectos destructivos.

---

## F1 · DJT-026 — `AdminCommands` (`reset_ninja` + stubs; cierra EP-CORE)

- **Qué:** comandos admin equivalentes a `cheats/`, incluido el reset de ninja (= objeto nuevo).
- **Archivos:** `common/.../core/command/AdminCommands.java`; `data/NinjaData.java` (`reset`); `DojutsuMod.java`
  (registro). `[ADD/00 §2.5]`, `[S00 §7 nota cheats]`, `[ADD/03 §7 invariantes]`.
- **Decisiones técnicas:**
  - `/dojutsu` con **permiso de op (nivel 2)**; `reset_ninja [target]` reemplaza `PlayerNinjaData` por una
    **instancia nueva** (`NinjaData.reset` → `handle.set(player, new PlayerNinjaData())`) — reset total, sin
    reset campo-a-campo, imposible olvidar un campo o desincronizar (corrige S01-R2).
  - Stubs registrados de `give_six_paths`, `reset_cooldowns`, `restore_all`, `set_test_ninja`,
    `spawn_shinobi_villager` (mensaje "pendiente"; se completan en su fase).
  - Target online vía `EntityArgument.player` (self por defecto). Sync completo tras reset = TODO(DJT-027).
  - Comandos de depuración (`/dojutsuninja`, `/dojutsusched`) se mantienen: `/dojutsuninja get/set` es la
    herramienta para observar/validar el reset hasta que el HUD de F2 permita observar el estado.
- **Validación:** `./gradlew build` multiloader verde (162 tests; el reset es "instancia nueva", ya cubierto
  por los defaults de DJT-017; el comando es wiring). **Validación runtime CONFIRMADA por el usuario en
  NeoForge y Fabric** (2026-07-13): set COMPLETED → reset_ninja → get muestra NOT_STARTED; stub responde
  "pendiente"; sin op no aparece.
- **Resultado:** reset total operativo + stubs. **EP-CORE completo.**

---

# Fase 1 — EP-NETWORK-BASE (sync del estado)

---

## F1 · DJT-027 — `SyncNinjaDataPayload` (full+delta) + `DirtyTracker`

- **Qué:** enviar snapshot completo (join/respawn/dimensión) y deltas (cambios) del estado observable al
  cliente (S2C). Retira el ping-pong de F0.
- **Archivos:** `common/.../network/SyncNinjaDataPayload.java`, `NinjaSync.java`; `data/ObservableField.java`,
  `data/DirtyTracker.java`; `PlayerNinjaData.java` (tracker transitorio + marca en `setOnboarding`); wiring en
  `DojutsuLifecycle`/`RespawnHandler`/`AdminCommands`/`NinjaDataDebugCommand`. **Eliminados:** ping-pong F0
  (`PingPongDebug`, `PingPongClientDebug`, `PingPayload`, `PongPayload`). Test: `data/DirtyTrackerTest.java`.
- **Motivo:** `[ADD/07]`, `[ADD/03 §2 "snapshot"]`, `[ADD/10 network]`. Depende de DJT-019 + DJT-005.
- **Decisiones técnicas:**
  - `SyncNinjaDataPayload` (S2C, record) con flag `full` + snapshot observable (en F1, la fase de onboarding
    — único campo observable; `StreamCodec` con `BOOL` + fase por ordinal VAR_INT, con clamp defensivo).
  - `DirtyTracker` (`EnumSet<ObservableField>`), **transitorio** por jugador (no se persiste); la puerta de
    escritura `setOnboarding` lo marca. `PlayerNinjaData` lo lleva como campo no serializado.
  - `NinjaSync`: `sendFull` (join/respawn/dimensión; limpia el tracker), `sendDelta` (no-op si el tracker está
    vacío), `register()` (S2C) y handler de cliente (log; DJT-028 escribirá en `ClientNinjaData`).
  - **Solo viaja lo observable** (`dataVersion` interno no). Con un único campo, el delta lleva el mismo dato
    que el full (solo cambia el flag); F2 ampliará payload/tracker para deltas por-campo reales.
  - Ping-pong F0 retirado (ya no hacía falta; el sync real lo sustituye). `initClient()` queda limpio.
- **Validación:** `./gradlew build` multiloader verde; **5 tests** de `DirtyTracker` verdes (167 en total).
  **Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric** (2026-07-13): log de cliente
  `Sync recibido` en join (full), tras `/dojutsuninja set` (delta), reset y cambio de dimensión (full).
- **Resultado:** sync base del estado observable operativo (full+delta); a falta de la confirmación runtime.

---

## F1 · DJT-028 — `ClientNinjaData` (cache read-only en cliente; cierra F1)

- **Qué:** cache de solo lectura en cliente que recibe los payloads de sync y guarda el snapshot observable
  para el HUD/GUI.
- **Archivos:** `common/.../client/ClientNinjaData.java`; `network/NinjaSync.java` (el handler escribe en la
  cache); `DojutsuMod.initClient()` (limpia al salir del mundo). Test: `client/ClientNinjaDataTest.java`.
- **Motivo:** `[ADD/10 client]`, `[ADD/07]`. Depende de DJT-027.
- **Decisiones técnicas:**
  - `ClientNinjaData` es un **holder estático** (un cliente por proceso) **sin dependencias de MC-cliente**
    (solo `OnboardingState.Phase` + logger) → es client-only por uso, pero inocuo aunque no se cargue en
    servidor dedicado (nunca se referencia desde código de servidor).
  - `applyFull` habilita la cache; `applyDelta` se **ignora antes del primer full** (evita estado incoherente);
    `clear` la vacía al cambiar de mundo (`ClientPlayerEvent.CLIENT_PLAYER_QUIT`, registrado en `initClient`,
    client-only).
  - El handler `NinjaSync.onClientReceive` (común, corre solo en cliente) pasa de loguear a escribir en la cache.
  - Read-only: no se persiste; el cliente **nunca** decide gameplay a partir de ella (autoridad del servidor).
- **Validación:** `./gradlew clean build` multiloader verde; **5 tests** de `ClientNinjaData` verdes
  (172 en total). **Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric** (2026-07-13):
  log de cliente `Cache cliente (full/delta)` en join y tras `/dojutsuninja set`; se limpia y rehidrata al reentrar.
- **Resultado:** cache observable en cliente operativa. **Cierra EP-NETWORK-BASE y la Fase 1.**

---

# CIERRE DE FASE 1 — 2026-07-13

**Fase 1 (Núcleo, dominio y estado) COMPLETADA y validada en runtime en NeoForge y Fabric.**

- **Épicas:** EP-DOMAIN (DJT-010…016), EP-DATA (DJT-017…019), EP-CORE (DJT-020…026), EP-NETWORK-BASE
  (DJT-027…028). **19 tickets, todos ✅.**
- **Cobertura:** 172 tests unitarios de dominio/datos verdes; `./gradlew clean build` multiloader verde.
- **DoD de fase alcanzada:** jugador entra → `PlayerNinjaData` (defaults) → tick pipeline corre → modificar
  estado y reloguear lo persiste → morir aplica guards (½ chakra/strain como hooks F2) → respawn (hooks) →
  `/dojutsu reset_ninja` deja defaults → migración transparente (idempotente) → estado observable
  sincronizado al cliente (full/delta) y cacheado read-only.
- **Contrato central estable:** `PlayerNinjaData` con patrón de sub-estado enchufable + versionado; cada fase
  posterior añade su sub-estado sin reescritura.
- **Deuda/hooks abiertos para F2+:** penalización de muerte real (½ chakra, strain=0) y desactivación de modos
  en respawn (hooks listos); sync HUD real; puerto `AttributeService` (adjuntar atributos a `Player`);
  `GatherTimeLevel_*` (tuning sage) en el módulo sage; deltas por-campo cuando haya >1 observable; retirar
  `/dojutsuninja` y `/dojutsusched` cuando el HUD de F2 permita observar el estado.

---

# FASE 2 — Recursos y progresión (VERTICAL SLICE) · inicio 2026-07-14

> Épicas: EP-CHAKRA, EP-PROGRESSION, EP-CLIENT-BASE. Objetivo: chakra vivo + XP/nivel/rango + HUD/menú
> (validación arquitectónica end-to-end). Un ticket a la vez. Decisión de arranque: se sigue el backlog de
> diseño (fuente de verdad) — el chakra usa `maxChakra` como campo de `ChakraState`, **sin** el puerto
> `AttributeService` (las notas de CLAUDE.md/handoff que lo daban por bloqueante eran más cautas de lo que la
> ficha exige; `AttributeService` se difiere a cuando un sistema necesite modificadores de atributo vanilla).

## F2 · DJT-030 — `ChakraState` + `ChakraService` (consume/restore/has)

- **Qué:** sub-estado de chakra en `PlayerNinjaData` y su única puerta de escritura, con clamp `[0,max]` en
  cada operación; chakra marcado observable y sincronizado al cliente (para el HUD de DJT-041).
- **Archivos (nuevos):** `common/.../data/state/ChakraState.java`, `common/.../chakra/ChakraService.java`,
  `common/.../debug/ChakraDebugCommand.java`; tests `data/state/ChakraStateTest.java`,
  `chakra/ChakraServiceTest.java`.
- **Archivos (editados):** `data/PlayerNinjaData.java` (campo `chakra` + getter + puerta `setChakra` + línea de
  codec), `data/ObservableField.java` (+`CHAKRA`), `network/SyncNinjaDataPayload.java` (+`chakra`/`maxChakra`,
  VAR_INT), `network/NinjaSync.java` (`snapshot()` incluye chakra en full/delta),
  `client/ClientNinjaData.java` (cachea chakra/maxChakra), `DojutsuMod.java` (registra el comando de debug);
  tests `data/PlayerNinjaDataTest.java` y `client/ClientNinjaDataTest.java` (ajustados a las nuevas firmas).
- **Motivo:** `[S02]`, `[ADD/03 §2.1]`, `[ADD/10 chakra]`. Depende de DJT-017, DJT-010, DJT-027.
- **Decisiones técnicas:**
  - `ChakraState` = **value object inmutable** (record) con `DEFAULT`+`CODEC`, patrón de sub-estado enchufable
    (como `OnboardingState`). Campos de `[S02]`: `chakra`/`maxChakra`(=100), `naturalChakra`/`maxNaturalChakra`
    (=100), `chakraTimer`, `jugoTimer`. **Chakra inicial 0** (fiel al datapack: inicializa `d.chakra` a 0). El
    constructor compacto **sanea el invariante** (maxes ≥ 0, actuales en `[0,max]`, timers ≥ 0) como defensa
    ante NBT corrupto.
  - `ChakraService` = **única puerta de escritura** del chakra normal (`has`/`consume` todo-o-nada/`restore`/
    `restoreFull`). Opera sobre `PlayerNinjaData` (capa `data`, sin `ServerPlayer`) → **testeable sin MC**. El
    clamp se **delega en el dominio puro `Resource`** (DRY, ya probado); la escritura pasa por
    `PlayerNinjaData.setChakra`, que marca `ObservableField.CHAKRA` sucio. El disparo del sync es del llamador.
  - `naturalChakra`/timers se **persisten** pero aún no tienen mutador (los escriben la regeneración DJT-031 y
    el Modo Sabio F6). Sin `AttributeService` (acordado): el máximo vive en el estado.
  - **Sync:** el payload lleva ahora `chakra`+`maxChakra`; con pocos observables el delta reenvía todo lo
    observable (delta por-campo real = deuda documentada). Cliente cachea en `ClientNinjaData` (read-only).
  - **NO se bumpea `dataVersion`:** la tolerancia de `optionalFieldOf` da el default a los saves de F1 sin la
    rama `chakra` (sin migración).
  - `/dojutsuchakra get|consume <n>|restore <n>|full` = andamiaje temporal (se retira con el HUD DJT-041);
    usa `integer(0)` para respetar el contrato no-negativo de `Resource`; sincroniza tras cada cambio.
- **Autorrevisión:** una sola puerta de escritura; clamp centralizado en `Resource`; `core` intacto (OCP);
  consumo rechazado no marca observable (no hubo escritura); sin duplicación relevante.
- **Validación:** `./gradlew clean build` multiloader **verde**; **188 tests** verdes (172 + 16 nuevos:
  6 `ChakraState`, 8 `ChakraService`, 2 en `PlayerNinjaData`). **Validación runtime CONFIRMADA por el usuario
  en NeoForge y Fabric** (2026-07-14): `/dojutsuchakra` consume/restaura con clamp, persiste al reloguear y
  sincroniza al cliente.
- **Resultado:** chakra persistente, mutable con clamp por una única puerta, y sincronizado al cliente. Base de
  la regeneración (DJT-031), reposición (DJT-032), penalización de muerte (DJT-033) y HUD (DJT-041).

## F2 · DJT-031 — `RegenService` (regeneración por tick, tabla de `[S02]`)

- **Qué:** regeneración de chakra por tick, fiel a `[S02]` (umbrales 120/60/30/15 e incrementos por factor),
  corriendo **primero** en el pipeline (`TickPhase.CHAKRA`), antes de cualquier consumo. Incluye la regen
  pasiva de chakra natural del clan Jugo (mecanismo presente pero **inactivo** hasta F3).
- **Archivos (nuevo):** `common/.../chakra/RegenService.java` (`PlayerTickSystem`); test
  `chakra/ChakraRegenTest.java`.
- **Archivos (editados):** `chakra/ChakraService.java` (`advanceRegen` / `advanceNaturalRegen`, lógica pura),
  `data/state/ChakraState.java` (withers `withChakraTimer`/`withNaturalChakra`/`withJugoTimer`),
  `data/PlayerNinjaData.java` (`setChakra` marca observable **solo si cambia la proyección observable**),
  `core/DojutsuLifecycle.java` (registro del sistema en el pipeline), `debug/ChakraDebugCommand.java`
  (`/dojutsuchakra regen <factor>` para validar factores in-game).
- **Motivo:** `[S02]`, `[ADD/10 chakra "timer→pulso"]`, `[ADD/03 §2.1]`. Depende de DJT-030, DJT-021, DJT-010.
- **Decisiones técnicas:**
  - **Se reutiliza el dominio `RegenPolicy` (DJT-010)**, que ya reproduce toda la tabla de `[S02]` y la
    resolución de combinaciones. `RegenService` no reimplementa la tabla: solo cablea el tick, deriva los
    factores del jugador y dispara el sync.
  - **El timer vive en `ChakraState.chakraTimer` (persistido)**, no en el objeto transitorio `RegenPolicy`
    (que nace con timer=0 en cada `resolve`). `advanceRegen` acumula el timer persistido y, al umbral, pulsa.
  - **SRP:** `RegenService` conoce jugador/mundo (guards espectador/muerto, factores, sync) — acoplado a MC;
    `ChakraService.advanceRegen/advanceNaturalRegen` es **pura** (sobre `PlayerNinjaData`) → testeable sin MC.
  - **`setChakra` inteligente:** marca `ObservableField.CHAKRA` solo si cambia chakra/maxChakra; así el timer
    (cada tick) y el chakra natural (no observables) **no** generan deltas de red inútiles. Solo el pulso que
    cambia el chakra sincroniza (`NinjaSync.sendDelta`).
  - **Factores inactivos:** clan Otsutsuki/Uzumaki, ADN Hashirama y Modo Sabio 1/2 (F3/F6) y el clan Jugo (F3)
    aún no existen → `activeFactors` devuelve vacío (regen estándar 120/+3) y `hasJugoClan` devuelve `false`.
    Patrón "presente pero inactivo" (igual que `RegenPolicy` ya acepta esos factores). Se enchufan luego sin
    tocar el resto. `/dojutsuchakra regen <none|otsutsuki|uzumaki|hashirama|sage1|sage2>` fuerza factores para
    validar la regen acelerada in-game (andamiaje temporal, se retira con natures/sage).
  - **Guards:** sin regen para espectador ni jugador muerto (`isSpectator()`/`!isAlive()`).
- **Autorrevisión:** timer persistido correctamente; el "clamp cada tick" del datapack queda subsumido por el
  invariante siempre-válido de `ChakraState`; mutación en sitio (misma referencia del attachment); `core`
  intacto (OCP); única puerta de escritura respetada.
- **Validación:** `./gradlew clean build` multiloader **verde**; **197 tests** verdes (188 + 9 de
  `ChakraRegenTest`: tabla estándar/Uzumaki, timer persistido, observable solo en pulso, chakra natural Jugo).
  **Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric** (2026-07-14): regen base +3/6s,
  acelerada con `/dojutsuchakra regen uzumaki`, sin regen al máximo ni en espectador.
- **Resultado:** el chakra se regenera solo por tick a la tasa correcta; base de la reposición (DJT-032) y del
  HUD (DJT-041). Con clanes/sabio (F3/F6) la regen acelerada se activa sin tocar `RegenService`.

## F2 · DJT-032a — Reposición de chakra por dormir (evento real)

> **Nota de división:** DJT-032 se dividió porque los eventos que necesita no son cross-loader en Architectury
> 13.0.8 (verificado). **DJT-032a = dormir→chakra** (este; ambos loaders, sin mixin). **DJT-032b = comer→chakra**
> (pendiente; requiere un mixin de Fabric porque fabric-api no tiene evento de "terminar de comer"). Decisión de
> alcance aprobada por el usuario (2026-07-14).

- **Qué:** al despertar de una noche completa, el chakra se repone al máximo (fiel a `chakra/sleep/wake_up` de
  `[S02]`). Reemplaza el bucle `schedule 1s` + advancement `slept_in_bed` del datapack por el evento nativo.
- **Archivos (nuevos):** `common/.../chakra/SleepChakraHandler.java`;
  `neoforge/.../DojutsuNeoForgeServerEvents.java`; `fabric/.../DojutsuFabricServerEvents.java`.
- **Archivos (editados):** `neoforge/.../DojutsuNeoForge.java` y `fabric/.../DojutsuFabric.java` (registran los
  eventos nativos del server).
- **Motivo:** `[S02]`, `[ADD/05 tabla "Dormir"]`, `[ADD/03 §2.1]`. Depende de DJT-030.
- **Decisiones técnicas:**
  - **Eventos nativos por loader, no Architectury** (que no los tiene): NeoForge `PlayerWakeUpEvent`
    (bus del juego); Fabric `EntitySleepEvents.STOP_SLEEPING` (fabric-api, sin mixin). Se **abstraen en los
    adaptadores** (`Dojutsu<Loader>ServerEvents`), que delegan en el handler común `SleepChakraHandler`
    (patrón "push": el adaptador suscribe, el común decide). Coherente con `[ADD/05 §línea 6]`.
  - **Excepción documentada al "punto único de suscripción":** los eventos loader-nativos se suscriben en el
    adaptador (no en `DojutsuLifecycle`, que solo cubre los cross-loader de Architectury).
  - **`SleepChakraHandler` decide con `Level.isDay()`** (fiel al `is_early_day` del datapack): solo la noche
    completa restaura; un sueño interrumpido de noche no repone. Timing correcto: el servidor avanza la hora a
    la mañana antes de despertar, así que `isDay()` ya es `true` en el evento. Filtra a `ServerPlayer` (aísla
    el lado servidor en ambos loaders).
  - **Reset de strain diferido:** `sleep/wake_up` también pone `eye_strain`/`eg_strain` a 0, pero esos
    sub-estados no existen hasta F5/F6 → hook `// TODO F5/F6` en el handler.
  - **Reposición progresiva (sueño parcial):** el `sleep/inc` (+max/20 por tick) del datapack es refinamiento
    del sueño interrumpido; se omite por ahora (la noche completa deja el chakra al máximo de todos modos).
- **Validación:** `./gradlew build` multiloader **verde**; **197 tests** verdes (la mutación `restoreFull` ya
  está cubierta por `ChakraServiceTest`; el handler es MC-coupled y se valida en runtime). **Validación runtime
  CONFIRMADA por el usuario en NeoForge y Fabric** (2026-07-14).
- **Bug encontrado y corregido en runtime:** el guard usaba `Level.isDay()`, pero `skyDarken` se calcula al
  inicio del tick y aún refleja la noche cuando el salto de sueño despierta al jugador en ese mismo tick →
  falso negativo, no restauraba. Fix: decidir por la hora del mundo (`getDayTime() % 24000 < 12000`, ya
  avanzada a la mañana al despertar) en vez del brillo del cielo. Log temporal de diagnóstico retirado.
- **Resultado:** dormir la noche entera rellena el chakra. Pendiente DJT-032b (comer→chakra, con mixin Fabric).

## F2 · DJT-032b — Reposición de chakra por comer (evento + primer mixin de Fabric)

- **Qué:** comer un alimento repone **+50** chakra (fiel a `chakra/eat_food` de `[S02]`). El `+100` de ramen
  depende del ítem (módulo `items`, S14/F8) → diferido.
- **Archivos (nuevos):** `common/.../chakra/FoodChakraHandler.java`;
  `fabric/.../mixin/LivingEntityMixin.java`; `fabric/src/main/resources/dojutsu.mixins.json`.
- **Archivos (editados):** `neoforge/.../DojutsuNeoForgeServerEvents.java` (+ `LivingEntityUseItemEvent.Finish`);
  `fabric/src/main/resources/fabric.mod.json` (referencia al mixin).
- **Motivo:** `[S02]`, `[ADD/05 tabla "Comer"]`, `[ADD/03 §2.1]`. Depende de DJT-030. Parte 2 del DJT-032 dividido.
- **Decisiones técnicas:**
  - **NeoForge:** evento nativo `LivingEntityUseItemEvent.Finish` (bus del juego) → `FoodChakraHandler`.
  - **Fabric — PRIMER MIXIN del proyecto:** fabric-api no tiene evento de "fin de uso de ítem", así que se
    mixinea `LivingEntity#completeUsingItem` (HEAD) y, solo para `ServerPlayer`, se delega en el handler común
    con `getUseItem()`. Infra: `dojutsu.mixins.json` (`compatibilityLevel JAVA_21`), referenciado en
    `fabric.mod.json`. **architectury-loom auto-genera el refmap** (`dojutsu-fabric-fabric-refmap.json`, target
    resuelto a intermediary `class_1309.method_6040`) y lo auto-inyecta en la config — sin configuración extra
    en `build.gradle`. El mixin vive solo en el módulo `fabric` (NeoForge no lo ve).
  - **Handler común `FoodChakraHandler`:** filtra por componente `DataComponents.FOOD` (excluye pociones/leche,
    fiel a `eat_food`), `ChakraService.restore(50)` (clamp) + `sendDelta`. Loader-agnóstico.
- **Autorrevisión:** guard de comida correcto; sin doble disparo; el mixin solo actúa en `ServerPlayer`
  (lado servidor); comer al máximo clampa y el smart-gate evita delta inútil.
- **Validación:** `./gradlew build` multiloader **verde**; **197 tests** verdes (la mutación `restore` ya está
  cubierta por `ChakraServiceTest`; los handlers son MC-coupled y se validan en runtime); refmap del mixin
  verificado en el jar. **Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric** (2026-07-14):
  comer +50 con clamp; pociones/leche no reponen; el mixin de Fabric aplica correctamente.
- **Resultado:** comer repone chakra en ambos loaders. **Cierra DJT-032** (dormir 032a + comer 032b). Queda
  disponible la infraestructura de mixins de Fabric para futuros hooks sin evento cross-loader.

## F2 · DJT-033 — Penalización de muerte = ½ chakra (wiring en `DeathHandler`)

- **Qué:** al morir en survival/adventure, `chakra = maxChakra/2` (mitad del **máximo**, no del actual). Completa
  el hook dejado en DJT-024. Fiel a `main/death` de `[S01]`.
- **Archivos (editados):** `common/.../core/lifecycle/DeathHandler.java` (rellena el TODO);
  `common/.../chakra/ChakraService.java` (nueva primitiva `set`); test `chakra/ChakraServiceTest.java` (+2).
- **Motivo:** `[S01]`, `[ADD/04 §6]`. Depende de DJT-024, DJT-030.
- **Decisiones técnicas:**
  - `ChakraService.set(data, value)`: puerta de escritura para asignaciones absolutas del datapack (clamp a
    `[0, maxChakra]` vía el invariante de `ChakraState`). `DeathHandler` expresa la política
    (`set(data, maxChakra/2)`); el servicio hace la mutación (SRP).
  - **½ del máximo, no del actual** (S01/`[ADD/04 §6]`): puede *subir* el chakra si el jugador muere con menos
    de la mitad. Cubierto por test.
  - **Guard ya presente** (`appliesPenalty`: solo SURVIVAL/ADVENTURE, nunca CREATIVE/SPECTATOR).
  - **Propagación:** se aplica sobre el jugador que muere; `copyOnDeath` (DJT-019) lo lleva al respawn y
    `RespawnHandler.sendFull` sincroniza al cliente (ya cableado). En hardcore es inocuo (sin respawn útil).
  - **Reset de strain diferido:** `main/death` también pone eye/eg strain a 0; esos sub-estados no existen hasta
    F5/F6 → hook `// TODO`.
- **Validación:** `./gradlew build` multiloader **verde**; **199 tests** verdes (197 + 2: `set` con clamp y
  penalización = ½ del max). **Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric** (2026-07-14).
- **Resultado:** morir en survival deja el chakra a la mitad del máximo, persistente al respawn.
  **Cierra EP-CHAKRA** (chakra vivo: estado, regen, comer/dormir, muerte).

## F2 · DJT-034 — Auditoría de economía (RA-4) + `ProgressionState` (abre EP-PROGRESSION)

- **Qué:** (1) auditoría de **todas** las fuentes de XP/puntos/costes del datapack a un dataset de referencia;
  (2) el sub-estado de progresión (skill points, 7 habilidades como `LeveledStat`, XP ninja, unlocks).
- **Archivos (nuevos):** `common/.../data/state/ProgressionState.java`, `common/.../data/state/Skill.java`,
  `common/src/main/resources/data/dojutsu/balance/xp_sources.json`; test `data/state/ProgressionStateTest.java`.
- **Archivos (editados):** `data/PlayerNinjaData.java` (sub-estado `progression` + puerta `setProgression`);
  `domain/progression/LeveledStat.java` (+`equals`/`hashCode`); test `data/PlayerNinjaDataTest.java` (+2).
- **Motivo:** `[S13]`, `[S20]`, `[S06]`, `[ADD/08 RA-4]`, `[ADD/03 §2.7]`. Depende de DJT-017, DJT-014.
- **Decisiones técnicas:**
  - **Auditoría (RA-4) → `xp_sources.json`** (dataset de REFERENCIA, no cargado por código): kills→puntos
    (config 1/3/200), gasto de punto (+1 stat / +2 ninja_xp; clan afín +3/+6), curva de XP (bandas de S13,
    cap 500), rango (umbrales S20), fuentes de ninja_xp y unlocks. Los valores no cuantificados en la ing.
    inversa se marcan **`TBD_datapack`** (se extraen al portar F3/F4). Nada se omite: base de balance única.
  - **`Skill` (enum, 7):** taijutsu/genjutsu/medical/fuinjutsu/kenjutsu (auto-nivelan) + senjutsu/cc
    (`autoLevels=false`, solo por puntos, `[ADD/00 §3]`). Las **afinidades** viven en `NatureProfile` (F3).
  - **`ProgressionState`:** value object inmutable (patrón enchufable), withers, codec **tolerante** (cada
    habilidad opcional; clamp de nivel/xp corrupto vía `safeLeveledStat`; relleno de habilidades ausentes).
    `unlocked` idempotente. **Escala compartida** afinidad↔skillpoint (`[S13-R1]`) documentada.
  - **Rango DERIVADO, no cacheado:** `rank()` = `NinjaRank.fromXp(ninjaXp)` (getter), no un campo persistido:
    satisface "derivado, no fuente de verdad" sin riesgo de desincronización (DJT-037 solo detectará el cambio).
  - **`LeveledStat.equals/hashCode`** por valor (level+xp): necesario para la igualdad de `ProgressionState`
    (se usa como VALOR del mapa, no clave → la mutabilidad no afecta a colecciones).
  - **`setProgression` aún no marca observable:** la sync de progresión al cliente llega con el menu/HUD
    (DJT-041/042), que añadirán su `ObservableField` y ampliarán el payload. Sin `dataVersion` bump.
- **Validación:** `./gradlew build` multiloader **verde**; **210 tests** verdes (199 + 11: 9 `ProgressionState`
  + 2 agregado). Sin comportamiento in-game todavía (la ficha marca prueba manual **N/A** = dataset + estado);
  la persistencia se cubre por round-trip NBT y usa el attachment ya validado. El comportamiento observable de
  progresión empieza en DJT-035 (kills→puntos).
- **Resultado:** economía auditada y sub-estado de progresión persistente. **Abre EP-PROGRESSION.** Base de
  `SkillPointService` (035), `LevelingService` (036), `RankService` (037) y `UnlockService` (038).

## F2 · DJT-035 — `SkillPointService` (kills → puntos; gasto → sube habilidad)

- **Qué:** matar entidades otorga skill points (por tipo de víctima, valores de config); gastar puntos sube una
  habilidad (+1 nivel y +2 ninja_xp por punto). Reemplaza los advancements `killed_*` y las `try_increase_*`.
- **Archivos (nuevos):** `common/.../progression/SkillPointService.java`;
  `common/.../debug/ProgressionDebugCommand.java` (`/dojutsuprogression get|award|spend`).
- **Archivos (editados):** `core/DojutsuLifecycle.java` (2ª suscripción a `LIVING_DEATH`: el killer gana puntos);
  `DojutsuMod.java` (registra el comando); test `progression/SkillPointServiceTest.java`.
- **Motivo:** `[S13]`, `[ADD/05 tabla "Kill"]`, `[ADD/03 §2.7]`. Depende de DJT-034.
- **Decisiones técnicas:**
  - **SRP:** `award(data, points)` y `spend(data, skill, points)` **puros** (mutan `PlayerNinjaData` por la
    puerta `setProgression`; testeables sin MC). `onKill(victim, source)` es el glue del evento de muerte.
  - **Kills → puntos por evento** (`EntityEvent.LIVING_DEATH`, cross-loader, junto a `DeathHandler`; sin
    advancements). `source.getEntity()` = responsable real → **cuenta al jugador también en kills por
    proyectil**. Guards: no-jugador y auto-muerte se ignoran.
  - **Puntos por víctima** (config `DojutsuConfig`, defaults del datapack): jugador `KillPlayerPoints`=3,
    jefe `KillBossPoints`=200 (jefes vanilla `EnderDragon`/`WitherBoss`; bestias con cola en F7),
    resto `KillMobPoints`=1.
  - **`spend` todo-o-nada:** rechaza si faltan puntos, si la cantidad no es positiva o si excede el cap 500;
    aplica +1 nivel/punto (afines de clan en F3), descuenta puntos y suma +2 ninja_xp/punto. **TODO DJT-036:**
    enrutar el ninja_xp por `XpApi` (única puerta de XP, emitirá `NinjaXpChangedEvent` para el rango).
  - Progresión aún no se sincroniza (sin HUD); se valida server-side con `/dojutsuprogression`.
- **Validación:** `./gradlew build` multiloader **verde**; **218 tests** verdes (210 + 8: award/spend, multi-punto,
  rechazos, cap, habilidad sin auto-nivel). **Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric** (2026-07-15).
- **Resultado:** matar suma skill points; gastarlos sube habilidades. Base de `LevelingService` (036) y del
  handler de gasto validado por payload (042).

## F2 · DJT-036 — `LevelingService` + `XpApi` (auto-nivelación por XP)

- **Qué:** añadir XP a una habilidad sube **todos los niveles que correspondan de una vez** (cap 500, sin la
  guardia "1 level-up/tick" del datapack) y emite eventos; `XpApi` es la **única puerta de escritura de XP**.
- **Archivos (nuevos):** `common/.../core/DomainEvents.java` (holder del bus compartido server-side);
  `common/.../progression/LevelingService.java` (motor puro); `common/.../progression/XpApi.java` (fachada MC);
  test `progression/LevelingServiceTest.java`.
- **Archivos (editados):** `progression/SkillPointService.java` (`spend` puro sin ninja_xp + sobrecarga MC
  `spend(ServerPlayer,...)` que enruta el ninja_xp por `XpApi`); `debug/ProgressionDebugCommand.java`
  (`spend` usa la sobrecarga MC; nuevo `xp <skill> <n>`); test `SkillPointServiceTest.java` (ajustado).
- **Motivo:** `[S13]`, `[ADD/03 §2.7]`, `[ADD/10 progression "subir todo"]`. Depende de DJT-034, DJT-016 (bus).
- **Decisiones técnicas:**
  - **REACTIVO, no por tick (decisión con matiz sobre la ficha):** la ficha decía "registro en pipeline", pero
    eso es el modelo de *polling* del datapack. F1 diseñó `LeveledStat.addXp` para subir multi-nivel al
    escribir; la propia checklist exige "multi-nivel en una operación / sin guardia 1/tick". Por tanto la
    nivelación ocurre **al añadir XP** (evento discreto), no sondeando cada tick. **No se registra ningún
    `PlayerTickSystem`** (no hay nada que sondear). Consistente con F1 y la checklist.
  - **`XpApi` = única puerta de XP** (fachada MC): resuelve estado + UUID y usa el bus compartido
    `DomainEvents.bus()`. `LevelingService` es el motor **puro** (recibe `DomainEventBus` inyectado →
    testeable sin MC, sin tocar el bus global).
  - **Eventos:** `addSkillXp` emite **1 `SkillLevelChangedEvent` agregado** (oldLevel→newLevel) solo si el
    nivel cambia; `addNinjaXp` emite `NinjaXpChangedEvent`. Base de `RankService` (037) y `UnlockService` (038,
    que deberá revisar el rango de niveles del evento agregado).
  - **Bus compartido** (`DomainEvents`): singleton por proceso (handlers sin estado); 037/038 se suscribiran.
  - **`spend` refactor:** la version pura solo sube nivel y descuenta puntos; la sobrecarga MC añade la
    recompensa de ninja_xp (+2/punto) por `XpApi` (emite evento → rango). `senjutsu`/`cc`: gastables por
    puntos, pero `addSkillXp` ignora su XP (sin auto-nivel).
  - **Afinidades:** `addAffinityXp` llega con `NatureProfile` (F3); su estado aún no existe.
- **Validación:** `./gradlew build` multiloader **verde**; **226 tests** verdes (218 + 8: multi-nivel, evento
  agregado, sin evento sin cambio, ignora sin-autonivel/no-positiva, cap, `addNinjaXp`). **Validación runtime
  CONFIRMADA por el usuario en NeoForge y Fabric** (2026-07-15).
- **Resultado:** la XP auto-nivela (multi-nivel, cap 500) y emite eventos. Base reactiva de rango (037) y
  desbloqueos (038).

## F2 · DJT-037 — `RankService` (ninja_xp → rango, reactivo)

- **Qué:** al cambiar la XP ninja, si se cruza un umbral de rango, se detecta y se notifica al jugador. El rango
  0..11 se deriva de `ninja_xp`; solo se reacciona **cuando cambia** (sin recálculo por tick).
- **Archivos (nuevos):** `common/.../progression/RankService.java`; test `progression/RankServiceTest.java`.
- **Archivos (editados):** `core/DojutsuLifecycle.java` (suscribe `RankService` al bus de dominio en el
  arranque); `debug/ProgressionDebugCommand.java` (nuevo `ninjaxp <n>` para cruzar umbrales y ver la notificación).
- **Motivo:** `[S20]`, `[ADD/05 §2]`, `[ADD/03 §2.7]`. Depende de DJT-036, DJT-014.
- **Decisiones técnicas:**
  - **Rango DERIVADO, no cacheado** (coherente con DJT-034): `ProgressionState.rank()` = `NinjaRank.fromXp`. No
    hay campo que actualizar; `RankService` solo **detecta el cambio** para el efecto observable. Satisface
    "cacheado, no fuente de verdad" sin riesgo de desincronización.
  - **Reactivo por evento:** suscrito a `NinjaXpChangedEvent` en el bus compartido `DomainEvents` (server-side).
    Actúa solo si `NinjaRank.fromXp(old) != fromXp(new)` (cruce de umbral). **Sin polling por tick** (`[S20-R3]`).
  - **Efecto = notificación** (`sendSystemMessage`): resuelve `UUID→ServerPlayer` vía `GameInstance.getServer()`
    (Architectury), con guards de servidor/jugador nulos. Cosmético (`[ADD/00 §3]`: el rango no gatea nada).
    La sincronización del rango al HUD llega con el cliente (DJT-041).
  - **Lógica pura testeable:** `rankChangeFrom(oldXp, newXp) -> Optional<NinjaRank>`.
- **Validación:** `./gradlew build` multiloader **verde**; **231 tests** verdes (226 + 5: cruce de umbral, umbral
  exacto, sin cambio, salto de varios rangos, rango máximo). **Validación runtime CONFIRMADA por el usuario en
  NeoForge y Fabric** (2026-07-15).
- **Resultado:** ganar ninja_xp que cruza un umbral sube el rango (mensaje al jugador), una vez por cambio. El
  rango de `/dojutsuprogression get` ya era correcto (derivado); ahora el cruce es observable.

## F2 · DJT-038 — `UnlockService` + `UnlockRegistry` (desbloqueos por hito, reactivo, cierra EP-PROGRESSION)

- **Qué:** al subir una habilidad, se otorgan desbloqueos data-driven cuyo nivel-hito se cruza. Reactivo
  (`SkillLevelChangedEvent`), **idempotente** (`[S20-R4]`), O(hitos) por cambio (sin polling por tick).
- **Archivos (nuevos):** `common/.../progression/UnlockDefinition.java` (record + Codec),
  `UnlockRegistry.java` (holder + consulta), `UnlockService.java` (reactivo), `UnlockReloadListener.java`
  (carga JSON); `common/src/main/resources/data/dojutsu/unlocks/progression.json`; test
  `progression/UnlockServiceTest.java`.
- **Archivos (editados):** `core/DojutsuLifecycle.java` (registra el reload listener + suscribe `UnlockService`);
  `debug/ProgressionDebugCommand.java` (`get` muestra `unlocked`).
- **Motivo:** `[S20]`, `[ADD/05 §Desbloqueos]`, `[ADD/03 §2.7]`. Depende de DJT-036, DJT-016.
- **Decisiones técnicas:**
  - **Data-driven:** `UnlockDefinition(unlockId, skillId, requiredLevel)` cargado de `data/dojutsu/unlocks/*.json`
    (cada archivo un array) por `UnlockReloadListener` (`SimpleJsonResourceReloadListener`, corre en carga de
    mundo y `/reload`, ambos loaders). Establece el **patrón de carga data-driven** que reutilizarán F4/F5.
  - **Reactivo, no por tick:** suscrito a `SkillLevelChangedEvent` en el bus compartido. `unlocksCrossing`
    (`oldLevel < requiredLevel <= newLevel`) maneja el **evento agregado multi-nivel** de DJT-036 (un salto
    otorga todos los hitos del rango) y **no revoca al bajar** de nivel.
  - **Idempotente:** `applyUnlocks` (puro, testeable) solo otorga ids no presentes; `ProgressionState.withUnlock`
    es idempotente por diseño. Acoplado a **nivel de habilidad**, no al rango (`[ADD/00 §3]`).
  - **Efecto F2 = registrar el id** en `ProgressionState.unlocked` + notificar; la entrega del contenido real
    (item de jutsu) es F4 (`// TODO F4`).
  - **JSON provisional:** hitos de kenjutsu (5/10/20/30/40) y fuinjutsu (5/15/30) con niveles **placeholder**;
    los umbrales reales se extraen del datapack al portar los jutsus (F4), como marca `xp_sources.json`.
- **Validación:** `./gradlew build` multiloader **verde**; **237 tests** verdes (231 + 6: cruce de hito, varios
  a la vez, idempotencia, sin hito, otra habilidad, bajar de nivel no revoca). **Validación runtime CONFIRMADA
  por el usuario en NeoForge y Fabric** (2026-07-15).
- **Resultado:** subir una habilidad al hito desbloquea (una sola vez). **Cierra EP-PROGRESSION** (progresión:
  puntos, nivelación, rango, desbloqueos). Falta EP-CLIENT-BASE (keybind/menú/HUD, DJT-039…042).

## F2 · DJT-039 — Keybind "Open Menu" + `MenuActionPayload` (C2S) — abre EP-CLIENT-BASE

- **Qué:** tecla remapeable que abre el menú ninja (Screen real), sin secuestrar el hotbar; y el contrato de
  acciones del menú (`MenuActionPayload` C2S) validado en servidor (el cliente nunca decide gameplay).
- **Archivos (nuevos):** `common/.../network/MenuActionPayload.java`;
  `common/.../client/keybind/KeyBindings.java`; `common/.../client/screen/NinjaMenuScreen.java` (mínima);
  `common/src/main/resources/assets/dojutsu/lang/en_us.json`.
- **Archivos (editados):** `DojutsuMod.initClient()` (registra los keybinds).
- **Motivo:** `[S13]`, `[ADD/05 §Input]`, `[ADD/10 client]`. Depende de DJT-005, DJT-028.
- **Decisiones técnicas:**
  - **Input real por keybind** (`KeyMapping` vía Architectury `KeyMappingRegistry`; sondeo con
    `consumeClick()` en `ClientTickEvent.CLIENT_POST` — NO polling de estado/slot, RM-1). Default `M`,
    remapeable en Controles. Abre la Screen solo con jugador presente y `screen == null` (no colisiona con
    inventario/chat). Reemplaza el "menu item + hotbar secuestrado" del datapack.
  - **Aislamiento cliente:** `KeyBindings`/`NinjaMenuScreen` viven en `client.*` y se invocan **solo** desde
    `initClient()` (no se cargan en servidor dedicado). Lang para el nombre/categoría del keybind y el título.
  - **`MenuActionPayload` (C2S)** en `network` (sin deps de cliente): `Action` (F2: solo `SPEND_SKILL`) +
    `skillId` + `amount`. Contrato de acción extensible (RM-1). **Se define aquí; se registra con handler en
    DJT-042** (no hay stub de handler ni doble registro).
  - **`NinjaMenuScreen` mínima:** read-only, muestra el chakra (`ClientNinjaData`). La lista de habilidades,
    rango, puntos y el gasto llegan en DJT-040 (requieren sincronizar la progresión al cliente).
- **Validación:** `./gradlew build` multiloader **verde**; **237 tests** (sin tests nuevos: keybind/Screen/red se
  validan en runtime). **Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric** (2026-07-15).
- **Resultado:** pulsar la tecla abre el menú ninja (GUI real). Base de input contextual para F5/F6. Falta
  poblar el menú (040), el handler de gasto (042) y el HUD (041).

## F2 · DJT-042 — Handler servidor de `MenuActionPayload` (gasto validado) + sync de progresión

- **Qué:** registrar el payload C2S y aplicar el gasto **solo si es válido** (nunca confiar en el cliente),
  sincronizando el resultado. Incluye **extender el sync con la progresión observable** (necesario para
  "sincroniza el resultado" y para el menú de DJT-040).
- **Archivos (nuevos):** `common/.../network/handler/MenuActionHandler.java`;
  `common/.../network/ProgressionSnapshot.java`; test `network/handler/MenuActionHandlerTest.java`.
- **Archivos (editados):** `network/SyncNinjaDataPayload.java` (+`progression`), `network/NinjaSync.java`,
  `client/ClientNinjaData.java` (cachea progresión), `data/ObservableField.java` (+`PROGRESSION`),
  `data/PlayerNinjaData.java` (`setProgression` marca observable), `core/DojutsuLifecycle.java` (registra el
  handler C2S; `forget` al desconectar), `progression/SkillPointService.java` (sync tras kill),
  `debug/ProgressionDebugCommand.java` (sync tras mutar); test `client/ClientNinjaDataTest.java` (firma).
- **Motivo:** `[ADD/07 "validar todo C2S"]`, `[ADD/10 network]`, `[S13]`. Depende de DJT-035, DJT-039, DJT-027.
- **Decisiones técnicas:**
  - **Validación completa server-side:** emisor real (`ServerPlayer` del `Context`), **anti-spam básico**
    (mín. 50 ms entre acciones por jugador; `forget` en quit → sin fuga de memoria), acción soportada,
    habilidad resuelta **por nombre** (no por índice del cliente), cantidad acotada (`>0` y `<=` cap 500) y
    legalidad del gasto (puntos/cap) delegada en `SkillPointService.spend`. Rechazo **sin efecto** + log.
  - **Sync de progresión:** `ProgressionSnapshot` (skillPoints, ninjaXp, niveles por `Skill.ordinal()`) como 5º
    componente del payload (record anidado: evita el límite de 6 de `StreamCodec.composite`). El **rango no se
    transmite**: se deriva en cliente (`NinjaRank` es dominio común). Desbloqueos y XP por habilidad **no** cruzan
    la red (no son observables del menú).
  - **`setProgression` marca observable siempre** (a diferencia del chakra, la progresión solo cambia en eventos
    discretos: gasto/XP/hito → no genera deltas por tick).
  - **Sync donde cambia lo observable:** el handler tras aplicar, `onKill` tras otorgar puntos, y los comandos de
    debug tras mutar (antes la progresión tardaba hasta un pulso de regen en propagarse).
- **Validación:** `./gradlew build` multiloader **verde**; **244 tests** verdes (237 + 7: parseo por nombre,
  rechazo de índice/inválidos, rate-limit por jugador/intervalo/`forget`). **Sync de progresión CONFIRMADO en
  runtime por el usuario en NeoForge y Fabric** (2026-07-15); el **emisor** del payload (botón "+") llega en
  DJT-040 → el flujo end-to-end se valida con 040.
- **Resultado:** el canal de acciones del menú está cerrado y seguro; la progresión ya viaja al cliente. Listo
  para poblar el menú (DJT-040) y el HUD (DJT-041).

## F2 · DJT-040 — `NinjaMenuScreen` (GUI real con gasto de puntos)

- **Qué:** poblar el menú ninja: chakra, rango/`ninjaXp`, skill points y las 7 habilidades con su nivel y un
  botón "+" que **solicita** gastar 1 punto. Cierra el vertical slice C↔S junto con DJT-042.
- **Archivos (editados):** `common/.../client/screen/NinjaMenuScreen.java` (de mínima a completa);
  `common/src/main/resources/assets/dojutsu/lang/en_us.json` (nombres de habilidades + tooltip).
- **Motivo:** `[S13]`, `[ADD/05 §GUI]`, `[ADD/10 client]`. Depende de DJT-039, DJT-042, DJT-028.
- **Decisiones técnicas:**
  - **El cliente nunca aplica gameplay** (`[ADD/07]`): "+" solo envía `MenuActionPayload(SPEND_SKILL, skill, 1)`
    por `PlatformServices.NETWORK.sendToServer`. El servidor valida (DJT-042), aplica y sincroniza; **la UI se
    refresca cuando llega el sync**, no antes (el `render` lee `ClientNinjaData` en vivo).
  - **Solo lectura de `ClientNinjaData`**; si aún no hay sync (`hasData()==false`) muestra `--`. El **rango** se
    deriva en cliente desde `ninjaXp` (`ProgressionSnapshot.rank()`), no se transmite.
  - **UX:** los "+" se deshabilitan (`tick()`) si no hay puntos observados — es solo UX; la autoridad sigue en el
    servidor (que rechaza igualmente). Tooltip por habilidad; nombres localizables (`skill.dojutsu.*`).
  - **Layout tolerante a valores grandes** (columnas fijas: "Nv 500", chakra de 4+ dígitos) `[ADD/08 §4]`.
  - Reemplaza el "fake-GUI" del datapack (title/actionbar + hotbar secuestrado + sneak) por una `Screen` real.
- **Autorrevisión:** se eliminó un componente sin usar (`SkillRow.skill` → lista de botones) y se importó
  `Tooltip` en vez de cualificarlo inline; tooltip pasado a lang.
- **Validación:** `./gradlew build` multiloader **verde**; **244 tests** (sin tests nuevos: GUI se valida en
  runtime). **Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric** (2026-07-15): con ella queda
  validado también **end-to-end** el handler de DJT-042 (clic "+" → validado → aplicado → sincronizado).
- **Resultado:** el jugador abre el menú, ve su estado real y gasta puntos con feedback tras la confirmación del
  servidor. Falta el HUD (DJT-041) para cerrar la Fase 2.

## F2 · DJT-041 — HUD de chakra y rango (overlays) — cierra EP-CLIENT-BASE y la Fase 2

- **Qué:** overlays permanentes: barra de chakra en tiempo real y rango ninja, leyendo la cache read-only.
- **Archivos (nuevos):** `common/.../client/hud/HudFormat.java` (formato compacto, puro),
  `ChakraHudOverlay.java`, `RankHudOverlay.java`, `HudOverlays.java` (registro + guards);
  test `client/hud/HudFormatTest.java`.
- **Archivos (editados):** `DojutsuMod.initClient()` (registra los overlays).
- **Motivo:** `[S13]`/`[S02]`/`[S20]` (render), `[ADD/10 client]`, `[ADD/08 §4 "valores grandes"]`. Depende de
  DJT-028, DJT-030, DJT-037.
- **Decisiones técnicas:**
  - **Una sola suscripción** a `ClientGuiEvent.RENDER_HUD` (cross-loader, `GuiGraphics`+`DeltaTracker`) en
    `HudOverlays`, con los **guards centralizados**: F1 (`options.hideGui`), espectador, y **sin sync** (no se
    dibujan valores falsos antes del primer full). Los overlays solo dibujan.
  - **Solo lectura de `ClientNinjaData`** (nunca consulta al servidor); el `render` lee la cache en vivo, así la
    barra sigue los deltas (regen/consumo) automáticamente. El **rango se deriva** de la ninja_xp observada.
  - **Formato compacto** (`HudFormat`, clase **pura** → testeable): `8000 -> "8.0k"` (chakra de Jinchuriki
    futuro), `1.5M`. Evita romper el layout con valores grandes.
  - **Sin texturas:** se dibuja con primitivas (`fill`); los assets reales llegan con `EP-ASSETS`. Posicionado
    arriba-izquierda para no solaparse con hotbar/vida/efectos.
- **Validación:** `./gradlew build` multiloader **verde**; **247 tests** verdes (244 + 3 de formato compacto).
  **Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric** (2026-07-15).
- **Resultado:** chakra y rango visibles en todo momento. **Cierra EP-CLIENT-BASE y el código de la Fase 2**
  (vertical slice completo). Tras la validación se genera el cierre documental (`handoff_phase2.md`).

---

# CIERRE DE FASE 2 — 2026-07-15

**Fase 2 (Recursos y progresión — VERTICAL SLICE) COMPLETADA y validada en runtime en NeoForge y Fabric.**

- **Épicas:** EP-CHAKRA (DJT-030…033), EP-PROGRESSION (DJT-034…038), EP-CLIENT-BASE (DJT-039…042).
  **13 tickets, todos ✅** (DJT-032 se dividió en 032a/032b por los eventos loader-nativos).
- **Cobertura:** **247 tests** unitarios verdes; `./gradlew build` multiloader verde. 145 clases Java
  (97 common + 30 tests + 10 fabric + 8 neoforge).
- **DoD de fase alcanzada (demo end-to-end, ambos loaders):** el jugador ve su **chakra regenerar** (barra HUD)
  → **mata mobs** y gana skill points → pulsa **M** y **abre el menú (GUI real)** → **gasta puntos** (payload
  validado en servidor) y **sube de nivel** → gana `ninjaXp` y **sube de rango** (HUD + aviso) → **come/duerme**
  y repone chakra → **muere** y respawnea con **½ chakra** → alcanza hitos y **desbloquea**. Todo persiste al
  reloguear.
- **HITO DE VALIDACIÓN ARQUITECTÓNICA SUPERADO** (`[ADD/08 §6.2]`): red + input + GUI + persistencia + economía
  funcionan juntos. **El resto del proyecto es contenido incremental sobre patrones probados.**

**Decisiones arquitectónicas de F2 (detalle en `handoff_phase2.md`):**
1. `AttributeService` **diferido** (maxChakra es campo de `ChakraState`; F2 no lo exige).
2. **Rango derivado**, no cacheado (no puede desincronizarse).
3. **Auto-nivelación reactiva**, no polling por tick (ningún `PlayerTickSystem` de progresión).
4. **`XpApi` = única puerta de XP** (auto-nivela y emite eventos en un solo sitio).
5. **Eventos loader-nativos** (comer/dormir) suscritos en los **adaptadores**; **primer mixin de Fabric**
   (infraestructura ya montada y reutilizable).
6. `setChakra` marca observable **solo si cambia** lo observable (el timer por tick no genera deltas);
   `setProgression` marca siempre (cambios discretos).
7. `ProgressionSnapshot` **anidado** en el payload (límite de 6 de `StreamCodec.composite`); el rango **no se
   transmite** (se deriva en cliente).
8. **El cliente nunca decide gameplay**: intención por payload, validación server-side (skill **por nombre**,
   rate-limit), UI refrescada tras el sync.
9. **Carga data-driven por reload listener** (`UnlockReloadListener`) = patrón para F4/F5.
10. **Bus de dominio compartido** (`DomainEvents`) para desacoplar emisores de reactivos.

**Bugs encontrados y corregidos durante F2:**
- `SleepChakraHandler` usaba `Level.isDay()`, pero `skyDarken` se calcula al inicio del tick y aún refleja la
  noche cuando el salto de sueño despierta al jugador en ese mismo tick → falso negativo (no reponía). Fix:
  decidir por la **hora del mundo** (`getDayTime() % 24000 < 12000`), ya avanzada al despertar.

**Deuda/hooks abiertos para F3+ (detalle en `handoff_phase2.md`):** `addAffinityXp` (F3, `NatureProfile`);
factores de regen (clan/ADN/sabio) y Jugo inactivos (F3/F6); reset de strain (F5/F6); entrega real de los
desbloqueos y umbrales reales (F4); `TBD_datapack` de `xp_sources.json`; ramen +100 (F8); `AttributeService`;
deltas por-campo; retirar el andamiaje de debug; assets/texturas; `LICENSE`.

---

# FASE 3 — Definición del ninja (natures, clans, onboarding)

**Decisiones acordadas al abrir la fase (antes de escribir código):**
1. **Se mantienen los nombres de fase actuales** de `OnboardingState.Phase` (`NOT_STARTED`/`SELECTING_CLAN`/
   `SELECTING_AFFINITY`/`COMPLETED`). La ficha de diseño habla de `SELECT_CLAN → SELECT_AFFINITY → FINISH →
   DONE`: es nomenclatura, no diseño. "FINISH" es el **paso de transición** a `COMPLETED` (otorgar KG + menu
   item + defaults), no una fase persistida. Se decide en DJT-059.
2. **El módulo `items` nace parcialmente en F3** (DJT-055, solo el learner), aunque `docs/architecture.md §7`
   lo situaba en F5–F9. Manda la ficha de fase. Corregir esa línea al llegar a DJT-055.
3. **Orden de la fase:** 050 → 051 → 052 → 053 → 054 → 055 → 056 → 057 → 058 → **061 → 062** → 059 → 060.
   Desviación consciente respecto a `00_INDICE §6`: 061/062 se adelantan (solo dependen de 056/057) para cerrar
   `ClanService` antes de que el onboarding lo consuma.

## F3 · DJT-050 — `NatureType` + `KekkeiGenkai` + `NatureProfile` — abre EP-NATURES

- **Qué:** el registro del perfil elemental (naturalezas y Kekkei Genkai) y el sub-estado que lo aloja, con las
  afinidades como `LeveledStat`. **Solo estado**: ningún servicio lo escribe todavía (DJT-051).
- **Archivos (nuevos):** `common/.../domain/nature/NatureType.java`, `domain/nature/KekkeiGenkai.java`,
  `common/.../data/state/NatureProfile.java`, `common/.../data/state/LeveledStats.java`; tests
  `domain/nature/NatureTypeTest.java`, `domain/nature/KekkeiGenkaiTest.java`, `data/state/NatureProfileTest.java`.
- **Archivos (editados):** `data/PlayerNinjaData.java` (campo + getter + puerta `setNatures` + 1 línea de codec
  + `toString`); `data/state/ProgressionState.java` (usa el codec compartido); `data/PlayerNinjaDataTest.java`
  (+2 tests).
- **Motivo:** `[S03]`, `[ADD/03 §2.2, §5]`, `[ADD/10 natures]`. Depende de DJT-017 y DJT-014.

**Hallazgos de ingeniería inversa (evidencia directa del datapack, no del resumen de `[S03]`):**
- **`[S03]` dice "18 KG" pero hay 19 ficheros `natures/give/kg/*`** (sin `all`/`random`). Resuelto:
  **`hashirama_cells` NO es un KG** — no escribe ningún `d.kg_*`, escribe **`d.has_hsdna`** (y da medical +10,
  medical_xp +30, ninja_xp +30). Por eso la ficha ya lo modelaba como `boolean hashiramaDna`. Con esa
  corrección, 18 KG + 1 flag de ADN = los 19 ficheros. **El enum tiene 18 constantes.**
- **Hay DOS ejes de afinidad, no uno** (aclara `[S03-R5]`):
  - `d.aff_<nat>` (nivel) + `d.<nat>_xp` (XP) = **la afinidad**. Los predicates `lvl_req/<nat>_0_10`…`_500` leen
    **`d.aff_<nat>`** y las bandas de `menu/leveling/main` son **exactamente la `XpCurve`** ya implementada
    (10/15/20/30/50/60/80/100/120, cap 500). Confirma `[S13-R1]`: **mismo eje que las habilidades**.
  - `d.<nat>_afflvl` + `d.<nat>_aff_xp` = sistema **distinto** (entrenamiento, `jutsu/training/*`, cap **30**)
    cuyo premio al completarse es **otorgar la naturaleza** (`natures/give/<nat>`). **No se modela aquí**: es del
    entrenamiento de jutsus (F4). No confundirlos.
- **Yin-Yang es derivada:** `menu/leveling/affinity/update_yinyang` calcula `d.aff_yinyang = (aff_yin + aff_yang)
  / 2` (división entera) y **no existe `yinyang_xp`**. Además `predicates/has_all_natures` exige solo **7**
  naturalezas (las 5 básicas + yin + yang): **Yin-Yang no cuenta** para el boost. Todo consistente: no almacena
  afinidad propia.
- **Nombres de flag ≠ nombres de fichero:** `shikotsumyaku` → `d.kg_shiko`; `jugo_clan` → `d.kg_jugo`. El mod usa
  el **nombre canónico** del fichero/JSON (no persiste nombres de scoreboard).
- **`TBD_datapack` que quedan cuantificados** (para DJT-051, **no** aplicados aún): ninja_xp por naturaleza =
  **básicas 25, Yin 30, Yang 30, Yin-Yang 100**; por KG **varía** (wood 10, **ice 8**, jugo_clan 20,
  shikotsumyaku 10) → **`[S03]` se equivoca al decir "KG = 10" uniforme**: hay que leer cada fichero. Además
  `give/kg/ice` da **`aff_water +10`** (un KG puede regalar afinidad de un componente) — relevante para DJT-052.
- **Dato para auditar en DJT-051 (fuera de este ticket):** `menu/leveling/inc/fire` sube **`aff_fire +2`** por
  auto-nivel y **resetea** la XP (sin arrastre), y da `ninja_xp +3`. Nuestro `LeveledStat` sube **+1 con
  arrastre** (decisión de F1/DJT-014, `[S13-R3]`). Es una divergencia **consciente y ya cerrada**; se anota aquí
  porque cuantifica el `ninja_xp_per_autolevel` de `xp_sources.json` (= 3 en afinidades).

**Decisiones técnicas:**
- **La afinidad es `LeveledStat`, no un tipo gemelo.** La ficha pedía `Affinity{level,xp}`: es exactamente
  `LeveledStat` (DJT-014), así que se reutiliza. Crear `Affinity` habría duplicado el eje que `[S13-R1]` prohíbe
  duplicar. `NatureProfile` usa `Map<NatureType, LeveledStat>`, igual que `ProgressionState` con `Skill`.
- **Yin-Yang derivada, nunca almacenada** (`affinity(YIN_YANG)` la calcula). Misma postura que el **rango**
  derivado de F2: lo que se puede derivar no se persiste, así no se desincroniza. `withAffinity(YIN_YANG, …)`
  **lanza**: escribirla sería crear una segunda fuente de verdad.
- **`NatureType.withAffinity()` (las 7) coincide exactamente con `predicates/has_all_natures`**, y no por
  casualidad: Yin-Yang queda fuera de ambos por ser derivada. Documentado en el enum y cubierto por test.
- **Codec de `LeveledStat` extraído a `data/state/LeveledStats`** (DRY): lo necesitaban `ProgressionState` y
  `NatureProfile`; era el mismo codec con el mismo saneo. `ProgressionState` pasa a usarlo (mismo NBT, sin
  cambio de formato ni de `dataVersion`).
- **Sets serializados como listas de nombres ordenadas** (salida determinista) y **tolerantes**: un nombre
  desconocido (NBT corrupto o de una versión futura) se **descarta** en vez de romper la carga — misma política
  que `OnboardingState.Phase::byName`. Niveles/XP corruptos se clampan (`LeveledStats.safe`).
- **`setNatures` NO marca observable** (no se añade `ObservableField.NATURES`): hoy ningún consumidor de cliente
  lee el perfil y "solo lo observable cruza la red" (`[ADD/07]`). Se añadirá en DJT-060 (pantalla de creación):
  una línea, igual que con la progresión. **Hook anotado en el Javadoc de la puerta.**
- **`TemporaryGrants` NO entra aquí** aunque la ficha de DJT-050 lo mencione: su clase es de **DJT-053**. Entrará
  por el mismo patrón enchufable sin tocar lo demás (un ticket a la vez).
- **`withoutNature` no borra la afinidad**: el `take/*` del datapack tampoco resetea `d.aff_<nat>`. Fidelidad +
  no perder progreso al revocar. (La política de revocar el **learner**, `[S03-R2]`, es de DJT-051.)
- Sin bumpear `dataVersion`: `optionalFieldOf` cubre los saves de F1/F2 (test explícito).

**Autorrevisión:** (1) un test propio falló al principio por un cálculo mío erróneo (nivel 49 está en la banda
41-80 = 30 XP/nivel, no 10) — el código de producción era correcto; se reescribió el test para que además
demuestre lo que pretendía: la misma XP sobre el mismo nivel da **idéntico resultado** en una afinidad y en una
habilidad. (2) Se eliminó el import `XpCurve` que quedó sin usar en `ProgressionState` tras el refactor. (3) Se
inlineó un alias privado redundante (`LEVELED_STAT_CODEC` → `LeveledStats.CODEC` directo). (4) Se sustituyeron
clases cualificadas inline por imports en los tests.

**Deuda técnica identificada (NO corregida en este ticket → RESUELTA después en DJT-063):** `NatureProfile.DEFAULT` y
`ProgressionState.DEFAULT` son estáticos que contienen `LeveledStat` **mutables**; `DEFAULT.affinity(FIRE)
.addXp(10)` corrompería el default global del proceso. **Es un riesgo latente preexistente de F2**, no
introducido aquí (`ProgressionState` ya lo tenía). No se dispara hoy porque el patrón establecido es
**reemplazar** el stat, nunca mutarlo en sitio. El arreglo limpio es hacer `LeveledStat` inmutable (`addXp`
devolviendo instancia nueva), lo que toca el dominio de F1 y `ProgressionState`/`LevelingService` → merece su
propio ticket. **DJT-051 será el primer escritor de afinidades: decidirlo antes de escribir.**

- **Validación:** `./gradlew build` multiloader **verde**; **273 tests** verdes (247 + 26 nuevos: 7 de
  `NatureType`, 3 de `KekkeiGenkai`, 14 de `NatureProfile`, 2 del agregado). **Sin validación en runtime**: el
  ticket no añade comportamiento observable (es estado puro); la persistencia queda cubierta por los round-trip
  con `NbtOps`. La primera validación en runtime de F3 llegará con DJT-051.
- **Resultado:** el perfil elemental persiste en el agregado con afinidades nivelables en la escala compartida.
  DJT-051 (`NatureService`) ya tiene dónde escribir.

## F3 · DJT-063 — `LeveledStat` inmutable (deuda técnica de F1) — ticket de refactor

- **Qué:** convertir `LeveledStat` (dominio, F1/DJT-014) en un **value object inmutable**: `addXp(int)` (que
  mutaba en sitio y devolvía `void`) pasa a ser **`withXpAdded(int)`**, que devuelve una instancia nueva.
  **Cero cambio de comportamiento de gameplay ni de formato NBT.**
- **Por qué (conflicto técnico real, no estética):** `ProgressionState.DEFAULT` y `NatureProfile.DEFAULT` son
  **estáticos** y contenían `LeveledStat` **mutables**; `DEFAULT.skill(X).addXp(n)` habría envenenado el default
  de **todo el proceso** (todos los jugadores nuevos a partir de ese momento, y el fallo sobreviviría al
  reinicio del mundo sin reiniciar el juego). El estado de `data.state` es inmutable **salvo** en esa hoja, así
  que la garantía se rompía justo en el tipo más manipulado. Detectado en la autorrevisión de DJT-050 y
  **aprobado por el usuario como ticket aparte antes de DJT-051**, que es el primer escritor de afinidades.
- **Numeración:** DJT-063 es el siguiente id libre del rango de F3; **se ejecuta antes que DJT-051**. La
  numeración es orden de asignación, no de ejecución (igual que 061/062, ya reordenados).
- **Archivos (editados):** `common/.../domain/progression/LeveledStat.java` (clase mutable → **record**);
  `common/.../progression/LevelingService.java` (único llamador de producción);
  `data/state/ProgressionState.java` y `data/state/NatureProfile.java` (javadoc: ya no hay advertencia de
  mutabilidad); tests `LeveledStatTest`, `NatureProfileTest`, `ProgressionStateTest`.
- **Motivo:** deuda registrada en la entrada de DJT-050. `[S13]`, `[ADD/00 §3]`. Bloquea a DJT-051.

**Decisiones técnicas:**
- **`record LeveledStat(int level, int xp)`**: el `equals`/`hashCode` por valor que ya se escribía a mano ahora
  lo genera el compilador (menos código, misma semántica). Se conservan la validación (en el constructor
  compacto), el constructor sin argumentos (`new LeveledStat()` = nivel 0) e `isMaxLevel()`.
- **Se renombra a `withXpAdded`, no se mantiene `addXp` devolviendo copia.** Con el nombre viejo,
  `stat.addXp(5);` seguiría **compilando** y sería un no-op silencioso: el peor fallo posible (silencioso y
  plausible). Con `with*` —prefijo que en este proyecto ya significa "copia con"— ignorar el retorno se lee como
  el error que es. **El compilador localizó los 3 usos** (1 de producción, 2 de test): la migración fue completa
  y verificada, no "a ojo".
- **Algoritmo idéntico** (bandas, multi-nivel de una, arrastre del sobrante, descarte en el cap): se trasladó
  tal cual a variables locales. Los 9 tests de comportamiento de DJT-014 pasan **sin cambiar una sola
  aserción** (solo la sintaxis de llamada) — esa es la prueba de que el refactor no toca el gameplay.
- **En el cap `withXpAdded` devuelve `this`** (no una copia): al ser inmutable, compartir la instancia es
  seguro y ahorra basura en el caso más frecuente de un jugador maxeado. Cubierto con `assertSame`.
- **`LevelingService` se simplifica**: su copia defensiva manual (`new LeveledStat(current.level(),
  current.xp())` + `addXp`) desaparece — era un parche para el problema que este ticket elimina de raíz.
  Ahora es `current.withXpAdded(xp)`.
- **No se toca `SkillPointService`**: ya construía instancias nuevas (`new LeveledStat(level + points, xp)`),
  que sigue siendo válido con el record.

**Autorrevisión:** sin hallazgos nuevos. Se revisó que no quedaran referencias al contrato viejo: en código solo
sobreviven **dos menciones deliberadas a `addXp` en el Javadoc** de `LeveledStat` (explican qué se corrigió y por
qué el nombre cambió); en `docs/development_log.md` las menciones son **historial append-only** (no se
reescriben) y la entrada de DJT-050 se anota con un puntero a este ticket; en `docs/handoff_phase2.md` se
corrigió una referencia en prosa que nombraba el método ya inexistente.

- **Validación:** `./gradlew build` multiloader **verde**; **277 tests** verdes (273 + 4 nuevos: no-mutación del
  original, igualdad por valor, y **un test por cada `DEFAULT`** que prueba que operar sobre él ya no lo
  corrompe — el fallo concreto que motivó el ticket). **Sin validación en runtime**: refactor interno sin
  comportamiento nuevo; el gameplay ya cubierto por F2 (nivelación multi-nivel) queda protegido por los tests de
  `LeveledStatTest`/`LevelingServiceTest`, que pasan sin cambios de aserción.
- **Resultado:** el estado del ninja es inmutable **hasta el fondo**. La deuda de F1 queda saldada antes de que
  DJT-051 empiece a escribir afinidades. **Regla para F3+:** un `LeveledStat` nunca se muta; se reemplaza.

## F3 · DJT-051 — `NatureService` (grant/revoke/has) + corrección del bug Yang `[S03-R1]`

- **Qué:** el servicio que otorga, revoca y consulta naturalezas: marca la naturaleza, mueve la ninja_xp
  (simétrica) y emite `NatureGrantedEvent`. Colapsa las 8 funciones `natures/give/<nat>` + 8 `natures/take/<nat>`
  (copy-paste) en una tabla + un servicio. **Primer comportamiento de runtime de la Fase 3.**
- **Archivos (nuevos):** `common/.../natures/NatureService.java`; `common/.../debug/NatureDebugCommand.java`
  (temporal); test `natures/NatureServiceTest.java`.
- **Archivos (editados):** `domain/nature/NatureType.java` (+`ninjaXp()`); `progression/LevelingService.java`
  (+`removeNinjaXp`); `progression/XpApi.java` (+`removeNinjaXp`, javadoc de afinidades); `DojutsuMod.java`
  (registro del comando); `resources/data/dojutsu/balance/xp_sources.json` (RA-4: `gain_nature` cuantificado);
  test `progression/LevelingServiceTest.java` (+3).
- **Motivo:** `[S03]`, `[ADD/00 §2.5]`, `[ADD/05 §2 NatureGrantedEvent]`, `[ADD/03 §2.2]`. Depende de DJT-050,
  DJT-016.

### CONFLICTO DE DISEÑO RESUELTO — la afinidad inicial de 50 **no** es del `grant`

La ficha de DJT-051 pedía, y repetía en su checklist: *"grant(nature) (añade naturaleza, **afinidad inicial =50**
según `[S03]`)"*. **Es una mala lectura de `[S03]`.** Se detuvo el ticket, se expuso la evidencia y **el usuario
aprobó la opción fiel al datapack** antes de escribir código. Evidencia:

- `natures/give/fire` **no fija** `d.aff_fire`: solo lo sanea si fuese negativo (`unless matches 0.. -> set 0`),
  marca `d.has_fire` y suma `ninja_xp 25`.
- El **50 lo pone el onboarding**, justo antes de llamar al give, y solo ahí:
  `main/internal/nature_select_main` (`set @s d.aff_fire 50` → `function natures/give/fire`) y
  `main/internal/random_aff` (idéntico patrón).
- **Prueba definitiva:** `clans/join_uchiha` hace `give/fire` y **luego** `aff_fire += 10` (add, no set). Si el
  grant fijase 50, un Uchiha acabaría con **60 en vez de 10**, y `give/kg/wood` pondría earth/water a 50 en vez
  de 0.

**Decisión:** `grant` **no toca la afinidad**. El 50 es responsabilidad del onboarding (**DJT-059**). Los bonus
de clan (**DJT-057**) suman sobre la afinidad existente, como en el datapack. Registrado también en el Javadoc
de `NatureService` para que no se "recorrija" por la ficha en el futuro.

**Corolario:** el saneo `unless aff_<nat> matches 0.. -> set 0` **no se porta**: la afinidad es un `LeveledStat`,
que no admite negativos por construcción. Era un parche contra scoreboards corruptos.

### Bug Yang `[S03-R1]` — corregido por construcción

En el datapack, `get_random_kg` hace:
`execute ... if score @s d.kg_rng matches 1..2 unless predicate dojutsu:natures/kekkei_genkai/has_yang run function dojutsu:natures/give/kg/yang`
y **ni la función `natures/give/kg/yang` ni el predicate `natures/kekkei_genkai/has_yang` existen** (verificado:
el bug es **doble**, no solo la función como decía `[S03-R1]`). La rama entera está muerta → la tirada 1..2 (2 de
N) **no hacía nada**.

En el mod, Yang **no es un KG**: es un `NatureType`, y `grant(YANG)` es una llamada **tipada** → la referencia
rota es *imposible de escribir*. Cubierto con test explícito. **Mapear la tirada 1..2 → Yang es de DJT-054** (la
tabla de RNG). **Nota para DJT-054:** arreglar esto **cambia las probabilidades reales** respecto al datapack tal
como se ejecuta hoy (donde 1..2 es un no-op); es una corrección intencionada (`[ADD/00 §2.5]`), no un accidente.

**Hallazgo RA-4 adicional:** `[S03]` dice "XP por naturaleza (observado): básicas elementales = 25". **Falso.**
Auditado uno a uno: fire 25, lightning 25, **wind 20, earth 20, water 20**, yin 30, yang 30, yin_yang 100. Los
`take/<nat>` restan **exactamente lo mismo** (simetría verificada en los 8). Cuantificado en
`xp_sources.json` (`gain_nature`) y en `NatureType.ninjaXp()`. De paso queda cuantificado
`ninja_xp_per_autolevel` = **3** (`menu/leveling/inc/<aff>`: `aff +2`, `ninja_xp +3`).

**Decisiones técnicas:**
- **Servicio puro + glue** (patrón de `SkillPointService`): `grant/revoke(data, uuid, nature, bus)` son puros y
  testeables sin MC; las sobrecargas con `ServerPlayer` **solo delegan** y añaden sync + log. La regla de negocio
  vive en **un** sitio (ver Autorrevisión: la primera versión duplicaba la lógica).
- **`ninjaXp()` vive en `NatureType`** (dominio), no en el servicio: es una propiedad intrínseca de la
  naturaleza, y el dominio ya aloja números auditados (`XpCurve`, `NinjaRank`). La XP **de los KG** sí será
  data-driven (JSON) porque varía por KG y la ficha de DJT-052 lo pide.
- **Idempotencia** (`unless has_<nat>` del datapack): re-otorgar no repite XP ni evento **ni resetea la
  afinidad**. Revocar lo que no se tiene es un no-op.
- **`LevelingService.removeNinjaXp` nuevo** (+ `XpApi.removeNinjaXp`): `revoke` debe restar, y `addNinjaXp`
  **ignora por contrato** lo no positivo (hay test de F2 que lo fija). Se añadió un método explícito en vez de
  admitir negativos en `addNinjaXp`, porque relajar aquel contrato convertiría un `addNinjaXp(-5)` accidental en
  una resta silenciosa. La XP sigue pasando por **una sola puerta**.
- **Suelo en 0 para la ninja_xp** (el datapack restaría a negativo): alimenta el rango, y `ProgressionState` ya
  la clampa. Sin cambio ⇒ sin evento.
- **`revoke` no borra la afinidad** (el `take/*` tampoco): no se pierde progreso al revocar y re-otorgar.
- **`addAffinityXp` NO se añade** (la deuda de F2 lo situaba en F3): en el datapack la XP de afinidad se gana
  **usando jutsus** (`jutsu/training/*`), así que su emisor es **F4/DJT-078**. Añadirla hoy sería una API sin
  llamador (YAGNI). Anotado en el Javadoc de `XpApi`.
- **El evento se emite, pero la XP no va por un suscriptor.** La ficha dice que `progression` "consume" el
  evento; se aplica la XP **directamente** (como `SkillPointService.spend`) porque el importe es una propiedad de
  la naturaleza que el emisor ya conoce, y porque el bus **aísla los fallos de los suscriptores** → un error en
  un handler dejaría la economía a medias sin avisar. `NatureGrantedEvent` se emite igual: lo consumirá el
  learner (DJT-055).
- **`TODO DJT-055` (`[S03-R2]`)**: el datapack no revoca el "Skill Learner" al hacer take; la postura del mod es
  revocarlo. Hook anotado en `revoke`.

**Autorrevisión:** (1) **DRY roto**: la sobrecarga de runtime `grant(ServerPlayer)` **duplicaba** la lógica del
servicio puro (marcar + XP + evento) en vez de delegar — dos copias de la misma regla que podían divergir. Se
reescribió para delegar (`XpApi` hace por dentro exactamente lo mismo que el puro con el bus compartido). (2) Se
inventó una clase inexistente (`NinjaSyncSafe`) en el primer borrador → corregido a `NinjaSync`. (3) Import
`NatureProfile` sin usar, eliminado. (4) `CommandSyntaxException` cualificado inline → import; doble
`NinjaData.get` en el comando → una sola resolución.

- **Validación:** `./gradlew build` multiloader **verde**; **293 tests** verdes (277 + 16: 13 de `NatureService`,
  3 de `removeNinjaXp`). **Pendiente de validación en runtime en ambos loaders** (primer ticket de F3 con
  comportamiento observable): ver `/dojutsunatures`.
- **Resultado:** el ninja ya puede ganar y perder naturalezas con su XP exacta, de forma idempotente y emitiendo
  el evento del que colgarán el learner (DJT-055) y los KG (DJT-052). Yang funciona.

## F3 · DJT-052 — Kekkei Genkai data-driven (naturalezas-componente)

- **Qué:** los 18 KG como **datos** (`data/dojutsu/kekkei_genkai/*.json`) + el servicio que los otorga
  **implicando sus naturalezas-componente**. Colapsa las 18 funciones `natures/give/kg/<kg>` (copy-paste) en una
  tabla + un servicio. Recarga con `/reload`.
- **Archivos (nuevos):** `common/.../natures/KekkeiGenkaiDefinition.java`, `KekkeiGenkaiRegistry.java`,
  `KekkeiGenkaiReloadListener.java`, `KekkeiGenkaiService.java`; **18 JSON** en
  `resources/data/dojutsu/kekkei_genkai/`; tests `natures/KekkeiGenkaiServiceTest.java`,
  `natures/KekkeiGenkaiDefinitionTest.java`.
- **Archivos (editados):** `natures/NatureService.java` (+`addAffinityLevels`); `core/DojutsuLifecycle.java`
  (registro del reload listener); `debug/NatureDebugCommand.java` (subcomando `kg`).
- **Motivo:** `[S03]`, `[ADD/03 §5]`, `[ADD/10 natures "Wood=Earth+Water"]`. Depende de DJT-051, DJT-007.

**Datos extraídos del datapack (RA-4, los 18 uno a uno):**

| KG | componentes | ninja_xp | bonus afinidad |
|----|-------------|----------|----------------|
| wood | earth, water | 10 | — |
| ice | wind, water | **8** | **water +10** |
| lava | fire, earth | **12** | — |
| storm | lightning, water | 8 | — |
| boil | fire, water | 8 | — |
| dust | fire, wind, earth | **12** | — |
| magnet | wind, earth | 9 | — |
| explosion | lightning, earth | 10 | — |
| scorch | fire, wind | 8 | — |
| steel | fire, earth, water | 8 | — |
| dark | wind, **yin** | 9 | — |
| swift | wind, lightning | 9 | — |
| crystal | earth, water, **yang** | 9 | — |
| typhoon | wind, water | 9 | — |
| mud | earth, water | 8 | — |
| blaze | **fire** (uno solo) | 10 | **fire +20** |
| shikotsumyaku | **ninguno** | 10 | — |
| jugo_clan | **ninguno** | **20** | — |

**Tercer error de `[S03]` confirmado:** dice "KG = 10" uniforme. **Falso**: va de **8 a 20**. Además el número de
componentes no es siempre 2 (blaze tiene 1; dust/steel/crystal tienen 3; shikotsumyaku y jugo_clan **ninguno**),
dos KG **regalan afinidad** (algo que `[S03]` no menciona en absoluto) y dos usan naturalezas **avanzadas** como
componente (dark→yin, crystal→yang). Ya van **tres** afirmaciones falsas de `[S03]` en tres tickets: por eso se
añadió la **regla 16** a `CLAUDE.md` (el datapack manda sobre los resúmenes de `DOCS/sistemas`).

**Decisiones técnicas:**
- **Los componentes viven en JSON, no en el enum** (`KekkeiGenkai` sigue siendo solo identidad, DJT-050):
  hardcodearlos era el error que la ficha pedía evitar, y así un pack puede redefinir la composición.
- **La implicación reutiliza `NatureService.grant`**, que ya es idempotente → cada componente aporta **su
  propia** ninja_xp y emite **su** `NatureGrantedEvent`, exactamente como el datapack (`give/kg/wood` llama a
  `give/earth` + `give/water`). Wood desde cero = 10 + 20 + 20 = **50** ninja_xp.
- **Refs validadas con codec estricto:** un nombre desconocido (`components: ["plasma"]`, o un `id` que no es
  KG) **falla el parseo** y el archivo se descarta con error en el log. Es lo contrario de los codecs de
  `data.state`, que son **tolerantes** a propósito: allí un dato corrupto no debe impedir cargar una partida;
  aquí una referencia rota en contenido debe **verse**, no colarse como un KG sin componentes. Además el
  listener avisa de cada KG del enum que se quede sin definición ("no se podrá otorgar").
- **Sin definición cargada ⇒ no se otorga** (devuelve `false` y loguea error): mejor nada que un KG a medias.
- **`NatureService.addAffinityLevels`** nuevo (la escritura del perfil sigue pasando por su única puerta):
  `scoreboard players add @s d.aff_<nat> N` del datapack. **Clampa al cap** (500) en vez de rechazar, a
  diferencia de `SkillPointService.spend` — allí el jugador **paga** puntos y perderlos sería injusto; aquí el
  bonus es un regalo. Lo reutilizará DJT-057 (Uchiha: `aff_fire += 10`).
- **`revoke` fiel a `take/kg/<kg>`** (verificado): quita el flag y **solo** la ninja_xp del KG; **no** revoca los
  componentes (quien obtuvo Wood conserva Earth y Water) **ni** retira el bonus de afinidad.
- **Sin evento propio de KG.** No se añade un noveno evento de dominio: hoy no hay consumidor. DJT-055 (learner)
  decidirá si necesita `KekkeiGenkaiGrantedEvent` o le basta con el `NatureGrantedEvent` de los componentes.
  Añadirlo ahora sería especular sobre el contrato del dominio.
- **Los JSON reales están bajo test** (`KekkeiGenkaiDefinitionTest` los lee del classpath): si alguien edita un
  valor o rompe una referencia, falla el build. El contenido enviado se valida, no una copia en el test.

**Hooks anotados (partes de `give/kg/*` que no son de este ticket):** el **learner** por KG (DJT-055; salvo
shikotsumyaku y jugo_clan, que no tienen); los **3 jutsus del clan Kaguya** que `give/kg/shikotsumyaku` entrega
si tienes clan (DJT-061/F4); y `hashirama_cells`, que **no es un KG** (escribe `d.has_hsdna`, +10 medical, +30
medical_xp, +30 ninja_xp) → el flag `NatureProfile.hashiramaDna` existe pero **nadie lo escribe aún**: lo otorgan
ítems/ADN (S14, F8).

**Autorrevisión:** (1) se verificó contra el datapack la afirmación que hice en el Javadoc y en un test sobre
`take/kg/*` (no revoca componentes) — **confirmada**, no asumida. (2) Orden de imports corregido en
`KekkeiGenkaiRegistry`. (3) Los tests resetean el `KekkeiGenkaiRegistry` (estado estático) en `@AfterEach` para
no contaminar a otros. (4) Falsa alarma investigada: un `grep` mío daba 0 JSON en el jar; era el glob de varios
jars — los 18 **sí** se empaquetan (verificado en los tres jars de `common`).

- **Validación:** `./gradlew clean build` multiloader **verde**; **309 tests** verdes (293 + 16: 11 del servicio,
  5 de las definiciones reales). **Pendiente de validación en runtime en ambos loaders.**
- **Resultado:** los 18 KG son datos recargables con `/reload`; otorgar Wood da Wood + Earth + Water con su XP
  correcta. Queda listo el terreno para DJT-054 (el RNG que los reparte).

## F3 · DJT-053 — Naturalezas/KG prestados (`TemporaryGrants`) — corrige `[S03-R3]`

- **Qué:** el modelo de naturalezas y KG **prestados por una fuente** y su retirada. Corrige `[S03-R3]`
  (duplicación permanente/temporal) con **un único** conjunto de naturalezas/KG poseídos + una marca de
  **procedencia** aparte.
- **Archivos (nuevos):** `common/.../data/state/TemporaryGrants.java`,
  `common/.../natures/TemporaryGrantService.java`; test `natures/TemporaryGrantServiceTest.java`.
- **Archivos (editados):** `data/state/NatureProfile.java` (campo `temporary` + wither + codec);
  `debug/NatureDebugCommand.java` (subcomando `tmp` + los préstamos en el `get`).
- **Motivo:** `[S03]`, `[ADD/10 natures S03-R3]`, `[ADD/03 §2.2]`. Depende de DJT-051.

### CONFLICTO DE DISEÑO RESUELTO — "temporal" es PROCEDENCIA, no caducidad

La ficha pedía *"`TemporaryGrants` con **expiración por tick/timestamp**; al vencer, se retiran"* y un *"**tick de
expiración** ligero"*. **En el datapack no existe ninguna expiración por tiempo.** Se detuvo el ticket, se
expuso la evidencia y **el usuario aprobó la opción fiel al datapack** antes de escribir código. Evidencia:

- **`natures/remove_tmp` solo lo llaman los 10 `jinchuriki/removal/*`.** Nadie más. No hay temporizador,
  timestamp, ni tick que sondee.
- **Los únicos que marcan `d.tmp_*`/`d.tkg_*` son los `jinchuriki/sealing/*`**, con este patrón:
  ```
  execute unless predicate dojutsu:natures/has_wind run scoreboard players set @s d.tmp_wind 1
  execute unless predicate dojutsu:natures/has_wind run function dojutsu:natures/give/wind
  ```
  La bestia con cola te **presta** sus naturalezas y marca `tmp_` **solo si no la tenías ya**; al extraerla,
  `remove_tmp` retira **únicamente las prestadas**.

**El flag no es tiempo, es procedencia.** Y ese `unless has_<nat>` es precisamente el mecanismo que garantiza que
retirar un préstamo **nunca** revoque un permanente — el riesgo principal que señalaba la ficha.

**Decisión:** `TemporaryGrants` guarda `naturaleza -> fuente` (p. ej. `"one_tail"`), **sin timestamps y sin
ningún `PlayerTickSystem`** (habría sido polling, prohibido por la regla 11, y además **sin un solo llamador**:
quien presta es el sellado de bijuu, que es **F7**).

**Mejora consciente sobre el datapack:** el datapack **no recuerda la fuente**, así que `remove_tmp` retira
**todos** los préstamos a la vez; con `AllowMultJinchuriki` activo, extraer una bestia te quitaría también lo que
te prestó otra (bug latente). Guardar la fuente lo hace preciso. Con una sola bestia (el caso por defecto) el
comportamiento es **idéntico**. Cubierto con test.

**Decisiones técnicas:**
- **`TemporaryGrants` vive en `data.state`, no en `natures/`** como decía la ficha: es **estado persistido**
  dentro de `NatureProfile` (capa `data`). Ponerlo en `natures/` haría que `data.state` dependiera del módulo de
  servicios, **invirtiendo las capas**. La ruta de la ficha era incompatible con la arquitectura.
- **`TemporaryGrantService` es una clase aparte** y no un método de `NatureService`/`KekkeiGenkaiService`: la
  semántica de préstamo abarca **ambos**, y meterla en cualquiera de los dos crearía un **ciclo** entre ellos.
- **Un solo conjunto de poseídos** (`NatureProfile.natures()`/`kekkeiGenkai()` = todo lo que el ninja tiene, sea
  propio o prestado) + la marca de procedencia. Eso es la corrección de `[S03-R3]`.
- **Prestar un KG NO presta sus componentes implícitamente**: `give/kg/<kg>` los otorga como permanentes, y es el
  **sellado** quien los presta uno a uno **antes** del KG (ver `sealing/ten_tails`, que marca `tmp_earth`/
  `tmp_water` *antes* de `give/kg/wood`). **Contrato anotado para F7** y fijado con un test propio.
- **Sin evento nuevo ni sync nuevo:** los préstamos no son observables (el cliente no los lee); el `sendDelta` va
  por el cambio de ninja_xp, igual que en DJT-051.

**Hooks pendientes:** `d.tmp_sp_chakra` (chakra de seis caminos temporal → `sage/six_paths/take_sp_chakra`) es
del módulo **sage (F6)**: mismo patrón de préstamo, pero su estado no existe aún. El **consumidor real** de todo
este ticket es **F7** (`jinchuriki/sealing|removal`); hoy solo lo ejercita `/dojutsunatures tmp`.

**Autorrevisión — un bug real encontrado por los tests:** `new EnumMap<>(otroMapa)` **lanza
`IllegalArgumentException: Specified map is empty`** si el mapa está vacío y no es ya un `EnumMap` (no puede
inferir la clase de la clave). Es justo el caso de `TemporaryGrants.DEFAULT`, cuyos mapas van envueltos en
`unmodifiableMap` → **los 4 withers petaban en el primer préstamo**. Corregido con un helper que pasa el tipo
explícito (`new EnumMap<>(Type.class)` + `putAll`), documentado para que no se repita. **Verificado que no está
en otro sitio:** `NatureProfile.withAffinity` y `ProgressionState.withSkill` usan el mismo constructor, pero sus
mapas los normaliza el constructor compacto a las **7 entradas siempre**, así que nunca están vacíos y no pueden
lanzar; se dejan como están.

**Otra corrección de autorrevisión:** un test mío esperaba que prestar el KG `magnet` marcase también sus
componentes como prestados. **El código era correcto** (y coincidía con el datapack y con mi propio Javadoc): se
reescribió como `unComponenteNoPrestadoAntesQuedaPermanente`, que ahora **fija ese contrato** para F7 en vez de
contradecirlo.

- **Validación:** `./gradlew build` multiloader **verde**; **319 tests** verdes (309 + 10). **Pendiente de
  validación en runtime en ambos loaders** (`/dojutsunatures tmp`).
- **Resultado:** el ninja puede recibir naturalezas/KG prestados con su procedencia, y al retirar la fuente
  recupera exactamente el estado previo, sin tocar lo propio ni lo prestado por otras fuentes. Sin polling.

## F3 · DJT-054 — `NatureRoll` (asignación aleatoria con los pesos del datapack)

- **Qué:** las tiradas de `main/internal/get_random_kg` + `random_aff` con la **distribución exacta** del
  datapack, data-driven y testeada con semilla. **Solo tira: no aplica nada** (eso es DJT-059).
- **Archivos (nuevos):** `common/.../natures/NatureRollTable.java` (+ `Entry`/`Outcome` anidados),
  `NatureRollRegistry.java`, `NatureRollReloadListener.java`, `NatureRoll.java`, `EnumCodecs.java`;
  `resources/data/dojutsu/rng/nature_roll.json`; test `natures/NatureRollTest.java`.
- **Archivos (editados):** `core/DojutsuLifecycle.java` (reload listener); `debug/NatureDebugCommand.java`
  (subcomando `roll`); `natures/KekkeiGenkaiDefinition.java` (usa `EnumCodecs`).
- **Motivo:** `[S03]`, `[S03-R4]`, `[ADD/10 natures "pesos de RNG"]`. Depende de DJT-015 y DJT-052.

**Pesos extraídos de las loot tables (RA-4):** el truco del datapack (`rolls:{min,max}` + `store result run loot
spawn`) produce un **entero uniforme** en `[1, range]`; el reparto real está en los rangos que compara
`get_random_kg`. Ranges: `random_kg_rng` = **1..50**, `random_aff_rng_2` = 1..10, `gen_ulk_rng` = 1..7,
`random_aff_rng` = 1..5.

| Tirada (1..50) | Resultado | Prob. |
|---|---|---|
| 1..2 | **Yang** (rama rota en el datapack) | 4% |
| 3..17 | un KG concreto (wood, ice, lava, storm, boil, dust, magnet, explosion, scorch, steel, dark, swift, crystal, typhoon, mud) | 2% c/u |
| 30..40 | **Wood, solo Senju** | 22% (Senju) |
| resto | sin premio → tirada de 2ª afinidad (1/10) | 66% / 44% (Senju) |

Siempre, aparte: **genjutsu/Yin 1 de 7**. Con premio: **+20 ninja_xp**.

**Hallazgos:**
- **`blaze`, `shikotsumyaku` y `jugo_clan` NO están en la tirada** (son de clan/evento): la tabla reparte 15 de
  los 18 KG. Cubierto con test.
- **El flag `d.rkg_gkg` se marca por caer en el tramo 1..17 aunque no se otorgue nada** (p. ej. si ya tenías ese
  KG, o por la rama rota de Yang) → **+20 ninja_xp igual** y sin 2ª afinidad. Modelado como `Outcome.claimed`,
  separado de "qué te tocó".
- **Evidencia definitiva del arreglo de Yang** (`[S03-R1]`): la tabla hermana `natures/give/kg/random` tira 1 →
  **`natures/give/yang`**, la referencia **correcta**, mientras que `get_random_kg` llama a la inexistente
  `give/kg/yang`. El arreglo de DJT-051 era exactamente lo que el autor pretendía. **Ojo:** arreglarlo hace que
  ese 4% pase a otorgar Yang de verdad (en el datapack no hacía nada) — corrección intencionada `[ADD/00 §2.5]`.
- **Tabla muerta no portada:** `natures/give/kg/random` (loot `random_kg_rng_alt`, 1..18, con blaze e yin) **no
  la llama nadie**; el propio autor lo admite en un comentario del archivo (*"I legit forgot"*). Portarla sería
  código muerto. Anotado por si un ítem de F8 la necesita.
- `join_finish` salta `get_random_kg` **entero** para el clan Lee (`unless predicate clan/is_lee`) → contrato
  para DJT-059.

**Decisiones técnicas:**
- **El JSON declara `[min, max]` por tramo**, no pesos: así se audita **línea a línea** contra `get_random_kg`.
  `rollTable(senju)` lo convierte al **`RollTable` del dominio** (DJT-015, ya testeado) con peso = tamaño del
  tramo + una entrada "sin premio" con los enteros sobrantes → **distribución idéntica** al entero uniforme.
- **`validate()` al cargar** (`[S03-R4]`): tramos dentro de rango, **sin solaparse** y con un único premio por
  entrada. Una tabla incoherente se **rechaza entera** (log de error + tabla vacía) en vez de repartir premios
  con probabilidades equivocadas **en silencio**, que es el riesgo que señala la ficha.
- **Tira, no aplica.** Devuelve `Outcome` y es DJT-059 quien lo ejecuta por las puertas de siempre
  (`NatureService`/`XpApi`). Mantiene el servicio **puro y determinista con semilla** — que es el DoD del ticket.
- **El clan Senju entra como `boolean`, no como dependencia:** los clanes son DJT-056; `NatureRoll` no debe
  depender de ellos. El onboarding pasará el flag.
- **`EnumCodecs` extraído** (DRY): el codec estricto por nombre estaba **duplicado** entre
  `KekkeiGenkaiDefinition` (DJT-052) y `NatureRollTable`. Además, tenerlo en una clase aparte evita un **ciclo de
  inicialización estática** entre un record y su record anidado (el primero en inicializarse vería `null`).

**Autorrevisión:** (1) el JSON inicial era **anidado** y el codec lo esperaba plano → se rehízo plano (los
codecs ignoran las claves `_comment`, así que la auditoría se conserva). (2) DRY: `EnumCodecs` (arriba). (3) El
reload listener llamaba a `validate()` **dos veces**; ahora una sola, con salida temprana. (4) Imports huérfanos
tras los refactors, eliminados.

- **Validación:** `./gradlew build` multiloader **verde**; **332 tests** verdes (319 + 13), incluidas las de
  **distribución con semilla** (200k tiradas: 4% Yang, 2% por KG, 66% sin premio; Senju 24% Wood) y las que leen
  el **JSON real** del classpath. **Pendiente de validación en runtime en ambos loaders** (`/dojutsunatures roll`).
- **Resultado:** la asignación aleatoria reproduce la distribución canónica y es retunable por datapack. DJT-059
  ya tiene las 4 tiradas que necesita para el FINISH del onboarding.

### F3 · DJT-054 — corrección tras la validación en runtime

**Mis instrucciones de prueba estaban mal, no el código.** Se pidió editar
`common/src/main/resources/data/dojutsu/rng/nature_roll.json` y hacer `/reload` para ver el rechazo de una tabla
incoherente. **`/reload` no lee `src/`**: en dev, el runtime sirve el datapack del mod desde los recursos
**compilados** (`common/build/resources/main/data/dojutsu/...`), que solo se actualizan al ejecutar
`processResources`/`build`. El log lo delató: `Tabla de asignacion aleatoria cargada: rango 1..50, 17 tramo(s)`
= el JSON original. La edición nunca llegó al juego, así que la validación no se ejercitó.

**Cómo probar cambios del datapack del mod en dev** (para futuros tickets data-driven):
- editar `common/build/resources/main/data/dojutsu/**` y `/reload` (rápido, se pierde al recompilar), **o**
- editar `common/src/main/resources/**` + `./gradlew :common:processResources` y luego `/reload`, **o**
- poner un datapack en `<run>/saves/<mundo>/datapacks/` que sobrescriba el id (la vía "de producción").

**Hueco de test cerrado:** el rechazo estaba cubierto solo a nivel de `validate()`; el camino real del
`/reload` (parseo → validate → registro) no. Se añaden **3 tests** que invocan el propio
`NatureRollReloadListener.apply(...)`: tabla con tramos solapados → **registro vacío**; tabla real → cargada
(rango 50, 17 tramos); sin archivo → registro vacío. **335 tests** verdes.

**Lo que sí validó la prueba en runtime:** la distribución. Con 100 tiradas se observó 61% sin premio (esperado
66%), yang 5% (4%), genjutsu 16% (14.3%), cada KG 1-4% (2%) — todo dentro del ruido de n=100.

## F3 · DJT-055 — `LearnerService` + `LearnerItem`/`LearnerComponent` — cierra EP-NATURES

- **Qué:** el "Skill Learner" como **ítem real con data component tipado** (primer uso de data components del
  proyecto, RM-6), entregado reactivamente al obtener una naturaleza o un KG, con validación de posesión al
  usarlo y retirada al perder el elemento. Reemplaza el `carrot_on_a_stick{CustomModelData:1000, LearnId:1b,
  LearnerItem:1b}` multiplexado por NBT, las 24 funciones `give/learner/<elem>` y el `jutsu/learner/check`.
- **Archivos (nuevos):** `common/.../items/LearnerComponent.java`, `items/LearnerItem.java`,
  `registry/ModDataComponents.java`, `natures/LearnerService.java`,
  `domain/event/KekkeiGenkaiGrantedEvent.java`; `assets/dojutsu/models/item/learner.json`;
  test `items/LearnerComponentTest.java`.
- **Archivos (editados):** `registry/ModItems.java` (+`LEARNER`), `natures/KekkeiGenkaiService.java` (emite el
  evento; sweep en revoke), `natures/NatureService.java` (sweep en revoke),
  `natures/TemporaryGrantService.java` (sweep en revoke), `DojutsuMod.java` (+`ModDataComponents.register()`),
  `core/DojutsuLifecycle.java` (+`LearnerService.register()`), `assets/dojutsu/lang/en_us.json` (+33 claves),
  test `KekkeiGenkaiServiceTest` (+1).
- **Motivo:** `[ADD/03 §4 LearnerComponent]`, `[ADD/05 §2]`, `[S03]`, `[S06]`. Depende de DJT-051, DJT-006.

**Cómo funciona el learner en el datapack (evidencia, para F4):** el learner **no se consume al usarlo**. Es un
ítem **reutilizable**: cada click derecho enseña el **siguiente** jutsu de la secuencia fija del elemento
(`d.lrn_<elem>` = índice de progreso; fuego tiene 8), entrega el ítem del jutsu, **reescribe el lore** con el
siguiente y pone cooldown `d.lrn_cd = 14`; solo se consume (`item replace ... with air`) con el **último** jutsu.
`jutsu/learner/check` valida que aún posees el elemento ("Lack required Nature Release." / "Lack required Kekkei
Genkai."). Además `give/learner/<elem>` **resetea `d.lrn_<elem>` a 0**: recibir un learner nuevo reinicia el
progreso del elemento. El "posible error: no consumir el learner" de la ficha partía de un modelo equivocado.
**Todo esto (secuencia, cooldown, entrega, consumo final) es del módulo de jutsus → TODO F4/DJT-075.**

**Alcance F3 del uso:** click derecho → el **servidor** valida la posesión (los dos mensajes exactos del
`check`) y, si posees el elemento, avisa (localizable) de que aún no hay jutsus que aprender. El cliente solo
captura el input.

**Decisiones técnicas:**
- **Componente tipado, no NBT** (`LearnerComponent{element}` con `CODEC` persistente + `STREAM_CODEC` de red):
  el "posible error" que la ficha pedía evitar. `element` se guarda como texto (sobrevive a elementos futuros) y
  se resuelve a `NatureType`/`KekkeiGenkai` al usarse; un elemento desconocido = learner inválido, no crash.
  Los espacios de nombres de naturaleza y KG son disjuntos (verificado).
- **`ModDataComponents`**: primer registro sobre `Registries.DATA_COMPONENT_TYPE` a través del puerto
  `RegistrarService` existente (genérico sobre cualquier registro). **Es el riesgo RM-6 del ticket: validar en
  runtime en ambos loaders.**
- **Entrega reactiva por el bus** (`[ADD/05 §2]`): `LearnerService` se suscribe a `NatureGrantedEvent` (el
  consumidor que el evento esperaba desde F1) y al nuevo `KekkeiGenkaiGrantedEvent`. Así la entrega cubre
  **todos** los caminos de grant (comando, KG que implica componentes, préstamo de bijuu, onboarding y clan
  futuros) sin repetirla en cada llamador — exactamente como cada `give/<elem>` llamaba a `give/learner/<elem>`.
  Con inventario lleno, el ítem se **dropea** (como el `give` vanilla; caso borde de la ficha).
- **`KekkeiGenkaiGrantedEvent` = noveno evento del dominio.** `[ADD/05 §2]` catalogaba 8 y no preveía uno de KG;
  la decisión quedó **explícitamente diferida a este ticket** en la entrada de DJT-052. Se añade porque el KG se
  otorga por varios caminos y sin evento la entrega habría que duplicarla en cada uno (el acoplamiento que el
  bus existe para evitar). Es una **adición** dentro del patrón establecido, no un cambio de arquitectura.
- **Sin learner:** `shikotsumyaku` y `jugo_clan` (verificado: `give/learner/` tiene 24 archivos = 8 naturalezas
  + 16 KG). `LearnerService.hasLearner` lo modela y está bajo test.
- **Política de revoke (`[S03-R2]`, fijada en DJT-051) implementada:** `removeStaleLearners(player)` barre del
  inventario los learners de elementos que ya no se poseen (semántica del `/clear` vanilla). Lo llaman las
  sobrecargas de runtime de los tres revoke (`NatureService`, `KekkeiGenkaiService`, `TemporaryGrantService`).
  El datapack los dejaba como residuo.
- **Modelo del ítem:** reutiliza la **textura vanilla de `carrot_on_a_stick`** (guiño de fidelidad: era el ítem
  base del datapack) — sin assets propios hasta EP-ASSETS. Nombre por elemento vía `ITEM_NAME` +
  `element.dojutsu.<id>` (26 claves de lang con los nombres exactos de los tellraw del datapack, reutilizables
  por la GUI de DJT-060).

**Autorrevisión:** (1) typo de sintaxis (`);` duplicado) en `LearnerItem.use`, corregido antes de compilar.
(2) Orden de imports en `LearnerService` y clase cualificada inline en un test, corregidos. (3) Se verificó que
`hasLearner` no toca registries de MC (testeable sin bootstrap). (4) El javadoc de `KekkeiGenkaiService`
actualizado: el hook de DJT-055 pasó de pendiente a "llega por evento".

- **Validación:** `./gradlew clean build` multiloader **verde**; **341 tests** verdes (335 + 6). **Pendiente de
  validación en runtime en ambos loaders** — imprescindible aquí por RM-6 (primer data component del proyecto).
- **Resultado:** obtener un elemento entrega su Skill Learner con nombre localizado; usarlo valida en servidor;
  perder el elemento lo retira. **Cierra EP-NATURES** (DJT-050…055). Siguen los clanes (DJT-056).

### F3 · DJT-055 — validación en runtime y falsa alarma del barrido

**Validado en runtime en NeoForge y Fabric** (data component persistente y sincronizado: RM-6 desriesgado).
Hubo un reporte de "el learner no se fue" tras `tmp revoke`: los logs demostraron que **toda la cadena
funciona** (entrega → marca → revoke → barrido retirando 1 learner → perfil limpio). La causa del reporte fue
**estado contaminado del mundo de pruebas**: wind ya era propia (quedó de validar DJT-053 en el mismo mundo),
así que `tmp grant wind` fue un no-op (`unless has_wind`) y el learner visible era de la adquisición anterior —
**comportamiento fiel, no bug**. Lección (segunda del mismo tipo tras la de `/reload` en DJT-054): **toda
secuencia de prueba que dependa del estado del ninja debe empezar con `/dojutsu reset_ninja`**, y las
instrucciones de prueba deben incluirlo explícitamente.

## F3 · DJT-056 — `ClanDefinition` (14 clanes en JSON) + `ClanRegistry` + `ClanMembership` — abre EP-CLANS

- **Qué:** los 14 clanes como **datos** (`data/dojutsu/clans/*.json`, recargables con `/reload`) — colapsa las
  14 funciones `clans/join_<clan>` copy-paste — y el sub-estado de pertenencia. **Solo datos + estado**: el
  `join` con bonus es DJT-057.
- **Archivos (nuevos):** `common/.../clans/ClanDefinition.java`, `clans/ClanRegistry.java`,
  `clans/ClanReloadListener.java`, `data/state/ClanMembership.java`, `debug/ClanDebugCommand.java` (temporal);
  **14 JSON** en `resources/data/dojutsu/clans/`; tests `clans/ClanDefinitionTest.java`,
  `data/state/ClanMembershipTest.java`.
- **Archivos (editados):** `data/PlayerNinjaData.java` (sexto sub-estado enchufable: campo + getter + puerta
  `setClan` + 1 línea de codec); `natures/EnumCodecs.java` (+codec de `Skill`); `core/DojutsuLifecycle.java`
  (reload listener); `DojutsuMod.java` (comando); test `PlayerNinjaDataTest` (+2).
- **Motivo:** `[S04]`, `[ADD/03 §2.3, §5]`, `[ADD/10 clans]`. Depende de DJT-007, DJT-050, DJT-017.

**Datos extraídos función a función (RA-4).** La **tabla-resumen de `[S04]` vuelve a tener errores** (regla 16):
omite `kenjutsu +10` de **Hatake** y **Kaguya**, `kenjutsu +5` de **Sarutobi** y `fuinjutsu +10` de
**Namikaze**. Tabla verificada (los 4 omitidos están bajo test):

| clan | naturalezas/KG | aff+ | skill+ | maxChakra | ninjaXp | jutsu/items iniciales | dojutsu potencial |
|---|---|---|---|---|---|---|---|
| hyuga | – | – | taijutsu 10 | 120 | 20 | – | byakugan |
| uchiha | fire | fire 10 | genjutsu 10 | 150 | 30 | – | sharingan |
| otsutsuki | – | – | – | 250 | 0 | – | byakugan, sharingan (50/50) |
| uzumaki | – | – | fuinjutsu 30 | 500 | 15 | – | – |
| chinoike | yin | – | genjutsu 20 | 140 | 60 | – | ketsuryugan |
| nara | yin | yin 10 | – | 100 | 10 | shadow_paralysis | – |
| kaguya | KG shikotsumyaku | – | kenjutsu 10 | 100 | 20 | dance_of_the_camellia | – |
| hatake | lightning | lightning 25 | taijutsu 10, kenjutsu 10, genjutsu 5 | 120 | 85 | white_light_chakra_sabre | – |
| senju | yang | – | medical 20 | 150 | 40 | – | – |
| aburame | – | – | – | 100 | 0 | insect_sphere | – |
| akimichi | yang | – | – | 110 | 0 | three_coloured_pills | – |
| namikaze | KG swift | (aleatorio → 057) | taijutsu 10, fuinjutsu 10 | 140 | 65 | – | – |
| sarutobi | fire (+50% wind → 057) | fire 10 | taijutsu 5, kenjutsu 5 | 180 | 30 | chakra_blade ×2 | – |
| lee | **ninguna** | – | taijutsu 30 | **50** | 90 | – | – |

**Ramas NO estáticas (documentadas, para DJT-057):** Namikaze rollea `random_aff_namikazi` (uniforme 1..2):
1 → wind +25, 2 → lightning +25 (la corrección S04-R3 lo hará **determinista Lightning**); Sarutobi usa la
misma tabla y solo actúa el 1 (**50% de recibir Wind**). La **velocidad 0.15 de Namikaze** (FTG) necesita el
puerto `AttributeService` (diferido). Otsutsuki/Uzumaki activan los `RegenFactor` de regen (hook de F2) — se
enchufan en 057. Nota F4: `join_kaguya` otorga shikotsumyaku **antes** de fijar `d.clan`, así que la rama de
los 3 dances dentro de `give/kg/shikotsumyaku` **no** se dispara en el join (solo da dance_of_the_camellia
explícito).

**Decisiones técnicas:**
- **El clan NO es un enum**: es contenido identificado por id (`"uchiha"`) — un pack puede añadir clanes sin
  tocar código. Por eso `ClanRegistry` es `Map<String, ClanDefinition>` (ordenado por id para listados
  estables) y `ClanMembership` guarda el **id**, nunca el índice numérico `d.clan` del datapack.
- **`ClanDefinition` vive en `clans/`, no en `domain/clan/`** como decía la ficha: el dominio no conoce
  `Codec` (regla dura de EP-DOMAIN). Misma desviación justificada que `TemporaryGrants` (DJT-053) y
  `KekkeiGenkaiDefinition` (DJT-052).
- **`ninjaXp` = solo la explícita del join** (el `add d.ninja_xp N`): las naturalezas/KG otorgados suman la
  suya propia por `NatureService` — igual que en el datapack, donde `join_uchiha` llama a `give/fire` (+25) y
  además añade 30.
- **`potentialDojutsu` como lista de ids** (dato, no predicates `is_*`): Otsutsuki lleva dos (50/50, decide F5).
- **`startingJutsu`/`startingItems` como ids con repetidos permitidos** (Sarutobi lleva 2 chakra_blade): la
  entrega es F4/F8, pero los datos quedan extraídos ya (no habrá que volver al datapack).
- **`ClanMembership.initialized`** = "bonus ya aplicados": la guarda de una-sola-vez que usará el join (057).
  `withClan(id)` marca ambas cosas a la vez.
- **Validación del listener**: codec estricto (`EnumCodecs`, ahora también con `Skill`) + el `id` del JSON debe
  coincidir con el nombre del archivo; archivo inválido se descarta con error en el log sin tumbar el resto.
- **Campos que se añadirán después por diseño** (optionalFieldOf hace las adiciones compatibles): el flag de
  "salta la afinidad" (Lee) lo añadirá DJT-059; el color/sufijo de team, DJT-062; la rama aleatoria de
  Namikaze/Sarutobi la decide DJT-057.

**Autorrevisión:** (1) consistencia del comando de debug: los KG se imprimían en MAYÚSCULAS y las naturalezas
en minúsculas → unificado con `KekkeiGenkaiService::id`. (2) Una clase cualificada inline (`ClanMembership`)
en el comando → import. (3) Los 14 JSON reales están bajo test desde el classpath (mismo patrón que DJT-052):
maxChakra y ninjaXp de los 14, los 4 bonus omitidos por `[S04]`, Lee sin naturalezas, Sarutobi con 2 blades y
las referencias rotas fallando el parseo.

- **Validación:** `./gradlew clean build` multiloader **verde**; **355 tests** verdes (341 + 14: 8 de las
  definiciones reales, 4 del sub-estado, 2 del agregado). **Pendiente de validación en runtime en ambos
  loaders** (`/dojutsuclans`).
- **Resultado:** los 14 clanes son datos recargables y la pertenencia persiste en el agregado. DJT-057
  (`ClanService.join`) ya tiene la tabla y la guarda de idempotencia que necesita.

## F3 · DJT-057 — `ClanService.join` (bonus una vez) + Namikaze determinista + factores de regen enchufados

- **Qué:** unirse a un clan aplica los bonus de su `ClanDefinition` **exactamente una vez** (naturalezas → KG →
  afinidades → habilidades → chakra máximo relleno → ninja_xp → pertenencia), corrige S04-R3 (Namikaze) y
  **salda la deuda de F2**: los factores de regen (Otsutsuki/Uzumaki/Hashirama/Jugo) se derivan del estado real.
- **Archivos (nuevos):** `common/.../clans/ClanService.java`; test `clans/ClanServiceTest.java`.
- **Archivos (editados):** `clans/ClanDefinition.java` (+`randomNature`, +`regenFactor`);
  `data/state/ChakraState.java` (+`withMaxChakra`); `chakra/ChakraService.java` (+`setMax`);
  `chakra/RegenService.java` (**proveedores de factores**; retirado el andamiaje de debug de F2);
  `progression/LevelingService.java` (+`addSkillLevels`); `natures/EnumCodecs.java` (+`REGEN_FACTOR`);
  JSON de namikaze/sarutobi/otsutsuki/uzumaki; `core/DojutsuLifecycle.java`; `debug/ClanDebugCommand.java`
  (+`join`); `debug/ChakraDebugCommand.java` (retirado `regen <factor>`; `get` muestra factores reales);
  tests `ChakraServiceTest` (+2), `LevelingServiceTest` (+2).
- **Motivo:** `[S04]`, `[ADD/00 §2.5]`, `[ADD/03 §2.3]`. Depende de DJT-056, DJT-051, DJT-035.

**Correcciones del datapack aplicadas:**
- **S04-R3 (Namikaze), resuelta EN LOS DATOS:** el datapack rolleaba `random_aff_namikazi` (1..2) sobre el
  temporal global `d.tmp` no garantizado → wind o lightning +25 al azar/frágil. Decisión del mod (ficha):
  `namikaze.json` declara `affinityBonus: lightning 25` **determinista**. Sin código especial: era un dato mal
  modelado. Test con 20 semillas distintas: siempre Lightning 25, nunca Wind.
- **El 50% de Wind de Sarutobi se CONSERVA** (azar intencionado, no bug): campo nuevo
  `randomNature {nature, oneIn}` en `ClanDefinition`, con el azar **inyectado** (`RandomGenerator`) → testeable
  con stubs (siempre-acierta / nunca-acierta).

**Deuda de F2 saldada — factores de regen reales:**
- El clan **declara su factor en datos** (`regenFactor`: `clan_otsutsuki`/`clan_uzumaki` en sus JSON): un pack
  puede dar regen acelerada a un clan propio.
- El **ADN de Hashirama** se deriva de `NatureProfile.hashiramaDna` y el **clan Jugo** (chakra natural) de
  poseer el KG `jugo_clan` — ambos en `RegenService`, puros y bajo test.
- **Retirado el andamiaje de F2**: `RegenService.setDebugForcedFactors`/`debugForcedFactors` y el subcomando
  `/dojutsuchakra regen <factor>` (el handoff lo mandaba al enchufar los factores). `get` ahora muestra los
  factores **derivados del estado real**.

**Autorrevisión — ciclo de módulos detectado y roto:** la primera versión hacía `chakra` → `clans`
(`RegenService` leía `ClanRegistry`) mientras `clans` → `chakra` (`setMax`): **el primer ciclo entre módulos
del proyecto**. Se rompió con **proveedores de factores** (OCP): `RegenService.registerFactorProvider(...)` y
`ClanService.registerRegenFactors()` se registra en el arranque (`DojutsuLifecycle`). Beneficio extra: **F6
(Modo Sabio) registrará su proveedor sin tocar `chakra`** — el `TODO F6: editar factorsFor` desaparece.
También: un sed que rompió el javadoc de `ChakraDebugCommand` (reparado) y tests dedicados para las dos puertas
nuevas (`setMax` con/sin relleno y clamp del caso Lee; `addSkillLevels` con evento, cap y no-positivos).

**Decisiones técnicas:**
- **Puertas nuevas, no escrituras directas:** `ChakraService.setMax(data, max, fill)` (el join rellena; F7
  usará `fill=false` — la extracción del bijuu resta max sin rellenar) y `LevelingService.addSkillLevels`
  (conserva la XP, clampa al cap, **emite `SkillLevelChangedEvent`** → los desbloqueos por hito reaccionan
  también a los bonus de clan; a diferencia de `spend`, clampa en vez de rechazar: el bonus es un regalo, no
  un gasto).
- **Una sola vez:** guard doble (`hasClan() || initialized()`). Re-join del mismo clan, join a otro clan y
  relogin a mitad: **cero re-aplicación** (bajo test).
- **`potentialDojutsu` NO se copia al estado:** el despertar (F5) lo deriva de la pertenencia + registro
  (derivado, nunca cacheado, como el rango).
- **XP del join** = naturalezas/KG (la suya propia, vía `NatureService`) + la explícita del clan. Uchiha desde
  cero: fire 25 + join 30 = **55** (bajo test). Los learners llegan solos por los eventos de DJT-055.
- **Hooks pendientes:** `startingJutsu` (F4) / `startingItems` (F8) se registran en el log al join pero no se
  entregan; team/nametag es DJT-062; la velocidad 0.15 de Namikaze espera al puerto `AttributeService`.
- Los tests del servicio usan las **definiciones reales** de los JSON del classpath (no copias).

- **Validación:** `./gradlew clean build` multiloader **verde**; **369 tests** verdes (355 + 14: 10 del join,
  2 de `setMax`, 2 de `addSkillLevels`). **Pendiente de validación en runtime en ambos loaders**
  (`/dojutsuclans join`).
- **Resultado:** unirse a un clan aplica sus bonus exactos una sola vez, Namikaze es determinista, y
  Otsutsuki/Uzumaki regeneran acelerado de verdad. Falta el reset (DJT-058) para poder cambiar de clan.

## F3 · DJT-058 — `ClanService.reset` (limpia TODO con defaults atómicos) — corrige S04-R2

- **Qué:** el reset de clan reemplaza los **cuatro sub-estados de gameplay** por sus defaults de forma
  **atómica**: pertenencia, perfil elemental (naturalezas, KG, afinidades, ADN, préstamos), progresión
  (habilidades, puntos, ninja_xp, unlocks) y chakra (máximo de vuelta a 100). El **onboarding se conserva**
  (el reset total sigue siendo `reset_ninja`).
- **Archivos (editados):** `clans/ClanService.java` (+`reset` puro y glue); `core/command/AdminCommands.java`
  (`reset_ninja` ahora barre learners residuales); `debug/ClanDebugCommand.java` (+`reset`); test
  `ClanServiceTest` (+3).
- **Motivo:** `[ADD/00 §2.5]`, `[ADD/03 §7 invariantes]`, `[S04]`. Depende de DJT-057.

**Lo que el `reset_clan` original olvidaba** (leído línea a línea): además de lo que documenta S04-R2
(naturalezas `d.has_*`, KG `d.kg_*` y jutsus), **tampoco** limpiaba `d.kenjutsu`, `d.cc`, `d.max_chakra` ni
`d.skill_points` (limpiaba afinidades, ninja_xp y solo 5 de las 7 habilidades). Y un dato más: **nadie llama a
`reset_clan` en el datapack** (solo invocable a mano) — era una utilidad de admin rota. El enfoque atómico de
la ficha ("preferir defaults atómicos" — el error a evitar era exactamente "limpiar campo a campo y olvidar
uno") hace imposible ese tipo de omisión por construcción.

**Decisiones técnicas:**
- **Atómico por las puertas:** el reset escribe `DEFAULT` de cada sub-estado por su puerta (`setClan`,
  `setNatures`, `setProgression`, `setChakra`) — mismo mecanismo que `reset_ninja` (instancia nueva), acotado a
  gameplay. Idempotente y válido **sin clan** (deja el perfil limpio igualmente, como el original).
- **El chakra queda a 0/100** (el `DEFAULT`): la regen lo rellena. El datapack no tocaba el chakra actual, pero
  conservarlo tras volver el máximo a 100 habría requerido lógica parcial — contra el espíritu atómico.
- **Los learners se barren** tras el reset (`removeStaleLearners`, política `[S03-R2]`). **También en
  `reset_ninja`** (ajuste de 1 línea en `AdminCommands`): desde DJT-055 el reset total dejaba learners
  residuales en el inventario — mismo principio "sin residuos", documentado aquí.
- **Préstamos de bijuu (F7):** se borran con el perfil. Cuando exista el sellado, su removal deberá
  reconciliarse con el reset (anotado en el Javadoc).
- **Pregunta abierta para DJT-059:** tras un reset de clan, el jugador queda **sin clan pero con
  `onboarding=COMPLETED`** → el controlador de onboarding debe decidir si un jugador sin clan re-entra en
  `SELECTING_CLAN` (probable) o si el re-join queda solo vía comando/menú. Se decide al diseñar la máquina de
  estados, no aquí.

**Autorrevisión:** sin hallazgos de código; se actualizó el Javadoc del join que aún decía "el reset es
DJT-058". Los 3 tests nuevos cubren el DoD de la ficha: reset tras join + estado poblado → **cero residuos**
(incluidos los campos que el datapack olvidaba: kenjutsu, puntos, max_chakra, unlocks, ADN); reset sin clan y
repetido inofensivos; y **unirse a otro clan tras el reset aplica los bonus correctos sin arrastre** (Uchiha →
reset → Hyuga: sin fire, sin genjutsu, taijutsu 10, maxChakra 120, ninja_xp 20 limpio).

- **Validación:** `./gradlew clean build` multiloader **verde**; **372 tests** verdes (369 + 3). **Pendiente de
  validación en runtime en ambos loaders** (`/dojutsuclans reset`).
- **Resultado:** cambiar de clan es seguro: el reset no deja nada colgando. Quedan 061/062 (stub de jutsu y
  team/nametag) para cerrar EP-CLANS.

## F3 · DJT-061 — Jutsu de clan (stub registrable, una vez por tick) — corrige la doble ejecución

- **Qué:** el **desbloqueo progresivo** de jutsus de clan como `PlayerTickSystem` registrado **una sola vez**
  en el pipeline (`TickPhase.UNLOCKS`), corrigiendo la doble ejecución del datapack. El contador
  (`d.<clan>_time`) avanza y persiste; la **entrega** del jutsu es F4.
- **Archivos (nuevos):** `clans/ClanJutsuUnlockService.java`.
- **Archivos (editados):** `clans/ClanDefinition.java` (+`jutsuUnlockTicks`); `data/state/ClanMembership.java`
  (+`jutsuTime` + wither); JSON de nara/kaguya/akimichi/aburame; `core/DojutsuLifecycle.java` (registro en el
  pipeline); `debug/ClanDebugCommand.java` (`get` muestra el contador); tests `ClanServiceTest` (+3),
  `ClanDefinitionTest` (+1).
- **Motivo:** `[ADD/00 §2.5]`, `[ADD/10 clans "una vez/tick"]`, `[S04]`. Depende de DJT-056, DJT-021.

**La doble ejecución, verificada:** `clans/jutsu/main` estaba registrado en `minecraft/tags/functions/tick.json`
**y** además lo llamaba `main/internal/player_functions` → corría **dos veces por tick** (contadores de
desbloqueo al doble de velocidad, timers de jutsu decrementando 2/tick). En el mod es imposible por
construcción: el sistema se registra en **un único punto** (`DojutsuLifecycle` → pipeline).

**Datos extraídos (RA-4):** los 4 clanes con jutsus ocultos (**Nara, Kaguya, Akimichi, Aburame**; predicates
`new_<clan>_jutsu`) usan el **mismo umbral: 78000 ticks = 65 minutos** de juego por jutsu. Al desbloquear, el
datapack entrega el siguiente de la secuencia y resetea el contador (Nara: shadow_paralysis inicial →
shadow_clutch → shadow_sewing). El umbral vive ahora en `ClanDefinition.jutsuUnlockTicks` (dato, por clan); las
**secuencias** llegan con los jutsus (F4/DJT-075).

**Decisiones técnicas:**
- **El contador se capa en el umbral** (no como el datapack, que seguía sumando hasta que el predicate
  disparaba): llegar al tope no pierde progreso ni puede desbordar; F4 entregará el jutsu al llegar y reseteará
  a 0. `/dojutsuclans get` marca "(LISTO: entrega en F4)".
- **`jutsuTime` vive en `ClanMembership`** (persiste como persistía el scoreboard) y los defaults atómicos del
  reset (DJT-058) lo limpian solos. `withClan` lo arranca en 0.
- **Guards de espectador/muerto**, como el resto de sistemas por tick. Sin clan o clan sin `jutsuUnlockTicks`
  (Uchiha, Lee...): no acumula nada (casos borde de la ficha, bajo test).
- **Escritura por tick sin tráfico de red:** `setClan` no marca observable (mismo patrón que el timer de regen).
- **Nota de rendimiento S04-R6 anotada para F4** (en el Javadoc del servicio): Shadow Clutch recorre y arrastra
  entidades cada tick — diseñar con presupuesto (área acotada, nunca `@e` global).
- La **mecánica** de los jutsus de clan (Shadow Paralysis/Sewing/Clutch, dances, Butterfly Mode, pills, HBT...)
  es F4: este ticket solo porta la pieza por-jugador-por-tick que causaba la doble ejecución.

**Validación del DoD ("contador confirma UNA ejecución por tick"):** el test avanza 100 ticks → contador
exactamente 100 (con doble registro habría sido 200); en runtime, `jutsuTime` debe crecer ≈20/segundo.

- **Validación:** `./gradlew clean build` multiloader **verde**; **376 tests** verdes (372 + 4). **Pendiente de
  validación en runtime en ambos loaders.**
- **Resultado:** el desbloqueo progresivo corre una sola vez por tick con su umbral canónico como dato. Queda
  DJT-062 (team/nametag) para cerrar EP-CLANS.

## F3 · DJT-062 — Team / nametag por clan — cierra EP-CLANS

- **Qué:** el nametag del jugador muestra el color y el sufijo de su clan (`create_teams` de `main/load` +
  `clans/join_team`). Un único punto de entrada (`ClanTeamService.sync`) reconcilia team ↔ clan, llamado desde
  el **join**, el **reset** y el **login** (cubre saves previos al ticket y deriva).
- **Archivos (nuevos):** `clans/ClanTeamService.java`; test `clans/ClanTeamServiceTest.java`.
- **Archivos (editados):** `clans/ClanDefinition.java` (+`teamColor` con codec estricto de `ChatFormatting`);
  los **14 JSON** (+`teamColor`); `clans/ClanService.java` (join/reset llaman a `sync`);
  `core/DojutsuLifecycle.java` (login llama a `sync`).
- **Motivo:** `[ADD/03 §2.3 "render de nametag"]`, `[S00 §3 create_teams]`, `[S04]`. Depende de DJT-057.

**Datos extraídos de `create_teams` (RA-4):** 14 teams con color (Uchiha `dark_red`, Hyuga/Hatake `gray`,
Otsutsuki `white`, Uzumaki/Sarutobi `gold`, Chinoike/Kaguya `red`, Nara `black`, Senju `green`, Aburame
`dark_purple`, Akimichi `dark_red`, Namikaze `yellow`, Lee `dark_green`), sufijo `" <Clan>"` (los 14 coinciden
con el id capitalizado, así que se deriva), `seeFriendlyInvisibles false` y prefix vacío. El team
**`MaskHidden`** (ocultar nametag con máscaras) es de los ítems de **F8** — anotado, no portado.

**Decisiones técnicas:**
- **Teams namespaceados** (`dojutsu_<clan>` en vez del `Uchiha` pelado del datapack): la colisión con teams del
  servidor — el riesgo de la ficha — desaparece por construcción. Creación **perezosa** (no hace falta el
  `create_teams` estático del load).
- **No pisa teams ajenos:** si el jugador está en un team que no es `dojutsu_*`, ni el join ni el reset lo
  tocan (warn en el log **solo** si había un clan que aplicar — corregido en autorrevisión: la primera versión
  habría avisado en cada login de cualquier jugador sin clan metido en un team del servidor). Entre teams
  `dojutsu_*` sí se mueve (cambio de clan tras reset).
- **Color como dato** (`teamColor` en el JSON) con codec estricto: un color inexistente (`fucsia`) o un formato
  que no es color (`bold`) **falla el parseo** del clan.

**Fallo de tests encontrado y resuelto (aprendizaje para F4+):** los 4 tests que **construían `PlayerTeam`**
petaron con `ExceptionInInitializerError`: su constructor arrastra `HoverEvent` → `ItemStack.<clinit>` →
`BuiltInRegistries` → *"Not bootstrapped"*. **`PlayerTeam` no es construible en tests sin bootstrap de MC**, y
la convención del proyecto es testear sin bootstrap. Se reescribió el test dejando lo puro (14 colores, sufijo,
namespacing, codec, y el único camino de `update` que no construye teams); la manipulación de teams (glue fino
sobre la API vanilla) **se valida en runtime**, igual que la GUI/HUD de F2. El build lo verificó el usuario
(incidencia de infraestructura en las herramientas de shell del asistente durante este ticket y el anterior);
el recuento (381, 0 fallos) se verificó leyendo los XML de resultados.

- **Validación:** `./gradlew clean build` multiloader **verde** (ejecutado por el usuario); **381 tests**
  verdes (376 + 5). **Pendiente de validación en runtime en ambos loaders.**
- **Resultado:** el nametag refleja el clan y se limpia al reset, sin tocar teams ajenos. **EP-CLANS completa**
  (DJT-056/057/058/061/062). Quedan solo DJT-059/060 (onboarding) para cerrar la Fase 3.

## F3 · DJT-059 — `OnboardingController` + `OnboardingChoicePayload` (máquina de estados del servidor) — abre EP-ONBOARDING

- **Qué:** la creación del ninja (`main/join` + `join_main` + `join_finish` `[S01]`) como **máquina de estados
  del servidor**: `NOT_STARTED → SELECTING_CLAN → SELECTING_AFFINITY → COMPLETED`, con el paso "finish" (rolls
  de KG/2ª afinidad/genjutsu + cierre) como **transición**, no como fase persistida. El cliente (DJT-060) solo
  enviará **intenciones** (`OnboardingChoicePayload` C2S); el servidor valida fase y elección por nombre.
- **Archivos (nuevos):** `core/onboarding/OnboardingController.java`; `network/OnboardingChoicePayload.java`;
  `network/handler/OnboardingChoiceHandler.java`; `debug/OnboardingDebugCommand.java`
  (`/dojutsuonboarding state|clan <id>|affinity <id>` — simula la Screen hasta DJT-060); test
  `core/onboarding/OnboardingControllerTest.java`.
- **Archivos (editados):** `clans/ClanDefinition.java` (+`skipsAffinity`, default `false`); `clans/lee.json`
  (`"skipsAffinity": true`); `natures/NatureService.java` (+`setAffinityLevel`: SET absoluto preservando XP);
  `core/DojutsuLifecycle.java` (payload registrado; `refresh` en el login **antes** de `sendFull`);
  `debug/ClanDebugCommand.java` (`reset` re-embarca al instante); `DojutsuMod.java` (registro del comando);
  `lang/en_us.json` (+`onboarding.dojutsu.*`, +14 `clan.dojutsu.<id>`).
- **Motivo:** `[ADD/07 flujo onboarding]`, `[S01]`, `[S03 get_random_kg]`. Depende de DJT-054/057; DJT-060
  consume la fase observable.

**Fidelidad al datapack (auditado contra `.mcfunction`, no contra el resumen — regla 16):**
- **La afinidad elegida se FIJA a 50** (`nature_select_main`: `set aff 50` y luego `give`): un Uchiha
  (aff_fire 10 del join) que elige Fuego queda en **50, no 60**. Para eso existe el nuevo gate
  `NatureService.setAffinityLevel` (SET absoluto; `addAffinityLevels` seguía siendo suma).
- **Caso Lee** (`skipsAffinity`): salta la selección de afinidad **y** el roll de KG entero — **ambas** ramas
  del datapack condicionan a `is_lee` (`join_finish: unless is_lee`). Dato en el JSON, no hardcodeado.
- **Finish = `get_random_kg` exacto:** roll principal (Yang en 1–2 `[S03-R1 corregido]`, KG en 3–17, Madera
  30–40 solo Senju); **solo sin premio** se tira la 2ª afinidad (1/10, `set 50 + give`); **siempre** se tira
  genjutsu (1/7: +10 niveles de genjutsu, +10 ninja_xp, naturaleza Yin); el premio da **+20 ninja_xp**.
- **`random` como elección de afinidad** (`random_aff`: uniforme entre las 5 básicas). El clan aleatorio queda
  para DJT-060 (es un botón de la Screen, no una regla del servidor).
- **No portado (obsoleto en el mod):** `assign_plys`/límite de 10 jugadores (UUID desde F1), títulos + hotbar
  secuestrado (serán la Screen de DJT-060) y el "menu item" del finish (el menú es la tecla **M** desde F2).

**Decisiones técnicas:**
- **El avance es SOLO del servidor:** un payload de fase incorrecta o con id inválido se **rechaza sin efecto**
  (y el handler lo loguea). No hace falta rate-limit: el gate de fase convierte los reenvíos en no-ops.
- **Fase observable** (`setOnboarding` marca sync): la Screen de DJT-060 sabrá qué pantalla mostrar sin canal
  nuevo.
- **`refresh` en cada login** reconcilia la fase: nuevo → `SELECTING_CLAN`; **`COMPLETED` sin clan (reset) →
  re-embarque** (la pregunta abierta de DJT-058, decidida aquí); una desconexión a mitad **reanuda** en la fase
  correcta sin repetir bonus (guard `initialized` del join).
- **Puertas únicas respetadas:** el controlador no escribe estado salvo su propia fase — clan vía
  `ClanService.join`, afinidad vía `NatureService`, XP vía `LevelingService`, KG vía `KekkeiGenkaiService`.
- Patrón puro+glue de siempre: métodos puros con `UUID`/`RandomGenerator`/`DomainEventBus` inyectados;
  overloads de `ServerPlayer` delegan y añaden team sync, delta y mensajes traducibles.

**Tests (10):** con los **JSON reales** del classpath (clanes, `wood.json`, `nature_roll.json` — RA-4) y el
azar por **cola de valores**: flujo Uchiha completo (XP exacta 75), SET-50 (no 60), Lee sin ningún roll
(el RNG del test **explota** si se consulta), rechazo por fase/elección inválida, reanudación tras relogin,
re-embarque tras reset, `random`, roll con premio (wood: XP 115 con componentes), Yang + genjutsu + 2ª afinidad.

- **Validación:** `./gradlew clean build` multiloader **verde** (ejecutado por el usuario; incidencia de
  infraestructura del shell del asistente de nuevo); **391 tests** verdes (381 + 10), recuento por los XML.
  **Pendiente de validación en runtime en ambos loaders** (`/dojutsuonboarding`).
- **Resultado:** el ninja se crea entero desde el servidor con la distribución canónica. Queda **DJT-060**
  (la Screen que envía los payloads) para cerrar EP-ONBOARDING y la Fase 3.

## F3 · DJT-060 — `NinjaCreationScreen` + `OnboardingOptionsPayload` + clan aleatorio — cierra EP-ONBOARDING y F3

- **Qué:** la GUI de la creación del ninja que reemplaza el fake-GUI del datapack (`join_main` +
  `join_clan_ab`/`ab2` + `nature_select_main`: títulos + hotbar secuestrado con sneak-para-elegir en páginas
  de 9 slots). Dos páginas decididas por la **fase sincronizada**; cada botón envía la **intención**
  (`OnboardingChoicePayload`) y la pantalla avanza cuando llega el sync del servidor.
- **Archivos (nuevos):** `client/screen/NinjaCreationScreen.java`; `client/screen/NinjaCreationOpener.java`
  (abre/reabre la pantalla por tick de cliente); `network/OnboardingOptionsPayload.java` (S2C: opciones de
  clan `(id, color)`); `network/OnboardingSync.java` (registro + emisor + receptor, patrón `NinjaSync`);
  `client/ClientOnboardingOptions.java` (cache read-only de las opciones).
- **Archivos (editados):** `core/onboarding/OnboardingController.java` (+clan `random`; `refresh` envía las
  opciones cuando queda en `SELECTING_CLAN`; el mensaje de join usa el clan **real** del estado, no el payload);
  `core/DojutsuLifecycle.java` (registro del payload); `DojutsuMod.java` (opener + limpieza de la cache al
  salir); `debug/OnboardingDebugCommand.java` (sugiere `random`); `lang/en_us.json` (+6 claves); tests (+2).
- **Motivo:** `[ADD/05 §GUI]`, `[ADD/10 client]`, `[S01]`, `[S13]`. Depende de DJT-059. **Cierra la Fase 3.**

**Hallazgo de auditoría (RA-4):** el "Random Clan" **existía en el datapack** (`clans/join_random`: loot
`clan_rng` uniforme 1..14 sobre los 14 clanes, **Lee incluido** — puede completar el ninja de una, sin pantalla
de afinidad). Estaba anotado como "diferido a 060" y resultó ser el slot 0 de la primera página del fake-GUI.
En el mod: tirada **del servidor**, uniforme sobre los ids **ordenados** del registro (determinista con semilla
para tests; con los 14 del datapack la distribución es idéntica).

**Decisiones técnicas:**
- **La página la decide la fase sincronizada, nunca el cliente:** `SELECTING_CLAN` → grid Random + clanes;
  `SELECTING_AFFINITY` → Random + 5 básicas; `COMPLETED` → la pantalla se cierra sola. El **caso Lee** sale
  gratis (el servidor salta la fase de afinidad y el delta cierra la pantalla).
- **`OnboardingOptionsPayload` (S2C):** el `ClanRegistry` es data-driven del servidor (en dedicado el cliente
  no lo tiene), así que la lista de clanes viaja por red con el RGB de su `teamColor` (DJT-062). Es **solo
  presentación**: la elección vuelve por nombre y el servidor la re-valida contra el registro real.
- **No se puede saltar, sin brickear el cliente** (riesgo de la ficha): `NinjaCreationOpener` reabre la
  pantalla si se cierra sin elegir, con un **margen de 2 s** que permite llegar al menú de pausa
  (guardar/salir). Con `COMPLETED` deja de actuar.
- **Anti doble-click:** tras enviar una elección los botones se desactivan hasta que el sync cambie la fase
  (con timeout de reactivación por si el servidor rechazara). El `join_delay` de 14 ticks del datapack no se
  porta: era el anti doble-click del truco por slots.
- **Colores fieles:** clanes con su `teamColor`; afinidades con los hex exactos del actionbar de
  `nature_select_main` (fire `red`, wind `#00D789`, lightning `yellow`, earth `#B5741B`, water `#389BFE`).
- **Orden alfabético de clanes** (única desviación cosmética documentada): el orden por páginas del datapack
  era un artefacto de la hotbar de 9 slots, no un ranking de diseño; ordenar hace determinista el índice del
  random y encaja clanes nuevos de packs sin tocar código.
- Fallo de compilación y arreglo: `StreamCodec.map` es invariante en el buffer (`ByteBuf` vs
  `RegistryFriendlyByteBuf`); el codec de la lista se construye con `StreamCodec.composite` (acepta `? super B`).

- **Validación:** `./gradlew clean build` multiloader **verde**; **393 tests** verdes (391 + 2: clan random
  determinista y clan random → Lee salta afinidad). La Screen/opener se validan en runtime (glue fino de
  cliente, como la GUI/HUD de F2). **Validado en runtime en NeoForge y Fabric.**
- **Resultado:** un jugador nuevo crea su ninja entero por GUI (demo de aceptación de la fase).
  **EP-ONBOARDING completa → FASE 3 COMPLETADA** (los 14 tickets validados). Cierre: `docs/handoff_phase3.md`.

---

# Fase 4 — Combate

---

## F4 · DJT-070 — `NinjaProjectile` base (física + colisión por raycast + dueño UUID) · #riesgo-alto RM-4 — abre EP-ENTITY

- **Qué:** la entidad-proyectil reutilizable que reemplaza los armor stands teletransportados del datapack
  (S06-R5/S16-R1): velocidad inicial configurable, gravedad opcional, impacto por **raycast de segmento**
  (bloque/entidad, sin tunneling), dueño por **UUID** (DJT-020), callback de impacto enchufable, vida máxima y
  **no persistente**. Base de 6 sistemas (jutsu/dojutsu/sage/throwing/summon/bijuu).
- **Archivos (nuevos):** `common/.../entity/NinjaProjectile.java`; `common/.../util/ProjectileMath.java`,
  `RaycastHelper.java` (nace el paquete `util`); `debug/ProjectileDebugCommand.java` (temporal); test
  `util/ProjectileMathTest.java`.
- **Archivos (editados):** `registry/ModEntities.java` (+`ninja_projectile`); `DojutsuMod.java` (comando);
  `fabric/DojutsuFabricClient.java` y `neoforge/client/DojutsuNeoForgeClientEvents.java` (+renderer).
- **Motivo:** `[ADD/03 §3]`, `[ADD/08 RM-4]`, `[ADD/10 entity]`, `[S06]`, `[S16]`. Riesgo crítico de F4:
  probar la semántica de colisión ANTES de que las familias de efecto (DJT-076) construyan encima.

**Hallazgo de plataforma (importante para F4+):** el constructor de `Projectile` es **package-private en el
jar vanilla** contra el que compila `common`; NeoForge lo ensancha a `protected` por Access Transformer (por
eso los ejemplos NeoForge-only compilan y `common` no). Verificado con `javap` sobre
`minecraft-merged-mojang.jar` vs `minecraft-merged-mojang-at-patched.jar`. **Solución sin tocar el build:**
extender **`ThrowableItemProjectile`** (constructor público), que además regala el item de display
sincronizado + `ItemSupplier` (compatible con `ThrownItemRenderer` sin assets propios). Alternativa descartada:
access widener común (infra de build innecesaria habiendo base vanilla adecuada).

**Decisiones técnicas:**
- **Física propia, sin fricción:** se sobreescribe `tick()` completo (`baseTick()` + lógica propia) en vez de
  heredar el del throwable vanilla, que frena 1%/tick (0.99) y 20% en agua — los proyectiles del datapack
  volaban a velocidad constante. La física pura vive en `ProjectileMath` (avance, gravedad como resta/tick en Y,
  expiración con frontera inclusiva), **testeable sin Minecraft** (patrón servicio puro + glue).
- **`RaycastHelper` como costura de colisión:** delega en `ProjectileUtil.getHitResultOnMoveVector` (segmento
  posición→posición+velocidad → sin tunneling a ninguna velocidad; dos entidades en la trayectoria → gana la
  más cercana; puede impactar en el mismo tick del spawn). Los sistemas de F4+ dependen del helper, no de la
  utilidad vanilla: un cambio de semántica (piercing, fluidos) se hace en un punto.
- **Detección y expiración solo en servidor;** el avance corre en ambos lados (el cliente interpola suave
  entre syncs con la misma física). **La gravedad es dato sincronizado** (`EntityDataAccessor<Float>`), no un
  campo solo-servidor — ver autorrevisión.
- **Dueño por UUID propio** (campo + `ownerUuid()`/`ownerRef()` → `PlayerRef`): disponible aunque el dueño esté
  offline (el vanilla `getOwner()` devuelve null si no resuelve). El dueño nunca es golpeado por su propio
  proyectil. Al no ejecutar `Projectile.tick()` (flags privados), no se emite el game event `PROJECTILE_SHOOT`
  (sculk) — limitación documentada en el Javadoc, se revisará si algún sistema lo necesita.
- **No persistente por construcción:** `shouldBeSaved()=false` (el DoD exige que desaparezca al recargar el
  chunk). El datapack necesitaba barridos de limpieza de armor stands precisamente porque persistían.
- **Callback tras `super.onHit`** (conserva interacciones vanilla del bloque golpeado) y descarte incondicional.
- Comando temporal `/dojutsuprojectile shoot [speed] [gravity] [life]`: dispara desde los ojos en la dirección
  de la mirada y reporta el impacto por chat. Captura `PlayerRef`, no al jugador (dueño offline al impactar =
  no-op, caso borde de la ficha).

**Autorrevisión — 2 hallazgos corregidos:**
1. **Gravedad no sincronizada:** era un campo solo-servidor pero el cliente ejecuta la misma física → las
   trayectorias curvas darían saltos visuales en cada sync (updateInterval 10). Ahora viaja como synched data.
2. **Callback sin aislamiento de fallos:** una excepción en el callback impedía el `discard()` → el proyectil
   re-impactaría cada tick spameando el error. Try/catch + log (mismo principio que `DomainEventBus`) y el
   descarte ocurre siempre.

- **Validación:** `./gradlew build` multiloader **verde**; **403 tests** verdes (393 + 10 de `ProjectileMath`:
  avance, gravedad solo-Y/acumulada/identidad-en-0, rechazos de gravedad negativa/no-finita, fronteras de
  expiración inclusivas, vida no positiva rechazada). **Validación runtime CONFIRMADA por el usuario en NeoForge
  y Fabric** (2026-07-16): suite manual de colisión (pared / mob / vacío-expira / alta velocidad sin tunneling /
  no persiste al recargar / dueño offline).
- **Resultado:** el proyectil base funciona con colisión real y física testeada — **RM-4 desriesgado**.
  Siguientes: DJT-071 (ancla/marcador) y DJT-072 (consolidación del registro).

## F4 · DJT-071 — `AbilityAnchor` + `MarkerEntity` (ancla posicional + marcador transitorio)

- **Qué:** las otras dos primitivas de EP-ENTITY. `AbilityAnchor`: ancla de una habilidad activa con **tick
  propio** (callback enchufable) y **expiración garantizada por construcción** (base de las habilidades
  ancladas de F5 oculares / F6 sage). `MarkerEntity`: marcador genérico transitorio (posición + caducidad +
  dueño opcional), sin lógica — quien necesite comportamiento usa un ancla. Reemplazan los armor stands "de
  servicio" y "chincheta" del datapack, que exigían barridos de limpieza.
- **Archivos (nuevos):** `common/.../entity/AbilityAnchor.java`, `MarkerEntity.java`;
  `debug/AnchorDebugCommand.java` (temporal).
- **Archivos (editados):** `registry/ModEntities.java` (+`ability_anchor`, +`marker`); `DojutsuMod.java`
  (comando); los renderers por loader (+2 `NoopRenderer` cada uno).
- **Motivo:** `[ADD/03 §3]`, `[ADD/10 entity]`, `[S05]`, `[S08]`. Depende de DJT-070.

**Decisiones técnicas:**
- **Sin anclas colgadas, por construcción** (el riesgo de la ficha): la vida máxima es **parámetro obligatorio**
  de `create(level, owner, lifetimeTicks)` — no existe forma de crear un ancla o marcador eternos. Al expirar
  corre el callback de limpieza (si lo hay) y se descarta. Un modo "mientras esté activo" crea el ancla con la
  vida del modo o la descarta él mismo al desactivar.
- **Huérfana ≠ colgada:** si el dueño se va, el ancla sigue su ciclo y expira igual; su callback resuelve al
  dueño vía `PlayerRef` (offline → no-op). Mismo patrón de dueño por UUID que el proyectil.
- **Aislamiento de fallos como política de EP-ENTITY:** un tick-callback que lanza excepción **descarta el
  ancla** (si solo se logueara, fallaría cada tick y spamearía el log); el expire-callback roto se loguea y el
  descarte ocurre igual. Coherente con `DomainEventBus` y el callback del proyectil.
- **Tracking asimétrico:** el ancla viaja al cliente (`clientTrackingRange 4` — F5/F6 colgarán visuales de
  ella; renderer `Noop` hoy); el marcador **no** (`trackingRange 0`, como el `Marker` vanilla: puro estado de
  servidor, cero tráfico de red).
- **DRY:** la expiración reutiliza `ProjectileMath.isExpired` (ya testeado en DJT-070). Sin tests nuevos: el
  único fragmento puro ya está cubierto y las entidades son glue fino sobre `Entity` (no construible sin
  bootstrap de MC — precedente `PlayerTeam`/DJT-062) → validación en runtime.
- **No persistentes** (`shouldBeSaved()=false`). Si un sistema futuro necesitara anclas persistentes, diseñará
  su persistencia entonces (anotado en el Javadoc; hoy YAGNI).
- `DebugMarkerEntity` (placeholder F0) queda **superado** por `MarkerEntity`: su retirada se propone en
  DJT-072 (consolidación del registro), no aquí.
- Comando temporal `/dojutsuanchor spawn|marker [life] · count`: latido por chat cada segundo de vida, aviso al
  expirar, y recuento por dimensión para verificar que nada queda vivo.

**Autorrevisión:** sin hallazgos de código. Nota de semántica documentada: con vida N, el tick-callback corre
en las edades 1..N-1 y la edad N es el tick de expiración (el `tickCount` se incrementa en `baseTick` antes de
la comprobación); frontera inclusiva, igual que el proyectil.

- **Validación:** `./gradlew build` multiloader **verde**; **403 tests** verdes (sin cambios: no hay lógica pura
  nueva). **Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric** (2026-07-16): latido y expiración
  del ancla, múltiples anclas, marcador silencioso con `count`, no-persistencia al recargar, huérfana sin errores.
- **Resultado:** las tres primitivas de entidad (proyectil/ancla/marcador) existen y funcionan. Queda DJT-072
  (consolidación del registro + retirada propuesta del placeholder) para cerrar EP-ENTITY.

## F4 · DJT-072 — Consolidación de `ModEntities` (retira `DebugMarkerEntity`) — cierra EP-ENTITY

- **Qué:** `ModEntities` queda como registro real de las tres primitivas de EP-ENTITY. Se **retira
  `DebugMarkerEntity`** (placeholder de F0, deuda de andamiaje abierta desde entonces), superada por
  `MarkerEntity` (DJT-071). Retirada **autorizada explícitamente por el usuario**.
- **Archivos (eliminados):** `common/.../entity/DebugMarkerEntity.java`.
- **Archivos (editados):** `registry/ModEntities.java` (registro `debug_marker` retirado; javadoc consolidado);
  `fabric/DojutsuFabricClient.java` y `neoforge/client/DojutsuNeoForgeClientEvents.java` (renderer retirado).
- **Motivo:** `[ADD/01 §3 registry]`, `[ADD/10 entity]`. Depende de DJT-070/071.

**Decisiones técnicas:**
- Verificado por grep antes de retirar: **nada** referenciaba `DebugMarkerEntity` salvo su registro y sus dos
  renderers (ningún comando, ningún recurso). Retirada limpia en 4 puntos.
- **Mundos de prueba con un `debug_marker` guardado:** al desaparecer el tipo, MC descarta la entidad con un
  warning benigno al cargar el chunk (mismo comportamiento que cualquier entidad de un mod desinstalado). Sin
  impacto: la entidad era un marcador invisible de depuración.
- El javadoc de `ModEntities` documenta el cierre de EP-ENTITY y que las entidades de contenido (bestias,
  summons, shinobi IA) se añadirán aquí en F7/F8.

**Autorrevisión:** sin hallazgos; el DoD de la ficha ("cada entidad spawnea en ambos loaders sin crash de
renderer") se cubre con la suite manual de abajo (los comandos de 070/071 + `/summon` de los 3 tipos).

- **Validación:** `./gradlew build` multiloader **verde**; **403 tests** verdes. **Validación runtime CONFIRMADA
  por el usuario en NeoForge y Fabric** (2026-07-16): spawn de las 3 entidades sin crash, `debug_marker`
  inexistente, mundo viejo carga con warning benigno.
- **Resultado:** **EP-ENTITY completa** (DJT-070/071/072): proyectil, ancla y marcador registrados, renderizados
  y sin andamiaje. Siguiente épica: EP-JUTSU-ENGINE (DJT-073, `JutsuRegistry`).

## F4 · DJT-073 — `JutsuRegistry` + carga de `JutsuDefinition` (reload) — abre EP-JUTSU-ENGINE

- **Qué:** los jutsus como **datos recargables**: `JutsuEntry` (codec estricto que construye el
  `JutsuDefinition` **de dominio ya existente** — DJT-013 — + parámetros de efecto en crudo), `JutsuRegistry`
  (id→entrada, ordenado) y `JutsuReloadListener` sobre `data/dojutsu/jutsu/**` (subcarpetas por naturaleza).
  **4 JSON piloto** de fuego/rayo con los valores del datapack real. Reemplaza la tabla de despacho de
  `run_jutsu` (141 ramas `if score matches N`).
- **Archivos (nuevos):** `jutsu/{JutsuEntry, JutsuRegistry, JutsuReloadListener}.java`;
  `debug/JutsuDebugCommand.java` (temporal); `resources/data/dojutsu/jutsu/fire/{great_fireball,fire_bullet}.json`,
  `lightning/{lightning_bolt,chidori}.json`; test `jutsu/JutsuEntryTest.java`.
- **Archivos (editados):** `core/DojutsuLifecycle.java` (listener registrado); `DojutsuMod.java` (comando).
- **Motivo:** `[ADD/03 §5]`, `[ADD/04 §3]`, `[ADD/10 jutsu]`, `[S06]`. Depende de DJT-013, DJT-007. RA-1.

**Valores auditados contra los `cast.mcfunction` (regla 16, no contra `[S06]`):** great_fireball 140 chakra /
cd 100t (timer propio `d.gfbtimer`) / aff fire ≥170 · fire_bullet 60 / 100t (`d.fb_timer`) / ≥30 ·
lightning_bolt 60 / 30t (**timer global compartido `d.jutsu_timer`**) / ≥30 · chidori 100 / **400t**
(`d.chidori_cd`, consumo y cd en `chidori/ready`) / ≥120. **Hallazgo para DJT-077:** el datapack mezcla timers
propios con un timer global compartido entre jutsus; el ADD unifica por-instancia (corrige S06-R3). Los ticks
se registran con semántica corregida de 1 decremento/tick (la doble ejecución del datapack — DJT-061 — los
decrementaba a 2/tick).

**Decisiones técnicas:**
- **El codec vive en `jutsu`, no en `domain`** (regla dura EP-DOMAIN) — misma desviación justificada que
  `KekkeiGenkaiDefinition`/`ClanDefinition`. El dominio (DJT-013) no se toca.
- **Refs validadas:** afinidad **estricta** vía `EnumCodecs.NATURE` (naturaleza desconocida → falla el archivo,
  aislado); id==nombre de archivo (sin subcarpeta); duplicados avisados. La validación estricta de
  `effect` contra el resolver de familias llega en **DJT-074/076** (ese registro aún no existe).
- **`effectParams` es passthrough** (JSON crudo, `ExtraCodecs.JSON`): el esquema lo define cada familia (076)
  con su codec; así los archivos no se reescriben cuando las familias existan.
- **Ids por nombre** (regla 12): el índice numérico `JutsuId` de `run_jutsu` no se porta; la tabla 1..141 es
  solo guía de extracción para DJT-085.
- `levelRequirement` opcional con default 0: la puerta de aprendizaje real la cablea DJT-075 (learner).

**Autorrevisión — 2 hallazgos corregidos (+2 tests):**
1. **Default mutable compartido:** `optionalFieldOf("effectParams", new JsonObject())` reutilizaba la MISMA
   instancia para todas las entradas sin parámetros (la trampa de la regla 10b/DJT-063). Ahora `Optional` +
   instancia fresca por decode (bajo test con `assertNotSame`).
2. **Excepción dentro del `apply` del codec:** un id de solo espacios pasa `NON_EMPTY_STRING` pero el
   constructor de dominio lanza DENTRO del parse (excepción, no `DataResult.error`) → habría tumbado el
   `/reload`. `JutsuReloadListener.parse` ahora captura y descarta el archivo (bajo test).

- **Validación:** `./gradlew build` multiloader **verde**; **417 tests** verdes (403 + 14: los 4 pilotos con
  valores exactos, universal+defaults, params conservados/no compartidos, rechazos de naturaleza desconocida /
  coste negativo / efecto ausente o vacío / id-archivo / id en blanco, nombre base con subcarpetas).
  **Pendiente de validación en runtime en ambos loaders** (`/dojutsujutsu`, `/reload`, ref rota).
- **Resultado:** el catálogo de jutsus es data-driven con validación y aislamiento. Siguiente: **DJT-075**
  (aprendizaje: `JutsuKnowledge` + learner + `JutsuItem`), adelantado a 074 según el orden acordado.
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-16: `/dojutsujutsu list|get`,
  `/reload` con ref rota aislada sin crash.)*

## F4 · DJT-075 — `JutsuKnowledge` + aprendizaje con learner + `JutsuItem` (cierra el TODO de DJT-055)

- **Qué:** el aprendizaje de jutsus completo, fiel al árbol `jutsu/learner/*` del datapack (auditado, regla 16):
  **secuencial por elemento** — cada uso del Skill Learner entrega el **siguiente** jutsu de la secuencia de su
  elemento, con cooldown de **14 ticks por jugador** (compartido entre elementos, como `d.lrn_cd`), y el
  **último de la secuencia consume el learner** (`replace ... with air`). El jutsu queda en
  `JutsuKnowledge.known` (persistente — en el datapack "saber" era tener el carrot) y se entrega como
  `JutsuItem` con `JutsuComponent{jutsuId}` (reemplaza `carrot_on_a_stick{JutsuItem:1b,JutsuId:Nb}`).
- **Archivos (nuevos):** `data/state/JutsuKnowledge.java` (6º sub-estado enchufable: `known` +
  `learnProgress` = los `d.lrn_<elem>`); `jutsu/{LearnSequence, LearnSequenceRegistry,
  LearnSequenceReloadListener, JutsuLearningService}.java`; `items/{JutsuComponent, JutsuItem}.java`;
  `resources/data/dojutsu/learner_sequences/{fire,lightning}.json`; tests `JutsuKnowledgeTest`,
  `JutsuLearningServiceTest`.
- **Archivos (editados):** `PlayerNinjaData` (+campo/puerta `setJutsu`/línea de codec, sin bump de versión);
  `ModItems`/`ModDataComponents` (+`jutsu`); `natures/LearnerService` (su `use` se retira — lo reemplaza
  `JutsuLearningService.use`; expone `possesses`; **la entrega resetea el progreso del elemento**, fiel a
  `give/learner/<elem>` que hacía `set d.lrn_<elem> 0`); `items/LearnerItem` (delega en el nuevo servicio);
  `clans/ClanService` (+`setJutsu(DEFAULT)` en el reset atómico + barrido de ítems de jutsu);
  `core/command/AdminCommands` (ídem en `reset_ninja`); `core/DojutsuLifecycle` (listener + `forget` al
  desconectar); `en_us.json` (+24 claves).
- **Motivo:** `[ADD/03 §4, §2.6]`, `[ADD/10 jutsu]`, `[S06]`. Depende de DJT-055, DJT-073, DJT-017.

**Fidelidad al datapack (auditada contra los `.mcfunction`):**
- **Puerta de aprendizaje = poseer la naturaleza/KG** (`jutsu/learner/check`), NO el nivel de afinidad —
  responde el caso borde de la ficha: aprender sin el elemento **no** está permitido; los mensajes son los
  exactos ("Lack required Nature Release." / "Lack required Kekkei Genkai.").
- Secuencias extraídas: **fire (8)** fire_bullet→fireproof→fire_transport→fire_dragon_flame_bullet→
  exploding_dragon_strike→great_fireball→great_fire_annihilation→fire_cloak; **lightning (9)** …→chidori(5º)→…→
  drilling_hand_of_sage_lightning. El resto de elementos llega con sus lotes (DJT-086+). **Los learners de
  yin/yang eran placeholders** (no enseñaban nada): sin archivo de secuencia = mensaje honesto.
- "Ya los aprendiste todos" tiene **precedencia sobre el cooldown** y re-aplica cd 14 (como `lrnd_all`).
- Re-aprender tras recibir learner nuevo re-entrega ítems duplicados: igual que el datapack.

**Decisiones técnicas:**
- **Dirección de dependencias sin ciclo:** `jutsu` ya importaba `natures` (EnumCodecs), así que el flujo vive
  en `jutsu/JutsuLearningService` y `LearnerItem` (items) lo llama directo: `items → jutsu → natures`. Poner el
  flujo en `LearnerService` habría creado el ciclo `natures↔jutsu` (la lección de DJT-057).
- **Decisión pura + glue:** `JutsuLearningService.evaluate(knowledge, sequence, onCooldown, jutsuLoaded)` es
  estática y testeable sin MC; el glue aplica estado/ítems/mensajes.
- **Contenido incremental:** un jutsu de la secuencia sin definición en el registry → aviso, **sin avanzar la
  secuencia ni aplicar cd** (los ids de secuencia no se validan al cargar: el orden de los reload listeners no
  está garantizado — validación en el momento de aprender).
- **Cooldown transitorio** (mapa por UUID, olvidado al desconectar — patrón `MenuActionHandler.forget`): 14
  ticks de anti-spam no ameritan persistencia ni el riesgo de tick absoluto (eso es de DJT-077).
- **Reset "sin residuos" extendido:** el reset atómico limpia ahora el 5º sub-estado de gameplay
  (`JutsuKnowledge`) y barre los ítems de jutsu no conocidos (`removeStaleJutsuItems`) — el `reset_clan` del
  datapack olvidaba exactamente los jutsus (la clase de bug S04-R2).
- `JutsuItem.use` **no castea** (el cast es keybind+payload, DJT-080): aviso honesto, precedente DJT-055.

**Autorrevisión — 2 hallazgos corregidos:** (1) el mapa de cooldowns no se limpiaba al desconectar → `forget`
en `onQuit` (patrón existente); (2) `Set.copyOf` perdía el **orden estable** prometido para `known` →
`Collections.unmodifiableSet(new TreeSet<>(...))`. Además se corrigió el javadoc de la puerta única (la
entrega del learner también escribe el progreso).

- **Validación:** `./gradlew build` multiloader **verde**; **434 tests** verdes (417 + 17: sub-estado con
  round-trip NBT/saneado/inmutabilidad; secuencias reales contra el orden del datapack; rechazos de elemento
  desconocido/secuencia vacía/element≠archivo; decisión pura: orden exacto, consumo en el último, precedencia
  all-learned>cd, cd bloquea, contenido pendiente no avanza). **Pendiente de validación en runtime en ambos
  loaders.**
- **Resultado:** el learner enseña de verdad y el conocimiento persiste; el pipeline (DJT-074) nace con
  `known` y el `JutsuItem` listos. Siguiente: **DJT-074** (`JutsuCastPipeline`).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-16, incluido el fix de abajo.)*

**Fix post-validación (reportado por el usuario): `/dojutsu reset_ninja` no re-embarcaba al instante.**
Hueco **preexistente de F3** destapado por la suite de prueba de este ticket: `OnboardingController.refresh`
se llamaba en el login y en `/dojutsuclans reset`, pero `AdminCommands.resetNinja` no — tras el reset la fase
quedaba `NOT_STARTED` y la Screen de creación solo reaparecía al reloguear. Fix de 1 línea:
`OnboardingController.refresh(target)` antes del `sendFull`, espejando el orden exacto de
`DojutsuLifecycle.onJoin` (el refresh transiciona a `SELECTING_CLAN` y envía las opciones de clan). Alineado
con el comportamiento documentado en el handoff de F3 ("reset_ninja… re-embarcan"). Build + 434 tests verdes.

## F4 · DJT-074 — `JutsuCastPipeline` (chakra→cd→naturaleza→afinidad→efecto) · caster genérico

- **Qué:** la secuencia canónica de todo lanzamiento — **validar → consumir → cooldown → efecto → evento** —
  como único camino de cast del mod (jutsus, kenjutsu DJT-083, oculares F5, IA F8). Reemplaza las ~141
  máquinas `<jutsu>/cast`. Nacen aquí la interfaz `JutsuEffect` y su registro (resuelve el ciclo 074↔076 de la
  ficha): DJT-076 aporta las implementaciones reales.
- **Archivos (nuevos):** `jutsu/effect/{JutsuEffect, JutsuEffectRegistry, JutsuEffects}.java` (registro en
  código; **placeholders no-op temporales** para projectile/area/buff — el pipeline es validable end-to-end en
  runtime y 076 los reemplaza); `jutsu/{PlayerCastContext, JutsuCastPipeline}.java`; test
  `JutsuCastPipelineTest`.
- **Archivos (editados):** dominio `CastContext`/`CastFailureReason`/`Jutsu` (ver hallazgo); `JutsuTest` (+1
  test, fake ampliado); `debug/JutsuDebugCommand` (+`cast <id>`, temporal hasta la tecla de DJT-080);
  `core/DojutsuLifecycle` (registro de familias); `en_us.json` (+6 claves).
- **Motivo:** `[ADD/10 jutsu]`, `[ADD/05 §2]`, `[ADD/03 §2.6]`, `[S06]`. Depende de 073/030/050/016/075.

**Hallazgo de fidelidad (regla 16) — la puerta de POSESIÓN de la naturaleza:** el datapack valida **cuatro**
puertas, no tres: chakra → cd → **posesión** (`check_affinity`: "Lack required Chakra Nature.") → nivel de
afinidad ("Requires higher X Affinity!"). `[S06]` y la ficha la omiten, y en el mod importa de verdad: revocar
una naturaleza **conserva** el nivel de afinidad (decisión de DJT-051) → sin esta puerta, un ninja sin Fuego
castearía con su afinidad residual. **Edición aditiva del dominio de F1** (misma clase de desviación
documentada que F3): `CastContext.hasNature(String)`, `CastFailureReason.MISSING_NATURE` y la puerta en
`Jutsu.evaluatePreconditions` — bajo test (dominio y pipeline, incluido el caso "revocada con afinidad 170").

**Decisiones técnicas:**
- **Caster genérico por construcción** (S17-R3): la decisión usa `CastContext` y el pago un **`ChakraSink`**
  funcional — la IA de F8 enchufa los suyos sin tocar el pipeline. El núcleo
  (`cast(casterId, entry, ctx, sink, effectRunner, bus)`) es **puro y testeable**; el glue de jugador añade
  mensajes, sonido y sync.
- **Todo-o-nada reforzado:** el chakra se consume solo si TODAS las puertas pasan, **incluida la
  disponibilidad del efecto** — castear un jutsu cuya familia no está implementada (contenido incremental) no
  cuesta nada y avisa.
- **Aislamiento de fallos:** un efecto que lanza excepción no crashea el tick; el chakra ya se pagó y el
  evento se emite igual (fiel: el datapack daba la XP **antes** del summon). Bajo test.
- **Cooldown:** la comprobación pasa por el contexto (`PlayerCastContext.isOnCooldown` → `false` con TODO);
  el almacenamiento llega en DJT-077 (`JutsuKnowledge.cooldowns`). `applyCooldown` es el punto marcado.
- **Mensajes fieles** (`failed_cast`/`check_affinity`): actionbar dorado "Casted: X!"; "Could not cast
  Jutsu." + **sonido de yunque 0.3**; "Lack required Chakra Nature."; "Requires higher X Affinity!".
  Simplificación cosmética documentada: los dos últimos eran subtitles y aquí van por actionbar.
- El registro de efectos es **en código** (las familias son lógica, no datos): `JutsuEffects.register()` en el
  arranque; extensible por F5+ sin tocar el pipeline (OCP).

- **Validación:** `./gradlew build` multiloader **verde**; **443 tests** verdes (434 + 9: éxito consume el
  coste exacto y emite el evento; fallo no consume/ejecuta/emite; efecto no implementado falla sin coste;
  efecto roto no suprime el evento; orden completo de precondiciones; sink que rechaza; adapter sobre estado
  real; naturaleza revocada con afinidad residual bloquea sin coste; +1 en dominio). **Pendiente de validación
  en runtime en ambos loaders** (`/dojutsujutsu cast`).
- **Resultado:** cualquier jutsu definido por datos se castea por el pipeline con las garantías del ADD.
  Siguiente: **DJT-076** (familias de efecto reales).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-16. Nota: durante la
  validación se detectó que los subcomandos documentados en el handoff de F3 eran `give`/`take` cuando los
  literales reales son `grant`/`revoke` — handoff corregido.)*

## F4 · DJT-076 — Familias de efecto (proyectil/área/buff/teleport/estructura/curación)

- **Qué:** las **6 familias del ADD** como implementaciones de `JutsuEffect` parametrizadas desde
  `effectParams` — una clase por **familia**, cero por técnica (RA-1). Reemplazan los placeholders de DJT-074.
  Cada familia queda probada con un **jutsu real del datapack** (valores auditados, regla 16): projectile ←
  fire_bullet/great_fireball · area ← **vacuum_wave** (nuevo) · buff ← **fireproof** (nuevo, ¡el 2º de la
  secuencia del learner de fuego!) · teleport ← **substitution** (nuevo) · structure ← **earth_wall** (nuevo) ·
  heal ← **healing** (nuevo).
- **Archivos (nuevos):** `jutsu/effect/{ProjectileEffect, AreaEffect, BuffEffect, TeleportEffect,
  StructureEffect, HealEffect, MobEffectSpec}.java` (cada familia con record `Params` de parseo **puro y
  testeable**); 5 JSON nuevos (`fire/fireproof`, `wind/vacuum_wave`, `universal/substitution`,
  `earth/earth_wall`, `universal/healing`); test `effect/JutsuEffectParamsTest`.
- **Archivos (editados):** `JutsuEffects` (placeholders → familias reales); `JutsuEffect` (+`canApply`);
  `JutsuCastPipeline` (+outcome `NO_TARGET`); `entity/NinjaProjectile` (+`withFlightTick`, extensión aditiva
  espejo del tick de `AbilityAnchor`); JSON de `great_fireball`/`fire_bullet` (+effectParams) y de
  `lightning_bolt`/`chidori` (corregidos, ver hallazgo); `en_us.json`; tests de 073/074 ajustados.
- **Motivo:** `[ADD/08 RA-1]`, `[ADD/10 jutsu "una clase por familia"]`, `[S06]`. Depende de DJT-070/074.

**Hallazgos de fidelidad (regla 16):**
1. **`lightning_bolt` y `chidori` NO son área/buff: son GOLPE CARGADO** (`primed_strike`): el cast arma el
   golpe ("Attack something to use Jutsu!"), el efecto ocurre en el siguiente melee (`did_hit` en `main`), y
   si la ventana expira → "Jutsu cancelled". El **cd real de lightning_bolt es 200t tras el golpe** (los 30t
   eran la ventana de carga → `effectParams.primingTicks`). JSONs corregidos a `effect: "primed_strike"` —
   familia **pendiente** (necesita el hook de melee): hoy castearlos da `EFFECT_UNAVAILABLE` **sin coste**
   (honesto). Se implementará con el volcado de rayo (DJT-085/086) o un ticket propio.
2. **"Nothing nearby" se valida ANTES de cobrar** (healing, vacuum_wave): nueva precondición
   `JutsuEffect.canApply` + outcome `NO_TARGET` en el pipeline (tras las puertas del dominio, antes del
   consumo — bajo test). Mensaje literal del datapack.
3. **El aura de vuelo del fireball**: los proyectiles de fuego incendian lo cercano DURANTE el vuelo (radio 3,
   `Fire:100` cada tick) y prenden el suelo — no solo al impactar → `NinjaProjectile.withFlightTick` (aditivo,
   con el mismo aislamiento de fallos del tick del ancla).

**Valores auditados de los jutsus nuevos:** fireproof 20 chakra / sin cd / fire ≥50 / fire_resistance 1s ·
vacuum_wave 70 / 300t / wind ≥70 / radio 10, lanza +8 bloques + slow_falling 6s nivel 8 (el "daño" era la
caída) · substitution 20 / 600t / universal / poof + invisibilidad 6s + teleport aleatorio 5..10 (el tronco
señuelo es cosmético → assets) · earth_wall 60 / earth ≥50 / muro dirt 3×4 a 2 bloques, semántica
`setblock destroy` (el timer `d.ewall_timer` se comprueba pero **nada lo setea** en el datapack → cd 0, fiel;
las variantes de ancho 5/7 llegan con el volcado) · healing 40 / 1200t / cura al **más cercano** en radio 3
(no al caster) con regeneración 2s nivel 20. **Nota DJT-083:** la puerta `medical >= 20` de healing es un
requisito de **habilidad** (la clase del req de kenjutsu) — hasta DJT-083, healing se castea sin ella
(desviación temporal documentada).

**Autorrevisión — 1 hallazgo corregido:** `ArmorStand` ES `LivingEntity` en vanilla y el datapack los excluía
(`#dojutsu:ignore`) → área y curación los excluyen explícitamente (un vacuum_wave ya no lanza la decoración
del jugador por los aires).

- **Validación:** `./gradlew build` multiloader **verde**; **452 tests** verdes (443 + 9: los params de las 6
  familias contra los JSON reales, defaults, params vacíos que nunca lanzan, `NO_TARGET` sin coste; el test de
  lightning_bolt de 073 actualizado a la semántica corregida). **Pendiente de validación en runtime en ambos
  loaders.**
- **Resultado:** castear tiene efectos de verdad; el patrón data-driven queda listo para el volcado (RA-1).
  Siguiente: **DJT-077** (cooldowns por-instancia — varios pilotos ya castean en cadena sin cd real).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-16.)*

## F4 · DJT-077 — Cooldowns por-instancia (sin barrido por tick; corrige S06-R3)

- **Qué:** los cooldowns de jutsu como `Map<jutsuId, tickDisponible>` en `JutsuKnowledge` — se **consulta solo
  el jutsu que se castea**, cero decrementos por tick. Corrige S06-R3: el datapack tenía ~80 timers
  `d.<jutsu>_timer` y los decrementaba **todos, cada uno, cada tick** (y a 2/tick por la doble ejecución de
  DJT-061). Cierra el `TODO F4/DJT-077` que dejó el pipeline (DJT-074).
- **Archivos (editados):** `data/state/JutsuKnowledge.java` (+campo `cooldowns` + línea de codec +
  `isOnCooldown`/`remainingCooldown`/`withCooldown`); `jutsu/PlayerCastContext.java` (+`nowTick`;
  `isOnCooldown` real); `jutsu/JutsuCastPipeline.java` (el stub `applyCooldown` → `Runnable cooldownApplier`
  inyectado; el glue de jugador lo cablea con el tick de mundo); `debug/JutsuDebugCommand.java` (`get` muestra
  el cd restante); tests `JutsuKnowledgeTest` (+5), `JutsuCastPipelineTest` (+2).
- **Motivo:** `[ADD/04 §5 nota rendimiento]`, `[ADD/03 §2.6]`, `[ADD/10 jutsu]`, `[S06]`. Depende de DJT-074.

**Decisiones técnicas:**
- **Tick ABSOLUTO de mundo** (`Level.getGameTime`, que **persiste en el save**), no relativo: `now < availableAt`
  → en cooldown. Sobrevive a relogin y a reinicio del servidor sin recalcular (el riesgo de la ficha), y en
  un mundo un jugador pausado el cd "se pausa" con el reloj del mundo — correcto. Los cooldowns se **persisten**
  en el NBT (sub-estado enchufable: +1 línea de codec, sin bump de versión).
- **Sin crecimiento sin límite, sin barrido:** `withCooldown` **poda** de paso los cooldowns ya vencidos al
  tick actual (poda perezosa cada vez que se escribe uno). No hay tarea por tick que recorra el mapa.
- **Frontera inclusiva** documentada y bajo test: con cd N desde `now`, disponible en `now+N` (los N ticks son
  `now..now+N-1`). `cooldownTicks == 0` no registra nada (fireproof, earth_wall — su timer nunca se seteaba en
  el datapack, ya visto en DJT-076).
- **Caster genérico mantenido:** el pipeline recibe un `Runnable cooldownApplier` (la IA de F8 persiste su
  cooldown a su manera); el orden del ADD se respeta: consumir → cooldown → efecto. El cooldown se aplica
  **solo en cast exitoso** (bajo test).
- **Cooldown no observable todavía:** el HUD de cd es DJT-081, que añadirá el `ObservableField` y el sync; hoy
  el estado persiste y el comando de debug lo muestra (`/dojutsujutsu get <id>` → "cooldown restante").

- **Validación:** `./gradlew build` multiloader **verde**; **459 tests** verdes (452 + 7: fronteras del cd,
  cd 0 no registra, poda de vencidos sin barrido, round-trip NBT, cooldown aplicado solo en éxito, contexto de
  jugador lee el cd al tick de mundo). **Pendiente de validación en runtime en ambos loaders.**
- **Resultado:** recastear un jutsu antes de que expire su cd ahora falla; sin coste por tick. Siguiente:
  **DJT-078** (`JutsuCastEvent` → XP de afinidad).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-16.)*

## F4 · DJT-078 — `JutsuCastEvent` → XP de afinidad (cierra el bucle de progresión maestro)

- **Qué:** usar un jutsu sube la XP de la afinidad de su elemento → auto-nivela → habilita jutsus más fuertes
  (`aff >= N`). Cierra el bucle **usar jutsu → XP → nivel → daño/desbloqueos** y salda el hook `addAffinityXp`
  que venía diferido desde F2/F3. Emisor: suscriptor de `JutsuCastEvent` (que emite el pipeline de DJT-074).
- **Archivos (nuevos):** `jutsu/JutsuXpHandler.java` (suscriptor + decisión pura `affinityNatureOf`); tests
  `jutsu/JutsuXpHandlerTest`, +3 en `natures/NatureServiceTest`.
- **Archivos (editados):** `natures/NatureService.java` (+`addAffinityXp` puro + glue de runtime);
  `progression/XpApi.java` (javadoc: la XP de afinidad NO pasa por aquí, ver decisión); `core/DojutsuLifecycle`
  (registro del handler); `debug/NatureDebugCommand` (`get` muestra la XP de afinidad); `xp_sources.json`
  (TBD resuelto).
- **Motivo:** `[ADD/00 §2.3]`, `[ADD/05 §2]`, `[S06]`, `[S13]`. Depende de DJT-074, DJT-036, DJT-034.

**Fidelidad al datapack (RA-4, regla 16):** cada cast exitoso de un jutsu con afinidad hace
`scoreboard players add @s d.<nat>_xp 1` — cantidad **uniforme +1**, verificada en great_fireball/fire_bullet/
fireproof (→ fire_xp), vacuum_wave (→ wind_xp), chidori/lightning_bolt (→ ligh_xp), earth_wall (→ earth_xp).
Los **universales no dan XP de afinidad** (substitution, healing → medical_xp, que es una *habilidad*, DJT-083).
Esto **resuelve el `TBD_datapack_per_jutsu`** de `xp_sources.json` (regla 15): `value_per_jutsu: 1`.

**Decisión de arquitectura — la XP de afinidad NO pasa por `XpApi`:** la ficha decía "`XpApi.addAffinityXp`",
pero `natures` **ya depende de `progression`** (NatureService usa XpApi para la ninja_xp del grant), así que meter
`progression → natures` cerraría un **ciclo entre módulos** — exactamente la clase de problema que DJT-057 tuvo
que romper. La puerta correcta es `NatureService.addAffinityXp` (regla 10: el perfil elemental solo lo escribe
`NatureService`); el emisor vive en `jutsu.JutsuXpHandler` (el módulo `jutsu` ya depende de `natures`). `XpApi`
queda para habilidades/ninja_xp; su javadoc lo documenta.

**Decisiones técnicas:**
- **Puro + glue:** `NatureService.addAffinityXp(data, nature, xp)` es puro (auto-nivela con
  `LeveledStat.withXpAdded` — multi-nivel con arrastre, cap 500, misma curva que las habilidades); la sobrecarga
  de `ServerPlayer` resuelve estado y sincroniza. Bajo test: acumulación/auto-nivel (10 casts = 1 nivel en la
  banda 0-10), ignora no-positiva e `YIN_YANG` (derivada), no progresa en el cap.
- **Reactivo, no polling** (patrón `RankService`/`UnlockService`): el handler se suscribe al bus en el arranque.
  XP **solo en cast exitoso** (el pipeline emite el evento solo tras consumir — sin doble conteo ni XP en fallo)
  y **solo para jutsus con afinidad** (universales excluidos). Caster no-jugador (IA, F8) → ignorado.
- **Decisión pura testeable** (`affinityNatureOf(jutsuId)`): resuelve la naturaleza desde el `JutsuRegistry`;
  jutsu desconocido o universal → vacío.

- **Validación:** `./gradlew build` multiloader **verde**; **466 tests** verdes (459 + 7: 3 de `addAffinityXp`,
  4 del handler). **Pendiente de validación en runtime en ambos loaders** (`/dojutsunatures get` muestra la XP).
- **Resultado:** el bucle de progresión maestro está cerrado: castear entrena la afinidad. Siguiente:
  **DJT-079** (`LastCastRegistry`, para la copia Sharingan de F5).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-16.)*

## F4 · DJT-079 — `LastCastRegistry` (copia Sharingan; corrige S06-R6)

- **Qué:** registro del **último cast observable** por caster (jutsuId, dimensión, posición, tick de expiración)
  — base de la copia del Sharingan (F5). Suscriptor de `JutsuCastEvent`; el Sharingan lo consultará por
  proximidad sin acoplarse al emisor (el pipeline no conoce el Sharingan). Cierra EP-JUTSU-ENGINE salvo el
  keybind/HUD.
- **Archivos (nuevos):** `jutsu/LastCastRegistry.java` (+ record anidado `CastRecord` con la lógica pura de
  candidatura); test `jutsu/LastCastRecordTest`.
- **Archivos (editados):** `core/DojutsuLifecycle` (registro del suscriptor + `clear()` en `SERVER_STOPPING`);
  `debug/JutsuDebugCommand` (+`lastcast`).
- **Motivo:** `[ADD/05 §2, tabla "Copia Sharingan"]`, `[S06]`. Depende de DJT-078.

**Fidelidad al datapack (regla 16):** `jutsu/allow_copy` marcaba a los jugadores con Sharingan/RinneSharingan a
≤20 bloques con el `JutsuId` visto (`d.sharingan_seen`) al lanzarse un jutsu. **Corrige S06-R6 (estado
global):** ese `d.sharingan_seen` no caducaba — el jutsu visto se recordaba para siempre. Aquí:
- **Desacoplado** (emisor publica el evento, el observador consulta el registro) en vez de marcar a cada
  observador — el ADD pide "desacopla emisor/observador".
- **Acotado por tiempo** (`DEFAULT_LIFETIME_TICKS = 200` = 10 s) y **por dimensión + distancia**: nada global,
  nada eterno. El radio 20 lo pone el consumidor (F5), como el datapack.
- **Poda perezosa** al escribir y al consultar (sin barrido por tick, coherente con DJT-077).

**Decisiones técnicas:**
- **Un registro por caster** (su último cast reemplaza al anterior — como `d.sharingan_seen`, que solo guardaba
  el último id). Caster genérico por UUID (jugador o entidad IA, resuelto en cualquier nivel del servidor).
- **Lógica pura testeable:** `CastRecord.isCandidateFor(dim, pos, radio, now, observador)` usa `Vec3`/
  `ResourceKey`, que **no necesitan bootstrap de MC** → bajo test sin arrancar Minecraft (expiración con
  frontera inclusiva, proximidad, dimensión, no-copiarte-a-ti-mismo). El glue (resolver el caster, `getGameTime`)
  se valida en runtime.
- **`clear()` en `SERVER_STOPPING`:** cada mundo tiene su propio `getGameTime` → sin limpiar, un registro del
  mundo anterior podría compararse contra el tick de otro mundo al cambiar de partida (edge case real, no solo
  teórico). La expiración por sí sola no basta porque los contadores de tick difieren entre mundos.
- La posición/caster se guardan porque F5 necesitará **quién** y **qué** para copiar; hoy solo se registra y
  se inspecciona (el consumidor real es F5, como dice la ficha: "validar por log").

- **Validación:** `./gradlew build` multiloader **verde**; **472 tests** verdes (466 + 6 de `CastRecord`).
  **Pendiente de validación en runtime en ambos loaders** (`/dojutsujutsu lastcast`).
- **Resultado:** el último cast queda disponible y acotado para que F5 lo copie. Siguiente: **DJT-080**
  (keybind CastJutsu + payload C2S validado).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-16.)*

## F4 · DJT-080 — Keybind CastJutsu + `CastJutsuPayload` (C2S) + handler validado

- **Qué:** lanzar el jutsu equipado con **input real** (tecla **V** por defecto, remapeable) validado en servidor.
  Reemplaza el polling de `carrot_on_a_stick{JutsuId}` del datapack (RM-1). El click derecho del `JutsuItem`
  también castea (equivalente, server-side) — el datapack usaba click derecho, la tecla es la modernización.
- **Archivos (nuevos):** `network/CastJutsuPayload.java` (C2S, lleva la mano); `network/handler/
  CastJutsuHandler.java`; test `network/handler/CastJutsuHandlerTest`.
- **Archivos (editados):** `client/keybind/KeyBindings.java` (+`CAST_JUTSU`, envía el payload); `items/JutsuItem.
  java` (`use` castea de verdad, cierra el `cast_pending`); `core/DojutsuLifecycle` (registro del handler +
  `forget` al desconectar); `en_us.json` (+nombre de la tecla, −`cast_pending`).
- **Motivo:** `[ADD/05 §Input]`, `[ADD/07]`, `[S06]`. Depende de DJT-074, DJT-075, DJT-039.

**Decisiones técnicas — el cliente NUNCA decide qué jutsu es (RM-1):**
- El `CastJutsuPayload` **solo lleva la mano**, no el jutsuId. El servidor lee el `JutsuComponent` del ítem
  **real** de esa mano → un payload no puede inyectar un jutsu arbitrario (el riesgo de la ficha: "no confiar en
  el cliente sobre qué jutsu es").
- **Puerta `known`:** aunque tengas el ítem en mano, el servidor exige que el jutsu esté en `JutsuKnowledge.known`
  — un ítem dado por `/give` o residual no se castea sin haberlo aprendido (bajo test). Normalmente held ⟹ known
  (el learner los da juntos, DJT-075); la puerta cubre los caminos anómalos.
- **Anti-spam** de la tecla (50 ms, patrón `MenuActionHandler`) además del cooldown por-jutsu del pipeline;
  `forget` al desconectar. El handler delega en `JutsuCastPipeline.cast(player, jutsuId)`, que aplica las puertas
  de chakra/cd/afinidad y da feedback + sync — sin duplicar lógica.
- **Dos vías equivalentes** (tecla y click derecho) porque el tooltip del ítem prometía click derecho y el
  datapack lo usaba; ambas server-autoritativas y terminan en el mismo pipeline. `JutsuItem.use` guarda por
  `ServerPlayer` (en cliente no hace nada).

**Autorrevisión:** sin hallazgos. Los tests cubren lo puro (anti-spam por jugador, `forget`, la puerta
`knows`); el glue de red/inventario/keybind se valida en runtime (precedente: GUI/HUD de F2, payloads C2S).

- **Validación:** `./gradlew build` multiloader **verde**; **476 tests** verdes (472 + 4). **Pendiente de
  validación en runtime en ambos loaders** (tecla V + click derecho, ítem no conocido, sin ítem).
- **Resultado:** castear es input real validado; solo faltan el HUD de cooldown (081) para cerrar
  EP-JUTSU-ENGINE (salvo la familia `primed_strike`). Siguiente: **DJT-081** (HUD de cooldown).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-16.)*

## F4 · DJT-081 — HUD de cooldown (cierra EP-JUTSU-ENGINE salvo `primed_strike`)

- **Qué:** overlay que muestra el jutsu equipado (mano principal) y una barra que decrece con su cooldown
  restante. Cierra el input de casteo: castear → cd visible → recastear cuando esté listo.
- **Archivos (nuevos):** `client/hud/AbilityBarOverlay.java`; +2 tests en `client/ClientNinjaDataTest`.
- **Archivos (editados):** `data/ObservableField.java` (+`JUTSU_COOLDOWNS`); `data/PlayerNinjaData.java`
  (`setJutsu` marca observable **solo si cambian los cooldowns**); `network/SyncNinjaDataPayload.java`
  (+campo `cooldowns` + codec de mapa); `network/NinjaSync.java` (incluye los cooldowns en el snapshot);
  `client/ClientNinjaData.java` (+cache + `remainingCooldown`); `client/hud/HudOverlays.java` (registro).
- **Motivo:** `[ADD/10 client]`, `[S06]`. Depende de DJT-077, DJT-027, DJT-028.

**Decisiones técnicas:**
- **El jutsu equipado se lee del componente del ItemStack** (`JutsuComponent`, ya `networkSynchronized` por
  vanilla), NO del sync del mod — así el HUD reacciona al instante al cambiar de ítem en la hotbar, sin que el
  servidor tenga que empujar "qué hay equipado" (evita polling).
- **Solo el cooldown cruza la red** (regla "solo lo observable"): `SyncNinjaDataPayload` gana un
  `Map<jutsuId, tickDisponible>`; el cliente calcula el **restante** con su propio `level.getGameTime()`
  (sincronizado con el servidor). Un cd ya vencido computa 0 aunque el mapa aún lo tenga → el HUD lo muestra
  "Ready" sin que el servidor tenga que empujar en la expiración.
- **Marca observable con parsimonia:** `setJutsu` marca `JUTSU_COOLDOWNS` dirty **solo si el mapa de cooldowns
  cambió** (mismo criterio que `setChakra`): aprender un jutsu o avanzar el progreso del learner no genera delta
  de red. En la práctica, el cast ya emitía delta por el consumo de chakra; esto lo hace correcto también para
  un hipotético jutsu de coste 0 con cd.
- **Barra cualitativa:** sin el cd total en el cliente (no viaja), la barra usa una escala fija de 200t como
  referencia visual y se rellena al expirar; el texto muestra los segundos exactos. Deuda menor documentada
  (el cd total podría viajar si se quiere una barra proporcional).

**Autorrevisión — 1 hallazgo:** un parámetro sin usar (`jutsuId`) en el helper de la barra → renombrado y
simplificado a `emptyPixels(remaining)`. Sin más hallazgos.

- **Validación:** `./gradlew build` multiloader **verde**; **477 tests** verdes (476 + 1 neto: el cálculo del cd
  restante en cliente y el ajuste de las firmas de `applyFull/applyDelta`). El overlay es glue de cliente → se
  valida en runtime (precedente GUI/HUD de F2). **Pendiente de validación en runtime en ambos loaders.**
- **Resultado:** el bucle de combate está cerrado de punta a punta (aprender → equipar → castear con tecla →
  efecto → XP → cd visible). **EP-JUTSU-ENGINE completa** salvo la familia `primed_strike` (lightning_bolt/
  chidori), diferida. Siguiente: **DJT-082** (EP-KENJUTSU: `WeaponComponent` + atributos reversibles).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-16.)*

## F4 · DJT-082 — `WeaponComponent` + `WeaponAttributeService` (atributos reversibles) — abre EP-KENJUTSU

- **Qué:** armas ninja con bonus de daño que **escala con el nivel de kenjutsu** y se **revierte solo** al
  desequipar (corrige S07-R2, el "atributo pegado"). Primer ticket de EP-KENJUTSU.
- **Archivos (nuevos):** `items/{WeaponComponent, WeaponItem}.java`; `jutsu/kenjutsu/WeaponAttributeService.java`;
  `debug/WeaponDebugCommand.java` (temporal); test `jutsu/kenjutsu/WeaponAttributeServiceTest`.
- **Archivos (editados):** `registry/ModDataComponents` (+`weapon`), `registry/ModItems` (+`chakra_blade`,
  `kubikiribocho` + factories `createWeapon`); `core/DojutsuLifecycle` (registro reactivo + update en join);
  `DojutsuMod` (comando); `en_us.json` (+2 nombres).
- **Motivo:** `[ADD/05 §Atributos]`, `[ADD/03 §4]`, `[ADD/10 jutsu kenjutsu]`, `[S07]`. Depende de DJT-075, DJT-034.

**Decisión de arquitectura (consultada con el usuario): `ItemAttributeModifiers` vanilla, NO el puerto
`AttributeService`.** El bonus vive en el **ItemStack** (data component vanilla 1.21) con un id de modifier
**estable** (`ResourceLocation`): MC lo aplica solo en MAINHAND y lo **retira al desequipar por construcción**.
Reescribir reemplaza (no acumula). Así el "atributo pegado" del datapack (`dmg_calc` añadía el modifier al
**jugador** por tick y `remove_attributes` lo quitaba; si no corría, se pegaba) es **imposible de reproducir**,
y el puerto `AttributeService` **sigue diferido** (menos superficie, menos riesgo).

**Hallazgos de fidelidad (regla 16):**
1. **Escala de kenjutsu:** las bandas de daño del datapack están a `d.kenjutsu` **400/800/1200/1600/2000**
   (+1..+5), pero el mod **capa las habilidades a nivel 500** desde F1 (`XpCurve`). Con el cap, los umbrales
   crudos dejarían inalcanzables los tiers altos. **Reescalado ÷4 → 100/200/300/400/500** (los 5 tiers
   repartidos por todo el rango); divergencia consciente documentada. Bajo test contra los tiers del datapack.
2. **Kenjutsu ya es `Skill.KENJUTSU`** (ProgressionState, F2) — la ficha sugería añadir `kenjutsuLevel/xp` a
   `JutsuKnowledge`; sería **duplicar** el estado. Se usa el skill existente. (Imprecisión de la ficha, como en F3.)
3. **Penalización de velocidad −0.03** al portar arma (`dmg_calc`), que desaparece con dominio (`kenjutsu ≥ 1600`
   datapack = **≥ 400** escala mod): también va en el `ItemAttributeModifiers` (MOVEMENT_SPEED en MAINHAND),
   reversible por construcción.

**Decisiones técnicas:**
- **Reactivo, no polling:** las armas del inventario se recalculan al **subir kenjutsu** (`SkillLevelChangedEvent`)
  y al **entrar** el jugador (cubre armas obtenidas offline). No hay barrido por tick (a diferencia del datapack,
  que corría `dmg_calc` cada tick). Divergencia menor documentada: un arma dada por `/give` sin subir de nivel
  muestra su daño base hasta el siguiente level-up o relog (el debug command aplica el bonus al entregar).
- **Puro + glue:** `proficiencyBonus(level)` y `hasSpeedPenalty(level)` son puros y testeables; `applyModifiers`
  (escribe el component) y `updateInventory` son glue (necesitan `ItemStack`/`Attributes`, bootstrap de MC).
- **2 armas de ejemplo** (chakra_blade +3, kubikiribocho +8 — valores exactos de `give/weapons/*`); el roster
  completo es F8. Sin modelo de ítem todavía (se ven con textura "missing"): assets por fase.

**Autorrevisión:** ajustado el resolver del jugador a `GameInstance.getServer()` (no existe `DomainEvents.
resolvePlayer`) y el recorrido de inventario a iterar `items` + `offhand` limpio.

- **Validación:** `./gradlew build` multiloader **verde**; **481 tests** verdes (477 + 4 de las bandas de
  proficiencia y la penalización de velocidad). **Pendiente de validación en runtime en ambos loaders** (equipar
  sube el daño, desequipar lo revierte sin residuo tras 50 ciclos; el bonus sube con el nivel de kenjutsu).
- **Resultado:** las armas dan daño reversible por nivel, sin atributo pegado. Siguiente: **DJT-083** (técnicas
  de espada como jutsus de categoría kenjutsu).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-16.)*

## F4 · DJT-083 — Técnicas de espada (jutsus de categoría kenjutsu)

- **Qué:** las técnicas de kenjutsu como `JutsuDefinition` de categoría `kenjutsu` con **requisito de nivel de
  habilidad** (kenjutsu) en vez de afinidad, reutilizando el `JutsuCastPipeline` y las familias de efecto.
  Cierra de paso el TODO de DJT-076 (la puerta `medical ≥ N` de healing).
- **Archivos (nuevos):** `domain/jutsu/SkillRequirement.java`; 5 JSON en `data/dojutsu/jutsu/kenjutsu/`.
- **Archivos (editados):** dominio `CastFailureReason` (+`INSUFFICIENT_SKILL`), `CastContext` (+`skillLevel`
  default 0), `JutsuDefinition` (+`skillRequirement`), `Jutsu` (chequeo tras afinidad); `jutsu/JutsuEntry`
  (codec `skill` estricto), `jutsu/PlayerCastContext` (+`skillLevel` real), `jutsu/JutsuCastPipeline`
  (+outcome + feedback "Requires higher X!"); `healing.json` (+`skill medical 5`); `en_us.json` (+6 claves);
  tests `JutsuTest` (+3, fake ampliado), `JutsuEntryTest` (+3).
- **Motivo:** `[ADD/10 jutsu kenjutsu]`, `[S07]`. Depende de DJT-074, DJT-082.

**Hallazgo de fidelidad (regla 16) — la ficha se equivoca en el "qué":** la ficha decía "una técnica de espada
se castea **con arma equipada** y nivel suficiente; sin arma/nivel falla". **Falso según el datapack:** las
técnicas de kenjutsu son **jutsu items** (`carrot_on_a_stick{JutsuItem:1b, JutsuId:136}`, "Right-Click to use
Kenjutsu") — el ítem que sostienes ES la técnica, no un arma aparte. `dancing_blade_risk/cast` solo comprueba
`d.kenjutsu >= 300` y el cooldown, **sin** chequear arma en mano. El `hold_check`/`dmg_calc` (que sí mira el
arma sostenida) es el **bonus pasivo** de DJT-082, un sistema distinto. Se implementa la puerta real (**solo
nivel de kenjutsu**), no "requiere arma". Cuarta imprecisión de ficha del proyecto (patrón F3).

**Valores auditados (reescalados ÷4 como en DJT-082, mod cap 500 = datapack 2000):** dancing_blade_risk
kenjutsu 300→**75** / sin chakra · assassination_technique 350→**88** / sin chakra · chakra_shock_slash 500→
**125** / 60 chakra · chakra_shockwave_slash 600→**150** / 100 chakra · secret_sword_snowstorm_beheading
1400→**350** / 100 chakra. Todas cd 60t. Efecto: familia `area` con daño (mecánica de multi-hit/dash exacta
simplificada — la refinaría el volcado de contenido; el deliverable es la **puerta de nivel**).

**Decisiones técnicas:**
- **`SkillRequirement` en el dominio** (junto a `AffinityRequirement`): edición aditiva de F1, gate en
  `Jutsu.evaluatePreconditions` **tras** la afinidad (orden chakra→cd→naturaleza→afinidad→**habilidad**). Bajo
  test. `CastContext.skillLevel` con default 0 (un caster sin progresión no cumple ningún req de skill).
- **Cierra el `medical ≥ 20` de healing** (deferido en DJT-076): mismo mecanismo, `medical` 20→**5**. Ya no es
  una desviación temporal.
- **Codec `skill` estricto** (`EnumCodecs.SKILL`): una habilidad desconocida falla el archivo (bajo test).
- Las técnicas se **aprenden y castean** como cualquier jutsu (learner + tecla/click, DJT-075/080) — no hay
  camino especial. Su learner/secuencia y modelos de ítem llegan con el contenido (DJT-086+/F8).

- **Validación:** `./gradlew build` multiloader **verde**; **487 tests** verdes (481 + 6: el gate de kenjutsu en
  dominio, el piloto real dancing_blade_risk, healing con medical, habilidad desconocida rechazada). **Pendiente
  de validación en runtime en ambos loaders.**
- **Resultado:** las técnicas de espada existen como jutsus gateados por nivel de kenjutsu. Siguiente:
  **DJT-084** (Samehada, arma especial con absorción de chakra).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-16.)*

## F4 · DJT-084 — Samehada (absorción de chakra) — cierra EP-KENJUTSU

- **Qué:** la Samehada, espada viva que **absorbe chakra al golpear**, fiel a `[S07]`. Cierra EP-KENJUTSU y,
  con ello, el motor de combate de F4 (EP-ENTITY + EP-JUTSU-ENGINE + EP-KENJUTSU).
- **Archivos (nuevos):** `jutsu/kenjutsu/SamehadaService.java`; test `SamehadaServiceTest`.
- **Archivos (editados):** `registry/ModItems` (+`samehada` + factory); `core/DojutsuLifecycle` (registro del
  hook); `debug/WeaponDebugCommand` (+`samehada`); `en_us.json` (+nombre).
- **Motivo:** `[ADD/10 jutsu kenjutsu "Samehada"]`, `[ADD/03 §2.6]`, `[S07]`. Depende de DJT-082, DJT-030.

**Fidelidad al datapack (auditado `samehada/absorb` + `absorb_ply`, regla 16):**
- Golpear un **mob** con la Samehada → el portador gana **+100 chakra** (fijo).
- Golpear un **jugador** → **drena** hasta 100 de su chakra y lo **transfiere** al portador (roba exactamente lo
  que la víctima pierde; si tiene <100, roba lo que haya).
- En ambos casos el chakra del portador se **clampa a su máximo** (`ChakraService`).
- **Requisito:** kenjutsu 1000 (datapack) → **250** (÷4, escala del mod, misma política que DJT-082/083). Sin el
  nivel o sin la Samehada en mano, el golpe es normal (sin absorción).
- **No cancela el daño:** la Samehada golpea normal (base 6) **y además** roba chakra (evento pasado, no
  consumido).

**Decisiones técnicas:**
- **Hook `EntityEvent.LIVING_HURT`** (Architectury, cross-loader — verificado que existe en 13.0.8): filtra al
  atacante directo (`ServerPlayer`), lee su arma en mano y aplica la absorción. Devuelve `EventResult.pass()`.
- **Puro + glue:** `drainFromPlayer(targetChakra)` = `min(100, max(0, targetChakra))` es puro y testeable; el
  glue lee el `ServerPlayer`/`ItemStack` y sincroniza (bajo test lo puro; el hook en runtime).
- **Puertas únicas:** el chakra solo por `ChakraService` (`consume` exacto del drenaje en la víctima, `restore`
  con clamp en el portador); sync a ambos jugadores.
- La Samehada es un `WeaponItem` (DJT-082): su daño base 6 + la penalización/bonus de kenjutsu van por el
  `ItemAttributeModifiers` reversible; el `weaponType: "samehada"` la identifica para la absorción.

**Autorrevisión:** sin hallazgos. Nota: la absorción usa `getDirectEntity()` (el atacante del melee), no
`getEntity()` — evita que un proyectil con dueño Samehada-portador absorba a distancia (fiel: el datapack usa
`did_hit` melee).

- **Validación:** `./gradlew build` multiloader **verde**; **491 tests** verdes (487 + 4 del drenaje/tope/req).
  **Pendiente de validación en runtime en ambos loaders**.
- **Resultado:** la Samehada roba chakra al golpear. **EP-KENJUTSU completa** (DJT-082/083/084) → **el motor de
  combate de F4 está cerrado**. Solo queda el **contenido** (EP-JUTSU-CONTENT: DJT-085 volcado fuego/rayo, 086+
  el resto). Siguiente: **DJT-085**.
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-16.)*

## F4 · DJT-085 — Volcado Fuego + Rayo (naturalezas piloto) — abre EP-JUTSU-CONTENT

- **Qué:** volcar **todas las técnicas de fuego y rayo** del datapack a JSON, validando **RA-1** (que el patrón
  data-driven escala). Sin código nuevo — solo datos e instancias de las familias de DJT-076. **24 jutsus**
  totales cargados (fuego 7, rayo 8, otros 9).
- **Archivos (nuevos):** 10 JSON — `jutsu/fire/{fire_transport, fire_dragon_flame_bullet,
  exploding_dragon_strike, great_fire_annihilation}.json`, `jutsu/lightning/{lightning_instantaneous_body,
  lightning_net, tiger_scissors, lightning_cutter, kirin, drilling_hand_of_sage_lightning}.json`.
- **Archivos (editados):** test `JutsuEntryTest` (+4: valores auditados de fuego/rayo, cap de kirin/drilling,
  primed_strike diferido). El `en_us.json` ya tenía los nombres (DJT-075).
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[ADD/08 RA-1]`, `[S06]`. Depende de DJT-076, DJT-073.

**Valores auditados (chakra / afinidad `d.aff_<nat>`), uno a uno en los `cast.mcfunction`:**
- **Fuego:** fire_transport 80/60 · fire_dragon_flame_bullet 100/100 · exploding_dragon_strike 150/140 ·
  great_fire_annihilation 180/200. (+ los ya volcados: fire_bullet 60/30, great_fireball 140/170, fireproof 20/50.)
- **Rayo:** lightning_instantaneous_body 80/60 · lightning_net 80/80 · tiger_scissors 100/100 · lightning_cutter
  200/200 · kirin 300/**600** · drilling_hand_of_sage_lightning 400/**800**. (+ lightning_bolt 60/30, chidori 100/120.)

**Hallazgos y decisiones (regla 16):**
1. **La afinidad usa las mismas bandas 0-500 que el mod** (verificado en `predicates/natures/leveling/lvl_req/`:
   `d.aff_earth` 0-10, 11-20, …, 500 — idénticas a `XpCurve`). Por eso las afinidades se portan **crudas** (no
   ÷4 como kenjutsu): coherente con lo validado (Uchiha aff 50 no castea great_fireball a 170). **kirin (600) y
   drilling (800) exceden el cap 500 del mod → capados a 500** (jutsus de máxima afinidad; divergencia mínima
   documentada, solo afecta a estos dos, imposibles de otro modo). Nota: kenjutsu SÍ se reescala ÷4 porque su
   escala real es 0-2000 (bandas de daño/req 400-2000); la afinidad es 0-500. Escalas distintas, ambas fieles.
2. **`primed_strike` (golpe cargado) reafirmado:** lightning_cutter y drilling_hand son cargas de golpe como
   chidori → familia `primed_strike`, **aún no implementada** (se castean con `EFFECT_UNAVAILABLE` sin coste).
   Se implementará en un ticket propio o con el hook de melee.
3. **`fire_cloak` / `lightning_cloak` NO se portan:** son **modos de empoderamiento** (chakra cloak), no jutsus
   de cast-y-efecto → pertenecen a **F6 (empowerment)**. Están en las secuencias del learner (últimos de cada
   elemento): al llegar a ellos, el learner avisa "todavía no se puede aprender" (`CONTENT_PENDING`) — el
   comportamiento incremental correcto.
4. **Cooldowns:** los localizables se portaron exactos (fdfb 100, eds 400, gfa 200, lightning_net 800, tiger
   200, kirin 1200); los de teleport/cutter/drilling (set en subfunciones no localizadas) se infirieron por
   tier (100/400/600) — anotado.

**Mapeo a familias** (DJT-076, sin clases nuevas): proyectil (los bullets/dragon de fuego, con aura de fuego),
teleport (los "instantaneous body"), área (lightning_net/tiger_scissors/kirin, con efectos de poción),
primed_strike (los melee-charge, diferido). **RA-1 validado:** 10 técnicas nuevas cubiertas **solo con datos**,
sin tocar el motor.

- **Validación:** `./gradlew build` multiloader **verde**; **495 tests** verdes (491 + 4). **Pendiente de
  validación en runtime en ambos loaders** (aprender y castear una muestra de fuego y rayo).
- **Resultado:** el catálogo de fuego y rayo está volcado; el patrón escala sin tocar código. Siguiente:
  **DJT-086+** (el resto de naturalezas — agua, tierra, viento, yin, yang, taijutsu, KG, jutsu de clan).

**Andamiaje añadido tras DJT-085 (hueco de pruebas detectado por el usuario):** no existía comando para subir
la **afinidad** (solo se lograba casteando, +1/cast — inviable para probar great_fireball 170 o kirin 500). Se
añadió `/dojutsunatures aff <nat> <0-500>`, que fija el nivel por la puerta única `NatureService.setAffinityLevel`
(la del onboarding); rechaza `yin_yang` (afinidad derivada). La afinidad no es observable → no cruza la red (el
server es autoridad; lo leen `get` y el casteo). Temporal, se retira con la GUI de progresión de afinidades.
También se añadió `/dojutsuchakra setmax <n>`: `restore`/`full` clampan al máximo (base 100), insuficiente para
los jutsus caros (great_fire_annihilation 180, kirin 300, drilling 400); `setmax` sube el tope por la puerta
`ChakraService.setMax` (la del join de clan, `fill=true`) y sincroniza (el chakra sí es observable). Temporal.

## F4 · DJT-085b — Assets de jutsus y armas (texturas/modelos) — porte del resource pack

- **Qué:** portar las texturas y modelos del `resource pack` del datapack para que los **24 ítems de jutsu** y
  las **3 armas** dejen de verse con textura "missing" (violeta). Ticket de assets dedicado (fuera del backlog
  de diseño), acordado con el usuario antes de DJT-086.
- **Archivos (nuevos):** `items/JutsuModelData.java` (mapa presentación jutsuId→CustomModelData) + test;
  **assets** — 24 texturas `assets/dojutsu/textures/item/jutsu/*.png` + 24 modelos `models/item/jutsu/*.json` +
  modelo base `models/item/jutsu.json` (overrides); 3 texturas `textures/item/weapon/*.png` + 3 modelos de arma.
- **Archivos (editados):** `items/JutsuItem.java` (`createStack` fija `CUSTOM_MODEL_DATA`).
- **Motivo:** `[S06]`, handoff F3 ("assets por fase"). El resource pack tenía los assets como overrides sobre
  `carrot_on_a_stick`/`iron_sword` vía `CustomModelData`; el mod usa ítems reales.

**Decisión de mecanismo (consultada implícitamente en la elección de enfoque): `custom_model_data` vanilla, sin
código de cliente.** El ítem `dojutsu:jutsu` es uno solo; su modelo se elige por el data component
`minecraft:custom_model_data` (predicado **vanilla** de 1.21) que `createStack` fija — el modelo base mapea cada
número a su modelo. **Cero código de cliente por loader** (funciona idéntico en NeoForge y Fabric por datos).
Las armas son **ítems propios** → cada una su modelo (trivial). Se reutilizan los **CustomModelData auténticos
del datapack** (extraídos de `give/jutsu/*` — fire_bullet 356, great_fireball 301...) para fidelidad.

**Separación presentación/gameplay:** el número de modelo NO va en el JSON de la técnica (coste/cd/efecto) sino
en `JutsuModelData` (Java) — separa el "qué hace" del "cómo se ve". Un jutsu sin entrada usa el modelo por
defecto (pergamino genérico).

**Autorrevisión — 1 bug corregido:** la resolución de overrides de `custom_model_data` en MC toma **el último
de la lista cuyo valor ≤ el del ítem**, así que deben ir **ordenados ascendente**. La primera versión los
generó en orden de inserción (301, 356, 314...) → fire_bullet (356) habría mostrado la textura de otro jutsu.
Regenerado ordenado; el test verifica **(1)** que el mapa y el modelo base declaran los mismos CMD y **(2)** que
el modelo base va ascendente (para que el bug no reaparezca).

**Alcance:** jutsus (24) + armas (3). **El learner queda como seguimiento**: su textura es **por elemento**
(~23 variantes: 7 naturalezas + 16 KG) y requiere la misma técnica de override keyed por `LearnerComponent`;
se porta en un ticket propio o con los ítems de F8. Sigue con textura "missing" por ahora.

- **Validación:** `./gradlew build` multiloader **verde**; **498 tests** verdes (495 + 3: números auténticos,
  default, sincronía mapa↔modelo base + orden ascendente). **Pendiente de validación en runtime en ambos
  loaders** (los ítems de fuego/rayo/kenjutsu y las armas muestran su textura real).
- **Resultado:** los ítems de combate tienen textura; el porte de assets restante (learner, ojos, máscaras,
  bijuu...) sigue el plan "por fase". Siguiente: **DJT-086+** (el resto de naturalezas en JSON).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-17. Reportó bugs menores en armas
  —los deja así— y pidió texturas de learner + efectos de los jutsus de rayo sin efecto → DJT-085c/d.)*

## F4 · DJT-085c — Texturas del Skill Learner (por elemento)

- **Qué:** portar las texturas del learner (una por elemento) — seguía viéndose como carrot_on_a_stick. Mismo
  mecanismo que DJT-085b (`custom_model_data` vanilla), keyed por `LearnerComponent.element`.
- **Archivos (nuevos):** `items/LearnerModelData.java` (elemento→CMD 1000-1023, auténticos del datapack) + test;
  **assets** — 24 texturas `textures/item/learner/*.png` + 24 modelos + modelo base `models/item/learner.json`
  (overrides ascendentes).
- **Archivos (editados):** `natures/LearnerService.createLearner` (fija `CUSTOM_MODEL_DATA` por elemento).
- **Resultado:** el learner de cada elemento muestra su textura real.

## F4 · DJT-085d — Familia de efecto `primed_strike` (golpe cargado del rayo)

- **Qué:** implementar la familia que quedó **diferida** en DJT-076/085 — los jutsus de rayo lightning_bolt,
  chidori, lightning_cutter y drilling_hand_of_sage_lightning no tenían efecto (`EFFECT_UNAVAILABLE`). Ahora
  **arman** el golpe al castear y **el siguiente golpe cuerpo a cuerpo lo descarga** (rayo visual + daño).
- **Archivos (nuevos):** `jutsu/effect/PrimedStrikeEffect.java` (arma el golpe), `jutsu/effect/
  PrimedStrikeManager.java` (estado transitorio + hook `LIVING_HURT` que descarga); test `PrimedStrikeTest`.
- **Archivos (editados):** `jutsu/effect/JutsuEffects` (registra `primed_strike`); `core/DojutsuLifecycle`
  (registro del gestor + `forget` al desconectar); los 4 JSON de rayo (+`damage` en `effectParams`);
  `en_us.json` (+`primed`); `JutsuEntryTest` (nombre de test actualizado).
- **Motivo:** `[S06]` (`lightning_bolt/hit_effect`, `chidori/ready`), petición del usuario. Depende de DJT-074/076.

**Fidelidad al datapack (auditado `lightning_bolt/hit_effect` + `hit_effects`):** el cast arma (`d.lb_ready`,
ventana ~30t); el golpe conecta (`hit_check` a ≤5 bloques) → `summon lightning_bolt` sobre el objetivo + daño +
cooldown; expirar la ventana = "Jutsu cancelled". Implementado: `PrimedStrikeEffect.apply` arma
(`PrimedStrikeManager.arm`, ventana `primingTicks`) y muestra "Attack something to use Jutsu!"; el hook
`LIVING_HURT` descarga sobre el objetivo golpeado un **rayo visual** (`LightningBolt.setVisualOnly`) + el `damage`
del JSON (lightning_bolt 6, chidori 10, lightning_cutter 12, drilling 14); un golpe, una descarga; expiración
perezosa.

**Divergencia documentada:** el datapack consume el chakra **al golpear**; aquí el pipeline lo consume **al
castear** (armar) — armar y fallar el golpe gasta el chakra. Coherente con el "consumir en cast" del pipeline;
divergencia menor. La expiración es silenciosa (sin el "Jutsu cancelled" del datapack, que exigiría un tick).

**Decisiones técnicas:** estado transitorio (mapa por UUID, `forget` al desconectar — patrón `MenuActionHandler`/
Samehada); hook `LIVING_HURT` independiente del de Samehada (Architectury permite varios listeners); caster IA
diferido (solo jugadores arman hoy); `damage` vía `indirectMagic` con el atacante como dueño (no daño de
entorno). Puro y testeable: `Primed.isLiveAt` y `Params.parse`.

- **Validación:** `./gradlew build` multiloader **verde**; **503 tests** verdes (498 + 5: ventana del golpe,
  params de los 4 pilotos, defaults, sincronía del modelo base del learner). **Pendiente de validación en
  runtime en ambos loaders** (learner con textura; castear chidori/lightning_bolt → "Attack something" → golpear
  → rayo + daño). Con esto **la familia `primed_strike` deja de estar diferida**: EP-JUTSU-CONTENT solo espera el
  resto de naturalezas. Siguiente: **DJT-086+**.
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-17: learner con textura por
  elemento; chidori/lightning_bolt/etc. arman y descargan al golpear.)*

## F4 · DJT-086 — Volcado Agua (naturaleza)

- **Qué:** volcar las **7 técnicas de agua** casteables (un ticket por naturaleza, según la ficha), con
  aprendizaje por el learner y texturas. Segundo lote de EP-JUTSU-CONTENT tras Fuego+Rayo.
- **Archivos (nuevos):** 7 JSON `jutsu/water/*.json` + `learner_sequences/water.json` + 7 texturas/modelos de
  jutsu (`custom_model_data`); test en `JutsuEntryTest`.
- **Archivos (editados):** `items/JutsuModelData` (+7 CMD de agua); modelo base `models/item/jutsu.json`
  (regenerado, 31 overrides ordenados); `en_us.json` (+8 nombres); `JutsuModelDataTest` (array ampliado).
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`. Depende de DJT-076/085.

**Valores auditados (chakra / afinidad `d.aff_water`, uno a uno en los `cast.mcfunction`):** water_prison 30/30 ·
water_puddle 30/40 · breathing_underwater 20/50 · water_instantaneous_body 80/60 · hidden_mist 60/80 ·
rising_water_slicer 100/100 · water_severing_wave 140/160. CMD auténticos (310-313, 348, 349, 483).

**Mapeo a familias** (sin código nuevo — RA-1 sigue validado): water_prison → área (slowness), water_puddle →
estructura (bloque water), breathing_underwater → buff (water_breathing 60 s), water_instantaneous_body →
teleport, hidden_mist → área (blindness), rising_water_slicer/water_severing_wave → proyectil (prismarine_shard).

**Diferido:** `water_cloak` es un **modo de empoderamiento** (chakra cloak) → **F6**, como fire_cloak/
lightning_cloak/earth_cloak/wind_cloak. Está en la secuencia del learner (último): el learner avisa
`CONTENT_PENDING` al llegar (incremental correcto).

- **Validación:** `./gradlew build` multiloader **verde**; **504 tests** verdes (503 + 1). **Pendiente de
  validación en runtime en ambos loaders** (aprender y castear una muestra de agua; texturas). **31 jutsus**
  totales. Siguiente: **DJT-087** (Tierra) / **DJT-088** (Viento), mismo patrón.
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-17.)*

## F4 · DJT-087 — Volcado Tierra (naturaleza)

- **Qué:** volcar las **6 técnicas de tierra** nuevas (earth_wall ya estaba desde DJT-076) + secuencia learner +
  texturas. Tercer lote de EP-JUTSU-CONTENT.
- **Archivos (nuevos):** 6 JSON `jutsu/earth/*.json` + `learner_sequences/earth.json` + 6 texturas/modelos;
  test en `JutsuEntryTest`.
- **Archivos (editados):** `items/JutsuModelData` (+6 CMD); modelo base (37 overrides, sin CMD duplicados
  —verificado en el script—); `en_us.json` (+7); `JutsuModelDataTest` (array).
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`. Depende de DJT-076/085/086.

**Valores auditados (chakra / afinidad `d.aff_earth`):** earth_prison 30/30 · rock_gun 60/30 ·
earth_instantaneous_body 80/60 · earth_golem 100/100 · earth_dragon_blast 120/150 · tunnelling 160/180.
CMD auténticos (306, 308, 309, 352, 353, 355; earth_wall 337 ya en el mapa).

**Familias:** earth_prison → área (slowness 5); rock_gun/earth_dragon_blast → proyectil (cobblestone, con
gravedad); earth_instantaneous_body/tunnelling → teleport (tunnelling con rango mayor); earth_wall → estructura
(ya existía).

**Divergencia documentada:** `earth_golem` **invoca una entidad golem** en el datapack → eso es el módulo
**Summoning (F9)**. Se mapea a `structure` (muro de piedra 3×3 defensivo) como aproximación **castable**, para
que la secuencia del learner fluya hasta tunnelling (si no tuviera JSON, el learner se atascaría en él con
`CONTENT_PENDING`, bloqueando earth_dragon_blast y tunnelling). El golem animado real llega con F9. `earth_cloak`
→ F6 (empowerment), como los otros cloaks.

- **Validación:** `./gradlew build` multiloader **verde**; **505 tests** verdes (504 + 1). **Pendiente de
  validación en runtime en ambos loaders**. **37 jutsus** totales. Siguiente: **DJT-088** (Viento).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-17.)*

## F4 · DJT-088 — Volcado Viento (naturaleza) — cierra las 5 naturalezas básicas

- **Qué:** volcar las **9 técnicas de viento** nuevas (vacuum_wave ya estaba desde DJT-076) + secuencia learner +
  texturas. Cuarto lote; con él quedan **completas las 5 naturalezas básicas** (fuego/rayo/agua/tierra/viento).
- **Archivos (nuevos):** 9 JSON `jutsu/wind/*.json` + `learner_sequences/wind.json` + 9 texturas/modelos; test.
- **Archivos (editados):** `items/JutsuModelData` (+9 CMD); modelo base (46 overrides, sin duplicados —
  verificado); `en_us.json` (+9); tests.
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`. Depende de DJT-076/085/086/087.

**Valores auditados (chakra / afinidad `d.aff_wind`):** propelling_winds 30/30 · wind_torpedo 60/50 ·
wind_instantaneous_body 80/60 · drilling_air_bullet 100/100 · rasengan 100/150 · big_ball_rasengan 200/200 ·
rasenshuriken 300/**600→500** (capado como kirin/drilling) · wind_mode 100/120 · wind_cloak 150/250.

**Familias:** propelling_winds → buff (jump_boost + slow_falling); wind_torpedo/drilling_air_bullet → proyectil;
wind_instantaneous_body → teleport; **rasengan/big_ball_rasengan → `primed_strike`** (la esfera se estampa en el
enemigo: armar + golpear, como chidori); rasenshuriken → proyectil (con radio, nether_star); vacuum_wave → área
(ya existía).

**Divergencia documentada — dos modos en medio de la secuencia:** `wind_mode` (#6) y `wind_cloak` (#9) van
**antes** de rasengan/rasenshuriken en la secuencia del learner. Son **modos de empoderamiento** (→ F6), pero si
no tuvieran JSON el learner se **atascaría** en ellos (`CONTENT_PENDING`) y **bloquearía** rasengan/rasenshuriken.
Se mapean a `buff` (empoderamiento temporal aproximado: wind_mode = speed+jump, wind_cloak = speed 2+resistance)
para que la secuencia fluya; el toggle real llega con F6. Misma solución que earth_golem (mapear a familia
castable, versión real diferida). Los cloaks que van **al final** de una secuencia (fire/water/earth) sí se
difieren sin JSON.

- **Validación:** `./gradlew build` multiloader **verde**; **507 tests** verdes (505 + 2: valores de viento,
  rasengan primed_strike). **46 jutsus** totales;
  **5 naturalezas básicas completas**. Siguiente: **DJT-089** (Yin + Yang).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-089 — Volcado Yin/Yang (jutsus del Sexto Camino)

- **Qué:** volcar las técnicas de **yin-yang** (Sexto Camino) que sí son learnable + secuencia learner + texturas.
- **Hallazgo (regla 16):** en el datapack los learners de **yin** y **yang por separado eran `placeholder`**
  (no enseñan nada). El contenido learnable está en la secuencia **combinada `yin_yang`** (`learn_yinyang`):
  truth_seeking_scorched_earth, sword_of_nunoboko, six_paths_cross_lantern, creation_of_all_things,
  creation_of_all_things_split.
- **Archivos (nuevos):** 4 JSON `jutsu/yin_yang/*.json` + `learner_sequences/yin_yang.json` + 3 texturas/modelos;
  tests.
- **Archivos (editados):** `items/JutsuModelData` (+3 CMD); modelo base (49 overrides, sin duplicados —
  verificado); `en_us.json` (+4); tests.
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`. Depende de DJT-088.

**Valores auditados (chakra / `aff_yinyang` del datapack):** truth_seeking_scorched_earth 900/800 ·
sword_of_nunoboko 200/1000 · six_paths_cross_lantern 1200/1000 · creation_of_all_things 4000/2000 ·
creation_of_all_things_split 6000/2000.

**Divergencia documentada — afinidad yin_yang capada a 500:** `aff_yinyang` es **derivada** en el mod
(`(yin+yang)/2`, `NatureProfile.affinity`), y como yin/yang capan a 500 el máximo derivable es **500**. Los
requisitos del datapack (800-2000) son **inalcanzables** → se capan todos a **500** (misma decisión que
kirin/drilling/rasenshuriken). El **coste de chakra** (200-4000, sin tocar) preserva la diferenciación de tier
(los más caros exigen `/dojutsuchakra setmax` alto). Requieren la naturaleza `yin_yang` (grant) **y** yin+yang
maxeadas.

**Familias (fieles a lo que hace cada cast):** truth_seeking_scorched_earth → **projectile** (bola busca-verdad
que vuela, `tp @s`+daño); sword_of_nunoboko → **primed_strike** (espada de creación, `instant_damage` al golpear;
usa modelo 3D de arma en el datapack → **sin textura plana, sin CMD**, item con textura por defecto);
six_paths_cross_lantern → **area** (explosión AoE, tnt+armor_stands); creation_of_all_things → **heal masivo**
(`full_heals` a sí mismo + aliados ≤8; regeneración + absorción en área).

**Divergencia documentada — `creation_of_all_things_split` diferido a F7:** su cast **no es combate**: divide el
chakra del **Diez Colas en las 9 bestias con cola** (`give/tailed_beast/chakra/1..9_tails`, mecánica jinchuriki).
Pertenece al sistema de bestias con cola (**F7**), no a F4. Es el **#5/último** de la secuencia → diferirlo (sin
JSON) no bloquea nada (mismo patrón que los cloaks finales). La secuencia yin_yang del mod tiene **4** entradas.

- **Validación:** `./gradlew build` multiloader **verde**; **509 tests** verdes (507 + 2: valores yin_yang,
  creation_of_all_things=heal / sword_of_nunoboko=primed_strike). **50 jutsus** totales. Siguiente:
  **DJT-090** (Taijutsu).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-090 — Volcado Taijutsu (técnicas de skill puro)

- **Qué:** volcar las técnicas de **taijutsu** gateadas solo por el **skill de taijutsu** (grupo `taij_req_N`) +
  auto-desbloqueo + texturas.
- **Hallazgo (regla 16):** **no existe learner de taijutsu** — el learner solo cubre naturalezas. Los jutsus de
  taijutsu se dan por **hitos de skill** (`relearn_taijutsu`/`unlocks`), no por item learner. Dos grupos:
  (a) `taij_req_1..4` gateados por `d.taijutsu` 100/150/200/250; (b) **Ocho Puertas** (`eg_req_N`) gateados por
  `d.taijutsu` alto **Y por tener la puerta abierta** (`eight_gates/stage_N_8`).
- **Archivos (nuevos):** 4 JSON `jutsu/taijutsu/*.json` + 4 texturas/modelos; test.
- **Archivos (editados):** `unlocks/progression.json` (+4 hitos taijutsu); `items/JutsuModelData` (+4 CMD);
  modelo base (53 overrides, sin duplicados — verificado); `en_us.json` (+4); tests.
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`. Depende de DJT-089.

**Valores auditados (chakra / `d.taijutsu` datapack → /4 mod):** chakra_enhanced_strength 40 / 100→**25** ·
heavenly_foot_of_pain 80 / 150→**38** · crimson_drizzle 100 / 200→**50** · cherry_blossom_impact 200 / 250→**63**.
El skill de taijutsu se **reescala /4** (datapack 0-2000 → mod 0-500), **misma convención que kenjutsu**.

**Familias (fieles al cast):** chakra_enhanced_strength → **primed_strike** (golpe potenciado, `instant_damage`+crit);
heavenly_foot_of_pain → **primed_strike** (patada de hachazo, lunge+daño); crimson_drizzle → **area** (explosión
tnt); cherry_blossom_impact → **area** (impacto mayor, 3× tnt).

**Desbloqueo (sin learner):** como kenjutsu/fuinjutsu, se añaden **hitos en `progression.json`**
(`taijutsu_N_<id>`). A diferencia de los de kenjutsu (provisionales, 5-40), los de taijutsu usan el **valor
auditado** (25/38/50/63): en el datapack el umbral de **casteo == umbral de desbloqueo** (`d.taijutsu` N), así que
alinear ambos es más fiel. `UnlockService` aún solo registra el hito (la **entrega del item es `TODO F4`**, común a
todos los skills); el casteo se prueba con `/dojutsujutsu cast`, que sí valida el skill.

**Divergencia documentada — Ocho Puertas diferidas a F6:** front_lotus, morning_peacock, daytime_tiger,
evening_elephant y night_guy exigen tener la **puerta abierta** para castear (`eight_gates/stage_N_8`). Eso es el
**subsistema de las Ocho Puertas** (apertura de puertas, autodaño, buffs escalados) que el mod **no tiene** → se
difieren a **F6** (empoderamientos), igual que los modes/cloaks. (`reverse_four_symbols_sealing` no es taijutsu:
es un desbloqueo de **fuinjutsu**.)

- **Validación:** `./gradlew build` multiloader **verde**; **510 tests** verdes (509 + 1: valores de taijutsu).
  **54 jutsus** totales. Siguiente: **DJT-091** (Kekkei Genkai / jutsu de clan).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-091 — Contrato de jutsu para KG (afinidad múltiple + gate de posesión) — *infraestructura*

- **Qué:** extender el contrato de técnica para poder volcar jutsus de **Kekkei Genkai**. **No vuelca jutsus**
  (eso es DJT-092+, una naturaleza KG por ticket); habilita el esquema que necesitan.
- **Hallazgo (regla 16) que forzó este ticket:** los jutsus KG del datapack **no** gatean por una "afinidad
  KG" (no existe). Gatean por las **afinidades de sus 2-3 naturalezas básicas componentes** (Madera =
  `aff_earth` **Y** `aff_water`; Cristal = earth+water+yang) **más** poseer el KG (`check_kekkei_genkai`). El
  `JutsuDefinition` solo admitía **una** `AffinityRequirement` y **ningún** gate de KG → bloqueaba todo el
  volcado KG. (Decisión de enfoque aprobada por el usuario: *extender el contrato, fiel*.)
- **Archivos (nuevos):** `domain/jutsu/KekkeiGenkaiRequirement.java` (record: id de KG, puerta de posesión).
- **Archivos (editados):**
  - `domain/jutsu/JutsuDefinition`: `Optional<AffinityRequirement>` → **`List<AffinityRequirement>
    affinityRequirements`** (todas obligatorias) + **`Optional<KekkeiGenkaiRequirement>`**. Constructor de
    conveniencia 8-arg (una afinidad `Optional`, sin KG) que delega al canónico → **cero churn** en call sites y
    tests previos. Accesor `affinityRequirement()` = la primera (feedback/XP).
  - `domain/jutsu/Jutsu.evaluatePreconditions`: nuevo orden **chakra → cd → posesión KG → posesión naturaleza(s)
    → nivel afinidad(es) → skill**; itera **todas** las afinidades.
  - `domain/jutsu/CastContext`: `default boolean hasKekkeiGenkai(id)`; `CastFailureReason.MISSING_KEKKEI_GENKAI`.
  - `jutsu/PlayerCastContext.hasKekkeiGenkai` → `data.natures().has(KekkeiGenkai)` (ya existía en `NatureProfile`).
  - `jutsu/JutsuEntry`: codec de afinidad **objeto-o-array** (`Either`, retrocompat: un objeto suelto sigue
    decodificando; array = varias) + campo `kekkeiGenkai` (`EnumCodecs.KEKKEI_GENKAI`).
  - `jutsu/JutsuCastPipeline`: `CastOutcome.MISSING_KEKKEI_GENKAI` + feedback (mensaje de KG; y con afinidad
    múltiple reporta la **naturaleza deficiente concreta**, no siempre la primera).
  - `en_us.json`: `jutsu.dojutsu.missing_kekkei_genkai` = "Lack required Kekkei Genkai."
- **Motivo:** `[06 §Plantilla-Jutsu]`, regla 16. Extiende el mecanismo de requisitos como ya creció con
  `SkillRequirement` (DJT-083) — no es rediseño, es más contrato data-driven (RA-1).

**Semántica fiel:** el cast de Madera ejecuta `check_kekkei_genkai` **y** comprueba las dos afinidades → ambos
gates son del datapack. La posesión de KG en el cast es defensa en profundidad además del gate del learner.

**Nota para DJT-092 (Madera):** la **XP de afinidad** (`JutsuXpHandler`) usa hoy la primera afinidad
(`affinityRequirement()`), así que un jutsu KG daría XP solo a `earth`. Verificar en DJT-092 qué naturaleza(s)
sube el datapack al castear madera y, si procede, iterar todas. No afecta a ningún jutsu actual (ninguno es
multi-afinidad todavía).

- **Autorrevisión:** retrocompat verificada por test (objeto suelto → 1 requisito; array → varios; sin campo →
  lista vacía). Orden de puertas testeado (KG antes que afinidad). Sin cambios en jutsus existentes.
- **Validación:** `./gradlew build` multiloader **verde**; **520 tests** verdes (510 + 10: 6 de precondiciones
  KG, 4 de codec). **54 jutsus** (sin volcado nuevo). Siguiente: **DJT-092** (volcado Madera/Mokuton, primer KG).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-092 — Volcado Madera / Mokuton (primer Kekkei Genkai)

- **Qué:** volcar las **8 técnicas de Madera** usando el contrato KG de DJT-091 (primer KG real) + secuencia
  learner + texturas + **XP multi-naturaleza**.
- **Archivos (nuevos):** 8 JSON `jutsu/wood/*.json` + `learner_sequences/wood.json` + 8 texturas/modelos; tests.
- **Archivos (editados):** `jutsu/JutsuXpHandler` (→ **multi-naturaleza**: `affinityNaturesOf`); `items/JutsuModelData`
  (+8 CMD); modelo base (61 overrides, sin duplicados — verificado); `en_us.json` (+8); tests.
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`, DJT-091. Primer consumidor del gate KG.

**Valores auditados (chakra / `aff_earth`==`aff_water` del datapack):** tree_bind_flourishing_burial 60/100 ·
great_spear_tree 100/120 · great_forest 120/130 · wood_dome 140/160 · spontaneous_tree_summoning 300/250 ·
four_pillar_prison 400/350 · four_pillar_house 800/400 · nativity_of_a_world_of_trees 1000/**800→500**. Cada uno
gatea por **poseer `wood`** (`check_kekkei_genkai`) **Y** las dos afinidades componentes (earth **y** water,
mismo nivel, raw; solo nativity capa a 500 como kirin/rasenshuriken). `"affinity"` va como **array** de dos.

**XP multi-naturaleza (regla 16):** el datapack hace al castear Madera `add d.earth_xp 1` **Y** `add d.water_xp 1`.
`JutsuXpHandler` pasó de resolver **una** naturaleza a `affinityNaturesOf` (itera **todas** las afinidades y da +1
a cada una) → castear Madera sube earth y water. Cierra la nota pendiente de DJT-091. (Los jutsus de una sola
afinidad no cambian: `affinityNatureOf` sigue devolviendo la primera.)

**Familias (fieles al cast — Mokuton construye madera):** casi todos **structure** (`oak_log`/`oak_leaves`, tamaños
según el jutsu: great_spear_tree = viga 1×1×5; wood_dome/prison/house = cajas; great_forest/nativity = follaje);
tree_bind_flourishing_burial → **area** (raíces: slowness+mining_fatigue, `effect give` del datapack).
`earth_golem`/summons reales de árbol con entidad (armor_stand del datapack) siguen siendo **F9**; aquí se
aproximan con structure, fiel al `setblock`/`place` dominante del cast.

- **Validación:** `./gradlew build` multiloader **verde**; **522 tests** verdes (520 + 2: valores de Madera con
  contrato KG, XP multi-naturaleza). **Pendiente de validación en runtime en ambos loaders**. **62 jutsus**
  totales. Siguiente: **DJT-093** (siguiente KG: Hielo, water+wind).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-093 — Volcado Hielo (KG water+wind)

- **Qué:** volcar las **5 técnicas de Hielo** con el contrato KG (DJT-091) + secuencia learner + texturas.
- **Archivos (nuevos):** 5 JSON `jutsu/ice/*.json` + `learner_sequences/ice.json` + 5 texturas/modelos; test.
- **Archivos (editados):** `items/JutsuModelData` (+5 CMD); modelo base (66 overrides, sin duplicados —
  verificado); `en_us.json` (+5); tests.
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`, DJT-091/092. Segundo KG.

**Valores auditados (chakra / `aff_water`==`aff_wind`):** frozen_waste 80/60 · blizzard 120/80 · ice_golem 100/100
· ice_spear 100/120 · ice_rock_dome 140/160. KG `ice` (componentes **wind+water**, confirmado con la def del KG);
gate `check_kekkei_genkai` + ambas afinidades (raw, todas ≤500). XP a water y wind (multi-naturaleza, DJT-092).

**Familias (fieles al cast):** frozen_waste/blizzard → **area** (congelación: slowness ± blindness);
ice_spear/ice_rock_dome → **structure** (`packed_ice`: viga 1×1×5 / cúpula); **ice_golem → structure**
(`snow_block` 3×3×2). Divergencia: el cast del datapack hace `summon snow_golem` (entidad vanilla), pero **no hay
familia summon** → se aproxima con structure, igual que `earth_golem`; el summon real de entidad es **F9**.

- **Validación:** `./gradlew build` multiloader **verde**; **523 tests** verdes (522 + 1: valores de Hielo con
  contrato KG). **Pendiente de validación en runtime en ambos loaders**. **67 jutsus** totales. Siguiente:
  **DJT-094** (Lava, earth+fire).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-094 — Volcado Lava (KG earth+fire)

- **Qué:** volcar las **3 técnicas de Lava** con el contrato KG (DJT-091) + secuencia learner + texturas.
- **Archivos (nuevos):** 3 JSON `jutsu/lava/*.json` + `learner_sequences/lava.json` + 3 texturas/modelos; test.
- **Archivos (editados):** `items/JutsuModelData` (+3 CMD); modelo base (69 overrides, sin duplicados —
  verificado); `en_us.json` (+3); tests.
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`, DJT-091. Tercer KG.

**Valores auditados (chakra / `aff_earth`==`aff_fire`):** lava_snake_bullet 180/150 · raining_debris 200/200 ·
intense_flow 400/300. KG `lava` (componentes **fire+earth**, confirmado); gate `check_kekkei_genkai` + ambas
afinidades (raw, todas ≤500). XP a earth y fire (multi-naturaleza).

**Familias (fieles al cast):** lava_snake_bullet → **projectile** (`summon arrow` del datapack; bala de lava,
`magma_cream`); raining_debris/intense_flow → **area** (lluvia/flujo de lava: daño + slowness). Nota: MC no tiene
efecto "fuego" como MobEffect, así que el daño representa el calor (igual que las técnicas de fuego).

- **Validación:** `./gradlew build` multiloader **verde**; **524 tests** verdes (523 + 1: valores de Lava con
  contrato KG). **Pendiente de validación en runtime en ambos loaders**. **70 jutsus** totales. Siguiente:
  **DJT-095** (Cristal, earth+water+yang — primer KG de **tres** afinidades).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-095 — Volcado Cristal (KG earth+water+yang, primer KG de tres afinidades)

- **Qué:** volcar las **4 técnicas de Cristal** con el contrato KG (DJT-091) + secuencia learner + texturas. Primer
  KG de **tres** naturalezas componentes — valida que el array de afinidad aguanta >2 sin tocar el contrato.
- **Archivos (nuevos):** 4 JSON `jutsu/crystal/*.json` + `learner_sequences/crystal.json` + 4 texturas/modelos;
  test.
- **Archivos (editados):** `items/JutsuModelData` (+4 CMD); modelo base (73 overrides, sin duplicados —
  verificado); `en_us.json` (+4); tests.
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`, DJT-091. Cuarto KG.

**Valores auditados (chakra / `aff_earth`==`aff_water`==`aff_yang`):** crystal_wall 60/50 · crystal_needles 120/80
· jade_crystal_prison 150/120 · the_gods_crossing 150/160. KG `crystal` (componentes **earth+water+yang**,
confirmado); gate `check_kekkei_genkai` + las **tres** afinidades (raw, ≤500). XP a earth, water y yang.

**Familias (fieles al cast):** crystal_wall → **structure** (`amethyst_block`, muro 7×5, del datapack con 77
setblocks); jade_crystal_prison → **structure** (caja de cristal); crystal_needles → **projectile**
(`amethyst_shard`); the_gods_crossing → **area** (cristalización AoE: daño + slowness).

- **Validación:** `./gradlew build` multiloader **verde**; **525 tests** verdes (524 + 1: valores de Cristal con
  **tres** afinidades). **Pendiente de validación en runtime en ambos loaders**. **74 jutsus** totales. Siguiente:
  **DJT-096** (Tormenta/Storm, lightning+water).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-096 — Volcado Tormenta / Storm (KG lightning+water)

- **Qué:** volcar las **3 técnicas de Tormenta** con el contrato KG (DJT-091) + secuencia learner + texturas.
- **Archivos (nuevos):** 3 JSON `jutsu/storm/*.json` + `learner_sequences/storm.json` + 3 texturas/modelos; test.
- **Archivos (editados):** `items/JutsuModelData` (+3 CMD); modelo base (76 overrides, sin duplicados —
  verificado); `en_us.json` (+3); tests.
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`, DJT-091. Quinto KG.

**Valores auditados (chakra / `aff_ligh`==`aff_water`):** thunder_cloud_inner_wave 140/120 · laser_circus 180/180
· weather_manipulation 250/250. KG `storm` (componentes **lightning+water**, confirmado; el datapack abrevia
`ligh`, en el mod la id de la naturaleza es `lightning`); gate `check_kekkei_genkai` + ambas afinidades (raw,
≤500). XP a lightning y water.

**Familias (fieles al cast):** thunder_cloud_inner_wave → **area** (onda: daño + slowness); laser_circus →
**projectile** (láseres, `end_rod`); weather_manipulation → **area** (tormenta AoE). Divergencia:
weather_manipulation en el datapack hace `weather`+`summon lightning_bolt` (cambia el clima) — **no hay familia
weather** → se aproxima con area de daño (rayo de tormenta); el cambio de clima real no se modela.

- **Validación:** `./gradlew build` multiloader **verde**; **526 tests** verdes (525 + 1: valores de Tormenta con
  contrato KG). **Pendiente de validación en runtime en ambos loaders**. **77 jutsus** totales. Siguiente:
  **DJT-097** (Ebullición/Boil, water+fire).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-097 — Volcado Ebullición / Boil (KG water+fire)

- **Qué:** volcar las **4 técnicas de Ebullición** con el contrato KG (DJT-091) + secuencia learner + texturas.
- **Archivos (nuevos):** 4 JSON `jutsu/boil/*.json` + `learner_sequences/boil.json` + 4 texturas/modelos; test.
- **Archivos (editados):** `items/JutsuModelData` (+4 CMD); modelo base (80 overrides, sin duplicados —
  verificado); `en_us.json` (+4); tests.
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`, DJT-091. Sexto KG.

**Valores auditados (chakra / `aff_fire`==`aff_water`):** boiling_mist_dragon 150/140 · erupting_propulsion_fist
200/180 · meltdown 280/240 · skilled_mist 330/300. KG `boil` (componentes **fire+water**, confirmado); gate
`check_kekkei_genkai` + ambas afinidades (raw, ≤500). XP a fire y water.

**Familias (fieles al cast):** boiling_mist_dragon → **projectile** (`magma_cream`); erupting_propulsion_fist →
**primed_strike** (puño de ebullición, golpe cuerpo a cuerpo); meltdown/skilled_mist → **area** (corrosión ácida:
daño + weakness/mining_fatigue/slowness, la técnica de fusión de Mei).

- **Validación:** `./gradlew build` multiloader **verde**; **527 tests** verdes (526 + 1: valores de Ebullición
  con contrato KG). **Pendiente de validación en runtime en ambos loaders**. **81 jutsus** totales. Siguiente:
  **DJT-098** (Magnetismo/Magnet).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-098 — Volcado Magnetismo / Magnet (KG wind+earth)

- **Qué:** volcar las **6 técnicas de Magnetismo** (arena de Gaara/Tercer Kazekage) con el contrato KG (DJT-091)
  + secuencia learner + texturas.
- **Archivos (nuevos):** 6 JSON `jutsu/magnet/*.json` + `learner_sequences/magnet.json` + 6 texturas/modelos; test.
- **Archivos (editados):** `items/JutsuModelData` (+6 CMD); modelo base (86 overrides, sin duplicados —
  verificado); `en_us.json` (+6); tests.
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`, DJT-091. Séptimo KG.

**Valores auditados (chakra / `aff_earth`==`aff_wind`):** sand_wave 120/120 · iron_sand_wall 200/140 ·
iron_sand_wave 140/140 · iron_sand_drizzle 160/160 · sand_binding_coffin 180/180 · sand_waterfall_funeral
300/250. KG `magnet` (componentes **wind+earth**, confirmado); gate `check_kekkei_genkai` + ambas afinidades
(raw, ≤500). XP a earth y wind.

**Familias (fieles al cast):** iron_sand_wall → **structure** (`iron_block`); iron_sand_wave → **projectile**
(`iron_nugget`); sand_wave/iron_sand_drizzle/sand_binding_coffin/sand_waterfall_funeral → **area** (control de
arena: daño + slowness; el ataúd/funeral con slowness fuerte + weakness — el aplastamiento de Gaara).

- **Validación:** `./gradlew build` multiloader **verde**; **528 tests** verdes (527 + 1: valores de Magnetismo
  con contrato KG). **Pendiente de validación en runtime en ambos loaders**. **87 jutsus** totales. Siguiente:
  **DJT-099** (Explosión/Explosion).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-099 — Volcado Explosión / Explosion (KG lightning+earth)

- **Qué:** volcar las **7 técnicas de Explosión** (bombas de arcilla C de Deidara + landmines) con el contrato KG
  (DJT-091) + secuencia learner + texturas.
- **Archivos (nuevos):** 7 JSON `jutsu/explosion/*.json` + `learner_sequences/explosion.json` + 6 texturas/modelos;
  test.
- **Archivos (editados):** `items/JutsuModelData` (+6 CMD); modelo base (92 overrides, sin duplicados —
  verificado); `en_us.json` (+7); tests.
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`, DJT-091. Octavo KG.

**Valores auditados (chakra / `aff_earth`==`aff_ligh`):** c1 140/120 · explosive_landmine 120/140 · landmine_fist
180/180 · exploding_palm 250/250 · c3 280/280 · clay_doll 280/300 · c0 600/**500**. KG `explosion` (componentes
**lightning+earth**, confirmado); gate `check_kekkei_genkai` + ambas afinidades (raw, c0 a 500). XP a earth y
lightning.

**Familias (fieles al cast):** c1 → **projectile** (`clay_ball`, bomba lanzada); landmine_fist → **primed_strike**
(puño explosivo); explosive_landmine/exploding_palm/c3/c0 → **area** (explosión, radio y daño crecientes; c0 =
"Arte Definitivo" AoE enorme).

**Divergencia — clay_doll:** su cast hace `summon wolf`+`summon tnt` (muñeco de arcilla que explota) y **no tiene
textura**. Se aproxima con **area** (explosión) — summon de entidad = **F9** — y va **sin CMD** (item con textura
por defecto, como sword_of_nunoboko). Es el **#6** de la secuencia (no final) → necesita JSON castable para no
bloquear c0; el JSON existe aunque sin textura.

- **Validación:** `./gradlew build` multiloader **verde**; **529 tests** verdes (528 + 1: valores de Explosión
  con contrato KG). **Pendiente de validación en runtime en ambos loaders**. **94 jutsus** totales. Siguiente:
  **DJT-100** (Abrasación/Scorch, fire+wind).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-100 — Volcado Abrasación / Scorch (KG fire+wind)

- **Qué:** volcar las **3 técnicas de Abrasación** con el contrato KG (DJT-091) + secuencia learner + texturas.
- **Archivos (nuevos):** 3 JSON `jutsu/scorch/*.json` + `learner_sequences/scorch.json` + 2 texturas/modelos; test.
- **Archivos (editados):** `items/JutsuModelData` (+2 CMD); modelo base (94 overrides, sin duplicados —
  verificado); `en_us.json` (+3); tests.
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`, DJT-091. Noveno KG.

**Valores auditados (chakra / `aff_fire`==`aff_wind`):** great_steaming_explosive_blast 180/150 ·
multiple_falling_suns 230/200 · extremely_steaming_murder 300/300. KG `scorch` (componentes **fire+wind**,
confirmado); gate `check_kekkei_genkai` + ambas afinidades (raw, ≤500). XP a fire y wind.

**Familias (fieles al cast):** great_steaming_explosive_blast → **projectile** (`blaze_powder`;
**sin textura → sin CMD**, textura por defecto; es el #1 de la secuencia, JSON castable);
multiple_falling_suns → **area** (soles cayendo, AoE); extremely_steaming_murder → **area** (la abrasación de
Pakura que evapora la humedad: daño + weakness + wither, la "deshidratación").

- **Validación:** `./gradlew build` multiloader **verde**; **530 tests** verdes (529 + 1: valores de Abrasación
  con contrato KG). **Pendiente de validación en runtime en ambos loaders**. **97 jutsus** totales. Siguiente:
  **DJT-101** (Acero/Steel).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-101 — Volcado Acero / Steel (KG fire+earth+water, segundo KG de tres afinidades)

- **Qué:** volcar las **4 técnicas de Acero** con el contrato KG (DJT-091) + secuencia learner + texturas.
- **Archivos (nuevos):** 4 JSON `jutsu/steel/*.json` + `learner_sequences/steel.json` + 4 texturas/modelos; test.
- **Archivos (editados):** `items/JutsuModelData` (+4 CMD); modelo base (98 overrides, sin duplicados —
  verificado); `en_us.json` (+4); tests.
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`, DJT-091. Décimo KG (segundo de **tres** afinidades tras Cristal).

**Valores auditados (chakra / `aff_earth`==`aff_fire`==`aff_water`):** steel_projectile 120/80 · impervious_armor
200/160 · steel_shield 140/160 · steel_spike 180/200. KG `steel` (componentes **fire+earth+water**, confirmado);
gate `check_kekkei_genkai` + las **tres** afinidades (raw, ≤500). XP a earth, fire y water.

**Familias (fieles al cast):** steel_projectile → **projectile** (`iron_ingot`); impervious_armor → **buff**
(armadura impenetrable: resistance 3 + slowness, del `effect give` del datapack); steel_shield → **structure**
(`iron_block`); steel_spike → **area** (púas de acero: daño + slowness).

- **Validación:** `./gradlew build` multiloader **verde**; **531 tests** verdes (530 + 1: valores de Acero con
  **tres** afinidades). **Pendiente de validación en runtime en ambos loaders**. **101 jutsus** totales.
  Siguiente: **DJT-102** (Oscuridad/Dark).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-102 — Volcado Oscuridad / Dark (KG wind+yin)

- **Qué:** volcar las **2 técnicas de Oscuridad** (Dark Release/Jiton) con el contrato KG (DJT-091) + secuencia
  learner + texturas.
- **Archivos (nuevos):** 2 JSON `jutsu/dark/*.json` + `learner_sequences/dark.json` + 2 texturas/modelos; test.
- **Archivos (editados):** `items/JutsuModelData` (+2 CMD); modelo base (100 overrides, sin duplicados —
  verificado); `en_us.json` (+2); tests.
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`, DJT-091. Undécimo KG (primero con `yin` como componente).

**Valores auditados (chakra / `aff_wind`==`aff_yin`):** judgment 120/140 · inhaling_maw 160/200. KG `dark`
(componentes **wind+yin**, confirmado); gate `check_kekkei_genkai` + ambas afinidades (raw, ≤500). XP a wind y
yin.

**Familias (fieles al cast):** judgment → **projectile** (`coal`, orbe oscuro); inhaling_maw → **area**
(la Fauce Inhaladora que absorbe chakra: drenaje con weakness + slowness).

- **Validación:** `./gradlew build` multiloader **verde**; **532 tests** verdes (531 + 1: valores de Oscuridad
  con contrato KG). **Pendiente de validación en runtime en ambos loaders**. **103 jutsus** totales. Siguiente:
  **DJT-103** (Rapidez/Swift).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-103 — Volcado Rapidez / Swift (KG wind+lightning)

- **Qué:** volcar las **2 técnicas de Rapidez** (Swift Release) con el contrato KG (DJT-091) + secuencia learner
  + texturas.
- **Archivos (nuevos):** 2 JSON `jutsu/swift/*.json` + `learner_sequences/swift.json` + 2 texturas/modelos; test.
- **Archivos (editados):** `items/JutsuModelData` (+2 CMD); modelo base (102 overrides, sin duplicados —
  verificado); `en_us.json` (+2); tests.
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`, DJT-091. Duodécimo KG (el del clan Namikaze).

**Valores auditados (chakra / `aff_ligh`==`aff_wind`):** shadowless_flight 100/160 · instantaneous_movement
200/300. KG `swift` (componentes **wind+lightning**, confirmado); gate `check_kekkei_genkai` + ambas afinidades
(raw, ≤500). XP a lightning y wind.

**Familias (fieles al cast):** shadowless_flight → **buff** (vuelo sin sombra: speed 4 + jump_boost 3 +
invisibility, del `effect give` del datapack); instantaneous_movement → **teleport** (parpadeo, como los
`_instantaneous_body`).

- **Validación:** `./gradlew build` multiloader **verde**; **533 tests** verdes (532 + 1: valores de Rapidez con
  contrato KG). **Pendiente de validación en runtime en ambos loaders**. **105 jutsus** totales. Siguiente:
  **DJT-104** (Tifón/Typhoon).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-104 — Volcado Tifón / Typhoon (KG wind+water)

- **Qué:** volcar las **2 técnicas de Tifón** con el contrato KG (DJT-091) + secuencia learner + texturas.
- **Archivos (nuevos):** 2 JSON `jutsu/typhoon/*.json` + `learner_sequences/typhoon.json` + 2 texturas/modelos;
  test.
- **Archivos (editados):** `items/JutsuModelData` (+2 CMD); modelo base (104 overrides, sin duplicados —
  verificado); `en_us.json` (+2); tests.
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`, DJT-091. Decimotercer KG.

**Valores auditados (chakra / `aff_water`==`aff_wind`):** consecutive_bursting_strong_winds 180/150 ·
great_consecutive_bursting_extreme_winds 230/250. KG `typhoon` (componentes **wind+water**, confirmado); gate
`check_kekkei_genkai` + ambas afinidades (raw, ≤500). XP a water y wind.

**Familias (fieles al cast):** ambas → **area** (ráfagas de tifón: daño + slowness, radio y daño crecientes).

- **Validación:** `./gradlew build` multiloader **verde**; **534 tests** verdes (533 + 1: valores de Tifón con
  contrato KG). **Pendiente de validación en runtime en ambos loaders**. **107 jutsus** totales. Siguiente:
  **DJT-105** (Lodo/Mud).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-105 — Volcado Lodo / Mud (KG earth+water)

- **Qué:** volcar las **3 técnicas de Lodo** con el contrato KG (DJT-091) + secuencia learner + texturas.
- **Archivos (nuevos):** 3 JSON `jutsu/mud/*.json` + `learner_sequences/mud.json` + 3 texturas/modelos; test.
- **Archivos (editados):** `items/JutsuModelData` (+3 CMD); modelo base (107 overrides, sin duplicados —
  verificado); `en_us.json` (+3); tests.
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`, DJT-091. Decimocuarto KG.

**Valores auditados (chakra / `aff_earth`==`aff_water`):** muddy_water_wall 100/100 · landslide 120/130 ·
mud_doll 240/200. KG `mud` (componentes **earth+water**, confirmado); gate `check_kekkei_genkai` + ambas
afinidades (raw, ≤500). XP a earth y water.

**Familias (fieles al cast):** muddy_water_wall → **structure** (`minecraft:mud`); landslide → **area**
(desprendimiento: daño + slowness); mud_doll → **area** (muñeco de lodo: daño + slowness). Divergencia: mud_doll
hace `summon wolf` (muñeco animado); se aproxima con area — summon de entidad = **F9** — pero **sí tiene textura**
(a diferencia de clay_doll), así que conserva su CMD.

- **Validación:** `./gradlew build` multiloader **verde**; **535 tests** verdes (534 + 1: valores de Lodo con
  contrato KG). **Pendiente de validación en runtime en ambos loaders**. **110 jutsus** totales. Siguiente:
  **DJT-106** (Ígnea/Blaze — la Llama de Amaterasu de Sasuke).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-106 — Volcado Ígnea / Blaze (KG solo posesión, sin afinidad)

- **Qué:** volcar las **2 técnicas de Ígnea** (Enton, las llamas negras de Amaterasu) con el contrato KG
  (DJT-091) + secuencia learner + texturas.
- **Archivos (nuevos):** 2 JSON `jutsu/blaze/*.json` + `learner_sequences/blaze.json` + 2 texturas/modelos; test.
- **Archivos (editados):** `items/JutsuModelData` (+2 CMD); modelo base (109 overrides, sin duplicados —
  verificado); `en_us.json` (+2); tests.
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`, DJT-091. Decimoquinto KG.

**Hallazgo (regla 16) — dos divergencias del patrón KG habitual:**
- **Gate solo por posesión:** el KG `blaze` tiene **una** componente (`fire`) y sus casts gatean **solo** por
  `check_kekkei_genkai` + chakra 180 — **sin `aff_X matches N`**. Es el **primer KG sin requisito de afinidad**:
  el array `affinity` va **vacío**, solo `kekkeiGenkai: "blaze"`. Buen caso del contrato (puerta de KG sola). Como
  no hay afinidad, **no da XP** (`affinityNaturesOf` vacío) — fiel (el cast no hace `add d.X_xp`).
- **`kagutsuchi_blade` es un arma, no un jutsu:** el datapack lo da como `iron_sword{KagutsuchiBlade:1b, +8
  attack, CustomModelData:1004}` ("Requirements: None") — pertenece al **sistema de armas** (como
  samehada/kubikiribocho), no al volcado de jutsus. Se **excluye** de la secuencia (que queda
  `[honoikazuchi, kagutsuchi]`); su implementación como arma es trabajo aparte de F4.

**Valores auditados (chakra, sin afinidad):** honoikazuchi 180 · kagutsuchi 180. Gate: poseer `blaze`.

**Familias (fieles al cast):** honoikazuchi → **projectile** (llama negra, `coal`); kagutsuchi → **area** (las
llamas negras moldeadas: daño + weakness + slowness).

- **Validación:** `./gradlew build` multiloader **verde**; **536 tests** verdes (535 + 1: gate de Ígnea solo por
  KG, afinidad vacía). **Pendiente de validación en runtime en ambos loaders**. **112 jutsus** totales.
  Siguiente: **DJT-107** (Polvo/Dust — último KG, earth+wind+fire, primer KG de 3 afinidades con las 3 básicas).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-107 — Volcado Polvo / Dust (último KG) — **cierra los 16 Kekkei Genkai**

- **Qué:** volcar la **técnica definitiva de Polvo** (Jinton de Onoki) con el contrato KG (DJT-091) + secuencia
  learner. **Último KG** — con él quedan **completos los 16**.
- **Archivos (nuevos):** 1 JSON `jutsu/dust/*.json` + `learner_sequences/dust.json`; test.
- **Archivos (editados):** `en_us.json` (+1); tests. (Sin textura → **no** toca JutsuModelData ni el modelo base.)
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`, DJT-091. Decimosexto y último KG.

**Valores auditados (chakra / afinidad):** detachment_of_the_primitive_world 600 / fire=wind=earth **500**
(todas al **máximo**). KG `dust` (componentes **fire+wind+earth**, confirmado); gate `check_kekkei_genkai` + las
**tres** afinidades básicas a 500. Es el jutsu más exigente del mod (endgame absoluto). **Sin textura → sin CMD**
(textura por defecto). XP a fire, wind y earth.

**Familia:** **area** (la Separación del Mundo Primitivo atomiza un cubo enorme: `504 particle dust`+`fill` →
AoE masivo de daño 22, radio 10, con slowness+weakness fuertes).

- **Validación:** `./gradlew build` multiloader **verde**; **537 tests** verdes (536 + 1: Polvo con las tres
  afinidades a 500). **Pendiente de validación en runtime en ambos loaders**. **113 jutsus** totales, **16 KG
  completos** (madera, hielo, lava, cristal, tormenta, ebullición, magnetismo, explosión, abrasación, acero,
  oscuridad, rapidez, tifón, lodo, ígnea, polvo). Siguiente: **DJT-108** (jutsu de clan — investigar qué queda:
  técnicas específicas de clan no cubiertas por naturaleza/KG).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-108 — Volcado Genjutsu (skill genjutsu + afinidad yin)

- **Qué:** volcar las **6 técnicas de genjutsu** (ilusiones) que gatean por skill genjutsu + afinidad yin +
  hitos de auto-desbloqueo. **Decisión de alcance aprobada por el usuario:** de los 22 jutsus sin volcar, este
  ticket cubre el grupo genjutsu; los **dōjutsu** (Sharingan/Rinnegan/Ketsuryugan) se difieren a **F5** (sistema
  de ojos), los **modos/Ocho Puertas** a **F6**, el **Diez Colas** a **F7** y los **summons de entidad** a **F9**.
- **Archivos (nuevos):** 6 JSON `jutsu/genjutsu/*.json` + 5 texturas/modelos; test.
- **Archivos (editados):** `unlocks/progression.json` (+6 hitos genjutsu); `items/JutsuModelData` (+5 CMD);
  modelo base (114 overrides, sin duplicados — verificado); `en_us.json` (+6); tests.
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`, DJT-090/091.

**Primer caso de skill + afinidad juntos:** estos jutsus exigen **ambos** — nivel de `genjutsu` (reescalado **/4**
como kenjutsu/taijutsu) **y** afinidad `yin` (raw). El contrato ya lo soportaba (`evaluatePreconditions` valida
afinidades y luego skill); es el primer volcado que ejercita las dos puertas a la vez.

**Valores auditados (chakra / genjutsu datapack→/4 / `aff_yin`):** paralysis 40 / 60→**15** / 20 ·
phantom_sound_chains **0** / 60→**15** / — · bringer_of_darkness 60 / 70→**18** / 10 · flower_hill 70 / 70→**18** /
30 · hundred_blooming_flowers 80 / 80→**20** / 30 · unknown_fire 120 / 150→**38** / 60. `phantom_sound_chains`
(la Flauta Demoníaca) no gasta chakra ni pide afinidad, solo skill; **sin textura → sin CMD**.

**Familia:** todos **area** — el genjutsu es ilusión, no impacto físico: aplican **debuffs**
(blindness/slowness/weakness, "paralysis" con slowness amp 4). Solo `unknown_fire` (el fuego ilusorio que "quema")
lleva algo de daño. Desbloqueo por **hitos en `progression.json`** (`genjutsu_N_<id>`, requiredLevel = valor
auditado /4), como taijutsu; la afinidad yin es puerta de cast adicional.

- **Validación:** `./gradlew build` multiloader **verde**; **538 tests** verdes (537 + 1: genjutsu con skill +
  afinidad combinados). **Pendiente de validación en runtime en ambos loaders**. **119 jutsus** totales.
  Siguiente: **DJT-109** (Medical: mystical_palm, wide_healing, regenerative_healing, creation_rebirth).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-109 — Volcado Medical (skill medical, familia heal)

- **Qué:** volcar las **4 técnicas médicas** que gatean por skill medical + hitos de auto-desbloqueo.
- **Archivos (nuevos):** 4 JSON `jutsu/medical/*.json` + 4 texturas/modelos; test.
- **Archivos (editados):** `unlocks/progression.json` (+4 hitos medical); `items/JutsuModelData` (+4 CMD);
  modelo base (118 overrides, sin duplicados — verificado); `en_us.json` (+4); tests.
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`, DJT-090/108. Misma vía que el jutsu `healing` piloto.

**Valores auditados (chakra / medical datapack→/4 / `aff_yin`):** regenerative_healing 40 / 10→**3** · wide_healing
80 / 50→**13** · mystical_palm 10 / 100→**25** · creation_rebirth 1000 / 500→**125** / yin 50. Skill medical
reescalado **/4** (como kenjutsu/taijutsu/genjutsu). `creation_rebirth` (la Fuerza de un Centenar de Tsunade)
combina **skill medical + afinidad yin** (segundo caso skill+afinidad tras genjutsu).

**Familia:** todos **heal** (como `healing`): regeneración en área; mystical_palm regen fuerte de corto alcance;
wide_healing en radio amplio; regenerative_healing regen lenta; creation_rebirth regen 20 + absorción + **weakness**
(el coste de la técnica prohibida). Desbloqueo por hitos en `progression.json` (`medical_N_<id>`, requiredLevel =
valor auditado /4).

- **Validación:** `./gradlew build` multiloader **verde**; **539 tests** verdes (538 + 1: medical con skill /4 +
  creation_rebirth con yin). **Pendiente de validación en runtime en ambos loaders**. **123 jutsus** totales.
  Siguiente: **DJT-110** (Fuinjutsu: four_symbols_seal, four_red_yang_formation; summoning→F9).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-110 — Volcado Fuinjutsu (skill fuinjutsu + afinidad yang)

- **Qué:** volcar las **3 técnicas de sellado** que gatean por skill fuinjutsu + afinidad yang. Se encontró un
  tercero (`reverse_four_symbols_sealing`, el Sello Mortal del Tercer Hokage) además de los dos previstos.
- **Archivos (nuevos):** 3 JSON `jutsu/fuinjutsu/*.json` + 3 texturas/modelos; test.
- **Archivos (editados):** `unlocks/progression.json` (**actualizadas** las 3 entradas fuinjutsu provisionales
  5/15/30 → auditadas **300/400/500**); `items/JutsuModelData` (+3 CMD); modelo base (121 overrides, sin
  duplicados — verificado); `en_us.json` (+3); tests.
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`, DJT-090/108/109.

**Valores auditados (chakra / fuinjutsu datapack→/4 / `aff_yang`):** four_symbols_seal 100 / 1200→**300** / 200 ·
reverse_four_symbols_sealing 1200 / 1600→**400** / 400 · four_red_yang_formation 2000 / 2000→**500** / 800→**500**
(cap). Skill fuinjutsu reescalado **/4**; combina **skill + afinidad yang** (tercer caso skill+afinidad). Las
entradas de desbloqueo de `progression.json` (que existían como provisionales de una fase previa) se **alinean**
al valor auditado (unlock == cast), como se hizo con taijutsu.

**Familias (fieles al cast):** four_symbols_seal → **area** (sello: daño + slowness + weakness);
reverse_four_symbols_sealing → **area** (el sello mortal, AoE de alto daño); four_red_yang_formation →
**structure** (la barrera de los Cuatro del Sonido, `red_stained_glass`).

**Nota:** `summoning` (invocación) queda **diferido a F9** (invoca una entidad; sin familia summon), como se
acordó.

- **Validación:** `./gradlew build` multiloader **verde**; **540 tests** verdes (539 + 1: fuinjutsu con skill +
  yang combinados). **Pendiente de validación en runtime en ambos loaders**. **126 jutsus** totales. Siguiente:
  **DJT-111** (Aburame: insect_sphere, insect_tornado — decidir el gate de clan).
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-111 — Volcado Aburame (jutsu de clan) — **cierra el volcado de contenido F4**

- **Qué:** volcar las **2 técnicas de insectos** del clan Aburame. **Último lote de contenido de combate F4.**
- **Hallazgo (regla 16):** insect_sphere/insect_tornado **no tienen gate** de afinidad/skill/KG — su cast solo
  comprueba chakra (20/30). Se entregan por **pertenencia al clan**: el `clans/aburame.json` del mod ya los
  referencia (`startingJutsu: ["insect_sphere"]` + `jutsuUnlockTicks: 78000` desbloquea insect_tornado tras 65
  min). Este ticket **completa esa referencia** (los JSON no existían). Se modelan como jutsus **sin puerta**
  (estilo universal); el clan controla el acceso.
- **Archivos (nuevos):** 2 JSON `jutsu/aburame/*.json` + 2 texturas/modelos; test.
- **Archivos (editados):** `items/JutsuModelData` (+2 CMD); modelo base (123 overrides, sin duplicados —
  verificado); `en_us.json` (+2); tests.
- **Motivo:** `[06 §Plantilla-Jutsu]`, `[S06]`, DJT-108.

**Valores auditados (chakra, sin afinidad/skill):** insect_sphere 20 · insect_tornado 30. **Familias:** ambos
**area** (enjambre: insect_sphere = nube defensiva con slowness+weakness sin daño; insect_tornado = tornado
ofensivo con daño+slowness). Divergencia: insect_tornado hace `function .../summon` (invoca insectos) → area
aproximada; el summon real de entidad es **F9**.

- **Validación:** `./gradlew build` multiloader **verde**; **541 tests** verdes (540 + 1: Aburame sin gate).
  **Pendiente de validación en runtime en ambos loaders**. **128 jutsus** totales.

### Cierre del volcado de contenido F4

Con DJT-111 queda volcado **todo el contenido de jutsus de combate portable con los sistemas actuales**: 5
naturalezas básicas + yin_yang + los **16 Kekkei Genkai** + taijutsu + genjutsu + medical + fuinjutsu + kenjutsu +
aburame + universales. **Diferido a fases posteriores** (subsistemas ausentes): **dōjutsu** (sharingan_izanagi,
sharingan/ketsuryugan_taming_genjutsu, cloak_of_invisibility, bansho_tenin_item_pull) → **F5** (ojos);
**modos/Ocho Puertas** (chakra_cloak, front_lotus, morning_peacock, daytime_tiger, evening_elephant, night_guy) +
todos los `_cloak`/`_mode` → **F6**; **creation_of_all_things_split** (Diez Colas) → **F7**; **summoning** y todos
los summons de entidad (earth_golem, ice_golem, clay_doll, mud_doll, insect_tornado, kagutsuchi_blade como arma) →
**F9**. Siguiente: cerrar F4 (repaso/limpieza del andamiaje debug) o lo que indique el usuario.
  *(Validación runtime CONFIRMADA por el usuario en NeoForge y Fabric, 2026-07-18.)*

## F4 · DJT-112 — Cierre formal de F4 (repaso + limpieza del andamiaje debug)

- **Qué:** repaso de la DoD de F4 y limpieza del andamiaje temporal cuya condición de retiro ya se cumple. **No
  hay volcado nuevo.**
- **Repaso DoD (todo ✅):** contenido de combate cerrado (128 jutsus); motores F4 (pipeline de cast, 7 familias
  de efecto, learner, cooldowns, XP de afinidad, `LastCastRegistry`); **541 tests** verdes; build multiloader
  verde; diferidos con destino claro (dōjutsu→F5, modos/Ocho Puertas→F6, Diez Colas→F7, summons→F9).
- **Limpieza (aprobada por el usuario):**
  - **Docs obsoletos corregidos** (sin cambio de comportamiento): `JutsuCastPipeline` — el javadoc del cooldown
    decía "llega con DJT-077" con un `@link` **roto** a un `applyCooldown` inexistente; ahora describe el
    `cooldownApplier` real. `HealEffect` — quitada la nota de "desviación temporal" (healing ya se gatea por
    `medical >= 5`, DJT-083/109).
  - **`ProjectileDebugCommand` eliminado** (archivo + registro en `DojutsuMod` + import): su condición de retiro
    (que las familias de efecto de **DJT-076** cubran el disparo real) **se cumple** — los proyectiles reales
    (fire_bullet, etc.) y la familia `projectile` lo sustituyen. Sin tests que dependieran de él. Quedan **10**
    comandos debug.
- **El resto del andamiaje debug se conserva:** los otros 10 comandos `/dojutsu*` documentan retiro para F5+
  (onboarding GUI DJT-059/060, menú DJT-040, Modo Sabio F6, items F8, AdminCommands DJT-026) — **no cumplido
  aún** — y son la vía de prueba en runtime hasta que esos sistemas existan.
- **Validación:** `./gradlew build` multiloader **verde**; **541 tests** verdes (sin cambio de conteo: solo
  borrado de andamiaje y correcciones de docs). **Pendiente de validación en runtime en ambos loaders**.

### 🏁 Fase 4 (Combate) — CERRADA

Sistema de jutsus completo y data-driven: **128 técnicas** con el pipeline de cast único (chakra→cd→KG→afinidad(es)
→skill), 7 familias de efecto, contrato extendido (afinidad múltiple + KG), learner, cooldowns por-instancia, XP de
afinidad y base de copia del Sharingan. Lo diferido no es de F4 (necesita ojos/modos/bijuu/invocación de fases
posteriores). Siguiente fase natural: **F5 (dōjutsu/ojos)** — o lo que indique el usuario.

---

## 2026-07-19 · Apertura de F5 — Validación runtime de la deuda de F4 (previa a implementar)

Al abrir la **Fase 5 (Poderes oculares)** se revisó toda la documentación y se detectaron inconsistencias
(reportadas al usuario): (1) el prompt de fase trata F5 como fase final, pero el diseño define **F5 = solo ojos**
y luego F6–F10; (2) **colisión de IDs**: el backlog de diseño planificó F5 como DJT-100…125, ya consumidos por el
volcado de contenido de F4 → los tickets de F5 se **renumeran a DJT-113+**; (3) encabezados de docs (current_state,
backlog, architecture, CLAUDE) decían "F4 en curso" mientras los cuerpos la daban por cerrada; (4) ~20 tickets de
F4 marcados "build+tests; pendiente validación runtime en ambos loaders". **Decisiones del usuario:** F5 = solo
ojos; **saldar la deuda de F4 antes de F5**.

**Validación ejecutada (saldo de la deuda de F4):**
- `./gradlew build` **verde**; `:common:test --rerun-tasks` = **541 tests, 0 fallos, 0 errores** (la doc decía 403;
  crecieron con el contenido de F4).
- **183 JSON de contenido** validados como bien formados (128 jutsus + 22 secuencias + 18 KG + 14 clanes + unlocks).
- **Servidor dedicado arrancado en ambos loaders** (`:neoforge:runServer`, `:fabric:runServer`), ambos "Done" sin
  fallo. Reload listeners bajo **codec estricto**, idénticos en NeoForge y Fabric:
  **Jutsus 128 · Kekkei Genkai 18/18 · Secuencias 22 · Clanes 14 · Desbloqueos 22 · NatureRoll 17 tramos**;
  familias de efecto `projectile, area, buff, teleport, structure, heal, primed_strike`. **0 ERROR del mod**
  (los `No data fixer registered for ninja_projectile/ability_anchor` son ruido vanilla de toda entidad custom).

**Conclusión:** la dimensión de riesgo del volcado masivo (¿parsea y carga bajo el codec estricto en ambos
loaders?) queda **validada en runtime**. Residual no bloqueante: spot-checks de casteo/visual in-game (requieren
cliente interactivo), documentados como checklist manual en `docs/known_issues.md`. Docs sincronizados
(current_state §3.4a, backlog §Fase 4, architecture, CLAUDE). **Listo para comenzar F5/DJT-113.**

---

## 2026-07-19 · F5/DJT-113 — `EyeState` + `EyeDefinition` (grafo de ojos data-driven) `#riesgo-alto`

Primer ticket de la **Fase 5 (Poderes oculares)**. Abre `EP-EYE-CORE`. Alcance: **modelo de datos + carga del
grafo** (los servicios de despertar/evolución/strain/menú son DJT-114…117). Es "estado + contenido sin
comportamiento", como `ProgressionState` (DJT-034) o `NatureProfile` (DJT-050) al abrir su épica.

**Verificación contra el datapack (regla 16), no solo `S05`:**
- Los **33 índices** `Eye:1..33` de `dojutsu/main` se despachan a ~10 `main` distintos; **9 son duplicados
  Eternal Mangekyo (EMS)** del Mangekyo base (mismo `main`) → colapsan a **flag `eternal` del estado** (R4). Los
  tomoe 1/2/3 del Sharingan (`Eye:2/3/4`) comparten `sharingan/main` → **un ojo con nivel interno** (`useLevels`),
  no tres ojos.
- **Umbrales de uso** (predicates): `sharingan_use_level` 1=78000, 2=144001, 3=216000 (acotado ..432000 para
  Uchiha/Otsutsuki), 3_alt=≥216000 (implante, abierto → **se queda en 3 tomoe**), 4=≥360000 (Mangekyō).
  `tenseigan_req2`=`byakugan_use≥864000`. Esta es la **ruta divergente** (R6): misma acción, distinto tramo.
- **Bonus de despertar** (de `<eye>/awaken.mcfunction`): Byakugan/Sharingan +20 max_chakra/+20 xp, Ketsuryugan
  +30/+30, Tenseigan +100/+100, Rinnegan +100/+150.

**Diseño (patrones existentes, sin rediseño):**
- `domain.eye` (puro): `EyeFamily` (enum de familias lógicas), `EyeRequirement` (+`Type` USE_LEVEL/CLAN/FLAG),
  `EyeEvolution` (arista `to`+requisitos). Requisitos y aristas son **datos**, no ramas hardcodeadas (RA-3).
- `dojutsu` (contenido): `EyeDefinition` (record + codec **estricto**) + `EyeRegistry` + `EyeReloadListener`
  (mismo par que `clans`/KG; aislamiento por archivo con try/catch como `JutsuReloadListener`; aviso de
  evoluciones colgantes sin descartar — contenido incremental).
- `data.state.EyeState` (record **inmutable** enchufable, patrón `NatureProfile`): `activeEye`, `eternal`,
  `useLevels` por familia, `awakened`, `awakenTimers`, `eyeStrain` (int; el `StrainMeter` es mutable → el
  servicio DJT-116 lo reconstruye transitorio), `selectedAbility`. Codec **tolerante** (familia desconocida se
  descarta). Enganchado en `PlayerNinjaData` como campo `eye` (sin `dataVersion` bump, sin marcar observable —
  sin consumidor de cliente aún, criterio de `setNatures`/`setClan`).
- `EnumCodecs.EYE_FAMILY` añadido (utilitario compartido de codecs de enum).
- **7 JSON representativos** (`data/dojutsu/eyes/`): byakugan, sharingan (2 evoluciones divergentes),
  mangekyo_itachi (eternalizable, ability piloto `amaterasu`), rinnegan, tenseigan, ketsuryugan, **naka**
  (`implemented=false`, R7 placeholder). El resto del roster se vuelca en tickets posteriores.

**Autorrevisión — bug encontrado y corregido antes de cerrar:** `new EnumMap<>(mapaVacío)` y
`EnumSet.copyOf(colecciónVacía)` lanzan `IllegalArgumentException` (gotcha de Java); rompía `EyeState.DEFAULT`
(142 fallos en cascada vía `PlayerNinjaData`). Corregido usando `new EnumMap<>(EyeFamily.class)+putAll` y
`EnumSet.noneOf(...)+addAll` (mismo patrón que `NatureProfile`). *Lección de proceso:* `gradle | tee | tail`
enmascara el exit code (reporta el de `tail`); siempre verificar el resultado en el log/XML, no el exit de la
tubería.

**Validación:** `./gradlew build` multiloader **verde**; **559 tests** (541 + **18 nuevos**: round-trip NBT,
clamp de strain, tolerancia a familias desconocidas, idempotencia de withers, aislamiento por archivo, y el grafo
Sharingan→Mangekyō→EMS con sus requisitos auditados). **Runtime validado en NeoForge y Fabric**: el
`EyeReloadListener` carga **"Ojos cargados: 7"** en ambos, sin ERROR ni avisos de referencias colgantes.
Docs sincronizados (current_state §3.5/§6, backlog §Fase 5, architecture, CLAUDE). **Siguiente: DJT-114**
(`AwakeningService`, despertar por clan; consume `clans.potentialDojutsu`).

---

## 2026-07-19 · F5/DJT-114 — `AwakeningService` (despertar del dojutsu por clan)

Segundo ticket de F5. Despierta el ojo del clan como el bloque de despertares de `dojutsu/main` {S05}.

**Verificación contra el datapack (regla 16):**
- Despertar por **timer por clan** (`d.<eye>_waketime`) hasta un umbral (predicates): `byak/shar/otsut_waketime`
  = **78000**, `kets_waketime` = **24000**. Bonus (de `<eye>/awaken.mcfunction`): +max_chakra/+ninja_xp
  Byakugan/Sharingan 20/20, Ketsuryugan 30/30 (auditados en DJT-113).
- **Otsutsuki**: `otsutsuki_awaken` tira la loot `50_50` → 1=byakugan, 2=sharingan; despierta **uno**.
- **Lee `potentialDojutsu` del clan, NO predicates `is_*`** (checklist DJT-101): Hyuga=[byakugan], Uchiha=
  [sharingan], Chinoike=[ketsuryugan], Otsutsuki=[byakugan,sharingan], Senju=[] (no despierta).

**Diseño (patrones existentes):**
- `AwakeningService implements PlayerTickSystem` en `TickPhase.DOJUTSU` (fase ya reservada para ojos).
- **Servicio puro + glue**: `evaluate(eyeState, clanId, clan, eyeLookup, random)` decide sin Minecraft
  (testeable con azar sembrado) y devuelve `AwakenResult(nuevoEstado, Optional<ojoDespierto>)`; devuelve la
  **misma instancia** de estado cuando no hay cambio (el glue evita escrituras inútiles). El glue `tick` aplica
  el estado y, al despertar, el bonus (`ChakraService.setMax` +delta, `XpApi.addNinjaXp`), publica
  `EyeAwakenedEvent` (ya existía en `domain.event`) y sincroniza.
- **`awakenTicks` (Optional<Integer>) añadido a `EyeDefinition`** (byakugan/sharingan 78000, ketsuryugan 24000;
  vacío = destino de evolución, no despertable por clan → filtrado en `evaluate`). Es el hogar correcto: el
  umbral es consistente por ojo. `EyeDefinition` tiene sito de sobra (evita tocar el límite de 16 campos de
  `ClanDefinition`).
- **Una vez por clan**: guard `unless _wake` = si ya despertó cualquier ojo potencial del clan, no dispara.
- Azar inyectado con `new Random()` como `ClanService.join` (RandomSource no garantiza `RandomGenerator`).
- Registrado en `DojutsuLifecycle` (pipeline), tras `ClanJutsuUnlockService`.

**Autorrevisión:** el despertar fija `activeEye` además de `awakened` (el DoD pide "ojo activo"; sin flujo de
equipar-ítem, activar el estado es la modelización correcta). El timer se escribe cada tick durante la
acumulación (como el `scoreboard add` del datapack) pero sin marcar observable → sin deltas de red; se detiene al
despertar. Consistencia SOLID/Clean: SRP (decisión pura vs. glue), sin duplicación (reusa `EyeAwakenedEvent`,
`ChakraService`, `XpApi`, `EyeRegistry`).

**Validación:** `./gradlew build` multiloader **verde**; **567 tests** (+8: umbral, roll 50/50, idempotencia una-
vez-por-clan, descarte de ojos sin `awakenTicks`/no implementados, clan sin dojutsu). **Cableado validado en
runtime**: NeoForge y Fabric arrancan limpios con el sistema en el pipeline (ciclo de vida OK, 7 ojos, 0 ERROR).
El **end-to-end in-game es time-gated** (~65 min con jugador conectado) → no automatizable headless; checklist
manual en `known_issues.md §1b`. **Siguiente: DJT-115** (`EvolutionService`, evolución por uso del grafo).

---

## 2026-07-19 · F5/DJT-115 — `EvolutionService` (evolución del ojo por uso, grafo)

Tercer ticket de F5. Evoluciona el ojo activo recorriendo el grafo data-driven de DJT-113 {S05}.

**Verificación contra el datapack (regla 16):**
- El uso sube **pasivamente +1/tick mientras se lleva el ojo** (`wearing_base_sharingan → sharingan_use += 1`;
  `wearing_eye_1 → byakugan_use += 1`; `wearing_mangekyo → sharingan_use+1 & mangekyo_use+1`). Los **terminales**
  (Rinnegan/Tenseigan) **no** acumulan uso → la acumulación es acotada.
- Umbrales de evolución (predicates, ya auditados en DJT-113): sharingan→mangekyo `sharingan_use_level_4`=360000;
  byakugan→tenseigan `tenseigan_req2`=`byakugan_use≥864000`; sharingan→rinnegan por Uchiha+ADN Hashirama.
- Bonus del destino (`<eye>/awaken`): Mangekyō **+20/+80** (mangekyo/awaken), Rinnegan +100/+150, Tenseigan
  +100/+100. Se corrigió `mangekyo_itachi.json` a 20/80.
- **EMS (eternal) es MANUAL** (`eternal/try_give`, +60/+150, exige Mangekyō activo, notificado) — NO está en el
  bloque de auto-evolución de `dojutsu/main`. Por tanto **fuera de DJT-115** (evolución por uso); va al menú/acción
  ocular (DJT-117+). Los **tomoe 1/2/3** son nivel derivado del uso, no transición de grafo.
- La variante de Mangekyō es **aleatoria** en el datapack (`mangekyo_rng`); DJT-115 usa el edge fijo a
  `mangekyo_itachi` (piloto) — el roll de variante se difiere con el resto del roster de ojos.

**Diseño (patrones existentes):**
- `EvolutionService implements PlayerTickSystem` en `TickPhase.DOJUTSU`, tras `AwakeningService`.
- **Servicio puro + glue**: `evaluate(eyeState, clanId, flags, eyeLookup)` decide sin Minecraft (devuelve la
  **misma instancia** si no hay cambio); el glue aplica bonus (`ChakraService`/`XpApi`), `EyeEvolvedEvent` (ya
  existía) y sync. Dos partes: acumulación pasiva (solo si `hasUseGatedEvolution`) + primera arista satisfecha.
- **Requisitos data-driven**: `USE_LEVEL` (rango inclusivo, como `entity_scores`), `CLAN` (predicate **compuesto**
  `otsutsuki_uchiha` = split por `_` → Otsutsuki o Uchiha; ids de clan son tokens simples), `FLAG`
  (`hashirama_dna` sobre `NatureProfile`). **Rutas divergentes = datos, no ramas** (RA-3): el implante nunca
  cumple el clan → no llega a Mangekyō. Precedencia = orden de la lista de aristas.

**Autorrevisión — deuda reportada (no corregida, fuera de alcance):** `sharingan/awaken` fija
`sharingan_use=78000` (tomoe 1 al despertar); `AwakeningService` (DJT-114) no lo hace → evolución a Mangekyō
tarda 78000 ticks de más. Reportado en `known_issues.md §1c` con fix propuesto (`awakenInitialUse`), pendiente de
autorización. Consistencia SOLID/Clean: SRP (decisión pura vs. glue), sin duplicación (reusa evento/servicios/
registro), retrocompatible.

**Validación:** `./gradlew build` multiloader **verde**; **575 tests** (+8: acumulación, evolución al umbral,
implante que no llega a Mangekyō, Rinnegan por ADN, precedencia por orden, terminal sin acumulación, sin ojo,
`clanMatches` compuesto). **Cableado validado en runtime**: NeoForge y Fabric arrancan limpios con el sistema en
el pipeline (0 ERROR, 7 ojos). El **end-to-end es time-gated** (uso a 360000 ≈ 5 h) → check manual en
`known_issues.md §1b`. **Siguiente: DJT-116** (`EyeStrainService`, fatiga ocular con `StrainMeter`).

---

## 2026-07-19 · F5 — Fix del hueco de DJT-114 + DJT-116 `EyeStrainService`

### Fix del hueco de DJT-114 (uso inicial del Sharingan al despertar) — autorizado
`sharingan/awaken` del datapack fija `d.sharingan_use=78000` (nace en tomoe 1). Se añadió el campo opcional
`awakenInitialUse` a `EyeDefinition` (`sharingan.json`=78000; Byakugan/Ketsuryugan no lo fijan) y
`AwakeningService.evaluate` hace `withUseLevel(family, awakenInitialUse)` al despertar. 2 tests nuevos. Cerrado en
`known_issues.md §1c`.

### DJT-116 — `EyeStrainService` (fatiga ocular)
Cuarto ticket de F5 (`eye_strain/main` del datapack, {S05}).

**Verificación contra el datapack (regla 16):**
- **Regen**: `d.eyes_timer` cuenta 200→0; al pulso, `eye_strain -= 1` (o `-= 3` con `has_hsdna`). Clamp a 0/100.
- **Efectos**: `if strain/95 → weakness 1 0 true` (todos); `if strain/98_101 unless wearing_eye_10 → blindness
  120 0 true`; `if wearing_dojutsu unless wearing_eye_10 if strain/100 → force_deactivate`. El **Rinne Sharingan**
  (`eye:10`) está exento de ceguera y apagado, **no** de weakness.
- **Reset al dormir**: `chakra/sleep/wake_up` hace `eye_strain=0`.

**Diseño:**
- `EyeStrainService implements PlayerTickSystem` en `TickPhase.STRAIN`. `regen(EyeState, hashiramaDna)` y
  `effectsFor(strain, strainlessEye)→StrainEffects` **puros** (13 tests); el glue aplica MobEffects
  (weakness 20t/blindness 2400t, hidden) y el apagado (`withoutActiveEye`).
- **`addStrain`** puerta única de subida (la llamarán las habilidades de DJT-118+; sin emisor aún, como
  `addAffinityXp` antes de F4). **`resetStrain`** enganchado en `SleepChakraHandler` vía un **registro de handlers
  de despertar** (patrón proveedor de `RegenService`): `chakra` NO importa `dojutsu`; `EyeStrainService`
  (dojutsu→chakra, descendente) se registra. Reemplaza el `// TODO F5` de `SleepChakraHandler.onWakeUp`.
- **Refactor**: `EyeState.awakenTimers`→`timers` (mapa genérico de temporizadores por propósito; el datapack
  tiene `eyes_timer` junto a `byak_waketime`). Métodos `awakenTimer`/`withAwakenTimer`→`timer`/`withTimer`. El
  timer de regen usa la clave `eye_strain_regen`. Actualizados `AwakeningService(+Test)`, `EyeStateTest`.
- **Avisos de chat** (25/50/75/90/99 con anti-spam `esw_*`) diferidos: son presentación y necesitan estado "ya
  avisado" persistido; no bloquean la mecánica.

**Validación:** `./gradlew build` multiloader **verde**; **590 tests** (+13 DJT-116: regen con/sin pulso y ADN,
clamp a 0, umbrales 95/98/100, exención del Rinne Sharingan, `addStrain`/`resetStrain`). **Cableado validado en
runtime** en NeoForge y Fabric (arranque limpio, 7 ojos, 0 ERROR). Los **efectos end-to-end** esperan un emisor de
strain (habilidades de DJT-118+) o inyección manual → check manual. **Siguiente: DJT-117** (menú de habilidades
oculares: `AbilitySelectScreen` + payload validado).

---

## 2026-07-19 · F5/DJT-117 — Menú de habilidades oculares (`AbilitySelectScreen` + payloads)

Quinto ticket de F5, **primer cliente/GUI de la fase** (`[S05]` menú por hotbar → GUI real, RM-2).

**Verificación contra el datapack (regla 16):** el datapack usa el **hotbar como selector** de habilidades
mientras el ojo esta activo (sneak sobre un slot ejecuta; slot_8 = Deactivate; page_1 = integracion Sabio). El
ADD manda GUI real (`[ADD/05 §GUI]`) → se reemplaza por una Screen con botones + payload validado.

**Diseño (patrones existentes: onboarding S2C-options + MenuAction C2S validado):**
- **Request/response autocontenido** (NO toca la sync principal `SyncNinjaDataPayload`): keybind **N** →
  `OpenEyeMenuPayload` (C2S, marcador) → el servidor construye la vista desde `EyeState`+`EyeRegistry`
  (`EyeMenuService.viewFor`) → `EyeMenuPayload` (S2C: ojo activo + habilidades + seleccion) → `ClientEyeMenu`
  (cache read-only) → `AbilitySelectScreen` se reconstruye. La **lista de habilidades viaja por red** porque el
  `EyeRegistry` es data-driven **server-side** (en dedicado el cliente no lo tiene) — mismo motivo y patron que
  `OnboardingOptionsPayload`.
- **Seleccion validada en servidor** (`[ADD/07]`): `SelectEyeAbilityPayload` (C2S) → `EyeMenuService.select`
  comprueba que la habilidad **pertenece al ojo activo real** (nadie selecciona una habilidad que su ojo no
  ofrece), guarda `EyeState.selectedAbility` y responde con la vista refrescada (la Screen la detecta por un
  contador de version y reconstruye la seleccion). **No usa hotbar** (checklist DJT-104).
- **Capas**: `EyeMenuService` (dojutsu) es puro y no conoce red ni MC-cliente (devuelve un `EyeMenuView`); el
  `EyeMenuHandler` (network/handler) es el glue. `ClientEyeMenu` sin imports de MC-cliente (inocuo en servidor).
  `AbilitySelectScreen` solo se referencia desde `KeyBindings` (client-only, initClient).
- **Anti-spam** solo en la seleccion (mutacion); el abrir es solo lectura. `forget` al desconectar. Claves de
  idioma anadidas (keybind, titulo, "sin ojo", 21 habilidades de los ojos piloto).

**Autorrevisión:** el cliente nunca decide gameplay (la vista y la validacion son del servidor); la Screen abre
aunque no haya ojo (muestra "No active dojutsu") — evita necesitar sincronizar el ojo activo solo para el keybind.
Consistencia SOLID/Clean: SRP (servicio puro vs. handler glue vs. Screen), sin duplicacion (reusa el puerto
`NetworkService`, el patron de cache read-only y el de payload validado).

**Validación:** `./gradlew build` multiloader **verde**; **596 tests** (+6: vista con/sin ojo, seleccion
valida/ajena/sin-ojo/repetida). **Servidor y payloads validados en runtime** en NeoForge y Fabric (los 3 payloads
del menu se registran, arranque limpio, 0 ERROR). El **end-to-end de la GUI** (pulsar N, ver el menu, seleccionar)
exige un **cliente interactivo** no automatizable headless → check manual. **Siguiente: DJT-118** (interfaz
`EyeAbility` + habilidad piloto **Amaterasu** vía `AbilityAnchor`; ejecuta la `selectedAbility` y consume strain).

---

## 2026-07-19 · F5/DJT-118 — `EyeAbility` + piloto Amaterasu (abre EP-EYE-ABILITIES)

Sexto ticket de F5, primera **habilidad ocular** ejecutable; establece la plantilla del catálogo `[S05]`.

**Verificación contra el datapack (regla 16):** Amaterasu (`sharingan/mangekyo/sasuke/amaterasu` + `summon` +
`cost`): **40 chakra** (`if score chakra matches 40..`), **+10 eye strain** al usar (`cost`: `unless
wearing_eternal add d.eye_strain 10` → el ojo **eterno EMS queda exento**), y coloca el fuego negro **anclado**
donde apunta (`anchored eyes` → summon del armor stand `d.amaterasu` que quema entidades a `distance 0..3`).

**Diseño (patrones existentes):**
- `dojutsu/ability/`: **`EyeAbility`** (interfaz: `chakraCost`/`strainCost`/`execute(EyeAbilityContext)`),
  **`EyeAbilityRegistry`** (id→clase, en código —las habilidades son clases con lógica propia, no datos+familia
  como los jutsus—; mismo patrón que `JutsuEffectRegistry`), **`EyeAbilityService`** (pipeline: `resolveCast`
  puro —ojo activo + `selectedAbility` registrada— + glue: consume chakra todo-o-nada, aplica fatiga
  (`EyeStrainService.addStrain`, **0 si el ojo es eterno**), ejecuta, sincroniza).
- **`AmaterasuAbility`**: 40/10; `caster.pick(20)` (raycast a donde apunta) → **`AbilityAnchor`** (F4/DJT-071,
  **no armor stand** — RA-3) con tick-callback que incendia (`setRemainingFireTicks`) y daña
  (`hurt(indirectMagic)`) a las entidades en radio 0..3; el **daño lo acotan las invuln-frames vanilla** (~cada
  10 t) sin temporizador propio; partículas de llama para verlo. Ajuste fino de daño/duración → balance (F10).
- **Disparo**: keybind **R** → `UseEyeAbilityPayload` (C2S marcador) → `EyeAbilityService.cast` lee la
  `selectedAbility` real (DJT-117) y valida (el cliente no decide, `[ADD/07]`). Anti-spam + `forget` al
  desconectar. Clave de idioma del keybind añadida.
- **Cierra el bucle de F5**: el menú (DJT-117) selecciona → R ejecuta → consume chakra y **sube el strain**
  (DJT-116, que hasta ahora no tenía emisor, como `addAffinityXp` antes de F4).

**Autorrevisión:** `AmaterasuAbility.execute` no valida recursos (lo hace el pipeline). `EyeAbilityService.register`
toca la red → el test prueba `EyeAbilityRegistry` directamente (sin MC). SOLID/Clean: SRP (contrato/registro/
pipeline/habilidad separados), sin duplicación (reusa `AbilityAnchor`, `ChakraService`, `EyeStrainService`,
`NetworkService`, `EyeState.selectedAbility`).

**Validación:** `./gradlew build` multiloader **verde**; **602 tests** (+6: `resolveCast` sin ojo/sin selección/
no registrada/ok, costes de Amaterasu, registro). **Servidor y payload validados en runtime** en NeoForge y Fabric
(arranque limpio, 0 ERROR). El **cast in-game** (con Mangekyō activo, seleccionar Amaterasu en el menú N, pulsar R
→ fuego negro anclado, −40 chakra, +10 strain) exige un **cliente interactivo** no automatizable headless → check
manual. **Siguiente: DJT-119+** (resto de habilidades clonando la plantilla; luego Susanoo y chakra vision).

---

## 2026-07-19 · F5 · Andamiaje: comando de depuración `/dojutsudojutsu` (QA del sistema de ojos)

Comando temporal para probar in-game todo F5 sin esperar las mecánicas time-gated (mismo patrón que
`/dojutsunatures`, `/dojutsuclans`…): `get`, `activate <eye>` (fija ojo activo + despierta su familia →
probar menú **N** y habilidades **R** al instante), `deactivate`, `use <family> <level>` (dispara la evolución),
`strain <0-100>` (probar weakness/blindness/apagado), `eternal <bool>` (EMS: exención de fatiga y ceguera/apagado).
Muta solo por la puerta `PlayerNinjaData.setEye`; ids por nombre. Registrado en `DojutsuMod` (11º comando debug).
`./gradlew build` verde (602 tests); servidor NeoForge arranca con el comando registrado, 0 ERROR. Se retirará
con el resto del andamiaje cuando el sistema de ojos sea plenamente obtenible in-game (despertar real + assets).

---

## 2026-07-19 · F5/DJT-119 (lote 1) — Más habilidades oculares (empuje/atracción)

Primer lote del `#plantilla` DJT-111+: clona la plantilla de Amaterasu (DJT-118) para 3 habilidades más,
cubriendo los efectos **novedosos de manipulación de velocidad** que el diseño destaca, en dos ojos.

**Verificación de costes (regla 16), de `<ability>/cast: d.chakra matches N..`:**
- **Vacuum Palm** (Byakugan): 40 chakra — empuje **dirigido** (en la dirección de apuntado) + daño.
- **Shinra Tensei** (Rinnegan): 600 chakra (`rinne_sharingan/shinra_tensei/cast`) — empuje **AoE** (radial, lejos
  del lanzador) + daño. El datapack empuja con explosiones de `tnt_minecart`; aquí impulso directo (equivalente,
  sin destruir terreno).
- **Bansho Ten'in** (Rinnegan): 360 chakra — **atracción** AoE (hacia el lanzador), distance 0..5 del datapack.
- **strain 0** en las tres: sus `cost.mcfunction` no añaden `eye_strain` (verificado en Bansho/Deva). Amaterasu
  (+10) sigue siendo la excepción.

**Diseño:** helper package-private **`EyeAbilityEffects`** (iteración de objetivos + `impulse` [`setDeltaMovement`
+ `hurtMarked` para que valga sobre **jugadores**, cuyo movimiento controla el cliente] + `damage`), sin duplicar
entre habilidades. Cada habilidad es una clase que implementa `EyeAbility` y se registra en
`EyeAbilityService.register` con su id (== el del `abilities` de la `EyeDefinition` y la `selectedAbility` del
menú). Ajuste de fuerza/daño/alcance → balance (F10).

**Validación:** `./gradlew build` multiloader **verde**; **603 tests** (+1: costes verificados de las 3). Servidor
NeoForge y Fabric arrancan limpios con las habilidades registradas (los 2 ERROR de Log4j de un arranque fueron
un **bloqueo de fichero de log por un proceso residual**, no del mod; el arranque limpio lo confirma). El **cast
in-game** (menú N → seleccionar → R) exige cliente interactivo → check manual (atajo: `/dojutsudojutsu activate
rinnegan` → N → Shinra Tensei → R). **Siguiente: más lotes de habilidades; luego Susanoo (DJT-12x) y chakra
vision (DJT-12x) cierran F5.**

---

## 2026-07-19 · F5/DJT-119 (lote 2) — Habilidades del Ketsuryugan (efectos de estado)

Segundo lote del `#plantilla`: 3 habilidades del Ketsuryugan, probando que la plantilla escala a **efectos de
estado** (potion effects) además de la manipulación de velocidad del lote 1. **Costes Y strain verificados**
(`<ability>/cost`):
- **Blood Eruption** (120 chakra / +8 strain): daño mágico (`MobEffects.HARM` amp 1) a la entidad **más cercana**
  (radio 10; `sort=nearest limit=1` del datapack).
- **Iron Blood Fist** (80 / +4): **auto-buff** de Fuerza II (`DAMAGE_BOOST`), la habilidad de tipo buff del lote.
- **Rising Blood Spear** (100 / +14): daño mágico **en área** (HARM amp 0) a donde apunta el lanzador.

**Diseño:** `MobEffects.HARM` (instant_damage) **ya invierte** su signo para los no-muertos en vanilla (los cura),
reproduciendo el `if type=#undead run instant_health` del datapack **sin ramas**. Helper `nearestLiving` añadido a
`EyeAbilityEffects`. **8 habilidades** implementadas en total. Ajuste de daño/duración → balance (F10).

**Validación:** `./gradlew build` multiloader **verde**; **604 tests** (+1: costes y strain de las 3). Servidor
NeoForge y Fabric arrancan limpios con las habilidades registradas.

**Incidente de entorno (no del código):** un arranque de Fabric crasheó con "Failed to initialize server" /
"another process has locked a portion of the file". Causa: **procesos JVM de servidores de run zombie** de
arranques solapados de esta sesión retenían `fabric/run/world/session.lock` y los logs. El filtro de kill buscaba
`neoforge\run`/`fabric\run` en el CommandLine, pero el JVM real del servidor se identifica por
`-Darchitectury.main.class` / `--add-opens=java.base/java.util`. Tras matar los 6 zombies + borrar el
`session.lock`, Fabric arrancó limpio (0 ERROR). NeoForge arrancaba bien todo el tiempo con el mismo binario =
confirmación de que era ambiental. **Regla:** un crash "Failed to initialize server" SIN clases del mod en el
stack es ambiental (lock/mundo), no del código.

---

## 2026-07-20 · F5/DJT-119 (lote 3) — Habilidades: genjutsu, teleport, melé AoE

Tercer lote del `#plantilla`: 3 habilidades en 3 ojos, cubriendo **tipos de efecto nuevos**. Costes/strain
verificados (`<ability>/cost` / `cast`):
- **Tsukuyomi** (Mangekyō, 100 chakra / **+30 eye strain** — el más caro, exento con ojo eterno): genjutsu de
  ilusión al más cercano — `slowness`(inmoviliza)+`blindness`+`mining_fatigue`+`HARM` (fiel a
  `.../tsukuyomi/*`).
- **Amenotejikara** (Rinnegan, 600 / 0): **teleport/intercambio** — si apunta a una entidad, swap de posición;
  si no, blink al punto de mira (`teleportTo`).
- **Eight Trigrams 64 Palms** (Byakugan, 60 / 0): golpe **melé AoE** — `HARM` amp 1 a las entidades en radio 5
  + auto-`slowness` (la postura enraizada del datapack).

**Diseño:** las 3 clonan la plantilla `EyeAbility`+`EyeAbilityService`. Confirma el **patrón de strain** (Mangekyō
y Ketsuryugan añaden fatiga; Byakugan y Rinnegan no). **10 habilidades** implementadas: la plantilla ya cubre
fuego anclado, empuje/atracción/dirigido, daño mágico single/AoE, auto-buff, genjutsu multi-debuff y teleport.

**Nota (deuda del pipeline):** `cast` consume chakra/strain **antes** de `execute`; una habilidad de objetivo
único sin objetivo gasta igual el coste. Un refund "Nothing nearby" (como `canApply`/`NO_TARGET` de los jutsus,
DJT-074) es una mejora futura del pipeline de habilidades.

**Validación:** `./gradlew build` multiloader **verde**; **605 tests** (+1: costes y strain de las 3). NeoForge y
Fabric arrancan limpios (0 ERROR) — esta vez sin el incidente de lock (procesos de run detenidos con el filtro
correcto `architectury.main.class`/`--add-opens`). **Siguiente: más lotes; luego Susanoo (DJT-12x) y chakra
vision (DJT-12x) cierran F5.**

---

## 2026-07-20 · F5/DJT-119 (lote 4) — Habilidades: explosión, melé fuerte, gravedad vertical

Cuarto lote del `#plantilla`: 3 habilidades más, costes/strain verificados (`<ability>/cost`|`cast`):
- **Exploding Human** (Ketsuryugan, 200 chakra / **+20 strain**): daño mágico devastador (`HARM` amp 2 = Harming
  III) al más cercano + partícula de explosión. El datapack marca hasta 3 y detona; aquí detonación inmediata.
- **Eight Trigrams 128 Palms** (Byakugan, 80 / 0): versión superior de las 64 Palms — `HARM` amp 2 (vs amp 1) a
  las entidades en radio 5 + postura enraizada.
- **Chibaku Tensei** (Rinnegan, 600 / 0): gravedad **vertical** — `LEVITATION` + `HARM` amp 1 en la zona (el
  datapack invoca un ancla a 20 bloques y arrastra hacia ella). Distinto de la atracción horizontal de Bansho.

**13 habilidades** implementadas en total. Confirma de nuevo el patrón de strain (Ketsuryugan sí; Byakugan/
Rinnegan no). Sin novedad de diseño (clones de la plantilla). **Validación:** `./gradlew build` multiloader
**verde**; **606 tests** (+1: costes/strain de las 3). NeoForge y Fabric arrancan limpios (0 ERROR), sin incidente
de lock (servidores detenidos con el filtro `architectury.main.class`/`--add-opens`). **Siguiente: seguir el
roster o pasar a Susanoo/chakra vision (DJT-12x) para cerrar F5.**

## 2026-07-21 · F5/DJT-119 (lote 5) — Habilidades: proyectil, defensa, 1ª del Tenseigan

Quinto lote del `#plantilla`: 3 habilidades más, costes/strain verificados (`<ability>/cost`|`cast`). Este lote
estrena **3 tipos de efecto nuevos** en la plantilla:
- **Asura Path** (Rinnegan, 450 chakra / 0 strain): 1ª habilidad de **proyectil** — lanza un `NinjaProjectile` real
  (el proyectil de F4/DJT-070) con `withImpactCallback`; al golpear un `LivingEntity` le aplica daño mágico
  indirecto y da `resistance` al lanzador (fiel a `asura/cast`, que da resistencia y dispara el misil-armorstand).
- **Preta Path** (Rinnegan, 150 / 0): 1ª habilidad **defensiva** del catálogo — buff de `absorption` (amp 2) +
  `damage_resistance` (amp 1) 200 ticks sobre el lanzador. Reinterpreta la barrera de absorción de chakra ajeno.
- **Truth-Seeking Orbs** (Tenseigan, 300 / 0): 1ª habilidad del **Tenseigan** — `HARM` amp 2 (Harming III) a las
  entidades en radio 5 del punto de mira. Coste 300 = lo que consume `truth_seeking_orb/summon` (el datapack lo
  **gatea** a 600 de disponibilidad; se usa el valor consumido).

**16 habilidades** implementadas en total (la plantilla ya cubre 9+ tipos de efecto: fuego-ancla, empuje/atracción/
dirigido, daño mágico single/AoE, auto-buff, genjutsu, teleport, gravedad, **proyectil**, **defensa**). Patrón de
strain confirmado (Rinnegan/Tenseigan sin fatiga). **Validación:** `./gradlew build` multiloader **verde**;
**607 tests** (+1: costes/strain de las 3). NeoForge y Fabric arrancan limpios (`Ojos cargados: 7`, 0 ERROR reales),
sin incidente de lock (servidores detenidos con el filtro `architectury.main.class`/`--add-opens`). **Siguiente:
seguir el roster o pasar a Susanoo/chakra vision (DJT-12x) para cerrar F5.**

## 2026-07-21 · F5/DJT-119 (lote 6) — Sharingan (stun + copia) y canalización; el pipeline gana cooldown

Sexto lote: **cierra el catálogo declarado** de los 7 ojos salvo `susanoo` y `tenseigan_chakra_mode` (los dos
`toggle`, que van con `ToggleableMode` en DJT-12x).

**Hallazgo que obligó a tocar el contrato:** verificando los costes salió que el `sharingan/cost.mcfunction` es el
goteo **pasivo** del ojo activo, **no** el coste de sus habilidades — `stun/cast` y `copy_jutsu.mcfunction` no
cobran chakra. El datapack las gatea por **cooldown** (`scoreboard players set @s d.sharstun_cd 200`). Así que
`EyeAbility` gana **`cooldownTicks()`** (default 0, el resto del catálogo se autolimita por chakra) y
`EyeAbilityService` una puerta de cooldown por **(jugador, habilidad)**, evaluada **antes** de cobrar chakra y
sembrada tras `execute`; `forget` la limpia al desconectar. Sin ella una habilidad de coste 0 sería spameable.

- **Genjutsu Stun** (Sharingan, 0 chakra / 0 strain / cd 200t): 1ª habilidad del **Sharingan** y primer **stun
  duro** — `slowness` amp **255** (inmovilización total) + `mining_fatigue` amp 3 durante 100 ticks al más cercano
  en radio 10; la `blindness` solo a **no jugadores**, fiel al `unless predicate dojutsu:player/is_player`.
- **Copy Jutsu** (Sharingan, 0 / 0): la más singular del catálogo — no produce efecto de combate, **aprende un
  jutsu**, y es el **único puente entre F5 y F4**. Consume `LastCastRegistry` (F4/DJT-079), que se construyó
  explícitamente como base de esta copia: casts vivos, en la dimensión del observador, radio 20. Exige **mano
  vacía** (fiel a "Hand not empty"). Al copiar hace `withKnown` + entrega el `JutsuItem` (= `give/jutsu/<id>`).
  **Corrige S06-R6**: el `d.sharingan_seen` del datapack no caducaba —el jutsu visto se recordaba para siempre—;
  aquí el registro expira.
- **Kinrin Tensei Baku** (Tenseigan, **400** / 0 — verificado: `cast` exige `matches 400..` y resta 400): 1ª
  **canalizada con auto-raíz** — `slowness`+`jump_boost` amp 255 al lanzar, limpiados al detonar; el orbe avanza
  lento (el datapack va a 0.2 por paso) y aplica HARM III en radio 5.

**19 habilidades** en total. **Validación:** `./gradlew build` multiloader **verde**; **610 tests** (+3: costes/
strain del lote, cooldown por defecto 0, y la puerta pura `isOnCooldown`). NeoForge y Fabric arrancan limpios
(`Ojos cargados: 7`, 0 ERROR reales), sin incidente de lock. **Siguiente: Susanoo y chakra vision (DJT-12x)
cierran F5.**

## 2026-07-21 · F5/DJT-120 — Susanoo: el primer modo activable del proyecto

El Susanoo `[S24]` estrena el framework **`ToggleableMode`** (patrón 2 del ADD), que existía desde F1 **sin un
solo consumidor**. Confirma que su único automatismo —"coste impagable → castigo + apagado"— es exactamente lo
que hace el datapack (`if no_chakra run deactivate`), y deja el montaje que clonarán sage (S08), sellos (S10),
puertas (S11), SOAH (S12) y formas de jinchūriki (S09).

**Lo que dijo el datapack y no era lo esperable:**
- **Sostenerlo es más barato cuanto mayor el nivel:** 6 chakra cada 20t en Ribcage frente a 1 cada 80t en
  Perfect; la fatiga baja igual (3→1). Lo que sube con el nivel es la protección (`resistance` y
  `fire_resistance` con amplificador `nivel-1`). Es la maestría del portador, no un error de lectura.
- **`slowness` solo en los niveles 3 y 4** (Armored y Humanoid): las etapas que lastran.
- **El EMS paga tarifa plana:** con `wearing_eternal` se salta *todo* el bloque de coste por nivel y aplica una
  sola línea — 0 chakra y +1 de fatiga cada 160t, sea cual sea el nivel.
- **El cobro es por pulsos** (`d.susanoo_time`), no por tick: se decrementa un temporizador y solo se cobra al
  vencer, con el intervalo del nivel.
- **Los niveles 4, 5 y 6 son inalcanzables** en el datapack: el uso solo se acumula `unless susanoo_lvl_3p`, así
  que sus `level/set_N`, costes y efectos son código muerto. Se trata como **defecto de S24** (regla 16) y aquí
  la maestría sube hasta 6 — que es lo que la propia tabla del datapack describe.

**Montaje:** `SusanooCostTable`+`SusanooTier` (dominio puro, tabla verificada) · `SusanooMode` (el ciclo:
`onActivate` veta si falta el chakra mínimo, `onTick` aplica el aura, `onCost` cobra por pulsos, y el framework
apaga si no se paga) · `SusanooContext` (costura estrecha: el modo nunca ve Minecraft, así que **el ciclo entero
se testea sin arrancar MC**) · `SusanooService` (glue: contexto sobre `ServerPlayer`, tick en
`TickPhase.DOJUTSU`, maestría por uso con XP y mensaje de subida) · `SusanooState` (**sub-estado propio** de
`PlayerNinjaData` —S24 no es S05—, persiste solo nivel+uso).

**Dos decisiones de encaje:** (1) **sin tecla nueva** — `susanoo` ya es una habilidad declarada del Mangekyō, así
que `SusanooAbility` lo enciende desde el menú ocular + R, con coste 0 (el chakra mínimo de 5 es **puerta**, la
veta `onActivate`; el coste real es por pulso) y el cooldown 14t del `activate`. (2) El **encendido no se
guarda**: arranca apagado en cada sesión, como `check_deactivate` lo apaga al perder el ojo; lo que sería injusto
perder al reconectar es el nivel, y eso sí persiste.

**Validación:** `./gradlew build` multiloader **verde**; **636 tests** (+26: ciclo completo del modo con contexto
falso —veto, aura, pulsos, caída por chakra agotado, EMS, alternancia—, tabla verificada nivel a nivel, y
normalización del sub-estado). NeoForge y Fabric arrancan limpios (`Ojos cargados: 7`, 0 ERROR reales).
**Siguiente: el avatar del Susanoo (DJT-121) y la chakra vision (DJT-125) cierran F5.**

## 2026-07-21 · F5/DJT-121 — Avatar del Susanoo y escalera de etapas (+ QA de niveles)

**Antes, el subcomando de QA** que faltaba para validar DJT-120 en el juego: `/dojutsudojutsu susanoo level <1-6>`
(recorre la tabla entera sin esperar los 32000 ticks de uso por nivel) y `susanoo use <n>` (para ver la subida de
nivel al instante); el `get` ahora muestra también nivel, uso y si está activo.

**El avatar es un `AbilityAnchor`, no un tipo de entidad nuevo.** El ancla de F4/DJT-071 ya se diseñó como "base
de las habilidades ancladas de F5", y su contrato contempla literalmente este caso: *"un sistema que necesite un
ancla mientras el modo esté activo la descarta él mismo al desactivar"*. **Corrige RA-3**: el datapack usaba un
`armor_stand` tageado `d.narr` con un `knowledge_book{CustomModelData:<personaje><etapa>}` en la cabeza, que
quedaba colgado si su limpieza no corría. Aquí la caducidad es por construcción y el servicio lo descarta en los
tres caminos: al apagar, al **caer por chakra agotado** (el apagado no pasa por `deactivate`) y al desconectar.

**Etapa ≠ nivel** — es la separación del propio datapack: `set_model` mira `susanoo_stg` y `effects` mira
`susanoo_lvl`. Aquí igual: la **etapa** decide el tamaño de la silueta (crece de 2,5 a 8 bloques), el **nivel**
decide coste y protección. La etapa es estado de ejecución; la maestría persiste.

**La escalera** (`advance.mcfunction`): cada pulsación hace crecer una etapa con tope en el nivel dominado.
**Divergencia consciente (regla 16):** al llegar al tope el datapack no hacía nada —se apagaba desde el menú o al
perder el ojo— y aquí la siguiente pulsación **lo apaga**, porque con una sola tecla una escalera sin salida
dejaría al jugador sin forma de bajarse. La decisión (`SusanooService.decide`) se extrajo **pura** y está testeada.

**Alcance honesto:** esto entrega la **presencia** del avatar (entidad, seguimiento, silueta creciente, despawn
garantizado), **no sus modelos 3D**. Los siete modelos por personaje (`CustomModelData` 1000..7000 + etapa) son
**trabajo de assets** aún sin portar, como en su día lo fueron los de los ítems de jutsu; hasta entonces la
silueta se dibuja con partículas, que es como se representan el resto de entidades del mod (ninguna tiene
renderer todavía).

**Validación:** `./gradlew build` multiloader **verde**; **641 tests** (+5: la escalera pura — manifestar,
crecer, apagar en el tope, y el estado imposible etapa>nivel que no debe dejarla sin salida). NeoForge y Fabric
arrancan limpios (`Ojos cargados: 7`, 0 ERROR reales; los `Unable to delete latest.log` son rotación de logs).
**Siguiente: la chakra vision (DJT-125) cierra F5.**

## 2026-07-21 · F5/DJT-125 — Chakra vision (glow nativo por relación) — CIERRA LA LÓGICA DE F5

Última pieza de F5. La chakra vision resalta las entidades cercanas (radio 30) coloreadas por su relación con el
observador. El reto era **RB-3**: hacerlo sin el `execute at @e[type=#tag] run particle` por-tick-y-por-categoría
del datapack.

**Solución: glow nativo + equipos de scoreboard.** Un `PlayerTickSystem` (fase `DOJUTSU`) reescanea **cada 10
ticks** (no cada tick) el entorno de cada jugador con vision y marca las entidades con el efecto vanilla
`GLOWING` + las mete en un **equipo** cuyo color pinta el contorno. El cliente dibuja el contorno por su cuenta —
así que **no hace falta `ChakraVisionRenderer`** (que el spec sugería): el contorno nativo de vanilla es
exactamente lo que RB-3 quería y sale gratis en ambos loaders, sin código de render del mod. Un barrido retira
del equipo lo que ya nadie renueva; el glow caduca solo (TTL 30t). Divergencia asumida: el glow es **global** (lo
ven todos), no `force @s` como las partículas del datapack — el precio de no usar selector por tick.

**Clasificación estática** (`ChakraVisionRelations`, dominio puro): traducción directa de los 4
`tags/entity_types/chakra_highlights/*` (special naranja / friendly verde / neutral amarillo / hostile rojo). Un
tipo no listado no se resalta, como el datapack.

**Disponibilidad data-driven** (nuevo en `EyeDefinition`: `chakraVision` + `chakraVisionUseLevel`):
byakugan/mangekyo/rinnegan/tenseigan la dan siempre; el **Sharingan solo maduro** — 3 tomoe, uso **≥216000**
(verificado: `sharingan_use_level_3` = 216000..); Ketsuryugan **no**. Así "Sharingan base sin vision" sale de un
número real, no inventado (regla 15).

**Toggle:** nueva tecla **C** → `ToggleVisionPayload` (C2S) → invierte `VisionState.chakraVisionOff`, que
**persiste** (fiel a `d.no_cv`). La vision está **encendida por defecto** con un ojo que la da; el servidor decide
si aplica (el cliente solo manda la intención). `VisionState` entra como sub-estado de `PlayerNinjaData`.

**Validación:** `./gradlew build` multiloader **verde**; **651 tests** (+10: clasificación por categoría, tipos no
etiquetados, la puerta pura `shouldHighlight`, y `grantsChakraVision` sobre los JSON reales —Sharingan maduro sí /
base no, Ketsuryugan no—). NeoForge y Fabric arrancan limpios (`Ojos cargados: 7`, 0 ERROR reales).

**F5 (Poderes oculares) queda COMPLETADA en lógica** (DJT-113…125). Lo único pendiente son los **modelos 3D del
Susanoo por personaje** (trabajo de assets). Próximo: cerrar F5 (handoff) y abrir **F6 (modos de potenciación:
sage + sellos + puertas + SOAH)**, que reutiliza el framework `ToggleableMode` que el Susanoo estrenó.

## 2026-07-21 · F5 (cierre de flecos) 1/N — `tenseigan_chakra_mode` (última habilidad declarada)

Antes de F6, el usuario pidió cerrar los flecos de F5. Triaje: **código** (tenseigan_chakra_mode, refund "sin
objetivo", avisos de fatiga en chat, EMS/eternal manual) vs **fuera de alcance** (modelos 3D del Susanoo = assets;
"colisión defensiva" = el datapack usa el efecto `resistance`, verificado en `effects.mcfunction`, **no** una
hitbox → ya implementado fielmente, añadirla sería inventar).

**Fleco 1: `tenseigan_chakra_mode`** — la única habilidad declarada (en `tenseigan.json`) que faltaba. Es un
**modo toggle**, así que es el **2º consumidor de `ToggleableMode`** tras el Susanoo. Verificado contra
`tenseigan/chakra_mode/main.mcfunction`: mientras se sostiene da **vuelo** + buffs (Fuerza II, Velocidad II, Salto
IV, Resistencia II), cuesta **10 chakra cada 200 ticks** (`tenseicm_cc`), y **cae si el chakra baja de 19**
(`unless chakra matches 19..`). Sin fatiga (Tenseigan). Piezas: `domain/tenseigan/{TenseiganChakraMode,
TenseiganChakraModeContext}` (dominio puro → ciclo testeable sin MC) + `dojutsu/tenseigan/…Service` (glue: tick en
`DOJUTSU`, gestión de vuelo con guarda de creativo/espectador) + `TenseiganChakraModeAbility` (toggle desde el
menú + R, sin tecla nueva). El encendido es de ejecución; el vuelo se retira al apagar, al caer por chakra y al
desconectar. **El catálogo declarado de habilidades oculares queda COMPLETO: 21** (DJT-119+ pasa de 🔄 a ✅).

Anotado para **F6/EP-TOGGLE-FRAMEWORK**: es el 2º servicio que replica el bucle "tick el modo → si cae, limpia";
ahí se extraerá un arnés compartido para sage/sellos/puertas/SOAH.

**Validación:** `./gradlew build` multiloader **verde**; **658 tests** (+7: ciclo del modo con contexto falso —veto,
aura+vuelo, pulso, caída por umbral, alternancia—). NeoForge y Fabric arrancan limpios (`Ojos cargados: 7`, 0 ERROR
reales). **Siguiente fleco: refund "sin objetivo" en el pipeline de habilidades oculares.**

## 2026-07-21 · F5 (cierre de flecos) 2/N — Refund "sin objetivo" (no_cast_nby)

**Fleco 2:** el pipeline ocular cobraba chakra/fatiga **antes** de `execute`, así que una habilidad de objetivo
único sin blanco malgastaba el coste. El datapack refunda con `no_cast_nby` → *"Nothing nearby"* (comprueba el
objetivo **antes** de correr `cost`).

**Verificación (regla 16):** busqué `no_cast_nby`/"Nothing nearby" en todo `functions/dojutsu`. De **nuestro**
catálogo lo llevan **solo tres**: `ketsuryugan/blood_eruption`, `ketsuryugan/exploding_human` y `sharingan/stun`
(= genjutsu_stun). **Tsukuyomi NO lo lleva** (aunque sea caro y de objetivo único) — así que, fiel al datapack,
**no** se le añade refund.

**Diseño (refleja el `canApply`/`NO_TARGET` del pipeline de jutsus, DJT-074):** `EyeAbility` gana
`default boolean canApply(ctx) { return true; }` — precondición de "hay algo que hacer". El pipeline la evalúa
**tras el cooldown y antes de cobrar**; si es `false`, muestra "Nothing nearby" (actionbar) y aborta **sin
consumir** chakra ni fatiga. Las tres habilidades la overridean con "hay una entidad en rango". Para que
`canApply` y `execute` no diverjan, extraje en cada una un `target(ctx)` privado que ambas usan (misma búsqueda,
mismo centro/radio 10). Los AoE, auto-buff, proyectil y punto-de-mira se quedan con el default `true` (siempre
aplican).

**Simplificación anotada:** el datapack pone un cooldown corto (10t) al fallar; aquí no se pone cooldown en el
fallo (el rate-limit de 200 ms ya evita el spam de reintento). No afecta al gameplay.

**Validación:** `./gradlew build` multiloader **verde**; **659 tests** (+1 de contrato: `canApply` default `true`;
el refund real de las tres es glue con MC y se valida en runtime). NeoForge y Fabric arrancan limpios
(`Ojos cargados: 7`, 0 ERROR reales). **Siguiente fleco: avisos de fatiga en chat (umbrales 25/50/75/90/99).**

## 2026-07-21 · F5 (cierre de flecos) 3/N — Avisos de fatiga en chat (esw_*)

**Fleco 3:** el `EyeStrainService` (DJT-116) dejó los avisos de fatiga como "presentación diferida" (lo dice su
propio javadoc). Este fleco los implementa, fiel a `eye_strain/main.mcfunction`.

**Mecánica verificada:** al entrar en una banda de fatiga se muestra `Eye Strain: N%` **una sola vez** (anti-spam
con los flags `esw_25/50/75/90/99`), con un **color por banda** y un `player.hurt` tenue (vol 0.1, pitch 0.65).
Bandas y colores (de los predicates `stage_1..4`/`99`): **25-49 verde, 50-74 amarillo, 75-89 oro, 90-98 rojo, 99
rojo oscuro**. El flag de una banda se limpia al **caer por debajo de su suelo** (así, subir otra vez re-avisa), y
todos se limpian **sin ojo activo** (`no_dojutsu`). A 100 no hay banda: el **force_deactivate** avisa aparte.

**Diseño:** decisión pura `strainWarningFloor(strain)` → suelo de la banda (0 = sin banda), testeable sin MC. El
glue `warnStrain(player, strain, eyeActive)` lleva el set "ya avisado" por jugador (**ejecución, no guardado**:
se olvida al desconectar vía `forgetWarnings`, divergencia menor —el datapack persiste los tags `esw_*`, aquí
reconectar puede re-avisar una vez—). Se llama tras `applyEffects`, con la fatiga ya regenerada del tick.
**Bonus:** el mensaje de force_deactivate ahora muestra la **2ª línea** del datapack `(Your Eye Strain is at N%)`,
que faltaba.

**Validación:** `./gradlew build` multiloader **verde**; **661 tests** (+2: los cortes de banda 24→99 y que 100 no
tiene banda). NeoForge y Fabric arrancan limpios (`Ojos cargados: 7`, 0 ERROR reales). **Último fleco: EMS/eternal
manual (volver eterno el Mangekyō, `ems_req` = uso ≥864000).**

## 2026-07-21 · F5 (cierre de flecos) 4/4 — EMS/eternal manual — CIERRA TODOS LOS FLECOS DE F5

**Fleco 4 (último):** el EMS (Eternal Mangekyo Sharingan) era una **acción manual** del datapack que no se podía
hacer aún. En el datapack se dispara con las **herramientas quirúrgicas** (`items/surgical_tools/implant_ems` →
`eternal/awaken` → `try_give`); aquí es una **acción del menú ocular**, server-side y validada.

**Requisitos verificados (regla 16):** ojo activo **eternalizable** (`eternalRequirements` no vacío = el Mangekyō),
no eterno ya, **clan** `otsutsuki_uchiha`, **uso ≥864000** (`ems_req`) y **Medical Ninjutsu ≥150** (`d.medical`, la
puerta de las herramientas). **Bonos** (`try_give`): +60 max_chakra, +150 ninja_xp, +78000 susanoo_use.

**El matiz de modelado que expuso el fleco:** `ems_req` es sobre `d.sharingan_use` (contador **continuo** del
linaje). En nuestro modelo colapsado, el Mangekyō es familia propia y —al ser terminal sin aristas— **no
acumulaba uso**, así que el 864000 era inalcanzable. Solución: puse `use_level: 864000` en las
`eternalRequirements` del Mangekyō y extendí `EvolutionService` para **acumular uso también en los ojos con un
uso-gate eterno** (`hasUseGatedEternal`). Divergencia consciente documentada: el uso es de la familia Mangekyō
(cuenta desde que se alcanza el Mangekyō), no el `sharingan_use` continuo — el número es fiel, el origen del
conteo difiere.

**Montaje:** `EternalAwakeningService` (decisión pura `evaluate` → `AwakenOutcome` con las 6 salidas, testeable
sin MC; glue `tryAwaken` que aplica bonos y avisa) · `AwakenEternalPayload` (C2S) · botón **"Awaken Eternal
Mangekyo"** en el menú ocular (`AbilitySelectScreen`), que se muestra con cualquier ojo activo y lo **valida el
servidor** (mensaje si no procede) · `SusanooState.plusUse(N)` (puro) para el bono de +78000 aplicando las subidas
de nivel. El flag `EyeState.eternal` (que ya eximía de fatiga en habilidades y Susanoo) por fin se **enciende
por acción del jugador**.

**Validación:** `./gradlew build` multiloader **verde** (incluye el cliente del botón nuevo); **673 tests** (+12:
las 6 salidas de `evaluate`, `plusUse` con subidas de nivel/tope, y que el Mangekyō eternalizable acumula uso).
NeoForge y Fabric arrancan limpios (`Ojos cargados: 7`, 0 ERROR reales).

**TODOS LOS FLECOS DE F5 CERRADOS** (tenseigan_chakra_mode, refund "sin objetivo", avisos de fatiga, EMS manual).
Único pendiente de F5: los **modelos 3D del Susanoo** (assets). **Listo para F6** (sage + sellos + puertas + SOAH),
que reutiliza `ToggleableMode` — ya con 2 consumidores (Susanoo, Tenseigan Chakra Mode) y el arnés a extraer en
`EP-TOGGLE-FRAMEWORK`.

## 2026-07-21 · Roster de ojos — lote 1: Mangekyō por personaje (variante aleatoria)

A petición del usuario, antes de F6 se abre el **roster de ojos diferido** (los per-character del datapack que
DJT-113 dejó para "tickets posteriores"). Es un **épico multi-lote**; este es el lote 1: el **framework de
variante aleatoria + los 8 Mangekyō por personaje** como nodos data-driven.

**El datapack elige el Mangekyō al azar** al despertarlo (`mangekyo_rng`, una loot table → obito/itachi/sasuke/
indra/madara/shisui/fugaku/shin). Se reproduce con un **pool de variantes** en la arista de evolución: `EyeEvolution`
gana `variants` (opcional), y `EvolutionService.evaluate` —ahora con `RandomGenerator` inyectado, como el 50/50 de
`AwakeningService`— elige una variante **al azar entre las que existen** en el registro. `sharingan.json` declara
las 8 variantes en su arista al Mangekyō.

**8 eye JSONs** (7 nuevos + itachi), familia mangekyo, chakra vision, `eternalRequirements` (EMS) clonados. Rosters
verificados (regla 16, de cada `<char>/main.mcfunction` + todos con Susanoo por `get_id`): itachi
(amaterasu/tsukuyomi/susanoo ✅), obito (kamui), sasuke (amaterasu/flame_control), madara (tengai_shinsei), indra
(hinokagutsuchi), shisui (kotoamatsukami), fugaku (tsukuyomi ✅), shin (chakra_blades/phase_shift) — todos +susanoo.

**Alcance honesto:** las habilidades **compartidas** (amaterasu/tsukuyomi/susanoo) funcionan ya, así que fugaku e
itachi son jugables completos y sasuke casi; las **técnicas únicas** (kamui, tengai_shinsei, hinokagutsuchi,
kotoamatsukami, flame_control, chakra_blades, phase_shift) quedan **declaradas data-only** (aparecen en el menú, se
implementan en lotes 2+, mismo patrón que DJT-117/119). El **EMS ya funciona para todas** (reutiliza
`EternalAwakeningService`, cualquier ojo eternalizable). La variante te toca al azar al evolucionar el Sharingan.

**Validación:** `./gradlew build` multiloader **verde**; **675 tests** (+2: el pool elige variantes distintas según
la semilla y solo entre las que cargan). NeoForge y Fabric arrancan limpios con **`Ojos cargados: 14`** (7→14).
**Siguiente: lote 2 del roster** — implementar la primera tanda de técnicas únicas (p. ej. Kamui de Obito).

## 2026-07-21 · Roster de ojos — lote 2: Kamui (Mangekyō de Obito)

Primera técnica única del roster. **Kamui** = jutsu espacio-tiempo de Obito: **marca un punto y luego te
teletransporta a él**. El datapack lo parte en dos slots ("Kamui Set Location" + "Kamui"); aquí es **una sola
habilidad que alterna**: el primer R marca tu posición (gratis), el siguiente te teletransporta a ella.

**Costes verificados** (`obito/kamui_tp`): el teletransporte cuesta **20 chakra**, **+2 eye_strain** (exento con
ojo eterno, `unless wearing_eternal`), **cooldown 300 ticks** (`mangekyo_time 300`); marcar es gratis. Sonido de
enderman al marcar y al volver, soporta cambio de dimensión.

**Auto-gestiona su coste** (como `CopyJutsuAbility`): declara 0/0 al pipeline porque las dos fases tienen costes
distintos, y cobra el teletransporte dentro de `execute` (chakra + fatiga + su propio cooldown). El punto marcado
y el cooldown son estado de ejecución (`KamuiAbility.forget` al desconectar).

**Completado con "Kamui (Others)"** (el usuario recordó que el Kamui de Obito tenía **dos** teletransportes —a uno
mismo y a otras entidades—; correcto, estaba anotado como pendiente y ahora se implementa). `KamuiOthersAbility`
(2º slot del datapack): manda a las **entidades vivas en radio 6** al punto marcado — el uso ofensivo (desterrar
enemigos a tu marca). Costes verificados (`kamui_others`): **20 chakra**, **+3 eye_strain** (una más que el
propio) y el **cooldown 300t compartido**. El estado común (punto + cooldown) se factorizó a **`KamuiWaypoints`**,
que usan ambas habilidades. Reubicación en la **misma dimensión** (como el `tp @e ... @s` del datapack); si la
marca está en otra dimensión, avisa y no reubica. Obito declara ahora `kamui`, `kamui_others`, `susanoo`.

**Validación:** `./gradlew build` multiloader **verde**; **676 tests** (contrato 0/0 de ambas Kamui; el
teletransporte es glue con MC → runtime). NeoForge y Fabric arrancan limpios (`Ojos cargados: 14`, 0 ERROR
reales). Con esto **Obito es un Mangekyō jugable completo** (Kamui + Kamui Others + Susanoo). **Siguiente: lote 3**
— otra técnica única (Tengai Shinsei de Madara, Hinokagutsuchi de Indra, o el control de llama de Sasuke).

## 2026-07-21 · Roster de ojos — lote 3: Tengai Shinsei (Mangekyō de Madara)

La técnica más destructiva del roster: un **meteorito** que cae del cielo sobre el punto de mira y **explota**.
A diferencia de Kamui (dos fases, auto-gestión), es una habilidad de **un disparo** → usa el **pipeline estándar**:
`chakraCost=200`, `strainCost=13`, `cooldownTicks=200` (todo verificado en `madara/cost`: -200 chakra, +13
eye_strain exento con eterno, `mangekyo_time 200`).

**Mecánica** (fiel a `tengai_shinsei`→`summon_meteor`→`hit`): raycast al punto de mira (`caster.pick`), se invoca un
`NinjaProjectile` **~30 bloques por encima** que cae con gravedad (estela de humo/fuego vía `withFlightTick`), y al
impactar provoca una **explosión con fuego y destrucción de bloques** (`level.explode`, interacción `MOB` que
respeta `mobGriefing`) — el equivalente moderno de los `tnt_minecart` + `fill ... air destroy` del datapack.
Truenos al lanzarlo.

**Validación:** `./gradlew build` multiloader **verde**; **677 tests** (+1: costes 200/+13/cd200 verificados; la
caída/explosión es glue con MC → runtime). NeoForge y Fabric arrancan limpios (`Ojos cargados: 14`, 0 ERROR
reales). **Madara ya es jugable** (Tengai Shinsei + Susanoo). **Siguiente: lote 4** — Hinokagutsuchi de Indra,
control de llama de Sasuke, Kotoamatsukami de Shisui, o blades/phase_shift de Shin.

## 2026-07-21 · Roster de ojos — lote 4: Hinokagutsuchi (Mangekyō de Indra)

Elegido por aportar un **tipo de efecto nuevo** (el de Sasuke sería una variación sobre Amaterasu, que ya existe).
Hinokagutsuchi es la **llama negra viviente**: una esfera de fuego que avanza despacio dejando incendio a su paso.

**Primer proyectil con aura persistente** del catálogo: los anteriores (`AsuraPath`, `KinrinTenseiBaku`,
`TengaiShinsei`) solo actuaban al impactar; este castiga **cada tick de vuelo** vía `withFlightTick`. Fiel al bucle
`run` del datapack: en radio **8** prende a las entidades (`Fire 400` → `setRemainingFireTicks`) y les aplica
`instant_damage` amp 1 (`MobEffects.HARM`, que cura no-muertos), y **prende el bloque de aire** que sobrevuela
(`setblock ~ ~ ~ fire`, comprobando `canSurvive`). Al impactar, detona con fuego (los `tnt_minecart` del datapack).

**Costes verificados** (`indra/hinokagutsuchi`): **20 chakra**, **+5 eye_strain** (exento con eterno), **cooldown
200t**. Fiel también el detalle de que el lanzador recibe `resistance` amp **255** un instante — queda a salvo de
su propia llama mientras la conduce. Partículas de humo denso + soul fire para la "llama negra".

**Validación:** `./gradlew build` multiloader **verde**; **678 tests** (+1: costes 20/+5/cd200 verificados; el
vuelo/quemado es glue con MC → runtime). NeoForge y Fabric arrancan limpios (`Ojos cargados: 14`, 0 ERROR reales).
**Indra ya es jugable** (Hinokagutsuchi + Susanoo). **Siguiente: lote 5** — control de llama de Sasuke,
Kotoamatsukami de Shisui, o blades/phase_shift de Shin.

## 2026-07-21 · Roster de ojos — lote 5: Kotoamatsukami (Mangekyō de Shisui)

El **genjutsu supremo**. Verificando el datapack salió que Shisui tiene **dos** técnicas (`afterglow` =
Kotoamatsukami y `beams_of_light`); esta entrega la primera.

**El genjutsu más largo del catálogo** (500 ticks = 25 s, frente a los 100 de Tsukuyomi) y el primero que
**manipula la orientación** de la víctima: fiel a `run_afterglow`, cada **50 ticks** le da media vuelta
(`tp @s ~ ~ ~ ~180 ~`) — el "control mental" del datapack, que hace perder el norte a quien lo sufre. Mientras
dura: `slowness` amp 2, `blindness` amp 1, `mining_fatigue` amp 1 y partículas de tinta.

**Costes verificados** (`shisui/afterglow`): **50 chakra**, **+15 eye_strain** (exento con eterno), **cooldown
500 ticks** — el más largo del roster, acorde a su poder. El datapack lo gatea con **`no_cast_nby`** (radio 10), así
que implementa `canApply` y **no cobra sin objetivo** (reutiliza el refund del fleco 2).

**Montaje:** el efecto persistente lo lleva un **`AbilityAnchor`** (F4/DJT-071, caducidad por construcción) que
**sigue a la víctima** cada tick, refresca los debuffs y la gira; si la víctima muere o cambia de mundo, el ancla
se descarta (nada colgado). Para girar a un **jugador** hace falta empujarle la rotación por red (`teleportTo` con
el yaw nuevo), porque el cliente manda su propia cámara.

**Validación:** `./gradlew build` multiloader **verde**; **679 tests** (+1: costes 50/+15/cd500 verificados; el
genjutsu por tick es glue con MC → runtime). NeoForge y Fabric arrancan limpios (`Ojos cargados: 14`, 0 ERROR
reales). **Shisui ya es jugable** (Kotoamatsukami + Susanoo) — **6 de 8 Mangekyō jugables**. **Siguiente: lote 6**
— `flame_control` de Sasuke, `blades`/`phase_shift` de Shin, o `beams_of_light` (2ª de Shisui).

## 2026-07-21 · Roster de ojos — lote 6: las dos técnicas de Shin

Shin es el único personaje con **dos técnicas propias** implementables de golpe; ambas verificadas:

- **Chakra Blades** (`blades/cast`): **20 chakra / +6 strain / cd 60t** — el cooldown **más corto del roster**,
  una habilidad de **hostigamiento rápido** frente a los ultimates. Reutiliza el esqueleto de proyectil con aura
  de `Hinokagutsuchi` (`withFlightTick`), pero **corta en vez de quemar**: `instant_damage` amp 0 en radio 5 cada
  tick de vuelo, sin fuego ni detonación. Destellos metálicos (el datapack usa `particle item netherite_ingot`).
- **Phase Shift** (`teleport/cast`): **100 chakra / +8 strain / cd 200t** — **teleporte "guiado"**, un tipo nuevo:
  a diferencia del blink instantáneo de Amenotejikara o del retorno a marca de Kamui, aquí vuela un **marcador**
  durante 10 ticks y el lanzador aparece **donde haya llegado** (si choca antes, el salto es más corto). Al
  reaparecer, `slow_falling` para no morir de caída, fiel a `teleport/main`.

**Detalle técnico:** `NinjaProjectile` **no tiene callback de expiración** (solo lo tiene `AbilityAnchor`; al
agotar vida se descarta sin avisar), así que Phase Shift dispara el salto desde el **último tick de vuelo** vía
`withFlightTick`, con un `AtomicBoolean` que garantiza **un solo salto** lo provoque el fin de vuelo o un choque
previo.

**Validación:** `./gradlew build` multiloader **verde**; **680 tests** (+1: los costes de ambas verificados).
NeoForge y Fabric arrancan limpios (`Ojos cargados: 14`, 0 ERROR reales). **Shin ya es jugable** — **7 de 8
Mangekyō jugables**; solo queda **Sasuke** (`flame_control`). **Siguiente: lote 7** — `flame_control` de Sasuke
(cerraría los 8) o `beams_of_light` (2ª de Shisui).

## 2026-07-21 · Roster de ojos — lote 7: Flame Control (Sasuke) — LOS 8 MANGEKYŌ JUGABLES

Cierra el roster de personajes. **Amaterasu: Flame Control** es lo que define a Sasuke frente a Itachi: no solo
**enciende** la llama negra, sino que puede **apagarla** a voluntad (`sasuke/extinguish`).

**La primera habilidad "de utilidad"** del catálogo: no daña ni desplaza — **limpia**. Un proyectil recorre la
zona apagando, en radio 3: (1) las **entidades ardiendo** (`d.amat_hit` → `setRemainingFireTicks(0)`), (2) las
**anclas de Amaterasu del propio lanzador** (`AbilityAnchor` filtrado por `ownerUuid` — la llama negra persistente
que Sasuke domina) y (3) los **bloques de fuego** del terreno (incluido `soul_fire`).

**Costes verificados** (`extinguish/cost`): **20 chakra / +10 eye_strain** (exento con eterno), **cooldown 100t**.

**Divergencia consciente anotada (regla 16):** en el datapack el coste **solo se cobra si el proyectil acierta**
a algo ardiendo (el `cost` corre desde el `run`, no desde el `cast`); aquí lo cobra el pipeline al lanzar, como
el resto del catálogo. Modelarlo exigiría que el pipeline soportara "cobrar más tarde", que hoy no existe —
queda como posible mejora (emparentada con el refund `canApply` del fleco 2).

**Validación:** `./gradlew build` multiloader **verde**; **681 tests** (+1: costes 20/+10/cd100 verificados).
NeoForge y Fabric arrancan limpios (`Ojos cargados: 14`, 0 ERROR reales).

**LOS 8 MANGEKYŌ POR PERSONAJE SON JUGABLES** (Itachi, Fugaku, Obito, Madara, Indra, Shisui, Shin, Sasuke).
**Pendiente del roster:** `beams_of_light` (2ª técnica de Shisui), el **Blaze KG** que el datapack concede a
Sasuke al despertar, los **assets 3D**, y el resto del roster de ojos (Isshiki, Jougan, Rinne Sharingan,
variantes Rinnegan).

## 2026-07-21 · Roster de ojos — lote 8: cierre de flecos (Beams of Light + Blaze KG)

Cierra los flecos de código del roster de Mangekyō, por decisión del usuario, antes de abrir F6. Los **assets 3D
quedan explícitamente fuera**: se recogen como **ticket futuro al final de las fases** (ver `backlog.md`).

**Beams of Light** (2ª técnica de Shisui, `beams_of_light`): **la habilidad más cara del catálogo** — **80 chakra
/ +20 eye_strain / cooldown 800 ticks (40 s)**, verificado. Rayo **dirigido** (sale hacia el objetivo cercano, no
a donde se mira) que primero lo **clava en el sitio** (`beam_hit`: slowness/jump_boost/mining_fatigue amp **128**)
y luego arrasa en radio 8 durante el vuelo. Lleva `no_cast_nby` → implementa `canApply` (no cobra sin objetivo).

**Blaze KG al despertar** (`natures/give/kg/blaze`): el datapack concede **Blaze Release** al despertar el
Mangekyō de Sasuke. Resuelto **data-driven** (RA-3), no con un `if` por personaje: `EyeDefinition` gana
`awakenKekkeiGenkai` (opcional) y `EvolutionService` lo concede al alcanzar el ojo, delegando en
`KekkeiGenkaiService.grant` (ya idempotente y con aviso por chat). Solo `mangekyo_sasuke.json` lo declara; el
mecanismo queda disponible para cualquier ojo futuro que otorgue un KG.

**Validación:** `./gradlew build` multiloader **verde**; **684 tests** (+3: costes de Beams, Blaze declarado solo
por Sasuke, y Shisui con sus dos técnicas). NeoForge y Fabric arrancan limpios (`Ojos cargados: 14`, 0 ERROR
reales).

**ROSTER DE MANGEKYŌ CERRADO EN CÓDIGO.** Pendiente aparte: **assets 3D** (ticket futuro) y el **resto del roster
de ojos** (Isshiki, Jougan, Rinne Sharingan, variantes Rinnegan), que no bloquean nada. **Siguiente: F6.**

## 2026-07-21 · F6/DJT-130 — Arnés de modos activables + `EmpowermentState` (ABRE F6)

Arranca **F6 (modos de potenciación: sage + sellos malditos + Ocho Puertas + SOAH)** por su ticket base, del que
dependen los otros cuatro.

**`ToggleableModeService` (`EP-TOGGLE-FRAMEWORK`)** — el arnés compartido. **Salda deuda real, no especulativa**:
`SusanooService` (F5/DJT-120) y `TenseiganChakraModeService` (fleco F5) ya repetían el mismo bucle palabra por
palabra ("coge el modo del jugador; si no está activo, no hagas nada; tickea; si dejó de estarlo, limpia y
avisa"). Los cuatro modos de F6 habrían sido del tercero al séptimo. Separa responsabilidades con el framework de
F1: `ToggleableMode` (DJT-012) define el **ciclo**; esto define el **hospedaje** — quién guarda el modo por
jugador, cuándo se tickea, qué pasa al caer, y dónde se emite el `ModeToggledEvent` y el sync.

Deliberadamente **no** sabe de chakra, fatiga ni efectos: eso sigue en el `ModeContext` de cada modo. Solo
contiene lo que era idéntico. Hook `OnFall` para la limpieza propia de cada modo cuando cae por coste impagable
(el avatar del Susanoo, el vuelo del Tenseigan Chakra Mode).

**`EmpowermentState`** — sub-estado de F6 en `PlayerNinjaData`, con campos **verificados contra los scoreboards
del datapack**: sello (`d.cseal_id`, `d.cs`, `d.cseal_deaths`), puertas (`d.eg_mastery`, `d.eg_strain`) y SOAH
(`d.soah_chakra`, `d.soah_gave`). Los 6 sellos confirmados en `functions/cursed_seal/`: heaven, earth, jirobo,
kidomaru, sukon_ukon, tayuya. **Solo persiste lo que sería injusto perder** (mismo criterio que `SusanooState`):
el sello y su nivel, la maestría/fatiga de puertas y la carga del SOAH — el **encendido** de cada modo es estado
de ejecución del arnés, así que reconectar no te deja con las Ocho Puertas abiertas.

**Nota:** migrar los dos servicios de F5 al arnés es refactor de comportamiento neutro; se hará cuando el primer
modo de F6 lo use en producción (así se valida el arnés antes de tocar lo que ya funciona).

**Validación:** `./gradlew build` multiloader **verde**; **692 tests** (+8: normalización de rangos y withers
aislados de los tres subsistemas). NeoForge y Fabric arrancan limpios (`Ojos cargados: 14`, 0 ERROR reales).
**Siguiente: DJT-135** (`SageState` + carga de energía natural por **umbral de velocidad**, no `sneak_time`).

## 2026-07-21 · F6/DJT-135 — Energía natural: carga por quietud (abre EP-SAGE)

Segundo ticket de F6 y primero del modo sabio. **Estarse quieto acumula energía natural y, al llenarla, te
vuelves sabio del linaje que corresponda al bioma.**

**La corrección de diseño (S08-R1), que es el punto del ticket:** el datapack detecta la quietud marcando al
jugador con un tag `d.moved` y contando `d.still_timer` mientras no lo tenga — un mecanismo frágil. Aquí la
quietud se mide **directamente por velocidad**: por debajo de 0.01 bloques/tick, en suelo, sin esprintar ni nadar.
Moverse **reinicia** el acumulado, igual que el original.

**Ritmo verificado** (`properties.mcfunction` + `chakra/main` + `gather_natural_energy`): primero hay que
**asentarse 1200 ticks** (60 s, `still_wait_1200`) — quedarse quieto un instante no basta; a partir de ahí se gana
**+1 de energía** cada `GatherTimeLevel_N` ticks, y el intervalo **se halva por nivel**: 800 (nivel 0, 40 s) → 25
(nivel 5). El **aceite de sapo** lo halva otra vez (400 → 12). Máximo **100** (`max_sagechakra`). Al llenarse:
`become_sage`, +30 ninja_xp y el linaje del bioma.

**Mapa bioma→linaje** (verificado en `become_sage.mcfunction`): pantano→**Sapo**, bosque oscuro→**Babosa**,
fortaleza→**Serpiente**. **Divergencia consciente:** la fortaleza es una *estructura*, no un bioma; se aproxima
por **profundidad** (Y<40) para no acoplarse al generador de estructuras. Anotada.

**Piezas:** `domain/sage/{SageType, NaturalEnergyTable}` (dominio puro, tabla verificada) +
`data/state/SageState` (sub-estado: sabio, linaje, nivel, carga, quietud, uso, aceite) +
`sage/NaturalEnergyChargingService` (`PlayerTickSystem` en `DOJUTSU`, con la decisión `charge()` **pura** y
testeable sin MC). **El encendido del modo no vive en el estado** (mismo criterio que Susanoo/Empowerment): lo
llevará el arnés en DJT-136, así que reconectar no te deja en modo sabio — pero **haberte vuelto sabio sí
persiste**.

**Nota de modelado:** la energía natural es **carga, no un `Resource` gastable con regen** — el datapack la trata
como contador simple (`sagechakra += 1`). Es la lectura fiel; si DJT-136 necesitara gastarla, se replantea.

**Validación:** `./gradlew build` multiloader **verde**; **704 tests** (+12: espera previa, pulsos por nivel,
aceite, interrupción al moverse, tope, y las dos tablas verificadas valor a valor). NeoForge y Fabric arrancan
limpios (`Ojos cargados: 14`, 0 ERROR reales). **Siguiente: DJT-136** (`SageMode` sobre el arnés, niveles 1-5,
técnicas por tipo; hook de Seis Caminos → F9).

## 2026-07-21 · F6/DJT-136 — Modo sabio + comando de QA del sage

Cierra EP-SAGE. **Primer consumidor real del arnés `ToggleableModeService`** (DJT-130) — el arnés lleva el
hospedaje (modo por jugador, tick, caída, evento y sync) y aquí solo queda lo propio del sage: contexto, subida
de nivel y el aviso del motivo cuando no se puede encender. Era justo el plan: validar el arnés en producción
antes de migrar los dos servicios de F5.

**Dos curvas opuestas, verificadas** (`toggle_check` + `sage/main`):
- **Encenderlo exige más energía cuanto MENOR es el nivel**: nivel 1 solo con la carga **al máximo (100)**, el 2
  con 50, el 3 con 25, el 4-5 con 1. Un sabio novato necesita reunir del todo para entrar en modo.
- **El nivel ALARGA el modo, no lo abarata**: cuesta siempre 1 de energía, pero el intervalo crece — 100 ticks
  (nivel 1) → 500 (nivel 5). Con la carga llena, el nivel 1 dura ~8 min y el 5 ~42.

**Exclusión con el sello maldito**: el datapack los hace incompatibles (`toggle_check`). Modelado como veto con
motivo (`BlockReason.CURSED_SEAL`). **Hoy nunca bloquea** porque el modo de sello llega en DJT-140 — es una línea
marcada en `SageService.cursedSealActive()`, no una omisión.

**Subida de nivel con requisitos compuestos** (`sm_lvlreq_N`): hacen falta los **tres** — chakra máximo
(7000→10000), Senjutsu (300→1200) y uso del modo (72000, o **144000** para el nivel 5). Solo progresa con el modo
activo, como el datapack. Aura verificada de `sage/effects`: Fuerza II, Velocidad II, Salto IV, Resistencia II.

**Cae** sin chakra normal (`force_deactivate_1`) o al agotar la energía natural (`force_deactivate`).

**Disparador:** el datapack usa ítems "activator" que se entregan al volverse sabio; aquí es una **tecla nueva
(G)** + `ToggleSagePayload`, siguiendo el patrón que el propio `KeyBindings` anticipaba ("ampliable sin rehacer
este registro"). El servidor valida; el cliente solo manda la intención.

**Comando de QA `/dojutsusage`** (pedido por el usuario, mismo papel que `/dojutsudojutsu`): `get`, `charge`,
`become <type>`, `level <1-5>`, `use <n>`, `oiled <bool>`. Sin él, validar el sage exigía ~22 h de juego reales.
El `get` muestra además los números del nivel actual (mínimo de activación, drenaje, ritmo de carga).

**Seis Caminos queda fuera** a propósito: no se obtiene meditando, converge en **F9**; la ficha lo excluye de F6.

**Validación:** `./gradlew build` multiloader **verde**; **717 tests** (+13: vetos de activación, las dos curvas
valor a valor, drenaje por pulsos, caída por chakra y por energía, y los requisitos compuestos de nivel).
NeoForge y Fabric arrancan limpios (`Ojos cargados: 14`, 0 ERROR reales).

**EP-SAGE cerrada.** **Siguiente: DJT-140** (`CursedSealService`: 6 behaviours, inyección probabilística, nivel
1→2) — que además cableará la exclusión con el modo sabio.

## 2026-07-28 · F6/DJT-140 — Sellos malditos: el primer modo que te mata

Cierra EP-CURSED-SEAL y **estrena `onPunish`**, el hook que existía en `ToggleableMode` desde F1 (DJT-012) sin
un solo cliente. Los tres modos anteriores (Susanoo, chakra mode del Tenseigan, sage) solo se **apagan** al
quedarse sin recurso; este **mata al portador**. Que el hook llevara cinco fases esperando y encajara sin tocarlo
es la mejor señal de que el framework estaba bien cortado.

**La inyección es una apuesta, y el fallo NO es la muerte.** El backlog decía "muerte por inyección fallida";
`inject.mcfunction` dice otra cosa y manda el datapack (regla 16). Tira `1..100`: `matches 0..60` = **éxito
(60%)**; `61..` = `fail`, que aplica daño instantáneo amp 1 + veneno 10 s amp 2 + hambre 10 s amp 2. Con la vida
llena **sobrevives**; a media vida puede rematarte. **Corregido en el checklist del backlog.**

**La muerte real llega después, por no poder pagarlo** (`punish`): sostener el sello sin chakra suficiente te
consume — *"You didn't have enough Chakra and your Cursed Seal consumed you!"*. Y en **nivel 2** cada muerte así
cuenta: a las **10** el sello **degrada** a nivel 1. Es el bucle completo del sello maldito, poder prestado con
factura.

**Madurar lo abarata**, al revés que casi todo en el datapack: nivel 1 cuesta **12** de chakra por pulso y nivel
2 solo **8**, siempre cada **40 ticks** (`d.cseal_timer >= 40`). El aura, en cambio, sube. Verificado en
`cursed_seal/cost`.

**Éxito de la inyección** (`success`): reparto aleatorio 1..6 del sello, `max_chakra += 300`, `ninja_xp += 50`,
`senjutsu += 50`, nivel 1, autoactivación y resaca (veneno/hambre 5 s amp 1). **Nivel 1→2** (`level_up`):
`ninja_xp += 100`.

**Seis sellos como datos, no seis clases.** La ficha pedía `SealBehaviour` ×6 (`#plantilla`); lo que difiere
entre ellos son cuatro amplificadores por nivel, así que son un **enum con tablas** (`CursedSealType`), no seis
implementaciones. Perfiles verificados: **Jirobó** es el más fuerte (Fuerza IV en nivel 2), **Kidomaru** no da
fuerza pero sí la mayor agilidad (Velocidad VII, Salto IX), **Sukon/Ukon** es el más flojo de base, **Cielo** y
**Tierra** son equilibrados, **Tayuya** intermedio. `-1` marca "este sello no da ese efecto" — sin ramas en el
glue. Las **habilidades propias del nivel 2** (vuelo del Cielo, aim-assist de Kidomaru, clon de Sukon/Ukon,
genjutsu sonoro de Tayuya) son efectos novedosos uno a uno: van en lotes posteriores, como se hizo con el roster
ocular.

**Exclusión con el modo sabio cableada.** La línea que DJT-136 dejó marcada (`SageService.cursedSealActive()`
devolviendo `false` con un TODO) ahora consulta de verdad a `CursedSealService.isActive`. Energía natural y
chakra maldito no se sostienen a la vez (`sage/toggle_check`).

**Disparador:** tecla nueva **B** + `ToggleCursedSealPayload` (mismo patrón que G para el sage; el servidor
valida, el cliente solo manda la intención). El **Injector** que inyecta el sello es un ítem de **F8** y delegará
en `CursedSealService.inject(...)`, que ya es API pública.

**Comando de QA `/dojutsuseal`**: `get`, `inject` (tirada real, para probar también el camino del fallo),
`give <type>`, `remove`, `level <1-2>`, `deaths <n>`. Sin él, ver la degradación exigía morir diez veces a mano.

**Validación:** `./gradlew build` multiloader **verde**; **731 tests** (+14: los bordes exactos de la tirada 60/61
con azar guionizado, el reparto de los seis, coste por nivel, cobro solo al vencer el intervalo, la muerte del
portador y que apagarlo a voluntad **no** cuesta la vida). NeoForge y Fabric arrancan limpios
(`Ojos cargados: 14`, 0 ERROR reales); sin JVMs zombie.

**Siguiente: DJT-145** (`EightGatesService`) — el de mayor riesgo de F6: ocho puertas con `StrainMeter`,
atributos reversibles (riesgo de modificador pegado), 8ª letal y reset de fatiga al dormir.

## 2026-07-28 · F6/DJT-145 — Ocho Puertas: dos riesgos de la ficha que el datapack disuelve

Cierra EP-EIGHT-GATES, el ticket marcado como **el de mayor riesgo de F6**. Los dos riesgos grandes que la ficha
anticipaba resultaron **no existir**, y por el mismo motivo: el datapack no hace lo que la ficha suponía.

**1. No hay atributos, luego no hay atributos pegados (S11-R4).** La ficha pedía `AttributeModifier` reversibles
y avisaba del clásico "modificador que se queda enganchado". `eight_gates/run` no toca ni un atributo: hace
`effect give @s strength 1 N true` **cada tick**, con duración 1. Portado igual (duración 2 para que no parpadee
entre ticks del pipeline): los efectos **caducan solos**, así que el riesgo se disuelve por construcción — el
mismo principio que el `AbilityAnchor` de F4. Cero código de reversión.

**2. La 8ª puerta no mata (S11-R2).** No hay ni un `kill` en las 23 funciones de `eight_gates/`, ni en el Night
Guy que exige tenerla abierta. Lo que hace la Puerta de la Muerte es **acumular fatiga al cuádruple** (+4 por
pulso frente al +1 de la primera). Lo verdaderamente letal es acercarse a 100: por encima de 95 llegan
debilidad/fatiga minera/lentitud, y **cerrar** con la fatiga alta cuesta daño instantáneo y hambre amp 20.
Implementado como está (regla 16) y **corregido en la ficha**.

**No es un interruptor, es una escalera.** La misma tecla abre la *siguiente* puerta; agachado, las cierra todas.
Por eso `activate` del arnés solo cubre el primer escalón y subir de la 1 a la 8 va por `openNext`, que reutiliza
las mismas comprobaciones. El framework sigue automatizando lo suyo: al llegar a 100 de fatiga, `onCost` devuelve
`false` y las puertas se cierran solas. **Quinto consumidor** del arnés y el que más deja *fuera* del modo: la
fatiga baja y castiga **también con las puertas cerradas**, así que recuperación, avisos y resaca viven en el
servicio.

**Dos curvas opuestas que premian progresar** (`get_cd`, indexadas por la puerta **máxima desbloqueada**, no por
la que abres): encadenar puertas baja de **180 a 20** ticks (10 dominándolas), y el **primer** mordisco de fatiga
sube de **20 a 65** (75 dominadas). Después, ritmo fijo de 40 ticks. Dominarlas además abarata la fatiga de las
dos últimas puertas (7ª: 3→2; 8ª: 4→3).

**Nada de esto se guarda.** El datapack recalcula `d.eg_max` y `d.eg_mastery` cada tick desde `d.taijutsu`; aquí
son funciones puras del nivel de taijutsu, así que **`gateMastery` se ha eliminado de `EmpowermentState`**: no
puede desincronizarse y no hay migración que mantener. Requisitos en escala del mod (÷4, convención de DJT-082):
75/125/175/225/275/325/375/425, maestría 500.

**El clan Lee recupera la MITAD de rápido** — `d.egstimer` 400 para Lee frente a 200 para el resto. Es lo
contrario de lo que dice la ficha ("recupera más rápido") y de lo que cabría esperar del clan especialista en
taijutsu. Portado tal cual (regla 16) con la anomalía anotada en `EightGatesTable.decayPulseTicks`: si se decide
que es un fallo de origen, es intercambiar dos constantes.

**S11-R1 saldado de verdad.** La ficha pedía "unificar la mecánica de fatiga" entre ojos y puertas. Al aparecer
el **segundo** consumidor real (no antes), las bandas de aviso 25/50/75/90/99 se extrajeron a
`domain/strain/StrainWarnings`; `EyeStrainService` ahora delega ahí.

**Deudas viejas cerradas de paso**, porque este ticket las destapó:
- `DeathHandler` tenía un TODO "al morir, eyeStrain = 0 y egStrain = 0 (aún no existen esos sub-estados)". Ya
  existen los dos → cerrado.
- `RespawnHandler` era un hook vacío desde **F1** ("desactivación de modos pendiente de fases posteriores"). Ahora
  apaga los tres modos de F6; las puertas con el cierre **seguro** (`safe_deactivate`), sin cobrar, porque la
  fatiga ya se limpió al morir.

**Disparador:** tecla **H** + `EightGatesPayload`, el primer payload de modo con **dato** (`close`) en vez de
marcador: el ítem del datapack distingue clic de clic-agachado y la tecla hace lo mismo.

**Comando de QA `/dojutsugates`**: `get`, `open`, `close`, `strain <0-100>`, `clearcd`. Sin él, ver el ciclo
completo exigía subir el taijutsu a 425 y esperar pulsos de 200-400 ticks.

**Validación:** `./gradlew build` multiloader **verde**; **752 tests** (+21: la escalera de ocho escalones, el
techo por taijutsu, los dos vetos, el retardo del primer mordisco, el cierre forzado a 100, el cierre seguro y
que su marca no contamina el siguiente cierre normal, y las dos curvas valor a valor). NeoForge y Fabric arrancan
limpios (`Ojos cargados: 14`, 0 ERROR reales); sin JVMs zombie.

**Diferido:** las **técnicas de taijutsu de puerta** (Front Lotus, Morning Peacock, Daytime Tiger, Evening
Elephant, Night Guy) ya estaban anotadas como diferidas a F6 en el backlog de F4; son jutsus del catálogo que
exigen `stage_N`, y el gate ya existe (`EightGatesService.stageOf`). Se hacen como lote de jutsus, no aquí.

**Siguiente: DJT-150** (`SoahSealService`: carga lenta + autocuración por tiers, pulsos 200/400) — el último de
F6.

## 2026-07-28 · F6/DJT-150 — SOAH: el único modo que devuelve recurso · **FASE 6 CERRADA**

Cierra EP-SOAH y con ella **toda la Fase 6**. Sexto y último consumidor del arnés `ToggleableModeService`, y el
que rompe el patrón de los otros cinco: **no cobra, paga**.

**No es "carga lenta" ni tiene "pulsos 200/400".** La ficha de `[S12]` pedía las dos cosas y el datapack no tiene
ninguna. La reserva se llena **de golpe y a mano**: agachado al activar, `recharge` transfiere **20 000** de tu
propio chakra al sello. Es el **único** origen de `d.soah_chakra` en todo el datapack (verificado con un grep
global fuera de `soah_seal/`). Y la devolución ocurre **cada tick**, no por pulsos. Tercera ficha corregida
seguida (tras el sello maldito y las puertas).

**Guardas hoy para no morirte mañana.** Con el sello activo, en cuanto tu chakra baja de **5000** el sello
empieza a devolverte de su reserva — y **no para hasta dejarte al máximo**, no hasta volver al umbral. Devuelve
por tramos y **uno solo por tick**: 100 mientras le quede mucho, luego 10, y al final gota a gota. Al vaciarse se
apaga solo y te explica cómo recargarlo.

**Por eso `onCost` aquí no descuenta nada**, solo comprueba que quede reserva. El framework se dejó abierto en F1
para "coste impagable → castigo + apagado"; un modo que regala recurso encaja igual, porque lo que se vuelve
impagable es *tener algo que dar*. Sexto modo, cero cambios al framework.

**Apagarlo cuesta quince veces más que encenderlo**: veto de **40** ticks al encender, **600** al apagar. No es un
interruptor que puedas ir dando en combate.

**Divergencia documentada (bug del datapack):** `recharge` veta con `chakra >= 10000` pero gasta 20000 — entre
esas dos cifras el chakra quedaría **en negativo**. Aquí el requisito es el importe que se paga de verdad
(20 000), porque el `Resource` del mod es todo-o-nada por diseño desde F1. Anotado en `SoahTable.RECHARGE_AMOUNT`.

**Tercera arista de exclusión de F6**: SOAH y sello maldito son incompatibles (`toggle`), como sage↔sello.

**Otorgamiento reactivo**: al cruzar Ninjutsu Médico **375** (datapack 1500 ÷4) llega el sello + 100 ninja_xp,
suscrito a `SkillLevelChangedEvent` e idempotente, mismo patrón que `UnlockService`. Añadida también la entrada
data-driven `medical_5_strength_of_a_hundred_seal` para que la entrega del **ítem** en F8 no necesite código.

**Disparador:** tecla **J** + `SoahSealPayload(sneaking)`. Agachado con la reserva vacía recarga; en cualquier
otro caso alterna — los dos gestos del ítem del datapack.

**Comando de QA `/dojutsusoah`**: `get`, `grant`, `store <n>`, `recharge`, `toggle`, `clearcd`. Sin él hacían
falta medicina 375, Yin 150 y chakra máximo 20 000 antes de poder ver nada.

**Validación:** `./gradlew build` multiloader **verde**; **763 tests** (+11: los cinco vetos en orden, entrada a
curar por umbral, que no para hasta el máximo, los tramos 100/10/1 en sus bordes, el apagado al vaciarse y la
caída sin chakra). NeoForge y Fabric arrancan limpios (`Ojos cargados: 14`, 0 ERROR reales); sin JVMs zombie.

---

### Balance de la Fase 6

Seis modos sobre **un solo framework** (`ToggleableMode`, F1/DJT-012) y **un solo arnés**
(`ToggleableModeService`, DJT-130), sin tocar ninguno de los dos al añadir modos. Cada uno estira el ciclo por un
sitio distinto y todos cupieron:

| Modo | Qué cuesta sostenerlo | Qué pasa si no puedes |
|------|----------------------|-----------------------|
| Susanoo (F5) | chakra por etapa | se apaga |
| Chakra mode del Tenseigan (F5) | chakra por pulso | se apaga (y caes) |
| Modo sabio (DJT-136) | energía natural | se apaga |
| Sello maldito (DJT-140) | chakra por pulso | **te mata** (`onPunish`) |
| Ocho Puertas (DJT-145) | fatiga corporal | cierre forzado + castigo |
| SOAH (DJT-150) | **nada: devuelve** | se apaga al vaciarse |

**Las tres fichas de F6 tenían errores de bulto** y el datapack los desmintió en los tres casos (regla 16): la
inyección fallida del sello **no mata**, la 8ª puerta **no es letal** y el SOAH **no tiene carga lenta**. Ninguno
se descubrió leyendo resúmenes; los tres salieron de abrir los `.mcfunction`.

**Deudas saldadas de paso en F6**: el hook vacío de `RespawnHandler` (de **F1**), el TODO de reset de fatiga en
`DeathHandler`, el `onPunish` sin clientes desde F1, y la unificación de bandas de fatiga ojos↔puertas (S11-R1).

**Diferido de F6** (contenido, no arquitectura): las habilidades de nivel 2 de cada sello maldito y las técnicas
de taijutsu de puerta (Front Lotus, Night Guy…), ambas como lotes de jutsus sobre plantillas ya existentes.

**Siguiente: F7** (Bestias con cola / Jinchūriki).

## 2026-07-28 · F7/DJT-160 — Bestias con Cola: entidad jefe propia + `BeastMove` compartido · **ARRANCA F7**

Abre la Fase 7 y `EP-BEAST-ENTITY`. **Primera criatura viva del mod**: hasta ahora las tres entidades propias
(proyectil, ancla, marcador) eran utilidades sin IA ni atributos.

**La ficha de `[S09]` acertó esta vez** — y conviene decirlo después de tres correcciones seguidas en F6. Todo lo
que anunciaba está en el datapack tal cual: el ravager reskineado, la barra por `store bossbar`, la duplicación
de técnicas. Verificado igual, función por función.

**Barra de jefe nativa (S09-R6 saldado).** El datapack mantiene diez `bossbar` a mano y **cada tick** lee el
`Health` por NBT de cada bestia viva para copiarlo a su barra, más otra pasada por jugador para meterlos y
sacarlos del rango. Aquí es un `ServerBossEvent`: el progreso sale de la vida y la visibilidad se resuelve una
vez por tick sobre `level.players()`, ya cargada. Cero NBT. Y se limpia tanto en `die` como en `remove`, así que
matar la bestia con `/kill` o descargar el chunk tampoco deja barras huérfanas — que era el fallo real del
enfoque manual.

**`BeastMove`: una implementación, dos lanzadores (S09-R2).** El riesgo no era hipotético: la Bola Bestia con
Cola ya vive **dos veces** en el datapack y **ya ha divergido**. La de la IA dura 100 ticks, hace daño
amplificador 3 y sabe que el Camino Preta puede absorberla; la del jugador dura **1200**, explota con tres
minecarts de TNT y no sabe nada de absorción. Portar eso literalmente sería heredar el bug.

La costura que lo resuelve es el **tipo del lanzador**: `BeastMoveContext` toma un `LivingEntity`, no un
`ServerPlayer`. La diferencia real —que el jugador paga chakra y la bestia no— se expresa en el pipeline, no en
dos copias del efecto. El datapack tenía que **fingirlo**: `ai/cast_2` le pone `d.chakra 10000` a la bestia justo
antes de invocar la función compartida, para que el chequeo de chakra pase. Nosotros no necesitamos esa mentira.

**Las diez comparten cuerpo, y por eso son un enum.** `spawn_tb_entity` es una sola función para las diez: 1024
de vida, 30 de armadura, 64 de alcance, resistencia al fuego permanente. Lo que cambia es la identidad y la
técnica insignia (`ai/cast_1`, una distinta por bestia, verificado sin repeticiones). Diez clases habrían sido
diez copias del mismo cuerpo.

**Anotado para balance:** `movement_speed base set 1` es ~4× un ravager vanilla y ~8× un jugador andando — una
bestia así es literalmente inescapable. Portado como está (regla 16) con la anomalía marcada en
`BeastStats.MOVEMENT_SPEED`: si resulta injugable, es esa constante.

**Matarla reparte y reparte por colas**: `death` no premia al que remata, da a **todos** los jugadores a 20
bloques `90 + 10·colas` de ninja_xp y un orbe de `1000 + 100·colas`. Portado igual. El **ítem de chakra** que
suelta (y que al usarse sella la bestia) llega con `SealingService` en DJT-165.

**El Diez Colas ya está marcado, no implementado**: ataca cada 40 ticks en vez de 60 y tiene un cuarto slot de
técnica que ninguna otra usa. Su contenido —Árbol Divino, Orbes Buscadores de la Verdad, Seis Caminos— converge
en **F9** (S09-R7).

**Visual provisional, deliberado.** El renderer hornea `ModelLayers.RAVAGER` —capa que **ya registra vanilla**—
así que no hace falta declarar capa propia en cada loader ni un solo asset. La bestia se ve, se pelea y se mata
desde hoy; los modelos reales son deuda de arte para F10, junto al Susanoo y los Mangekyō. La **escala crece con
las colas** (1.4 + 0.2 por cola), que es la única pista visual de cuál tienes delante hasta entonces.

**Comando de QA `/dojutsubeast`**: `summon <beast>`, `info <beast>` (incluye si su técnica insignia ya está
implementada) y `killall`. Los huevos de invocación del datapack son ítems → F8.

**Validación:** `./gradlew build` multiloader **verde**; **770 tests** (+7: el catálogo de diez, insignias sin
repetir, el Diez Colas como único especial, su cadencia, y las dos fórmulas de recompensa). NeoForge y Fabric
arrancan limpios; sin JVMs zombie.

**Limitación honesta:** el **renderer no está validado en runtime**. Un servidor dedicado nunca carga código de
cliente, así que las dos validaciones de arranque prueban registro, atributos y lógica, no el dibujado. La
primera vez que alguien invoque una bestia con un cliente gráfico es la prueba real de `TailedBeastRenderer`.

**Siguiente: DJT-161** (`TailedBeastAi`): los goals de combate y el selector de técnica de cuatro slots
(`get_move`), que ya tengo verificado — tira 0..5, y el 0 y el 5 no hacen nada.

## 2026-07-28 · F7/DJT-161 — IA de bestia: cuatro slots y la mitad de los turnos en blanco

Cierra `EP-BEAST-AI`. Los goals de combate, el selector de técnica y la **primera técnica compartida** real.

**El hallazgo: la bestia falla su turno a propósito, y muy a menudo.** `get_move` tira un número con una tabla de
botín que suelta entre **0 y 5** objetos (`loot_tables/ai_rng`) y **no todos los valores están mapeados**: el 0 y
el 5 no hacen nada, y para una bestia normal el 3 tampoco. Una bestia corriente ataca en **3 de cada 6** turnos;
el Diez Colas, en **4 de 6**. Sumado a su temporizador más corto (40 vs 60 ticks), el Diez Colas ataca del orden
de **2,5 veces más** que cualquier otra. Esas pausas son lo que hace la pelea jugable — replicarlas importa tanto
como replicar el daño, y era invisible salvo abriendo la tabla de botín.

**Cuatro slots, no diez comportamientos.** `cast_1..cast_4` son cuatro **papeles** y cada bestia rellena los
suyos: insignia, Bola Bestia con Cola (solo si te tiene a ≤5 bloques; si no, cae al slot 3), la insignia otra vez
y —solo el Diez Colas— los Orbes Buscadores de la Verdad. Todo eso vive en `BeastAiPlan`, **dominio puro**: el
goal no sabe qué técnica toca, solo quién está cerca y hacia dónde mirar.

**La Bola Bestia con Cola, primera prueba real de `BeastMove`.** Es exactamente donde el datapack se había
partido en dos, así que era la que había que escribir primero. Elegida la versión de la IA y documentado por qué:
la del jugador vive **1200 ticks** a 2,2 bloques/tick, o sea más de 2600 bloques de alcance — eso no es una
decisión de diseño, es un copy-paste de un efecto mucho más lento. Con los 100 ticks de la IA el alcance queda en
~220, coherente con una bola de chakra.

Números verificados: se mueve en once pasos de 0,2 bloques por tick, pero **no arranca hasta el cuarto tick** —
las tags `d.hit1/2/3` son una cuenta atrás de tres, o sea que la bola se forma quieta antes de salir. Al impactar
reparte `instant_damage` amplificador 3 (24 puntos, doce corazones) en 5 bloques y detona. El `HARM` se
**invierte solo** en los no-muertos, como en todo el proyecto: la rama `#dojutsu:undead` del datapack sale gratis.

**Goals.** El datapack deja al ravager su IA vanilla —persigue y golpea— y le añade **encima** el bloque de
técnicas. Aquí es lo mismo dicho como goals, y el de técnicas se declara con `EnumSet.noneOf(Flag.class)`: no
bloquea moverse ni mirar, así que la bestia dispara mientras persigue, igual que el original.

El `if entity @p[distance=0..32]` que envuelve todo el bloque de IA es ahora el `canUse()` del goal — la misma
idea dicha donde corresponde, y de paso una bestia en un chunk cargado y vacío no gasta ni un tick.

**Tenpenchii: decisión modelada, contenido diferido.** `BeastAiPlan` ya sabe que el Diez Colas cae a Brazos de
Chakra si su Tenpenchii está en cuenta atrás, y está testeado. Lo único que falta es que algo ponga ese
temporizador, y eso es contenido del Diez Colas → **F9** (S09-R7). Marcado con una línea en
`TailedBeastEntity.tenpenchiiReady()`, como se hizo con la exclusión sage↔sello en DJT-136.

**Validación:** `./gradlew build` multiloader **verde**; **780 tests** (+10: los seis valores de la tirada uno a
uno, el recuento de turnos útiles de una bestia normal frente al Diez Colas, los cuatro slots, la excepción del
Tenpenchii y que el slot 4 es exclusivo). NeoForge y Fabric arrancan limpios y registran la técnica
(`Tecnicas de bestia registradas: 1`); sin JVMs zombie.

**Sigue sin validar en runtime el renderer** (código de cliente, ver DJT-160). Y el **combate real** —que la
bestia persiga, dispare y acierte— tampoco: eso pide un cliente gráfico, no un servidor dedicado.

**Diferido:** las **nueve técnicas insignia restantes** (Sand Buckshot, Cat Flame, Great Water Mass Bullet,
Monkey Flame Arson, Unrivalled Strength, Soap Bubble Barrage, Scale Powder, Tentacle Barrage, Chakra Arms
Barrage) son un lote de contenido sobre la plantilla `BeastMove` ya probada, como se hizo con el roster ocular.
Mientras tanto la bestia pierde ese turno en vez de romperse. También la absorción por el Camino Preta
(`d.absorbed`), que necesita un gancho en el proyectil.

**Siguiente: DJT-165** (`SealingService` + `TailedBeastRegistry`): sellar una bestia en un jugador, el +8000 como
estado especial (RA-2) y el registro global de exclusividad (`AllowMultJinchuriki`).

## 2026-07-28 · F7/DJT-165 — Sellado: "+8000" era "1000 por cola", y el préstamo se guarda

Abre `EP-JINCHURIKI`. **Cuarta corrección de ficha del proyecto**, y de las gordas.

**No hay ningún +8000.** El ADD lo repetía en tres sitios (la ficha de F7, el javadoc de `BeastSealedEvent`, el
riesgo RA-2). `jinchuriki/sealing/*` dice otra cosa: **`max_chakra += 1000 × colas`**, de 1000 con Shukaku a
10 000 con el Diez Colas. El aviso sigue siendo correcto en lo que importa —eso revienta la escala normal— pero
la forma es distinta: **escala con la bestia**. Corregido en los tres sitios.

**Y la XP tiene dos escalones que rompen su propio patrón.** De la 1 a la 8 es `100 + 10×colas` (110…180). Pero
el **Nueve Colas da 230**, no 190, y el **Diez Colas da 1000**. Guardado como tabla y no como fórmula
precisamente porque la fórmula miente justo en los dos casos que más importan.

**La respuesta real a RA-2 no es "marcarlo", es guardarlo.** El riesgo de un número que rompe la escala no es su
tamaño, es no poder deshacerlo. `removal/*` resta la **constante literal** que sumó `sealing/*` — lo cual
funciona solo mientras nadie toque esas cifras entre versiones. Aquí `JinchurikiState.grantedMaxChakra` guarda
**cuánto se prestó de verdad**, así que extraer la bestia devuelve exactamente eso aunque la tabla cambie. La
reversión es exacta por construcción, no por coincidencia.

**El registro global existe porque la pregunta no es sobre el jugador que tienes delante.** Con
`AllowMultJinchuriki=false` solo puede haber un jinchuriki por bestia **en todo el servidor** (S09-R4), y el
portador **puede estar desconectado**: eso no se responde ni mirando al jugador ni recorriendo a los conectados.
De ahí `TailedBeastRegistry` como `SavedData` del overworld. Guarda el **UUID del portador** en vez del 0/1 del
datapack: cuesta lo mismo y responde a la pregunta útil —no solo *si* está tomada, sino **por quién**—.

**`TemporaryGrantService` encuentra por fin su consumidor.** Cada bestia presta naturalezas y a veces un Kekkei
Genkai, y el datapack los marca `d.tmp_*`/`d.tkg_*` para poder retirarlos sin llevarse por delante lo que ya era
tuyo. Ese servicio se escribió en **F3/DJT-053** con un javadoc que decía literalmente *"Consumidor real: F7
(bijuu)... hoy solo lo ejercita el comando de depuración"*. Cuatro fases después, se conectó sin tocar una línea.

Detalle bonito y verificado: **los elementos prestados son justo los componentes del KG que da la bestia** —
Shukaku presta viento + tierra y otorga Magnetismo; el Cuatro Colas, fuego + tierra y Lava; el Cinco, fuego +
agua y Ebullición. Solo cuatro de las diez prestan KG, y el 7, el 8 y el 9 no prestan **nada**.

**Validación:** `./gradlew build` multiloader **verde**; **787 tests** (+7: el chakra por cola, los dos escalones
rotos de la XP, los componentes de cada KG, las tres bestias sin elemento, que solo cuatro dan KG y que ninguna
presta el yin-yang derivado —que se calcula de yin+yang y duplicarlo sería un bug—). NeoForge y Fabric arrancan
limpios; sin JVMs zombie.

**Lo que falta por dependencia, no por olvido:** el disparador real es el **ítem de chakra** que suelta la bestia
al morir, y exige el jutsu **Sello de los Cuatro Símbolos** activo (`fss_timer`). Los ítems son **F8**; hoy el
disparador es `/dojutsubeast seal`. El caso especial del **Diez Colas** (`ten_tails/seal`, que salta la
comprobación de "ya llevas una") converge en **F9** con el resto de su nodo.

**Siguiente: DJT-166** (`FormService`), que cierra F7: formas cloak/full/baryon, la Bola Bestia con Cola desde el
lado del jugador —el otro extremo del `BeastMove` que ya existe— y el invariante de que **la forma desactiva los
ojos**.

## 2026-07-28 · F7/DJT-166 — Formas, vínculo y Baryon · **FASE 7 CERRADA**

Cierra `EP-JINCHURIKI` y con ella toda la Fase 7. **Séptimo** consumidor del arnés de F1, el primero fuera de
F5/F6, y el que **cierra el círculo de `BeastMove`**: DJT-160 creó el contrato, DJT-161 lo estrenó desde la IA, y
aquí entra el otro extremo — el jinchuriki lanza **la misma instancia** de la técnica que le lanzaría la bestia.
Lo único que cambia es el pipeline: el jugador paga chakra, la bestia no. Exactamente lo que decía el javadoc de
`BeastMove` antes de que existiera un jugador capaz de probarlo.

**El hallazgo que cambió la implementación: los ojos se SUPRIMEN, no se apagan.** `[ADD/04 §8]` pedía "la forma
desactiva los ojos", y lo natural habría sido llamar a `withoutActiveEye()`. El datapack hace otra cosa:
`dojutsu/main` ejecuta cada ojo `unless is_in_jinchuriki_form` — el ojo sigue **equipado**, simplemente no
responde. Y esa diferencia no es cosmética: **la forma del Diez Colas exige el Rinnegan activo** (`form/toggle` y
`form/main`), así que apagarlo de verdad haría que esa forma se apagase a sí misma en el mismo tick en que
empieza. Implementado como supresión, con la razón anotada en el código para que nadie lo "arregle" luego.

**El aura la fija el vínculo, no la bestia.** `effects.mcfunction` reparte fuerza/velocidad/salto/resistencia por
**nivel de jinchuriki**: llevar a Kurama con vínculo 1 da exactamente lo mismo que llevar a Shukaku con vínculo 1.
Lo que cambia entre bestias es el **color de las partículas** y las **técnicas**. Por eso es una tabla de cuatro
filas y no de diez — y por eso el color del chakra que ya teníamos de DJT-160 acaba siendo la única señal visual
de qué forma llevas.

**Las técnicas llegan tarde, a propósito.** `form/main` solo ejecuta el bloque de la bestia con `jinchuriki_lvl
>= 3`. Los dos primeros niveles de forma son **solo aura**: la potencia llega antes que el arsenal. Y el nivel 3
es justo donde el mensaje cambia de "sientes que tu vínculo se fortalece" a "**te has hecho amigo de Kurama**" —
la bestia pasa a tener nombre en el mismo momento en que te presta sus técnicas. Es el mejor detalle del sistema.

**El vínculo se gana esperando.** Es el único progreso del juego que se mide en **tiempo con la bestia dentro**,
no en actos: `bond_main` suma un tick por tick sin que hagas nada. 2 horas por nivel con las dóciles, 3 con
Shukaku y el Ocho, **5 con Kurama**; y el último escalón (el que abre el Baryon) casi el doble — **8 horas** con
el Nueve Colas. El Diez Colas **no tiene vínculo**: `main` lo excluye del bloque.

**El Baryon no es un modo caro, es terminal.** Dura 12 000 ticks (10 minutos), duplica el chakra máximo y lo
rellena entero… y al agotarse `out_of_time` **extrae la bestia**, con despedida incluida ("This is farewell...").
No es un coste que puedas pagar dos veces. Modelado con esa consecuencia dentro del propio tick para que el
servicio no pueda olvidarla.

**Tres intenciones en una tecla** (K): pulsar transforma, **agachado** lanza la técnica de la bestia,
**corriendo** alterna el Baryon. El datapack usa tres ítems "activator" distintos; el patrón de payload con dato
ya estaba probado en las Ocho Puertas.

**Validación:** `./gradlew build` multiloader **verde**; **799 tests** (+12: los tres vetos, el préstamo de
chakra devuelto exacto, el Rinnegan del Diez Colas —incluido perderlo a media forma—, que el aura la fija el
nivel, que el Baryon **reemplaza** el aura en vez de sumarse, el umbral de técnicas y los tiempos de vínculo por
bestia). NeoForge y Fabric arrancan limpios; sin JVMs zombie.

**Diferido de F7** (contenido, no arquitectura): el **refuerzo a baja vida** (`jin_boost`: a 3 corazones la
bestia te presta 500 de chakra y absorción, con diálogo propio por bestia y nivel) — mecánica completa que merece
su propio lote; las **nueve técnicas insignia** restantes; y todo el **nodo del Diez Colas** (Estatua Gedo,
Tenpenchii, Orbes Buscadores de la Verdad), que converge en **F9** como estaba planificado.

---

### Balance de la Fase 7

Cuatro tickets, y el hilo conductor fue **una sola decisión de diseño sostenida**: `BeastMove` con un
`LivingEntity` por lanzador en vez de un `ServerPlayer`. Esa línea es lo que impide que las técnicas vuelvan a
partirse en dos como ya les había pasado en el datapack — donde las dos Bolas Bestia con Cola llevaban tiempo
divergiendo sin que nadie lo notara (100 vs 1200 ticks de vida, distinto daño, y solo una sabía de la absorción
del Camino Preta).

**La ficha de `[S09]` acertó en casi todo**, al contrario que las tres de F6. La única corrección fue el
**"+8000"**, que resultó ser **1000 por cola**. A cambio, los dos riesgos que sí anticipaba (S09-R2 duplicación,
S09-R6 bossbars por NBT) eran reales y se resolvieron por diseño, no por parche.

**Piezas de fases anteriores que encajaron sin tocarlas:** `TemporaryGrantService` (F3, escrito con un javadoc
que decía "consumidor real: F7"), el arnés `ToggleableModeService` (F6), `NinjaProjectile` (F4) y el framework
`ToggleableMode` (F1). Ninguna necesitó cambios.

**Siguiente: F8** (ítems, invocaciones, throwing/FTG, IA shinobi, comercio) — que además desbloqueará los
disparadores reales de medio F6 y F7: el Injector del sello maldito, el ítem del SOAH, los huevos de invocación y
el ítem de chakra que sella la bestia.

## 2026-07-29 · F8/DJT-170 — Ítems: tres de los cuatro campos que pedía la ficha ya tenían dueño · **ARRANCA F8**

Abre la Fase 8 y `EP-ITEMS`.

**El hallazgo es de contabilidad, no de mecánica.** La ficha pedía un `ItemFlags/DnaState` con cuatro campos:
`oiled`, `maskId`, `pills` y `dna`. Al abrir el datapack, **tres ya estaban modelados** en fases anteriores, y
duplicarlos habría creado dos fuentes de verdad para lo mismo:

- **`oiled`** (aceite de sapo) vive en `SageState` desde **F6/DJT-135**: es un modificador de la velocidad de
  carga de energía natural, no un flag de inventario.
- **`dna`** no es estado propio en absoluto: implantar concede naturalezas y KG, que viven en `NatureProfile`
  desde **F3** — incluidas las células de Hashirama (`hashiramaDna`).
- **`pills`** son cargas **del ítem** (`PillCount`), no del jugador; la resaca es un efecto temporal.

Queda **un solo campo real**: la máscara puesta. `ItemFlags` tiene exactamente ese, y el javadoc explica por qué
los otros tres no están — para que nadie los "eche en falta" y los añada.

**Los 26 ADN cuadran exactos, y solo si separas las tres familias.** El datapack los multiplexa sobre un
`d.dna_tmp`, pero conceden cosas distintas: **7 naturalezas** + **18 Kekkei Genkai** + **las células de
Hashirama**, que no son un KG (no escriben ningún `d.kg_*` sino `d.has_hsdna`). Eso ya estaba visto en F3 —el
javadoc de `KekkeiGenkai` lo explica y por eso el enum tiene 18 y no 19— así que `DnaType` lo respeta en vez de
forzarlas dentro. 7 + 18 + 1 = 26.

**Implantar sale mal dos de cada tres veces** (`dna_rng` 1..3, gana el 1) y fallar **consume el ADN** además de
dejar náusea, debilidad y veneno. Un ADN raro se puede perder tres veces seguidas: es el coste de jugar a ser
Orochimaru.

**Salvo dos veces, y las dos cuentan la misma historia.** El azar no manda si **tu cuerpo ya aceptó ese linaje**:
Madera teniendo ya células de Hashirama, o células de Hashirama siendo del clan **Senju** —el de Hashirama—. Son
las dos únicas excepciones del datapack y ninguna es de balance.

**Y lo que ya tienes no se te cobra**: `idna_alr` anula las **dos** etiquetas (ni éxito ni fallo) y marca
`d.dont_take`, así que el ítem **no se consume**. Un no-intento, no un intento perdido — que es justo lo que
evita quemar un ADN raro por despiste. Modelado como un cuarto resultado (`ALREADY_OWNED`) para que el ítem no
pueda equivocarse al decidir si gastar el stack.

**La máscara es estado, no casco.** El datapack la mete en `armor.head` y por eso te veta llevar casco
(`masks/check_eye` aborta si el slot está ocupado). Aquí es estado — **la misma decisión que ya se tomó con los
ojos en F5**, y por el mismo motivo. Consecuencia asumida y buscada: puedes llevar máscara y casco.

Detalle que sí hubo que respetar: ocultar el nombre se hace con un **equipo**, y un jugador solo puede estar en
uno. El datapack ya vivía con eso (al ponértela hace `team join MaskHidden`, al quitártela llama a
`clans/join_team`), así que aquí `MaskService` llama a `ClanTeamService.sync` — que **ya existía y dejó anotado
desde F3 que el equipo `MaskHidden` era "de los ítems de F8"**. El sufijo del clan desaparece mientras llevas
máscara; es lo que tiene esconder quién eres.

**Encadenar pastillas no encadena resacas.** `eat` solo encola la resaca `unless ration_pill_queued`: comerte las
cinco seguidas te da las cinco recargas y **una sola** resaca. Es un detalle facilísimo de perder al portar, y es
lo que hace que atiborrarse sea una jugada válida en vez de un suicidio. Un `putIfAbsent` lo dice mejor que la
bandera + `schedule` del original.

**Cinco ítems reducidos a un contador.** `ration_pills/eat` **sustituye el ítem entero** en cada mordisco: cinco
`carrot_on_a_stick` distintos con su `CustomModelData` y su lore, solo para mostrar "Pills Left: 3". Aquí son
cargas en la durabilidad, que además pinta la barra vanilla sin escribir nada.

**Escalas:** identificar pide medicina **63** y implantar **75** (datapack 250 y 300, ÷4 — la convención de
DJT-082).

**Comando de QA `/dojutsuitem`**: `get`, `dna give [type]` (sin argumento, sin identificar), `dna implant <type>`
(intento real), `mask <type>`, `pills`.

**Validación:** `./gradlew build` multiloader **verde**; **815 tests** (+16: las tres familias sumando 26 exactos,
que las células de Hashirama no son ni naturaleza ni KG, los bordes de la tirada 1/2/3, las dos excepciones
garantizadas —y que no se contagian a otros KG—, que un garantizado prende con la peor tirada, que lo ya poseído
se comprueba **antes** del dado, la fracción de chakra por pastilla y la simetría de poner/quitar máscara).
NeoForge y Fabric arrancan limpios; sin JVMs zombie.

**Diferido de DJT-170** (contenido, no arquitectura): las **píldoras de colores** del Akimichi (rojo/amarillo/
verde con `pill_stage`, atadas al Modo Mariposa del clan), las **herramientas quirúrgicas** (el implante de ojos
ya existe desde F5, solo falta el ítem), y los **pergaminos** (experiencia, médico, sage, entrenamiento). Todos
son ítems finos sobre servicios que ya existen. Y los **modelos** de los nuevos ítems → F10, con el resto.

**Siguiente: DJT-175** (`ContractService`/`SummonService`) — invocaciones, con la vida en la propia entidad y no
en un selector global por tick (S15-R3).

## 2026-07-29 · F8/DJT-175 — Invocaciones: el ancla de F4 era la respuesta a S15-R3

Cierra `EP-SUMMONING`.

**S15-R3 se resolvió reutilizando una primitiva de tres fases atrás.** El riesgo era "tiempo de vida por selector
global", y el datapack lo confirma: `main/tick` corre **dos selectores por tick** sobre
`@e[type=#dojutsu:contractable,tag=d.summon]` —uno suma `d.summon_time` a cada invocación viva, otro mata a las
vencidas— y eso se ejecuta **siempre**, haya invocaciones o no.

La solución no fue escribir una entidad nueva: cada invocación nace con un **`AbilityAnchor` pegado**, que ya
tiene **caducidad por construcción** desde F4/DJT-071 (la vida es parámetro obligatorio y al expirar corre su
callback). La criatura desaparece porque su ancla venció, no porque alguien barra el mundo buscándola. Con cero
invocaciones el coste es literalmente cero. El ancla se escribió para las habilidades oculares de F5 y resulta que
era exactamente la primitiva que pedía este riesgo.

Matiz a favor del jugador: el ancla hace `discard()`, no `kill()`. El datapack usa `kill @s`, que **suelta botín y
cuenta como muerte**; una invocación que se desvanece no debería dar carne de vaca. Divergencia consciente.

**Invocar no cuesta chakra.** La ficha decía "invocar por tier (**coste**)". No hay ni una línea de chakra en
`jutsu/summoning/cast`: lo que se paga es **tiempo**. El veto arranca en 600 ticks (30 s) y baja a 200 (10 s) según
el fuinjutsu — el recurso de un maestro del sellado es su propia maestría.

**Firmar y usar son dos puertas distintas**, y es fácil no verlo porque los números se parecen. Firmar exige rango
**Chūnin** para cualquier tier y fuinjutsu solo en mid/high; usar exige fuinjutsu **también en el tier bajo**. Se
puede acabar con un contrato firmado que todavía no puedes usar — coherente: firmar es un pacto, invocar es
pagarlo.

**Inconsistencia del datapack anotada:** el tier bajo abre a fuinjutsu 300 (mod **75**) pero `cast` exige 500 (mod
**125**). Entre esas dos cifras pasas la puerta del tier y **después** recibes "Requires higher Fuinjutsu!". El
suelo efectivo es 125; portado así y documentado en `SummonTable`.

**Firmar mata a la criatura.** `attempt_form_contract` acaba en `kill @s`: el bicho con el que firmas **no
sobrevive**. Suena brutal y lo es, pero cambia cómo se siente el sistema — no coleccionas amigos, sellas un
linaje. Portado (con `discard`, sin botín).

**27 especies eran 108 líneas repetidas cuatro veces.** El datapack las multiplexa a mano en `contract_main`,
`summon_main`, `setup` y `do_summon` — añadir una especie obliga a tocar los cuatro. Ahora es **un JSON por
especie** y cero código: un pack puede añadir contratos sin recompilar (RA-3). Reparto verificado: 13 en el tier
bajo (granja), 12 en el medio (monturas y bestias), 2 en el alto (aldeano y mercader errante). Y `tame`/`saddle`/
`chest` son **datos de la especie**, no ramas: las monturas se invocan domadas y con silla, burro y mula además
con cofres.

**Validación:** `./gradlew build` multiloader **verde**; **823 tests** (+8: las dos puertas por separado, que el
tier bajo no pide fuinjutsu para firmar pero sí para usar, el rango extra del alto, los tres tramos del veto valor
a valor y la inconsistencia de suelos como test explícito). NeoForge y Fabric arrancan limpios y cargan los
**27 contratos**; sin JVMs zombie.

**Diferido:** el **círculo de invocación** como visual propio (hoy son partículas de humo, como el datapack antes
de tener modelos) → F10; y las invocaciones **de combate** con IA propia (los sapos de Jiraiya y compañía), que en
el datapack no existen — son mobs vanilla domados.

**Siguiente: DJT-180** (`ThrowingService` + `FtgMarkerService`) — kunai y shuriken sobre `NinjaProjectile`, y las
marcas del Vuelo del Dios del Trueno en el Attachment para que no dependan del chunk.

## 2026-07-29 · F8/DJT-180 — Arrojadizas y Vuelo del Dios del Trueno: la marca sale del mundo

Cierra `EP-THROWING`.

**El hallazgo que reordena el ticket: el datapack usa `forceload` para que la marca sobreviva.** En
`throwing/move`, cuando el kunai del Dios del Trueno se clava, ejecuta **`forceload add`** sobre su chunk; y
`teleport`/`reset` hacen `forceload remove`. Es decir: **cada marca mantiene un chunk cargado indefinidamente**, y
si algo mata el kunai por cualquier otra vía el forceload queda **huérfano para siempre**. Eso es exactamente el
borde que la ficha señalaba ("marca en chunk descargado") — y el parche del autor es peor que el problema.

Aquí la marca son **coordenadas en el estado del jugador** (`GlobalPos`: dimensión + posición, en el Attachment).
No depende de ningún chunk, **funciona entre dimensiones por construcción** y no hay nada que liberar. El kunai
clavado pasa a ser la parte *visible* de la marca, no la marca. S16 resuelto por modelado, no por parche.

**Y no hay múltiples marcas.** La ficha listaba "múltiples marcas (selección)" como caso borde. `reset` mata el
kunai anterior en cuanto lanzas otro, así que hay **exactamente una por jugador** — y teletransportarse la
**consume** (`kill @s` tras el `tp`). No es una red de puntos de viaje: es **una salida de emergencia** que
preparas y gastas. Es un `Optional`, no una lista, y no por simplificar.

**Dos daños que es fácil confundir.** El atributo del item (5 el kunai, 3 el shuriken, 4 el fuma) es para
**blandirlo**. Lanzadas, las cinco armas hacen **lo mismo**: `instant_damage` amp 0, seis puntos. Un fuma shuriken
no pega más por volar. Los tests lo fijan explícitamente porque es el tipo de detalle que un port "razonable"
cambiaría sin darse cuenta.

**El shuriken vuela más rápido que el kunai** (0,8 vs 0,5 bloques/tick) — contraintuitivo, pero es lo que dice
`move`. A cambio el kunai se lanza **el doble de seguido** (veto de 10 ticks frente a 20). Portado tal cual.

**Dominar el salto lo abarata cinco veces**: de 50 de chakra a **10** en el nivel 21 de la técnica. Y el **clan
Namikaze** —el de Minato— gana el **doble** de XP por salto. Cap nivel 30. Además apareces **3 bloques por
encima** de la marca, no en el punto exacto: si clavaste el kunai en una pared, sales arriba y no incrustado.

**El proyectil y la recuperación ya existían.** El datapack invoca un armor stand por tiro y lo teletransporta a
mano cada tick comprobando el bloque de delante en cuatro pasos; `NinjaProjectile` (F4/DJT-070) ya hace eso con
raycast y **dueño por UUID** — que es el riesgo **S16-R2** resuelto de antes, sin el `d.throw_ply`/`d.throw_as`
pareado por scoreboard. Y `throwing/drop` reconstruye a mano el NBT de cada arma para soltarla al suelo tras 120
ticks; aquí el proyectil **suelta su propio item** al clavarse y recogerlo es la mecánica vanilla, sin código.

**Validación:** `./gradlew build` multiloader **verde**; **834 tests** (+11: las dos velocidades, el veto doble
del kunai, la separación entre daño en mano y daño lanzado, que solo el FTG es marca y no se recupera, los tres
tramos de coste del salto, el doble de XP del Namikaze, el cap de nivel, y que marcar **reemplaza** —incluso entre
dimensiones— y saltar consume). NeoForge y Fabric arrancan limpios; sin JVMs zombie.

**Diferido:** el **shuriken rotando** en vuelo (`throwing/rotate`) es presentación → F10 con el resto de visuales;
y el **Rasenshuriken** aparece en `move` como un arrojadizo más, pero es un **jutsu** del catálogo de F4, así que
se enganchará ahí cuando se implemente.

**Siguiente: DJT-185** (`ShinobiEntity` + `ShinobiCombatAi`) — el de mayor riesgo de F8: la ficha avisa de que el
datapack **estaba incompleto** (WIP con `say` de depuración), así que la IA hay que **diseñarla**, no portarla.

## 2026-07-29 · F8/DJT-185 — IA shinobi: el único sistema del proyecto que hay que diseñar

Cierra `EP-SHINOBI-AI`, el ticket marcado como el de mayor riesgo de F8 (**RM-5**).

**La ficha acertó: el datapack está a medias, y de forma literal.** `shinobi/ai/get_move` tiene **seis ramas** y
solo dos hacen algo — lanzar un kunai y un `great_fireball`. Las otras cuatro son `say do nothing (0)` y
`say cast stronger jutsu`. Y `ai/get_jutsu`, que lista **30 jutsus**, **no se llama desde ningún sitio**: es
código muerto con un `say` pegado a cada cast. Portarlo literalmente sería portar un depurador a medias.

**Pero el datapack sí decide dos cosas, y las dos se respetan.** El **ritmo** está terminado y es **idéntico al de
las bestias** de F7 — decide cada **60 ticks**, solo con un jugador a **32 bloques**, y tira el **mismo dado 0..5**
donde **la mitad de los turnos no hace nada**. Eso no puede ser casualidad: el autor tenía un patrón de IA y lo
aplicó a los dos sistemas. Y la **intención** de cada tirada está declarada en los propios `say`: el 1 es kunai, el
2 un jutsu débil, el 3 uno más fuerte. Así que la mitad del "diseño" ya estaba escrito en los mensajes de
depuración.

**Lo genuinamente nuevo es la distancia.** El datapack no mira dónde está el objetivo, así que un shinobi pegado a
ti te lanzaría una bola de fuego a los pies. Aquí el dado sigue mandando **cuándo** actúa y con qué intención, y la
banda de distancia **filtra** lo que tiene sentido: a bocajarro pelea a mano, a media distancia tira kunai, de
lejos lanza jutsus (y un kunai desde lejos se convierte en jutsu, porque no llegaría). Está marcado como diseño
nuevo en el javadoc y fijado con tests para que no derive.

**S17-R3 estaba cerrado desde F4, en dos sitios y sin que hiciera falta tocar nada:**
- `CastContext` decía en su javadoc, cuatro fases antes de que existiera un solo NPC: *"No asume jugador
  (S17-R3): un NPC con IA implementa esta interfaz igual que un Player"*.
- `JutsuEffect.apply` ya recibía un **`LivingEntity`**, no un `ServerPlayer`.

El shinobi entra por el **núcleo genérico** `JutsuCastPipeline.cast(UUID, ...)` — el mismo que usa el jugador tras
su sobrecarga de conveniencia — así que pasa por **las mismas puertas**: naturaleza, afinidad, Kekkei Genkai y
coste. Un shinobi no puede saltarse un requisito que un jugador no puede saltarse. Tercer "ya estaba anticipado"
de la fase, y el más limpio.

**La tabla de amenaza sí estaba completa**, y es exacta: la vida crece **de 120 en 120** del Académico al Jōnin y
el **Kage rompe el patrón** con 700 en vez de 720. Guardada como tabla, no como fórmula, precisamente por eso. Los
seis: 120 / 240 / 360 / 480 / 600 / 700, `follow_range 64`, `chakra 10000`, `aff_fire 100` y **sin botín**.

**Y una entidad extra que desaparece.** El datapack invoca un `zombie` con nombre y le cuelga un **armor stand
flotante** para mostrar `Threat: Chūnin` — más un barrido por tick (`spawn/internal/main`) solo para
teletransportar esa etiqueta y matarla si su shinobi se fue. Aquí el nivel de amenaza es estado **sincronizado** y
el título es el nombre de la propia entidad: ni armor stand, ni barrido, ni etiquetas huérfanas.

**Edo Tensei: lo que hay y lo que no.** La reanimación existe como **resultado** (los cuatro `spawn/undead_*`) pero
**no hay técnica que la dispare**: se obtienen con un huevo de invocación. O sea que el Edo Tensei del datapack es,
literalmente, un ítem de creativo. `EdoTenseiService` es la **puerta** con los seis niveles; el disparador queda
para el ítem (F8) o el jutsu narrativo (F9), sin tener que reimplementar nada.

**Validación:** `./gradlew build` multiloader **verde**; **845 tests** (+11: la tabla de vida valor a valor
incluido el escalón roto del Kage, que la mitad de los turnos es IDLE, que a bocajarro las tres intenciones caen en
melee, que de lejos el kunai se vuelve jutsu, que un Académico sin jutsus siempre tira kunai —y de lejos no hace
nada—, la contigüidad de las bandas y que el ritmo coincide con el de las bestias). NeoForge y Fabric arrancan
limpios; sin JVMs zombie.

**Sin validar en runtime:** el renderer y el **combate real** (que persiga, tire kunai y castee). Piden cliente
gráfico, igual que las bestias.

**Diferido:** el **roster de jutsus por nivel** es diseño nuevo y conservador (solo fuego, la afinidad que el
datapack le da al aparecer); ampliarlo a otras naturalezas es contenido, no arquitectura. Y el **protector de
frente** por aldea (24 combinaciones de color × aldea en `give_forehead_protector`) es puro arte → **F10**.

**Siguiente: DJT-190** (`ShinobiVillagerService` + ofertas JSON + `TraderSpawnPolicy`), que **cierra F8**.

## 2026-07-29 · F8/DJT-190 — Comercio: 36 líneas de NBT ilegible pasan a dos JSON · **FASE 8 CERRADA**

Cierra `EP-TRADING` y con ella toda la Fase 8.

**RB-4 en su forma más pura.** El datapack construye las ofertas **con el NBT literal dentro de la función**:
`trades/setup_shinobi` tiene **14** líneas de `data modify ... Offers.Recipes append`, y cada una lleva el ítem
entero incrustado —`CustomModelData`, lore, atributos, durabilidad— en una sola línea de miles de caracteres.
`setup_trader` tiene **22** más. Cambiar un precio significa editar una línea ilegible; añadir una oferta, copiarla
y rezar. Ahora son dos JSON de cinco campos por oferta, y el ítem se referencia por su id de registro: el objeto ya
sabe cómo es.

**Conversión por evento, no por barrido.** `setup_custom_entities` es un bucle `schedule 1s` que **recorre todos
los aldeanos y mercaderes del mundo cada segundo** preguntando si ya los procesó (tag `d.tsetup`). Funciona, pero
el coste es permanente y crece con la aldea. Aquí es `EntityEvent.ADD`: se decide **una vez, al aparecer**. Ni
barrido, ni tag de "ya visto", ni coste con cero comerciantes — el checklist de la ficha lo pedía explícitamente.

**Y el catálogo informa de lo que aún no existe, en vez de callarlo.** Las ofertas del datapack incluyen mercancía
cuyo ítem todavía no está implementado (bombas de humo, pergaminos, ojos como ítem). Había tres opciones y dos eran
malas: recortar el catálogo (y perder la referencia de lo que falta) o fallar la carga (y quedarse sin
comerciantes por mercancía futura). La tercera: **declararlo completo y contar en el log lo que falta**. El arranque
dice literalmente `2 catálogos (24 ofertas, 15 pendientes de su ítem)` — así que hoy 9 ofertas funcionan y las
otras **aparecerán solas** el día que su ítem se registre, sin tocar código.

**Dos números que se portan del revés con facilidad, y van con test propio:**

- **El ritmo del mercader es "veces por día", no un intervalo.** `trader/try` programa el siguiente intento a
  `24000 / rate` ticks — la tabla de 24 líneas del datapack es esa división escrita a mano, valor a valor
  (24000, 12000, 8000… 3429 para rate 7). La config no dice cada cuánto aparece: dice **cuántas oportunidades hay
  al día**.
- **La "probabilidad" va al revés.** `traderspawnchance` tira en el rango **[chance, 10]** y gana el **10**. O sea
  que `TraderSpawnChance = 10` es **certeza** y `= 1` es una de cada diez: el número no es la probabilidad, es
  **cuánto se acerca al techo**. Portarlo del revés habría sido un error silencioso.

**Y solo uno de cada diez aldeanos es un shinobi** (`trades_rng` 0..19, gana 0..1 = **10%**). El mercader errante,
en cambio, se convierte **siempre**: el datapack no le tira dados, porque ya es raro de por sí. Encontrar un
shinobi significa algo — si todos vendieran kunai, no serían shinobi, serían una tienda.

**Validación:** `./gradlew build` multiloader **verde**; **855 tests** (+10: el 10% de conversión en sus bordes, la
división de "veces por día" valor a valor incluido el redondeo del 7, el cap en 24, la dirección de la
probabilidad —certeza con 10, 1/10 con 1—, el clamp fuera de rango y el saneo de una oferta con precio 0). NeoForge
y Fabric arrancan limpios y cargan los 2 catálogos reportando las 15 pendientes; sin JVMs zombie.

**Diferido:** el **ítem** de cada mercancía pendiente (bombas de humo, pergaminos, Kotarō, aceite de sapo, ramen,
píldoras de colores, herramientas quirúrgicas, ojos) — el catálogo ya los espera. Y la **invocación forzada** del
mercader por la política (hoy `TraderSpawnPolicy` decide *si* debe aparecer; quien lo invoque es trabajo de F9,
donde vive la generación del mundo).

---

### Balance de la Fase 8

Cinco tickets, y el hilo conductor fue distinto al de F7: aquí no hubo una decisión de diseño que sostuviera la
fase, sino **cinco veces la misma comprobación** — *¿esto ya existe?*

**Tres piezas de fases anteriores encajaron sin tocarlas, y las tres estaban anotadas por escrito:**
- `TemporaryGrantService` (F3) decía *"consumidor real: F7 (bijuu)"* → lo usó el sellado de DJT-165.
- `ClanTeamService` (F3) decía *"el equipo `MaskHidden` es de los ítems de F8"* → llegó en DJT-170.
- `CastContext` (F4) decía *"No asume jugador (S17-R3)"* y `JutsuEffect.apply` ya tomaba un `LivingEntity` → el
  shinobi de DJT-185 castea por el mismo pipeline, **cero cambios**.

Y una cuarta que no estaba anotada pero encajó igual: el **`AbilityAnchor`** de F4 resolvió S15-R3 (la caducidad de
las invocaciones) porque ya tenía caducidad por construcción.

**Los cuatro riesgos de la fase se cerraron por modelado, no por parche:**
- **S15-R3** (vida por selector global) → ancla por invocación.
- **S16** (marca en chunk descargado) → `GlobalPos` en el Attachment, y **desaparece el `forceload`** con el que
  el datapack mantenía un chunk cargado por marca.
- **S17-R3** (caster no-jugador) → ya estaba resuelto en F4.
- **RB-4** (catálogo hardcodeado) → dos JSON, con recuento honesto de lo que falta.

**Correcciones de ficha de F8:** de los 4 campos que pedía `ItemFlags`, **tres ya tenían dueño**; invocar **no
cuesta chakra**; **no hay múltiples marcas** FTG (hay una, y saltar la consume); y la IA shinobi estaba
**literalmente a medias** (`say do nothing`), aunque su ritmo sí estaba decidido.

**Siguiente: F9** (Mundo, narrativa, Seis Caminos, Diez Colas, Árbol Divino) — la cúspide, donde convergen los
hilos que F5, F6 y F7 dejaron abiertos a propósito.

---

## 2026-08-01 · F10/DJT-225 — Los diecisiete ítems que nadie creó nunca

Este ticket no añade contenido: **enciende contenido que ya estaba escrito**. Al empezar había dos catálogos
declarando mercancía que no existe y un recetario de drop-crafting con **9 de sus 16 combinaciones inertes**.

**Lo primero fue contar bien.** El encargo hablaba de «los 8 ítems de F8 pendientes: bomba de humo, ramen ×2,
pergaminos médico/de retirada/de sabio, sello explosivo suelto». Eso son **siete**. El octavo estaba escrito
desde DJT-206, en `CraftingTable.PENDING_NATIVE_RECIPES`, y es el **tanto**: `data/dojutsu/recipes/` tiene 13
recetas, el mod había portado 5, quedaban 8. Manda el datapack.

**El ramen repone 150, y es el hallazgo que más fácil se porta mal.** `chakra/eat_ramen` dice `+100`, así que
lo natural es escribir 100 y pasar. Pero el ramen es `cooked_beef{Ramen:1b}` y **`cooked_beef` está dentro del
tag `dojutsu:food`**, el que dispara `player/eat_food`. Al comer un ramen se cumplen **los dos** advancements
y corren **las dos** funciones: 50 + 100. El ramen no sustituye la reposición base, la suma. Un tercio del
efecto vivía en un archivo que no menciona el ramen por ningún lado.

**La bomba de humo no ciega a nadie.** Uno abre `items/smoke_bomb/smoke.mcfunction` esperando un `blindness` y
no hay ninguno: son 100 partículas `large_smoke` por tick durante 120 ticks y un `redstone_torch.burnout` a 10
bloques. La cortina es **literal** — tapa la vista de quien mire a través, y a quien no le tape no le hace
nada. Portarla con un `MobEffect` habría sido más cómodo de escribir, más fácil de balancear, y habría
cambiado el juego: un efecto de estado ignora las paredes y la geometría, y una nube de partículas no.

**El sello explosivo es una mina, no una granada.** `place` fija `d.extagtimer 200`, y hasta que ese contador
no llega a 0 no se marca `d.primed`. Son **diez segundos** de armado en los que no lo dispara nadie, y esa
espera es la mecánica: quien lo suelte a sus pies y se quede, lo detona él mismo. Después detona con
**cualquier cosa** que entre a 5 bloques —el selector sólo excluye cuadros, marcos, soportes de armadura,
ítems y los demás sellos: todo lo que no se mueve por su cuenta—. Y reparte **dos** daños distintos, que es
fácil leer como uno: la explosión del `tnt_minecart` (con `TNTFuse:0` y sin velocidad, potencia **4.0**
exacta, la misma que el kunai con sello) **y además** un `instant_damage` amplificador 1 en el mismo radio,
que es un golpe mágico que la armadura no para y que la distancia no atenúa.

**El Diez Colas no tiene huevo craftable.** Hay diez `give/tailed_beast/spawn_egg/*` pero
`main/internal/crafting/tb/` tiene **nueve** archivos. El décimo huevo del datapack es puro creativo. Se
registran nueve; registrar el décimo habría creado un ítem sin forma de obtenerse, y su equivalente exacto ya
existe: `/dojutsubeast summon ten_tails`.

### Dos decisiones que valía la pena razonar

**Se dispara al aterrizar, no al soltar.** Los dos predicates (`items/smoke_bomb`, `items/explos_tag`) exigen
`OnGround:1b`. Importa: un sello armado a la altura de los ojos quedaría flotando, y una cortina de humo que
saliera del jugador no taparía una retirada. El datapack lo resuelve recorriendo **todas** las entidades ítem
del mundo cada tick. Aquí el alta es reactiva (`PlayerEvent.DROP_ITEM`, que Architectury 13.0.8 implementa en
los dos loaders — NeoForge por `ItemTossEvent`, Fabric por mixin en `Player#drop`) y sólo entra en la lista de
vigilancia lo que de verdad es una bomba o un sello. El bucle no desaparece —"tocar el suelo" no es un evento
en Minecraft— pero su coste pasa a depender de cuántas bombas hay en el aire, que casi siempre es cero, en vez
de cuántos ítems hay en el mundo. Con un techo de 400 ticks para que un ítem que nunca aterriza no se quede
vigilado para siempre.

**El sello colocado es la primera entidad persistente del mod que no es una criatura.** El `AbilityAnchor` de
F4 encajaba en todo —tick propio, aislamiento de fallos, una sola vía de retirada— salvo en lo único que aquí
importa: `shouldBeSaved()` devuelve `false`. Una trampa se deja puesta y se vuelve a por ella luego, quizá
tras reiniciar el servidor; con un ancla, todas las minas del mundo desaparecerían al descargar el chunk y
nadie lo notaría hasta pisar donde ya no hay nada. El `armor_stand` del datapack persiste, y
`ExplosiveTagEntity` también. La cortina de humo, en cambio, **sí** es un `AbilityAnchor`, y sin reservas: es
transitoria por definición. Es el mismo criterio aplicado dos veces con resultados opuestos, que es como
debería quedar.

### La puerta que DJT-206 dejó escrita a medias

`CraftingGateService` decía por escrito, desde F9: *"De las cuatro, la única cuyo item existe hoy es el
contrato de invocación"*. Ahora existen las cuatro y la puerta está entera:

| Receta | Puerta (escala del mod = datapack ÷ 4) | Además |
|---|---|---|
| `summoning_contract` | fuinjutsu ≥ 75 | — |
| `medical_scroll` | medicina ≥ 75 | — |
| `removal_scroll` | fuinjutsu ≥ **250** | la puerta más alta del juego: la mitad del tope de habilidad |
| `sage_scroll` | sabio **y** senjutsu ≥ 75 **y** energía natural ≥ 50 | **gasta** esos 50, y sólo si sale bien |

La energía natural es lo único que **no** se reescala: `chakra/main` fija `d.max_sagechakra` a 100, igual que
`NaturalEnergyTable.MAX_CHARGE`, así que el 50 del datapack es 50 aquí. Los cuatro reembolsos
(`loot_tables/crafting/items_*`) devuelven la receta exacta, ingrediente a ingrediente.

Y los pergaminos **no se gastan si fallan**: el datapack hace el `clear` dentro de la rama con éxito. Un
pergamino de sabio usado por quien no es sabio avisa en rojo, suena el yunque y se queda en el inventario.

### De propina

El **pergamino médico hace dos cosas**, no una: llena el chakra **y** pone `d.eye_strain` a 0. Es la única vía,
aparte de dormir, para quitarse la fatiga ocular de golpe. Y se añadió la oferta del **Pergamino de Retirada**
al mercader errante (48 esmeraldas, 1 uso, 6 de XP — `setup_trader` línea 15): estaba en el datapack y faltaba
en el catálogo del mod.

**Divergencia anotada, no corregida:** el catálogo del mercader errante del mod y el del datapack **no son el
mismo** (el datapack vende también inyector, píldoras de despertar mental, Kotarō, pergamino de entrenamiento,
Kubikiribōchō, Samehada, Sword of Kusanagi y pergamino de experiencia, y sus ofertas de ojos cuestan 64+32
esmeraldas, no 64). Eso es deuda de DJT-190, no de este ticket; queda escrito aquí para que DJT-223 lo recoja.
De paso: `kubikiribocho` anuncia "Kenjutsu: 800" en el datapack y el mod lo registra con `kenjutsuReq` 0 —
inofensivo hoy (nada lo lee salvo la Samehada), pero es una cifra que se perdió.

**Validación:** `./gradlew build` multiloader **verde**; **930 tests** (+18: los 120 ticks y las 100 partículas
de la bomba, los 200 de armado y la doble fuente de daño del sello, las tres puertas nuevas con sus bordes y
sus reembolsos ingrediente a ingrediente, y que los ids del enum de pergamino son los mismos que los de la
receta y el ítem). NeoForge (21.1.235) y Fabric (0.16.10) arrancan **limpios y con idéntica carga**: 1303
recetas, `2 catálogos (25 ofertas, 12 pendientes de su item)` y `Drop-crafting cargado: 16 combinaciones (0
pendientes de su item)`. Cero líneas ERROR atribuibles al mod; los cinco `No data fixer registered` son el
ruido vanilla de toda entidad custom, ahora cinco porque hay una más. Sin JVMs zombie.

**Sin validar en partida:** todo lo visual y lo que depende de física — que la cortina de humo tape de verdad,
que el sello se vea donde cayó y estalle al acercarse, que los nueve huevos invoquen a su bestia y no a otra,
y que los modelos 3D del tanto y los tres pergaminos se vean bien. Nada de esto lo ve un test ni un servidor
headless (lección de DJT-205: dos versiones pasaron 895 tests en verde sin funcionar en partida). Entra en el
checklist de DJT-223.

### Cómo probarlo

Arranca el cliente (no vale el servidor headless: todo lo que queda por ver es visual o físico):

```bash
./gradlew :neoforge:runClient
```

Ya dentro, en un mundo creativo, los ítems nuevos por el atajo de QA:

```bash
/dojutsuitem egg nine_tails
```

```bash
/dojutsuitem smoke_bomb
```

```bash
/dojutsuitem explosive_tag
```

```bash
/dojutsuitem scroll medical
```

```bash
/dojutsuweapon tanto
```

```bash
/give @s dojutsu:ramen
```

Suelta la bomba de humo (Q) y comprueba que la cortina sale **donde cayó el ítem**, no en tu mano, y que dura
6 s sin aplicar ningún efecto. Suelta el sello, aléjate, espera 10 s y vuelve a acercarte: debe estallar sólo
entonces. Usa el huevo **sobre un bloque** y confirma que sale Kurama, no Shukaku.

Para el crafteo gateado del pergamino de sabio, primero quédate **por debajo** del requisito:

```bash
/dojutsusage become toad
```

```bash
/dojutsusage charge 100
```

```bash
/dojutsuprogression xp senjutsu 1
```

Craftea el pergamino (3 papel / 2 bloques de esmeralda / 1 manzana dorada / 3 sacos de tinta brillante): debe
rechazarlo, devolver esos mismos ingredientes y sonar el yunque. Ahora sube el senjutsu por encima de 75 y
repite: sale el pergamino y la energía natural baja 50.

```bash
/dojutsuprogression xp senjutsu 30000
```

Comprobación del ramen: vacíate el chakra, come uno y confirma que sube **150**, no 100.

```bash
/dojutsuchakra consume 500
```

```bash
/dojutsuchakra get
```

Y el drop-crafting, que es lo que este ticket desbloquea: tira al suelo, juntos, una estrella del Nether, un
bloque de diamante, un bloque de oro y lana naranja → sale el huevo del Nueve Colas.

---

## 2026-08-01 · F10/DJT-223 — La primera vez que se arranca el cliente

Nueve fases, ~500 clases, 930 tests, y todo se había validado con el **servidor dedicado**. Eso deja fuera, por
construcción, la mitad del mod: renderers, keybinds, HUD, pantallas y el atlas de texturas no existen en un
servidor. Este ticket arranca el cliente por primera vez.

### Lo que salió bien, y no era evidente

**Los dos loaders arrancan el cliente, entran en un mundo y no se cae nada.** Con
`--quickPlaySingleplayer` el cliente carga directo en un mundo de pruebas (`run/saves/dojutsu-qa`, creado
copiando el mundo del servidor dedicado), el jugador entra, el `OnboardingController` lo mete en
`SELECTING_CLAN`, llega el sync completo y el chakra empieza a regenerar. Se observó **+3 cada 120 ticks** y el
clamp exacto en 100/100: la tabla de `[S02]`, viva, en el cliente.

**Cero modelos y cero texturas sin resolver.** ~200 modelos de ítem cargados en los dos loaders sin un solo
`Unable to load model` ni `Using missing texture`. Es la primera confirmación real de que el porte de assets de
DJT-224 y los 19 PNG de DJT-225 están bien enlazados — hasta hoy eso era una suposición, porque un servidor
dedicado ni siquiera lee `assets/`.

**Los 6 renderers quedan registrados en ambos loaders.** La regla 5 dice que una `EntityType` sin renderer
revienta el cliente; nunca se había comprobado, y el `explosive_tag` de ayer era el sexto. No revienta.

**Y la GUI se ve.** La pantalla de creación del ninja se dibuja entera: título, subtítulo, «Random Clan» y los
**14 clanes**, cada uno con su icono y su color, con tooltips al pasar el ratón. El HUD (barra de chakra y
rango) se dibuja sobre el mundo y por debajo de la pantalla, que es donde toca.

### Lo que no se pudo automatizar, y por qué se deja escrito

Se intentó conducir la partida inyectando teclado y ratón en la ventana. **El movimiento del ratón llega**
—aparecen los tooltips de clan— pero **las pulsaciones no**: GLFW ignora la entrada sintética de `SendKeys`, y
`SetForegroundWindow` desde un proceso en segundo plano falla de forma intermitente en Windows, con lo que la
captura acaba fotografiando **otra ventana del escritorio**. Se abandonó la vía y se borró lo capturado.

La conclusión operativa importa más que el detalle técnico: **la verificación interactiva de este mod la tiene
que hacer una persona**. No es una limitación temporal que se pueda rodear con más ingenio; es la misma lección
de DJT-205 (dos versiones con 895 tests en verde que no funcionaban en partida) vista desde el otro lado. Por
eso el entregable de este ticket es un **checklist ejecutable**, no un informe de «funciona».

### La migración desde saves ya no es una suposición

La auditoría avisaba: se enchufaron **16 sub-estados** a lo largo de F1–F9 **sin subir nunca**
`CURRENT_DATA_VERSION`, y la cadena de migración está construida y sin estrenar. El argumento era «funciona
porque los codecs son opcionales». Cierto — pero nadie había cargado nunca un NBT de la época de F1 contra el
agregado de hoy.

Ahora sí: `LegacySaveMigrationTest`, 6 tests. Un save de F1 (sólo `onboarding` y `chakra`, **sin**
`dataVersion`) carga, conserva lo que traía, y **los 14 sub-estados que F1 no conocía caen a su `DEFAULT`**.
Migrar es idempotente, y un save de una versión *futura* no se degrada. Lo valioso no es que pase hoy: es que
el día que alguien añada un sub-estado con un campo **obligatorio**, ese test se pone rojo **antes** de que se
rompa la partida de nadie.

Sigue pendiente, y no bloquea: `CURRENT_DATA_VERSION` es 1 y la cadena tiene un solo paso identidad. Está bien
mientras todo sea aditivo; el primer cambio incompatible obliga a subir versión y escribir el paso de verdad.

### Paridad de adaptadores: auditada, y sin sorpresas

Se comparó archivo a archivo lo que hay en `fabric/` y en `neoforge/`. Los dos llaman a `init()` + `initClient()`
por el mismo camino, registran los **6** renderers, los **11** keybinds (con sus 12 claves de idioma, ninguna
huérfana) y los mismos eventos nativos: dormir por evento en ambos, comer por evento en NeoForge y por mixin en
Fabric, exactamente como documenta `[ADD/05]`. **No se encontró ninguna asimetría.** Los logs de carga de los
dos son idénticos línea a línea.

### Dos cosas que sí conviene mirar

1. **`Delta recibido antes del primer full; ignorado.`** Al entrar llega un delta S2C **antes** del full, en los
   dos loaders. Hoy es inofensivo —`ClientNinjaData` lo descarta a propósito y el full llega justo detrás— pero
   significa que algo emite un delta durante el join antes de que se haya mandado el estado completo. Si algún
   día el full tardara más, ese cambio se perdería sin ruido.
2. **El chakra regenera durante el onboarding**, con el ninja todavía sin crear. Es coherente con el datapack
   (el tick corre para todo jugador conectado), pero conviene que sea una decisión y no una omisión.

**Entregable:** `docs/checklist_paridad_cliente.md` — qué está verificado sin jugar y qué no puede estarlo, la
sesión de QA montada (mundo `dojutsu-qa` en los dos loaders + los 20 comandos con su sintaxis real), los cinco
bloques prioritarios de lo que **nunca se ha visto** (renderers de criaturas, menú ocular y Susanoo, Árbol
Divino por etapas, diálogo del Sabio, formas de jinchuriki) y una fila por sistema S01–S25 con casilla por
loader.

**Validación:** `./gradlew build` multiloader **verde**; **936 tests** (+6 de migración). Cliente y servidor
arrancados en NeoForge 21.1.235 y Fabric 0.16.10; sin JVMs zombie.

### Cómo seguir

```bash
./gradlew :neoforge:runClient --args="--quickPlaySingleplayer dojutsu-qa"
```

```bash
./gradlew :fabric:runClient --args="--quickPlaySingleplayer dojutsu-qa"
```

Ya dentro, y antes de nada:

```bash
/gamemode creative
```

Y a partir de ahí, `docs/checklist_paridad_cliente.md` §2 en orden: es lo que menos se ha visto y lo que más
superficie visual tiene.

---

## 2026-08-01 · F10/DJT-226 — Deuda de sistemas: el arnés, el Rinne Sharingan y las nueve andanadas

Tres cosas sin relación entre sí salvo la de siempre: alguien las dejó a medias y lo anotó por escrito.

### 1. Los dos modos que se quedaron fuera de su propio arnés

`ToggleableModeService` (F6/DJT-130) se extrajo **precisamente** del bucle que repetían `SusanooService` y
`TenseiganChakraModeService` — su javadoc lo dice con nombre y apellidos—, y los dos se quedaron sin adoptarlo.
Peor: `TenseiganChakraModeService` llevaba escrito *"cuando F6 traiga sage/sellos/puertas/SOAH conviene extraer
un arnés compartido; hasta entonces se mantiene el patrón"*. F6 lo trajo, cinco modos lo adoptaron, y estos dos
siguieron con su `Map<UUID, Mode>` a mano durante cuatro fases.

Ya no. **Siete de siete modos** usan el arnés.

**Antes de tocar nada, una pregunta de orden.** El arnés emite `ModeToggledEvent` en momentos ligeramente
distintos a los dos servicios (publica antes de que el Susanoo invoque su avatar, por ejemplo). ¿Rompe eso a
alguien? Serena (`find_referencing_symbols`) responde en un segundo lo que grep no: **nadie se suscribe a
`ModeToggledEvent`**. Sólo hay publicadores. Así que el refactor es neutro por construcción, y de paso queda
anotado que tenemos un evento de dominio sin un solo consumidor.

**El Susanoo no usa `toggle()`, y es correcto que no lo use.** Una pulsación sobre el Susanoo no es «enciende o
apaga»: son **tres** respuestas (manifestar / crecer una etapa / apagar). El arnés expone `activate` y
`deactivate` por separado justo para casos así, de modo que la escalera de etapas —lo único realmente propio de
este servicio— se queda intacta y lo que desaparece es el hospedaje.

**Un cambio de comportamiento, y es una corrección.** Cuando el Tenseigan Chakra Mode **caía** por chakra
agotado avisaba al jugador pero **no emitía** `ModeToggledEvent`; el Susanoo, en el mismo caso, sí. Ahora lo
emite el arnés, igual para los siete. Hoy es inerte —nadie escucha— pero deja de ser una diferencia arbitraria
que alguien habría heredado al escribir el octavo modo.

Neto: −60 líneas de hospedaje duplicado, misma API pública, mismos tests.

### 2. Las nueve técnicas insignia son la misma técnica

`BeastMoveRegistry` registraba **una** técnica. El plan de IA (`BeastAiPlan`) puede pedir la insignia de
cualquier bestia en tres de sus cuatro slots útiles, así que ocho de cada nueve bestias no hacían nada en la
mayoría de sus turnos.

Al abrir las nueve carpetas del datapack aparece el hallazgo del ticket: **son idénticas**. Mismos doce pasos de
0,1 bloques por tick, misma cuenta atrás de tres ticks antes de salir, mismo `instant_damage` amplificador 1 en
radio 5, misma vida. Lo único que cambia son **las partículas** — y un sonido de chapoteo en la del Tres Colas.

Así que hay **una** clase (`SignatureBarrageMove`) parametrizada por bestia, no nueve. Es el caso de manual de
RA-1 («una clase por familia, nunca por técnica»): portarlas literalmente habría multiplicado por nueve la
superficie de mantenimiento de un solo comportamiento.

**Y el temporizador no era un enfriamiento: era el alcance.** El `d.<N>t_<xx>timer` vive en la **bestia**, no en
el proyectil, y cuando llega a 0 el barrido de `tailed_beasts/main` le pone `d.hit` al disparo — o sea que lo
**detona**. Es la vida máxima del proyectil. Con la carga de tres ticks descontada, el alcance real es
**8,4 bloques**: son técnicas de corta distancia, coherentes con llamarse «perdigonada» y «andanada». Leerlo como
cooldown habría dado un proyectil de alcance infinito.

**El Nueve Colas vuelve a romper el patrón** —ya lo hizo en la XP de sellado (DJT-165)—: su timer vale **20** y
no 10, así que su andanada llega el doble de lejos que la de sus ocho hermanas.

Quedan fuera, y se anotan con sus números ya verificados para que el próximo ticket sea trivial: **Tenpenchii**
(Diez Colas: 3 rayos por objetivo en radio 10 + `instant_damage` amplificador 3 + trueno + 1200 t de
enfriamiento), **Truth Seeking Orb** y la **Bola Bestia con Cola del Diez Colas**.

### 3. El Rinne Sharingan tenía el roster del Rinnegan copiado

`rinne_sharingan.json` declaraba **exactamente** las seis habilidades del `rinnegan.json`. El ojo que corona el
juego —el que concede el Árbol Divino al florecer— no tenía nada propio.

En el datapack es un **menú de tres páginas**: la suya (Shinra Tensei + Amenominaka), la del Sharingan/Mangekyō
y la del Rinnegan, y se cambia de página con agacharse + slot del hotbar. Pero el mod tiene un **menú de
verdad** desde DJT-117, así que las páginas no se portan: **se disuelven**. El roster del Rinne Sharingan es la
**unión** de las tres, que es lo que el jugador podía alcanzar de todos modos, sin el rodeo de navegar.

**Amenominaka** es lo nuevo, y no es un teleport:

- Cuesta **8000 de chakra** —cuatro veces el Shinra Tensei, la técnica más cara del juego— y no se entiende
  hasta ver el selector: `as @a[distance=0..10] in <dimension> run tp @s ~ ~ ~`. No te llevas a ti: te llevas a
  **todos los jugadores en 10 bloques**, contigo, a las mismas coordenadas de otra dimensión. No es movilidad,
  es secuestrar una pelea entera.
- **La vuelta a casa se gana al irse**: la posición de regreso se graba sólo al **salir del Overworld**, y
  volver al Overworld la borra. Por eso «Posición original» aparece tachada hasta que te has ido de verdad.
- **Sin `forceload`** (mismo arreglo que la marca del Dios del Trueno en DJT-180): el datapack invoca un
  `marker` y **mantiene su chunk cargado** mientras tengas un regreso pendiente. Aquí el origen es un
  `GlobalPos` en `EyeState`: cuesta cero y sobrevive a los reinicios, cosa que el marker no hacía si alguien
  limpiaba entidades.

Los cuatro destinos son cuatro entradas del menú servidas por una sola clase parametrizada. Y `canApply` aborta
**sin cobrar** si el salto no tiene sentido (ir a donde ya estás, o pedir el regreso sin tenerlo), que es el
equivalente exacto de las entradas tachadas del datapack.

**De propina:** la técnica usa `dojutsu.rinne_sharingan.amenominaka`, uno de los **20 sonidos registrados que
no tocaba nadie** según la auditoría. Quedan 19.

**Validación:** `./gradlew build` multiloader **verde**; **947 tests** (+11: que las nueve comparten todos los
números menos la vida, que el Nueve Colas llega el doble de lejos, que el alcance real son 8,4 bloques, que el
Diez Colas no tiene andanada, y que el origen del Amenominaka sobrevive al round-trip NBT y a **los ocho**
withers de `EyeState` — si alguno lo perdiera, volver a casa se rompería en silencio). NeoForge y Fabric
arrancan limpios y registran ahora **10 técnicas de bestia**, no 1. Sin JVMs zombie.

**Sin validar en partida:** que las andanadas se vean y peguen, que el Amenominaka arrastre de verdad a quien
tengas cerca y que el menú del Rinne Sharingan liste las 13 entradas. Va al checklist de DJT-223 §2.1 y §2.2.

---

## 2026-08-01 · F10/DJT-222 — Rendimiento: lo que se corrigió y lo que se decidió no tocar

**El resultado principal de este ticket es un "no".** Se auditaron los **21 puntos de entrada por tick** del mod
—18 `PlayerTickSystem` más 3 en `SERVER_POST`— y **ninguno está sin acotar**: diecisiete de los dieciocho salen
sin hacer nada en el caso común, y el que siempre trabaja (`RegenService`) es el que tiene por cometido trabajar
siempre. La regla 11 del proyecto («reactivo, nunca polling») se ha pagado sola.

Así que el ticket se convirtió en dos cosas: tres correcciones de higiene con números, y **dejar por escrito qué
no se va a optimizar y por qué**, que en un proyecto de este tamaño vale más que las correcciones.

### La escala, antes que nada

Un tick son **50 ms**. Con 20 jugadores, un sistema que cueste **1 µs por jugador** consume el **0,04 %** del
presupuesto. Sin esa cifra delante, cualquier micro-medición parece urgente. Con ella, casi ninguna lo es — y
eso es lo que dicen los números de abajo.

### Las tres correcciones

**1. Un `new Random()` por jugador y por tick, en dos servicios.** `AwakeningService` y `EvolutionService`
construían un `java.util.Random` **nuevo cada tick para cada jugador** para un azar que se tira **una vez en la
vida de un ninja**: el 50/50 de Otsutsuki y la variante de Mangekyō. Y no es sólo la asignación — el constructor
sin argumentos llama a `System.nanoTime()` y hace un **CAS sobre un `AtomicLong` estático de la JVM**. Se pagaba
una barrera de memoria 40 veces por segundo por jugador para no usar el resultado. Ahora comparten un
`ThreadLocalRandom.current()`, que no asigna y ya es un `RandomGenerator`. **El parámetro sigue inyectado**: los
tests le siguen dando semilla fija, así que no se cambió testabilidad por velocidad.

**2. `EvolutionService` trabajaba para jugadores sin ojo.** Construía el predicado de flags (un lambda que
captura), un `Random`, y entraba a recorrer el grafo **para que `evaluate` devolviera exactamente lo que le
entró**. Ése es el caso de casi todos los jugadores casi todo el rato. Una guarda de una línea. Los cinco
servicios hermanos ya tenían la suya; éste era el único que faltaba.

**3. Un `String` por entidad y por barrido, en la chakra vision.** `ChakraVisionRelations` vive en `domain` y
por eso **no puede conocer `EntityType`**: su clave es texto. Traducirlo dentro del bucle construía un `String`
con `EntityType.getKey(...).toString()` **por cada entidad y cada barrido**, y ese bucle recorre un cubo de 30
bloques de radio cada 10 ticks por cada jugador con la visión encendida — en una aldea son cientos de entidades.
Ahora se memoiza **por tipo**: el `String` se construye una vez y lo demás es una consulta de mapa. El cache lo
acota el registro de tipos (fijo tras el arranque) y la tabla de dominio **sigue siendo pura**, que era el
motivo de que la clave fuese texto. Es la de más recorrido de las tres, porque su coste escala con las
**entidades del mundo**, no con los jugadores.

### Los números, y lo que de verdad significan

| Camino | Antes | Ahora | Factor |
|---|---:|---:|---:|
| Azar del glue | 35 ns/op | 5 ns/op | 7,0× |
| Tick de jugador sin ojo | 32 ns/op | 3 ns/op | 10,7× |

Siete y once veces más rápido suena a titular. La cifra honesta es la otra: pasan de **0,0014 %** de un tick con
20 jugadores a **0,0002 %**. Son correcciones correctas, no un rescate — y decirlo así es parte del entregable.

### El eje de escalado real

Lo que puede degradar un servidor no es el trabajo constante por jugador, que está acotado y medido. Son los
**barridos de entidades**, cuyo coste crece con lo que haya en el mundo: la chakra vision (radio 30, cada 10
ticks, sólo con la visión encendida), el drop-crafting (radio 16 por jugador, cada 5 ticks, con emparejamiento
O(n²) dentro del racimo) y los goals de la IA.

**No se han tocado, a propósito.** Los tres ya están limitados por intervalo y por puerta, y son la traducción
de un comportamiento que en el datapack corría **cada tick sobre todo el mundo**: el porte ya los mejoró una
vez. Tocarlos ahora sería optimizar sin una medición que lo justifique, y la medición que haría falta —MSPT con
muchas entidades y varios jugadores— **sólo se puede tomar en partida**. Queda como ítem de DJT-223.

### Lo que se decidió no optimizar

Está en `docs/perf.md §4` con su razón cada uno, pero el resumen: `RegenService` asigna un `EnumSet` y varios
`Optional` por jugador y tick (~2 000 objetos efímeros por segundo con 20 jugadores: para el generacional eso es
un incremento de puntero); los sub-estados inmutables se copian al escribir (regla 10b, se paga a propósito); el
pipeline recorre los 18 sistemas por jugador (360 llamadas con guarda barata por tick). Ninguna de las tres
merece la complejidad de arreglarse.

### El banco de pruebas

`TickHotPathBenchmark`, **`@Disabled`** a propósito: mide tiempo, y un test que mide tiempo falla en CI el día
que la máquina va cargada. Se activa con `-Pbench` (una condición nueva en `common/build.gradle`) e imprime
ns/op **y** el porcentaje de tick que eso supone — precisamente para que el próximo no optimice ruido.

**Validación:** `./gradlew build` multiloader **verde**; **949 tests** (947 + los 2 del banco, omitidos salvo con
`-Pbench`), 0 fallos. NeoForge y Fabric arrancan limpios con la misma carga. Sin JVMs zombie.

**Sin validar en partida:** el MSPT real. Es la misma frontera de DJT-223 y por el mismo motivo.

**Cómo repetir la medición:**

```bash
./gradlew :common:test --tests "*TickHotPathBenchmark*" -Pbench
```

---

## 2026-08-01 · F10/DJT-221 — Balance: los cinco números que nadie había mirado

RA-4 dice «no improvisar números» y remite a `balance/xp_sources.json`, donde quedaban **16 `TBD_datapack`**.
Este ticket los resuelve todos y, al hacerlo, encuentra que una familia entera de umbrales estaba **inventada**.

### El hallazgo

`unlocks/progression.json` decía por escrito que sus umbrales eran «provisionales». Al contrastarlos uno a uno
contra `predicates/unlocks/*.json`, la mayoría resultaron correctos —taijutsu, fuinjutsu, genjutsu y medical
cuadraban— **salvo los cinco de kenjutsu**, que eran `5 / 10 / 20 / 30 / 40`. Los reales son
**75 / 88 / 125 / 150 / 350**. No estaban mal por poco: la quinta técnica de espada se desbloqueaba a nivel 40
en vez de a 350, casi nueve veces antes de tiempo. Eran números redondos escritos a mano, y se notaba.

### La escala, por fin escrita

La convención «datapack ÷ 4» venía usándose desde DJT-082 sin justificación. Ahora la tiene, y es sólida: el
techo de habilidad del mod es **500** (`XpCurve`) y los cuatro requisitos más altos del datapack valen
exactamente **2000** — maestría de las Ocho Puertas, tercer sello de fuinjutsu, Blaze y Creation Rebirth SOAH.
2000 ÷ 4 = 500. **No son dos escalas, es la misma con distinto zoom**, y el tope coincide en las dos.

Lo que **no** se reescala, y confundirlo sería el error fácil: **chakra máximo, ninja_xp y contadores de uso**
(`d.<eye>_use`) valen lo mismo en los dos lados. Queda escrito en `xp_sources.json` y en el javadoc de
`UnlockDefinition`.

### Los 25 hitos, cuantificados

`main/internal/unlocks.mcfunction` da la ninja_xp de cada hito y **no es uniforme**: va de **1** (Sustitución) a
**100** (Izanagi, primera de las Ocho Puertas, Sello de la Fuerza de Cien). El mod **no daba ninguna**. Ahora
cada hito la declara y `XpApi` la paga al otorgarse — por la puerta única de la regla 10.

Y faltaban hitos enteros: las **nueve** puertas (incluida la maestría), el contrato de invocación, el kunai del
Dios del Trueno, la Sustitución, Creation Rebirth y su versión SOAH. El recetario pasa de **23 a 36**.

También se corrigieron dos desalineaciones de la lista médica —faltaba `healing` (medical 20 → 5) y todo lo
demás iba corrido una posición— y se retiró un `genjutsu_2_phantom_sound_chains` que **no existe** en el
datapack.

### Un bug del datapack que se porta tal cual

`flower_hill` se comprueba con `gj_req_2`, el predicate de `bringer_of_darkness`: **no existe ningún
`gj_req_3`**. Los dos genjutsus salen a la vez, a genjutsu 70. Es un copy-paste del original, y se porta como
está —con la nota puesta— porque cambiarlo sería rebalancear, no portar.

### El punto ciego del modelo reactivo

Cuatro hitos del datapack exigen **dos cosas a la vez**: Creation Rebirth (medical 500 **y** 10 000 de chakra
máximo), su versión SOAH (2000 y 30 000), el kunai del Dios del Trueno (fuinjutsu 1000 y 10 000) y el acceso a
Senjutsu (sólo chakra).

El mod otorga hitos **reaccionando a `SkillLevelChangedEvent`**, que sólo ve el **instante** del cruce de nivel.
Si el chakra máximo llega *después* de la habilidad —y llega después casi siempre, porque sube al sellar una
bestia—, ese instante ya pasó y el hito **no se otorgaría jamás**. El datapack no tenía el problema porque
revisaba los hitos cada tick.

Se recupera la garantía sin volver al polling: un **barrido de puesta al día al entrar**
(`UnlockService.onJoin`), O(hitos) una vez por sesión en vez de por tick. Va con test propio, que es el caso
exacto: subir la habilidad con poco chakra, sellar una bestia, entrar, y comprobar que el hito aparece.

De paso, la Sustitución —que depende del **ninja_xp global**, no de una habilidad— gana su disparador
suscribiendo `NinjaXpChangedEvent`. Es el primer jutsu que recibe un ninja (a 1000 de ninja_xp = Genin) y
estaba fuera del recetario.

### Lo que queda fuera, con su motivo

- **Izanagi**: exige `aff_yin >= 1000` **y** `aff_yang >= 1000` **y** una referencia a otro predicate. El
  esquema expresa una habilidad + chakra; esto necesita requisitos compuestos de verdad.
- **Acceso a Senjutsu**: es un hito **sólo** de chakra máximo (6000), sin habilidad que lo dispare. Y en el mod
  nada impide ya gastar puntos en Senjutsu, así que el hito sería únicamente el aviso.
- **Blaze por afinidad de fuego 2000**: en el mod el KG se concede por `EyeDefinition.awakenKekkeiGenkai`
  (Mangekyō de Sasuke); la vía por afinidad no está portada.

Los tres están anotados en `xp_sources.json` con su requisito exacto, así que quien los retome no tiene que
volver al datapack.

**Validación:** `./gradlew build` multiloader **verde**; **956 tests** (+7: el co-requisito de chakra en sus dos
bordes, el barrido que recupera el hito que el evento no pudo ver, la idempotencia, que la ninja_xp por hito no
es uniforme, que la Sustitución no la dispara ninguna habilidad, y que la escala cuadra en el tope). NeoForge y
Fabric arrancan limpios y cargan **36 hitos**, no 23. Sin JVMs zombie.

**Sin validar en partida:** que los umbrales se sientan bien jugando. Eso no lo dice ningún test — lo único que
este ticket garantiza es que **los números son los del datapack**, que es una pregunta distinta de si el
datapack estaba bien balanceado.

---

## 2026-08-01 · F10 — Primera tanda de la verificación en partida: siete puntos

La primera pasada del checklist de DJT-223 con una persona jugando. Siete observaciones, y el reparto dice
mucho: **cinco eran fallos reales**, uno era una **petición de diseño** y uno era **comportamiento correcto que
parecía fallo**. Ninguno lo podía haber encontrado un test.

### 1. Las bestias con cola no recibían daño — y no era la armadura

El síntoma que reportó el usuario fue «no parecen tener hitbox». Exacto.

`TailedBeastRenderer.scale` dibujaba la bestia a `1.4 + 0.2 × (colas − 1)`, pero la caja de colisión se quedaba
en el `sized(2.0F, 2.6F)` del `EntityType`, que es **fijo para las diez**. Un Nueve Colas se dibujaba a escala
**3,0** —casi seis bloques de ancho— con dos bloques de caja pegados a los pies. Le pegabas al cuerpo que veías
y no había nada que golpear.

**El error no era el número: era tenerlo en dos sitios y aplicar sólo uno.** Ahora la escala vive en
`BeastStats.renderScale` y la leen los dos — `getDefaultDimensions` del entity y `scale` del renderer—, así que
volver a separarlos exige borrar una función compartida. La caja real del Nueve Colas pasa de 2,0 a **5,85** de
ancho.

De paso: `getDimensions(Pose)` es **final** en `LivingEntity`; el gancho que se puede sobrescribir es
`getDefaultDimensions(Pose)`, que es `protected`. Un `javap -p` lo resuelve en un segundo y evita pelearse con
el compilador.

### 2. El Amaterasu no era negro

`ParticleTypes.FLAME` — naranja. La llama más famosa de la serie salía del color equivocado.

El datapack no usa **ninguna** partícula de fuego: usa `dust` con RGB casi cero, en dos capas
(`sasuke/hit_particles`): un núcleo denso a ras de suelo (`dust 0.05 0.05 0.05 0.9`) y un halo más fino y con un
punto de azul por encima (`dust 0.02 0.03 0.03 0.5`). El negro puro no se ve contra un bloque oscuro; ese
casi-negro sí, y es lo que le da aspecto de llama. Portado literal.

### 3 y 4. El botón del Mangekyō Eterno salía donde no debía y no decía nada

Dos observaciones distintas, un solo origen. El botón se dibujaba con **cualquier** ojo activo porque el cliente
no sabe si el ojo es eternalizable —el `EyeRegistry` vive en el servidor— y se dejaba que el servidor rechazara
al pulsar. En partida eso son dos problemas: un botón de «despertar» permanente sobre el **Rinnegan** y el
**Rinne Sharingan**, que no tienen forma eterna, y **ninguna pista** de qué hacía falta en los que sí.

Ahora el servidor manda el estado (`AwakenOutcome`) junto con la vista del menú:

- `NO_ETERNAL_EYE` / `ALREADY_ETERNAL` → **el botón no aparece**.
- `AWAKENED` → botón activo.
- `NEEDS_MEDICAL` / `WRONG_CLAN` / `NEEDS_DEVELOPMENT` → botón **desactivado, diciendo qué falta**
  («needs Medical Ninjutsu 150», «needs an Uchiha or Otsutsuki bloodline», «keep the eye active for longer»).

El servidor sigue validando al pulsar: esto es presentación, no una puerta. Y responde a la pregunta que el
usuario hacía —«no está claro cómo evolucionar a Eternal Mangekyō»— en el sitio donde se hace, no en la wiki.

### 5. Faltaban los comandos de jinchūriki

`/dojutsubeast` tenía `summon`, `info`, `killall`, `seal`, `extract` y `registry`, pero no forma ni Baryon: se
podía sellar una bestia por comando y luego había que ir a buscar la tecla **K** y adivinar que corriendo se
alterna el Baryon. Se añaden `/dojutsubeast form` y `/dojutsubeast baryon`. El Baryon avisa en su respuesta de
que es **terminal**.

### 6. Interruptor de caminar sobre el agua — divergencia consciente

**El datapack no tiene interruptor**: caminar sobre el agua es automático y la única salida es mantenerse
agachado. En partida eso obliga a sostener el sneak durante todo un río.

Se añade la tecla **U** con el mismo patrón que el `d.no_cv` de la chakra vision, que sí es fiel: preferencia
persistente en `VisionState`, el cliente sólo manda la intención y el servidor decide. Agacharse **sigue
funcionando igual** — el interruptor no lo sustituye, lo complementa. Queda anotado como divergencia en el
javadoc de `VisionState` y de la tecla.

### 7. El stun del Sharingan no cuesta nada — y así es correcto

`sharingan/stun/cast.mcfunction` **no toca `d.chakra` ni `d.eye_strain`**: lo único que fija es
`d.sharstun_cd`. El Genjutsu Stun del Sharingan base está gateado **sólo por enfriamiento** (200 t), y el mod ya
lo portaba así desde DJT-119 —está escrito en su javadoc—. No es un fallo: es el datapack.

Vale la pena decirlo porque es exactamente el tipo de cosa que se «arregla» por instinto y rompe la fidelidad.
Si se quiere cambiar, es una decisión de balance (DJT-221), no una corrección.

### Balance de la tanda

Cinco fallos reales de siete observaciones, y **ninguno** era detectable sin jugar: dos eran de render, dos de
UI y uno de andamiaje. Es la confirmación práctica de lo que decía DJT-223 — el build verde y los 961 tests no
ven nada de esto.

El más instructivo es el de la hitbox, porque el bug no estaba en ninguna de las dos líneas: estaba **en que
eran dos**.

**Validación:** `./gradlew build` multiloader **verde**; **961 tests** (+5: que la escala crece por cola, que la
caja crece con ella, y que el Nueve Colas es casi el triple de ancho que la caja fija que tenía). NeoForge y
Fabric arrancan limpios. Sin JVMs zombie.

**Pendiente de volver a mirar en partida** (va al checklist): que la bestia reciba golpes de verdad, que el
Amaterasu se vea negro, que el botón del EMS aparezca sólo en los Mangekyō y con su motivo, y que la tecla U
haga lo que promete.

---

## 2026-08-01 · F10 — Deuda de fondo: el commit, los 26 jutsus inalcanzables y la puerta de permisos

Tres cosas que no eran tickets pero pesaban más que varios de los que sí lo eran.

### 1. Nueve fases vivían sólo en el working tree

El último commit era **F0, del 12 de julio**: andamiaje y nada más. Todo F1–F10 —unas 500 clases, 128 jutsus,
15 ojos, la documentación entera— existía únicamente como archivos sin versionar. Un `git clean` mal escrito, un
disco lleno o una carpeta movida y se perdían nueve fases.

Commiteado en la rama **`f1-f10-port`**: 1328 archivos, 1302 altas, 19 modificaciones y 7 bajas. Las bajas son
el andamiaje de F0 que ya tenía sustituto y estaba documentado como retirado: el ping-pong de red (DJT-005), el
attachment de prueba (DJT-004) y `DebugMarkerEntity` (retirado en DJT-072). El `.gitignore` ya cubría
`build/`, `run/`, `.gradle/` y `.serena/`, así que no entró nada que no fuera fuente, recursos o documentación.

Va en rama y no directo a `main` a propósito: es un commit de 1328 archivos y conviene que quien lo integre lo
vea antes. Un `git merge --ff-only f1-f10-port` lo pone en `main` cuando se quiera.

### 2. Los 26 jutsus que nadie podía conseguir

**El agujero más grande que quedaba, y llevaba abierto desde F2.** 26 de las 128 técnicas existían como datos
—con su efecto, su coste y, desde DJT-221, su umbral— y **no había forma de obtenerlas en partida**. Las
secuencias del Skill Learner cubren las naturalezas y los Kekkei Genkai; siete categorías se quedaban fuera:
aburame (2), fuinjutsu (3), genjutsu (6), kenjutsu (5), medical (4), taijutsu (4) y universal (2).

La causa eran **tres `TODO F4` distintos** esperando exactamente lo mismo: `UnlockService` (hitos),
`ClanService` (jutsu inicial del clan) y `ClanJutsuUnlockService` (desbloqueo progresivo). Cada uno registraba
el id, avisaba por chat o por log, y ahí se quedaba.

Se escribe **una vez**, en `JutsuGrantService`: marca la técnica como conocida y pone el ítem en el inventario
(al suelo si está lleno). El datapack tenía **145 funciones `give/jutsu/*`**, una por técnica, con el NBT
copiado a mano en cada una.

**Qué entrega cada cosa, verificado contra `main/internal/unlocks.mcfunction`:**

- **23 técnicas por hito** — declaradas en el JSON (`grantsJutsus`), no derivadas del id del hito por cirugía de
  cadenas. Es una **lista** porque el datapack tiene un caso de dos: la octava puerta da el Elefante Nocturno y
  el Night Guy a la vez.
- **`insect_sphere`** al unirse al clan Aburame (`startingJutsu`, que ya estaba declarado en datos desde F3 y
  nadie leía).
- **`insect_tornado`** por el temporizador del clan (`unlockJutsu`, campo nuevo). El datapack entrega **una por
  vuelta** (`unless predicate has_<clan>_jutsu`), no todas de golpe, y así se porta.

**Los hitos que no dan técnica es correcto que no la den**: las Ocho Puertas dan una *capacidad*, el contrato y
el kunai del Dios del Trueno dan *items*. Hay un test que lo fija, para que nadie «arregle» eso más adelante.

**La entrega es tolerante.** Un id sin jutsu se salta con aviso en vez de romper. Hace falta de verdad: el clan
Kaguya declara `dance_of_the_camellia` y el Nara `shadow_paralysis`, y **ninguno de los dos está portado**. Es
el mismo patrón del catálogo de comercio y el drop-crafting — los datos declaran el surtido completo y el código
cuenta lo que falta.

**Queda uno solo fuera de alcance, y también en el original:** `phantom_sound_chains` no tiene ninguna función
`give/` en el datapack. Sólo sale de `jutsu/get_random`, o sea que en el datapack tampoco se puede pedir. Se
deja igual.

Y hay un test que comprueba que **toda técnica prometida por un hito existe como archivo**. Una errata ahí no
rompería el arranque —la entrega es tolerante— y el jugador simplemente no recibiría nada, en silencio, meses
después. Ese es justo el fallo que ningún test veía.

### 3. Los veinte comandos de depuración estaban abiertos a cualquiera

Sin una sola puerta de permisos: **0 de 20** usaban `requires`. En un servidor, cualquier jugador podía
invocarse un Nueve Colas, sellárselo, ponerse la maestría del Susanoo al máximo o fijarse la fatiga ocular.

Ahora todos pasan por `DebugCommands.root(...)`, que aplica el **mismo nivel de operador (2)** que
`/dojutsu reset_ninja`. Es el nivel de los comandos de trampa de vanilla (`/give`, `/summon`), que es
exactamente lo que hacen estos. Y un comando nuevo hereda la puerta por construcción — que es el punto de tener
una raíz común en vez de veinte `requires` copiados que alguien acabará olvidando.

En un mundo de un jugador el anfitrión suele ser operador, así que la sesión de QA no cambia. Lo que cambia es
lo que pasa cuando entra alguien más.

**Validación:** `./gradlew build` multiloader **verde**; **964 tests** (+3: que toda técnica prometida existe,
que son 23, y que los hitos sin entrega son exactamente los que dan otra cosa). NeoForge y Fabric arrancan
limpios. Sin JVMs zombie.

**Sin validar en partida:** que el ítem llegue de verdad al inventario al cruzar un umbral, y que el Aburame
reciba su segunda técnica a los 65 minutos. Va al checklist.

---

## 2026-08-01 · F10/DJT-220 — La API pública, y por qué llegó la última · **FASE 10 CERRADA**

El último ticket del proyecto. La ficha pedía «superficie pública estable (`DojutsuApi` + eventos de registro de
jutsu/ojo/clan/contrato) versionada», y validarla con «un mod de prueba que registra un jutsu sin tocar el core».

### Se dejó para el final a propósito, y valió la pena

Una superficie pública es una **promesa**. Prometer sobre sistemas que aún no se habían visto funcionar habría
sido firmar a ciegas — y la primera sesión de verificación en partida, dos tickets antes de éste, encontró
**cinco fallos reales** que ningún test veía, incluido que las Bestias con Cola no tenían caja de colisión donde
se las dibujaba. Si la API se hubiera congelado en su hueco «natural» del backlog, habría congelado eso.

Lo que se publica ahora ya se ha ejecutado, mirado y corregido.

### El contrato, en una frase

Lo que está en `com.zamu.dojutsu.api` es público; **todo lo demás es implementación aunque sea `public`**. El
mod es un solo jar y no hay forma de esconder los paquetes internos del compilador de un tercero, así que el
contrato no puede ser «lo que compila»: tiene que ser **lo que está declarado**. Eso va escrito en el
`package-info`, que es donde alguien lo va a leer.

`API_VERSION` sigue *semver* sobre **esta superficie**, no sobre la versión del mod: son cosas distintas y
mezclarlas obligaría a subir la mayor cada vez que cambie cualquier cosa del mod.

### Tres cosas, y sólo tres

**Leer** — `NinjaView`, una foto inmutable. No se entrega el `PlayerNinjaData` real, y no por purismo: sus
sub-estados son inmutables pero **el agregado tiene puertas de escritura**, y repartirlo dejaría a cualquiera
saltarse los servicios que existen para que el chakra no se toque por dos sitios. Hay un test que comprueba por
reflexión que ningún método de la vista devuelve el agregado — si alguien añade ese getter, salta.

Deliberadamente **no** expone temporizadores de ejecución (pulsos de modo, cooldowns internos, contadores de
despertar). Serían contrato, y cambiarían cada vez que se afinara un sistema.

**Escribir** — por las **mismas puertas** que usa el mod: el chakra sólo por `ChakraService`, la XP sólo por
`XpApi`, las técnicas sólo por `JutsuGrantService`. No es ceremonia: esas puertas hacen el clamp, emiten los
eventos de dominio y sincronizan al cliente. Una escritura por fuera dejaría el HUD mintiendo y los desbloqueos
sin disparar. Las reglas 10 y 10b valen para terceros igual que para el propio mod.

**Aportar contenido** — y aquí está la decisión de diseño del ticket.

### Datos y código no se registran igual, porque no se recargan igual

Las técnicas, los ojos, los clanes y los contratos son **datos**: se leen de JSON y se recargan con `/reload`.
Las familias de efecto, las habilidades oculares y las técnicas de bestia son **código**: se registran una vez
en el arranque.

Meterlos en el mismo mecanismo habría sido más «uniforme» y habría estado mal. Los cuatro de datos van por
`DojutsuRegistryEvents`, que **salta dentro de cada recarga** —después de leer los archivos del mod y antes de
publicar el registro—, así que el contenido de un tercero se recarga con el resto y no se queda pegado de un
arranque anterior. Los tres de código son un `DojutsuApi.register*` y ya.

Los cuatro reload listeners tenían exactamente la misma forma (`parsear → load()`), así que el enganche es una
línea en cada uno y el evento es genérico: un `Sink<T>` con validación de id delante.

**Sustituir un id existente está permitido a propósito**: es cómo se rebalancea contenido base. Pero el arranque
**dice en el log cuántas entradas aportó cada evento**, de modo que una sustitución no pasa desapercibida — el
mismo criterio que el catálogo de comercio, que cuenta lo que le falta en vez de callarlo.

### Un detalle de testabilidad que cambió el diseño

Los eventos de Architectury **no se pueden desuscribir**. Un test que se suscribiera a `DojutsuRegistryEvents.JUTSU`
dejaría su aportador puesto para todos los tests siguientes del mismo JVM: orden de ejecución significativo y
fallos intermitentes, justo lo que un test no debe tener.

La solución no fue una API de test sino hacer `create()` visible al paquete: cada test fabrica su propio evento
y prueba **la misma mecánica** sin tocar los cuatro reales. Nueve tests escritos **desde fuera**, importando sólo
`com.zamu.dojutsu.api` — si algún día uno de ellos necesitara un servicio interno, la API se habría quedado
corta y ese archivo sería el que lo dijera.

### Validación

`./gradlew build` multiloader **verde**; **973 tests** (+9, el criterio de la ficha entre ellos: un tercero
aporta un jutsu, varios aportan a la vez, uno sustituye contenido base, un id vacío o un valor nulo se rechazan
sin envenenar el registro, y la vista no filtra el agregado). NeoForge y Fabric arrancan limpios y los cuatro
registros cargan igual que antes: sin suscriptores, la API no cambia nada. Sin JVMs zombie.

Documentación de uso en `docs/api.md`, con ejemplos copiables y una sección explícita de **lo que la API no
hace** — que en un contrato importa tanto como lo que sí.

---

### Balance de la Fase 10, y del port

Siete tickets: DJT-225 (los ítems que nunca se crearon), DJT-223 (paridad en cliente), DJT-226 (deuda de
sistemas), DJT-222 (rendimiento), DJT-221 (balance), la deuda de fondo (commit, entrega de jutsus, permisos) y
éste.

**El hilo conductor de la fase fue mirar de verdad.** Cada ticket encontró algo que llevaba fases dándose por
bueno: 9 combinaciones de crafteo inertes, 5 umbrales inventados, dos modos fuera de su propio arnés, 26 jutsus
inalcanzables, veinte comandos sin permisos y nueve fases sin commitear. Ninguno era un bug difícil; todos eran
cosas que nadie había ido a comprobar.

Y la lección de F10 confirma la de DJT-205, ahora con datos: **el build verde no ve el juego**. 973 tests y dos
loaders arrancando limpios convivieron con bestias sin caja de colisión y un Amaterasu naranja. La primera hora
de alguien jugando encontró cinco fallos que nueve fases de tests no habían tocado.

---

## 2026-08-03 · DJT-227 — El sonido, y un recuento que estaba mal

Primer ticket del corte hacia la v1. El mod tenía **19 de sus 28 sonidos sin reproducirse nunca**, y castear una
bola de fuego era silencioso.

### Lo primero que apareció fue un error de medición

La ficha de `ModSounds`, escrita en DJT-224, decía que **ocho de los 28 eran «arte producido y nunca cableado»**.
Es falso. El datapack invoca buena parte de sus sonidos **con el namespace delante** —`playsound
minecraft:dojutsu.isshiki.reappear`— y el grep que se usó buscaba `playsound dojutsu.`. Contando las dos formas
salen **92 llamadas** en 252 ficheros y **los 28 aparecen en alguna**.

No es una anécdota de grep. Un recuento equivocado **se guarda como hecho y deja de mirarse**: durante dos
tickets hubo ocho sonidos catalogados como imposibles de cablear que tenían su sitio desde el principio — el del
Susanoo, sin ir más lejos, en `susanoo/activate` y `deactivate`, mientras el mod tocaba ahí un sonido de vanilla.

### `Level.playSound` no servía, y esa es la razón de `NinjaSfx`

El `playsound` del datapack tiene **dos mandos** para decidir quién oye algo: el volumen y el selector
`@a[distance=0..N]`. Vanilla sólo tiene uno —el radio es `max(16, 16 × volumen)` y no se puede tocar—, así que
copiar los valores literales habría mandado la curación a **160 bloques** donde el original la acotaba a **5**.
Factor treinta y dos.

Por eso se envía el `ClientboundSoundPacket` a mano a los jugadores dentro del radio. Y con el alcance ya
separado, **el volumen literal vuelve a ser el correcto**: DJT-224 había bajado la curación de 10 a 1.0 razonando
que un volumen mayor que 1 no suena más fuerte. Es cierto a medias — el cliente recorta la sonoridad a 1.0 pero
usa el volumen crudo para la **atenuación por distancia**, así que a 10 el sonido es plano en todo su radio y a
1.0 se desvanece a los pocos bloques. Eran sonidos distintos.

### La puerta `d.sfx` es del sonido, no de la técnica

Catorce llamadas van tras `unless predicate dojutsu:sfx_delay`. Es un contador **por jugador y compartido entre
todos los sonidos**, para que castear en ráfaga no solape treinta copias del mismo audio; encadenar dos
habilidades distintas dentro de la ventana deja muda a la segunda, y así era. Se porta guardando **el tick en que
vuelve a estar permitido** en vez de un contador que baje solo: misma semántica, cero trabajo por tick.

### La tentación era una regla por familia, y habría estado mal

«Cada ojo tiene su sonido y suena al usar cualquiera de sus técnicas» es falso. El `cost.mcfunction` compartido
del Mangekyō **no toca nada**; el `playsound` vive en el coste de **cada** técnica. Y se nota en quién se queda
callado: **Amaterasu** no suena (la carpeta de Itachi no tiene un solo `playsound`), **las tres del Byakugan**
tampoco, **las dos del Tenseigan** tampoco, y **Kamui** suena con un sonido **vanilla**.

Haberlo puesto en el pipeline como regla de familia habría dado voz a cuatro técnicas que el original dejó mudas
y habría pisado la de Obito. Es una **tabla por habilidad**, leída desde el único punto de casteo — y hay tests
que fallan si alguien la convierte en regla, que es exactamente la clase de cambio que parece una mejora.

### Los jutsus declaran su sonido en su JSON

Ni el volumen, ni el tono, ni el alcance se derivan de la familia: el Chidori y el Corta-Relámpagos comparten
sonido pero no tono (1 y **0.9**), la madera suena a **5 en 15 bloques** y el golpe cargado a **0.6 en 10**.
Cablearlo por id habría dejado una lista de nombres dentro del código; declarado como parámetro (`EffectSfx`,
compartido por cuatro familias), la técnica trae su sonido igual que trae su daño. **Sin `sound` el jutsu es mudo
a propósito**: de las nueve estructuras de madera sólo tres sonaban.

### Cinco que ya sonaban, y sonaban mal

Susanoo con `illusioner.cast_spell` teniendo el suyo; el Tenseigan Chakra Mode con el par desparejo (tono 1.0 al
encender, 2 al apagar); Kamui por `players` y tono 1 cuando el datapack usa **`master` y tono 0** —de ahí su
timbre hueco—; **Kamui sonando al marcar**, que era inventado (`kamui_set` no tiene ningún `playsound`); y la
recarga del sello SOAH con un tintineo de amatista en vez del sonido de curación.

### Ocho siguen mudos, y no es deuda de sonido

`jutsu.wind`, `flute_song`, `frog_call`, `white_rage_technique`, `ear_ringing`, `sharingan.blind` y los dos de
Isshiki no tienen dónde sonar porque **su técnica no está portada** (entrenamiento de naturaleza, cadenas
fantasma, sabio sapo y serpiente, Izanagi, Isshiki). El Jōgan igual: está en el enum de familias pero sus dos
técnicas no existen. Quedan registrados y listos.

### Validación

`build` multiloader verde, **985 tests** (+12). Ambos loaders arrancan limpios y cargan los **128 jutsus** con
los cinco parámetros nuevos sin romper el codec estricto. Sin JVMs colgadas. Inventario completo en
`docs/sonido.md`.

**Lo que no ve un test**: que el sonido se dispare se comprueba; que se oiga bien, no. Queda para partida si la
puerta de 20 ticks corta de más al encadenar técnicas del Mangekyō, si el alcance manual suena como el original,
y si el Susanoo con su sonido propio resulta molesto a 8 bloques.

---

## 2026-08-03 · DJT-228 — El cuerpo de las bestias, y un supuesto que no se sostenía

Segundo ticket del corte hacia la v1. La premisa era «las nueve Bestias con Cola se ven como un ravager, hay que
darles su malla». La premisa era falsa por los dos lados.

### El ravager es lo que hace el datapack

`tailed_beasts/spawn/spawn_tb_entity` invoca un **ravager de verdad** —1024 de vida, 30 de armadura— y **la línea
que lo haría invisible está comentada**. Encima le pone un `armor_stand` invisible sosteniendo un
`knowledge_book` con `CustomModelData`. El mod llevaba nueve fases con una ficha que llamaba a eso «malla
prestada, provisional».

### Y de los diez modelos que su resource pack declara, sólo existen dos

El pack mapea `14600..14609` a `tailed_beast/1_tail` … `10_tails`, pero **sólo trae `8_tails.json` y
`9_tails.json`**. Los otros ocho apuntan a ficheros que no existen: en el juego original, **siete bestias y el
Diez Colas se ven como un ravager con el cubo morado y negro de "modelo ausente" flotando encima**.

Así que el ticket no era «portar nueve mallas» sino **portar las dos que existen**. Las siete restantes serían
autoría de arte nuevo — otro trabajo y otra decisión, no un porte. Lo único que no se reproduce a propósito es
el cubo de modelo ausente: copiar eso sería portar un fallo.

### Un modelo de ítem no es una malla de entidad

Gyūki y Kurama son modelos de **ítem** de 965 y 1243 cubos. Para dibujarlos sobre una entidad hacen falta o bien
modelos *sueltos* registrados —que **Architectury 13.0.8 no cubre**, y habría costado un puerto de plataforma por
loader— o bien el pipeline de ítems. Se usa el segundo, que además es literalmente la maniobra del datapack: un
ítem-vehículo `beast_body` con `CustomModelData` = número de colas. La diferencia es que aquí el vehículo es
propio y se llama por lo que hace, en vez de secuestrar un `knowledge_book`.

### Dos cosas que sólo se ven arrancando el cliente

**La primera**: las texturas puestas en `textures/tailed_beast/` no se cosían al atlas y el modelo salía sin
texturar. El atlas de bloques de vanilla sólo escanea los directorios `block/` e `item/` —de todos los
namespaces—, así que van en `textures/item/tailed_beast/`. Un servidor dedicado no habría dicho ni una palabra.

**La segunda**, y es la interesante: las texturas vienen a **4096×4096** y **2048×2048** mientras sus modelos
declaran un espacio UV de **512**. Al entrar en el atlas compartido lo hacían saltar de 1024² a **8192×4096** —
de 4 MB a **128 MB** de VRAM, más mipmaps, en toda máquina que cargue el mod tenga bestias delante o no.

Las dos cifras se **midieron arrancando el cliente**, no se estimaron. Con las texturas a 1024 el atlas queda en
**2048×2048 (16 MB)**, y a esa resolución sigue habiendo el doble de píxeles que el espacio UV que el modelo
direcciona. La decisión —fidelidad total del arte contra 8× de VRAM— es del usuario y se le presentó con los dos
números medidos; eligió 1024. Los originales siguen en el resource pack de referencia.

### Validación

`build` multiloader verde, **988 tests** (+3). El servidor de NeoForge arranca limpio con el ítem nuevo. El
**cliente** arranca con **0 modelos y 0 texturas sin resolver** y el atlas en 2048×2048. Sin JVMs colgadas.
Detalle en `docs/bijuu_arte.md`.

**Lo que no ve un test**: que resuelvan lo dice el arranque; que la bestia se vea bien, no. Quedan para partida la
escala del cuerpo frente a su caja de colisión, si mira hacia donde camina, y si las dos quedan a la altura
correcta sobre el suelo.

---

## 2026-08-03 · DJT-229 — La clave del mercader, y un README

Dos piezas del corte hacia la v1, pequeñas y de distinta naturaleza.

### Una clave que no rompía nada, y por eso duró

`ShinobiVillagerService` renombraba al aldeano convertido con
`Component.translatable("entity.dojutsu.shinobi_villager")`, y esa clave **no estaba en `en_us.json`**. El
resultado en partida: un mercader llamado literalmente `entity.dojutsu.shinobi_villager`, en dorado, sobre la
cabeza.

El valor sale del datapack (`trades/setup_shinobi`), que le pone `{"text":"Shinobi","color":"gold"}` — así que
la traducción es **"Shinobi"** y el dorado ya lo aplicaba el mod.

Lo interesante es por qué sobrevivió: **una clave sin entrada no rompe nada**. El juego pinta el identificador
crudo y sigue. Ni el build, ni los 988 tests, ni el arranque de los dos loaders dicen una palabra. Es
exactamente la clase de fallo que sólo ve un jugador mirando.

### Así que se le pone un guardián

`LocalizationKeysTest` recorre las fuentes de los tres módulos, saca toda clave **literal completa** —
`translatable("x.y")`, descartando las que el código concatena, porque ésas no se pueden resolver sin ejecutar
el mod— y comprueba que exista en `en_us.json`. También detecta claves duplicadas, que Gson resuelve en silencio
quedándose con la última.

Con un detalle que importa: **un escáner que no encuentra ficheros aprueba en vacío**, que es peor que fallar.
Lleva un suelo de cordura (más de 80 claves) para que una ruta rota se note como fallo y no como éxito.

### README

No había. Es la portada del mod y lo primero que abre alguien que se lo descarga: requisitos, instalación,
**las 12 teclas** —que hasta ahora no estaban escritas en ningún sitio de cara al jugador—, qué contiene, la
configuración, cómo compilar, y qué falta.

Todos los números están contados del repo, no de memoria, y **dos salieron mal al contrastarlos**:

- Iba a decir «17 Kekkei Genkai» tomándolos de las categorías de jutsu. Son **18**, y no coinciden con las
  categorías: el Shikotsumyaku de los Kaguya y la línea del clan Jūgo son KG sin categoría propia, y `yin_yang`
  es categoría sin ser KG.
- Iba a presentar `/dojutsu reset_ninja` como comando de jugador. **Exige nivel de operador 2**, igual que los
  20 de depuración: el mod no tiene ni un comando de jugador, se juega con el menú y las teclas.

La sección de licencia dice lo que hay que decir: el código es MIT, **el arte no**, y `ATTRIBUTION.md` es una
lista de tareas para que sustituirlo sea posible.

### Validación

`build` multiloader verde, **991 tests** (+3). Fabric arranca limpio. Los enlaces del README se comprobaron uno
a uno. Sin JVMs colgadas.

## 2026-08-28 · DJT-238 — El ojo encendido vuelve a costar chakra

Cierra la deuda que el javadoc de `EyeDefinition` declaraba desde DJT-113: aquel ticket modeló el grafo y el
estado, DJT-116 portó la **fatiga**, y el **drenaje de chakra** se quedó sin portar. Hasta hoy llevar un
dōjutsu encendido salía gratis.

### La tabla del plan se había quedado corta

El plan (`PLAN_UI_ETAPAS_5-1-4-3.txt`, §A3-BIS) resumía los costes en dos columnas: despierto e implantado.
Al leer los `cost.mcfunction` en origen aparecieron **tres** niveles para el Sharingan, no dos, y el tercero
—el más caro— no estaba en el resumen:

| Sharingan | 1 tomoe | 2 tomoe | 3 tomoe | 3 tomoe + Mangekyō |
|---|---|---|---|---|
| Uchiha/Ōtsutsuki despierto | 6 | 7 | 8 | 4 |
| Uchiha/Ōtsutsuki sin despertar | 12 | 14 | 16 | 8 |
| Hatake (cualquier estado) | 12 | 14 | 16 | 8 |
| **Otros clanes** | **16** | **18** | **20** | **12** |

Un Sharingan robado por alguien de otro clan cuesta 20 cada 2 segundos, no 16. Es la penalización más dura del
sistema y era justo la que faltaba. **Confirmación de la regla 16:** el datapack es la fuente de verdad, no los
resúmenes, ni siquiera los que escribimos nosotros la semana pasada.

### Por qué el coste es una lista de reglas y no unos campos

Cada `cost.mcfunction` es una cadena de `execute if predicate ... run remove` evaluada de arriba abajo. El
modelo lo reproduce: `passiveCost.rules`, en orden, **la primera que encaja manda**. Así el JSON se puede leer
al lado del mcfunction original y comprobar que dicen lo mismo.

Un esquema de campos fijos (despierto/implantado) no llegaba: el Sharingan cruza tres niveles de clan con tres
tomoe y con si tienes el Mangekyō, o sea doce números.

### `eyematch` obligó a ampliar el estado

El datapack cobra **14** por el Mangekyō que tú despertaste y **18** por el de otro Uchiha. `EyeState` guardaba
`awakened` como `Set<EyeFamily>` — sabía que despertaste *un* Mangekyō, no *cuál*, así que esas dos situaciones
eran indistinguibles.

Se añade `awakenedEyes: Map<EyeFamily, String>` y el wither `withAwakenedEye(familia, ojoId)`, que usan los tres
sitios que despiertan (`AwakeningService` ×2 y `EvolutionService`). Campo tolerante: las partidas anteriores lo
traen vacío y se tratan **como si casara**, para no cobrarle de más a quien ya tenía el ojo.

### Tres desviaciones deliberadas

**1. Sólo se cobra con el ojo ENCENDIDO.** En el datapack el coste corre mientras *llevas* el ojo, porque allí
no existe interruptor. Con el interruptor de DJT-230, un ojo apagado no cuesta nada. Se acepta: cobrar por
poseerlo convertiría el interruptor en un adorno. Abarata los ojos robados frente al datapack — vigilar en
partida, pero con datos, no antes.

**2. El Rinnegan sin `eyematch` deja de ser gratis.** `rinnegan/cost_0.mcfunction` sólo cobra `if eyematch` y
no tiene rama `else`: un Uchiha o Uzumaki despierto con una variante que no casa lo lleva **a coste cero**. Es
casi seguro un olvido. Se cobra **18**, que es la tarifa que el propio datapack aplica a esos mismos clanes con
el Rinnegan sin despertar: no se inventa un número, se reutiliza uno existente para un caso comparable.

**3. Si no puedes pagar, el ojo se apaga.** El datapack dejaba el chakra en negativo, que aquí no es
representable: `ChakraService` no permite consumos que no caben. Apagarlo es la lectura más cercana —te quedas
sin fuerzas para sostenerlo— y encaja con lo que ya hace la fatiga al 100 %. **El ojo no se pierde**, que es el
error que DJT-230 corrigió y que no se va a reintroducir por la puerta de atrás.

### Lo que no lleva coste, y por qué

**Isshiki (16) y Jōugan (8)** no se portan: esos ojos **no existen todavía** en el mod, son roster diferido.
Sus números quedan en el plan para cuando lleguen. El **Rinne Sharingan** no aparece en `basic_costs` del
datapack — no cuesta chakra, y así se queda.

### Validación

`build` multiloader verde, **1097 tests** (+17). Los tests del coste cargan **el JSON real de cada ojo**, no
una tabla escrita a mano: si alguien toca un dato, saltan. Falta la pasada en partida.

## 2026-08-28 · DJT-239 — Un sitio donde colgar lo que aún no existe

De las 128 técnicas, **sólo 23 dibujaban algo**: las de proyectil, porque son una entidad real volando. Las
**54 de área** no emitían ni una partícula — un Katon que abrasa todo en 5 bloques funcionaba, hacía daño, y
visualmente no pasaba **nada**. Igual los 11 golpes cargados, los 7 buffs y las 6 curaciones.

No era un defecto del porte: el datapack tenía el mismo agujero. Pero sí es un bloqueo para el arte, porque un
modelo 3D necesita un sitio donde aparecer, y ese sitio no existía.

### Dos capas, y la primera funciona sin arte

`EffectVfx` es el hermano de `EffectSfx`, y por el mismo motivo: se declara en el JSON de la técnica, no en un
`switch` por id dentro del código.

- **Partículas**, que funcionan hoy. Si la técnica no declara ninguna, **se deduce de su afinidad**: un jutsu de
  fuego echa llamas, uno de agua salpica. **109 de las 128 declaran afinidad**, así que la inmensa mayoría pasa
  a verse sin escribir un solo dato nuevo. Es lo que convierte este ticket en algo con valor **hoy** y no sólo
  cuando llegue el arte.
- **Un modelo**, opcional: el id de un ítem mostrado con un `ItemDisplay` de vanilla.

### Por qué un ItemDisplay y no una entidad propia

La regla 5 obliga a dar renderer en cliente a toda `EntityType` que el mod registre. El `ItemDisplay` es de
vanilla: ya tiene renderer, ya sincroniza, soporta escala y rotación, y muestra **cualquier** modelo de ítem
—incluidos los `custom_model_data` por técnica que lleguen después—. **El mod no registra ninguna entidad
nueva**, así que los 128 modelos futuros no traen 128 renderers.

Se construye **por NBT**, el camino de `/summon`: `ItemDisplay` no expone setters públicos para su ítem ni para
su transformación en Mojmap, y la vía por NBT no depende de internos que cambian entre versiones.

### Dos decisiones pequeñas que se notan

**La dispersión sigue al radio real del efecto.** Un jutsu de área de 10 bloques no puede verse igual que uno
de 2. Sin este detalle, el sistema habría hecho que todo pareciera lo mismo.

**La decisión de qué partícula usar devuelve texto, no una partícula resuelta.** Es lo que la hace **pura** y
por tanto testeable: este repo no arranca Minecraft en tests, y mi primer intento —un mapa estático de
`ParticleTypes`— tumbó los ocho tests con `ExceptionInInitializerError`. Resolver el id contra el registro es
glue y vive aparte.

### Retirada de los modelos

Cola propia por vencimiento, drenada en `TickEvent.SERVER_POST`, con tope de 256 modelos vivos. **No usa
`ModScheduler`** a propósito: ese scheduler se inyecta en el dispatcher y no tiene acceso global; dárselo por
una vía estática sólo para esto habría roto ese diseño.

### Lo que este ticket NO hace

No trae arte. Ninguna técnica declara `vfxModel` todavía: el campo existe y está probado, esperando a que haya
modelos. Las 19 técnicas de estructura siguen sin visual propio y es correcto — colocan bloques vanilla reales,
que ya se ven.

### Validación

`build` multiloader verde, **1106 tests** (+9). Falta la pasada en partida.

## 2026-10-11 · DJT-241 — La Bala de fuego, como se ve en partida

Ticket nacido de un vídeo de prueba del usuario sobre DJT-240. Señaló dos cosas (la hitbox no encaja con el
modelo; la bola no tiene animación ni efectos) y al revisarlo salieron cinco más.

### Lo que se vio y su causa

- **La hitbox no encajaba, por dos motivos a la vez.** La caja medía 0.25, la de una bola de nieve, contra una
  bola de 0.8. Y la bola ni siquiera estaba encima de la caja: Minecraft centra el modelo en (8, 8, 8) y la bola
  va delante de la estela, así que a escala 1.6 volaba **un bloque por delante** de la entidad. Corregido el
  pivote en el generador (traslación de `ground`) y añadida una hitbox por proyectil (`hitboxSize`).
- **El rayo de vanilla no mira el tamaño del proyectil.** `ProjectileUtil` traza desde los pies y engorda a los
  objetivos 0.3 fijos. Con hitbox propia, el rayo sale ahora del centro de la caja y engorda con su medio lado:
  el barrido exacto de caja contra caja. Sin `hitboxSize`, la colisión de siempre; los otros 12 sitios que
  lanzan `NinjaProjectile` no cambian.
- **No ardía el suelo.** `fire_bullet/run.mcfunction` hace `setblock ~ ~0 ~ fire` en cada tick; el JSON del port
  no tenía `igniteGround`. Era una pérdida de fidelidad, no un efecto nuevo.
- **No había estela.** El datapack soltaba 120 partículas de polvo por tick sobre el armor stand; el port, cero.
  Ahora cada cliente la pinta desde el tick de la entidad con los mismos colores, repartida por el segmento
  recorrido. Viaja un byte (`ProjectileTrail`), no un paquete por tick (regla 13).
- **Al impactar no pasaba nada visible.** Estallido de llamas, lava y humo con el sonido de una carga ígnea. El
  original no lo tenía; el cliente no se entera del impacto (solo ve desaparecer la entidad), así que lo emite el
  servidor.
- **Contra el cielo se veía marrón.** El modelo usaba la luz del sitio. Ahora brillo máximo.
- **Era un objeto rígido.** Gira sobre el eje de vuelo y late un 6 %.

### La prueba que faltaba

Nada de lo anterior daba error: se veía mal y ya. `Jutsu3dFitTest` lo hace fallar en el build para **todo**
jutsu con `displayModel3d`: la hitbox debe medir lo que la bola (`bola_*`), la bola debe caer en la entidad y el
jutsu debe llevar estela.

### Lo que este ticket NO hace

- El proyectil del datapack **atravesaba** a las entidades (solo lo paraban los bloques y el agua); el del port
  se detiene en la primera. Con una caja mayor se detendrá antes. Se deja como estaba: cambiarlo es jugabilidad.
- Las variantes azul y negra del fuego (`d.blue`/`d.black`) siguen sin portar.
- El brazo no hace gesto al lanzar.

### Segunda prueba en partida (vídeo del usuario)

Dos fallos que no estaban en el primer vídeo y que trajo este mismo ticket:

- **El lanzador se quemaba con su propio fuego.** Las llamas que tapaban la pantalla eran el overlay de estar
  ardiendo. Los cinco jutsus de fuego con proyectil daban en su `cast.mcfunction`
  `effect give @s fire_resistance 2 255 true`, justo porque el proyectil sale de la cara del lanzador y prende el
  suelo desde el primer tick. El port lo había perdido y no se notaba hasta activar `igniteGround`. Nuevo
  parámetro `casterEffects`, puesto en los cinco JSON.
- **La pantalla entera amarilla al lanzar.** La cámara quedaba dentro del modelo: al centrar la bola en la
  entidad, la estela quedó 1.8 bloques por detrás y atravesaba la cara del lanzador. Ahora el modelo **crece** al
  salir (de 0 a tamaño real entre los ticks 2 y 6) y la cola nunca llega a la cámara. Lo comprueba un test con la
  longitud real de la estela.

### Validación

`build` multiloader verde, **1134 tests** (+18). Falta repetir la pasada en partida.
