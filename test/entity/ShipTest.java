package entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ShipTest {

    @BeforeEach
    void resetState() throws Exception {
        ShopItem.resetAllItems();
        clearBulletPool();
    }

    @Test
    void movementUsesBaseSpeed() {
        Ship ship = new Ship(10, 20, Color.GREEN);

        ship.moveRight();
        ship.moveDown();
        ship.moveLeft();
        ship.moveUp();

        assertEquals(10, ship.getPositionX());
        assertEquals(20, ship.getPositionY());
    }

    @Test
    void shootingRespectsCooldownAndCreatesBullet() {
        Ship ship = new Ship(0, 0, Color.GREEN);
        Set<Bullet> bullets = new HashSet<>();

        assertTrue(ship.shoot(bullets));
        assertEquals(1, bullets.size());

        assertFalse(ship.shoot(bullets)); // cooldown not finished
    }

    @Test
    void meleeModeTriggersParryInsteadOfBullet() {
        Ship ship = new Ship(0, 0, Color.GREEN);
        ship.setMeleeMode(true);
        Set<Bullet> bullets = new HashSet<>();

        assertTrue(ship.shoot(bullets));
        assertTrue(ship.isParrying());
        assertTrue(bullets.isEmpty());
    }

    @Test
    void invincibilityExpiresAfterDuration() throws InterruptedException {
        Ship ship = new Ship(0, 0, Color.GREEN);

        ship.activateInvincibility(50);
        assertTrue(ship.isInvincible());

        Thread.sleep(80);
        ship.update();

        assertFalse(ship.isInvincible());
        assertEquals(Color.GREEN, ship.getColor());
    }

    private void clearBulletPool() throws Exception {
        Field poolField = BulletPool.class.getDeclaredField("pool");
        poolField.setAccessible(true);
        poolField.set(null, new HashSet<Bullet>());
    }
}
