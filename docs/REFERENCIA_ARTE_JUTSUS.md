# Referencia de arte — Efectos de jutsu en el mundo

> **Propósito:** alimentar a una IA generadora de imágenes para obtener una **imagen de referencia** por jutsu,
> que después sirva de guía al modelar en **Blockbench**.
> **Qué se modela:** **el efecto que aparece al activar el jutsu** — la bola de fuego que vuela, el rayo que
> envuelve la mano, la descarga que cubre el área. **No** el icono del ítem.
> **Cobertura actual:** ✅ Fuego (7/7) · ✅ Rayo (8/8). Pendientes: viento, tierra, agua, yin-yang, taijutsu,
> genjutsu, médico, fuinjutsu, kenjutsu, aburame, universales y los 16 Kekkei Genkai.
> **Fuente de los datos mecánicos:** los JSON de `data/dojutsu/jutsu/`, verificados uno a uno.
> **Fuente del criterio visual:** las 15 texturas 2D existentes, inspeccionadas una a una.

---

## 1. Cómo se manifiesta hoy cada familia de efecto

Esto condiciona todo lo demás, porque determina **si el modelo tiene dónde colgarse o hay que programarlo
antes**. Verificado en el código, no supuesto.

| Familia | Qué existe hoy en el mundo | ¿Hay dónde poner un modelo? |
|---|---|---|
| `projectile` | Una entidad real `dojutsu:ninja_projectile` (hitbox 0.25×0.25) que vuela, colisiona por raycast y arrastra un aura que incendia. Se dibuja con `ThrownItemRenderer`, es decir **como el ítem vanilla que indique `displayItem`** (`fire_charge`, `phantom_membrane`, `end_rod`…) | ✅ **Sí.** Basta sustituir el renderer por uno que dibuje el modelo |
| `primed_strike` | **Nada visible** mientras el golpe está cargado. Al descargar, invoca un **rayo vanilla** (`LightningBolt`) sobre el objetivo | ❌ **No.** Hace falta programar el visual del periodo de carga |
| `area` | **Nada en absoluto.** Aplica daño y efectos de poción a las entidades del radio, sin una sola partícula | ❌ **No.** Hace falta programar el visual completo |
| `teleport` | Partículas `POOF` en el punto de origen | ❌ **No** (sólo partículas) |
| `buff` | **Nada.** Sólo el efecto de poción sobre el lanzador | ❌ **No** |
| `structure` | Los bloques colocados (son bloques vanilla reales) | ➖ No aplica |

**Consecuencia práctica para tu plan:**

- De los **7 jutsus de fuego**, **5 son proyectiles** → tienen entidad y el modelo entra casi directo.
- De los **8 jutsus de rayo**, **ninguno es proyectil** (4 `primed_strike`, 3 `area`, 1 `teleport`) → **los ocho
  necesitan trabajo de programación** antes de que un modelo se pueda ver en partida.

No es un motivo para no diseñarlos: tener la referencia de arte primero es sensato. Pero conviene saber que
rayo no es "modelar y listo" como fuego.

---

## 2. Estilo común a todas las piezas

Va repetido dentro de cada prompt para que sean autónomos, pero se edita aquí:

- Estética **Minecraft**: voxel, caras planas, bordes duros, **sin degradados suaves ni antialiasing**.
- Paleta corta y saturada: 4–6 tonos por pieza.
- Fondo **transparente o gris neutro liso**. Sin escenario, sin paisaje, sin personajes.
- Vista **3/4 (isométrica ligera)**, la más útil para reconstruir volúmenes en Blockbench.
- Objeto **centrado, completo, bien iluminado**, sin recortes ni sombras dramáticas.
- **Emisivo**: estos efectos son luz. El color debe leerse como si brillara por sí mismo, no como plástico
  iluminado desde fuera.
- Sin texto, sin logos, sin marcos, sin firma.

**Sobre la escala.** Cada entrada indica el tamaño aproximado **en bloques de Minecraft** (1 bloque = 16 px).
Es el dato que más falta hace al pasar a Blockbench y el que una imagen suelta nunca transmite.

**Sobre la resolución de textura.** Las texturas existentes son **32×32** (no 16×16), salvo
`drilling_hand_of_sage_lightning`, que es **64×128** — un caso raro que conviene revisar (§6).

---

## 3. Fuego (Katon) — 7 jutsus

Los siete son la secuencia de aprendizaje del Skill Learner de fuego, en este orden.

