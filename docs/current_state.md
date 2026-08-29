# Estado actual del proyecto — Dojutsu Port

> ⚠️ **Este documento va por detrás del código.** Su cabecera y sus tablas llegan hasta F8; **F9 está cerrada
> en código** (paquetes `sixpaths`, `story`, `world`) sin que nadie escribiera aquí su entrada, **F10 está
> cerrada** y **F11 (UX) tiene el código terminado**. Para el estado verificado archivo a archivo la referencia es
> [`AUDITORIA_ESTADO_ACTUAL.md`](AUDITORIA_ESTADO_ACTUAL.md) (2026-07-30, 912 tests); para lo hecho después,
> `development_log.md` y `backlog.md §Fase 10`. Ponerlo al día entero es trabajo de DJT-223.
>
> **F10 — hecho hasta hoy (2026-08-01):**
> - **DJT-225 ✅** — los **17 ítems que nunca se crearon**: los **9 huevos de bestia** (el Diez Colas no tiene,
>   porque el datapack tampoco le dio receta) y los **8 craftables** que `CraftingTable.PENDING_NATIVE_RECIPES`
>   dejó anotados en DJT-206 (bomba de humo, sello explosivo suelto, ramen ×2, los tres pergaminos y el
>   **tanto** — el que la ficha se dejó al contar 8 y nombrar 7). Enciende contenido ya escrito: el
>   drop-crafting pasa de **9 combinaciones inertes a 0** y el comercio de **15 mercancías inexistentes a 12**.
>   Cierra además la puerta de crafteo que DJT-206 dejó a medias (las **4** recetas gateadas, no 1). Cuatro
>   hallazgos: el **ramen repone 150** (se cumplen los dos advancements y se suman), la **bomba de humo no
>   ciega a nadie**, el **sello explosivo es una mina** con 10 s de armado y doble daño, y el Diez Colas no
>   tiene huevo craftable. **930 tests**; ambos loaders arrancan limpios con carga idéntica (1303 recetas).
> - **DJT-223 🔄** — **el cliente arrancó por primera vez en el proyecto**, en los dos loaders y entrando en un
>   mundo: 0 modelos/texturas sin resolver, los 6 renderers registran, la GUI de creación se ve con sus 14
>   iconos de clan, y la **migración desde saves de F1 quedó verificada** (`LegacySaveMigrationTest`). La mitad
>   interactiva no se puede automatizar y vive como checklist: `docs/checklist_paridad_cliente.md`.
> - **DJT-226 ✅** — deuda de sistemas: los **siete** modos usan ya el arnés `ToggleableModeService` (los dos de
>   F5 llevaban cuatro fases fuera del arnés que se extrajo de ellos); las **9 técnicas insignia** de bestia
>   (registro 1 → **10**), que resultaron ser **la misma técnica con distintas partículas** — una clase
>   parametrizada, no nueve—; y el **roster del Rinne Sharingan**, que tenía copiado el del Rinnegan: ahora las
>   **13** entradas de sus tres páginas más **Amenominaka** (8000 de chakra, arrastra a todo el que tengas a 10
>   bloques a otra dimensión, sin el `forceload` del datapack). **947 tests**.
> - **DJT-222 ✅** — rendimiento: auditados los **21 puntos de entrada por tick** y **ninguno está sin acotar**
>   (`docs/perf.md`). Tres correcciones con números —`new Random()` por jugador y tick en dos servicios, la
>   guarda que faltaba en `EvolutionService`, y un `String` por entidad y barrido en la chakra vision— que son
>   7-11× más rápidas y, dicho honestamente, el **0,001 %** de un tick. El eje de escalado real son los barridos
>   de entidades, identificados y **no tocados a propósito**: medirlos exige jugar.
> - **DJT-221 ✅** — balance: los **16 `TBD_datapack`** de RA-4 resueltos contra el datapack, y con ellos el
>   hallazgo de que **los cinco umbrales de kenjutsu estaban inventados** (5/10/20/30/40 → 75/88/125/150/350).
>   La escala «datapack ÷ 4» queda **justificada** (el tope del mod, 500, es exactamente el requisito más alto
>   del datapack, 2000, dividido). Recetario de hitos **23 → 36**, cada uno con su ninja_xp real (1..100, que
>   antes no se pagaba). Y se tapó el punto ciego del modelo reactivo: los cuatro hitos con co-requisito de
>   chakra se recuperan con un barrido al entrar.
> - **DJT-224 🔄** — pasada 1 de assets hecha (ítems, iconos de clan, 28 sonidos, `LICENSE` + `ATTRIBUTION.md`).
> - **Deuda de fondo ✅** — las nueve fases sin commitear pasaron a git (rama `f1-f10-port`), los **26 jutsus
>   inalcanzables** bajaron a 1 con un `JutsuGrantService` compartido, y los **20 comandos de depuración** dejaron
>   de estar abiertos a cualquier jugador (nivel de OP 2).
> - **DJT-220 ✅** — **API pública** `com.zamu.dojutsu.api` v**1.0.0**: `DojutsuApi` (leer vía `NinjaView`,
>   escribir por las puertas del mod, registrar efectos/habilidades/técnicas de bestia, suscribirse a los eventos
>   de dominio) y `DojutsuRegistryEvents` (jutsu/ojo/clan/contrato, disparados **dentro de cada recarga**, de modo
>   que el contenido de un tercero se recarga con `/reload`). Los 9 tests del criterio de la ficha se escriben
>   **desde fuera**, importando sólo `api`. **973 tests**; ambos loaders cargan igual que antes — sin suscriptores
>   la API no cambia nada. Uso en `docs/api.md`.
> - **Pendiente de F10**: sólo **DJT-224 pasada 2** (arte: modelos de Susanoo y Mangekyō, protectores de frente)
>   y la mitad interactiva de DJT-223, que únicamente se cierra jugando.
>
> **F11 — UX e interfaces (2026-08-28): código terminado, falta la pasada en partida.**
> Nace de cinco ideas del usuario; plan y desviaciones en `PLAN_UI_ETAPAS_5-1-4-3.txt`, detalle por ticket en
> `backlog.md §Fase 11`. Build multiloader verde, **1080 tests**.
> - **DJT-230 ✅** — **toggle de dōjutsu** (tecla **X**): `EyeState.enabled` separa *qué ojo tienes* de *si está
>   encendido*, y apagarlo quita los bonos. De paso **corrige un bug real**: al llegar al 100 % de fatiga el ojo
>   se **borraba** —con su forma eterna— sin ninguna vía de recuperación en partida.
> - **DJT-231 ✅** — **selector de jutsu**: el cliente ya conoce su lista de aprendidos (delta propio: la lista
>   sólo viaja cuando cambia, porque los deltas los dispara la regeneración de chakra) y el **jutsu
>   seleccionado** vive en el servidor, dentro de `JutsuKnowledge`. La tecla de cast lanza eso.
> - **DJT-232 ✅** — **el ítem deja de castear**: `JutsuItem` es un pergamino; el clic derecho **selecciona**.
>   Sin migración: el componente no cambió. Arrastró repuntar la barra de cooldown del HUD, que leía el ítem de
>   la mano y habría quedado mintiendo.
> - **DJT-233 ✅** — **HUD movible**: anclas + offsets **en fracción de pantalla** (sobreviven a un cambio de
>   resolución) y `HudEditorScreen` con arrastre y guías. Auditoría del coste **por frame** en `perf.md §7`.
> - **DJT-234 ✅** — **rueda radial** (tecla **Z**, mantener) que muestra **sólo los 8 huecos**, más los huecos
>   rápidos **ALT+1..8**, que se comen la pulsación **antes** que vanilla para no mover la hotbar.
> - **DJT-235 ✅** — **libro de jutsus**: fichas del `JutsuRegistry` (coste, cd, daño, afinidad) enviadas por red,
>   porque el registro es data-driven y sólo existe en servidor; aquí se reparten los huecos.
> - **DJT-236 ✅** — **las tres pantallas de preparación pasan a pestañas bajo M** (Ninja / Jutsus / Eye).
>   **N y P quedan libres**: el mod usa **13** teclas donde antes usaba 15, con más funcionalidad.
> - **DJT-237 ⬜ BLOQUEADO** — editor de posición de la textura del ojo: **no existe** render de overlay ocular
>   sobre el jugador ni texturas de ojo en assets, y la etapa da por hechas las dos cosas.
> - **Pendiente de F11**: la validación **en partida en los dos loaders** (regla 2) —
>   `CHECKLIST_PRUEBAS_F11.txt` (A y C) y `CHECKLIST_PRUEBAS_F11_ETAPA_B.txt` (B) — y el **coste pasivo de
>   chakra del ojo**, que sigue sin implementar (deuda de DJT-116; números ya extraídos en el plan, §A3-BIS).
>   Con el toggle ya en juego, tener el ojo encendido hoy no cuesta nada.
>
> ---
>
> Última actualización de lo que sigue: 2026-07-28 · **Fases 0–4 COMPLETADAS · F5 (Poderes oculares) EN CURSO**.
> **Fase 4 (Combate) CERRADA**: motor completo + 128 jutsus, validada en runtime en ambos loaders (§3.4a).
> **F5 (Poderes oculares) COMPLETADA en lógica** (incl. flecos): DJT-113…**125** (grafo+`EyeState`, despertar,
> evolución, fatiga, menú, **catálogo completo de 21 habilidades** oculares, **Susanoo completo**
> —modo+avatar+etapas—, **chakra vision** y EMS manual). **Roster de ojos EN CURSO**: **8 Mangekyō por
> personaje** (variante aleatoria `mangekyo_rng`) → **14 ojos cargan**; técnicas únicas hechas: **Kamui**
> (Obito, +Others), **Tengai Shinsei** (Madara) **Hinokagutsuchi** (Indra) **Kotoamatsukami** (Shisui) las dos de **Shin** y **Flame Control** (Sasuke) → **los 8 Mangekyō jugables**. **717 tests** verdes; build multiloader
> verde. **Roster de Mangekyō CERRADO en código** (incl. beams_of_light y el Blaze KG data-driven de Sasuke). Assets 3D → ticket F10.
> **F6 (Modos de potenciación) ARRANCADA**: DJT-130 ✅ (arnés `ToggleableModeService` + `EmpowermentState`) , DJT-135 ✅ (energía natural por quietud) y DJT-136 ✅ (modo sabio, tecla G, +`/dojutsusage` QA) — EP-SAGE cerrada.
> **DJT-140 ✅** (sellos malditos): 6 sellos **como datos**, inyección al **60%**, nivel 1→2 que **abarata** el
> mantenimiento (12→8), y **`onPunish` estrenado** — es el primer modo que **mata** al portador si no puede
> pagarlo (y a las 10 muertes en nivel 2 el sello degrada). Tecla **B** + `/dojutsuseal` (QA); exclusión
> sage↔sello cableada. EP-CURSED-SEAL cerrada.
> **DJT-145 ✅** (Ocho Puertas): **escalera** de 8 puertas (tecla **H**; agachado cierra) con fatiga corporal que
> sube al abrirlas y baja siempre; a 100 se cierran solas. Los **dos riesgos grandes de `[S11]` no existían**: el
> datapack usa `MobEffect` refrescados, no atributos (S11-R4 disuelto), y la 8ª puerta **no mata**. Bandas de
> aviso unificadas con la fatiga ocular (S11-R1). De paso se cerraron el TODO de `DeathHandler` y el hook vacío
> de `RespawnHandler` (de F1). EP-EIGHT-GATES cerrada.
> **DJT-150 ✅ · F6 COMPLETADA**: el SOAH es una **hucha de chakra** (recarga manual de 20 000 agachándote;
> devuelve por tramos 100/10/1 cuando bajas de 5000, hasta dejarte al máximo). Tecla **J** + `/dojutsusoah` (QA).
> Es el **único de los seis modos que devuelve recurso en vez de cobrarlo**, y encajó en el framework de F1 sin
> tocarlo. **763 tests** verdes. **Las tres fichas de F6 tenían errores de bulto** —la inyección fallida del sello
> no mata, la 8ª puerta no es letal y el SOAH no tiene carga lenta— desmentidos leyendo los `.mcfunction`.
> **F7 (Bestias con cola / Jinchūriki) ARRANCADA**: **DJT-160 ✅** — `TailedBeastEntity`, la **primera criatura
> viva** del mod: entidad jefe propia con **bossbar nativa** (S09-R6: adiós a diez `bossbar` leyendo NBT cada
> tick) y `BeastMove`, una sola implementación para la IA y para el jinchuriki transformado (S09-R2: en el
> datapack las dos Bolas Bestia con Cola **ya habían divergido**). Diez bestias como enum, `/dojutsubeast` (QA),
> visual provisional con la malla del ravager vanilla → modelos a F10. **770 tests** verdes.
> **DJT-161 ✅**: goals de combate + `BeastAiPlan` (dominio puro). Hallazgo: la IA **desperdicia la mitad de sus
> turnos a propósito** (la tirada 0..5 deja el 0, el 3 y el 5 sin mapear para una bestia normal) — esas pausas
> son lo que hace la pelea jugable. Primera técnica compartida real, la **Bola Bestia con Cola**, justo donde el
> datapack se había partido en dos.
> **DJT-165 ✅**: sellado + `TailedBeastRegistry` global (`SavedData`). **Cuarta corrección de ficha**: no existe
> ningún "+8000" — son **1000 de chakra máximo por cola** (1000…10 000), y la XP rompe su patrón dos veces (el 9
> da 230, el 10 da 1000). La respuesta real a RA-2 es **guardar el préstamo** para devolverlo exacto.
> `TemporaryGrantService` (escrito en **F3** anticipando F7) se conectó sin tocar una línea. **787 tests** verdes.
> **DJT-166 ✅ · F7 COMPLETADA**: forma de jinchuriki (tecla **K**), **vínculo** que sube solo por portar la
> bestia (2-5 h por nivel; 8 h el último con Kurama) y **Modo Baryon terminal** — al agotarse, la bestia **se
> va**. Cierra el círculo de `BeastMove`: el jinchuriki lanza **la misma instancia** que la IA. Hallazgo: los
> ojos se **suprimen**, no se apagan (apagarlos rompería la forma del Diez Colas, que exige el Rinnegan activo).
> **799 tests** verdes.
> **F8 (Ítems, mundo y comercio) ARRANCADA**: **DJT-170 ✅** — ADN (26 tipos, implante al 33% con dos excepciones
> garantizadas), máscaras (**estado, no casco**, como los ojos en F5) y píldoras (5 cargas en la durabilidad; la
> resaca **no se encadena**). Hallazgo: de los 4 campos que la ficha pedía, **3 ya tenían dueño** en F3/F6 —
> `ItemFlags` tiene un solo campo real. `/dojutsuitem` (QA).
> **DJT-175 ✅** (invocaciones): 27 contratos **data-driven** (13/12/2 por tier) y caducidad por
> **`AbilityAnchor`** — S15-R3 resuelto **reusando la primitiva de F4**, sin los dos selectores globales por tick
> del datapack. Invocar **no cuesta chakra**: cuesta veto (600→200 ticks según fuinjutsu). **823 tests** verdes.
> **DJT-180 ✅** (arrojadizas + Vuelo del Dios del Trueno): la marca es un `GlobalPos` **en el Attachment**, así
> que **desaparece el `forceload`** con el que el datapack mantenía un chunk cargado por marca. Hay **una sola
> marca** y saltar la **consume**; dominarlo abarata el salto 5× y el clan **Namikaze** aprende al doble. Tecla
> **Y**. **834 tests** verdes.
> **DJT-185 ✅** (IA shinobi, el de mayor riesgo de F8): la IA del datapack estaba **literalmente a medias**
> (`say do nothing`, y un roster de 30 jutsus que nadie llama), pero su **ritmo** sí estaba decidido y es el mismo
> que el de las bestias. Lo diseñado de cero son las **bandas de distancia**. **S17-R3 estaba cerrado desde F4**:
> `CastContext` y `JutsuEffect` ya aceptaban casters no-jugador → un NPC castea por **el mismo pipeline y las
> mismas puertas**, sin tocar nada. 6 niveles de amenaza (120…700 de vida) + `EdoTenseiService`. **845 tests**
> verdes.
> **DJT-190 ✅ · F8 COMPLETADA**: comercio ninja. Las **36 líneas de NBT literal** del datapack pasan a **dos
> JSON** (RB-4), y la conversión va **por evento** en vez del bucle que recorría todos los aldeanos cada segundo.
> El catálogo declara el surtido completo y **cuenta en el log lo que falta** (`24 ofertas, 15 pendientes de su
> ítem`). **855 tests** verdes.
> **Balance de F8:** cuatro riesgos cerrados **por modelado** (S15-R3, S16, S17-R3, RB-4) y **cuatro piezas de
> fases anteriores que encajaron sin tocarlas** — tres de ellas anotadas por escrito desde F3/F4.
> Siguiente: **F9** (Mundo, narrativa, Seis Caminos, Diez Colas, Árbol Divino), la cúspide.
> Este documento refleja el estado real del código en `D:\Projectos programacion\Dojutsu Port`.

