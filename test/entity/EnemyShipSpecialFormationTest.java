package entity;

import engine.Cooldown;
import engine.GameSettings;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

class EnemyShipSpecialFormationTest {

    @Test
    void constructorCreatesTwoSpecialShips() {
        GameSettings settings = new GameSettings(1, 1, 8, 1000);
        Cooldown activeCooldown = engine.Core.getCooldown(0);
        Cooldown explosionCooldown = engine.Core.getCooldown(0);

        EnemyShipSpecialFormation formation = new EnemyShipSpecialFormation(settings, activeCooldown, explosionCooldown);

        int count = 0;
        for (EnemyShip ignored : formation) {
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    void destroyMarksShipDestroyed() throws Exception {
        GameSettings settings = new GameSettings(1, 1, 8, 1000);
        Cooldown activeCooldown = engine.Core.getCooldown(0);
        Cooldown explosionCooldown = engine.Core.getCooldown(0);
        EnemyShipSpecialFormation formation = new EnemyShipSpecialFormation(settings, activeCooldown, explosionCooldown);

        EnemyShip red = getShipByColor(formation, java.awt.Color.RED);
        assertNotNull(red);
        assertFalse(red.isDestroyed());

        formation.destroy(red);

        assertTrue(red.isDestroyed());
    }

    private EnemyShip getShipByColor(EnemyShipSpecialFormation formation, java.awt.Color color) throws Exception {
        Field redField = EnemyShipSpecialFormation.class.getDeclaredField("enemyShipSpecialRed");
        Field blueField = EnemyShipSpecialFormation.class.getDeclaredField("enemyShipSpecialBlue");
        redField.setAccessible(true);
        blueField.setAccessible(true);
        EnemyShip red = (EnemyShip) redField.get(formation);
        EnemyShip blue = (EnemyShip) blueField.get(formation);
        if (red != null && red.getColor().equals(color)) {
            return red;
        }
        if (blue != null && blue.getColor().equals(color)) {
            return blue;
        }
        return null;
    }
}
