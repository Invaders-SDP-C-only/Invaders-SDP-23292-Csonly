package entity;

import audio.SoundManager;
import engine.*;
import screen.GameScreen;

import java.awt.Color;
import java.util.*;
import java.util.List;
import java.util.ArrayList;
public class FinalBoss_3 extends Entity implements BossEntity {

    private int healPoint;
    private int maxHP;
    private int pointValue;
    private boolean destroyed = false;

    private int screenWidth, screenHeight;

    private int moveDirX = 1;
    private int moveDirY = 1;

    private Cooldown moveCooldown;
    private Cooldown threeWayCooldown;
    private Cooldown laserCooldown;

    private LaserBeamManager laserBeamManager;

    private boolean isLaserFiring = false;
    private long laserEndtime = 0;

    private boolean laserWarningActive = false;
    private long laserWarningEndTime = 0;

    private List<Float> pendingWarningAngles = new ArrayList<>();

    public float getWarningOriginX() {
        return this.positionX + this.width / 2f;
    }

    public float getWarningOriginY() {
        return this.positionY + this.height;
    }

    public FinalBoss_3(int x, int y, int screenWidth, int screenHeight, LaserBeamManager laserBeamManager) {
        super(x, y, 90, 60, Color.ORANGE);

        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        this.maxHP = 100;
        this.healPoint = 100;

        this.pointValue = 700;
        this.spriteType = DrawManager.SpriteType.FinalBoss1;

        this.moveCooldown = Core.getCooldown(30);
        this.threeWayCooldown = Core.getCooldown(1500);

        this.laserBeamManager = laserBeamManager;

        this.laserCooldown = Core.getCooldown(3000);
        this.laserCooldown.reset();
    }

    @Override
    public void update() {

        if (laserWarningActive) {

            if (System.currentTimeMillis() >= laserWarningEndTime) {
                laserWarningActive = false;

                if (healPoint < maxHP * 0.2f) {
                    for (float angle : pendingWarningAngles) {
                        fireLaser(angle, false);
                    }
                    pendingWarningAngles.clear();
                    return;
                } else {
                    for (float angle : pendingWarningAngles) {
                        fireLaser(angle, false);
                    }
                }
            }
            return;
        }

        if (isLaserFiring) {
            if (System.currentTimeMillis() >= laserEndtime) {
                isLaserFiring = false;
            }
            return;
        }

        movePattern();
    }
    private void startLaserWarning(float angle, long warnMS) {
        pendingWarningAngles.add(angle);

        laserWarningActive = true;
        laserWarningEndTime = System.currentTimeMillis() + warnMS;
    }
    private void movePattern() {
        if (!moveCooldown.checkFinished()) return;
        moveCooldown.reset();
        int speed;
        if (healPoint >= maxHP * 0.5f)      speed = 3;
        else if (healPoint >= maxHP * 0.2f) speed = 5;
        else                                 speed = 7;

        if (Math.random() < 0.12) {
            double r = Math.random();
            if (r < 0.125)      { moveDirX = 1;  moveDirY = 0;  }
            else if (r < 0.250) { moveDirX = -1; moveDirY = 0;  }
            else if (r < 0.375) { moveDirX = 0;  moveDirY = 1;  }
            else if (r < 0.500) { moveDirX = 0;  moveDirY = -1; }
            else if (r < 0.625) { moveDirX = 1;  moveDirY = 1;  }
            else if (r < 0.750) { moveDirX = 1;  moveDirY = -1; }
            else if (r < 0.875) { moveDirX = -1; moveDirY = 1;  }
            else                { moveDirX = -1; moveDirY = -1; }
        }

        positionX += moveDirX * speed;
        positionY += moveDirY * speed;

        if (positionX < 20) {
            positionX = 20;
            moveDirX *= -1;
        }
        if (positionX > screenWidth - width - 20) {
            positionX = screenWidth - width - 20;
            moveDirX *= -1;
        }

        if (positionY < 50) {
            positionY = 50;
            moveDirY *= -1;
        }
        if (positionY > screenHeight / 2 - height) {
            positionY = screenHeight / 2 - height;
            moveDirY *= -1;
        }
    }
    private void fireLaser(float angle, boolean centered) {

        float originX;
        float originY;

        if (centered) {
            originX = positionX + width / 2f;
            originY = positionY + height / 2f;
        } else {
            originX = positionX + width / 2f;
            originY = positionY + height;
        }

        LaserBeam beam = new LaserBeam(
                originX, originY,
                angle,
                screenHeight,
                12f,
                800
        );

        laserBeamManager.addBeam(beam);

        isLaserFiring = true;
        laserEndtime = System.currentTimeMillis() + 800;
    }
    public Set<BossBullet> shoot() {

        Set<BossBullet> bullets = new HashSet<>();
        bullets.addAll(shoot3way());

        if (!laserWarningActive && !isLaserFiring && laserCooldown.checkFinished()) {
            fireLaserPattern();
            laserCooldown.reset();
        }

        return bullets;
    }

    private Set<BossBullet> shoot3way() {
        if (!threeWayCooldown.checkFinished()) return Collections.emptySet();
        threeWayCooldown.reset();

        int cx = positionX + width / 2;

        Set<BossBullet> b = new HashSet<>();
        b.add(new BossBullet(cx, positionY + height, 0, 4, 6, 10, Color.ORANGE));
        b.add(new BossBullet(cx, positionY + height, -2, 4, 6, 10, Color.ORANGE));
        b.add(new BossBullet(cx, positionY + height, +2, 4, 6, 10, Color.ORANGE));

        return b;
    }

    private void fireLaserPattern() {

        if (healPoint >= maxHP * 0.5f) {
            pendingWarningAngles.clear();
            startLaserWarning(90f, 600);
            return;
        }

        if (healPoint >= maxHP * 0.2f) {
            pendingWarningAngles.clear();
            startLaserWarning(60f, 600);
            startLaserWarning(90f, 600);
            startLaserWarning(120f, 600);
            return;
        }

        pendingWarningAngles.clear();
        for (int i = 0; i < 8; i++) {
            float angle = (360f / 8) * i;
            startLaserWarning(angle, 600);
        }
        return;
    }
    @Override
    public void takeDamage(int dmg) {
        healPoint -= dmg;
        if (healPoint <= 0) destroy();
    }

    @Override
    public void destroy() {
        destroyed = true;
        spriteType = DrawManager.SpriteType.FinalBossDeath;
    }

    @Override public int getHealPoint() { return healPoint; }
    @Override public int getPointValue() { return pointValue; }
    @Override public boolean isDestroyed() { return destroyed; }
    @Override public void move(int dx, int dy) { positionX += dx; positionY += dy; }

    @Override
    public void draw(DrawManager drawManager) {
        drawManager.drawEntity(this, positionX, positionY);
    }
    public boolean isLaserWarningActive() {
        return laserWarningActive;
    }
    public List<Float> getPendingWarningAngles() {
        return pendingWarningAngles;
    }

}