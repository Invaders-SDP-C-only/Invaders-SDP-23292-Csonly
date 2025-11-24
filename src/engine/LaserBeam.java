package engine;

import java.awt.*;

public class LaserBeam {
    private float originX; // 레이저가 X에서 시작
    private float originY; // 레이저가 Y에서 시작
    private float angle; // 레이저의 발사 방향(라디안)
    private float length; // 레이저가 화면 끝까지 뻗는 길이
    private float thickness; // 레이저의 발사 두께

    private long StartTime; // 레이저의 생성 시각
    private long durationMs; // 레이저가 얼마나 화면에서 유지되는지 (페이드 효과 포함 시간)
    private boolean expired = false;
    public LaserBeam(float originX, float originY,
                     float angleDegress, float length,
                     float thickness, long durationMs) {
        this.originX = originX;
        this.originY = originY;
        this.angle = (float)Math.toRadians(angleDegress); // 도 -> 라디안으로 변화
        this.length = length;
        this.thickness = thickness;
        this.durationMs = durationMs;

        this.StartTime = System.currentTimeMillis();
    }
    // 레이저가 살아있는지 여부
    public boolean isExpired() {
        return expired;
    }

    // 프레임마다 레이저 시간 업데이트
    public void update() {
        long elapsed = System.currentTimeMillis() - StartTime;
        if (elapsed > durationMs) {
            expired = true;
        }
    }
    //0.0 ~ 1.0의 레이저 투명도 개수
    public float getAlphaFactor() {
        long elapsed = System.currentTimeMillis() - StartTime;
        return Math.max(0f, 1f - (float)elapsed / durationMs);
    }
    public float getOriginX() { return originX; }
    public float getOriginY() { return originY; }
    public float getAngle() { return angle; }
    public float getLength() { return length; }
    public float getThickness() { return thickness; }
}
