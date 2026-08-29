# Rendimiento — auditoría del coste por tick y por frame

> **§0–§6 (F10/DJT-222, 2026-08-01) — el tick de servidor.** Cubre **todos** los puntos de entrada por tick del
> mod. La conclusión en una línea: **el mod no tiene un problema de rendimiento por tick**; lo que sí tiene es
> un eje de escalado —los barridos de entidades— que crece con el contenido del mundo y no con el trabajo
> constante por jugador.
>
> **§7 (F11/DJT-233, 2026-08-08) — el frame de cliente.** Eje distinto y más frecuente (60–144 veces por
> segundo, frente a 20). Ahí manda la asignación, no el cálculo. Léelo antes de tocar el HUD.

## 0. La escala, primero

Un tick de servidor son **50 ms**. Con 20 jugadores conectados, un sistema por tick que cueste **1 µs por
jugador** consume 20 µs = el **0,04 %** del presupuesto. Sin esa referencia delante, cualquier micro-medición
parece importante y se acaba optimizando ruido.

Este documento existe tanto para justificar los tres cambios que se hicieron como para **dejar por escrito lo
que no se va a tocar y por qué**.

## 1. Lo que se corrigió, con números

Medido con `TickHotPathBenchmark` (banco manual, ver §5). Máquina de desarrollo, JIT calentado, 5 M
iteraciones:

| Camino | Antes | Ahora | Factor |
|---|---:|---:|---:|
| Azar del glue (`new Random()` por llamada → compartido) | 35 ns/op | 5 ns/op | **7,0×** |
| Tick de un jugador **sin ojo** (sin guarda → con guarda) | 32 ns/op | 3 ns/op | **10,7×** |

Y lo que esos factores significan de verdad, que es lo importante: **0,0014 % de un tick con 20 jugadores,
frente a 0,0002 %**. Son correcciones de higiene, no un rescate.

### 1.1. `new Random()` por jugador y por tick — `AwakeningService`, `EvolutionService`

Los dos servicios construían un `java.util.Random` **nuevo en cada tick de cada jugador** para un azar que se
tira **una vez en la vida de un ninja** (el 50/50 de Otsutsuki; la variante de Mangekyō de `mangekyo_rng`).

No es sólo la asignación: el constructor sin argumentos de `Random` llama a `System.nanoTime()` y hace un
**CAS sobre un `AtomicLong` estático** de la JVM. Se pagaba una barrera de memoria 40 veces por segundo por
jugador para no usar el resultado.

Ahora comparten un `ThreadLocalRandom.current()`, que no asigna nada y es un `RandomGenerator` — que es
justo lo que pedían las funciones puras. **El parámetro sigue inyectado**: los tests le siguen pasando una
semilla fija, así que la testabilidad no se sacrificó por el rendimiento.

### 1.2. `EvolutionService` trabajaba para jugadores sin ojo

`tick` construía el predicado de flags (un lambda que captura, o sea una asignación), un `Random`, y entraba en
`evaluate` a recorrer el grafo — **para que `evaluate` devolviera exactamente el estado que le entró**. Y ese es
el caso de casi todos los jugadores casi todo el rato: la inmensa mayoría no tiene dōjutsu.

Ahora hay una guarda de una línea sobre el estado (`activeEye().isEmpty()`). El comportamiento es idéntico —
`evaluate` ya devolvía la misma instancia en ese caso— pero deja de construirse nada.

Los servicios hermanos (`AwakeningService`, `EyeStrainService`, `NaturalEnergyChargingService`, `FormService`,
`EightGatesService`) ya tenían su guarda barata; éste era el único que faltaba.

### 1.3. Un `String` por entidad y por barrido — `ChakraVisionService`

`ChakraVisionRelations` vive en `domain` y por tanto **no puede conocer `EntityType`**: su clave es el id como
texto. Traducirlo dentro del bucle significaba construir un `String` con
`EntityType.getKey(type).toString()` **por cada entidad y cada barrido**, y ese bucle recorre un cubo de 30
bloques de radio cada 10 ticks por **cada** jugador con la visión encendida. En una aldea o una granja son
cientos de entidades por barrido.

