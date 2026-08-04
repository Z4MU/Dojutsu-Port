# Dojutsu

Sistema de habilidades ninja para Minecraft **1.21.1** — clanes, chakra, dojutsus, jutsus por naturaleza,
modos de potenciación, Bestias con Cola e invocaciones.

Es el porte a mod del datapack **Dojutsu v1.9.3** de Prof Technic. No es una reinterpretación: cada valor
—coste de chakra, umbral de habilidad, radio, enfriamiento— está contrastado contra los `.mcfunction` y los
predicates del original, y donde el mod se aparta queda escrito por qué.

> **Versión 1.0.0.** Las once fases del porte están cerradas y validadas en los dos loaders. Lo que queda es
> arte —modelos de Susanoo y de Mangekyō por personaje— y traducciones. Ver [Qué falta](#qué-falta).

## Requisitos

| | |
|---|---|
| Minecraft | 1.21.1 |
| Java | 21 |
| Loader | **NeoForge** 21.1.235+ o **Fabric** 0.16.10+ |
| Dependencias | [Architectury API](https://modrinth.com/mod/architectury-api) 13.0.8+ · en Fabric también [Fabric API](https://modrinth.com/mod/fabric-api) |

## Instalación

1. Instala NeoForge o Fabric para 1.21.1.
2. Pon en `mods/` el jar de tu loader —`dojutsu-neoforge-<versión>.jar` o `dojutsu-fabric-<versión>.jar`— **más
   Architectury API** (y Fabric API si vas por Fabric).
3. Arranca. Al entrar por primera vez se te pedirá elegir clan.

Funciona en un mundo local y en servidor dedicado. **Toda la lógica es de servidor**: en multijugador basta con
que lo tenga el servidor y los clientes para ver la interfaz.

## Cómo se empieza

Al entrar eliges **clan**, y el clan decide de qué vas: qué ojo puedes despertar, qué naturaleza te viene de
serie y qué técnica te llevas. Hay **14**: Uchiha, Hyūga, Ōtsutsuki, Senju, Uzumaki, Nara, Akimichi, Sarutobi,
Aburame, Hatake, Kaguya, Chinoike, Namikaze y Lee.

A partir de ahí el chakra se regenera solo, subes de rango acumulando ninja_xp, y las técnicas se desbloquean
por hitos de habilidad. Abre el menú con **M** para verlo todo.

## Controles

Todas reasignables en Opciones → Controles → categoría **Dojutsu**.

| Tecla | Qué hace |
|---|---|
| **M** | Menú ninja (chakra, rango, habilidades, naturalezas, clan) |
| **V** | Lanzar el jutsu de la mano principal |
| **N** | Menú de habilidades oculares |
| **R** | Usar la habilidad ocular seleccionada |
| **C** | Visión de chakra |
| **U** | Caminar sobre el agua |
| **G** | Modo Sabio |
| **B** | Sello Maldito |
| **H** | Ocho Puertas (agáchate para cerrar) |
| **J** | Sello de la Fuerza de Cien (agáchate para recargar) |
| **K** | Forma de jinchūriki (agachado: técnica de bestia · corriendo: Baryon) |
| **Y** | Dios del Trueno Volador (teletransporte a la marca) |

## Qué hay dentro

- **128 jutsus** en 29 categorías: las cinco naturalezas básicas, las combinadas y avanzadas (hielo, madera,
  explosión, tormenta, lava, quemadura, polvo, imán, cristal, acero, hervor, oscuridad, veloz, tifón, lodo,
  blaze, yin-yang) y las disciplinas — taijutsu, genjutsu, kenjutsu, médico, fuinjutsu, aburame y universales.
- **18 Kekkei Genkai** declarados como datos, incluidos los que no son un elemento: el Shikotsumyaku de los
  Kaguya y la línea del clan Jūgo.
- **15 ojos** con su grafo de evolución: Sharingan → los **8 Mangekyō por personaje** → Eterno, Rinnegan,
  Rinne Sharingan, Byakugan, Tenseigan, Ketsuryūgan, Jōgan.
- **Bestias con Cola** — las nueve más el Diez Colas: sellado, vínculo, formas de jinchūriki, Modo Baryon.
- **Modos**: Sabio, Sello Maldito, Ocho Puertas, Fuerza de Cien, Susanoo, Tenseigan Chakra Mode y forma de
  jinchūriki.
- **27 contratos** de invocación, comercio con mercaderes shinobi, drop-crafting y los Seis Caminos.

## Configuración

Se genera `config/dojutsu.properties` al primer arranque. Las opciones son las del datapack original:

| Opción | Def. | Qué hace |
|---|---|---|
| `allowCustomTrader` | `true` | El mercader errante vende mercancía ninja |
| `allowShinobis` | `true` | Algunos aldeanos se convierten en mercaderes shinobi |
| `allowMultJinchuriki` | `true` | Varios jinchūrikis de la misma bestia |
| `checkHardcore` | `false` | Reglas de hardcore |
| `customMangekyoObtainable` | `true` | Los Mangekyō por personaje se pueden despertar |
| `godTreeProtectsBuilds` | `false` | **Opción del mod**, no del datapack: el Árbol Divino no arrasa tus construcciones. `false` = fiel al original |
| `traderSpawnRate` · `traderSpawnChance` | `1` | Frecuencia del mercader |
| `killMobPoints` · `killPlayerPoints` · `killBossPoints` | `1` · `3` · `200` | Puntos de habilidad por muerte |
| `natureEnergyCharge` | `1` | Ritmo de carga de energía natural |

## Comandos

**Todos exigen nivel de operador 2**; no hay comandos de jugador — se juega con el menú y las teclas.

- `/dojutsu reset_ninja [jugador]` — reinicia el progreso ninja y devuelve al jugador a la creación de
  personaje.
- **20 comandos de depuración** (`/dojutsubeast`, `/dojutsuchakra`, `/dojutsudojutsu`, `/dojutsugates`…),
  pensados para QA y no para jugar.

## Compilar desde el código

```bash
./gradlew build
```

Los jars quedan en `neoforge/build/libs/` y `fabric/build/libs/`. Para probar:

```bash
./gradlew :neoforge:runClient
```

Los tests son de dominio puro, sin Minecraft en el classpath:

```bash
./gradlew :common:test
```

Ojo con eso último: **el build verde no ve el juego**. Nada que dependa del cliente, del mundo o de la
sensación de movimiento lo comprueba un test — está dicho en `docs/` y es la lección más cara del proyecto.

## Para otros mods

Hay una **API pública estable** en `com.zamu.dojutsu.api` (v1.0.0): leer el estado de un ninja, escribirlo por
las puertas del mod, aportar técnicas/ojos/clanes/contratos y escuchar los eventos. Ver [`docs/api.md`](docs/api.md).

## Documentación

| | |
|---|---|
| [`docs/api.md`](docs/api.md) | API pública para otros mods |
| [`docs/architecture.md`](docs/architecture.md) | Cómo está montado el mod |
| [`docs/development_log.md`](docs/development_log.md) | Diario por ticket: qué se decidió y por qué |
| [`docs/known_issues.md`](docs/known_issues.md) | Deuda y límites conocidos |
| [`docs/sonido.md`](docs/sonido.md) · [`docs/bijuu_arte.md`](docs/bijuu_arte.md) | Inventarios de sonido y de arte |

## Qué falta

- **Arte**: modelos del Susanoo y de los Mangekyō por personaje. Siete de las nueve Bestias con Cola se dibujan
  con el ravager — [y así es en el original](docs/bijuu_arte.md), cuyo resource pack sólo trae dos cuerpos.
- **Idiomas**: sólo inglés.
- **Tres técnicas del Diez Colas** y ocho sonidos cuya técnica aún no está portada
  ([detalle](docs/sonido.md)).

## Licencia

**El código es MIT. El arte no.**

Las texturas y modelos vienen del resource pack de **Prof Technic** y son de **uso no comercial**, cláusula que
alcanza a todo el distribuible mientras quede una sola pieza suya. [`LICENSE`](LICENSE) tiene las dos secciones
y [`ATTRIBUTION.md`](ATTRIBUTION.md) el inventario archivo por archivo — que es una lista de tareas: cuando cada
fila se sustituya por obra propia, el proyecto pasa a ser MIT completo.

Naruto y Boruto son propiedad de Masashi Kishimoto y Shueisha. Esto es un proyecto de fans, sin ánimo de lucro
y sin relación con los titulares.
