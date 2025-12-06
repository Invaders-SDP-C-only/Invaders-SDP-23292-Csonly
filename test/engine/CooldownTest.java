package engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CooldownTest {

    @Test
    void checkFinished_initial() {
        Cooldown cd = new Cooldown(100);
        assertTrue(cd.checkFinished(), "Cooldown should be finished initially.");
    }

    @Test
    void checkFinished_afterReset() {
        Cooldown cd = new Cooldown(100);
        cd.reset();
        assertFalse(cd.checkFinished(), "Cooldown should not be finished immediately after reset.");
    }

    @Test
    void checkFinished_afterWait() throws InterruptedException {
        int cooldownTime = 100;
        Cooldown cd = new Cooldown(cooldownTime);
        cd.reset();
        Thread.sleep(cooldownTime + 50); // Wait for cooldown to expire
        assertTrue(cd.checkFinished(), "Cooldown should be finished after waiting.");
    }
    
    @Test
    void setMilliseconds() throws InterruptedException {
        Cooldown cd = new Cooldown(200);
        cd.reset();
        assertFalse(cd.checkFinished());
        cd.setMilliseconds(50);
        Thread.sleep(100);
        assertTrue(cd.checkFinished(), "Cooldown should have finished earlier after setMilliseconds.");
    }
    
    @Test
    void varianceConstructor() {
        // This test is hard to make deterministic because of Math.random().
        // We can't directly check the duration.
        // We can only check that it doesn't throw an error.
        assertDoesNotThrow(() -> {
            Cooldown cd = new Cooldown(100, 50);
            cd.reset();
        });
    }
}
