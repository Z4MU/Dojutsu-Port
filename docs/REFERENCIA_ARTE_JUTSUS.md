# Referencia de arte — Jutsus

> **Propósito:** alimentar a una IA generadora de imágenes para obtener una **imagen de referencia** por jutsu,
> que después sirva de guía al modelar en **Blockbench**.
> **Cobertura actual:** ✅ Fuego (7/7). Pendientes: rayo, viento, tierra, agua, yin-yang, taijutsu, genjutsu,
> médico, fuinjutsu, kenjutsu, aburame, universales y los 16 Kekkei Genkai.
> **Fuente de los datos mecánicos:** los JSON de `data/dojutsu/jutsu/fire/` (verificados, no inventados).
> **Fuente de la descripción visual:** las texturas 2D que ya existen en
> `assets/dojutsu/textures/item/jutsu/*.png`, inspeccionadas una a una.

---

## Cómo usar este documento

Cada entrada tiene tres bloques:

1. **Qué hace** — la mecánica real en el mod (coste, efecto, daño). Contexto para que el arte tenga sentido.
2. **Qué debe representar el modelo** — la forma, el volumen y la silueta propuestos.
3. **Prompt** — texto listo para pegar en la IA de imágenes.

### Supuesto que he tomado (conviene confirmarlo)

He asumido que el modelo 3D es para el **ítem del jutsu** — el objeto que el jugador lleva en la mano y ve en el
inventario, hoy resuelto con una textura plana 2D vía `custom_model_data`. Es el caso que encaja con Blockbench
y con el único modelo 3D real que ya tiene el mod (`mask_tobi_3.json`).

Si lo que quieres modelar es el **efecto en el mundo** (la bola de fuego que vuela, el muro, el aura), dímelo:
las descripciones cambiarían bastante — dejarían de ser "objeto sostenible" para ser "volumen en vuelo con
estela", y la paleta y la escala serían otras.

### Estilo común a todas las piezas

Conviene que la IA lo reciba en cada prompt, así que va repetido en cada bloque, pero queda aquí para que sea
fácil de editar en un solo sitio:

- Estética **Minecraft**: voxel, caras planas, bordes duros, **sin degradados suaves ni antialiasing**.
- Paleta corta y saturada, 4–6 tonos por pieza como máximo.
- Fondo **transparente o gris neutro liso**, sin escenario ni personajes.
- Vista **3/4 (isométrica ligera)**, la más útil para reconstruir volúmenes en Blockbench.
- Objeto **centrado, completo y bien iluminado**, sin recortes ni sombras dramáticas.
- Sin texto, sin logos, sin marcos, sin firma.

---

## Fuego (Katon) — 7 jutsus

Los siete forman la **secuencia de aprendizaje del Skill Learner de fuego**, en este orden. La progresión
visual debería notarse: el primero es un proyectil menor y el último, una técnica de aniquilación.

| # | Jutsu | Coste | Enfriamiento | Requisito | Familia |
|---|---|---|---|---|---|
| 1 | Fire Bullet | 60 | 100 t | fire 30 | proyectil |
| 2 | Fireproof | 20 | 0 t | fire 50 | buff |
| 3 | Fire Transport | 80 | 100 t | fire 60 | teleport |
| 4 | Fire Dragon Flame Bullet | 100 | 100 t | fire 100 | proyectil |
| 5 | Exploding Dragon Strike | 150 | 400 t | fire 140 | proyectil |
| 6 | Great Fireball | 140 | 100 t | fire 170 | proyectil |
| 7 | Great Fire Annihilation | 180 | 200 t | fire 200 | proyectil |

---

### 1. Fire Bullet — *Katon: Endan*

**Qué hace.** La técnica de fuego más básica: escupe un proyectil compacto que vuela recto, prende lo que toca
en un radio de 3 bloques y deja al objetivo ardiendo 5 segundos. No hace daño directo por impacto — el daño es
la quemadura.

**Qué debe representar el modelo.** Un **proyectil pequeño y denso**, no una llamarada. La textura actual es un
disco achatado naranja, más ancho que alto, como una gota de fuego aplastada vista de canto. En 3D: un cuerpo
ovoide comprimido en el eje vertical, superficie irregular pero **maciza**, con el núcleo más claro que el
borde. Debe leerse como "munición", la pieza más humilde de las siete.

**Paleta.** Naranja quemado dominante `#E05A10`, núcleo amarillo-naranja `#F5A03C`, borde rojo oscuro `#8C2A06`.

**Silueta.** Compacta y redondeada, sin púas. Debe distinguirse de las demás por ser **la más pequeña y la más
lisa**.

