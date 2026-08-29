# Referencia de arte — Efectos de jutsu en el mundo

> **Propósito:** alimentar a una IA generadora de imágenes para obtener una **imagen de referencia** por jutsu,
> que después sirva de guía al modelar en **Blockbench**.
> **Qué se modela:** **el efecto que aparece al activar el jutsu** — la bola de fuego que vuela, el rayo que
> envuelve la mano, la descarga que cubre el área. **No** el icono del ítem.
> **Cobertura actual:** ✅ Fuego (7/7) · ✅ Rayo (8/8) · ✅ Viento (10/10) · ✅ Tierra (7/7) · ✅ Agua (7/7).
> **Las cinco naturalezas básicas están completas.** Pendientes: yin-yang, taijutsu, genjutsu, médico,
> fuinjutsu, kenjutsu, aburame, universales y los 16 Kekkei Genkai.
> **Fuente de los datos mecánicos:** los JSON de `data/dojutsu/jutsu/`, verificados uno a uno.
> **Fuente del criterio visual:** las 39 texturas 2D existentes, inspeccionadas una a una.
> **Última verificación contra el repo:** 2026-08-07.

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
| `structure` | **Bloques vanilla reales** colocados delante del lanzador (dirt, stone, ice, iron_block…). El efecto *es* el bloque | ➖ **No aplica.** Ver la nota de abajo |

**Consecuencia práctica para tu plan:**

- De los **7 jutsus de fuego**, **5 son proyectiles** → tienen entidad y el modelo entra casi directo.
- De los **8 jutsus de rayo**, **ninguno es proyectil** (4 `primed_strike`, 3 `area`, 1 `teleport`) → **los ocho
  necesitan trabajo de programación** antes de que un modelo se pueda ver en partida.
- De los **10 jutsus de viento**, **3 son proyectiles**; los otros 7 se reparten entre 2 `primed_strike`,
  3 `buff` y 1 `area` + 1 `teleport`. Viento es el elemento con **más efectos sobre el propio jugador** del mod
  (3 auras), y ninguna de las tres tiene hoy visual.

- De los **7 jutsus de tierra**, **2 son `structure`** — y esos dos son un caso aparte (ver abajo). Los otros 5
  se reparten en 2 proyectiles, 2 teleports y 1 área.
- De los **7 jutsus de agua**, sólo 2 son proyectiles. Es el elemento **menos ofensivo** del mod: cinco de sus
  siete técnicas no hacen daño.

No es un motivo para no diseñarlos: tener la referencia de arte primero es sensato. Pero conviene saber que
rayo no es "modelar y listo" como fuego, y que viento pide resolver el problema de las auras.

### Nota sobre `structure`: no hay nada que modelar (todavía)

Los jutsus de esta familia **colocan bloques vanilla de verdad** — `earth_wall` levanta un muro de 3×4 de
`minecraft:dirt` a dos bloques del lanzador, y ya está. No hay entidad, no hay modelo, no hay dónde colgar
nada: el efecto son cubos de tierra normales y corrientes.

Eso deja dos caminos, y **es una decisión de diseño, no de arte**:

1. **Dejarlos como bloques vanilla.** Es lo fiel al datapack y no cuesta nada. Pierdes identidad visual.
2. **Registrar bloques propios** (`dojutsu:earth_wall_block`…) con su textura, y que `structure` los coloque.
   Ahí sí hay trabajo de arte, pero es **textura de bloque**, no modelo de Blockbench.

Para las dos piezas de tierra afectadas he descrito abajo **qué textura de bloque tendría sentido** si eliges
la opción 2, en vez de un modelo que no tendría dónde ir. Si prefieres la opción 1, sáltate esos dos prompts.

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

## 5. Viento (Fūton) — 10 jutsus

El elemento **más largo del mod** y el más variado en familias: 3 proyectiles, 2 golpes cargados, 3 auras,
1 área y 1 teleport. También es el **único cuya secuencia de learner está completa** — fuego, agua, tierra y
rayo terminan en un `*_cloak` que no existe; aquí `wind_cloak` sí está implementado.

| # | Jutsu | Familia | Coste | Datos | Daño |
|---|---|---|---|---|---|
| 1 | Propelling Winds | buff | 30 | jump_boost IV + slow_falling, 6 s | — |
| 2 | Wind Torpedo | proyectil | 60 | vel. 1.6 | 7 |
| 3 | Wind Instantaneous Body | teleport | 80 | 4–10 bl. | — |
| 4 | Vacuum Wave | area | 70 | radio **10**, lanza 8 bl. hacia arriba | — (el daño es la caída) |
| 5 | Drilling Air Bullet | proyectil | 100 | vel. **1.8** (el más rápido del mod) | 9 |
| 6 | Wind Mode | buff | 100 | speed II + jump_boost II, 30 s | — |
| 7 | Rasengan | primed_strike | 100 | carga 3 s | 10 |
| 8 | Big Ball Rasengan | primed_strike | 200 | carga 3 s | 16 |
| 9 | Wind Cloak | buff | 150 | speed III + resistance I, 30 s | — |
| 10 | Rasenshuriken | proyectil | 300 | vel. 1.2, radio 6 | **18** (el mayor del mod) |

> **Las tres paletas de viento — esto sí conviene conservarlo.** A diferencia del violeta suelto de Lightning
> Cutter, el arte de viento está dividido en tres gamas y la división **tiene sentido**:
>
> | Gama | Jutsus | Lectura |
> |---|---|---|
> | **Blanco / gris pálido** | Propelling Winds, Wind Torpedo, Wind Instantaneous Body, Vacuum Wave, Drilling Air Bullet | Aire puro, corriente, presión. Casi incoloro |
> | **Azul cian** | Rasengan, Big Ball Rasengan, Rasenshuriken | La familia Rasengan: chakra concentrado, no naturaleza |
> | **Verde** | Wind Mode, Wind Cloak | La naturaleza viento aplicada al cuerpo |
>
> Es una distinción legible y fiel al material original (el Rasengan es manipulación de forma, azul; el Fūton
> como naturaleza suele representarse verde). **Recomiendo mantener las tres.** Los prompts de abajo las
> respetan.

---

### 1. Propelling Winds — *Fūton: Reppūshō* · buff

**Qué hace.** El jutsu más barato del elemento (30). Da salto potenciado IV y caída lenta durante 6 segundos:
es un impulso vertical, no un ataque.

**Qué se ve hoy.** Nada.

**Qué debe representar el modelo.** El único que va **bajo los pies**, y eso lo separa de las otras dos auras
de golpe. Un **cojín de aire** de **2 × 0.4 × 2 bloques**: un disco achatado y aplanado justo debajo del
jugador, formado por anillos concéntricos de borde dentado que giran. La textura actual es exactamente eso —
un aro serrado comprimido en vertical, visto en escorzo. Casi incoloro: se ve por la distorsión, no por el
color.

**Paleta.** Blanco `#FFFFFF`, gris pálido `#D8E2E8`, sombra gris azulada `#9EB0BC`.

> **Prompt:**
> Un cojín de aire comprimido justo bajo los pies: un disco muy achatado de unos 2 bloques de Minecraft de
> ancho y muy poca altura, formado por dos o tres anillos concéntricos de borde dentado, ligeramente
> desalineados entre sí como si girasen. Casi incoloro: blanco y gris pálido con sombras gris azulado, se lee
> por la forma y no por el color. Semitransparente. Estilo Minecraft voxel: caras planas, bordes duros, sin
> degradados suaves ni antialiasing. Paleta de 3 tonos. Efecto único centrado sobre fondo gris neutro liso,
> vista tres cuartos isométrica ligera, sin personajes ni escenario. Sin texto, sin marco.

---

### 2. Wind Torpedo — *Fūton: Shinkūgyoku* · proyectil

**Qué hace.** El proyectil básico del elemento: rápido (1.6), 7 de daño, sin aura ni área. Vuela y pega.

**Qué se ve hoy.** Una `phantom_membrane` vanilla volando.

