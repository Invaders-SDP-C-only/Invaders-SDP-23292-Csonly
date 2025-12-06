package engine.level;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class EnemyTypeTest {

    @Test
    void constructorAndGetters() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "EnemyA");
        map.put("count", 10);

        EnemyType enemyType = new EnemyType(map);

        assertEquals("EnemyA", enemyType.getType());
        assertEquals(10, enemyType.getCount());
    }
}