| # | Jutsu | Familia | Coste | Radio / alcance | Daño |
|---|---|---|---|---|---|
| 1 | Fire Bullet | proyectil | 60 | aura 3 bl., vel. 1.0 | — (sólo quemadura 5 s) |
| 2 | Fireproof | buff | 20 | sobre el lanzador | — |
| 3 | Fire Transport | teleport | 80 | 3–8 bl. | — |
| 4 | Fire Dragon Flame Bullet | proyectil | 100 | aura 3 bl., vel. 1.2 | 6 |
| 5 | Exploding Dragon Strike | proyectil | 150 | aura 4 bl., vel. 1.0 | 10 |
| 6 | Great Fireball | proyectil | 140 | aura 3 bl., vel. 1.0 | — (quemadura + incendia suelo) |
| 7 | Great Fire Annihilation | proyectil | 180 | aura 5 bl., vel. 1.0 | 12 |

---

### 1. Fire Bullet — *Katon: Endan* · proyectil

**Qué hace.** Escupe un proyectil compacto que vuela recto 5 segundos, prende a todo lo vivo en 3 bloques a su
alrededor y deja ardiendo 5 segundos. No hace daño de impacto: el daño es la quemadura.

**Qué se ve hoy.** Una `fire_charge` vanilla volando.

**Qué debe representar el modelo.** El **proyectil en vuelo**, el más humilde del set. Un cuerpo ovoide de
**0.5 × 0.4 × 0.6 bloques**, comprimido en vertical y alargado en la dirección de vuelo, con el núcleo más
claro que la superficie. Una estela corta detrás — dos o tres jirones de llama pegados al cuerpo, nunca sueltos.
Debe leerse como **munición**, no como llamarada.

**Paleta.** Naranja quemado `#E05A10`, núcleo amarillo `#F5A03C`, borde rojo oscuro `#8C2A06`.

> **Prompt:**
> Un proyectil de fuego pequeño en pleno vuelo, cuerpo ovoide compacto ligeramente aplastado y alargado en la
> dirección del movimiento, de unos 0,5 bloques de Minecraft de ancho. Núcleo amarillo brillante que se satura
> hacia naranja quemado y rojo oscuro en el borde. Estela muy corta: dos o tres jirones de llama pegados al
> cuerpo, sin chispas sueltas. Efecto emisivo, brilla por sí mismo. Estilo Minecraft voxel: caras planas,
> bordes duros, sin degradados suaves ni antialiasing. Paleta de 4 tonos. Objeto único centrado sobre fondo gris
> neutro liso, vista tres cuartos isométrica ligera, sin personajes ni escenario. Sin texto, sin marco.

---

### 2. Fireproof — *Katon: Kaen no Yoroi* · buff

**Qué hace.** El único no ofensivo: da resistencia al fuego al lanzador. Coste 20, sin enfriamiento.

**Qué se ve hoy.** **Nada.** Ni partículas.

**Qué debe representar el modelo.** No es un objeto: es un **aura que envuelve al jugador**. Una cáscara
ovoide de **1.2 × 2.2 × 1.2 bloques** (justo por fuera de la silueta del jugador), **abierta y translúcida**,
formada por lengüetas de llama verticales que suben desde los pies y se afinan por encima de la cabeza. Debe
verse el interior — no puede ser una burbuja opaca o taparía al jugador.

**Paleta.** Naranja translúcido `#F2731C`, puntas amarillo pálido `#FFDE9E`, base rojo `#B33A0C`.

> **Prompt:**
> Un aura protectora de fuego con forma de cáscara ovoide vertical, del tamaño justo para envolver a un
> personaje de Minecraft, hueca y abierta para que se vea el interior. Está formada por lengüetas de llama
> verticales separadas entre sí, anchas y densas en la base y afinándose hasta desaparecer por encima de la
> parte superior. Translúcida, sin superficie sólida ni burbuja cerrada. Efecto emisivo. Estilo Minecraft
> voxel: caras planas, bordes duros, sin degradados suaves ni antialiasing. Paleta de 4 tonos: amarillo pálido,
> naranja, rojo, negro de contorno. Aura sola centrada sobre fondo gris neutro liso, vista tres cuartos
> isométrica ligera, sin personaje dentro. Sin texto, sin marco.

---

### 3. Fire Transport — *Katon: Shunshin* · teleport

**Qué hace.** Reubica al lanzador entre 3 y 8 bloques. No daña ni incendia: es la huida envuelta en fuego.

**Qué se ve hoy.** Partículas `POOF` en el punto de salida.

**Qué debe representar el modelo.** El **estallido que queda atrás** al desaparecer. Un burst radial de
**2 × 2 × 2 bloques**: núcleo esférico pequeño del que salen puntas cónicas cortas y **regulares** en todas
direcciones, como una mina de erizo. La textura actual (sol de puntas limpias) apunta exactamente ahí. La
lectura debe ser "algo que se dispersó desde un punto", que es literalmente lo que pasó.

