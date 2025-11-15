package screen;

import java.awt.event.KeyEvent;
import engine.Core;
import engine.Cooldown;
import engine.DrawManager;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import entity.Entity;
import engine.DrawManager.SpriteType;
import screen.TitleScreen.Star;

public class ModeSelectScreen extends Screen {
    private static final float ROTATION_SPEED = 4.0f;
    private float currentAngle = 0f;
    private float targetAngle = 0f;
    private static final int SELECTION_TIME = 200;

    private final DrawManager drawManager;
    private List<Star> stars;
    private final Cooldown selectionCooldown;
    private int selection = 0;
    private String selectedMode = "1P";
    private boolean inputArmed = false;
    // ModeSelectScreen.java
    public static final String MODE_1P     = "1P";
    public static final String MODE_2P     = "2P";
    public static final String CANCEL_MODE = "CANCEL";

    // Enemy background (same feel as TitleScreen)
    private List<Entity> backgroundEnemies;
    private Cooldown enemySpawnCooldown;
    private Random random;

    private static final int ENEMY_SPAWN_COOLDOWN = 2000;
    private static final double ENEMY_SPAWN_CHANCE = 0.3;

    /** Simple background enemy used for the animated menu scene. */
    private static class BackgroundEnemy extends Entity {
        private final int speed;

        public BackgroundEnemy(int positionX, int positionY, int speed, SpriteType spriteType) {
            super(positionX, positionY, 12 * 2, 8 * 2, java.awt.Color.WHITE);
            this.speed = speed;
            this.spriteType = spriteType;
        }

        public int getSpeed() { return speed; }
    }

    public ModeSelectScreen(final int width, final int height, final int fps) {
        this(width, height, fps, null, 0f);
    }

    public ModeSelectScreen(final int width, final int height, final int fps,
                            final List<Star> sharedStars, final float startAngle) {
        super(width, height, fps);
        this.drawManager = Core.getDrawManager();
        this.selectionCooldown = Core.getCooldown(SELECTION_TIME);
        this.selectionCooldown.reset();

        // angle continuity
        this.currentAngle = startAngle;
        this.targetAngle  = startAngle;

        // Stars: reuse if provided, otherwise create new ones
        if (sharedStars != null) {
            this.stars = sharedStars;
        } else {
            this.stars = new ArrayList<>();
            for (int i = 0; i < 150; i++) {
                float x = (float) (Math.random() * width);
                float y = (float) (Math.random() * height);
                float speed = (float) (Math.random() * 2.5 + 0.5);
                this.stars.add(new Star(x, y, speed));
            }
        }

        // Background enemies animation (independent of TitleScreen enemies)
        this.backgroundEnemies = new ArrayList<Entity>();
        this.enemySpawnCooldown = Core.getCooldown(ENEMY_SPAWN_COOLDOWN);
        this.enemySpawnCooldown.reset();
        this.random = new Random();
    }

    @Override
    protected void update() {
        // --- Animate stars (falling + twinkle) ---
        for (Star star : this.stars) {
            star.baseY += star.speed;
            if (star.baseY > this.getHeight()) {
                star.baseY = 0;
                star.baseX = (float) (Math.random() * this.getWidth());
            }
            star.brightness = 0.5f + (float)
                    ((Math.sin(star.brightnessOffset + System.currentTimeMillis() / 500.0) + 1.0) / 4.0f);
        }
        if (this.enemySpawnCooldown.checkFinished()) {
            this.enemySpawnCooldown.reset();
            if (Math.random() < ENEMY_SPAWN_CHANCE) {
                SpriteType[] enemyTypes = {
                        SpriteType.EnemyShipA1,
                        SpriteType.EnemyShipB1,
                        SpriteType.EnemyShipC1
                };
                SpriteType randomEnemyType = enemyTypes[random.nextInt(enemyTypes.length)];
                int randomX = (int) (Math.random() * this.getWidth());
                int speed = random.nextInt(2) + 1;
                this.backgroundEnemies.add(new BackgroundEnemy(randomX, -20, speed, randomEnemyType));
            }
        }

        java.util.Iterator<Entity> enemyIterator = this.backgroundEnemies.iterator();
        while (enemyIterator.hasNext()) {
            BackgroundEnemy enemy = (BackgroundEnemy) enemyIterator.next();
            enemy.setPositionY(enemy.getPositionY() + enemy.getSpeed());
            if (enemy.getPositionY() > this.getHeight()) {
                enemyIterator.remove();
            }
        }

        super.update();

        if (!this.inputDelay.checkFinished()) {
            draw();
            return;
        }
        if (!this.inputArmed) {
            boolean enterDown = inputManager.isKeyDown(KeyEvent.VK_ENTER);
            boolean spaceDown = inputManager.isKeyDown(KeyEvent.VK_SPACE);
            if (!enterDown && !spaceDown) {
                this.inputArmed = true;
            }
            draw();
            return;
        }

        // ---- Navigation: once per cooldown, like TitleScreen ----
        if (this.selectionCooldown.checkFinished()) {
            if (inputManager.isKeyDown(KeyEvent.VK_UP) || inputManager.isKeyDown(KeyEvent.VK_W)) {
                this.selection = (this.selection - 1 + 3) % 3; // 0↔1↔2
                this.targetAngle -= 90;
                this.selectionCooldown.reset();
            } else if (inputManager.isKeyDown(KeyEvent.VK_DOWN) || inputManager.isKeyDown(KeyEvent.VK_S)) {
                this.selection = (this.selection + 1) % 3;
                this.targetAngle += 90;
                this.selectionCooldown.reset();
            }
        }

        if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
            this.selectedMode = "CANCEL";
            this.isRunning = false;
            return;
        }

        if (inputManager.isKeyDown(KeyEvent.VK_ENTER)
                || inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
            if (this.selection == 0) this.selectedMode = "1P";
            else if (this.selection == 1) this.selectedMode = "2P";
            else this.selectedMode = "CANCEL";
            this.isRunning = false;
            return;
        }

        if (this.currentAngle < this.targetAngle) {
            this.currentAngle = Math.min(this.currentAngle + ROTATION_SPEED, this.targetAngle);
        } else if (this.currentAngle > this.targetAngle) {
            this.currentAngle = Math.max(this.currentAngle - ROTATION_SPEED, this.targetAngle);
        }

        draw();
    }
    private void draw() {
        drawManager.initDrawing(this);
        drawManager.drawStars(this, this.stars, currentAngle);

        final double angleRad = Math.toRadians(this.currentAngle);
        final double cosAngle = Math.cos(angleRad);
        final double sinAngle = Math.sin(angleRad);
        final int centerX = this.getWidth() / 2;
        final int centerY = this.getHeight() / 2;

        for (Entity enemy : this.backgroundEnemies) {
            float relX = enemy.getPositionX() - centerX;
            float relY = enemy.getPositionY() - centerY;

            double rotatedX = relX * cosAngle - relY * sinAngle;
            double rotatedY = relX * sinAngle + relY * cosAngle;

            int screenX = (int) (rotatedX + centerX);
            int screenY = (int) (rotatedY + centerY);

            drawManager.drawEntity(enemy, screenX, screenY);
        }

        drawManager.drawModeSelectMenu(this, selection);
        drawManager.completeDrawing(this);
    }
    public String getSelectedMode() {
        return selectedMode;
    }
}