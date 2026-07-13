# Registro de desarrollo — Dojutsu Port

> Historial técnico cronológico. Cada entrada = un ticket de la Fase 0.
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