**Paleta.** Naranja intenso `#F26A0C`, centro amarillo pálido `#FFD98A`, puntas rojo `#C0300A`.

> **Prompt:**
> Una explosión de fuego radial congelada en el instante de expandirse: núcleo esférico compacto del que
> irradian puntas cónicas cortas y regulares en todas las direcciones, con simetría limpia, de unos 2 bloques de
> Minecraft de diámetro. Centro amarillo pálido muy brillante que se satura a naranja intenso y termina en rojo
> en cada punta. Efecto emisivo. Estilo Minecraft voxel: caras planas, bordes duros, sin degradados suaves ni
> antialiasing. Paleta de 4 tonos. Efecto único centrado sobre fondo gris neutro liso, vista tres cuartos
> isométrica ligera, sin personajes ni escenario. Sin texto, sin marco.

---

### 4. Fire Dragon Flame Bullet — *Katon: Karyū Endan* · proyectil

**Qué hace.** Primer jutsu de fuego con daño de impacto (6). El más rápido del set (velocidad 1.2), aura 3.

**Qué se ve hoy.** Una `fire_charge` vanilla volando.

**Qué debe representar el modelo.** La versión **agresiva** del nº 3, pero en vuelo. Esfera erizada de
**0.9 × 0.9 × 1.2 bloques**, con lengüetas de llama **desiguales**, algunas largas y afiladas, otras curvadas
hacia atrás por la velocidad. Cuerpo denso y rugoso, no hueco. Es el punto de inflexión de la serie: a partir de
aquí las formas dejan de ser limpias.

**Paleta.** Naranja saturado `#F04E08`, núcleo amarillo `#FFC24D`, puntas rojo oscuro `#9E2205`.

> **Prompt:**
> Un proyectil de fuego en vuelo rápido con forma de esfera erizada de casi un bloque de Minecraft de diámetro:
> masa central densa y rugosa de la que brotan lengüetas de llama de longitudes desiguales, unas afiladas hacia
> delante y otras curvadas hacia atrás como arrastradas por la velocidad. Núcleo amarillo intenso, cuerpo
> naranja saturado, puntas rojo oscuro. Aspecto agresivo y claramente en movimiento. Efecto emisivo. Estilo
> Minecraft voxel: caras planas, bordes duros, sin degradados suaves ni antialiasing. Paleta de 4 tonos. Objeto
> único centrado sobre fondo gris neutro liso, vista tres cuartos isométrica ligera, sin personajes ni
> escenario. Sin texto, sin marco.

---

### 5. Exploding Dragon Strike — *Katon: Ryūka no Jutsu* · proyectil

**Qué hace.** El de enfriamiento más largo del set (20 s), lo que lo convierte en la técnica "de peso". Aura 4,
daño 10, prende el suelo a su paso.

**Qué se ve hoy.** Una `fire_charge` vanilla volando.

**Qué debe representar el modelo.** El único donde debe aparecer una **cabeza de dragón**, aunque sea sugerida.
Cuerpo de **1.2 × 1.2 × 2.0 bloques**, claramente **direccional** (tiene frente y cola, a diferencia de las
esferas anteriores). Masa bulbosa envuelta en llamas con la parte frontal resuelta como un **hocico abierto muy
estilizado**: dos planos de mandíbula angulosos y una cavidad oscura dentro. La textura actual ya insinúa esas
fauces y tiene un núcleo rojo oscuro diferenciado — conviene conservarlo, es lo que le da profundidad.
**Nada de escamas ni ojos:** la lectura de "dragón" viene de la silueta y del contraste, no del detalle.

**Paleta.** Naranja llama `#EE6414`, núcleo rojo oscuro `#7A1608`, brillo amarillo `#FFD070`, cavidad casi negra
`#2B0C04`.

> **Prompt:**
> Una cabeza de dragón de fuego muy estilizada surcando el aire, de unos 2 bloques de Minecraft de largo:
> cuerpo bulboso envuelto en llamas naranjas con un núcleo rojo oscuro claramente diferenciado, y una parte
> frontal que sugiere unas fauces abiertas mediante dos planos de mandíbula angulosos y una cavidad interior
> casi negra. Sin escamas, sin ojos, sin detalle orgánico fino: la lectura de dragón viene solo de la silueta.
> Forma direccional con frente definido y estela de llamas detrás. Efecto emisivo. Estilo Minecraft voxel:
> caras planas, bordes duros, sin degradados suaves ni antialiasing. Paleta de 5 tonos. Objeto único centrado
> sobre fondo gris neutro liso, vista tres cuartos isométrica ligera, sin personajes ni escenario. Sin texto,
> sin marco.

