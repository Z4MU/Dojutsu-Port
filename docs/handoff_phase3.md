# Handoff — Fase 3 → Fase 4 · Dojutsu Port

> Documento de traspaso. Un desarrollador que abra el proyecto debería poder continuar leyendo solo `docs/`.
> Estado a 2026-07-16. **Fases 0, 1, 2 y 3 completadas y validadas en runtime** en NeoForge y Fabric.

## Resumen ejecutivo

**Dojutsu Port** reconstruye el datapack *Dojutsu* (Naruto/Boruto) como mod moderno de Minecraft (**1.21.1**,
Java **21**, Architectury multiloader). Tras la **Fase 3 (Definición del ninja)** el jugador **crea su ninja
de principio a fin por GUI**: elige clan (o aleatorio), elige afinidad (o aleatoria), recibe los bonus de clan
exactamente una vez, el roll canónico de Kekkei Genkai/Yang/2ª afinidad/genjutsu, el Skill Learner de cada
naturaleza, y su nametag de clan. Todo con autoridad de servidor, data-driven y con la distribución **idéntica**
a la del datapack (RA-4: pesos auditados loot table a loot table).

- **393 tests** unitarios verdes; `./gradlew build` multiloader verde.
- **198 clases Java** (136 common + 44 tests + 10 fabric + 8 neoforge).
- Repo: https://github.com/Z4MU/Dojutsu-Port.git (`main`).

**Demo de aceptación (DoD de F3) — validada en ambos loaders:** un jugador nuevo entra → la **Screen de
creación se abre sola** → elige **clan** (bonus una vez, nametag, `potentialDojutsu` en datos) → elige
**afinidad** (fijada a 50, learner entregado) → el **finish** tira el KG con los pesos del datapack → **ninja
activo** (mensaje + menú M). El caso **Lee** salta la pantalla de afinidad y todos los rolls. Cerrar la
pantalla sin elegir **la reabre**; `reset_ninja`/reset de clan limpian todo y **re-embarcan**.

## Qué quedó implementado (F3)

### EP-NATURES — perfil elemental (`natures`, `[S03]`)
- `domain/nature/{NatureType, KekkeiGenkai}` (DJT-050): enums puros; ninja_xp **por naturaleza** auditada
  (fire/lightning 25; wind/earth/water 20; yin/yang 30 — `[S03]` decía "25 uniforme" y es falso).
- `data/state/NatureProfile` (DJT-050): sub-estado enchufable; afinidades como `LeveledStat`;
  `affinity(YIN_YANG)` **derivada** (`(yin+yang)/2`); flag `hashiramaDna`.
- `natures/NatureService` (DJT-051): **única puerta** del perfil — grant/revoke idempotentes con XP exacta y
  simétrica, `addAffinityLevels` (suma, cap) y `setAffinityLevel` (SET absoluto del onboarding). **Corrige el
  bug Yang `[S03-R1]`** (rama muerta del datapack) por construcción.
- KG **data-driven** (DJT-052): 18 JSON en `data/dojutsu/kekkei_genkai/` (componentes, ninja_xp 8–20 no
  uniforme, bonus de afinidad de ice/blaze); otorgar un KG implica sus naturalezas. Codecs **estrictos**
  (`EnumCodecs`): una ref rota falla el archivo, con aislamiento por archivo.
- `TemporaryGrants` + `TemporaryGrantService` (DJT-053): naturalezas/KG **prestados por fuente**
  (procedencia, **sin temporizador** — la ficha pedía expiración y el datapack no la tiene; decidido con el
  usuario). Retirar la fuente no toca lo propio ni lo de otras fuentes. Consumidor real: F7 (bijuu).
- `NatureRoll` + tabla data-driven (DJT-054): `rng/nature_roll.json` reproduce los pesos **exactos** de las
  loot tables `*_rng` (KG 1..17 de 50, Madera 30..40 solo Senju, 2ª afinidad 1/10, genjutsu 1/7); la tabla se
  **valida al cargar** (tramos solapados/fuera de rango = error). Solo tira; el onboarding aplica.