> **Prompt:**
> Un pequeño proyectil de fuego comprimido, con forma de gota achatada más ancha que alta, superficie
> ligeramente irregular pero sólida y maciza, núcleo amarillo-naranja brillante que se oscurece hacia un borde
> rojo quemado. Sin llamas sueltas ni estela. Estilo Minecraft voxel: caras planas, bordes duros, sin
> degradados suaves ni antialiasing. Paleta de 4 tonos: naranja quemado, amarillo-naranja, rojo oscuro, negro
> de contorno. Objeto único centrado sobre fondo gris neutro liso, vista tres cuartos isométrica ligera,
> iluminación uniforme. Sin texto, sin marco, sin fondo decorativo.

---

### 2. Fireproof — *Katon: Kaen no Yoroi*

**Qué hace.** El único de los siete que **no es ofensivo**: concede resistencia al fuego al lanzador. Coste
mínimo (20) y sin enfriamiento, pensado para encadenarse antes de entrar en combate.

**Qué debe representar el modelo.** Es el único que debe leerse como **objeto de protección, no como fuego
proyectado**. La textura actual es un **escudo pequeño de contorno oscuro con un emblema rojo y amarillo** en el
centro. En 3D: una placa curvada tipo escudo, gruesa, con el borde biselado y un relieve central. La llama, si
aparece, debe estar **contenida dentro del emblema**, no escapando de la pieza.

**Paleta.** Contorno negro carbón `#1A1410`, cuerpo rojo profundo `#B32418`, emblema amarillo cálido `#F2C438`.

**Silueta.** **Rompe deliberadamente con las otras seis**: es la única con geometría plana, simétrica y de
contorno definido. En una fila de miniaturas debe reconocerse al instante como "la defensiva".

> **Prompt:**
> Un escudo ninja pequeño y grueso, de contorno oscuro y cuerpo rojo profundo, con un emblema de llama estilizada
> amarillo cálido grabado en relieve en el centro. Placa ligeramente curvada, bordes biselados, aspecto sólido y
> metálico. El fuego aparece únicamente como emblema grabado, nunca desbordando la pieza. Estilo Minecraft
> voxel: caras planas, bordes duros, sin degradados suaves ni antialiasing. Paleta de 4 tonos: negro carbón,
> rojo profundo, amarillo cálido, naranja de acento. Objeto único centrado sobre fondo gris neutro liso, vista
> tres cuartos isométrica ligera, iluminación uniforme. Sin texto, sin marco, sin fondo decorativo.

---

### 3. Fire Transport — *Katon: Shunshin*

**Qué hace.** Técnica de **desplazamiento**: reubica al lanzador entre 3 y 8 bloques. No daña ni incendia; es la
huida rápida envuelta en fuego.

**Qué debe representar el modelo.** Un **estallido radial**, no un proyectil. La textura actual es una
estrella/sol de puntas cortas y regulares, naranja intenso, con el centro más claro — la silueta de una
detonación vista de frente. En 3D: un núcleo esférico pequeño del que salen **puntas cónicas cortas repartidas
en todas las direcciones**, como una mina de erizo. La lectura debe ser "algo que se dispersa desde un punto",
que es exactamente lo que hace la técnica.

**Paleta.** Naranja intenso `#F26A0C`, centro amarillo pálido `#FFD98A`, puntas rojo `#C0300A`.

**Silueta.** **Radial y simétrica**, con puntas cortas y uniformes. Se diferencia del nº 4 en que aquí las
puntas son **regulares y limpias**; allí serán irregulares y agresivas.

> **Prompt:**
> Una explosión de fuego radial cristalizada en forma de objeto sólido: un núcleo esférico compacto del que
> irradian puntas cónicas cortas y regulares en todas las direcciones, como un erizo de llamas. Centro amarillo
> pálido brillante que se satura hacia naranja intenso en el cuerpo y rojo en la punta de cada pincho. Simetría
> radial limpia. Estilo Minecraft voxel: caras planas, bordes duros, sin degradados suaves ni antialiasing.
> Paleta de 4 tonos: amarillo pálido, naranja intenso, rojo, marrón oscuro de contorno. Objeto único centrado
> sobre fondo gris neutro liso, vista tres cuartos isométrica ligera, iluminación uniforme. Sin texto, sin
> marco, sin fondo decorativo.

---

### 4. Fire Dragon Flame Bullet — *Katon: Karyū Endan*

**Qué hace.** Primer jutsu de fuego que hace **daño de impacto** (6) además de incendiar. Proyectil más rápido
que el nº 1 (velocidad 1.2), radio 3, quemadura de 5 segundos.

