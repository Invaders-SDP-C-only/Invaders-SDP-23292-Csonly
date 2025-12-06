package entity;

import audio.SoundManager;
import engine.Cooldown;
import engine.Core;
import engine.DrawManager;
import engine.FileManager;
import engine.LaserBeam;
import engine.LaserBeamManager;
import screen.Screen;

import java.awt.*;
import java.awt.image.*;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Implements the True Final Boss entity.
 */
public class TrueFinalBoss extends Entity implements BossEntity {

    /**
     * Boss state definitions.
     */
    private enum State {
        STANCE_MAGIC, STANCE_LASER, STANCE_SWORD, BROKEN, RETURNING
    }
    private State currentState;

    /** Sub-states for Sword Stance. */
    private enum SwordSubState { APPROACH, ATTACK, BACKSTEP, STUN }
    private SwordSubState swordState;

    // === Stats ===
    /** Current health points. */
    private int health = 600;
    /** Maximum health points. */
    private int maxHealth = 600;
    /** Current posture (sekiro-style balance). */
    private int posture = 0;
    /** Maximum posture before break. */
    private int maxPosture = 600;
    /** Destruction flag. */
    private boolean isDestroyed = false;
    /** Phase 2 flag (below 50% HP). */
    private boolean isPhase2 = false;

    // === Environment ===
    /** Screen reference for drawing and logic. */
    private Screen screen;
    /** Screen dimensions. */
    private int screenWidth, screenHeight;
    /** Player ship references. */
    private Ship player1, player2;
    /** Manager for laser beams. */
    private LaserBeamManager laserManager;

    // === Timers & Cooldowns ===
    /** Timer for switching main states. */
    private Cooldown stateTimer;
    /** Cooldown between bullet shots. */
    private Cooldown bulletCooldown;
    /** Cooldown for melee attack combo. */
    private Cooldown meleeAttackTimer;
    /** Warning duration before laser fires. */
    private Cooldown laserWarnTimer;
    /** Duration of broken state. */
    private Cooldown brokenDuration;
    /** Timer for natural posture recovery. */
    private Cooldown postureDecay;

    // === Pattern Variables ===
    /** Angle for spiral bullet patterns. */
    private float spiralAngle = 0;
    /** Current count of melee combo attacks. */
    private int meleeComboCount = 0;
    /** Target number of melee attacks in a combo. */
    private int targetMeleeCombo = 5;

    // === Laser Warning ===
    /** Flag indicating laser warning is active. */
    private boolean isLaserWarning = false;
    /** List of angles for pending lasers. */
    private List<Float> warningAngles = new ArrayList<>();

    // === Graphics ===
    /** Sprite sheet (Phase x State). */
    private BufferedImage[][] sprites;
    /** Current sprite image to draw. */
    private BufferedImage currentSprite;
    /** Drawing dimensions. */
    private int drawWidth = 150;
    private int drawHeight = 150;
    /** Previous X position for movement animation. */
    private double prevX;

    /** Sprite index: Idle. */
    private static final int SPR_IDLE = 0;
    /** Sprite index: Movement. */
    private static final int SPR_MOVE = 1;
    /** Sprite index: Attack. */
    private static final int SPR_ATTACK = 2;
    /** Sprite index: Broken/Stunned. */
    private static final int SPR_BROKEN = 3;

    /**
     * Constructor, establishes the boss's properties.
     *
     * @param x Initial X position.
     * @param y Initial Y position.
     * @param w Screen width.
     * @param h Screen height.
     * @param lm Laser beam manager.
     * @param p1 Player 1 ship.
     * @param p2 Player 2 ship.
     * @param screen Game screen reference.
     */
    public TrueFinalBoss(int x, int y, int w, int h, LaserBeamManager lm, Ship p1, Ship p2, Screen screen) {
        super(x, y, 60, 100, Color.BLACK);
        this.screenWidth = w;
        this.screenHeight = h;
        this.laserManager = lm;
        this.player1 = p1;
        this.player2 = p2;
        this.screen = screen;

        loadSpriteSheet(); // Load graphic resources
        this.currentSprite = (sprites != null) ? sprites[0][SPR_IDLE] : null;
        this.currentState = State.STANCE_MAGIC;

        // Initialize Cooldowns
        this.stateTimer = Core.getCooldown(8000);
        this.bulletCooldown = Core.getCooldown(500); // Initial bullet speed
        this.meleeAttackTimer = Core.getCooldown(400);
        this.laserWarnTimer = Core.getCooldown(1500);
        this.brokenDuration = Core.getCooldown(5000);
        this.postureDecay = Core.getCooldown(200);
    }

