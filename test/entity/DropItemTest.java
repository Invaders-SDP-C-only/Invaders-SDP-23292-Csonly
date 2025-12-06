package entity;

import engine.DrawManager.SpriteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DropItemTest {

    @BeforeEach
    void resetItems() {
        ShopItem.resetAllItems();
    }

    @Test
    void constructorSetsSpriteAndColorByType() {
        Map<DropItem.ItemType, SpriteType> expected = new EnumMap<>(DropItem.ItemType.class);
        expected.put(DropItem.ItemType.Explode, SpriteType.Item_Explode);
        expected.put(DropItem.ItemType.Slow, SpriteType.Item_Slow);
        expected.put(DropItem.ItemType.Stop, SpriteType.Item_Stop);
        expected.put(DropItem.ItemType.Push, SpriteType.Item_Push);
        expected.put(DropItem.ItemType.Shield, SpriteType.Item_Shield);
        expected.put(DropItem.ItemType.Heal, SpriteType.Item_Heal);

        for (DropItem.ItemType type : DropItem.ItemType.values()) {
            DropItem item = new DropItem(0, 0, 1, type);
            assertEquals(expected.get(type), item.getSpriteType(), "Sprite mismatch for " + type);
            assertNotNull(item.getColor());
        }
    }

    @Test
    void updateMovesDownward() {
        DropItem item = new DropItem(5, 10, 3, DropItem.ItemType.Heal);

        item.update();

        assertEquals(13, item.getPositionY());
    }

    @Test
    void setItemTypeUpdatesSpriteAndColor() {
        DropItem item = new DropItem(0, 0, 1, DropItem.ItemType.Explode);

        item.setItemType(DropItem.ItemType.Shield);

        assertEquals(DropItem.ItemType.Shield, item.getItemType());
        assertEquals(SpriteType.Item_Shield, item.getSpriteType());
        assertEquals(Color.CYAN, item.getColor());
    }

    @Test
    void timeFreezeFlagExpires() throws InterruptedException {
        DropItem.applyTimeFreezeItem(50);
        assertTrue(DropItem.isTimeFreezeActive());

        Thread.sleep(80);
        assertFalse(DropItem.isTimeFreezeActive());
    }
}
