# Handoff — Fase 0 → Fase 1 · Dojutsu Port

> Documento de traspaso. Un desarrollador que abra el proyecto por primera vez debería poder
> continuar leyendo solo `docs/`. Estado a 2026-07-13.

## Resumen ejecutivo

**Dojutsu Port** es la reconstrucción, como mod moderno de Minecraft, del datapack *Dojutsu*
(Naruto/Boruto). La **Fase 0 (andamiaje multiloader)** está **completa y verificada en runtime**:
el mod compila y arranca en **NeoForge** y **Fabric**, cliente y servidor dedicado, con los tres
puertos de plataforma (registro, attachments, red) operativos y un registro base placeholder.
**No hay mecánicas de gameplay todavía**; el contenido real empieza en la Fase 1.

- MC **1.21.1**, Java **21**, Gradle wrapper **8.10.2**, Architectury multiloader.
- Repo: https://github.com/Z4MU/Dojutsu-Port.git (`main`, CI en verde).

## Arquitectura utilizada

**Clean/Hexagonal Architecture sobre Architectury multiloader.**

- **`common`** — 99% de la lógica, loader-agnóstica.
- **`neoforge` / `fabric`** — adaptadores finos (registro, red, attachments, entrypoint, renderers).
- Toda diferencia de loader se accede tras un **puerto** en `com.zamu.dojutsu.platform`, resuelto por
  **`ServiceLoader`** a través de la fachada `PlatformServices` (`META-INF/services/*` en cada loader).
- Mappings **Mojmap** unificados en los 3 subproyectos.

Ver `docs/architecture.md` para módulos, dependencias y diagramas.

## Qué quedó implementado (F0)

- **Entrypoints** comunes + adaptadores; separación estricta cliente/servidor.
- **Puerto `RegistrarService`**: registro diferido de items/entidades/atributos/sonidos/partículas.
- **Puerto `AttachmentService`**: Data Attachments por entidad, persistentes (Codec), copia opcional
  en muerte. Validado con save/reload/respawn/dimensión.
- **Puerto `NetworkService`**: payloads C2S/S2C; round-trip Ping→Pong validado.
- **Registro base placeholder** (`Mod*`) + `DataReloadListener` (datos de servidor).
- **CI** en GitHub Actions.
- **Andamiaje temporal** (a retirar en F1): paquete `debug`, `network` (Ping/Pong),
  `entity.DebugMarkerEntity` — sirvieron para validar los puertos; el comando `/dojutsudummy` sigue
  disponible para pruebas manuales.

## Qué módulos existen (paquetes reales bajo `com.zamu.dojutsu`)

| Paquete | Contenido actual | Destino |
|---------|------------------|---------|
| (raíz) | `DojutsuMod` (MOD_ID, init/initClient, `id()`) | permanente |
| `platform` | Puertos + fachada `PlatformServices` (Registrar/Attachment/Network) | permanente |
| `registry` | `ModItems/ModEntities/ModAttributes/ModSounds/ModParticles/ModAttachments` | permanente |
| `core` | `DataReloadListener` | permanente |
| `entity` | `DebugMarkerEntity` (placeholder) | temporal / se ampliará |
| `network` | `PingPayload`, `PongPayload` | **temporal (F0)** |
| `debug` | `DummyAttachment(+Command)`, `PingPongDebug(+Client)` | **temporal (F0)** |

Adaptadores: `fabric.*` (8 clases) y `neoforge.*` (7 clases). **Aún NO existen** los módulos de
contenido del ADD (`domain`, `data`, `chakra`, `natures`, `progression`, `jutsu`, `dojutsu`, `sage`,
`empowerment`, `bijuu`, `items`, `summoning`, `throwing`, `ai`, `trading`, `story`, `world`,
`client`, `api`, `util`).

## Qué archivos importantes cambiaron / se crearon

- **Build:** `settings.gradle`, `build.gradle`, `gradle.properties`, `neoforge/gradle.properties`
  (`loom.platform=neoforge`), `*/build.gradle`, `gradle/wrapper/*`, `.gitignore`, `.gitattributes`,
  `.github/workflows/ci.yml`.
- **Metadatos:** `fabric/.../fabric.mod.json`, `neoforge/.../META-INF/neoforge.mods.toml`.
- **Núcleo:** `common/.../DojutsuMod.java` (orquesta todo el registro en `init()`).
- **Puertos:** `common/.../platform/*` + impls por loader + `META-INF/services/*` (3 servicios ×
  2 loaders).

## Decisiones arquitectónicas (y su porqué)

