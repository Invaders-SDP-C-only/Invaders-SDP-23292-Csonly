package engine.level;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class LevelEnemyFormationTest {

    @Test
    void constructorAndGetters() {
        Map<String, Object> map = new HashMap<>();
        map.put("formationWidth", 8);
        map.put("formationHeight", 6);
        map.put("baseSpeed", 50);
        map.put("shootingFrecuency", 1000);

        LevelEnemyFormation formation = new LevelEnemyFormation(map);

        assertEquals(8, formation.getFormationWidth());
        assertEquals(6, formation.getFormationHeight());
        assertEquals(50, formation.getBaseSpeed());
        assertEquals(1000, formation.getShootingFrecuency());
    }
}
