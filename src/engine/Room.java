package engine;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import entity.EnemyShipFormation;
import entity.Entity;
import entity.BonFire;
import entity.Coin;
import entity.DropItem;

/**
 * Manages individual rooms within the Sandbox Mode map.
 * Each room can contain enemies, doors, a shop (Bonfire), and collectable coins.
 * It handles the room's state (cleared/locked), drawing, and updates.
 */
public class Room {

    /** Types of rooms available in Sandbox Mode. */
    public enum RoomType { START, NORMAL, BOSS }
    /** Directions for doors connecting rooms. */
    public enum Direction { NORTH, SOUTH, WEST, EAST }

    /** List of coins present in the room. */
    private List<Coin> coins;

    /**
     * Inner class representing a Door entity.
     * Connects to adjacent rooms.
     */
    public static class Door extends Entity {
        public Direction direction;
        public boolean isBossDoor;

        /**
         * Constructor for a Door.
         * @param x X coordinate.
         * @param y Y coordinate.
         * @param w Width.
         * @param h Height.
         * @param dir Direction (NORTH, SOUTH, etc.).
         * @param boss True if this door leads to a Boss room (Yellow color).
         */
        public Door(int x, int y, int w, int h, Direction dir, boolean boss) {
            super(x, y, w, h, boss ? Color.YELLOW : Color.CYAN);
            this.direction = dir;
            this.isBossDoor = boss;
        }
    }

    /** Type of this room. */
    private RoomType type;
    /** Formation of enemies in this room. */
    private EnemyShipFormation enemyFormation;
    /** Flag indicating if the room is cleared. */
    private boolean isCleared;
    /** List of doors in this room. */
    private List<Door> doors;
    /** Shop entity (Bonfire). Null if no shop. */
    private BonFire bonfire;
    /** Grid coordinates (Row, Col). */
    private int row, col;
    /** Difficulty level associated with this room. */
    private int levelNumber;
    /** Index for normal room background music (randomized). -1 if not set. */
    private int normalBgmIndex = -1;
    /** Set to contain items created in the rooms.*/
    private Set<DropItem> items;

    /**
     * Constructor, initializes the room properties.
     *
     * @param row Grid row index.
     * @param col Grid column index.
     * @param type Room type (START, NORMAL, BOSS).
     * @param formation Enemy formation for this room.
     * @param levelNum Difficulty level number.
     */
    public Room(int row, int col, RoomType type, EnemyShipFormation formation, int levelNum) {
        this.row = row;
        this.col = col;
        this.type = type;
        this.enemyFormation = formation;
        this.levelNumber = levelNum;

        // Start room is cleared by default.
        this.isCleared = (type == RoomType.START);

        this.doors = new ArrayList<>();
        this.normalBgmIndex = -1;
        this.coins = new ArrayList<>();
        this.items = new HashSet<>();
    }

    /**
     * Updates the logic for the room (e.g., enemy status).
     * Marks the room as cleared if all enemies are defeated.
     */
    public void update() {
        if (enemyFormation != null && !isCleared) {
            enemyFormation.update();
            if (enemyFormation.isEmpty()) {
                this.isCleared = true;
            }
        }
    }

    /**
     * Adds a coin to the room at the specified position.
     * @param x X coordinate.
     * @param y Y coordinate.
     */
    public void addCoin(int x, int y) {
        this.coins.add(new Coin(x, y));
    }

    /**
     * Item management methods of get the items.
     */
    public Set<DropItem> getItems() { return this.items; }

    /**
     * Item management methods of setting the items.
     * @param items Set to contain items created in the rooms.
     */
    public void setItems(Set<DropItem> items) { this.items = items; }

    /**
     * Returns the list of coins in the room.
     * @return List of Coin objects.
     */
    public List<Coin> getCoins() {
        return this.coins;
    }

    /**
     * Draws the room content (Enemies, Doors, Shop, Coins).
     * @param dm DrawManager instance.
     */
    public void draw(engine.DrawManager dm) {
        // Draw enemies if not cleared
        if (enemyFormation != null && !isCleared) enemyFormation.draw();

        // Lock mechanic: Hide doors in Boss rooms until cleared (Trap room)
        if (this.type == RoomType.BOSS && !this.isCleared) {
            return;
        }

        // Draw Doors
        for (Door d : doors) {
            dm.drawRectangle(d.getPositionX(), d.getPositionY(), d.getWidth(), d.getHeight(), d.getColor());
        }

        // Draw Shop (Bonfire) if present
        if (bonfire != null) {
            bonfire.draw(dm);
        }

        // Draw Coins
        for(Coin c : coins) {
            c.draw(dm);
        }
    }

    /**
     * Adds a door to the room.
     * Calculates position based on direction and screen size.
     *
     * @param dir Direction of the door.
     * @param isBossDoor True if it connects to a boss room.
     * @param screenWidth Width of the screen.
     * @param screenHeight Height of the screen.
     */
    public void addDoor(Direction dir, boolean isBossDoor, int screenWidth, int screenHeight) {
        int length = 60;
        int thickness = 10;
        int x = 0, y = 0;
        int padding = 10;
        int topOffset = 50; // Offset for UI bar

        switch(dir) {
            case NORTH: x = screenWidth/2 - length/2; y = topOffset + padding; doors.add(new Door(x, y, length, thickness, dir, isBossDoor)); break;
            case SOUTH: x = screenWidth/2 - length/2; y = screenHeight - thickness - padding; doors.add(new Door(x, y, length, thickness, dir, isBossDoor)); break;
            case WEST: x = padding; y = (screenHeight + topOffset)/2 - length/2; doors.add(new Door(x, y, thickness, length, dir, isBossDoor)); break;
            case EAST: x = screenWidth - thickness - padding; y = (screenHeight + topOffset)/2 - length/2; doors.add(new Door(x, y, thickness, length, dir, isBossDoor)); break;
        }
    }

    /**
     * Adds a default Bonfire (Shop) to the center of the room.
     * Used for the Start Room.
     *
     * @param screenWidth Screen width.
     * @param screenHeight Screen height.
     */
    public void addBonfire(int screenWidth, int screenHeight) {
        int size = 150;
        int x = screenWidth / 2 - size / 2;
        int y = screenHeight / 2 - size / 2;
        this.bonfire = new BonFire(x, y, size, size);
    }

    /**
     * Sets a specific Bonfire object for the room.
     * Used when spawning a shop after a boss fight.
     * @param bonfire BonFire entity.
     */
    public void setBonfire(BonFire bonfire) {
        this.bonfire = bonfire;
    }

    /**
     * Checks if the room is locked (Uncleared Boss Room).
     * @return True if locked.
     */
    public boolean isLocked() { return (type == RoomType.BOSS && !isCleared); }

    public boolean isCleared() { return isCleared; }
    public void setCleared(boolean cleared) { this.isCleared = cleared; }
    public RoomType getType() { return type; }
    public EnemyShipFormation getEnemyFormation() { return enemyFormation; }
    public List<Door> getDoors() { return doors; }
    public BonFire getBonfire() { return bonfire; }
    public int getLevelNumber() { return levelNumber; }
    public int getNormalBgmIndex() { return normalBgmIndex; }
    public void setNormalBgmIndex(int index) { this.normalBgmIndex = index; }
}