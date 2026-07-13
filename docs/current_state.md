# Estado actual del proyecto — Dojutsu Port

> Última actualización: 2026-07-13 · Fase 0 completada.
> Este documento refleja el estado real del código en `D:\Projectos programacion\Dojutsu Port`.

## 1. Resumen

Mod de Minecraft **multiloader (Architectury)** que reconstruye el datapack *Dojutsu*
(Naruto/Boruto) como mod moderno. Se ha completado la **Fase 0 (andamiaje)**: el proyecto
compila y arranca en **NeoForge** y **Fabric**, cliente y servidor dedicado, con los puertos
de plataforma operativos y un registro base placeholder. **Todavía no hay mecánicas de
gameplay** (chakra, jutsus, ojos, etc.): eso empieza en la Fase 1.

- **Minecraft:** 1.21.1 · **Java:** 21 · **Gradle wrapper:** 8.10.2
- **Loader:** Architectury (módulos `common` + `neoforge` + `fabric`)
- **Namespace / mod id:** `com.zamu.dojutsu` / `dojutsu` · **Versión:** 0.1.0
- **Repositorio:** https://github.com/Z4MU/Dojutsu-Port.git (rama `main`, CI en verde)

## 2. Fases

| Fase | Nombre | Estado |
|------|--------|--------|
| **F0** | Fundaciones y andamiaje multiloader | ✅ **Completada** |
| F1 | Núcleo de dominio y datos (`domain`, `data`/`PlayerNinjaData`) | ⬜ No iniciada (siguiente) |
| F2 | Recursos y progresión | ⬜ No iniciada |
| F3 | Definición del ninja | ⬜ No iniciada |
| F4 | Combate | ⬜ No iniciada |
| F5–F10 | Contenido incremental (plantillas) | ⬜ No iniciada |

## 3. Historias y subtareas (F0)

Todas las subtareas de F0 están **terminadas y validadas en runtime en ambos loaders**:

| Ticket | Historia | Estado |
|--------|----------|--------|
| DJT-001 | Estructura Gradle multiloader (Architectury) | ✅ |
| DJT-002 | Entrypoints y `MOD_ID` (common + adaptadores) | ✅ |
| DJT-003 | Puerto `RegistrarService` (registro diferido cross-loader) | ✅ |
| DJT-004 | Puerto `AttachmentService` + test con campo dummy | ✅ |
| DJT-005 | Puerto `NetworkService` (payloads C2S/S2C) + ping-pong | ✅ |
| DJT-006 | Registro base placeholder (`ModItems`/`ModEntities`/…) | ✅ |
| DJT-007 | Reload listener del datapack del mod (vacío) | ✅ |
| DJT-008 | CI: build + tests | ✅ (verde) |

**No quedan subtareas pendientes dentro de F0.**

## 4. Qué funciona hoy

- El mod **carga** en NeoForge y Fabric, en cliente y servidor dedicado (logs de arranque
  `init()` / `initClient()`).
- **Puerto `RegistrarService`**: registrar contenido (items, entidades, atributos, sonidos,
  partículas) desde `common` sin conocer el loader. Ejercitado por las clases `Mod*`.
- **Puerto `AttachmentService`**: Data Attachments por entidad, persistentes (`Codec`),
  con copia en muerte opcional. Validado con save/reload/respawn/dimensión (comando
  `/dojutsudummy`).
- **Puerto `NetworkService`**: payloads C2S/S2C sobre `CustomPacketPayload` de 1.21.
  Round-trip Ping→Pong validado en runtime.
- **Registro base placeholder**: `dojutsu:ninja_debug` (item), `dojutsu:debug_marker`
  (entidad marcador con `NoopRenderer`), atributo `max_chakra`, sonido y partícula debug,
  attachment `placeholder`.
- **Reload listener** de datos de servidor (`/reload` dispara `apply()`).
- **CI** en GitHub Actions (build + test en runner limpio con JDK 21).

## 5. Qué NO existe todavía

Ninguna mecánica de gameplay. En concreto, **no existen** aún:

- Dominio ninja puro (`Resource`, `StrainMeter`, `ToggleableMode`, `Jutsu`, `LeveledStat`/
  `XpCurve`, `NinjaRank`) — Fase 1.
- `PlayerNinjaData` (el estado real del jugador que reemplaza ~800 scoreboards) — Fase 1.
- Módulos de contenido: `chakra`, `natures`, `progression`, `clans`, `jutsu`, `dojutsu`,
  `sage`, `empowerment`, `bijuu`, `items`, `summoning`, `throwing`, `ai`, `trading`,
  `story`, `world`, `client` (GUI/HUD/renders reales), `api`, `util`.
- Puerto `AttributeService` (para **adjuntar** atributos a entidades vanilla como `Player`).
- Contenido data-driven (JSON de jutsus/clanes/ojos) y datagen.
- Assets (texturas, modelos, sonidos reales, traducciones).

## 6. Estado del código

- **Fuente:** 22 clases en `common`, 8 en `fabric`, 7 en `neoforge` (37 clases Java).
- **Andamiaje temporal de F0** (se retira al llegar el módulo `data`): paquete
  `com.zamu.dojutsu.debug` (`DummyAttachment`, `DummyAttachmentCommand`, `PingPongDebug`,
  `PingPongClientDebug`), `com.zamu.dojutsu.network` (`PingPayload`, `PongPayload`) y
  `com.zamu.dojutsu.entity.DebugMarkerEntity`.
- **Compila limpio** (`./gradlew build`) y arranca en los 4 run configs
  (`fabric/neoforge` × `runClient/runServer`).
- **Sin tests unitarios** todavía (llegan en F1 con TDD del dominio); el task `test` pasa
  trivialmente.
- **Fuente de verdad del diseño** (fuera de este repo): `D:\Projectos claude\Dojutsu reverse\DOCS\`
  → `sistemas/` (ingeniería inversa S01–S25), `arquitectura/` (ADD), `backlog/` (DJT-###).