    /**
     * Attaches the boss to a screen.
     * @param s Screen to attach.
     */
    public void attach(Screen s) { this.screen = s; }

    /**
     * Updates the boss's logic, state, and animation.
     */
    @Override
    public void update() {
        if (isDestroyed) return;

        // Calculate movement direction for sprite flipping
        double dx = this.positionX - this.prevX;
        this.prevX = this.positionX;

        clampPosition();

        // Check Phase 2 Transition (50% HP)
        if (!isPhase2 && health <= maxHealth / 2) {
            isPhase2 = true;
            // SoundManager.play("sfx/boss_enrage.wav"); // Optional sound
            this.bulletCooldown = Core.getCooldown(80); // Faster shooting
        }

        // === Posture Recovery Logic ===
        // Recovers posture if not attacking or broken
        if (posture > 0 && currentState != State.STANCE_SWORD && currentState != State.BROKEN && postureDecay.checkFinished()) {
            double hpRatio = (double) health / maxHealth;
            // Higher HP = Faster Recovery
            int recoveryAmount = 1 + (int)(4 * hpRatio);

            if (isPhase2) recoveryAmount = Math.max(1, recoveryAmount - 1); // Slower recovery in Phase 2

            posture -= recoveryAmount;
            if (posture < 0) posture = 0;

            postureDecay.reset();
        }

        // State Machine
        switch (currentState) {
            case STANCE_MAGIC: updateMagicState(); break;
            case STANCE_LASER: updateLaserState(); break;
            case STANCE_SWORD: updateSwordState(); break;
            case BROKEN:       updateBrokenState(); break;
            case RETURNING:    updateReturningState(); break;
        }

        updateSprite(dx);
    }

    /**
     * Keeps the boss within the screen boundaries.
     */
    private void clampPosition() {
        if (this.positionX < 0) this.positionX = 0;
        if (this.positionX > screenWidth - this.width) this.positionX = screenWidth - this.width;

        // Keep boss in upper portion of screen
        if (this.positionY < 50) this.positionY = 50;
        if (this.positionY > screenHeight - this.height) this.positionY = screenHeight - this.height;
    }

    /**
     * Updates the Magic (Danmaku) Stance.
     * Moves slowly and periodically switches to Sword or Laser stance.
     */
    private void updateMagicState() {
        // Hover movement
        this.positionX += Math.sin(System.currentTimeMillis() / 1000.0) * 3;

        if (stateTimer.checkFinished()) {
            stateTimer.reset();
            // 60% chance for Sword, 40% for Laser
            if (Math.random() < 0.6) startSwordPattern();
            else startLaserPattern();
        }
    }

    /**
     * Updates the Laser Stance.
     * Waits for warning timer, then fires lasers.
     */
    private void updateLaserState() {
        if (isLaserWarning && laserWarnTimer.checkFinished()) {
            fireLasers();
            isLaserWarning = false;
            stateTimer.setMilliseconds(1500); // Wait briefly after firing
            stateTimer.reset();
        }
        if (!isLaserWarning && stateTimer.checkFinished()) {
            currentState = State.RETURNING;
        }
    }

    /**
     * Initiates the Laser attack pattern.
     * Calculates angles towards the player.
     */
    private void startLaserPattern() {
        this.currentState = State.STANCE_LASER;
        this.isLaserWarning = true;
        this.laserWarnTimer.reset();
        this.warningAngles.clear();

        Ship target = getClosestPlayer();
        if (target != null) {
            double angleToPlayer = Math.atan2(target.getPositionY() - (positionY + height),
                    target.getPositionX() - (positionX + width/2));
            float baseAngle = (float)Math.toDegrees(angleToPlayer);

            // 3-way laser
            warningAngles.add(baseAngle);
            warningAngles.add(baseAngle - 30);
            warningAngles.add(baseAngle + 30);

            // Phase 2 adds wider spread
            if (isPhase2) {
                warningAngles.add(baseAngle - 60);
                warningAngles.add(baseAngle + 60);
            }
        }
        SoundManager.play("sfx/lazer-ready.wav");
    }

