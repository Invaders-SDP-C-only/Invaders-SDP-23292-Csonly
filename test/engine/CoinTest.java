package engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoinTest {

    @Test
    void constructorAndGetters() {
        String testName = "Player1";
        int testCoin = 100;
        Coin coin = new Coin(testName, testCoin);

        assertEquals(testName, coin.getName(), "getName() should return the correct name.");
        assertEquals(testCoin, coin.getCoin(), "getCoin() should return the correct coin value.");
    }
}
