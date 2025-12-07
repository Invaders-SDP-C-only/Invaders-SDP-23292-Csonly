package screen;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Random;
import java.util.Iterator;
import java.util.List;

import audio.SoundManager;
import engine.*;
import engine.Room;
import engine.Room.RoomType;
import engine.Room.Direction;
import engine.Room.Door;
import engine.level.ItemDrop;
import engine.level.Level;
import engine.level.LevelManager;
import entity.*;
import entity.Coin;

/**
 * Implements the Sandbox Mode screen.
 * <p>
 * This class manages the open-world gameplay features including:
 * <ul>
 * <li>5x5 Room Grid Exploration</li>
 * <li>Persistent State (Cleared rooms, Boss status)</li>
 * <li>Various Boss Fights (Samurai, Laser, Touhou-style, etc.)</li>
 * <li>Shop System (Bonfire)</li>
 * <li>Visual Effects (Screen Shake, Hit Stop)</li>
 * </ul>
 *
 * @author Amartsogt / CHO
 */
public class SandboxScreen extends Screen {

    /** Map grid size (5x5). */
    private static final int MAP_SIZE = 5;
    /** Height of the interface separation line. */
    private static final int SEPARATION_LINE_HEIGHT = 45;
    /** Height of the items separation line. */
    private static final int ITEMS_SEPARATION_LINE_HEIGHT = 400;

    /** 2D Array representing the room grid. */
    private Room[][] map;
    /** Current room row index. */
    private int currentRoomRow;
    /** Current room column index. */
    private int currentRoomCol;
    /** Set of coordinates ("row,col") for cleared boss rooms. */
    private Set<String> clearedBossRooms;

    /** Player's ship. */
    private Ship ship;
    /** Second Player's ship. */
    private Ship shipP2;
    /** Player 1 lives left. */
    private int livesP1;
    /** Player 2 lives left. */
    private int livesP2;
    /** Current score. */
    private int score;
    /** Current coin amount. */
    private int coin;
    /** Saved starting row (for resumption). */
    private int startRow = -1;
    /** Saved starting column (for resumption). */
    private int startCol = -1;

    // === Visual Effects Variables ===
    /** Screen shake intensity. */
    private int shakeIntensity = 0;
    /** Duration of screen shake. */
    private int shakeDuration = 0;
    /** Hit stop duration (frames to freeze game logic). */
    private int hitStopFrames = 0;
    /** List of active visual particles. */
    private List<Particle> particles;

    /** Set of bullets fired by players. */
    private Set<Bullet> bullets;
    /** Set of sword waves fired by Samurai Boss. */
    private Set<SwordWave> swordWaves;
    /** Set of items dropped by enemies. */
    private Set<DropItem> dropItems;
    /** Set of bullets fired by bosses. */
    private Set<BossBullet> bossBullets;
    /** Message to display on screen (e.g. Parry instructions). */
    private String instructionMessage;
    /** Cooldown for instruction message display. */
    private Cooldown instructionCooldown;

    /** Set of temporary visual effects (Explosions, etc.). */
    private Set<TemporaryEffect> effects;

    /** Current active boss entity. */
    private Entity currentBoss;
    /** Reference to Samurai Boss (for specific logic). */
    private SamuraiBoss samuraiBoss;
    /** Laser manager for FinalBoss_3. */
    private LaserBeamManager laserBeamManager;
    /** Shop icon image (Bonfire). */
    private BufferedImage shopImage;
    /** Reference to True Final Boss. */
    private TrueFinalBoss trueFinalBoss;

    /** Input delay cooldown before game starts. */
    private Cooldown inputDelay;
    /** Game timer for tracking playtime. */
    private GameTimer gameTimer;
    /** Game Over flag. */
    private boolean isGameOver = false;
    /** Set of cleared normal rooms. */
    private Set<String> clearedRooms;

    // === Cheat Modes ===
    /** Cheat Mode flag (Infinite Money, High Damage). Toggled with F10. */
    private boolean isCheatMode = false;
    /** God Mode flag (Invincibility). Toggled with F12. */
    private boolean isGodMode = false;

    /** Total elapsed time displayed on UI. */
    private long elapsedTime;
    /** Time recorded when paused to prevent reset. */
    private long recordedTime = 0;

    // === Pause System ===
    /** Pause state flag. */
    private boolean isPaused = false;
    /** Selected option in pause menu. */
    private int pauseSelection = 0;
    /** Cooldown for menu selection input. */
    private Cooldown selectionCooldown;

    /** Parry effect cooldown. */
    private Cooldown parrySparkCooldown;
    /** Parry spark effect entities. */
    private Entity parrySparkEffect, parrySparkEffect2;
    /** Level manager instance. */
    private static LevelManager levelManager;
    /** Checks shooting key is pressed. */
    private boolean isShootingP1 = false;
    private boolean isShootingP2 = false;

    /**
     * Returns the Y-coordinate of the bottom boundary.
     * @return Separation line height.
     */
    public static int getItemsSeparationLineHeight() {
        return ITEMS_SEPARATION_LINE_HEIGHT;
    }

    /**
     * Inner class for managing temporary visual effects (e.g., explosions).
     */
    private class TemporaryEffect {
        Entity entity;
        Cooldown cooldown;

        public TemporaryEffect(Entity entity, int duration) {
            this.entity = entity;
            this.cooldown = Core.getCooldown(duration);
            this.cooldown.reset();
        }

        public boolean isFinished() {
            return cooldown.checkFinished();
        }
    }

    /**
     * Inner class for particle effects (Sparks, Debris).
     */
    private class Particle {
        double x, y;
        double dx, dy;
        Color color;
        int life;
        int size;

        public Particle(double x, double y, Color color, int speed, int size) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.size = size;
            this.life = 10 + (int)(Math.random() * 10); // 10~20 frames life
            double angle = Math.random() * Math.PI * 2;
            double spd = Math.random() * speed;
            this.dx = Math.cos(angle) * spd;
            this.dy = Math.sin(angle) * spd;
        }

        public boolean update() {
            x += dx;
            y += dy;
            life--;
            return life > 0;
        }

