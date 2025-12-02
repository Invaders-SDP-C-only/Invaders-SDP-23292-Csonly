package engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController; // 추가됨
import java.security.PrivilegedAction; // 추가됨
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("java:S3011")
class FileManagerTest {

    private FileManager fileManager;

    @BeforeEach
    void setUp() throws Exception {
        // Mock the logger to avoid NullPointerException
        Logger logger = mock(Logger.class);
        // Get the singleton instance of FileManager
        fileManager = FileManager.getInstance();

        // Inject mock logger into FileManager instance
        Field loggerField = FileManager.class.getDeclaredField("logger");

        // [Option 2 적용] setAccessible을 doPrivileged 블록으로 감쌉니다.
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            loggerField.setAccessible(true);
            return null;
        });

        loggerField.set(fileManager, logger);
    }

    @Test
    void testGetInstance() {
        assertNotNull(fileManager);
        assertSame(fileManager, FileManager.getInstance());
    }


    @Test
    void testSaveAndLoadAchievements() throws IOException {
        // This test will write to "achievements.dat" in the project root.
        File achievementsFile = new File("achievements.dat");
        if (achievementsFile.exists()) {
            achievementsFile.delete();
        }

        List<Achievement> achievements = new ArrayList<>();
        Achievement ac1 = new Achievement("Test Achievement 1", "Test Desc 1");
        ac1.unlock();
        achievements.add(ac1);

        Achievement ac2 = new Achievement("Test Achievement 2", "Test Desc 2");
        achievements.add(ac2);

        fileManager.saveAchievements(achievements);

        assertTrue(achievementsFile.exists());

        Map<String, Boolean> loadedAchievements = fileManager.loadAchievements();
        assertEquals(2, loadedAchievements.size());
        assertTrue(loadedAchievements.get("Test Achievement 1"));
        assertFalse(loadedAchievements.getOrDefault("Test Achievement 2", false));

        // Cleanup
        achievementsFile.delete();
    }
}