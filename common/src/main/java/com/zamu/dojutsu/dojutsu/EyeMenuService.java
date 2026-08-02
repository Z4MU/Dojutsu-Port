package com.zamu.dojutsu.dojutsu;

import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.EyeState;
import java.util.List;
import java.util.Optional;

/**
 * Logica del menu de habilidades oculares (DJT-117), pura y testeable: construye la <b>vista</b> del menu
 * (ojo activo + sus habilidades + la seleccion) desde el {@code EyeState} y el {@link EyeRegistry}, y valida y
 * aplica la seleccion. La red (payloads) y la Screen viven aparte; esto no conoce Minecraft-cliente ni codecs.
 *
 * <p><b>Autoridad del servidor</b> ({@code [ADD/07]}): la seleccion se valida contra el ojo activo <b>real</b>
 * (sus {@code abilities} en datos), no contra lo que diga el cliente — nadie puede seleccionar una habilidad
 * que su ojo no ofrece.
 */
public final class EyeMenuService {

    /**
     * Vista del menu para la Screen (solo presentacion). {@code activeEye} vacio = sin ojo activo.
     *
     * @param activeEye       id del ojo activo, o cadena vacia
     * @param abilities       habilidades del ojo activo, en orden
     * @param selectedAbility habilidad seleccionada, o cadena vacia
     */
    public record EyeMenuView(String activeEye, List<String> abilities, String selectedAbility,
            String eternalStatus) {

        public EyeMenuView {
            abilities = List.copyOf(abilities);
        }
    }

    private EyeMenuService() {
    }

    /** Construye la vista del menu para el estado actual del jugador. */
    public static EyeMenuView viewFor(PlayerNinjaData data) {
        EyeState eye = data.eye();
        Optional<String> activeId = eye.activeEye();
        if (activeId.isEmpty()) {
            return new EyeMenuView("", List.of(), "", EternalAwakeningService.AwakenOutcome.NO_ETERNAL_EYE.name());
        }
        List<String> abilities = abilitiesOf(activeId.get());
        return new EyeMenuView(activeId.get(), abilities, eye.selectedAbility().orElse(""),
                EternalAwakeningService.statusFor(data).name());
    }

    /**
     * Valida y aplica la seleccion de habilidad: debe existir un ojo activo cuya lista de habilidades contenga
     * {@code abilityId}. Devuelve {@code true} si cambio la seleccion (el llamador sincroniza).
     */
    public static boolean select(PlayerNinjaData data, String abilityId) {
        EyeState eye = data.eye();
        Optional<String> activeId = eye.activeEye();
        if (activeId.isEmpty() || !abilitiesOf(activeId.get()).contains(abilityId)) {
            return false; // sin ojo activo o habilidad ajena al ojo: se rechaza sin efecto
        }
        if (eye.selectedAbility().filter(abilityId::equals).isPresent()) {
            return false; // ya seleccionada: sin cambio
        }
        data.setEye(eye.withSelectedAbility(abilityId));
        return true;
    }

    /** Habilidades del ojo por id (vacio si el ojo no existe en el registro). */
    private static List<String> abilitiesOf(String eyeId) {
        return EyeRegistry.get(eyeId).map(EyeDefinition::abilities).orElse(List.of());
    }
}
