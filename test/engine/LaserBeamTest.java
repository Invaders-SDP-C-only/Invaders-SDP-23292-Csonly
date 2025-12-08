package engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LaserBeamTest {

    @Test
    void testConstructorAndGetters() {
        LaserBeam laser = new LaserBeam(100, 200, 45, 300, 5, 1000);
        assertEquals(100, laser.getOriginX());
        assertEquals(200, laser.getOriginY());
        assertEquals((float)Math.toRadians(45), laser.getAngle());
        assertEquals(300, laser.getLength());
        assertEquals(5, laser.getThickness());
    }

    @Test
    void testUpdateAndIsExpired() throws InterruptedException {
        LaserBeam laser = new LaserBeam(100, 200, 45, 300, 5, 100);
        assertFalse(laser.isExpired());
        laser.update();
        assertFalse(laser.isExpired());
        Thread.sleep(150);
        laser.update();
        assertTrue(laser.isExpired());
    }

    @Test
    void testGetAlphaFactor() throws InterruptedException {
        LaserBeam laser = new LaserBeam(100, 200, 45, 300, 5, 100);
        assertTrue(laser.getAlphaFactor() > 0.9);
        Thread.sleep(50);
        laser.update();
        assertTrue(laser.getAlphaFactor() > 0 && laser.getAlphaFactor() < 1);
        Thread.sleep(100);
        laser.update();
        assertEquals(0, laser.getAlphaFactor());
    }
}
