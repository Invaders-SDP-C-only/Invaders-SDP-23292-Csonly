package engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScoreTest {

    @Test
    void constructorAndGetters_full() {
        Score score = new Score("Player1", 100, 1, 10, 20, 0.5f);
        assertEquals("Player1", score.getName());
        assertEquals(100, score.getScore());
        assertEquals(1, score.getStage());
        assertEquals(10, score.getKilled());
        assertEquals(20, score.getBullets());
        assertEquals(0.5f, score.getAccuracy());
    }

    @Test
    void constructorAndGetters_simple() {
        Score score = new Score("Player2", 200);
        assertEquals("Player2", score.getName());
        assertEquals(200, score.getScore());
        // Other fields will have default values (0, 0.0f)
        assertEquals(0, score.getStage());
        assertEquals(0, score.getKilled());
        assertEquals(0, score.getBullets());
        assertEquals(0.0f, score.getAccuracy());
    }

    @Test
    void compareTo_higherScore() {
        Score score1 = new Score("A", 200);
        Score score2 = new Score("B", 100);
        assertTrue(score1.compareTo(score2) < 0, "Score1 should be greater than Score2");
    }

    @Test
    void compareTo_lowerScore() {
        Score score1 = new Score("A", 100);
        Score score2 = new Score("B", 200);
        assertTrue(score1.compareTo(score2) > 0, "Score1 should be less than Score2");
    }

    @Test
    void compareTo_equalScore_higherAccuracy() {
        Score score1 = new Score("A", 100, 1, 10, 20, 0.8f);
        Score score2 = new Score("B", 100, 1, 10, 25, 0.6f);
        assertTrue(score1.compareTo(score2) < 0, "Score1 should be greater than Score2 due to higher accuracy");
    }
    
    @Test
    void compareTo_equalScore_lowerAccuracy() {
        Score score1 = new Score("A", 100, 1, 10, 25, 0.6f);
        Score score2 = new Score("B", 100, 1, 10, 20, 0.8f);
        assertTrue(score1.compareTo(score2) > 0, "Score1 should be less than Score2 due to lower accuracy");
    }

    @Test
    void compareTo_equalScore_equalAccuracy() {
        Score score1 = new Score("A", 100, 1, 10, 20, 0.8f);
        Score score2 = new Score("B", 100, 1, 12, 22, 0.8f);
        assertEquals(0, score1.compareTo(score2), "Scores should be equal");
    }
}