**Qué debe representar el modelo.** Una **espiral de aire alargada en vuelo**. Cuerpo de
**0.4 × 0.4 × 1.1 bloques**: una hélice de arcos blancos apilados que se estrechan hacia la punta, como un
sacacorchos de viento visto de lado. La textura actual es justo eso — una pila de arcos curvos blancos
solapados. **Es hueco**: se ve entre las vueltas de la espiral, a diferencia de las masas macizas de fuego.

**Paleta.** Blanco `#FFFFFF`, gris claro `#DCE6EC`, sombra `#A6B8C4`.

> **Prompt:**
> Un torpedo de aire en vuelo con forma de espiral alargada: una hélice de arcos blancos apilados que giran
> alrededor de un eje horizontal y se estrechan progresivamente hasta una punta, de aproximadamente 1 bloque de
> Minecraft de largo y muy estrecho. Estructura hueca y calada, se ve entre las vueltas de la espiral. Blanco
> y gris pálido, casi incoloro, semitransparente. Estilo Minecraft voxel: caras planas, bordes duros, sin
> degradados suaves ni antialiasing. Paleta de 3 tonos. Objeto único centrado sobre fondo gris neutro liso,
> vista tres cuartos isométrica ligera, sin personajes ni escenario. Sin texto, sin marco.

---

### 3. Wind Instantaneous Body — *Fūton: Shunshin* · teleport

**Qué hace.** Reubica al lanzador entre 4 y 10 bloques. Sin daño.

**Qué se ve hoy.** Partículas `POOF`.

**Qué debe representar el modelo.** El **anillo de aire desplazado** que queda al desaparecer. Aro de
**2 × 2 bloques**, plano, con el **centro completamente vacío** y el borde erizado de púas cortas — la textura
actual es un aro dentado pálido con hueco central. Esa es su diferencia clave con los otros dos "estallidos de
salida" del documento: Fire Transport tiene núcleo macizo y Lightning Instantaneous Body tiene rayos que nacen
de un centro brillante; **aquí no hay centro en absoluto**, porque el aire se aparta, no estalla.

**Paleta.** Blanco `#FFFFFF`, gris pálido `#D0DDE4`, sombra `#98ACB8`.

> **Prompt:**
> Un anillo de aire desplazado, plano y de unos 2 bloques de Minecraft de diámetro, con el centro
> completamente hueco y vacío. El borde del aro está erizado de púas cortas e irregulares apuntando hacia
> fuera, como una onda de presión que aparta el aire. Blanco y gris pálido, casi incoloro, semitransparente,
> sin ningún núcleo brillante en el centro. Estilo Minecraft voxel: caras planas, bordes duros, sin degradados
> suaves ni antialiasing. Paleta de 3 tonos. Efecto único centrado sobre fondo gris neutro liso, vista tres
> cuartos isométrica ligera, sin personajes ni escenario. Sin texto, sin marco.

---

### 4. Vacuum Wave — *Fūton: Shinkūha* · area

**Qué hace.** El **área más amplia del mod** (radio 10). No hace daño directo: **lanza a todo lo que atrapa
8 bloques hacia arriba** y les da caída lenta. El daño real es la caída — un detalle heredado del datapack.

**Qué se ve hoy.** Nada.

**Qué debe representar el modelo.** Una **onda expansiva horizontal**, la pieza más ancha de todo el documento.
Anillos concéntricos planos de **20 bloques de diámetro** y muy poca altura, expandiéndose desde el centro —
la textura actual son exactamente círculos concéntricos. Debe leerse como **desplazamiento de aire a ras de
suelo**, no como explosión: sin púas, sin masa, solo ondas limpias que se abren. Conviene que los anillos
exteriores sean más finos y tenues que los interiores.

**Paleta.** Blanco `#FFFFFF`, gris pálido `#DAE4EA`, borde tenue `#B4C4CE`.

> **Prompt:**
> Una onda expansiva de aire a ras de suelo: tres o cuatro anillos concéntricos planos y muy finos
> expandiéndose desde un centro común, cubriendo un área muy amplia y baja. Los anillos exteriores son más
> delgados y tenues que los interiores, sugiriendo que se disipan al alejarse. Sin púas, sin masa, sin
> explosión: solo desplazamiento limpio de aire. Blanco y gris pálido, casi incoloro, semitransparente. Estilo
> Minecraft voxel: caras planas, bordes duros, sin degradados suaves ni antialiasing. Paleta de 3 tonos.
> Efecto único centrado sobre fondo gris neutro liso, vista tres cuartos isométrica ligera con la cámara
> ligeramente elevada, sin personajes ni escenario. Sin texto, sin marco.

---

### 5. Drilling Air Bullet — *Fūton: Renkūdan* · proyectil

**Qué hace.** El **proyectil más rápido de todo el mod** (velocidad 1.8), 9 de daño. Sin aura: es puntería pura.

**Qué se ve hoy.** Una `phantom_membrane` vanilla volando.

**Qué debe representar el modelo.** Un **taladro de aire comprimido**. Cuerpo de **0.5 × 0.5 × 1.3 bloques**
formado por una **retícula de líneas cruzadas** que envuelven un núcleo hueco, estrechándose hacia una punta
cónica. La textura actual es una malla romboidal de líneas blancas solapadas, y esa cualidad **de rejilla** es
lo que lo distingue del nº 2: el Wind Torpedo es una espiral suelta; este es una estructura tensa y apretada.

**Paleta.** Blanco `#FFFFFF`, gris claro `#DCE6EC`, sombra `#A0B4C0`.

> **Prompt:**
> Un proyectil de aire comprimido con forma de taladro alargado: un cuerpo cónico de aproximadamente 1,3
> bloques de Minecraft de largo, envuelto por una retícula tensa de líneas blancas finas que se cruzan
> formando rombos apretados alrededor de un núcleo hueco, convergiendo en una punta afilada. Estructura de
> rejilla, no de espiral suelta. Blanco y gris pálido, casi incoloro, semitransparente. Aspecto de velocidad
> extrema. Estilo Minecraft voxel: caras planas, bordes duros, sin degradados suaves ni antialiasing. Paleta
> de 3 tonos. Objeto único centrado sobre fondo gris neutro liso, vista tres cuartos isométrica ligera, sin
> personajes ni escenario. Sin texto, sin marco.

---

### 6. Wind Mode — *Fūton Mōdo* · buff

**Qué hace.** Aura de 30 segundos: velocidad II y salto II. La versión ligera del nº 9.

**Qué se ve hoy.** Nada.

**Qué debe representar el modelo.** La primera de las dos auras **verdes**, y la que debe leerse como
**contenida y geométrica**. La textura actual es un octógono verde con orbes dentro, y esa cualidad ordenada
es su identidad. En 3D: **placas planas y angulosas de viento verde orbitando cerca del cuerpo**, unas seis u
ocho, separadas entre sí y dejando ver al jugador — una envoltura de facetas, no una masa. Volumen total
aproximado **1.4 × 2.2 × 1.4 bloques**.

**Paleta.** Verde `#3FBF6A`, brillo verde claro `#8AE8A8`, sombra verde oscuro `#1E7A42`.

> **Prompt:**
> Una envoltura de viento verde formada por seis u ocho placas planas y angulosas suspendidas alrededor de un
> espacio vertical del tamaño de un personaje, claramente separadas entre sí y dejando grandes huecos por los
> que se ve a través. Cada placa tiene borde definido y un brillo verde claro en el canto. Aspecto ordenado y
> geométrico, contenido, nada caótico. Verde saturado con sombras verde oscuro. Semitransparente. Estilo
> Minecraft voxel: caras planas, bordes duros, sin degradados suaves ni antialiasing. Paleta de 3 tonos.
> Efecto solo, centrado sobre fondo gris neutro liso, vista tres cuartos isométrica ligera, sin personaje
> dentro. Sin texto, sin marco.

---

### 7. Rasengan — *Rasengan* · primed_strike

**Qué hace.** El golpe cargado emblemático. Carga 3 segundos y descarga 10 de daño al siguiente golpe cuerpo a
cuerpo. Afinidad de viento 150.

**Qué se ve hoy.** Nada durante la carga.

