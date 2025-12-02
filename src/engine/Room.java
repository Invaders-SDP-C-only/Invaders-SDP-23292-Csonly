package engine;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import entity.EnemyShipFormation;
import entity.Entity;

public class Room {
    public enum RoomType { START, NORMAL, BOSS }
    public enum Direction { NORTH, SOUTH, WEST, EAST }

    public static class Door extends Entity {
        public Direction direction;
        public boolean isBossDoor;
        public Door(int x, int y, int w, int h, Direction dir, boolean boss) {
            super(x, y, w, h, boss ? Color.YELLOW : Color.CYAN);
            this.direction = dir;
            this.isBossDoor = boss;
        }
    }

    public static class Bonfire extends Entity {
        public Bonfire(int x, int y) {
            super(x, y, 20, 20, Color.ORANGE);
        }
    }

    private RoomType type;
    private EnemyShipFormation enemyFormation;
    private boolean isCleared;
    private List<Door> doors;
    private Bonfire bonfire;
    private int row, col;
    private int levelNumber;

    public Room(int row, int col, RoomType type, EnemyShipFormation formation, int levelNum) {
        this.row = row;
        this.col = col;
        this.type = type;
        this.enemyFormation = formation;
        this.levelNumber = levelNum;
        this.isCleared = (type == RoomType.START);
        this.doors = new ArrayList<>();
    }

    public void update() {
        if (enemyFormation != null && !isCleared) {
            enemyFormation.update();
            if (enemyFormation.isEmpty()) {
                this.isCleared = true;
            }
        }
    }

    public void draw(engine.DrawManager dm) {
        // [수정] SpriteType 경고 방지
        if (enemyFormation != null && !isCleared) enemyFormation.draw();

        for (Door d : doors) {
            // 이미지가 없으므로 사각형으로 그리기
            dm.drawRectangle(d.getPositionX(), d.getPositionY(), d.getWidth(), d.getHeight(), d.getColor());
        }
        if (bonfire != null) {
            dm.drawRectangle(bonfire.getPositionX(), bonfire.getPositionY(), bonfire.getWidth(), bonfire.getHeight(), bonfire.getColor());
        }
    }

    public void addDoor(Direction dir, boolean isBossDoor, int screenWidth, int screenHeight) {
        int length = 60;
        int thickness = 10;
        int x = 0, y = 0;
        int padding = 10;
        int topOffset = 50;

        switch(dir) {
            case NORTH: x = screenWidth/2 - length/2; y = topOffset + padding; doors.add(new Door(x, y, length, thickness, dir, isBossDoor)); break;
            case SOUTH: x = screenWidth/2 - length/2; y = screenHeight - thickness - padding; doors.add(new Door(x, y, length, thickness, dir, isBossDoor)); break;
            case WEST: x = padding; y = (screenHeight + topOffset)/2 - length/2; doors.add(new Door(x, y, thickness, length, dir, isBossDoor)); break;
            case EAST: x = screenWidth - thickness - padding; y = (screenHeight + topOffset)/2 - length/2; doors.add(new Door(x, y, thickness, length, dir, isBossDoor)); break;
        }
    }

    public void addBonfire(int screenWidth, int screenHeight) {
        this.bonfire = new Bonfire(screenWidth/2 - 10, screenHeight/2 + 50);
    }

    public boolean isLocked() { return (type == RoomType.BOSS && !isCleared); }
    public boolean isCleared() { return isCleared; }
    public void setCleared(boolean cleared) { this.isCleared = cleared; }
    public RoomType getType() { return type; }
    public EnemyShipFormation getEnemyFormation() { return enemyFormation; }
    public List<Door> getDoors() { return doors; }
    public Bonfire getBonfire() { return bonfire; }
    public int getLevelNumber() { return levelNumber; }
}