    /**
     * Updates the Sword (Melee) Stance.
     * Handles Approach -> Attack -> Backstep logic.
     */
    private void updateSwordState() {
        Ship target = getClosestPlayer();
        if (target == null || target.isDestroyed()) { currentState = State.RETURNING; return; }

        int speed = isPhase2 ? 18 : 12;

        switch (swordState) {
            case APPROACH:
                // Move towards player
                if (positionX < target.getPositionX()) positionX += speed;
                else positionX -= speed;
                if (positionY < target.getPositionY()) positionY += speed;
                else positionY -= speed;

                // Close enough to attack?
                if (getDistance(target) < 60) {
                    swordState = SwordSubState.ATTACK;
                    meleeAttackTimer.reset();
                    SoundManager.play("sfx/melee.wav");
                }
                break;

            case ATTACK:
                if (meleeAttackTimer.checkFinished()) {
                    meleeComboCount++;
                    if (meleeComboCount >= targetMeleeCombo) {
                        currentState = State.RETURNING; // Combo finished
                    } else {
                        swordState = SwordSubState.BACKSTEP;
                        meleeAttackTimer.setMilliseconds(250);
                        meleeAttackTimer.reset();
                    }
                }
                break;

            case BACKSTEP:
                positionY -= 15; // Retreat slightly
                if (meleeAttackTimer.checkFinished()) {
                    swordState = SwordSubState.APPROACH; // Attack again
                }
                break;

            case STUN:
                if (meleeAttackTimer.checkFinished()) {
                    // Parried attack counts as combo hit
                    incrementComboAndCheck();

                    // Continue combo if state hasn't changed
                    if (currentState == State.STANCE_SWORD) {
                        swordState = SwordSubState.APPROACH;
                    }
                }
                break;
        }
    }

    /**
     * Increments combo count and checks if combo is finished.
     */
    private void incrementComboAndCheck() {
        meleeComboCount++;
        if (meleeComboCount >= targetMeleeCombo) {
            currentState = State.RETURNING;
        }
    }

    /**
     * Initiates the Sword attack pattern.
     */
    private void startSwordPattern() {
        this.currentState = State.STANCE_SWORD;
        this.swordState = SwordSubState.APPROACH;
        this.meleeComboCount = 0;
        this.targetMeleeCombo = isPhase2 ? 7 : 5; // More aggressive in Phase 2
        SoundManager.play("sfx/attack-ready.wav");
    }

    /**
     * Returns the boss to its default position.
     */
    private void updateReturningState() {
        int targetX = screenWidth / 2 - width / 2;
        int targetY = 100;
        int speed = 12;

        if (positionX < targetX) positionX += speed; else positionX -= speed;
        if (positionY < targetY) positionY += speed; else positionY -= speed;

        if (Math.abs(positionX - targetX) < 20 && Math.abs(positionY - targetY) < 20) {
            this.currentState = State.STANCE_MAGIC;
            this.stateTimer.reset();
        }
    }

    /**
     * Updates the Broken state (posture broken).
     */
    private void updateBrokenState() {
        if (brokenDuration.checkFinished()) {
            posture = 0; // Recover posture
            currentState = State.RETURNING;
        }
    }

    /**
     * Shoots bullets in Magic Stance.
     * @return Set of generated BossBullets.
     */
    public Set<BossBullet> shoot() {
        Set<BossBullet> bullets = new HashSet<>();

        if (currentState == State.STANCE_MAGIC) {
            if (bulletCooldown.checkFinished()) {
                bulletCooldown.reset();
                int cx = positionX + width / 2;
                int cy = positionY + height / 2;

                if (isPhase2) {
                    // Phase 2: Spiral Pattern
                    for (int i = 0; i < 4; i++) {
                        double angle = Math.toRadians(spiralAngle + (i * 90));
                        int sx = (int)(Math.cos(angle) * 5);
                        int sy = (int)(Math.sin(angle) * 5);
                        bullets.add(new BossBullet(cx, cy, sx, sy, 8, 8, Color.MAGENTA));
                    }
                    spiralAngle += 10;
                } else {
                    // Phase 1: Aimed + Spread
                    Ship target = getClosestPlayer();
                    if(target != null) {
                        double a = Math.atan2(target.getPositionY()-cy, target.getPositionX()-cx);
                        for(int i=-1; i<=1; i++) {
                            double finalA = a + Math.toRadians(i * 15);
                            bullets.add(new BossBullet(cx, cy, (int)(Math.cos(finalA)*6), (int)(Math.sin(finalA)*6), 10, 10, Color.RED));
                        }
                    }
                }
            }
        }
        return bullets;
    }

