# Handoff — Fase 2 → Fase 3 · Dojutsu Port

> Documento de traspaso. Un desarrollador que abra el proyecto debería poder continuar leyendo solo `docs/`.
> Estado a 2026-07-15. **Fases 0, 1 y 2 completadas y validadas en runtime** en NeoForge y Fabric.

## Resumen ejecutivo

**Dojutsu Port** reconstruye el datapack *Dojutsu* (Naruto/Boruto) como mod moderno de Minecraft (**1.21.1**,
Java **21**, Architectury multiloader). Tras la **Fase 2 (Recursos y progresión)** el mod tiene el
**vertical slice completo end-to-end**: chakra vivo, progresión (puntos/nivel/rango/desbloqueos), GUI real y
HUD, todo con autoridad de servidor y persistencia.

**El hito de validación arquitectónica está superado** (`[ADD/08 §6.2]`): red + input + GUI + persistencia +
economía funcionan juntos. **A partir de aquí, el resto del proyecto es contenido incremental sobre patrones
probados.**

- **247 tests** unitarios verdes; `./gradlew build` multiloader verde.
- **145 clases Java** (97 common + 30 tests + 10 fabric + 8 neoforge).
- Repo: https://github.com/Z4MU/Dojutsu-Port.git (`main`).

**Demo de aceptación (DoD de F2) — validada en ambos loaders:** el jugador ve su **chakra regenerar** (barra
HUD) → **mata mobs** y gana skill points → pulsa **M** y **abre el menú (GUI real)** → **gasta puntos** (payload
validado en servidor) y **sube de nivel** → gana `ninjaXp` y **sube de rango** (HUD + aviso) → **come/duerme** y
repone chakra → **muere** y respawnea con **½ chakra** → alcanza hitos y **desbloquea**. Todo persiste al reloguear.

## Qué quedó implementado (F2)

### EP-CHAKRA — chakra vivo (`chakra`, `[S02]`)
- `data/state/ChakraState`: sub-estado (chakra/maxChakra=100, naturalChakra/maxNaturalChakra=100, chakraTimer,
  jugoTimer). Value object inmutable con invariante saneado (defensa ante NBT corrupto).
- `chakra/ChakraService`: **única puerta de escritura** del chakra — `has`/`consume` (todo-o-nada)/`restore`/
  `restoreFull`/`set` + `advanceRegen`/`advanceNaturalRegen`. Clamp delegado en el dominio `Resource`. **Puro**
  (opera sobre `PlayerNinjaData`) → testeable sin Minecraft.
- `chakra/RegenService`: `PlayerTickSystem` en fase `CHAKRA` (**corre primero**, antes de cualquier consumo).
  Reutiliza el dominio `RegenPolicy` (tabla exacta de `[S02]`); el **timer se persiste** en `ChakraState`.
  Guards: sin regen en espectador ni muerto.
- `chakra/SleepChakraHandler` (dormir la noche completa → chakra al máximo) y `chakra/FoodChakraHandler`
  (comer → +50, filtrado por componente `FOOD`).
- Penalización de muerte: `core/lifecycle/DeathHandler` → `chakra = maxChakra/2` (½ del **máximo**).

### EP-PROGRESSION — progresión (`progression`, `[S13]`/`[S20]`)
- **Auditoría de economía (RA-4):** `resources/data/dojutsu/balance/xp_sources.json` — dataset de **referencia**
  (no cargado por código) con todas las fuentes de XP/puntos/costes. Los valores no cuantificados en la
  ingeniería inversa están marcados **`TBD_datapack`** (se extraen al portar F3/F4).
- `data/state/ProgressionState` + `data/state/Skill` (7 habilidades; `autoLevels=false` en senjutsu/cc).
  Rango **derivado** (`rank()`), no cacheado. `unlocked` idempotente.
- `progression/SkillPointService`: kills → puntos (por `LivingDeathEvent`, valores de config 1/3/200) y `spend`
  (puro: +nivel/−puntos; sobrecarga MC que enruta el ninja_xp por `XpApi`).
