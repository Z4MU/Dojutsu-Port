# Checklist de paridad y verificación en cliente (F10/DJT-223)

> Creado el 2026-08-01. Documento **de trabajo**: se marca según se verifica. Cubre los 25 sistemas
> (`[S01]`…`[S25]`) en **NeoForge 21.1.235** y **Fabric 0.16.10**, y sustituye a la deuda dispersa de
> `known_issues.md §1`, §1b y §1d, que quedan como referencia histórica.

## 0. Qué se ha verificado ya sin jugar, y qué no puede verificarse así

**Verificado el 2026-08-01, por primera vez en el proyecto: el arranque de CLIENTE.** Hasta esta fecha todo
—F1 a F10— se había validado con el **servidor dedicado**. Eso deja fuera, por construcción, todo el registro
de cliente: renderers, keybinds, HUD, pantallas y la carga del atlas de texturas.

| Comprobación | NeoForge | Fabric | Cómo |
|---|---|---|---|
| El cliente arranca y llega al menú | ✅ | ✅ | `runClient` |
| `initClient()` se ejecuta (keybinds, HUD, pantallas) | ✅ | ✅ | log `Inicializacion de cliente completada` |
| **Los 6 renderers de entidad quedan registrados** (regla 5: si falta uno, el cliente revienta) | ✅ | ✅ | no hay crash al cargar mundo |
| **Cero modelos sin resolver** (`Unable to load model`) | ✅ **0** | ✅ **0** | grep del log de recarga de recursos |
| **Cero texturas ausentes** (`Using missing texture`) | ✅ **0** | ✅ **0** | ídem |
| Se entra en un mundo y el jugador aparece | ✅ | ✅ | `--quickPlaySingleplayer` |
| El servidor integrado carga los mismos datos que el dedicado | ✅ | ✅ | 128 jutsus, 15 ojos, 14 clanes, 18 KG, 27 contratos, 25 ofertas/12 pendientes, 16 combinaciones/0 pendientes |
| `OnboardingController` dispara al entrar | ✅ | ✅ | log `entra en la creacion del ninja (SELECTING_CLAN)` |
| Sync S2C full + delta llega al cliente | ✅ | ✅ | log `Cache cliente (full)` y `(delta)` |
| **La regeneración de chakra es la tabla de `[S02]`** | ✅ | ✅ | +3 cada 120 t observado, clampa en 100/100 |
| **La GUI de creación del ninja se dibuja** con los 14 iconos de clan | ✅ | — | inspección visual (una sola vez, ver §5) |

**Lo que este método NO puede verificar** y por tanto sigue abierto: cualquier cosa que dependa de **mirar la
pantalla** o de **pulsar una tecla**. El resto de este documento es esa lista.

> **Nota de método.** Se intentó automatizar la partida inyectando teclado y ratón en la ventana del juego.
> El movimiento del ratón sí llega (aparecen los tooltips), pero **las pulsaciones no**: GLFW ignora la entrada
> sintética de `SendKeys`, y traer la ventana al frente desde un proceso en segundo plano falla de forma
> intermitente en Windows, con lo que la captura acaba fotografiando otra ventana. **La verificación interactiva
> de este documento la tiene que hacer una persona.**

## 1. Cómo montar la sesión de QA

Hay un mundo de pruebas ya creado en los dos loaders (`run/saves/dojutsu-qa`), así que el cliente puede entrar
directo:

```bash
./gradlew :neoforge:runClient --args="--quickPlaySingleplayer dojutsu-qa"
```

```bash
./gradlew :fabric:runClient --args="--quickPlaySingleplayer dojutsu-qa"
```

Dentro, lo primero:

```bash
/gamemode creative
```

```bash
/dojutsuninja get
```

**Los 20 comandos de depuración no tienen puerta de permisos** (hallazgo de la auditoría del 2026-07-30): en
esta sesión es cómodo, pero es deuda a cerrar antes de publicar nada.

## 2. Prioridad — lo que no se ha visto NUNCA

Ordenado por riesgo: arriba, lo que tiene más superficie visual y menos verificación previa.

### 2.1. Renderers de las criaturas `[S09]` `[S17]`

Las dos únicas entidades vivas del mod usan malla **prestada** (ravager y zombi). Nunca se ha visto ninguna.

- [ ] `/dojutsubeast summon nine_tails` — aparece, se mueve, **la bossbar nativa** sale con su nombre y color.
- [ ] La bestia ataca y usa su técnica insignia; la Bola Bestia con Cola se ve.
- [ ] **Las 9 andanadas** (F10/DJT-226): invocar bestias distintas y comprobar que cada una dispara con **su**
      partícula (arena, llama azul, lluvia, burbujas, nube, terracota magenta, chakra rojo…), que el alcance es
      corto (~8 bloques) y que la del **Nueve Colas llega el doble**.