Ahora la relación se memoiza **por tipo** en el servicio: el `String` se construye una vez por tipo de entidad y
el resto es una consulta de mapa. El cache está acotado por el registro de tipos (unos cientos, fijos tras el
arranque), así que no crece con la partida — y la tabla de dominio **sigue siendo pura**, que era el motivo de
que la clave fuese texto.

Es la corrección con más recorrido real de las tres, porque su coste **escala con las entidades del mundo**, no
con los jugadores.

## 2. Inventario: los 21 puntos de entrada por tick

18 `PlayerTickSystem` (por jugador, por tick) + 3 en `TickEvent.SERVER_POST` (una vez por tick de servidor).

`O(1)*` = coste constante y con guarda barata: sale sin hacer nada en el caso común.

| Sistema | Fase | Coste en el caso común | Nota |
|---|---|---|---|
| `RegenService` | CHAKRA | `O(1)` **siempre trabaja** | Es el único que hace trabajo real cada tick para cada jugador: es su cometido. Ver §3. |
| `PillService` | CHAKRA | `O(1)*` | Dos consultas de mapa. |
| `ContractService` | CHAKRA | `O(1)*` | Cuenta atrás. |
| `FtgMarkerService` | CHAKRA | `O(1)*` | Cuenta atrás. |
| `EyeStrainService` | STRAIN | `O(1)*` | Sale si no hay fatiga ni ojo. |
| `EightGatesService` | STRAIN | `O(1)*` | Sale si no hay puertas ni fatiga ni temporizadores. |
| `WaterWalkService` | WATER_WALK | `O(1)*` | Sale por chakra **antes** de mirar bloques; máx. 2 consultas de bloque. |
| `AwakeningService` | DOJUTSU | `O(1)*` | Sale sin clan o sin dōjutsu potencial. |
| `EvolutionService` | DOJUTSU | `O(1)*` | **Corregido en este ticket** (§1.2). |
| `SusanooService` | DOJUTSU | `O(1)*` | Vía arnés: sale si el modo no está activo. |
| `TenseiganChakraModeService` | DOJUTSU | `O(1)*` | Ídem. |
| `SageService` | DOJUTSU | `O(1)*` | Ídem. |
| `CursedSealService` | DOJUTSU | `O(1)*` | Ídem. |
| `SoahSealService` | DOJUTSU | `O(1)*` | Ídem + cuenta atrás. |
| `FormService` | DOJUTSU | `O(1)*` | Sale si no eres jinchūriki. |
| `NaturalEnergyChargingService` | DOJUTSU | `O(1)*` | Sale si ya eres sabio y estás al máximo. |
| `ChakraVisionService` | DOJUTSU | `O(1)*` / **`O(entidades)`** | Sale salvo 1 de cada 10 ticks **y** con la visión activa; entonces barre. Ver §3. |
| `ClanJutsuUnlockService` | UNLOCKS | `O(1)*` | Sale para espectador/muerto. |
| `GodTreeService` | SERVER_POST | `O(árboles)` | Normalmente 0. |
| `DropCraftingService` | SERVER_POST | **`O(items)`** cada 5 ticks | Ver §3. |
| `DropUseService` | SERVER_POST | `O(bombas en el aire)` | Normalmente 0; lista acotada con caducidad de 400 ticks. |

**Siete fases del pipeline están vacías** (`JUTSU_TIMERS`, `SAGE`, `EMPOWERMENT`, `JINCHURIKI`, `THROWING`,
`SUMMONING`, `PROGRESSION`): no cuestan nada, pero conviene saber que existen — son puntos de extensión
declarados, no olvidos.

## 3. El eje de escalado real: los barridos de entidades

Lo que puede degradar un servidor **no** es el trabajo constante por jugador, que está acotado y medido arriba.
Son los tres sitios cuyo coste crece con lo que haya en el mundo:

