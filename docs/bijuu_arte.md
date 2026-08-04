# El cuerpo de las Bestias con Cola (F10/DJT-228)

## El ravager no era una improvisación del mod

Hasta este ticket, la ficha decía que las nueve bestias «toman prestada la malla del ravager» a la espera de
modelos propios, y `known_issues.md` contaba 24 assets pendientes. Lo que hace el datapack es **exactamente
eso**:

```
summon ravager ~ ~1 ~ {Tags:["d.tailed_beast"],PersistenceRequired:1b,...}
#effect give @e[type=ravager,tag=d.tailed_beast,...] invisibility 9999 255 true
```

Un ravager de verdad, con 1024 de vida y 30 de armadura — y **la línea que lo haría invisible está comentada**.
Encima le pone un `armor_stand` invisible que sostiene un `knowledge_book` con `CustomModelData` 14600..14609.

## Y de los diez modelos que declara, sólo existen dos

El resource pack mapea los diez valores a `tailed_beast/1_tail` … `10_tails`, pero **sólo trae dos ficheros**:

| CustomModelData | Modelo declarado | ¿Existe? |
|---|---|---|
| 14600–14606 | `1_tail` … `7_tails` | **no** |
| 14607 | `8_tails` | sí — 965 cubos |
| 14608 | `9_tails` | sí — 1243 cubos |
| 14609 | `10_tails` | **no** |

Es decir: en el juego original, **siete bestias y el Diez Colas se ven como un ravager con el cubo morado y
negro de "modelo ausente" flotando encima**. Sólo Gyūki y Kurama tienen cuerpo.

Portar las siete que faltan no sería portar: sería **autoría de arte nuevo**, que es otro trabajo y otra
decisión. Lo único que el mod no reproduce a propósito es el cubo de modelo ausente — copiar eso sería portar
un fallo.

## Cómo se dibujan las dos que sí existen

El cuerpo es un **modelo de ítem** de mil cubos, no una malla de entidad, así que se dibuja con el pipeline de
ítems: un ítem-vehículo `beast_body` con `CustomModelData` = número de colas, renderizado en
`TailedBeastRenderer`. Es la misma maniobra del datapack —que cuelga sus modelos de un `knowledge_book`— con la
diferencia de que aquí el vehículo es propio y se llama por lo que hace.

La alternativa era registrar los modelos como *sueltos*, pero **Architectury 13.0.8 no lo cubre**: habría
costado un puerto de plataforma por loader para ahorrarse una entrada de registro.

## Las texturas están a 1024, y es una decisión medida

Vienen a **4096×4096** (Gyūki) y **2048×2048** (Kurama), mientras sus modelos declaran un espacio UV de **512**.
Metidas en el atlas de bloques compartido:

| Resolución | Atlas resultante | VRAM del atlas |
|---|---|---|
| Original (4096 / 2048) | 8192×4096 | **128 MB** + mipmaps |
| **1024 (elegida)** | **2048×2048** | **16 MB** |

Ambas cifras están **medidas arrancando el cliente**, no estimadas. A 1024 sigue habiendo el doble de píxeles
que el espacio UV que el modelo direcciona, así que se ve nítido; el detalle que se pierde sólo se aprecia
pegado a la bestia. El coste del original lo paga toda máquina que cargue el mod, tenga bestias delante o no.

Los originales sin reescalar siguen en el resource pack de referencia si alguna vez se quiere revertir.

## Dónde viven las texturas, y por qué ahí

En `assets/dojutsu/textures/**item**/tailed_beast/`. No es cosmético: el atlas de bloques de vanilla sólo
escanea los directorios `block/` e `item/` —de **todos** los namespaces—, así que una textura fuera de ellos no
se cose al atlas y el modelo sale sin texturar. Es el fallo que dio el primer arranque de este ticket.

## Lo que no ve un test

Que los modelos y las texturas **resuelvan** lo verifica el arranque del cliente (0 sin resolver). Que la
bestia se vea **bien** no lo ve nada: quedan para partida la escala del cuerpo frente a su caja de colisión, si
mira hacia donde camina, y si Gyūki y Kurama quedan a la altura correcta sobre el suelo.
