package screen;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import audio.SoundManager;
import engine.*;
import engine.Room;
import engine.Room.RoomType;
import engine.Room.Direction;
import engine.Room.Door;
import engine.level.Level;
import engine.level.LevelManager;
import entity.*;

public class SandboxScreen extends Screen {

    private static final int MAP_SIZE = 5;
    private static final int SEPARATION_LINE_HEIGHT = 45;
    private static final int ITEMS_SEPARATION_LINE_HEIGHT = 400;

    private Room[][] map;
    private int currentRoomRow;
    private int currentRoomCol;

    private Set<String> clearedBossRooms;

    private Ship ship;
    private Ship shipP2;
    private int livesP1;
    private int livesP2;
    private int score;
    private int coin;

    private Set<Bullet> bullets;
    private Set<SwordWave> swordWaves;
    private Set<DropItem> dropItems;
    private Set<BossBullet> bossBullets;

    private Entity currentBoss;
    private SamuraiBoss samuraiBoss;
    private LaserBeamManager laserBeamManager;

    private Cooldown inputDelay;
    private GameTimer gameTimer;
    private boolean isGameOver = false;
    private Cooldown parrySparkCooldown;
    private Entity parrySparkEffect, parrySparkEffect2;
    private static LevelManager levelManager;

    public static int getItemsSeparationLineHeight() { return ITEMS_SEPARATION_LINE_HEIGHT; }

    // [수정] 생성자 파라미터에 savedClearedBossRooms 추가
    public SandboxScreen(final GameState gameState, final int width, final int height, final int fps, Set<String> savedClearedBossRooms) {
        super(width, height, fps);
        levelManager = new LevelManager();
        this.livesP1 = gameState.getLivesRemaining();
        this.livesP2 = gameState.getLivesRemainingP2();
        this.score = gameState.getScore();
        this.coin = gameState.getCoin();
        // 저장된 보스 목록이 있으면 불러오고, 없으면 새로 생성
        this.clearedBossRooms = (savedClearedBossRooms != null) ? savedClearedBossRooms : new HashSet<>();
    }

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
        this.laserBeamManager = new LaserBeamManager();

        createMap();
        this.currentRoomRow = 2;
        this.currentRoomCol = 2;

        this.inputDelay = Core.getCooldown(1000);
        this.inputDelay.reset();
        this.gameTimer = new GameTimer();

        this.parrySparkCooldown = Core.getCooldown(400);
        this.parrySparkEffect = new Entity(0, 0, 13*2, 8*2, Color.ORANGE);
        this.parrySparkEffect.spriteType = DrawManager.SpriteType.ShipDestroyed;
        this.parrySparkEffect2 = new Entity(0, 0, 13*2, 8*2, Color.ORANGE);
        this.parrySparkEffect2.spriteType = DrawManager.SpriteType.ShipDestroyed;
        this.parrySparkCooldown.checkFinished();

