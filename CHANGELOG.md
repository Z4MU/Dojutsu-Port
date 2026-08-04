# Changelog

Formato: [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/). Versionado semántico.

## [1.0.0] — 2026-08-03

Primera versión completa. El datapack **Dojutsu v1.9.3** portado a mod multiloader para Minecraft 1.21.1, en
once fases.

### Contenido

- **Clanes y creación de personaje** — 14 clanes, cada uno con su ojo potencial, su naturaleza y su técnica
  inicial.
- **Chakra y progresión** — regeneración, rangos por ninja_xp, puntos de habilidad, 36 hitos de desbloqueo.
- **128 jutsus** en 29 categorías, con siete familias de efecto data-driven.
- **15 ojos** con su grafo de evolución, incluidos los 8 Mangekyō por personaje, el Eterno, el Rinnegan, el
  Rinne Sharingan y el catálogo de 21 habilidades oculares.
- **Siete modos de potenciación** sobre un arnés compartido: Sabio, Sello Maldito, Ocho Puertas, Fuerza de
  Cien, Susanoo, Tenseigan Chakra Mode y forma de jinchūriki.
- **Bestias con Cola** — las nueve más el Diez Colas: sellado, vínculo, IA, técnicas insignia, Modo Baryon.
- **Mundo y narrativa** — Seis Caminos, Árbol Divino, encuentros con los Sabios.
- **Ítems, comercio y crafteo** — armas, arrojadizas, máscaras, pergaminos, drop-crafting y mercaderes shinobi.
- **API pública** `com.zamu.dojutsu.api` v1.0.0 para otros mods.

### Lo destacado del último tramo

- **Sonido** — 19 de los 28 sonidos del pack no se reproducían nunca y cinco sonaban con valores equivocados.
  Se cablearon todos salvo los ocho cuya técnica aún no está portada. [`docs/sonido.md`](docs/sonido.md)
- **Cuerpo de las Bestias con Cola** — Gyūki y Kurama se dibujan con su modelo real. Las otras siete usan el
  ravager, que es lo que hace el original. [`docs/bijuu_arte.md`](docs/bijuu_arte.md)
- **Balance** — resueltos los 16 valores pendientes contra el datapack; corregidos cinco umbrales de kenjutsu
  que estaban inventados.
- **Rendimiento** — auditados los 21 puntos de entrada por tick; ninguno sin acotar. [`docs/perf.md`](docs/perf.md)
- **Permisos** — los 20 comandos de depuración pasaron a exigir nivel de operador 2.

### Fidelidad

Cada valor está contrastado contra los `.mcfunction`, los predicates y los JSON del datapack original. Donde el
mod se aparta —y se aparta en varios sitios— está escrito por qué en
[`docs/development_log.md`](docs/development_log.md). Las divergencias deliberadas más visibles:

- El **Amenominaka** no hace `forceload` de la dimensión destino.
- El **Árbol Divino** puede proteger construcciones con `godTreeProtectsBuilds`, que es opción del mod y viene
  desactivada para no alterar el comportamiento original.
- No se reproduce el **cubo de modelo ausente** que en el original flota sobre siete de las bestias.

### Conocido

- Sólo hay traducción al **inglés**.
- Falta arte: modelos del Susanoo y de los Mangekyō por personaje.
- Tres técnicas del Diez Colas sin implementar, y ocho sonidos cuya técnica no está portada.
- El catálogo del mercader errante diverge del datapack en ocho mercancías y en el precio de las ofertas de ojo.

Lista completa en [`docs/known_issues.md`](docs/known_issues.md).
