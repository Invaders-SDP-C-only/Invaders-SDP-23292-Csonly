package entity;

import audio.SoundManager;
import engine.Cooldown;
import engine.Core;
import engine.DrawManager;
import engine.FileManager;
import screen.Screen;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Implements the Samurai Boss entity.
 * This boss features complex patterns, including melee rushes, wave attacks, and a "posture" system similar to Sekiro.
 *
 */
public class SamuraiBoss extends Entity implements BossEntity {

    /** Boss state definition. */
    private enum BossState { NORMAL, RUSHING, PAUSE, RETREATING, STUN, BROKEN, FAIL, DANMAKU }
    private BossState currentState;
    private Logger logger;
    private Ship player1;
    private Ship player2;
    private Screen screen;

    /** Boss status (Health & Posture). */
    private int health = 3000;
    private int maxHealth = 3000;
    private int posture = 0;
    private int maxPosture = 500;
    private int pointValue = 5000;
    private boolean isfailed = false;

    /** Phase 2 (Enraged Mode) flags. */
    private boolean isEnraged = false;
    private boolean enrageTriggered = false;
    private double postureDamageScalar = 1.0;

    /** Combo logic variables. */
    private int currentComboCount = 0;
    private int targetComboCount = 3;
    private Random random = new Random();

    /** Danmaku pattern variables. */
    private int danmakuShotsFired = 0;
    private int targetDanmakuShots = 15;
    private int danmakuDirection = 1;

    /** Timers and Cooldowns. */
    private Cooldown postBrokenInvincibility;
    private boolean isInvincibleAfterBroken = false;
    private Cooldown brokenDuration;
    private Cooldown danmakuFireRate;

    /** Screen properties. */
    private int screenWidth;
    private int screenHeight;
    private int originalY;
    private int bottomBoundary;

    /** Pattern cooldowns. */
    private Cooldown rushCooldown;
    private Cooldown stunCooldown;
    private Cooldown pauseCooldown;
    private Cooldown postureDecayCooldown;
    private Cooldown waveCooldown;

    /** Movement in normal state. */
    private int patrolSpeed = 3;
    private int patrolDirection = 1;

    /** Graphics resources. */
    private BufferedImage[] sprites;
    private BufferedImage currentSprite;

    private int drawWidth = 150;
    private int drawHeight = 150;

    /** Sprite indices. */
    private static final int SPR_IDLE = 0;
    private static final int SPR_MOVE = 1;
    private static final int SPR_ATTACK = 2;
    private static final int SPR_BROKEN = 3;
    private static final int SPR_ENRAGED = 4;

    /**
     * Constructor, establishes the boss's properties.
     *
     * @param positionX Initial X position.
     * @param positionY Initial Y position.
     * @param screenWidth Screen width.
     * @param player1 Player 1 ship instance.
     * @param player2 Player 2 ship instance (can be null).
     * @param bottomBoundary Bottom boundary for movement.
     */
    public SamuraiBoss(int positionX, int positionY, int screenWidth, Ship player1, Ship player2, int bottomBoundary) {
        super(positionX, positionY, 20, 80, Color.RED);
        this.logger = Core.getLogger();
        this.screenWidth = screenWidth;
        this.originalY = positionY;
        this.bottomBoundary = bottomBoundary;
        this.screenHeight = bottomBoundary + 150;

        this.spriteType = null;
        this.currentState = BossState.DANMAKU; // Starts with Danmaku phase

        this.player1 = player1;
        this.player2 = player2;

        loadSpriteSheet();
        this.currentSprite = this.sprites[SPR_IDLE];

        // Initialize Cooldowns
        this.postBrokenInvincibility = Core.getCooldown(500);
        this.brokenDuration = Core.getCooldown(5000);

        this.rushCooldown = Core.getCooldown(1000);
        this.waveCooldown = Core.getCooldown(1000);
        this.stunCooldown = Core.getCooldown(400);
        this.pauseCooldown = Core.getCooldown(200);
        this.postureDecayCooldown = Core.getCooldown(250);

        this.danmakuFireRate = Core.getCooldown(150);
    }

    /**
     * Associates the boss to a given screen.
     * @param newScreen Screen to attach.
     */
    public final void attach(final Screen newScreen) {
        this.screen = newScreen;
        this.screenHeight = newScreen.getHeight();
    }