        enterRoom(true);
    }

    private void createMap() {
        map = new Room[MAP_SIZE][MAP_SIZE];
        int center = 2;
        for (int row = 0; row < MAP_SIZE; row++) {
            for (int col = 0; col < MAP_SIZE; col++) {
                RoomType type = RoomType.NORMAL;
                EnemyShipFormation formation = null;
                int levelNum = ((row * MAP_SIZE + col) % 7) + 1;

                if (row == center && col == center) type = RoomType.START;
                else if ((row == 0 && col == center) || (row == MAP_SIZE-1 && col == center) ||
                        (row == center && col == 0) || (row == center && col == MAP_SIZE-1)) type = RoomType.BOSS;

                if (type == RoomType.NORMAL) {
                    Level lvl = levelManager.getLevel(levelNum);
                    if (lvl != null) {
                        formation = new EnemyShipFormation(lvl);
                        formation.attach(this);
                        formation.applyEnemyColorByLevel(lvl);
                    }
                }
                map[row][col] = new Room(row, col, type, formation, levelNum);

                // [중요] 이미 깬 보스는 클리어 처리 (부활 방지)
                if (type == RoomType.BOSS && clearedBossRooms.contains(row + "," + col)) {
                    map[row][col].setCleared(true);
                }
                if (type == RoomType.START) map[row][col].addBonfire(width, height);
            }
        }
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

    @Override
    public int run() {
        super.run();
        return this.returnCode;
    }

    @Override
    protected void update() {
        super.update();
        if (!this.inputDelay.checkFinished() || isGameOver) return;
        if (!this.gameTimer.isRunning()) this.gameTimer.start();

        manageInput();
        this.ship.update();
        if (this.shipP2 != null) this.shipP2.update();

        Room room = getCurrentRoom();
        room.update();

        if (room.getType() == RoomType.BOSS && this.currentBoss != null && !room.isCleared()) {
            if (currentBoss instanceof SamuraiBoss) sekiroBossManage();
            else updateCurrentBoss();
        } else if (room.getEnemyFormation() != null && !room.isCleared()) {
            room.getEnemyFormation().update();
            room.getEnemyFormation().shoot(this.bullets);
        }

        if (laserBeamManager != null) laserBeamManager.update();

        manageInteractions(room);
        manageCollisions(room);
        manageItemCollisions();

        cleanBullets();
        cleanSwordWaves();
        cleanItems();
        checkGameOver();

        draw();
    }

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
        }
    }

    private void sekiroBossManage() {
        if (this.samuraiBoss == null) return;
        if (this.samuraiBoss.isDestroyed()) {
            handleBossDeath(getCurrentRoom());
            return;
        }
        this.samuraiBoss.update();
        this.swordWaves.addAll(this.samuraiBoss.shootWave());
    }

    private void manageInteractions(Room room) {
        if (room.isLocked()) return;
        for (Door door : room.getDoors()) {
            boolean p1Touch = checkCollision(ship, door);
            boolean p2Touch = (livesP2 <= 0 || shipP2 == null || shipP2.isDestroyed()) || checkCollision(shipP2, door);
            if (p1Touch && p2Touch) {
                moveRoom(door.direction);
                return;
            }
        }
        if (room.getBonfire() != null) {
            if (checkCollision(ship, room.getBonfire())) {
                if (inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
                    this.livesP1 = 3;
                    if(this.shipP2 != null) this.livesP2 = 3;
                    this.returnCode = 11;
                    this.isRunning = false;
                }
            }
        }
    }

    private void moveRoom(Direction dir) {
        switch(dir) {
            case NORTH: currentRoomRow--; ship.setPositionY(this.height - 100); break;
            case SOUTH: currentRoomRow++; ship.setPositionY(100); break;
            case WEST:  currentRoomCol--; ship.setPositionX(this.width - 100); break;
            case EAST:  currentRoomCol++; ship.setPositionX(100); break;
        }
        if (shipP2 != null && !shipP2.isDestroyed()) {
            shipP2.setPositionX(ship.getPositionX());
            shipP2.setPositionY(ship.getPositionY());
        }
        enterRoom(false);
    }

    private void enterRoom(boolean isFirst) {
        this.bullets.clear();
        this.swordWaves.clear();
        this.bossBullets.clear();
        this.currentBoss = null;
        this.samuraiBoss = null;
        if (this.laserBeamManager != null) this.laserBeamManager = new LaserBeamManager();

        ship.activateInvincibility(2000);
        if (shipP2 != null) shipP2.activateInvincibility(2000);

        // 사무라이 모드 일단 해제
        ship.setMeleeMode(false);
        if(shipP2!=null) shipP2.setMeleeMode(false);

        Room room = getCurrentRoom();
        SoundManager.stopAll();

        // [수정] 사무라이 보스 방이면 칼 모션 켜기
        int center = 2;
        boolean isSamuraiRoom = (currentRoomRow == center && currentRoomCol == MAP_SIZE - 1); // 동쪽 방
        if (isSamuraiRoom) {
            ship.setMeleeMode(true);
            if (shipP2 != null) shipP2.setMeleeMode(true);
        }

        if (room.getType() == RoomType.BOSS && !room.isCleared()) {
            // SoundManager.playLoop("sfx/bgm_boss.wav");
            spawnBoss(room);
        } else if (room.getType() == RoomType.START) {
            // SoundManager.playLoop("sfx/bgm_shop.wav");
        } else {
            // SoundManager.playLoop("sfx/level" + room.getLevelNumber() + ".wav");
        }
    }

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
            // 여기서도 확실하게 켜줌
            ship.setMeleeMode(true);
            if(shipP2 != null) shipP2.setMeleeMode(true);
        } else if (r == center && c == 0) {
            OmegaBoss oBoss = new OmegaBoss(Color.MAGENTA, ITEMS_SEPARATION_LINE_HEIGHT);
            oBoss.attach(this);
            this.currentBoss = oBoss;
        } else if (r == MAP_SIZE-1 && c == center) {
            this.currentBoss = new FinalBoss_3(width/2 - 50, 50, width, height, this.laserBeamManager);
        }
    }

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
                ship.destroy();
                livesP1--;
                SoundManager.play("sfx/explosion.wav");
            }
            if (shipP2 != null && livesP2 > 0 && !shipP2.isDestroyed() && !shipP2.isInvincible() && laserBeamManager.checkCollisionWithShip(shipP2)) {
                shipP2.destroy();
                livesP2--;
                SoundManager.play("sfx/explosion.wav");
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

    private boolean checkPlayerCollision(Entity e) {
        boolean hit = false;
        if (livesP1 > 0 && !ship.isDestroyed() && !ship.isInvincible() && checkCollision(e, ship)) {
            ship.destroy();
            livesP1--;
            SoundManager.play("sfx/explosion.wav");
            hit = true;
        }
        if (shipP2 != null && livesP2 > 0 && !shipP2.isDestroyed() && !shipP2.isInvincible() && checkCollision(e, shipP2)) {
            shipP2.destroy();
            livesP2--;
            SoundManager.play("sfx/explosion.wav");
            hit = true;
        }
        return hit;
    }

    private boolean checkEnemyCollision(Bullet b, Room room) {
        if (room.getEnemyFormation() != null && !room.isCleared()) {
            for (EnemyShip e : room.getEnemyFormation()) {
                if (!e.isDestroyed() && checkCollision(b, e)) {
                    room.getEnemyFormation().destroy(e);
                    this.score += e.getPointValue();
                    this.coin += e.getPointValue() / 3;
                    // [수정] 아이템 드랍 호출
                    spawnItem(e, room.getLevelNumber());
                    return true;
                }
            }
        }
        if (this.currentBoss != null) {
            boolean hit = false;
            if (currentBoss instanceof SamuraiBoss && !((SamuraiBoss)currentBoss).isDestroyed()) { /*Immune*/ }
            else if (currentBoss instanceof FinalBoss && !((FinalBoss)currentBoss).isDestroyed()) {
                if (checkCollision(b, currentBoss)) { ((FinalBoss)currentBoss).takeDamage(1); hit = true; }
            } else if (currentBoss instanceof OmegaBoss && !((OmegaBoss)currentBoss).isDestroyed()) {
                if (checkCollision(b, currentBoss)) { ((OmegaBoss)currentBoss).takeDamage(1); hit = true; }
            } else if (currentBoss instanceof FinalBoss_3 && !((FinalBoss_3)currentBoss).isDestroyed()) {
                if (checkCollision(b, currentBoss)) { ((FinalBoss_3)currentBoss).takeDamage(1); hit = true; }
            }
            if (hit) {
                if (isBossDead(currentBoss)) handleBossDeath(room);
                return true;
            }
        }
        return false;
    }

    private void spawnItem(EnemyShip enemy, int levelNum) {
        // [수정] Core.getFileManager를 통해 레벨을 직접 로드하여 확실하게 처리
        Level lvl = levelManager.getLevel(levelNum);
        if (lvl == null || lvl.getItemDrops() == null) return;
        List<engine.level.ItemDrop> drops = lvl.getItemDrops();
        for (engine.level.ItemDrop drop : drops) {
            if (drop.getEnemyType().equals(enemy.getEnemyType()) && Math.random() < drop.getDropChance()) {
                DropItem.ItemType type = DropItem.fromString(drop.getItemId());
                if (type != null) {
                    dropItems.add(ItemPool.getItem(enemy.getPositionX(), enemy.getPositionY(), 2, type));
                }
            }
        }
    }

    private boolean isBossDead(Entity boss) {
        if (boss instanceof SamuraiBoss) return ((SamuraiBoss)boss).isDestroyed();
        if (boss instanceof FinalBoss) return ((FinalBoss)boss).getHealPoint() <= 0;
        if (boss instanceof OmegaBoss) return ((OmegaBoss)boss).isDestroyed();
        if (boss instanceof FinalBoss_3) return ((FinalBoss_3)boss).isDestroyed();
        return false;
    }

    private void handleBossDeath(Room room) {
        this.score += 1000;
        room.setCleared(true);
        clearedBossRooms.add(currentRoomRow + "," + currentRoomCol);
        this.ship.setMeleeMode(false);
        if(shipP2!=null) shipP2.setMeleeMode(false);
        this.currentBoss = null;
        this.bossBullets.clear();
        SoundManager.stopAll();
        // SoundManager.play("sfx/level1.wav");
        if (clearedBossRooms.size() >= 4) {
            this.returnCode = 2; // ScoreScreen
            this.isRunning = false;
        }
    }

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
            } else if (!isBossDead(currentBoss)) {
                checkPlayerCollision(currentBoss);
            }
        }
    }

    private void handleSekiroCollision(Ship player, SamuraiBoss boss) {
        if (!checkCollision(player, boss)) return;
        if (player.isParrying() && boss.isPostureBroken() && !boss.isInvincibleAfterBroken()) {
            boss.executeDeathblow();
            // SoundManager.play("sfx/samurai-kill.wav");
            player.activateInvincibility(3000);
            if (boss.isDestroyed()) handleBossDeath(getCurrentRoom());
        } else if (player.isParrying() && boss.isAttacking()) {
            boss.onParried();
            // SoundManager.play("sfx/parry.wav");
            this.parrySparkCooldown.reset();
        } else if (player.isParrying() && !boss.isAttacking()) {
            boss.takeDamage(1);
            if (!boss.isPostureBroken()) boss.takePostureDamage(boss.isEnraged() ? 10 : 20);
        } else if (boss.isAttacking() && !player.isInvincible()) {
            player.destroy();
            if (player.getPlayerId() == 1) livesP1--; else livesP2--;
            SoundManager.play("sfx/explosion.wav");
        }
    }

    private boolean checkCollision(Entity a, Entity b) {
        return a.getPositionX() < b.getPositionX() + b.getWidth() &&
                a.getPositionX() + a.getWidth() > b.getPositionX() &&
                a.getPositionY() < b.getPositionY() + b.getHeight() &&
                a.getHeight() + a.getPositionY() > b.getPositionY();
    }

    private void manageInput() {
        if (this.livesP1 > 0 && !this.ship.isDestroyed()) {
            if (inputManager.isKeyDown(KeyEvent.VK_D) && ship.getPositionX() + ship.getWidth() < width) this.ship.moveRight();
            if (inputManager.isKeyDown(KeyEvent.VK_A) && ship.getPositionX() > 0) this.ship.moveLeft();
            if (inputManager.isKeyDown(KeyEvent.VK_W) && ship.getPositionY() > SEPARATION_LINE_HEIGHT) this.ship.moveUp();
            if (inputManager.isKeyDown(KeyEvent.VK_S) && ship.getPositionY() + ship.getHeight() < height) this.ship.moveDown();
            if (inputManager.isKeyDown(KeyEvent.VK_SPACE)) this.ship.shoot(this.bullets);
        }
        if (this.shipP2 != null && this.livesP2 > 0 && !this.shipP2.isDestroyed()) {
            if (inputManager.isKeyDown(KeyEvent.VK_RIGHT) && shipP2.getPositionX() + shipP2.getWidth() < width) this.shipP2.moveRight();
            if (inputManager.isKeyDown(KeyEvent.VK_LEFT) && shipP2.getPositionX() > 0) this.shipP2.moveLeft();
            if (inputManager.isKeyDown(KeyEvent.VK_UP) && shipP2.getPositionY() > SEPARATION_LINE_HEIGHT) this.shipP2.moveUp();
            if (inputManager.isKeyDown(KeyEvent.VK_DOWN) && shipP2.getPositionY() + shipP2.getHeight() < height) this.shipP2.moveDown();
            if (inputManager.isKeyDown(KeyEvent.VK_ENTER) || inputManager.isKeyDown(KeyEvent.VK_NUMPAD0)) this.shipP2.shoot(this.bullets);
        }
    }

    private void cleanBullets() {
        Set<Bullet> recyclable = new HashSet<>();
        for (Bullet b : this.bullets) {
            b.update();
            if (b.getPositionY() < SEPARATION_LINE_HEIGHT || b.getPositionY() > this.height) recyclable.add(b);
        }
        this.bullets.removeAll(recyclable);
        BulletPool.recycle(recyclable);
    }

    private void cleanSwordWaves() {
        Set<SwordWave> recyclable = new HashSet<>();
        for (SwordWave w : this.swordWaves) {
            w.update();
            if (w.getPositionY() > this.height) recyclable.add(w);
        }
        this.swordWaves.removeAll(recyclable);
    }

    private void cleanItems() {
        Set<DropItem> recyclable = new HashSet<>();
        for (DropItem i : this.dropItems) {
            i.update();
            if (i.getPositionY() > this.height) recyclable.add(i);
        }
        this.dropItems.removeAll(recyclable);
        ItemPool.recycle(recyclable);
    }

    private void checkGameOver() {
        if (livesP1 <= 0 && (shipP2 == null || livesP2 <= 0)) {
            isGameOver = true;
            // SoundManager.play("sfx/gameover.wav");
            this.returnCode = 2;
            this.isRunning = false;
        }
    }

    private Room getCurrentRoom() { return map[currentRoomRow][currentRoomCol]; }

    public Set<String> getClearedBossRooms() { return this.clearedBossRooms; } // [추가] Getter

    public GameState getGameState() {
        return new GameState(1, score, livesP1, livesP2, 0, 0, coin);
    }

    private void draw() {
        drawManager.initDrawing(this);
        Room room = getCurrentRoom();
        room.draw(drawManager);
        if (this.ship != null && this.livesP1 > 0) {
            drawManager.drawEntity(ship, ship.getPositionX(), ship.getPositionY());
            if (this.ship.isParrying()) {
                Entity slash = this.ship.getSwordSlashEffect();
                drawManager.drawEntity(slash, slash.getPositionX(), slash.getPositionY());
            }
        }
        if (this.shipP2 != null && this.livesP2 > 0) {
            drawManager.drawEntity(shipP2, shipP2.getPositionX(), shipP2.getPositionY());

            if (this.shipP2.isParrying()) {
                Entity slash = this.shipP2.getSwordSlashEffect();
                drawManager.drawEntity(slash, slash.getPositionX(), slash.getPositionY());
            }
        }

        for (Bullet b : this.bullets) drawManager.drawEntity(b, b.getPositionX(), b.getPositionY());
        for (BossBullet bb : this.bossBullets) drawManager.drawEntity(bb, bb.getPositionX(), bb.getPositionY());
        for (SwordWave w : this.swordWaves) drawManager.drawEntity(w, w.getPositionX(), w.getPositionY());
        for (DropItem d : this.dropItems) drawManager.drawEntity(d, d.getPositionX(), d.getPositionY());

        if (laserBeamManager != null) {
            for (LaserBeam beam : laserBeamManager.getBeams()) drawManager.drawLaserBeam(beam);
        }

        if (this.currentBoss != null && room.getType() == RoomType.BOSS && !room.isCleared()) {
            drawManager.drawEntity(this.currentBoss, this.currentBoss.getPositionX(), this.currentBoss.getPositionY());
            if (currentBoss instanceof SamuraiBoss) {
                SamuraiBoss sBoss = (SamuraiBoss) currentBoss;
                drawManager.drawBossHealthBar(this, sBoss.getHealPoint(), sBoss.getMaxHealth());
                drawManager.drawBossPostureBar(this, sBoss.getPosture(), sBoss.getMaxPosture());
                if (sBoss.isPostureBroken()) drawManager.drawDeathblowMarker(this, sBoss.getPositionX() + sBoss.getWidth()/2, sBoss.getPositionY() + sBoss.getHeight()/2);
                if (!this.parrySparkCooldown.checkFinished()) {
                    drawManager.drawEntity(this.parrySparkEffect, this.parrySparkEffect.getPositionX(), this.parrySparkEffect.getPositionY());
                    drawManager.drawEntity(this.parrySparkEffect2, this.parrySparkEffect2.getPositionX(), this.parrySparkEffect2.getPositionY());
                }
            } else if (currentBoss instanceof FinalBoss_3) {
                FinalBoss_3 fb3 = (FinalBoss_3) currentBoss;
                if (fb3.isLaserWarningActive()) {
                    for (float angle : fb3.getPendingWarningAngles()) {
                        drawManager.drawLaserWarningLine(fb3.getWarningOriginX(), fb3.getWarningOriginY(), angle);
                    }
                }
            }
        }

        drawMinimap();
        drawManager.drawScore(this, this.score);
        drawManager.drawLives(this, this.livesP1);
        if (this.shipP2 != null) drawManager.drawLivesP2(this, this.livesP2);
        drawManager.drawItemsHUD(this);
        drawManager.drawHorizontalLine(this, SEPARATION_LINE_HEIGHT - 1);
        drawManager.drawHorizontalLine(this, ITEMS_SEPARATION_LINE_HEIGHT);

        drawManager.completeDrawing(this);
    }

    private void drawMinimap() {
        int boxSize = 10;
        int startX = this.width - (MAP_SIZE * boxSize) - 20;
        int startY = 50;
        for (int r = 0; r < MAP_SIZE; r++) {
            for (int c = 0; c < MAP_SIZE; c++) {
                Color color = Color.GRAY;
                if (r == currentRoomRow && c == currentRoomCol) color = Color.GREEN;
                else if (map[r][c].isCleared()) color = Color.DARK_GRAY;
                else if (map[r][c].getType() == RoomType.BOSS) color = Color.RED;
                else if (map[r][c].getType() == RoomType.START) color = Color.BLUE;
                drawManager.drawRectangle(startX + c * boxSize, startY + r * boxSize, boxSize - 2, boxSize - 2, color);
            }
        }
    }
}