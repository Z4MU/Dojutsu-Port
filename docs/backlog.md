# Backlog — Dojutsu Port

> Estado de las fases y tickets. El backlog completo y detallado (fichas por subtarea) vive en el
> repo de diseño: `D:\Projectos claude\Dojutsu reverse\DOCS\backlog\`. Aquí se refleja el progreso real.

## Leyenda
✅ terminado · 🔄 en curso · ⬜ pendiente

---

## Fase 0 — Fundaciones y andamiaje multiloader — ✅ COMPLETADA

Épicas: `EP-BUILD-CI`, `EP-PLATFORM`, `EP-REGISTRY-BASE`.

| Ticket | Descripción | Estado |
|--------|-------------|--------|
| DJT-001 | Estructura Gradle multiloader (Architectury) | ✅ |
| DJT-002 | Entrypoints y `MOD_ID` (common + adaptadores) | ✅ |
| DJT-003 | Puerto `RegistrarService` (registro diferido cross-loader) | ✅ |
| DJT-004 | Puerto `AttachmentService` + test con campo dummy | ✅ |
| DJT-005 | Puerto `NetworkService` (payloads C2S/S2C) + ping-pong | ✅ |
| DJT-006 | Registro base placeholder (`ModItems`/`ModEntities`/…) | ✅ |
| DJT-007 | Reload listener del datapack del mod (vacío) | ✅ |
| DJT-008 | CI: build + tests en ambos loaders | ✅ |

**DoD de fase alcanzada:** el mod carga en cliente y servidor dedicado en NeoForge y Fabric; los tres
puertos de plataforma validados en runtime; CI en verde. RM-6 desriesgado.

**Deuda/deferidos abiertos tras F0 (estado tras F1):**
- ⬜ Puerto `AttributeService` (adjuntar atributos a entidades vanilla como `Player`). *Sigue abierto (F2).*
- ✅ Retirar andamiaje temporal: dummy de attachment (DJT-019) y ping-pong (DJT-027) retirados. Quedan
  `DebugMarkerEntity` y los comandos `/dojutsuninja`/`/dojutsusched` (hasta el HUD de F2).
  *(`DebugMarkerEntity` retirada finalmente en F4/DJT-072, superada por `MarkerEntity`.)*
- ✅ Tests unitarios: 172 verdes (TDD del dominio/datos en F1).
- ⬜ `LICENSE` (assets de Prof Technic son **no-comerciales**).

---

## Fase 1 — Núcleo, dominio y estado — ✅ COMPLETADA

Épicas: `EP-DOMAIN`, `EP-DATA`, `EP-CORE`, `EP-NETWORK-BASE`.

| Ticket | Descripción | Estado |
|--------|-------------|--------|
| DJT-010 | `Resource` + `RegenPolicy` + `RegenFactor` (recurso/regen, TDD) | ✅ |
| DJT-011 | `StrainMeter` + `StrainThreshold` (fatiga con umbrales) | ✅ |
| DJT-012 | `ToggleableMode` + `ModeContext` (modo activable) | ✅ |
| DJT-013 | `Jutsu`/`JutsuDefinition`/`CastContext`/`CastResult` (contrato de técnica) | ✅ |
| DJT-014 | `LeveledStat`/`XpCurve`/`NinjaRank` (progresión, RA-4) | ✅ |
| DJT-015 | `RollTable` + `WeightedEntry` (RNG por pesos) | ✅ |
| DJT-016 | `DomainEventBus` + 8 eventos internos | ✅ |
| DJT-017 | `PlayerNinjaData` (agregado raíz) + `Codec` mínimo + `OnboardingState` | ✅ |
| DJT-018 | `DataVersion` + `MigrationChain` + `Migration` (idempotente) | ✅ |
| DJT-019 | `NinjaData.get` sobre `AttachmentService` (attachment real, codec migratorio) | ✅ |
| DJT-020 | `PlayerRef` (identidad por UUID) | ✅ |
| DJT-021 | `PlayerTickSystem` + `PlayerTickPipeline` + `TickPhase` | ✅ |
| DJT-022 | `ServerTickDispatcher` + `ModScheduler` | ✅ |
| DJT-023 | `DojutsuLifecycle` (punto único de suscripción) | ✅ |
| DJT-024 | `DeathHandler` + `RespawnHandler` (guards + hooks) | ✅ |
| DJT-025 | `DojutsuConfig` + detección de hardcore no destructiva | ✅ |
| DJT-026 | `AdminCommands` (`reset_ninja` + stubs) | ✅ |
| DJT-027 | `SyncNinjaDataPayload` (full+delta) + `DirtyTracker` | ✅ |
| DJT-028 | `ClientNinjaData` (cache read-only en cliente) | ✅ |

**DoD de fase alcanzada:** 172 tests unitarios verdes; `./gradlew clean build` multiloader verde; la demo de
aceptación (join→defaults→tick→persistencia→muerte/respawn con guards→reset→migración→sync full/delta→cache
cliente) validada en runtime en NeoForge y Fabric. Contrato central (`PlayerNinjaData`) estable y enchufable.

**Deuda/hooks abiertos tras F1 (para F2+):**
- ⬜ Puerto `AttributeService` (adjuntar atributos a `Player`, necesario para chakra).
- ⬜ Conectar la penalización de muerte real (½ chakra, strain=0) y la desactivación de modos en respawn
  (hooks ya listos en `DeathHandler`/`RespawnHandler`).
- ⬜ Deltas de sync por-campo cuando haya >1 campo observable; HUD real que consuma `ClientNinjaData`.
- ⬜ `GatherTimeLevel_*` (tuning sage) en el módulo sage; retirar `/dojutsuninja` y `/dojutsusched` al haber HUD.
- ⬜ `LICENSE` (assets de Prof Technic son **no-comerciales**).

---

## Fase 2 — Recursos y progresión (VERTICAL SLICE) — ✅ COMPLETADA

Épicas: `EP-CHAKRA`, `EP-PROGRESSION`, `EP-CLIENT-BASE`. Ficha de diseño completa:
`D:\Projectos claude\Dojutsu reverse\DOCS\backlog\03_FASE2_RECURSOS_PROGRESION.md`.

| Ticket | Descripción | Estado |
|--------|-------------|--------|
| DJT-030 | `ChakraState` + `ChakraService` (consume/restore/has, clamp, observable) | ✅ (validado en runtime) |
| DJT-031 | `RegenService` (fórmula/tabla `[S02]`, 1º del pipeline) | ✅ (validado en runtime) |
| DJT-032a | Reposición por **dormir** → chakra (evento nativo por loader, sin mixin) | ✅ (validado en runtime) |
| DJT-032b | Reposición por **comer** → chakra (mixin de Fabric para "fin de uso de ítem") | ✅ (validado en runtime) |
| DJT-033 | Penalización de muerte = ½ chakra (wiring `DeathHandler`) | ✅ (validado en runtime; cierra EP-CHAKRA) |
| DJT-034 | Auditoría economía (RA-4) + `ProgressionState` | ✅ (build+tests; abre EP-PROGRESSION) |
| DJT-035 | `SkillPointService` (kills → puntos; gasto → sube skill) | ✅ (validado en runtime) |
| DJT-036 | `LevelingService` (auto-nivelación por XP, reactivo) + `XpApi` | ✅ (validado en runtime) |
| DJT-037 | `RankService` (ninjaXp → rango, reactivo) | ✅ (validado en runtime) |
| DJT-038 | `UnlockService` + `UnlockRegistry` (desbloqueos por hito, reactivo, data-driven) | ✅ (validado en runtime; cierra EP-PROGRESSION) |
| DJT-039 | Keybind Open Menu + `MenuActionPayload` (C2S) | ✅ (validado en runtime) |
| DJT-040 | `NinjaMenuScreen` (GUI real: estado + gasto de puntos) | ✅ (validado en runtime) |
| DJT-041 | HUD de chakra y rango (overlays) | ✅ (validado en runtime; cierra EP-CLIENT-BASE) |
| DJT-042 | Handler servidor de `MenuActionPayload` (gasto validado) + sync de progresión | ✅ (validado en runtime end-to-end) |

**DoD de fase ALCANZADA** (demo end-to-end validada en NeoForge y Fabric): chakra regenera (barra HUD) →
kills dan skill points → tecla **M** abre el menú (GUI real) → gasto validado en servidor → sube nivel/rango →
comer/dormir repone → muerte deja ½ chakra → hitos desbloquean. Todo persiste al reloguear.
**Hito de validación arquitectónica superado** (`[ADD/08 §6.2]`): el resto es contenido incremental.
**247 tests verdes**; `./gradlew build` multiloader verde. Ver `docs/handoff_phase2.md`.

**Decisión de fase:** F2 siguió el backlog de diseño — chakra con `maxChakra` como campo de `ChakraState`,
**sin** el puerto `AttributeService` (diferido hasta que un sistema requiera modificadores de atributo vanilla).

**Orden recomendado** (dependencias): 030 → 031 → 032a → 032b → 033 → 034 → 035 → 036 → 037 → 038 → 039 → 042 → 040 → 041.

**Nota (DJT-032 dividido):** los eventos "comer" y "dormir" **no son cross-loader** en Architectury 13.0.8
(verificado). Se abstraen en los adaptadores por loader (`[ADD/05 §línea 6]`). **032a (dormir)** usa eventos
nativos sin mixin (NeoForge `PlayerWakeUpEvent`, Fabric `EntitySleepEvents.STOP_SLEEPING`). **032b (comer)**
necesita un **mixin de Fabric** (fabric-api no tiene evento de "terminar de comer"; NeoForge sí:
`LivingEntityUseItemEvent.Finish`). El `+100` de ramen depende del ítem (módulo `items`, F8) → diferido.

---

**Deuda/hooks abiertos tras F2 (para F3+):** ver `docs/handoff_phase2.md` §Deuda. Resumen: `addAffinityXp` +
`NatureProfile` (F3); factores de regen (clan/ADN/sabio) y Jugo inactivos (F3/F6); reset de strain (F5/F6);
entrega real de desbloqueos y umbrales reales (F4); `TBD_datapack` en `xp_sources.json`; ramen +100 (F8);
puerto `AttributeService`; deltas por-campo; retirar andamiaje de debug; assets/texturas; `LICENSE`.

---

## Fase 3 — Definición del ninja — ✅ COMPLETADA (validada en runtime; ver `docs/handoff_phase3.md`)

Épicas: `EP-NATURES`, `EP-CLANS`, `EP-ONBOARDING`. Ficha de diseño completa:
`D:\Projectos claude\Dojutsu reverse\DOCS\backlog\04_FASE3_DEFINICION_NINJA.md`.

| Ticket | Descripción | Estado |
|--------|-------------|--------|
| DJT-050 | `NatureType` + `KekkeiGenkai` + `NatureProfile` (sub-estado, afinidades como `LeveledStat`) | ✅ (build+tests; abre EP-NATURES; sin comportamiento runtime aún) |
| **DJT-063** | **`LeveledStat` inmutable** (deuda técnica de F1; refactor sin cambio de comportamiento) | ✅ (build+tests) |
| DJT-051 | `NatureService` (grant/revoke/has) + corrección bug `give/kg/yang` | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-052 | Kekkei Genkai data-driven (naturalezas-componente, JSON) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-053 | Naturalezas/KG **prestados** (`TemporaryGrants`, por fuente; corrige S03-R3) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-054 | `NatureRoll` (RNG con los pesos de las loot tables, data-driven + validado) | ✅ (validado en runtime: distribución en juego + rechazo cubierto por tests del listener) |
| DJT-055 | `LearnerService` + `LearnerItem`/`LearnerComponent` (cierra EP-NATURES; 1er data component) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-056 | `ClanDefinition` (JSON, 14) + `ClanRegistry` + `ClanMembership` (abre EP-CLANS) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-057 | `ClanService.join` (bonus una vez) + Namikaze determinista + regen de clan enchufada | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-058 | `ClanService.reset` (defaults atómicos de los 4 sub-estados; corrige S04-R2) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-061 | Jutsu de clan (desbloqueo progresivo, UNA vez por tick; corrige la doble ejecución) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-062 | Team / nametag por clan, namespaceado y sin pisar teams del servidor (cierra EP-CLANS) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-059 | `OnboardingController` (máquina de estados) + `OnboardingChoicePayload` (abre EP-ONBOARDING) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-060 | `NinjaCreationScreen` + `OnboardingOptionsPayload` + clan aleatorio (cierra EP-ONBOARDING y la fase) | ✅ (validado en runtime en NeoForge y Fabric) |

**Orden acordado** (dependencias): 050 → **063** → 051 → 052 → 053 → 054 → 055 → 056 → 057 → 058 →
**061 → 062** → 059 → 060. Desviaciones respecto a `00_INDICE §6`: (1) 061/062 se adelantan al onboarding (solo
dependen de 056/057, y así `ClanService` queda cerrado antes de que 059 lo consuma); (2) **DJT-063 es un ticket
nuevo** (no del backlog de diseño): salda una deuda técnica de F1 detectada en la autorrevisión de DJT-050 y se
ejecuta antes de DJT-051, que es el primer escritor de afinidades. **Los ids son orden de asignación, no de
ejecución.**

**DoD de fase:** crear un ninja de principio a fin (clan + afinidad), con bonus correctos y KG aleatorio; las
naturalezas dan learner y XP; el caso Lee salta la afinidad.

**Decisiones de fase (acordadas al abrir F3):**
- Se **mantienen los nombres de fase actuales** de `OnboardingState.Phase` (`NOT_STARTED`/`SELECTING_CLAN`/
  `SELECTING_AFFINITY`/`COMPLETED`); el "FINISH" de la ficha es el paso de transición a `COMPLETED`, no una fase.
- El módulo `items` **nace parcialmente en F3** (DJT-055, solo el learner), aunque `[ADD]` lo situaba en F5–F9.
- **`LeveledStat` es inmutable** (DJT-063): nunca se muta, se reemplaza (`withXpAdded` devuelve copia). Es la
  única decisión de F1 revisada en F3, con aprobación explícita y sin cambio de comportamiento.
- **La afinidad inicial de 50 es del ONBOARDING, no del `grant`** (DJT-051, aprobado tras exponer la evidencia).
  La ficha de diseño pedía que `NatureService.grant` fijase 50: es una **mala lectura de `[S03]`**. En el
  datapack, `natures/give/<nat>` no toca la afinidad; el 50 lo fijan `nature_select_main`/`random_aff` antes de
  llamar al give. Prueba: `join_uchiha` hace `give/fire` y **luego** `aff_fire += 10` → con el 50 en el grant, un
  Uchiha tendría 60 en vez de 10. **DJT-059 aplica el 50; DJT-057 suma los bonus encima.**
- **`addAffinityXp` no es de F3** (pese a lo que decía el handoff de F2): en el datapack la XP de afinidad se
  gana **usando jutsus** (`jutsu/training/*`) → su emisor es **F4/DJT-078**. Sin llamador, sería API muerta.
- **Las naturalezas "temporales" son PRESTADAS, no caducan** (DJT-053, aprobado tras exponer la evidencia). La
  ficha pedía expiración por tick/timestamp: **el datapack no tiene ningún temporizador**. Solo
  `jinchuriki/sealing/*` marca `d.tmp_*` (y únicamente si no tenías ya la naturaleza) y solo
  `jinchuriki/removal/*` la limpia. Se modela la **fuente** del préstamo, sin timestamps ni tick. Su consumidor
  real es **F7** (bijuu); `d.tmp_sp_chakra` es de **F6** (sage).
- **Patrón:** una ficha de diseño puede equivocarse en el **qué** (números, ramas, mecánicas) porque resume el
  datapack; en 4 tickets de F3 han aparecido 4 desviaciones. **Verificar siempre contra el datapack antes de
  implementar** (regla 16 de `CLAUDE.md`).

---

## Fase 4 — Combate — ✅ COMPLETADA

> **Validación runtime (2026-07-19):** build verde + **541 tests** (0 fallos); servidor dedicado arrancado en
> **NeoForge y Fabric** con carga limpia bajo el codec estricto (**128 jutsus · 18/18 KG · 22 secuencias · 14
> clanes**), 0 ERROR del mod, familias de efecto incl. `primed_strike`. Los tickets que figuraban "build+tests;
> pendiente validación runtime en ambos loaders" quedan **validados en su dimensión de carga**; residual =
> spot-checks de casteo/visual in-game (checklist en `docs/known_issues.md`). Detalle en `current_state.md §3.4a`.

Épicas: `EP-ENTITY`, `EP-JUTSU-ENGINE`, `EP-KENJUTSU`, `EP-JUTSU-CONTENT`. Ficha de diseño completa:
`D:\Projectos claude\Dojutsu reverse\DOCS\backlog\05_FASE4_COMBATE.md`. Riesgos que desriesga: **RM-4**
(entidad-proyectil) y **RA-1** (que el patrón data-driven escale a ~200 técnicas).

| Ticket | Descripción | Estado |
|--------|-------------|--------|
| DJT-070 | `NinjaProjectile` base (física + colisión por raycast + dueño UUID) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-071 | `AbilityAnchor` + `MarkerEntity` (ancla posicional + marcador transitorio) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-072 | Consolidación `ModEntities` + renderers (retira `DebugMarkerEntity`; cierra EP-ENTITY) | ✅ (validado en runtime en NeoForge y Fabric — **EP-ENTITY completa**) |
| DJT-073 | `JutsuRegistry` + carga de `JutsuDefinition` (reload, refs validadas) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-075 | `JutsuKnowledge` + aprendizaje secuencial con learner + `JutsuItem`/`JutsuComponent` | ✅ (validado en runtime en NeoForge y Fabric; incl. fix del re-embarque de `reset_ninja`) |
| DJT-074 | `JutsuCastPipeline` (chakra→cd→**naturaleza**→afinidad→efecto, caster genérico) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-076 | Familias de efecto (las 6 del ADD, con un jutsu real por familia; +hallazgo `primed_strike`) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-077 | Cooldowns por-instancia (tick absoluto de mundo, poda perezosa; corrige S06-R3) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-078 | `JutsuCastEvent` → `NatureService.addAffinityXp` (+1/cast, bucle de progresión maestro) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-079 | `LastCastRegistry` (copia Sharingan, acotado por dimensión/distancia/tiempo; corrige S06-R6) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-080 | Keybind CastJutsu (V) + `CastJutsuPayload` (C2S) + handler validado (+cast por click derecho) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-081 | HUD de cooldown del jutsu equipado (cierra EP-JUTSU-ENGINE salvo `primed_strike`) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-082 | `WeaponComponent` + `ItemAttributeModifiers` reversibles (bonus por kenjutsu; corrige S07-R2; abre EP-KENJUTSU) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-083 | Técnicas de espada (jutsus categoría kenjutsu; **requisito de nivel de kenjutsu**, no de arma; +5 JSON) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-084 | Samehada (absorción de chakra al golpear: +100 de mob, drena de jugador; **cierra EP-KENJUTSU**) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-085 | Volcado Fuego + Rayo (10 JSON nuevos, 24 jutsus totales; valida RA-1; abre EP-JUTSU-CONTENT) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-085b | Assets de jutsus (24) y armas (3): texturas/modelos del resource pack vía `custom_model_data` vanilla (ticket de assets) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-085c | Texturas del Skill Learner (24 elementos, `custom_model_data`) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-085d | Familia `primed_strike` (golpe cargado: chidori/lightning_bolt/lightning_cutter/drilling arman y descargan al golpear) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-086 | Volcado Agua (7 jutsus + secuencia learner + texturas; `water_cloak`→F6) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-087 | Volcado Tierra (6 jutsus + earth_wall + secuencia + texturas; `earth_golem`→structure, real es F9; `earth_cloak`→F6) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-088 | Volcado Viento (9 jutsus + vacuum_wave + secuencia + texturas; rasengan/big_ball→primed_strike; wind_mode/cloak→buff aprox, real F6) — **5 básicas completas** | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-089 | Volcado Yin/Yang (4 jutsus Sexto Camino; learners yin/yang solos eran placeholder; aff_yinyang capada a 500 derivada; creation_of_all_things=heal; split→F7) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-090 | Volcado Taijutsu (4 jutsus de skill puro; sin learner→hitos progression.json; skill /4 como kenjutsu; Ocho Puertas→F6) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-091 | Contrato de jutsu para KG (afinidad múltiple `List<AffinityRequirement>` + `KekkeiGenkaiRequirement`; codec objeto-o-array; gate `check_kekkei_genkai`) — infraestructura, sin volcado | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-092 | Volcado Madera/Mokuton (8 jutsus, primer KG; gate wood + earth&water; XP multi-naturaleza; casi todos structure) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-093 | Volcado Hielo (5 jutsus, KG water+wind; area+structure; ice_golem→structure, summon real F9) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-094 | Volcado Lava (3 jutsus, KG earth+fire; snake_bullet→projectile, resto area) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-095 | Volcado Cristal (4 jutsus, KG earth+water+yang, primer KG de 3 afinidades; structure/projectile/area) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-096 | Volcado Tormenta/Storm (3 jutsus, KG lightning+water; area/projectile; weather real no modelado) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-097 | Volcado Ebullición/Boil (4 jutsus, KG water+fire; projectile/primed_strike/area corrosiva) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-098 | Volcado Magnetismo/Magnet (6 jutsus arena, KG wind+earth; structure/projectile/area) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-099 | Volcado Explosión/Explosion (7 jutsus, KG lightning+earth; C de Deidara; clay_doll→area sin CMD, summon F9) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-100 | Volcado Abrasación/Scorch (3 jutsus, KG fire+wind; projectile/area; great_steaming sin CMD) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-101 | Volcado Acero/Steel (4 jutsus, KG fire+earth+water, 2º KG de 3 afinidades; projectile/buff/structure/area) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-102 | Volcado Oscuridad/Dark (2 jutsus, KG wind+yin; projectile/area drenaje) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-103 | Volcado Rapidez/Swift (2 jutsus, KG wind+lightning; buff/teleport) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-104 | Volcado Tifón/Typhoon (2 jutsus, KG wind+water; area) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-105 | Volcado Lodo/Mud (3 jutsus, KG earth+water; structure/area; mud_doll summon F9) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-106 | Volcado Ígnea/Blaze (2 jutsus, KG solo posesión sin afinidad; projectile/area; kagutsuchi_blade es arma→F4 armas) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-107 | Volcado Polvo/Dust (1 jutsu, último KG, 3 afinidades a 500; area; sin CMD) — **16 KG completos** | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-108 | Volcado Genjutsu (6 jutsus, skill genjutsu ÷4 + afinidad yin; area de debuffs; hitos progression.json) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-109 | Volcado Medical (4 jutsus, skill medical ÷4, familia heal; creation_rebirth +yin) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-110 | Volcado Fuinjutsu (3 jutsus, skill fuinjutsu ÷4 + yang; area/structure; unlocks alineados a auditado. summoning→F9) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-111 | Volcado Aburame (2 jutsus insecto, sin gate; entregados por el clan) — **cierra el volcado de contenido F4** | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-112 | **Cierre formal de F4**: repaso DoD + limpieza andamiaje (docs obsoletos corregidos; `ProjectileDebugCommand` eliminado por DJT-076; 10 comandos debug conservados para F5+) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |

**Diferido a fases posteriores:** dōjutsu (sharingan_izanagi, sharingan/ketsuryugan_taming_genjutsu, cloak_of_invisibility, bansho_tenin_item_pull) → **F5** (sistema de ojos); modos/Ocho Puertas (chakra_cloak, front_lotus, morning_peacock, daytime_tiger, evening_elephant, night_guy) + cloaks → **F6**; creation_of_all_things_split (Diez Colas) → **F7**; summoning y summons de entidad → **F9**.

**Diferido a F6 (subsistema Ocho Puertas):** front_lotus, morning_peacock, daytime_tiger, evening_elephant, night_guy — exigen puerta abierta (`eight_gates/stage_N_8`).

**Orden acordado** (dependencias): 070 → 071 → 072 → 073 → **075 → 074** → 076 → 077 → 078 → 079 →
080 → 081 → 082 → 083 → 084 → 085 → 086+. Desviaciones respecto al orden por id, acordadas al abrir F4:
(1) **075 se adelanta a 074** — crea el sub-estado `JutsuKnowledge` que el pipeline consulta (conocido/cooldown),
así 074 nace sin stubs; (2) las **dependencias circulares de la ficha se resuelven así**: 070↔072 — la propia
ficha de 070 incluye su registro en `ModEntities`, y 072 queda como consolidación/validación; 074↔076 — la
interfaz `JutsuEffect` y su resolución nacen en 074 (con efecto no-op), las implementaciones en 076.

**DoD de fase:** aprender (learner) y lanzar (keybind, payload validado) jutsus de fuego y rayo con
coste/cd/afinidad correctos → usar jutsu da XP de afinidad (sube nivel → daño y desbloqueos) → HUD de cooldown →
la entidad-proyectil impacta por colisión real → kenjutsu con bonus reversible (sin atributo pegado).

---

## Fase 5 — Poderes oculares — ✅ LÓGICA COMPLETADA (DJT-113…125; solo faltan los modelos 3D del Susanoo, assets)

Épicas: `EP-EYE-CORE`, `EP-EYE-ABILITIES`, `EP-SUSANOO`, `EP-VISION`. Ficha de diseño completa:
`D:\Projectos claude\Dojutsu reverse\DOCS\backlog\06_FASES5-10_RESUMEN_Y_PLANTILLAS.md §FASE 5`.
Sistemas de verdad: `DOCS/sistemas/S05_dojutsus.md`, `S24_susanoo.md`, `S25_chakra_vision.md` (verificar SIEMPRE
los valores/ramas contra el **datapack**, no contra el resumen — regla 16). Riesgo que desriesga: **RA-3**
(evolución divergente de ojos; anclas de habilidad).

> **Nota de numeración:** el backlog de diseño planificó F5 como DJT-100…125, pero esos ids ya los consumió el
> volcado de contenido de F4. **Se renumeran a la serie libre DJT-113+** (columna "ID diseño" para trazabilidad).
> Los ids son orden de asignación, no de ejecución.

| Orden | Ticket | (ID diseño) | Épica | Descripción | Estado |
|------|--------|-------------|-------|-------------|--------|
| 1 | DJT-113 | DJT-100 | EP-EYE-CORE | `EyeState` + `EyeDefinition` (grafo de evolución data-driven; 33 índices → ~12 ojos + `eternal`) `#riesgo-alto` | ✅ (validado en runtime en NeoForge y Fabric — 7 ojos cargan) |
| 2 | DJT-114 | DJT-101 | EP-EYE-CORE | `AwakeningService` (despertar por `potentialDojutsu` del clan; `EyeAwakenedEvent` + XP) | ✅ (build+8 tests; cableado validado en runtime en ambos loaders; end-to-end time-gated = check manual) |
| 3 | DJT-115 | DJT-102 | EP-EYE-CORE | `EvolutionService` (evolución por uso, transición por grafo; `EyeEvolvedEvent`) | ✅ (build+8 tests; cableado validado en runtime en ambos loaders; end-to-end time-gated = check manual) |
| 4 | DJT-116 | DJT-103 | EP-EYE-CORE | `EyeStrainService` (fatiga ocular: regen por pulso; 95 weakness/98 blindness/100 apaga; dormir resetea) | ✅ (build+13 tests; cableado validado en runtime en ambos loaders; efectos end-to-end esperan emisor de strain DJT-118+ = check manual) |
| 5 | DJT-117 | DJT-104 | EP-EYE-CORE | Menú de habilidades oculares (`AbilitySelectScreen` + payloads C2S/S2C validados) | ✅ (build+6 tests; servidor/payloads validados en runtime en ambos loaders; GUI in-game = check manual) |
| 6 | DJT-118 | DJT-110 | EP-EYE-ABILITIES | Interfaz `EyeAbility` + piloto **Amaterasu** (`AbilityAnchor`) — establece la plantilla `#plantilla` | ✅ (build+6 tests; servidor/payload validados en runtime en ambos loaders; cast in-game = check manual) |
| 7 | DJT-119+ | DJT-111+ | EP-EYE-ABILITIES | Resto de habilidades clonando la plantilla `#plantilla` — **catálogo declarado COMPLETO: 21 habilidades** (lotes 1-6 + Susanoo + Tenseigan Chakra Mode) | ✅ (build+21 tests; servidor validado en runtime en ambos loaders; cast in-game = check manual) |
| 8 | DJT-120 | DJT-120 | EP-SUSANOO | `SusanooService` (`ToggleableMode` + `SusanooCostTable` niveles 1..6) | ✅ (build+26 tests; servidor validado en runtime en ambos loaders; ciclo in-game = check manual) |
| 9 | DJT-121 | DJT-121 | EP-SUSANOO | `SusanooAvatar` (ancla parenteada + silueta por etapa; despawn al desactivar) + escalera de etapas (`susanoo_stg`) | ✅ (build+5 tests; servidor validado en runtime en ambos loaders; avatar in-game = check manual). **Pendiente aparte**: los 7 modelos 3D por personaje (`susanoo_act`) son trabajo de **assets** |
| 10 | DJT-125 | DJT-125 | EP-VISION | `ChakraVisionService` + toggle (tecla C / `ToggleVisionPayload`) + glow **nativo** por relación (sin renderer propio ni selector `@e`; Sharingan base sin vision) | ✅ (build+10 tests; servidor validado en runtime en ambos loaders; glow in-game = check manual) |

**Orden y justificación (dependencias):** 113 primero e incondicional (sub-estado + grafo del que dependen todos;
además es el ticket de riesgo alto → desriesga la fase). 114→115 (no se evoluciona lo no despertado). 116 y 117
dependen solo de 113 (paralelizables). 118 requiere 117 (menú) + `AbilityAnchor` (DJT-071, ya existe) y fija la
plantilla; 119+ la clonan. Susanoo (12x) y Vision (12x) dependen de 113 y de motores existentes; cierran la fase.

**DoD de fase:** despertar el Sharingan por clan, evolucionarlo por uso, usar Amaterasu/Susanoo, sufrir eye strain
(apaga/ciega al 100%, reset al dormir) y activar la chakra vision — todo fiel a `[S05/S24/S25]` y persistente.

**Decisiones de DJT-113** (verificadas contra el datapack, regla 16):
- **Colapso 33→lógicos (R4):** el flag `eternal` (EMS) vive en el *estado* (`EyeState.activeEye` + `eternal`), no
  como nodos duplicados; los tomoe 1/2/3 del Sharingan son nivel interno (`useLevels`), no ojos aparte.
- **Rutas divergentes (R6) como datos, no ramas:** cada evolución lleva `requirements` (`use_level`/`clan`/`flag`).
  El Sharingan→Mangekyō exige `clan=otsutsuki_uchiha` + `use≥360000` (`sharingan_use_level_4`); el implante no lo
  cumple y se queda en 3 tomoe. Sharingan→Rinnegan: `clan=uchiha` + `flag=hashirama_dna`. Byakugan→Tenseigan:
  `clan=otsutsuki` + `use≥864000` (`tenseigan_req2`).
- **Bonus de despertar auditados:** Byakugan/Sharingan +20/+20, Ketsuryugan +30/+30, Tenseigan +100/+100,
  Rinnegan +100/+150 (extraídos de `<eye>/awaken.mcfunction`).
- **Placeholder `Eye:18` (R7):** `naka.json` con `implemented=false` carga sin romper.
- **Alcance:** 7 ojos representativos que prueban el modelo (grafo + eternal + placeholder + rutas divergentes);
  el resto del roster (otros Mangekyō por personaje, Isshiki, Jougan, Rinne Sharingan, variantes Rinnegan) se
  vuelca como contenido en tickets posteriores de F5, clonando el mismo esquema.

**Cierre de flecos de F5 — fleco 4: EMS/eternal manual** (`EternalAwakeningService`) — CIERRA LOS FLECOS DE F5:
- El EMS era acción manual del datapack (herramientas quirúrgicas) sin implementar. Ahora es una **acción del
  menú ocular** (botón "Awaken Eternal Mangekyo" → `AwakenEternalPayload` → validación server-side). Requisitos
  verificados: ojo eternalizable + no-eterno-ya + clan `otsutsuki_uchiha` + uso ≥864000 (`ems_req`) + medical ≥150.
  Bonos: +60 max_chakra, +150 ninja_xp, +78000 susanoo_use (`SusanooState.plusUse`).
- **Matiz de modelado:** `ems_req` es sobre `sharingan_use` continuo; el Mangekyō (familia propia, terminal) no
  acumulaba uso. Solución: `use_level: 864000` en `eternalRequirements` + `EvolutionService` acumula uso en ojos
  con uso-gate eterno (`hasUseGatedEternal`). Divergencia: el uso cuenta desde que se alcanza el Mangekyō.
- Decisión pura `evaluate` (6 salidas, testeable); el flag `EyeState.eternal` por fin se enciende por acción.

**Cierre de flecos de F5 — fleco 3: avisos de fatiga en chat (`esw_*`)** en `EyeStrainService`:
- El servicio (DJT-116) los dejó como "presentación diferida"; ahora los emite fiel a `eye_strain/main`: al
  entrar en una banda, `Eye Strain: N%` **una vez** (anti-spam `esw_25/50/75/90/99`), color por banda (25 verde,
  50 amarillo, 75 oro, 90 rojo, 99 rojo oscuro) + `player.hurt` tenue. El flag se limpia al caer bajo su suelo o
  sin ojo (`no_dojutsu`); a 100 avisa el force_deactivate aparte (+2ª línea `(N%)` que faltaba).
- Decisión pura `strainWarningFloor` (testeable); el "ya avisado" es de ejecución (`forgetWarnings` al
  desconectar; divergencia menor: el datapack persiste los tags, aquí reconectar re-avisa una vez).

**Cierre de flecos de F5 — fleco 2: refund "sin objetivo" (`no_cast_nby`)** en el pipeline ocular:
- El pipeline cobraba chakra/fatiga **antes** de `execute` → una habilidad de objetivo único sin blanco
  malgastaba el coste. Ahora `EyeAbility` tiene `canApply(ctx)` (default `true`), que el pipeline evalúa **tras
  el cooldown y antes de cobrar**; `false` → "Nothing nearby", **sin consumir**. Refleja el `canApply`/`NO_TARGET`
  del pipeline de jutsus (DJT-074).
- **Verificado (regla 16):** de nuestro catálogo, solo tres llevan `no_cast_nby` en el datapack —
  `blood_eruption`, `exploding_human`, `genjutsu_stun`. **Tsukuyomi no** (aunque sea caro y single-target) → no
  se le añade. Cada una extrae un `target(ctx)` privado compartido por `canApply` y `execute` (no divergen).
- Simplificación anotada: el datapack pone un cd corto (10t) al fallar; aquí no (el rate-limit de 200 ms basta).

**Cierre de flecos de F5 — fleco 1: `tenseigan_chakra_mode`** (última habilidad declarada):
- **Segundo consumidor de `ToggleableMode`** tras el Susanoo, y última habilidad del catálogo declarado (21 en
  total). Es un **modo toggle** (no efecto de un disparo): buffs + **vuelo** mientras se sostiene, pulso de **10
  chakra/200t**, **cae si el chakra < 19** (verificado en `tenseigan/chakra_mode/main`). Sin fatiga (Tenseigan).
- Se enciende desde el **menú ocular + R** (`TenseiganChakraModeAbility`), sin tecla nueva. El encendido es de
  ejecución (no se guarda); el vuelo se retira al apagar, al caer por chakra y al desconectar (guardando creativo/
  espectador). Buffs verificados: Fuerza II, Velocidad II, Salto IV, Resistencia II.
- **Nota F6:** es el 2º servicio que replica el bucle "tick el modo → si cae, limpia". `EP-TOGGLE-FRAMEWORK`
  (F6) es el sitio natural para extraer un arnés compartido; hasta entonces se mantiene el patrón.

**Decisiones de DJT-125** (chakra vision — cierra la lógica de F5):
- **Glow nativo + equipos, no renderer propio ni selector por tick (RB-3).** El datapack corría
  `execute at @e[type=#tag] run particle` cada tick y por categoría. Aquí un `PlayerTickSystem` reescanea **cada
  10 ticks** (no cada tick) y marca las entidades con el efecto vanilla `GLOWING` + un **equipo de scoreboard**
  cuyo color pinta el contorno; el cliente lo dibuja solo. Resultado: **no hace falta `ChakraVisionRenderer`**
  (el spec lo sugería) — el contorno nativo de vanilla es exactamente lo que RB-3 quería, y sale gratis en ambos
  loaders. Un barrido retira del equipo lo que ya nadie renueva; el glow caduca por sí mismo (TTL 30t).
- **Clasificación estática, no selector por tick.** `ChakraVisionRelations` traduce a código puro los 4
  `tags/entity_types/chakra_highlights/*` del datapack (special/friendly/neutral/hostile). Un tipo no listado no
  se resalta — fiel: el datapack tampoco resaltaba lo que no estaba en una etiqueta.
- **Disponibilidad data-driven** (`EyeDefinition.chakraVision` + `chakraVisionUseLevel`): byakugan/mangekyo/
  rinnegan/tenseigan la dan siempre; el **Sharingan solo maduro** — 3 tomoe, uso **≥216000** (verificado:
  `sharingan_use_level_3` = 216000..); Ketsuryugan **no** (no corre `chakra_vision` en el datapack). Así se
  preserva "Sharingan base sin vision" con un número real, no inventado.
- **Activa por defecto, se apaga a voluntad** (fiel a `d.no_cv`): quien lleva un ojo que la da la tiene encendida;
  la **tecla C** (`ToggleVisionPayload`) la silencia. La preferencia (`VisionState.chakraVisionOff`) **persiste**,
  como el tag del datapack. El servidor decide si aplica (el cliente solo envía la intención, `[ADD/07]`).
- **Divergencia asumida:** el glow nativo es **global** (lo ven todos), frente al `particle ... force @s` que solo
  veía el observador. Es el precio de no usar un selector por tick; se acepta como parte de RB-3.

**Decisiones de DJT-121** (avatar y escalera de etapas):
- **El avatar es un `AbilityAnchor`, no un tipo de entidad nuevo.** El ancla de F4/DJT-071 ya se diseñó como
  "base de las habilidades ancladas de F5" y su contrato contempla literalmente este caso ("un sistema que
  necesite un ancla mientras el modo esté activo la descarta él mismo al desactivar"). **Corrige RA-3**: el
  datapack usaba un `armor_stand` tageado `d.narr` con un `knowledge_book{CustomModelData}` en la cabeza, que
  quedaba colgado si su limpieza no corría; aquí la caducidad es por construcción y el servicio lo descarta al
  apagar, al caer por chakra agotado y al desconectar.
- **Etapa ≠ nivel.** Es la separación del datapack: `set_model` mira `susanoo_stg` (la etapa manifestada) y
  `effects` mira `susanoo_lvl` (la maestría). Aquí igual: la **etapa** decide el tamaño de la silueta, el
  **nivel** decide coste y protección. La etapa es estado de ejecución; la maestría persiste.
- **La escalera** (`advance.mcfunction`): cada pulsación hace crecer una etapa con **tope en el nivel dominado**.
  **Divergencia consciente (regla 16):** al llegar al tope el datapack no hacía nada y se apagaba desde el menú;
  aquí la siguiente pulsación **lo apaga**, porque con una sola tecla una escalera sin salida dejaría al jugador
  sin forma de bajarse. La decisión (`decide`) es pura y está testeada.
- **Alcance honesto:** esto entrega la **presencia** del avatar (entidad, seguimiento, silueta creciente por
  etapa, despawn garantizado), no sus modelos 3D. Los siete modelos por personaje (`CustomModelData`
  1000..7000 + etapa) son **trabajo de assets** aún sin portar, como en su día lo fueron los modelos de los
  ítems de jutsu; hasta entonces la silueta se dibuja con partículas, que es como se representan el resto de
  entidades del mod (ninguna tiene renderer todavía).

**Decisiones de DJT-120** (Susanoo como modo activable):
- **Primer consumidor de `ToggleableMode`.** El framework (patrón 2 del ADD) existía desde F1 sin clientes; el
  Susanoo lo estrena y confirma que su único automatismo —"coste impagable → castigo + apagado"— es exactamente
  lo que pide `main.mcfunction` (`if no_chakra run deactivate`). El resto (sage S08, sellos S10, puertas S11,
  SOAH S12, formas S09) clonará este montaje.
- **La curva de coste va al revés de lo esperable:** sostenerlo es **más barato cuanto mayor el nivel** (6
  chakra/20t en Ribcage → 1 chakra/80t en Perfect), y la protección sube (`resistance`/`fire_resistance` amp
  `nivel-1`). Es maestría, no error de lectura. `slowness` solo en los niveles 3 y 4 (los que lastran).
- **El EMS paga tarifa plana:** con `wearing_eternal` el datapack **salta todo el bloque por nivel** — 0 chakra y
  +1 de fatiga cada 160t a cualquier nivel. Mismo privilegio que ya exime de fatiga en las habilidades.
- **Cobro por pulsos, no por tick** (`d.susanoo_time`): el modo decrementa un temporizador y solo cobra al vencer.
- **Sin tecla nueva:** `susanoo` ya es una habilidad declarada de `mangekyo_itachi.json`, así que se enciende
  desde el menú ocular + R (`SusanooAbility`, coste 0 porque el chakra se paga por pulso; cooldown 14t del
  `activate`). El chakra mínimo 5 es **puerta, no cobro**: lo veta `onActivate`, no lo consume nadie.
- **`SusanooState` es sub-estado propio** de `PlayerNinjaData` (S24 ≠ S05), no un campo de `EyeState`. Persiste
  **solo la maestría** (nivel+uso): el encendido y su pulso son estado de ejecución, como el `check_deactivate`
  que lo apaga al perder el ojo.
- **Divergencia consciente (regla 16):** en el datapack el uso solo se acumula `unless susanoo_lvl_3p`, lo que
  deja los **niveles 4, 5 y 6 inalcanzables** — sus `level/set_N`, costes y efectos son código muerto. Se trata
  como defecto de S24 y aquí la maestría sube hasta 6, que es lo que la propia tabla describe.

**Decisiones de DJT-119+** (resto de habilidades, lote 6 — cierra el catálogo declarado):
- **El coste no siempre es chakra.** El `sharingan/cost.mcfunction` es el goteo **pasivo** del ojo activo, no el
  coste de sus habilidades: `stun/cast` y `copy_jutsu` **no cobran nada** y se gatean por **cooldown**
  (`d.sharstun_cd 200`) y por condición. Por eso el contrato gana **`EyeAbility.cooldownTicks()`** (default 0) y
  el pipeline una puerta de cooldown por **(jugador, habilidad)** antes de cobrar chakra — sin ella, una habilidad
  de coste 0 sería spameable. La mayoría del catálogo sigue autolimitándose por chakra (default 0).
- **Genjutsu Stun** (Sharingan, 0/0, cd 200t): 1ª del Sharingan y 1er **stun duro** — `slowness` amp **255**
  (inmovilización) + `mining_fatigue` amp 3, 100 ticks; `blindness` solo a **no jugadores**, fiel al
  `unless predicate dojutsu:player/is_player`. Alcance 10.
- **Copy Jutsu** (Sharingan, 0/0): la habilidad más singular — no es efecto de combate, **aprende un jutsu**. Es
  el **único puente F5→F4** y consume `LastCastRegistry` (F4/DJT-079), que se construyó justo para esto. Exige
  **mano vacía** (fiel a "Hand not empty") y un cast visto en radio 20. **Corrige S06-R6**: el `d.sharingan_seen`
  del datapack no caducaba nunca; aquí el registro expira (200t) y está acotado por dimensión y distancia.
- **Kinrin Tensei Baku** (Tenseigan, 400/0 — verificado: `matches 400..` y resta 400): 1ª **canalizada con
  auto-raíz** — `slowness`+`jump_boost` 255 al lanzar, limpiados al detonar; HARM III en radio 5.
- **19 habilidades**. Quedan solo `susanoo` y `tenseigan_chakra_mode` del catálogo declarado: ambos son **toggle**,
  van con `ToggleableMode` en DJT-12x.

**Decisiones de DJT-119+** (resto de habilidades, lote 5):
- Cubre 3 tipos nuevos en 3 ojos distintos: **Asura Path** (Rinnegan, 450/0): habilidad de **proyectil** — lanza un
  `NinjaProjectile` real (misil de F4/DJT-070) con callback de impacto que daña al `LivingEntity` golpeado + da
  `resistance` al lanzador (fiel a `asura/cast`). **Preta Path** (Rinnegan, 150/0): 1ª habilidad **defensiva** del
  catálogo — buff de `absorption`+`damage_resistance` sobre el lanzador (reinterpreta la barrera de absorción).
  **Truth-Seeking Orbs** (Tenseigan, 300/0): 1ª habilidad del **Tenseigan** — HARM III en el punto de mira (radio 5).
  Coste 300 = lo que consume `truth_seeking_orb/summon`; el datapack lo gatea a 600 de disponibilidad. Costes/strain
  verificados; Rinnegan/Tenseigan sin strain.
- **16 habilidades** implementadas hasta el lote 5. La plantilla cubre 9+ tipos de efecto (incl. proyectil y defensa).

**Decisiones de DJT-119+** (resto de habilidades, lote 4):
- **Exploding Human** (Ketsuryugan, 200/+20): daño mágico devastador (HARM III) al más cercano + explosión.
  **128 Palms** (Byakugan, 80/0): versión superior de 64 Palms (HARM III AoE radio 5). **Chibaku Tensei**
  (Rinnegan, 600/0): gravedad **vertical** — levita + daña en la zona (distinto de la atracción horizontal de
  Bansho Ten'in). Costes/strain verificados; patrón de strain confirmado una vez más.
- **13 habilidades** implementadas hasta el lote 4. La plantilla cubre 8+ tipos de efecto.

**Decisiones de DJT-119+** (resto de habilidades, lote 3):
- Cubre 3 tipos de efecto nuevos en 3 ojos: **Tsukuyomi** (Mangekyō, 100 chakra/**+30 strain**): genjutsu de
  ilusión (blindness+slowness+mining_fatigue+HARM) al más cercano. **Amenotejikara** (Rinnegan, 600/0):
  **teleport/intercambio** (swap de posición con la entidad apuntada, o blink al punto de mira). **64 Palms**
  (Byakugan, 60/0): golpe **melé AoE** (HARM radio 5) + auto-inmovilización de la postura. Costes/strain
  verificados; el strain sigue el patrón (Mangekyō/Ketsuryugan sí, Byakugan/Rinnegan no).
- **10 habilidades** implementadas en total. La plantilla ya cubre: fuego anclado, empuje/atracción/dirigido,
  daño mágico single/AoE, auto-buff, genjutsu multi-debuff y teleport.
- **Nota (pipeline):** el cast consume chakra/strain **antes** de `execute`; si una habilidad de objetivo único
  (Tsukuyomi/Blood Eruption/Amenotejikara-swap) no encuentra objetivo, el coste igual se gasta. Un refund "Nothing
  nearby" (como el `check_affinity`/`canApply` de los jutsus) es una mejora futura del pipeline.

**Decisiones de DJT-119+** (resto de habilidades, lote 2 — Ketsuryugan):
- **Blood Eruption** (120 chakra/+8 strain): daño mágico a la entidad **más cercana** (radio 10). **Iron Blood
  Fist** (80/+4): **auto-buff** de Fuerza II (habilidad de tipo buff). **Rising Blood Spear** (100/+14): daño
  mágico **en área** a donde apunta. Todos con costes **y strain** verificados (`<ability>/cost`).
- **Uso de `MobEffects.HARM`** (instant_damage): vanilla **ya invierte** el signo para los no-muertos (los cura),
  reproduciendo `if type=#undead run instant_health` del datapack **sin ramas**. Helper `nearestLiving` añadido.
- Confirma que la plantilla escala a efectos **de estado** (potion effects) además de la manipulación de velocidad
  del lote 1. **8 habilidades** implementadas en total (Amaterasu + 3 lote1 + 3 lote2 [+ Iron Blood Fist buff]).

**Decisiones de DJT-119+** (resto de habilidades, lote 1):
- **Lote representativo** que prueba la plantilla con los efectos **novedosos de manipulación de velocidad** que
  el diseño destaca (empuje/atracción), en dos ojos: **Vacuum Palm** (Byakugan, 40 chakra, empuje **dirigido** +
  daño), **Shinra Tensei** (Rinnegan, 600, empuje **AoE** + daño), **Bansho Ten'in** (Rinnegan, 360, **atracción**
  AoE). Costes **verificados** contra el datapack (`<ability>/cast: d.chakra matches N..`).
- **Helper `EyeAbilityEffects`**: iteración de objetivos + impulso de velocidad (`setDeltaMovement` + `hurtMarked`
  para que valga sobre jugadores) + daño, sin duplicar entre habilidades.
- **strain 0** en estas: sus `cost.mcfunction` **no** añaden `eye_strain` (verificado en Bansho/Deva; el Rinnegan
  es el ojo cúspide). Amaterasu (+10) sigue siendo la excepción verificada.
- **Simplificación fiel**: el empuje del datapack usa explosiones de `tnt_minecart`; aquí es un impulso directo
  (equivalente, sin destruir terreno). Ajuste de fuerza/daño/alcance → balance (F10).
- **Pendiente**: el resto del roster (Kamui, Tsukuyomi, Susanoo→DJT-12x, resto de Rinnegan/Ketsuryugan/Tenseigan/
  Isshiki/Jougan) se clona con la misma plantilla, un lote por sesión; los que tienen coste no verificable o
  coreografía multi-etapa compleja se abordan con su ficha.

**Decisiones de DJT-118** (`EyeAbility` + Amaterasu — abre `EP-EYE-ABILITIES`, plantilla):
- **Contrato `EyeAbility`** (`chakraCost`/`strainCost`/`execute(ctx)`) + `EyeAbilityRegistry` (id→clase, en
  código; los jutsus son datos+familia, las habilidades oculares son clases con lógica propia) + `EyeAbilityService`
  (pipeline: valida ojo activo + `selectedAbility` registrada + chakra → consume chakra (todo-o-nada) + fatiga →
  ejecuta → sync). El id de la habilidad coincide con el `abilities` de la `EyeDefinition` y la `selectedAbility`
  del menú (DJT-117).
- **Amaterasu piloto** (`sasuke/amaterasu`): **40 chakra + 10 strain** (verificado; exento si el ojo es **eterno**,
  fiel a `sasuke/cost unless wearing_eternal`). Raycast a donde apunta → **`AbilityAnchor`** (nunca armor stand,
  RA-3) de fuego negro que incendia/daña en radio 0..3; el daño se acota con las invuln-frames vanilla (sin
  temporizador propio). Ajuste de daño/duración → balance (F10).
- **Disparo**: keybind **R** → `UseEyeAbilityPayload` (C2S marcador) → el servidor lee la `selectedAbility` real y
  valida (el cliente no decide). `resolveCast` puro (6 tests).

**Decisiones de DJT-117** (menú de habilidades oculares):
- **Request/response autocontenido** (no toca la sync principal): keybind **N** → `OpenEyeMenuPayload` (C2S) →
  el servidor construye la vista desde `EyeState`+`EyeRegistry` → `EyeMenuPayload` (S2C) → la Screen se
  reconstruye. La lista de habilidades **viaja por red** (el `EyeRegistry` es server-side data-driven; el cliente
  no lo tiene), mismo patrón que `OnboardingOptionsPayload`.
- **Selección validada en servidor** (`[ADD/07]`): `SelectEyeAbilityPayload` (C2S) → el servidor comprueba que
  la habilidad **pertenece al ojo activo real** (`EyeMenuService.select`), la guarda en `EyeState.selectedAbility`
  y responde con la vista refrescada. **No usa hotbar** (checklist DJT-104): GUI real con botones.
- `EyeMenuService` puro (6 tests: vista con/sin ojo, selección válida/ajena/sin-ojo/repetida). `ClientEyeMenu`
  cache read-only (patrón `ClientOnboardingOptions`). Claves de idioma añadidas (menú + 21 habilidades).

**Decisiones de DJT-116** (`EyeStrainService`):
- `PlayerTickSystem` en `TickPhase.STRAIN`. **Regen** por pulso de 200 ticks: −1 (o −3 con ADN de Hashirama);
  solo si hay fatiga. **Efectos por umbral** (sobre fatiga regenerada): 95→weakness (todos), 98→blindness,
  100→apaga el ojo; el **Rinne Sharingan** exento de ceguera/apagado (no de weakness), fiel a `eye_strain/main`
  (`unless wearing_eye_10`). `regen` y `effectsFor` puros (13 tests).
- **Subida de fatiga = `addStrain`** (puerta única, la llamarán las habilidades de DJT-118+; hoy sin emisor,
  como `addAffinityXp` antes de F4). **Reset al dormir** = `resetStrain`, enganchado en `SleepChakraHandler` vía
  un **registro de handlers de despertar** (patrón proveedor de `RegenService`: `chakra` no importa `dojutsu`).
- **Refactor**: `EyeState.awakenTimers` → `timers` (mapa genérico de temporizadores por propósito: despertar +
  regen de fatiga); el timer de regen usa la clave `eye_strain_regen`.
- **Avisos de chat** (25/50/75/90/99 con anti-spam) diferidos como presentación (necesitan estado "ya avisado").

**Decisiones de DJT-115** (`EvolutionService`):
- **Dos partes** (`PlayerTickSystem` en `TickPhase.DOJUTSU`): (1) **acumulación pasiva del uso** (+1/tick) de la
  familia del ojo activo, **solo si** alguna arista depende del uso (los terminales Rinnegan/Tenseigan no
  acumulan — el datapack tampoco); (2) **transición por la primera arista** cuyos `requirements` se cumplen
  (uso inclusivo, clan, flag), aplicando el bonus del destino y emitiendo `EyeEvolvedEvent`.
- **Requisito `CLAN` compuesto**: `otsutsuki_uchiha` = Otsutsuki **o** Uchiha (split por `_`; ids de clan son
  tokens simples). Rutas divergentes = datos, no ramas: el implante nunca cumple el clan → no llega a Mangekyō.
- **EMS (eternal) NO se maneja aquí**: en el datapack es una **acción manual** (`eternal/try_give`, +60/+150,
  exige Mangekyō activo), no auto-evolución por uso → va al menú/acción (DJT-117+). Los **tomoe 1/2/3** son
  nivel derivado del uso (no transición). Variante de Mangekyō aleatoria (`mangekyo_rng`) diferida con el roster.
- **Bonus de destino auditados** (aplicados al evolucionar): Mangekyō +20/+80, Rinnegan +100/+150, Tenseigan
  +100/+100 (de `<eye>/awaken`). `mangekyo_itachi.json` corregido a 20/80.
- **Hueco de DJT-114 corregido (2026-07-19):** `sharingan/awaken` del datapack fija `sharingan_use=78000`
  (arranca en tomoe 1); se añadió `awakenInitialUse` a `EyeDefinition` (sharingan=78000) y `AwakeningService` lo
  fija al despertar. Ver `known_issues.md §1c`.

**Decisiones de DJT-114** (`AwakeningService`):
- **Lee `potentialDojutsu` del clan, no predicates `is_*`** (checklist DJT-101): `PlayerTickSystem` en
  `TickPhase.DOJUTSU` que acumula un temporizador y, al alcanzar `awakenTicks`, despierta un ojo potencial del
  clan (50/50 en Otsutsuki con azar inyectado, como la loot `50_50`). **Una vez por clan** (guard `unless _wake`).
- **`awakenTicks` añadido a `EyeDefinition`** (byakugan/sharingan 78000, ketsuryugan 24000; vacío = destino de
  evolución, no despertable por clan). El despertar aplica el bonus de max_chakra/XP de la `EyeDefinition`
  (auditados) y emite `EyeAwakenedEvent`; marca la familia `awakened` y activa el ojo (`activeEye`).
- **Validación:** lógica pura exhaustiva en 8 tests (umbral, roll, idempotencia, descartes). El **cableado** (el
  sistema en el pipeline, arranque limpio) está validado en runtime en ambos loaders; el **end-to-end in-game**
  es time-gated (~65 min de juego) → check manual documentado en `known_issues.md`.

---

## Fase 6 — Modos de potenciación (S08 + S10 + S11 + S12) — ✅ COMPLETADA

Épicas: `EP-TOGGLE-FRAMEWORK`, `EP-SAGE`, `EP-CURSED-SEAL`, `EP-EIGHT-GATES`, `EP-SOAH`. Ficha de diseño:
`DOCS/backlog/06_FASES5-10_RESUMEN_Y_PLANTILLAS.md §FASE 6`. Sistemas de verdad: `S08_sage.md`, `S10`, `S11`,
`S12` (verificar SIEMPRE contra el **datapack**, regla 16). Riesgo que desriesga: **RM-3** (matices de cada modo).

| Orden | Ticket | Épica | Descripción | Estado |
|------|--------|-------|-------------|--------|
| 1 | DJT-130 | EP-TOGGLE-FRAMEWORK | `ToggleableModeService` (arnés compartido activate→tick→cost→punish→deactivate) + `EmpowermentState` | ✅ (build+8 tests; servidor validado en runtime en ambos loaders) |
| 2 | DJT-135 | EP-SAGE | `SageState` + `NaturalEnergyChargingService` (carga por **umbral de velocidad**, no `sneak_time` — S08-R1) | ✅ (build+12 tests; servidor validado en runtime en ambos loaders; carga time-gated = check manual) |
| 3 | DJT-136 | EP-SAGE | `SageMode` (niveles 1-5) sobre el arnés + tecla G + `/dojutsusage` (QA); Seis Caminos → F9 | ✅ (build+13 tests; servidor validado en runtime en ambos loaders; modo in-game = check manual) |
| 4 | DJT-140 | EP-CURSED-SEAL | `CursedSealService` (6 sellos **como datos**, inyección 60%, nivel 1→2, `onPunish` mata) + tecla B + `/dojutsuseal` (QA) | ✅ (build+14 tests; servidor validado en runtime en ambos loaders; sello in-game = check manual) |
| 5 | DJT-145 | EP-EIGHT-GATES | `EightGatesService` (escalera de 8 puertas, fatiga corporal, cierre forzado a 100, reset al dormir/morir) + tecla H + `/dojutsugates` (QA) | ✅ (build+21 tests; servidor validado en runtime en ambos loaders; escalera in-game = check manual) |
| 6 | DJT-150 | EP-SOAH | `SoahSealService` (**hucha de chakra**: recarga manual de 20 000 + devolución por tramos 100/10/1) + tecla J + `/dojutsusoah` (QA) | ✅ (build+11 tests; servidor validado en runtime en ambos loaders; sello in-game = check manual) |

**Orden y justificación:** DJT-130 primero e incondicional — es el arnés del que dependen los otros cuatro, y
**salda deuda ya anotada**: `SusanooService` (F5/DJT-120) y `TenseiganChakraModeService` (fleco F5) ya replican
el mismo bucle "tick el modo → si cae, limpia". Sage (135→136) antes que los sellos porque su carga introduce el
**chakra natural** como segundo `Resource`. Puertas (145) es el de mayor riesgo (atributos reversibles + muerte).

**DoD de fase:** volverse sabio por bioma y activar el modo; recibir/inyectar un sello maldito (probabilístico);
abrir puertas acumulando strain con la 8ª letal; cargar y activar el SOAH — todo fiel a `[S08/S10/S11/S12]` y
persistente.

**Corrección de ficha (DJT-140, regla 16):** el checklist decía "muerte por inyección fallida"; el datapack
(`cursed_seal/inject` + `fail`) **no mata al fallar** — aplica daño instantáneo + veneno + hambre, y con la vida
llena se sobrevive. La muerte real es `punish`: sostener el sello **sin chakra** te consume, y a las 10 muertes
así en nivel 2 el sello **degrada**. Implementado como dice el datapack.

**Corrección de ficha (DJT-150, regla 16):** el SOAH **no tiene "carga lenta" ni "pulsos 200/400"**. La reserva
se llena de golpe y a mano (`recharge`: 20 000 de tu propio chakra, agachándote al activar) — es el **único**
origen de `d.soah_chakra` en todo el datapack — y la devolución ocurre **cada tick**, por tramos de 100/10/1.
Además, `recharge` veta con `chakra >= 10000` pero gasta 20000 (dejaría el chakra en negativo): aquí el requisito
es el importe real, porque el `Resource` es todo-o-nada por diseño.

**Correcciones de ficha (DJT-145, regla 16):** dos riesgos declarados de `[S11]` **no existen en el datapack**.
(1) **S11-R4 "atributos pegados"**: `eight_gates/run` no usa atributos, usa `effect give` de duración 1 tick
refrescado cada tick → caducan solos, no hay nada que revertir. (2) **"la 8ª puerta es letal"**: no hay ni un
`kill` en las 23 funciones de `eight_gates/`, ni en el Night Guy que la exige; lo letal es acercarse a 100 de
fatiga (la Puerta de la Muerte llega cuatro veces más rápido y cerrar con la fatiga alta duele). Además, el
**clan Lee recupera la MITAD de rápido** (pulso 400 vs 200), al revés de lo que dice la ficha — portado tal cual
y anotado en `EightGatesTable.decayPulseTicks` como posible fallo de origen.

**Diferido de DJT-145:** las **técnicas de taijutsu de puerta** (Front Lotus, Morning Peacock, Daytime Tiger,
Evening Elephant, Night Guy), ya anotadas como diferidas a F6 desde el backlog de F4: son jutsus del catálogo que
exigen `stage_N`, y el gate ya existe (`EightGatesService.stageOf`). Se hacen como lote de jutsus.

**Diferido de DJT-140:** las **habilidades propias del nivel 2** de cada sello (vuelo del Cielo, aim-assist de
Kidomaru, clon de Sukon/Ukon, genjutsu sonoro de Tayuya) son cuatro efectos novedosos uno a uno — se clonan con
la plantilla en lotes posteriores, como el roster ocular. El aura de los seis sellos sí está completa.

---

## Fase 7 — Bestias con cola / Jinchūriki (S09) — ✅ COMPLETADA

| Orden | Ticket | Épica | Descripción | Estado |
|------|--------|-------|-------------|--------|
| 1 | DJT-160 | EP-BEAST-ENTITY | `TailedBeastEntity` (**bossbar nativa**, S09-R6) + `BeastMove` compartido IA↔forma (S09-R2) + `/dojutsubeast` (QA) | ✅ (build+7 tests; servidor validado en runtime en ambos loaders; **renderer sin validar** — es código de cliente) |
| 2 | DJT-161 | EP-BEAST-AI | `TailedBeastMoveGoal` + `BeastAiPlan` (4 slots, **la mitad de los turnos en blanco**) + `TailedBeastBallMove` (1ª técnica compartida) | ✅ (build+10 tests; servidor validado en runtime en ambos loaders; **combate real y renderer sin validar** — piden cliente gráfico) |
| 3 | DJT-165 | EP-JINCHURIKI | `SealingService` + `TailedBeastRegistry` (Saved Data global) + `JinchurikiState` (**guarda el préstamo**, RA-2) | ✅ (build+7 tests; servidor validado en runtime en ambos loaders; sellado in-game = check manual) |
| 4 | DJT-166 | EP-JINCHURIKI | `FormService` (forma + vínculo + Baryon **terminal**, técnica de bestia desde el jugador, **la forma SUPRIME los ojos**) + tecla K | ✅ (build+12 tests; servidor validado en runtime en ambos loaders; forma in-game = check manual) |

**Nota de fidelidad:** a diferencia de las tres fichas de F6, la de `[S09]` **coincide con el datapack** en todo
lo verificado hasta ahora (ravager reskineado, barra por `store bossbar`, técnicas duplicadas). La duplicación de
S09-R2 no era hipotética: las dos Bolas Bestia con Cola **ya han divergido** (100 vs 1200 ticks de vida, distinto
daño, solo una conoce la absorción del Camino Preta).

**Anotado para balance:** `movement_speed base set 1` (≈4× un ravager vanilla) hace a la bestia inescapable.
Portado como está; la constante es `BeastStats.MOVEMENT_SPEED`.

**Precisión de DJT-166 (regla 16):** `[ADD/04 §8]` pedía "la forma **desactiva** los ojos"; el datapack los
**suprime** — `dojutsu/main` ejecuta cada ojo `unless is_in_jinchuriki_form`, con el ojo aún equipado. La
diferencia es funcional: la forma del Diez Colas **exige el Rinnegan activo**, así que apagarlo de verdad haría
que esa forma se apagase a sí misma. Implementado como supresión (`FormService.suppressesEyes`).

**Diferido de DJT-166:** el **refuerzo a baja vida** (`jin_boost`: a 3 corazones la bestia presta 500 de chakra
y absorción, con diálogo por bestia y nivel) — mecánica completa, merece su lote. El nodo del **Diez Colas**
(Estatua Gedo, Tenpenchii, Orbes) converge en **F9** como estaba planificado.

**Corrección de ficha (DJT-165, regla 16):** **no existe ningún "+8000"** — el ADD lo repetía en tres sitios.
`sealing/*` presta **`1000 × colas`** de chakra máximo (1000…10 000). Y la ninja_xp sigue `100 + 10×colas` solo
de la 1 a la 8: el **Nueve Colas da 230** (no 190) y el **Diez Colas, 1000**. Corregido también en el javadoc de
`BeastSealedEvent`.

**Precisión sobre RA-2:** la respuesta no es "marcarlo como estado especial" sino **guardar cuánto se prestó**
(`JinchurikiState.grantedMaxChakra`), para que la extracción devuelva exactamente eso aunque la tabla cambie.
`removal/*` resta la constante literal, lo que se rompe si alguien toca las cifras.

**Hallazgo de DJT-161:** la IA **desperdicia la mitad de sus turnos a propósito**. `ai_rng` tira 0..5 y el 0 y el
5 no están mapeados; para una bestia normal el 3 tampoco. Ataca en 3 de 6 turnos (el Diez Colas, 4 de 6), lo que
sumado a su temporizador más corto lo hace atacar ~2,5× más. Esas pausas son lo que hace la pelea jugable.

**Divergencia resuelta (DJT-161):** de las dos Bolas Bestia con Cola se toma la de la IA (100 ticks de vida). La
del jugador declara 1200 ticks, que a 2,2 bloques/tick son >2600 bloques de alcance — copy-paste de un efecto más
lento, no una decisión de diseño.

**Diferido de DJT-161:** las **nueve técnicas insignia restantes** (sand_buckshot, cat_flame_roaring_fire,
great_water_mass_bullet, monkey_flame_arson, unrivalled_strength, soap_bubble_barrage, scale_powder,
tentacle_barrage, chakra_arms_barrage) son un lote de contenido sobre la plantilla `BeastMove`; la bestia pierde
el turno mientras tanto. También la absorción por el Camino Preta (`d.absorbed`), que pide un gancho en el
proyectil. El **Tenpenchii** del Diez Colas ya está modelado en `BeastAiPlan` y testeado: solo falta que algo
ponga su temporizador, y eso es contenido de F9 (`TailedBeastEntity.tenpenchiiReady()`).

---

## Fase 8 — Ítems, mundo y comercio (S14+S15+S16+S17+S18) — ✅ COMPLETADA

| Orden | Ticket | Épica | Descripción | Estado |
|------|--------|-------|-------------|--------|
| 1 | DJT-170 | EP-ITEMS | `ItemFlags` (**solo la máscara**) + `DnaService`/`MaskService`/`PillService` + ítems ADN/máscaras/píldoras + `/dojutsuitem` (QA) | ✅ (build+16 tests; servidor validado en runtime en ambos loaders; ítems in-game = check manual) |
| 2 | DJT-175 | EP-SUMMONING | `ContractService`/`SummonService` + 27 contratos JSON; caducidad por **`AbilityAnchor`** (S15-R3 resuelto reusando F4) | ✅ (build+8 tests; servidor validado en runtime en ambos loaders, 27 contratos cargan; invocar in-game = check manual) |
| 3 | DJT-180 | EP-THROWING | `ThrowingService` + `FtgMarkerService` (marca como `GlobalPos` en el Attachment: **adiós al `forceload`**) + tecla Y | ✅ (build+11 tests; servidor validado en runtime en ambos loaders; lanzar/saltar in-game = check manual) |
| 4 | DJT-185 | EP-SHINOBI-AI | `ShinobiEntity` + `ShinobiCombatPlan` (ritmo del datapack + **bandas de distancia nuevas**) + `EdoTenseiService`; **S17-R3 cerrado sin tocar el pipeline** | ✅ (build+11 tests; servidor validado en runtime en ambos loaders; **combate real y renderer sin validar** — piden cliente) |
| 5 | DJT-190 | EP-TRADING | `ShinobiVillagerService` (conversión **por evento**) + 2 catálogos JSON (RB-4) + `TraderSpawnPolicy` (`SavedData`) | ✅ (build+10 tests; servidor validado en runtime en ambos loaders, 2 catálogos con recuento de pendientes; comprar in-game = check manual) |

**Hallazgo de DJT-170:** de los cuatro campos que la ficha pedía en `ItemFlags/DnaState`, **tres ya tenían dueño**
— `oiled` en `SageState` (F6), `dna` en `NatureProfile` (F3, incluidas las células de Hashirama), y `pills` son
cargas del ítem. Queda **un** campo: la máscara. Duplicar los otros habría creado dos fuentes de verdad.

**Los 26 ADN cuadran solo separando tres familias:** 7 naturalezas + 18 KG + las células de Hashirama (que **no**
son un KG — ya estaba visto en F3). Implantar prende **33%** y fallar consume la muestra; hay **dos excepciones
garantizadas** (Madera con células dentro; células siendo Senju) y lo ya poseído es un **no-intento** que no gasta
el ítem.

**Decisión consistente con F5:** la máscara es **estado**, no casco (el datapack usa `armor.head` y veta el casco).
Consecuencia asumida: puedes llevar máscara y casco. El equipo `MaskHidden` que `ClanTeamService` dejó anotado en
F3 llega aquí.

**RB-4 resuelto (DJT-190):** el datapack construye las ofertas con **NBT literal dentro de la función** — 14 líneas
en `setup_shinobi` y 22 en `setup_trader`, cada una con el ítem entero incrustado. Ahora son **dos JSON**. Y la
conversión va por **`EntityEvent.ADD`**, no por el bucle `schedule 1s` que recorría todos los aldeanos del mundo.

**Decisión sobre la mercancía que aún no existe (DJT-190):** el catálogo declara el **surtido completo** y el
arranque **cuenta lo que falta** (`24 ofertas, 15 pendientes de su ítem`). Ni se recorta el catálogo ni falla la
carga: las ofertas aparecen solas cuando su ítem se registre.

**Dos valores que se portan del revés (DJT-190):** el ritmo del mercader es **`24000 / rate`** = *veces por día*,
no un intervalo; y `traderspawnchance` tira en **[chance, 10]** ganando el 10, así que **10 = certeza** y
**1 = 1/10**. Ambos con test propio.

**RM-5 confirmado (DJT-185):** la IA del datapack está **a medias y de forma literal** — `ai/get_move` tiene 6
ramas y solo 2 hacen algo; las otras son `say do nothing`. Y `ai/get_jutsu` (30 jutsus) **no se llama desde ningún
sitio**. Pero el **ritmo** sí estaba decidido y es **idéntico al de las bestias** (60 ticks, 32 bloques, dado 0..5
con la mitad de turnos vacíos), y la **intención** de cada tirada está declarada en los `say`. Lo único diseñado de
cero son las **bandas de distancia** (melee ≤4, kunai ≤16, jutsu más allá), marcadas como tal y fijadas con tests.

**S17-R3 estaba cerrado desde F4 (DJT-185):** `CastContext` declaraba en su javadoc *"No asume jugador (S17-R3)"* y
`JutsuEffect.apply` ya recibía un `LivingEntity`. El shinobi entra por el núcleo genérico del pipeline y pasa por
**las mismas puertas** que un jugador. **Cero cambios** en el pipeline, el dominio o las precondiciones.

**Diferido de DJT-185:** el roster de jutsus por nivel es diseño conservador (solo fuego, la afinidad que el
datapack le da); ampliarlo es contenido. El **protector de frente** por aldea (24 combinaciones) es arte → F10.

**S16 resuelto por modelado (DJT-180):** el datapack hace **`forceload add`** sobre el chunk donde se clava el
kunai del Dios del Trueno, así que **cada marca mantiene un chunk cargado indefinidamente** — y si el kunai muere
por otra vía, el forceload queda huérfano. Aquí la marca es un `GlobalPos` en el Attachment: sin chunk, sin
forceload y multi-dimensión por construcción.

**Correcciones de ficha (DJT-180):** **no hay múltiples marcas** — `reset` mata la anterior al lanzar otro kunai,
así que hay **una por jugador**, y saltar la **consume**. Y hay **dos daños distintos**: el atributo (5/3/4) es
para blandir el arma; lanzadas las cinco hacen lo mismo (6 puntos). El shuriken además vuela **más rápido** que el
kunai (0,8 vs 0,5), pero el kunai se lanza el doble de seguido.

**Diferido de DJT-180:** el shuriken rotando en vuelo (presentación) → F10; el Rasenshuriken aparece en `move`
como arrojadizo pero es un **jutsu** del catálogo de F4.

**S15-R3 resuelto sin código nuevo (DJT-175):** el datapack caduca las invocaciones con **dos selectores globales
por tick**. Aquí cada invocación nace con un `AbilityAnchor` pegado — la primitiva de **F4/DJT-071** ya tenía
caducidad por construcción. Con cero invocaciones, coste cero.

**Correcciones de ficha (DJT-175):** invocar **no cuesta chakra** (no hay una línea de chakra en `cast`); lo que
se paga es el veto, que baja de 600 a 200 ticks según el fuinjutsu. Y **firmar y usar son dos puertas distintas**:
firmar pide Chūnin siempre y fuinjutsu solo en mid/high; usar pide fuinjutsu también en el tier bajo. Además el
datapack **no cuadra sus dos suelos** (tier bajo abre a mod 75, `cast` exige mod 125): el suelo real es 125.

**Diferido de DJT-175:** el círculo de invocación como visual propio → F10; invocaciones de combate con IA propia
(en el datapack no existen: son mobs vanilla domados).

**Diferido de DJT-170:** píldoras de colores del Akimichi (atadas al Modo Mariposa), herramientas quirúrgicas (el
implante de ojos ya existe desde F5) y los pergaminos. Ítems finos sobre servicios existentes.

---

## Fase 10 — Pulido, paridad, assets y API — 🔄 EN CURSO

| Ticket | Historia | Estado |
|--------|----------|--------|
| **DJT-225** | **Ítems que nunca se crearon**: los 9 huevos de bestia + los 8 craftables pendientes | ✅ (build multiloader verde + **930 tests**; ambos loaders arrancan limpios y cargan 1303 recetas, 16 combinaciones **sin pendientes** y 25 ofertas con 12 pendientes) |
| **DJT-223** | Paridad y verificación **en cliente** (ambos loaders, en partida) | 🔄 **mitad automatizable hecha** (cliente arrancado por primera vez en los dos loaders, 0 modelos/texturas sin resolver, migración desde saves verificada, paridad de adaptadores auditada); la mitad interactiva queda como checklist: `docs/checklist_paridad_cliente.md` |
| DJT-224 | Assets: resource pack → assets del mod, renderers y localización (pasada 1 hecha) | 🔄 |
| **DJT-226** | Deuda de sistemas: `Susanoo`/`TenseiganChakraMode` al arnés, roster del Rinne Sharingan, 9 movimientos de bestia | ✅ (build multiloader verde + **947 tests**; ambos loaders registran **10** técnicas de bestia, no 1) |
| **DJT-221** | Balance | ✅ (los 16 `TBD_datapack` resueltos; **5 umbrales de kenjutsu inventados** corregidos; recetario 23 → **36** hitos; **956 tests**) |
| **DJT-222** | Rendimiento | ✅ (auditoría de los 21 puntos de entrada por tick en `docs/perf.md`; 3 correcciones con números; **949 tests**) |
| DJT-220 | API pública (**al final**: congelarla antes de ver los sistemas funcionar sería firmar sobre lo no verificado) | ⬜ |

### DJT-225 — Ítems que nunca se crearon ✅

**Por qué va primero:** no añade contenido nuevo, **desbloquea contenido ya escrito**. Antes de este ticket
había dos catálogos declarando mercancía inexistente y un recetario de drop-crafting con 9 de sus 16
combinaciones inertes.

**Alcance (17 ítems, cada valor verificado contra el datapack):**

- **9 huevos de invocación de bestia** (`spawn_egg_<bestia>`), los que `main/internal/crafting/tb/*` sabía
  fabricar y `data/dojutsu/drop_crafting/` ya declaraba. Un ítem por bestia (el tipo **es** el ítem, como las
  máscaras y las arrojadizas), no un `SpawnEggItem` vanilla: las nueve son la **misma** `EntityType` con
  identidad distinta dentro, y el huevo vanilla las invocaría todas iguales.
- **Los 8 craftables** que `CraftingTable.PENDING_NATIVE_RECIPES` dejó anotados en DJT-206 — las 13 recetas de
  `data/dojutsu/recipes/` menos las 5 ya portadas: `smoke_bomb`, `explosive_tag`, `ramen`, `shoyu_ramen`,
  `medical_scroll`, `removal_scroll`, `sage_scroll` y **`tanto`**.

**Corrección de ficha (regla 16).** El encargo enumeraba «bomba de humo, ramen ×2, pergaminos médico/de
retirada/de sabio, sello explosivo suelto» = **7**, y decía 8. El octavo es el **tanto**, la cuarta arma
(`iron_sword` con daño 6, kenjutsu 100 en el datapack = 25 en escala del mod y 1000 de durabilidad — la única
arma del mod que se gasta, porque es la única que el datapack marcaba con `HasCustomDurability`). Manda el
datapack: 13 recetas − 5 portadas = 8.

**Cuatro hallazgos del datapack:**

1. **El ramen repone 150, no 100.** Es `cooked_beef{Ramen:1b}`, y `cooked_beef` **está dentro del tag
   `dojutsu:food`** → al comerlo se cumplen **los dos** advancements y corren **las dos** funciones:
   `chakra/eat_food` (+50) y `chakra/eat_ramen` (+100). El ramen no sustituye la reposición base: la **suma**.
   Leyendo sólo `eat_ramen` uno concluiría 100 y se quedaría un tercio corto.
2. **La bomba de humo no ciega a nadie.** Uno espera `blindness`; no hay **ningún** efecto de estado. Son 100
   partículas `large_smoke` por tick durante 120 ticks (6 s) y un `redstone_torch.burnout`. La cortina es
   literal: tapa la vista de quien mire a través, y a quien no le tape no le hace nada. Portarla con un
   `MobEffect` habría sido más cómodo y habría cambiado el juego.
3. **El sello explosivo es una mina, no una granada.** Tarda **200 ticks (10 s)** en armarse y sólo entonces
   detona con lo que entre a 5 bloques. Esos diez segundos son la ventana para alejarse: quien lo suelte a sus
   pies y se quede, lo detona él mismo. Y reparte **dos** daños, no uno: la explosión del `tnt_minecart`
   (potencia 4.0 exacta, la misma que el kunai con sello) **más** un `instant_damage` II que la armadura no
   para y que la distancia no atenúa. No lo disparan ni cuadros, ni marcos, ni soportes de armadura, ni ítems.
4. **El Diez Colas no tiene huevo craftable.** El datapack define diez `give/tailed_beast/spawn_egg/*` pero
   sólo nueve recetas: el décimo se obtiene **únicamente por comando**. Registrarlo habría creado un ítem
   inobtenible; su equivalente exacto sigue siendo `/dojutsubeast summon ten_tails`.

**Decisiones de diseño:**

- **Se dispara al aterrizar, no al soltar.** Los dos predicates del datapack (`items/smoke_bomb`,
  `items/explos_tag`) exigen `OnGround:1b`: el efecto ocurre **donde cae el ítem**, no donde estaba tu mano —
  un sello armado a la altura de los ojos quedaría flotando, y una cortina que saliera del jugador no taparía
  una retirada. El alta es reactiva (`PlayerEvent.DROP_ITEM`, nativo en los dos loaders) y sólo entra en la
  lista de vigilancia lo que de verdad es una bomba o un sello. No se elimina el bucle —tocar el suelo no es
  un evento en Minecraft—, pero su coste pasa a depender de cuántas bombas hay en el aire y no de cuántos
  ítems hay en el mundo, que es lo que recorría el datapack cada tick.
- **El sello colocado es la primera entidad persistente del mod que no es una criatura.** El `AbilityAnchor`
  encajaba en todo salvo en lo único que aquí importa: `shouldBeSaved()` devuelve `false`. Una trampa se deja
  puesta y se vuelve a por ella luego, quizá tras reiniciar; con un ancla, todas las minas del mundo
  desaparecerían al descargar el chunk y nadie lo notaría hasta pisar donde ya no hay nada. El `armor_stand`
  del datapack persiste, y ésta también. La cortina de humo, en cambio, **sí** es un `AbilityAnchor`: es
  transitoria por definición y su caducidad ya venía garantizada de F4.
- **Se cierra la puerta de crafteo que DJT-206 dejó a medias.** `CraftingGateService` decía por escrito que de
  las cuatro recetas gateadas «la única cuyo ítem existe hoy es el contrato». Ahora existen las cuatro:
  medicina ≥ 75, fuinjutsu ≥ 250 (la puerta más alta del juego: la mitad del tope de habilidad) y sabio +
  senjutsu ≥ 75 + 50 de energía natural, que además **gasta** esos 50 y sólo en la rama que sale bien. Los
  cuatro reembolsos devuelven la receta exacta.
- **Los pergaminos no se gastan si fallan.** El datapack hace el `clear` **dentro** de la rama con éxito. Un
  pergamino de sabio usado por quien no es sabio avisa en rojo, suena el yunque y **se queda en el inventario**.

**De propina:** el pergamino médico también pone la **fatiga ocular a 0** — es la única vía, aparte de dormir,
para bajarla de golpe. Y se añadió la oferta del **Pergamino de Retirada** al mercader errante (48 esmeraldas,
1 uso, 6 de XP, verificado en `setup_trader` línea 15): estaba en el datapack y faltaba en el catálogo.

**Efecto medible:** drop-crafting **9 pendientes → 0**; comercio **15 pendientes → 12**; recetas cargadas
**1295 → 1303**; tests **912 → 930**.

**Cómo probarlo:** ver `development_log.md`, entrada del 2026-08-01.

### DJT-223 — Paridad y verificación en cliente 🔄

**Lo automatizable, hecho.** Nueve fases validadas con **servidor dedicado** dejaban fuera media superficie del
mod: renderers, keybinds, HUD, pantallas y atlas de texturas no existen en un servidor. Este ticket arranca el
**cliente** por primera vez, en los dos loaders, entrando directo a un mundo (`--quickPlaySingleplayer` sobre
`run/saves/dojutsu-qa`).

Resultado: los dos loaders cargan, entran en el mundo, el onboarding dispara, el sync llega y el chakra
regenera con la tabla de `[S02]` (+3/120 t, clamp en 100). **Cero modelos y cero texturas sin resolver** con
~200 modelos de ítem — primera confirmación real de que los assets de DJT-224 y DJT-225 están bien enlazados.
Los **6 renderers** quedan registrados (regla 5, nunca comprobada). Y la **pantalla de creación del ninja se ve
entera**, con los 14 iconos de clan.

**Migración desde saves: ya no es una suposición.** La auditoría avisaba de 16 sub-estados enchufados en F1–F9
sin subir nunca `CURRENT_DATA_VERSION`. `LegacySaveMigrationTest` (6 tests) comprueba que un save de la época
de **F1** carga en el agregado de hoy, conserva lo suyo y deja los 14 sub-estados desconocidos en `DEFAULT`;
que migrar es idempotente; y que un save *futuro* no se degrada. El valor está en el futuro: el día que un
sub-estado lleve un campo obligatorio, el test se pone rojo antes de romperle la partida a nadie.

**Paridad de adaptadores: auditada, sin asimetrías.** Mismo camino de `init()`/`initClient()`, mismos 6
renderers, mismos 11 keybinds (12 claves de idioma, ninguna huérfana) y los mismos eventos nativos. Logs de
carga idénticos línea a línea.

**Lo interactivo NO se pudo automatizar y se deja escrito.** Inyectar teclado/ratón en la ventana no funciona
de forma fiable (GLFW ignora la entrada sintética; traer la ventana al frente desde segundo plano falla de
forma intermitente). **La verificación en partida la tiene que hacer una persona**, y por eso el entregable es
un checklist ejecutable: `docs/checklist_paridad_cliente.md` — prioridad en los cinco bloques que **nunca se
han visto** (renderers de criaturas, menú ocular y Susanoo, Árbol Divino por etapas, diálogo del Sabio, formas
de jinchuriki) + una fila por sistema S01–S25 con casilla por loader.

**Dos hallazgos:** llega un **delta S2C antes del primer full** al entrar (inofensivo hoy, se descarta a
propósito, pero alguien emite un delta durante el join); y el **chakra regenera durante el onboarding**, con el
ninja aún sin crear.

### DJT-226 — Deuda de sistemas ✅

Tres deudas sin relación entre sí salvo la de siempre: alguien las dejó a medias y lo anotó por escrito.

**1. Los dos modos que se quedaron fuera de su propio arnés.** `ToggleableModeService` (F6/DJT-130) se extrajo
**precisamente** del bucle que repetían `SusanooService` y `TenseiganChakraModeService`; los dos se quedaron sin
adoptarlo durante cuatro fases, con un comentario que decía «cuando F6 traiga el arnés conviene extraerlo».
Ahora son **siete de siete**. El Susanoo conserva su `toggle` propio porque una pulsación suya no es «enciende o
apaga» sino **tres** respuestas (manifestar / crecer / apagar) — el arnés expone `activate`/`deactivate` por
separado justo para eso. Antes de tocar nada, **Serena** confirmó que **nadie se suscribe a `ModeToggledEvent`**,
así que el cambio de orden de emisión es neutro por construcción. Un cambio de comportamiento, y es corrección:
al **caer** por chakra, el Tenseigan no emitía el evento y el Susanoo sí; ahora lo emiten los siete.

**2. Las nueve técnicas insignia son la misma técnica.** El registro tenía **1** de 10, así que ocho de cada
nueve bestias no hacían nada en la mayoría de sus turnos. Al abrir las nueve carpetas del datapack: **son
idénticas** —misma velocidad, misma vida, mismo radio, mismo daño— y sólo cambian **las partículas**. Una clase
parametrizada (`SignatureBarrageMove`), no nueve (RA-1). **Y el temporizador no era un enfriamiento: era el
alcance** — vive en la bestia y al llegar a 0 detona el disparo, así que con la carga descontada el alcance real
son **8,4 bloques**. El **Nueve Colas rompe el patrón otra vez** (timer 20, no 10: llega el doble de lejos).
Quedan fuera, con sus números ya verificados: Tenpenchii, Truth Seeking Orb y la Bola del Diez Colas.


**3. El Rinne Sharingan tenía el roster del Rinnegan copiado**, literal. El ojo que concede el Árbol Divino no
tenía nada propio. En el datapack es un menú de **tres páginas** (la suya, la del Mangekyō y la del Rinnegan)
que se cambian con agacharse + slot; como el mod tiene menú de verdad desde DJT-117, las páginas **se disuelven**
en una lista de 13 entradas. **Amenominaka** es lo nuevo y no es un teleport: cuesta **8000 de chakra** (cuatro
veces el Shinra Tensei) y arrastra a **todos los jugadores en 10 bloques** a otra dimensión — no es movilidad,
es secuestrar una pelea. La vuelta a casa **se gana al irse** (el origen se graba sólo al salir del Overworld) y
va como `GlobalPos` en el estado, **sin el `forceload`** con el que el datapack mantenía un chunk cargado por
regreso pendiente (mismo arreglo que DJT-180). De propina, estrena `dojutsu.rinne_sharingan.amenominaka`: uno de
los 20 sonidos registrados que no tocaba nadie. Quedan 19.

### DJT-222 — Rendimiento ✅

**El resultado principal es un «no».** Auditados los **21 puntos de entrada por tick** (18 `PlayerTickSystem` +
3 en `SERVER_POST`): **ninguno está sin acotar**. Diecisiete de los dieciocho salen sin hacer nada en el caso
común, y el que siempre trabaja (`RegenService`) es el que tiene por cometido hacerlo. La regla 11 («reactivo,
nunca polling») se ha pagado sola. Inventario completo en [`perf.md`](perf.md).

**Tres correcciones, con números.** (a) `AwakeningService` y `EvolutionService` construían un `new Random()`
**por jugador y por tick** —con su `nanoTime()` y su CAS sobre un estático de la JVM— para un azar que se tira
una vez en la vida de un ninja; ahora comparten un `ThreadLocalRandom`, con el parámetro **todavía inyectado**
para que los tests le den semilla. (b) `EvolutionService` recorría el grafo para jugadores **sin ojo**, que son
casi todos casi siempre: guarda de una línea. (c) La chakra vision construía un `String` **por entidad y por
barrido** para consultar una tabla de `domain` que no puede conocer `EntityType`; ahora se memoiza por **tipo**.

| Camino | Antes | Ahora | Factor |
|---|---:|---:|---:|
| Azar del glue | 35 ns/op | 5 ns/op | 7,0× |
| Tick de jugador sin ojo | 32 ns/op | 3 ns/op | 10,7× |

**Y la cifra honesta**: pasan de **0,0014 %** de un tick con 20 jugadores a **0,0002 %**. Higiene, no rescate.

**El eje de escalado real son los barridos de entidades** (chakra vision radio 30/10 t; drop-crafting radio 16/5
t con emparejamiento O(n²); goals de IA). **No se tocaron a propósito**: ya están limitados por intervalo y
puerta, y son la traducción de algo que en el datapack corría *cada tick sobre todo el mundo*. Optimizarlos sin
medir sería adivinar, y la medición que hace falta —MSPT con entidades y jugadores— **sólo se toma en partida**.

**Banco de pruebas** `TickHotPathBenchmark`, `@Disabled` a propósito (un test que mide tiempo falla en CI el día
que la máquina va cargada); se activa con `-Pbench` e imprime ns/op **y** el % de tick que supone, para que el
próximo no optimice ruido.

### DJT-221 — Balance ✅

RA-4 decía «no improvisar números» y dejaba **16 `TBD_datapack`** en `balance/xp_sources.json`. Resueltos todos
— y al resolverlos apareció que **los cinco umbrales de kenjutsu estaban inventados**: eran `5/10/20/30/40` y
los reales son **75/88/125/150/350**. La quinta técnica de espada se desbloqueaba casi **nueve veces antes de
tiempo**. El resto de familias sí cuadraban; era la única sin contrastar.

**La escala, por fin justificada.** El «datapack ÷ 4» se usaba desde DJT-082 sin explicación. Ahora la tiene: el
techo de habilidad del mod es **500** y los cuatro requisitos más altos del datapack valen exactamente **2000**
(maestría de las Ocho Puertas, fuinjutsu 3, Blaze, Creation Rebirth SOAH). 2000 ÷ 4 = 500 — **es la misma escala
con distinto zoom**, y los topes coinciden. Lo que **no** se reescala: chakra máximo, ninja_xp y contadores de
uso, 1:1 en los dos lados.

**Los 25 hitos cuantificados.** Su ninja_xp **no es uniforme** (de **1** en la Sustitución a **100** en Izanagi,
Ocho Puertas y SOAH) y el mod **no daba ninguna**; ahora cada hito la declara y `XpApi` la paga. Faltaban hitos
enteros —las nueve puertas, el contrato, el kunai del Dios del Trueno, la Sustitución, Creation Rebirth y su
versión SOAH— y la lista médica iba corrida una posición por un `healing` ausente. El recetario pasa de **23 a
36**.

**El punto ciego del modelo reactivo.** Cuatro hitos exigen habilidad **y** chakra máximo, que se cumplen en
momentos distintos; `SkillLevelChangedEvent` sólo ve el instante del cruce, así que si el chakra llega después
—y llega después casi siempre, porque sube al sellar una bestia— el hito **no se otorgaría jamás**. El datapack
revisaba los hitos cada tick. Se recupera la garantía con un **barrido de puesta al día al entrar**, O(hitos)
una vez por sesión. La Sustitución, que depende del ninja_xp global, gana su disparador vía
`NinjaXpChangedEvent`.

**Un bug del datapack portado tal cual:** `flower_hill` se comprueba con el predicate de `bringer_of_darkness`
—no existe ningún `gj_req_3`— así que los dos salen a la vez. Con la nota puesta: cambiarlo sería rebalancear.

**Fuera, con su motivo anotado:** Izanagi (dos afinidades + otro predicate: necesita requisitos compuestos),
acceso a Senjutsu (sólo chakra, sin habilidad que lo dispare) y Blaze por afinidad de fuego.

---

## Fases posteriores — ⬜ PENDIENTES

| Fase | Tema | Estado |
|------|------|--------|
| F9 | Mundo, narrativa, Seis Caminos, Diez Colas, Árbol Divino | ✅ cerrada en código (`sixpaths`, `story`, `world`); esta sección del backlog nunca se escribió |
| F10 | Pulido, balance, API pública, paridad, assets | 🔄 en curso (ver arriba) |

Camino crítico del ADD: F0 → F1 → F2 → F4 → F5/F7 → F9. Vertical slice en F2.

## Tickets diferidos a F10 (assets) — acordado 2026-07-21

Trabajo **de arte, no de código**: la lógica está completa y validada; solo falta el porte visual. Se agrupa aquí
a propósito para no fragmentar las fases de gameplay.

| Ticket | Descripción | Origen |
|--------|-------------|--------|
| **F10-A1** | **Modelos 3D del Susanoo** por personaje (los 7 `susanoo_act` del datapack, `CustomModelData` 1000..7000 + etapa). Hoy la silueta se dibuja con partículas por etapa (`SusanooAvatar`), que es funcional pero no es el arte original | F5/DJT-121 |
| **F10-A2** | **Modelos/texturas del Mangekyō por personaje** (8 variantes: itachi, obito, sasuke, madara, indra, shisui, fugaku, shin) y del **EMS**. Hoy los ojos son estado lógico sin representación visual propia | Roster de ojos (lotes 1-8) |

**Nota:** el resto del **roster de ojos** (Isshiki, Jougan, Rinne Sharingan, variantes del Rinnegan) sigue siendo
**contenido de código diferido** —no assets—; se clona con el mismo esquema data-driven cuando se quiera, sin
bloquear ninguna fase.
