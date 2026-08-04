# Problemas conocidos y validación manual pendiente — Dojutsu Port

> ⚠️ **Desde el 2026-08-01 la validación manual vive en
> [`checklist_paridad_cliente.md`](checklist_paridad_cliente.md)** (F10/DJT-223), que la organiza por sistema y
> por loader y dice qué está ya verificado sin jugar. Las secciones §1, §1b y §1d de este documento se
> conservan como referencia histórica de lo que pidió cada fase.

> Documento vivo. Registra deuda de validación, riesgos residuales y limitaciones conocidas que **no** son
> bugs demostrados (esos van a los tickets/`development_log.md`). Última actualización: 2026-07-19.

## 1. Validación manual in-game pendiente (residual de F4)

La sesión de validación runtime del 2026-07-19 (ver `current_state.md §3.4a`) confirmó, en **NeoForge y Fabric**,
que todo el contenido de F4 **compila, pasa los 541 tests y carga sin error** bajo el codec estricto al arrancar
el servidor dedicado. Lo que **no** se pudo automatizar (requiere un jugador en el mundo, cliente interactivo)
queda como checklist manual. No bloquea F5, pero conviene ejecutarlo antes de un release.

Checklist (marcar al verificar en **ambos** loaders, cliente + servidor):

- [ ] Castear una técnica de **cada familia de efecto** y observar el efecto real:
  - [ ] `projectile` (p. ej. `fire/fire_bullet`) — vuela, colisiona por raycast, aplica daño.
  - [ ] `area` (p. ej. `genjutsu/*`) — afecta a las entidades en el radio.
  - [ ] `buff` (p. ej. `fire/fireproof`) — aplica el efecto al lanzador.
  - [ ] `teleport` (p. ej. `*_instantaneous_body`) — reubica al lanzador.
  - [ ] `structure` (p. ej. `earth/earth_wall`) — coloca los bloques y revierte si aplica.
  - [ ] `heal` (p. ej. `medical/mystical_palm`, `universal/healing`) — cura la cantidad correcta.
  - [ ] `primed_strike` (p. ej. `lightning/chidori`, `wind/rasengan`) — arma al castear y **descarga al golpear**.
- [ ] **Aprendizaje secuencial**: usar el Skill Learner de un elemento entrega los jutsus en orden y el último
      consume el learner.
- [ ] **Coste/cooldown/afinidad**: el cast respeta chakra, cd por-instancia y gate de naturaleza/afinidad/KG.
- [ ] **XP de afinidad**: cada cast con afinidad da +1 XP y, al subir de nivel, escala daño/desbloqueos.
- [ ] **Kenjutsu**: arma con bonus reversible (`ItemAttributeModifiers`); Samehada absorbe chakra al golpear.
- [ ] **Assets (DJT-085b/c/d)**: texturas/modelos de los 24 jutsus, 3 armas y 24 elementos del learner se ven
      correctamente vía `custom_model_data` (DJT-085c/d nunca se verificaron visualmente).

## 1b. Validación manual in-game pendiente (F5 — mecánicas time-gated)

Algunas mecánicas de ojos son **time-gated** y no se pueden conducir en un servidor headless (requieren un
jugador conectado jugando hasta el umbral). La lógica está cubierta por tests unitarios; el cableado y el
arranque están validados en runtime en ambos loaders. Falta el end-to-end in-game:

- [ ] **Despertar del dojutsu (DJT-114):** con un ninja de clan Hyuga/Uchiha/Chinoike/Otsutsuki, jugar hasta el
      umbral (`awakenTicks`: 78000 ≈ 65 min; Ketsuryugan 24000 ≈ 20 min) y confirmar que el ojo despierta:
      `EyeState.activeEye` fijado, familia en `awakened`, +max_chakra y +ninja_xp aplicados (barra/menú), y que
      Otsutsuki obtiene byakugan **o** sharingan (50/50). Verificar que **no** vuelve a despertar (una vez por
      clan) y que un Senju (sin `potentialDojutsu`) nunca despierta. *(Atajo para QA: bajar temporalmente
      `awakenTicks` en el JSON del ojo y `/reload`.)*
- [ ] **Evolución por uso (DJT-115):** con el ojo activo, jugar hasta el umbral de uso (`use_level`: Sharingan→
      Mangekyō 360000 ≈ 5 h de ojo activo) y confirmar la transición correcta por la **ruta del clan**: un
      Uchiha/Otsutsuki pasa a Mangekyō; un implante (otro clan) **no** (se queda en Sharingan aunque acumule uso);
      un Uchiha con ADN de Hashirama pasa a Rinnegan. Verificar el bonus del destino (Mangekyō +20/+80, Rinnegan
      +100/+150) y `EyeEvolvedEvent`. *(Atajo QA: bajar `use_level.min` en el JSON + `/reload`.)*

## 1c. Fidelidad — hueco de DJT-114 (uso inicial del Sharingan al despertar) — ✅ RESUELTO (2026-07-19)

