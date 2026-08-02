# Handoff — Fase 1 → Fase 2 · Dojutsu Port

> Documento de traspaso. Un desarrollador que abra el proyecto debería poder continuar leyendo solo `docs/`.
> Estado a 2026-07-13. **Fase 0 y Fase 1 completadas y validadas en runtime** en NeoForge y Fabric.

## Resumen ejecutivo

**Dojutsu Port** reconstruye el datapack *Dojutsu* (Naruto/Boruto) como mod moderno de Minecraft
(**1.21.1**, Java **21**, Architectury multiloader). Tras la **Fase 1 (Núcleo, dominio y estado)**, el mod
tiene: el **dominio ninja puro** (POJOs testeables sin Minecraft), el **estado por jugador**
(`PlayerNinjaData`, persistente/versionado/migrable) y el **motor** (identidad, tick pipeline, ciclo de vida,
config, muerte/respawn con guards) y la **sincronización base** al cliente (full/delta + cache read-only).
**Todavía no hay mecánicas de gameplay**: la Fase 2 conecta el dominio con el juego (chakra primero).

- **172 tests** unitarios verdes; `./gradlew clean build` multiloader verde.
- Repo: https://github.com/Z4MU/Dojutsu-Port.git (`main`).

## Qué quedó implementado (F1)

### EP-DOMAIN — dominio puro (paquete `com.zamu.dojutsu.domain.*`, cero imports de Minecraft)
- `resource`: `Resource` (clamp), `RegenPolicy` + `RegenFactor` (regen timer→pulso, tabla exacta de `[S02]`).
- `strain`: `StrainMeter` + `StrainThreshold` + `StrainListener` (umbrales 25/50/75/90/95/98/100, anti-spam).
- `mode`: `ToggleableMode<C>` + `ModeContext` (framework activate→run→cost→punish→deactivate, genérico).
- `jutsu`: `Jutsu` + `JutsuDefinition` + `CastContext` + `CastResult` + `AffinityRequirement` + `CastFailureReason`
  (contrato de técnica data-driven, caster genérico; máquina de precondiciones chakra→cooldown→afinidad).
- `progression`: `XpCurve` (9 bandas, cap 500), `LeveledStat` (multinivel con arrastre, sin guardia 1/tick),
  `NinjaRank` (12 rangos de `[S20]`).
- `rng`: `RollTable<T>` + `WeightedEntry` (RNG por pesos con `java.util.random.RandomGenerator` inyectado).
- `event`: `DomainEventBus` (síncrono, server-side, aísla fallos) + 8 eventos internos.

### EP-DATA — estado por jugador (paquete `data`)
- `PlayerNinjaData` (agregado raíz) con **patrón de sub-estado enchufable** + `Codec`/NBT; hoy solo
  `OnboardingState` + `dataVersion`. Escritura solo por puertas (`setOnboarding`).
- `DataVersion` + `MigrationChain` + `Migration` (migración a nivel NBT antes de decodificar, idempotente).
- `NinjaData.get(player)` sobre `AttachmentService` (attachment `player_ninja_data`, persistente, copyOnDeath,
  codec migratorio); `NinjaData.reset(player)` = instancia nueva.
- `DirtyTracker` + `ObservableField` (campos observables sucios, transitorio).

### EP-CORE — motor (paquete `core.*`)
- `identity.PlayerRef` (UUID; sin límite de 10 jugadores).
- `TickPhase` (orden invariante de `[S01 §4]`), `PlayerTickSystem`, `PlayerTickPipeline` (inyección, OCP).
- `ServerTickDispatcher` (3 fases: jugador con aislamiento / global placeholder / scheduler) +
  `scheduler.ModScheduler` (tareas diferidas).
- `DojutsuLifecycle` (punto único de suscripción: tick, reload, join/quit, cambio de dimensión, muerte/respawn,
  carga de nivel, payloads).
- `lifecycle.DeathHandler` (guard survival/adventure) + `RespawnHandler` (hooks F2).
- `config.DojutsuConfig` (claves de `properties` con defaults de `[S01]`; hardcore por `getServer().isHardcore()`).
- `command.AdminCommands` (`/dojutsu reset_ninja` + stubs, op).

### EP-NETWORK-BASE — sync (paquetes `network`, `client`)
- `SyncNinjaDataPayload` (S2C full/delta) + `NinjaSync` (sendFull en join/respawn/dimensión; sendDelta en cambio).
- `client.ClientNinjaData` (cache read-only; ignora deltas pre-full; se limpia al cambiar de mundo).