- [ ] `/dojutsubeast killall` — desaparecen la entidad **y su barra**.
- [ ] `/dojutsushinobi spawn` — el shinobi persigue, tira kunai a media distancia y **castea** de lejos.
- [ ] El nivel de amenaza sale como nombre de la entidad, con su color.
- [ ] **Los 9 huevos de DJT-225**: `/dojutsuitem egg <bestia>` y usar sobre un bloque → sale **su** bestia.

### 2.2. Menú ocular y habilidades `[S05]` `[S24]`

30 clases de habilidad, 15 ojos, un menú entero: validado por payload, nunca visto.

- [ ] `/dojutsudojutsu activate sharingan` y **N** → se abre `AbilitySelectScreen` con la lista del ojo activo.
- [ ] Seleccionar una habilidad, cerrar, **R** → se lanza; el chakra y la fatiga bajan en el HUD.
- [ ] Amaterasu: el fuego negro se ve y prende.
- [ ] Fatiga: `/dojutsudojutsu strain 94` y subir → avisos en chat en 95 / 98, y a 100 **se apaga el ojo**.
- [ ] `/dojutsudojutsu susanoo level 4` + activar → **la silueta por etapas** (hoy partículas, no malla).
- [ ] Chakra vision (**C**): las entidades cercanas se contornean **con el color de su relación**.
- [ ] **Rinne Sharingan** (F10/DJT-226): `/dojutsugodtree bloom` lo concede; el menú debe listar **13**
      entradas (las suyas + las del Mangekyō + las del Rinnegan), no 6.
- [ ] **Amenominaka**: con 8000 de chakra, saltar al Nether **con otro jugador a menos de 10 bloques** y
      comprobar que **se lo lleva**; que suena `dojutsu.rinne_sharingan.amenominaka`; que «Posición original»
      no funciona antes de irse y sí después; y que volver al Overworld la consume.

### 2.3. Árbol Divino por etapas `[S21]`

25 minutos de crecimiento en 5 etapas, 6 estructuras `.nbt`. Nunca se ha visto crecer.

- [ ] `/dojutsugodtree plant` (cuesta 1000 de chakra) → aparece la etapa 1.
- [ ] `/dojutsugodtree advance` ×5 → **cada etapa cambia la estructura** y la anterior se retira limpia.
- [ ] `/dojutsugodtree bloom` → florece y **concede el Rinne Sharingan** (aviso en chat).
- [ ] Plantarlo donde no cabe → aviso `would_destroy` y no destruye nada.

### 2.4. Diálogo del Sabio `[S19]`

- [ ] `/dojutsustory force` → se abre `DialogueScreen` con el texto del Sabio.
- [ ] Las tres ramas (`path yin` / `path yang` / `path none`) dan lo suyo y **+100 ninja_xp**.
- [ ] `/dojutsustory otherhalf` → entrega la mitad restante.

### 2.5. Formas de jinchuriki `[S09]`

- [ ] `/dojutsubeast seal nine_tails` → +chakra máximo, mensaje, y el registro global lo marca tomado.
- [ ] **K** → forma de jinchuriki: buffs visibles, y **los ojos se suprimen** (no se apagan).
- [ ] Modo Baryon: se enciende, drena, y al agotarse **la bestia se va**.
- [ ] `/dojutsuitem scroll removal` + clic derecho → apaga Baryon y forma, y extrae la bestia (DJT-225).

## 3. Checklist por sistema (S01–S25)

Marcar en **los dos loaders**. `H` = ya verificado headless; sólo queda la mitad visual.

