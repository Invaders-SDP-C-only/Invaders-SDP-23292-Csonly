package entity;

import engine.DrawManager;
import java.awt.Color;

/**
 *  sword wave entity fired by Samurai Boss.
 */
public class SwordWave extends Entity {

    /** Y-axis movement speed of wave. */
    private static final int WAVE_SPEED_Y = 3;

    /**
     * SwordWave parameter
     *
     * @param positionX launch position X
     * @param positionY launch position Y
     * @param width     width of wave
     * @param height    height of wave
     * @param color     color of wave
     */
    public SwordWave(int positionX, int positionY, int width, int height, Color color) {
        super(positionX, positionY, width, height, color);
        this.spriteType = DrawManager.SpriteType.SwordWave;
    }

    /**
     * Move the wavelength from each frame.
     */
    public void update() {
        this.positionY += WAVE_SPEED_Y;
    }
}