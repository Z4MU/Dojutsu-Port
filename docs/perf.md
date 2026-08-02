# Rendimiento — auditoría del coste por tick (F10/DJT-222)

> 2026-08-01. Cubre **todos** los puntos de entrada por tick del mod. La conclusión en una línea: **el mod no
> tiene un problema de rendimiento por tick**; lo que sí tiene es un eje de escalado —los barridos de
> entidades— que crece con el contenido del mundo y no con el trabajo constante por jugador.

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