        public void draw(Graphics2D g) {
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(255, life * 25)));
            g.fillRect((int)x, (int)y, size, size);
        }
    }

    /**
     * Constructor, initializes the Sandbox Screen.
     *
     * @param gameState Current game state.
     * @param width Screen width.
     * @param height Screen height.
     * @param fps Frames per second.
     * @param savedClearedBossRooms Set of cleared boss rooms (Persistence).
     * @param savedClearedRooms Set of cleared normal rooms (Persistence).
     * @param startRow Row to start player at.
     * @param startCol Column to start player at.
     */
    public SandboxScreen(final GameState gameState, final int width, final int height, final int fps,
                         Set<String> savedClearedBossRooms, Set<String> savedClearedRooms,
                         int startRow, int startCol) {
        super(width, height, fps);
        levelManager = new LevelManager();
        this.livesP1 = gameState.getLivesRemaining();
        this.livesP2 = gameState.getLivesRemainingP2();
        this.score = gameState.getScore();
        this.coin = gameState.getCoin();
        this.clearedRooms = (savedClearedRooms != null) ? savedClearedRooms : new HashSet<>();
        this.clearedBossRooms = (savedClearedBossRooms != null) ? savedClearedBossRooms : new HashSet<>();

        this.selectionCooldown = Core.getCooldown(200);
        this.instructionCooldown = Core.getCooldown(3000);
        this.selectionCooldown.reset();
        this.startRow = startRow;
        this.startCol = startCol;
        this.particles = new ArrayList<>();

        // Load Shop Image (Bonfire)
        try {
            InputStream is = FileManager.class.getClassLoader().getResourceAsStream("bonfire.png");
            if (is == null) {
                logger.warning("Shop image not found, using placeholder.");
            }
            if (is != null) {
                this.shopImage = ImageIO.read(is);
            }
        } catch (IOException e) {
            logger.severe("Failed to load shop image!");
            e.printStackTrace();
        }
        this.initialize();
    }

    /**
     * Initializes basic screen properties and entities.
     */
    @Override
    public void initialize() {
        super.initialize();
        this.ship = new Ship(this.width / 2 - 20, this.height / 2, Color.GREEN);
        this.ship.setPlayerId(1);
        if (this.livesP2 > 0) {
            this.shipP2 = new Ship(this.width / 2 + 20, this.height / 2, Color.PINK);
            this.shipP2.setPlayerId(2);
        }

        this.bullets = new HashSet<>();
        this.swordWaves = new HashSet<>();
        this.bossBullets = new HashSet<>();
        this.dropItems = new HashSet<>();
        this.effects = new HashSet<>();
        this.laserBeamManager = new LaserBeamManager();

        createMap();
        this.currentRoomRow = 2;
        this.currentRoomCol = 2;

        this.inputDelay = Core.getCooldown(1000);
        this.inputDelay.reset();
        this.gameTimer = new GameTimer();
        this.particles = new ArrayList<>();

        this.parrySparkCooldown = Core.getCooldown(400);
        this.parrySparkEffect = new Entity(0, 0, 13*2, 8*2, Color.ORANGE);
        this.parrySparkEffect.spriteType = DrawManager.SpriteType.ShipDestroyed;
        this.parrySparkEffect2 = new Entity(0, 0, 13*2, 8*2, Color.ORANGE);
        this.parrySparkEffect2.spriteType = DrawManager.SpriteType.ShipDestroyed;
        this.parrySparkCooldown.checkFinished();
        this.elapsedTime = 0;

        if (this.startRow != -1 && this.startCol != -1) {
            this.currentRoomRow = this.startRow;
            this.currentRoomCol = this.startCol;
        } else {
            this.currentRoomRow = 2;
            this.currentRoomCol = 2;
        }

        enterRoom(true);
    }

    /**
     * Generates the 5x5 Map with randomized contents.
     * Determines Boss rooms, Normal rooms, and connects Doors.
     */
    private void createMap() {
        map = new Room[MAP_SIZE][MAP_SIZE];
        int center = 2;
        java.util.Random random = new java.util.Random();

        for (int row = 0; row < MAP_SIZE; row++) {
            for (int col = 0; col < MAP_SIZE; col++) {
                RoomType type = RoomType.NORMAL;
                EnemyShipFormation formation = null;
                int levelNum = ((row * MAP_SIZE + col) % 7) + 1;

                // 1. Determine Room Type
                if (row == center && col == center) type = RoomType.START;
                else if ((row == 0 && col == center) || (row == MAP_SIZE-1 && col == center) ||
                        (row == center && col == 0) || (row == center && col == MAP_SIZE-1)) type = RoomType.BOSS;

                // 2. Create Enemy Formation (for normal rooms) with random patterns
                if (type == RoomType.NORMAL) {
                    Level lvl = levelManager.getLevel(levelNum);
                    if (lvl != null) {
                        // Apply Random Patterns for Sandbox Mode
                        EnemyShipFormation.FormationPattern[] patterns = EnemyShipFormation.FormationPattern.values();
                        EnemyShipFormation.FormationPattern randomPattern = patterns[random.nextInt(patterns.length)];

                        formation = new EnemyShipFormation(lvl, randomPattern);

                        formation.attach(this);
                        formation.applyEnemyColorByLevel(lvl);
                    }
                }

                // 3. Create Room Object
                map[row][col] = new Room(row, col, type, formation, levelNum);

                // 4. Add Coins to Normal Rooms
                if (type == RoomType.NORMAL) {
                    int coinCount = 3 + (int)(Math.random() * 3);
                    for(int k=0; k<coinCount; k++) {
                        int cx = 50 + (int)(Math.random() * (width - 100));
                        int cy = 100 + (int)(Math.random() * (height - 200));
                        map[row][col].addCoin(cx, cy);
                    }
                }

                // 5. Restore Cleared Status from Persistence
                String key = row + "," + col;
                if (type == RoomType.BOSS && clearedBossRooms.contains(key)) map[row][col].setCleared(true);
                else if (type == RoomType.NORMAL && clearedRooms.contains(key)) map[row][col].setCleared(true);

                // 6. Add Shop (Bonfire) to Start Room
                if (type == RoomType.START) {
                    map[row][col].addBonfire(width, height);
                    if (this.shopImage != null && map[row][col].getBonfire() != null) {
                        map[row][col].getBonfire().setSprite(this.shopImage);
                    }
                }
            }
        }

        // Connect Doors based on adjacency
        for (int row = 0; row < MAP_SIZE; row++) {
            for (int col = 0; col < MAP_SIZE; col++) {
                Room room = map[row][col];
                if (row > 0) room.addDoor(Direction.NORTH, map[row-1][col].getType() == RoomType.BOSS, width, height);
                if (row < MAP_SIZE-1) room.addDoor(Direction.SOUTH, map[row+1][col].getType() == RoomType.BOSS, width, height);
                if (col > 0) room.addDoor(Direction.WEST, map[row][col-1].getType() == RoomType.BOSS, width, height);
                if (col < MAP_SIZE-1) room.addDoor(Direction.EAST, map[row][col+1].getType() == RoomType.BOSS, width, height);
            }
        }
    }

    /**
     * Starts the action.
     * @return Next screen code.
     */
    @Override
    public int run() {
        super.run();
        return this.returnCode;
    }

    /**
     * Updates the elements on screen and checks for events.
     * Handles Input, Physics, Boss Logic, and State Transitions.
     */
    @Override
    protected void update() {
        super.update();
        if (!this.inputDelay.checkFinished() || isGameOver) return;

        // Toggle Pause
        if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE) && this.selectionCooldown.checkFinished()) {
            this.isPaused = !this.isPaused;
            this.selectionCooldown.reset();

            // Pause Logic for Timer: Save current duration to recordedTime
            if (this.isPaused) {
                if (this.gameTimer.isRunning()) {
                    this.gameTimer.stop();
                    this.recordedTime += this.gameTimer.getElapsedTime();
                }
            } else {
                if (!this.gameTimer.isRunning()) {
                    this.gameTimer.start();
                }
            }
        }

        // Cheat Input Handling (F10, F12)
        if (this.selectionCooldown.checkFinished()) {
            // F10: Cheat Mode (Infinite Money, High Damage, Extra Lives)
            if (inputManager.isKeyDown(KeyEvent.VK_F10)) {
                this.isCheatMode = !this.isCheatMode;
                this.selectionCooldown.reset();
                if (isCheatMode) {
                    this.coin = 99999;
                    this.livesP1 = 20;
                    if (this.shipP2 != null) {
                        this.livesP2 = 20;
                    }
                    logger.info("CHEAT MODE: ON");
                } else {
                    this.coin = 0;
                    this.livesP1 = 3;
                    this.ship.setCheatMode(false);
                    if (this.shipP2 != null) {
                        this.livesP2 = 3;
                        this.shipP2.setCheatMode(false);
                    }
                    logger.info("CHEAT MODE: OFF");
                }
            }
            // F12: God Mode (Invincibility)
            if (inputManager.isKeyDown(KeyEvent.VK_F12)) {
                this.isGodMode = !this.isGodMode;
                this.selectionCooldown.reset();
                if (isGodMode) logger.info("GOD MODE: ON");
                else logger.info("GOD MODE: OFF");
            }
        }

        // God Mode Logic: Continuously refresh invincibility
        if (isGodMode) {
            this.ship.activateInvincibility(500);
            if (this.shipP2 != null) this.shipP2.activateInvincibility(500);
        }

        // Pause Menu Handling (Stops Update loop if paused)
        if (this.isPaused) {
            draw();
            managePauseInput();
            return;
        }

        // Timer Update Logic (Calculates elapsed time correctly)
        if (!this.gameTimer.isRunning()) {
            this.gameTimer.start();
        }
        this.elapsedTime = this.recordedTime + (this.gameTimer.isRunning() ? this.gameTimer.getElapsedTime() : 0);

        manageInput();
        this.ship.update();
        if (this.shipP2 != null) this.shipP2.update();

        Room room = getCurrentRoom();
        room.update();

        // Coin Collection Logic
        List<Coin> roomCoins = room.getCoins();
        for (int i = 0; i < roomCoins.size(); i++) {
            Coin c = roomCoins.get(i);
            if (livesP1 > 0 && !ship.isDestroyed() && checkCollision(ship, c)) {
                this.coin += 10;
                SoundManager.play("sfx/coin.wav");
                roomCoins.remove(i--);
                continue;
            }
            if (shipP2 != null && livesP2 > 0 && !shipP2.isDestroyed() && checkCollision(shipP2, c)) {
                this.coin += 10;
                SoundManager.play("sfx/coin.wav");
                roomCoins.remove(i--);
            }
        }

        // Room Cleared Logic: Mark as cleared in persistence set
        if (room.isCleared()) {
            String key = currentRoomRow + "," + currentRoomCol;
            if (room.getType() == RoomType.BOSS) {
                clearedBossRooms.add(key);
            } else if (room.getType() == RoomType.NORMAL) {
                clearedRooms.add(key);
            }
        }

        // Spawn Shop in Cleared Boss Rooms (except Center)
        if (room.isCleared() && room.getType() == RoomType.BOSS && room.getBonfire() == null) {
            if (!(currentRoomRow == 2 && currentRoomCol == 2)) {
                int shopSize = 100;
                int centerX = this.width / 2 - shopSize / 2;
                int centerY = this.height / 2 - shopSize / 2;

                BonFire centralShop = new BonFire(centerX, centerY, shopSize, shopSize);
                if (this.shopImage != null) centralShop.setSprite(this.shopImage);

                room.setBonfire(centralShop);
                logger.info("Shop appeared in Boss Room!");
            }
        }

        // True Final Boss Trigger: Check if 4 bosses are defeated
        if (clearedBossRooms.size() >= 4 && map[2][2].getType() != RoomType.BOSS) {
            map[2][2] = new Room(2, 2, RoomType.BOSS, null, 99);
            Core.getLogger().info("THE SEAL IS BROKEN! CENTER ROOM IS NOW A BOSS ROOM.");
        }

        // Update Boss / Enemies
        if (room.getType() == RoomType.BOSS && this.currentBoss != null && !room.isCleared()) {
            if (currentBoss instanceof SamuraiBoss) sekiroBossManage();
            else updateCurrentBoss();
        } else if (room.getEnemyFormation() != null && !room.isCleared()) {
            room.getEnemyFormation().update();
            room.getEnemyFormation().shoot(this.bullets);
        }

        if (laserBeamManager != null) laserBeamManager.update();

        // Hit Stop Logic (Freeze frame for impact)
        if (hitStopFrames > 0) {
            hitStopFrames--;
            return;
        }

        // Screen Shake Decay
        if (shakeDuration > 0) {
            shakeDuration--;
            if (shakeDuration <= 0) shakeIntensity = 0;
        }

        // Particle Update
        for (int i = 0; i < particles.size(); i++) {
            if (!particles.get(i).update()) {
                particles.remove(i--);
            }
        }

        manageInteractions(room);
        manageCollisions(room);
        manageItemCollisions();
        manageWaveShipCollisions();

        cleanBullets();
        cleanSwordWaves();
        cleanItems();
        cleanEffects();
        checkGameOver();

        draw();
    }

    /**
     * Triggers visual impact effects (Shake, Freeze, Particles).
     *
     * @param x X coordinate.
     * @param y Y coordinate.
     * @param shake Shake intensity.
     * @param stopFrames Frames to freeze game logic.
     * @param sparkColor Color of particles.
     */
    private void triggerImpactEffect(int x, int y, int shake, int stopFrames, Color sparkColor) {
        this.shakeIntensity = shake;
        this.shakeDuration = 5;
        this.hitStopFrames = stopFrames;
        for (int i = 0; i < 10; i++) {
            particles.add(new Particle(x, y, sparkColor, 5, 3));
        }
    }

    /**
     * Handles input within the Pause Menu.
     */
    private void managePauseInput() {
        if (this.selectionCooldown.checkFinished()) {
            if (inputManager.isKeyDown(KeyEvent.VK_UP) || inputManager.isKeyDown(KeyEvent.VK_W)) {
                pauseSelection--;
                if (pauseSelection < 0) pauseSelection = 2;
                this.selectionCooldown.reset();
            }
            if (inputManager.isKeyDown(KeyEvent.VK_DOWN) || inputManager.isKeyDown(KeyEvent.VK_S)) {
                pauseSelection++;
                if (pauseSelection > 2) pauseSelection = 0;
                this.selectionCooldown.reset();
            }
            if (inputManager.isKeyDown(KeyEvent.VK_SPACE) || inputManager.isKeyDown(KeyEvent.VK_ENTER)) {
                this.selectionCooldown.reset();
                if (pauseSelection == 2) { // Resume
                    this.isPaused = false;
                    if (!this.gameTimer.isRunning()) {
                        this.gameTimer.start();
                    }
                }
                else if (pauseSelection == 1) { this.returnCode = 12; this.isRunning = false; } // Restart
                else if (pauseSelection == 0) { this.returnCode = 1; this.isRunning = false; }  // Quit
            }
        }
    }

    /**
     * Updates the current boss entity's logic depending on its type.
     */
    private void updateCurrentBoss() {
        if (currentBoss instanceof FinalBoss) {
            FinalBoss fb = (FinalBoss) currentBoss;
            fb.update();
            if (fb.getHealPoint() > fb.getMaxHp() / 4) {
                bossBullets.addAll(fb.shoot1());
                bossBullets.addAll(fb.shoot2());
            } else {
                bossBullets.addAll(fb.shoot3());
            }
        } else if (currentBoss instanceof OmegaBoss) {
            ((OmegaBoss) currentBoss).update();
        } else if (currentBoss instanceof FinalBoss_3) {
            FinalBoss_3 fb3 = (FinalBoss_3) currentBoss;
            fb3.update();
            bossBullets.addAll(fb3.shoot());
        } else if (currentBoss instanceof Boss4) {
            Boss4 b4 = (Boss4) currentBoss;
            b4.update();
            if (b4.isSpellCardActive()) {
                bossBullets.addAll(b4.shootSpellCard(ship));
            } else {
                bossBullets.addAll(b4.shootNormal());
            }
        }
        else if (currentBoss instanceof TrueFinalBoss) {
            TrueFinalBoss tfb = (TrueFinalBoss) currentBoss;
            tfb.update();
            bossBullets.addAll(tfb.shoot());
        }
    }

    /**
     * Specific management logic for Samurai Boss (Waves, etc).
     */
    private void sekiroBossManage() {
        if (this.samuraiBoss == null) return;
        if (this.samuraiBoss.isDestroyed()) {
            handleBossDeath(getCurrentRoom());
            return;
        }
        this.samuraiBoss.update();
        this.swordWaves.addAll(this.samuraiBoss.shootWave());
    }

    /**
     * Handles interactions with Doors and Shops.
     * Moves player to the next room or opens shop.
     *
     * @param room Current room.
     */
    private void manageInteractions(Room room) {
        if (room.isLocked()) return;
        for (Door door : room.getDoors()) {
            boolean p1Alive = livesP1 > 0 && !ship.isDestroyed();
            boolean p2Alive = shipP2 != null && livesP2 > 0 && !shipP2.isDestroyed();

            boolean p1Satisfied = !p1Alive || checkCollision(ship, door);
            boolean p2Satisfied = !p2Alive || checkCollision(shipP2, door);

            if (p1Satisfied && p2Satisfied && (p1Alive || p2Alive)) {
                moveRoom(door.direction);
                return;
            }
        }
        if (room.getBonfire() != null) {
            boolean p1Touch = livesP1 > 0 && !ship.isDestroyed() && checkCollision(ship, room.getBonfire());
            boolean p2Touch = shipP2 != null && livesP2 > 0 && !shipP2.isDestroyed() && checkCollision(shipP2, room.getBonfire());
            if (p1Touch || p2Touch) {
                if (inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
                    this.livesP1 = 3;
                    if(this.shipP2 != null) this.livesP2 = 3;
                    this.returnCode = 11;
                    this.isRunning = false;
                }
            }
        }
    }

    /**
     * Transitions player to the adjacent room based on direction.
     * @param dir Direction to move.
     */
    private void moveRoom(Direction dir) {
        switch(dir) {
            case NORTH: currentRoomRow--; if(livesP1>0) ship.setPositionY(this.height - 100); break;
            case SOUTH: currentRoomRow++; if(livesP1>0) ship.setPositionY(100); break;
            case WEST:  currentRoomCol--; if(livesP1>0) ship.setPositionX(this.width - 100); break;
            case EAST:  currentRoomCol++; if(livesP1>0) ship.setPositionX(100); break;
        }
        if (shipP2 != null && livesP2 > 0) {
            if(livesP1 > 0) {
                shipP2.setPositionX(ship.getPositionX());
                shipP2.setPositionY(ship.getPositionY());
            } else {
                switch(dir) {
                    case NORTH: shipP2.setPositionY(this.height - 100); break;
                    case SOUTH: shipP2.setPositionY(100); break;
                    case WEST:  shipP2.setPositionX(this.width - 100); break;
                    case EAST:  shipP2.setPositionX(100); break;
                }
            }
        }
        enterRoom(false);
    }

    /**
     * Sets up the room upon entry (Spawns, BGM, UI).
     * @param isFirst True if this is the first room entered (start).
     */
    private void enterRoom(boolean isFirst) {
        this.bullets.clear();
        this.swordWaves.clear();
        this.bossBullets.clear();

        this.currentBoss = null;
        this.samuraiBoss = null;
        this.trueFinalBoss = null;

        this.instructionMessage = null;

        if (this.laserBeamManager != null) this.laserBeamManager = new LaserBeamManager();

        if (livesP1 > 0) ship.activateInvincibility(2000);
        if (shipP2 != null && livesP2 > 0) shipP2.activateInvincibility(2000);

        ship.setMeleeMode(false);
        ship.setHybridMode(false);
        if(shipP2!=null) shipP2.setMeleeMode(false);

        // Force True Final Boss logic for Center Room (if conditions met)
        if (currentRoomRow == 2 && currentRoomCol == 2 && clearedBossRooms.size() >= 4) {
            if (map[2][2].getType() != RoomType.BOSS) {
                map[2][2] = new Room(2, 2, RoomType.BOSS, null, 99);
                logger.info("Center room FORCE transformed to BOSS room!");
            }

            SoundManager.stopAll();
            SoundManager.playLoop("sfx/truefinalboss.wav");

            this.instructionMessage = "PRESS SPACE TO PARRY!";
            this.instructionCooldown.reset();
            if (livesP1 > 0) ship.activateInvincibility(3000);
            if (shipP2 != null && livesP2 > 0) shipP2.activateInvincibility(3000);

            spawnBoss(map[2][2]);
            return;
        }

        Room room = getCurrentRoom();
        SoundManager.stopAll();

        if (currentRoomRow == 2 && currentRoomCol == MAP_SIZE - 1) {
            ship.setMeleeMode(true);
            if (shipP2 != null) shipP2.setMeleeMode(true);
            int safeY = ITEMS_SEPARATION_LINE_HEIGHT - 40;
            if (livesP1 > 0) ship.setPositionY(safeY);
            if (shipP2 != null && livesP2 > 0) shipP2.setPositionY(safeY);
            if (!room.isCleared()) {
                this.instructionMessage = "PRESS SPACE TO PARRY!";
                this.instructionCooldown.reset();
                if (livesP1 > 0) ship.activateInvincibility(2000);
                if (shipP2 != null && livesP2 > 0) shipP2.activateInvincibility(2000);
            }
        }

        if (room.getType() == RoomType.BOSS && !room.isCleared()) {
            spawnBoss(room);

            if (currentBoss instanceof SamuraiBoss) SoundManager.playLoop("sfx/Samurai-Boss.wav");
            else if (currentBoss instanceof FinalBoss_3) SoundManager.playLoop("sfx/laserboss.wav");
            else if (currentBoss instanceof Boss4) SoundManager.playLoop("sfx/boss4.wav");
            else if (currentBoss instanceof FinalBoss) SoundManager.playLoop("sfx/normalboss.wav");

        } else if (room.getBonfire() != null || room.getType() == RoomType.START) {
            SoundManager.playLoop("sfx/shop.wav");
        } else {
            int idx = room.getNormalBgmIndex();
            if(idx == -1) {
                idx = (int)(Math.random()*3);
                room.setNormalBgmIndex(idx);
            }
            SoundManager.playLoop("sfx/normalroom-" + (idx+1) + ".wav");
        }
    }

    /**
     * Spawns specific boss based on room coordinates.
     * @param room The room to spawn the boss in.
     */
    private void spawnBoss(Room room) {
        int r = currentRoomRow;
        int c = currentRoomCol;
        int center = 2;
        if (r == 0 && c == center) {
            this.currentBoss = new FinalBoss(width/2 - 50, 70, width, height);
        } else if (r == center && c == MAP_SIZE-1) {
            this.samuraiBoss = new SamuraiBoss(width/2, 100, width, ship, shipP2, ITEMS_SEPARATION_LINE_HEIGHT);
            this.samuraiBoss.attach(this);
            this.currentBoss = this.samuraiBoss;
            ship.setMeleeMode(true);
            if(shipP2 != null) shipP2.setMeleeMode(true);
        } else if (r == center && c == 0) {
            this.currentBoss = new Boss4(width/2 - 30, 50, width, height);
        } else if (r == MAP_SIZE-1 && c == center) {
            this.currentBoss = new FinalBoss_3(width/2 - 50, 50, width, height, this.laserBeamManager);
        }
        else if (r == 2 && c == 2 && clearedBossRooms.size() >= 4) {
            this.trueFinalBoss = new TrueFinalBoss(width/2, 100, width, height, laserBeamManager, ship, shipP2, this);
            this.currentBoss = this.trueFinalBoss;
            ship.setHybridMode(true);
            if(shipP2 != null) shipP2.setHybridMode(true);
            logger.info("Spawned True Final Boss!");
        }
    }

    /**
     * Manages all collisions (bullets, enemies, items, players).
     */
    private void manageCollisions(Room room) {
        Set<Bullet> recyclable = new HashSet<>();
        for (Bullet b : this.bullets) {
            if (b.getSpeed() > 0) {
                if (checkPlayerCollision(b)) recyclable.add(b);
            } else {
                if (checkEnemyCollision(b, room)) recyclable.add(b);
            }
        }
        Set<BossBullet> bossBulletsToRemove = new HashSet<>();
        for (BossBullet bb : this.bossBullets) {
            bb.update();
            if (bb.isOffScreen(width, height)) bossBulletsToRemove.add(bb);
            else if (checkPlayerCollision(bb)) bossBulletsToRemove.add(bb);
        }
        this.bossBullets.removeAll(bossBulletsToRemove);

        if (laserBeamManager != null) {
            if (livesP1 > 0 && !ship.isDestroyed() && !ship.isInvincible() && laserBeamManager.checkCollisionWithShip(ship)) {
                triggerImpactEffect(ship.getPositionX(), ship.getPositionY(), 5, 5, Color.WHITE);
                triggerExplosion(ship.getPositionX(), ship.getPositionY(), ship.getColor());
                ship.destroy();
                livesP1--;
                SoundManager.play("sfx/impact.wav");
                if (livesP1 > 0) ship.activateInvincibility(2000);
            }
            if (shipP2 != null && livesP2 > 0 && !shipP2.isDestroyed() && !shipP2.isInvincible() && laserBeamManager.checkCollisionWithShip(shipP2)) {
                triggerImpactEffect(shipP2.getPositionX(), shipP2.getPositionY(), 5, 5, Color.WHITE);
                triggerExplosion(shipP2.getPositionX(), shipP2.getPositionY(), shipP2.getColor());
                shipP2.destroy();
                livesP2--;
                SoundManager.play("sfx/impact.wav");
                if (livesP2 > 0) ship.activateInvincibility(2000);
            }
        }
        checkBodyCollisions(room);
        for (SwordWave w : swordWaves) {
            // if (checkPlayerCollision(w)) recyclable.add(w);
        }
        this.bullets.removeAll(recyclable);
        this.swordWaves.removeAll(recyclable);
        BulletPool.recycle(recyclable);
    }

    /**
     * Handles item pickups and applies their effects.
     */
    private void manageItemCollisions() {
        Set<DropItem> acquired = new HashSet<>();
        for (DropItem item : dropItems) {
            boolean p1 = livesP1 > 0 && !ship.isDestroyed() && checkCollision(ship, item);
            boolean p2 = shipP2 != null && livesP2 > 0 && !shipP2.isDestroyed() && checkCollision(shipP2, item);
            if (p1 || p2) {
                ItemHUDManager.getInstance().addDroppedItem(item.getItemType());
                switch (item.getItemType()) {
                    case Heal: if(p1 && livesP1 < 3) livesP1++; if(p2 && livesP2 < 3) livesP2++; break;
                    case Shield: if(p1) ship.activateInvincibility(5000); if(p2) shipP2.activateInvincibility(5000); break;
                    case Slow: if(getCurrentRoom().getEnemyFormation() != null) getCurrentRoom().getEnemyFormation().activateSlowdown(); break;
                    case Stop: DropItem.applyTimeFreezeItem(3000); break;
                    case Push: if(getCurrentRoom().getEnemyFormation() != null) DropItem.PushbackItem(getCurrentRoom().getEnemyFormation(), 20); break;
                    case Explode: if(getCurrentRoom().getEnemyFormation() != null) { int count = getCurrentRoom().getEnemyFormation().destroyAll(); score += count * 10; } break;
                    default: break;
                }
                acquired.add(item);
            }
        }
        dropItems.removeAll(acquired);
        ItemPool.recycle(acquired);
    }

    /**
     * Checks if a player ship collides with an entity.
     * @param e Entity to check against.
     * @return True if collision detected.
     */
    private boolean checkPlayerCollision(Entity e) {
        boolean hit = false;
        if (livesP1 > 0 && !ship.isDestroyed() && !ship.isInvincible() && checkCollision(e, ship)) {
            triggerImpactEffect(ship.getPositionX(), ship.getPositionY(), 5, 5, Color.YELLOW);
            triggerExplosion(ship.getPositionX(), ship.getPositionY(), ship.getColor());
            ship.destroy();
            livesP1--;
            SoundManager.play("sfx/exposion.wav");
            if (livesP1 > 0) ship.activateInvincibility(2000);
            hit = true;
        }
        if (shipP2 != null && livesP2 > 0 && !shipP2.isDestroyed() && !shipP2.isInvincible() && checkCollision(e, shipP2)) {
            triggerImpactEffect(ship.getPositionX(), ship.getPositionY(), 5, 5, Color.RED);
            triggerExplosion(shipP2.getPositionX(), shipP2.getPositionY(), shipP2.getColor());
            shipP2.destroy();
            livesP2--;
            SoundManager.play("sfx/exposion.wav");
            if (livesP2 > 0) shipP2.activateInvincibility(2000);
            hit = true;
        }
        return hit;
    }

    /**
     * Spawns an explosion effect.
     * @param x X coordinate.
     * @param y Y coordinate.
     * @param color Color of explosion.
     */
    private void triggerExplosion(int x, int y, Color color) {
        Entity explosion = new Entity(x, y, 13*2, 8*2, color);
        explosion.spriteType = DrawManager.SpriteType.ShipDestroyed;
        effects.add(new TemporaryEffect(explosion, 500));
    }

    /**
     * Checks if enemies collide with player bullets.
     * @param b Bullet.
     * @param room Current room.
     * @return True if collision occurred.
     */
    private boolean checkEnemyCollision(Bullet b, Room room) {
        if (room.getEnemyFormation() != null && !room.isCleared()) {
            for (EnemyShip e : room.getEnemyFormation()) {
                if (!e.isDestroyed() && checkCollision(b, e)) {
                    triggerImpactEffect(e.getPositionX(), e.getPositionY(), 2, 0, Color.YELLOW);
                    room.getEnemyFormation().destroy(e);
                    this.score += e.getPointValue();
                    this.coin += 5;
                    spawnItem(e, room.getLevelNumber());
                    return true;
                }
            }
        }
        if (this.currentBoss != null) {
            boolean hit = false;
            if (currentBoss instanceof SamuraiBoss && !((SamuraiBoss)currentBoss).isDestroyed()) {  }
            else if (currentBoss instanceof FinalBoss && !((FinalBoss)currentBoss).isDestroyed()) {
                if (checkCollision(b, currentBoss)) { ((FinalBoss)currentBoss).takeDamage(1);
                    triggerImpactEffect(b.getPositionX(), b.getPositionY(), 2, 0, Color.WHITE);
                    hit = true; }
            } else if (currentBoss instanceof OmegaBoss && !((OmegaBoss)currentBoss).isDestroyed()) {
                if (checkCollision(b, currentBoss)) { ((OmegaBoss)currentBoss).takeDamage(1);
                    triggerImpactEffect(b.getPositionX(), b.getPositionY(), 2, 0, Color.WHITE);
                    hit = true; }
            } else if (currentBoss instanceof FinalBoss_3 && !((FinalBoss_3)currentBoss).isDestroyed()) {
                if (checkCollision(b, currentBoss)) {
                    ((FinalBoss_3)currentBoss).takeDamage(1);
                    triggerImpactEffect(b.getPositionX(), b.getPositionY(), 2, 0, Color.YELLOW);
                    hit = true; }
            } else if (currentBoss instanceof Boss4 && !((Boss4)currentBoss).isDestroyed()) {
                if (checkCollision(b, currentBoss)) {
                    ((Boss4)currentBoss).takeDamage(1);
                    triggerImpactEffect(b.getPositionX(), b.getPositionY(), 2, 0, Color.WHITE);
                    hit = true; }
            }
            if (currentBoss instanceof TrueFinalBoss && !((TrueFinalBoss)currentBoss).isDestroyed()) {
                if (checkCollision(b, currentBoss)) {
                    ((TrueFinalBoss)currentBoss).takeDamage(1);
                    triggerImpactEffect(b.getPositionX(), b.getPositionY(), 2, 0, Color.RED);
                    hit = true;
                }
            }

            if (hit) {
                if (isBossDead(currentBoss)) handleBossDeath(room);
                return true;
            }
        }
        return false;
    }

    /**
     * Spawns an item drop from a destroyed enemy.
     * @param enemy The destroyed enemy.
     * @param levelNum Current level number.
     */
    private void spawnItem(EnemyShip enemy, int levelNum) {
        Level lvl = levelManager.getLevel(levelNum);
        if (lvl == null || lvl.getItemDrops() == null) return;
        List<ItemDrop> drops = lvl.getItemDrops();
        for (engine.level.ItemDrop drop : drops) {
            if (drop.getEnemyType().equals(enemy.getEnemyType()) && Math.random() < drop.getDropChance()) {
                DropItem.ItemType type = DropItem.fromString(drop.getItemId());
                if (type != null) {
                    dropItems.add(ItemPool.getItem(enemy.getPositionX(), enemy.getPositionY(), 2, type));
                }
            }
        }
    }

    /**
     * Checks if the boss is dead.
     * @param boss Boss entity.
     * @return True if dead.
     */
    private boolean isBossDead(Entity boss) {
        if (boss instanceof SamuraiBoss) return ((SamuraiBoss)boss).isDestroyed();
        if (boss instanceof FinalBoss) return ((FinalBoss)boss).getHealPoint() <= 0;
        if (boss instanceof OmegaBoss) return ((OmegaBoss)boss).isDestroyed();
        if (boss instanceof FinalBoss_3) return ((FinalBoss_3)boss).isDestroyed();
        if (boss instanceof Boss4) return ((Boss4)boss).isDestroyed();
        if (boss instanceof TrueFinalBoss) return ((TrueFinalBoss)boss).isDestroyed();
        return false;
    }

    /**
     * Handles logic when a boss is defeated (Score, Coin, Persistence).
     * @param room The room where boss died.
     */
    private void handleBossDeath(Room room) {
        this.score += 300;
        this.coin += 300;
        room.setCleared(true);
        clearedBossRooms.add(currentRoomRow + "," + currentRoomCol);

        this.ship.setMeleeMode(false);
        this.ship.setHybridMode(false);
        if(shipP2!=null) { shipP2.setMeleeMode(false); shipP2.setHybridMode(false); }

        this.currentBoss = null;
        this.bossBullets.clear();
        SoundManager.stopAll();

        if (clearedBossRooms.size() >= 5) {
            logger.info("Congratulation! True Final Boss Defeated!");
            this.returnCode = 2;
            this.isRunning = false;
            return;
        }

        if (clearedBossRooms.size() == 4) {
            logger.info("4 Bosses Defeated! Go to Center!");
        }
    }

    /**
     * Checks collisions between player ship body and enemies/bosses.
     * @param room Current room.
     */
    private void checkBodyCollisions(Room room) {
        if (room.getEnemyFormation() != null && !room.isCleared()) {
            for (EnemyShip e : room.getEnemyFormation()) {
                if (!e.isDestroyed() && checkPlayerCollision(e)) e.destroy();
            }
        }
        if (this.currentBoss != null) {
            if (currentBoss instanceof SamuraiBoss && !((SamuraiBoss)currentBoss).isDestroyed()) {
                handleSekiroCollision(ship, (SamuraiBoss)currentBoss);
                if(shipP2!=null) handleSekiroCollision(shipP2, (SamuraiBoss)currentBoss);
            }
            else if (currentBoss instanceof TrueFinalBoss && !((TrueFinalBoss)currentBoss).isDestroyed()) {
                handleTrueFinalBossCollision(ship, (TrueFinalBoss)currentBoss);
                if(shipP2!=null) handleTrueFinalBossCollision(shipP2, (TrueFinalBoss)currentBoss);
            }
            else if (!isBossDead(currentBoss)) {
                checkPlayerCollision(currentBoss);
            }
        }
    }

    /**
     * Handles specific collision logic for Samurai Boss (Parry system).
     * @param player Player ship.
     * @param boss Samurai Boss.
     */
    private void handleSekiroCollision(Ship player, SamuraiBoss boss) {
        if (!checkCollision(player, boss)) return;

        if (player.isDestroyed()) return;

        if (player.isParrying() && boss.isPostureBroken() && !boss.isInvincibleAfterBroken()) {
            boss.executeDeathblow();
            SoundManager.play("sfx/samurai-kill.wav");
            triggerImpactEffect(player.getPositionX(), player.getPositionY(), 10, 10, Color.RED);
            player.activateInvincibility(2000);
            if (boss.isDestroyed()) handleBossDeath(getCurrentRoom());
        }
        else if (player.isParrying() && boss.isAttacking()) {
            boss.onParried();
            SoundManager.play("sfx/parry.wav");
            triggerImpactEffect(player.getPositionX(), player.getPositionY(), 5, 5, Color.YELLOW);
            triggerParryEffect(player);
        }
        else if (player.isParrying() && !boss.isAttacking()) {
            boss.takeDamage(1);
            if (!boss.isPostureBroken()) boss.takePostureDamage(boss.isEnraged() ? 10 : 20);
        }
        else if (boss.isAttacking() && !player.isInvincible()) {
            triggerImpactEffect(player.getPositionX(), player.getPositionY(), 10, 10, Color.RED);
            triggerExplosion(player.getPositionX(), player.getPositionY(), player.getColor());
            player.destroy();
            if (player.getPlayerId() == 1) livesP1--;
            else livesP2--;

            SoundManager.play("sfx/samurai-kill.wav");

        }
    }

    /**
     * Handles collision logic for True Final Boss.
     * @param player Player ship.
     * @param boss True Final Boss.
     */
    private void handleTrueFinalBossCollision(Ship player, TrueFinalBoss boss) {
        if (!checkCollision(player, boss)) return;
        if (player.isDestroyed()) return;

        if (player.isParrying() && boss.isPostureBroken()) {
            boss.executeDeathblow();
            SoundManager.play("sfx/samurai-kill.wav");
            triggerImpactEffect(player.getPositionX(), player.getPositionY(), 10, 10, Color.RED);
            player.activateInvincibility(1000);
            if (boss.isDestroyed()) handleBossDeath(getCurrentRoom());
        }
        else if (player.isParrying() && boss.isAttacking()) {
            boss.onParried();
            SoundManager.play("sfx/parry.wav");
            triggerImpactEffect(player.getPositionX(), player.getPositionY(), 5, 5, Color.YELLOW);
            triggerParryEffect(player);
        }
        else if (player.isParrying()) {
        }
        else if (boss.isAttacking() && !player.isInvincible()) {
            triggerExplosion(player.getPositionX(), player.getPositionY(), player.getColor());
            player.destroy();
            triggerImpactEffect(player.getPositionX(), player.getPositionY(), 5, 5, Color.RED);
            if (player.getPlayerId() == 1) livesP1--; else livesP2--;
            SoundManager.play("sfx/samurai-kill.wav");
        }
    }

    /**
     * Spawns parry spark effects.
     */
    private void triggerParryEffect(Ship player) {
        int centerX = player.getPositionX() + (player.getWidth() / 2);
        int boundaryY = player.getPositionY();
        this.parrySparkEffect.setPositionX(centerX -40);
        this.parrySparkEffect.setPositionY(boundaryY - 10);
        this.parrySparkEffect2.setPositionX(centerX + 15);
        this.parrySparkEffect2.setPositionY(boundaryY - 10);
        this.parrySparkCooldown.reset();
    }

    /**
     * Checks if two entities collide.
     */
    private boolean checkCollision(Entity a, Entity b) {
        if (a == null || b == null) return false;
        return a.getPositionX() < b.getPositionX() + b.getWidth() &&
                a.getPositionX() + a.getWidth() > b.getPositionX() &&
                a.getPositionY() < b.getPositionY() + b.getHeight() &&
                a.getHeight() + a.getPositionY() > b.getPositionY();
    }

    /**
     * Handles player movement and shooting input.
     */
    private void manageInput() {
        if (this.livesP1 > 0 && !this.ship.isDestroyed()) {
            int minYLimit = SEPARATION_LINE_HEIGHT;
            if (this.currentBoss instanceof SamuraiBoss && !((SamuraiBoss) this.currentBoss).isDestroyed()) minYLimit = ITEMS_SEPARATION_LINE_HEIGHT / 2;
            if (inputManager.isActionPressed("RIGHT") && ship.getPositionX() + ship.getWidth() < width) this.ship.moveRight();
            if (inputManager.isActionPressed("LEFT") && ship.getPositionX() > 0) this.ship.moveLeft();
            if (inputManager.isActionPressed("UP") && ship.getPositionY() > minYLimit) this.ship.moveUp();
            if (inputManager.isActionPressed("DOWN") && ship.getPositionY() + ship.getHeight() < height) this.ship.moveDown();
            boolean p1Fire = inputManager.isActionPressed("SHOOT");
            // Shooting only when press SHOOT key and not pressed previous.
            if (p1Fire && !isShootingP1) {
                this.ship.shoot(this.bullets);
            }
            // Save current state of shooting.
            isShootingP1 = p1Fire;
        }
        if (this.shipP2 != null && this.livesP2 > 0 && !this.shipP2.isDestroyed()) {
            int minYLimitP2 = SEPARATION_LINE_HEIGHT;
            if (this.currentBoss instanceof SamuraiBoss && !((SamuraiBoss) this.currentBoss).isDestroyed()) minYLimitP2 = ITEMS_SEPARATION_LINE_HEIGHT / 2;
            if (inputManager.isActionPressedP2("RIGHT") && shipP2.getPositionX() + shipP2.getWidth() < width) this.shipP2.moveRight();
            if (inputManager.isActionPressedP2("LEFT") && shipP2.getPositionX() > 0) this.shipP2.moveLeft();
            if (inputManager.isActionPressedP2("UP") && shipP2.getPositionY() > minYLimitP2) this.shipP2.moveUp();
            if (inputManager.isActionPressedP2("DOWN") && shipP2.getPositionY() + shipP2.getHeight() < height) this.shipP2.moveDown();
            boolean p2Fire = inputManager.isActionPressedP2("SHOOT");
            if (p2Fire && !isShootingP2) {
                this.shipP2.shoot(this.bullets);
            }
            isShootingP2 = p2Fire;
        }
    }

    /**
     * Cleans bullets that went off-screen.
     */
    private void cleanBullets() {
        Set<Bullet> recyclable = new HashSet<>();
        for (Bullet b : this.bullets) {
            b.update();
            if (b.getPositionY() < SEPARATION_LINE_HEIGHT || b.getPositionY() > this.height) recyclable.add(b);
        }
        this.bullets.removeAll(recyclable);
        BulletPool.recycle(recyclable);
    }

    /**
     * Cleans sword waves that went off-screen.
     */
    private void cleanSwordWaves() {
        Set<SwordWave> recyclable = new HashSet<>();
        for (SwordWave w : this.swordWaves) {
            w.update();
            if (w.getPositionY() > this.height) recyclable.add(w);
        }
        this.swordWaves.removeAll(recyclable);
    }

    /**
     * Cleans items that went off-screen.
     */
    private void cleanItems() {
        Set<DropItem> recyclable = new HashSet<>();
        for (DropItem i : this.dropItems) {
            i.update();
            if (i.getPositionY() > this.height) recyclable.add(i);
        }
        this.dropItems.removeAll(recyclable);
        ItemPool.recycle(recyclable);
    }

    /**
     * Cleans finished visual effects.
     */
    private void cleanEffects() {
        Set<TemporaryEffect> finished = new HashSet<>();
        for(TemporaryEffect e : effects) {
            if(e.isFinished()) finished.add(e);
        }
        effects.removeAll(finished);
    }

    /**
     * Checks game over condition (all lives lost).
     */
    private void checkGameOver() {
        if (livesP1 <= 0 && (shipP2 == null || livesP2 <= 0)) {
            isGameOver = true;
            this.returnCode = 2;
            this.isRunning = false;
        }
    }

    /**
     * Returns the current room object.
     * @return Current Room.
     */
    private Room getCurrentRoom() { return map[currentRoomRow][currentRoomCol]; }

    /**
     * Returns the set of cleared boss rooms.
     * @return Set of coordinate strings.
     */
    public Set<String> getClearedBossRooms() { return this.clearedBossRooms; }

    /**
     * Returns the current game state.
     * Handles restart logic (return code 12).
     * @return Current GameState.
     */
    public GameState getGameState() {
        if (this.returnCode == 12) {
            // Restart Logic: P2 lives are reset if P2 existed
            int initialLivesP2 = (this.shipP2 != null) ? 3 : 0;
            return new GameState(1, 0, 3, initialLivesP2, 0, 0, 0);
        }
        return new GameState(1, score, livesP1, livesP2, 0, 0, coin);
    }

    /**
     * Draws the elements associated with the screen.
     */
    private void draw() {
        drawManager.initDrawing(this);
        Graphics2D g = (Graphics2D) drawManager.getBackBufferGraphics();

        // Apply Screen Shake
        int dx = 0, dy = 0;
        if (shakeIntensity > 0) {
            dx = (int) ((Math.random() * shakeIntensity * 2) - shakeIntensity);
            dy = (int) ((Math.random() * shakeIntensity * 2) - shakeIntensity);
            g.translate(dx, dy);
        }

        Room room = getCurrentRoom();
        room.draw(drawManager);

        // Draw Bosses
        if (this.currentBoss != null && room.getType() == RoomType.BOSS && !room.isCleared()) {
            if (currentBoss instanceof Boss4) {
                drawManager.drawBoss4((Boss4)currentBoss);
            } else if (currentBoss instanceof FinalBoss_3) {
                FinalBoss_3 fb3 = (FinalBoss_3) currentBoss;
                drawManager.drawLaserBoss(fb3);
                if (fb3.isLaserWarningActive()) {
                    for (float angle : fb3.getPendingWarningAngles()) {
                        drawManager.drawLaserWarningLine(fb3.getWarningOriginX(), fb3.getWarningOriginY(), angle);
                    }
                }
            } else if (currentBoss instanceof TrueFinalBoss){
                ((TrueFinalBoss)currentBoss).draw(drawManager);
            }else {
                drawManager.drawEntity(this.currentBoss, this.currentBoss.getPositionX(), this.currentBoss.getPositionY());
            }

            if (currentBoss instanceof SamuraiBoss) {
                SamuraiBoss sBoss = (SamuraiBoss) currentBoss;
                ((SamuraiBoss)currentBoss).draw(drawManager);
                drawManager.drawBossHealthBar(this, sBoss.getHealPoint(), sBoss.getMaxHealth());
                drawManager.drawBossPostureBar(this, sBoss.getPosture(), sBoss.getMaxPosture());
                if (sBoss.isPostureBroken()) drawManager.drawDeathblowMarker(this, sBoss.getPositionX() + sBoss.getWidth()/2, sBoss.getPositionY() + sBoss.getHeight()/2);
                if (!this.parrySparkCooldown.checkFinished()) {
                    drawManager.drawEntity(this.parrySparkEffect, this.parrySparkEffect.getPositionX(), this.parrySparkEffect.getPositionY());
                    drawManager.drawEntity(this.parrySparkEffect2, this.parrySparkEffect2.getPositionX(), this.parrySparkEffect2.getPositionY());
                }
            }
        }

        // Draw Players
        if (livesP1 > 0 && !ship.isDestroyed()) {
            drawManager.drawEntity(ship, ship.getPositionX(), ship.getPositionY());
            if (this.ship.isParrying()) {
                Entity slash = this.ship.getSwordSlashEffect();
                drawManager.drawEntity(slash, slash.getPositionX(), slash.getPositionY());
            }
        }

        if (shipP2 != null && livesP2 > 0 && !shipP2.isDestroyed()) {
            drawManager.drawEntity(shipP2, shipP2.getPositionX(), shipP2.getPositionY());
            if (this.shipP2.isParrying()) {
                Entity slash = this.shipP2.getSwordSlashEffect();
                drawManager.drawEntity(slash, slash.getPositionX(), slash.getPositionY());
            }
        }

        // Draw Effects and Projectiles
        for(TemporaryEffect e : effects) {
            drawManager.drawEntity(e.entity, e.entity.getPositionX(), e.entity.getPositionY());
        }

        for (Bullet b : this.bullets) drawManager.drawEntity(b, b.getPositionX(), b.getPositionY());
        for (BossBullet bb : this.bossBullets) drawManager.drawEntity(bb, bb.getPositionX(), bb.getPositionY());
        for (SwordWave w : this.swordWaves) drawManager.drawEntity(w, w.getPositionX(), w.getPositionY());
        for (DropItem d : this.dropItems) drawManager.drawEntity(d, d.getPositionX(), d.getPositionY());

        if (laserBeamManager != null) {
            for (LaserBeam beam : laserBeamManager.getBeams()) drawManager.drawLaserBeam(beam);
        }

        for (Particle p : particles) {
            p.draw(g);
        }

        // Revert Screen Shake for UI
        if (shakeIntensity > 0) {
            g.translate(-dx, -dy);
        }

        // Draw UI
        drawMinimap();
        drawManager.drawScore(this, this.score);
        drawManager.drawCoin(this, this.coin);
        drawManager.drawTime(this, this.elapsedTime);
        drawManager.drawLives(this, this.livesP1);
        if (this.shipP2 != null) drawManager.drawLivesP2(this, this.livesP2);
        drawManager.drawItemsHUD(this);
        drawManager.drawHorizontalLine(this, SEPARATION_LINE_HEIGHT - 1);
        drawManager.drawHorizontalLine(this, ITEMS_SEPARATION_LINE_HEIGHT);
        if (currentBoss instanceof SamuraiBoss && !((SamuraiBoss) currentBoss).isDestroyed()) {
            g.setColor(new Color(255, 0, 0, 100));
            g.drawLine(0, ITEMS_SEPARATION_LINE_HEIGHT / 2, width, ITEMS_SEPARATION_LINE_HEIGHT / 2);
        }
        // Draw Messages
        if (this.instructionMessage != null && !this.instructionCooldown.checkFinished()) {
            drawManager.drawCenteredBigString(this, this.instructionMessage, this.height / 2 - 100);
        }

        if (isCheatMode) {
            drawManager.drawCenteredRegularString(this, "CHEAT MODE ACTIVATED", 100);
        }
        if (isGodMode) {
            drawManager.drawCenteredRegularString(this, "GOD MODE ON", 120);
        }
        if (isPaused) {
            drawManager.drawPauseMenu(this, pauseSelection);
        }

        drawManager.completeDrawing(this);
    }

    /**
     * Draws the minimap HUD.
     * Cleared rooms are Green, Uncleared Bosses Red, Cleared Bosses Cyan.
     */
    private void drawMinimap() {
        int boxSize = 10;
        int startX = this.width - (MAP_SIZE * boxSize) - 20;
        int startY = 50;
        for (int r = 0; r < MAP_SIZE; r++) {
            for (int c = 0; c < MAP_SIZE; c++) {
                Color color = Color.GRAY;

                if (r == currentRoomRow && c == currentRoomCol) color = Color.GREEN;
                else if (map[r][c].getType() == RoomType.BOSS && !map[r][c].isCleared()) color = Color.RED;
                else if (map[r][c].isCleared()) color = Color.CYAN;
                else if (map[r][c].getType() == RoomType.START) color = Color.BLUE;

                drawManager.drawRectangle(startX + c * boxSize, startY + r * boxSize, boxSize - 2, boxSize - 2, color);
            }
        }
    }

    /**
     * Manages collisions between Sword Waves and Players.
     * Handles parry logic for waves.
     */
    private void manageWaveShipCollisions() {
        Set<SwordWave> recyclable = new HashSet<>();

        for (SwordWave wave : this.swordWaves) {
            // 1P Collision Check
            if (livesP1 > 0 && !ship.isDestroyed() && !ship.isInvincible()) {
                if (checkCollision(wave, ship)) {
                    if (ship.isParrying()) {
                        SoundManager.play("sfx/parry.wav");
                        triggerImpactEffect(ship.getPositionX(), ship.getPositionY(), 5, 3, Color.YELLOW);
                        recyclable.add(wave);
                    } else {
                        recyclable.add(wave);
                        triggerImpactEffect(ship.getPositionX(), ship.getPositionY(), 5, 3, Color.WHITE);
                        triggerExplosion(ship.getPositionX(), ship.getPositionY(), ship.getColor());
                        ship.destroy();
                        livesP1--;
                        SoundManager.play("sfx/exposion.wav");
                        if (livesP1 > 0) ship.activateInvincibility(2000);
                    }
                }
            }

            // 2P Collision Check
            if (shipP2 != null && livesP2 > 0 && !shipP2.isDestroyed() && !shipP2.isInvincible()) {
                if (checkCollision(wave, shipP2)) {
                    if (shipP2.isParrying()) {
                        SoundManager.play("sfx/parry.wav");
                        triggerImpactEffect(ship.getPositionX(), ship.getPositionY(), 5, 5, Color.YELLOW);
                        recyclable.add(wave);
                    } else {
                        recyclable.add(wave);
                        triggerImpactEffect(shipP2.getPositionX(), shipP2.getPositionY(), 5, 5, Color.YELLOW);
                        triggerExplosion(shipP2.getPositionX(), shipP2.getPositionY(), shipP2.getColor());shipP2.destroy();
                        livesP2--;
                        SoundManager.play("sfx/exposion.wav");
                        if (livesP2 > 0) ship.activateInvincibility(2000);
                    }
                }
            }
        }
        this.swordWaves.removeAll(recyclable);
    }

    /**
     * Returns the set of cleared normal rooms.
     * @return Set of coordinate strings.
     */
    public Set<String> getClearedRooms() { return this.clearedRooms; }

    /**
     * Returns the current row index.
     * @return Row index.
     */
    public int getCurrentRow() { return currentRoomRow; }

    /**
     * Returns the current column index.
     * @return Column index.
     */
    public int getCurrentCol() { return currentRoomCol; }
}