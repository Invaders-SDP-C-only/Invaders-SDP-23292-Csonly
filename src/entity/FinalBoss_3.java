package entity;

import audio.SoundManager;
import engine.*;

import java.awt.Color;
import java.security.SecureRandom;
import java.util.*;
import java.util.List;
import java.util.ArrayList;
public class FinalBoss_3 extends Entity implements BossEntity {

    private SecureRandom random;
    private int healPoint;
    private int maxHP;
    private int pointValue;
    private boolean destroyed = false;

    private int screenWidth;
    private int screenHeight;

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

    // Boss wave (spin laser) state
    private boolean bossWaveTransition = false;
    private boolean bossWaveActive = false;
    private boolean wave20Triggered = false;
    private boolean wave5Triggered = false;
    //private long bossWaveStartTime = 0;
    private float bossWaveAngularSpeedDegPhase1 = -22f; // 시계 방향(음수)
    private float bossWaveAngularSpeedDegPhase2 = -26f; // 2번째 웨이브에서 조금 더 빠르게
    private float bossWaveAngularSpeedDeg = -22f;
    private float bossWaveAngleDeg = 0f;
    private float bossWaveRotationAccum = 0f;
    private int bossWaveDirection = -1;
    private boolean bossWaveReverseDone = false;
    private long bossWaveLastUpdateTime = 0;
    private boolean bossWavePauseBeforeReverse = false;
    private long bossWavePauseStart = 0;
    private long bossWavePauseMs = 1800;
    private boolean bossWavePreAlert = false;
    private long bossWavePreAlertStart = 0;
    private long bossWavePreAlertMs = 1500;
    //private int bossWaveTargetX;
    //private int bossWaveTargetY;
    private Cooldown bossWaveFireCooldown;
    private int bossWaveBeamDurationMs = 1800;
    private long bossWaveTransitionStartTime = 0;
    private long bossWaveMinWarningMs = 2000;
    private int bossWaveCount = 0;
    private boolean bossWaveWarningMode = false;
    private long bossWaveWarningEndTime = 0;
    private long bossWaveStartWarnMs = 1000;

    public float getWarningOriginX() {
        return this.positionX + this.width / 2f;
    }

    public float getWarningOriginY() {
        return this.positionY + (float)this.height;
    }

    public FinalBoss_3(int x, int y, int screenWidth, int screenHeight, LaserBeamManager laserBeamManager) {
        super(x, y, 90, 60, Color.ORANGE);

        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        this.random = new SecureRandom();

        this.maxHP = 100;
        this.healPoint = this.maxHP;

        this.pointValue = 700;
        this.spriteType = DrawManager.SpriteType.FinalBoss1;

        this.moveCooldown = Core.getCooldown(30);
        this.threeWayCooldown = Core.getCooldown(1500);

        this.laserBeamManager = laserBeamManager;

        this.laserCooldown = Core.getCooldown(3000);
        this.laserCooldown.reset();

        this.bossWaveFireCooldown = Core.getCooldown(200);
        this.bossWaveFireCooldown.reset();
    }