**Qué debe representar el modelo.** La versión **agresiva** del nº 3: mismo esquema radial, pero con las puntas
**desiguales, más largas y afiladas**, y un cuerpo más denso y rugoso. La textura actual es una esfera erizada
naranja saturada, de contorno mucho más irregular que la de Fire Transport. En 3D: masa central claramente
esférica con lengüetas de llama de longitudes distintas, algunas curvadas hacia atrás como si el proyectil
estuviera en pleno vuelo.

**Paleta.** Naranja saturado `#F04E08`, núcleo amarillo `#FFC24D`, puntas rojo oscuro `#9E2205`.

**Silueta.** Radial pero **caótica**. Es el punto de inflexión de la serie: a partir de aquí las formas dejan de
ser limpias.

> **Prompt:**
> Un proyectil de fuego con forma de esfera erizada: masa central densa y rugosa de la que brotan lengüetas de
> llama de longitudes desiguales, algunas afiladas y otras curvadas hacia atrás como arrastradas por la
> velocidad. Núcleo amarillo intenso, cuerpo naranja saturado, puntas rojo oscuro. Aspecto agresivo y caótico,
> claramente en movimiento. Estilo Minecraft voxel: caras planas, bordes duros, sin degradados suaves ni
> antialiasing. Paleta de 4 tonos: amarillo, naranja saturado, rojo oscuro, negro de contorno. Objeto único
> centrado sobre fondo gris neutro liso, vista tres cuartos isométrica ligera, iluminación uniforme. Sin texto,
> sin marco, sin fondo decorativo.

---

### 5. Exploding Dragon Strike — *Katon: Ryūka no Jutsu*

**Qué hace.** El de **enfriamiento más largo de los siete** (400 ticks = 20 s), lo que lo convierte en la
técnica "de peso" del set. Radio de impacto 4, daño 10, quemadura de 5 segundos y **prende el suelo a su paso**.

**Qué debe representar el modelo.** El único donde debe aparecer una **cabeza de dragón**, aunque sea sugerida.
La textura actual es la más compleja de las siete: una masa de fuego naranja con un **núcleo rojo oscuro
diferenciado** y una silueta irregular que insinúa fauces. En 3D: cuerpo bulboso envuelto en llamas, con la
parte frontal resuelta como un **hocico abierto muy estilizado** — dos planos de mandíbula y una cavidad oscura
dentro. Nada de detalle orgánico fino: la lectura de "dragón" debe venir de la silueta y del contraste del
núcleo oscuro, no de escamas ni ojos.

**Paleta.** Naranja llama `#EE6414`, núcleo rojo oscuro `#7A1608`, brillo amarillo `#FFD070`, cavidad casi negra
`#2B0C04`.

**Silueta.** **Direccional**: tiene un frente y una cola clara, a diferencia de las esferas radiales anteriores.
Es la primera pieza del set que apunta hacia algún lado.

> **Prompt:**
> Una cabeza de dragón de fuego muy estilizada, formada por llamas sólidas: cuerpo bulboso envuelto en fuego
> naranja con un núcleo rojo oscuro claramente diferenciado, y una parte frontal que sugiere unas fauces
> abiertas mediante dos planos de mandíbula angulosos y una cavidad interior casi negra. Sin escamas, sin ojos,
> sin detalle orgánico fino: la lectura de dragón viene solo de la silueta. Forma direccional, con frente y
> estela definidos. Estilo Minecraft voxel: caras planas, bordes duros, sin degradados suaves ni antialiasing.
> Paleta de 5 tonos: amarillo brillante, naranja llama, rojo oscuro, marrón muy oscuro, negro de contorno.
> Objeto único centrado sobre fondo gris neutro liso, vista tres cuartos isométrica ligera, iluminación
> uniforme. Sin texto, sin marco, sin fondo decorativo.

---

### 6. Great Fireball — *Katon: Gōkakyū no Jutsu*

**Qué hace.** La técnica emblemática del clan Uchiha y la más reconocible del set. Radio 3, quemadura de 5
segundos, **prende el suelo**; no lleva daño de impacto declarado — el daño es el incendio. Es el jutsu con el
`custom_model_data` **301**, el primero del catálogo entero.

**Qué debe representar el modelo.** Una **esfera limpia**, y ahí está su fuerza: es el único de los siete cuya
textura actual es un círculo **liso y bien cerrado**, rojo-anaranjado con moteado interno. En 3D: bola casi
perfecta, superficie con relieve muy leve (facetas grandes, no púas), degradado por bandas del amarillo central
al rojo del borde. **Nada de lengüetas ni pinchos**: la contundencia viene de la masa, no del contorno.

