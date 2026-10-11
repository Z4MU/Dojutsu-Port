"""Vista previa de un modelo de item de Minecraft (cubos), sin abrir el juego ni Blockbench.

Dibuja cuatro vistas -frontal, lateral, superior y 3/4- con el algoritmo del pintor: cada cara de cada cubo
se proyecta, se ordena por profundidad y se rellena con el color medio de su UV en la textura, sombreado
segun la orientacion de la cara. No es el render exacto del juego, pero basta para juzgar silueta y color.
"""
import json
import math
import sys
from PIL import Image, ImageDraw, ImageFont

MODEL, TEX, OUT = sys.argv[1], sys.argv[2], sys.argv[3]
model = json.load(open(MODEL, encoding="utf-8"))
tex = Image.open(TEX).convert("RGBA")

SHADE = {"up": 1.0, "north": 0.86, "south": 0.86, "east": 0.72, "west": 0.72, "down": 0.55}


def face_color(face, name, shaded=True):
    u0, v0, u1, v1 = face["uv"]
    px = [tex.getpixel((x, y)) for x in range(int(u0), max(int(u0) + 1, int(u1)))
          for y in range(int(v0), max(int(v0) + 1, int(v1)))]
    r = sum(p[0] for p in px) / len(px)
    g = sum(p[1] for p in px) / len(px)
    b = sum(p[2] for p in px) / len(px)
    s = SHADE[name] if shaded else (1.0 if name in ("up", "north", "south") else 0.94)
    return (int(r * s), int(g * s), int(b * s))


def faces_of(e):
    (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
    return {
        "north": [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)],
        "south": [(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)],
        "west":  [(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)],
        "east":  [(x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0)],
        "down":  [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)],
        "up":    [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
    }


def rot(p, yaw, pitch):
    x, y, z = p[0] - 8, p[1] - 8, p[2] - 12
    cy, sy = math.cos(yaw), math.sin(yaw)
    x, z = x * cy - z * sy, x * sy + z * cy
    cp, sp = math.cos(pitch), math.sin(pitch)
    y, z = y * cp - z * sp, y * sp + z * cp
    return x, y, z


def render(yaw, pitch, size=300, scale=9.0):
    img = Image.new("RGB", (size, size), (30, 30, 34))
    d = ImageDraw.Draw(img)
    # rejilla de fondo de 1 pixel de modelo, como la de Blockbench
    for i in range(0, size, int(scale)):
        d.line([(i, 0), (i, size)], fill=(40, 40, 46))
        d.line([(0, i), (size, i)], fill=(40, 40, 46))
    polys = []
    for e in model["elements"]:
        for name, quad in faces_of(e).items():
            pts = [rot(p, yaw, pitch) for p in quad]
            depth = sum(p[2] for p in pts) / 4
            proj = [(size / 2 + p[0] * scale, size / 2 - p[1] * scale) for p in pts]
            polys.append((depth, proj, face_color(e["faces"][name], name, e.get("shade", True))))
    for _, proj, col in sorted(polys, key=lambda t: t[0]):
        d.polygon(proj, fill=col, outline=(max(0, col[0] - 40), max(0, col[1] - 40), max(0, col[2] - 40)))
    return img


# Con NinjaProjectileRenderer (DJT-240) el modelo se orienta segun el vuelo, nariz hacia +Z. El visor esta en
# +Z, asi que la vista frontal es lo que ve el OBJETIVO, y girando 180 grados, lo que ve quien lanza.
views = [
    ("LO QUE VE EL OBJETIVO", 0.0, 0.0),
    ("LO QUE VE QUIEN LANZA", math.pi, math.radians(8)),
    ("LATERAL", math.pi / 2, 0.0),
    ("PERSPECTIVA 3/4", math.radians(55), math.radians(25)),
]
W, H, PAD = 300, 300, 28
sheet = Image.new("RGB", (W * 4, H + PAD), (22, 22, 26))
draw = ImageDraw.Draw(sheet)
try:
    font = ImageFont.truetype("arial.ttf", 15)
except OSError:
    font = ImageFont.load_default()
for i, (label, yaw, pitch) in enumerate(views):
    sheet.paste(render(yaw, pitch), (i * W, PAD))
    draw.text((i * W + 10, 6), label, fill=(255, 190, 80), font=font)
sheet.save(OUT)
print("vista previa:", OUT)