- `LearnerService` + `LearnerItem`/`LearnerComponent` (DJT-055): entrega **reactiva** (eventos de grant),
  retirada de learners huérfanos al revocar, validación de posesión al usar. **Primer data component** del
  proyecto (`ModDataComponents`). Enseñar jutsus: F4/DJT-075.

### EP-CLANS — clanes (`clans`, `[S04]`)
- `ClanDefinition` (DJT-056): **14 clanes data-driven** (15 componentes de record: natures, KG, bonus,
  maxChakra, ninjaXp, startingJutsu/Items, potentialDojutsu, randomNature, regenFactor, jutsuUnlockTicks,
  teamColor, skipsAffinity). `ClanMembership` = sub-estado enchufable.
- `ClanService.join` (DJT-057): bonus **exactamente una vez** (guard `initialized`); Namikaze **determinista**
  (lightning 25 — corrige S04-R3); azar inyectado (Sarutobi wind 1/2); registra los **factores de regen** por
  proveedor (rompe el ciclo chakra↔clans; pagó la deuda de F2: Otsutsuki/Uzumaki/Hashirama/Jugo activos).
- `ClanService.reset` (DJT-058): defaults **atómicos** de los 4 sub-estados de gameplay (corrige S04-R2:
  el reset del datapack dejaba restos); onboarding se preserva y `refresh` re-embarca.
- `ClanJutsuUnlockService` (DJT-061): desbloqueo progresivo por tick (umbral 78000 en datos, cap sin
  desborde), **una vez por tick** — corrige la **doble ejecución** del datapack (`tick.json` +
  `player_functions` llamaban ambos a `clans/jutsu/main`). Entrega del jutsu: F4.
- `ClanTeamService` (DJT-062): team/nametag por clan — `dojutsu_<clan>` **namespaceado** (sin colisiones),
  color/sufijo en datos, **no pisa teams ajenos**; `sync` en join/reset/login.

### EP-ONBOARDING — creación del ninja (`core.onboarding`, `client.screen`, `[S01]`)
- `OnboardingController` (DJT-059): máquina de estados **del servidor**
  (`NOT_STARTED→SELECTING_CLAN→SELECTING_AFFINITY→COMPLETED`; el "finish" con los rolls es transición, no
  fase). Payload de fase o id inválidos = **no-op**. Afinidad elegida **fijada a 50** (`set`, no `add`).
  **Caso Lee** (`skipsAffinity` en datos): salta afinidad **y** roll. `refresh` en login arranca nuevos,
  reanuda a medias y **re-embarca** tras reset. Clan y afinidad **aleatorios del servidor** (`join_random`
  uniforme 1..14 con Lee incluido; `random_aff` uniforme 1..5).
- `OnboardingChoicePayload` (C2S) + `OnboardingChoiceHandler`: intenciones por **nombre**, validadas.
- `NinjaCreationScreen` + `NinjaCreationOpener` (DJT-060): páginas decididas por la **fase sincronizada**;
  clanes con su `teamColor` (vía `OnboardingOptionsPayload` S2C — en dedicado el cliente no tiene el
  registro); afinidades con los colores exactos del datapack; reapertura a los 2 s si se cierra sin elegir
  (sin brickear: deja llegar al menú de pausa).

### Extra
- **DJT-063** (`LeveledStat` inmutable): deuda de F1 pagada antes de DJT-051 (estado inmutable hasta las hojas).

## Decisiones y hallazgos clave de F3 (y su porqué)

1. **Los resúmenes de `DOCS/sistemas` NO son fuente de verdad** (regla 16 de CLAUDE.md, nacida aquí): se
   encontraron **7+ falsedades** en S03/S04 (XP "uniforme", "KG=10", afinidad-50 "en el grant"…). Siempre
   verificar contra `.mcfunction`/predicates/loot tables.