| Barrido | Cuándo | Volumen | Puerta |
|---|---|---|---|
| `ChakraVisionService.highlightAround` | cada **10** ticks | cubo de radio **30** (60³) | sólo jugadores con visión **encendida** |
| `DropCraftingService.scan` | cada **5** ticks | cubo de radio **16** por jugador, y emparejamiento O(n²) dentro de cada racimo | sólo si hay ≥2 ítems cerca |
| `ShinobiCombatGoal` / IA de bestia | por goal | radio de combate | sólo con la entidad viva y activa |

**No se han tocado, a propósito.** Los tres ya están limitados por intervalo y por puerta, y son la traducción
directa de un comportamiento que en el datapack corría **cada tick sobre todo el mundo** — o sea que el porte ya
los mejoró una vez. Cambiarlos ahora sería optimizar sin una medición que lo justifique, y la medición que
haría falta (MSPT con muchas entidades y varios jugadores) **sólo se puede tomar en partida**.

Queda como ítem del checklist de DJT-223: mirar el MSPT con una granja grande a la vista y la chakra vision
encendida.

## 4. Lo que se decidió NO optimizar, y por qué

- **`RegenService` asigna un `EnumSet` y varios `Optional` por jugador y tick** (`factorsFor` + el proveedor del
  clan). Con 20 jugadores son ~2 000 objetos efímeros por segundo: para el generacional de la JVM eso es un
  incremento de puntero y una guardería que se vacía sola. Cambiarlo obligaría a cachear estado derivado y a
  invalidarlo a mano — más superficie de error que ganancia.
- **Los sub-estados inmutables se copian al escribir** (`withChakraTimer` cada tick, por ejemplo). Es la
  regla 10b del proyecto, y es lo que hace que los `DEFAULT` compartidos no se envenenen. Se paga a propósito.
- **El pipeline recorre los 18 sistemas para cada jugador.** Son 18 llamadas virtuales con guarda barata: 360
  por tick con 20 jugadores. Indexar por fase o filtrar por "sistemas interesados" añadiría un registro de
  suscripción para ahorrar nanosegundos.
- **`GodTreeService` consulta el `SavedData` cada tick** aunque no haya árboles. Es una consulta de mapa.

## 5. El banco de pruebas

`common/src/test/java/com/zamu/dojutsu/perf/TickHotPathBenchmark.java`. Va **`@Disabled`** a propósito: mide
tiempo, y un test que mide tiempo falla en CI el día que la máquina va cargada. Se activa con `-Pbench`:

```bash
./gradlew :common:test --tests "*TickHotPathBenchmark*" -Pbench
```

Imprime ns/op **y** lo que eso supone sobre un tick de 50 ms con 20 jugadores, que es la cifra que evita
optimizar ruido.

## 6. Lo que este ticket no puede afirmar

No se ha medido el mod **en partida**. Todo lo de aquí es coste unitario de caminos concretos y análisis
estructural; el MSPT real, con jugadores, entidades y chunks de verdad, sólo se ve jugando. Es la misma
frontera que encontró DJT-223 y por el mismo motivo. Lo que sí se puede afirmar: **no hay ningún sistema por
tick sin acotar**, y los tres barridos que escalan están identificados, con su intervalo y su puerta.


---

## 7. El otro eje: el coste **por frame** (F11/DJT-233)

> Añadido 2026-08-08, al portar el HUD movible. Las secciones 0–6 son de DJT-222 y miran **el tick de
> servidor**. Este apartado abre el eje que faltaba, y no por completismo: el HUD se dibuja en el **frame de
> cliente**, que a 60 fps corre **tres veces más a menudo** que el tick, y a 144 fps, siete.

### 7.1. Por qué el tick no era todo

Todo lo auditado en DJT-222 vive en `PlayerTickSystem` o en `TickEvent.SERVER_POST`: 20 veces por segundo. El
HUD cuelga de `ClientGuiEvent.RENDER_HUD`, que corre **una vez por frame renderizado**. La referencia de §0
(«1 µs por jugador es el 0,04 % del tick») no sirve aquí, porque ni hay 20 jugadores ni el presupuesto es de
50 ms: es un solo cliente y un frame de ~16 ms (60 fps) o ~7 ms (144 fps).

