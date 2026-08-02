package com.zamu.dojutsu.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import org.junit.jupiter.api.Test;

/** Tests del parseo de configuracion (DJT-025). Defaults fieles a [S01]. */
class DojutsuConfigTest {

    @Test
    void defaultsFielesAlDatapack() {
        DojutsuConfig d = DojutsuConfig.DEFAULTS;
        assertTrue(d.allowCustomTrader());
        assertTrue(d.allowShinobis());
        assertTrue(d.allowMultJinchuriki());
        assertFalse(d.checkHardcore());
        assertFalse(d.useTraderSpawnRate());
        assertTrue(d.customMangekyoObtainable());
        assertEquals(1, d.killMobPoints());
        assertEquals(3, d.killPlayerPoints());
        assertEquals(200, d.killBossPoints());
        assertEquals(1, d.traderSpawnRate());
        assertEquals(1, d.traderSpawnChance());
        assertEquals(1, d.natureEnergyCharge());
    }

    @Test
    void propertiesVacioDaDefaults() {
        assertEquals(DojutsuConfig.DEFAULTS, DojutsuConfig.fromProperties(new Properties()));
    }

    @Test
    void parseaValoresValidos() {
        Properties p = new Properties();
        p.setProperty("AllowShinobis", "0");
        p.setProperty("CheckHardcore", "1");
        p.setProperty("KillMobPoints", "5");
        p.setProperty("KillBossPoints", "1000");
        DojutsuConfig c = DojutsuConfig.fromProperties(p);
        assertFalse(c.allowShinobis());
        assertTrue(c.checkHardcore());
        assertEquals(5, c.killMobPoints());
        assertEquals(1000, c.killBossPoints());
        // Las claves no tocadas conservan su default:
        assertEquals(3, c.killPlayerPoints());
        assertTrue(c.allowCustomTrader());
    }

    @Test
    void aceptaTrueFalseAdemasDe1Y0() {
        Properties p = new Properties();
        p.setProperty("AllowShinobis", "false");
        p.setProperty("UseTraderSpawnRate", "true");
        DojutsuConfig c = DojutsuConfig.fromProperties(p);
        assertFalse(c.allowShinobis());
        assertTrue(c.useTraderSpawnRate());
    }

    @Test
    void valorEnteroInvalidoCaeAlDefault() {
        Properties p = new Properties();
        p.setProperty("KillMobPoints", "abc");
        assertEquals(DojutsuConfig.DEFAULTS.killMobPoints(), DojutsuConfig.fromProperties(p).killMobPoints());
    }

    @Test
    void valorBooleanoInvalidoCaeAlDefault() {
        Properties p = new Properties();
        p.setProperty("CheckHardcore", "quiza");
        assertEquals(DojutsuConfig.DEFAULTS.checkHardcore(), DojutsuConfig.fromProperties(p).checkHardcore());
    }
}
