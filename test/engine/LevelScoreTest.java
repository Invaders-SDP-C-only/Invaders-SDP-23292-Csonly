package engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LevelScoreTest {

    private LevelScore levelScore;
    private Map<String, Object> scoringCriteria;

    @BeforeEach
    void setUp() {
        levelScore = new LevelScore();
        scoringCriteria = new HashMap<>();
        List<Map<String, Object>> timeBonusTiers = new ArrayList<>();

        Map<String, Object> tier1 = new HashMap<>();
        tier1.put("time", 30);
        tier1.put("bonus", 200);
        timeBonusTiers.add(tier1);

        Map<String, Object> tier2 = new HashMap<>();
        tier2.put("time", 60);
        tier2.put("bonus", 100);
        timeBonusTiers.add(tier2);
        
        // Tiers should be sorted by time ascending for the logic to work correctly,
        // although the current implementation just takes the first one that matches.
        // The implementation iterates and the first match returns, so order matters.
        // The tiers should be ordered from fastest to slowest time.

        scoringCriteria.put("timeBonus", timeBonusTiers);
    }

    @Test
    void calculateTimeBonus_fastestTier() {
        int bonus = levelScore.calculateTimeBonus(scoringCriteria, 25); // Faster than 30s
        assertEquals(200, bonus);
    }

    @Test
    void calculateTimeBonus_secondTier() {
        int bonus = levelScore.calculateTimeBonus(scoringCriteria, 55); // Faster than 60s, slower than 30s
        assertEquals(100, bonus);
    }

    @Test
    void calculateTimeBonus_slowest() {
        int bonus = levelScore.calculateTimeBonus(scoringCriteria, 70); // Slower than 60s
        assertEquals(0, bonus);
    }
    
    @Test
    void calculateTimeBonus_exactBoundary() {
        int bonus = levelScore.calculateTimeBonus(scoringCriteria, 30); // Exactly 30s
        assertEquals(200, bonus);
    }

    @Test
    void calculateTimeBonus_nullCriteria() {
        int bonus = levelScore.calculateTimeBonus(null, 50);
        assertEquals(0, bonus);
    }

    @Test
    void calculateTimeBonus_noTimeBonusKey() {
        int bonus = levelScore.calculateTimeBonus(new HashMap<>(), 50);
        assertEquals(0, bonus);
    }
    
    @Test
    void calculateTimeBonus_nullTiers() {
        scoringCriteria.put("timeBonus", null);
        int bonus = levelScore.calculateTimeBonus(scoringCriteria, 50);
        assertEquals(0, bonus);
    }

    @Test
    void calculateFinalScore() {
        int finalScore = levelScore.calculateFinalScore(1000, scoringCriteria, 28);
        assertEquals(1200, finalScore, "Base score 1000 + bonus 200");
    }
    
    @Test
    void calculateFinalScore_noBonus() {
        int finalScore = levelScore.calculateFinalScore(1000, scoringCriteria, 90);
        assertEquals(1000, finalScore, "Base score 1000 + bonus 0");
    }
}
