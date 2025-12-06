package entity;

import engine.DrawManager;
import engine.LaserBeamManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FinalBoss3Test {

    @Test
    void takeDamageDestroysWhenHpBelowZero() {
        FinalBoss_3 boss = new FinalBoss_3(0, 0, 300, 300, new LaserBeamManager());

        boss.takeDamage(150);

        assertTrue(boss.isDestroyed());
        assertEquals(DrawManager.SpriteType.FinalBossDeath, boss.getSpriteType());
    }

    @Test
    void lowHealthTriggersBossWaveWarning() {
        FinalBoss_3 boss = new FinalBoss_3(0, 0, 300, 300, new LaserBeamManager());

        boss.takeDamage(91); // drop below 10%

        assertTrue(boss.isBossWaveActiveOrTransition());
        assertTrue(boss.isLaserWarningActive());
        assertFalse(boss.isDestroyed());
    }
}