---

### 6. Great Fireball — *Katon: Gōkakyū no Jutsu* · proyectil

**Qué hace.** La técnica emblemática del clan Uchiha y la más reconocible del set. Aura 3, prende el suelo,
quemadura de 5 s. Es el primer jutsu del catálogo entero (`custom_model_data` 301).

**Qué se ve hoy.** Una `fire_charge` vanilla volando.

**Qué debe representar el modelo.** Una **esfera limpia**, y ahí está su fuerza. Bola de **2 × 2 × 2 bloques**
casi perfecta, con relieve muy leve (facetas grandes, no púas) y moteado de brasa por dentro. Degradado por
bandas duras del amarillo central al rojo del borde. **Nada de lengüetas ni pinchos:** la contundencia viene de
la masa, no del contorno. Es el proyectil más grande del set salvo el nº 7, y el único perfectamente esférico.

**Paleta.** Rojo-naranja `#DD3F0C`, moteado `#F58A2A`, brillo central `#FFCE73`, borde rojo profundo `#8E1A04`.

> **Prompt:**
> Una gran bola de fuego esférica de unos 2 bloques de Minecraft de diámetro, de contorno limpio y
> perfectamente redondo, con superficie facetada de relieve muy leve y moteado interno de brasa. Degradado por
> bandas duras desde un brillo amarillo cálido en el centro hacia naranja y rojo profundo en el borde. Masa
> densa y contundente, sin lengüetas de llama, sin pinchos, sin estela. Efecto emisivo, brilla por sí mismo.
> Estilo Minecraft voxel: caras planas, bordes duros, sin degradados suaves ni antialiasing. Paleta de 4 tonos.
> Objeto único centrado sobre fondo gris neutro liso, vista tres cuartos isométrica ligera, sin personajes ni
> escenario. Sin texto, sin marco.

---

### 7. Great Fire Annihilation — *Katon: Gōka Mekkyaku* · proyectil

**Qué hace.** El techo del elemento: aura **5** (la mayor), daño 12, quemadura de **10 segundos** (el doble que
el resto) y prende el suelo. Exige afinidad de fuego 200.

**Qué se ve hoy.** Una `fire_charge` vanilla volando — y es donde más chirría, porque debería ser una marea.

**Qué debe representar el modelo.** El **más grande y el más informe**. Masa volumétrica irregular de
**3.5 × 2.5 × 3 bloques**, con protuberancias grandes y desiguales en varias direcciones y bolsas de brasa más
clara repartidas por dentro. **No es un proyectil que vuela: es un frente de fuego que avanza.** Sin centro
geométrico claro — el opuesto exacto del nº 6.

**Paleta.** Naranja incendio `#F1580E`, bolsas de brasa `#FFB347`, sombras rojo profundo `#8B1E03`, carbón
`#2A0E05`.

> **Prompt:**
> Una masa de fuego enorme e informe avanzando como un frente de llamas, de unos 3 bloques de Minecraft de
> ancho: volumen irregular con grandes protuberancias desiguales en varias direcciones, sin contorno
> geométrico reconocible ni centro definido. Bolsas de brasa amarillo-anaranjada repartidas por el interior,
> sombras rojo profundo en los pliegues y algunos puntos de carbón oscuro. Sensación de escala y de avance
> imparable, no de proyectil. Efecto emisivo. Estilo Minecraft voxel: caras planas, bordes duros, sin
> degradados suaves ni antialiasing. Paleta de 5 tonos. Efecto único centrado sobre fondo gris neutro liso,
> vista tres cuartos isométrica ligera, sin personajes ni escenario. Sin texto, sin marco.

---

## 4. Rayo (Raiton) — 8 jutsus

**Ninguno es proyectil.** Cuatro son `primed_strike` (se cargan en la mano y se descargan al golpear), tres son
`area` (estallido alrededor del lanzador) y uno es `teleport`. Es un elemento **de contacto**, no de disparo, y
eso debe notarse en el arte: aquí casi todo va **pegado al cuerpo**.