1. **Architectury multiloader** (ADR-001 del ADD): una lógica común + adaptadores. Evita duplicar
   NeoForge/Fabric.
2. **Puertos por `ServiceLoader`** (no `@ExpectPlatform`): encaja con servicios con estado; patrón
   `Services` documentado por Architectury.
3. **`NetworkService` nativo por loader (NO Architectury `NetworkManager`)**: el `NetworkManager` de
   architectury **13.0.8** tiene un bug de S2C en Fabric (`AbstractMethodError`, issue #518). Se usó
   el sistema `CustomPacketPayload` nativo de cada loader.
4. **Matriz de versiones exacta** (ver `CLAUDE.md`): "la última de cada cosa" rompió el build; se fija
   la matriz coetánea de 1.21.1 (loom 1.7.435, fabric-loader 0.16.10, etc.).
5. **`copyOnDeath`** disponible en el puerto de attachments; `PlayerNinjaData` (F1) deberá usarlo.

## Convenciones utilizadas

Ver `docs/coding_conventions.md`. En resumen: paquetes por capacidad de dominio; `Mod*` para
registries; puertos en `platform` + impl `<Loader>XxxService` por loader; español en comentarios/logs
(sin acentos en logs para evitar problemas de encoding); Clean Code + SOLID + DRY/KISS.

## Riesgos conocidos

- **RM-6 (diferencias de loader):** desriesgado en F0 (los 3 puertos probados en ambos loaders),
  pero cada nueva capacidad de plataforma (p. ej. `AttributeService`) puede reabrirlo.
- **Networking de Architectury roto (13.0.8):** no volver a usar `NetworkManager`; usar el puerto
  `NetworkService` (nativo).
- **Versiones sensibles:** no actualizar loom/fabric-loader/neoforge/architectury a la ligera; están
  emparejadas para 1.21.1 (ver la matriz).
- **Entidades sin renderer** crashean el cliente y pueden corromper el mundo → toda `EntityType`
  necesita renderer (usar `NoopRenderer` para marcadores).

## Problemas pendientes

- **Atributo → `Player` no adjuntado:** `max_chakra` se registra pero no se aplica a la entidad
  jugador; falta el puerto **`AttributeService`** (NeoForge `EntityAttributeModificationEvent`;
  Fabric mixin). Necesario cuando el módulo `chakra` use el atributo.
- **Andamiaje temporal por retirar** (`debug`, `network` Ping/Pong, `DebugMarkerEntity`) cuando exista
  `PlayerNinjaData` / la red real.
- **Sin tests unitarios** aún (el task `test` pasa vacío). F1 introduce TDD del dominio.
- **Sin assets** (texturas/modelos/sonidos/idiomas); el resource pack original está documentado en el
  ADD (`11_RESOURCE_PACK.md`) con licencia **no-comercial**.

## Próxima fase del roadmap (F1 — Núcleo de dominio y datos)

- Módulo **`domain`** (POJOs sin Minecraft, testeables): las 5 abstracciones que colapsan la
  duplicación del datapack — `Resource`+`RegenPolicy`, `StrainMeter`, `ToggleableMode`, `Jutsu`+
  `JutsuDefinition`, `LeveledStat`+`XpCurve`+`NinjaRank`. **Con TDD** (números exactos del ADD).
- Módulo **`data`**: `PlayerNinjaData` (attachment real que reemplaza ~800 scoreboards), `Codec`/NBT
  versionado (`dataVersion`), `MigrationChain`. Reset = instancia nueva.
- Empezar a usar los puertos ya construidos (`AttachmentService` para `PlayerNinjaData`).

## Recomendaciones para continuar

1. **Leer la fuente de verdad** (fuera de este repo): `D:\Projectos claude\Dojutsu reverse\DOCS\`
   → `sistemas/` (S01–S25), `arquitectura/` (ADD, 00–11), `backlog/` (fases y fichas DJT-###).
2. **Un ticket a la vez**, siguiendo el flujo del backlog; validar en runtime en **ambos** loaders.
3. **Reutilizar los puertos** existentes; no meter imports de loader en `common`.
4. Para nuevos registros, seguir el patrón `Mod*` + `PlatformServices.REGISTRAR`.
5. Al añadir la primera capacidad de estado real (`PlayerNinjaData`), **retirar** el andamiaje `debug`.
6. Añadir un `LICENSE` acorde a la condición **no-comercial** de los assets de Prof Technic.
7. Mantener el CI verde; añadir tests de dominio en F1 para que el `test` deje de ser trivial.
