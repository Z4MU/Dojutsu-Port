# Herramientas de modelos de jutsu

Scripts para construir y previsualizar los modelos 3D de los jutsus **sin abrir el juego**.
Nacieron con el piloto de la Bala de fuego (DJT-240).

## preview.py — vista previa de cualquier modelo

```
py -3 tools/modelos/preview.py <modelo.json> <textura.png> <salida.png>
```

Dibuja cuatro vistas: **lo que ve el objetivo**, **lo que ve quien lanza**, lateral y 3/4. Los modelos 3D
de jutsu se orientan según la dirección de vuelo (`NinjaProjectileRenderer`), así que el objetivo ve la
nariz y quien lanza ve la estela alejándose. No es el render exacto del juego, pero basta para juzgar
silueta y color.

Respeta `"shade": false` igual que Minecraft.

## fire_bullet.py — ejemplo de modelo generado

Construye el modelo de cubos y su textura 16×16 a partir de la paleta de la referencia
(`imagenes referencias jutsus/Fuego/Bala de fuego.png`). Sirve de plantilla para los siguientes.

## Reglas que hay que respetar (o el modelo no carga, sin avisar)

- Coordenadas dentro de **[-16, 32]**. Fuera de ahí sale el cubo morado.
- Rotación en **un solo eje**, a ±22.5° o ±45°.
- La nariz del proyectil va hacia **+Z**: el renderer la apunta en la dirección de vuelo.
- Evita colores muy desaturados en lo que deba brillar: con luz de día, un crema casi blanco se lee como
  **gris** en partida. Pasó con el primer núcleo de la Bala de fuego.
- Para fuego, rayo y todo lo que deba brillar: `"shade": false` en cada cubo.
- `custom_model_data` del modelo 3D = **3000 + el del pergamino** (`JutsuModelData.worldModelOf`).
- Los cubos de la bola se llaman **`bola_*`**. De ellos salen el pivote y la hitbox (abajo).
- **Pivote:** Minecraft centra el modelo en (8, 8, 8), pero la bola va delante de la estela. La traslación Z de
  `display.ground` debe llevar el centro de la bola a la posición de la entidad:
  `translation.z = -(centro_z_bola - 8) × escala`. Sin ella, la Bala de fuego volaba un bloque por delante de
  su hitbox (DJT-241). `fire_bullet.py` la calcula sola.

## Conectar un modelo nuevo

1. Modelo en `assets/dojutsu/models/item/jutsu_3d/<jutsu>.json` y textura en
   `assets/dojutsu/textures/item/jutsu_3d/<jutsu>.png`.
2. Override en `assets/dojutsu/models/item/jutsu.json` con el número 3D, **en orden ascendente**.
3. En el JSON del jutsu: `"displayModel3d": true` (proyectiles) o `"vfxModel"` (el resto, DJT-239).
4. Si es proyectil, también `"hitboxSize"` (el diámetro de la bola en bloques: ancho de `bola_*` × escala / 16,
   que `fire_bullet.py` imprime) y `"trail"` (la estela; hoy existe `"fire"`).

`JutsuModelDataTest` comprueba los puntos 1 y 2: una override 3D huérfana o un modelo que no existe
hacen fallar el build. `Jutsu3dFitTest` comprueba el 4 y el pivote: si la hitbox no mide lo que la bola, si la
bola no cae en la entidad o si falta la estela, el build falla.
