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

`DojutsuMod` (raíz) · `platform` (puertos) · `registry` (`Mod*`) · `core` (`DataReloadListener`) ·
`entity` (placeholder) · `network`/`debug` (**temporales de F0**). Adaptadores: `neoforge.*`, `fabric.*`.
**Aún no existen** los módulos de contenido del diseño (`domain`, `data`, `chakra`, `jutsu`, etc.).

## Convenciones clave

- `common` **nunca** importa clases de loader; solo puertos.
- Registries = clases `Mod<Tipo>` que usan `PlatformServices.REGISTRAR`/`ATTACHMENTS`; IDs con
  `DojutsuMod.id(path)`.
- Puertos = interfaz en `platform` + impl `<Loader>XxxService` + service file.
- Comentarios/logs en español; **logs sin acentos**. Ver `docs/coding_conventions.md`.

## Estado actual

- **Fase 0 (andamiaje multiloader): COMPLETADA y validada en runtime** en NeoForge y Fabric
  (cliente y servidor dedicado). Tickets DJT-001…008 todos ✅; CI en verde.
- **No hay mecánicas de gameplay todavía.** Lo que funciona: los 3 puertos de plataforma, un registro
  base placeholder, un reload listener, y andamiaje de validación (comando `/dojutsudummy`).

## Fases

- ✅ **F0** — Fundaciones y andamiaje.
- ⬜ **F1 (siguiente)** — Núcleo de dominio (`domain`, con **TDD**) + `data`/`PlayerNinjaData`.
- ⬜ F2 recursos/progresión · F3 definición del ninja · F4 combate · F5–F10 contenido incremental.

## Instrucciones importantes para futuros desarrollos

1. **Fuente de verdad del diseño (fuera de este repo):** `D:\Projectos claude\Dojutsu reverse\DOCS\`
   → `sistemas/` (ingeniería inversa S01–S25), `arquitectura/` (ADD), `backlog/` (fichas DJT-###).
   No re-diseñar ni reinterpretar el datapack; implementar los tickets.
2. **Un ticket a la vez**; validar en runtime en **ambos** loaders antes de darlo por cerrado.
3. **NO usar el `NetworkManager` de Architectury** (bug S2C en Fabric 13.0.8, issue #518): usar el
   puerto `NetworkService` (nativo por loader).
4. **No actualizar** loom/fabric-loader/neoforge/architectury sin verificar la matriz (rompe el build).
5. Toda `EntityType` registrada necesita **renderer** en cliente (`NoopRenderer` para marcadores).
6. Al construir `PlayerNinjaData` (F1), usar el puerto `AttachmentService` y **retirar** el andamiaje
   `debug`/`network` de F0.
7. Falta un puerto **`AttributeService`** para adjuntar atributos a `Player` (pendiente para chakra).
8. Antes de tocar temas de build/versiones/networking, leer `docs/development_log.md` (registra los
   problemas ya resueltos y por qué).
