package engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.awt.event.KeyEvent;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InputManagerTest {

    private InputManager inputManager;

    @BeforeEach
    void setUp() {
        // Since InputManager is a singleton, we get the instance.
        // We need to reset its state before each test.
        inputManager = InputManager.getInstance();
        // A way to reset the keys array is needed. Since there is no public reset method,
        // we can use reflection, or just press and release a bunch of keys to get a known state.
        // For simplicity, we'll rely on the fact that each test will set the keys it needs.
    }

    @Test
    void testGetInstance() {
        assertNotNull(inputManager);
        assertSame(inputManager, InputManager.getInstance());
    }

    @Test
    void testIsKeyDown_notPressed() {
        // Ensure the key is not pressed before the test
        KeyEvent keyEvent = new KeyEvent(mock(java.awt.Component.class), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_A, 'a');
        inputManager.keyReleased(keyEvent);
        assertFalse(inputManager.isKeyDown(KeyEvent.VK_A));
    }

    @Test
    void testKeyPressedAndIsKeyDown_validKey() {
        KeyEvent keyEvent = new KeyEvent(mock(java.awt.Component.class), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_B, 'b');
        inputManager.keyPressed(keyEvent);
        assertTrue(inputManager.isKeyDown(KeyEvent.VK_B));
    }

    @Test
    void testKeyReleased_validKey() {
        // First press the key
        KeyEvent pressEvent = new KeyEvent(mock(java.awt.Component.class), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_C, 'c');
        inputManager.keyPressed(pressEvent);
        assertTrue(inputManager.isKeyDown(KeyEvent.VK_C));

        // Then release it
        KeyEvent releaseEvent = new KeyEvent(mock(java.awt.Component.class), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_C, 'c');
        inputManager.keyReleased(releaseEvent);
        assertFalse(inputManager.isKeyDown(KeyEvent.VK_C));
    }

    @Test
    void testKeyPressed_invalidKey_outOfBounds() {
        // Test with a key code outside the valid range
        KeyEvent keyEvent = new KeyEvent(mock(java.awt.Component.class), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, 300, ' ');
        // The method should not throw an exception
        assertDoesNotThrow(() -> inputManager.keyPressed(keyEvent));
    }

    @Test
    void testKeyReleased_invalidKey_outOfBounds() {
        // Test with a key code outside the valid range
        KeyEvent keyEvent = new KeyEvent(mock(java.awt.Component.class), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, 300, ' ');
        // The method should not throw an exception
        assertDoesNotThrow(() -> inputManager.keyReleased(keyEvent));
    }
    
    @Test
    void testKeyTyped() {
        KeyEvent keyEvent = new KeyEvent(mock(java.awt.Component.class), KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, 'd');
        // The method is empty, so we just call it for completeness and coverage.
        assertDoesNotThrow(() -> inputManager.keyTyped(keyEvent));
    }
    
    @Test
    void testIsP1KeyDown() {
        KeyEvent keyEvent = new KeyEvent(mock(java.awt.Component.class), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_P, 'p');
        inputManager.keyPressed(keyEvent);
        assertTrue(inputManager.isP1KeyDown(KeyEvent.VK_P));
        
        KeyEvent releaseEvent = new KeyEvent(mock(java.awt.Component.class), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_P, 'p');
        inputManager.keyReleased(releaseEvent);
        assertFalse(inputManager.isP1KeyDown(KeyEvent.VK_P));
    }

    @Test
    void testIsP2KeyDown() {
        KeyEvent keyEvent = new KeyEvent(mock(java.awt.Component.class), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_Q, 'q');
        inputManager.keyPressed(keyEvent);
        assertTrue(inputManager.isP2KeyDown(KeyEvent.VK_Q));
        
        KeyEvent releaseEvent = new KeyEvent(mock(java.awt.Component.class), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_Q, 'q');
        inputManager.keyReleased(releaseEvent);
        assertFalse(inputManager.isP2KeyDown(KeyEvent.VK_Q));
    }
}
