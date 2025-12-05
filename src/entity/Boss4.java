package entity;

import audio.SoundManager;
import engine.Cooldown;
import engine.Core;
import engine.DrawManager;
import engine.FileManager;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import java.security.SecureRandom;

public class Boss4 extends Entity implements BossEntity{

    private int healPoint;
    private int maxHp;
    private final int pointValue;
    private boolean isDestroyed;

    private Cooldown animationCooldown;
    /** Shoot1's cool down */
    private Cooldown shootCooldown1;
    /** Shoot2's cool down */
    private Cooldown shootCooldown2;
    /** Shoot3's cool down */
    private Cooldown shootCooldown3;
    private int screenWidth;
    private int screenHeight;

    private double spiralAngle = 0;
    private double spiralAngle2 = 180;

    // Sprites for Boss4
    private transient BufferedImage[] sprites;
    private transient BufferedImage currentSprite;
    private int animationFrame = 0;
    // Sprite indexes
    private static final int SPRITE_IDLE = 0;
    private static final int SPRITE_MOVE_R = 1;
    private static final int SPRITE_BACK = 4;
    private static final int SPRITE_MOVE_L = 5;


    // Spell Card fields
    private String currentSpellCardName = null;
    private Cooldown spellCardAnnounceCooldown;
    private boolean isSpellCardActive = false;
    private Cooldown spellCardShootCooldown;
    private Cooldown aimedShotCooldown;
    private double spellCardRotationAngle = 0;
    private int numberOfNozzles = 3; // Adjusted
    private Cooldown spellCardDurationTimer;
    private static final int SPELL_CARD_DURATION = 30000; // 30 seconds
    private boolean survivedLastSpellCard = false;
    private boolean hasUsedSpellCard = false;
    private static final SecureRandom RANDOM = new SecureRandom();


    // Non-spell fields
    private Cooldown nonSpellPhaseTimer;
    private int nonSpellAttackPhase = 0;

    // Movement fields
    private enum MovementState { STATIONARY, SLOW_DRIFT, MOVING_TO_POINT }
    private MovementState currentMovementState = MovementState.STATIONARY;
    private int targetX, targetY;
    private int zigDirection = 1;


    private static final Logger logger = Core.getLogger();

    public Boss4(int positionX, int positionY, int screenWidth, int screenHeight){
        super(positionX, positionY, 60, 84, Color.MAGENTA); // New dimensions
        this.healPoint = 80; // Adjusted HP
        this.maxHp = healPoint;
        this.pointValue = 1500;
        this.isDestroyed = false;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        this.spriteType = null;

        InputStream inputStream = null;
        try {
            inputStream = FileManager.class.getClassLoader().getResourceAsStream("touhouboss.png");
            if (inputStream == null) {
                throw new IOException("Resource 'touhouboss.png' not found. Ensure it's in the classpath.");
            }
            BufferedImage spriteSheet = ImageIO.read(inputStream);
            
            this.sprites = new BufferedImage[6];

            this.sprites[0] = spriteSheet.getSubimage(40, 30, 150, 200);    // Idle
            this.sprites[1] = spriteSheet.getSubimage(380, 30, 150, 200);  // Move R

            this.sprites[4] = spriteSheet.getSubimage(380, 280, 150, 200);   // Back
            this.sprites[5] = spriteSheet.getSubimage(200, 30, 150, 200);  // Move L
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load boss sprite sheet", e);
            // Fallback to placeholders if image loading fails
            this.sprites = new BufferedImage[6];
            for (int i = 0; i < 6; i++) {
                this.sprites[i] = createPlaceholderSprite(this.width, this.height, Color.RED);
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.warning("Failed to close InputStream for touhouboss.png: " + e.getMessage());
                }
            }
        }
        
        this.currentSprite = this.sprites[SPRITE_IDLE];

        this.animationCooldown = Core.getCooldown(250); // Faster animation
        this.shootCooldown1 = Core.getCooldown(1500);
        this.shootCooldown2 = Core.getCooldown(2000);
        this.shootCooldown3 = Core.getCooldown(1800);
        
        this.spellCardShootCooldown = Core.getCooldown(120); // Adjusted
        this.aimedShotCooldown = Core.getCooldown(2500); // Adjusted
        this.nonSpellPhaseTimer = Core.getCooldown(5000);
        this.spellCardDurationTimer = Core.getCooldown(SPELL_CARD_DURATION);