| Sistema | Qué mirar en partida | NF | FA |
|---|---|:--:|:--:|
| **S01** Núcleo | `H` Persistencia: salir y volver conserva todo. Muerte → ½ chakra. Respawn no pierde el ninja | ☐ | ☐ |
| **S02** Chakra | `H` Barra del HUD sube sola; comer da +50; **ramen +150**; dormir la noche llena | ☐ | ☐ |
| **S03** Naturalezas | Menú **M**: afinidades y su nivel; el learner enseña en orden | ☐ | ☐ |
| **S04** Clanes | `H` Los 14 iconos salen en la creación ✅; nametag por clan con su color | ☐ | ☐ |
| **S05** Dōjutsu | Ver §2.2 | ☐ | ☐ |
| **S06** Jutsus | Castear una de **cada** familia y ver el efecto (7 familias) + HUD de cooldown | ☐ | ☐ |
| **S07** Kenjutsu | Las 4 armas en mano con su modelo; Samehada absorbe; **tanto** se gasta (1000 dur.) | ☐ | ☐ |
| **S08** Modo Sabio | **G**: aura, drenaje por nivel; `/dojutsusage` para los requisitos | ☐ | ☐ |
| **S09** Bestias | Ver §2.1 y §2.5 | ☐ | ☐ |
| **S10** Sello maldito | **B**: se enciende, mantiene, y **mata** si no puedes pagarlo | ☐ | ☐ |
| **S11** Ocho Puertas | **H**: la escalera de 8, la fatiga corporal, y que a 100 se cierran solas | ☐ | ☐ |
| **S12** SOAH | **J**: la hucha devuelve por tramos al bajar de 5000 | ☐ | ☐ |
| **S13** Menú/nivelación | **M**: gastar puntos sube la habilidad y el rango; la UI se refresca **tras** el sync | ☐ | ☐ |
| **S14** Ítems | Los 20+17 ítems con su modelo; máscaras ocultan el nombre; píldoras y su resaca | ☐ | ☐ |
| **S15** Invocaciones | Contrato firmado → invoca; caduca sola; **no cuesta chakra**, cuesta veto | ☐ | ☐ |
| **S16** Arrojadizas | Las 5 vuelan, se clavan y se recuperan; el explosivo detona; **Y** teletransporta | ☐ | ☐ |
| **S17** Shinobi IA | Ver §2.1 | ☐ | ☐ |
| **S18** Comercio | Un aldeano de cada 10 es shinobi; el mercader se convierte siempre; las 13 ofertas vivas | ☐ | ☐ |
| **S19** Narrativa | Ver §2.4 | ☐ | ☐ |
| **S20** Rango | El HUD de rango cambia al cruzar umbral, con su aviso | ☐ | ☐ |
| **S21** Árbol Divino | Ver §2.3 | ☐ | ☐ |
| **S22** Water walking | ✅ **único visto antes de hoy** — reverificar tras cambios | ☐ | ☐ |
| **S23** Crafteo | Las 13 recetas; **drop-crafting** de las 16 combinaciones; las 4 puertas de habilidad | ☐ | ☐ |
| **S24** Susanoo | Ver §2.2 | ☐ | ☐ |
| **S25** Chakra vision | Ver §2.2 | ☐ | ☐ |

## 3b. Rendimiento en partida (lo único que DJT-222 no pudo medir)

`docs/perf.md` acota el coste por tick, pero el MSPT real sólo se ve jugando. Con F3 abierto:

- [ ] **MSPT en reposo** con 1 jugador y sin nada especial encendido (línea base).
- [ ] **MSPT con la chakra vision encendida** mirando una granja o una aldea (el barrido de radio 30 es el
      camino que más escala con las entidades del mundo).
- [ ] **MSPT con varios ítems tirados juntos** (el drop-crafting barre radio 16 cada 5 ticks por jugador).
- [ ] **MSPT con una bestia y varios shinobi** activos a la vez.
- [ ] Nada de lo anterior debería mover el MSPT de forma perceptible; si lo mueve, es el dato que faltaba.

## 4. Migración desde saves — ✅ verificado (2026-08-01)

La auditoría avisaba de que se enchufaron **16 sub-estados** a lo largo de F1–F9 **sin subir nunca**
`CURRENT_DATA_VERSION`, y de que la cadena de migración está construida pero sin estrenar. Se sospechaba que
funcionaba «porque los codecs son opcionales» — una suposición que nadie había comprobado.

**Ahora está comprobada**, en `LegacySaveMigrationTest` (6 tests): un NBT tal y como lo escribía **F1** (sólo
`onboarding` y `chakra`, sin `dataVersion`) carga contra el agregado de hoy, conserva lo que traía, y **los 14
sub-estados que F1 no conocía caen a su `DEFAULT`**. También que migrar es idempotente y que un save de una
versión *futura* no se degrada.

**Qué significa para el jugador:** un mundo de cualquier fase anterior se abre sin perder nada. **Qué significa
para quien programe:** el día que un sub-estado nuevo lleve un campo **obligatorio**, ese test se pone rojo
antes de que se rompa la partida de nadie. Es la red que faltaba.

**Lo que sigue pendiente** (no bloqueante): `CURRENT_DATA_VERSION` sigue en 1 y la cadena sigue teniendo un
solo paso identidad. Está bien mientras todo sea aditivo y opcional; el primer cambio **incompatible** (renombrar
un campo, cambiar un tipo) obliga a subir la versión y escribir el paso de verdad.

## 5. Estado visual comprobado

- **Pantalla de creación del ninja**: se dibuja correcta — título, subtítulo, «Random Clan» y los **14 clanes**,
  cada uno con **su icono** (los del DJT-224) y su color. Los tooltips de clan salen al pasar el ratón.