| # | Jutsu | Familia | Coste | Datos | Daño |
|---|---|---|---|---|---|
| 1 | Lightning Bolt | primed_strike | 60 | carga 1,5 s | 6 |
| 2 | Lightning Instantaneous Body | teleport | 80 | 4–10 bl. | — |
| 3 | Lightning Net | area | 80 | radio 8, slowness II 4 s | 6 |
| 4 | Tiger Scissors | area | 100 | radio 4 | 8 |
| 5 | Chidori | primed_strike | 100 | carga 3 s | 10 |
| 6 | Lightning Cutter | primed_strike | 200 | carga 2 s | 12 |
| 7 | Kirin | area | 300 | radio 8, glowing 3 s | 16 |
| 8 | Drilling Hand of Sage Lightning | primed_strike | 400 | carga 3 s | 14 |

> **Nota sobre los colores del arte existente.** Las texturas actuales no siguen un criterio único: Chidori y
> Kirin son cian, Lightning Cutter es **violeta**, Tiger Scissors es dorado y Lightning Bolt es un crema liso.
> En el material original Chidori y Lightning Cutter son la **misma técnica** (la segunda es la versión
> perfeccionada), así que el violeta rompe esa relación. Abajo propongo unificarlos en la gama cian-blanco y
> reservar el violeta para otra cosa; si prefieres conservar el arte tal cual, avísame y cambio los prompts.

---

### 1. Lightning Bolt — *Raiton: Inazuma* · primed_strike

**Qué hace.** El golpe cargado más básico. Al castear **arma** la mano durante 1,5 segundos; el siguiente golpe
cuerpo a cuerpo descarga 6 de daño y **cae un rayo vanilla** sobre el objetivo.

**Qué se ve hoy.** Nada durante la carga. El rayo del impacto es el de Minecraft.

**Qué debe representar el modelo.** El **arco eléctrico que envuelve el puño** durante la ventana de carga.
Una masa suelta de **0.7 × 0.7 × 0.7 bloques** alrededor de la mano: cuatro o cinco arcos angulosos en zigzag
que nacen de los nudillos y se cierran sobre sí mismos, con un núcleo pálido. Debe verse **escaso** — es el
primer peldaño de la rama y el nº 5 y el nº 8 tienen que poder superarlo claramente.

**Paleta.** Crema eléctrica `#E8DCA8`, núcleo blanco `#FFFAE8`, sombra `#B8A874`.

> **Prompt:**
> Un arco eléctrico pequeño envolviendo un puño, de unos 0,7 bloques de Minecraft: cuatro o cinco descargas
> angulosas en zigzag que nacen de los nudillos y se curvan sobre sí mismas, dejando mucho aire entre ellas.
> Núcleo blanco muy brillante con los brazos en crema eléctrica y sombras doradas apagadas. Aspecto escaso y
> contenido, la versión más débil de una técnica eléctrica. Puede incluirse una mano cúbica estilizada de
> Minecraft solo como referencia de escala, en gris plano. Efecto emisivo. Estilo Minecraft voxel: caras
> planas, bordes duros, sin degradados suaves ni antialiasing. Paleta de 4 tonos. Centrado sobre fondo gris
> neutro liso, vista tres cuartos isométrica ligera. Sin texto, sin marco.

---

### 2. Lightning Instantaneous Body — *Raiton: Shunshin* · teleport

**Qué hace.** Reubica al lanzador entre 4 y 10 bloques. Sin daño.

**Qué se ve hoy.** Partículas `POOF`.

**Qué debe representar el modelo.** El **destello que queda atrás**. Disco radial de **2 × 2 bloques**, plano y
orientado hacia el espectador, con **rayos finos y rectos** irradiando desde un centro brillante — la textura
actual es exactamente eso, un sol de púas finas y regulares en dorado apagado. Debe leerse como **velocidad**,
no como explosión: púas largas, delgadas y limpias, muy distinto del burst grueso de Fire Transport.

**Paleta.** Dorado apagado `#D9C87E`, centro blanco `#FFFBEA`, puntas pálidas `#F0E4B0`.

> **Prompt:**
> Un destello eléctrico radial y plano, de unos 2 bloques de Minecraft de diámetro, formado por rayos muy finos,
> rectos y regulares que irradian desde un centro pequeño y blanco muy brillante. Las púas son largas y
> delgadas, con aspecto de velocidad pura, no de explosión. Tono dorado apagado, casi pálido. Efecto emisivo.
> Estilo Minecraft voxel: caras planas, bordes duros, sin degradados suaves ni antialiasing. Paleta de 4 tonos.
> Efecto único centrado sobre fondo gris neutro liso, vista tres cuartos isométrica ligera, sin personajes ni
> escenario. Sin texto, sin marco.

---

### 3. Lightning Net — *Raiton: Kaminari no Ami* · area

**Qué hace.** El de **mayor alcance del set junto a Kirin** (radio 8) y el de enfriamiento más largo (40 s).
Daño 6 y slowness II 4 s a todo lo que atrapa.

