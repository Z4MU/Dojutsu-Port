# Sonido — inventario y estado (F10/DJT-227)

Los **28 eventos** del resource pack original, dónde suena cada uno en el datapack y qué hace el mod hoy.

## El recuento anterior estaba mal

DJT-224 dejó escrito que **ocho de los 28 eran «arte producido y nunca cableado»**. Es falso. El error estaba en
cómo se buscó: el datapack invoca buena parte de sus sonidos **con el namespace delante**

```
playsound minecraft:dojutsu.isshiki.reappear master @a[distance=0..8] ~ ~ ~ 10 1
```

y un `grep` de `playsound dojutsu.` los dejaba fuera. Contando las dos formas salen **92 llamadas** en 252
ficheros, y **los 28 sonidos aparecen en alguna**.

Lo que se guardó como hecho —«estos ocho no los usa nadie»— es justo lo que nadie vuelve a comprobar. De ahí que
esta tabla exista: para que el siguiente recuento se pueda contrastar en vez de creerse.

## Dos mandos, no uno

El `playsound` del datapack decide quién oye algo con **volumen y selector**. Vanilla sólo tiene el volumen: el
radio de difusión es `max(16, 16 × volumen)` y `Level.playSound` no deja tocarlo. La diferencia no es teórica —
`medical_ninjutsu.healing` suena a **volumen 10 en 5 bloques**, y por la vía vanilla se oiría a **160**.

Por eso `NinjaSfx` manda el `ClientboundSoundPacket` a los jugadores dentro del radio: el volumen queda para
cuánto suena y el alcance lo pone el selector, como en el original.

El volumen sigue importando aunque el cliente recorte la sonoridad a 1.0, porque **la atenuación por distancia
usa el volumen crudo**: a 10 el sonido es plano en todo su radio; a 1.0 se desvanece a los pocos bloques. Son
sonidos distintos, y por eso ahora se copian los valores literales.

## La puerta `d.sfx`

Catorce llamadas van precedidas de `unless predicate dojutsu:sfx_delay`. No es el cooldown de la técnica: es el
del **sonido**, para que castear en ráfaga no solape treinta copias del mismo audio. Es **uno solo y
compartido** por jugador —encadenar dos habilidades distintas dentro de la ventana deja muda a la segunda— y el
datapack lo baja 1 por tick desde 20 (8, 5, 3 y 2 en las de sabio y algunos jutsus).

En el mod se guarda **el tick en que vuelve a estar permitido** en lugar de un contador que baje solo: misma
semántica, cero trabajo por tick.

## Estado por sonido

### Suenan (20)

| Sonido | Cuándo | Dónde vive |
|---|---|---|
| `byakugan.activate` | despertar (0.3) y evolución (0.1) | `EyeSounds` |
| `ketsuryugan.activate` | despertar; **+ sus 4 técnicas** (0.5) | `EyeSounds` · `EyeAbilitySfx` |
| `tenseigan.activate` | despertar y activación del ojo (1.0) | `EyeSounds` |
| `sharingan.mangekyo.activate` | despertar de Sharingan y Mangekyō (0.1); **+ 8 técnicas** | `EyeSounds` · `EyeAbilitySfx` |
| `rinnegan.activate` | despertar; **+ 4 de los Seis Caminos** (0.5) | `EyeSounds` · `EyeAbilitySfx` |
| `rinne_sharingan.activate` | florecer del Árbol Divino (0.8) | `GodTreeService` |
| `rinne_sharingan.amenominaka` | los 4 destinos y la vuelta (0.5, `@s`) | `AmenominakaAbility` |
| `rinnegan.amenotejikara` | intercambio de posición (0.5, 5 bloques) | `EyeAbilitySfx` |
| `rinnegan.bansho_tenin` | atracción (0.5) | `EyeAbilitySfx` |
| `sharingan.genjutsu` | aturdimiento (**master, 10, 5 bloques**) | `EyeAbilitySfx` |
| `sharingan.mangekyo.susanoo.activate_1` | encender (1.0) y apagar (0.6, tono 1.5) | `SusanooService` |
| `medical_ninjutsu.healing` | los 6 jutsus médicos **+ recarga del sello SOAH** | `HealEffect` · `SoahSealService` |
| `jutsu.summoning` | invocación (0.4) | `SummonService` |
| `jutsu.rasengan` | **al armar** Rasengan y Gran Bola (0.6) | `PrimedStrikeEffect` (JSON) |
| `jutsu.chidori` | al armar Chidori (tono 1) y Corta-Relámpagos (**0.9**) | `PrimedStrikeEffect` (JSON) |
| `jutsu.lightning` | al armar la Mano Perforadora del Sabio (0.6) | `PrimedStrikeEffect` (JSON) |
| `jutsu.rasenshuriken` | **al lanzar** (1.0) | `ProjectileEffect` (JSON) |
| `jutsu.wood` | 3 de las 9 estructuras de madera (**5.0, 15 bloques**) | `StructureEffect` (JSON) |
| `jutsu.earth` | Cuerpo Parpadeante de tierra (0.8, tono 2) | `TeleportEffect` (JSON) |
| `jougan.activate` | sus 2 técnicas (0.5) | — *(ver abajo)* |

