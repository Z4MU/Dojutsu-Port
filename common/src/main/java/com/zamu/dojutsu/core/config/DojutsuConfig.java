package com.zamu.dojutsu.core.config;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import dev.architectury.platform.Platform;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.slf4j.Logger;

/**
 * Configuracion global del mod, equivalente al {@code properties} del datapack ({@code [S01]}). Valores con
 * los defaults exactos del datapack; los booleanos se expresan como {@code 1}/{@code 0} en el archivo.
 *
 * <p>{@link #fromProperties(Properties)} es puro y testeable: usa el default de cada clave ausente y, ante un
 * valor invalido, el default con aviso. {@link #load()} lee (o crea con defaults) el archivo
 * {@code config/dojutsu.properties} y publica la instancia accesible via {@link #get()}.
 *
 * <p>Nota: las claves de tuning de Modo Sabio ({@code GatherTimeLevel_*}) viven con el modulo sage (fase
 * posterior), no en este config global.
 */
public record DojutsuConfig(
        boolean allowCustomTrader,
        boolean allowShinobis,
        boolean allowMultJinchuriki,
        boolean checkHardcore,
        boolean useTraderSpawnRate,
        boolean customMangekyoObtainable,
        /** Opcion del mod (no del datapack): si esta activa, el Arbol Divino no se planta donde arrasaria
         * block entities del jugador. Default false = comportamiento fiel al original (RM-7, F9/DJT-215). */
        boolean godTreeProtectsBuilds,
        int killMobPoints,
        int killPlayerPoints,
        int killBossPoints,
        int traderSpawnRate,
        int traderSpawnChance,
        int natureEnergyCharge) {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Nombre del archivo de configuracion en la carpeta de config. */
    public static final String FILE_NAME = "dojutsu.properties";

    /** Defaults exactos de {@code properties} ({@code [S01]}). */
    public static final DojutsuConfig DEFAULTS = new DojutsuConfig(
            true, true, true, false, false, true, false,
            1, 3, 200, 1, 1, 1);

    private static DojutsuConfig instance = DEFAULTS;

    /** Config activa (los defaults hasta que {@link #load()} corra). */
    public static DojutsuConfig get() {
        return instance;
    }

    /**
     * Construye la config desde un {@link Properties}: clave ausente -&gt; default; valor invalido -&gt;
     * default con aviso. Metodo puro (no toca disco).
     */
    public static DojutsuConfig fromProperties(Properties props) {
        return new DojutsuConfig(
                readBool(props, "AllowCustomTrader", DEFAULTS.allowCustomTrader),
                readBool(props, "AllowShinobis", DEFAULTS.allowShinobis),
                readBool(props, "AllowMultJinchuriki", DEFAULTS.allowMultJinchuriki),
                readBool(props, "CheckHardcore", DEFAULTS.checkHardcore),
                readBool(props, "UseTraderSpawnRate", DEFAULTS.useTraderSpawnRate),
                readBool(props, "CustomMangekyoObtainable", DEFAULTS.customMangekyoObtainable),
                readBool(props, "GodTreeProtectsBuilds", DEFAULTS.godTreeProtectsBuilds),
                readInt(props, "KillMobPoints", DEFAULTS.killMobPoints),
                readInt(props, "KillPlayerPoints", DEFAULTS.killPlayerPoints),
                readInt(props, "KillBossPoints", DEFAULTS.killBossPoints),
                readInt(props, "TraderSpawnRate", DEFAULTS.traderSpawnRate),
                readInt(props, "TraderSpawnChance", DEFAULTS.traderSpawnChance),
                readInt(props, "NatureEnergyCharge", DEFAULTS.natureEnergyCharge));
    }

    /** Carga la config desde {@code config/dojutsu.properties} (creandola con defaults si no existe). */
    public static void load() {
        Path file = Platform.getConfigFolder().resolve(FILE_NAME);
        Properties props = new Properties();
        try {
            if (Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    props.load(in);
                }
            } else {
                Files.createDirectories(file.getParent());
                Files.writeString(file, defaultFileContent(), StandardCharsets.UTF_8);
                LOGGER.info("[{}] Config creada con defaults en {}.", DojutsuMod.MOD_NAME, file);
            }
        } catch (IOException ex) {
            LOGGER.warn("[{}] No se pudo leer/crear la config; se usan defaults.", DojutsuMod.MOD_NAME, ex);
        }
        instance = fromProperties(props);
        LOGGER.info("[{}] Config cargada.", DojutsuMod.MOD_NAME);
    }

    private static boolean readBool(Properties props, String key, boolean def) {
        String raw = props.getProperty(key);
        if (raw == null) {
            return def;
        }
        raw = raw.trim();
        if (raw.equals("1") || raw.equalsIgnoreCase("true")) {
            return true;
        }
        if (raw.equals("0") || raw.equalsIgnoreCase("false")) {
            return false;
        }
        LOGGER.warn("[{}] Valor invalido para {}: '{}'; usando default {}.", DojutsuMod.MOD_NAME, key, raw, def);
        return def;
    }

    private static int readInt(Properties props, String key, int def) {
        String raw = props.getProperty(key);
        if (raw == null) {
            return def;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            LOGGER.warn("[{}] Valor invalido para {}: '{}'; usando default {}.", DojutsuMod.MOD_NAME, key, raw, def);
            return def;
        }
    }

    private static int b(boolean value) {
        return value ? 1 : 0;
    }

    private static String defaultFileContent() {
        return """
                # Configuracion de Dojutsu (equivale a 'properties' del datapack).
                # Booleanos: 1 = si, 0 = no. Tras editar, reinicia el servidor/mundo.
                AllowCustomTrader=%d
                AllowShinobis=%d
                AllowMultJinchuriki=%d
                CheckHardcore=%d
                UseTraderSpawnRate=%d
                CustomMangekyoObtainable=%d
                GodTreeProtectsBuilds=%s
                KillMobPoints=%d
                KillPlayerPoints=%d
                KillBossPoints=%d
                TraderSpawnRate=%d
                TraderSpawnChance=%d
                NatureEnergyCharge=%d
                """.formatted(
                b(DEFAULTS.allowCustomTrader), b(DEFAULTS.allowShinobis), b(DEFAULTS.allowMultJinchuriki),
                b(DEFAULTS.checkHardcore), b(DEFAULTS.useTraderSpawnRate), b(DEFAULTS.customMangekyoObtainable),
                b(DEFAULTS.godTreeProtectsBuilds),
                DEFAULTS.killMobPoints, DEFAULTS.killPlayerPoints, DEFAULTS.killBossPoints,
                DEFAULTS.traderSpawnRate, DEFAULTS.traderSpawnChance, DEFAULTS.natureEnergyCharge);
    }
}
