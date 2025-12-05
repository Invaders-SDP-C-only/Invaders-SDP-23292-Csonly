package engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.awt.event.KeyEvent;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InputManagerTest {

    private InputManager inputManager;
    private GameSettingsManager gameSettingsManager;

    @BeforeEach
    void setUp() {
        // We need to get the singleton instance for testing.
        inputManager = InputManager.getInstance();
        gameSettingsManager = GameSettingsManager.getInstance();

        // Reset key states before each test.
        for (int i = 0; i < 256; i++) {
            if (inputManager.isKeyDown(i)) {
                KeyEvent keyEvent = new KeyEvent(mock(java.awt.Component.class), KeyEvent.KEY_RELEASED, 0, 0, i, (char) i);
                inputManager.keyReleased(keyEvent);
            }
        }
        inputManager.clearLastKeyCode();
    }

    @Test
    void testGetInstance() {
        assertNotNull(inputManager, "getInstance should not return null.");
        InputManager anotherInstance = InputManager.getInstance();
        assertSame(inputManager, anotherInstance, "getInstance should return the same singleton instance.");
    }

    @Test
    void testKeyPressedAndIsKeyDown() {
        int keyCode = KeyEvent.VK_A;
        assertFalse(inputManager.isKeyDown(keyCode), "Key should not be down initially.");

        KeyEvent pressEvent = new KeyEvent(mock(java.awt.Component.class), KeyEvent.KEY_PRESSED, 0, 0, keyCode, 'a');
        inputManager.keyPressed(pressEvent);

        assertTrue(inputManager.isKeyDown(keyCode), "Key should be down after keyPressed event.");
    }

    @Test
    void testKeyReleased() {
        int keyCode = KeyEvent.VK_B;
        KeyEvent pressEvent = new KeyEvent(mock(java.awt.Component.class), KeyEvent.KEY_PRESSED, 0, 0, keyCode, 'b');
        inputManager.keyPressed(pressEvent);
        assertTrue(inputManager.isKeyDown(keyCode), "Key should be down after press.");

        KeyEvent releaseEvent = new KeyEvent(mock(java.awt.Component.class), KeyEvent.KEY_RELEASED, 0, 0, keyCode, 'b');
        inputManager.keyReleased(releaseEvent);

        assertFalse(inputManager.isKeyDown(keyCode), "Key should not be down after keyReleased event.");
    }

    @Test
    void testLastKeyCode() {
        assertEquals(-1, inputManager.getLastKeyCode(), "Last key code should be -1 initially.");

        int firstKeyCode = KeyEvent.VK_C;
        KeyEvent firstPress = new KeyEvent(mock(java.awt.Component.class), KeyEvent.KEY_PRESSED, 0, 0, firstKeyCode, 'c');
        inputManager.keyPressed(firstPress);
        assertEquals(firstKeyCode, inputManager.getLastKeyCode(), "Last key code should be updated after a key press.");

        int secondKeyCode = KeyEvent.VK_D;
        KeyEvent secondPress = new KeyEvent(mock(java.awt.Component.class), KeyEvent.KEY_PRESSED, 0, 0, secondKeyCode, 'd');
        inputManager.keyPressed(secondPress);
        assertEquals(secondKeyCode, inputManager.getLastKeyCode(), "Last key code should be updated to the most recent key press.");
    }

    @Test
    void testClearLastKeyCode() {
        int keyCode = KeyEvent.VK_E;
        KeyEvent pressEvent = new KeyEvent(mock(java.awt.Component.class), KeyEvent.KEY_PRESSED, 0, 0, keyCode, 'e');
        inputManager.keyPressed(pressEvent);
        assertNotEquals(-1, inputManager.getLastKeyCode(), "Last key code should be set.");

        inputManager.clearLastKeyCode();
        assertEquals(-1, inputManager.getLastKeyCode(), "Last key code should be -1 after clearing.");
    }
}