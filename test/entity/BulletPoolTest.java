package entity;

import engine.DrawManager.SpriteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BulletPoolTest {

    @BeforeEach
    void resetState() throws Exception {
        ShopItem.resetAllItems();
        clearPool();
    }

    @Test
    void getBulletAdjustsPositionAndSpeed() {
        Bullet bullet = BulletPool.getBullet(50, 60, -4);

        assertEquals(47, bullet.getPositionX()); // center adjustment by width / 2 (6 / 2)
        assertEquals(60, bullet.getPositionY());
        assertEquals(-4, bullet.getSpeed());
        assertEquals(SpriteType.Bullet, bullet.getSpriteType());
    }

    @Test
    void recycleReusesBulletAndResetsState() {
        ShopItem.setPenetrationLevel(2);
        Bullet first = BulletPool.getBullet(50, 60, -4);
        first.penetration(); // bump penetration count
        first.setSpeed(5);
        first.setPositionX(100);
        first.setPositionY(200);

        Set<Bullet> recycle = new HashSet<>();
        recycle.add(first);
        BulletPool.recycle(recycle);

        Bullet reused = BulletPool.getBullet(30, 40, -6);
        assertSame(first, reused);
        assertEquals(27, reused.getPositionX());
        assertEquals(40, reused.getPositionY());
        assertEquals(-6, reused.getSpeed());
        assertTrue(reused.canPenetration());
        assertEquals(SpriteType.Bullet, reused.getSpriteType());
    }

    private void clearPool() throws Exception {
        Field poolField = BulletPool.class.getDeclaredField("pool");
        poolField.setAccessible(true);
        poolField.set(null, new HashSet<Bullet>());
    }
}
