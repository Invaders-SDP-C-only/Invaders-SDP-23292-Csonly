package entity;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class Boss4Test {

    @Test
    void startSpellCardActivatesAndMovesState() throws Exception {
        Boss4 boss = new Boss4(10, 20, 300, 200);

        boss.startSpellCard("TestCard");

        assertTrue(boss.isSpellCardActive());
        assertEquals("TestCard", getPrivateString(boss, "currentSpellCardName"));
        assertNotEquals(getPrivateEnum(boss, "currentMovementState").name(), "STATIONARY");
    }

    @Test
    void destroySetsDestroyedFlag() {
        Boss4 boss = new Boss4(0, 0, 200, 200);

        boss.destroy();

        assertTrue(boss.isDestroyed());
    }

    private String getPrivateString(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String) field.get(target);
    }

    private Enum<?> getPrivateEnum(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Enum<?>) field.get(target);
    }
}
