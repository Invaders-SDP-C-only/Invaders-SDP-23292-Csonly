package engine;

import java.awt.*;

public class LaserBeam {
    private float originX;
    private float originY;
    private float angle;
    private float length;
    private float thickness;

    private long StartTime;
    private long durationMs;
    private boolean expired = false;
    public LaserBeam(float originX, float originY,
                     float angleDegress, float length,
                     float thickness, long durationMs) {
        this.originX = originX;
        this.originY = originY;
        this.angle = (float)Math.toRadians(angleDegress);
        this.length = length;
        this.thickness = thickness;
        this.durationMs = durationMs;

        this.StartTime = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return expired;
    }


    public void update() {
        long elapsed = System.currentTimeMillis() - StartTime;
        if (elapsed > durationMs) {
            expired = true;
        }
    }
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