## 1. Resumen

Mod de Minecraft **multiloader (Architectury)** que reconstruye el datapack *Dojutsu*
(Naruto/Boruto) como mod moderno. Las **Fases 0 (andamiaje), 1 (núcleo, dominio y estado) y 2
(recursos y progresión)** están **completadas y validadas en runtime** en NeoForge y Fabric.

Sobre el dominio puro y el estado por jugador de F1, la **Fase 2 entregó el vertical slice completo**:
**chakra vivo** (regeneración con la tabla de `[S02]`, reposición por comer/dormir, ½ al morir),
**progresión** (skill points por kills, auto-nivelación reactiva por XP, rango, desbloqueos data-driven) y
**cliente** (keybind, menú GUI real con gasto validado en servidor, HUD de chakra y rango).

**El hito de validación arquitectónica está superado** (`[ADD/08 §6.2]`): red + input + GUI + persistencia +
economía funcionan juntos → **el resto del proyecto es contenido incremental sobre patrones probados**. La
Fase 3 define al ninja (naturalezas, clanes, onboarding).

- **Minecraft:** 1.21.1 · **Java:** 21 · **Gradle wrapper:** 8.10.2
- **Loader:** Architectury (módulos `common` + `neoforge` + `fabric`)
- **Namespace / mod id:** `com.zamu.dojutsu` / `dojutsu` · **Versión:** 1.0.0
- **Repositorio:** https://github.com/Z4MU/Dojutsu-Port.git (rama `main`, CI en verde)

