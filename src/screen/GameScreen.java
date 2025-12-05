package screen;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

import audio.SoundManager;
import engine.*;
import engine.LaserBeamManager;
import engine.level.Level;
import engine.level.LevelManager;
import entity.*;

/**
 * Implements the game screen, where the action happens.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public class GameScreen extends Screen {

	/** Milliseconds until the screen accepts user input. */
	private static final int INPUT_DELAY = 6000;
	/** Bonus score for each life remaining at the end of the level. */
	private static final int LIFE_SCORE = 100;
	/** Minimum time between bonus ship's appearances. */
	private static final int BONUS_SHIP_INTERVAL = 20000;
	/** Maximum variance in the time between bonus ship's appearances. */
	private static final int BONUS_SHIP_VARIANCE = 10000;
	/** Time until bonus ship explosion disappears. */
	private static final int BONUS_SHIP_EXPLOSION = 500;
	/** Time until boss explosion disappears. */
	private static final int BOSS_EXPLOSION = 600;
	/** Time from finishing the level to screen change. */
	private static final int SCREEN_CHANGE_INTERVAL = 1500;
	/** Height of the interface separation line. */
	private static final int SEPARATION_LINE_HEIGHT = 45;
	/** Height of the items separation line (above items). */
	private static final int ITEMS_SEPARATION_LINE_HEIGHT = 400;
	/** Popup text shown on boss collisions. */
	private static final String BOSS_COLLISION_HEALTH_POPUP = "-1 Life (Boss Collision!)";
	/** Popup text shown on standard health loss. */
	private static final String GENERIC_HEALTH_POPUP = "-1 Health";
	/** Log suffix indicating lives left. */
	private static final String LIVES_REMAINING_SUFFIX = " lives remaining.";
	/** Log prefix for generic player hit. */
	private static final String HIT_ON_PLAYER_PREFIX = "Hit on player ship, ";

	/** Returns the Y-coordinate of the bottom boundary for enemies (above items HUD) */
	public static int getItemsSeparationLineHeight() {
		return ITEMS_SEPARATION_LINE_HEIGHT;
	}

	/** Current level data (direct from Level system). */
	private Level currentLevel;
	/** Current difficulty level number. */
	private int level;
	/** Formation of enemy ships. */
	private EnemyShipFormation enemyShipFormation;
	/** Formation of special enemy ships. */
	private EnemyShipSpecialFormation enemyShipSpecialFormation;
	/** Player's ship. */
	private Ship ship;
	/** Second Player's ship. */
	private Ship shipP2;
	/** Bonus enemy ship that appears sometimes. */
	private EnemyShip enemyShipSpecial;
	/** Minimum time between bonus ship appearances. */
	private Cooldown enemyShipSpecialCooldown;
	/** team drawing may implement */
	private FinalBoss finalBoss;
	private FinalBoss_3 finalBoss3;
	/** Time until bonus ship explosion disappears. */
	private Cooldown enemyShipSpecialExplosionCooldown;
	/** Time until Boss explosion disappears. */
	private Cooldown bossExplosionCooldown;
	/** Time from finishing the level to screen change. */
	private Cooldown screenFinishedCooldown;
	/** OmegaBoss */
	private MidBoss omegaBoss;
	/** SamuraiBoss */
	private SamuraiBoss samuraiBoss;
	/** boss4 */
	private Boss4 boss4;
	/** SamuraiBoss Sword wave pattern. */
	private Set<SwordWave> swordWaves;
	/** Set of all bullets fired by on-screen ships. */
	private Set<Bullet> bullets;
	/** Set of all dropItems dropped by on screen ships. */
	private Set<DropItem> dropItems;
	/** Current score (legacy total). */
	private int score;
	// === [ADD] Independent scores for two players ===
	private int scoreP1 = 0;
	private int scoreP2 = 0;
	/** current level parameter */
	public Level currentlevel;
	/** Player lives left. */
	private int livesP1;
	private int livesP2;
	/** Total bullets shot by the player. */
	private int bulletsShot;
	/** Total ships destroyed by the player. */
	private int shipsDestroyed;
	/** Moment the game starts. */
	private long gameStartTime;
	/** Checks if the level is finished. */
	private boolean levelFinished;
	/** Checks if a bonus life is received. */
	private boolean bonusLife;
	/** Maximum number of lives. */
	private int maxLives;
	/** Current coin. */
	private int coin;

	/** Melee mode parry spark effect. */
	private Entity parrySparkEffect;
	private Entity parrySparkEffect2;
	/** Melee mode parry spark cooldown. */
	private Cooldown parrySparkCooldown;

	/** Laser beam manager for handling laser beams. */
	private LaserBeamManager laserBeamManager;

	// Unified scoring entry: maintains both P1/P2 and legacy this.score (total score)
	private void addPointsFor(Bullet bullet, int pts) {
		Integer owner = (bullet != null ? bullet.getOwnerId() : null);
		if (owner != null && owner == 2) {
			this.scoreP2 += pts;   // P2
		} else {
			this.scoreP1 += pts;   // Default to P1 (for null compatibility)
		}
		this.score += pts;        // Keep maintaining the total score, for legacy process compatibility
	}

	/** bossBullets carry bullets which Boss fires */
	private Set<BossBullet> bossBullets;
	/** Is the bullet on the screen erased */
	private boolean is_cleared = false;
	/** Timer to track elapsed time. */
	private GameTimer gameTimer;
	/** Elapsed time since the game started. */
	private long elapsedTime;
	// Achievement popup
	private String achievementText;
	private Cooldown achievementPopupCooldown;

	private enum StagePhase {wave, boss_wave}
	private StagePhase currentPhase;

	/** Health change popup. */
	private String healthPopupText;
	private Cooldown healthPopupCooldown;

	private boolean smallHitboxMode = false;

	private GameState gameState;

	/** Input delay cooldown (was referenced in code). */
	private Cooldown inputDelay;

    private PauseManager pauseManager;
    private boolean escLast;

	/** Laser beam manager for handling laser beams. */
	// (already declared above as laserBeamManager)

	/**
	 * Constructor, establishes the properties of the screen.
	 *
	 * @param gameState Current game state.
	 * @param level     Current level settings.
	 * @param bonusLife Checks if a bonus life is awarded this level.
	 * @param maxLives  Maximum number of lives.
	 * @param width     Screen width.
	 * @param height    Screen height.
	 * @param fps       Frames per second, frame rate at which the game is run.
	 */
	public GameScreen(final GameState gameState,
					  final Level level, final boolean bonusLife, final int maxLives,
					  final int width, final int height, final int fps) {
		super(width, height, fps);

		this.currentLevel = level;
		this.bonusLife = bonusLife;
		this.currentlevel = level;
		this.maxLives = maxLives;
		this.level = gameState.getLevel();
		this.score = gameState.getScore();
		this.coin = gameState.getCoin();
		this.livesP1 = gameState.getLivesRemaining();
		this.livesP2 = gameState.getLivesRemainingP2();
		this.gameState = gameState;
		if (this.bonusLife) {
			if (this.livesP1 > 0 && this.livesP1 < this.maxLives) {
				this.livesP1++;
			}
			if (this.livesP2 > 0 && this.livesP2 < this.maxLives) {
				this.livesP2++;
			}
		}
		this.bulletsShot = gameState.getBulletsShot();
		this.shipsDestroyed = gameState.getShipsDestroyed();
	}

	/**
	 * Initializes basic screen properties, and adds necessary elements.
	 */
	public final void initialize() {
		super.initialize();
		/** Initialize the bullet Boss fired */
		this.bossBullets = new HashSet<>();
		this.swordWaves = new HashSet<>();
		this.laserBeamManager = new LaserBeamManager();

		enemyShipFormation = new EnemyShipFormation(this.currentLevel);
		enemyShipFormation.attach(this);
		this.enemyShipFormation.applyEnemyColorByLevel(this.currentLevel);

		this.ship = new Ship(this.width / 2 - 100, ITEMS_SEPARATION_LINE_HEIGHT - 20, Color.green);
		this.ship.setPlayerId(1);

		if (this.livesP2 > 0) {
			this.shipP2 = new Ship(this.width / 2 + 100, ITEMS_SEPARATION_LINE_HEIGHT - 20, Color.pink);
			this.shipP2.setPlayerId(2);
		} else {
			this.shipP2 = null;
		}

		// special enemy initial
		enemyShipSpecialFormation = new EnemyShipSpecialFormation(this.currentLevel,
				Core.getVariableCooldown(BONUS_SHIP_INTERVAL, BONUS_SHIP_VARIANCE),
				Core.getCooldown(BONUS_SHIP_EXPLOSION));
		enemyShipSpecialFormation.attach(this);

		this.bossExplosionCooldown = Core.getCooldown(BOSS_EXPLOSION);
		this.screenFinishedCooldown = Core.getCooldown(SCREEN_CHANGE_INTERVAL);
		this.bullets = new HashSet<Bullet>();
		this.dropItems = new HashSet<DropItem>();

		// Special input delay / countdown.
		this.gameStartTime = System.currentTimeMillis();
		this.inputDelay = Core.getCooldown(INPUT_DELAY);
		this.inputDelay.reset();

		this.ship.setMeleeMode(false);
		if (this.shipP2 != null) {
			this.shipP2.setMeleeMode(false);
		}
		this.parrySparkCooldown = Core.getCooldown(400);
		this.parrySparkEffect = new Entity(0, 0, 13*2, 8*2, Color.ORANGE);
		this.parrySparkEffect.spriteType = DrawManager.SpriteType.ShipDestroyed;
		this.parrySparkEffect2 = new Entity(0, 0, 13*2, 8*2, Color.ORANGE);
		this.parrySparkEffect2.spriteType = DrawManager.SpriteType.ShipDestroyed;
		this.parrySparkCooldown.checkFinished();

		this.gameTimer = new GameTimer();
		this.elapsedTime = 0;
		this.finalBoss = null;
		this.finalBoss3 = null;
		this.omegaBoss = null;
		this.samuraiBoss = null;
		this.boss4 = null;
		this.currentPhase = StagePhase.wave;

        this.pauseManager = new PauseManager();
        this.escLast = false;
	}

	/**
	 * Starts the action.
	 *
	 * @return Next screen code.
	 */
	public final int run() {
		super.run();

		// legacy end-of-screen score addition is performed when screen finishes.
		// Actual gameplay loop updates happen via update() called externally.
		return this.returnCode;
	}

	/**
	 * Updates the elements on screen and checks for events.
	 */
	protected final void update() {
		super.update();

		// ----- Pause handling (from feature branch) -----
		boolean esc = inputManager.isKeyDown(KeyEvent.VK_ESCAPE);
		boolean inputReady = (this.inputDelay != null && this.inputDelay.checkFinished());

		if (inputReady && esc && !escLast && !this.levelFinished) {
			pauseManager.togglePause();
			pauseManager.resetFlags();
			if (pauseManager.isPaused() && this.gameTimer != null && this.gameTimer.isRunning()) {
				this.gameTimer.stop();
			}
		}
		escLast = esc;

		if (pauseManager.isPaused()) {
			pauseManager.update(inputManager);

			if (pauseManager.wantQuit) {
				this.returnCode = 1; // Back to title
				this.isRunning = false;
			} else if (pauseManager.wantReset) {
				this.returnCode = 2; // Restart current level
				this.isRunning = false;
			} else if (!pauseManager.isPaused() && inputReady) {
				if (this.gameTimer != null && !this.gameTimer.isRunning()) {
					this.gameTimer.start();
				}
			}

			pauseManager.resetFlags();
			draw();
			return;
		}

		if (this.inputDelay.checkFinished() && !this.levelFinished) {

			if (!this.gameTimer.isRunning()) {
				this.gameTimer.start();
			}

			if (this.livesP1 > 0 && !this.ship.isDestroyed()) {
				boolean p1Right = inputManager.isP1KeyDown(KeyEvent.VK_D);
				boolean p1Left = inputManager.isP1KeyDown(KeyEvent.VK_A);
				boolean p1Up = inputManager.isP1KeyDown(KeyEvent.VK_W);
				boolean p1Down = inputManager.isP1KeyDown(KeyEvent.VK_S);
				boolean p1Fire = inputManager.isP1KeyDown(KeyEvent.VK_SPACE);

				boolean isRightBorder = this.ship.getPositionX()
						+ this.ship.getWidth() + this.ship.getSpeed() > this.width - 1;
				boolean isLeftBorder = this.ship.getPositionX() - this.ship.getSpeed() < 1;
				boolean isUpBorder = this.ship.getPositionY() - this.ship.getSpeed() < SEPARATION_LINE_HEIGHT;
				boolean isDownBorder = this.ship.getPositionY()
						+ this.ship.getHeight() + this.ship.getSpeed() > ITEMS_SEPARATION_LINE_HEIGHT;

				if (p1Right && !isRightBorder) this.ship.moveRight();
				if (p1Left && !isLeftBorder) this.ship.moveLeft();
				if (p1Up && !isUpBorder) this.ship.moveUp();
				if (p1Down && !isDownBorder) this.ship.moveDown();

				if (p1Fire) {
					if (this.ship.shoot(this.bullets)) {
						this.bulletsShot++;
						AchievementManager.getInstance().onShotFired();
					}
				}
			}

			if (this.shipP2 != null && this.livesP2 > 0 && !this.shipP2.isDestroyed()) {
				boolean p2Right = inputManager.isP2KeyDown(KeyEvent.VK_RIGHT);
				boolean p2Left = inputManager.isP2KeyDown(KeyEvent.VK_LEFT);
				boolean p2Up = inputManager.isP2KeyDown(KeyEvent.VK_UP);
				boolean p2Down = inputManager.isP2KeyDown(KeyEvent.VK_DOWN);
				boolean p2Fire = inputManager.isP2KeyDown(KeyEvent.VK_ENTER);

				boolean p2RightBorder = this.shipP2.getPositionX()
						+ this.shipP2.getWidth() + this.shipP2.getSpeed() > this.width - 1;
				boolean p2LeftBorder = this.shipP2.getPositionX() - this.shipP2.getSpeed() < 1;
				boolean p2UpBorder = this.shipP2.getPositionY() - this.shipP2.getSpeed() < SEPARATION_LINE_HEIGHT;
				boolean p2DownBorder = this.shipP2.getPositionY()
						+ this.shipP2.getHeight() + this.shipP2.getSpeed() > ITEMS_SEPARATION_LINE_HEIGHT;

				if (p2Right && !p2RightBorder) this.shipP2.moveRight();
				if (p2Left && !p2LeftBorder) this.shipP2.moveLeft();
				if (p2Up && !p2UpBorder) this.shipP2.moveUp();
				if (p2Down && !p2DownBorder) this.shipP2.moveDown();

				if (p2Fire) {
					if (this.shipP2.shoot(this.bullets)) {
						this.bulletsShot++;
						AchievementManager.getInstance().onShotFired();
					}
				}
			}

			switch (this.currentPhase) {
				case wave:
					if (!DropItem.isTimeFreezeActive()) {
						this.enemyShipFormation.update();
						this.enemyShipFormation.shoot(this.bullets);
					}
					if (this.enemyShipFormation.isEmpty()) {
						this.currentPhase = StagePhase.boss_wave;
					}
					break;
				case boss_wave:
					if (this.finalBoss == null && this.omegaBoss == null && this.samuraiBoss == null && this.finalBoss3 == null && this.boss4 == null) {
						bossReveal();
						this.enemyShipFormation.clear();
					}
					// manage various bosses
					if (this.finalBoss3 != null && !this.finalBoss3.isDestroyed()) {
						finalBoss3Manage();
					}
					if (this.finalBoss != null) {
						finalbossManage();
					} else if (this.omegaBoss != null) {
						this.omegaBoss.update();
						if (this.omegaBoss.isDestroyed()) {
							if ("omegaAndFinal".equals(this.currentlevel.getBossId())) {
								this.omegaBoss = null;
								this.finalBoss = new FinalBoss(this.width / 2 - 50, 50, this.width, this.height);
								this.logger.info("Final Boss has spawned!");
							} else {
								this.levelFinished = true;
								this.screenFinishedCooldown.reset();
							}
						}
					} else if (this.samuraiBoss != null) {
						// Samurai boss handled in its manager later
					} else if (this.finalBoss3 != null) {
						// already handled
					} else if (this.boss4 != null) {
						boss4Manage();
					} else {
						if (!this.levelFinished) {
							this.levelFinished = true;
							this.screenFinishedCooldown.reset();
						}
					}
					break;
			}

			// Update player ships
			this.ship.update();
			if (this.shipP2 != null) {
				this.shipP2.update();
			}

			// Samurai boss manager
			if (this.samuraiBoss != null) {
				sekiroBossManage();
			}

			// special enemy update
			this.enemyShipSpecialFormation.update();

			// Manage collisions and cleanup
			manageBulletShipCollisions();
			manageShipEnemyCollisions();
			manageItemCollisions();
			manageWaveShipCollisions();

			cleanBullets();
			cleanItems();
			cleanSwordWaves();
		}

		// Collision LaserBeam
		if (!this.levelFinished) {
			//P1 Collision
			if (this.livesP1 > 0 && this.ship != null && !this.ship.isDestroyed()
					&& laserBeamManager.checkCollisionWithShip(this.ship)) {

				if (!this.ship.isInvincible()) {
					this.ship.destroy();
					this.livesP1--;
					showHealthPopup(GENERIC_HEALTH_POPUP);
					this.logger.info("Player hit by LaserBeam!");
				}
			}

			// P2 Collision
			if (this.shipP2 != null && this.livesP2 > 0 && !this.shipP2.isDestroyed()
					&& laserBeamManager.checkCollisionWithShip(this.shipP2)) {

				if (!this.shipP2.isInvincible()) {
					this.shipP2.destroy();
					this.livesP2--;
					showHealthPopup(GENERIC_HEALTH_POPUP);
					this.logger.info("Player2 hit by LaserBeam!");
				}
			}
		}

		laserBeamManager.update();
		draw();

		if (((this.livesP1 == 0) && (this.shipP2 == null || this.livesP2 == 0)) && !this.levelFinished) {
			this.levelFinished = true;
			this.screenFinishedCooldown.reset();
			if (this.gameTimer.isRunning()) {
				this.gameTimer.stop();
			}

			if ((this.livesP1 > 0) || (this.shipP2 != null && this.livesP2 > 0)) {
				if (this.level == 1) {
					AchievementManager.getInstance().unlockAchievement("Beginner");
				} else if (this.level == 3) {
					AchievementManager.getInstance().unlockAchievement("Intermediate");
				}
			}
		}
		if (this.levelFinished && this.screenFinishedCooldown.checkFinished()) {
			if (this.livesP1 > 0 || (this.shipP2 != null && this.livesP2 > 0)) { // Check for win condition
				if (this.currentlevel.getCompletionBonus() != null) {
					this.coin += this.currentlevel.getCompletionBonus().getCurrency();
					this.logger.info("Awarded " + this.currentlevel.getCompletionBonus().getCurrency() + " coins for level completion.");
				}

				String achievement = this.currentlevel.getAchievementTrigger();
				if (achievement != null && !achievement.isEmpty()) {
					AchievementManager.getInstance().unlockAchievement(achievement);
					this.logger.info("Unlocked achievement: " + achievement);
				}
			}
			this.isRunning = false;
			// award life score when the screen finishes
			this.score += LIFE_SCORE * (this.livesP1 - 1);
			this.score += LIFE_SCORE * (this.livesP2 - 1);
			this.logger.info("Screen cleared with a score of " + this.score);
		}
	}

	/**
	 * Draws the elements associated with the screen.
	 */
	private void draw() {
		drawManager.initDrawing(this);
		Graphics g = drawManager.getBackBufferGraphics();

		if (this.livesP1 > 0) {
			drawManager.drawEntity(this.ship, this.ship.getPositionX(),
					this.ship.getPositionY());
			if (this.smallHitboxMode) {
				g.setColor(Color.WHITE);
				g.fillRect(this.ship.getPositionX() + this.ship.getWidth() / 2 - 2, this.ship.getPositionY() + this.ship.getHeight() / 2 - 2, 5, 5);
			}

			if (this.ship.isParrying()) {
				Entity slash = this.ship.getSwordSlashEffect();
				drawManager.drawEntity(slash, slash.getPositionX(), slash.getPositionY());
			}
		}

		if (this.shipP2 != null && this.livesP2 > 0) {
			drawManager.drawEntity(this.shipP2, this.shipP2.getPositionX(), this.shipP2.getPositionY());
			if (this.smallHitboxMode) {
				g.setColor(Color.WHITE);
				g.fillRect(this.shipP2.getPositionX() + this.shipP2.getWidth() / 2 - 2, this.shipP2.getPositionY() + this.shipP2.getHeight() / 2 - 2, 5, 5);
			}

			if (this.shipP2.isParrying()) {
				Entity slashP2 = this.shipP2.getSwordSlashEffect();
				drawManager.drawEntity(slashP2, slashP2.getPositionX(), slashP2.getPositionY());
			}
		}

		if (this.samuraiBoss != null && !this.samuraiBoss.isDestroyed()) {
			drawManager.drawEntity(this.samuraiBoss, this.samuraiBoss.getPositionX(), this.samuraiBoss.getPositionY());
			// Draws Health / Posture bar in screen.
			drawManager.drawBossHealthBar(this, this.samuraiBoss.getHealPoint(), this.samuraiBoss.getMaxHealth());
			drawManager.drawBossPostureBar(this, this.samuraiBoss.getPosture(), this.samuraiBoss.getMaxPosture());

			// Draws death marker on samurai boss.
			if (this.samuraiBoss.isPostureBroken()) {
				drawManager.drawDeathblowMarker(this,
						this.samuraiBoss.getPositionX() + (this.samuraiBoss.getWidth() / 2),
						this.samuraiBoss.getPositionY() + (this.samuraiBoss.getWidth() / 2));
			}
			// Draws parry spark effect.
			if (!this.parrySparkCooldown.checkFinished()) {
				drawManager.drawEntity(this.parrySparkEffect,
						this.parrySparkEffect.getPositionX(),
						this.parrySparkEffect.getPositionY());
				drawManager.drawEntity(this.parrySparkEffect2,
						this.parrySparkEffect2.getPositionX(),
						this.parrySparkEffect2.getPositionY());
			}
		}

		// special enemy draw
		enemyShipSpecialFormation.draw();

		// draw boss bullets (shared for any boss)
		if (bossBullets != null) {
			for (BossBullet bossBullet : bossBullets) {
				drawManager.drawEntity(bossBullet, bossBullet.getPositionX(), bossBullet.getPositionY());
			}
		}

		// draw bosses
		if (this.finalBoss != null && !this.finalBoss.isDestroyed()) {
			drawManager.drawEntity(finalBoss, finalBoss.getPositionX(), finalBoss.getPositionY());
		}

		if (this.finalBoss3 != null && !this.finalBoss3.isDestroyed()) {
			drawManager.drawLaserBoss(finalBoss3);
			drawManager.drawBossHealthBar(this, this.finalBoss3.getHealPoint(), this.finalBoss3.getMaxHp());
		}
		if (this.finalBoss3 != null && this.finalBoss3.isBossWaveActiveOrTransition()) {
			drawManager.drawBossWaveDim(this, 0.35f);
		}
		if (this.finalBoss3 != null && this.finalBoss3.isBossWaveActive()) {
			drawManager.drawBossGlow(finalBoss3.getPositionX(), finalBoss3.getPositionY(), finalBoss3.getWidth(), finalBoss3.getHeight(), 1f);
		}
		if (finalBoss3 != null && finalBoss3.isLaserWarningActive()) {
			Color warnColor = finalBoss3.isBossWaveWarningActive()
					? new Color(255, 80, 80, 200)
					: new Color(0, 255, 255, 180);
			for (float angle : finalBoss3.getPendingWarningAngles()) {
				drawManager.drawLaserWarningLine(
						finalBoss3.getWarningOriginX(),
						finalBoss3.getWarningOriginY(),
						angle,
						warnColor
				);
			}
		}



		for (LaserBeam beam : laserBeamManager.getBeams()) {
			drawManager.drawLaserBeam(beam);
		}

		if (this.boss4 != null && !this.boss4.isDestroyed()) {
			// boss4 has its own draw which expects Graphics
			this.boss4.draw(g);
		}

		enemyShipFormation.draw();

		if (this.omegaBoss != null) {
			this.omegaBoss.draw(drawManager);
		}

		for (Bullet bullet : this.bullets)
			drawManager.drawEntity(bullet, bullet.getPositionX(),
					bullet.getPositionY());

		for (SwordWave wave : this.swordWaves)
			drawManager.drawEntity(wave, wave.getPositionX(),
					wave.getPositionY());

		for (DropItem dropItem : this.dropItems)
			drawManager.drawEntity(dropItem, dropItem.getPositionX(), dropItem.getPositionY());

		// Interface.
		drawManager.drawScore(this, this.scoreP1);   // Top line still displays P1
		drawManager.drawScoreP2(this, this.scoreP2); // Added second line for P2
		drawManager.drawCoin(this, this.coin);
		drawManager.drawLives(this, this.livesP1);
		drawManager.drawLivesP2(this, this.livesP2);
		drawManager.drawTime(this, this.elapsedTime);
		drawManager.drawItemsHUD(this);
		drawManager.drawLevel(this, this.currentLevel.getLevelName());
		drawManager.drawHorizontalLine(this, SEPARATION_LINE_HEIGHT - 1);
		drawManager.drawHorizontalLine(this, ITEMS_SEPARATION_LINE_HEIGHT);

		if (this.achievementText != null && !this.achievementPopupCooldown.checkFinished()) {
			drawManager.drawAchievementPopup(this, this.achievementText);
		} else {
			this.achievementText = null; // clear once expired
		}

		// Health notification popup
		if (this.healthPopupText != null && !this.healthPopupCooldown.checkFinished()) {
			drawManager.drawHealthPopup(this, this.healthPopupText);
		} else {
			this.healthPopupText = null;
		}

		// Countdown to game start.
		if (!this.inputDelay.checkFinished()) {
			int countdown = (int) ((INPUT_DELAY
					- (System.currentTimeMillis()
					- this.gameStartTime)) / 1000);
			drawManager.drawCountDown(this, this.level, countdown,
					this.bonusLife);
			drawManager.drawHorizontalLine(this, this.height / 2 - this.height
					/ 12);
			drawManager.drawHorizontalLine(this, this.height / 2 + this.height
					/ 12);
		}

		// If paused, draw the overlay and menu on top of the frozen game screen.
		if (this.pauseManager.isPaused()) {
			drawManager.drawPauseOverlay(this);
			drawManager.drawPauseMenu(this, this.pauseManager.getMenuIndex());
		}

		drawManager.completeDrawing(this);
	}

	/**
	 * Cleans bullets that go off screen.
	 */
	private void cleanBullets() {
		Set<Bullet> recyclable = new HashSet<Bullet>();
		for (Bullet bullet : this.bullets) {
			bullet.update();
			if (bullet.getPositionY() < SEPARATION_LINE_HEIGHT
					|| bullet.getPositionY() > this.height)
				recyclable.add(bullet);
		}
		this.bullets.removeAll(recyclable);
		BulletPool.recycle(recyclable);
	}

	/**
	 * Cleans Items that go off screen.
	 */
	private void cleanItems() {
		Set<DropItem> recyclable = new HashSet<DropItem>();
		for (DropItem dropItem : this.dropItems) {
			dropItem.update();
			if (dropItem.getPositionY() < SEPARATION_LINE_HEIGHT
					|| dropItem.getPositionY() > this.height)
				recyclable.add(dropItem);
		}
		this.dropItems.removeAll(recyclable);
		ItemPool.recycle(recyclable);
	}

	/**
	 * Cleans sword waves that go off screen.
	 */
	private void cleanSwordWaves() {
		Set<SwordWave> recyclable = new HashSet<>();
		for (SwordWave wave : this.swordWaves) {
			wave.update();
			if (wave.getPositionY() < SEPARATION_LINE_HEIGHT
					|| wave.getPositionY() > this.height)
				recyclable.add(wave);
		}
		this.swordWaves.removeAll(recyclable);
	}

	/**
	 * Manages collisions between bullets and ships.
	 */
	private void manageBulletShipCollisions() {
		Set<Bullet> recyclable = new HashSet<Bullet>();
		for (Bullet bullet : this.bullets)
			if (bullet.getSpeed() > 0) {
				if (this.livesP1 > 0 && checkCollision(bullet, this.ship) && !this.levelFinished) {
					recyclable.add(bullet);
					if (!this.ship.isInvincible()) {
						if (!this.ship.isDestroyed()) {
							this.ship.destroy();
							this.livesP1--;
							showHealthPopup(GENERIC_HEALTH_POPUP);
							this.logger.info(HIT_ON_PLAYER_PREFIX + this.livesP1
									+ LIVES_REMAINING_SUFFIX);
						}
					}
				} else if (this.shipP2 != null && this.livesP2 > 0 && !this.shipP2.isDestroyed()
						&& checkCollision(bullet, this.shipP2) && !this.levelFinished) {
					recyclable.add(bullet);
					if (!this.shipP2.isInvincible()) {
						if (!this.shipP2.isDestroyed()) {
							this.shipP2.destroy();
							this.livesP2--;
							showHealthPopup(GENERIC_HEALTH_POPUP);
							this.logger.info(HIT_ON_PLAYER_PREFIX + this.livesP2
									+ LIVES_REMAINING_SUFFIX);
						}
					}
				}
			} else {
				for (EnemyShip enemyShip : this.enemyShipFormation)
					if (!enemyShip.isDestroyed()
							&& checkCollision(bullet, enemyShip)) {
						int pts = enemyShip.getPointValue();
						addPointsFor(bullet, pts);
						this.coin += (pts / 10);
						this.shipsDestroyed++;

						String enemyType = enemyShip.getEnemyType();
						this.enemyShipFormation.destroy(enemyShip);
						AchievementManager.getInstance().onEnemyDefeated();
						if (enemyType != null && this.currentLevel.getItemDrops() != null) {
							List<engine.level.ItemDrop> potentialDrops = new ArrayList<>();
							for (engine.level.ItemDrop itemDrop : this.currentLevel.getItemDrops()) {
								if (enemyType.equals(itemDrop.getEnemyType())) {
									potentialDrops.add(itemDrop);
								}
							}

							List<engine.level.ItemDrop> successfulDrops = new ArrayList<>();
							for (engine.level.ItemDrop itemDrop : potentialDrops) {
								if (Math.random() < itemDrop.getDropChance()) {
									successfulDrops.add(itemDrop);
								}
							}

							if (!successfulDrops.isEmpty()) {
								engine.level.ItemDrop selectedDrop = successfulDrops.get((int) (Math.random() * successfulDrops.size()));
								DropItem.ItemType droppedType = DropItem.fromString(selectedDrop.getItemId());
								if (droppedType != null) {
									final int ITEM_DROP_SPEED = 2;

									DropItem newDropItem = ItemPool.getItem(
											enemyShip.getPositionX() + enemyShip.getWidth() / 2,
											enemyShip.getPositionY() + enemyShip.getHeight() / 2,
											ITEM_DROP_SPEED,
											droppedType
									);
									this.dropItems.add(newDropItem);
									this.logger.info("An item (" + droppedType + ") dropped");
								}
							}
						}
						if (!bullet.penetration()) {
							recyclable.add(bullet);
							break;
						}
					}

				// special enemy bullet event
				for (EnemyShip enemyShipSpecial : this.enemyShipSpecialFormation)
					if (enemyShipSpecial != null && !enemyShipSpecial.isDestroyed()
							&& checkCollision(bullet, enemyShipSpecial)) {
						int pts = enemyShipSpecial.getPointValue();
						addPointsFor(bullet, pts);
						this.coin += (pts / 10);
						this.shipsDestroyed++;
						this.enemyShipSpecialFormation.destroy(enemyShipSpecial);
						recyclable.add(bullet);
					}
				if (this.omegaBoss != null
						&& !this.omegaBoss.isDestroyed()
						&& checkCollision(bullet, this.omegaBoss)) {
					this.omegaBoss.takeDamage(2);
					if (this.omegaBoss.getHealPoint() <= 0) {
						this.shipsDestroyed++;
						int pts = this.omegaBoss.getPointValue();
						addPointsFor(bullet, pts);
						this.coin += (pts / 10);
						this.omegaBoss.destroy();
						AchievementManager.getInstance().unlockAchievement("Boss Slayer");
						this.bossExplosionCooldown.reset();
					}
					recyclable.add(bullet);
				}

				/** when final boss collide with bullet */
				if (this.finalBoss != null && !this.finalBoss.isDestroyed() && checkCollision(bullet, this.finalBoss)) {
					this.finalBoss.takeDamage(1);
					if (this.finalBoss.getHealPoint() <= 0) {
						int pts = this.finalBoss.getPointValue();
						addPointsFor(bullet, pts);
						this.coin += (pts / 10);
						this.finalBoss.destroy();
						AchievementManager.getInstance().unlockAchievement("Boss Slayer");
						recyclable.add(bullet);
					}
				}
				/** Samurai boss is bullet immunity.*/
				if (this.samuraiBoss != null && !this.samuraiBoss.isDestroyed()
						&& checkCollision(bullet, this.samuraiBoss)) {
					recyclable.add(bullet);
				}

				if (this.finalBoss3 != null && !this.finalBoss3.isDestroyed() && checkCollision(bullet, this.finalBoss3)) {
					this.finalBoss3.takeDamage(1);
					recyclable.add(bullet);
				}

				if (this.boss4 != null && !this.boss4.isDestroyed() && checkCollision(bullet, this.boss4)) {
					this.boss4.takeDamage(1);
					recyclable.add(bullet);
				}
			}
		this.bullets.removeAll(recyclable);
		BulletPool.recycle(recyclable);
	}

	/**
	 * Manages collisions between player ship and enemy ships.
	 * Player loses a life immediately upon collision with any enemy.
	 */
	private void manageShipEnemyCollisions() {
		// ===== P1 collision check =====
		if (!this.levelFinished && this.livesP1 > 0 && !this.ship.isDestroyed()
				&& !this.ship.isInvincible()) {
			// Check collision with normal enemy ships
			for (EnemyShip enemyShip : this.enemyShipFormation) {
				if (!enemyShip.isDestroyed() && checkCollision(this.ship, enemyShip)) {
					this.enemyShipFormation.destroy(enemyShip);
					this.ship.destroy();
					this.livesP1--;
					showHealthPopup("-1 Life (Collision!)");
					this.logger.info("Ship collided with enemy! " + this.livesP1
							+ LIVES_REMAINING_SUFFIX);
					return;
				}
			}

			// Check collision with special enemy formation (red/blue ships)
			for (EnemyShip enemyShipSpecial : this.enemyShipSpecialFormation) {
				if (enemyShipSpecial != null && !enemyShipSpecial.isDestroyed()
						&& checkCollision(this.ship, enemyShipSpecial)) {
					enemyShipSpecial.destroy();
					this.ship.destroy();
					this.livesP1--;
					showHealthPopup("-1 Life (Collision!)");
					this.logger.info("Ship collided with special enemy formation! "
							+ this.livesP1 + LIVES_REMAINING_SUFFIX);
					return;
				}

				if (this.samuraiBoss != null && !this.samuraiBoss.isDestroyed()) {
					handleSekiroCollision(this.ship, this.samuraiBoss);
				}
			}

			// Check collision with omega boss (mid boss - yellow/pink ship)
			if (this.omegaBoss != null && !this.omegaBoss.isDestroyed()
					&& checkCollision(this.ship, this.omegaBoss)) {
				this.ship.destroy();
				this.livesP1--;
				showHealthPopup(BOSS_COLLISION_HEALTH_POPUP);
				this.logger.info("Ship collided with omega boss! " + this.livesP1
						+ LIVES_REMAINING_SUFFIX);
				return;
			}

			// Check collision with final boss
			if (this.finalBoss != null && !this.finalBoss.isDestroyed()
					&& checkCollision(this.ship, this.finalBoss)) {
				this.ship.destroy();
				this.livesP1--;
				showHealthPopup(BOSS_COLLISION_HEALTH_POPUP);
				this.logger.info("Ship collided with final boss! " + this.livesP1
						+ LIVES_REMAINING_SUFFIX);
				return;
			}

			// Check collision with boss4
			if (this.boss4 != null && !this.boss4.isDestroyed()
					&& checkCollision(this.ship, this.boss4)) {
				this.ship.destroy();
				this.livesP1--;
				showHealthPopup(BOSS_COLLISION_HEALTH_POPUP);
				this.logger.info("Ship collided with boss4! " + this.livesP1
						+ LIVES_REMAINING_SUFFIX);
				return;
			}

			if (this.samuraiBoss != null && !this.samuraiBoss.isDestroyed()) {
				handleSekiroCollision(this.ship, this.samuraiBoss);
			}
		}

		// ===== P2 collision check =====
		if (!this.levelFinished && this.shipP2 != null && this.livesP2 > 0
				&& !this.shipP2.isDestroyed() && !this.shipP2.isInvincible()) {
			// Check collision with normal enemy ships
			for (EnemyShip enemyShip : this.enemyShipFormation) {
				if (!enemyShip.isDestroyed() && checkCollision(this.shipP2, enemyShip)) {
					this.enemyShipFormation.destroy(enemyShip);
					this.shipP2.destroy();
					this.livesP2--;
					showHealthPopup("-1 Life (Collision!)");
					this.logger.info("Ship P2 collided with enemy! " + this.livesP2
							+ LIVES_REMAINING_SUFFIX);
					return;
				}
			}

			// Check collision with special enemy formation
			for (EnemyShip enemyShipSpecial : this.enemyShipSpecialFormation) {
				if (enemyShipSpecial != null && !enemyShipSpecial.isDestroyed()
						&& checkCollision(this.shipP2, enemyShipSpecial)) {
					enemyShipSpecial.destroy();
					this.shipP2.destroy();
					this.livesP2--;
					showHealthPopup("-1 Life (Collision!)");
					this.logger.info("Ship P2 collided with special enemy formation! "
							+ this.livesP2 + LIVES_REMAINING_SUFFIX);
					return;
				}
			}

			// Check collision with omega boss
			if (this.omegaBoss != null && !this.omegaBoss.isDestroyed()
					&& checkCollision(this.shipP2, this.omegaBoss)) {
				this.shipP2.destroy();
				this.livesP2--;
				showHealthPopup(BOSS_COLLISION_HEALTH_POPUP);
				this.logger.info("Ship P2 collided with omega boss! " + this.livesP2
						+ LIVES_REMAINING_SUFFIX);
				return;
			}

			// Check collision with final boss
			if (this.finalBoss != null && !this.finalBoss.isDestroyed()
					&& checkCollision(this.shipP2, this.finalBoss)) {
				this.shipP2.destroy();
				this.livesP2--;
				showHealthPopup(BOSS_COLLISION_HEALTH_POPUP);
				this.logger.info("Ship P2 collided with final boss! " + this.livesP2
						+ LIVES_REMAINING_SUFFIX);
				return;
			}

			// Check collision with boss4
			if (this.boss4 != null && !this.boss4.isDestroyed()
					&& checkCollision(this.shipP2, this.boss4)) {
				this.shipP2.destroy();
				this.livesP2--;
				showHealthPopup(BOSS_COLLISION_HEALTH_POPUP);
				this.logger.info("Ship P2 collided with boss4! " + this.livesP2
						+ LIVES_REMAINING_SUFFIX);
				return;
			}

			// Check collision with Samurai boss
			if (this.samuraiBoss != null && !this.samuraiBoss.isDestroyed()) {
				handleSekiroCollision(this.shipP2, this.samuraiBoss);
			}
		}
	}

	/**
	 * Check collision with swordwave.
	 */
	private void manageWaveShipCollisions() {
		Set<SwordWave> recyclable = new HashSet<>();
		for (SwordWave wave : this.swordWaves) {
			if (this.livesP1 > 0 && !this.ship.isDestroyed() && !this.ship.isInvincible()
					&& checkCollision(wave, this.ship)) {
				recyclable.add(wave);
				this.ship.destroy();
				this.livesP1--;
				showHealthPopup("-1 Life (Wave Hit!)");
				this.logger.info("Ship 1 hit by Sword Wave! " + this.livesP1 + LIVES_REMAINING_SUFFIX);
			}
			else if (this.shipP2 != null && this.livesP2 > 0 && !this.shipP2.isDestroyed()
					&& !this.shipP2.isInvincible() && checkCollision(wave, this.shipP2)) {

				recyclable.add(wave);
				this.shipP2.destroy();
				this.livesP2--;
				showHealthPopup("-1 Life (Wave Hit!)");
				this.logger.info("Ship 2 hit by Sword Wave! " + this.livesP2 + LIVES_REMAINING_SUFFIX);
			}
		}
		this.swordWaves.removeAll(recyclable);
	}

	/**
	 * Manages collisions between player ship and dropped items.
	 * Applies item effects when player collects them.
	 */
	private void manageItemCollisions() {
		Set<DropItem> acquiredDropItems = new HashSet<DropItem>();

		if (!this.levelFinished && ((this.livesP1 > 0 && !this.ship.isDestroyed())
				|| (this.shipP2 != null && this.livesP2 > 0 && !this.shipP2.isDestroyed()))) {
			for (DropItem dropItem : this.dropItems) {

				if (this.livesP1 > 0 && !this.ship.isDestroyed() && checkCollision(this.ship, dropItem)) {
					this.logger.info("Player acquired dropItem: " + dropItem.getItemType());

					// Add item to HUD display
					ItemHUDManager.getInstance().addDroppedItem(dropItem.getItemType());

					switch (dropItem.getItemType()) {
						case Heal:
							gainLife();
							break;
						case Shield:
							ship.activateInvincibility(5000); // 5 seconds of invincibility
							break;
						case Stop:
							DropItem.applyTimeFreezeItem(3000);
							break;
						case Push:
							DropItem.PushbackItem(this.enemyShipFormation, 20);
							break;
						case Explode:
							int destroyedEnemy = this.enemyShipFormation.destroyAll();
							int pts = destroyedEnemy * 5;
							addPointsFor(null, pts);
							break;
						case Slow:
							enemyShipFormation.activateSlowdown();
							this.logger.info("Enemy formation slowed down!");
							break;
						default:
							// For other dropItem types. Free to add!
							break;
					}
					acquiredDropItems.add(dropItem);
				} else if (this.shipP2 != null && this.livesP2 > 0 && !this.shipP2.isDestroyed()
						&& checkCollision(this.shipP2, dropItem)) {
					this.logger.info("Player acquired dropItem: " + dropItem.getItemType());

					// Add item to HUD display
					ItemHUDManager.getInstance().addDroppedItem(dropItem.getItemType());

					switch (dropItem.getItemType()) {
						case Heal:
							gainLifeP2();
							break;
						case Shield:
							shipP2.activateInvincibility(5000); // 5 seconds of invincibility
							break;
						case Stop:
							DropItem.applyTimeFreezeItem(3000);
							break;
						case Push:
							DropItem.PushbackItem(this.enemyShipFormation, 20);
							break;
						case Explode:
							int destroyedEnemy = this.enemyShipFormation.destroyAll();
							int pts = destroyedEnemy * 5;
							addPointsFor(null, pts);
							break;
						case Slow:
							enemyShipFormation.activateSlowdown();
							this.logger.info("Enemy formation slowed down!");
							break;
						default:
							// For other dropItem types. Free to add!
							break;
					}
					acquiredDropItems.add(dropItem);
				}
			}
			this.dropItems.removeAll(acquiredDropItems);
			ItemPool.recycle(acquiredDropItems);
		}
	}

	/**
	 * Checks if two entities are colliding.
	 *
	 * @param a First entity, the bullet.
	 * @param b Second entity, the ship.
	 * @return Result of the collision test.
	 */
	private boolean checkCollision(final Entity a, final Entity b) {
		Entity hitboxA = a;
		Entity hitboxB = b;

		if (this.smallHitboxMode) {
			if (a == this.ship || a == this.shipP2) {
				hitboxA = new Entity(a.getPositionX() + a.getWidth() / 2 - 2, a.getPositionY() + a.getHeight() / 2 - 2, 5, 5, a.getColor());
			}
			if (b == this.ship || b == this.shipP2) {
				hitboxB = new Entity(b.getPositionX() + b.getWidth() / 2 - 2, b.getPositionY() + b.getHeight() / 2 - 2, 5, 5, b.getColor());
			}
		}

		// Calculate center point of the entities in both axis.
		int centerAX = hitboxA.getPositionX() + hitboxA.getWidth() / 2;
		int centerAY = hitboxA.getPositionY() + hitboxA.getHeight() / 2;
		int centerBX = hitboxB.getPositionX() + hitboxB.getWidth() / 2;
		int centerBY = hitboxB.getPositionY() + hitboxB.getHeight() / 2;
		// Calculate maximum distance without collision.
		int maxDistanceX = hitboxA.getWidth() / 2 + hitboxB.getWidth() / 2;
		int maxDistanceY = hitboxA.getHeight() / 2 + hitboxB.getHeight() / 2;
		// Calculates distance.
		int distanceX = Math.abs(centerAX - centerBX);
		int distanceY = Math.abs(centerAY - centerBY);

		return distanceX < maxDistanceX && distanceY < maxDistanceY;
	}

	/**
	 * Shows an achievement popup message on the HUD.
	 *
	 * @param message Text to display in the popup.
	 */
	public void showAchievement(String message) {
		this.achievementText = message;
		this.achievementPopupCooldown = Core.getCooldown(2500); // Show for 2.5 seconds
		this.achievementPopupCooldown.reset();
	}

	/**
	 * Displays a notification popup when the player gains or loses health
	 *
	 * @param message Text to display in the popup
	 */
	public void showHealthPopup(String message) {
		this.healthPopupText = message;
		this.healthPopupCooldown = Core.getCooldown(500);
		this.healthPopupCooldown.reset();
	}

	/**
	 * Returns a GameState object representing the status of the game.
	 *
	 * @return Current game state.
	 */
	public final GameState getGameState() {
		if (this.coin > 2000) {
			AchievementManager.getInstance().unlockAchievement("Mr. Greedy");
		}
		return new GameState(this.level, this.score, this.livesP1, this.livesP2,
				this.bulletsShot, this.shipsDestroyed, this.coin);
	}

	/**
	 * Adds one life to the player.
	 */
	public final void gainLife() {
		if (this.livesP1 < this.maxLives) {
			this.livesP1++;
		}
	}

	public final void gainLifeP2() {
		if (this.livesP2 < this.maxLives) {
			this.livesP2++;
		}
	}

	/**
	 * Spawns the configured boss for the level.
	 */
	private void bossReveal() {
		String bossName = this.currentlevel.getBossId();
		if (bossName == null || bossName.isEmpty()) {
			this.logger.info("No boss for this level. Proceeding to finish.");
			this.levelFinished = true;
			this.screenFinishedCooldown.reset();
			return;
		}

		this.logger.info("Spawning boss: " + bossName);
		switch (bossName) {
			case "finalBoss":
				this.finalBoss = new FinalBoss(this.width / 2 - 50, 50, this.width, this.height);
				this.logger.info("Final Boss has spawned!");
				break;
			case "omegaBoss":
			case "omegaAndFinal":
				this.omegaBoss = new OmegaBoss(Color.ORANGE, ITEMS_SEPARATION_LINE_HEIGHT);
				omegaBoss.attach(this);
				this.logger.info("Omega Boss has spawned!");
				break;
			case "SamuraiBoss":
				this.samuraiBoss = new SamuraiBoss(this.width / 2, 100, this.width, this.ship, this.shipP2, ITEMS_SEPARATION_LINE_HEIGHT);
				this.samuraiBoss.attach(this);
				this.ship.setMeleeMode(true);
				if (this.shipP2 != null) {
					this.shipP2.setMeleeMode(true);
				}
				this.logger.info("Samurai Boss has spawned!");
				break;
			case "laserBoss":
				this.finalBoss3 = new FinalBoss_3(this.width / 2 - 50, 50, this.width, this.height, this.laserBeamManager);
				this.logger.info("Final Boss (laser) has spawned!");
				break;
			case "boss4":
				this.boss4 = new Boss4(this.width / 2 - 50, 50, this.width, this.height);
				this.smallHitboxMode = true;
				this.logger.info("Boss4 has spawned!");
				break;
			default:
				this.logger.warning("Unknown bossId: " + bossName);
				break;
		}
	}

	public void finalbossManage() {
		if (this.finalBoss != null && !this.finalBoss.isDestroyed()) {
			this.finalBoss.update();
			/** called the boss shoot logic */
			if (this.finalBoss.getHealPoint() > this.finalBoss.getMaxHp() / 4) {
				bossBullets.addAll(this.finalBoss.shoot1());
				bossBullets.addAll(this.finalBoss.shoot2());
			} else {
				/** Is the bullet on the screen erased */
				if (!is_cleared) {
					bossBullets.clear();
					is_cleared = true;
					logger.info("boss is angry");
				} else {
					bossBullets.addAll(this.finalBoss.shoot3());
				}
			}

			/** bullets to erase */
			Set<BossBullet> bulletsToRemove = new HashSet<>();

			for (BossBullet b : bossBullets) {
				b.update();
				/** If the bullet goes off the screen */
				if (b.isOffScreen(width, height)) {
					/** bulletsToRemove carry bullet */
					bulletsToRemove.add(b);
				}
				/** If the bullet collides with ship */
				else if (this.livesP1 > 0 && this.checkCollision(b, this.ship)) {
					if (!this.ship.isDestroyed()) {
						this.ship.destroy();
						this.livesP1--;
						this.logger.info(HIT_ON_PLAYER_PREFIX + this.livesP1 + LIVES_REMAINING_SUFFIX);
					}
					bulletsToRemove.add(b);
				} else if (this.shipP2 != null && this.livesP2 > 0 && !this.shipP2.isDestroyed() && this.checkCollision(b, this.shipP2)) {
					if (!this.shipP2.isDestroyed()) {
						this.shipP2.destroy();
						this.livesP2--;
						this.logger.info(HIT_ON_PLAYER_PREFIX + this.livesP2 + LIVES_REMAINING_SUFFIX);
					}
					bulletsToRemove.add(b);
				}
			}
			/** all bullets are removed */
			bossBullets.removeAll(bulletsToRemove);

		}
		if (this.finalBoss != null && this.finalBoss.isDestroyed()) {
			this.levelFinished = true;
			this.screenFinishedCooldown.reset();
		}
	}

	/**
	 * Manages the SekiroBoss's state and updates.
	 */
	private void sekiroBossManage() {
		if (this.samuraiBoss == null) return;

		if (this.samuraiBoss.isDestroyed()) {
			if (!this.levelFinished) {
				this.ship.setMeleeMode(false);
				if (this.shipP2 != null) this.shipP2.setMeleeMode(false);

				this.levelFinished = true;
				this.screenFinishedCooldown.reset();
			}
			return;
		}

		this.samuraiBoss.update();
		this.swordWaves.addAll(this.samuraiBoss.shootWave());
	}

	/**
	 * Check collision with Samurai Boss.
	 */
	private void handleSekiroCollision(Ship playerShip, SamuraiBoss boss) {
		if (checkCollision(playerShip, boss)) {

			/**
			 * 1. DeathBlow
			 * When the player is attacking (isParrying) and the boss is in Broken state.
			 */
			if (playerShip.isParrying() && boss.isPostureBroken() && !boss.isInvincibleAfterBroken()) {
				boss.executeDeathblow();
				SoundManager.play("sfx/samurai-kill.wav");
				playerShip.activateInvincibility(1500);
			}
			/**
			 * 2. Parry
			 * When the player is attacking and the boss is attacking.
			 */
			else if (playerShip.isParrying() && boss.isAttacking()) {
				boss.onParried();
				SoundManager.play("sfx/parry.wav");

				// Position spark effects.
				int centerX = playerShip.getPositionX() + (playerShip.getWidth() / 2);
				int boundaryY = playerShip.getPositionY();
				int sparkWidth = parrySparkEffect.getWidth();
				int sparkHeight = parrySparkEffect.getHeight();
				int gap = 10;
				int spark1X = centerX - sparkWidth - (gap / 2);
				int spark2X = centerX + (gap / 2);
				int sparkY = boundaryY - (sparkHeight / 2);

				this.parrySparkEffect.setPositionX(spark1X);
				this.parrySparkEffect.setPositionY(sparkY);
				this.parrySparkEffect2.setPositionX(spark2X);
				this.parrySparkEffect2.setPositionY(sparkY);
				this.parrySparkCooldown.reset();
			}

			/**
			 * 3. Melee attack
			 * When the player is attacking (isParrying), and the boss is not attacking.
			 */
			else if (playerShip.isParrying() && !boss.isAttacking()) {
				if(!boss.isPostureBroken()){
					boss.takeDamage(0);
					boss.takePostureDamage(0);
				}
				// SoundManager.play("sfx/sword_hit.wav");
			}
			/**
			 * Player Damage
			 * Not the above three cases, but when the boss is attacking and the player is not invincible.
			 */
			else if (boss.isAttacking() && !playerShip.isInvincible() && !playerShip.isDestroyed()) {
				playerShip.destroy();
				SoundManager.play("sfx/samurai-kill.wav");
				if (playerShip.getPlayerId() == 1) this.livesP1--;
				else if (this.shipP2 != null) this.livesP2--;
				showHealthPopup("-1 Life (Boss Hit!)");
				logger.info("Ship " + playerShip.getPlayerId() + " hit by boss!");
			}
		}
	}

	/**
	 * Returns Player 1's current lives.
	 *
	 * @return livesP1
	 */
	public final int getLivesP1() {
		return this.livesP1;
	}

	/**
	 * Returns Player 2's current lives.
	 *
	 * @return livesP2
	 */
	public final int getLivesP2() {
		return this.livesP2;
	}

	/**
	 * Manages boss4 behavior (Touhou-like spell card behavior)
	 */
	public void boss4Manage() {
		if (this.boss4 != null && !this.boss4.isDestroyed()) {
			this.boss4.update();

			// Check for spell card survival bonus
			if (this.boss4.hasSurvivedSpellCard()) {
				this.score += 5000; // Survival bonus
				this.scoreP1 += 5000; // Award to P1 by default
				this.logger.info("Awarded 5000 points for surviving the spell card.");
				this.boss4.resetSurvivalFlag();
			}

			/** called the boss shoot logic */
			if (this.boss4.isSpellCardActive()) {
				bossBullets.addAll(this.boss4.shootSpellCard(this.ship));
			} else {
				bossBullets.addAll(this.boss4.shootNormal());
			}

			/** bullets to erase */
			Set<BossBullet> bulletsToRemove = new HashSet<>();

			for (BossBullet b : bossBullets) {
				b.update();
				/** If the bullet goes off the screen */
				if (b.isOffScreen(width, height)) {
					/** bulletsToRemove carry bullet */
					bulletsToRemove.add(b);
				}
				/** If the bullet collides with ship */
				else if (this.livesP1 > 0 && this.checkCollision(b, this.ship)) {
					if (!this.ship.isDestroyed()) {
						this.ship.destroy();
						this.livesP1--;
						this.logger.info(HIT_ON_PLAYER_PREFIX + this.livesP1 + LIVES_REMAINING_SUFFIX);
					}
					bulletsToRemove.add(b);
				}
				else if (this.shipP2 != null && this.livesP2 > 0 && !this.shipP2.isDestroyed() && this.checkCollision(b, this.shipP2)) {
					if (!this.shipP2.isDestroyed()) {
						this.shipP2.destroy();
						this.livesP2--;
						this.logger.info(HIT_ON_PLAYER_PREFIX + this.livesP2 + LIVES_REMAINING_SUFFIX);
					}
					bulletsToRemove.add(b);
				}
			}
			/** all bullets are removed */
			bossBullets.removeAll(bulletsToRemove);

		}
		if (this.boss4 != null && this.boss4.isDestroyed()) {
			this.levelFinished = true;
			this.screenFinishedCooldown.reset();
			this.smallHitboxMode = false;
		}
	}

	/**
	 * finalBoss3 (laser boss) management
	 */
	public void finalBoss3Manage() {
		if (this.finalBoss3 != null && !this.finalBoss3.isDestroyed()) {
			this.finalBoss3.update();
			bossBullets.addAll(this.finalBoss3.shoot());

			Set<BossBullet> bulletsToRemove = new HashSet<>();

			for (BossBullet b : bossBullets) {
				b.update();

				if (b.isOffScreen(width, height)) {
					bulletsToRemove.add(b);
				} else if (this.livesP1 > 0 && this.ship != null && !this.ship.isDestroyed()
						&& this.checkCollision(b, this.ship)) {
					// P1 충돌
					if (!this.ship.isInvincible()) {
						this.ship.destroy();
						this.livesP1--;
						showHealthPopup(GENERIC_HEALTH_POPUP);
						this.logger.info(HIT_ON_PLAYER_PREFIX + this.livesP1 + LIVES_REMAINING_SUFFIX);
					}
					bulletsToRemove.add(b);
				} else if (this.shipP2 != null && this.livesP2 > 0 && !this.shipP2.isDestroyed()
						&& this.checkCollision(b, this.shipP2)) {
					// P2 충돌
					if (!this.shipP2.isInvincible()) {
						this.shipP2.destroy();
						this.livesP2--;
						showHealthPopup(GENERIC_HEALTH_POPUP);
						this.logger.info("Hit on player ship (P2), " + this.livesP2 + LIVES_REMAINING_SUFFIX);
					}
					bulletsToRemove.add(b);
				}
			}

			bossBullets.removeAll(bulletsToRemove);
		}

		if (this.finalBoss3 != null && this.finalBoss3.isDestroyed()) {
			this.levelFinished = true;
			this.screenFinishedCooldown.reset();
		}
	}
}
