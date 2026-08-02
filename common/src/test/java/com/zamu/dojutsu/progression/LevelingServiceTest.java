package com.zamu.dojutsu.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.event.DomainEventBus;
import com.zamu.dojutsu.domain.event.NinjaXpChangedEvent;
import com.zamu.dojutsu.domain.event.SkillLevelChangedEvent;
import com.zamu.dojutsu.domain.progression.LeveledStat;
import com.zamu.dojutsu.domain.progression.XpCurve;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Tests del motor de auto-nivelacion por XP (DJT-036): multi-nivel, eventos y cap. Puro, sin Minecraft. */
class LevelingServiceTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void addSkillXpSubeVariosNivelesDeUnaVez() {
        // Banda 0-10 cuesta 10 XP/nivel: 30 XP => 3 niveles de una (sin guardia 1/tick).
        PlayerNinjaData data = new PlayerNinjaData();
        DomainEventBus bus = new DomainEventBus();
        LevelingService.addSkillXp(data, PLAYER, Skill.TAIJUTSU, 30, bus);
        assertEquals(3, data.progression().skill(Skill.TAIJUTSU).level());
        assertEquals(0, data.progression().skill(Skill.TAIJUTSU).xp());
    }

    @Test
    void addSkillXpEmiteUnEventoAgregadoAlCambiarNivel() {
        PlayerNinjaData data = new PlayerNinjaData();
        DomainEventBus bus = new DomainEventBus();
        AtomicReference<SkillLevelChangedEvent> captured = new AtomicReference<>();
        bus.subscribe(SkillLevelChangedEvent.class, captured::set);

        LevelingService.addSkillXp(data, PLAYER, Skill.TAIJUTSU, 30, bus);

        SkillLevelChangedEvent event = captured.get();
        assertEquals("taijutsu", event.skillId());
        assertEquals(0, event.oldLevel());
        assertEquals(3, event.newLevel());
        assertEquals(PLAYER, event.player());
    }

    @Test
    void addSkillXpSinCambioDeNivelNoEmiteEvento() {
        // 5 XP en banda de coste 10: no cambia de nivel -> sin evento (aunque acumula xp).
        PlayerNinjaData data = new PlayerNinjaData();
        DomainEventBus bus = new DomainEventBus();
        AtomicReference<SkillLevelChangedEvent> captured = new AtomicReference<>();
        bus.subscribe(SkillLevelChangedEvent.class, captured::set);

        LevelingService.addSkillXp(data, PLAYER, Skill.TAIJUTSU, 5, bus);

        assertEquals(0, data.progression().skill(Skill.TAIJUTSU).level());
        assertEquals(5, data.progression().skill(Skill.TAIJUTSU).xp());
        assertNull(captured.get());
    }

    @Test
    void addSkillXpIgnoraHabilidadSinAutonivel() {
        PlayerNinjaData data = new PlayerNinjaData();
        DomainEventBus bus = new DomainEventBus();
        AtomicReference<SkillLevelChangedEvent> captured = new AtomicReference<>();
        bus.subscribe(SkillLevelChangedEvent.class, captured::set);

        LevelingService.addSkillXp(data, PLAYER, Skill.SENJUTSU, 1000, bus);

        assertEquals(new LeveledStat(0, 0), data.progression().skill(Skill.SENJUTSU));
        assertNull(captured.get());
    }

    @Test
    void addSkillXpNoPositivaSeIgnora() {
        PlayerNinjaData data = new PlayerNinjaData();
        DomainEventBus bus = new DomainEventBus();
        LevelingService.addSkillXp(data, PLAYER, Skill.MEDICAL, 0, bus);
        LevelingService.addSkillXp(data, PLAYER, Skill.MEDICAL, -50, bus);
        assertEquals(0, data.progression().skill(Skill.MEDICAL).level());
    }

    @Test
    void addSkillXpRespetaElCap() {
        PlayerNinjaData data = new PlayerNinjaData();
        data.setProgression(data.progression().withSkill(Skill.KENJUTSU, new LeveledStat(XpCurve.MAX_LEVEL, 0)));
        DomainEventBus bus = new DomainEventBus();
        AtomicReference<SkillLevelChangedEvent> captured = new AtomicReference<>();
        bus.subscribe(SkillLevelChangedEvent.class, captured::set);

        LevelingService.addSkillXp(data, PLAYER, Skill.KENJUTSU, 100_000, bus);

        assertEquals(XpCurve.MAX_LEVEL, data.progression().skill(Skill.KENJUTSU).level());
        assertNull(captured.get()); // ya estaba al cap: sin cambio ni evento
    }

    @Test
    void addNinjaXpAcumulaYEmiteEvento() {
        PlayerNinjaData data = new PlayerNinjaData();
        DomainEventBus bus = new DomainEventBus();
        AtomicReference<NinjaXpChangedEvent> captured = new AtomicReference<>();
        bus.subscribe(NinjaXpChangedEvent.class, captured::set);

        LevelingService.addNinjaXp(data, PLAYER, 1_500, bus);

        assertEquals(1_500, data.progression().ninjaXp());
        assertEquals(0, captured.get().oldXp());
        assertEquals(1_500, captured.get().newXp());
    }

    @Test
    void addNinjaXpNoPositivaSeIgnora() {
        PlayerNinjaData data = new PlayerNinjaData();
        DomainEventBus bus = new DomainEventBus();
        AtomicReference<NinjaXpChangedEvent> captured = new AtomicReference<>();
        bus.subscribe(NinjaXpChangedEvent.class, captured::set);

        LevelingService.addNinjaXp(data, PLAYER, 0, bus);

        assertEquals(0, data.progression().ninjaXp());
        assertNull(captured.get());
    }

    @Test
    void addSkillLevelsSubeDirectoYEmiteEvento() {
        // DJT-057: bonus de clan 'd.<skill> += N'. Conserva la XP y emite evento (los hitos reaccionan).
        PlayerNinjaData data = new PlayerNinjaData();
        data.setProgression(data.progression().withSkill(Skill.TAIJUTSU, new LeveledStat(5, 7)));
        DomainEventBus bus = new DomainEventBus();
        AtomicReference<SkillLevelChangedEvent> captured = new AtomicReference<>();
        bus.subscribe(SkillLevelChangedEvent.class, captured::set);

        LevelingService.addSkillLevels(data, PLAYER, Skill.TAIJUTSU, 10, bus);

        assertEquals(15, data.progression().skill(Skill.TAIJUTSU).level());
        assertEquals(7, data.progression().skill(Skill.TAIJUTSU).xp()); // la XP no se toca
        assertEquals(5, captured.get().oldLevel());
        assertEquals(15, captured.get().newLevel());
    }

    @Test
    void addSkillLevelsClampaAlCapYNoEmiteSiNoCambia() {
        PlayerNinjaData data = new PlayerNinjaData();
        data.setProgression(data.progression().withSkill(Skill.KENJUTSU, new LeveledStat(495, 0)));
        DomainEventBus bus = new DomainEventBus();
        AtomicReference<SkillLevelChangedEvent> captured = new AtomicReference<>();

        LevelingService.addSkillLevels(data, PLAYER, Skill.KENJUTSU, 100, bus); // 495+100 -> clamp 500
        assertEquals(XpCurve.MAX_LEVEL, data.progression().skill(Skill.KENJUTSU).level());

        bus.subscribe(SkillLevelChangedEvent.class, captured::set);
        LevelingService.addSkillLevels(data, PLAYER, Skill.KENJUTSU, 10, bus); // ya al cap: sin cambio
        LevelingService.addSkillLevels(data, PLAYER, Skill.KENJUTSU, 0, bus); // no positivo: ignorado
        assertNull(captured.get());
    }

    @Test
    void removeNinjaXpRestaYEmiteEvento() {
        PlayerNinjaData data = new PlayerNinjaData();
        DomainEventBus bus = new DomainEventBus();
        LevelingService.addNinjaXp(data, PLAYER, 1_500, bus);
        AtomicReference<NinjaXpChangedEvent> captured = new AtomicReference<>();
        bus.subscribe(NinjaXpChangedEvent.class, captured::set);

        LevelingService.removeNinjaXp(data, PLAYER, 500, bus);

        assertEquals(1_000, data.progression().ninjaXp());
        assertEquals(1_500, captured.get().oldXp());
        assertEquals(1_000, captured.get().newXp());
    }

    @Test
    void removeNinjaXpTieneSueloEnCero() {
        PlayerNinjaData data = new PlayerNinjaData();
        DomainEventBus bus = new DomainEventBus();
        LevelingService.addNinjaXp(data, PLAYER, 30, bus);

        LevelingService.removeNinjaXp(data, PLAYER, 999, bus);

        assertEquals(0, data.progression().ninjaXp()); // nunca negativa (alimenta el rango)
    }

    @Test
    void removeNinjaXpSinCambioNoEmiteEvento() {
        PlayerNinjaData data = new PlayerNinjaData(); // ya esta a 0
        DomainEventBus bus = new DomainEventBus();
        AtomicReference<NinjaXpChangedEvent> captured = new AtomicReference<>();
        bus.subscribe(NinjaXpChangedEvent.class, captured::set);

        LevelingService.removeNinjaXp(data, PLAYER, 50, bus);
        LevelingService.removeNinjaXp(data, PLAYER, 0, bus); // cantidad no positiva: ignorada

        assertEquals(0, data.progression().ninjaXp());
        assertNull(captured.get());
    }
}
