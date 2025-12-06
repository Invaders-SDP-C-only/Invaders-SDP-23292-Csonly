package entity;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityTest {

    @Test
    void constructorSetsBaseFields() {
        Entity entity = new Entity(5, 10, 20, 30, Color.RED);

        assertEquals(5, entity.getPositionX());
        assertEquals(10, entity.getPositionY());
        assertEquals(20, entity.getWidth());
        assertEquals(30, entity.getHeight());
        assertEquals(Color.RED, entity.getColor());
    }

    @Test
    void settersUpdatePositionAndColor() {
        Entity entity = new Entity(0, 0, 10, 10, Color.WHITE);

        entity.setPositionX(15);
        entity.setPositionY(25);
        entity.setColor(Color.BLUE);

        assertEquals(15, entity.getPositionX());
        assertEquals(25, entity.getPositionY());
        assertEquals(Color.BLUE, entity.getColor());
    }
}