**Qué se ve hoy.** Nada.

**Qué debe representar el modelo.** Es el **más literal de los ocho** y el más fácil de reconocer: una **red**.
La textura actual es una malla de rombos entrelazados en cian y blanco, y hay que respetarla. En 3D: una
**cúpula o malla abierta de 8 bloques de radio** formada por cables eléctricos rectos que se cruzan en nudos
brillantes, dejando huecos romboidales grandes. Estructura, no masa: debe verse a través de ella de lado a
lado.

**Paleta.** Cian claro `#9FE8F5`, nudos blanco `#FFFFFF`, sombra azul `#4FA8C4`.

> **Prompt:**
> Una red eléctrica en forma de malla abierta y amplia, construida con cables de rayo rectos que se cruzan
> formando huecos romboidales grandes, con nudos blancos muy brillantes en cada intersección. Estructura
> completamente calada: se ve a través de ella de lado a lado, no es una masa sólida. Cian claro con sombras
> azules. Amplia y baja, cubriendo un área extensa. Efecto emisivo. Estilo Minecraft voxel: caras planas,
> bordes duros, sin degradados suaves ni antialiasing. Paleta de 4 tonos. Efecto único centrado sobre fondo
> gris neutro liso, vista tres cuartos isométrica ligera, sin personajes ni escenario. Sin texto, sin marco.

---

### 4. Tiger Scissors — *Raiton: Tora Hasami* · area

**Qué hace.** Estallido corto alrededor del lanzador (radio 4), daño 8, sin efectos de estado. El más directo
de los tres de área.

**Qué se ve hoy.** Nada.

**Qué debe representar el modelo.** Un **estallido denso y compacto**, opuesto a la red del nº 3. Esfera
irregular de **4 × 4 × 4 bloques** completamente **rellena de púas cortas y apretadas**, sin huecos por los que
mirar. La textura actual es un sol dorado de púas densas y así debe quedarse: **el dorado lo separa de los tres
cian del set** y le da identidad propia.

**Paleta.** Dorado eléctrico `#E8C63A`, centro pálido `#FFF0A0`, sombra ámbar `#A8801C`.

> **Prompt:**
> Un estallido eléctrico denso y compacto con forma de esfera erizada de unos 4 bloques de Minecraft, cubierta
> por completo de púas cortas y muy apretadas, sin huecos ni transparencia. Centro amarillo pálido muy
> brillante que se satura hacia dorado eléctrico y ámbar en las puntas. Aspecto macizo y contundente, lo
> contrario de una estructura calada. Efecto emisivo. Estilo Minecraft voxel: caras planas, bordes duros, sin
> degradados suaves ni antialiasing. Paleta de 4 tonos dorados. Efecto único centrado sobre fondo gris neutro
> liso, vista tres cuartos isométrica ligera, sin personajes ni escenario. Sin texto, sin marco.

---

### 5. Chidori — *Chidori* · primed_strike

**Qué hace.** La técnica más icónica del elemento. Carga 3 segundos y descarga 10 de daño con un rayo sobre el
objetivo. Requiere afinidad de rayo 120.

**Qué se ve hoy.** Nada durante la carga.

**Qué debe representar el modelo.** La **esfera de mil pájaros concentrada en la mano**. Bola de
**0.9 × 0.9 × 0.9 bloques** de arcos eléctricos densamente entrelazados alrededor del puño, con el núcleo casi
blanco y los brazos saliendo en zigzag corto y **caótico** hacia todas las direcciones. Frente al nº 1, aquí la
masa es **compacta y llena**: se nota que es una concentración, no unas chispas.

**Paleta.** Cian pálido `#8FD8F0`, núcleo blanco `#FFFFFF`, azul profundo `#3A7FA8`.

> **Prompt:**
> Una esfera densa de electricidad concentrada alrededor de un puño, de casi un bloque de Minecraft de
> diámetro: decenas de arcos en zigzag corto entrelazados de forma caótica formando una masa compacta y llena,
> con un núcleo casi blanco cegador del que salen brazos cian pálido y azul profundo en las puntas. Sensación
> de energía comprimida a punto de descargarse. Puede incluirse una mano cúbica estilizada de Minecraft solo
> como referencia de escala, en gris plano. Efecto emisivo. Estilo Minecraft voxel: caras planas, bordes duros,
> sin degradados suaves ni antialiasing. Paleta de 4 tonos. Centrado sobre fondo gris neutro liso, vista tres
> cuartos isométrica ligera. Sin texto, sin marco.

---

### 6. Lightning Cutter — *Raikiri* · primed_strike