## 2. Fases

| Fase | Nombre | Estado |
|------|--------|--------|
| **F0** | Fundaciones y andamiaje multiloader | ✅ **Completada** |
| **F1** | Núcleo de dominio y datos (`domain`, `data`/`PlayerNinjaData`) | ✅ **Completada** (DJT-010…028, validada en runtime) |
| **F2** | Recursos y progresión (**VERTICAL SLICE**) | ✅ **Completada** (DJT-030…042, validada en runtime) |
| **F3** | Definición del ninja | ✅ **Completada** (DJT-050…063, validada en runtime) |
| **F4** | Combate | ✅ **Completada** (DJT-070…112; contenido cargado y validado en runtime en ambos loaders; ver §3.4a) |
| F5–F10 | Contenido incremental (plantillas) | ⬜ No iniciada |
| **F11** | UX e interfaces (DJT-230…237) | 🔄 **Código terminado** (230…236 ✅, 1080 tests); falta partida. DJT-237 bloqueado por falta de arte |

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

## 3.1. Historias y subtareas (F1 — en curso)

| Ticket | Historia | Estado |
|--------|----------|--------|
| DJT-010 | `Resource` + `RegenPolicy` + `RegenFactor` (dominio de recurso, TDD) | ✅ |
| DJT-011 | `StrainMeter` + `StrainThreshold` (fatiga con umbrales, TDD) | ✅ |
| DJT-012 | `ToggleableMode` + `ModeContext` (modo activable, TDD) | ✅ |
| DJT-013 | `Jutsu`/`JutsuDefinition`/`CastContext`/`CastResult` (contrato de técnica, TDD) | ✅ |
| DJT-014 | `LeveledStat`/`XpCurve`/`NinjaRank` (progresión, TDD, RA-4) | ✅ |
| DJT-015 | `RollTable` + `WeightedEntry` (RNG por pesos, TDD) | ✅ |
| DJT-016 | `DomainEventBus` + 8 eventos internos (cierra EP-DOMAIN) | ✅ |
| DJT-017 | `PlayerNinjaData` (agregado raíz) + `Codec` mínimo + `OnboardingState` | ✅ |
| DJT-018 | `DataVersion` + `MigrationChain` + `Migration` (idempotente) | ✅ |
| DJT-019 | `NinjaData.get` sobre `AttachmentService` (attachment real, codec migratorio) | ✅ (persistencia validada en runtime en NeoForge y Fabric) |
| DJT-020 | `PlayerRef` — identidad por UUID (abre EP-CORE) | ✅ |
| DJT-021 | `PlayerTickSystem` + `PlayerTickPipeline` + `TickPhase` (motor por tick) | ✅ |
| DJT-022 | `ServerTickDispatcher` + `ModScheduler` (tick del servidor + scheduler) | ✅ (hook validado en runtime en NeoForge y Fabric) |
| DJT-023 | `DojutsuLifecycle` (consolida el wiring de listeners) | ✅ (join/quit y tick reubicado validados en runtime en NeoForge y Fabric) |
| DJT-024 | `DeathHandler` + `RespawnHandler` (muerte/respawn) | ✅ (guard y eventos validados en runtime en NeoForge y Fabric) |
| DJT-025 | `DojutsuConfig` + detección de hardcore | ✅ (config y hardcore no destructivo validados en runtime en NeoForge y Fabric) |
| DJT-026 | `AdminCommands` (`reset_ninja` + stubs; cierra EP-CORE) | ✅ (reset_ninja y permiso de op validados en runtime en NeoForge y Fabric) |
| DJT-027 | `SyncNinjaDataPayload` (full+delta) + `DirtyTracker` | ✅ (round-trip S2C full/delta validado en runtime en NeoForge y Fabric) |
| DJT-028 | `ClientNinjaData` (cache read-only en cliente) — **cierra F1** | ✅ (cache en cliente validada en runtime en NeoForge y Fabric) |

**Fase 1 completa: los 19 tickets (DJT-010…028) ✅, validados en runtime. Ver `handoff_phase1.md`.**

## 3.2. Historias y subtareas (F2 — en curso)

| Ticket | Historia | Estado |
|--------|----------|--------|
| DJT-030 | `ChakraState` + `ChakraService` (consume/restore/has, clamp, observable) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-031 | `RegenService` (fórmula/tabla `[S02]`, 1º del pipeline) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-032a | Reposición por **dormir** → chakra (evento nativo, sin mixin) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-032b | Reposición por **comer** → chakra (evento NeoForge + **primer mixin de Fabric**) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-033 | Penalización de muerte = ½ chakra (wiring `DeathHandler`) | ✅ (validado en runtime; **cierra EP-CHAKRA**) |
| DJT-034 | Auditoría economía + `ProgressionState` | ✅ (build+tests; abre EP-PROGRESSION; sin comportamiento runtime aún) |
| DJT-035 | `SkillPointService` (kills → puntos; gasto → sube skill) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-036 | `LevelingService` (auto-nivelación por XP, reactivo) + `XpApi` | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-037 | `RankService` (ninjaXp → rango, reactivo) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-038 | `UnlockService` + `UnlockRegistry` (desbloqueos por hito, reactivo, data-driven) | ✅ (validado en runtime; cierra EP-PROGRESSION) |
| DJT-039 | Keybind Open Menu + `MenuActionPayload` (C2S) | ✅ (validado en runtime; abre EP-CLIENT-BASE) |
| DJT-040 | `NinjaMenuScreen` (GUI real: estado + gasto de puntos) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-041 | HUD de chakra y rango (overlays) | ✅ (validado en runtime; cierra EP-CLIENT-BASE) |
| DJT-042 | Handler servidor de `MenuActionPayload` (gasto validado) + sync de progresión | ✅ (validado en runtime **end-to-end** con DJT-040) |

## 3.3. Historias y subtareas (F3 — en curso)

| Ticket | Historia | Estado |
|--------|----------|--------|
| DJT-050 | `NatureType` + `KekkeiGenkai` + `NatureProfile` (sub-estado, afinidades como `LeveledStat`) | ✅ (build+tests; abre EP-NATURES; sin comportamiento runtime aún) |
| **DJT-063** | **`LeveledStat` inmutable** (deuda de F1; refactor sin cambio de comportamiento ni de NBT) | ✅ (build+tests) |
| DJT-051 | `NatureService` (grant/revoke/has) + corrección del bug Yang `[S03-R1]` | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-052 | Kekkei Genkai data-driven (18 JSON, componentes, bonus de afinidad) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-053 | Naturalezas/KG **prestados** (`TemporaryGrants`, por fuente; corrige S03-R3) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-054 | `NatureRoll` (RNG con los pesos de las loot tables, data-driven + validado) | ✅ (validado en runtime: distribución en juego + rechazo cubierto por tests del listener) |
| DJT-055 | `LearnerService` + `LearnerItem`/`LearnerComponent` (cierra EP-NATURES; 1er data component) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-056 | `ClanDefinition` (JSON, 14) + `ClanRegistry` + `ClanMembership` (abre EP-CLANS) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-057 | `ClanService.join` (bonus una vez) + Namikaze determinista + regen de clan enchufada | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-058 | `ClanService.reset` (defaults atómicos de los 4 sub-estados; corrige S04-R2) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-061 | Jutsu de clan (desbloqueo progresivo, UNA vez por tick; corrige la doble ejecución) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-062 | Team / nametag por clan (namespaceado, sin pisar teams del servidor; **cierra EP-CLANS**) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-059 | `OnboardingController` + `OnboardingChoicePayload` (máquina de estados del servidor; abre EP-ONBOARDING) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-060 | `NinjaCreationScreen` + `OnboardingOptionsPayload` + clan aleatorio (**cierra EP-ONBOARDING y F3**) | ✅ (validado en runtime en NeoForge y Fabric) |

