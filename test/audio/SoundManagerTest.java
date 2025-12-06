package audio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SoundManagerTest {

    private float originalVolume;

    @BeforeEach
    void setUp() {
        originalVolume = SoundManager.getVolume();
        // Ensure sound is not muted before each test
        SoundManager.uncutAllSound();
    }

    @AfterEach
    void tearDown() {
        // Restore original volume
        SoundManager.setVolume(originalVolume);
        SoundManager.stopAll();
        // Reset mute state
        SoundManager.uncutAllSound();
    }

    @Test
    void testSetAndGetVolume() {
        SoundManager.setVolume(0.75f);
        assertEquals(0.75f, SoundManager.getVolume(), 0.001, "Volume should be set to 0.75");

        SoundManager.setVolume(0.0f);
        assertEquals(0.0f, SoundManager.getVolume(), 0.001, "Volume should be set to 0.0");

        SoundManager.setVolume(1.0f);
        assertEquals(1.0f, SoundManager.getVolume(), 0.001, "Volume should be set to 1.0");
    }

    @Test
    void testVolumeClamping() {
        SoundManager.setVolume(1.5f);
        assertEquals(1.0f, SoundManager.getVolume(), 0.001, "Volume should be clamped to 1.0");

        SoundManager.setVolume(-0.5f);
        assertEquals(0.0f, SoundManager.getVolume(), 0.001, "Volume should be clamped to 0.0");
    }
}
