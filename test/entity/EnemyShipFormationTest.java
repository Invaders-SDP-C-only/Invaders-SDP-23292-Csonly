package entity;

import engine.GameSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EnemyShipFormationTest {

    @BeforeEach
    void resetSharedState() {
        ShopItem.resetAllItems();
    }

    @Test
    void constructsWithExpectedShipCount() {
        GameSettings settings = new GameSettings(2, 2, 8, 1000);

        EnemyShipFormation formation = new EnemyShipFormation(settings);

        int count = 0;
        for (EnemyShip ignored : formation) {
            count++;
        }
        assertEquals(4, count);
        assertFalse(formation.isEmpty());
    }

    @Test
    void destroyAllMarksFormationEmpty() {
        GameSettings settings = new GameSettings(2, 2, 8, 1000);
        EnemyShipFormation formation = new EnemyShipFormation(settings);

        int destroyed = formation.destroyAll();

        assertEquals(4, destroyed);
        assertTrue(formation.isEmpty());
    }

    @Test
    void destroyUpdatesShootersList() {
        GameSettings settings = new GameSettings(1, 2, 8, 1000);
        EnemyShipFormation formation = new EnemyShipFormation(settings);
        setShootingCooldown(formation);
        Iterator<EnemyShip> iterator = formation.iterator();
        EnemyShip ship = iterator.next();

        formation.destroy(ship);

        Set<Bullet> bullets = new HashSet<>();
        formation.shoot(bullets); // should not throw even when shooter list shrinks
    }

    @Test
    void slowdownDoesNotBreakShooting() {
        GameSettings settings = new GameSettings(1, 1, 8, 1000);
        EnemyShipFormation formation = new EnemyShipFormation(settings);
        setShootingCooldown(formation);

        formation.activateSlowdown();
        Set<Bullet> bullets = new HashSet<>();
        formation.shoot(bullets); // should not throw while slowed

        // call shoot again after some cycles
        for (int i = 0; i < 30; i++) {
            formation.shoot(bullets);
        }
    }

    private void setShootingCooldown(EnemyShipFormation formation) {
        try {
            java.lang.reflect.Field f = EnemyShipFormation.class.getDeclaredField("shootingCooldown");
            f.setAccessible(true);
            f.set(formation, engine.Core.getCooldown(0));
        } catch (Exception ignored) {
        }
    }
}
