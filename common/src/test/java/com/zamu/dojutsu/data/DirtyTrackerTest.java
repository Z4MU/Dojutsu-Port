package com.zamu.dojutsu.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.state.OnboardingState;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Tests del tracker de campos observables sucios (DJT-027). */
class DirtyTrackerTest {

    @Test
    void nuevoTrackerEstaVacio() {
        DirtyTracker t = new DirtyTracker();
        assertTrue(t.isEmpty());
        assertFalse(t.isDirty(ObservableField.ONBOARDING));
        assertEquals(Set.of(), t.dirtyFields());
    }

    @Test
    void markDirtyMarcaElCampo() {
        DirtyTracker t = new DirtyTracker();
        t.markDirty(ObservableField.ONBOARDING);
        assertTrue(t.isDirty(ObservableField.ONBOARDING));
        assertFalse(t.isEmpty());
        assertEquals(Set.of(ObservableField.ONBOARDING), t.dirtyFields());
    }

    @Test
    void clearLimpiaLasMarcas() {
        DirtyTracker t = new DirtyTracker();
        t.markDirty(ObservableField.ONBOARDING);
        t.clear();
        assertTrue(t.isEmpty());
    }

    @Test
    void setOnboardingMarcaElCampoObservable() {
        PlayerNinjaData data = new PlayerNinjaData();
        assertTrue(data.dirtyTracker().isEmpty());
        data.setOnboarding(data.onboarding().withPhase(OnboardingState.Phase.SELECTING_CLAN));
        assertTrue(data.dirtyTracker().isDirty(ObservableField.ONBOARDING));
    }

    @Test
    void markDirtyNuloEsRechazado() {
        DirtyTracker t = new DirtyTracker();
        assertThrows(NullPointerException.class, () -> t.markDirty(null));
    }
}
