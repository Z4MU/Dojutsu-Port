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

**Deuda/deferidos abiertos tras F0:**
- ⬜ Puerto `AttributeService` (adjuntar atributos a entidades vanilla como `Player`).
- ⬜ Retirar andamiaje temporal (`debug`, `network` Ping/Pong, `DebugMarkerEntity`) al llegar `PlayerNinjaData`.
- ⬜ Tests unitarios (el task `test` pasa vacío; se introducen en F1 con TDD).
- ⬜ `LICENSE` (assets de Prof Technic son **no-comerciales**).

---

## Fase 1 — Núcleo de dominio y datos — ⬜ PENDIENTE (siguiente)

Objetivo: dominio ninja puro (testeable sin Minecraft) + estado del jugador.

Próximas historias/temas (según el ADD; los IDs exactos están en el backlog de diseño):
- Módulo **`domain`** con TDD: `Resource`+`RegenPolicy`, `StrainMeter`, `ToggleableMode`,
  `Jutsu`+`JutsuDefinition`, `LeveledStat`+`XpCurve`+`NinjaRank`.
- Módulo **`data`**: `PlayerNinjaData` (attachment real), `Codec`/NBT versionado (`dataVersion`),
  `MigrationChain`; reset = instancia nueva. Usa el puerto `AttachmentService` ya construido.

---

## Fases posteriores — ⬜ PENDIENTES

| Fase | Tema |
|------|------|
| F2 | Recursos y progresión (`chakra`, `progression`) |
| F3 | Definición del ninja (`natures`, `clans`) |
| F4 | Combate (`jutsu`, `entity`, `throwing`) |
| F5–F10 | Contenido incremental (ojos, sage, empowerment, bijuu, items, IA, trader, historia, mundo, cliente/GUI/renders) — mediante plantillas clonables |

Camino crítico del ADD: F0 → F1 → F2 → F4 → F5/F7 → F9. Vertical slice en F2.