    /**
     * Updates the boss's state and behavior.
     */
    @Override
    public void update(){
        if (isfailed) return;

        // Trigger Enrage mode if health drops below threshold
        float hpPercent = (float)health / maxHealth;
        if (hpPercent <= 0.66f && !isEnraged) triggerEnrageMode();

        // Adjust Danmaku fire rate based on health (Phase 2)
        if (isEnraged) {
            int fireRate = (int)(150 * hpPercent) + 50;
            this.danmakuFireRate.setMilliseconds(Math.max(50, fireRate));
        }

        // Auto-reduce posture over time if not in combat
        if (this.posture > 0 &&
                (currentState == BossState.NORMAL || currentState == BossState.RETREATING) &&
                postureDecayCooldown.checkFinished()) {
            this.posture -= (isEnraged ? 1 : 3);
            if (this.posture < 0) this.posture = 0;
            postureDecayCooldown.reset();
        }

        // Check invincibility expiration after broken state
        if (this.isInvincibleAfterBroken && this.postBrokenInvincibility.checkFinished()) {
            this.isInvincibleAfterBroken = false;
        }

        // Safety check for Enrage trigger
        if (!enrageTriggered && (this.health <= this.maxHealth / 2)) {
            triggerEnrageMode();
        }

        // === AI State Machine ===
        switch (currentState) {
            case NORMAL:
                patrol();
                if (rushCooldown.checkFinished()) {
                    Ship target = getClosestPlayer();
                    // Randomize combo count (attack when player is not in invincible mode and not null)
                    if (target != null && !target.isInvincible()) {
                        if (isEnraged) {
                            this.targetComboCount = 6 + random.nextInt(5); // 6~10
                        } else {
                            this.targetComboCount = 2 + random.nextInt(5); // 2~6
                        }
                        this.currentComboCount = 0;
                        startRush();
                    }
                }
                break;

            case DANMAKU:
                performDanmakuMove();

                // Check if Danmaku phase is finished
                if (danmakuShotsFired >= targetDanmakuShots) {
                    logger.info("Danmaku Finished. Retreating.");
                    this.currentState = BossState.RETREATING;
                    this.rushCooldown.reset();
                }
                break;

            case RUSHING:
                Ship target = getClosestPlayer();
                if (target == null) {
                    this.currentState = BossState.RETREATING;
                    return;
                }

                // Calculate rush speed based on health
                int baseSpeed = isEnraged ? 18 : 12;
                int speedBonus = (int)((1.0f - hpPercent) * 10);
                int rushSpeedX = baseSpeed + speedBonus;
                int rushSpeedY = (baseSpeed + speedBonus) / 2 + 8;

                // Move towards player
                if (this.positionX < target.getPositionX()) this.positionX += rushSpeedX;
                else if (this.positionX > target.getPositionX()) this.positionX -= rushSpeedX;

                if (this.positionY < target.getPositionY()) this.positionY += rushSpeedY;
                else if (this.positionY > target.getPositionY()) this.positionY -= rushSpeedY;

                // Check if reached target or bottom boundary
                if (Math.abs(this.positionY - target.getPositionY()) < 20 ||
                        this.positionY + this.height >= this.screenHeight) {
                    currentState = BossState.PAUSE;
                    pauseCooldown.reset();
                }
                break;

            case PAUSE:
                if (pauseCooldown.checkFinished()) {
                    checkComboAndDecide();
                }
                break;

            case RETREATING:
                int retreatSpeed = isEnraged ? 5 : 3;
                this.positionY -= retreatSpeed;

                if (this.positionY <= this.originalY) {
                    this.positionY = this.originalY;
                    this.currentState = BossState.NORMAL;
                    this.rushCooldown.reset();
                }
                break;

            case STUN:
                if (stunCooldown.checkFinished()) {
                    checkComboAndDecide();
                }
                break;

            case BROKEN:
                if (brokenDuration.checkFinished()) {
                    logger.info("Boss Recovered!");
                    this.posture = 0;
                    this.currentState = BossState.RETREATING;
                }
                break;

            case FAIL: break;
        }

        updateSprite();
    }

    /**
     * Executes Danmaku movement pattern (side-to-side sine wave).
     */
    private void performDanmakuMove() {
        int speed = 8;
        this.positionX += speed * danmakuDirection;
        if (this.positionX <= 20 || this.positionX + this.width >= this.screenWidth - 20) {
            danmakuDirection *= -1;
        }
        this.positionY = this.originalY + (int)(Math.sin(System.currentTimeMillis() / 200.0) * 30);
    }

    /**
     * Initiates Rush attack.
     */
    private void startRush() {
        this.currentState = BossState.RUSHING;
        SoundManager.play("sfx/melee.wav");
    }

    /**
     * Checks combo progress and decides next state (Rush or Danmaku).
     */
    private void checkComboAndDecide() {
        this.currentComboCount++;
        if (currentComboCount < targetComboCount) {
            this.currentState = BossState.RUSHING;
        } else {
            startDanmakuPhase();
        }
    }

