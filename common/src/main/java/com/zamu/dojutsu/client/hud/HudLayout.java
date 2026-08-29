package com.zamu.dojutsu.client.hud;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import dev.architectury.platform.Platform;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;

/**
 * Colocacion del HUD elegida por el jugador (F11/DJT-233). <b>Solo cliente y solo cliente:</b> donde cada uno
 * quiere su barra de chakra es una preferencia de interfaz, no estado de juego, asi que no cruza la red ni se
 * guarda en la partida (regla 13). Vive en {@code config/dojutsu-hud.properties}, junto al config del mod.
 *
 * <p><b>Se guarda en fraccion de pantalla, no en pixeles.</b> Ver {@link HudPlacement}: es lo que hace que la
 * colocacion sobreviva a cambiar la escala de GUI o la resolucion, que es el fallo clasico de los HUD
 * movibles.
 *
 * <p>Lectura tolerante: una clave ausente, mal escrita o de otra version usa el default del elemento en vez de
 * romper la carga. Es un fichero que la gente va a editar a mano.
 */
public final class HudLayout {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Nombre del archivo en la carpeta de config. */
    public static final String FILE_NAME = "dojutsu-hud.properties";

    private static final Map<HudElement, HudPlacement> PLACEMENTS = new EnumMap<>(HudElement.class);

    private HudLayout() {
    }

    /** Colocacion actual de un elemento (la de fabrica si aun no se cargo nada). */
    public static HudPlacement get(HudElement element) {
        HudPlacement placement = PLACEMENTS.get(element);
        return placement != null ? placement : element.defaultPlacement();
    }

    /** Cambia la colocacion de un elemento en memoria. No guarda: eso lo hace {@link #save()} al cerrar el editor. */
    public static void set(HudElement element, HudPlacement placement) {
        PLACEMENTS.put(element, placement);
    }

    /** Devuelve todos los elementos a su sitio de fabrica. */
    public static void reset() {
        PLACEMENTS.clear();
    }

    /** Devuelve un elemento a su sitio de fabrica. */
    public static void reset(HudElement element) {
        PLACEMENTS.remove(element);
    }

    /**
     * Carga la colocacion del disco. Si el archivo no existe se queda con los defaults <b>sin escribirlo</b>:
     * un jugador que nunca abra el editor no tiene por que encontrarse un archivo nuevo en su carpeta.
     */
    public static void load() {
        Path file = Platform.getConfigFolder().resolve(FILE_NAME);
        if (!Files.exists(file)) {
            PLACEMENTS.clear();
            return;
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        } catch (IOException e) {
            LOGGER.warn("[{}] No se pudo leer {}: se usa la colocacion por defecto del HUD.",
                    DojutsuMod.MOD_NAME, FILE_NAME, e);
            PLACEMENTS.clear();
            return;
        }
        PLACEMENTS.clear();
        PLACEMENTS.putAll(fromProperties(properties));
    }

    /** Escribe la colocacion actual al disco. La llama el editor al cerrarse. */
    public static void save() {
        Path file = Platform.getConfigFolder().resolve(FILE_NAME);
        Properties properties = toProperties();
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                properties.store(out, "Colocacion del HUD de Dojutsu. Los offsets van en fraccion de pantalla.");
            }
        } catch (IOException e) {
            LOGGER.warn("[{}] No se pudo guardar {}.", DojutsuMod.MOD_NAME, FILE_NAME, e);
        }
    }

    /**
     * Lectura pura y tolerante de las propiedades (testeable sin disco). Cualquier valor invalido cae al
     * default de su elemento, y un elemento entero ausente se omite (usara su default al consultarse).
     */
    static Map<HudElement, HudPlacement> fromProperties(Properties properties) {
        Map<HudElement, HudPlacement> out = new EnumMap<>(HudElement.class);
        for (HudElement element : HudElement.all()) {
            HudPlacement fallback = element.defaultPlacement();
            String prefix = element.key() + ".";
            HudAnchor anchor = HudAnchor.byName(properties.getProperty(prefix + "anchor"), fallback.anchor());
            float x = parseFloat(properties.getProperty(prefix + "x"), fallback.offsetX());
            float y = parseFloat(properties.getProperty(prefix + "y"), fallback.offsetY());
            boolean visible = parseBoolean(properties.getProperty(prefix + "visible"), fallback.visible());
            out.put(element, new HudPlacement(anchor, x, y, visible));
        }
        return out;
    }

    /** Escritura pura de las propiedades (testeable sin disco). */
    static Properties toProperties() {
        Properties properties = new Properties();
        for (HudElement element : HudElement.all()) {
            HudPlacement placement = get(element);
            String prefix = element.key() + ".";
            properties.setProperty(prefix + "anchor", placement.anchor().name());
            properties.setProperty(prefix + "x", Float.toString(placement.offsetX()));
            properties.setProperty(prefix + "y", Float.toString(placement.offsetY()));
            properties.setProperty(prefix + "visible", Boolean.toString(placement.visible()));
        }
        return properties;
    }

    private static float parseFloat(String raw, float fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean parseBoolean(String raw, boolean fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String value = raw.trim();
        if (value.equalsIgnoreCase("true") || value.equals("1")) {
            return true;
        }
        if (value.equalsIgnoreCase("false") || value.equals("0")) {
            return false;
        }
        return fallback;
    }
}
