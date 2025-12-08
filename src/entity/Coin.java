package entity;

import java.awt.Color;
import engine.DrawManager;

/**
 * Implements a Coin entity.
 */
public class Coin extends Entity {

    /**
     * Constructor, establishes the entity's generic properties.
     *
     * @param x Initial position of the coin in the X axis.
     * @param y Initial position of the coin in the Y axis.
     */
    public Coin(int x, int y) {
        // 12x12 size, Yellow color
        super(x, y, 12, 12, Color.YELLOW);
    }

    /**
     * Draws the coin on the screen.
     *
     * @param dm DrawManager instance responsible for rendering.
     */
    public void draw(DrawManager dm) {
        // Draws a simple yellow rectangle to represent the coin.
        // If a sprite is needed later, spriteType can be set here.
        dm.drawRectangle(positionX, positionY, width, height, Color.YELLOW);
    }
}