    /**
     * Initiates Danmaku phase.
     */
    private void startDanmakuPhase() {
        this.currentState = BossState.DANMAKU;
        this.danmakuShotsFired = 0;
        this.danmakuFireRate.reset();
        this.targetDanmakuShots = isEnraged ? 30 : 15;
        logger.info("Starting Danmaku Phase!");
    }

    /**
     * Handles parry logic when attacked by player.
     */
    public void onParried() {
        if (currentState == BossState.BROKEN) return;

        if (isAttacking()) {
            this.positionY -= 60; // Knockback
            takePostureDamage(50);

            if (currentState != BossState.BROKEN) {
                this.currentState = BossState.STUN;
                this.stunCooldown.reset();
            }
        }
    }

    /**
     * Applies posture damage to the boss.
     * @param baseDamage Base damage amount.
     */
    public void takePostureDamage(int baseDamage) {
        if (currentState == BossState.BROKEN) return;

        int actualDamage = (int) (baseDamage * this.postureDamageScalar);
        this.posture += actualDamage;

        if (this.posture >= this.maxPosture) {
            this.posture = this.maxPosture;
            this.currentState = BossState.BROKEN;
            this.isInvincibleAfterBroken = true;
            this.postBrokenInvincibility.reset();
            this.brokenDuration.reset();
            this.positionY -= 30;
            SoundManager.play("sfx/samurai-broken.wav");
            logger.info("Posture Broken Instantly!");
        }
    }

    /**
     * Executes Deathblow logic (Massive damage when posture broken).
     */
    public void executeDeathblow() {
        if (currentState == BossState.BROKEN) {
            this.health -= 1000;
            if (this.health <= 0) {
                destroy();
            } else {
                this.posture = 0;
                this.currentState = BossState.RETREATING;
                if (this.health <= 2000 && !isEnraged) {
                    triggerEnrageMode();
                }
            }
        }
    }

    /**
     * Generates wave attacks (Danmaku or Pattern).
     * @return Set of SwordWaves generated.
     */
    public Set<SwordWave> shootWave() {
        Set<SwordWave> waves = new HashSet<>();

        if (currentState == BossState.RETREATING ||
                currentState == BossState.BROKEN ||
                currentState == BossState.STUN) {
            return waves;
        }

        // 1. DANMAKU Mode
        if (currentState == BossState.DANMAKU) {
            // Check cooldown and fire
            if (danmakuFireRate.checkFinished()) {
                danmakuFireRate.reset();
                danmakuShotsFired++;

                if (isEnraged) {
                    waves.add(createRainShot()); // Rain pattern
                } else {
                    waves.add(createAimedShot()); // Aimed pattern
                }
            }
        }
        // 2. Normal Mode Pattern
        else if (currentState != BossState.RUSHING && waveCooldown.checkFinished()) {
            waveCooldown.reset();
            waves.addAll(createWavePattern());
        }

        return waves;
    }

    // [Pattern 1: Rain] Random falling shots
    private SwordWave createRainShot() {
        SoundManager.play("sfx/rainshot.wav");

        int randomX = 20 + random.nextInt(screenWidth - 40);
        int startY = this.positionY + this.height;

        return new SwordWave(randomX, startY, 24, 36, Color.ORANGE);
    }

    // [Pattern 2: Aimed] Aimed at player
    private SwordWave createAimedShot() {
        SoundManager.play("sfx/aimshot.wav");
        Ship target = getClosestPlayer();
        int tx = (target != null) ? target.getPositionX() : positionX;
        return new SwordWave(this.positionX + this.width/2 - 12, this.positionY + this.height,
                24, 36, Color.CYAN);
    }

    // [Pattern 3: Wave] 3-way spread
    private Set<SwordWave> createWavePattern() {
        Set<SwordWave> waves = new HashSet<>();
        SoundManager.play("sfx/aimshot.wav");

        int cx = this.positionX + this.width / 2;
        int y = this.positionY + this.height;
        Color color = isEnraged ? Color.ORANGE : Color.CYAN;

        waves.add(new SwordWave(cx - 12, y, 24, 36, color));
        waves.add(new SwordWave(cx - 92, y, 24, 36, color));
        waves.add(new SwordWave(cx + 68, y, 24, 36, color));
        return waves;
    }

