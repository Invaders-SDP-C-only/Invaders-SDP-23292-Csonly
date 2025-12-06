package engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameSettingsTest {

    @Test
    void constructorAndGetters() {
        int formationWidth = 10;
        int formationHeight = 5;
        int baseSpeed = 100;
        int shootingFrequency = 500;

        GameSettings settings = new GameSettings(formationWidth, formationHeight, baseSpeed, shootingFrequency);

        assertEquals(formationWidth, settings.getFormationWidth());
        assertEquals(formationHeight, settings.getFormationHeight());
        assertEquals(baseSpeed, settings.getBaseSpeed());
        assertEquals(shootingFrequency, settings.getShootingFrecuency());
    }
}
