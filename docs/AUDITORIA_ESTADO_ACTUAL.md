# Auditoría técnica — Estado actual del mod Dojutsu Port

> **Fecha de la auditoría:** 2026-07-30
> **Repositorio auditado:** `D:\Projectos programacion\Dojutsu Port` (rama `main`)
> **Método:** lectura directa del código, los recursos y los datos. **No se ha tomado ningún dato de
> `docs/current_state.md`, `docs/backlog.md` ni `CLAUDE.md`**: esos documentos están desactualizados en varios
> puntos (se detallan en §14.1). Todo lo que aquí se afirma está verificado contra el archivo fuente y se cita
> su ubicación.
> **Verificación ejecutada durante la auditoría:** `./gradlew build` → **BUILD SUCCESSFUL**;
> `./gradlew :common:test` → **912 tests, 0 fallos, 0 errores, 0 omitidos**.

---

## Índice

0. [Resumen ejecutivo](#0-resumen-ejecutivo)
1. [Jutsus y técnicas](#1-jutsus-y-técnicas)
2. [Kekkei Genkai, Dōjutsu y habilidades especiales](#2-kekkei-genkai-dōjutsu-y-habilidades-especiales)
3. [Objetos](#3-objetos)
4. [Entidades](#4-entidades)
5. [Modelos y recursos gráficos](#5-modelos-y-recursos-gráficos)
6. [Animaciones](#6-animaciones)
7. [Comandos](#7-comandos)
8. [Configuración](#8-configuración)
9. [Sistemas del mod](#9-sistemas-del-mod)
10. [Estructura del proyecto](#10-estructura-del-proyecto)
11. [Código pendiente](#11-código-pendiente)
12. [Contenido sin uso](#12-contenido-sin-uso)
13. [Estado general](#13-estado-general)
14. [Observaciones y oportunidades de mejora](#14-observaciones-y-oportunidades-de-mejora)

---

## 0. Resumen ejecutivo

| Métrica | Valor verificado |
|---|---|
| Clases Java | **504** (486 `common` — de las cuales 384 en `main` y 102 en `test` —, 10 `fabric`, 8 `neoforge`) |
| Tests | **912** métodos `@Test`, 102 clases de test, **todos en verde** |
| Build multiloader | **Verde** (`fabric` y `neoforge` producen jar de 3 MB cada uno) |
| Jutsus (JSON) | **128** |
| Ojos (JSON) | **15** (14 jugables + 1 placeholder no jugable) |
| Habilidades oculares (clases) | **30** |
| Clanes | **14** |
| Kekkei Genkai | **18** |
| Contratos de invocación | **27** |
| Ítems registrados | **20** |
| Tipos de entidad registrados | **5** |
| Sonidos registrados | **28** (28 ficheros `.ogg`) |
| Partículas propias registradas | **1** (placeholder sin uso) |
| Modelos de ítem | **173** JSON |
| Texturas | **184** PNG |
| Idiomas | **1** (`en_us`, 508 claves) |
| Comandos raíz | **21** (`/dojutsu` + 20 de depuración) |

**Conclusión en una línea:** el mod está **funcionalmente muy completo y técnicamente sano** (build y tests
verdes, arquitectura consistente), pero tiene **tres agujeros reales que no son de texturas**: (a) el trabajo de
las fases F1–F9 **no está commiteado en git**, (b) **26 de los 128 jutsus no tienen forma de obtenerse en
partida** porque `UnlockService` y `ClanService` nunca entregan el ítem, y (c) **20 de los 28 sonidos y toda la
capa de partículas propia están sin cablear**. Detalle en §13 y §14.

---

## 1. Jutsus y técnicas

**Estado global:** ✅ **Funcional**. Motor de casteo único y completo; 128 técnicas cargadas por datos.
**Ubicación:** definición en `common/src/main/resources/data/dojutsu/jutsu/<categoria>/<id>.json`;
motor en `common/src/main/java/com/zamu/dojutsu/jutsu/`.
**Dependencias:** `ChakraService` (coste), `NatureService` (afinidad), `JutsuKnowledge` (conocimiento y
cooldowns), `NinjaProjectile`/`AbilityAnchor` (efectos), `DomainEvents` (evento de cast).

### 1.1. Arquitectura de una técnica

Un jutsu **no es una clase**: es un JSON con un `effect` que apunta a una de **7 familias de efecto**
implementadas en código. Esto es una decisión deliberada (RA-1 del ADD: "una clase por familia, nunca por
técnica").

Esquema del JSON (codec en [`JutsuEntry.java`](common/src/main/java/com/zamu/dojutsu/jutsu/JutsuEntry.java)):

| Campo | Obligatorio | Descripción |
|---|---|---|
| `id` | sí | Identificador único, no vacío |
| `chakraCost` | sí | Coste de chakra (≥ 0) |
| `cooldownTicks` | sí | Enfriamiento por instancia (≥ 0) |
| `affinity` | no | Objeto `{nature, amount}` **o** array de objetos (KG multi-naturaleza) |
| `kekkeiGenkai` | no | Requisito de **posesión** de un KG |
| `skill` | no | `{skill, amount}` — habilidad y nivel mínimo |
| `levelRequirement` | no | Default 0 |
| `category` | sí | Carpeta/elemento |
| `effect` | sí | Id de la familia de efecto |
| `effectParams` | no | Passthrough; cada familia define su esquema |

### 1.2. Las 7 familias de efecto

Registradas en [`JutsuEffects.java:24-31`](common/src/main/java/com/zamu/dojutsu/jutsu/effect/JutsuEffects.java).
El pipeline resuelve la familia **antes** de consumir chakra: un jutsu con `effect` desconocido falla sin coste.

| Familia | Clase | Estado | Comportamiento |
|---|---|---|---|
| `projectile` | `ProjectileEffect` | ✅ | Lanza un `NinjaProjectile` en la mirada; aura de fuego opcional (`fireTicks`, `igniteGround`), radio de impacto, `displayItem` como aspecto |
| `area` | `AreaEffect` | ✅ | Daño y/o efectos de poción a las entidades vivas en `radius`; soporta `launchY` (empuje vertical) |
| `buff` | `BuffEffect` | ✅ | Efectos de poción sobre el lanzador |
| `teleport` | `TeleportEffect` | ✅ | Reubicación aleatoria entre `minRange` y `maxRange`; `invisibilitySeconds` opcional |
| `structure` | `StructureEffect` | ✅ | Coloca un muro `width × height` a `forward` bloques, orientado a la mirada |
| `heal` | `HealEffect` | ✅ | Cura a la entidad viva más cercana en `radius` (**no** al lanzador); sin objetivo → sin coste |
| `primed_strike` | `PrimedStrikeEffect` + `PrimedStrikeManager` | ✅ | Castear **arma** el golpe; el siguiente golpe cuerpo a cuerpo lo descarga |

### 1.3. Pipeline de casteo

[`JutsuCastPipeline`](common/src/main/java/com/zamu/dojutsu/jutsu/JutsuCastPipeline.java) es el **único camino
de cast** del mod (jutsus, kenjutsu, habilidades oculares y la IA de NPCs usan el mismo). Secuencia:
**validar** (chakra → cooldown → naturaleza → afinidad → KG → habilidad) → **consumir** → **fijar cooldown** →
**ejecutar efecto** → **emitir `JutsuCastEvent`**.

Consecuencias del evento: `JutsuXpHandler` (+1 XP a la afinidad), `LastCastRegistry` (registra el último cast
para la copia del Sharingan).

Razones de fallo tipadas en `CastFailureReason`; los mensajes viven en `jutsu.dojutsu.cast_failed`,
`missing_nature`, `missing_kekkei_genkai`, `higher_affinity`, `higher_skill`, `no_target`, `effect_pending`.

### 1.4. Catálogo completo (128 jutsus)

Notación: `chakra` / `cd` = ticks de enfriamiento / `req` = requisito.

#### Fuego (7)
| id | chakra | cd | req | efecto | daño / efecto |
|---|---|---|---|---|---|
| `fire_bullet` | 60 | 100 | fire 30 | projectile | radio 3, incendia 100t |
| `fireproof` | 20 | 0 | fire 50 | buff | fire_resistance 1s |
| `fire_transport` | 80 | 100 | fire 60 | teleport | 3–8 bloques |
| `fire_dragon_flame_bullet` | 100 | 100 | fire 100 | projectile | 6 dmg, radio 3 |
| `exploding_dragon_strike` | 150 | 400 | fire 140 | projectile | 10 dmg, radio 4, prende suelo |
| `great_fireball` | 140 | 100 | fire 170 | projectile | radio 3, prende suelo |
| `great_fire_annihilation` | 180 | 200 | fire 200 | projectile | 12 dmg, radio 5, fuego 200t |

#### Rayo (8)
| id | chakra | cd | req | efecto | daño / efecto |
|---|---|---|---|---|---|
| `lightning_bolt` | 60 | 200 | lightning 30 | primed_strike | 6 dmg, carga 30t |
| `lightning_instantaneous_body` | 80 | 100 | lightning 60 | teleport | 4–10 |
| `lightning_net` | 80 | 800 | lightning 80 | area | radio 8, 6 dmg, slowness II 4s |
| `tiger_scissors` | 100 | 200 | lightning 100 | area | radio 4, 8 dmg |
| `chidori` | 100 | 400 | lightning 120 | primed_strike | 10 dmg, carga 60t |
| `lightning_cutter` | 200 | 400 | lightning 200 | primed_strike | 12 dmg, carga 40t |
| `kirin` | 300 | 1200 | lightning 500 | area | radio 8, 16 dmg |
| `drilling_hand_of_sage_lightning` | 400 | 600 | lightning 500 | primed_strike | 14 dmg |

#### Viento (10)
| id | chakra | cd | req | efecto | daño / efecto |
|---|---|---|---|---|---|
| `propelling_winds` | 30 | 100 | wind 30 | buff | jump_boost IV + slow_falling 6s |
| `wind_torpedo` | 60 | 100 | wind 50 | projectile | 7 dmg |
| `vacuum_wave` | 70 | 300 | wind 70 | area | radio 10, lanza 8 bloques arriba |
| `wind_instantaneous_body` | 80 | 100 | wind 60 | teleport | 4–10 |
| `drilling_air_bullet` | 100 | 200 | wind 100 | projectile | 9 dmg |
| `rasengan` | 100 | 200 | wind 150 | primed_strike | 10 dmg |
| `wind_mode` | 100 | 400 | wind 120 | buff | speed II + jump_boost II, 30s |
| `wind_cloak` | 150 | 600 | wind 250 | buff | speed III + resistance I, 30s |
| `big_ball_rasengan` | 200 | 400 | wind 200 | primed_strike | 16 dmg |
| `rasenshuriken` | 300 | 1200 | wind 500 | projectile | 18 dmg, radio 6 |

#### Tierra (7)
`earth_prison` (30/600, earth 30, area radio 5 slowness VI) · `earth_wall` (60/0, earth 50, structure 3×4 dirt) ·
`rock_gun` (60/100, earth 30, projectile 7 dmg) · `earth_instantaneous_body` (80/100, earth 60, teleport 4–10) ·
`earth_golem` (100/600, earth 100, structure 3×3 stone) · `earth_dragon_blast` (120/600, earth 150, projectile
11 dmg radio 4) · `tunnelling` (160/100, earth 180, teleport 8–16)

#### Agua (7)
`breathing_underwater` (20/20, water 50, buff 60s) · `water_prison` (30/500, water 30, area slowness V) ·
`water_puddle` (30/600, water 40, structure agua) · `hidden_mist` (60/200, water 80, area blindness radio 8) ·
`water_instantaneous_body` (80/100, water 60, teleport) · `rising_water_slicer` (100/300, water 100, projectile
8 dmg) · `water_severing_wave` (140/300, water 160, projectile 12 dmg radio 4)

#### Yin-Yang (4) — Sexto Camino
`sword_of_nunoboko` (200/200, primed_strike 14) · `truth_seeking_scorched_earth` (900/300, projectile 16 dmg) ·
`six_paths_cross_lantern` (1200/400, area 18 dmg) · `creation_of_all_things` (4000/1200, heal radio 8,
regeneration XXI + absorción). Todos exigen `yin_yang 500`.

#### Taijutsu (4) — sin afinidad, sólo habilidad
`chakra_enhanced_strength` (40/100, taijutsu 25, primed_strike 8) · `heavenly_foot_of_pain` (80/150, taijutsu 38,
primed_strike 11) · `crimson_drizzle` (100/200, taijutsu 50, area 8) · `cherry_blossom_impact` (200/300,
taijutsu 63, area 14)

#### Genjutsu (6)
`phantom_sound_chains` (**0 chakra**/200, genjutsu 15) · `paralysis` (40/200, genjutsu 15, yin 20) ·
`bringer_of_darkness` (60/200, genjutsu 18, yin 10) · `flower_hill` (70/200, genjutsu 18, yin 30) ·
`hundred_blooming_flowers` (80/300, genjutsu 20, yin 30) · `unknown_fire` (120/300, genjutsu 38, yin 60).
Todos `area` con blindness/slowness/weakness; sólo `unknown_fire` hace daño (6).

#### Ninjutsu médico (4) + universales (2)
`regenerative_healing` (40/200, medical 3) · `wide_healing` (80/300, medical 13) · `mystical_palm` (10/100,
medical 25) · `creation_rebirth` (1000/1200, medical 125, yin 50) · `healing` (40/1200, medical 5, universal) ·
`substitution` (20/600, teleport 5–10 + invisibilidad 6s, universal — **sin requisito alguno**)

#### Fuinjutsu (3)
`four_symbols_seal` (100/300, fuinjutsu 300, yang 200) · `reverse_four_symbols_sealing` (1200/600, fuinjutsu 400,
yang 400) · `four_red_yang_formation` (**2000**/1200, fuinjutsu 500, yang 500, structure cristal rojo 5×4)

#### Kenjutsu (5) — requieren arma equipada
`dancing_blade_risk` (0/60, kenjutsu 75) · `assassination_technique` (0/60, kenjutsu 88) · `chakra_shock_slash`
(60/60, kenjutsu 125) · `chakra_shockwave_slash` (100/60, kenjutsu 150) ·
`secret_sword_snowstorm_beheading` (100/60, kenjutsu 350). Todos `area`, 6–14 daño.

#### Aburame (2) — sin requisito
`insect_sphere` (20/100, area radio 4, slowness III + weakness II) · `insect_tornado` (30/200, area radio 6, 8 dmg)

#### Kekkei Genkai (16 elementos, 55 jutsus)

| KG | Jutsus | Requisito de afinidad | Rango de coste |
|---|---|---|---|
| **Wood** (8) | `great_spear_tree`, `tree_bind_flourishing_burial`, `great_forest`, `wood_dome`, `spontaneous_tree_summoning`, `four_pillar_prison`, `four_pillar_house`, `nativity_of_a_world_of_trees` | earth + water 100→500 | 60–1000 |
| **Ice** (5) | `frozen_waste`, `ice_spear`, `ice_golem`, `blizzard`, `ice_rock_dome` | water + wind 60→160 | 80–140 |
| **Lava** (3) | `lava_snake_bullet`, `raining_debris`, `intense_flow` | earth + fire 150→300 | 180–400 |
| **Crystal** (4) | `crystal_wall`, `crystal_needles`, `jade_crystal_prison`, `the_gods_crossing` | earth + water + yang 50→160 | 60–150 |
| **Storm** (3) | `thunder_cloud_inner_wave`, `laser_circus`, `weather_manipulation` | lightning + water 120→250 | 140–250 |
| **Boil** (4) | `boiling_mist_dragon`, `erupting_propulsion_fist`, `meltdown`, `skilled_mist` | fire + water 140→300 | 150–330 |
| **Magnet** (6) | `sand_wave`, `iron_sand_wave`, `iron_sand_drizzle`, `sand_binding_coffin`, `iron_sand_wall`, `sand_waterfall_funeral` | earth + wind 120→250 | 120–300 |
| **Explosion** (7) | `c1`, `explosive_landmine`, `landmine_fist`, `exploding_palm`, `clay_doll`, `c3`, `c0` | earth + lightning 120→500 | 120–600 |
| **Scorch** (3) | `great_steaming_explosive_blast`, `multiple_falling_suns`, `extremely_steaming_murder` | fire + wind 150→300 | 180–300 |
| **Steel** (4) | `steel_projectile`, `steel_shield`, `impervious_armor`, `steel_spike` | earth + fire + water 80→200 | 120–200 |
| **Dark** (2) | `judgment`, `inhaling_maw` | wind + yin 140→200 | 120–160 |
| **Swift** (2) | `shadowless_flight`, `instantaneous_movement` | wind + lightning 160→300 | 100–200 |
| **Typhoon** (2) | `consecutive_bursting_strong_winds`, `great_consecutive_bursting_extreme_winds` | water + wind 150→250 | 180–230 |
| **Mud** (3) | `muddy_water_wall`, `landslide`, `mud_doll` | earth + water 100→200 | 100–240 |
| **Blaze** (2) | `honoikazuchi`, `kagutsuchi` | **sin afinidad**, sólo posesión de `blaze` | 180 |
| **Dust** (1) | `detachment_of_the_primitive_world` | fire + wind + earth 500 | 600 |

### 1.5. Aprendizaje y obtención de técnicas

| Vía | Estado | Cobertura |
|---|---|---|
| **Skill Learner** (`LearnerItem` → `JutsuLearningService`) | ✅ Funcional | 22 secuencias, **102 de 128** jutsus |
| **Copia del Sharingan** (`CopyJutsuAbility`) | ✅ Funcional | Cualquier jutsu que se vea castear |
| **Desbloqueos por hito** (`UnlockService`) | ⚠️ **INCOMPLETO** | Marca el id pero **no entrega el ítem** (`TODO F4` en [`UnlockService.java:68`](common/src/main/java/com/zamu/dojutsu/progression/UnlockService.java)) |
| **Jutsus iniciales de clan** (`ClanService.startingJutsu`) | ⚠️ **INCOMPLETO** | Sólo lo registra en el log (`TODO F4/F8` en [`ClanService.java:165`](common/src/main/java/com/zamu/dojutsu/clans/ClanService.java)) |
| **Desbloqueo progresivo de clan** (`ClanJutsuUnlockService`) | ⚠️ **INCOMPLETO** | Cuenta hasta el umbral y **sólo escribe un log** ([`ClanJutsuUnlockService.java:52`](common/src/main/java/com/zamu/dojutsu/clans/ClanJutsuUnlockService.java)) |

**Impacto medido:** **26 jutsus** no aparecen en ninguna secuencia de learner y, por tanto, **no son
obtenibles** con las vías funcionales (salvo copiándolos a alguien que ya los tenga, lo cual es circular):

```
assassination_technique, bringer_of_darkness, chakra_enhanced_strength, chakra_shock_slash,
chakra_shockwave_slash, cherry_blossom_impact, creation_rebirth, crimson_drizzle, dancing_blade_risk,
flower_hill, four_red_yang_formation, four_symbols_seal, healing, heavenly_foot_of_pain,
hundred_blooming_flowers, insect_sphere, insect_tornado, mystical_palm, paralysis, phantom_sound_chains,
regenerative_healing, reverse_four_symbols_sealing, secret_sword_snowstorm_beheading, substitution,
unknown_fire, wide_healing
```

Es decir: **todo el kenjutsu, todo el taijutsu, todo el genjutsu, todo el ninjutsu médico, todo el fuinjutsu,
los dos de Aburame y los dos universales**.

### 1.6. Técnicas declaradas sin implementar

| id | Dónde se declara | Estado |
|---|---|---|
| `fire_cloak` | `learner_sequences/fire.json` (último paso) + `lang` | ❌ **No existe el JSON** |
| `water_cloak` | `learner_sequences/water.json` (último paso) + `lang` | ❌ **No existe el JSON** |
| `earth_cloak` | `learner_sequences/earth.json` (último paso) + `lang` | ❌ **No existe el JSON** |
| `lightning_cloak` | `learner_sequences/lightning.json` + `lang` | ❌ **No existe el JSON** |
| `dance_of_the_camellia` | `clans/kaguya.json` `startingJutsu` | ❌ No existe |
| `shadow_paralysis` | `clans/nara.json` `startingJutsu` | ❌ No existe |
| `strength_of_a_hundred_seal` | `unlocks/progression.json` (medical 375) | ⚠️ Existe como **modo** (SOAH), no como jutsu |

Los cuatro "cloaks" son el **último paso** de las secuencias de fuego, agua, tierra y rayo: un jugador que
complete esas cuatro ramas se quedará con el learner sin consumir y sin recibir el último jutsu. El código lo
maneja sin romper (`JutsuLearningService` tiene el caso "lote pendiente"), pero es contenido inalcanzable.

### 1.7. Recursos por técnica

- **Modelos:** 128/128 jutsus tienen modelo (`models/item/jutsu/<id>.json`), seleccionados por
  `custom_model_data` desde [`JutsuModelData`](common/src/main/java/com/zamu/dojutsu/items/JutsuModelData.java)
  a través del modelo padre `models/item/jutsu.json`.
- **Texturas:** 128/128 (`textures/item/jutsu/<id>.png`).
- **Partículas:** ninguna técnica emite partículas propias salvo `TeleportEffect` (`ParticleTypes.POOF`) y
  `PrimedStrikeManager` (rayo visual). Las familias `projectile`, `area`, `buff`, `structure` y `heal` **no
  emiten partículas**.
- **Sonidos:** sólo `HealEffect` reproduce sonido (`medical_ninjutsu.healing`). **El resto del catálogo de
  jutsus es mudo**, pese a existir `jutsu.earth`, `jutsu.wind`, `jutsu.lightning`, `jutsu.wood`,
  `jutsu.chidori`, `jutsu.rasengan`, `jutsu.rasenshuriken` registrados y con fichero.
- **Animaciones:** ninguna. El mod no tiene sistema de animación (§6).

---

## 2. Kekkei Genkai, Dōjutsu y habilidades especiales

### 2.1. Kekkei Genkai — ✅ Funcional

**Ubicación:** `data/dojutsu/kekkei_genkai/*.json` (18) · código en `natures/KekkeiGenkai*`.
**Enum:** [`KekkeiGenkai`](common/src/main/java/com/zamu/dojutsu/domain/nature/KekkeiGenkai.java) — 18 valores.

| KG | Componentes | ninja_xp | Bonus de afinidad |
|---|---|---|---|
| wood | earth, water | 10 | — |
| ice | wind, water | 8 | water +10 |
| lava | fire, earth | 12 | — |
| storm | lightning, water | 8 | — |
| boil | fire, water | 8 | — |
| dust | fire, wind, earth | 12 | — |
| magnet | wind, earth | 9 | — |
| explosion | lightning, earth | 10 | — |
| scorch | fire, wind | 8 | — |
| steel | fire, earth, water | 8 | — |
| dark | wind, yin | 9 | — |
| swift | wind, lightning | 9 | — |
| crystal | earth, water, yang | 9 | — |
| typhoon | wind, water | 9 | — |
| mud | earth, water | 8 | — |
| blaze | fire | 10 | fire +20 |
| shikotsumyaku | — (clan Kaguya) | 10 | — |
| jugo_clan | — | 20 | — |

**Obtención:** tirada del onboarding (`rng/nature_roll.json`, 17/50 entradas premian KG; Senju tiene 11/50 extra
para Wood), clan (`kekkeiGenkai` en el JSON del clan: Kaguya→shikotsumyaku, Namikaze→swift), implante de ADN
(§3), Mangekyō de Sasuke (`awakenKekkeiGenkai: "blaze"`) y préstamos temporales (`TemporaryGrantService`).

**Limitación conocida:** `jugo_clan` habilita la regeneración de chakra natural en `RegenService`, pero
**no existe el clan Jūgo**: el KG sólo se puede obtener por tirada o implante de ADN.

### 2.2. Sistema de ojos (Dōjutsu) — ✅ Funcional (lógica) / ❌ Sin assets

**Ubicación:** `data/dojutsu/eyes/*.json` (15) · código en `common/src/main/java/com/zamu/dojutsu/dojutsu/`.

**Grafo:** data-driven. `EyeDefinition` declara `family`, bonus de despertar, `awakenTicks`,
`chakraVision`, `abilities`, `evolutions` (con `requirements` de tipo `clan`/`use_level`/`flag` y `variants`
para rutas aleatorias) y `eternalRequirements`.

| Ojo | Familia | Despertar | Evoluciona a | Habilidades |
|---|---|---|---|---|
| `byakugan` | byakugan | 78 000 t (clan Hyūga/Ōtsutsuki) | `tenseigan` (Ōtsutsuki + uso 864 000) | vacuum_palm, sixty_four_palms, hundred_twenty_eight_palms |
| `sharingan` | sharingan | 78 000 t (Uchiha/Ōtsutsuki), uso inicial 78 000 | `mangekyo_*` (8 variantes, uso 360 000) · `rinnegan` (Uchiha + flag `hashirama_dna`) | genjutsu_stun, copy_jutsu |
| `mangekyo_itachi` | mangekyo | por evolución | eterno (EMS) | amaterasu, tsukuyomi, susanoo |
| `mangekyo_obito` | mangekyo | por evolución | eterno | kamui, kamui_others, susanoo |
| `mangekyo_sasuke` | mangekyo | por evolución (+ otorga KG `blaze`) | eterno | amaterasu, flame_control, susanoo |
| `mangekyo_madara` | mangekyo | por evolución | eterno | tengai_shinsei, susanoo |
| `mangekyo_indra` | mangekyo | por evolución | eterno | hinokagutsuchi, susanoo |
| `mangekyo_shisui` | mangekyo | por evolución | eterno | kotoamatsukami, beams_of_light, susanoo |
| `mangekyo_fugaku` | mangekyo | por evolución | eterno | tsukuyomi, susanoo |
| `mangekyo_shin` | mangekyo | por evolución | eterno | chakra_blades, phase_shift, susanoo |
| `rinnegan` | rinnegan | por evolución (+100 chakra, +150 xp) | — | shinra_tensei, bansho_tenin, amenotejikara, chibaku_tensei, preta_path, asura_path |
| `rinne_sharingan` | rinne_sharingan | Árbol Divino (§9.14) (+100 chakra, **+1000 xp**) | — | las 6 del Rinnegan |
| `tenseigan` | tenseigan | por evolución (+100/+100) | — | tenseigan_chakra_mode, truth_seeking_orbs, kinrin_tensei_baku |
| `ketsuryugan` | ketsuryugan | 24 000 t (clan Chinoike) | — | blood_eruption, exploding_human, iron_blood_fist, rising_blood_spear |
| `naka` | mangekyo | — | — | ⚠️ **`"implemented": false`** — placeholder que carga pero no es jugable |

**Servicios (todos ✅ funcionales y cableados en `DojutsuLifecycle`):**

| Servicio | Fase de tick | Función |
|---|---|---|
| `AwakeningService` | DOJUTSU | Acumula el temporizador y despierta el ojo potencial del clan (50/50 en Ōtsutsuki). Una vez por clan |
| `EvolutionService` | DOJUTSU | Acumula uso del ojo activo y transiciona por la primera arista satisfecha |
| `EternalAwakeningService` | — (acción de menú) | EMS manual: clan + uso ≥ 864 000 + medical ≥ 150 → eterno, +60 chakra máx |
| `EyeStrainService` | STRAIN | Regen −1 (−3 con ADN de Hashirama) cada 200 t; 95→weakness, 98→blindness, 100→apaga. Rinne Sharingan exento. Reset al dormir |
| `EyeMenuService` | — (payloads) | Construye la vista del menú (tecla **N**) y valida la selección en servidor |
| `EyeAbilityService` | — (payloads) | Pipeline de habilidad (tecla **R**): valida ojo/selección/chakra/cooldown → consume → ejecuta → sync |
| `ChakraVisionService` | DOJUTSU | Glow nativo por relación cada 10 t, radio 30 (§9.10) |

### 2.3. Habilidades oculares (30 implementadas) — ✅ Funcional

Registro en código ([`EyeAbilityService.java:55-84`](common/src/main/java/com/zamu/dojutsu/dojutsu/ability/EyeAbilityService.java)),
no data-driven. **Comprobación de consistencia ejecutada: 0 habilidades declaradas sin clase, 0 clases sin
declarar en ningún ojo.**

| id | Chakra | Fatiga | Cooldown | Tipo |
|---|---|---|---|---|
| `amaterasu` | 40 | +10 | — | Ancla de fuego negro (raycast) |
| `vacuum_palm` | 40 | 0 | — | Empuje dirigido |
| `sixty_four_palms` | 60 | 0 | — | Golpe melé AoE |
| `hundred_twenty_eight_palms` | 80 | 0 | — | HARM III AoE |
| `blood_eruption` | 120 | +8 | — | Efectos de estado |
| `iron_blood_fist` | 80 | +4 | — | Auto-buff |
| `rising_blood_spear` | 100 | +14 | — | HARM AoE |
| `exploding_human` | 200 | +20 | — | HARM III |
| `tsukuyomi` | 100 | +30 | — | Genjutsu multi-debuff |
| `shinra_tensei` | 600 | 0 | — | Empuje AoE |
| `bansho_tenin` | 360 | 0 | — | Atracción AoE |
| `amenotejikara` | 600 | 0 | — | Teleport / intercambio |
| `chibaku_tensei` | 600 | 0 | — | Gravedad vertical |
| `asura_path` | 450 | 0 | — | Misil `NinjaProjectile` + resistencia |
| `preta_path` | 150 | 0 | — | Defensivo (absorción + resistencia) |
| `truth_seeking_orbs` | 300 | 0 | — | HARM III en el punto de mira |
| `kinrin_tensei_baku` | 400 | 0 | — | Proyectil canalizado |
| `genjutsu_stun` | **0** | 0 | **200 t** | Stun duro (slowness 255) |
| `copy_jutsu` | **0** | 0 | **200 t** | Aprende el jutsu visto (`LastCastRegistry`) |
| `susanoo` | 0 | 0 | — | Activa el modo (§2.4) |
| `tenseigan_chakra_mode` | 0 | 0 | — | Activa el modo (buffs + vuelo) |
| `kamui` | 0 | 0 | — | Marca-y-vuelve (`KamuiWaypoints`) |
| `kamui_others` | 0 | 0 | — | Envía a otros al punto marcado |
| `tengai_shinsei` | 200 | +13 | 200 t | Meteorito |
| `hinokagutsuchi` | 20 | +5 | 200 t | Llama negra |
| `kotoamatsukami` | 50 | +15 | 500 t | Genjutsu supremo |
| `chakra_blades` | 20 | +6 | 60 t | Cuchillas |
| `phase_shift` | 100 | +8 | 200 t | Salto guiado |
| `flame_control` | 20 | +10 | 100 t | Apaga la llama |
| `beams_of_light` | 80 | +20 | 800 t | Rayo dirigido |

### 2.4. Susanoo — ⚠️ Funcional sin modelos

**Ubicación:** `dojutsu/susanoo/{SusanooService, SusanooAvatar}` + `domain/susanoo/*`.
Primer consumidor del arnés `ToggleableModeService`.

| Nivel | Nombre | Chakra/pulso | Fatiga/pulso | Pulso (t) | Resistencia | Fire res. | Lastra | ninja_xp |
|---|---|---|---|---|---|---|---|---|
| 1 | Ribcage | 6 | 3 | 20 | 0 | 0 | no | 10 |
| 2 | Skeletal | 4 | 2 | 20 | 1 | 1 | no | 12 |
| 3 | Armored | 2 | 1 | 40 | 2 | 2 | **sí** | 13 |
| 4 | Humanoid | 1 | 1 | 50 | 3 | 3 | **sí** | 14 |
| 5 | Full-Body | 1 | 1 | 60 | 4 | 4 | no | 15 |
| 6 | Perfect | 1 | 1 | 80 | 5 | 5 | no | 16 |

Con ojo **eterno**: 0 chakra, +1 fatiga cada 160 t. Puerta de activación: 5 de chakra, cooldown 14 t, 32 000
ticks de uso por nivel de maestría.

**El avatar** es un `AbilityAnchor` parenteado; la silueta se dibuja **con partículas**
(`HEIGHT_BY_STAGE = {2.5, 3.2, 4.0, 5.0, 6.5, 8.0}`, `RADIUS_BY_STAGE = {1.0…3.2}` en
[`SusanooAvatar.java:39-42`](common/src/main/java/com/zamu/dojutsu/dojutsu/susanoo/SusanooAvatar.java)).

**Pendiente conocido:** los modelos 3D por personaje. Es el único pendiente de F5 declarado y sigue abierto.

### 2.5. Modo Sabio (Senjutsu) — ✅ Funcional

| Elemento | Valor |
|---|---|
| Carga | Quietud por umbral de **velocidad** (no sneak); espera inicial 1200 t; +1 por pulso hasta 100 |
| Linaje | Por bioma → `SageType` = TOAD / SNAKE / SLUG |
| Encendido | Exige **más** energía a menor nivel (100/50/25/1) |
| Duración | El nivel **alarga** el modo: drena 1 cada 100→500 t |
| Nivel máx. | 5 · uso 72 000 (niveles bajos) / 144 000 (nivel máximo) |
| Exclusión | Incompatible con el sello maldito activo |
| Tecla | **G** |

### 2.6. Bijū / Jinchūriki — ✅ Funcional (lógica) / ⚠️ Modelo prestado

**10 bestias** como enum ([`TailedBeast`](common/src/main/java/com/zamu/dojutsu/domain/bijuu/TailedBeast.java)),
cada una con su técnica emblemática y color de bossbar:

| Colas | Técnica | Color |
|---|---|---|
| 1 | sand_buckshot | `0xE2C496` |
| 2 | cat_flame_roaring_fire | `0x4F72FF` |
| 3 | great_water_mass_bullet | `0x800049` |
| 4 | monkey_flame_arson | `0xA71B1B` |
| 5 | unrivalled_strength | `0xE8E8F0` |
| 6 | soap_bubble_barrage | `0xD3D9E7` |
| 7 | scale_powder | `0x584FD0` |
| 8 | tentacle_barrage | `0xB0717A` |
| 9 | chakra_arms_barrage | `0xFF8000` |
| 10 | tenpenchii | `0x8E776C` |

**⚠️ Sólo hay UNA `BeastMove` implementada** (`TailedBeastBallMove`, la Bola Bestia con Cola): las 10 técnicas
del enum son nombres, no comportamientos distintos. El `BeastMoveRegistry` tiene **1 entrada**.

**Sellado** (`SealingService`): 1000 de chakra máximo por cola (1000…10 000). **Vínculo** (`BondTable`):
+50 chakra máx. y +10 ninja_xp por nivel; nivel 3 = amistad. **Forma de jinchūriki** (`FormService`, tecla
**K**): 4 niveles, +100 chakra máx., técnicas a partir del nivel 3, **Modo Baryon** en el 4 (12 000 t de
duración, mínimo 200 de chakra; al agotarse **la bestia se va**).

### 2.7. Sello maldito, Ocho Puertas, SOAH y Seis Caminos — ✅ Funcionales

| Sistema | Estado | Datos clave | Tecla |
|---|---|---|---|
| **Sello maldito** | ✅ | 6 tipos (`HEAVEN`, `EARTH`, `JIROBO`, `KIDOMARU`, `SUKON_UKON`, `TAYUYA`); inyección 60 % de éxito (+300 chakra máx., +50 xp, +50 senjutsu); nivel 1→2 abarata 12→8 chakra/40 t; **mata si no puedes pagarlo**; degrada a las 10 muertes en nivel 2 | **B** |
| **Ocho Puertas** | ✅ | Escalera de 8 (`OPENING`…`DEATH`), taijutsu 75 para activar / 500 para dominar; fatiga corporal sube al abrir y baja siempre (−1 cada 200 t, −3 con ADN Hashirama, cada 400 t para clan Lee); 95 castiga, 98 castiga duro, 100 cierra solas. **La 8ª no mata** | **H** |
| **SOAH** (Fuerza de un Centenar) | ✅ | Desbloqueo: medical 375; requiere yin 150 y 20 000 de chakra máximo. Recarga manual de 20 000; devuelve por tramos 100/10/1 cuando el chakra baja de 5000. **Único modo que devuelve recurso** | **J** |
| **Seis Caminos** | ✅ | `SixPathsService`: +6000 chakra, +200 xp, +100 afinidad, +75 senjutsu, otorga naturalezas. ⚠️ `RINNEGAN_ROUTE_DISABLED = true` — la vía del Rinnegan está **desactivada por bandera** | — |

---

## 3. Objetos

**Ubicación:** registro en [`ModItems.java`](common/src/main/java/com/zamu/dojutsu/registry/ModItems.java) ·
lógica en `common/src/main/java/com/zamu/dojutsu/items/`.

### 3.1. Ítems registrados (20) — todos con modelo y textura

| Ítem | Categoría | Estado | Modelo | Textura | ¿Se usa en juego? |
|---|---|---|---|---|---|
| `ninja_debug` | Debug | ⚠️ **Placeholder de F0** | ✅ (nether_star reciclado) | vanilla | ❌ **No lo usa nadie** |
| `learner` | Herramienta | ✅ Funcional | ✅ + 24 variantes | ✅ 24 | ✅ Entregado por `LearnerService` |
| `jutsu` | Consumible/uso | ✅ Funcional | ✅ + 128 variantes | ✅ 128 | ✅ Entregado por el learner y `copy_jutsu` |
| `chakra_blade` | Arma | ✅ Funcional (+3 daño) | ✅ | ✅ | ✅ Clan Sarutobi (declarado) + comercio + `/dojutsuweapon` |
| `kubikiribocho` | Arma | ✅ Funcional (+8 daño) | ✅ | ✅ | ⚠️ **Sólo por `/dojutsuweapon`** |
| `samehada` | Arma | ✅ Funcional (+6, absorbe chakra, kenjutsu 250) | ✅ | ✅ | ⚠️ **Sólo por `/dojutsuweapon`** |
| `dna` | Material | ✅ Funcional (26 tipos) | ✅ | ✅ | ✅ Comercio + drop-crafting + `/dojutsuitem` |
| `ration_pills` | Consumible | ✅ Funcional (5 cargas en durabilidad) | ✅ + 5 variantes | ✅ 6 (**1 sin usar**) | ✅ Comercio |
| `contract` | Pergamino | ✅ Funcional (27 especies) | ✅ | ✅ | ✅ Receta de mesa + comercio |
| `mask_anbu_1/2/3` | Accesorio | ✅ Funcional (oculta el nombre) | ✅ 3 | ✅ 3 | ✅ Drop-crafting + comercio (`anbu_1`) |
| `mask_tobi_1/2/3` | Accesorio | ✅ Funcional | ✅ 3 | ✅ 3 (+2 auxiliares de `tobi_3`) | ✅ Drop-crafting |
| `kunai` | Arrojadiza | ✅ Funcional (0.5 vel., 10 t, 5 dmg) | ✅ | ✅ | ✅ Receta + comercio |
| `shuriken` | Arrojadiza | ✅ (0.8, 20 t, 3 dmg) | ✅ | ✅ | ✅ Receta + comercio |
| `fuma_shuriken` | Arrojadiza | ✅ (0.8, 20 t, 4 dmg) | ✅ | ✅ | ✅ Receta + comercio |
| `explosive_kunai` | Arrojadiza | ✅ (explosión potencia 4) | ✅ | ✅ | ✅ Receta + comercio |
| `ftg_kunai` | Arrojadiza | ✅ (marca del Vuelo del Dios del Trueno) | ✅ | ✅ | ⚠️ **Sin receta ni oferta: inobtenible** |

**Armadura:** no existe ninguna. El mod **no registra ningún ítem de armadura** (las máscaras son estado, no
casco: ver [`MaskService`](common/src/main/java/com/zamu/dojutsu/items/MaskService.java)).

### 3.2. Data components (5)

`learner`, `jutsu`, `weapon`, `dna`, `contract` — todos persistentes y sincronizados al cliente
([`ModDataComponents`](common/src/main/java/com/zamu/dojutsu/registry/ModDataComponents.java)).

### 3.3. Recetas (5 de mesa + 16 de drop-crafting)

**Mesa** (`data/dojutsu/recipe/`, con su advancement de desbloqueo en `advancement/recipes/`):
`kunai`, `shuriken`, `fuma_shuriken`, `explosive_kunai`, `contract`.
El contrato pasa además por `CraftingGateService` (exige **fuinjutsu 75**; si falta, devuelve 6 papeles y 3
tintas y avisa en rojo).

**Drop-crafting** (`data/dojutsu/drop_crafting/`, combina ítems soltados juntos en el suelo):
6 máscaras + `reset_dna` **funcionales**, y **9 huevos de invocación de bestia (`spawn_egg_*`) declarados cuyo
ítem no existe** → esas 9 combinaciones son inertes. El listener lo reporta en el log
(`"N combinaciones (M pendientes de su item)"`).

### 3.4. Ítems referenciados que NO existen (24)

Los catálogos declaran el surtido completo del datapack aunque el ítem no esté portado (patrón deliberado; el
log lo cuenta). Inventario exacto:

| Origen | Ítems inexistentes |
|---|---|
| `trades/shinobi_villager.json` | `explosive_tag`, `smoke_bomb`, `surgical_tools`, `kotaro`, `toad_oil`, `experience_scroll`, `training_scroll`, `coloured_pills`, `ramen` (9) |
| `trades/wandering_trader.json` | `eye_sharingan`, `eye_byakugan`, `eye_ketsuryugan`, `eye_mangekyo_obito`, `eye_mangekyo_itachi`, `eye_mangekyo_sasuke` (6) |
| `drop_crafting/spawn_egg_*.json` | 9 huevos de bestia con cola |
| `clans/akimichi.json` | `three_coloured_pills` |
| `clans/hatake.json` | `white_light_chakra_sabre` |

De las **24 ofertas** de comercio declaradas, **15 apuntan a ítems inexistentes** (el 62 %).

---

## 4. Entidades

**Ubicación:** registro en [`ModEntities`](common/src/main/java/com/zamu/dojutsu/registry/ModEntities.java) ·
renderers en `fabric/…/DojutsuFabricClient` y `neoforge/…/DojutsuNeoForgeClientEvents` (idénticos).

| Entidad | Categoría | Estado | IA | Modelo / Renderer | Texturas | Animaciones |
|---|---|---|---|---|---|---|
| `ninja_projectile` | Proyectil | ✅ Funcional | — (física propia, colisión por raycast, dueño por UUID) | `ThrownItemRenderer` — se dibuja como el `displayItem` vanilla del jutsu | ❌ Propias: ninguna | ❌ |
| `ability_anchor` | Auxiliar | ✅ Funcional | Tick propio + expiración garantizada; ejecuta callbacks | `NoopRenderer` (invisible) | ❌ | ❌ |
| `marker` | Auxiliar | ✅ Registrada | Posición + caducidad, sin lógica | `NoopRenderer`; `clientTrackingRange(0)` | ❌ | ❌ |
| `tailed_beast` | Boss | ✅ Funcional | `TailedBeastMoveGoal` + goals de combate; radio de activación 32, intervalo 60 t (40 el Diez Colas); bossbar nativa (radio 100) | ⚠️ **`TailedBeastModel` reutiliza la malla del ravager vanilla** | ⚠️ Textura del ravager | ❌ (las del ravager) |
| `shinobi` | NPC hostil | ✅ Funcional | `ShinobiCombatGoal`; castea jutsus por el **mismo pipeline** que el jugador; 6 niveles de amenaza | ⚠️ **`ShinobiRenderer` reutiliza el humanoide y la textura del zombi** | ⚠️ Del zombi | ❌ (las del zombi) |

### 4.1. Atributos

**Bestia con Cola** ([`BeastStats`](common/src/main/java/com/zamu/dojutsu/domain/bijuu/BeastStats.java)):
vida 1024 · armadura 30 · rango de seguimiento 64 · velocidad 1.0 · daño 12 · resistencia a empuje 1.0 ·
recompensa: 90 + 10/cola de ninja_xp y 1000 + 100/cola de experiencia, radio 20.

**Shinobi** (`ShinobiEntity.createAttributes`): vida = la del nivel `ACADEMY` (120) ajustada al invocar ·
velocidad 0.3 · daño 6 · armadura 4.

| Nivel de amenaza | Vida | Color | Jutsus |
|---|---|---|---|
| ACADEMY | 120 | blanco | — |
| GENIN | 240 | blanco | great_fireball |
| CHUNIN | 360 | amarillo | +fire_bullet |
| TOKUBETSU_JONIN | 480 | naranja | +fire_dragon_flame_bullet |
| JONIN | 600 | naranja osc. | (lista ampliada) |
| KAGE | 700 | granate | (lista ampliada) |

⚠️ **Los seis niveles usan sólo jutsus de fuego.** No hay variedad elemental en la IA.

### 4.2. Invocaciones (summons)

`SummonService` **no registra entidades propias**: invoca entidades **vanilla** (`entityType` del contrato) y
les cuelga un `AbilityAnchor` que las despacha a los 1200 ticks. 27 contratos: 13 `low`, 12 `mid`, 2 `high`.

| Tier | Fuinjutsu | Rango mínimo | Cooldown |
|---|---|---|---|
| low | 0 (75 para firmar) | 2 | 600 t (fuinjutsu < 251) |
| mid | 150 | 2 | 400 t (251–375) |
| high | 200 | 3 | 200 t (≥ 376) |

Castear la invocación exige **fuinjutsu 125**. **Invocar no cuesta chakra**: cuesta veto de cooldown.

### 4.3. Entidades que NO existen

- **Clones** (Kage Bunshin y familia): ❌ **no hay ninguna implementación**. No aparece ni el término en el
  código fuente.
- **Edo Tensei** (`EdoTenseiService`): la lógica existe, pero **sólo es alcanzable desde el comando de
  depuración** `/dojutsushinobi`. No hay técnica ni ítem que la dispare.
- **Comerciante ninja:** no es una entidad propia; `ShinobiVillagerService` **convierte** aldeanos y mercaderes
  errantes vanilla al aparecer.

---

## 5. Modelos y recursos gráficos

**Ubicación:** `common/src/main/resources/assets/dojutsu/`.

### 5.1. Inventario

| Tipo | Cantidad | Notas |
|---|---|---|
| Modelos de ítem | **173** JSON | 128 jutsu + 24 learner + 6 ration_pills + 6 máscaras + 5 arrojadizas + 3 armas + jutsu.json/learner.json/ration_pills.json (padres) + contract + dna + ninja_debug |
| Texturas de ítem | **170** PNG | 128 jutsu + 24 learner + 8 máscara + 6 pills + 4 arrojadiza + 3 arma + contract + dna |
| Texturas de GUI | **14** PNG | Iconos de clan 32×32 (`textures/gui/clan/*`) — **los 14 en uso** por `NinjaCreationScreen` |
| Sonidos | **28** `.ogg` | `sounds.json` y `ModSounds` **coinciden exactamente**: 0 declarados sin fichero, 0 ficheros sin declarar, 0 registrados fuera de `sounds.json` |
| Modelos de entidad | **0** propios | Las 2 entidades vivas reutilizan mallas vanilla |
| Modelos de bloque | **0** | El mod no registra bloques |

### 5.2. Modelos faltantes

| Falta | Sistema afectado | Impacto |
|---|---|---|
| **7 modelos de Susanoo** (uno por personaje) | §2.4 | Silueta dibujada con partículas |
| **10 modelos de Bestia con Cola** | §2.6 | Todas se ven como un ravager |
| **Modelo/skin del shinobi** | §4 | Todos se ven como un zombi |
| **Texturas de ojos** (overlay del Sharingan/Byakugan/…) | §2.2 | El ojo activo **no se ve** |
| **HUD** | §9.12 | Barras y textos dibujados con primitivas |
| 24 ítems declarados en comercio/drop-crafting | §3.4 | Ofertas inertes |

### 5.3. Assets placeholder / temporales

| Asset | Naturaleza |
|---|---|
| `models/item/ninja_debug.json` | Placeholder de F0: `minecraft:item/nether_star` reciclado |
| `client/render/TailedBeastModel.java` | Reutiliza `ModelLayers.RAVAGER` — declarado provisional en su javadoc |
| `client/render/ShinobiRenderer.java` | Reutiliza el humanoide y la textura del zombi — declarado provisional |
| `ModParticles.CHAKRA_DEBUG` | Partícula placeholder **sin proveedor de render y sin uso** |
| `ModAttributes.MAX_CHAKRA` | Atributo placeholder registrado pero **nunca adjuntado a ninguna entidad** |

### 5.4. Recursos huérfanos y duplicados

Detección automatizada ejecutada sobre todo el árbol de assets:

| Recurso | Situación |
|---|---|
| `models/item/ration_pills/pills_6.json` + `textures/item/ration_pills/pills_6.png` | ⚠️ **Huérfanos**: el modelo padre sólo mapea `pills_1`…`pills_5` (5 cargas) |
| `models/item/ninja_debug.json` | Vinculado a un ítem sin uso |
| Los 14 iconos de clan | ✅ En uso (resolución dinámica por id, no aparecen en grep literal) |
| Todas las texturas de jutsu, learner, arma, arrojadiza, máscara | ✅ En uso |
| **Duplicados** | ❌ Ninguno detectado |

**Bug de asset detectado:** `models/item/mask_tobi_3.json` referencia dos veces la textura `#missing`
(elementos con `"texture": "#missing"` en las caras `south`), residuo de la exportación de Blockbench. Se
renderizará con la textura de "falta textura" en esas dos caras.

---

## 6. Animaciones

**Estado: ❌ EL MOD NO TIENE SISTEMA DE ANIMACIONES.**

Verificado exhaustivamente:

- No existe ninguna carpeta `assets/dojutsu/animations/`.
- No hay ninguna dependencia de librería de animación (GeckoLib, AzureLib, Blockbench animations) en
  `build.gradle` ni en `gradle.properties`.
- Las dos entidades vivas usan modelos vanilla y, por tanto, **heredan las animaciones del ravager y del
  zombi**. No hay animación propia ni de ataque, ni de casteo, ni de transformación.
- Ninguna técnica, habilidad ocular ni modo tiene animación de lanzamiento. El feedback visual es **partículas
  + efectos de poción**, exclusivamente.

**Animaciones pendientes (inferidas de los sistemas existentes, no declaradas en ningún sitio del repo):**
sellos de mano al castear, transformación de jinchūriki, apertura de puertas, invocación del Susanoo, ataque de
las bestias. **Animaciones huérfanas: ninguna, porque no hay ninguna.**

---

## 7. Comandos

**Registro:** `AdminCommands.register()` + 20 `*DebugCommand.register()` en
[`DojutsuMod.init()`](common/src/main/java/com/zamu/dojutsu/DojutsuMod.java).

### 7.1. Comando de administración

`/dojutsu` — **requiere permiso de op (nivel 2)**. Archivo:
[`core/command/AdminCommands.java`](common/src/main/java/com/zamu/dojutsu/core/command/AdminCommands.java).

| Sintaxis | Estado | Descripción |
|---|---|---|
| `/dojutsu reset_ninja [<jugador>]` | ✅ Funcional | Reemplaza `PlayerNinjaData` por una instancia nueva, retira learners y jutsus residuales, reinicia el onboarding y sincroniza |
| `/dojutsu give_six_paths` | ❌ **Stub** | Responde "pendiente (se completa en su fase)" |
| `/dojutsu reset_cooldowns` | ❌ **Stub** | ídem |
| `/dojutsu restore_all` | ❌ **Stub** | ídem |
| `/dojutsu set_test_ninja` | ❌ **Stub** | ídem |
| `/dojutsu spawn_shinobi_villager` | ❌ **Stub** | ídem |

Ejemplo: `/dojutsu reset_ninja Zamu`

### 7.2. Comandos de depuración (20)

> ⚠️ **NINGUNO de los 20 declara `.requires(...)`.** Están disponibles para **cualquier jugador**, sin permiso
> de op. Muchos conceden recursos, ojos, bestias o niveles arbitrarios. Ver §14.2.

| Comando | Archivo | Subcomandos |
|---|---|---|
| `/dojutsuninja` | `NinjaDataDebugCommand` | `get` · `set <phase>` |
| `/dojutsusched` | `SchedulerDebugCommand` | `count` · `in <ticks>` |
| `/dojutsuchakra` | `ChakraDebugCommand` | `get` · `consume <n>` · `restore <n>` · `full` · `setmax <n>` |
| `/dojutsuprogression` | `ProgressionDebugCommand` | `get` · `award <puntos>` · `spend <skill> <puntos>` · `xp <skill> <n>` · `ninjaxp <n>` |
| `/dojutsunatures` | `NatureDebugCommand` | `get` · `grant/revoke <nature>` · `aff <nature> <0-500>` · `roll <veces> [senju]` · `tmp grant/grant_kg/revoke` · `kg list/grant/revoke` |
| `/dojutsuclans` | `ClanDebugCommand` | `list` · `get` · `join <clan>` · `set <clan>` · `reset` · `clear` |
| `/dojutsuonboarding` | `OnboardingDebugCommand` | `state` · `clan <clan>` · `affinity <aff>` |
| `/dojutsuanchor` | `AnchorDebugCommand` | `spawn [life]` · `marker [life]` · `count` |
| `/dojutsujutsu` | `JutsuDebugCommand` | `list` · `get <id>` · `cast <id>` · `lastcast` |
| `/dojutsuweapon` | `WeaponDebugCommand` | `chakra_blade` · `kubikiribocho` · `samehada` · `bonus` |
| `/dojutsudojutsu` | `EyeDebugCommand` | `get` · `activate <ojo>` · `deactivate` · `use <familia> <nivel>` · `strain <0-100>` · `eternal <bool>` · `susanoo level <n>` · `susanoo use <n>` |
| `/dojutsusage` | `SageDebugCommand` | `get` · `charge <n>` · `become <tipo>` · `level <n>` · `use <n>` · `oiled <bool>` |
| `/dojutsuseal` | `CursedSealDebugCommand` | `get` · `inject` · `give <tipo>` · `remove` · `level <n>` · `deaths <n>` |
| `/dojutsugates` | `EightGatesDebugCommand` | `get` · `open` · `close` · `strain <n>` · `clearcd` |
| `/dojutsusoah` | `SoahDebugCommand` | `get` · `grant` · `store <n>` · `recharge` · `toggle` · `clearcd` |
| `/dojutsubeast` | `BeastDebugCommand` | `summon <bestia>` · `info <bestia>` · `killall` · `seal <bestia>` · `extract` · `registry` |
| `/dojutsuitem` | `ItemDebugCommand` | `get` · `dna give <tipo>` · `dna implant <tipo>` · `mask <tipo>` · `pills` |
| `/dojutsushinobi` | `ShinobiDebugCommand` | `spawn <amenaza>` · `info <amenaza>` · `killall` |
| `/dojutsustory` | `StoryDebugCommand` | `get` · `try` · `force` · `path yin\|yang\|none` · `otherhalf` · `sixpaths get/grant/lend/revoke/sagemode` · `reset` |
| `/dojutsugodtree` | `GodTreeDebugCommand` | `plant` · `get` · `advance [etapas]` · `bloom` |

**Sistemas sin comando de depuración:** invocaciones (`summoning`), arrojadizas/FTG (`throwing`), comercio
(`trading`), drop-crafting, agua (`waterwalk`), jinchūriki (la forma; el sellado sí, vía `/dojutsubeast`).

---

## 8. Configuración

**Archivo:** `config/dojutsu.properties` (se crea con los defaults si no existe).
**Código:** [`core/config/DojutsuConfig.java`](common/src/main/java/com/zamu/dojutsu/core/config/DojutsuConfig.java).
**Semántica:** booleanos como `1`/`0` (también acepta `true`/`false`); clave ausente → default; valor inválido
→ default con aviso en el log. **Se lee una sola vez al arrancar** (no hay recarga en caliente).

| Clave | Tipo | Default | Descripción | Estado |
|---|---|---|---|---|
| `AllowCustomTrader` | bool | `1` | Permite el comerciante ninja | ✅ Usado |
| `AllowShinobis` | bool | `1` | Permite los NPC shinobi | ✅ Usado |
| `AllowMultJinchuriki` | bool | `1` | Permite varios jinchūriki | ✅ Usado |
| `CheckHardcore` | bool | `0` | Detección de mundo hardcore | ✅ Usado (no destructiva) |
| `UseTraderSpawnRate` | bool | `0` | Usa el ritmo propio de aparición del mercader | ⚠️ Ver nota |
| `CustomMangekyoObtainable` | bool | `1` | Los Mangekyō de personaje son obtenibles | ✅ Usado |
| `GodTreeProtectsBuilds` | bool | `0` | **Opción del mod, no del datapack**: evita que el Árbol Divino arrase block entities del jugador. Default = fiel al original | ✅ Usado |
| `KillMobPoints` | int | `1` | Skill points por matar un mob | ✅ Usado |
| `KillPlayerPoints` | int | `3` | Skill points por matar un jugador | ✅ Usado |
| `KillBossPoints` | int | `200` | Skill points por matar un boss | ✅ Usado |
| `TraderSpawnRate` | int | `1` | Intentos de aparición del mercader por día | ⚠️ Ver nota |
| `TraderSpawnChance` | int | `1` | Probabilidad de aparición | ⚠️ Ver nota |
| `NatureEnergyCharge` | int | `1` | Multiplicador de carga de energía natural | ✅ Usado |

> ⚠️ **Nota sobre las tres claves de trader:** `TraderSpawnPolicy`, la clase `SavedData` que implementa ese
> ritmo de aparición, **no la instancia ni la consulta nadie**. Ver §12.

### 8.1. Configuración "oculta" (no expuesta al usuario)

Todo el balance vive en **constantes de dominio**, no en config:
`SusanooCostTable`, `SageModeTable`, `NaturalEnergyTable`, `CursedSealTable`, `EightGatesTable`, `SoahTable`,
`SummonTable`, `ThrowingTable`, `FtgTable`, `DnaTable`, `PillTable`, `WaterWalkTable`, `GodTreeTable`,
`StoryTable`, `SixPathsTable`, `BondTable`, `SealingTable`, `JinchurikiFormTable`, `TradingTable`,
`CraftingTable`, `BeastStats`, `ThreatLevel`.

**Dataset de referencia no cargado por código:** `data/dojutsu/balance/xp_sources.json` — auditoría de economía
(RA-4). Contiene **12 valores marcados `TBD_datapack` sin resolver** (`ninja_xp_per_autolevel`, `learn_jutsu`, y
los requisitos/xp de 10 hitos de desbloqueo).

---

## 9. Sistemas del mod

Toda la infraestructura se cablea en un único sitio:
[`core/DojutsuLifecycle.init()`](common/src/main/java/com/zamu/dojutsu/core/DojutsuLifecycle.java).

### 9.1. Motor por tick (`PlayerTickPipeline`)

12 fases declaradas en [`TickPhase`](common/src/main/java/com/zamu/dojutsu/core/TickPhase.java). **18 sistemas
registrados**, repartidos así:

| Fase | Sistemas registrados |
|---|---|
| `CHAKRA` | `RegenService`, `PillService`, `ContractService`, `FtgMarkerService` |
| `STRAIN` | `EyeStrainService`, `EightGatesService` |
| `WATER_WALK` | `WaterWalkService` |
| `DOJUTSU` | `AwakeningService`, `EvolutionService`, `SusanooService`, `ChakraVisionService`, `TenseiganChakraModeService`, `NaturalEnergyChargingService`, `SageService`, `CursedSealService`, `SoahSealService`, `FormService` |
| `UNLOCKS` | `ClanJutsuUnlockService` |
| `JUTSU_TIMERS`, `SAGE`, `EMPOWERMENT`, `JINCHURIKI`, `THROWING`, `SUMMONING`, `PROGRESSION` | ⚠️ **VACÍAS** |

⚠️ **7 de las 12 fases están vacías** y varios sistemas están declarados en una fase que no les corresponde
semánticamente (`SageService` en `DOJUTSU` en vez de `SAGE`; `CursedSealService`/`SoahSealService` en `DOJUTSU`
en vez de `EMPOWERMENT`; `FormService` en `DOJUTSU` en vez de `JINCHURIKI`; `ContractService` en `CHAKRA` en vez
de `SUMMONING`; `FtgMarkerService` en `CHAKRA` en vez de `THROWING`). Ver §14.3.

### 9.2. Chakra — ✅ Completo

`ChakraService` es la **única puerta de escritura**. `RegenService` corre primero en el pipeline (tabla del
datapack: base 120 ticks/+3, con factores de clan Uzumaki/Ōtsutsuki, ADN y sabio). Comer repone +50 (evento
nativo NeoForge + **mixin en Fabric**); dormir la noche completa lo llena; morir deja ½ del máximo.
Chakra natural separado (`naturalChakra`/`maxNaturalChakra`) para el KG Jūgo.

### 9.3. Progresión — ✅ Completo (excepto la entrega de desbloqueos)

- **Skill points** por kill (`SkillPointService`, config 1/3/200).
- **7 habilidades** ([`Skill`](common/src/main/java/com/zamu/dojutsu/data/state/Skill.java)): taijutsu,
  genjutsu, medical, fuinjutsu, kenjutsu (auto-nivelan) + senjutsu, cc (sólo por puntos).
- **8 naturalezas** ([`NatureType`](common/src/main/java/com/zamu/dojutsu/domain/nature/NatureType.java)):
  fire 25, wind 20, lightning 25, earth 20, water 20, yin 30, yang 30, yin_yang 100 (el número es el ninja_xp
  que concede obtenerla).
- **`XpApi`** es la única puerta de XP; auto-nivela multi-nivel de una vez, cap 500.
- **12 rangos** derivados de `ninjaXp` (Academy Student 0 → God 40 000). Cosmético, no gatea nada.
- **23 desbloqueos** data-driven en `unlocks/progression.json`. ⚠️ Sólo marcan el id (§1.5).

### 9.4. Naturalezas y KG — ✅ Completo

`NatureService` es la única puerta del perfil elemental. `KekkeiGenkaiService` + registro data-driven (18).
`TemporaryGrantService` gestiona préstamos por fuente (usado por bijū y Seis Caminos).
`NatureRoll` implementa la tirada del onboarding con los pesos exactos de las loot tables.

### 9.5. Clanes — ✅ Funcional (parcial en entregas)

14 clanes data-driven. `ClanService.join` aplica el bonus una sola vez; `reset` restablece los 4 sub-estados
atómicamente. `ClanTeamService` crea un team namespaceado por clan para el color del nametag.

| Clan | Color | Chakra máx. | Bonus | Dōjutsu potencial |
|---|---|---|---|---|
| aburame | dark_purple | 100 | — | — |
| akimichi | dark_red | 110 | yang | — |
| chinoike | red | 140 | yin, genjutsu +20, xp 60 | **ketsuryugan** |
| hatake | gray | 120 | lightning +25, tai/ken/gen, xp 85 | — |
| hyuga | gray | 120 | taijutsu +10, xp 20 | **byakugan** |
| kaguya | red | 100 | shikotsumyaku, kenjutsu +10 | — |
| lee | dark_green | **50** | taijutsu +30, xp 90, **`skipsAffinity`** | — |
| namikaze | yellow | 140 | KG swift, lightning +25, tai/fuin, xp 65 | — |
| nara | black | 100 | yin +10, xp 10 | — |
| otsutsuki | white | **250** | `regenFactor: clan_otsutsuki` | **byakugan o sharingan** (50/50) |
| sarutobi | gold | 180 | fire (+50 % wind aleatorio), fire +10, xp 30 | — |
| senju | green | 150 | yang, medical +20, xp 40 | — |
| uchiha | dark_red | 150 | fire +10, genjutsu +10, xp 30 | **sharingan** |
| uzumaki | gold | **500** | fuinjutsu +30, xp 15, `regenFactor: clan_uzumaki` | — |

⚠️ `startingJutsu` y `startingItems` **están declarados y no se entregan** (§1.5, §3.4).

### 9.6. Onboarding — ✅ Completo

`OnboardingController` (máquina de estados en servidor) + `NinjaCreationScreen` (GUI real con los 14 iconos de
clan). Elección de clan → afinidad (salvo Lee, `skipsAffinity`) → tirada de KG. Reabre la pantalla mientras la
fase sea de selección.

### 9.7. Cooldowns — ✅ Completo

Por instancia, guardados en `JutsuKnowledge` con el **tick absoluto de mundo**. Sin barrido por tick. Las
habilidades oculares tienen su propia puerta de cooldown por (jugador, habilidad) en `EyeAbilityService`.

### 9.8. Modos activables — ✅ Completo

`ToggleableModeService` (arnés compartido): hospeda un `ToggleableMode` por jugador, lo tickea, detecta la
caída por coste impagable, emite `ModeToggledEvent` y sincroniza. **6 consumidores**: Susanoo, Tenseigan Chakra
Mode, Sage, Cursed Seal, Eight Gates, SOAH.

### 9.9. Fatiga (strain) — ✅ Completo

Dos medidores separados con bandas de aviso unificadas (`StrainWarnings`): **fatiga ocular** (`EyeStrainService`,
95/98/100) y **fatiga corporal** (`EightGatesService`, 95/98/100). Ambas se resetean al dormir.

### 9.10. Chakra Vision — ✅ Completo

Glow nativo de vanilla + equipos de scoreboard para el color. Reescaneo cada 10 ticks, radio 30. Clasificación
**estática** en [`ChakraVisionRelations`](common/src/main/java/com/zamu/dojutsu/domain/vision/ChakraVisionRelations.java):
`SPECIAL` (villager, wolf, cat, ocelot, fox, donkey, horse…), `FRIENDLY` (chicken, cow, pig, sheep…),
`NEUTRAL` (cave_spider, dolphin, enderman, llama, panda…), `HOSTILE` (blaze, creeper, drowned, ghast…).
Disponibilidad data-driven; el Sharingan sólo la da con uso ≥ 216 000. Tecla **C** la silencia.

### 9.11. Red y sincronización — ✅ Completo

**20 payloads** sobre el puerto `NetworkService` (nativo por loader; **no** se usa el `NetworkManager` de
Architectury por un bug S2C conocido en Fabric). Sólo lo observable cruza la red (`ObservableField` +
`DirtyTracker`): snapshot completo en join/respawn/cambio de dimensión, delta en cada cambio.
**Todo payload C2S se valida en servidor** (emisor, ids por nombre, rangos, rate-limit).

### 9.12. Cliente — ✅ Funcional / ⚠️ HUD con primitivas

- **11 keybinds**: M (menú), V (cast), N (menú ocular), R (habilidad ocular), C (chakra vision), G (sabio),
  B (sello maldito), H (Ocho Puertas), J (SOAH), K (jinchūriki), Y (FTG). Todos remapeables.
- **3 overlays**: chakra, rango, cooldown del jutsu equipado. Dibujados con primitivas, sin texturas de HUD.
- **5 pantallas**: `NinjaMenuScreen`, `NinjaCreationScreen`, `AbilitySelectScreen`, `DialogueScreen` +
  sus openers.
- **5 cachés read-only** en cliente, limpiadas al desconectar.

### 9.13. Persistencia y migración — ⚠️ Funcional pero sin usar

Attachment `player_ninja_data`, persistente y con `copyOnDeath`. **16 sub-estados** enchufados en
`PlayerNinjaData`: onboarding, chakra, progression, natures, clan, jutsu, eye, susanoo, vision, empowerment,
sage, jinchuriki, items, throwing, story, sixPaths.

⚠️ **`CURRENT_DATA_VERSION = 1` y la cadena de migración sólo contiene un paso identidad (v0→v1)**. Se han
añadido 16 sub-estados a lo largo de F1–F9 sin subir la versión. Funciona porque los codecs son opcionales
(un save antiguo recibe los `DEFAULT`), pero la maquinaria de migración está **construida y sin estrenar**.

### 9.14. Mundo y narrativa — ✅ Completo

| Sistema | Datos clave |
|---|---|
| **Encuentros con los Sabios** (`SageEncounterService`) | Cola global de 3600 s; requiere rango 6 (Kage); probabilidad 5 % → 8 % → 10 %; 2 intentos al máximo. Elección de camino Yin/Yang/Ninguno (+100 ninja_xp) por `DialogueScreen` |
| **Árbol Divino** (`GodTreeService`) | Plantar cuesta 1000 de chakra, cooldown 300 t; 5 etapas de 6000 t (30 000 t totales ≈ 25 min); al florecer concede el **Rinne Sharingan**. Usa 6 estructuras `.nbt` |
| **Caminar sobre el agua** (`WaterWalkService`) | Exige 300 de chakra máximo y 2 de chakra actual; cuesta 1 cada 3 ticks. Un golpe te tira al agua |
| **Comercio** (`ShinobiVillagerService`) | Conversión **por evento** al aparecer el aldeano/mercader, no por barrido. 2 catálogos, 24 ofertas (15 inertes) |
| **Drop-crafting** (`DropCraftingService`) | Barrido de entidades ítem en `TickEvent.SERVER_POST`; 16 combinaciones (9 inertes) |
| **Recetas de mesa** (`CraftingGateService`) | Puerta de fuinjutsu del contrato con devolución de ingredientes |

### 9.15. Eventos de dominio

`DomainEvents` (bus compartido). **11 eventos**: `BeastSealedEvent`, `EyeAwakenedEvent`, `EyeEvolvedEvent`,
`JutsuCastEvent`, `KekkeiGenkaiGrantedEvent`, `ModeToggledEvent`, `NatureGrantedEvent`, `NinjaXpChangedEvent`,
`SkillLevelChangedEvent` (+ `DomainEvent` y `DomainEventBus`).
Principio arquitectónico declarado y respetado: **reactivo, nunca polling**.

### 9.16. Sistema de partículas

**No existe un sistema propio.** Cada servicio llama directamente a `level.sendParticles(...)` con **26 tipos
de partícula vanilla** distintos, repartidos por **41 archivos**. La única partícula propia registrada
(`dojutsu:chakra_debug`) **no tiene proveedor de render ni un solo uso**.

---

## 10. Estructura del proyecto

### 10.1. Stack

| Elemento | Versión |
|---|---|
| Minecraft | 1.21.1 |
| Java | 21 |
| Gradle wrapper | 8.10.2 |
| Loader | Architectury multiloader |
| architectury API | 13.0.8 |
| fabric-loader / fabric-api | 0.16.10 / 0.116.13+1.21.1 |
| neoforge | 21.1.235 |
| Mappings | Mojmap |
| JUnit | 5.10.2 |
| Mod id / namespace | `dojutsu` / `com.zamu.dojutsu` |
| Versión del mod | 0.1.0 |

### 10.2. Módulos

```
common/    486 clases — toda la lógica; nunca importa clases de loader
neoforge/    8 clases — adaptador (loom.platform=neoforge)
fabric/     10 clases — adaptador (+1 mixin)
docs/       10 documentos
```

### 10.3. Arquitectura

**Clean/Hexagonal sobre Architectury.** Toda diferencia de loader se accede tras un **puerto** en
`com.zamu.dojutsu.platform`, resuelto por `ServiceLoader` a través de la fachada `PlatformServices`.

**Puertos existentes (3):** `RegistrarService`, `AttachmentService`, `NetworkService`.
**Puerto diferido:** `AttributeService` (adjuntar atributos a `Player`) — nunca se abrió; kenjutsu lo evitó con
el data component `ItemAttributeModifiers` vanilla.

**Capas:**
- `domain.*` — **puro, sin Minecraft en el classpath** (27 subpaquetes). Todo lo testeable vive aquí.
- `data.*` — codecs/NBT: `PlayerNinjaData` + 16 sub-estados inmutables + migración.
- `core.*` — motor: tick pipeline, dispatcher, scheduler, ciclo de vida, config, identidad, comandos.
- Módulos de gameplay: `chakra`, `progression`, `natures`, `clans`, `items`, `jutsu`, `dojutsu`, `sage`,
  `empowerment`, `bijuu`, `summoning`, `throwing`, `trading`, `story`, `sixpaths`, `world`, `ai`, `entity`,
  `network`, `client`, `registry`, `util`, `debug`.

### 10.4. Convenciones verificadas

| Convención | Cumplimiento |
|---|---|
| `common` nunca importa clases de loader | ✅ |
| Registros = clases `Mod<Tipo>` con `PlatformServices` | ✅ (7 clases `Mod*`) |
| Ids con `DojutsuMod.id(path)` | ✅ |
| Comentarios y javadoc en **español** | ✅ |
| Logs sin acentos | ✅ (verificado por muestreo) |
| Sub-estado enchufable (value object + `DEFAULT` + `CODEC`) | ✅ (16/16) |
| Estado inmutable hasta el fondo | ✅ (`LeveledStat` es `record`, `withXpAdded` devuelve copia) |
| Toda `EntityType` tiene renderer | ✅ (5/5, en ambos loaders) |
| El cliente nunca decide gameplay | ✅ (20 payloads, todos los C2S validados en servidor) |

### 10.5. Patrones establecidos y reutilizados

1. **Sub-estado enchufable** — 16 usos.
2. **Carga data-driven por reload listener** — 11 listeners registrados (`unlocks`, `kekkei_genkai`,
   `nature_roll`, `clans`, `jutsu`, `learner_sequences`, `eyes`, `contracts`, `trades`, `drop_crafting`,
   `data_reload`).
3. **Servicio puro + glue de Minecraft** — todos los modos y servicios de fase.
4. **Evento loader-nativo con "push" desde el adaptador** — comer, dormir, daño.
5. **Acción C2S validada en servidor** — 20 payloads.
6. **Declarar contenido que aún no existe y contarlo en el log** — comercio y drop-crafting.

---

## 11. Código pendiente

### 11.1. TODOs reales en código de producción (5)

| Ubicación | Contenido | Impacto |
|---|---|---|
| [`progression/UnlockService.java:68`](common/src/main/java/com/zamu/dojutsu/progression/UnlockService.java) | `// TODO F4: entregar el item de jutsu / habilitar el contenido real del desbloqueo` | 🔴 **Alto** — 23 desbloqueos no entregan nada |
| [`clans/ClanJutsuUnlockService.java:52`](common/src/main/java/com/zamu/dojutsu/clans/ClanJutsuUnlockService.java) | `// TODO F4/DJT-075: entregar el siguiente jutsu de la secuencia del clan y resetear el contador` | 🔴 **Alto** — 4 clanes acumulan un contador que no produce nada |
| [`clans/ClanService.java:165`](common/src/main/java/com/zamu/dojutsu/clans/ClanService.java) | `// TODO F4 (jutsus iniciales) / F8 (items iniciales)` | 🟠 **Medio** — 6 clanes declaran entregas que no ocurren |
| [`core/lifecycle/RespawnHandler.java:41`](common/src/main/java/com/zamu/dojutsu/core/lifecycle/RespawnHandler.java) | `// TODO F7+: formas de jinchuriki, velocidad base (namikaze) y bonus de taijutsu` | 🟠 **Medio** — F7 está cerrada; la forma no se restaura al respawnear |
| [`core/DojutsuLifecycle.java:109-110`](common/src/main/java/com/zamu/dojutsu/core/DojutsuLifecycle.java) | `loadConfig(); // TODO DJT-025` y `registerNetworkPayloads(); // TODO DJT-027` | 🟢 **Nulo** — comentarios obsoletos; ambos tickets están hechos |

**FIXME / XXX / HACK: ninguno.**

### 11.2. Métodos stub

- 5 subcomandos de `/dojutsu` que sólo imprimen "pendiente" (§7.1).
- `MigrationChain`: paso `UnaryIdentity.INSTANCE` (v0→v1) sin cambios estructurales.

### 11.3. Banderas y rutas desactivadas

| Bandera | Ubicación | Efecto |
|---|---|---|
| `RINNEGAN_ROUTE_DISABLED = true` | `domain/sixpaths/SixPathsTable.java:62` | La vía del Rinnegan al chakra de los Seis Caminos está desactivada |
| `"implemented": false` | `data/dojutsu/eyes/naka.json` | El ojo `naka` carga pero no es jugable |

### 11.4. Clases incompletas / sistemas experimentales

- `EdoTenseiService` — completo pero **sin disparador de gameplay**.
- `TraderSpawnPolicy` — completo pero **nunca instanciado** (§12).
- `ModParticles` / `ModAttributes` — registros de F0 con una entrada placeholder cada uno, sin consumidores.

### 11.5. Código comentado

No se ha encontrado ningún bloque de código comentado de tamaño relevante. Los comentarios del repo son
javadoc y explicaciones de decisión, no código muerto.

---

## 12. Contenido sin uso

### 12.1. Código muerto confirmado

| Elemento | Evidencia |
|---|---|
| **`trading/TraderSpawnPolicy.java`** | Sólo se referencia a sí misma. La única mención externa es una **frase de javadoc** en `GodTreeRegistry`. Nadie llama a `TraderSpawnPolicy.of(server)`. Las claves `UseTraderSpawnRate`, `TraderSpawnRate` y `TraderSpawnChance` de la config **no tienen consumidor real** |
| **`ai/EdoTenseiService.java`** | Sólo lo invoca `ShinobiDebugCommand`. Inalcanzable en partida normal |
| **`registry/ModParticles.CHAKRA_DEBUG`** | Registrado; sin proveedor de render y sin ninguna llamada |
| **`registry/ModAttributes.MAX_CHAKRA`** | Registrado; nunca adjuntado a ninguna entidad. Además **le falta la traducción** `attribute.dojutsu.max_chakra` |
| **`registry/ModItems.NINJA_DEBUG`** | Ítem placeholder de F0; ningún sistema lo usa ni lo entrega |
| **`entity/MarkerEntity`** | Registrada y funcional, pero **sólo la instancia `AnchorDebugCommand`**. Ningún sistema de gameplay la usa |

### 12.2. Assets huérfanos

| Asset | Situación |
|---|---|
| `models/item/ration_pills/pills_6.json` | No lo referencia el modelo padre (5 cargas, no 6) |
| `textures/item/ration_pills/pills_6.png` | Sólo lo usa el modelo huérfano anterior |

### 12.3. Sonidos registrados y sin cablear (20 de 28)

Sólo **8** se reproducen en algún punto del código:
`dojutsu.byakugan.activate`, `dojutsu.sharingan.mangekyo.activate`, `dojutsu.tenseigan.activate`,
`dojutsu.ketsuryugan.activate`, `dojutsu.rinnegan.activate`, `dojutsu.rinne_sharingan.activate`
(vía `EyeSounds`), `medical_ninjutsu.healing` (`HealEffect`) y `jutsu.summoning` (`SummonService`).

Los **20 restantes** están registrados, tienen fichero `.ogg` y no suena ninguno:

```
dojutsu.jougan.activate            dojutsu.isshiki.activate          dojutsu.isshiki.reappear
dojutsu.sharingan.blind            dojutsu.sharingan.genjutsu        dojutsu.sharingan.mangekyo.susanoo.activate_1
dojutsu.rinnegan.amenotejikara     dojutsu.rinnegan.bansho_tenin     dojutsu.rinne_sharingan.amenominaka
jutsu.earth   jutsu.wind   jutsu.lightning   jutsu.wood   jutsu.chidori   jutsu.rasengan
jutsu.rasenshuriken   jutsu.frog_call   jutsu.flute_song   jutsu.white_rage_technique   misc.ear_ringing
```

Nota: el javadoc de `ModSounds` explica que 8 de los 28 tampoco los llamaba el datapack. **Los otros 12 sí los
llamaba el datapack y aquí no se han cableado** — en particular `amenotejikara`, `bansho_tenin` y
`susanoo.activate_1`, que corresponden a habilidades **ya implementadas**.

### 12.4. Contenido de datos declarado sin destino

| Categoría | Cantidad | Detalle |
|---|---|---|
| Ofertas de comercio inertes | 15 de 24 | §3.4 |
| Combinaciones de drop-crafting inertes | 9 de 16 | Huevos `spawn_egg_*` |
| Jutsus sin vía de obtención | 26 de 128 | §1.5 |
| Jutsus declarados en secuencia sin JSON | 4 | Los `*_cloak` |
| Jutsus de clan sin JSON | 2 | `dance_of_the_camellia`, `shadow_paralysis` |
| Ítems iniciales de clan sin ítem | 2 | `three_coloured_pills`, `white_light_chakra_sabre` |
| Fases de tick vacías | 7 de 12 | §9.1 |

### 12.5. Traducciones faltantes (3)

| Clave usada en código | Archivo | Consecuencia |
|---|---|---|
| `entity.dojutsu.shinobi_villager` | `trading/ShinobiVillagerService.java:76` | 🔴 El comerciante ninja muestra **la clave en crudo** como nombre |
| `attribute.dojutsu.max_chakra` | `registry/ModAttributes.java` | 🟢 Sin impacto (el atributo no se usa) |
| `story.dojutsu.chose_<path>` | `story/SagePathService.java:95` | ⚠️ Concatenación dinámica; **las variantes concretas sí existen** en el lang |

**Sólo existe `en_us.json`.** No hay traducción al español pese a que todo el código y el javadoc lo están.

---

## 13. Estado general

### 13.1. Completamente terminado ✅

| Área | Evidencia |
|---|---|
| Andamiaje multiloader y los 3 puertos de plataforma | Build verde en ambos loaders |
| Dominio puro | 27 subpaquetes, 912 tests verdes |
| Chakra y regeneración | Única puerta de escritura, comer/dormir/morir cableados |
| Progresión (puntos, XP, niveles, rangos) | Bucle usar→XP→nivel cerrado |
| Naturalezas, Kekkei Genkai, clanes, onboarding | Data-driven + GUI real |
| Motor de casteo y las 7 familias de efecto | Único camino de cast, compartido por jugador y NPC |
| Kenjutsu (armas con bonus reversible + Samehada) | `ItemAttributeModifiers` vanilla |
| Sistema de ojos completo (grafo, despertar, evolución, fatiga, menú, 30 habilidades) | 15 JSON + 30 clases, consistencia verificada al 100 % |
| Los 6 modos activables sobre un arnés común | Susanoo, Tenseigan, Sage, Sello, Puertas, SOAH |
| Bijū y jinchūriki (sellado, vínculo, forma, Baryon) | Entidad jefe con bossbar nativa |
| Invocaciones, arrojadizas, FTG | 27 contratos, 5 arrojadizas |
| IA de shinobi (6 niveles) y de bestias | Castean por el mismo pipeline |
| Comercio, drop-crafting, recetas | Por evento, sin barridos globales |
| Narrativa (Sabios, caminos, Seis Caminos) y Árbol Divino | Cola global + `SavedData` |
| Persistencia, sincronización y cliente | 16 sub-estados, 20 payloads, 5 pantallas, 11 keybinds |
| Sonidos: `sounds.json` ↔ ficheros ↔ `ModSounds` | Coincidencia exacta, 28/28/28 |

### 13.2. Parcialmente implementado ⚠️

| Área | Qué falta |
|---|---|
| **Entrega de contenido desbloqueado** | 3 TODOs (`UnlockService`, `ClanJutsuUnlockService`, `ClanService`) → 26 jutsus inobtenibles |
| **Técnicas de bestia** | 10 declaradas en el enum, **1 implementada** |
| **Variedad de la IA shinobi** | Los 6 niveles sólo usan jutsus de fuego |
| **Catálogos de comercio y drop-crafting** | 15/24 ofertas y 9/16 combinaciones inertes |
| **Restauración al respawn** | TODO F7+ pendiente pese a que F7 está cerrada |
| **Migración de datos** | Maquinaria completa, versión nunca subida de 1 |
| **Balance auditado** | 12 `TBD_datapack` sin resolver en `xp_sources.json` |
| **Comandos de administración** | 5 de 6 son stubs |
| **Seis Caminos** | Ruta del Rinnegan desactivada por bandera |

### 13.3. Requiere modelos 🎨

| Prioridad | Elemento | Cantidad |
|---|---|---|
| Alta | Bestias con Cola (hoy: ravager) | 10 |
| Alta | Susanoo por personaje (hoy: partículas) | 7 |
| Media | Shinobi NPC (hoy: zombi) | 1 + variantes por aldea |
| Baja | Ítems del comercio no portados | hasta 24 |

### 13.4. Requiere texturas 🎨

| Prioridad | Elemento |
|---|---|
| Alta | **Overlay de ojos** — el ojo activo no es visible de ninguna manera |
| Alta | Texturas de las bestias y del Susanoo (acompañan a los modelos) |
| Media | HUD (hoy sólo primitivas) |
| Media | Skin del shinobi |
| Baja | Bandanas / protectores |
| Trivial | Corregir `#missing` en `mask_tobi_3.json` |

### 13.5. Requiere programación adicional 💻

Ordenado por impacto en la jugabilidad:

1. 🔴 **Entrega de jutsus por desbloqueo y por clan** (3 TODOs). Sin esto, el 20 % del catálogo es inalcanzable.
2. 🔴 **Los 4 `*_cloak`** — cierran las cuatro secuencias de learner más largas.
3. 🟠 **Cablear los 12 sonidos que el datapack sí usaba** (amenotejikara, bansho_tenin, susanoo, los 4
   elementales, chidori/rasengan…). El mod es prácticamente mudo.
4. 🟠 **Las 9 técnicas de bestia restantes.**
5. 🟠 **Restauración de estado al respawn** (forma de jinchūriki, velocidad Namikaze, bonus de taijutsu).
6. 🟠 **`entity.dojutsu.shinobi_villager`** en el lang (bug visible de una línea).
7. 🟡 **Rutas elementales para la IA shinobi.**
8. 🟡 **Instanciar `TraderSpawnPolicy`** o retirar sus 3 claves de config.
9. 🟡 **Completar o retirar los 5 stubs de `/dojutsu`.**
10. 🟡 **Resolver los 12 `TBD_datapack`.**
11. 🟢 **Sistema de partículas propio** (hoy 26 tipos vanilla dispersos en 41 archivos).
12. 🟢 **Traducción `es_es`.**

### 13.6. Debería refactorizarse ♻️

Ver §14.3 para la justificación completa.

1. **Fases de tick** — 7 de 12 vacías y 5 sistemas en la fase equivocada.
2. **Permisos de los comandos de depuración** — 20 comandos sin `.requires()`.
3. **Emisión de partículas** — sin abstracción; duplicación entre las 30 habilidades oculares.
4. **`ModParticles` / `ModAttributes` / `NINJA_DEBUG`** — placeholders de F0 que ya no aportan.
5. **`MarkerEntity`** — registrada, mantenida y sin consumidor real.

---

## 14. Observaciones y oportunidades de mejora

### 14.1. Inconsistencias detectadas entre la documentación y el código

Esto no es una crítica menor: los documentos del repo se usan como punto de partida en cada sesión de trabajo,
y **varios de sus datos son falsos hoy**.

| Documento | Afirmación | Realidad verificada |
|---|---|---|
| `CLAUDE.md` | "F5 EN CURSO… Siguiente: DJT-140 (sellos malditos)" | F9 está cerrada; DJT-140 hecho hace días. `CLAUDE.md` se quedó en 2026-07-27 |
| `CLAUDE.md` §Módulos | "Aún no existen: `throwing`, `sage`, `empowerment`, `bijuu`, el resto de `items`, `summoning`, `ai`, `trading`, `story`, `world`, `api`" | **Todos existen salvo `api`** |
| `CLAUDE.md` §Estado | "651→717 tests" | **912 tests** |
| `docs/current_state.md` §2 | "F5–F10: No iniciada" | F5–F9 completadas; la propia cabecera del documento las describe |
| `docs/current_state.md` §5 | "Módulos de contenido pendientes: `jutsu`, `throwing`… `dojutsu`, `sage`…" | Obsoleto por completo |
| `docs/current_state.md` §6 | "299 clases Java: 217 en common (+66 de test)" | **504 clases**: 384 main + 102 test en `common`, 10 fabric, 8 neoforge |
| `docs/current_state.md` (cabecera) | "855 tests verdes" | **912** |
| `docs/known_issues.md` | "Assets parciales… ojos (52 texturas), Susanoo (56 modelos), bijuu (24)" | Cifras plausibles pero **no verificables desde este repo** (los recursos originales están fuera). Se conservan como estimación, no como hecho |

**Recomendación:** el cuerpo de `current_state.md` (§2 a §6) está fosilizado en F4 mientras la cabecera crece
como changelog. O se reescribe el cuerpo, o se convierte el documento en changelog puro y **este documento pasa
a ser la referencia de estado**.

### 14.2. 🔴 Riesgos que conviene atender antes que cualquier funcionalidad nueva

#### a) El trabajo de F1–F9 no está en git

```
Último commit: 3e61aae "docs: handoff y estado tras Fase 0" — 2026-07-12
Modificados/borrados rastreados: 26 archivos
Sin rastrear: 108 entradas (incluye directorios completos con ~490 clases)
```

**Dieciocho días y nueve fases de trabajo viven exclusivamente en el working tree.** Un `git clean`, un fallo de
disco o un `checkout` accidental destruyen el proyecto entero. `known_issues.md` ya lo anotaba en F4 y **ha
seguido creciendo desde entonces**.

*Recomendación:* commitear **hoy**, aunque sea en un solo commit "F1–F9". La granularidad se puede mejorar
después; la pérdida no se puede deshacer. Añadir `common/logs/` al `.gitignore` (hoy aparece como sin rastrear).

#### b) Los 20 comandos de depuración no exigen permisos

Ninguno declara `.requires(source -> source.hasPermission(...))`. En un servidor con el mod instalado,
**cualquier jugador** puede ejecutar:

- `/dojutsuchakra setmax 999999` · `/dojutsuprogression ninjaxp 999999`
- `/dojutsudojutsu activate rinne_sharingan` + `eternal true`
- `/dojutsubeast summon nine_tails` + `seal nine_tails`
- `/dojutsunatures kg grant <cualquiera>` · `/dojutsuitem dna implant <cualquiera>`

`/dojutsu` sí exige nivel 2. La asimetría parece un descuido, no una decisión.

*Recomendación:* añadir un `.requires(...)` compartido a los 20 (una línea por comando, o un helper común), y
plantear si deberían compilarse condicionalmente fuera de las builds de release.

#### c) El 20 % del catálogo de jutsus es inalcanzable

Tres TODOs con el mismo origen (la entrega del ítem de jutsu) dejan sin vía de obtención a **todo el kenjutsu,
taijutsu, genjutsu, ninjutsu médico, fuinjutsu, los de Aburame y los universales**. El contenido está escrito,
probado y con assets; sólo falta la línea que entrega el ítem.

*Recomendación:* es probablemente el ticket con mejor relación impacto/esfuerzo del backlog. `JutsuItem.createStack(id)`
ya existe y `JutsuLearningService` ya demuestra el patrón completo (dar ítem + `withKnown`).

### 14.3. 🟠 Decisiones de diseño cuestionables

#### a) `TickPhase` es un contrato que no se está honrando

Se declararon 12 fases con un orden semántico explícito y documentado (chakra primero para que la regeneración
preceda a los consumos, etc.). Hoy **7 están vacías** y 5 sistemas están archivados en la fase equivocada:

| Sistema | Fase declarada | Fase que le correspondería |
|---|---|---|
| `SageService` | `DOJUTSU` | `SAGE` |
| `CursedSealService` | `DOJUTSU` | `EMPOWERMENT` |
| `SoahSealService` | `DOJUTSU` | `EMPOWERMENT` |
| `FormService` | `DOJUTSU` | `JINCHURIKI` |
| `ContractService` | `CHAKRA` | `SUMMONING` |
| `FtgMarkerService` | `CHAKRA` | `THROWING` |

**Por qué importa:** el valor del pipeline por fases es garantizar el orden entre sistemas que interactúan. Con
9 sistemas amontonados en `DOJUTSU`, su orden relativo depende del **orden de registro en `DojutsuLifecycle`**,
no de la fase — es decir, del azar de la fase de desarrollo en que se añadió cada uno. Un bug de orden aquí
sería sutil y difícil de diagnosticar (p. ej. el sello maldito cobrando antes de que el SOAH devuelva chakra).

*Recomendación:* reasignar los 6 sistemas a su fase y eliminar `JUTSU_TIMERS` y `PROGRESSION` si nadie las va a
usar (los cooldowns son por tick absoluto y la nivelación es reactiva — ambas fases nacieron de un diseño que se
superó). Coste: ~10 líneas. Beneficio: el contrato vuelve a significar algo.

#### b) El mod es casi mudo teniendo los sonidos ya portados

28 sonidos registrados, con fichero, declarados en `sounds.json`, verificados… y **8 en uso**. Entre los no
cableados hay tres que corresponden a habilidades **ya implementadas y jugables**: `amenotejikara`,
`bansho_tenin` y `susanoo.activate_1`. Y los cuatro elementales (`jutsu.earth/wind/lightning/wood`) que el
datapack sí reproducía al castear.

**Por qué importa:** el trabajo caro (conseguir, convertir, declarar y registrar los audios) ya está hecho. Lo
que falta es una línea `level.playSound(...)` en cada sitio. El retorno percibido por el jugador es
desproporcionadamente alto respecto al esfuerzo.

*Recomendación:* un ticket "cablear sonidos" que (1) añada el sonido a las 3 habilidades que ya lo tienen
nominalmente, y (2) meta un campo opcional `sound` en `effectParams` o en `JutsuEntry` para que las técnicas
elementales suenen sin tocar código por técnica.

#### c) No hay abstracción de partículas y sí un registro de partículas vacío

`ModParticles` registra `chakra_debug`, que nadie usa y que no tiene proveedor de render. Mientras tanto, 41
archivos llaman directamente a `sendParticles` con 26 tipos vanilla. Las 30 habilidades oculares repiten
patrones de emisión casi idénticos (esfera, rayo, explosión) sin compartir helper — `EyeAbilityEffects` existe
pero cubre efectos de estado, no visuales.

*Recomendación:* o se crea un `ParticleFx` con 5–6 formas reutilizables (esfera, haz, anillo, columna, estela) y
se migra, o se acepta la dispersión y se **borra `ModParticles`** para no sostener un registro vacío que además
imprime en el log de arranque. La opción intermedia (dejarlo "por si acaso") es la única que no aporta nada.

#### d) La cadena de migración lleva 16 sub-estados sin subir de versión

`CURRENT_DATA_VERSION = 1` desde F1, con una única migración identidad. Los 15 sub-estados añadidos después
funcionan porque sus codecs son opcionales, lo cual es correcto **hoy**. El riesgo aparece la primera vez que
haya que **renombrar o reestructurar** un campo ya persistido: en ese momento no habrá un punto de anclaje
(¿desde qué versión migro?), porque todos los saves de F1 a F9 dicen `dataVersion: 1` describiendo esquemas
completamente distintos.

*Recomendación:* subir la versión ahora a un número que refleje el esquema actual y registrar el paso identidad
correspondiente. Es barato en este momento y deja de serlo en cuanto haya saves de jugadores reales.

#### e) `TraderSpawnPolicy`: tres claves de config que no hacen nada

`UseTraderSpawnRate`, `TraderSpawnRate` y `TraderSpawnChance` se leen, se validan, se escriben en el archivo de
config por defecto… y su único consumidor posible (`TraderSpawnPolicy`) nunca se instancia. Un administrador
que las configure obtendrá silencio.

*Recomendación:* o se conecta la política a la aparición del mercader errante, o se retiran las tres claves del
archivo generado. Documentar una opción inerte es peor que no ofrecerla.

#### f) La IA shinobi tiene 6 niveles de amenaza y un solo elemento

Los seis niveles de `ThreatLevel` se diferencian en vida, color y **cuántos jutsus de fuego** conocen. Un Kage
lanza las mismas técnicas que un Chūnin, sólo que más. El pipeline ya soporta cualquier jutsu para casters
no-jugador (`ShinobiCastContext`), así que la limitación es puramente de datos.

*Recomendación:* poblar las listas con rutas elementales (un shinobi de rayo, uno de tierra…) o hacer el roster
data-driven como los contratos. Es el sistema con más superficie de juego desaprovechada del mod.

#### g) `MarkerEntity` se mantiene sin cliente

Es una `EntityType` registrada, con renderer, con tracking configurado y con tests, cuyo único invocador es un
comando de depuración. Se creó como primitiva compartida en F4 y ningún sistema posterior la necesitó (todos
usaron `AbilityAnchor`).

*Recomendación:* no es urgente, pero conviene decidirlo explícitamente: o se le encuentra un uso (p. ej. la
marca del FTG, que hoy es un `GlobalPos` en el attachment) o se retira. Mantener una entidad registrada tiene
coste permanente: aparece en el registro, en los data fixers y en toda validación de arranque.

### 14.4. 🟢 Aciertos que conviene preservar

No todo son pendientes. Estas decisiones están bien tomadas y merecen protegerse en futuras refactorizaciones:

- **Un solo pipeline de casteo** para jugador, NPC, habilidad ocular y kenjutsu. Se diseñó en F4 anticipando
  casters no-jugador y en F8 la IA se enchufó sin tocar una línea.
- **Contenido data-driven con codec estricto**: un JSON roto falla **aislado**, con error en el log, sin tumbar
  el resto ni el arranque.
- **Declarar contenido que aún no existe y contarlo en el log** (`"24 ofertas, 15 pendientes de su item"`).
  Convierte una deuda invisible en una métrica que se ve en cada arranque.
- **Estado inmutable hasta las hojas** y una única puerta de escritura por recurso (chakra, XP, naturalezas).
- **Reactivo, nunca polling.** El datapack original barría selectores globales cada tick; aquí hay un bus de
  eventos y sistemas por fase.
- **Divergencias conscientes documentadas** en el javadoc (Namikaze determinista, escalera del Susanoo,
  `EyeAbility.canApply`). Cada una explica qué hacía el datapack y por qué se cambió.
- **912 tests de dominio puro** sin Minecraft en el classpath: rápidos, deterministas y ejecutables en CI.

> ⚠️ **Contrapunto importante sobre los tests:** los 912 tests son **puramente de dominio**, sin Minecraft en
> el classpath. Todo lo que depende del cliente, del mundo o del manejo de ítems —renderizado, GUI, entrega de
> ítems, entidades, partículas, sonidos— **no está cubierto**. Un build verde y unos tests verdes **no
> demuestran que el mod funcione en partida**. La checklist manual de `known_issues.md §1` sigue sin ejecutarse.

---

## Apéndice A — Cómo se ha verificado este documento

| Comprobación | Método |
|---|---|
| Inventario de clases y paquetes | `find` sobre `*/src/main/java` y `*/src/test/java` |
| Catálogo de 128 jutsus | Script Node que parsea los 128 JSON y extrae coste/cd/requisitos/efecto/parámetros |
| Consistencia de habilidades oculares | Script que cruza `EyeAbilityService.register(...)` con el campo `abilities` de los 15 JSON de ojo |
| Assets huérfanos | Script que busca cada textura/modelo por su id completo en todo el corpus de JSON + Java |
| `sounds.json` ↔ ficheros ↔ `ModSounds` | Script de triple cruce |
| Traducciones faltantes | Script que extrae todas las claves `*.dojutsu.*` del código y las contrasta con `en_us.json` |
| Ítems inexistentes | Script que extrae todo `"dojutsu:<id>"` de `data/` y lo cruza con el registro (incluidos los registros dinámicos de máscaras y arrojadizas) |
| Código muerto | Script de referencias cruzadas por nombre simple entre todos los `.java`, con verificación manual de los candidatos |
| Fases de tick | `grep` de `return TickPhase.*` en todas las implementaciones de `PlayerTickSystem` |
| Estado de git | `git status`, `git log` |
| Build y tests | `./gradlew build` y `./gradlew :common:test`, con agregación de los XML de `test-results` |

Los scripts de auditoría son reproducibles y quedaron en el scratchpad de la sesión; ninguno modifica el repo.
