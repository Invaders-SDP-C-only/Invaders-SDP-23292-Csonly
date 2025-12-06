package entity;

import engine.DrawManager.SpriteType;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

class EnemyShipTest {

    @Test
    void constructorSetsPointValueBySprite() {
        EnemyShip shipA = new EnemyShip(0, 0, SpriteType.EnemyShipA1);
        EnemyShip shipB = new EnemyShip(0, 0, SpriteType.EnemyShipB1);
        EnemyShip shipC = new EnemyShip(0, 0, SpriteType.EnemyShipC1);

        assertEquals(10, shipA.getPointValue());
        assertEquals(20, shipB.getPointValue());
        assertEquals(30, shipC.getPointValue());
    }

    @Test
    void updateTogglesAnimationFrame() {
        EnemyShip ship = new EnemyShip(0, 0, SpriteType.EnemyShipA1);

        ship.update();

        assertEquals(SpriteType.EnemyShipA2, ship.getSpriteType());
    }

    @Test
    void destroySetsExplosionAndFlag() throws InterruptedException {
        EnemyShip ship = new EnemyShip(0, 0, SpriteType.EnemyShipA1);

        ship.destroy();

        assertTrue(ship.isDestroyed());
        assertEquals(SpriteType.Explosion, ship.getSpriteType());

        Thread.sleep(550); // explosionCooldown is 500ms
        assertTrue(ship.isExplosionFinished());
    }

    @Test
    void specialConstructorSetsDefaults() {
        EnemyShip ship = new EnemyShip(Color.RED, EnemyShip.Direction.LEFT, 3);

        assertEquals(SpriteType.EnemyShipSpecial, ship.getSpriteType());
        assertEquals(EnemyShip.Direction.LEFT, ship.getDirection());
        assertEquals(3, ship.getXSpeed());
        assertFalse(ship.isDestroyed());
        assertEquals(100, ship.getPointValue());
    }

    @Test
    void movementAdjustsPositions() {
        EnemyShip ship = new EnemyShip(5, 7, SpriteType.EnemyShipB1);

        ship.move(3, -2);

        assertEquals(8, ship.getPositionX());
        assertEquals(5, ship.getPositionY());
    }
}