    @Override
    public void update() {

        if (bossWavePreAlert) {
            if (System.currentTimeMillis() - bossWavePreAlertStart >= bossWavePreAlertMs) {
                bossWavePreAlert = false;
                bossWaveTransition = true;
                bossWaveTransitionStartTime = System.currentTimeMillis();
                // Moving to center: 더 이상 경고선을 표시하지 않는다.
                laserWarningActive = false;
                pendingWarningAngles.clear();
            } else {
                return;
            }
        }

        if (bossWaveTransition) {
            if (moveToCenter()) {
                if (System.currentTimeMillis() - bossWaveTransitionStartTime >= bossWaveMinWarningMs) {
                    beginBossWave();
                }
            }
            return;
        }

        if (bossWaveActive) {
            updateBossWave();
            return;
        }

        if (laserWarningActive) {

            if (System.currentTimeMillis() >= laserWarningEndTime) {
                laserWarningActive = false;

                if (healPoint < maxHP * 0.2f) {
                    for (float angle : pendingWarningAngles) {
                        fireLaser(angle, false);
                    }
                    pendingWarningAngles.clear();
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

    private boolean moveToCenter() {
        int targetX = (screenWidth - width) / 2;
        int targetY = (screenHeight - height) / 2 - 50; // slightly above center
        //bossWaveTargetX = targetX;
        //bossWaveTargetY = targetY;

        int speed = 5; // slightly slower approach to center
        if (positionX < targetX) positionX = Math.min(positionX + speed, targetX);
        else if (positionX > targetX) positionX = Math.max(positionX - speed, targetX);

        if (positionY < targetY) positionY = Math.min(positionY + speed, targetY);
        else if (positionY > targetY) positionY = Math.max(positionY - speed, targetY);

        return positionX == targetX && positionY == targetY;
    }

    private void beginBossWave() {
        bossWaveCount++;
        bossWaveAngularSpeedDeg = (bossWaveCount >= 2) ? bossWaveAngularSpeedDegPhase2 : bossWaveAngularSpeedDegPhase1;
        bossWaveTransition = false;
        bossWaveActive = true;
        //bossWaveStartTime = System.currentTimeMillis();
        bossWaveFireCooldown.reset();
        bossWaveAngleDeg = 0f;
        bossWaveRotationAccum = 0f;
        bossWaveDirection = -1;
        bossWaveReverseDone = false;
        bossWaveLastUpdateTime = System.currentTimeMillis();
        // Show a short red warning before the rotation begins.
        pendingWarningAngles.clear();
        for (int i = 0; i < 8; i++) {
            float angle = (360f / 8) * i;
            pendingWarningAngles.add(angle);
        }
        bossWaveWarningMode = true;
        laserWarningActive = true;
        bossWaveWarningEndTime = System.currentTimeMillis() + bossWaveStartWarnMs;
        isLaserFiring = false;
    }

    private void updateBossWave() {
        long now = System.currentTimeMillis();
        long deltaMs = now - bossWaveLastUpdateTime;
        if (deltaMs < 0) deltaMs = 0;
        bossWaveLastUpdateTime = now;
        float deltaSec = deltaMs / 1000f;

        // Keep warnings visible briefly before rotation starts.
        if (bossWaveWarningMode) {
            if (now < bossWaveWarningEndTime) {
                return;
            } else {
                bossWaveWarningMode = false;
                laserWarningActive = false;
                pendingWarningAngles.clear();
                bossWaveLastUpdateTime = now; // reset timing to avoid jump
            }
        }

        if (bossWavePauseBeforeReverse) {
            if (now - bossWavePauseStart >= bossWavePauseMs) {
                bossWavePauseBeforeReverse = false;
                bossWaveDirection *= -1;
                bossWaveRotationAccum = 0f;
            } else {
                // Pause movement but keep beams alive
                return;
            }
        }

        bossWaveAngleDeg += bossWaveAngularSpeedDeg * bossWaveDirection * deltaSec;
        bossWaveRotationAccum += Math.abs(bossWaveAngularSpeedDeg * deltaSec);

        if (bossWaveRotationAccum >= 360f) {
            if (!bossWaveReverseDone) {
                bossWaveReverseDone = true;
                bossWavePauseBeforeReverse = true;
                bossWavePauseStart = now;
                bossWaveRotationAccum = 0f;
            } else {
                bossWaveActive = false;
                bossWaveWarningMode = false;
                laserWarningActive = false;
                pendingWarningAngles.clear();
                return;
            }
        }

        if (bossWaveFireCooldown.checkFinished()) {
            bossWaveFireCooldown.reset();
            fireRotatingLaser((float) Math.toRadians(bossWaveAngleDeg));
        }
    }

    private void fireRotatingLaser(float baseAngleRad) {
        // Wave 연출 시 잔상이 겹치지 않도록, 이전 세트를 지우고 현재 세트만 남긴다.
        laserBeamManager.clear();
        for (int i = 0; i < 8; i++) {
            float angle = (float) (baseAngleRad + Math.toRadians(45 * (double)i));
            spawnLaser(angle, bossWaveBeamDurationMs);
        }
        // keep beam count bounded to avoid buildup
        List<LaserBeam> beams = laserBeamManager.getBeams();
        int maxBeams = 24;
        if (beams.size() > maxBeams) {
            int removeCount = beams.size() - maxBeams;
            for (int i = 0; i < removeCount && !beams.isEmpty(); i++) {
                beams.remove(0);
            }
        }
    }

    private void spawnLaser(float angleRad, long durationMs) {
        float originX = positionX + width / 2f;
        float originY = positionY + (float)height;

        LaserBeam beam = new LaserBeam(
                originX, originY,
                (float) Math.toDegrees(angleRad),
                screenHeight,
                12f,
                durationMs
        );

        laserBeamManager.addBeam(beam);
    }

    private boolean shouldStartBossWave() {
        if (bossWaveActive || bossWaveTransition) return false;

        float hpPercent = (float) healPoint / maxHP;
        if (!wave20Triggered && hpPercent <= 0.10f) {
            wave20Triggered = true;
            return true;
        }
        if (!wave5Triggered && hpPercent <= 0.05f) {
            wave5Triggered = true;
            return true;
        }
        return false;
    }

    private void startBossWaveTransitionIfNeeded() {
        if (shouldStartBossWave()) {
            bossWavePreAlert = true;
            bossWavePreAlertStart = System.currentTimeMillis();
            pendingWarningAngles.clear();
            for (int i = 0; i < 8; i++) {
                float angle = (360f / 8) * i;
                startLaserWarning(angle, bossWaveMinWarningMs);
            }
        }
    }

    private void movePattern() {
        if (!moveCooldown.checkFinished()) return;
        moveCooldown.reset();
        int speed;
        if (healPoint >= maxHP * 0.5f)      speed = 3;
        else if (healPoint >= maxHP * 0.2f) speed = 5;
        else                                 speed = 7;

        if (this.random.nextDouble() < 0.12) {
            double r = this.random.nextDouble();
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
            originY = positionY + (float)height;
        }

        LaserBeam beam = new LaserBeam(
                originX, originY,
                angle,
                screenHeight,
                12f,
                800
        );

        laserBeamManager.addBeam(beam);
        SoundManager.play("sfx/boss_laser.wav");

        isLaserFiring = true;
        laserEndtime = System.currentTimeMillis() + 800;
    }
    public Set<BossBullet> shoot() {

        Set<BossBullet> bullets = new HashSet<>();

        if (bossWaveActive || bossWaveTransition || bossWavePreAlert) {
            return bullets;
        }
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

        bossWaveWarningMode = false;
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
    }
    @Override
    public void takeDamage(int dmg) {
        // 웨이브 준비(프리알럿/중앙 이동) 및 웨이브 진행 중에는 무적.
        if (bossWavePreAlert || bossWaveTransition || bossWaveActive) return;

        healPoint -= dmg;
        startBossWaveTransitionIfNeeded();
        if (healPoint <= 0) destroy();
    }

    @Override
    public void destroy() {
        destroyed = true;
        spriteType = DrawManager.SpriteType.FinalBossDeath;
    }

    @Override public int getHealPoint() { return healPoint; }
    public int getMaxHp() { return maxHP; }
    @Override public int getPointValue() { return pointValue; }
    @Override public boolean isDestroyed() { return destroyed; }
    @Override public void move(int dx, int dy) { positionX += dx; positionY += dy; }

    @Override
    public void draw(DrawManager drawManager) {
        drawManager.drawLaserBoss(this);
    }
    public boolean isLaserWarningActive() {
        return laserWarningActive;
    }
    public List<Float> getPendingWarningAngles() {
        return pendingWarningAngles;
    }

    public boolean isBossWaveActiveOrTransition() {
        return bossWaveActive || bossWaveTransition || bossWavePreAlert;
    }

    public boolean isBossWaveActive() {
        return bossWaveActive;
    }
    public boolean isBossWaveWarningActive() { return bossWaveWarningMode && laserWarningActive; }

}
