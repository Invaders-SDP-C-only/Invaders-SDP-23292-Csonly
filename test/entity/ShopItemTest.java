package entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShopItemTest {

    @BeforeEach
    void resetItems() {
        ShopItem.resetAllItems();
    }

    @Test
    void setValidLevelsUpdatesState() {
        assertTrue(ShopItem.setMultiShotLevel(1));
        assertEquals(1, ShopItem.getMultiShotLevel());
        assertEquals(2, ShopItem.getMultiShotBulletCount());
        assertEquals(10, ShopItem.getMultiShotSpacing());

        assertTrue(ShopItem.setRapidFireLevel(3));
        assertEquals(3, ShopItem.getRapidFireLevel());
        assertEquals(637, ShopItem.getShootingInterval());

        assertTrue(ShopItem.setPenetrationLevel(2));
        assertEquals(2, ShopItem.getPenetrationLevel());
        assertEquals(2, ShopItem.getPenetrationCount());

        assertTrue(ShopItem.setBulletSpeedLevel(2));
        assertEquals(2, ShopItem.getBulletSpeedLevel());
        assertEquals(-10, ShopItem.getBulletSpeed());

        assertTrue(ShopItem.setSHIPSPEED(4));
        assertEquals(20, ShopItem.getSHIPSpeedCOUNT());
    }

    @Test
    void invalidLevelsReturnFalse() {
        assertFalse(ShopItem.setMultiShotLevel(5));
        assertFalse(ShopItem.setRapidFireLevel(-1));
        assertFalse(ShopItem.setPenetrationLevel(3));
        assertFalse(ShopItem.setBulletSpeedLevel(4));
        assertFalse(ShopItem.setSHIPSPEED(9));
    }

    @Test
    void resetAllItemsClearsEnhancements() {
        ShopItem.setMultiShotLevel(2);
        ShopItem.setRapidFireLevel(4);
        ShopItem.setPenetrationLevel(2);
        ShopItem.setBulletSpeedLevel(3);
        ShopItem.setSHIPSPEED(5);

        ShopItem.resetAllItems();

        assertEquals(0, ShopItem.getMultiShotLevel());
        assertEquals(0, ShopItem.getRapidFireLevel());
        assertEquals(0, ShopItem.getPenetrationLevel());
        assertEquals(0, ShopItem.getBulletSpeedLevel());
        assertEquals(0, ShopItem.getSHIPSpeedCOUNT());
    }
}
