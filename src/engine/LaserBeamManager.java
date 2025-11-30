package engine;

import entity.Ship;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LaserBeamManager {

    private final List<LaserBeam> beams = new ArrayList<>();

    // 레이저 추가
    public void addBeam(LaserBeam beam) {
        beams.add(beam);
    }

    // 매 프레임마다 레이저 업데이트 및 제거
    public void update() {
        Iterator<LaserBeam> it = beams.iterator();
        while (it.hasNext()) {
            LaserBeam beam = it.next();
            beam.update();

            if (beam.isExpired()) {
                it.remove();
            }
        }
    }

    public List<LaserBeam> getBeams() {
        return beams;
    }

    public void clear() {
        beams.clear();
    }
    public boolean checkCollisionWithShip(Ship ship) {

        float shipX = ship.getPositionX() + ship.getWidth() / 2f;
        float shipY = ship.getPositionY() + ship.getHeight() / 2f;

        for (LaserBeam beam : beams) {
            if (isPointInsideBeam(shipX, shipY, beam)) {
                return true;
            }
        }
        return false;
    }
    private boolean isPointInsideBeam(float x, float y, LaserBeam beam) {

        float x1 = beam.getOriginX();
        float y1 = beam.getOriginY();

        float angle = beam.getAngle();
        float length = beam.getLength();
        float halfThickness = beam.getThickness() / 2f;

        float dirX = (float) Math.cos(angle);
        float dirY = (float) Math.sin(angle);

        float perpX = (float) Math.sin(angle);
        float perpY = (float) -Math.cos(angle);


        float dx = x - x1;
        float dy = y - y1;

        float projAlong = dx * dirX + dy * dirY;
        float projPerp  = dx * perpX + dy * perpY;

        if (projAlong < 0 || projAlong > length)
            return false;

        return Math.abs(projPerp) <= halfThickness;
    }
}