package engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameTimerTest {

    @Test
    void initialState() {
        GameTimer timer = new GameTimer();
        assertFalse(timer.isRunning(), "Timer should not be running initially.");
        // Elapsed time is not well-defined before starting, but let's check it doesn't crash.
        assertDoesNotThrow(timer::getElapsedTime);
    }

    @Test
    void start() {
        GameTimer timer = new GameTimer();
        timer.start();
        assertTrue(timer.isRunning(), "Timer should be running after start().");
    }

    @Test
    void stop() {
        GameTimer timer = new GameTimer();
        timer.start();
        timer.stop();
        assertFalse(timer.isRunning(), "Timer should not be running after stop().");
    }
    
    @Test
    void stop_notRunning() {
        GameTimer timer = new GameTimer();
        timer.stop();
        assertFalse(timer.isRunning(), "Stopping a non-running timer should not change its state.");
    }

    @Test
    void getElapsedTime() throws InterruptedException {
        GameTimer timer = new GameTimer();
        timer.start();
        Thread.sleep(100);
        timer.stop();
        long elapsedTime = timer.getElapsedTime();
        assertTrue(elapsedTime >= 100 && elapsedTime < 150, "Elapsed time should be around 100ms.");

        // Check if elapsed time is stable after stop
        long firstRead = timer.getElapsedTime();
        Thread.sleep(50);
        long secondRead = timer.getElapsedTime();
        assertEquals(firstRead, secondRead, "Elapsed time should not change after the timer is stopped.");
    }
    
    @Test
    void getElapsedTime_whileRunning() throws InterruptedException {
        GameTimer timer = new GameTimer();
        timer.start();
        Thread.sleep(50);
        long firstRead = timer.getElapsedTime();
        assertTrue(firstRead >= 50 && firstRead < 100);
        Thread.sleep(50);
        long secondRead = timer.getElapsedTime();
        assertTrue(secondRead > firstRead, "Elapsed time should increase while the timer is running.");
        timer.stop();
    }
}