**Qué debe representar el modelo.** La **esfera girando en la palma**. Bola de **0.8 × 0.8 × 0.8 bloques**
sostenida sobre la mano abierta —**no envolviéndola** como el Chidori, esa es la diferencia clave entre las dos
técnicas de mano del mod—. Superficie con **estriaciones en espiral** que giran alrededor de un eje visible y
un núcleo blanco brillante; borde erizado de púas cortas. La textura actual es una esfera cian de borde
dentado y así debe quedarse.

**Paleta.** Azul cian `#3FA8E8`, núcleo blanco `#FFFFFF`, azul profundo `#1B6FA8`.

> **Prompt:**
> Una esfera de chakra azul girando, del tamaño de un puño (unos 0,8 bloques de Minecraft), sostenida sobre una
> palma abierta y sin tocarla ni envolverla. Superficie recorrida por estriaciones en espiral que giran
> alrededor de un eje visible, núcleo blanco brillante en el centro y borde erizado de púas muy cortas y
> regulares. Azul cian con sombras azul profundo. Sensación de rotación rápida y contenida. Puede incluirse
> una mano cúbica estilizada de Minecraft abierta, en gris plano, solo como referencia de escala. Efecto
> emisivo. Estilo Minecraft voxel: caras planas, bordes duros, sin degradados suaves ni antialiasing. Paleta de
> 4 tonos. Centrado sobre fondo gris neutro liso, vista tres cuartos isométrica ligera. Sin texto, sin marco.

---

### 8. Big Ball Rasengan — *Ōdama Rasengan* · primed_strike

**Qué hace.** El Rasengan agrandado: mismo tiempo de carga (3 s), el doble de coste (200) y 16 de daño.

**Qué se ve hoy.** Nada durante la carga.

**Qué debe representar el modelo.** **Exactamente el nº 7 pero al doble de tamaño**, y ahí está la gracia: la
diferencia debe ser de **escala pura**, no de forma. Bola de **1.6 × 1.6 × 1.6 bloques**, tan grande que ya no
se sostiene sobre la palma sino que la envuelve. Su textura es la de mayor resolución del catálogo (128×128) y
muestra las estriaciones en espiral con mucho más detalle: conviene aprovecharlas y hacerlas más marcadas que
en el Rasengan pequeño.

**Paleta.** La misma del nº 7 — azul cian `#3FA8E8`, núcleo blanco `#FFFFFF`, azul profundo `#1B6FA8`.

> **Prompt:**
> Una esfera de chakra azul girando, muy grande, de aproximadamente 1,6 bloques de Minecraft de diámetro,
> sostenida desde abajo por un brazo que apenas la abarca. Superficie recorrida por estriaciones en espiral
> muy marcadas y densas girando alrededor de un eje visible, con un núcleo blanco cegador en el centro que se
> difumina hacia el azul cian del exterior. Borde ligeramente irregular por la velocidad de giro. Sensación de
> masa y poder contenido. Puede incluirse un brazo cúbico estilizado de Minecraft, en gris plano, solo como
> referencia de escala. Efecto emisivo. Estilo Minecraft voxel: caras planas, bordes duros, sin degradados
> suaves ni antialiasing. Paleta de 4 tonos. Centrado sobre fondo gris neutro liso, vista tres cuartos
> isométrica ligera. Sin texto, sin marco.

---

### 9. Wind Cloak — *Fūton no Yoroi* · buff

**Qué hace.** El aura mayor: velocidad III y resistencia I durante 30 segundos, por 150 de chakra. Afinidad 250.
Es el único de los tres buffs que **protege**, no solo acelera.

**Qué se ve hoy.** Nada.

**Qué debe representar el modelo.** La segunda aura **verde**, y la opuesta al nº 6: donde Wind Mode es
geométrico y separado, **Wind Cloak es una envoltura continua**. Cáscara con forma de **gota o llama vertical**
de **1.3 × 2.4 × 1.3 bloques** que rodea al jugador entero y se afina en punta por encima de la cabeza — la
textura actual es exactamente esa silueta de llama verde. Superficie estriada con líneas verticales que suben,
sugiriendo corriente ascendente. Debe ser **translúcida**, no puede tapar al jugador.

**Paleta.** Verde `#35B562`, brillo `#7FE49B`, verde oscuro `#166B38`.

> **Prompt:**
> Un manto de viento verde envolviendo por completo un espacio vertical del tamaño de un personaje, con forma
> de gota o llama que rodea el cuerpo y se afina en una punta por encima de la cabeza. Superficie continua y
> translúcida, recorrida por estriaciones verticales que ascienden sugiriendo corriente de aire. A diferencia
> de una envoltura de placas separadas, esta es una cáscara entera y cerrada, pero lo bastante transparente
> para ver a través. Verde saturado con brillo verde claro en los bordes y sombras verde oscuro. Estilo
> Minecraft voxel: caras planas, bordes duros, sin degradados suaves ni antialiasing. Paleta de 3 tonos.
> Efecto solo, centrado sobre fondo gris neutro liso, vista tres cuartos isométrica ligera, sin personaje
> dentro. Sin texto, sin marco.

---

### 10. Rasenshuriken — *Fūton: Rasenshuriken* · proyectil

**Qué hace.** El **jutsu más letal del mod entero**: 18 de daño, radio 6, coste 300 y enfriamiento de 60
segundos. Exige afinidad de viento 500, el techo. Cierra la rama.

**Qué se ve hoy.** Una `nether_star` vanilla volando — el único jutsu de viento que no usa `phantom_membrane`,
señal de que ya se le quiso dar un aspecto distinto.

**Qué debe representar el modelo.** La única pieza **con forma de arma reconocible** de los tres elementos
documentados. Shuriken de **2 × 2 × 0.5 bloques**: **cuatro aspas curvas** que nacen de un núcleo esférico
brillante y se abren en molinete, todas con el mismo sentido de giro. La textura actual es exactamente ese
molinete cian de cuatro palas. En vuelo debe girar sobre su propio eje. El aro exterior de viento (radio 6) se
puede sugerir con un **disco tenue y translúcido** que une las puntas de las aspas.

**Paleta.** Cian `#4FC4F0`, núcleo blanco `#FFFFFF`, azul profundo `#1D6E9E`, disco tenue `#B8E8FA`.

> **Prompt:**
> Un shuriken gigante de viento y chakra, de unos 2 bloques de Minecraft de envergadura y muy plano: cuatro
> aspas curvas idénticas que nacen de un núcleo esférico blanco cegador y se abren en molinete, todas
> inclinadas en el mismo sentido de giro, con los bordes afilados. Un disco muy tenue y translúcido de aire
> conecta las puntas de las cuatro aspas, sugiriendo el anillo de viento que genera al girar. Cian brillante
> con sombras azul profundo. Sensación de rotación violenta y de arma peligrosa. Efecto emisivo. Estilo
> Minecraft voxel: caras planas, bordes duros, sin degradados suaves ni antialiasing. Paleta de 4 tonos.
> Objeto único centrado sobre fondo gris neutro liso, vista tres cuartos isométrica ligera, sin personajes ni
> escenario. Sin texto, sin marco.

---

## 6. Tierra (Doton) — 7 jutsus

El elemento **más pesado y más terrestre**: es el único cuyos proyectiles tienen **gravedad** (arquean y caen),
el único con dos técnicas `structure` y el que más se apoya en el suelo. La paleta del arte existente es
uniforme —tierra y roca, sin excepciones— lo que lo hace el más coherente de los cuatro documentados.

| # | Jutsu | Familia | Coste | Datos | Daño |
|---|---|---|---|---|---|
| 1 | Earth Prison | area | 30 | radio 5, slowness VI 5 s | — |
| 2 | Rock Gun | proyectil | 60 | vel. 1.5, **gravedad 0.03** | 7 |
| 3 | Earth Wall | structure | 60 | dirt 3×4 a 2 bl., **cd 0** | — |
| 4 | Earth Instantaneous Body | teleport | 80 | 4–10 bl. · **suena** `jutsu.earth` | — |
| 5 | Earth Golem | structure | 100 | stone 3×3 a 2 bl. | — |
| 6 | Earth Dragon Blast | proyectil | 120 | vel. 1.0, **gravedad 0.02**, radio 4 | 11 |
| 7 | Tunnelling | teleport | 160 | **8–16 bl.** (el mayor alcance del mod) | — |