## 3.4. Historias y subtareas (F4 — en curso)

| Ticket | Historia | Estado |
|--------|----------|--------|
| DJT-070 | `NinjaProjectile` base (física + colisión por raycast + dueño UUID; abre EP-ENTITY) | ✅ (validado en runtime en NeoForge y Fabric — **RM-4 desriesgado**) |
| DJT-071 | `AbilityAnchor` + `MarkerEntity` (ancla con tick propio y expiración garantizada + marcador) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-072 | Consolidación `ModEntities` + retirada de `DebugMarkerEntity` (**cierra EP-ENTITY**) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-073 | `JutsuRegistry` + carga data-driven de jutsus (4 pilotos fuego/rayo; abre EP-JUTSU-ENGINE) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-075 | `JutsuKnowledge` + aprendizaje secuencial con learner + `JutsuItem` (cierra el TODO de DJT-055) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-074 | `JutsuCastPipeline` (validar→consumir→cd→efecto→evento; caster genérico; puerta MISSING_NATURE) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-076 | Las 6 familias de efecto con piloto real cada una (+5 jutsus nuevos; `primed_strike` pendiente) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-077 | Cooldowns por-instancia (tick absoluto de mundo, sin barrido por tick; corrige S06-R3) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-078 | `JutsuCastEvent` → XP de afinidad (+1/cast; cierra el bucle usar→XP→nivel; puerta `NatureService`) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-079 | `LastCastRegistry` (último cast observable, acotado; base de la copia Sharingan de F5) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-080 | Keybind CastJutsu (V) + `CastJutsuPayload` C2S validado (lee el ítem real; +cast por click derecho) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-081 | HUD de cooldown del jutsu equipado (sync del mapa de cd; restante con el tick del cliente) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-082 | Armas: `WeaponComponent` + `ItemAttributeModifiers` reversibles (bonus por kenjutsu; abre EP-KENJUTSU) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-083 | Técnicas de espada como jutsus kenjutsu (`SkillRequirement`; 5 JSON; cierra el `medical≥5` de healing) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-084 | Samehada: absorción de chakra al golpear (hook `LIVING_HURT`; **cierra EP-KENJUTSU y el motor de F4**) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-085 | Volcado Fuego + Rayo (24 jutsus totales; RA-1 validado a escala; abre EP-JUTSU-CONTENT) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-085b | Assets de jutsus (24) y armas (3): texturas/modelos vía `custom_model_data` vanilla | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-085c | Texturas del Skill Learner (24 elementos) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-085d | Familia `primed_strike` (golpe cargado del rayo: chidori/lightning_bolt/lightning_cutter/drilling) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-086 | Volcado Agua (7 jutsus + secuencia learner + texturas; `water_cloak`→F6) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-087 | Volcado Tierra (6 jutsus + earth_wall + secuencia + texturas; `earth_golem`→structure) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-088 | Volcado Viento (9 jutsus + vacuum_wave; rasengan→primed_strike) — **5 naturalezas básicas completas** | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-089 | Volcado Yin/Yang (4 jutsus Sexto Camino; aff_yinyang derivada capada a 500; creation_of_all_things=heal; split→F7) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-090 | Volcado Taijutsu (4 jutsus de skill puro; hitos progression.json; skill /4; Ocho Puertas→F6) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-091 | Contrato de jutsu para KG (afinidad múltiple + gate posesión `check_kekkei_genkai`; codec objeto-o-array) — infraestructura | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-092 | Volcado Madera/Mokuton (8 jutsus, primer KG; gate wood + earth&water; XP multi-naturaleza) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-093 | Volcado Hielo (5 jutsus, KG water+wind; ice_golem→structure) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-094 | Volcado Lava (3 jutsus, KG earth+fire; snake_bullet→projectile) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-095 | Volcado Cristal (4 jutsus, KG earth+water+yang, primer KG de 3 afinidades) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-096 | Volcado Tormenta/Storm (3 jutsus, KG lightning+water) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-097 | Volcado Ebullición/Boil (4 jutsus, KG water+fire) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-098 | Volcado Magnetismo/Magnet (6 jutsus arena, KG wind+earth) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-099 | Volcado Explosión/Explosion (7 jutsus, KG lightning+earth; C de Deidara) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-100 | Volcado Abrasación/Scorch (3 jutsus, KG fire+wind) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-101 | Volcado Acero/Steel (4 jutsus, KG fire+earth+water, 2º KG de 3 afinidades) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-102 | Volcado Oscuridad/Dark (2 jutsus, KG wind+yin) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-103 | Volcado Rapidez/Swift (2 jutsus, KG wind+lightning) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-104 | Volcado Tifón/Typhoon (2 jutsus, KG wind+water) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-105 | Volcado Lodo/Mud (3 jutsus, KG earth+water) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-106 | Volcado Ígnea/Blaze (2 jutsus, KG solo posesión sin afinidad) | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-107 | Volcado Polvo/Dust (1 jutsu, último KG, 3 afinidades a 500) — **16 KG completos** | ✅ (build+tests; **pendiente validación runtime en ambos loaders**) |
| DJT-108 | Volcado Genjutsu (6 jutsus, skill genjutsu + yin; area debuffs) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-109 | Volcado Medical (4 jutsus, skill medical, familia heal) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-110 | Volcado Fuinjutsu (3 jutsus, skill fuinjutsu + yang) | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-111 | Volcado Aburame (2 jutsus insecto, sin gate) — **cierra el volcado de contenido F4** | ✅ (validado en runtime en NeoForge y Fabric) |
| DJT-112 | **Cierre formal de F4**: repaso DoD + limpieza (docs obsoletos; ProjectileDebugCommand eliminado; 10 comandos debug conservados) — **F4 CERRADA** | ✅ (build+tests; **carga validada en runtime**, ver §3.4a) |

## 3.4a. Validación runtime de la deuda de F4 (2026-07-19)

Antes de abrir la Fase 5 se saldó la deuda de los ~20 tickets de contenido de F4 marcados "build+tests;
pendiente validación runtime en ambos loaders" (DJT-085c/d, 090, 091…107, 108…110, 112). Resultado:

- **`./gradlew build` verde** y **`:common:test --rerun-tasks` = 541 tests, 0 fallos, 0 errores** (la doc antigua
  decía 403; los tests crecieron con el contenido de F4).
- **183 JSON de contenido bien formados** (128 jutsus + 22 secuencias learner + 18 KG + 14 clanes + unlocks).
- **Arranque del servidor dedicado en NeoForge (21.1.235) y Fabric (0.16.10)**, ambos "Done" sin fallo. Los
  reload listeners cargaron —en **ambos** loaders, idéntico— bajo el **codec estricto**:
  **Jutsus 128 · Kekkei Genkai 18/18 · Secuencias 22 · Clanes 14 · Desbloqueos 22 hitos · NatureRoll 17 tramos**;
  familias de efecto registradas: `projectile, area, buff, teleport, structure, heal, primed_strike`.
  **0 líneas ERROR** atribuibles al mod (los `No data fixer registered for ninja_projectile/ability_anchor` son
  ruido vanilla benigno de toda entidad custom; el WARN de `log4j.xml` es del loom-cache de dev).

**Qué queda como residual (no bloqueante para F5):** spot-checks *in-game* que exigen un jugador en el mundo —
castear una técnica de cada familia y observar el efecto (incl. la descarga de `primed_strike`), y la apariencia
de texturas/modelos del learner (DJT-085c) y jutsus/armas. Esto **no** se puede conducir headless; queda
documentado como checklist manual en `docs/known_issues.md`. La dimensión de **riesgo real** del volcado masivo
(¿parsea y carga el contenido bajo el codec estricto en ambos loaders?) **está validada**.