**Qué hace.** La versión perfeccionada del Chidori: carga **más rápido** (2 s en vez de 3) y pega más fuerte
(12 en vez de 10). Coste 200, afinidad 200.

**Qué se ve hoy.** Nada durante la carga.

**Qué debe representar el modelo.** Lo mismo que el Chidori pero **más afilado y más ordenado** — es la versión
dominada de la técnica, y eso se transmite con **disciplina de la forma**, no con más caos. Masa de
**0.8 × 0.8 × 1.4 bloques**: núcleo compacto en la mano del que sale **una punta alargada y recta hacia
delante**, como una hoja. El resto de arcos, cortos y pegados. La lectura es "esto corta", frente al "esto
estalla" del nº 5.

**Paleta.** Recomendada: cian-blanco como Chidori pero con más blanco — blanco `#FFFFFF`, cian `#A8E4F5`, azul
`#2E6E96`.
**Alternativa (arte actual):** violeta `#A96FD9`, lila pálido `#D9B8F0`, púrpura `#6B3A9E`.

> **Prompt:**
> Una concentración de electricidad en la mano que se prolonga hacia delante en una única punta alargada, recta
> y afilada como una hoja de cuchillo, de aproximadamente 1,4 bloques de Minecraft de largo. Núcleo compacto y
> blanco cegador en la base, arcos secundarios cortos y pegados al cuerpo, todo muy ordenado y direccional.
> Transmite control y filo, no caos. Tonos blanco y cian con sombras azul profundo. Puede incluirse una mano
> cúbica estilizada de Minecraft solo como referencia de escala, en gris plano. Efecto emisivo. Estilo
> Minecraft voxel: caras planas, bordes duros, sin degradados suaves ni antialiasing. Paleta de 4 tonos.
> Centrado sobre fondo gris neutro liso, vista tres cuartos isométrica ligera. Sin texto, sin marco.

---

### 7. Kirin — *Kirin* · area

**Qué hace.** El **golpe más fuerte del elemento** (16 de daño, radio 8) y el de enfriamiento más largo de todo
el rayo (60 s). Marca a los alcanzados con `glowing` 3 s. Afinidad 500.

**Qué se ve hoy.** Nada. Es el que más pide un visual a gritos.

**Qué debe representar el modelo.** El único que debe venir **de arriba**: es un rayo natural atraído desde las
nubes. Columna vertical de **2 × 12 × 2 bloques** que baja desde muy alto y se abre al llegar al suelo en un
estallido radial. La parte alta es un **canal grueso y anguloso** con ramificaciones cortas; la base, una
corona de arcos que se dispersan a 8 bloques. Debe leerse como **descarga descendente**, algo que ninguna otra
pieza del set hace.

**Paleta.** Cian `#6FD4F0`, canal blanco `#EAFBFF`, ramas azul `#2E8FB8`, base violácea `#7B6FD4`.

> **Prompt:**
> Un rayo descendente colosal: una columna vertical gruesa y angulosa que baja desde lo alto, con ramificaciones
> cortas a los lados, y que al llegar abajo se abre en una corona radial de arcos eléctricos dispersándose por
> el suelo. Proporción muy alta y estrecha, unas seis veces más alta que ancha. Canal central blanco cegador,
> cuerpo cian y ramas azules, con un tinte violáceo en la base. Sensación de descarga natural venida del cielo,
> enorme. Efecto emisivo. Estilo Minecraft voxel: caras planas, bordes duros, sin degradados suaves ni
> antialiasing. Paleta de 5 tonos. Efecto único centrado sobre fondo gris neutro liso, vista tres cuartos
> isométrica ligera, sin personajes ni escenario. Sin texto, sin marco.

---

### 8. Drilling Hand of Sage Lightning — *Raiton: Sennin no Kiritsu* · primed_strike

**Qué hace.** El **más caro del elemento** (400 de chakra, afinidad 500) y el golpe cargado más fuerte (14).
Carga 3 s. Cierra la rama de rayo.

**Qué se ve hoy.** Nada durante la carga.

**Qué debe representar el modelo.** El **taladro** — el nombre lo pide y la textura actual (64×128, la única
alta del catálogo) ya sugiere una forma alargada. Cono helicoidal de **1 × 1 × 2.5 bloques** que sale del
antebrazo hacia delante: **espirales eléctricas que giran alrededor de un eje central** y convergen en una
punta. Frente al nº 6 (una hoja recta y quieta), aquí la lectura es **rotación**. Es la pieza más larga y
elaborada de las cuatro de mano, como corresponde al techo de la rama.