> **La gravedad es la seña de identidad del elemento.** Los proyectiles de fuego, rayo y viento vuelan rectos;
> los dos de tierra **caen mientras avanzan**. El arte debe acompañarlo: nada de formas etéreas ni de estelas
> ligeras — estas piezas deben **pesar**. Opacas, mates, sin emisión de luz. Son las únicas del documento que
> **no brillan**.

**Paleta común del elemento** (el arte existente ya es consistente, conviene respetarlo):
tierra media `#8B6534`, tierra oscura `#5C3F1E`, roca gris-marrón `#A08256`, sombra profunda `#33220F`,
luz de canto `#C4A574`.

---

### 1. Earth Prison — *Doton: Doryū Taiga* · area

**Qué hace.** El jutsu más barato del elemento (30) con el enfriamiento más largo (30 s). Radio 5, sin daño:
aplica **slowness VI durante 5 segundos** — inmoviliza, no mata. Es una trampa.

**Qué se ve hoy.** Nada.

**Qué debe representar el modelo.** La **tierra cerrándose alrededor de los atrapados**. La textura actual es
un montículo redondeado con sombreado concéntrico, así que la lectura es "algo que emerge del suelo y envuelve".
En 3D: un **anillo de losas de roca inclinadas** que brotan del suelo y se cierran hacia dentro, de
**10 bloques de diámetro** y unos 2 de alto, dejando huecos entre placa y placa. Debe leerse como **cepo**, no
como pinchos: bordes romos, nada afilado — la técnica no hace daño.

> **Prompt:**
> Una trampa de tierra emergiendo del suelo: un anillo amplio de losas de roca gruesas e inclinadas hacia
> dentro, brotando del terreno y cerrándose sobre el centro, con huecos irregulares entre unas y otras. Bordes
> romos y desgastados, nada afilado ni puntiagudo. Unos 10 bloques de Minecraft de diámetro y baja altura.
> Marrón tierra y gris roca, superficie mate y opaca, sin brillo propio ni emisión de luz. Estilo Minecraft
> voxel: caras planas, bordes duros, sin degradados suaves ni antialiasing. Paleta de 4 tonos terrosos. Efecto
> único centrado sobre fondo gris neutro liso, vista tres cuartos isométrica ligera con la cámara ligeramente
> elevada, sin personajes ni escenario. Sin texto, sin marco.

---

### 2. Rock Gun — *Doton: Iwa Teppō* · proyectil

**Qué hace.** El proyectil básico: velocidad 1.5 y **gravedad 0.03** (la mayor del mod, cae rápido), 7 de daño.
Sin aura ni radio: impacto directo.

**Qué se ve hoy.** Un `cobblestone` vanilla volando — que, siendo un cubo de piedra, no queda mal del todo.

**Qué debe representar el modelo.** Una **piedra lanzada**, sin más pretensión. Canto rodado irregular de
**0.5 × 0.35 × 0.55 bloques**, achatado, con facetas grandes y desiguales y un par de aristas marcadas. La
textura actual es exactamente eso: un guijarro achatado marrón. **No debe tener efecto de chakra visible** —
es el único jutsu del documento que puede pasar por un objeto natural, y esa humildad es su carácter.

> **Prompt:**
> Una piedra irregular lanzada por el aire, achatada y más ancha que alta, de aproximadamente medio bloque de
> Minecraft. Superficie de facetas grandes y desiguales con dos o tres aristas marcadas y esquinas
> desgastadas, como un canto rodado. Marrón tierra con zonas de gris roca y sombras profundas en las
> hendiduras. Completamente mate y opaca, sin brillo, sin aura, sin energía visible: parece una piedra
> corriente. Estilo Minecraft voxel: caras planas, bordes duros, sin degradados suaves ni antialiasing. Paleta
> de 4 tonos terrosos. Objeto único centrado sobre fondo gris neutro liso, vista tres cuartos isométrica
> ligera, sin personajes ni escenario. Sin texto, sin marco.

---

### 3. Earth Wall — *Doton: Doryūheki* · structure ⚠️

**Qué hace.** Levanta un muro de **`minecraft:dirt` de 3 de ancho por 4 de alto**, dos bloques por delante,
orientado a la mirada. **Enfriamiento 0**: se puede encadenar sin parar, la única técnica del mod sin espera.

**Qué se ve hoy.** Bloques de tierra vanilla. Es literalmente lo que hace.

**Qué debe representar.** ⚠️ **Aquí no hay modelo que hacer** (§1). Si eliges registrar un bloque propio, lo
que hace falta es una **textura de bloque de 16×16 que tesele** — no una pieza de Blockbench. La textura de
jutsu actual (una losa de roca rugosa vista de frente) ya da el tono: superficie de tierra prensada, más dura
y más gris que el `dirt` vanilla, para que se note que es tierra **levantada por chakra** y no del terreno.

> **Prompt (solo si registras bloque propio):**
> Una textura de bloque de Minecraft para tierra compactada por chakra, cuadrada y que tesele sin costuras
> visibles al repetirse. Superficie de tierra prensada con guijarros incrustados, más dura, más gris y más
> compacta que la tierra normal de Minecraft, con grietas finas y algunas vetas de roca. Marrón tierra medio
> con sombras profundas y motas de gris roca. Mate y opaca. Estilo Minecraft: píxeles definidos, sin degradados
> suaves ni antialiasing, paleta de 4 tonos. Textura plana vista completamente de frente, ocupando todo el
> encuadre. Sin objeto en perspectiva, sin texto, sin marco.

---

### 4. Earth Instantaneous Body — *Doton: Shunshin* · teleport

**Qué hace.** Reubica al lanzador entre 4 y 10 bloques. **Es el único de los cuatro shunshin que ya tiene
sonido**: `dojutsu:jutsu.earth`, a tono 2.0 y volumen 0.8.

**Qué se ve hoy.** Partículas `POOF` y el sonido.

**Qué debe representar el modelo.** El **reventón de tierra** en el punto de salida. Esfera erizada de
**2 × 2 × 2 bloques** con púas cortas, gruesas y **desiguales** apuntando en todas direcciones — la textura
actual es un disco marrón erizado de púas finas, y conviene engrosarlas al pasar a 3D para que no se confunda
con el aro de viento. Terrones y guijarros sueltos, no una forma limpia.

Es el tercer "destello de salida" del documento y debe separarse de los otros dos: **fuego arde, viento se
aparta, tierra revienta**.

> **Prompt:**
> Un reventón de tierra y grava saliendo del suelo: una masa esférica de unos 2 bloques de Minecraft erizada de
> púas cortas, gruesas y desiguales apuntando en todas direcciones, con terrones sueltos y guijarros
> desprendiéndose por los bordes. Aspecto sucio e irregular, nada limpio ni simétrico. Marrón tierra con gris
> roca y sombras profundas. Completamente mate y opaca, sin brillo ni emisión de luz. Estilo Minecraft voxel:
> caras planas, bordes duros, sin degradados suaves ni antialiasing. Paleta de 4 tonos terrosos. Efecto único
> centrado sobre fondo gris neutro liso, vista tres cuartos isométrica ligera, sin personajes ni escenario. Sin
> texto, sin marco.

---

### 5. Earth Golem — *Doton: Golemu no Jutsu* · structure ⚠️

**Qué hace.** Coloca un bloque de **`minecraft:stone` de 3×3**, dos bloques por delante. Coste 100,
enfriamiento 30 s.

**Qué se ve hoy.** Un cubo de piedra de 3×3. Nada más.

> ⚠️ **Aquí hay un desajuste real entre el arte y el código.** La textura del jutsu es **una figura humanoide
> de tierra** —cabeza, brazos, piernas, claramente un gólem—, pero la implementación levanta un **muro de
> piedra**. La técnica promete una criatura y entrega un cubo.
>
> Son tres caminos distintos, y **conviene decidirlo antes de encargar arte**:
>
> | Opción | Qué implica | Coste |
> |---|---|---|
> | **A. Dejarlo como está** | Sigue siendo un muro; la textura del ítem miente | Cero |
> | **B. Bloque propio** | Textura de bloque de piedra tallada; sigue siendo un muro | Bajo (1 textura) |
> | **C. Entidad gólem** | Una criatura invocada de verdad, con modelo, IA y caducidad | **Alto** — entidad nueva, como las bestias |
>
> La opción C es la que honra la textura y el nombre, y el mod **ya tiene la primitiva** para hacerlo sin
> inventar nada: `AbilityAnchor` para la caducidad y el patrón de `SummonService`, que invoca criaturas con
> vida limitada. Pero es un ticket de programación, no de arte.

