package entity;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SamuraiBossTest {

    @Test
    void enrageTriggersBelowHalfHealth() {
        SamuraiBoss boss = new SamuraiBoss(0, 0, 200, null, null, 300);

        boss.takeDamage(1500); // drop health below half
        boss.update();

        assertTrue(boss.isEnraged());
    }

    @Test
    void shootWaveEmitsWaveWhenAvailable() {
        SamuraiBoss boss = new SamuraiBoss(0, 0, 200, null, null, 300);

        Set<SwordWave> waves = boss.shootWave();

        assertFalse(waves.isEmpty());
        assertEquals(1, waves.size());
    }

    @Test
    void invincibleAfterBrokenPreventsDamage() throws Exception {
        SamuraiBoss boss = new SamuraiBoss(0, 0, 200, null, null, 300);
        setInvincibleAfterBroken(boss, true);
        int hpBefore = boss.getHealPoint();

        boss.takeDamage(1500);

        assertEquals(hpBefore, boss.getHealPoint());

        setInvincibleAfterBroken(boss, false);
        boss.takeDamage(3000);
        assertTrue(boss.isDestroyed());
    }

    private void setInvincibleAfterBroken(SamuraiBoss boss, boolean value) throws Exception {
        Field f = SamuraiBoss.class.getDeclaredField("isInvincibleAfterBroken");
        f.setAccessible(true);
        f.setBoolean(boss, value);
    }
}
