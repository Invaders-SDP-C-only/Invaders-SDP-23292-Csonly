package entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ItemPoolTest {

    @BeforeEach
    void clearPool() throws Exception {
        Field poolField = ItemPool.class.getDeclaredField("pool");
        poolField.setAccessible(true);
        poolField.set(null, new HashSet<DropItem>());
    }

    @Test
    void getItemCreatesOrReusesAndCenters() {
        DropItem item = ItemPool.getItem(20, 30, 2, DropItem.ItemType.Heal);

        assertEquals(20 - item.getWidth() / 2, item.getPositionX());
        assertEquals(30, item.getPositionY());
        assertEquals(2, item.getSpeed());
        assertEquals(DropItem.ItemType.Heal, item.getItemType());

        Set<DropItem> recycle = new HashSet<>();
        recycle.add(item);
        ItemPool.recycle(recycle);

        DropItem reused = ItemPool.getItem(10, 15, 5, DropItem.ItemType.Explode);
        assertSame(item, reused);
        assertEquals(10 - reused.getWidth() / 2, reused.getPositionX());
        assertEquals(15, reused.getPositionY());
        assertEquals(5, reused.getSpeed());
        assertEquals(DropItem.ItemType.Explode, reused.getItemType());
    }
}
