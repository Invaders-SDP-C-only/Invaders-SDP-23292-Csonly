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
    private boolean laserActive = false;
    private long laserStartTime = 0L;
    private static final long LASER_DURATION_MS = 450;
    private BossBullet currentLaserBullet;


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
        this.laserActive = false;
        this.currentLaserBullet = null;

    }

    @Override
    public void update() {
        if (!laserWarningActive && !laserActive) {
            movePattern();
        }
        if (laserActive) {
            long elapsed = System.currentTimeMillis() - laserStartTime;
            if (elapsed >= LASER_DURATION_MS) {
                laserActive = false;
            }
        }
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

        // 1) 이미 예고선이 켜져 있는 상태라면 → 예고 시간이 끝났는지 체크
        if (laserWarningActive) {
            if (!laserWarningCooldown.checkFinished()) {
                // 아직 예고 시간 남음 → 이번 프레임에는 발사 안 함
                return bullets;
            }

            // 예고 종료 → 실제 레이저 발사
            laserWarningActive = false;
            laserWarningCooldown.reset();

            // 레이저 지속 시간 (게임스크린에서 isLaserActive()로 체크할 그거)
            this.laserActive = true;
            this.laserStartTime = System.currentTimeMillis();

            // === 여기서부터 "한 점에서 3갈래로 나가는" 패턴 ===

            int originX = laserWarningX;
            int originY = laserWarningStartY;

            int laserWidth = 9;      // 레이저 두께
            int laserLength = 500;    // 레이저 세로 길이 (짧은 막대)
            int baseSpeedY = 7;      // 공통 내려가는 속도
            int spreadSpeedX = 3;    // 좌우로 퍼지는 속도

            // 중앙 레이저 (직선)
            BossBullet center = new BossBullet(
                    originX,
                    originY,
                    0, baseSpeedY,          // straight down
                    laserWidth, laserLength,
                    Color.CYAN
            );
            bullets.add(center);
            this.currentLaserBullet = center;   // 수명 관리용(중앙 탄만 isLaserActive()로 제거)

            // 체력이 절반 이하일 때 → 좌우 레이저 추가 (삼각 패턴)
            if (healPoint <= maxHP / 2) {
                // 왼쪽 대각선 레이저
                BossBullet left = new BossBullet(
                        originX,
                        originY,
                        -spreadSpeedX, baseSpeedY,   // 왼쪽 아래로
                        laserWidth, laserLength,
                        Color.CYAN
                );

                // 오른쪽 대각선 레이저
                BossBullet right = new BossBullet(
                        originX,
                        originY,
                        spreadSpeedX, baseSpeedY,    // 오른쪽 아래로
                        laserWidth, laserLength,
                        Color.CYAN
                );

                bullets.add(left);
                bullets.add(right);
            }

            return bullets;
        }

        // 2) 아직 예고 상태가 아니면 → 쿨타임 체크
        if (!laserCooldown.checkFinished())
            return bullets;

        // 쿨타임 종료 → 예고선 ON
        laserCooldown.reset();

        laserWarningActive = true;
        laserWarningCooldown.reset();

        // 예고선 시작 위치 = 보스 가운데 아랫부분
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
    public boolean isLaserActive() {
        return laserActive;
    }
    public int getLaserWarningX() {
        return laserWarningX;
    }
    public int getLaserWarningStartY() {
        return laserWarningStartY;
    }
    public float getLaserAlpha() {
        if (!laserActive) return 0f;

        long elapsed = System.currentTimeMillis() - laserStartTime;
        float t = (float) elapsed / (float) LASER_DURATION_MS;

        if (t >= 1f) {
            laserActive = false;
            return 0f;
        }
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;

        if (t < 0.3f) {
            return 1.0f;
        } else {
            float fadeT = (t - 0.3f) / 0.7f;
            return 1.0f - fadeT;
        }
    }

    public BossBullet getCurrentLaserBullet() {
        return currentLaserBullet;
    }
    public float getLaserProgress() {
        if(!laserActive) return 0f;
        long elapsed = System.currentTimeMillis() - laserStartTime;
        float t = (float) elapsed / (float) LASER_DURATION_MS;
        if (t >= 0f) t = 0f;
        if(t > 1f) t = 1f;
        return t;
    }
}