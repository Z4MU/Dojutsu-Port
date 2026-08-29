package com.zamu.dojutsu.dojutsu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.EyeState;
import com.zamu.dojutsu.domain.eye.EyeFamily;
import com.zamu.dojutsu.dojutsu.EyeMenuService.EyeMenuView;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests de la lógica del menú ocular (DJT-117): construcción de la vista desde `EyeState`+`EyeRegistry` y
 * validación server-side de la selección (una habilidad ajena al ojo activo se rechaza sin efecto). Sin
 * Minecraft; puebla el `EyeRegistry` con un ojo de prueba.
 */
class EyeMenuServiceTest {

    private static final List<String> SHARINGAN_ABILITIES = List.of("genjutsu_stun", "copy_jutsu");

    @BeforeEach
    void loadRegistry() {
        EyeDefinition sharingan = new EyeDefinition("sharingan", EyeFamily.SHARINGAN, true, 20, 20,
                Optional.of(78000), Optional.of(78000), SHARINGAN_ABILITIES, List.of(), List.of(), false, 0, Optional.empty(), Optional.empty());
        EyeRegistry.load(Map.of("sharingan", sharingan));
    }

    private static PlayerNinjaData withActiveEye(String eyeId) {
        PlayerNinjaData data = new PlayerNinjaData();
        data.setEye(EyeState.DEFAULT.withActiveEye(eyeId));
        return data;
    }

    @Test
    void viewSinOjoActivoEstaVacia() {
        EyeMenuView view = EyeMenuService.viewFor(new PlayerNinjaData());
        assertEquals("", view.activeEye());
        assertTrue(view.abilities().isEmpty());
        assertEquals("", view.selectedAbility());
    }

    @Test
    void viewConOjoActivoListaSusHabilidades() {
        EyeMenuView view = EyeMenuService.viewFor(withActiveEye("sharingan"));
        assertEquals("sharingan", view.activeEye());
        assertEquals(SHARINGAN_ABILITIES, view.abilities());
        assertEquals("", view.selectedAbility());
    }

    @Test
    void seleccionarUnaHabilidadDelOjoLaGuarda() {
        PlayerNinjaData data = withActiveEye("sharingan");
        assertTrue(EyeMenuService.select(data, "copy_jutsu"));
        assertEquals("copy_jutsu", data.eye().selectedAbility().orElseThrow());
        // y la vista lo refleja
        assertEquals("copy_jutsu", EyeMenuService.viewFor(data).selectedAbility());
    }

    @Test
    void seleccionarUnaHabilidadAjenaAlOjoSeRechaza() {
        PlayerNinjaData data = withActiveEye("sharingan");
        assertFalse(EyeMenuService.select(data, "amaterasu")); // no está en las habilidades del sharingan
        assertTrue(data.eye().selectedAbility().isEmpty());
    }

    @Test
    void seleccionarSinOjoActivoSeRechaza() {
        PlayerNinjaData data = new PlayerNinjaData();
        assertFalse(EyeMenuService.select(data, "copy_jutsu"));
    }

    @Test
    void reSeleccionarLaMismaEsNoOp() {
        PlayerNinjaData data = withActiveEye("sharingan");
        assertTrue(EyeMenuService.select(data, "copy_jutsu"));
        assertFalse(EyeMenuService.select(data, "copy_jutsu")); // ya seleccionada: sin cambio
    }
}
