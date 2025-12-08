package engine.level;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CompletionBonusTest {

    @Test
    void constructorAndGetter() {
        Map<String, Object> map = new HashMap<>();
        map.put("currency", 100);

        CompletionBonus bonus = new CompletionBonus(map);

        assertEquals(100, bonus.getCurrency());
    }

    @Test
    void constructorWithDifferentNumberType() {
        Map<String, Object> map = new HashMap<>();
        map.put("currency", 150.0); // Double

        CompletionBonus bonus = new CompletionBonus(map);

        assertEquals(150, bonus.getCurrency(), "Should handle floating point numbers by converting to int.");
    }
}