2. **Conflictos ficha-vs-datapack se paran y se preguntan.** Dos casos decididos por el usuario a favor del
   datapack: la afinidad 50 es del **onboarding** (no del grant — un Uchiha tendría 60), y las temporales son
   **prestadas sin temporizador** (el datapack no tiene ninguno).
3. **Contenido data-driven con codecs estrictos** (18 KG + 14 clanes + tabla RNG): refs rotas fallan al
   cargar con aislamiento por archivo; los codecs de **estado** siguen siendo tolerantes (NBT corrupto nunca
   bloquea). La tabla RNG además se **auto-valida** (S03-R4: un JSON mal editado alteraría las probabilidades
   en silencio).
4. **Proveedores de factores de regen** (`RegenService.registerFactorProvider`): los clanes enchufan su factor
   sin que `chakra` importe `clans`; F6 (sabio) se enchufa igual sin tocar chakra.
5. **Onboarding = fase observable + intenciones.** El cliente nunca decide: la Screen pinta la fase sincronizada
   y envía nombres; el servidor valida fase+registro. El caso Lee "sale gratis" en el cliente.
6. **Distribución del RNG idéntica por construcción:** las loot tables `*_rng` eran un truco de entero
   uniforme + rangos; el JSON declara los tramos `[min,max]` auditables línea a línea y el peso es el tamaño
   del tramo.
7. **`PlayerTeam` no es construible en tests** sin bootstrap de MC (su ctor arrastra `ItemStack.<clinit>`):
   el glue fino de teams se valida en runtime, como la GUI/HUD de F2.
8. **Trampa de `EnumMap`:** `new EnumMap<>(mapaVacioNoEnum)` lanza; usar `new EnumMap<>(Clase.class)+putAll`
   (helper documentado en `TemporaryGrants`).
9. **`/reload` lee recursos compilados** (`common/build/resources/main`), no `src/` (regla 8b): editar JSON en
   src y recargar sin build no hace nada.

## Cómo se valida hoy

- **Unit:** `./gradlew :common:test` (**393 verdes**). **Build:** `./gradlew build` multiloader verde.
- **Runtime:** la creación completa es **por GUI** (se abre sola). Comandos de depuración temporales:
  - `/dojutsuonboarding state|clan <id|random>|affinity <id|random>` (simula la Screen)
  - `/dojutsunatures get|grant <nat>|revoke <nat>|aff <nat> <0-500>|kg list|grant|revoke <kg>|tmp grant|grant_kg|revoke|roll <veces>`
    *(`aff` añadido en F4/DJT-085: fija el nivel de afinidad para probar jutsus de tier alto — sin él, la afinidad solo sube casteando, +1/cast)*
    *(corregido 2026-07-16: este doc decía `give`/`take`, pero los literales reales del código son `grant`/`revoke`)*
  - `/dojutsuclans list|get|join <clan>|set <clan>|reset|clear`
  - Los de F1/F2: `/dojutsuninja`, `/dojutsusched`, `/dojutsuchakra`, `/dojutsuprogression`; real:
    `/dojutsu reset_ninja`
- Secuencia de prueba recomendada: **empezar siempre con `/dojutsu reset_ninja`** (los estados de mundos de
  prueba contaminan diagnósticos — pasó en DJT-055).

## Deuda / hooks abiertos para la Fase 4+

- **Enseñar jutsus con el learner** (`LearnerService.use` → `// TODO F4/DJT-075`): hoy valida posesión y avisa
  "jutsu pending"; la entrega real del jutsu es F4.
- **Entrega del jutsu de clan** (`ClanJutsuUnlockService.reachedThreshold` → F4): el contador llega al umbral
  y espera; F4 entrega el siguiente de la **secuencia** (Nara: paralysis→clutch→sewing) y resetea a 0.
  **Nota S04-R6:** Shadow Clutch arrastraba entidades cada tick — diseñar con presupuesto (área acotada).
- **`startingJutsu`/`startingItems` de los clanes**: en datos desde DJT-056, **no se entregan** (los ítems de
  jutsu son F4).
