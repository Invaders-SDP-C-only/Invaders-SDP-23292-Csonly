package entity;

import audio.SoundManager;
import entity.BossEntity;
import entity.Ship;
import engine.DrawManager;
import engine.DrawManager.SpriteType;
import engine.Core;
import engine.Cooldown;
import screen.GameScreen;
import screen.Screen;

import java.awt.Color;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class SamuraiBoss extends Entity implements BossEntity{

    /** Boss status define */
    private enum BossState { NORMAL, RUSHING, PAUSE, RETREATING, STUN, BROKEN, FAIL }
    private BossState currentState;
    private Logger logger;
    private Ship player1;
    private Ship player2;
    private GameScreen screen;

    /** Boss status hard coding */
    private int health = 100;
    private int maxHealth = 100;
    private int posture = 0;
    private int maxPosture = 200;
    private int pointValue = 5000;
    private boolean isfailed = false;

    /** define 2 phase mode */
    private boolean isEnraged = false;
    private boolean enrageTriggered = false;
    private double postureDamageScalar = 1.0;

    /** Invincible time after entering broken state */
    private Cooldown postBrokenInvincibility;
    /** current status after broken state */
    private boolean isInvincibleAfterBroken = false;
    /** screen size */
    private int screenWidth;
    /** Original boss's position Y */
    private int originalY;
    /** boundary area of movement*/
    private int bottomBoundary;
    /** pattern cooldown */
    private Cooldown rushCooldown;
    private Cooldown waveCooldown;

    /** status cooldown */
    private Cooldown stunCooldown;
    private Cooldown pauseCooldown; //
    private Cooldown postureDecayCooldown;

    /** Movement in the normal state. */
    private int patrolSpeed = 2;
    private int patrolDirection = 1;

    /**
     * Constructor, establishes the entity's generic properties.
     */
    public SamuraiBoss(int positionX, int positionY, int screenWidth, Ship player1, Ship player2, int bottomBoundary) {
        super(positionX, positionY, 24 * 2, 24 * 2, Color.WHITE);
        this.logger = Core.getLogger();
        this.screenWidth = screenWidth;
        this.originalY = positionY;
        this.bottomBoundary = bottomBoundary;

        this.spriteType = SpriteType.SamuraiNormal;
        this.currentState = BossState.NORMAL;

        this.player1 = player1;
        this.player2 = player2;

        /** Invincible Cooldown Initialization */
         this.postBrokenInvincibility = Core.getCooldown(2000); // 2s

        /** Initial Cooldowns */
        this.rushCooldown = Core.getCooldown(3000);
        this.waveCooldown = Core.getCooldown(8000);
        this.stunCooldown = Core.getCooldown(400); // 0.4s stun
        this.pauseCooldown = Core.getCooldown(500); // 0.5s delay after attack
        this.postureDecayCooldown = Core.getCooldown(250);
    }
    /**
     * Associates the boss to a given screen.
     * @param newScreen Screen to attach.
     */
    public final void attach(final Screen newScreen) {
        this.screen = (GameScreen) newScreen;
    }

    @Override
    public void update(){
        if (isfailed) return;
        // Cooldown adjustment by boss's health in 2 phase.
        if (this.isEnraged) {

            // Velocity change according to posture value.
            float posturePercent = (float)this.posture / (float)this.maxPosture;
            int MAX_RUSH = 1500; int MIN_RUSH = 700;
            int MAX_WAVE = 5000; int MIN_WAVE = 2500;
            int MAX_STUN = 200;  int MIN_STUN = 100;
            int newRushCooldown = (int) (MAX_RUSH - (MAX_RUSH - MIN_RUSH) * posturePercent);
            int newWaveCooldown = (int) (MAX_WAVE - (MAX_WAVE - MIN_WAVE) * posturePercent);
            int newStunCooldown = (int) (MAX_STUN - (MAX_STUN - MIN_STUN) * posturePercent);
            this.rushCooldown.setMilliseconds(newRushCooldown);
            this.waveCooldown.setMilliseconds(newWaveCooldown);
            this.stunCooldown.setMilliseconds(newStunCooldown);
        }
        // Auto reduction of posture logic
        if (this.posture > 0 &&
                (currentState == BossState.NORMAL || currentState == BossState.RETREATING) &&
                postureDecayCooldown.checkFinished()) {

            this.posture -= (isEnraged ? 1 : 2);
            if (this.posture < 0) this.posture = 0;
            postureDecayCooldown.reset();
        }

        // Check invincible time after broken status
        if (this.isInvincibleAfterBroken && this.postBrokenInvincibility.checkFinished()) {
            this.isInvincibleAfterBroken = false;
            logger.info("Boss's posture is broken ! Ready for Deathblow.");
        }

        // After deathblow
        if (!enrageTriggered &&
                (this.health <= this.maxHealth / 2)) {
            triggerEnrageMode();
        }

        // Boss AI
        switch (currentState) {
            case NORMAL:
                this.spriteType = SpriteType.SamuraiNormal;

                // Move from side to side
                patrol();

                if (rushCooldown.checkFinished()) {
                    currentState = BossState.RUSHING;
                    logger.info("Boss is RUSHING");
                    rushCooldown.reset();
                }
                break;

            case RUSHING:
                this.spriteType = SpriteType.SamuraiAttack;
                Ship target = getClosestPlayer();
                if (target == null) {
                    currentState = BossState.PAUSE;
                    pauseCooldown.reset();
                    return;
                }

                // Track the Player
                int targetX = target.getPositionX();
                int targetY = target.getPositionY();
                int rushSpeedX = isEnraged ? 15 : 10;
                int rushSpeedY = isEnraged ? 10 : 7;

                if (this.positionX < targetX) this.positionX += rushSpeedX;
                else if (this.positionX > targetX) this.positionX -= rushSpeedX;

                // rushing logic
                if (this.positionY < targetY) this.positionY += rushSpeedY;
                else if (this.positionY > targetY) this.positionY -= rushSpeedY;
                if (Math.abs(this.positionY - targetY) < 10 ||
                        this.positionY + this.height >= this.bottomBoundary) {
                    currentState = BossState.PAUSE;
                    pauseCooldown.reset();
                    // logger.info("Boss attack finished, PAUSING");
                }
                break;

            // delay after rushing
            case PAUSE:
                this.spriteType = SpriteType.SamuraiNormal;
                if (pauseCooldown.checkFinished()) {
                    currentState = BossState.RETREATING;
                    // logger.info("Boss PAUSE ended, RETREATING");
                }
                break;

            // retreat after rushing
            case RETREATING:
                this.spriteType = SpriteType.SamuraiNormal;

                int retreatSpeed = isEnraged ? 15 : 10;
                this.positionY -= retreatSpeed;

                if (this.positionY <= this.originalY) {
                    this.positionY = this.originalY;
                    currentState = BossState.NORMAL;
                    rushCooldown.reset();
                    // logger.info("Boss returned to NORMAL");
                }
                break;

            // stun logic
            case STUN:
                this.spriteType = SpriteType.SamuraiNormal;
                if (stunCooldown.checkFinished()) {
                    currentState = BossState.RETREATING;
                    // logger.info("Boss STUN ended, RETREATING");
                }
                break;

            // posture broken logic
            case BROKEN:
                this.spriteType = SpriteType.SamuraiBroken;
                retreatSpeed = 10;
                targetX = (this.screenWidth / 2) - (this.width / 2);

                boolean atRetreatX = false;
                boolean atRetreatY = false;

                // Go to the center of the screen.
                if (this.positionX < targetX) {
                    this.positionX = Math.min(this.positionX + retreatSpeed, targetX);
                } else if (this.positionX > targetX) {
                    this.positionX = Math.max(this.positionX - retreatSpeed, targetX);
                }
                if (Math.abs(this.positionX - targetX) < retreatSpeed) atRetreatX = true;

                if (this.positionY > this.originalY) {
                    this.positionY = Math.max(this.positionY - retreatSpeed, this.originalY);
                }
                if (Math.abs(this.positionY - this.originalY) < retreatSpeed) atRetreatY = true;

                if (atRetreatX && atRetreatY) {}
                break;
            // death of samurai
            case FAIL:
                this.spriteType = SpriteType.Explosion;
                break;
        }
    }

    /**
     * Move from side to side in NORMAL state.
     */
    private void patrol() {
        int currentPatrolSpeed = isEnraged ? (int)(patrolSpeed * 1.5) : patrolSpeed;
        this.positionX += (currentPatrolSpeed * this.patrolDirection);

        if (this.positionX <= 0) {
            this.positionX = 0;
            this.patrolDirection = 1;
        }
        else if (this.positionX + this.width >= this.screenWidth) {
            this.positionX = this.screenWidth - this.width;
            this.patrolDirection = -1;
        }
    }

    /**
     * A long-range wave attack.
     */
    public Set<SwordWave> shootWave() {
        if (currentState == BossState.NORMAL && waveCooldown.checkFinished()) {
            waveCooldown.reset();
            SoundManager.play("sfx/melee.wav");

            Set<SwordWave> waves = new HashSet<>();

            // size of wave
            int waveWidth = 12 * 2;
            int waveHeight = 18 * 2;

            SwordWave wave = new SwordWave(this.positionX + (this.width / 2) - (waveWidth / 2),
                    this.positionY + this.height,
                    waveWidth, waveHeight, Color.CYAN);
            waves.add(wave);
            return waves;
        }
        return Collections.emptySet();
    }

    // Target the nearest player.
    private Ship getClosestPlayer() {
        if (this.player2 == null) {
            return (this.player1 != null && !this.player1.isDestroyed()) ? this.player1 : null;
        }
        if (this.screen == null) return null;

        // Get actual lives.
        boolean p1Alive = (this.player1 != null && this.screen.getLivesP1() > 0 && !this.player1.isDestroyed());
        boolean p2Alive = (this.player2 != null && this.screen.getLivesP2() > 0 && !this.player2.isDestroyed());

        if (this.player2 == null) {
            return p1Alive ? this.player1 : null;
        }

        // 2p mode
        if (!p1Alive && !p2Alive) return null;
        if (p1Alive && !p2Alive) return this.player1;
        if (!p1Alive && p2Alive) return this.player2;

        int dist1 = Math.abs(this.positionX - this.player1.getPositionX());
        int dist2 = Math.abs(this.positionX - this.player2.getPositionX());
        return (dist1 <= dist2) ? this.player1 : this.player2;
    }

    /**
     * Switch the boss to 2 phases.
     */
    private void triggerEnrageMode() {
        if (enrageTriggered) return;
        this.isEnraged = true;
        this.enrageTriggered = true;
        this.logger.info("Samurai BOSS is ENRAGED!");
        this.postureDamageScalar = 0.55;
        this.setColor(Color.ORANGE);
        this.currentState = BossState.RUSHING;
        this.rushCooldown.reset();
    }

    // parry logic
    public void onParried() {
        if (currentState == BossState.RUSHING || currentState == BossState.PAUSE) {
            SoundManager.play("sfx/parry.wav");
            logger.info("Boss Parried!");
            // Posture increase through parry
            takePostureDamage(80);
            this.currentState = BossState.STUN;
            this.stunCooldown.reset();
        }
    }

    /**
     * Posture damage that boss gets.
     * @param baseDamage base damage of posture.
     */
    public void takePostureDamage(int baseDamage) {
        if (currentState == BossState.BROKEN) return;
        int actualDamage = (int) (baseDamage * this.postureDamageScalar);
        this.posture += actualDamage;
        // logger.info("Boss Posture: " + this.posture + " / " + this.maxPosture);

        // if boss is broken
        if (this.posture >= this.maxPosture) {
            this.posture = this.maxPosture;
            this.currentState = BossState.BROKEN;
            this.isInvincibleAfterBroken = true;
            this.postBrokenInvincibility.reset();
            SoundManager.play("sfx/samurai-broken.wav");
            logger.info("Boss Posture BROKEN! Ready for Deathblow!");
        }
    }

    // Deathblow logic
    public void executeDeathblow() {
        if (currentState == BossState.BROKEN) {
            this.health -= 50 ;

            if (this.health <= 0) {
                destroy();
            } else {
                this.posture = 0;
                this.currentState = BossState.RETREATING;
                logger.info("Deathblow success!");
            }
        }
    }

    @Override
    public void takeDamage(int damage) {
        if (this.isInvincibleAfterBroken) return;
        this.health -= damage;
        if (this.health <= 0) {
            destroy();
        }
    }

    /**
     * Gatter Boss invincible state.
     */
    public boolean isInvincibleAfterBroken() {
        return this.isInvincibleAfterBroken;
    }

    // BossEntity interface
    @Override public void destroy() { this.isfailed = true; this.currentState = BossState.FAIL; }
    @Override public boolean isDestroyed() { return this.isfailed; }
    @Override public int getHealPoint() { return this.health; }
    @Override public int getPointValue() { return this.pointValue; }
    @Override public void move(int distanceX, int distanceY) { }
    @Override public void draw(DrawManager drawManager) { drawManager.drawEntity(this, this.positionX, this.positionY); }

    public boolean isAttacking() { return this.currentState == BossState.RUSHING || this.currentState == BossState.PAUSE; }
    public boolean isPostureBroken() { return this.currentState == BossState.BROKEN; }
    public boolean isEnraged() { return this.isEnraged; }
    public int getPosture() { return this.posture; }
    public int getMaxPosture() { return this.maxPosture; }
    public int getMaxHealth() { return this.maxHealth; }
}