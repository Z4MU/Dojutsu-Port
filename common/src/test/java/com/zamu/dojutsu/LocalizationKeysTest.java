package com.zamu.dojutsu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Toda clave de traduccion literal que usa el codigo esta declarada (F10/DJT-229).
 *
 * <h2>Por que existe</h2>
 * Una clave sin entrada <b>no rompe nada</b>: el juego pinta el identificador crudo en pantalla y sigue. Por eso
 * sobrevivio hasta la revision previa a la v1 un {@code entity.dojutsu.shinobi_villager} que dejaba al
 * comerciante llamandose literalmente asi, en dorado, encima de la cabeza. Ni el build ni los 988 tests ni el
 * arranque de los dos loaders dijeron una palabra.
 *
 * <p>Es la clase de fallo que solo ve un jugador mirando — salvo que se compruebe aqui.
 *
 * <h2>Que NO cubre</h2>
 * Solo las claves <b>literales completas</b>: {@code translatable("x.y")}. Las que el codigo compone
 * ({@code translatable("item.dojutsu." + id)}) no se pueden resolver sin ejecutar el mod, y son la mayoria de
 * los catalogos. Que esas cuadren lo dice el juego, no un test.
 */
class LocalizationKeysTest {

    /**
     * Una cadena literal seguida <b>inmediatamente</b> de {@code ,} o {@code )} — es decir, el argumento
     * completo. Asi se descartan las concatenaciones, donde tras la comilla viene un {@code +}.
     */
    private static final Pattern LITERAL_KEY =
            Pattern.compile("translatable\\(\\s*\"([^\"]*)\"\\s*[,)]");

    private static final List<String> SOURCE_ROOTS =
            List.of("common/src/main/java", "fabric/src/main/java", "neoforge/src/main/java");

    /** Habia 120 claves literales al escribir el test; el suelo solo tiene que descartar "no encontro nada". */
    private static final int MIN_EXPECTED_KEYS = 80;

    @Test
    void todaClaveLiteralDelCodigoEstaDeclarada() {
        Path root = projectRoot();
        Assumptions.assumeTrue(root != null, "no se localizo la raiz del proyecto");

        Set<String> declared = declaredKeys();
        Set<String> used = new TreeSet<>();
        for (String source : SOURCE_ROOTS) {
            Path dir = root.resolve(source);
            if (Files.isDirectory(dir)) {
                used.addAll(keysUsedIn(dir));
            }
        }
        // Suelo de cordura: un escaner que no encuentra ficheros aprueba en vacio, que es peor que fallar.
        assertTrue(used.size() > MIN_EXPECTED_KEYS,
                "el escaner solo vio " + used.size() + " claves; la ruta a las fuentes esta mal");

        Set<String> missing = new TreeSet<>(used);
        missing.removeAll(declared);
        assertTrue(missing.isEmpty(), "claves usadas por el codigo y sin traducir: " + missing);
    }

    @Test
    void elFicheroDeIdiomaEstaBienFormado() {
        Set<String> declared = declaredKeys();
        assertFalse(declared.isEmpty());
        assertTrue(declared.contains("entity.dojutsu.shinobi_villager"),
                "la que falto hasta DJT-229; si desaparece, el comerciante vuelve a mostrar la clave cruda");
    }

    @Test
    void noHayClavesDuplicadas() {
        // Gson se queda con la ultima y no avisa: dos entradas iguales dejarian una traduccion muerta.
        long lines;
        try (InputStream in = open()) {
            lines = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .filter(line -> line.trim().startsWith("\"")).count();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertEquals(lines, declaredKeys().size(), "hay claves repetidas en en_us.json");
    }

    private static Set<String> keysUsedIn(Path dir) {
        Set<String> keys = new TreeSet<>();
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher matcher = LITERAL_KEY.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (matcher.find()) {
                    String key = matcher.group(1);
                    if (!key.isBlank()) {
                        keys.add(key);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return keys;
    }

    private static Set<String> declaredKeys() {
        try (InputStream in = open()) {
            JsonObject json = JsonParser
                    .parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            return new TreeSet<>(json.keySet());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static InputStream open() {
        InputStream in = LocalizationKeysTest.class.getResourceAsStream("/assets/dojutsu/lang/en_us.json");
        if (in == null) {
            throw new IllegalStateException("en_us.json no esta en el classpath de test");
        }
        return in;
    }

    /** Sube desde el directorio de trabajo hasta el que tiene {@code settings.gradle}. */
    private static Path projectRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            if (Files.exists(dir.resolve("settings.gradle")) || Files.exists(dir.resolve("settings.gradle.kts"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        return null;
    }
}
