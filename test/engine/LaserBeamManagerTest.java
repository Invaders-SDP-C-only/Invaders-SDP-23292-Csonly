package engine;

import entity.Ship;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LaserBeamManagerTest {

    private LaserBeamManager laserBeamManager;
    private Ship ship;

    @BeforeEach
    void setUp() {
        laserBeamManager = new LaserBeamManager();
        ship = mock(Ship.class);
    }

    @Test
    void testAddAndGetBeams() {
        LaserBeam beam1 = new LaserBeam(0, 0, 0, 100, 5, 1000);
        LaserBeam beam2 = new LaserBeam(0, 0, 0, 100, 5, 1000);
        laserBeamManager.addBeam(beam1);
        laserBeamManager.addBeam(beam2);
        assertEquals(2, laserBeamManager.getBeams().size());
    }

    @Test
    void testUpdate() throws InterruptedException {
        LaserBeam beam1 = new LaserBeam(0, 0, 0, 100, 5, 100);
        LaserBeam beam2 = new LaserBeam(0, 0, 0, 100, 5, 300);
        laserBeamManager.addBeam(beam1);
        laserBeamManager.addBeam(beam2);
        assertEquals(2, laserBeamManager.getBeams().size());
        Thread.sleep(150);
        laserBeamManager.update();
        assertEquals(1, laserBeamManager.getBeams().size());
        Thread.sleep(200);
        laserBeamManager.update();
        assertEquals(0, laserBeamManager.getBeams().size());
    }

    @Test
    void testClear() {
        LaserBeam beam1 = new LaserBeam(0, 0, 0, 100, 5, 1000);
        laserBeamManager.addBeam(beam1);
        assertFalse(laserBeamManager.getBeams().isEmpty());
        laserBeamManager.clear();
        assertTrue(laserBeamManager.getBeams().isEmpty());
    }

    @Test
    void testCheckCollisionWithShip() {
        when(ship.getPositionX()).thenReturn((int) 50f);
        when(ship.getPositionY()).thenReturn((int) 0f);
        when(ship.getWidth()).thenReturn((int) 10f);
        when(ship.getHeight()).thenReturn((int) 10f);

        // No collision
        LaserBeam beamNoCollision = new LaserBeam(0, 100, 90, 100, 5, 1000);
        laserBeamManager.addBeam(beamNoCollision);
        assertFalse(laserBeamManager.checkCollisionWithShip(ship));
        laserBeamManager.clear();

        // Collision
        LaserBeam beamCollision = new LaserBeam(0, 0, 0, 100, 10, 1000);
        laserBeamManager.addBeam(beamCollision);
        assertTrue(laserBeamManager.checkCollisionWithShip(ship));
        laserBeamManager.clear();

        // Edge case: point on the edge of the beam
        LaserBeam beamEdge = new LaserBeam(0, 5, 0, 100, 10, 1000);
        laserBeamManager.addBeam(beamEdge);
        assertTrue(laserBeamManager.checkCollisionWithShip(ship));
        laserBeamManager.clear();

    }
}