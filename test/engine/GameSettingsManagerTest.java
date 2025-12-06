package engine;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class GameSettingsManagerTest {

    private static final String SETTINGS_FILE = "settings.properties";
    private GameSettingsManager settingsManager;

    @BeforeEach
    void setUp() throws Exception {
        // Reset the singleton instance before each test using reflection.
        Field instance = GameSettingsManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);

        // Ensure there's no settings file from previous runs.
        deleteSettingsFile();
        settingsManager = GameSettingsManager.getInstance();
    }

    @AfterEach
    void tearDown() {
        // Clean up the created settings file.
        deleteSettingsFile();
    }

    private void deleteSettingsFile() {
        try {
            Files.deleteIfExists(Paths.get(SETTINGS_FILE));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testGetInstance() {
        assertNotNull(settingsManager, "getInstance should not return null.");
        GameSettingsManager anotherInstance = GameSettingsManager.getInstance();
        assertSame(settingsManager, anotherInstance, "getInstance should return the same instance.");
    }

    @Test
    void testDefaultSettings() {
        // When no file exists, default settings should be loaded.
        assertEquals(0.5f, settingsManager.getVolume());
        assertEquals(KeyEvent.VK_W, settingsManager.getKey("UP"));
        assertEquals(KeyEvent.VK_S, settingsManager.getKey("DOWN"));
        assertEquals(KeyEvent.VK_A, settingsManager.getKey("LEFT"));
        assertEquals(KeyEvent.VK_D, settingsManager.getKey("RIGHT"));
        assertEquals(KeyEvent.VK_SPACE, settingsManager.getKey("SHOOT"));
        // P2
        assertEquals(KeyEvent.VK_UP, settingsManager.getKeyP2("UP"));
        assertEquals(KeyEvent.VK_DOWN, settingsManager.getKeyP2("DOWN"));
        assertEquals(KeyEvent.VK_LEFT, settingsManager.getKeyP2("LEFT"));
        assertEquals(KeyEvent.VK_RIGHT, settingsManager.getKeyP2("RIGHT"));
        assertEquals(KeyEvent.VK_ENTER, settingsManager.getKeyP2("SHOOT"));
    }

    @Test
    void testSetAndGetVolume() {
        settingsManager.setVolume(0.8f);
        assertEquals(0.8f, settingsManager.getVolume(), "getVolume should return the updated volume.");
        assertTrue(new File(SETTINGS_FILE).exists(), "settings.properties file should be created after setting volume.");
    }

    @Test
    void testSetAndGetKey() {
        settingsManager.setKey("UP", KeyEvent.VK_Z);
        assertEquals(KeyEvent.VK_Z, settingsManager.getKey("UP"), "getKey should return the updated key for P1.");
        assertTrue(new File(SETTINGS_FILE).exists(), "settings.properties file should be created after setting a key.");
    }

    @Test
    void testSetAndGetKeyP2() {
        settingsManager.setKeyP2("SHOOT", KeyEvent.VK_SHIFT);
        assertEquals(KeyEvent.VK_SHIFT, settingsManager.getKeyP2("SHOOT"), "getKeyP2 should return the updated key for P2.");
        assertTrue(new File(SETTINGS_FILE).exists(), "settings.properties file should be created after setting a P2 key.");
    }

    @Test
    void testGetUnknownKey() {
        assertEquals(-1, settingsManager.getKey("UNKNOWN_ACTION"), "getKey should return -1 for an unknown action.");
        assertEquals(-1, settingsManager.getKeyP2("UNKNOWN_ACTION"), "getKeyP2 should return -1 for an unknown action.");
    }

    @Test
    void testGetKeyBindings() {
        assertNotNull(settingsManager.getKeyBindings(), "getKeyBindings should not return null.");
        assertEquals(5, settingsManager.getKeyBindings().size(), "Default key bindings for P1 should have 5 actions.");
    }

    @Test
    void testGetKeyBindingsP2() {
        assertNotNull(settingsManager.getKeyBindingsP2(), "getKeyBindingsP2 should not return null.");
        assertEquals(5, settingsManager.getKeyBindingsP2().size(), "Default key bindings for P2 should have 5 actions.");
    }
}