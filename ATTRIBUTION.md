# Inventario de arte de terceros

Todo lo que hay aquí viene del **resource pack original del datapack Dojutsu**, obra de **Prof Technic**, usado
con su permiso y **sólo para uso no comercial** (ver [`LICENSE`](LICENSE) §2).

Este fichero existe para una cosa concreta: que **sustituir el arte por obra propia sea una lista de tareas y
no una excavación**. Cada fila dice qué hay, cuántos archivos son y de dónde salieron. Cuando una fila se
reemplace entera por arte propio, márcala; cuando estén todas, el proyecto pasa a ser MIT completo.

Origen: `D:\Projectos claude\Dojutsu reverse\resource pack\assets\minecraft\`.

## Texturas (203 PNG)

| ☐ | Destino en el mod | Nº | Origen en el resource pack | Ticket |
|---|---|---:|---|---|
| ☐ | `textures/item/jutsu/` | 123 | `textures/jutsu/` | DJT-085b |
| ☐ | `textures/item/learner/` | 24 | `textures/jutsu/learner/` | DJT-085c |
| ☐ | `textures/item/weapon/` | 3 | `textures/weapons/` | DJT-085d |
| ☐ | `textures/gui/clan/` | 14 | `textures/icon/clans/` | DJT-224 |
| ☐ | `textures/item/mask/` | 8 | `textures/masks/` | DJT-224 |
| ☐ | `textures/item/ration_pills/` | 6 | `textures/food/military_rations_pills_*` | DJT-224 |
| ☐ | `textures/item/throwable/` | 4 | `textures/weapons/` + `textures/item/weapons/` | DJT-224 |
| ☐ | `textures/item/dna.png` | 1 | `textures/dna_vile.png` | DJT-224 |
| ☐ | `textures/item/contract.png` | 1 | `textures/item/summoning_contract.png` | DJT-224 |
| ☐ | `textures/item/spawn_egg/` | 9 | `textures/item/custom/spawn_eggs/` | DJT-225 |
| ☐ | `textures/item/scroll/` | 5 | `textures/item/{medical_scroll,medical_scroll_text,scroll,scroll_text,sage_scroll}.png` | DJT-225 |
| ☐ | `textures/item/food/` | 2 | `textures/food/ramen_{1,2}.png` | DJT-225 |
| ☐ | `textures/item/smoke_bomb.png` | 1 | `textures/item/smoke_bomb.png` | DJT-225 |
| ☐ | `textures/item/explosive_tag.png` | 1 | `textures/item/weapons/explosive_tag.png` | DJT-225 |
| ☐ | `textures/item/weapon/tanto.png` | 1 | `textures/weapons/tanto.png` | DJT-225 |

## Sonidos (28 OGG)

| ☐ | Destino en el mod | Nº | Origen | Ticket |
|---|---|---:|---|---|
| ☐ | `sounds/dojutsu/` | 15 | `sounds/dojutsu/` | DJT-224 |
| ☐ | `sounds/jutsu/` | 11 | `sounds/jutsu/` | DJT-224 |
| ☐ | `sounds/medical_ninjutsu/`, `sounds/misc/` | 2 | ídem | DJT-224 |

La estructura de carpetas **se conserva idéntica al RP** a propósito: los nombres de evento en `sounds.json`
derivan de ella y el datapack los invoca por ese nombre exacto (ver `ModSounds`).

## Modelos (173 JSON)

Los modelos son obra derivada: son los del RP con las referencias de textura reapuntadas al namespace
`dojutsu:`. Los 3D vienen de Blockbench y conservan su bloque `credit`.

| ☐ | Destino en el mod | Nº | Origen | Nota |
|---|---|---:|---|---|
| ☐ | `models/item/jutsu/`, `models/item/learner/` | 147 | `models/jutsu/`, `models/jutsu/learner/` | 2D (`item/generated`) |
| ☐ | `models/item/mask_*.json` | 6 | `models/masks/` | **3D** (Blockbench) |
| ☐ | `models/item/{kunai,explosive_kunai,ftg_kunai}.json` | 3 | `models/weapons/` | **3D** |
| ☐ | `models/item/{shuriken,fuma_shuriken}.json` | 2 | `models/weapons/` | 2D con `display` propio |
| ☐ | `models/item/contract.json` | 1 | `models/summoning_contract.json` | **3D** |
| ☐ | `models/item/dna.json` | 1 | `models/dna_vile.json` | 2D |
| ☐ | `models/item/ration_pills/` | 6 | `models/food/military_rations_pills_*` | 2D |
| ☐ | `models/item/{chakra_blade,kubikiribocho,samehada}.json` | 3 | `models/weapons/` | 2D |

## Lo que NO es de terceros

- `models/item/ninja_debug.json` — apunta a una textura **vanilla** (`minecraft:item/nether_star`); es un ítem
  de depuración del mod, sin arte propio.
- `models/item/ration_pills.json` — la raíz con los `overrides` por durabilidad la escribió el port; el
  datapack cambiaba de ítem, no de modelo.
- `sounds.json` — generado por el port a partir de los ficheros.
- Todo bajo `resources/data/dojutsu/` — datos del mod, no arte.

## Notas de la conversión (DJT-224)

- **`yamanaka.png` se descartó.** Está en el resource pack pero **ningún modelo lo referencia** y el mod no
  tiene clan Yamanaka: era arte huérfano ya en el original.
- **`military_rations_pills_6.png` se porta pero no se usa hoy.** El datapack da **6** píldoras
  (`PillCount:6b`) y el mod modela **5** (`PillTable.PILLS_PER_ITEM`, decisión de DJT-170). Se deja el archivo
  para que el sexto modelo esté ahí si alguna vez se corrige la cuenta.
- **Ocho sonidos no los invoca el datapack** (`isshiki.reappear`, `susanoo.activate_1`, `chidori`, `rasengan`,
  `rasenshuriken`, `frog_call`, `white_rage_technique`, `ear_ringing`): arte producido y nunca cableado. Se
  registran igual, ver `ModSounds`.