- `progression/XpApi` (**única puerta de XP**) + `progression/LevelingService` (motor **puro**: multi-nivel de
  una, cap 500, emite `SkillLevelChangedEvent`/`NinjaXpChangedEvent`).
- `core/DomainEvents`: holder del `DomainEventBus` **compartido** server-side (emisores publican; reactivos se
  suscriben en el arranque).
- `progression/RankService`: reactivo a `NinjaXpChangedEvent`; detecta el cruce de umbral y notifica.
- `progression/UnlockService` + `UnlockRegistry` + `UnlockReloadListener`: desbloqueos por hito **data-driven**
  (`data/dojutsu/unlocks/*.json`), reactivos a `SkillLevelChangedEvent`, **idempotentes**.

### EP-CLIENT-BASE — GUI y HUD (`client`, `network`)
- `client/keybind/KeyBindings`: `OPEN_MENU` (**M**, remapeable) vía Architectury `KeyMappingRegistry`; sondeo con
  `consumeClick()` (input real, sin polling de slot).
- `client/screen/NinjaMenuScreen`: GUI real (chakra, rango/ninjaXp, skill points, las 7 habilidades con "+").
  **Solo lectura** de `ClientNinjaData`; el "+" solo **solicita**.
- `network/MenuActionPayload` (C2S) + `network/handler/MenuActionHandler`: validación completa server-side
  (emisor, skill **por nombre**, cantidad acotada, legalidad, **rate-limit** con limpieza al desconectar).
- `network/ProgressionSnapshot` + extensión de `SyncNinjaDataPayload`: la progresión observable viaja al cliente.
- `client/hud/`: `ChakraHudOverlay` (barra en tiempo real), `RankHudOverlay`, `HudOverlays` (una suscripción a
  `ClientGuiEvent.RENDER_HUD` con guards: F1, espectador, sin sync) y `HudFormat` (compacto, puro/testeable).

## Decisiones arquitectónicas clave de F2 (y su porqué)

1. **Puerto `AttributeService` DIFERIDO.** La ficha de F2 modela `maxChakra` como campo de `ChakraState`, no como
   atributo vanilla → F2 **no lo necesita** (las notas previas de CLAUDE.md/handoff que lo daban por bloqueante
   eran más cautas que el diseño). Se retomará cuando un sistema requiera **modificadores de atributo** vanilla.
2. **Rango DERIVADO, no cacheado.** `ProgressionState.rank()` = `NinjaRank.fromXp(ninjaXp)`. Satisface "derivado,
   no fuente de verdad" **sin** un campo persistido que pueda desincronizarse. `RankService` solo detecta el cambio.
