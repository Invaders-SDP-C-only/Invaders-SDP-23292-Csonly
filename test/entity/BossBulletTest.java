package entity;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

class BossBulletTest {

    @Test
    void updateAdvancesByDelta() {
        BossBullet bullet = new BossBullet(10, 20, 2, -3, 4, 4, Color.RED);

        bullet.update();

        assertEquals(12, bullet.getPositionX());
        assertEquals(17, bullet.getPositionY());
    }

    @Test
    void detectsOffScreen() {
        BossBullet bullet = new BossBullet(0, 0, 0, 0, 4, 4, Color.RED);

        assertTrue(bullet.isOffScreen(-1, 100));
        assertTrue(bullet.isOffScreen(100, -1));
        assertFalse(bullet.isOffScreen(100, 100));
    }
}
