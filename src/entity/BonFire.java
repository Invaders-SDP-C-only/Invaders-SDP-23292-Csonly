package entity;

import engine.DrawManager;
import engine.Room;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Implements the Bonfire entity.
 * This entity acts as a shop portal or rest point in the Sandbox mode.
 * It renders a sprite if available, otherwise falls back to a colored rectangle.
 */
public class BonFire extends Entity {

    /** Sprite image for the bonfire. */
    private BufferedImage sprite;

    /**
     * Constructor, establishes the entity's generic properties.
     *
     * @param x      Initial X position.
     * @param y      Initial Y position.
     * @param width  Width of the entity.
     * @param height Height of the entity.
     */
    public BonFire(int x, int y, int width, int height) {
        super(x, y, width, height, Color.YELLOW); // Default fallback color is YELLOW
        this.sprite = null;
    }

    /**
     * Sets the sprite image for this entity.
     *
     * @param image The BufferedImage to be rendered.
     */
    public void setSprite(BufferedImage image) {
        this.sprite = image;
    }

    /**
     * Draws the entity on the screen.
     * @param dm DrawManager instance responsible for rendering.
     */
    public void draw(DrawManager dm) {
        if (this.sprite != null) {
            // Draw the sprite image if loaded
            dm.getBackBufferGraphics().drawImage(this.sprite, getPositionX(), getPositionY(), getWidth(), getHeight(), null);
        } else {
            // Draw fallback rectangle if image is missing
            dm.drawRectangle(getPositionX(), getPositionY(), getWidth(), getHeight(), getColor());
        }
    }
}