3. **Auto-nivelación REACTIVA, no por tick.** La nivelación ocurre **al escribir XP** (evento discreto), no
   sondeando el pipeline. Es lo que habilitó `LeveledStat` en F1 y lo que exige la checklist ("multi-nivel
   en una operación / sin guardia 1/tick"). **Ningún `PlayerTickSystem` de progresión.**
4. **`XpApi` = única puerta de XP.** Todo XP pasa por ahí → la auto-nivelación y la emisión de eventos ocurren en
   un solo sitio. `spend` enruta su ninja_xp por `XpApi` (así el rango reacciona).
5. **Eventos loader-nativos fuera de Architectury.** Comer/dormir **no** existen cross-loader en Architectury
   13.0.8 (verificado). Se suscriben en los **adaptadores** (`Dojutsu<Loader>ServerEvents`, patrón "push") y
   delegan en handlers comunes. NeoForge tiene ambos eventos; **Fabric necesitó el primer mixin** del proyecto
   (`LivingEntity#completeUsingItem`) → **la infraestructura de mixins de Fabric ya está montada** y es reutilizable.
6. **`setChakra` "inteligente" vs `setProgression` incondicional.** El chakra escribe un timer **cada tick**, así
   que su puerta marca observable **solo si cambia la proyección observable** (evita deltas de red por tick). La
   progresión solo cambia en eventos discretos → marca siempre.
7. **Sync:** `ProgressionSnapshot` **anidado** en `SyncNinjaDataPayload` (esquiva el límite de 6 componentes de
   `StreamCodec.composite`). El **rango no se transmite**: se deriva en cliente (el dominio es común). Los
   desbloqueos y la XP por habilidad **no** cruzan la red (no son observables).
8. **El cliente nunca decide gameplay.** El menú envía intención; el servidor valida y sincroniza; la UI se
   refresca **tras** el sync. La skill se resuelve **por nombre**, nunca por índice del cliente.
9. **Carga data-driven por reload listener** (`UnlockReloadListener`, `SimpleJsonResourceReloadListener`):
   establece el **patrón que reutilizarán F4/F5** para jutsus, ojos, clanes, etc.
10. **Bus de dominio compartido** (`DomainEvents`) para desacoplar emisores (XP) de reactivos (rango, unlocks).

## Convenciones (recordatorio)

Ver `docs/coding_conventions.md`. Dominio en `domain.*` (sin MC); estado en `data.*` (puede usar Codec/NBT);
motor en `core.*`; servicios de gameplay en su módulo (`chakra`, `progression`); `client.*` **solo cliente**
(invocado desde `initClient()`); comentarios/logs en español (logs sin acentos); **un ticket a la vez**; validar
en runtime en **ambos** loaders lo que toque plataforma.

**Patrón de sub-estado enchufable** (no reescribir el contrato central): value object con `DEFAULT`+`CODEC` →
campo + getter + **puerta de escritura** en `PlayerNinjaData` → 1 línea de codec con `optionalFieldOf`. F2 añadió
así `ChakraState` y `ProgressionState` **sin** bumpear `dataVersion` (la tolerancia cubre los saves viejos).

## Cómo se valida hoy

- **Unit:** `./gradlew :common:test` (**247 verdes**) — dominio, datos y servicios puros, con `NbtOps` sin
  bootstrap de Minecraft.
- **Build:** `./gradlew build` — multiloader verde.
- **Runtime:** tecla **M** (menú ninja), HUD de chakra/rango, y los comandos de depuración **temporales**:
  - `/dojutsuchakra get|consume <n>|restore <n>|full|regen <factor>`
  - `/dojutsuprogression get|award <n>|spend <skill> <n>|xp <skill> <n>|ninjaxp <n>`
  - `/dojutsuninja get|set <fase>`, `/dojutsusched count|in <ticks>`, `/dojutsu reset_ninja`

## Deuda / hooks abiertos para la Fase 3+

- **`addAffinityXp` (F3):** las afinidades comparten curva/escala (`XpCurve`) pero su estado vive en
  `NatureProfile`, que **no existe todavía**. `XpApi` lo añadirá cuando exista.
- **Factores de regen inactivos (F3/F6):** clan Otsutsuki/Uzumaki, ADN Hashirama y Modo Sabio 1/2 ya están en
  `RegenPolicy`, pero `RegenService.activeFactors()` devuelve vacío (no hay clanes/sabio). El clan **Jugo**
  (chakra natural) igual: `hasJugoClan()` → `false`. Enchufar ahí, sin tocar el resto. **Retirar entonces** el
  andamiaje `/dojutsuchakra regen <factor>`.
- **Reset de strain (F5/F6):** dormir y morir deben poner `eye_strain`/`eg_strain` a 0; los sub-estados no
  existen aún → hooks `// TODO F5/F6` en `SleepChakraHandler` y `DeathHandler`.
- **Contenido de desbloqueos (F4):** `UnlockService` registra el id pero **no entrega** el ítem de jutsu
  (`// TODO F4`). Los niveles de `data/dojutsu/unlocks/progression.json` son **provisionales**: extraer los
  umbrales reales del datapack.
- **`TBD_datapack` en `xp_sources.json`:** ninja_xp por auto-nivel, por naturaleza, por aprender jutsu, XP de
  afinidad por jutsu y las recompensas de los unlocks. Cuantificar al portar F3/F4 (**RA-4**: no improvisar).
- **Ramen +100 (F8):** `FoodChakraHandler` da +50 a toda comida; el ítem de ramen es del módulo `items`.
- **Puerto `AttributeService`:** cuando se necesiten modificadores de atributo vanilla (kenjutsu/puertas, F4/F6).
- **Deltas por-campo:** el delta reenvía todo lo observable (onboarding+chakra+progresión). Optimizar si crece.
- **Retirar el andamiaje de debug** cuando el menú/HUD cubran la observación (`/dojutsuninja`, `/dojutsusched`,
  `/dojutsuchakra`, `/dojutsuprogression`) y `entity.DebugMarkerEntity`.
- **Assets:** el HUD se dibuja con primitivas (sin texturas); el menú es funcional pero mínimo (RM-2:
  enriquecible). `EP-ASSETS` puede ir en paralelo (`[ADD/11]`).
- **`LICENSE`** acorde a la condición **no-comercial** de los assets de Prof Technic.
- **Confirmar contra el datapack:** senjutsu/cc sin auto-nivelación (postura `[ADD/00 §3]`).

## Próxima fase (F3 — Definición del ninja)

Épicas `EP-NATURES`, `EP-CLANS`, `EP-ONBOARDING`. Ficha completa:
`D:\Projectos claude\Dojutsu reverse\DOCS\backlog\04_FASE3_DEFINICION_NINJA.md`.

Orden del backlog (`00_INDICE §6`): `DJT-050` NatureType/KG registro → `DJT-051` NatureService (grant/revoke,
corrige el bug yang) → `DJT-052` KG data-driven → `DJT-053` naturalezas temporales → `DJT-054` NatureRoll (RNG
por pesos) → `DJT-055` LearnerService → `DJT-056` ClanDefinition (JSON, 14 clanes) → `DJT-057` ClanService.join
(bonus una vez) → `DJT-058` ClanService.reset (limpia TODO, corrige S04-R2) → `DJT-059` OnboardingController →
`DJT-060` NinjaCreationScreen → `DJT-061` jutsu de clan (stubs) → `DJT-062` team/nametag.

**DoD de F3:** crear un ninja de principio a fin (clan + afinidad), con bonus correctos y KG aleatorio; las
naturalezas dan learner y XP.

**Qué reutilizar directamente:**
- `NatureProfile` = **nuevo sub-estado enchufable** (mismo patrón que `ChakraState`/`ProgressionState`).
- Afinidades = `LeveledStat` + `XpCurve` (misma escala compartida, `[S13-R1]`) → añadir `XpApi.addAffinityXp`.
- `NatureRoll` = dominio `RollTable`/`WeightedEntry` (DJT-015, ya testeado).
- Clanes data-driven = patrón de `UnlockReloadListener` (JSON + reload listener + registry).
- Onboarding = `OnboardingState` (ya existe, con sus fases) + `NinjaCreationScreen` sobre el patrón de
  `NinjaMenuScreen` + `MenuActionPayload` (o un payload propio) validado en servidor.
- Bonus de clan al gastar puntos: el hook está en `SkillPointService` (hoy base +1/+2 ninja_xp).

## Recomendaciones para continuar

1. **Leer la fuente de verdad** (fuera de este repo): `D:\Projectos claude\Dojutsu reverse\DOCS\` →
   `sistemas/` (S01–S25), `arquitectura/` (ADD 00–11), `backlog/` (fases y fichas DJT-###).
2. **Un ticket a la vez**; validar en runtime en **ambos** loaders lo que toque plataforma/cliente.
3. **Reutilizar el dominio y los patrones** existentes; no meter imports de loader en `common`, ni de MC en
   `domain`. Al añadir un sub-estado, seguir el patrón enchufable (no reescribir el contrato central).
4. **Toda escritura de XP por `XpApi`**; toda escritura de chakra por `ChakraService`; toda acción del cliente,
   un payload **validado en servidor**.
5. Si un evento no existe cross-loader en Architectury, **suscribirlo en los adaptadores** (patrón "push");
   Fabric ya tiene infraestructura de mixins si hiciera falta.
6. Antes de tocar build/versiones/networking/eventos, leer `docs/development_log.md` (registra los problemas ya
   resueltos y por qué, incluido el bug de `isDay()` en el sueño y el bug S2C de Architectury).
7. Mantener el `build` multiloader verde y ampliar los tests (la lógica pura es testeable sin MC).