        // Movement Initialization
        this.targetX = this.positionX;
        this.targetY = this.positionY;

    }

    private BufferedImage createPlaceholderSprite(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(color);
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(Color.YELLOW);
        g2d.drawRect(0, 0, width - 1, height - 1);
        g2d.dispose();
        return image;
    }

    public void startSpellCard(String name) {
        this.currentSpellCardName = name;
        this.spellCardAnnounceCooldown = Core.getCooldown(3000);
        this.spellCardAnnounceCooldown.reset();
        this.isSpellCardActive = true;
        this.spellCardDurationTimer.reset();
        this.hasUsedSpellCard = true; // Set the flag here
        logger.info("Boss4 activated Spell Card: " + name);
        // Set spell card movement
        this.currentMovementState = MovementState.MOVING_TO_POINT;
        this.targetX = RANDOM.nextInt(screenWidth - this.width);
        this.targetY = 50 + RANDOM.nextInt(screenHeight / 4);
    }

    private void endSpellCard(boolean survived) {
        this.isSpellCardActive = false;
        this.currentSpellCardName = null;
        this.survivedLastSpellCard = survived;
        if (survived) {
            logger.info("Boss4 Spell Card survived!");
        } else {
            logger.info("Boss4 Spell Card ended.");
        }
        // Reset movement after spell card
        this.currentMovementState = MovementState.STATIONARY;
    }

    @Override
    public void update(){
        if (this.isDestroyed) {
            this.currentSprite = this.sprites[SPRITE_BACK];
            return;
        }

        // Animation timing
        if (this.animationCooldown.checkFinished()) {
            this.animationCooldown.reset();
            this.animationFrame = (this.animationFrame + 1) % 2; // Simple 2-frame animation toggle
        }

        // Sprite selection logic
        switch (this.currentMovementState) {
            case STATIONARY:
                this.currentSprite = this.sprites[SPRITE_IDLE];
                break;
            case MOVING_TO_POINT:
                if (this.targetX > this.positionX) { // Moving right
                    this.currentSprite = this.sprites[SPRITE_MOVE_R];
                } else { // Moving left
                    this.currentSprite = this.sprites[SPRITE_MOVE_L];
                }
                break;
            case SLOW_DRIFT:
                if (this.zigDirection > 0) { // Moving right
                    this.currentSprite = this.sprites[SPRITE_MOVE_R];
                } else { // Moving left
                    this.currentSprite = this.sprites[SPRITE_MOVE_L];
                }
                break;
        }


        // Check for spell card timeout
        if (this.isSpellCardActive && this.spellCardDurationTimer.checkFinished()) {
            endSpellCard(true);
        }

        // Cycle non-spell patterns and link movement
        if (!this.isSpellCardActive && this.nonSpellPhaseTimer.checkFinished()) {
            this.nonSpellPhaseTimer.reset();
            this.nonSpellAttackPhase = (this.nonSpellAttackPhase + 1) % 3;

            switch (this.nonSpellAttackPhase) {
                case 0: // shoot1 (double circle) -> Move to top-center and stay
                    this.currentMovementState = MovementState.MOVING_TO_POINT;
                    this.targetX = this.screenWidth / 2 - this.width / 2;
                    this.targetY = 50;
                    break;
                case 1: // shoot2 (spiral) -> Slow drift
                    this.currentMovementState = MovementState.SLOW_DRIFT;
                    this.zigDirection = RANDOM.nextBoolean() ? 1 : -1;
                    break;
                case 2: // shoot3 (rain) -> Move to a random point
                    this.currentMovementState = MovementState.MOVING_TO_POINT;
                    this.targetX = RANDOM.nextInt(screenWidth - this.width);
                    this.targetY = 50 + RANDOM.nextInt(screenHeight / 4);
                    break;
            }
        }

        // Trigger Spell Card
        if (!this.hasUsedSpellCard && !this.isSpellCardActive && this.healPoint <= this.maxHp / 2 && this.healPoint > 0) {
            startSpellCard("Waltz of Blades");
        }
        
        movePattern();
    }

    @Override
    public void takeDamage(int damage){
        this.healPoint -= damage;
        SoundManager.stop("sfx/TWINKLE3.wav");
        SoundManager.play("sfx/TWINKLE3.wav");
        if(this.healPoint <= 0){
            if (this.isSpellCardActive) {
                endSpellCard(false);
            }
            this.destroy();
        }
    }

    @Override
    public int getHealPoint(){
        return this.healPoint;
    }

    public int getMaxHp(){
        return  this.maxHp;
    }

    @Override
    public int getPointValue(){
        return this.pointValue;
    }

    public void movePattern() {
        if (this.isSpellCardActive) {
            // Slower movement during spell card
            moveToPoint(3);
            if (this.currentMovementState == MovementState.STATIONARY) { // If arrived, pick a new point
                this.targetX = RANDOM.nextInt(screenWidth - this.width);
                this.targetY = 50 + RANDOM.nextInt(screenHeight / 4);
                 this.currentMovementState = MovementState.MOVING_TO_POINT;
            }
            return;
        }

        // Non-spell movement is now controlled by the state set in update()
        switch (this.currentMovementState) {
            case MOVING_TO_POINT:
                moveToPoint(3);
                break;
            case SLOW_DRIFT:
                slowDrift(2);
                break;
            case STATIONARY:
                // Do nothing
                break;
        }
    }

    private void moveToPoint(int speed) {
        if (Math.abs(this.positionX - this.targetX) < speed && Math.abs(this.positionY - this.targetY) < speed) {
            this.positionX = this.targetX;
            this.positionY = this.targetY;
            this.currentMovementState = MovementState.STATIONARY; // Arrived
            return;
        }

        double angle = Math.atan2(this.targetY - this.positionY, this.targetX - this.positionX);
        this.positionX += (int)(Math.cos(angle) * speed);
        this.positionY += (int)(Math.sin(angle) * speed);
    }

    public void slowDrift(int driftSpeed){
        this.positionX += (this.zigDirection * driftSpeed);
        if(this.positionX <= 20 || this.positionX >= this.screenWidth - this.width - 20){
            this.zigDirection *= -1;
        }
    }

    @Override
    public void move(int distanceX, int distanceY){
        this.positionX += distanceX;
        this.positionY += distanceY;
    }

    public Set<BossBullet> shootNormal() {
        switch (this.nonSpellAttackPhase) {
            case 0:
                return shoot1();
            case 1:
                return shoot2();
            case 2:
                return shoot3();
            default:
                return java.util.Collections.emptySet();
        }
    }

    public Set<BossBullet> shoot1(){
        if(this.shootCooldown1.checkFinished()){
            this.shootCooldown1.reset();
            Set<BossBullet> bullets = new HashSet<>();
            int bulletCount = 12;
            // First layer
            for (int i = 0; i < bulletCount; i++){
                double angle = 2 * Math.PI * i / bulletCount;
                int speedX = (int) (Math.cos(angle) * 2);
                int speedY = (int) (Math.sin(angle) * 2);
                BossBullet bullet = new BossBullet(this.getPositionX() + this.getWidth() / 2, this.getPositionY() + this.getHeight() / 2, speedX, speedY, 8, 12, Color.MAGENTA);
                bullets.add(bullet);
            }
            // Second layer
            for (int i = 0; i < bulletCount; i++){
                double angle = 2 * Math.PI * (i + 0.5) / bulletCount;
                int speedX = (int) (Math.cos(angle) * 3);
                int speedY = (int) (Math.sin(angle) * 3);
                BossBullet bullet = new BossBullet(this.getPositionX() + this.getWidth() / 2, this.getPositionY() + this.getHeight() / 2, speedX, speedY, 6, 10, Color.CYAN);
                bullets.add(bullet);
            }
            return bullets;
        }
        return java.util.Collections.emptySet();
    }

    public Set<BossBullet> shoot2() {
        if (this.shootCooldown2.checkFinished()) {
            this.shootCooldown2.reset();
            Set<BossBullet> bullets = new HashSet<>();
            int bulletsPerShot = 2;
            // Stream 1
            for (int i = 0; i < bulletsPerShot; i++) {
                double angle = 2 * Math.PI * (spiralAngle / 360.0);
                int speedX = (int) (Math.cos(angle) * 4);
                int speedY = (int) (Math.sin(angle) * 4);
                BossBullet bullet = new BossBullet(this.getPositionX() + this.getWidth() / 2, this.getPositionY() + this.getHeight() / 2, speedX, speedY, 6, 10, Color.PINK);
                bullets.add(bullet);
                spiralAngle = (spiralAngle + 15) % 360;
            }
            // Stream 2 (opposite direction)
            for (int i = 0; i < bulletsPerShot; i++) {
                double angle = 2 * Math.PI * (spiralAngle2 / 360.0);
                int speedX = (int) (Math.cos(angle) * 4);
                int speedY = (int) (Math.sin(angle) * 4);
                BossBullet bullet = new BossBullet(this.getPositionX() + this.getWidth() / 2, this.getPositionY() + this.getHeight() / 2, speedX, speedY, 6, 10, Color.WHITE);
                bullets.add(bullet);
                spiralAngle2 = (spiralAngle2 - 15);
                if (spiralAngle2 < 0) spiralAngle2 += 360;
            }
            return bullets;
        }
        return java.util.Collections.emptySet();
    }

    public Set<BossBullet> shoot3() {
        Set<BossBullet> bullets = new HashSet<>();
        if (this.shootCooldown3.checkFinished()) {
            this.shootCooldown3.reset();
            int bulletCount = 7; // Adjusted
            for (int i = 0; i < bulletCount; i++) {
               int randomX = RANDOM.nextInt(screenWidth);
               int speedX = RANDOM.nextInt(3) - 1;
                BossBullet bullet = new BossBullet(randomX, 1, speedX, 3, 6, 10, Color.GREEN);
                bullets.add(bullet);
            }
        }
        return bullets;
    }

    public Set<BossBullet> shootSpellCard(Ship playerShip) {
        Set<BossBullet> bullets = new HashSet<>();
        
        // SINGLE rotating stream
        if (this.spellCardShootCooldown.checkFinished()) {
            this.spellCardShootCooldown.reset();
            this.spellCardRotationAngle = (this.spellCardRotationAngle + 4) % 360; // Adjusted rotation speed

            for (int i = 0; i < this.numberOfNozzles; i++) {
                double nozzleAngle = 360.0 / this.numberOfNozzles * i;
                double totalAngle = Math.toRadians(this.spellCardRotationAngle + nozzleAngle);
                
                int speedX = (int) (Math.cos(totalAngle) * 2); // Adjusted bullet speed
                int speedY = (int) (Math.sin(totalAngle) * 2); // Adjusted bullet speed

                BossBullet bullet = new BossBullet(
                        this.getPositionX() + this.getWidth() / 2,
                        this.getPositionY() + this.getHeight() / 2,
                        speedX, speedY, 6, 10, Color.ORANGE);
                bullets.add(bullet);
            }
        }

        // Aimed FAN of bullets
        if (playerShip != null && this.aimedShotCooldown.checkFinished()) {
            this.aimedShotCooldown.reset();
            double angleToPlayer = Math.atan2(playerShip.getPositionY() - (this.positionY + this.height / 2),
                                              playerShip.getPositionX() - (this.positionX + this.width / 2));
            
            int fanBullets = 3; // 3-shot fan
            double spread = Math.toRadians(20); // 20 degree spread
            for (int i = -(fanBullets / 2); i <= fanBullets / 2; i++) {
                double bulletAngle = angleToPlayer + (i * spread / (fanBullets -1));
                int speedX = (int) (Math.cos(bulletAngle) * 2); // Adjusted bullet speed
                int speedY = (int) (Math.sin(bulletAngle) * 2); // Adjusted bullet speed
                BossBullet bullet = new BossBullet(
                            this.getPositionX() + this.getWidth() / 2,
                            this.getPositionY() + this.getHeight() / 2,
                            speedX, speedY, 10, 14, Color.YELLOW);
                bullets.add(bullet);
            }
        }

        return bullets;
    }

    @Override
    public void destroy(){
        if(!this.isDestroyed){
            this.isDestroyed = true;
        }
    }

    @Override
    public boolean isDestroyed(){
        return this.isDestroyed;
    }

    public boolean isSpellCardActive() {
        return this.isSpellCardActive;
    }

    public boolean hasSurvivedSpellCard() {
        return this.survivedLastSpellCard;
    }

    public void resetSurvivalFlag() {
        this.survivedLastSpellCard = false;
    }

    @Override
    public void draw(DrawManager drawManager) {
        // Unused
    }

    public void draw(Graphics g) {
        int drawnWidth = this.width;
        int drawnHeight = this.height;

        if (this.currentSprite != null) {
            g.drawImage(this.currentSprite, this.positionX, this.positionY, drawnWidth, drawnHeight, null);
        }

        int barWidth = drawnWidth;
        int barHeight = 10;
        int barX = this.positionX;
        int barY = this.positionY - barHeight - 5;

        g.setColor(Color.BLACK);
        g.fillRect(barX, barY, barWidth, barHeight);

        float healthPercentage;
        healthPercentage = (float) this.healPoint / this.maxHp;
        if (healthPercentage < 0) healthPercentage = 0;
        int healthBarWidth = (int) (barWidth * healthPercentage);
        g.setColor(Color.RED);
        g.fillRect(barX, barY, healthBarWidth, barHeight);

        g.setColor(Color.WHITE);
        g.drawRect(barX, barY, barWidth, barHeight);

        if (this.currentSpellCardName != null && this.spellCardAnnounceCooldown != null && !this.spellCardAnnounceCooldown.checkFinished()) {
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.setColor(Color.YELLOW);
            String text = "Spell Card: " + this.currentSpellCardName;
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textX = (screenWidth - textWidth) / 2;
            int textY = screenHeight / 4;
            g.drawString(text, textX, textY);
        }
    }
}
