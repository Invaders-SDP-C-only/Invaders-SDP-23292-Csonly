package entity;

import java.awt.Color;
import java.util.Random;
import engine.DrawManager;

public class ExplosionEntity extends Entity {

    private static final long DURATION = 300;

    private long startTime;

    private boolean isFinished = false;

    private static final Random random = new Random();

    public ExplosionEntity(final int x, final int y) {
        super(x, y, 13 * 2, 7 * 2, Color.WHITE);
        
        this.spriteType = DrawManager.SpriteType.Explosion;
        
        int colorType = random.nextInt(3);
        if (colorType == 0) {
            this.setColor(Color.ORANGE);
        } else if (colorType == 1) {
            this.setColor(Color.YELLOW);
        } else {
            this.setColor(Color.RED);
        }

        this.startTime = System.currentTimeMillis();
    }

    public void update() {
        long now = System.currentTimeMillis();
        if (now - this.startTime > DURATION) {
            this.isFinished = true;
        }
    }

    public boolean isFinished() {
        return this.isFinished;
    }
}
