package com.zamu.dojutsu.clans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.world.scores.Scoreboard;
import org.junit.jupiter.api.Test;

/**
 * Tests del team/nametag por clan (DJT-062): los datos (14 colores de {@code create_teams}), el sufijo y los
 * codecs.
 *
 * <p><b>La manipulacion de teams se valida en runtime, no aqui:</b> el constructor de {@code PlayerTeam}
 * arrastra {@code HoverEvent} → {@code ItemStack} → registries ("Not bootstrapped"), y la convencion del
 * proyecto es testear sin bootstrap de Minecraft. Igual que la GUI/HUD de F2, la logica de crear/mover/quitar
 * teams (glue fino sobre la API vanilla) se verifica in-game: join → team, reset → fuera, cambio de clan →
 * mueve, team del servidor → intocable.
 */
class ClanTeamServiceTest {

    private static final Gson GSON = new Gson();

    private static ClanDefinition clan(String id) {
        try (InputStream in = ClanTeamServiceTest.class.getResourceAsStream("/data/dojutsu/clans/" + id + ".json")) {
            JsonElement json = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonElement.class);
            return ClanDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        } catch (Exception e) {
            throw new AssertionError("no se pudo cargar el clan " + id, e);
        }
    }

    @Test
    void los14ClanesTienenSuColorDeCreateTeams() {
        assertEquals(ChatFormatting.DARK_RED, clan("uchiha").teamColor().orElseThrow());
        assertEquals(ChatFormatting.GRAY, clan("hyuga").teamColor().orElseThrow());
        assertEquals(ChatFormatting.WHITE, clan("otsutsuki").teamColor().orElseThrow());
        assertEquals(ChatFormatting.GOLD, clan("uzumaki").teamColor().orElseThrow());
        assertEquals(ChatFormatting.RED, clan("chinoike").teamColor().orElseThrow());
        assertEquals(ChatFormatting.BLACK, clan("nara").teamColor().orElseThrow());
        assertEquals(ChatFormatting.RED, clan("kaguya").teamColor().orElseThrow());
        assertEquals(ChatFormatting.GRAY, clan("hatake").teamColor().orElseThrow());
        assertEquals(ChatFormatting.GREEN, clan("senju").teamColor().orElseThrow());
        assertEquals(ChatFormatting.DARK_PURPLE, clan("aburame").teamColor().orElseThrow());
        assertEquals(ChatFormatting.DARK_RED, clan("akimichi").teamColor().orElseThrow());
        assertEquals(ChatFormatting.YELLOW, clan("namikaze").teamColor().orElseThrow());
        assertEquals(ChatFormatting.GOLD, clan("sarutobi").teamColor().orElseThrow());
        assertEquals(ChatFormatting.DARK_GREEN, clan("lee").teamColor().orElseThrow());
    }

    @Test
    void elSufijoSeDerivaDelIdCapitalizado() {
        // create_teams: suffix " <Clan>"; los 14 coinciden con el id capitalizado.
        assertEquals("Uchiha", ClanTeamService.capitalize("uchiha"));
        assertEquals("Lee", ClanTeamService.capitalize("lee"));
        assertEquals("", ClanTeamService.capitalize(""));
    }

    @Test
    void elNombreDelTeamVaNamespaceado() {
        // "dojutsu_<clan>": no puede colisionar con un team "Uchiha" creado por el servidor.
        assertTrue(ClanTeamService.TEAM_PREFIX.startsWith("dojutsu"));
    }

    @Test
    void sinClanYSinTeamEsInofensivo() {
        // El unico camino de update() que no construye PlayerTeam (testeable sin bootstrap).
        Scoreboard scoreboard = new Scoreboard();
        assertTrue(ClanTeamService.update(scoreboard, "Zamu", Optional.empty()));
        assertNull(scoreboard.getPlayersTeam("Zamu"));
    }

    @Test
    void unColorInvalidoFallaElParseo() {
        JsonElement bad = GSON.fromJson(
                "{\"id\":\"x\",\"teamColor\":\"fucsia\",\"maxChakra\":100}", JsonElement.class);
        assertTrue(ClanDefinition.CODEC.parse(JsonOps.INSTANCE, bad).error().isPresent());

        JsonElement notColor = GSON.fromJson(
                "{\"id\":\"x\",\"teamColor\":\"bold\",\"maxChakra\":100}", JsonElement.class); // formato, no color
        assertTrue(ClanDefinition.CODEC.parse(JsonOps.INSTANCE, notColor).error().isPresent());
    }
}
