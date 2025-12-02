package engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AchievementTest {

    private Achievement achievement;
    private final String name = "Test Achievement";
    private final String description = "This is a test achievement.";

    @BeforeEach
    void setUp() {
        achievement = new Achievement(name, description);
    }

    @Test
    void constructorAndGetters() {
        assertEquals(name, achievement.getName());
        assertEquals(description, achievement.getDescription());
        assertFalse(achievement.isUnlocked(), "Achievement should be locked initially.");
    }

    @Test
    void unlock() {
        assertFalse(achievement.isUnlocked(), "Achievement should be locked before unlocking.");
        achievement.unlock();
        assertTrue(achievement.isUnlocked(), "Achievement should be unlocked after calling unlock().");
    }

    @Test
    void unlock_alreadyUnlocked() {
        achievement.unlock();
        assertTrue(achievement.isUnlocked(), "Achievement should be unlocked.");
        achievement.unlock();
        assertTrue(achievement.isUnlocked(), "Unlocking an already unlocked achievement should not change its state.");
    }
}