## 3.5. Historias y subtareas (F5 — Poderes oculares, EN CURSO)

| Ticket | Historia | Estado |
|--------|----------|--------|
| DJT-113 | `EyeState` + `EyeDefinition` (grafo de evolución de ojos data-driven; `#riesgo-alto` RA-3) | ✅ (validado en runtime en NeoForge y Fabric — 7 ojos cargan sin error) |
| DJT-114 | `AwakeningService` (despertar por clan; `EyeAwakenedEvent` + bonus max_chakra/XP) | ✅ (build+8 tests; cableado validado en runtime en ambos loaders; end-to-end time-gated = check manual) |
| DJT-115 | `EvolutionService` (evolución por uso: acumula uso + transición por grafo; `EyeEvolvedEvent`) | ✅ (build+8 tests; cableado validado en runtime en ambos loaders; end-to-end time-gated = check manual) |
| DJT-116 | `EyeStrainService` (fatiga: regen por pulso; 95/98/100 weakness/blindness/apaga; reset al dormir) | ✅ (build+13 tests; cableado validado en runtime en ambos loaders; efectos esperan emisor de strain DJT-118+) |
| DJT-117 | Menú de habilidades oculares (`AbilitySelectScreen` + payloads C2S/S2C validados) | ✅ (build+6 tests; servidor/payloads validados en runtime en ambos loaders; GUI in-game = check manual) |
| DJT-118 | Interfaz `EyeAbility` + piloto **Amaterasu** (`AbilityAnchor`) + disparo (tecla R) — abre EP-EYE-ABILITIES | ✅ (build+6 tests; servidor/payload validados en runtime en ambos loaders; cast in-game = check manual) |
| DJT-119+ | Resto de habilidades clonando la plantilla — **catálogo completo: 21 habilidades** | ✅ (build+21 tests; servidor validado en runtime en ambos loaders; cast in-game = check manual) |
| DJT-120 | **Susanoo**: `SusanooService` (`ToggleableMode`) + `SusanooCostTable` niveles 1..6 + `SusanooState` | ✅ (build+26 tests; servidor validado en runtime en ambos loaders; ciclo in-game = check manual) |
| DJT-121 | Susanoo: **avatar** (`AbilityAnchor` parenteado + silueta por etapa; despawn) + **escalera de etapas** | ✅ (build+5 tests; servidor validado en runtime en ambos loaders; avatar in-game = check manual) |
| DJT-125 | **Chakra Vision**: `ChakraVisionService` + toggle (tecla C / `ToggleVisionPayload`) + glow nativo por relación | ✅ (build+10 tests; servidor validado en runtime en ambos loaders; glow in-game = check manual) |

**DJT-125 (hecho — cierra la lógica de F5):** la vision resalta las entidades cercanas (radio 30) con el **glow
nativo de vanilla** coloreado por relación (special/friendly/neutral/hostile), **sin renderer propio ni selector
por tick** (RB-3): un `PlayerTickSystem` reescanea cada 10 ticks y marca las entidades con el efecto `GLOWING` +
un **equipo de scoreboard** cuyo color pinta el contorno; el cliente lo dibuja solo. La clasificación es **dato
estático** (`ChakraVisionRelations`, de los 4 tags `chakra_highlights/*`). Disponibilidad **data-driven**
(`EyeDefinition.chakraVision` + `chakraVisionUseLevel`): byakugan/mangekyo/rinnegan/tenseigan siempre; **Sharingan
solo maduro** (3 tomoe, uso ≥216000 verificado); Ketsuryugan no. **Activa por defecto** con un ojo que la da; la
**tecla C** la silencia (`VisionState.chakraVisionOff`, persistente, fiel a `d.no_cv`).

**DJT-121 (hecho):** el avatar es un **`AbilityAnchor`** (F4/DJT-071), no un tipo de entidad nuevo — su contrato
ya contemplaba el ancla "mientras el modo esté activo". **Corrige RA-3**: el datapack usaba un `armor_stand`
`d.narr` con `knowledge_book{CustomModelData}` que quedaba colgado; aquí la caducidad es por construcción y se
descarta al apagar, al caer por chakra agotado y al desconectar. **Escalera de etapas** (`advance`): cada
pulsación crece una etapa con tope en el nivel dominado; en el tope, apaga (divergencia consciente: con una sola
tecla, una escalera sin salida no tendría salida). Etapa = tamaño de la silueta; nivel = coste y protección.
**Pendiente aparte:** los 7 modelos 3D por personaje (`susanoo_act`) son **trabajo de assets**; hasta entonces
la silueta se dibuja con partículas, como el resto de entidades del mod (ninguna tiene renderer todavía).

**DJT-120 (hecho):** el Susanoo es el **primer consumidor** del framework `ToggleableMode` (patrón 2 del ADD),
que existía sin clientes desde F1. `SusanooCostTable` (dominio puro, verificada): niveles 1..6
(Ribcage→Perfect), **coste decreciente** con la maestría (6 chakra/20t → 1 chakra/80t), fatiga 3→1,
`resistance`+`fire_resistance` amp `nivel-1`, `slowness` solo en 3 y 4. El **EMS** paga tarifa plana: 0 chakra,
+1 fatiga cada 160t. `SusanooMode` cobra **por pulsos** y cae solo si el chakra no llega (el único automatismo
que promete el framework). Se enciende desde el **menú ocular + tecla R** (`SusanooAbility`) — sin tecla nueva.
`SusanooState` (nivel+uso) es sub-estado propio de `PlayerNinjaData`; el encendido es de ejecución, no se guarda.

