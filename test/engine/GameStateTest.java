package engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    private GameState gameState;

    @BeforeEach
    void setUp() {
        gameState = new GameState(1, 100, 3, 3, 50, 10, 200);
    }

    @Test
    void constructorAndGetters() {
        assertEquals(1, gameState.getLevel());
        assertEquals(100, gameState.getScore());
        assertEquals(3, gameState.getLivesRemaining());
        assertEquals(3, gameState.getLivesRemainingP2());
        assertEquals(50, gameState.getBulletsShot());
        assertEquals(10, gameState.getShipsDestroyed());
        assertEquals(200, gameState.getCoin());
    }

    @Test
    void deductCoins_sufficient() {
        assertTrue(gameState.deductCoins(50));
        assertEquals(150, gameState.getCoin());
    }

    @Test
    void deductCoins_insufficient() {
        assertFalse(gameState.deductCoins(250));
        assertEquals(200, gameState.getCoin());
    }

    @Test
    void deductCoins_exactAmount() {
        assertTrue(gameState.deductCoins(200));
        assertEquals(0, gameState.getCoin());
    }

    @Test
    void deductCoins_negativeAmount() {
        assertFalse(gameState.deductCoins(-50));
        assertEquals(200, gameState.getCoin());
    }
    
    @Test
    void deductCoins_zeroAmount() {
        assertTrue(gameState.deductCoins(0));
        assertEquals(200, gameState.getCoin());
    }

    @Test
    void addCoins_positiveAmount() {
        gameState.addCoins(50);
        assertEquals(250, gameState.getCoin());
    }

    @Test
    void addCoins_negativeAmount() {
        gameState.addCoins(-50);
        assertEquals(200, gameState.getCoin());
    }
    
    @Test
    void addCoins_zeroAmount() {
        gameState.addCoins(0);
        assertEquals(200, gameState.getCoin());
    }

    @Test
    void setCoins_positiveAmount() {
        gameState.setCoins(500);
        assertEquals(500, gameState.getCoin());
    }

    @Test
    void setCoins_zeroAmount() {
        gameState.setCoins(0);
        assertEquals(0, gameState.getCoin());
    }

    @Test
    void setCoins_negativeAmount() {
        gameState.setCoins(-100);
        assertEquals(200, gameState.getCoin());
    }
}