- **`XpApi.addAffinityXp` (F4/DJT-078):** la XP de afinidad se gana **usando jutsus** (`jutsu/training/*`);
  el estado ya existe, falta el emisor.
- **Puerto `AttributeService`:** sigue diferido. Primer cliente real: velocidad Namikaze (+0.15) y los boosts
  de taijutsu (F4/F6).
- **`hashirama_cells`:** el flag `hashiramaDna` y su factor de regen ya operan; el **grant completo** (+30
  medical_xp, +10 medical, +30 ninja_xp) llega con su ítem (DNA vile/injector, F7/F8).
- **Fuentes pendientes de 3 KG** (fiel al datapack): `jugo_clan` (implante DNA, F7/F8), `blaze` (Mangekyo/DNA,
  F5/F7); `shikotsumyaku` ya sale por el clan Kaguya.
- **Team `MaskHidden`** (ocultar nametag con máscaras): F8.
- **`tmp_sp_chakra`** (chakra especial temporal): F6.
- **Consumidor real de `TemporaryGrantService`:** F7 (bijuu prestan naturalezas al jinchūriki).
- **Retirar andamiaje:** `/dojutsuonboarding` ya es redundante con la Screen (se mantiene como herramienta de
  admin/CI manual hasta F4); el resto de comandos debug, cuando el contenido los cubra.
- **Assets:** la Screen usa widgets vanilla con texto coloreado; los assets reales del `resource pack` del
  datapack (modelos CustomModelData, fuente, sonidos) se portan **por fase** a `assets/dojutsu` (F4 jutsu/armas,
  F5 ojos, F7 bijuu, F8 ítems/máscaras).
- Heredados de F2: ramen +100 (F8), deltas por-campo si crecen, `LICENSE` no-comercial, retirar
  `DebugMarkerEntity`.

## Próxima fase (F4 — Combate)

Ficha completa: `D:\Projectos claude\Dojutsu reverse\DOCS\backlog\05_FASE4_COMBATE.md` (épicas de jutsus,
entidades de técnica, proyectiles, entrenamiento de afinidad). **No empezar sin aprobación.**

**Qué reutilizar directamente:**
- El contrato de técnica ya existe en dominio (`Jutsu`/`JutsuDefinition`/`CastContext`/`CastResult`, DJT-013)
  y el perfil elemental completo (afinidades reales con nivel/XP) está poblado por F3.
- Jutsus **data-driven** = patrón de los KG/clanes (JSON + codec estricto + registry + reload listener).
- Consumo de chakra = `ChakraService.consume` (todo-o-nada); XP = `XpApi`; eventos = `DomainEvents`.
- Entrega de ítems al desbloquear = patrón reactivo del `LearnerService` (suscripción a eventos de dominio).
- Los hooks listados arriba (learner→enseñar, clan→entregar jutsu, addAffinityXp) marcan los puntos de
  enganche exactos con `// TODO F4`.

## Recomendaciones para continuar

1. **Leer la fuente de verdad** (`D:\Projectos claude\Dojutsu reverse\DOCS\` → sistemas/arquitectura/backlog)
   pero **verificar todo número y todo flujo contra el datapack** (`data/dojutsu/functions/...`): los
   resúmenes tienen errores documentados (regla 16).
2. **Un ticket a la vez**; validar en runtime en **ambos** loaders lo que toque plataforma/cliente; empezar
   las pruebas manuales con `/dojutsu reset_ninja`.
3. **Puertas únicas**: chakra→`ChakraService`, XP→`XpApi`, perfil elemental→`NatureService`,
   clan→`ClanService`, fase→`OnboardingController`. Reactivo por `DomainEvents`, nunca polling.
4. **El cliente nunca decide gameplay**: payloads con ids por nombre, validados contra la fase/el registro.
5. Al añadir sub-estados (`JutsuState`?), seguir el patrón enchufable (DEFAULT+CODEC+puerta+`optionalFieldOf`).
6. Antes de tocar build/red/eventos, leer `docs/development_log.md` (los problemas ya resueltos están ahí).