**Descripción para la opción C** (la única que necesita un modelo de Blockbench):

Un **gólem de tierra tosco** de **1.4 × 2.6 × 0.9 bloques** — más alto que un jugador, ancho de hombros,
brazos largos y desproporcionados que casi llegan al suelo, piernas cortas y macizas. Sin cuello, cabeza
hundida entre los hombros. Construido con **cuatro o cinco cubos grandes por miembro**, nada de detalle fino:
debe verse **montado a piedra bruta**, como si alguien hubiera apilado rocas con forma de persona. Grietas
oscuras en las junturas.

> **Prompt (opción C):**
> Un gólem de tierra y roca tosco y macizo, más alto que un personaje de Minecraft, con hombros muy anchos,
> brazos largos y desproporcionados que casi rozan el suelo, piernas cortas y gruesas y la cabeza hundida entre
> los hombros sin cuello visible. Construido con pocos bloques grandes por miembro, sin detalle fino, como
> rocas apiladas con forma humanoide. Grietas oscuras en las junturas entre bloques. Marrón tierra y gris roca,
> superficie mate y opaca, sin brillo ni ojos luminosos. Estilo Minecraft voxel: caras planas, bordes duros,
> sin degradados suaves ni antialiasing. Paleta de 4 tonos terrosos. Criatura sola centrada sobre fondo gris
> neutro liso, de cuerpo entero, vista tres cuartos isométrica ligera, sin escenario. Sin texto, sin marco.

---

### 6. Earth Dragon Blast — *Doton: Doryūdan* · proyectil

**Qué hace.** El más fuerte del elemento: 11 de daño, radio 4, **gravedad 0.02** (arquea) y enfriamiento de
30 s. Afinidad 150.

**Qué se ve hoy.** Un `cobblestone` vanilla volando.

**Qué debe representar el modelo.** Igual que en fuego, el nombre pide **cabeza de dragón** — y conviene que la
haya, porque es el par directo de `exploding_dragon_strike` y así los dos elementos riman. Cuerpo de
**1.5 × 1.3 × 2.2 bloques**, direccional, con el hocico resuelto en **planos de roca angulosos** y una cavidad
oscura entre las mandíbulas.

La textura actual es una bola erizada de púas, no una cabeza; se puede conciliar haciendo que **las púas sean
la melena de esquirlas** que rodea el cráneo, con terrones desprendiéndose por detrás. Si prefieres respetar la
textura literalmente, el prompt alternativo está debajo.

> **Prompt (cabeza de dragón, recomendado):**
> Una cabeza de dragón hecha de roca surcando el aire en trayectoria descendente, de unos 2 bloques de
> Minecraft de largo: cráneo construido con planos de piedra angulosos y grandes, fauces abiertas formadas por
> dos mandíbulas rectas con una cavidad oscura y profunda entre ellas, y una melena de esquirlas de roca
> puntiagudas rodeando la parte trasera del cráneo. Terrones y guijarros desprendiéndose detrás. Sin escamas ni
> ojos. Marrón tierra y gris roca, mate y opaca, sin brillo. Aspecto pesado, se nota que cae. Estilo Minecraft
> voxel: caras planas, bordes duros, sin degradados suaves ni antialiasing. Paleta de 5 tonos terrosos. Objeto
> único centrado sobre fondo gris neutro liso, vista tres cuartos isométrica ligera, sin personajes ni
> escenario. Sin texto, sin marco.

> **Prompt (alternativo, fiel a la textura):**
> Un proyectil de roca en vuelo con forma de esfera erizada de casi 1,5 bloques de Minecraft: núcleo de piedra
> compacto cubierto de esquirlas puntiagudas y desiguales apuntando hacia fuera, con terrones desprendiéndose
> por detrás. Marrón tierra y gris roca, mate y opaca. Aspecto pesado y contundente. Estilo Minecraft voxel:
> caras planas, bordes duros, sin degradados suaves ni antialiasing. Paleta de 4 tonos terrosos. Objeto único
> centrado sobre fondo gris neutro liso, vista tres cuartos isométrica ligera, sin personajes ni escenario. Sin
> texto, sin marco.

---

### 7. Tunnelling — *Doton: Moguragakure* · teleport

**Qué hace.** El **teleport de mayor alcance de todo el mod** (8–16 bloques). Coste 160, afinidad 180: la
técnica más cara del elemento.

**Qué se ve hoy.** Partículas `POOF`.

**Qué debe representar el modelo.** El **agujero** que queda en el suelo. La textura actual es un disco marrón
oscuro casi liso, de borde ligeramente festoneado: literalmente una boca de túnel vista desde arriba. En 3D:
un **hueco circular de 1.5 bloques de diámetro** con el reborde de tierra levantada alrededor —un anillo bajo
de terrones— y el interior **oscureciéndose hasta el negro**, sugiriendo profundidad.

Es la pieza más sutil del documento y la única que representa una **ausencia** en vez de una presencia. Ese
contraste conviene conservarlo: no debe tener partículas ni energía, solo el agujero y la tierra removida.

> **Prompt:**
> Una boca de túnel excavada en el suelo: un hueco circular de aproximadamente 1,5 bloques de Minecraft de
> diámetro, rodeado por un reborde bajo de terrones de tierra levantada y desordenada. El interior se oscurece
> progresivamente por bandas hasta el negro absoluto en el centro, sugiriendo profundidad. Sin partículas, sin
> energía, sin brillo: solo tierra removida y oscuridad. Marrón tierra en el reborde, marrones cada vez más
> oscuros hacia dentro. Mate y opaco. Estilo Minecraft voxel: caras planas, bordes duros, sin degradados suaves
> ni antialiasing. Paleta de 5 tonos terrosos. Efecto único centrado sobre fondo gris neutro liso, vista tres
> cuartos isométrica ligera con la cámara elevada mirando hacia el agujero, sin personajes ni escenario. Sin
> texto, sin marco.

---

## 7. Agua (Suiton) — 7 jutsus

El elemento **más translúcido y el más brillante**: donde viento es incoloro y mate, agua es **transparente con
reflejo**. Es también el único que puede presentarse como **volumen de fluido** en vez de como forma sólida.
Reparto: 2 áreas, 2 proyectiles, 1 aura, 1 teleport y 1 `structure`.

| # | Jutsu | Familia | Coste | Datos | Daño |
|---|---|---|---|---|---|
| 1 | Water Prison | area | 30 | radio 5, slowness V 5 s | — |
| 2 | Water Puddle | structure | 30 | `minecraft:water` 3×1 a 1 bl. | — |
| 3 | Breathing Underwater | buff | 20 | water_breathing 60 s · **cd 20 t** | — |
| 4 | Water Instantaneous Body | teleport | 80 | 4–10 bl. · **suena** (splash vanilla) | — |
| 5 | Hidden Mist | area | 60 | radio 8, blindness 6 s | — |
| 6 | Rising Water Slicer | proyectil | 100 | vel. 1.2 | 8 |
| 7 | Water Severing Wave | proyectil | 140 | vel. 1.0, radio 4 | 12 |

> **Agua es el elemento menos letal del mod.** Cinco de sus siete técnicas **no hacen daño**: aprisionan,
> ciegan, dan aire o crean terreno. Es control, no ofensiva. El arte debe acompañarlo — nada de bordes
> agresivos salvo en las dos técnicas de corte (nº 6 y nº 7), que son justo las únicas que hieren.

**Paleta común del elemento:** azul cian `#2E9BD8`, azul claro `#7FD4F5`, blanco de espuma `#EAF8FF`, azul
profundo `#14567E`. **Excepción deliberada:** Hidden Mist es blanco y gris, no azul (nº 5).

