package entity;


import audio.SoundManager;
import engine.DrawManager;
import engine.Cooldown;
import engine.Core;
import screen.GameScreen;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import java.util.logging.Logger;
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
    private Cooldown laserWarningCooldown;
    private int laserWarningX;
    private int laserWarningStartY;
    private boolean laserWarningActive = false;
    public FinalBoss_3(int x, int y, int screenWidth, int screenHeight) {
        super(x, y, 90, 60, Color.ORANGE);
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        this.healPoint = 60;
        this.maxHP = healPoint;
        this.pointValue = 700;

        this.spriteType = DrawManager.SpriteType.FinalBoss1;

        this.moveCooldown = Core.getCooldown(30);
        this.threeWayCooldown = Core.getCooldown(1500);
        this.laserCooldown = Core.getCooldown(4000);
        this.laserWarningCooldown = Core.getCooldown(800);

    }

    @Override
    public void update() {
        movePattern();
    }

    private void movePattern() {
        if (!moveCooldown.checkFinished()) return;
        moveCooldown.reset();

        int speedX;
        if (healPoint > maxHP * 2 / 3) {
            speedX = 2;
        } else if (healPoint > maxHP / 3) {
            speedX = 3;
        } else {
            speedX = 4;
        }

        this.positionX += moveDirX * speedX;
        int speedY = Math.max(1, speedX - 1);
        this.positionY += moveDirY * speedY;

        if (positionX < 20 || positionX > screenWidth - width - 20) {
            moveDirX *= -1;
        }
        if (positionY < 50 || positionY > screenHeight / 2 - height) {
            moveDirY *= -1;
        }
    }

    public Set<BossBullet> shoot() {
        Set<BossBullet> bullets = new HashSet<>();

        bullets.addAll(shoot3way());
        bullets.addAll(shootLaser());

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

    private Set<BossBullet> shootLaser() {
        Set<BossBullet> bullets = new HashSet<>();
        if (laserWarningActive) {
            if (!laserWarningCooldown.checkFinished()) {
                return bullets;
            }
            laserWarningActive = false;
            laserWarningCooldown.reset();

            int laserHeight = screenHeight - laserWarningStartY;
            bullets.add(new BossBullet(
                    laserWarningX,
                    laserWarningStartY,
                    0, 0,
                    4, laserHeight,
                    Color.CYAN
            ));
            return bullets;
        }
        if (!laserCooldown.checkFinished())
            return bullets;

        laserCooldown.reset();

        laserWarningActive = true;
        laserWarningCooldown.reset();

        laserWarningX = positionX + width / 2;
        laserWarningStartY = positionY + height;

        return bullets;
    }

    @Override
    public void takeDamage(int dmg) {
        healPoint -= dmg;
        if (healPoint <= 0) destroy();
    }

    @Override public int getHealPoint() { return healPoint; }
    @Override public int getPointValue() { return pointValue; }
    @Override public void move(int dx, int dy) { positionX += dx; positionY += dy; }
    @Override public boolean isDestroyed() { return destroyed; }

    @Override
    public void destroy() {
        destroyed = true;
        spriteType = DrawManager.SpriteType.FinalBossDeath;
    }

    @Override
    public void draw(DrawManager drawManager) {
        drawManager.drawEntity(this, positionX, positionY);
    }
    public boolean isLaserWarningActive() {
        return laserWarningActive;
    }

    public int getLaserWarningX() {
        return laserWarningX;
    }

    public int getLaserWarningStartY() {
        return laserWarningStartY;
    }
}