En el datapack, `sharingan/awaken` fija `d.sharingan_use = 78000` (el Sharingan despierta ya en **tomoe 1**). El
`AwakeningService` (DJT-114) no lo fijaba → el Sharingan arrancaba en uso 0. **Corregido**: se añadió el campo
opcional `awakenInitialUse` a `EyeDefinition` (`sharingan.json` = 78000; Byakugan/Ketsuryugan no lo fijan) y
`AwakeningService.evaluate` hace `withUseLevel(family, awakenInitialUse)` al despertar. Cubierto por 2 tests
(`AwakeningServiceTest`, `EyeDefinitionTest`).

## 1d. Validación manual in-game pendiente (F10/DJT-225 — los 17 ítems nuevos)

Todo lo de DJT-225 está validado headless (build, 930 tests, arranque limpio en ambos loaders con la carga
correcta), pero **nada de esto lo ve un test**: es visual o depende de la física del ítem al caer. Va al
checklist de DJT-223.

- [ ] **Bomba de humo**: soltarla y confirmar que la cortina sale **donde cayó el ítem** (no en la mano), dura
      6 s y **tapa la vista de verdad**. No debe aplicar ningún efecto de estado a nadie.
- [ ] **Sello explosivo**: soltarlo, confirmar que **se ve** el papel donde cayó, que **no** estalla durante
      10 s, y que después detona al acercarse. Confirmar que **sobrevive a un reinicio del servidor** (es la
      única entidad persistente no-criatura del mod, y esa es toda la razón de que no sea un `AbilityAnchor`).
- [ ] **Los 9 huevos**: cada uno invoca **su** bestia (no siempre Shukaku) al usarlo sobre un bloque, con su
      bossbar, y se consume fuera de creativo.
- [ ] **Drop-crafting de los huevos**: tirar juntos `nether_star` + `diamond_block` + `gold_block` + la lana
      del color correcto y ver salir el huevo (las 9 combinaciones ya cargan sin pendientes).
- [ ] **Ramen**: comerlo repone **150** de chakra (50 + 100), no 100.
- [ ] **Pergaminos**: el médico llena chakra **y** pone la fatiga ocular a 0; el de sabio **no se gasta** si no
      eres sabio (aviso rojo + yunque); el de retirada apaga Baryon y la forma antes de extraer la bestia.
- [ ] **Puertas de crafteo**: las tres nuevas rechazan, **devuelven los ingredientes exactos** y suenan el
      yunque; el de sabio **cobra 50** de energía natural sólo cuando sale bien.
- [ ] **Modelos 3D**: tanto y los tres pergaminos (Blockbench, reapuntados a `dojutsu:`) se ven bien en mano,
      en el suelo y en el GUI.

## 2. Riesgos y limitaciones conocidas (no bugs)

- ~~**Sin control de versiones desde F0**~~ ✅ **RESUELTO (F10)**: las nueve fases que vivían sin commitear están
  en la rama **`f1-f10-port`**. `main` sigue en F0 a la espera de un `git merge --ff-only` cuando se quiera
  publicar.
- **Puerto `AttributeService` diferido**: kenjutsu lo evitó con `ItemAttributeModifiers` vanilla. Se retomará
  cuando un sistema necesite modificadores **sobre la entidad** (previsiblemente Ocho Puertas, F6).
- ~~**Umbrales `unlocks/progression.json` provisionales** y `TBD_datapack` en `xp_sources.json`~~ ✅ **RESUELTO
  (F10/DJT-221)**: los 16 TBD cuantificados contra el datapack y los umbrales corregidos (los cinco de kenjutsu
  estaban inventados). Quedan **tres hitos sin portar, con su motivo escrito** en `xp_sources.json`: `izanagi`
  (exige dos afinidades **y** otro predicate: necesita requisitos compuestos), `senjutsu_access` (hito **sólo**
  de chakra máximo, sin habilidad que lo dispare) y la vía de `blaze_release` por afinidad de fuego 2000 (en el
  mod el KG se concede por `EyeDefinition.awakenKekkeiGenkai`).
- **`ModeToggledEvent` no tiene ni un suscriptor** (verificado con Serena en DJT-226): los siete modos lo
  publican y nadie lo escucha. No es un fallo —el evento existe para que el día que haga falta reaccionar a un
  modo no haya que tocar los siete servicios— pero conviene saberlo antes de razonar sobre su orden de emisión.
- **Tres técnicas del Diez Colas siguen sin implementar** (de DJT-226, con sus números ya verificados):
  `tenpenchii` (3 rayos por objetivo en radio 10 + `instant_damage` amp 3 + trueno + 1200 t de enfriamiento),
  `truth_seeking_orb` y `tailed_beast_ball_ten_tails`. El plan de IA ya los pide; hoy el Diez Colas cae a la
  andanada de Brazos de Chakra, que sí existe.
- **Catálogo del mercader errante divergente del datapack** (deuda de DJT-190, detectada en DJT-225): el
  datapack vende también inyector, píldoras de despertar mental, Kotarō, pergamino de entrenamiento,
  Kubikiribōchō, Samehada, Sword of Kusanagi y pergamino de experiencia; y sus ofertas de ojos cuestan
  **64+32** esmeraldas, no 64. El mod ofrece además ADN, contrato, máscara ANBU y fuma shuriken, que en el
  datapack no vende el mercader. Reconciliarlo es trabajo de DJT-223. En la misma línea: `kubikiribocho`
  anuncia "Kenjutsu: 800" en el datapack y el mod lo registra con `kenjutsuReq` 0 (inofensivo hoy —sólo lo lee
  la Samehada— pero es una cifra perdida).