---

### 1. Water Prison — *Suiton: Suirō no Jutsu* · area

**Qué hace.** Radio 5, sin daño: **slowness V durante 5 segundos**. Junto a Earth Prison forma la pareja de
inmovilización del mod, y cuesta lo mismo (30) con algo menos de espera (25 s frente a 30).

**Qué se ve hoy.** Nada.

**Qué debe representar el modelo.** La **esfera de agua que encierra al objetivo**, el clásico de la técnica.
Burbuja de **2.4 × 2.4 × 2.4 bloques** —lo justo para contener a un jugador— de superficie tensa y **muy
translúcida**, con el borde erizado de ondulaciones cortas y romas (la textura actual es una esfera azul de
canto serrado). Debe verse **el interior a través**: es una prisión, y se tiene que notar que hay algo dentro.

> **Prompt:**
> Una esfera de agua tensa y muy translúcida, de unos 2,4 bloques de Minecraft de diámetro, lo bastante grande
> para encerrar a una persona dentro. Superficie recorrida por ondas concéntricas suaves y borde festoneado con
> ondulaciones cortas y romas, nada afilado. Muy transparente: se ve claramente a través de ella hasta el otro
> lado. Azul cian con reflejos blancos de espuma arriba y azul profundo en la base. Acabado brillante y húmedo,
> con un reflejo especular marcado. Estilo Minecraft voxel: caras planas, bordes duros, sin degradados suaves
> ni antialiasing. Paleta de 4 tonos azules. Efecto único centrado sobre fondo gris neutro liso, vista tres
> cuartos isométrica ligera, sin personajes ni escenario. Sin texto, sin marco.

---

### 2. Water Puddle — *Suiton: Mizu Tamari* · structure

**Qué hace.** Coloca **3 bloques de `minecraft:water`** justo delante, a ras de suelo. Técnica de utilidad:
crear agua donde no la hay.

**Qué se ve hoy.** Agua vanilla. Y a diferencia de `earth_wall`, **aquí eso está bien**: el agua de Minecraft
ya es agua, con su animación y su transparencia.

**Qué debe representar.** ⚠️ De las cuatro piezas `structure` documentadas, **esta es la que mejor argumento
tiene para quedarse como está**. Un bloque propio de "agua de chakra" sería casi indistinguible del agua normal
y encima perdería el comportamiento de fluido vanilla (fluir, empapar, apagar fuego), que es justo para lo que
sirve la técnica.

**Recomendación: no encargar arte para esta.** Si aun así quieres marcarla, el camino barato no es un bloque
nuevo sino un **destello de aparición**: una salpicadura de metro y medio que dure medio segundo al colocarse
el agua, hecha con partículas, no con modelo.

---

### 3. Breathing Underwater — *Suiton: Suichūkokyū* · buff

**Qué hace.** Respiración acuática 60 segundos por sólo 20 de chakra. **Enfriamiento de 20 ticks (1 s)**, el
más corto del mod después de Earth Wall: está pensado para reactivarse sin fricción.

**Qué se ve hoy.** Nada.

**Qué debe representar el modelo.** El **casco de burbujas alrededor de la cabeza**, no un aura de cuerpo
entero — y eso lo separa limpiamente de las tres auras de viento y de Fireproof. La textura actual es un cúmulo
de burbujas azul pálido, así que la lectura ya está dada. En 3D: esfera irregular de **1 × 1 × 1 bloque**
formada por **burbujas de tamaños distintos** apiñadas, muy translúcida, con las grandes abajo y las pequeñas
desprendiéndose por arriba.

> **Prompt:**
> Un casco de burbujas de agua flotando en el aire, del tamaño justo para rodear una cabeza, aproximadamente un
> bloque de Minecraft. Formado por burbujas esféricas de tamaños muy distintos apiñadas entre sí, las grandes
> agrupadas abajo y las pequeñas desprendiéndose y ascendiendo por arriba. Extremadamente translúcido, casi
> todo transparente, con finos bordes blancos definiendo cada burbuja. Azul muy pálido con reflejos blancos.
> Hueco por dentro. Estilo Minecraft voxel: caras planas, bordes duros, sin degradados suaves ni antialiasing.
> Paleta de 3 tonos. Efecto solo, centrado sobre fondo gris neutro liso, vista tres cuartos isométrica ligera,
> sin personaje dentro. Sin texto, sin marco.

---

### 4. Water Instantaneous Body — *Suiton: Shunshin* · teleport

**Qué hace.** Reubica al lanzador entre 4 y 10 bloques. **Ya tiene sonido**: el splash de vanilla
(`entity.player.splash.high_speed`) a tono 2.0.

**Qué se ve hoy.** Partículas `POOF` y el chapoteo.

**Qué debe representar el modelo.** La **salpicadura** que queda al desaparecer — el cuarto y último shunshin
del documento. Corona de **2 × 2 bloques** con gotas radiando desde el centro. La textura actual es un disco
azul de púas finas y regulares, **muy parecido al de rayo**, así que en 3D conviene diferenciarlo **engordando
las puntas en gotas redondeadas**, con la cabeza más ancha que la base.

Con este se cierra la serie: **fuego arde, rayo destella, viento se aparta, tierra revienta, agua salpica.**

> **Prompt:**
> Una corona de salpicadura de agua vista desde arriba, de unos 2 bloques de Minecraft de diámetro: un anillo
> de gotas alargadas irradiando desde un centro hundido, cada gota con la punta redondeada y más ancha que su
> base, como en el instante de romperse una superficie de agua. Algunas gotas ya desprendidas flotando por
> encima del anillo. Azul cian translúcido con espuma blanca en las crestas. Acabado brillante y húmedo. Estilo
> Minecraft voxel: caras planas, bordes duros, sin degradados suaves ni antialiasing. Paleta de 4 tonos azules.
> Efecto único centrado sobre fondo gris neutro liso, vista tres cuartos isométrica ligera con la cámara
> ligeramente elevada, sin personajes ni escenario. Sin texto, sin marco.

---

### 5. Hidden Mist — *Kirigakure no Jutsu* · area

**Qué hace.** Radio 8, sin daño: **ceguera 6 segundos**. La técnica insignia de la Niebla, pensada para
desaparecer, no para matar.

**Qué se ve hoy.** Nada.

**Qué debe representar el modelo.** ⚠️ **Es la excepción cromática del elemento y hay que respetarla:** su
textura es **blanca y gris**, no azul, y además mide **64×64**, el doble que el resto del set. Recibió un trato
aparte a propósito.

Un **banco de niebla**: masa de **16 bloques de diámetro** y unos 3 de alto, formada por **cúmulos redondeados
y blandos** apilados sin orden, de bordes mullidos, sin una sola arista. Debe ser **lo más opaco del
elemento**, justo al revés que las otras seis piezas: la niebla no deja ver, y eso es literalmente su efecto.

Es además la única pieza de los cinco elementos documentados que **debe leerse como suave**.

> **Prompt:**
> Un banco de niebla denso y bajo, muy ancho, formado por cúmulos redondeados y mullidos apilados sin orden
> aparente, con los bordes blandos y esponjosos, sin ninguna arista ni punta. Blanco y gris claro, sin nada de
> azul. Opaco en el interior y algo más deshilachado en los bordes exteriores. Aspecto algodonoso, lo contrario
> de una forma cristalina o afilada. Estilo Minecraft voxel: caras planas, bordes duros, pero con la silueta
> general redondeada. Sin degradados suaves ni antialiasing. Paleta de 3 tonos: blanco, gris claro, gris medio.
> Efecto único centrado sobre fondo gris neutro **oscuro** para que contraste, vista tres cuartos isométrica
> ligera, sin personajes ni escenario. Sin texto, sin marco.

---

### 6. Rising Water Slicer — *Suiton: Suiryūdan* · proyectil

**Qué hace.** Primera de las dos técnicas de agua que hieren: 8 de daño, velocidad 1.2, sin radio de área.
Impacto directo.

**Qué se ve hoy.** Un `prismarine_shard` vanilla volando.

