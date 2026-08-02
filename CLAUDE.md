# CLAUDE.md — Dojutsu Port

Contexto inicial para retomar el proyecto en una conversación nueva. Resumen ejecutivo; el detalle
está en `docs/`.

## Objetivo

Reconstruir el datapack *Dojutsu* (habilidades ninja de Naruto/Boruto) como **mod moderno de
Minecraft**, preservando el 100% del comportamiento del datapack pero con una arquitectura limpia y
mantenible. Este repo es el **código del mod**.

## Stack

- **Minecraft:** 1.21.1 · **Java:** 21 · **Gradle wrapper:** 8.10.2
- **Loader:** **Architectury multiloader** → subproyectos `common` + `neoforge` + `fabric`
- **Mod id / namespace:** `dojutsu` / `com.zamu.dojutsu` · **Autor:** Zamu · **Versión:** 0.1.0
- **Repo:** https://github.com/Z4MU/Dojutsu-Port.git (rama `main`, CI en GitHub Actions)
- **Matriz de versiones (NO cambiar a la ligera):** architectury-loom `1.7.435`,
  architectury-plugin `3.4.164`, architectury API `13.0.8`, fabric-loader `0.16.10`,
  fabric-api `0.116.13+1.21.1`, neoforge `21.1.235`. Mappings **Mojmap**.

## Arquitectura (resumen)

Clean/Hexagonal sobre Architectury: la lógica vive en `common`; toda diferencia de loader se accede
tras un **puerto** en `com.zamu.dojutsu.platform`, resuelto por **`ServiceLoader`** vía la fachada
`PlatformServices`, con impl por loader en `neoforge`/`fabric` (`META-INF/services/*`).
Puertos existentes: `RegistrarService`, `AttachmentService`, `NetworkService`.

## Estructura del proyecto

```
common/   → lógica común: DojutsuMod, platform/ (puertos), registry/ (Mod*), core/, y andamiaje F0
neoforge/ → adaptador NeoForge (loom.platform=neoforge)
fabric/   → adaptador Fabric
docs/     → documentación viva del proyecto (leer current_state, handoff_phase0, architecture...)
```

## Módulos actuales (paquetes bajo `com.zamu.dojutsu`)