La consecuencia práctica es que **en el HUD importa la asignación, no el cálculo**. La aritmética de colocar
tres elementos es despreciable; lo que se nota es la basura que se genera para tirarla en el frame siguiente.

### 7.2. Lo que se corrigió

**`HudElement.defaultPlacement()` construía un `HudPlacement` nuevo en cada llamada.**

`HudLayout.get()` cae en ese default siempre que el jugador **no** haya movido nada — que es el caso por
defecto y, por tanto, el de la mayoría. Con tres elementos dibujados por frame, eso son **180 objetos por
segundo a 60 fps** (432 a 144 fps) para devolver siempre exactamente la misma respuesta.

Ahora se construye una vez en el constructor del enum y se comparte. Es seguro porque `HudPlacement` es un
value object inmutable: la misma propiedad que en §4 justifica *pagar* las copias de los sub-estados es la que
aquí permite *no* pagarlas, porque nadie puede mutar la instancia compartida.

**`HudElement.values()` en bucles por frame.** `values()` clona el array defensivamente en cada llamada (la
JVM no puede saber que no lo vas a modificar). Se llamaba en el bucle de render del editor y en el
`elementAt` de cada clic. Añadido `HudElement.all()`, que devuelve un array cacheado.

### 7.3. Método, y qué NO se puede afirmar

**No hay benchmark de esto.** `TickHotPathBenchmark` (§5) mide caminos de tick y no se ha extendido al frame.
Las dos correcciones salieron de **lectura del código**, y las cifras de arriba son **aritmética**
(elementos × fps), no medición: describen cuántos objetos se dejan de crear, no cuántos milisegundos se
ahorran — que es casi seguro que sean cero coma nada.

Se hicieron igualmente por dos motivos, y conviene que consten para no confundirlos con una optimización
justificada por datos:

1. Son de **coste nulo**: menos código y más simple, no un cache que haya que invalidar. La regla de §4 («no
   añadir superficie de error para ahorrar nanosegundos») no se rompe porque aquí no se añade superficie.
2. Estaban en el camino de crecer. El HUD va a ganar elementos (el jutsu seleccionado de la etapa B), y el
   editor los recorre todos por frame. Es más barato no arrancar torcido.

Si algún día hace falta la medición de verdad, lo que hay que mirar **no** es ns/op: es el ritmo de asignación
del cliente con el HUD visible, en un perfilador, con el editor abierto y arrastrando.

### 7.4. Lo que se decidió NO tocar en el HUD

Mismo criterio que §4: se anota para no volver a discutirlo.

- **`RankHudOverlay` y `ChakraHudOverlay` construyen `Component` y `String` por frame** (el título del rango,
  el `chakra / maxChakra` formateado). Es anterior a este ticket y es lo que hace todo el HUD de vanilla.
  Cachearlo obligaría a invalidar cuando cambia el valor observado — justo el tipo de estado derivado que §4
  descarta.
- **`HudLayout` es estado estático mutable.** Es deliberado y sigue el patrón de `ClientNinjaData`: hay un
  solo cliente por proceso, y pasar la colocación por parámetro hasta cada overlay no compraría nada.
- **Los overlays consultan `HudLayout.get()` por frame** en vez de recibir la colocación resuelta. Es una
  consulta a un `EnumMap` y mantiene cada overlay autónomo, que es lo que permite dibujarlos también desde el
  editor.

### 7.5. Guía para lo que venga

Al añadir un elemento nuevo al HUD:

- Declararlo en el enum `HudElement` (una línea): hereda editor, guardado, clamp y tolerancia del config.
- Recorrer con `HudElement.all()`, nunca con `values()`, en cualquier código por frame.
- No asignar en el camino de dibujado si el valor no cambia entre frames.