    /**
     * Fires laser beams based on warning angles.
     */
    private void fireLasers() {
        SoundManager.play("sfx/boss_laser.wav");
        float cx = positionX + width / 2f;
        float cy = positionY + height;

        for (float angle : warningAngles) {
            laserManager.addBeam(new LaserBeam(
                    cx, cy, angle, screenHeight * 1.5f,
                    20, 800
            ));
        }
    }

    /**
     * Handles parry event.
     * Applies knockback and posture damage to the boss.
     */
    public void onParried() {
        if (currentState == State.STANCE_SWORD && swordState != SwordSubState.BACKSTEP) {
            SoundManager.play("sfx/parry.wav");

            this.positionY -= 40; // Knockback
            takePostureDamage(80);

            if (currentState != State.BROKEN) {
                this.swordState = SwordSubState.STUN;
                this.meleeAttackTimer.setMilliseconds(500);
                this.meleeAttackTimer.reset();
            }
        }
    }

    /**
     * Checks if the boss is currently performing a melee attack.
     * @return True if attacking.
     */
    public boolean isAttacking() {
        return currentState == State.STANCE_SWORD &&
                (swordState == SwordSubState.ATTACK || swordState == SwordSubState.APPROACH);
    }

    @Override
    public void takeDamage(int dmg) {
        if (currentState == State.BROKEN) return;

        this.health -= dmg;

        // Ranged attacks also deal small posture damage to prevent decay
        takePostureDamage(3);

        if (this.health <= 0) destroy();
    }

    /**
     * Applies posture damage. Triggers BROKEN state if max posture reached.
     * @param dmg Amount of posture damage.
     */
    public void takePostureDamage(int dmg) {
        if (currentState == State.BROKEN) return;

        this.posture += dmg;
        if (this.posture >= this.maxPosture) {
            this.posture = this.maxPosture;
            this.currentState = State.BROKEN;
            this.brokenDuration.reset();
            SoundManager.play("sfx/samurai-broken.wav");
        }
    }

    /**
     * Executes Deathblow logic (Massive Damage).
     * Called when player attacks a Broken boss.
     */
    public void executeDeathblow() {
        if (currentState == State.BROKEN) {
            // Deal 50% of current health
            int damage = this.health / 2;
            this.health -= damage;

            SoundManager.play("sfx/samurai-kill.wav");
            Core.getLogger().info("Deathblow! Damage: " + damage + ", Remaining HP: " + this.health);

            if (this.health <= 10) {
                destroy();
            } else {
                posture = 0;
                currentState = State.RETURNING;
            }
        }
    }

    public boolean isLaserWarningActive() { return currentState == State.STANCE_LASER && isLaserWarning; }
    public List<Float> getWarningAngles() { return warningAngles; }
    public float getWarningOriginX() { return positionX + width/2f; }
    public float getWarningOriginY() { return positionY + height; }
    public boolean isPostureBroken() { return currentState == State.BROKEN; }
    public boolean isInvincibleAfterBroken() { return false; }
    public BufferedImage getSprite() { return currentSprite; }
    public int getDrawWidth() { return drawWidth; }
    public int getDrawHeight() { return drawHeight; }
    public int getMaxHealth() { return maxHealth; }
    public int getPosture() { return posture; }
    public int getMaxPosture() { return maxPosture; }


    /**
     * Updates the sprite animation based on state and movement.
     * @param dx Horizontal movement delta.
     */
    private void updateSprite(double dx) {
        int phaseIndex = isPhase2 ? 1 : 0;
        int stateIndex = SPR_IDLE;
        boolean flip = false; // Default facing left

        if (currentState == State.BROKEN) {
            stateIndex = SPR_BROKEN;
        } else if (currentState == State.STANCE_SWORD && swordState == SwordSubState.ATTACK) {
            stateIndex = SPR_ATTACK;
            // Face the player during attack
            Ship target = getClosestPlayer();
            if (target != null && target.getPositionX() > this.positionX + this.width / 2) {
                flip = false;
            }
        } else {
            if (Math.abs(dx) > 0.5) {
                stateIndex = SPR_MOVE;
                if (dx > 0) flip = true; // Face right if moving right
            } else {
                stateIndex = SPR_IDLE;
            }
        }

        if (sprites != null && sprites[phaseIndex] != null && sprites[phaseIndex][stateIndex] != null) {
            BufferedImage target = sprites[phaseIndex][stateIndex];
            this.currentSprite = flip ? flipImage(target) : target;
        }
    }