**Qué debe representar el modelo.** Una **hoja de agua a presión**, y aquí sí caben los bordes afilados. La
textura actual son **espigas cristalinas cian apuntando hacia arriba**. En 3D: tres o cuatro **láminas
verticales** de agua comprimida de **0.6 × 1.4 × 0.4 bloques**, de alturas distintas, con el canto superior
afilado y la base ancha, viajando de perfil para que el filo vaya por delante.

⚠️ **Cuidado con el hielo.** Esta pieza y las del Kekkei Genkai de hielo pueden confundirse: la textura ya se
parece bastante. Se separan porque aquí el material debe verse **líquido** —bordes con ondulación, superficie
que fluye, algún goteo—. El hielo es opaco, rígido y facetado; esto no.

> **Prompt:**
> Una hoja de agua a presión viajando por el aire: tres o cuatro láminas verticales de agua comprimida de
> alturas distintas, la mayor de aproximadamente 1,4 bloques de Minecraft, con el canto superior afilado como
> una cuchilla y la base ancha, dispuestas en fila y avanzando de perfil con el filo por delante. El material
> se ve claramente líquido: bordes ligeramente ondulados, superficie que fluye y alguna gota desprendiéndose.
> Azul cian translúcido con espuma blanca en los filos. Brillante y húmedo, nunca rígido ni cristalino como el
> hielo. Estilo Minecraft voxel: caras planas, bordes duros, sin degradados suaves ni antialiasing. Paleta de 4
> tonos azules. Objeto único centrado sobre fondo gris neutro liso, vista tres cuartos isométrica ligera, sin
> personajes ni escenario. Sin texto, sin marco.

---

### 7. Water Severing Wave — *Suiton: Suidanha* · proyectil

**Qué hace.** El más fuerte del elemento: **12 de daño**, radio 4, coste 140. Cierra la rama junto al
`water_cloak` que no existe.

**Qué se ve hoy.** Un `prismarine_shard` vanilla volando.

**Qué debe representar el modelo.** La textura actual es una **esfera azul lisa y brillante** con un reflejo
blanco marcado: el objeto más limpio de todo el elemento. Pero el nombre dice "ola cortante" y el radio 4 pide
volumen, así que propongo conciliarlo con una **masa de agua en forma de ola que rompe**, de
**2.2 × 1.6 × 1.4 bloques**, con la cresta enrollándose hacia delante y el frente afilado.

El reflejo especular liso de la textura se conserva en el **cuerpo** de la ola, que debe ser la superficie más
pulida y brillante de los cinco elementos documentados. Si prefieres respetar la textura al pie de la letra, el
prompt alternativo está debajo.

> **Prompt (ola que rompe, recomendado):**
> Una ola de agua rompiendo, congelada a media rotura y avanzando por el aire, de unos 2,2 bloques de Minecraft
> de ancho. La cresta se enrolla hacia delante formando un tubo, con el frente afilado como una hoja y espuma
> blanca desprendiéndose del borde superior. El cuerpo de la ola es liso, pulido y muy brillante, con un
> reflejo especular blanco marcado, y muy translúcido: se ve el otro lado a través. Azul cian con azul profundo
> en el hueco del tubo. Estilo Minecraft voxel: caras planas, bordes duros, sin degradados suaves ni
> antialiasing. Paleta de 4 tonos azules. Objeto único centrado sobre fondo gris neutro liso, vista tres
> cuartos isométrica ligera, sin personajes ni escenario. Sin texto, sin marco.

> **Prompt (alternativo, fiel a la textura):**
> Una esfera de agua perfectamente lisa y pulida, de unos 1,8 bloques de Minecraft de diámetro, con la
> superficie tensa como una gota gigante y un reflejo especular blanco marcado arriba a la izquierda. Muy
> translúcida, se ve el otro lado a través. Azul cian con azul profundo en la base. Sin espuma, sin
> ondulaciones, sin aristas: la contundencia viene de lo limpia que es. Estilo Minecraft voxel: caras planas,
> bordes duros, sin degradados suaves ni antialiasing. Paleta de 4 tonos azules. Objeto único centrado sobre
> fondo gris neutro liso, vista tres cuartos isométrica ligera, sin personajes ni escenario. Sin texto, sin
> marco.

---

## 8. Coherencia entre piezas

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

**Viento — casi todo es hueco y calado; se ve por la forma, no por el color:**

| # | Jutsu | Forma | Tamaño aprox. | Dónde aparece | Gama |
|---|---|---|---|---|---|
| 1 | Propelling Winds | Disco achatado | 2 bl. | Bajo los pies | Blanco |
| 2 | Wind Torpedo | Espiral alargada | 1,1 bl. | En vuelo | Blanco |
| 3 | Wind Instantaneous Body | Aro sin centro | 2 bl. | Punto de salida | Blanco |
| 4 | Vacuum Wave | Anillos concéntricos | 20 bl. Ø | A ras de suelo | Blanco |
| 5 | Drilling Air Bullet | Cono de rejilla | 1,3 bl. | En vuelo | Blanco |
| 6 | Wind Mode | Placas separadas | 1,4 × 2,2 bl. | Envolviendo | Verde |
| 7 | Rasengan | Esfera estriada | 0,8 bl. | Sobre la palma | Cian |
| 8 | Big Ball Rasengan | Esfera estriada | 1,6 bl. | Sobre el brazo | Cian |
| 9 | Wind Cloak | Cáscara continua | 1,3 × 2,4 bl. | Envolviendo | Verde |
| 10 | Rasenshuriken | Molinete de 4 aspas | 2 bl. | En vuelo | Cian |

**Tierra — pesado, opaco y pegado al suelo; el único elemento que no brilla:**

| # | Jutsu | Forma | Tamaño aprox. | Dónde aparece |
|---|---|---|---|---|
| 1 | Earth Prison | Anillo de losas | 10 bl. Ø | Del suelo |
| 2 | Rock Gun | Canto rodado | 0,5 bl. | En vuelo (arquea) |
| 3 | Earth Wall | *(bloques vanilla)* | 3×4 bl. | Delante |
| 4 | Earth Instantaneous Body | Esfera de púas gruesas | 2 bl. | Punto de salida |
| 5 | Earth Golem | *(bloques vanilla, o entidad)* | 3×3 bl. | Delante |
| 6 | Earth Dragon Blast | Cabeza direccional | 2,2 bl. | En vuelo (arquea) |
| 7 | Tunnelling | Agujero | 1,5 bl. Ø | En el suelo |

**Agua — translúcido, brillante y poco agresivo; el elemento de control:**

| # | Jutsu | Forma | Tamaño aprox. | Dónde aparece |
|---|---|---|---|---|
| 1 | Water Prison | Burbuja envolvente | 2,4 bl. | Sobre el objetivo |
| 2 | Water Puddle | *(agua vanilla)* | 3×1 bl. | Delante, en el suelo |
| 3 | Breathing Underwater | Cúmulo de burbujas | 1 bl. | En la cabeza |
| 4 | Water Instantaneous Body | Corona de gotas | 2 bl. | Punto de salida |
| 5 | Hidden Mist | Banco de niebla | 16 bl. Ø | Alrededor |
| 6 | Rising Water Slicer | Láminas afiladas | 1,4 bl. | En vuelo |
| 7 | Water Severing Wave | Ola que rompe | 2,2 bl. | En vuelo |

### Cómo se separan los elementos entre sí

Con cuatro elementos documentados conviene fijar la regla, porque a partir de aquí es fácil que empiecen a
parecerse:

| | Fuego | Rayo | Viento | Tierra | Agua |
|---|---|---|---|---|---|
| **Materia** | Masa maciza | Estructura angulosa | Estructura hueca y calada | Materia real, con peso | **Fluido con volumen** |
| **Contorno** | Orgánico, irregular | Zigzag duro | Curvas limpias, espirales | Facetado, romo | Ondulado, tenso |
| **Color** | Naranja-rojo saturado | Cian, dorado, blanco | Casi incoloro (+ verde y cian) | Marrones y grises | Azul cian (+ blanco) |
| **Dónde vive** | Volando y envolviendo | Pegado al cuerpo | Repartido en las tres | Pegado al suelo | Envolviendo y volando |
| **Luz** | Emisivo | Emisivo denso | Semitransparente | Mate y opaco | **Translúcido y con brillo especular** |
| **Física** | Vuelo recto | Instantáneo | Vuelo recto | Gravedad: arquea y cae | Vuelo recto |
| **Carácter** | Ofensivo | Ofensivo | Movilidad | Defensivo y de control | **Control: 5 de 7 sin daño** |