**Paleta.** Cian `#4FD0EA`, núcleo blanco `#FFFFFF`, espiral azul profundo `#1F7A96`, chispas pálidas `#CFF6FF`.

> **Prompt:**
> Un taladro de electricidad proyectado hacia delante desde un antebrazo: un cono alargado de unos 2,5 bloques
> de Minecraft en el que varias bandas de rayo se enroscan en espiral alrededor de un eje central blanco
> cegador y convergen en una punta afilada. La torsión helicoidal debe ser muy visible, transmitiendo rotación
> y perforación. Cuerpo cian con las espirales en azul profundo y chispas pálidas desprendiéndose de la punta.
> Puede incluirse un antebrazo cúbico estilizado de Minecraft solo como referencia de escala, en gris plano.
> Efecto emisivo. Estilo Minecraft voxel: caras planas, bordes duros, sin degradados suaves ni antialiasing.
> Paleta de 5 tonos. Centrado sobre fondo gris neutro liso, vista tres cuartos isométrica ligera. Sin texto,
> sin marco.

---

## 5. Coherencia entre piezas

Las formas están pensadas para no repetirse **dentro de cada elemento** y para que fuego y rayo no se confundan
entre sí.

**Fuego — todo vuela o envuelve, todo es masa:**

| # | Jutsu | Forma | Tamaño aprox. |
|---|---|---|---|
| 1 | Fire Bullet | Ovoide liso en vuelo | 0,5 bl. |
| 2 | Fireproof | Cáscara envolvente hueca | 1,2 × 2,2 bl. |
| 3 | Fire Transport | Burst radial regular | 2 bl. |
| 4 | Fire Dragon Flame Bullet | Esfera erizada caótica | 0,9 bl. |
| 5 | Exploding Dragon Strike | Cabeza direccional | 2 bl. de largo |
| 6 | Great Fireball | Esfera perfecta | 2 bl. |
| 7 | Great Fire Annihilation | Masa informe | 3,5 bl. |

**Rayo — casi todo va pegado al cuerpo, todo es estructura angulosa:**

| # | Jutsu | Forma | Tamaño aprox. | Dónde aparece |
|---|---|---|---|---|
| 1 | Lightning Bolt | Arcos sueltos | 0,7 bl. | Mano |
| 2 | Lightning Instantaneous Body | Disco de púas finas | 2 bl. | Punto de salida |
| 3 | Lightning Net | Malla calada | 8 bl. radio | Alrededor |
| 4 | Tiger Scissors | Esfera de púas densas | 4 bl. | Alrededor |
| 5 | Chidori | Bola compacta | 0,9 bl. | Mano |
| 6 | Lightning Cutter | Hoja recta | 1,4 bl. | Mano |
| 7 | Kirin | Columna descendente | 12 bl. alto | Desde el cielo |
| 8 | Drilling Hand | Cono helicoidal | 2,5 bl. | Antebrazo |

**Las cuatro piezas de mano son la familia interna más delicada**, porque comparten sitio y escala. Se separan
así: nº 1 **escaso**, nº 5 **compacto**, nº 6 **afilado y recto**, nº 8 **largo y en espiral**.

---

## 6. Pendiente de decidir

1. **El color de Lightning Cutter.** Hoy es violeta y rompe su parentesco con el Chidori (§4). He escrito el
   prompt en cian-blanco con la alternativa violeta anotada. Decide antes de generar.
2. **`fire_cloak` y `lightning_cloak` no existen.** Ambos cierran su secuencia de learner en los datos, pero
   **el jutsu no está implementado** (auditoría §1.6). Serían auras envolventes, como Fireproof. Si se van a
   implementar, pídemelos y los añado.
3. **`drilling_hand_of_sage_lightning` tiene una textura de 64×128**, la única no cuadrada del catálogo, cuando
   todas las demás son 32×32. Con `item/generated` eso se renderiza raro. Conviene mirarlo al tocar esa pieza.
4. **Programación previa.** Los 8 de rayo y 2 de fuego (Fireproof, Fire Transport) **no tienen hoy dónde
   colgarse** (§1). Antes de que se vean en partida hacen falta:
   - un renderer propio para `ninja_projectile` que dibuje modelos en vez del `displayItem` (desbloquea los 5
     proyectiles de fuego de golpe, y todos los proyectiles del resto de elementos);
   - un visual de **ventana de carga** para `primed_strike` (desbloquea 4 de los 8 de rayo);
   - un visual de **estallido de área** para `area` (desbloquea 3 de los 8 de rayo, y buena parte de los KG).

   Ese orden es también el de mejor retorno: el renderer de proyectil es una pieza y sirve a decenas de jutsus.