**Permanentes:** `DojutsuMod` (raíz) · `platform` (puertos) · `registry` (`Mod*`) · `domain.*` (dominio puro
sin MC: `resource`, `strain`, `mode`, `jutsu`, `progression`, `rng`, `event`, **`nature`**, **`eye`**/**`susanoo`**/**`vision`** [F5]) · `data.*` (`PlayerNinjaData` +
sub-estados `state/{Onboarding,Chakra,Progression}State` + **`NatureProfile`**+`TemporaryGrants`, `LeveledStats`, **`EyeState`**/**`SusanooState`**/**`VisionState`** [F5], `NinjaData`, `DirtyTracker`, `migration`) · `core.*`
(`identity`, tick pipeline, `scheduler`, `lifecycle`, `config`, `command`, `DojutsuLifecycle`, **`DomainEvents`**)
· **`chakra`** (`ChakraService`, `RegenService`, `Sleep`/`FoodChakraHandler`) · **`progression`**
(`SkillPointService`, `XpApi`, `LevelingService`, `RankService`, `Unlock*`) · **`natures`**
(`NatureService`, `KekkeiGenkai{Service,Registry,Definition,ReloadListener}`, `TemporaryGrantService`, `NatureRoll*`, `LearnerService`) · **`items`** (`LearnerItem`+`LearnerComponent`, `JutsuItem`+`JutsuComponent`+`JutsuModelData`, `WeaponItem`+`WeaponComponent`, `LearnerModelData`; assets de jutsu/armas/learner portados vía `custom_model_data` — F4/DJT-085b/c) · **`jutsu.kenjutsu`** (`WeaponAttributeService` — F4/DJT-082, `SamehadaService` — F4/DJT-084) · **`clans`**
(`ClanDefinition/Registry/ReloadListener`, `ClanService`, `ClanJutsuUnlockService`, `ClanTeamService`) · **`dojutsu`** (grafo de ojos data-driven: `EyeDefinition`+`EyeRegistry`+`EyeReloadListener` desde `data/dojutsu/eyes/*.json` — F5/DJT-113; **`AwakeningService`** despertar por clan — F5/DJT-114; **`EvolutionService`** evolución por uso/grafo — F5/DJT-115; **`EyeStrainService`** fatiga ocular — F5/DJT-116; **`EyeMenuService`** menú de habilidad ocular — F5/DJT-117; **`ability/*`** (`EyeAbility`+`EyeAbilityService`+`AmaterasuAbility` vía `AbilityAnchor`, tecla R — F5/DJT-118; +18 habilidades [velocidad; Ketsuryugan; genjutsu/teleport/melé; gravedad; proyectil/defensa/Tenseigan; Sharingan stun+copia/canalización] + `cooldownTicks()` en el contrato — F5/DJT-119 lotes 1-6; **`susanoo/SusanooService`** primer consumidor de `ToggleableMode`, coste por pulsos y maestría 1..6 — F5/DJT-120; **`susanoo/SusanooAvatar`** ancla parenteada con silueta por etapa + escalera de etapas — F5/DJT-121; **`vision/ChakraVisionService`** chakra vision con glow nativo + equipos por relación (RB-3: sin renderer propio ni selector por tick), disponibilidad data-driven, tecla C — F5/DJT-125; **`tenseigan/TenseiganChakraModeService`** 2º `ToggleableMode` (buffs+vuelo, pulso 10/200t), la 21ª habilidad cierra el catálogo — F5 fleco; **`EternalAwakeningService`** despertar del EMS por acción del menú (clan+uso≥864000+medical≥150 → eternal + bonos) — F5 fleco) ·
**`core.onboarding`** (`OnboardingController`) · **`entity`** (`NinjaProjectile`, `AbilityAnchor`, `MarkerEntity` — EP-ENTITY completa, F4/DJT-070..072) · **`util`** (`ProjectileMath`, `RaycastHelper`) · **`jutsu`** (`JutsuEntry`+`JutsuRegistry`+`JutsuReloadListener` — F4/DJT-073; `LearnSequence*`+`JutsuLearningService` — F4/DJT-075; **`JutsuCastPipeline`**+`PlayerCastContext` — F4/DJT-074, **único camino de cast**; `effect/*` las 6 familias del ADD parametrizadas — F4/DJT-076; `JutsuXpHandler` XP de afinidad por cast — F4/DJT-078; `LastCastRegistry` último cast para copia Sharingan — F4/DJT-079) · `network`
(`SyncNinjaDataPayload`+`ProgressionSnapshot`, `NinjaSync`, `MenuActionPayload`, `OnboardingChoicePayload`,
`OnboardingOptionsPayload`+`OnboardingSync`, `CastJutsuPayload` (F4/DJT-080), `handler/*`) ·
`client` (solo cliente: `ClientNinjaData`, `ClientOnboardingOptions`, `keybind` [M menu, V cast, N eye-menu, R eye-ability, C chakra-vision], `screen`, `hud` [chakra, rango, cd de jutsu]). Adaptadores: `neoforge.*`, `fabric.*`
(incl. `mixin/LivingEntityMixin` y los `*ServerEvents` de eventos nativos).
**Temporal:** `debug` (`/dojutsuninja`, `/dojutsusched`, `/dojutsuchakra`,
`/dojutsuprogression`, `/dojutsunatures`, `/dojutsuclans`, `/dojutsuonboarding`, `/dojutsuprojectile`, `/dojutsuanchor`, `/dojutsujutsu`, `/dojutsuweapon`, **`/dojutsudojutsu`** [F5: activar ojo/uso/strain/eternal + `susanoo level|use` para QA]). **Aún no existen:** los **modelos 3D** del Susanoo (assets), `throwing`,
`sage`, `empowerment`, `bijuu`, el resto de `items` (F8), `summoning`, `ai`, `trading`, `story`, `world`, `api`.

## Convenciones clave

- `common` **nunca** importa clases de loader; solo puertos.
- Registries = clases `Mod<Tipo>` que usan `PlatformServices.REGISTRAR`/`ATTACHMENTS`; IDs con
  `DojutsuMod.id(path)`.
- Puertos = interfaz en `platform` + impl `<Loader>XxxService` + service file.
- Comentarios/logs en español; **logs sin acentos**. Ver `docs/coding_conventions.md`.

## Estado actual

> ⚠️ **Lo que sigue en esta sección va por detrás del código** (se quedó en F5/F8). El estado real:
> **F0–F9 cerradas**, **F10 en curso**. La foto verificada archivo a archivo es
> `docs/AUDITORIA_ESTADO_ACTUAL.md` (2026-07-30); lo posterior, `docs/development_log.md` y
> `docs/backlog.md §Fase 10`. Ponerlo al día entero es DJT-223.
>
> **F10:** DJT-225 ✅ (los 17 ítems que nunca se crearon — 9 huevos de bestia + los 8 craftables; el
> drop-crafting deja de tener combinaciones inertes y el comercio baja de 15 a 12 mercancías inexistentes;
> **930 tests**) · DJT-224 🔄 (assets, pasada 1) · DJT-223/226/221/222/220 ⬜.

- **Fases 0–4: COMPLETADAS** en NeoForge y Fabric. **F5 (Poderes oculares) EN CURSO.**
  **F4 (Combate) CERRADA** (DJT-001…112): motor de cast único + 128 jutsus + kenjutsu; validación runtime del
  2026-07-19 (build + tests + arranque de servidor en ambos loaders con carga limpia — ver `current_state.md §3.4a`).
  **F5 LÓGICA COMPLETADA**: DJT-113…**125** hechos (grafo+`EyeState`, despertar, evolución, fatiga, menú,
  **catálogo completo de 21 habilidades** oculares, **Susanoo completo** —`ToggleableMode` + avatar + etapas—
  y la **chakra vision** —glow nativo por relación, tecla C—); **14 ojos** cargan y ambos loaders arrancan limpios
  (`current_state.md §3.5`). **FLECOS DE F5 CERRADOS**: `tenseigan_chakra_mode` (2º `ToggleableMode`), refund
  "sin objetivo" (`EyeAbility.canApply`, no_cast_nby), avisos de fatiga en chat (`esw_*`) y **EMS/eternal manual**
  (`EternalAwakeningService`, botón en el menú). Único pendiente de F5: los modelos 3D del Susanoo (assets).
  **Roster de ojos EN CURSO**: **8 Mangekyō por personaje** (variante aleatoria `mangekyo_rng`, `EyeEvolution.variants`) → **14 ojos**; técnicas únicas hechas: **Kamui** (Obito, `KamuiAbility`+`KamuiOthersAbility`) **Tengai Shinsei** (Madara) **Hinokagutsuchi** (Indra) **Kotoamatsukami** (Shisui) las dos de **Shin** y **Flame Control** (Sasuke, apaga la llama) → **los 8 Mangekyō jugables**. **Roster de Mangekyō CERRADO en código** (+beams_of_light, +Blaze KG data-driven vía `EyeDefinition.awakenKekkeiGenkai`). Assets 3D diferidos a F10. **651→717 tests** verdes; build multiloader verde.
- **F6 (Modos de potenciación) ARRANCADA**: DJT-130 ✅ — `empowerment/ToggleableModeService` (arnés compartido de modos: hospedaje por jugador, tick, caída, evento+sync; salda la deuda de los 2 servicios de modo de F5) + `data.state.EmpowermentState` (sello maldito/puertas/SOAH, campos verificados del datapack). + DJT-135 ✅ `sage/NaturalEnergyChargingService` (carga por umbral de VELOCIDAD —S08-R1, no sneak_time—; 1200t de espera + pulsos 800→25 por nivel, mitad con aceite; al llenar 100 te vuelves sabio del bioma) + `SageState`. + DJT-136 ✅ `sage/SageService` (modo sabio sobre el ARNÉS —1er consumidor real—; encender exige MÁS energía a menor nivel [100/50/25/1], el nivel ALARGA el modo [drena 1 cada 100→500t]; excluye sello maldito; niveles por requisitos compuestos chakra+senjutsu+uso; tecla G) + `/dojutsusage` QA. EP-SAGE cerrada. Siguiente: DJT-140 (sellos malditos).
- **El vertical slice funciona end-to-end:** chakra regenera (barra HUD) → matas mobs y ganas skill points →
  **M** abre el menú (GUI real) → gastas puntos (payload **validado en servidor**) → subes nivel/rango →
  comes/duermes y repones chakra → mueres y respawneas con ½ chakra → hitos desbloquean. Todo persiste.
- **Hito de validación arquitectónica superado** (`[ADD/08 §6.2]`): red + input + GUI + persistencia + economía
  funcionan juntos → **el resto es contenido incremental sobre patrones probados** (ver `docs/architecture.md §8`).
- Ver **`docs/handoff_phase3.md`** para el traspaso completo F3→F4 (decisiones, deuda, hooks `// TODO F4` y qué reutilizar).

## Fases

- ✅ **F0** — Fundaciones y andamiaje.
- ✅ **F1** — Núcleo de dominio (`domain`, con **TDD**) + `data`/`PlayerNinjaData` + motor + sync base.
- ✅ **F2** — Recursos y progresión (`chakra`, `progression`, cliente) = **VERTICAL SLICE** (DJT-030…042).
- ✅ **F3** — Definición del ninja (DJT-050…063: naturalezas, KG, clanes, onboarding por GUI).
- ✅ **F4** — Combate (DJT-070…112: entidades, motor de jutsus, kenjutsu, 128 jutsus de contenido).
- 🔄 **F5 (en curso)** — Poderes oculares (S05 ojos + S24 Susanoo + S25 Chakra Vision). IDs renumerados a
  **DJT-113+** (los DJT-100…112 del backlog de diseño colisionan con el contenido ya volcado en F4).
  **DJT-113…118 ✅ + DJT-119 lotes 1-6 ✅ + DJT-120/121 ✅ + DJT-125 ✅ + flecos** (grafo+`EyeState`, despertar, evolución, fatiga, menú, 21 habilidades oculares [catálogo completo], Susanoo completo, chakra vision). **F5 lógica completa**; cerrando flecos (tenseigan_chakra_mode ✅). Siguiente: F6–F10.

## Instrucciones importantes para futuros desarrollos

1. **Fuente de verdad del diseño (fuera de este repo):** `D:\Projectos claude\Dojutsu reverse\DOCS\`
   → `sistemas/` (ingeniería inversa S01–S25), `arquitectura/` (ADD), `backlog/` (fichas DJT-###).
   No re-diseñar ni reinterpretar el datapack; implementar los tickets.
2. **Un ticket a la vez**; validar en runtime en **ambos** loaders antes de darlo por cerrado.
3. **NO usar el `NetworkManager` de Architectury** (bug S2C en Fabric 13.0.8, issue #518): usar el
   puerto `NetworkService` (nativo por loader).
4. **No actualizar** loom/fabric-loader/neoforge/architectury sin verificar la matriz (rompe el build).
5. Toda `EntityType` registrada necesita **renderer** en cliente (`NoopRenderer` para marcadores).
6. Al añadir un sub-estado a `PlayerNinjaData`, seguir el **patrón enchufable** (value object con
   `DEFAULT`+`CODEC` + 1 línea de codec + puerta de escritura); **no reescribir el contrato central**.
7. El puerto **`AttributeService`** (adjuntar atributos a `Player`; NeoForge `EntityAttributeModificationEvent`,
   Fabric mixin) sigue **diferido**. F4/DJT-082 (kenjutsu) —el cliente previsto— optó por el data component
   **`ItemAttributeModifiers` vanilla** (el bonus vive en el ítem, reversible por construcción), evitando abrirlo.
   Se retomará solo si un sistema necesita modificadores **sobre la entidad** que no puedan ir en un ítem
   (p. ej. puertas/boosts corporales de F6).
8. Antes de tocar temas de build/versiones/networking, leer `docs/development_log.md` (registra los
   problemas ya resueltos y por qué).
8b. **`/reload` NO lee `common/src/main/resources`.** En dev, el datapack del mod se sirve desde los recursos
    **compilados** (`common/build/resources/main/data/dojutsu/**`). Para probar un cambio de JSON in-game:
    editar ahí directamente y `/reload`, o editar en `src/` + `./gradlew :common:processResources` + `/reload`.
    Si no, se prueba el archivo viejo **sin notarlo** (pasó en DJT-054: el log decía "17 tramos" = el original).
    Comprobar siempre el log del reload listener, que dice qué cargó de verdad.
9. **Eventos loader-nativos no cubiertos por Architectury** (p. ej. comer/dormir): Architectury 13.0.8 no los
   expone cross-loader. Se suscriben en los adaptadores (`Dojutsu<Loader>ServerEvents`, patrón "push") y delegan
   en un handler común. NeoForge suele tener el evento; Fabric a veces necesita **mixin** (p. ej. "fin de uso de
   ítem": NeoForge `LivingEntityUseItemEvent.Finish`, Fabric mixin en `LivingEntity#completeUsingItem`). La
   **infraestructura de mixins de Fabric ya existe** (`fabric/.../mixin/`, `dojutsu.mixins.json` referenciado en
   `fabric.mod.json`; architectury-loom auto-genera el refmap). Ver `[ADD/05 §línea 6]` y DJT-032.
10. **Únicas puertas de escritura:** el chakra **solo** por `ChakraService`; la XP **solo** por `XpApi` (que
    auto-nivela y emite los eventos de dominio); el perfil elemental **solo** por `NatureService`. No escribir
    esos campos por otra vía.
10b. **El estado del ninja es inmutable hasta el fondo** (DJT-063): `PlayerNinjaData` aloja sub-estados que son
    value objects, y sus hojas también (`LeveledStat` es un `record`: `withXpAdded` **devuelve copia**, no muta).
    Para cambiar una habilidad/afinidad se **reemplaza** el stat (`withSkill`/`withAffinity`), nunca se muta.
    Motivo: los `DEFAULT` son estáticos compartidos y una mutación los envenenaría para todo el proceso.
11. **Reactivo, nunca polling:** lo que reacciona a un cambio se suscribe al bus (`DomainEvents`) o a un evento
    del loader. No añadir sistemas por tick para sondear estado (el `PlayerTickPipeline` es para trabajo real
    por tick, como la regen). No reintroducir la guardia "1 level-up/tick" del datapack.
12. **El cliente nunca decide gameplay:** toda acción del cliente es un payload **validado en servidor**
    (emisor, ids **por nombre** —nunca índices—, rangos, rate-limit) que aplica y sincroniza; la UI se refresca
    **tras** el sync. Ver `MenuActionHandler` como referencia.
13. **Solo lo observable cruza la red** (`ObservableField` + `DirtyTracker`); lo derivable se calcula en cliente
    (p. ej. el rango desde `ninjaXp`). El código de `client.*` se invoca **solo** desde `initClient()`.
14. **Patrones establecidos a reutilizar** (sub-estado enchufable, servicio puro + glue MC, carga data-driven por
    reload listener, evento loader-nativo, acción validada): ver la tabla de `docs/architecture.md §8`.
15. **RA-4 (economía):** no improvisar números. Las fuentes de XP/costes están auditadas en
    `common/src/main/resources/data/dojutsu/balance/xp_sources.json`; lo marcado `TBD_datapack` debe extraerse
    del datapack al portar el sistema correspondiente.
16. **Los resúmenes de `DOCS/sistemas` (S01–S25) NO son la fuente de verdad de los números ni de las ramas: lo
    es el datapack** (`D:\Projectos claude\Dojutsu reverse\data\`). F3 ya encontró tres afirmaciones falsas de
    `[S03]` (XP de naturaleza "uniforme 25" → es 25/20/30/100; "18 KG" vs 19 ficheros; "afinidad inicial 50 en el
    give" → es del onboarding). **Verificar cada valor y cada rama contra los `.mcfunction`/predicates antes de
    implementarlos**, y registrar la discrepancia en `docs/development_log.md`.