**Paleta.** Rojo-naranja `#DD3F0C`, moteado amarillo-naranja `#F58A2A`, brillo central `#FFCE73`, borde rojo
profundo `#8E1A04`.

**Silueta.** **Un círculo perfecto.** En una fila de miniaturas debe ser la única inconfundiblemente esférica.
Ese contraste es intencionado: es la técnica más famosa y la que más limpio debe leerse.

> **Prompt:**
> Una gran bola de fuego esférica, de contorno limpio y perfectamente redondo, con superficie facetada de
> relieve muy leve y moteado interno de brasa. Degradado por bandas duras desde un brillo amarillo cálido en el
> centro hacia naranja y rojo profundo en el borde. Masa densa y contundente, sin lengüetas de llama, sin
> pinchos y sin estela. Estilo Minecraft voxel: caras planas, bordes duros, sin degradados suaves ni
> antialiasing. Paleta de 4 tonos: amarillo cálido, naranja, rojo-naranja, rojo profundo. Objeto único centrado
> sobre fondo gris neutro liso, vista tres cuartos isométrica ligera, iluminación uniforme. Sin texto, sin
> marco, sin fondo decorativo.

---

### 7. Great Fire Annihilation — *Katon: Gōka Mekkyaku*

**Qué hace.** El **techo del elemento**: radio 5, daño 12, quemadura de **10 segundos** (el doble que el resto) y
prende el suelo. Requiere afinidad de fuego 200, la más alta de la rama.

**Qué debe representar el modelo.** El **más grande y el más informe** de los siete. La textura actual es una
mancha de fuego amplia y desbordada, sin contorno reconocible, como una marea que se expande. En 3D: **masa
volumétrica irregular** que ocupa todo el espacio disponible, con protuberancias grandes y desiguales en varias
direcciones y bolsas de brasa más clara repartidas por dentro. No es un proyectil que vuela: es un **frente de
fuego** que arrasa.

**Paleta.** Naranja incendio `#F1580E`, bolsas de brasa `#FFB347`, sombras rojo profundo `#8B1E03`, puntos de
carbón `#2A0E05`.

**Silueta.** **Deliberadamente irregular y desbordada.** Debe ser la pieza que más área ocupa y la única sin un
centro geométrico claro — el opuesto exacto del nº 6.

> **Prompt:**
> Una masa de fuego enorme e informe, un frente de llamas que se expande: volumen irregular con grandes
> protuberancias desiguales en varias direcciones, sin contorno geométrico reconocible ni centro definido.
> Bolsas de brasa amarillo-anaranjada repartidas por el interior, sombras rojo profundo en los pliegues y
> algunos puntos de carbón oscuro. Sensación de escala y de avance imparable, no de proyectil. Estilo Minecraft
> voxel: caras planas, bordes duros, sin degradados suaves ni antialiasing. Paleta de 5 tonos: amarillo brasa,
> naranja incendio, rojo profundo, marrón carbón, negro de contorno. Objeto único centrado sobre fondo gris
> neutro liso, vista tres cuartos isométrica ligera, iluminación uniforme. Sin texto, sin marco, sin fondo
> decorativo.

---

## Nota sobre la coherencia del set

Las siete siluetas están pensadas para **no repetirse entre sí**, porque en el inventario se ven como
miniaturas de 16×16 y ahí sólo se distingue el contorno:

| # | Jutsu | Silueta | Tamaño relativo |
|---|---|---|---|
| 1 | Fire Bullet | Gota achatada lisa | El más pequeño |
| 2 | Fireproof | Escudo plano simétrico | Pequeño |
| 3 | Fire Transport | Estrella radial regular | Medio |
| 4 | Fire Dragon Flame Bullet | Estrella radial caótica | Medio |
| 5 | Exploding Dragon Strike | Cabeza direccional | Medio-grande |
| 6 | Great Fireball | Esfera perfecta | Grande |
| 7 | Great Fire Annihilation | Mancha informe | El más grande |

---

## Pendiente de decidir antes de modelar

1. **Ítem o efecto en el mundo** (ver el supuesto al principio). Si es efecto, este documento hay que rehacerlo.
2. **`fire_cloak`** cierra la secuencia del learner en los datos pero **el jutsu no existe** (ver la auditoría,
   §1.6). Si se va a implementar, necesitará su propia entrada aquí: sería un **aura envolvente**, no un objeto
   sostenible, y ese es un problema de arte distinto.
3. **Resolución de destino de la textura.** Los jutsus actuales son 16×16. Un modelo de Blockbench admite más
   detalle, pero conviene fijar la resolución antes de generar referencias para no diseñar algo que luego no se
   puede texturizar en el presupuesto elegido.