    /**
     * Loads and slices the sprite sheet.
     * Handles 2x2 grid for 2 phases.
     */
    private void loadSpriteSheet() {
        this.sprites = new BufferedImage[2][4]; // [Phase][Action]
        String files = "TrueFinalBoss.png";

        for(int p=0; p<2; p++) {
            try {
                InputStream is = FileManager.class.getClassLoader().getResourceAsStream(files);
                if (is == null) {
                    createPlaceholders(p);
                    continue;
                }
                BufferedImage rawSheet = ImageIO.read(is);
                // Remove background color (Black)
                BufferedImage sheet = makeColorTransparent(rawSheet, Color.BLACK);

                // Slice sheet: 2 columns, 2 rows
                int w = sheet.getWidth() / 2;
                int h = sheet.getHeight() / 2;

                // Row 0
                sprites[p][SPR_IDLE] = sheet.getSubimage(0, 0, w, h);
                sprites[p][SPR_MOVE] = sheet.getSubimage(w, 0, w, h);

                // Row 1
                sprites[p][SPR_ATTACK] = sheet.getSubimage(0, h, w, h);
                sprites[p][SPR_BROKEN] = sheet.getSubimage(w, h, w, h);

            } catch (Exception e) {
                e.printStackTrace();
                createPlaceholders(p);
            }
        }
    }

    /**
     * Flips an image horizontally.
     * @param src Source image.
     * @return Flipped image.
     */
    private BufferedImage flipImage(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage flipped = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = flipped.createGraphics();
        g.drawImage(src, 0, 0, w, h, w, 0, 0, h, null);
        g.dispose();
        return flipped;
    }

    /**
     * Creates placeholder images if resource loading fails.
     */
    private void createPlaceholders(int p) {
        for(int i=0; i<4; i++) {
            sprites[p][i] = new BufferedImage(50, 100, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sprites[p][i].createGraphics();
            g.setColor(p==0 ? Color.GRAY : Color.RED);
            g.fillRect(0,0,50,100);
            g.dispose();
        }
    }

    /**
     * Finds the closest active player.
     * @return Closest Ship object.
     */
    private Ship getClosestPlayer() {
        if (player2 == null) return player1;
        if (player1.isDestroyed()) return player2;
        if (player2.isDestroyed()) return player1;
        return (Math.abs(positionX - player1.getPositionX()) < Math.abs(positionX - player2.getPositionX())) ? player1 : player2;
    }

    private double getDistance(Entity e) {
        return Math.sqrt(Math.pow(positionX - e.getPositionX(), 2) + Math.pow(positionY - e.getPositionY(), 2));
    }

    /**
     * Utility to make a specific color transparent in an image.
     * Used to remove background from sprites.
     * @param im Source image.
     * @param color Color to make transparent.
     * @return Processed image.
     */
    public static BufferedImage makeColorTransparent(BufferedImage im, final Color color) {
        ImageFilter filter = new RGBImageFilter() {
            public int markerRGB = color.getRGB() | 0xFF000000;

            public final int filterRGB(int x, int y, int rgb) {
                if ((rgb | 0xFF000000) == markerRGB) {
                    return 0x00FFFFFF & rgb;
                } else {
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    if (r < 20 && g < 20 && b < 20) return 0x00FFFFFF & rgb; // Filter dark noise

                    return rgb;
                }
            }
        };

        ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        Image toolkitImage = Toolkit.getDefaultToolkit().createImage(ip);

        BufferedImage transparentImage = new BufferedImage(im.getWidth(), im.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = transparentImage.createGraphics();
        g2d.drawImage(toolkitImage, 0, 0, null);
        g2d.dispose();
        return transparentImage;
    }
    /** Getters */
    @Override public void destroy() { isDestroyed = true; }
    @Override public boolean isDestroyed() { return isDestroyed; }
    @Override public int getHealPoint() { return health; }
    @Override public int getPointValue() { return 20000; }
    @Override public void move(int x, int y) {}
    @Override public void draw(DrawManager dm) { dm.drawTrueFinalBoss(this.screen, this); }
}