- **HUD**: la barra de chakra y el rango se dibujan sobre el mundo y **por debajo** de la pantalla abierta,
  como debe ser.
- Nada más se ha visto. El resto de §2 y §3 está sin mirar.

## 5b. Primera tanda con una persona jugando — 7 observaciones, 5 arregladas

| # | Observación | Veredicto | Estado |
|---|---|---|---|
| 1 | Las bestias con cola no reciben daño | **Fallo real**: el renderer las agrandaba pero la caja de colisión seguía fija en `2.0 × 2.6` a sus pies | ✅ arreglado — escala compartida en `BeastStats.renderScale` |
| 2 | Las llamas del Amaterasu no se ven negras | **Fallo real**: se usaba `ParticleTypes.FLAME` (naranja); el datapack usa `dust` casi negro en dos capas | ✅ arreglado |
| 3 | No está claro cómo evolucionar al Mangekyō Eterno | **Fallo real** (de UI): el botón no decía qué faltaba | ✅ arreglado — el botón dice el motivo |
| 4 | El botón de evolucionar sale en Rinnegan y Rinne Sharingan | **Fallo real**: se dibujaba con cualquier ojo porque el cliente no sabía si era eternalizable | ✅ arreglado — el servidor manda el estado |
| 5 | Falta interruptor de caminar sobre el agua | **Petición** — el datapack no lo tiene | ✅ añadido como divergencia consciente (tecla **U**) |
| 6 | Faltan comandos de jinchūriki (Baryon y la forma) | **Fallo real** (de andamiaje) | ✅ `/dojutsubeast form` y `/dojutsubeast baryon` |
| 7 | El stun del Sharingan no consume chakra ni fatiga | **Correcto** — el datapack lo gatea **sólo por enfriamiento** (200 t) | ✔️ sin cambios, es fiel |

**Cinco de siete eran fallos reales, y ninguno lo veía un test**: dos de render, dos de UI y uno de andamiaje.
Es la confirmación práctica de por qué existe este documento.

**Volver a comprobar en partida** (lo arreglado, sin verificar todavía):

- [ ] La bestia **recibe golpes** y la caja coincide con lo que se ve, en las nueve.
- [ ] El Amaterasu se ve **negro**.
- [ ] El botón del EMS **no aparece** en Rinnegan ni Rinne Sharingan, y en un Mangekyō dice qué falta.
- [ ] `/dojutsubeast form` y `/dojutsubeast baryon` hacen lo suyo.
- [ ] La tecla **U** activa y desactiva la caminata sobre el agua, y se conserva al reconectar.

## 6. Hallazgos de esta pasada

1. **`Delta recibido antes del primer full; ignorado.`** — al entrar, llega un delta S2C **antes** del full, en
   **los dos loaders**. Hoy es inofensivo (`ClientNinjaData` lo descarta a propósito y el full llega justo
   después), pero significa que algo emite un delta durante el join antes de que se haya enviado el estado
   completo. Merece mirarse: si algún día el full tardara más, se perdería ese cambio.
2. **El chakra regenera durante el onboarding**, con el ninja aún sin crear. Es coherente con el datapack
   (el tick corre para todo jugador) pero conviene decidirlo a propósito, no por omisión.
3. **Cero modelos y cero texturas sin resolver** en los dos loaders, con ~200 modelos de ítem cargados. Es la
   primera confirmación de que el porte de assets de DJT-224 y DJT-225 está bien enlazado.
4. **Los 5 `No data fixer registered`** (uno por entidad propia) son ruido vanilla benigno y aparecen igual en
   los dos loaders; no son un fallo del mod.
5. **Paridad de adaptadores auditada y correcta**: los dos loaders llaman a `init()` + `initClient()` por el
   mismo camino, registran los **6** renderers, los **11** keybinds (con sus 12 claves de idioma) y los mismos
   eventos nativos — dormir por evento en ambos, comer por evento en NeoForge y por mixin en Fabric, tal y como
   documenta `[ADD/05]`. No se encontró ninguna asimetría.

## 7. Deuda que este ticket deja anotada, no resuelta

- **Catálogo del mercader errante divergente** del datapack (de DJT-190; detalle en `known_issues.md §2`).
- **26 de 128 jutsus siguen sin forma de obtenerse** (tres `TODO F4` en `UnlockService`,
  `ClanJutsuUnlockService` y `ClanService`, de la auditoría).
- **20 de 28 sonidos registrados no los reproduce nadie.**
- **Los 20 comandos de depuración no tienen puerta de permisos.**
- **F1–F10 sigue sin commitear** (último commit: F0, 2026-07-12).
