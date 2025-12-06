package engine.level;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LevelTest {

    @Test
    void simpleConstructor() {
        Level level = new Level(1, 5, 4, 60, 2000);
        assertEquals(1, level.getLevel());
        assertEquals(5, level.getFormationWidth());
        assertEquals(4, level.getFormationHeight());
        assertEquals(60, level.getBaseSpeed());
        assertEquals(2000, level.getShootingFrecuency());
        assertNull(level.getLevelName());
        assertNull(level.getEnemyFormation());
        assertNull(level.getEnemyTypes());
    }

    @Test
    void mapConstructor_basicFields() {
        Map<String, Object> map = new HashMap<>();
        map.put("level", 2);
        map.put("levelName", "Test Level");
        map.put("achievementTrigger", "LEVEL_2_CLEARED");
        map.put("specialGimmick", "Asteroids");
        map.put("unlockCondition", "LEVEL_1_CLEARED");

        Level level = new Level(map);

        assertEquals(2, level.getLevel());
        assertEquals("Test Level", level.getLevelName());
        assertEquals("LEVEL_2_CLEARED", level.getAchievementTrigger());
        assertEquals("Asteroids", level.getSpecialGimmick());
        assertEquals("LEVEL_1_CLEARED", level.getUnlockCondition());
    }

    @Test
    void mapConstructor_nestedObjects() {
        Map<String, Object> map = new HashMap<>();
        map.put("level", 3);

        Map<String, Object> formationMap = new HashMap<>();
        formationMap.put("formationWidth", 8);
        formationMap.put("formationHeight", 6);
        formationMap.put("baseSpeed", 50);
        formationMap.put("shootingFrecuency", 1000);
        map.put("enemyFormation", formationMap);

        List<Map<String, Object>> enemyTypesList = new ArrayList<>();
        Map<String, Object> enemyTypeMap = new HashMap<>();
        enemyTypeMap.put("type", "Advanced");
        enemyTypeMap.put("count", 5);
        enemyTypesList.add(enemyTypeMap);
        map.put("enemyTypes", enemyTypesList);

        List<Map<String, Object>> itemDropsList = new ArrayList<>();
        Map<String, Object> itemDropMap = new HashMap<>();
        itemDropMap.put("enemyType", "Advanced");
        itemDropMap.put("itemId", "ExtraLife");
        itemDropMap.put("dropChance", 0.1);
        itemDropsList.add(itemDropMap);
        map.put("itemDrops", itemDropsList);
        
        Map<String, Object> bonusMap = new HashMap<>();
        bonusMap.put("currency", 500);
        map.put("completionBonus", bonusMap);

        map.put("bossId", "MegaBoss");
        map.put("achievementTrigger", "MEGA_BOSS_DEFEATED");

        Level level = new Level(map);
        
        assertEquals(3, level.getLevel());
        assertEquals("MegaBoss", level.getBossId());
        assertEquals("MEGA_BOSS_DEFEATED", level.getAchievementTrigger());

        assertNotNull(level.getEnemyFormation());
        assertEquals(8, level.getFormationWidth()); // check fallback
        assertEquals(8, level.getEnemyFormation().getFormationWidth());

        assertNotNull(level.getEnemyTypes());
        assertEquals(1, level.getEnemyTypes().size());
        assertEquals("Advanced", level.getEnemyTypes().get(0).getType());

        assertNotNull(level.getCompletionBonus());
        assertEquals(500, level.getCompletionBonus().getCurrency());

        assertNotNull(level.getItemDrops());
        assertEquals(1, level.getItemDrops().size());
        assertEquals("ExtraLife", level.getItemDrops().get(0).getItemId());
    }

    @Test
    void mapConstructor_withNulls() {
        // This test covers the code paths where optional JSON fields are missing (null)
        Map<String, Object> map = new HashMap<>();
        map.put("level", 4);
        // "enemyFormation", "enemyTypes", "itemDrops", "completionBonus" are missing

        Level level = new Level(map);

        assertEquals(4, level.getLevel());
        // Verify that null fields are handled gracefully
        assertNull(level.getEnemyFormation());
        assertNull(level.getEnemyTypes());
        assertNull(level.getItemDrops());
        assertNull(level.getCompletionBonus());

        // Also check fallback values are not set
        assertEquals(0, level.getFormationWidth());
        assertEquals(0, level.getFormationHeight());
        assertEquals(0, level.getBaseSpeed());
        assertEquals(0, level.getShootingFrecuency());
    }
}
