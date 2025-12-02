package engine.level;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LevelManagerTest {

    @Test
    void constructor_loadsLevels() {
        // This test assumes that maps/maps.json is in the classpath and is valid.
        LevelManager levelManager = new LevelManager();
        assertTrue(levelManager.getNumberOfLevels() > 0, "Should load at least one level from JSON or fallback.");
    }

    @Test
    void getLevel_existing() {
        LevelManager levelManager = new LevelManager();
        Level level1 = levelManager.getLevel(1);
        assertNotNull(level1, "Level 1 should exist.");
        assertEquals(1, level1.getLevel());
    }

    @Test
    void getLevel_nonExisting() {
        LevelManager levelManager = new LevelManager();
        // Assuming there are less than 999 levels.
        Level level999 = levelManager.getLevel(999);
        assertNull(level999, "Level 999 should not exist.");
    }

    @Test
    void getNumberOfLevels_isConsistent() {
        LevelManager levelManager = new LevelManager();
        int numLevels = levelManager.getNumberOfLevels();
        // A simple check. The actual number depends on the file.
        assertTrue(numLevels > 0); 
    }

    @SuppressWarnings("java:S3011")
    @Test
    void nullLevels_shouldReturnSafely() throws NoSuchFieldException, IllegalAccessException {
        LevelManager levelManager = new LevelManager();
        
        // Use reflection to set the private 'levels' field to null to test null-check branches.
        java.lang.reflect.Field levelsField = LevelManager.class.getDeclaredField("levels");
        levelsField.setAccessible(true);
        levelsField.set(levelManager, null);

        // Test getLevel when levels is null
        assertNull(levelManager.getLevel(1), "getLevel should return null when the levels list is null.");

        // Test getNumberOfLevels when levels is null
        assertEquals(0, levelManager.getNumberOfLevels(), "getNumberOfLevels should return 0 when the levels list is null.");
    }
}
