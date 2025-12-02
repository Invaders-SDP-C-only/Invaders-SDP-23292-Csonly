package engine;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class CoreTest {

    @Test
    void getLogger() {
        // The logger is statically initialized, so it should not be null.
        // Configuration happens in main(), but the instance should exist.
        Logger logger = Core.getLogger();
        assertNotNull(logger, "getLogger() should not return null.");
    }

    @Test
    @Disabled
    void getDrawManager() {
        DrawManager drawManager = Core.getDrawManager();
        assertNotNull(drawManager, "getDrawManager() should not return null.");
        // Test singleton property
        assertSame(drawManager, Core.getDrawManager(), "getDrawManager() should always return the same instance.");
    }

    @Test
    void getInputManager() {
        InputManager inputManager = Core.getInputManager();
        assertNotNull(inputManager, "getInputManager() should not return null.");
        // Test singleton property
        assertSame(inputManager, Core.getInputManager(), "getInputManager() should always return the same instance.");
    }

    @Test
    void getFileManager() {
        FileManager fileManager = Core.getFileManager();
        assertNotNull(fileManager, "getFileManager() should not return null.");
        // Test singleton property
        assertSame(fileManager, Core.getFileManager(), "getFileManager() should always return the same instance.");
    }

    @Test
    void getCooldown() {
        Cooldown cooldown = Core.getCooldown(1000);
        assertNotNull(cooldown, "getCooldown() should return a new Cooldown instance.");
        Cooldown anotherCooldown = Core.getCooldown(1000);
        assertNotSame(cooldown, anotherCooldown, "getCooldown() should return a new instance each time.");
    }

    @Test
    void getVariableCooldown() {
        Cooldown cooldown = Core.getVariableCooldown(1000, 100);
        assertNotNull(cooldown, "getVariableCooldown() should return a new Cooldown instance.");
        Cooldown anotherCooldown = Core.getVariableCooldown(1000, 100);
        assertNotSame(cooldown, anotherCooldown, "getVariableCooldown() should return a new instance each time.");
    }
}