# Arquitectura — Dojutsu Port (estado tras Fase 0)

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
| `registry` | Clases `Mod*`: declaran contenido vía los puertos | permanente |
| `core` | `DataReloadListener` (datos de servidor) | permanente |
| `entity` | `DebugMarkerEntity` (marcador placeholder) | placeholder |
| `network` | `PingPayload`/`PongPayload` (dummy) | **temporal F0** |
| `debug` | `DummyAttachment(+Command)`, `PingPongDebug(+Client)` | **temporal F0** |

### Adaptadores por loader

| NeoForge (`com.zamu.dojutsu.neoforge`) | Fabric (`com.zamu.dojutsu.fabric`) |
|----------------------------------------|-------------------------------------|
| `DojutsuNeoForge` (`@Mod`) | `DojutsuFabric` (main), `DojutsuFabricClient` (client) |
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
    ├─ ModItems/ModEntities/ModAttributes/ModSounds/ModParticles/ModAttachments.register()
    │     └─ usan PlatformServices.REGISTRAR / ATTACHMENTS
    ├─ ReloadListenerRegistry.register(SERVER_DATA, DataReloadListener, id("data_reload"))
    └─ (andamiaje F0) DummyAttachment/DummyAttachmentCommand/PingPongDebug.registerReceivers()

  DojutsuMod.initClient()  [solo cliente]
    └─ (andamiaje F0) PingPongClientDebug.registerClientTrigger()
       + registro de NoopRenderer para debug_marker (en el código de cliente de cada loader)

Carga de mundo / /reload
  DataReloadListener.apply()  → (F4/F5 cargará definiciones data-driven)

Runtime (dummy de validación)
  Cliente entra al mundo → envía PingPayload (C2S)
     → servidor: PingPongDebug.onPing → responde PongPayload (S2C)
        → cliente: PingPongDebug.onPong (log)
```

## 7. Qué falta por construir (según el ADD, aún no en código)

Módulos de dominio y contenido: `domain`, `data` (`PlayerNinjaData`), `chakra`, `natures`,
`progression`, `clans`, `jutsu`, `dojutsu`, `sage`, `empowerment`, `bijuu`, `items`, `summoning`,
`throwing`, `ai`, `trading`, `story`, `world`, `client`, `api`, `util`; y el puerto `AttributeService`.
