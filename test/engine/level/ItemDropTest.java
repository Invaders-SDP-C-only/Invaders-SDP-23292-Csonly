package engine.level;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ItemDropTest {

    @Test
    void constructorAndGetters_withEnemyType() {
        Map<String, Object> map = new HashMap<>();
        map.put("enemyType", "EnemyB");
        map.put("itemId", "Shield");
        map.put("dropChance", 0.25);

        ItemDrop itemDrop = new ItemDrop(map);

        assertEquals("EnemyB", itemDrop.getEnemyType());
        assertNull(itemDrop.getBossId());
        assertEquals("Shield", itemDrop.getItemId());
        assertEquals(0.25, itemDrop.getDropChance());
    }

    @Test
    void constructorAndGetters_withBossId() {
        Map<String, Object> map = new HashMap<>();
        map.put("bossId", "MidBoss1");
        map.put("itemId", "ExtraLife");
        map.put("dropChance", 1.0);

        ItemDrop itemDrop = new ItemDrop(map);

        assertNull(itemDrop.getEnemyType());
        assertEquals("MidBoss1", itemDrop.getBossId());
        assertEquals("ExtraLife", itemDrop.getItemId());
        assertEquals(1.0, itemDrop.getDropChance());
    }
}