## Decisiones arquitectónicas clave de F1 (y su porqué)

1. **Dominio puro con cero imports de Minecraft** (regla dura de EP-DOMAIN): testeable sin arrancar el juego;
   los tests unitarios son el DoD. Excepción documentada: `RollTable` usa `java.util.random.RandomGenerator`
   (JDK) en vez del `RandomSource` de MC para no romper la pureza.
2. **Patrón de sub-estado enchufable en `PlayerNinjaData`**: cada fase añade un sub-estado (value object con
   `DEFAULT`+`CODEC`) con 1 campo + 1 línea de codec + puerta de escritura; `optionalFieldOf` da tolerancia
   automática. **No reescribir el contrato central.**
3. **Migración a nivel NBT antes de decodificar** (no sobre el objeto): los cambios estructurales se pierden en
   el decode si se hacen después. Idempotencia por version-gating.
4. **`TickPhase` como enum del orden invariante** (chakra primero): añadir un sistema en F2+ elige su fase y
   **no toca `core`** (OCP). No reintroducir polling/reset de input.
5. **Muerte/respawn con guards ya correctos; penalización real como hooks** (chakra/strain no existen hasta F2).
6. **Config global = `properties` del datapack** con defaults exactos; hardcore **no destructivo**
   (`getServer().isHardcore()`), elimina el "matar a un jugador" del datapack.
7. **Sync solo de lo observable**, con full/delta y cache read-only en cliente; el cliente **nunca** decide
   gameplay (autoridad del servidor).

## Convenciones (recordatorio)

Ver `docs/coding_conventions.md`. Dominio en `domain.*` (sin MC); estado en `data.*` (puede usar Codec/NBT);
motor en `core.*`; comentarios/logs en español (logs sin acentos); un ticket a la vez; validar en runtime en
**ambos** loaders lo que toque plataforma.

## Cómo se valida hoy

- **Unit:** `./gradlew :common:test` (172 verdes) — dominio y datos, con `NbtOps` sin bootstrap.
- **Build:** `./gradlew clean build` — multiloader verde.
- **Runtime (herramientas de depuración temporales):** `/dojutsu reset_ninja` (admin, op); `/dojutsuninja
  get|set <fase>` (inspecciona/escribe onboarding); `/dojutsusched count|in <ticks>` (tick/scheduler). El log
  del cliente muestra `Cache cliente (full/delta)` al sincronizar.

## Deuda / hooks abiertos para la Fase 2

- **Puerto `AttributeService`** (adjuntar `max_chakra` y otros atributos a `Player`): NeoForge
  `EntityAttributeModificationEvent`; Fabric mixin. **Necesario para chakra.**
- **Conectar los hooks de muerte/respawn**: `DeathHandler` (½ chakra, strain=0) y `RespawnHandler`
  (desactivar modos, recalcular velocidad/taijutsu) cuando existan chakra/strain/modos.
- **Deltas por-campo** cuando haya >1 campo observable; **HUD** real que consuma `ClientNinjaData`.
- **`GatherTimeLevel_*`** (tuning sage) en el módulo sage.
- Retirar `/dojutsuninja` y `/dojutsusched` cuando el HUD permita observar el estado.
- `LICENSE` acorde a la condición **no-comercial** de los assets.

## Próxima fase (F2 — Recursos y progresión)

Según el ADD/backlog de diseño (`D:\Projectos claude\Dojutsu reverse\DOCS\backlog\03_FASE2_...`): módulos
`chakra` (usa `Resource`/`RegenPolicy`/`RegenFactor`, se engancha al `PlayerTickPipeline` en `TickPhase.CHAKRA`,
requiere el puerto `AttributeService`) y `progression` (usa `LeveledStat`/`XpCurve`/`NinjaRank`), con el
**vertical slice** y el HUD de chakra. Empezar leyendo la ficha de la Fase 2 en el repo de diseño.

## Recomendaciones para continuar

1. **Leer la fuente de verdad** (fuera de este repo): `D:\Projectos claude\Dojutsu reverse\DOCS\` →
   `sistemas/` (S01–S25), `arquitectura/` (ADD 00–11), `backlog/` (fases y fichas DJT-###).
2. **Un ticket a la vez**; validar en runtime en **ambos** loaders lo que toque plataforma.
3. **Reutilizar el dominio y los puertos** existentes; no meter imports de loader en `common`, ni de MC en `domain`.
4. Al añadir un sub-estado, **seguir el patrón enchufable** de `PlayerNinjaData` (no reescribir el contrato).
5. Mantener el `build` multiloader verde y ampliar los tests de dominio.
