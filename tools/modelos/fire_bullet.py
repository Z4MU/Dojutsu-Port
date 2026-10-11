"""Genera el modelo de cubos y la textura 16x16 de la Bala de fuego (DJT-240, piloto de arte).

Fuente: imagenes referencias jutsus/Fuego/Bala de fuego.png
  - Vistas: bola casi esferica al frente, estela que se afina hacia atras, chispas sueltas.
  - Paleta: nucleo brillante, fuego claro, fuego medio, fuego oscuro, contorno caliente, chispas.
  - Escala: 0.8 ~ 1 bloque de diametro.

El modelo mira hacia -Z (la "nariz" de la bala va al frente, la estela hacia +Z). El ThrownItemRenderer
lo orienta a camara, asi que lo que importa es que se lea como bola con cola desde cualquier lado.

Unidades: 16 = 1 bloque. Limites vanilla: coordenadas en [-16, 32]; rotacion un eje, +-22.5/45.
"""
import json
import sys
from PIL import Image

OUT_MODEL, OUT_TEX = sys.argv[1], sys.argv[2]

# ---------------------------------------------------------------- textura 16x16
# Seis franjas horizontales de la paleta de la referencia, con ruido de pixel para que no sea plano.
PALETTE = {
    # El nucleo era crema (255, 250, 210) y en partida se leia BLANCO GRISACEO, no fuego: con luz de dia un
    # color tan desaturado pierde el tono. Amarillo saturado, como el centro de la ficha de referencia.
    "core":   (255, 228, 92),    # NUCLEO BRILLANTE
    "light":  (255, 194, 40),    # FUEGO CLARO
    "mid":    (255, 146, 20),    # FUEGO MEDIO
    "dark":   (236, 84, 12),     # FUEGO OSCURO
    "rim":    (222, 40, 6),      # CONTORNO CALIENTE
    "spark":  (255, 170, 40),    # CHISPAS
}
# Cada material ocupa una banda de la textura; las caras de los cubos apuntan a su banda.
BANDS = {"core": (0, 4), "light": (4, 8), "mid": (8, 12), "dark": (12, 14), "rim": (14, 16)}

