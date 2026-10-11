# Herramientas de modelos de jutsu

Scripts para construir y previsualizar los modelos 3D de los jutsus **sin abrir el juego**.
Nacieron con el piloto de la Bala de fuego (DJT-240).

## preview.py — vista previa de cualquier modelo

```
py -3 tools/modelos/preview.py <modelo.json> <textura.png> <salida.png>
```

Dibuja cuatro vistas (frontal, lateral, superior y 3/4). La **frontal es lo que ves en vuelo**: el
`ThrownItemRenderer` de vanilla gira el ítem hacia la cámara y lo mira desde +Z, y la vista previa se
coloca igual. No es el render exacto del juego, pero basta para juzgar silueta y color.

Respeta `"shade": false` igual que Minecraft.

## fire_bullet.py — ejemplo de modelo generado

Construye el modelo de cubos y su textura 16×16 a partir de la paleta de la referencia
(`imagenes referencias jutsus/Fuego/Bala de fuego.png`). Sirve de plantilla para los siguientes.

## Reglas que hay que respetar (o el modelo no carga, sin avisar)

- Coordenadas dentro de **[-16, 32]**. Fuera de ahí sale el cubo morado.
- Rotación en **un solo eje**, a ±22.5° o ±45°.
- La nariz del proyectil va hacia **+Z**: es lo que mira la cámara en vuelo.
- Para fuego, rayo y todo lo que deba brillar: `"shade": false` en cada cubo.
- `custom_model_data` del modelo 3D = **3000 + el del pergamino** (`JutsuModelData.worldModelOf`).

## Conectar un modelo nuevo

1. Modelo en `assets/dojutsu/models/item/jutsu_3d/<jutsu>.json` y textura en
   `assets/dojutsu/textures/item/jutsu_3d/<jutsu>.png`.
2. Override en `assets/dojutsu/models/item/jutsu.json` con el número 3D, **en orden ascendente**.
3. En el JSON del jutsu: `"displayModel3d": true` (proyectiles) o `"vfxModel"` (el resto, DJT-239).

`JutsuModelDataTest` comprueba los puntos 1 y 2: una override 3D huérfana o un modelo que no existe
hacen fallar el build.
