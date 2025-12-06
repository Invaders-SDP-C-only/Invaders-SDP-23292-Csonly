package entity;

import engine.DrawManager.SpriteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BulletTest {

    @BeforeEach
    void resetItems() {
        ShopItem.resetAllItems();
    }

    @Test
    void updateMovesBySpeed() {
        Bullet bullet = new Bullet(10, 20, -5);

        bullet.update();

        assertEquals(15, bullet.getPositionY());
    }

    @Test
    void setSpriteUsesDirection() {
        Bullet upward = new Bullet(0, 0, -1);
        assertEquals(SpriteType.Bullet, upward.getSpriteType());

        Bullet downward = new Bullet(0, 0, 1);
        assertEquals(SpriteType.EnemyBullet, downward.getSpriteType());
    }

    @Test
    void penetrationRespectsMaximumAndReset() {
        ShopItem.setPenetrationLevel(2);
        Bullet bullet = new Bullet(0, 0, 1);
        bullet.resetPenetration();

        assertTrue(bullet.penetration());
        assertTrue(bullet.penetration());
        assertFalse(bullet.penetration());

        bullet.resetPenetration();
        assertTrue(bullet.canPenetration());
    }
}