**DJT-119+ (catálogo COMPLETO, 21 habilidades):** los dos modos toggle (`susanoo` DJT-120, `tenseigan_chakra_mode`
—fleco F5: buffs+vuelo, pulso 10/200t, cae si chakra<19—) cierran el catálogo declarado de los 7 ojos. Lote 6:
`GenjutsuStun`[Sharingan **0 chakra**/0 strain/**cd 200t**, stun duro slowness 255],
`CopyJutsu`[Sharingan 0/0, **aprende el jutsu visto** vía `LastCastRegistry`], `KinrinTenseiBaku`[Tenseigan 400/0,
proyectil canalizado con auto-raíz]. **Añade `EyeAbility.cooldownTicks()`** (default 0) + puerta de cooldown por
(jugador, habilidad) en el pipeline: el datapack gatea el `genjutsu_stun` **solo por cooldown**, sin cobrar chakra.

**DJT-119+ (lotes 1-5 hechos):** 15 habilidades más clonando la plantilla de Amaterasu (**16 hasta el lote 5**; **lote 5**
—nuevos tipos: `AsuraPath`[Rinnegan 450, **misil** `NinjaProjectile` + resistencia propia], `PretaPath`[Rinnegan 150,
**defensivo**: absorción+resistencia], `TruthSeekingOrbs`[Tenseigan 300, HARM III en el punto de mira — 1ª del Tenseigan]).
**Lote 4**: `ExplodingHuman`[Ketsuryugan 200/+20, HARM III], `HundredTwentyEightPalms`[Byakugan 80, HARM III AoE], `ChibakuTensei`[Rinnegan 600, gravedad vertical: levita+daña]). **Lote 1**
(velocidad): `VacuumPalm`(Byakugan 40), `ShinraTensei`(Rinnegan 600), `BanshoTenin`(Rinnegan 360) — empuje/atracción.
**Lote 2** (Ketsuryugan, efectos de estado): `BloodEruption`(120/+8), `IronBloodFist`(80/+4, auto-buff), `RisingBloodSpear`(100/+14).
**Lote 3** (nuevos tipos): `Tsukuyomi`(Mangekyō 100/+30, genjutsu multi-debuff), `Amenotejikara`(Rinnegan 600, teleport/swap),
`SixtyFourPalms`(Byakugan 60, golpe melé AoE). Helper `EyeAbilityEffects`. `MobEffects.HARM` invierte el signo para
no-muertos (fiel sin ramas). Costes/strain verificados. El resto del roster continúa por lote.

**DJT-118 (hecho):** abre `EP-EYE-ABILITIES`. Contrato `EyeAbility` (`chakraCost`/`strainCost`/`execute`) +
`EyeAbilityRegistry` (en código) + `EyeAbilityService` (pipeline: valida ojo/`selectedAbility`/chakra → consume
chakra+fatiga → ejecuta → sync) + piloto **`AmaterasuAbility`** (40 chakra/+10 strain verificados; exento con ojo
eterno; raycast → `AbilityAnchor` de fuego negro que incendia/daña, **no armor stand**). Disparo: keybind **R** →
`UseEyeAbilityPayload` C2S → el servidor casta la habilidad seleccionada. `resolveCast` puro (6 tests). Establece
la plantilla del resto del catálogo.

**DJT-117 (hecho):** menú de habilidades oculares (primer cliente/GUI de F5). Keybind **N** → request C2S
(`OpenEyeMenuPayload`) → el servidor construye la vista desde `EyeState`+`EyeRegistry` → `EyeMenuPayload` S2C →
`AbilitySelectScreen` (la lista viaja por red, el `EyeRegistry` es server-side). Selección por
`SelectEyeAbilityPayload` **validada en servidor** (`EyeMenuService.select`: la habilidad debe pertenecer al ojo
activo real), guarda `EyeState.selectedAbility`, refresca la vista. Reemplaza el hotbar-as-selector. Nuevos:
`EyeMenuService` (puro, 6 tests), 3 payloads, `EyeMenuHandler`, `ClientEyeMenu`, `AbilitySelectScreen`. Servidor/
payloads validados en runtime; GUI in-game = check manual.

**DJT-116 (hecho):** `EyeStrainService` — `PlayerTickSystem` en `TickPhase.STRAIN`. **Regen** por pulso de 200
ticks (−1, o −3 con ADN de Hashirama; solo si hay fatiga). **Efectos por umbral**: 95→weakness (todos los ojos),
98→blindness, 100→apaga el ojo (`withoutActiveEye`); el **Rinne Sharingan** exento de ceguera/apagado. La subida
es `addStrain` (puerta única, la llamarán las habilidades de DJT-118+; sin emisor aún). **Reset al dormir** vía
`SleepChakraHandler` (registro de handlers de despertar, patrón proveedor). Se generalizó `EyeState.awakenTimers`→
`timers` (mapa de temporizadores por propósito). `regen`/`effectsFor` puros: 13 tests. Cableado validado en runtime.

**DJT-115 (hecho):** `EvolutionService` — `PlayerTickSystem` en `TickPhase.DOJUTSU` con dos partes: **acumula el
uso** de la familia del ojo activo (+1/tick, solo si el uso alimenta alguna evolución; los terminales no) y
**transiciona por el grafo** en la primera arista satisfecha (uso/clan/flag), aplicando el bonus del destino
(Mangekyō +20/+80, Rinnegan +100/+150) y emitiendo `EyeEvolvedEvent`. Rutas divergentes por `CLAN` compuesto
(`otsutsuki_uchiha`). El **EMS/eternal** es acción manual (implementado en `EternalAwakeningService`, fleco F5);
los tomoe son nivel derivado. El `EvolutionService` acumula uso también para ojos con uso-gate eterno (Mangekyō).
8 tests puros; cableado validado en runtime; end-to-end time-gated (check manual). **Reportado**: hueco de
DJT-114 (uso inicial del sharingan al despertar) — ver `known_issues.md`.

**DJT-114 (hecho):** `AwakeningService` — `PlayerTickSystem` en `TickPhase.DOJUTSU` que despierta el dojutsu del
clan leyendo `clan.potentialDojutsu` (no predicates `is_*`): acumula el temporizador de despertar y, al alcanzar
`awakenTicks` (byakugan/sharingan 78000, ketsuryugan 24000), despierta un ojo potencial (50/50 en Otsutsuki),
aplica el bonus de max_chakra/XP de la `EyeDefinition` y emite `EyeAwakenedEvent`. Una vez por clan. Se añadió
`awakenTicks` a `EyeDefinition`. Lógica pura en 8 tests; cableado validado en runtime en ambos loaders (el
end-to-end es time-gated, ~65 min → check manual en `known_issues.md`).

**DJT-113 (hecho):** abre `EP-EYE-CORE`. Modelo de datos + carga del grafo (los servicios de despertar/evolución/
strain/menú son DJT-114…117). Nuevo paquete `domain.eye` (`EyeFamily`, `EyeRequirement`, `EyeEvolution` — dominio
puro) + módulo `dojutsu` (`EyeDefinition`+`EyeRegistry`+`EyeReloadListener`, mismo par que clanes) + sub-estado
`data.state.EyeState` (enchufado en `PlayerNinjaData` como `eye`). 7 JSON representativos en `data/dojutsu/eyes/`
(byakugan, sharingan, mangekyo_itachi, rinnegan, tenseigan, ketsuryugan, naka=placeholder). Colapso 33→lógicos
por flag `eternal` en el estado; rutas divergentes como `requirements` data-driven (regla 16, valores auditados
del datapack). **18 tests nuevos** (codec round-trip, tolerancia, aislamiento por archivo, grafo). Sin servicios
todavía: es estado + contenido, como `ProgressionState` (DJT-034) o `NatureProfile` (DJT-050) al abrir su épica.

## 4. Qué funciona hoy

**Vertical slice completo (F2), validado en runtime en NeoForge y Fabric:**

- **Chakra vivo** `[S02]`: regenera por tick con la tabla exacta del datapack (base 120 ticks/+3; factores de
  clan/ADN/sabio listos pero inactivos hasta F3/F6), corre **primero** en el pipeline; comer repone +50; dormir
  la noche completa lo llena; morir deja ½ del máximo. Única puerta de escritura (`ChakraService`).
- **Progresión** `[S13]`/`[S20]`: matar da skill points (config 1/3/200); gastarlos sube habilidades (+ninja_xp);
  la XP auto-nivela **multi-nivel de una** (cap 500) de forma **reactiva**; el rango deriva de `ninjaXp` y avisa
  al cruzar umbral; los hitos **desbloquean** (data-driven, idempotente).
- **Cliente**: tecla **M** (remapeable) abre el **menú GUI real** (chakra, rango/ninjaXp, puntos, 7 habilidades
  con "+"); el gasto va por payload **validado en servidor**; **HUD** permanente de chakra (barra en tiempo real)
  y rango, oculto en espectador y con F1.
- **Persistencia y sync**: todo el estado persiste (attachment versionado/migrable) y lo observable se
  sincroniza (full en join/respawn/dimensión; delta en cada cambio).

**Base (F0/F1):** el mod carga en ambos loaders (cliente y servidor dedicado); los tres puertos de plataforma
(`RegistrarService`, `AttachmentService`, `NetworkService`) operativos; dominio puro testeable; motor (tick
pipeline, scheduler, ciclo de vida, config, muerte/respawn); reload listener de datos; CI en verde.

## 5. Qué NO existe todavía

- **F3 está código-completo**: la creación del ninja funciona entera in-game (Screen + payloads + máquina de
  estados del servidor, clan/afinidad aleatorios incluidos). El learner valida posesión al usarse; **enseñar
  jutsus es F4/DJT-075**.
- **Módulos de contenido pendientes:** `jutsu`, `throwing` (F4, en curso — `entity` ya tiene el
  `NinjaProjectile` de DJT-070; faltan `AbilityAnchor`/`MarkerEntity` y todo el motor de casteo); `dojutsu`,
  `sage`, `empowerment`, `bijuu`, `items`, `summoning`, `ai`, `trading`, `story`, `world` (F5–F9); `api`.
- **Puerto `AttributeService`** (adjuntar/modificar atributos vanilla en `Player`): **diferido**; F2 no lo
  necesitó (`maxChakra` es un campo de `ChakraState`). Se retomará cuando haga falta un modificador de atributo.
- **Contenido real de los desbloqueos:** `UnlockService` registra el id, pero **no entrega** el ítem de jutsu
  (F4). Los umbrales de `data/dojutsu/unlocks/progression.json` son **provisionales**.
- **`XpApi.addAffinityXp`**: **F4/DJT-078**, no F3 (corrige lo que suponía el handoff de F2). El estado ya existe
  (`NatureProfile`, DJT-050) y comparte curva/escala con las habilidades, pero en el datapack la XP de afinidad
  se gana **usando jutsus** (`jutsu/training/*`) → sin emisor hasta F4, sería API muerta.
- **Sistema de ojos (dōjutsu, F5):** grafo, estado, despertar, evolución, fatiga (con **avisos de chat**), menú,
  **21 habilidades** con su pipeline de cast (chakra + fatiga + **cooldown** + refund "sin objetivo"), el
  **Susanoo completo** —modo, avatar y escalera—, la **chakra vision** y el **EMS/eternal manual**
  (DJT-113…125 + flecos) ya existen: **la lógica de F5 está completa y los flecos cerrados**. **Falta solo**:
  los **modelos 3D** del Susanoo por personaje (assets).
- **Assets**: **texturas/modelos portados para los 24 ítems de jutsu, las 3 armas y el learner (24 elementos)**
  (F4/DJT-085b/c, del resource pack del datapack vía `custom_model_data` vanilla). Pendientes: ojos, máscaras,
  bijuu, sonidos; el HUD sigue con primitivas. Se portan por fase.
- **Datagen** y contenido data-driven de jutsus/clanes/ojos.

## 6. Estado del código

- **299 clases Java**: **217** en `common` (+ **66** de test), **10** en `fabric`, **8** en `neoforge`.
- **Paquetes bajo `com.zamu.dojutsu`:**
  - `domain.*` (puro, sin MC): `resource`, `strain`, `mode`, `jutsu` (+`AffinityRequirement`, **`SkillRequirement`**
    F4/DJT-083), `progression`, `rng`, `event`, **`nature`** (`NatureType`, `KekkeiGenkai` — F3/DJT-050),
    **`eye`** (`EyeFamily`, `EyeRequirement`, `EyeEvolution` — grafo de ojos, F5/DJT-113).
  - `data.*` (Codec/NBT): `PlayerNinjaData`, `NinjaData`, `DataVersion`, `DirtyTracker`, `ObservableField`,
    `state/{OnboardingState, ChakraState, ProgressionState, Skill, NatureProfile, LeveledStats,
    JutsuKnowledge, EyeState}` (JutsuKnowledge: known + progreso del learner + **cooldowns por-instancia**, DJT-077;
    **EyeState**: ojo activo + `eternal` + `useLevels` por familia + `awakened` + strain + selección — F5/DJT-113),
    `migration/*`.
  - `core.*` (motor): tick pipeline/dispatcher, `scheduler`, `lifecycle`, `config`, `command`, `identity`,
    `DojutsuLifecycle`, `DataReloadListener`, **`DomainEvents`** (bus compartido),
    **`onboarding/OnboardingController`** (máquina de estados de la creación del ninja — F3/DJT-059).
  - `chakra` (F2): `ChakraService`, `RegenService`, `SleepChakraHandler`, `FoodChakraHandler`.
  - `natures` (F3/F4): `NatureService` (única puerta del perfil elemental; +`addAffinityXp` en F4/DJT-078), `KekkeiGenkai{Service,Registry,Definition,ReloadListener}` (18 KG data-driven), `TemporaryGrantService` (préstamos), `NatureRoll{,Table,Registry,ReloadListener}` (RNG canónico), `LearnerService` (entrega/uso/retirada del Skill Learner), `EnumCodecs`.
  - `items` (F3/F4): `LearnerItem` + `LearnerComponent` (primer data component; registro `ModDataComponents`)
    + **`JutsuItem` + `JutsuComponent`** (F4/DJT-075: la técnica como ítem con componente tipado; el cast por
    keybind es DJT-080) + **`WeaponItem` + `WeaponComponent`** (F4/DJT-082: armas con daño reversible por
    `ItemAttributeModifiers`; chakra_blade, kubikiribocho, **Samehada** —absorbe chakra al golpear, DJT-084).
  - `clans` (F3): `ClanDefinition` + `ClanRegistry` + `ClanReloadListener` (14 clanes data-driven), `ClanService` (join con bonus una vez + reset atómico; proveedor del regenFactor del clan), `ClanJutsuUnlockService` (desbloqueo progresivo por tick), `ClanTeamService` (team/nametag).
  - `progression` (F2): `SkillPointService`, `XpApi`, `LevelingService`, `RankService`, `UnlockService`,
    `UnlockRegistry`, `UnlockDefinition`, `UnlockReloadListener`.
  - `network`: `SyncNinjaDataPayload`, `ProgressionSnapshot`, `NinjaSync`, `MenuActionPayload`,
    `OnboardingChoicePayload` (C2S), `OnboardingOptionsPayload`+`OnboardingSync` (S2C, opciones de clan),
    **`CastJutsuPayload`** (C2S, tecla de cast — F4/DJT-080), **`OpenEyeMenuPayload`**/**`SelectEyeAbilityPayload`**
    (C2S) + **`EyeMenuPayload`** (S2C, vista del menú ocular — F5/DJT-117), **`UseEyeAbilityPayload`** (C2S, lanzar
    la habilidad ocular — F5/DJT-118),
    `handler/{MenuActionHandler, OnboardingChoiceHandler, CastJutsuHandler, EyeMenuHandler}`.
  - `client` (solo cliente): `ClientNinjaData` (+cooldowns observables, F4/DJT-081), `ClientOnboardingOptions`,
    **`ClientEyeMenu`** (vista del menú ocular — F5/DJT-117), `keybind/KeyBindings` (+`CAST_JUTSU` F4/DJT-080,
    +`OPEN_EYE_MENU` [N] F5/DJT-117, +`USE_EYE_ABILITY` [R] F5/DJT-118),
    `screen/{NinjaMenuScreen, NinjaCreationScreen, NinjaCreationOpener, AbilitySelectScreen (F5/DJT-117)}`,
    `hud/{HudOverlays, ChakraHudOverlay, RankHudOverlay, AbilityBarOverlay (cd del jutsu equipado, F4/DJT-081), HudFormat}`.
  - `entity` (F4, **EP-ENTITY completa**): **`NinjaProjectile`** (proyectil reutilizable: física sin fricción,
    raycast de segmento, dueño por UUID, callback, no persistente — DJT-070) + **`AbilityAnchor`** (ancla de
    habilidad con tick propio y expiración garantizada) + **`MarkerEntity`** (marcador transitorio, server-only
    — DJT-071). El placeholder `DebugMarkerEntity` de F0 se retiró en DJT-072.
  - `util` (F4): `ProjectileMath` (física pura, testeable sin MC) + `RaycastHelper` (costura de colisión
    sobre `ProjectileUtil`).
  - `jutsu` (F4): **`JutsuEntry`** (codec estricto sobre el `JutsuDefinition` de dominio + `effectParams`
    crudo) + **`JutsuRegistry`** + **`JutsuReloadListener`** (`data/dojutsu/jutsu/**`, subcarpetas por
    naturaleza, id==archivo, aislamiento por archivo) — DJT-073 · **`LearnSequence{,Registry,ReloadListener}`**
    (secuencias de aprendizaje por elemento) + **`JutsuLearningService`** (aprendizaje secuencial: posesión →
    cd 14t → siguiente de la secuencia; el último consume el learner; decisión pura testeable) — DJT-075 ·
    **`JutsuCastPipeline`** (validar chakra→cd→**posesión**→afinidad → `canApply`/NO_TARGET → consumir
    todo-o-nada → cooldown → efecto aislado → `JutsuCastEvent`; caster genérico vía `CastContext`+`ChakraSink`;
    glue con los mensajes del datapack) + **`PlayerCastContext`** — DJT-074 · **`effect/*`**: las **6 familias
    del ADD** (`Projectile` con aura de vuelo sobre `NinjaProjectile`, `Area`, `Buff`, `Teleport`, `Structure`,
    `Heal` + `MobEffectSpec` compartido), parametrizadas desde `effectParams` con un jutsu real por familia;
    familia **`primed_strike`** (golpe cargado del rayo: arma al castear, descarga al golpear — `PrimedStrikeEffect`
    + `PrimedStrikeManager` con hook `LIVING_HURT`; DJT-085d) ·
    **`JutsuXpHandler`** (suscriptor de `JutsuCastEvent` → +1 XP a la afinidad del jutsu; universales excluidos;
    la puerta es `NatureService.addAffinityXp`, no `XpApi`, para no cerrar el ciclo `progression↔natures`) —
    DJT-078 · **`LastCastRegistry`** (último cast observable por caster, acotado por dimensión/distancia/tiempo;
    base de la copia Sharingan de F5; corrige el estado global S06-R6) — DJT-079.
  - `dojutsu` (F5): **`EyeDefinition`** (codec estricto del grafo de ojos: familia, bonus de despertar,
    `awakenTicks`, habilidades, `evolutions` con `requirements` data-driven, `eternalRequirements`) +
    **`EyeRegistry`** + **`EyeReloadListener`** (`data/dojutsu/eyes/*.json`, id==archivo, aislamiento por
    archivo) — DJT-113 · **`AwakeningService`** (`PlayerTickSystem` en `TickPhase.DOJUTSU`: despierta el dojutsu
    del clan por `potentialDojutsu`, 50/50 Otsutsuki, bonus + `EyeAwakenedEvent`; decisión pura + glue) — DJT-114 ·
    **`EvolutionService`** (`PlayerTickSystem` en `TickPhase.DOJUTSU`: acumula el uso del ojo activo y transiciona
    por el grafo —sharingan→mangekyo/rinnegan, byakugan→tenseigan— con bonus + `EyeEvolvedEvent`) — DJT-115 ·
    **`EyeStrainService`** (`PlayerTickSystem` en `TickPhase.STRAIN`: regen por pulso; efectos 95/98/100
    weakness/blindness/apaga —Rinne Sharingan exento—; `addStrain` puerta de subida; reset al dormir vía
    `SleepChakraHandler`) — DJT-116 · **`EyeMenuService`** (vista del menú + selección validada, puro) — DJT-117 ·
    **`ability/*`** (`EyeAbility`+`EyeAbilityContext`+`EyeAbilityRegistry`+`EyeAbilityService` pipeline de cast +
    piloto `AmaterasuAbility` vía `AbilityAnchor`) — DJT-118 · **`ability/{VacuumPalm, ShinraTensei, BanshoTenin}Ability`**
    + `{BloodEruption, IronBloodFist, RisingBloodSpear, ExplodingHuman}Ability` (Ketsuryugan) + `{Tsukuyomi,
    Amenotejikara, SixtyFourPalms, HundredTwentyEightPalms, ChibakuTensei}Ability` + `{AsuraPath, PretaPath,
    TruthSeekingOrbs}Ability` (proyectil/defensa/Tenseigan) + `{GenjutsuStun, CopyJutsu}Ability` (Sharingan) +
    `KinrinTenseiBakuAbility` (canalizada) + `EyeAbilityEffects`
    (**19 habilidades**; lotes 1-6) — DJT-119 · **`susanoo/SusanooService`** (glue del `SusanooMode`:
    tick en `TickPhase.DOJUTSU`, contexto sobre `ServerPlayer`, maestría por uso) + `SusanooAbility` +
    `susanoo/SusanooAvatar` (ancla parenteada, silueta por etapa) — DJT-120/121 ·
    **`tenseigan/TenseiganChakraModeService`** + `TenseiganChakraModeAbility` (2º `ToggleableMode`: buffs+vuelo,
    pulso 10/200t; la 21ª, cierra el catálogo — fleco F5) · **`vision/ChakraVisionService`** (glow nativo por
    relación, clasificación estática `ChakraVisionRelations`, tecla C → `ToggleVisionPayload`; `VisionState`
    sub-estado) — DJT-125.
  - `platform` (puertos), `registry` (`Mod*`). Adaptadores: `fabric.*` (incl. **`mixin/LivingEntityMixin`** y
    `DojutsuFabricServerEvents`), `neoforge.*` (incl. `DojutsuNeoForgeServerEvents`).
- **Recursos:** `assets/dojutsu/lang/en_us.json`; `data/dojutsu/balance/xp_sources.json` (auditoría RA-4, de
  referencia); `data/dojutsu/unlocks/progression.json` (hitos provisionales); `data/dojutsu/jutsu/**` (**50
  jutsus** con valores auditados: **las 5 naturalezas básicas completas** —fuego 7 + rayo 8 + agua 7 + tierra 7 +
  viento 10— (DJT-085…088) + **yin_yang 4** (Sexto Camino, DJT-089) + **taijutsu 4** (skill puro, DJT-090) +
  **madera 8** (Mokuton, DJT-092) + **hielo 5** (DJT-093) + **lava 3** (DJT-094) + **cristal 4** (DJT-095) + **tormenta 3** (DJT-096) + **ebullición 4** (DJT-097) + **magnetismo 6** (DJT-098) + **explosión 7** (DJT-099) + **abrasación 3** (DJT-100) + **acero 4** (DJT-101) + **oscuridad 2** (DJT-102) + **rapidez 2** (DJT-103) + **tifón 2** (DJT-104) + **lodo 3** (DJT-105) + **ígnea 2** (DJT-106) + **polvo 1** (DJT-107) — **16 KG completos** — + **genjutsu 6** (DJT-108) + **medical 4** (DJT-109) + **fuinjutsu 3** (DJT-110) + **aburame 2** (DJT-111) + universal + 5 técnicas de kenjutsu — **volcado de contenido F4 cerrado**; diferido dōjutsu→F5, modos/Ocho Puertas→F6, Diez Colas→F7, summons→F9);
  `data/dojutsu/learner_sequences/*.json` (22 secuencias: 5 básicas + yin_yang + 16 KG del datapack; el resto de
  elementos llega con sus lotes; **fire/lightning/water** portadas). Jutsus con efecto real (DJT-076): fire_bullet, great_fireball, fireproof,
  vacuum_wave, substitution, earth_wall, healing (9 definiciones en total; lightning_bolt/chidori esperan la
  familia `primed_strike`). **`data/dojutsu/eyes/*.json`** (**14 ojos**: los 7 del grafo base de F5/DJT-113
  —byakugan, sharingan, mangekyo_itachi, rinnegan, tenseigan, ketsuryugan, naka=placeholder— + los **7 Mangekyō
  por personaje** del roster: obito, sasuke, madara, indra, shisui, fugaku, shin, elegidos al azar al despertar
  el Mangekyō vía `mangekyo_rng`; el resto del roster —Isshiki, Jougan, Rinne Sharingan, variantes Rinnegan— sigue
  en tickets posteriores).
- **Andamiaje temporal:** `debug` (`/dojutsuninja`, `/dojutsusched`, `/dojutsuchakra`, `/dojutsuprogression`,
  `/dojutsunatures`, `/dojutsuclans`, `/dojutsuonboarding`, `/dojutsuprojectile`, `/dojutsuanchor`,
  `/dojutsujutsu`, `/dojutsuweapon`, **`/dojutsudojutsu`** [F5: activar ojo/fijar uso/strain/eternal para QA]).
  Real y permanente: `/dojutsu reset_ninja`.
- **Tests unitarios:** JUnit 5 en `common/src/test`; **717 tests verdes** (F4 + DJT-113…125 + flecos + roster; dominio/datos/
  servicios puros, vía `NbtOps` sin bootstrap de MC). `./gradlew build` multiloader verde.
- **Fuente de verdad del diseño** (fuera de este repo): `D:\Projectos claude\Dojutsu reverse\DOCS\`
  → `sistemas/` (ingeniería inversa S01–S25), `arquitectura/` (ADD), `backlog/` (DJT-###).

## 7. Fase actual

> **Nota (2026-08-28):** esta sección quedó congelada en F5 y no se actualizó en F6…F10. La fase realmente en
> curso es **F11 (UX e interfaces)**, con el código terminado y la validación en partida pendiente; ver el
> bloque F11 de la cabecera y `backlog.md §Fase 11`. Lo de abajo se conserva por trazabilidad histórica.

**F5 — Poderes oculares, LÓGICA COMPLETADA** (S05 ojos + S24 Susanoo + S25 Chakra Vision). Ficha de diseño:
`D:\Projectos claude\Dojutsu reverse\DOCS\backlog\06_FASES5-10_RESUMEN_Y_PLANTILLAS.md §FASE 5`; orden, tabla de
tickets (renumerados a **DJT-113+**) y DoD en `docs/backlog.md §Fase 5`. **Hecho: DJT-113** (`EyeState` +
`EyeDefinition` + grafo data-driven; 7 ojos cargan), **DJT-114** (`AwakeningService`, despertar por clan) y
**DJT-115** (`EvolutionService`), **DJT-116** (`EyeStrainService`), **DJT-117** (menú), **DJT-118** (`EyeAbility`
+ Amaterasu), **DJT-119 lotes 1-6** (19 habilidades; lote 6: Genjutsu Stun/Copy Jutsu/Kinrin Tensei Baku, que
cierran el catálogo declarado), **DJT-120** (Susanoo como `ToggleableMode` + tabla de niveles 1..6) y
**DJT-121** (avatar anclado + escalera de etapas) y **DJT-125** (chakra vision: glow nativo por relación, tecla C,
disponibilidad data-driven). **F5 cerrada en lógica**; solo faltan los modelos 3D del Susanoo (assets).

**F4 — Combate: 🏁 CERRADA** (128 jutsus, pipeline de cast único, 7 familias de efecto, contrato extendido con
afinidad múltiple + KG, learner, cooldowns, XP de afinidad, base del Sharingan; validación de carga en runtime en
ambos loaders — §3.4a). Quedó diferido lo que necesita subsistemas posteriores: dōjutsu (F5, en curso),
modos/Ocho Puertas (F6), Diez Colas (F7), summons de entidad (F9).
