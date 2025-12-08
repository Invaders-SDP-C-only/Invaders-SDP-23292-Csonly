package engine;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.AccessController; // 추가됨
import java.security.PrivilegedAction; // 추가됨
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AchievementManagerTest {

    @Mock
    private FileManager fileManagerMock; // Mocking a final class, requires inline mock maker

    private MockedStatic<Core> coreMock; // For mocking a static method

    private AchievementManager achievementManager;

    @SuppressWarnings("java:S3011")
    @BeforeEach
    void setUp() throws IOException {
        // Mock the static Core.getFileManager() to return our mock FileManager
        coreMock = Mockito.mockStatic(Core.class);
        coreMock.when(Core::getFileManager).thenReturn(fileManagerMock);

        // Reset the singleton instance so each test gets a fresh one
        try {
            Field instanceField = AchievementManager.class.getDeclaredField("instance");

            // [Option 2] setAccessible을 doPrivileged 블록으로 감쌉니다.
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                instanceField.setAccessible(true);
                return null;
            });

            instanceField.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Mock the loadAchievements behavior for the constructor call
        when(fileManagerMock.loadAchievements()).thenReturn(new HashMap<>());

        // getInstance() will now create a new instance with fresh fields
        achievementManager = AchievementManager.getInstance();

        // Clear invocations that happened during constructor
        clearInvocations(fileManagerMock);
    }

    @AfterEach
    void tearDown() {
        coreMock.close();
    }

    @Test
    void loadAchievements_unlocksCorrectAchievements() throws IOException {
        // Arrange
        Map<String, Boolean> unlockedStatus = new HashMap<>();
        unlockedStatus.put("Beginner", true);
        unlockedStatus.put("Boss Slayer", true);
        unlockedStatus.put("Intermediate", false); // Explicitly false

        when(fileManagerMock.loadAchievements()).thenReturn(unlockedStatus);

        // Act
        achievementManager.loadAchievements();

        // Assert
        Map<String, Boolean> resultStatus = new HashMap<>();
        achievementManager.getAchievements().forEach(a -> resultStatus.put(a.getName(), a.isUnlocked()));

        assertTrue(resultStatus.get("Beginner"));
        assertTrue(resultStatus.get("Boss Slayer"));
        assertFalse(resultStatus.get("Intermediate"));
    }

    @Test
    void loadAchievements_ioException_callsSave() throws IOException {
        // Arrange
        when(fileManagerMock.loadAchievements()).thenThrow(new IOException("Test exception"));

        // Act
        achievementManager.loadAchievements();

        // Assert
        verify(fileManagerMock, times(1)).saveAchievements(any());
    }

    @Test
    void unlockAchievement_savesState() throws IOException {
        // Act
        achievementManager.unlockAchievement("Intermediate");

        // Assert
        Achievement achievement = achievementManager.getAchievements().stream()
                .filter(a -> a.getName().equals("Intermediate")).findFirst().get();
        assertTrue(achievement.isUnlocked());
        verify(fileManagerMock, times(1)).saveAchievements(any());
    }

    @Test
    void unlockAchievement_alreadyUnlocked_doesNotSave() throws IOException {
        // Arrange: unlock it once
        achievementManager.unlockAchievement("Intermediate");
        verify(fileManagerMock, times(1)).saveAchievements(any()); // Verify initial save

        // Act: unlock it again
        achievementManager.unlockAchievement("Intermediate");

        // Assert: saveAchievements should not be called a second time
        verifyNoMoreInteractions(fileManagerMock);
    }

    @Test
    void unlockAchievement_nonExistent_doesNothing(){
        // Act
        achievementManager.unlockAchievement("Non-existent Achievement");

        // Assert
        verifyNoInteractions(fileManagerMock);
    }

    @Test
    void onTimeElapsed_unlocksSurvivor() throws IOException {
        // Act
        achievementManager.onTimeElapsedSeconds(61);

        // Assert
        verify(fileManagerMock, times(1)).saveAchievements(any());
        assertTrue(getAchievement("Bear Grylls").isUnlocked());
    }

    @Test
    void onTimeElapsed_lessThan60_doesNotUnlock(){
        // Act
        achievementManager.onTimeElapsedSeconds(59);

        // Assert
        verifyNoInteractions(fileManagerMock);
        assertFalse(getAchievement("Bear Grylls").isUnlocked());
    }

    @Test
    void onTimeElapsed_alreadyUnlocked_doesNotSave() throws IOException {
        // Arrange
        achievementManager.onTimeElapsedSeconds(60); // Unlock first time
        verify(fileManagerMock, times(1)).saveAchievements(any());

        // Act
        achievementManager.onTimeElapsedSeconds(120); // Call again

        // Assert
        verifyNoMoreInteractions(fileManagerMock);
    }

    @Test
    void onEnemyDefeated_unlocksFirstBlood_firstTime() throws IOException {
        // Act
        achievementManager.onEnemyDefeated();

        // Assert
        verify(fileManagerMock, times(1)).saveAchievements(any());
        assertTrue(getAchievement("First Blood").isUnlocked());
    }

    @Test
    void onEnemyDefeated_alreadyUnlocked_doesNotUnlockFirstBlood() throws IOException {
        // Arrange
        achievementManager.onEnemyDefeated(); // First kill
        verify(fileManagerMock, times(1)).saveAchievements(any());

        // Act
        achievementManager.onEnemyDefeated(); // Second kill

        // Assert
        verifyNoMoreInteractions(fileManagerMock);
    }

    @Test
    void onEnemyDefeated_unlocksBadSniper(){
        // Arrange: 5 shots, 5 hits. Maintain high accuracy so the achievement isn't unlocked early.
        for (int i = 0; i < 5; i++) {
            achievementManager.onShotFired();
            achievementManager.onEnemyDefeated();
        }
        // At this point, the check for "Bad Sniper" is not active yet.

        // Act: Fire 5 more shots (all misses) to drop accuracy.
        for (int i = 0; i < 5; i++) {
            achievementManager.onShotFired();
        }
        // State: 10 total shots fired, 5 total hits.

        // Now, get one more hit. This will trigger the check with low accuracy.
        achievementManager.onEnemyDefeated();
        // Inside this call, shotsHit becomes 6. Check is performed with 10 shots, 6 hits.
        // Accuracy is 60%, which is <= 80%, so it should unlock.

        // Assert
        assertTrue(getAchievement("Bad Sniper").isUnlocked());
    }

    @Test
    void onEnemyDefeated_doesNotUnlockBadSniper_highAccuracy(){
        // Arrange: Simulate a perfect run up to the point of checking.
        // 5 shots, 5 hits. The "Bad Sniper" check is not active yet.
        for (int i = 0; i < 5; i++) {
            achievementManager.onShotFired();
            achievementManager.onEnemyDefeated();
        }
        assertFalse(getAchievement("Bad Sniper").isUnlocked(), "Should not unlock before 6 shots");

        // Act: Fire the 6th shot and get the 6th hit. Now the check is active.
        achievementManager.onShotFired();     // 6 shots total
        achievementManager.onEnemyDefeated(); // 6 hits total
        // Inside this call, shotsHit becomes 6. Check is performed with 6 shots, 6 hits.
        // Accuracy is 100%, which is > 80%, so no unlock.

        // Assert
        assertFalse(getAchievement("Bad Sniper").isUnlocked());
    }

    @Test
    void onEnemyDefeated_doesNotUnlockBadSniper_notEnoughShots(){
        // Arrange: 5 shots, 1 hit (20% accuracy, but not enough shots)
        for (int i = 0; i < 5; i++) achievementManager.onShotFired();
        achievementManager.onEnemyDefeated();

        // Assert
        assertFalse(getAchievement("Bad Sniper").isUnlocked());
    }

    private Achievement getAchievement(String name) {
        return achievementManager.getAchievements().stream()
                .filter(a -> a.getName().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("Achievement not found: " + name));
    }
}