    /**
     * Updates the current sprite based on state.
     */
    private void updateSprite() {
        if (this.isPostureBroken()) {
            this.currentSprite = this.sprites[SPR_BROKEN];
        }
        else if (this.isAttacking()) {
            this.currentSprite = this.sprites[SPR_ATTACK];
        }
        else if (this.isEnraged) {
            this.currentSprite = this.sprites[SPR_ENRAGED];
        }
        else {
            if (currentState == BossState.NORMAL || currentState == BossState.DANMAKU) {
                if (patrolDirection < 0) this.currentSprite = this.sprites[SPR_MOVE];
                else this.currentSprite = flipImage(this.sprites[SPR_MOVE]);
            } else {
                this.currentSprite = this.sprites[SPR_IDLE];
            }
        }
    }

    /**
     * Loads sprite sheet resources.
     */
    private void loadSpriteSheet() {
        this.sprites = new BufferedImage[6];
        try {
            InputStream is = FileManager.class.getClassLoader().getResourceAsStream("sekiro.png");
            if (is == null) {
                is = FileManager.class.getClassLoader().getResourceAsStream("sekiro.png");
                if(is == null) { createPlaceholders(); return; }
            }
            BufferedImage sheet = ImageIO.read(is);
            int cellW = sheet.getWidth() / 3;
            int cellH = sheet.getHeight() / 2;

            this.sprites[SPR_IDLE] = sheet.getSubimage(0, 0, cellW, cellH);
            this.sprites[SPR_MOVE] = sheet.getSubimage(cellW, 0, cellW, cellH);
            this.sprites[SPR_ATTACK] = sheet.getSubimage(0, cellH, cellW, cellH);
            this.sprites[SPR_BROKEN] = sheet.getSubimage(cellW, cellH, cellW, cellH);
            this.sprites[SPR_ENRAGED] = sheet.getSubimage(cellW * 2, cellH, cellW, cellH);
        } catch (Exception e) {
            e.printStackTrace();
            createPlaceholders();
        }
    }

    private BufferedImage flipImage(BufferedImage src) {
        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        tx.translate(-src.getWidth(null), 0);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        return op.filter(src, null);
    }

    private void createPlaceholders() {
        for (int i = 0; i < sprites.length; i++) {
            this.sprites[i] = new BufferedImage(50, 100, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = this.sprites[i].createGraphics();
            g.setColor(Color.RED); g.fillRect(0,0,50,100); g.dispose();
        }
    }

    /**
     * Finds the nearest active player ship.
     * @return Nearest Ship object or null.
     */
    private Ship getClosestPlayer() {
        boolean p1Alive = (player1 != null && !player1.isDestroyed());
        boolean p2Alive = (player2 != null && !player2.isDestroyed());
        if (!p1Alive && !p2Alive) return null;
        if (p1Alive && !p2Alive) return player1;
        if (!p1Alive && p2Alive) return player2;
        double d1 = Math.abs(this.positionX - player1.getPositionX());
        double d2 = Math.abs(this.positionX - player2.getPositionX());
        return (d1 <= d2) ? player1 : player2;
    }

    /**
     * Enables Phase 2 (Enraged Mode).
     */
    private void triggerEnrageMode() {
        if (!enrageTriggered) {
            isEnraged = true; enrageTriggered = true;
            postureDamageScalar = 0.55;
            startRush();
        }
    }

    /**
     * Horizontal patrol movement.
     */
    private void patrol() {
        int speed = isEnraged ? (int)(patrolSpeed * 1.5) : patrolSpeed;
        this.positionX += (speed * this.patrolDirection);
        if (this.positionX <= 0 || this.positionX + this.width >= this.screenWidth)
            this.patrolDirection *= -1;
    }

    @Override public void takeDamage(int damage) { if (!isInvincibleAfterBroken) { health -= damage; if(health<=0) destroy(); } }
    @Override public void destroy() { isfailed = true; currentState = BossState.FAIL; }
    @Override public boolean isDestroyed() { return isfailed; }
    @Override public int getHealPoint() { return this.health; }
    @Override public int getPointValue() { return this.pointValue; }
    @Override public void move(int dx, int dy) {}
    @Override public void draw(DrawManager dm) { dm.drawSamuraiBoss(this.screen, this); }

    public int getDrawWidth() { return drawWidth; }
    public int getDrawHeight() { return drawHeight; }
    public BufferedImage getSprite() { return currentSprite; }
    public boolean isRushing() { return currentState == BossState.RUSHING; }
    public boolean isAttacking() { return currentState == BossState.RUSHING || currentState == BossState.PAUSE; }
    public boolean isPostureBroken() { return currentState == BossState.BROKEN; }
    public boolean isEnraged() { return isEnraged; }
    public boolean isInvincibleAfterBroken() { return isInvincibleAfterBroken; }
    public int getPosture() { return posture; }
    public int getMaxPosture() { return maxPosture; }
    public int getMaxHealth() { return maxHealth; }
}