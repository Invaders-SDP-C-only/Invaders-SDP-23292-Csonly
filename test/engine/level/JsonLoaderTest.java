package engine.level;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonLoaderTest {

    @Test
    void parse_validJson() throws IOException {
        String json = "{\"levels\": [{\"level\": 1, \"levelName\": \"First Level\"}]}";
        List<Level> levels = JsonLoader.parse(json);

        assertNotNull(levels);
        assertEquals(1, levels.size());
        Level level1 = levels.get(0);
        assertEquals(1, level1.getLevel());
        assertEquals("First Level", level1.getLevelName());
    }

    @Test
    void parse_multipleLevels() throws IOException {
        String json = "{\"levels\": [{\"level\": 1}, {\"level\": 2}]}";
        List<Level> levels = JsonLoader.parse(json);
        assertEquals(2, levels.size());
        assertEquals(1, levels.get(0).getLevel());
        assertEquals(2, levels.get(1).getLevel());
    }

    @Test
    void parse_complexLevel() throws IOException {
        String json = "{\"levels\": [{"
                + "\"level\": 3,"
                + "\"levelName\": \"Complex Level with \\\"Quotes\\\" and \\nNewline\","
                + "\"enemyFormation\": {\"formationWidth\": 5, \"formationHeight\": 4, \"baseSpeed\": 10, \"shootingFrecuency\": 100},"
                + "\"enemyTypes\": [{\"type\": \"A\", \"count\": 10}, {\"type\": \"B\", \"count\": 5}],"
                + "\"itemDrops\": [{\"enemyType\": \"C\", \"itemId\": \"Shield\", \"dropChance\": 0.5}],"
                + "\"bossId\": \"FinalBoss\","
                + "\"completionBonus\": {\"currency\": 100},"
                + "\"achievementTrigger\": \"BOSS_KILL\","
                + "\"specialGimmick\": \"Speed Boost\","
                + "\"unlockCondition\": \"Level 2 Cleared\","
                + "\"isBonusLevel\": true,"
                + "\"optionalField\": null"
                + "}]}";

        List<Level> levels = JsonLoader.parse(json);
        assertEquals(1, levels.size());
        Level level = levels.get(0);
        assertEquals(3, level.getLevel());
        assertEquals("Complex Level with \"Quotes\" and \nNewline", level.getLevelName());
        assertNotNull(level.getEnemyFormation());
        assertEquals(5, level.getEnemyFormation().getFormationWidth());
        assertEquals(2, level.getEnemyTypes().size());
        assertEquals("A", level.getEnemyTypes().get(0).getType());
        assertEquals(1, level.getItemDrops().size());
        assertEquals("Shield", level.getItemDrops().get(0).getItemId());
        assertEquals("FinalBoss", level.getBossId());
        assertNotNull(level.getCompletionBonus());
        assertEquals(100, level.getCompletionBonus().getCurrency());
        assertEquals("BOSS_KILL", level.getAchievementTrigger());
        assertEquals("Speed Boost", level.getSpecialGimmick());
        assertEquals("Level 2 Cleared", level.getUnlockCondition());
    }

    @Test
    void parse_invalidJson_throwsException() {
        final String invalidJson1 = "{\"levels\": [{\"level\": 1, }]"; // trailing comma
        assertThrows(IOException.class, () -> JsonLoader.parse(invalidJson1));
        final String invalidJson2 = "{\"levels\": [{\"level\": 1, \"name\":}"; // missing value
        assertThrows(IOException.class, () -> JsonLoader.parse(invalidJson2));
        final String invalidJson3 = "{\"levels\": [{invalid}]}"; // invalid object
        assertThrows(IOException.class, () -> JsonLoader.parse(invalidJson3));
        final String invalidJson4 = "{\"levels\": [\"string\"]}"; // array of string, not object for Level
        assertThrows(IOException.class, () -> JsonLoader.parse(invalidJson4));

        // Malformed number
        final String invalidJson5 = "{\"levels\": [{\"value\": 1.2.3}]}";
        assertThrows(IOException.class, () -> JsonLoader.parse(invalidJson5));
        final String invalidJson6 = "{\"levels\": [{\"value\": -}]}";
        assertThrows(IOException.class, () -> JsonLoader.parse(invalidJson6));

        // Malformed boolean
        final String invalidJson7 = "{\"levels\": [{\"value\": tru}]}";
        assertThrows(IOException.class, () -> JsonLoader.parse(invalidJson7));
        final String invalidJson8 = "{\"levels\": [{\"value\": false_}]}";
        assertThrows(IOException.class, () -> JsonLoader.parse(invalidJson8));

        // Malformed null
        final String invalidJson9 = "{\"levels\": [{\"value\": nul_}]}";
        assertThrows(IOException.class, () -> JsonLoader.parse(invalidJson9));

        // Unclosed string
        final String invalidJson10 = "{\"levels\": [{\"value\": \"unclosed}]}";
        assertThrows(IOException.class, () -> JsonLoader.parse(invalidJson10));
    }

    @Test
    void parse_emptyLevels_returnsEmptyList() throws IOException {
        String json = "{\"levels\": []}";
        List<Level> levels = JsonLoader.parse(json);
        assertTrue(levels.isEmpty());
    }

    @Test
    void parse_noLevelsKey_throwsException() {
        String json = "{\"data\": []}";
        assertThrows(IOException.class, () -> JsonLoader.parse(json));
    }

    @Test
    void parse_emptyObject(){
        // The "level" field is mandatory for the Level constructor.
        // This test ensures that an empty object can be parsed as a value for another field.
        String json = "{\"levels\": [{\"level\": 1, \"empty\": {}}]}";
        assertDoesNotThrow(() -> JsonLoader.parse(json));
    }

    @Test
    void parse_emptyArray(){
        // The "level" field is mandatory for the Level constructor.
        // This test ensures that an empty array can be parsed as a value for another field.
        String json = "{\"levels\": [{\"level\": 1, \"empty\": []}]}";
        assertDoesNotThrow(() -> JsonLoader.parse(json));
    }

    @Test
    void parse_stringWithAllEscapes() throws IOException {
        String json = "{\"levels\": [{\"level\": 1, \"levelName\": \"\\\" \\\\ \\/ \\b \\f \\n \\r \\t\"}]}";
        List<Level> levels = JsonLoader.parse(json);
        assertEquals(1, levels.size());
        assertEquals("\" \\ / \b \f \n \r \t", levels.get(0).getLevelName());
    }

    @Test
    void parse_unclosedObject() {
        String json = "{\"levels\": [{\"level\": 1, \"name\": \"test\""; // Missing closing brace
        assertThrows(IOException.class, () -> JsonLoader.parse(json));
    }

    @Test
    void parse_unclosedArray() {
        String json = "{\"levels\": [{\"level\": 1}"; // Missing closing bracket
        assertThrows(IOException.class, () -> JsonLoader.parse(json));
    }
}