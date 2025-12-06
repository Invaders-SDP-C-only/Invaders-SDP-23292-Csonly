package entity;

import engine.DrawManager;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

class OmegaBossTest {

    @Test
    void switchesToSecondPatternBelowHalfHealth() {
        OmegaBoss boss = new OmegaBoss(Color.WHITE, 300);
        boss.attach(new screen.Screen(400, 400, 60));

        boss.takeDamage(30); // drop below half of 45
        boss.update();

        assertEquals(DrawManager.SpriteType.OmegaBoss2, boss.getSpriteType());
        assertEquals(Color.MAGENTA, boss.getColor());
    }

    @Test
    void destroyMarksAsDestroyedAndSetsSprite() {
        OmegaBoss boss = new OmegaBoss(Color.WHITE, 300);

        boss.destroy();

        assertTrue(boss.isDestroyed());
        assertEquals(DrawManager.SpriteType.OmegaBossDeath, boss.getSpriteType());
    }
}
