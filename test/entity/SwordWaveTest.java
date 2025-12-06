package entity;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SwordWaveTest {

    @Test
    void updateMovesDownByWaveSpeed() {
        SwordWave wave = new SwordWave(0, 0, 10, 10, Color.WHITE);

        wave.update();

        assertEquals(3, wave.getPositionY());
    }
}