- ~~**19 de los 28 sonidos no se reproducen nunca**~~ ✅ **RESUELTO (DJT-227)**: quedan **8 mudos**, y no por
  deuda de sonido sino porque **su técnica no está portada** — `jutsu.wind` (entrenamiento de naturaleza),
  `flute_song` (`phantom_sound_chains`), `frog_call` y `white_rage_technique` + `ear_ringing` (invocaciones de
  sabio sapo y serpiente), `sharingan.blind` (Izanagi) y los dos de Isshiki. El **Jōgan** está en el enum de
  familias pero sus dos técnicas tampoco existen. Todos registrados y listos para cuando se porte el sistema.
- **La verificación del sonido es de partida** (DJT-227): que se dispare lo cubre el build; que **se oiga bien**
  no lo ve ningún test. Sin comprobar: si la puerta compartida de 20 ticks corta de más al encadenar técnicas
  del Mangekyō, si el alcance enviado a mano suena igual que el selector del datapack, y si el Susanoo con su
  sonido propio resulta molesto a 8 bloques.
- **Corrección: las Bestias con Cola no tenían «24 assets pendientes»** (DJT-228). El recuento anterior daba por
  hecho que existían nueve modelos por portar. **El resource pack original sólo trae dos** —Gyūki (965 cubos) y
  Kurama (1243)—; los otros ocho `CustomModelData` apuntan a ficheros que no existen, así que en el juego
  original siete bestias y el Diez Colas se ven como un **ravager** con el cubo de "modelo ausente" encima. Y el
  ravager tampoco era una improvisación del mod: el datapack invoca uno de verdad y **la línea que lo haría
  invisible está comentada**. Portar las siete que faltan sería **autoría de arte nuevo**, no porte. Detalle en
  `docs/bijuu_arte.md`.
- **Las texturas de las dos bestias van a 1024px, no a su resolución original** (DJT-228, decisión del usuario
  con las dos cifras medidas): a 4096/2048 el atlas de bloques compartido salta a 8192×4096 (**128 MB** de VRAM
  + mipmaps) frente a 2048×2048 (**16 MB**). Los originales siguen en el resource pack de referencia.
- **Assets parciales** (DJT-224, pasada 1 hecha; +DJT-225 aportó los 19 PNG de sus ítems): ya están **ítems**
  (máscaras, arrojadizas, ADN, contrato, píldoras, huevos de bestia, pergaminos, ramen, bomba de humo, sello,
  tanto), **iconos de clan** y los **28 sonidos**. Siguen pendientes de la pasada 2: **ojos** (52 texturas),
  **Susanoo** (56 modelos), **bijuu** (24, hoy con la malla prestada del ravager), bandanas/protectores; HUD
  con primitivas. El arte de Susanoo y bijuu del RP son **modelos de ítem**, no mallas de entidad, así que la
  pasada 2 es una capa de render que dibuja un `BakedModel`, no autoría de mallas.
- **La API pública no tiene consumidor real todavía** (F10/DJT-220). Está validada por 9 tests escritos desde
  fuera del paquete, que es el criterio que pedía la ficha, pero **ningún mod de terceros la ha usado en
  ejecución**: un aportador que se registre tarde —después de la primera recarga de datos— no aparecerá hasta el
  siguiente `/reload`, y eso sólo se ve con un segundo mod cargado de verdad. Lo mismo con las escrituras
  (`restoreChakra`, `consumeChakra`, `addNinjaXp`, `grantJutsu`): necesitan un `ServerPlayer` y por tanto un
  servidor, así que su verificación es de partida, no de test.
- **La API no expone temporizadores ni nada de cliente**, a propósito: los pulsos de modo, los enfriamientos
  internos y los contadores de despertar son detalle de implementación, y todo lo publicado es de servidor. Un
  mod que quiera pintar algo tiene que usar sus propios hooks de cliente. Si alguna vez hace falta, es una subida
  de versión **menor**, no un cambio de contrato.
- ~~**`LICENSE` no-comercial pendiente**~~ ✅ **RESUELTO (DJT-224)**: `LICENSE` (MIT código / CC BY-NC arte,
  cláusula no comercial de Prof Technic marcada como vinculante y prevalente), atribución en los metadatos de
  los dos loaders y `ATTRIBUTION.md` con el inventario de qué asset vino de dónde.

## 3. Colisión de numeración de tickets F5

El backlog de diseño (`DOCS/backlog/06_FASES5-10…`) planificó la Fase 5 como DJT-100…125, pero la ejecución de
F4 ya consumió DJT-085…112. **Los tickets de F5 se renumeran a la serie libre DJT-113+** (mapa en
`docs/backlog.md §Fase 5`). Los ids son orden de asignación, no de ejecución (precedente: DJT-063 en F3).