img = Image.new("RGBA", (16, 16))
# patron determinista para el "ruido" de pixel (sin aleatoriedad: el fichero sale igual cada vez)
JITTER = [0, 8, -6, 5, -4, 7, -7, 3, 6, -3, 4, -8, 2, -5, 9, -2]
for name, (y0, y1) in BANDS.items():
    r, g, b = PALETTE[name]
    for y in range(y0, y1):
        for x in range(16):
            j = JITTER[(x * 7 + y * 3) % 16]
            img.putpixel((x, y), (max(0, min(255, r + j)), max(0, min(255, g + j)),
                                  max(0, min(255, b + j // 2)), 255))
img.save(OUT_TEX)


# ---------------------------------------------------------------- cubos
def uv(band):
    """UV de una banda entera de la paleta (u de 0 a 16, v la franja del material)."""
    y0, y1 = BANDS[band]
    return [0, y0, 16, y1]


def cube(frm, to, band, name):
    faces = {f: {"uv": uv(band), "texture": "#fire"} for f in
             ("north", "south", "east", "west", "up", "down")}
    # shade=false: Minecraft no oscurece las caras segun su orientacion. Sin esto la bola sale apagada,
    # que es justo lo contrario de un nucleo de fuego.
    return {"name": name, "from": frm, "to": to, "shade": False, "faces": faces}


C = 8.0  # centro del modelo en x/y

def disc(d, z0, z1, band, name):
    """Rebanada redonda de diametro d: tres cajas cruzadas (ancha, cuadrada, alta) recortan las esquinas."""
    h = d / 2.0
    q = d * 0.375
    t = d / 4.0
    return [
        cube([C - h, C - t, z0], [C + h, C + t, z1], band, name + "_ancha"),
        cube([C - q, C - q, z0], [C + q, C + q, z1], band, name + "_cuadrada"),
        cube([C - t, C - h, z0], [C + t, C + h, z1], band, name + "_alta"),
    ]


elements = []
# Bola, de la nariz (z bajo) hacia atras. El frente es el nucleo brillante; el cuerpo, fuego claro.
elements += [cube([C - 1.5, C - 1.5, 1.0], [C + 1.5, C + 1.5, 2.0], "core", "nariz")]
elements += disc(6.0, 2.0, 3.0, "core", "bola_frente")
elements += disc(8.0, 3.0, 8.0, "light", "bola_cuerpo")
elements += disc(6.0, 8.0, 10.0, "mid", "bola_trasera")
# Contorno caliente: el aro donde la bola empieza a deshacerse en estela.
elements += disc(5.0, 10.0, 11.0, "rim", "contorno")

# Estela: se afina hacia atras (+Z) y se oscurece; es lo que hace leer "bala que viaja".
elements += [
    cube([C - 2.5, C - 2.5, 10.0], [C + 2.5, C + 2.5, 14.0], "mid", "estela_1"),
    cube([C - 1.75, C - 1.75, 14.0], [C + 1.75, C + 1.75, 18.0], "dark", "estela_2"),
    cube([C - 1.0, C - 1.0, 18.0], [C + 1.0, C + 1.0, 22.0], "rim", "estela_3"),

    # Chispas sueltas alrededor de la estela, como los pixeles que se desprenden en la referencia.
    cube([C + 3.0, C + 1.5, 13.0], [C + 4.0, C + 2.5, 14.0], "light", "chispa_1"),
    cube([C - 4.0, C - 2.5, 15.0], [C - 3.0, C - 1.5, 16.0], "light", "chispa_2"),
    cube([C + 1.5, C - 4.0, 17.0], [C + 2.5, C - 3.0, 18.0], "mid", "chispa_3"),
    cube([C - 2.0, C + 3.0, 19.0], [C - 1.0, C + 4.0, 20.0], "light", "chispa_4"),
    cube([C + 0.5, C + 0.5, 23.0], [C + 1.5, C + 1.5, 24.0], "dark", "chispa_5"),
]

# ORIENTACION. El ThrownItemRenderer de vanilla gira el item hacia la camara y lo mira desde +Z (igual que la
# GUI, que por eso enseña la cara "south"). Construido con la nariz en z bajo, en vuelo se veria la PUNTA DE LA
# ESTELA delante de la bola. Se refleja en Z: nariz al frente de la camara, estela detras.
for e in elements:
    z0, z1 = e["from"][2], e["to"][2]
    e["from"][2], e["to"][2] = 24.0 - z1, 24.0 - z0
    e["faces"]["north"], e["faces"]["south"] = e["faces"]["south"], e["faces"]["north"]

# Comprobacion de los limites vanilla: un modelo fuera de [-16, 32] no carga.
for e in elements:
    for v in e["from"] + e["to"]:
        assert -16 <= v <= 32, (e["name"], v)

model = {
    "credit": "Dojutsu Port - piloto de arte DJT-240 (base para retocar en Blockbench)",
    "textures": {"fire": "dojutsu:item/jutsu_3d/fire_bullet", "particle": "dojutsu:item/jutsu_3d/fire_bullet"},
    "elements": elements,
    # ground es lo que usa el ThrownItemRenderer para un proyectil en vuelo. La escala sale de la referencia:
    # 0.8 ~ 1 bloque de diametro. La bola mide 8 de ancho (media vuelta de bloque) y la estela alarga hasta 22,
    # asi que a escala 1.0 la bola ronda los 0.5 bloques; 1.6 la deja en ~0.8.
    "display": {
        "ground":            {"rotation": [0, 0, 0],   "translation": [0, 0, 0],   "scale": [1.6, 1.6, 1.6]},
        "fixed":             {"rotation": [0, 90, 0],  "translation": [0, 0, 0],   "scale": [1.0, 1.0, 1.0]},
        "gui":               {"rotation": [30, 45, 0], "translation": [0, 0, 0],   "scale": [0.6, 0.6, 0.6]},
        "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
        "firstperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    },
}
with open(OUT_MODEL, "w", encoding="utf-8") as f:
    json.dump(model, f, indent=2)

print("cubos:", len(elements))