### Mudos porque su sistema no está portado (8)

No es deuda de sonido: es que la técnica no existe todavía. Cuando se porte, el sonido ya está registrado.

| Sonido | Le falta |
|---|---|
| `jutsu.wind` | entrenamiento de naturaleza |
| `jutsu.flute_song` | `phantom_sound_chains` |
| `jutsu.frog_call` | invocaciones de sabio sapo |
| `jutsu.white_rage_technique` | invocaciones de sabio serpiente |
| `misc.ear_ringing` | ídem (lo sufre quien la recibe, `@s`) |
| `sharingan.blind` | Izanagi |
| `isshiki.activate` | Isshiki (Sukunahikona) |
| `isshiki.reappear` | ídem |

El **Jōgan** está en el enum de familias pero sus dos técnicas (`bodily_restriction`,
`explosive_shockwave`) no están portadas, así que su sonido tampoco tiene dónde sonar todavía.

## Cinco que sonaban mal

Además de los que no sonaban, había cinco cableados con valores que no eran los del datapack:

| Qué | Estaba | Es |
|---|---|---|
| Susanoo al encender/apagar | `entity.illusioner.cast_spell` | **su propio sonido**, que existía sin usarse |
| Tenseigan Chakra Mode al encender | tono 1.0 | tono **2** (el par sonaba desparejo) |
| Kamui al teletransportar | `players`, tono 1.0 | **`master`, tono 0** — de ahí su timbre hueco |
| Kamui al marcar | sonaba | **silencio**: `kamui_set` no tiene ningún `playsound` |
| Recarga del sello SOAH | tintineo de amatista | `medical_ninjutsu.healing` |
| Curación | volumen 1.0 | volumen **10** en 5 bloques |

## Dónde se declara

- **Por habilidad ocular**: `EyeAbilitySfx`, una tabla leída desde el único punto de casteo.
  **No es una regla por familia** — Amaterasu, las tres del Byakugan y las dos del Tenseigan están mudas en el
  original, y Kamui usa un sonido vanilla. Hay tests que fallan si alguien lo convierte en regla.
- **Por jutsu**: en su JSON (`sound`, `soundSource`, `soundVolume`, `soundPitch`, `soundRange`), leído por
  `EffectSfx`. Ni el volumen, ni el tono, ni el alcance se pueden derivar de la familia: la madera suena a 5 en
  15 bloques y el golpe cargado a 0.6 en 10. **Sin `sound` el jutsu es mudo a propósito.**
- **Por modo**: en su servicio, que es quien sabe si acaba de encenderse o de apagarse.

## Lo que no ve un test

Que el sonido **exista y se dispare** se puede comprobar; que **se oiga bien** no. Quedan para verificar en
partida: que la puerta de 20 ticks no corte de más al encadenar técnicas del Mangekyō, que el alcance manual
suene igual que el del datapack, y que el Susanoo con su sonido propio no resulte molesto a 8 bloques.