Tres bazas que conviene explotar porque son exclusivas:

- **Viento es el único incoloro.** Se ve por la deformación, no por el color.
- **Tierra es el único que no brilla.** En una escena con los cinco, tierra debe leerse como lo único
  material opaco.
- **Agua es el único con reflejo especular.** Translúcido *y* pulido: es lo que lo separa de viento, que
  también deja ver a través pero es mate y sin brillo.

### Colisiones internas a vigilar

Estas parejas comparten sitio, escala o material, y pueden confundirse si el arte no las separa a propósito:

| Pareja | Riesgo | Cómo se resuelve |
|---|---|---|
| Rasengan (viento 7) vs. Chidori (rayo 5) | Ambas son esferas de ~0,8 bl. en la mano | El Rasengan **se sostiene sobre la palma abierta**; el Chidori **envuelve el puño cerrado**. Además azul liso vs. cian erizado |
| Fire Transport (fuego 3) vs. Wind Inst. Body (viento 3) vs. Lightning Inst. Body (rayo 2) | Los tres son el destello del teleport, todos de ~2 bl. | Fuego: **núcleo macizo** con púas gruesas. Rayo: **rayos finos desde un centro brillante**. Viento: **aro sin centro alguno** |
| Drilling Air Bullet (viento 5) vs. Drilling Hand (rayo 8) | Ambos son "taladros" cónicos | Viento: **rejilla romboidal tensa**, en vuelo, incoloro. Rayo: **espiral helicoidal**, en el antebrazo, cian brillante |
| Los **cuatro shunshin** (fuego 3, rayo 2, viento 3, tierra 4) | Los cuatro son el destello del teleport, todos de ~2 bl. | Fuego **arde** (núcleo macizo, púas gruesas). Rayo **destella** (rayos finos desde un centro). Viento **se aparta** (aro sin centro). Tierra **revienta** (púas gruesas, terrones sueltos) |
| Earth Dragon Blast (tierra 6) vs. Exploding Dragon Strike (fuego 5) | Ambas son cabezas de dragón direccionales de ~2 bl. | Fuego: **llamas envolviendo** un núcleo oscuro, emisivo. Tierra: **planos de piedra** angulosos, mate, con melena de esquirlas y trayectoria descendente |
| Water Prison (agua 1) vs. Earth Prison (tierra 1) | Las dos inmovilizan, mismo coste, radio parecido | Agua **encierra** (burbuja cerrada sobre el objetivo). Tierra **cerca** (anillo abierto de losas que brota del suelo) |
| Rising Water Slicer (agua 6) vs. los jutsus de **hielo** (KG) | Mismo azul y forma de espiga; la textura ya se parece | Agua **fluye**: bordes ondulados, goteo, translúcida. Hielo: **opaco, rígido y facetado**. Pendiente de fijar al documentar el KG |
| Water Instantaneous Body (agua 4) vs. Lightning Inst. Body (rayo 2) | Discos radiales, ambos azul-cian, mismo tamaño | Agua: **gotas de punta redondeada**, más anchas que su base. Rayo: **rayos rectos y finos** desde un centro brillante |

---

## 9. Pendiente de decidir

1. **El color de Lightning Cutter.** Hoy es violeta y rompe su parentesco con el Chidori (§4). He escrito el
   prompt en cian-blanco con la alternativa violeta anotada. Decide antes de generar.
2. **Las tres gamas de viento** (§5). A diferencia del punto anterior, aquí **recomiendo no tocar nada**: la
   división blanco / cian / verde es coherente y fiel al material original. Lo anoto para que sea una decisión
   consciente y no una inconsistencia heredada.
3. **Cuatro `*_cloak` no existen.** `fire_cloak`, `lightning_cloak`, `earth_cloak` y `water_cloak` cierran su
   secuencia de learner en los datos, pero **el jutsu no está implementado** (auditoría §1.6). Los cuatro
   serían auras envolventes; ahora que `wind_cloak` y `wind_mode` dan la plantilla, replicarla es directo.
   **Con las cinco naturalezas básicas ya documentadas, viento sigue siendo el único con la rama entera.**
4. **Tres texturas se salen del formato.** De las 39 revisadas, todas son 32×32 salvo
   `drilling_hand_of_sage_lightning` (**64×128**, la única no cuadrada), `big_ball_rasengan` (**128×128**) y
   `hidden_mist` (**64×64**). Con `item/generated` la primera se renderiza raro; conviene mirarla al tocar esa
   pieza. Las otras dos no son un problema — el detalle extra es una ventaja al modelar, y en los tres casos
   el tamaño mayor delata **qué piezas le importaban más al autor original**.
5. **Earth Golem: la textura promete una criatura y el código da un muro** (§6.5). Tres opciones con costes muy
   distintos; **decidir antes de encargar arte**, porque la opción C es un ticket de programación completo.
6. **Los dos `structure` de tierra** (§1): bloques vanilla, bloque propio, o nada. Si eliges bloque propio, lo
   que se encarga es **textura de 16×16 que tesele**, no modelo de Blockbench. Afecta también a los 16 KG:
   `structure` aparece en madera, hielo, cristal, acero, magnetismo, lodo y fuinjutsu.
7. **Programación previa.** De los 39 jutsus documentados, **26 no tienen hoy dónde colgar un modelo** (§1).
   Antes de que se vean en partida hacen falta, por orden de retorno:
   - un **renderer propio para `ninja_projectile`** que dibuje modelos en vez del `displayItem` → desbloquea de
     golpe **12 piezas** (5 de fuego + 3 de viento + 2 de tierra + 2 de agua) y todos los proyectiles de los
     elementos restantes. Es una sola pieza de código y la de mayor impacto con diferencia;
   - un visual de **ventana de carga** para `primed_strike` → desbloquea **6** (4 de rayo + 2 de viento);
   - un visual de **estallido de área** para `area` → desbloquea **7** (3 de rayo + 1 de viento + 1 de tierra
     + 2 de agua) y buena parte de los 16 Kekkei Genkai, donde `area` es la familia dominante;
   - un visual de **aura sobre el jugador** para `buff` → desbloquea **5** (1 de fuego + 3 de viento + 1 de
     agua), y hará falta igualmente para los seis modos activables (Susanoo, sabio, sello maldito, puertas,
     SOAH, Tenseigan);
   - un visual de **salida de teleport** para `teleport` → desbloquea **5** (uno por naturaleza básica, más
     `tunnelling`, que son 6 en total), que hoy comparten las mismas partículas `POOF` y son indistinguibles
     entre sí.

---

## 10. Nota de vigencia

Este documento se escribió en cuatro tandas y **el repositorio avanzó entre medias**. Durante la sesión del
2026-08-07 se comprobó que el árbol había crecido respecto al estado de `AUDITORIA_ESTADO_ACTUAL.md`: el mod
llegó a la **versión 1.0.0**, apareció el paquete `api`, una segunda `BeastMove` y —lo relevante para este
documento— **`EffectSfx`, el sistema de sonido por técnica** (F10/DJT-227), con 13 jutsus ya declarando `sound`
en sus `effectParams`.

Consecuencia para el arte: **los efectos ya no son mudos**. Al diseñar una pieza conviene mirar si su JSON
declara `sound`, porque el sonido condiciona el ritmo del efecto (un `primed_strike` con sonido de carga pide
un visual que dure lo mismo). De los 32 documentados, tienen sonido: `earth_instantaneous_body`, `chidori`,
`lightning_cutter`, `drilling_hand_of_sage_lightning`, `rasengan`, `big_ball_rasengan`, `rasenshuriken` y
`wind_instantaneous_body`.

**Antes de una tanda grande de encargos de arte conviene revalidar §1 contra el código**, no fiarse de esta
tabla: es lo que decide si hay dónde colgar cada modelo, y es justo lo que está cambiando.
