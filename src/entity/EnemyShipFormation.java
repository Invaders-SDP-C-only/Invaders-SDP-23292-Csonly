package entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.awt.Color;
import java.util.Random;

import screen.Screen;
import screen.GameScreen;
import engine.Cooldown;
import engine.Core;
import engine.DrawManager;
import engine.DrawManager.SpriteType;
import engine.GameSettings;
import engine.level.Level;

/**
 * Groups enemy ships into a formation that moves together.
 * This class handles the movement, shooting, and destruction of enemy ships
 * as a collective unit. It supports various formation patterns (Rectangle, Circle, etc.)
 * and integrates with the game's level system.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 */
public class EnemyShipFormation implements Iterable<EnemyShip> {

	/** Initial position in the x-axis. */
	private static final int INIT_POS_X = 20;
	/** Initial position in the y-axis. */
	private static final int INIT_POS_Y = 100;
	/** Distance between ships. */
	private static final int SEPARATION_DISTANCE = 40;
	/** Proportion of C-type ships. */
	private static final double PROPORTION_C = 0.2;
	/** Proportion of B-type ships. */
	private static final double PROPORTION_B = 0.4;
	/** Downwards speed of the formation. */
	private static final int Y_SPEED = 4;
	/** Speed of the bullets shot by the members. */
	private static final int BULLET_SPEED = 4;
	/** Proportion of differences between shooting times. */
	private static final double SHOOTING_VARIANCE = .2;
	/** Margin on the sides of the screen. */
	private static final int SIDE_MARGIN = 20;
	/** Margin on the bottom of the screen. */
	private static final int BOTTOM_MARGIN = 80;
	/** Minimum speed allowed. */
	private static final int MINIMUM_SPEED = 10;

	/** DrawManager instance. */
	private DrawManager drawManager;
	/** Application logger. */
	private Logger logger;
	/** Screen to draw ships on. */
	private Screen screen;

	/** List of enemy ships forming the formation. */
	private List<List<EnemyShip>> enemyShips;
	/** Minimum time between shots. */
	private Cooldown shootingCooldown;
	/** Number of ships in the formation - horizontally. */
	private int nShipsWide;
	/** Number of ships in the formation - vertically. */
	private int nShipsHigh;
	/** Time between shots. */
	private int shootingInterval;
	/** Variance in the time between shots. */
	private int shootingVariance;
	/** Initial ship speed. */
	private int baseSpeed;
	/** Speed of the ships. */
	private int movementSpeed;
	/** Current direction the formation is moving on. */
	private Direction currentDirection;
	/** Interval between movements, in frames. */
	private int movementInterval;
	/** Total width of the formation. */
	private int width;
	/** Total height of the formation. */
	private int height;
	/** Position in the x-axis of the upper left corner of the formation. */
	private int positionX;
	/** Position in the y-axis of the upper left corner of the formation. */
	private int positionY;
	/** Width of one ship. */
	private int shipWidth;
	/** Height of one ship. */
	private int shipHeight;
	/** List of ships that are able to shoot. */
	private List<EnemyShip> shooters;
	/** Number of not destroyed ships. */
	private int shipCount;

	// === Slowdown Effect Variables ===
	/** Number of movement cycles the slowdown effect has been active. */
	private int slowDownCount;
	/** Flag to check if slowdown is active. */
	private boolean isSlowedDown;
	/** Original horizontal speed value. */
	private static final int ORIGINAL_X_SPEED = 8;
	/** Slowed down horizontal speed value. */
	private static final int SLOWED_X_SPEED = 4;
	/** Duration of slowdown effect (in movement cycles). */
	private static final int SLOWDOWN_DURATION = 18;

	/** Directions the formation can move. */
	private enum Direction {
		/** Down and Right. */
		DOWN_RIGHT,
		/** Down and Left. */
		DOWN_LEFT,
		/** Up and Right. */
		UP_RIGHT,
		/** Up and Left. */
		UP_LEFT
	};

	/** * Formation patterns available for enemy ships.
	 * Used especially in Sandbox mode for variety.
	 */
	public enum FormationPattern {
		RECTANGLE,  // Standard Rectangle
		CIRCLE,     // Circular/Donut Shape
		TRIANGLE,   // Inverted Triangle
		V_SHAPE,    // V-Formation
		RANDOM      // Scattered Randomly
	}
	/** Current formation pattern. */
	private FormationPattern currentPattern;

	/**
	 * Constructor, sets the initial conditions.
	 * * @param gameSettings Current game settings.
	 */
	public EnemyShipFormation(final GameSettings gameSettings) {
		this(gameSettings, FormationPattern.RECTANGLE);
	}

	/**
	 * Constructor with pattern selection (for Classic Mode mostly).
	 * * @param gameSettings Current game settings.
	 * @param pattern Desired formation pattern.
	 */
	public EnemyShipFormation(final GameSettings gameSettings, FormationPattern pattern) {
		this.drawManager = Core.getDrawManager();
		this.logger = Core.getLogger();
		this.enemyShips = new ArrayList<List<EnemyShip>>();
		this.currentDirection = Direction.DOWN_RIGHT;
		this.movementInterval = 0;
		this.nShipsWide = gameSettings.getFormationWidth();
		this.nShipsHigh = gameSettings.getFormationHeight();
		this.shootingInterval = gameSettings.getShootingFrecuency();
		this.shootingVariance = (int) (gameSettings.getShootingFrecuency() * SHOOTING_VARIANCE);
		this.baseSpeed = gameSettings.getBaseSpeed();
		this.movementSpeed = this.baseSpeed;
		this.positionX = INIT_POS_X;
		this.positionY = INIT_POS_Y;
		this.shooters = new ArrayList<EnemyShip>();
		this.currentPattern = pattern;

		createStandardFormation(this.nShipsWide, this.nShipsHigh);
		finalizeFormation();
	}

	/**
	 * Constructor that uses Level directly (Sandbox Mode).
	 * Defaults to Rectangle pattern.
	 * * @param level Current level data.
	 */
	public EnemyShipFormation(final Level level) {
		this(level, FormationPattern.RECTANGLE);
	}

	/**
	 * Constructor that uses Level AND Pattern (For Sandbox).
	 * Allows creating diverse enemy formations based on level configuration.
	 * * @param level Current level data.
	 * @param pattern Desired formation pattern.
	 */
	public EnemyShipFormation(final Level level, FormationPattern pattern) {
		this.drawManager = Core.getDrawManager();
		this.logger = Core.getLogger();
		this.enemyShips = new ArrayList<List<EnemyShip>>();
		this.currentDirection = Direction.DOWN_RIGHT;
		this.movementInterval = 0;

		// Load Level Values
		this.nShipsWide = level.getFormationWidth();
		this.nShipsHigh = level.getFormationHeight();
		this.shootingInterval = level.getShootingFrecuency();
		this.shootingVariance = (int) (level.getShootingFrecuency() * SHOOTING_VARIANCE);
		this.baseSpeed = level.getBaseSpeed();
		this.movementSpeed = this.baseSpeed;
		this.positionX = INIT_POS_X;
		this.positionY = INIT_POS_Y;
		this.shooters = new ArrayList<EnemyShip>();
		this.currentPattern = pattern;

		if (this.currentPattern == null) this.currentPattern = FormationPattern.RECTANGLE;

		this.logger.info("Initializing formation with pattern: " + this.currentPattern);

		// Generation logic branching according to pattern
		switch (this.currentPattern) {
			case CIRCLE:
				createCircleFormation(level);
				break;
			case TRIANGLE:
				createTriangleFormation(level);
				break;
			case V_SHAPE:
				createVShapeFormation(level);
				break;
			case RANDOM:
				createRandomFormation(level);
				break;
			case RECTANGLE:
			default:
				// Original square logic
				createRectangularFormationFromLevel(level);
				break;
		}

		finalizeFormation();
	}

	/**
	 * Common post-processing logic: Calculates width/height and sets initial shooters.
	 * Should be called after filling the enemyShips list.
	 */
	private void finalizeFormation() {
		if (this.enemyShips.isEmpty() || this.enemyShips.get(0).isEmpty()) return;

		this.shipWidth = this.enemyShips.get(0).get(0).getWidth();
		this.shipHeight = this.enemyShips.get(0).get(0).getHeight();

		// Fit width, height, positionX, positionY exactly
		cleanUp();

		// Shooter Initialization (Bottom-most ship in each column)
		this.shooters.clear();
		for (List<EnemyShip> column : this.enemyShips) {
			if (!column.isEmpty()) {
				this.shooters.add(column.get(column.size() - 1));
			}
		}
	}

	/**
	 * Creates a standard rectangular formation based on dimensions.
	 * Used for Classic Mode.
	 * * @param width Number of columns.
	 * @param height Number of rows.
	 */
	private void createStandardFormation(int width, int height) {
		for (int i = 0; i < width; i++) {
			this.enemyShips.add(new ArrayList<EnemyShip>());
			for (int j = 0; j < height; j++) {
				SpriteType spriteType = calculateSpriteType(j);
				int x = 100 + (i * 48);
				int y = 100 + (j * 32);
				EnemyShip ship = new EnemyShip(x, y, spriteType);
				this.enemyShips.get(i).add(ship);
				this.shipCount++;
			}
		}
	}

	/**
	 * Creates a rectangular formation using Level data.
	 * Ensures correct enemy types are used.
	 * * @param level Level data.
	 */
	private void createRectangularFormationFromLevel(Level level) {
		int width = level.getFormationWidth();
		int height = level.getFormationHeight();

		List<SpriteType> spriteQueue = buildLayeredQueueFromLevel(level, width, height);
		int qIndex = 0;

		for (int i = 0; i < width; i++) {
			this.enemyShips.add(new ArrayList<EnemyShip>());
			for (int j = 0; j < height; j++) {
				SpriteType spriteType;
				if (qIndex < spriteQueue.size()) spriteType = spriteQueue.get(qIndex++);
				else spriteType = SpriteType.EnemyShipA1;

				int x = (SEPARATION_DISTANCE * i) + positionX;
				int y = (SEPARATION_DISTANCE * j) + positionY;

				EnemyShip ship = new EnemyShip(x, y, spriteType);
				this.enemyShips.get(i).add(ship);
				this.shipCount++;
			}
		}
	}

	/**
	 * Creates a circular (donut) formation.
	 * * @param level Level data.
	 */
	private void createCircleFormation(Level level) {
		int totalShips = 16;
		int centerX = 224;
		int centerY = 180;
		int radius = 100;

		List<SpriteType> spriteQueue = buildLayeredQueueFromLevel(level, totalShips, 1);
		int qIndex = 0;

		for (int i = 0; i < totalShips; i++) {
			// In circular formation, each ship gets its own column to act as a shooter.
			this.enemyShips.add(new ArrayList<EnemyShip>());

			double angle = (2 * Math.PI / totalShips) * i;
			int x = (int) (centerX + radius * Math.cos(angle));
			int y = (int) (centerY + radius * Math.sin(angle));

			SpriteType type = (qIndex < spriteQueue.size()) ? spriteQueue.get(qIndex++) : SpriteType.EnemyShipA1;

			EnemyShip ship = new EnemyShip(x, y, type);
			this.enemyShips.get(i).add(ship);
			this.shipCount++;
		}
	}

	/**
	 * Creates an inverted triangle formation.
	 * * @param level Level data.
	 */
	private void createTriangleFormation(Level level) {
		int rows = 5;
		int startY = 80;
		int centerX = 224;

		// Estimate total ships
		int totalShipsEstimate = 0;
		for(int i=0; i<rows; i++) totalShipsEstimate += ((rows - i) * 2 - 1);
		List<SpriteType> spriteQueue = buildLayeredQueueFromLevel(level, totalShipsEstimate, 1);
		int qIndex = 0;

		for (int i = 0; i < rows; i++) {
			int shipsInRow = (rows - i) * 2 - 1;
			int startX = centerX - (shipsInRow * 24) + 24; // Center alignment correction

			for (int j = 0; j < shipsInRow; j++) {
				this.enemyShips.add(new ArrayList<EnemyShip>()); // New column
				int colIdx = this.enemyShips.size() - 1;

				int x = startX + (j * 48);
				int y = startY + (i * 40);

				SpriteType type = (qIndex < spriteQueue.size()) ? spriteQueue.get(qIndex++) : SpriteType.EnemyShipA1;

				EnemyShip ship = new EnemyShip(x, y, type);
				this.enemyShips.get(colIdx).add(ship);
				this.shipCount++;
			}
		}
	}

	/**
	 * Creates a V-shape formation.
	 * * @param level Level data.
	 */
	private void createVShapeFormation(Level level) {
		int wings = 7;
		int centerX = 224;
		int startY = 80;

		List<SpriteType> spriteQueue = buildLayeredQueueFromLevel(level, wings * 2 + 1, 1);
		int qIndex = 0;

		// Center Vertex
		this.enemyShips.add(new ArrayList<EnemyShip>());
		SpriteType centerType = (qIndex < spriteQueue.size()) ? spriteQueue.get(qIndex++) : SpriteType.EnemyShipC1;
		this.enemyShips.get(0).add(new EnemyShip(centerX, startY + 140, centerType));
		this.shipCount++;

		for (int i = 1; i <= wings; i++) {
			int offsetX = i * 30;
			int offsetY = i * 20;

			// Left Wing
			this.enemyShips.add(new ArrayList<EnemyShip>());
			SpriteType typeL = (qIndex < spriteQueue.size()) ? spriteQueue.get(qIndex++) : SpriteType.EnemyShipB1;
			this.enemyShips.get(this.enemyShips.size()-1).add(new EnemyShip(centerX - offsetX, startY + 140 - offsetY, typeL));
			this.shipCount++;

			// Right Wing
			this.enemyShips.add(new ArrayList<EnemyShip>());
			SpriteType typeR = (qIndex < spriteQueue.size()) ? spriteQueue.get(qIndex++) : SpriteType.EnemyShipB1;
			this.enemyShips.get(this.enemyShips.size()-1).add(new EnemyShip(centerX + offsetX, startY + 140 - offsetY, typeR));
			this.shipCount++;
		}
	}

	/**
	 * Creates a randomly scattered formation.
	 * * @param level Level data.
	 */
	private void createRandomFormation(Level level) {
		int totalShips = 15;
		Random r = new Random();
		List<SpriteType> spriteQueue = buildLayeredQueueFromLevel(level, totalShips, 1);

		for(int i=0; i<totalShips; i++) {
			this.enemyShips.add(new ArrayList<EnemyShip>());
			int x = 50 + r.nextInt(350);
			int y = 80 + r.nextInt(200);

			SpriteType type = (i < spriteQueue.size()) ? spriteQueue.get(i) : SpriteType.values()[4 + r.nextInt(3)];

			EnemyShip ship = new EnemyShip(x, y, type);
			this.enemyShips.get(i).add(ship);
			this.shipCount++;
		}
	}

	/**
	 * Determines sprite type based on row index (Legacy).
	 * @param row Row index.
	 * @return SpriteType.
	 */
	private SpriteType calculateSpriteType(int row) {
		switch (row) {
			case 0: return SpriteType.EnemyShipC1;
			case 1: return SpriteType.EnemyShipB1;
			default: return SpriteType.EnemyShipA1;
		}
	}

	/**
	 * Associates the formation to a given screen.
	 *
	 * @param newScreen
	 * Screen to attach.
	 */
	public final void attach(final Screen newScreen) {
		screen = newScreen;
	}

	/**
	 * Draws every individual component of the formation.
	 */
	public final void draw() {
		for (List<EnemyShip> column : this.enemyShips)
			for (EnemyShip enemyShip : column)
				drawManager.drawEntity(enemyShip, enemyShip.getPositionX(),
						enemyShip.getPositionY());
	}

	/**
	 * Updates the position of the ships.
	 */
	public final void update() {
		if(this.shootingCooldown == null) {
			this.shootingCooldown = Core.getVariableCooldown(shootingInterval,
					shootingVariance);
			this.shootingCooldown.reset();
		}

		cleanUp();

		int movementX = 0;
		int movementY = 0;
		double remainingProportion = (double) this.shipCount
				/ (this.nShipsHigh * this.nShipsWide);
		this.movementSpeed = (int) (Math.pow(remainingProportion, 2)
				* this.baseSpeed);
		this.movementSpeed += MINIMUM_SPEED;

		movementInterval++;
		if (movementInterval >= this.movementSpeed) {
			movementInterval = 0;

			updateSlowdown();

			boolean isAtBottom = positionY
					+ this.height > GameScreen.getItemsSeparationLineHeight();
			boolean isAtRightSide = positionX
					+ this.width >= screen.getWidth() - SIDE_MARGIN;
			boolean isAtLeftSide = positionX <= SIDE_MARGIN;
			boolean isAtTop = positionY <= INIT_POS_Y;

			// Diagonal movement direction change logic
			if (currentDirection == Direction.DOWN_RIGHT) {
				if (isAtBottom && isAtRightSide) {
					currentDirection = Direction.UP_LEFT;
				} else if (isAtBottom) {
					currentDirection = Direction.UP_RIGHT;
				} else if (isAtRightSide) {
					currentDirection = Direction.DOWN_LEFT;
				}
			} else if (currentDirection == Direction.DOWN_LEFT) {
				if (isAtBottom && isAtLeftSide) {
					currentDirection = Direction.UP_RIGHT;
				} else if (isAtBottom) {
					currentDirection = Direction.UP_LEFT;
				} else if (isAtLeftSide) {
					currentDirection = Direction.DOWN_RIGHT;
				}
			} else if (currentDirection == Direction.UP_RIGHT) {
				if (isAtTop && isAtRightSide) {
					currentDirection = Direction.DOWN_LEFT;
				} else if (isAtTop) {
					currentDirection = Direction.DOWN_RIGHT;
				} else if (isAtRightSide) {
					currentDirection = Direction.UP_LEFT;
				}
			} else if (currentDirection == Direction.UP_LEFT) {
				if (isAtTop && isAtLeftSide) {
					currentDirection = Direction.DOWN_RIGHT;
				} else if (isAtTop) {
					currentDirection = Direction.DOWN_LEFT;
				} else if (isAtLeftSide) {
					currentDirection = Direction.UP_RIGHT;
				}
			}

			int currentXSpeed = getCurrentXSpeed();
			if (currentDirection == Direction.DOWN_RIGHT) {
				movementX = currentXSpeed;   // right
				movementY = Y_SPEED;   // down
			} else if (currentDirection == Direction.DOWN_LEFT) {
				movementX = -currentXSpeed;  // left
				movementY = Y_SPEED;   // down
			} else if (currentDirection == Direction.UP_RIGHT) {
				movementX = currentXSpeed;   // right
				movementY = -Y_SPEED;  // up
			} else if (currentDirection == Direction.UP_LEFT) {
				movementX = -currentXSpeed;  // left
				movementY = -Y_SPEED;  // up
			}

			positionX += movementX;
			positionY += movementY;

			// Cleans explosions.
			List<EnemyShip> destroyed;
			for (List<EnemyShip> column : this.enemyShips) {
				destroyed = new ArrayList<EnemyShip>();
				for (EnemyShip ship : column) {
					if (ship != null && ship.isExplosionFinished()) {
						destroyed.add(ship);
					}
				}
				column.removeAll(destroyed);
			}

			for (List<EnemyShip> column : this.enemyShips)
				for (EnemyShip enemyShip : column) {
					enemyShip.move(movementX, movementY);
					enemyShip.update();
				}
		}
	}

	/**
	 * Cleans empty columns, adjusts the width and height of the formation.
	 */
	private void cleanUp() {
		Set<Integer> emptyColumns = new HashSet<Integer>();
		int maxColumn = 0;
		int minPositionY = Integer.MAX_VALUE;
		for (List<EnemyShip> column : this.enemyShips) {
			if (!column.isEmpty()) {
				// Height of this column
				int columnSize = column.get(column.size() - 1).positionY
						- this.positionY + this.shipHeight;
				maxColumn = Math.max(maxColumn, columnSize);
				minPositionY = Math.min(minPositionY, column.get(0)
						.getPositionY());
			} else {
				// Empty column, we remove it.
				emptyColumns.add(this.enemyShips.indexOf(column));
			}
		}
		for (int index : emptyColumns) {
			this.enemyShips.remove(index);
		}

		int leftMostPoint = 0;
		int rightMostPoint = 0;

		for (List<EnemyShip> column : this.enemyShips) {
			if (!column.isEmpty()) {
				if (leftMostPoint == 0)
					leftMostPoint = column.get(0).getPositionX();
				rightMostPoint = column.get(0).getPositionX();
			}
		}

		this.width = rightMostPoint - leftMostPoint + this.shipWidth;
		this.height = maxColumn;

		this.positionX = leftMostPoint;
		this.positionY = minPositionY;
	}

	/**
	 * Shoots a bullet downwards.
	 *
	 * @param bullets
	 * Bullets set to add the bullet being shot.
	 */
	public final void shoot(final Set<Bullet> bullets) {
		// For now, only ships in the bottom row are able to shoot.
		if (this.shooters.isEmpty()) {return; }
		int index = (int) (Math.random() * this.shooters.size());
		EnemyShip shooter = this.shooters.get(index);

		if (this.shootingCooldown.checkFinished()) {
			this.shootingCooldown.reset();
			bullets.add(BulletPool.getBullet(shooter.getPositionX()
					+ shooter.width / 2, shooter.getPositionY(), BULLET_SPEED));
		}
	}

	/**
	 * Destroys a ship.
	 *
	 * @param destroyedShip
	 * Ship to be destroyed.
	 */
	public final void destroy(final EnemyShip destroyedShip) {
		for (List<EnemyShip> column : this.enemyShips)
			for (int i = 0; i < column.size(); i++)
				if (column.get(i).equals(destroyedShip)) {
					column.get(i).destroy();
					this.logger.info("Destroyed ship in ("
							+ this.enemyShips.indexOf(column) + "," + i + ")");
				}

		// Updates the list of ships that can shoot the player.
		if (this.shooters.contains(destroyedShip)) {
			int destroyedShipIndex = this.shooters.indexOf(destroyedShip);
			int destroyedShipColumnIndex = -1;

			for (List<EnemyShip> column : this.enemyShips)
				if (column.contains(destroyedShip)) {
					destroyedShipColumnIndex = this.enemyShips.indexOf(column);
					break;
				}

			EnemyShip nextShooter = getNextShooter(this.enemyShips
					.get(destroyedShipColumnIndex));

			if (nextShooter != null)
				this.shooters.set(destroyedShipIndex, nextShooter);
			else {
				this.shooters.remove(destroyedShipIndex);
				this.logger.info("Shooters list reduced to "
						+ this.shooters.size() + " members.");
			}
		}

		this.shipCount--;
	}

	/**
	 * Gets the ship on a given column that will be in charge of shooting.
	 *
	 * @param column
	 * Column to search.
	 * @return New shooter ship.
	 */
	public final EnemyShip getNextShooter(final List<EnemyShip> column) {
		Iterator<EnemyShip> iterator = column.iterator();
		EnemyShip nextShooter = null;
		while (iterator.hasNext()) {
			EnemyShip checkShip = iterator.next();
			if (checkShip != null && !checkShip.isDestroyed())
				nextShooter = checkShip;
		}

		return nextShooter;
	}

	/**
	 * Returns an iterator over the ships in the formation.
	 *
	 * @return Iterator over the enemy ships.
	 */
	@Override
	public final Iterator<EnemyShip> iterator() {
		Set<EnemyShip> enemyShipsList = new HashSet<EnemyShip>();

		for (List<EnemyShip> column : this.enemyShips)
			for (EnemyShip enemyShip : column)
				enemyShipsList.add(enemyShip);

		return enemyShipsList.iterator();
	}

	/**
	 * Destroy all ships in the formation.
	 *
	 * @return The number of destroyed ships.
	 */

	public final int destroyAll() {
		int destroyed = 0;
		for (List<EnemyShip> column : this.enemyShips) {
			for (EnemyShip enemyShip : column) {
				if (!enemyShip.isDestroyed()) {
					enemyShip.destroy();
					destroyed++;
				}
			}
		}
		this.shipCount = 0;
		return destroyed;
	}

	/**
	 * Checks if there are any ships remaining.
	 * * @return True if formation is empty.
	 */
	public final boolean isEmpty() {
		return this.shipCount <= 0;
	}

	/**
	 * Activates the slowdown effect on the formation.
	 */
	public void activateSlowdown() {
		this.isSlowedDown = true;
		this.slowDownCount = 0;
		this.logger.info("Enemy formation slowed down!");
	}

	/**
	 * Gets the current horizontal speed based on slowdown status.
	 * @return Current X speed.
	 */
	private int getCurrentXSpeed() {
		if (isSlowedDown) {
			return SLOWED_X_SPEED;
		}
		return ORIGINAL_X_SPEED;
	}

	/**
	 * Updates the slowdown effect timer.
	 */
	private void updateSlowdown() {
		if (isSlowedDown) {
			slowDownCount++;
			if (slowDownCount >= SLOWDOWN_DURATION) {
				isSlowedDown = false;
				slowDownCount = 0;
				this.logger.info("Slowdown effect ended.");
			}
		}
	}

	/**
	 * Clears the formation (removes all ships).
	 */
	public final void clear() {
		for (List<EnemyShip> column : this.enemyShips) {
			column.clear();
		}
		this.enemyShips.clear();
		this.shipCount = 0;
	}

	/**
	 * Constructs a queue of sprite types based on level data.
	 * Ensures distribution matches enemy counts defined in the Level.
	 * * @param level Current Level data.
	 * @param width Formation width.
	 * @param height Formation height.
	 * @return List of SpriteTypes.
	 */
	private List<SpriteType> buildLayeredQueueFromLevel(final Level level, final int width, final int height) {
		final int cells = width * height;
		List<SpriteType> rowMajor = new ArrayList<>(cells);

		if (level == null || level.getEnemyTypes() == null || level.getEnemyTypes().isEmpty()) {
			return new ArrayList<>();
		}

		int countA = 0, countB = 0, countC = 0;
		for (engine.level.EnemyType t : level.getEnemyTypes()) {
			String kind = (t.getType() == null) ? "enemya" : t.getType().trim().toLowerCase();
			int cnt = Math.max(0, t.getCount());
			switch (kind) {
				case "enemya": case "a": countA += cnt; break;
				case "enemyb": case "b": countB += cnt; break;
				case "enemyc": case "c": countC += cnt; break;
				default: countA += cnt;
			}
		}

		int total = countA + countB + countC;
		// Pad with type A if counts don't match total cells
		if (total < cells) countA += (cells - total);

		// Distribute types (Strongest first generally implies top rows)
		for(int k=0; k<countC; k++) rowMajor.add(SpriteType.EnemyShipC1);
		for(int k=0; k<countB; k++) rowMajor.add(SpriteType.EnemyShipB1);
		for(int k=0; k<countA; k++) rowMajor.add(SpriteType.EnemyShipA1);

		return rowMajor;
	}

	/**
	 * Applies a specific color to all ships in the formation.
	 * @param color Target color.
	 */
	public void applyEnemyColor(final Color color) {
		for (java.util.List<EnemyShip> column : this.enemyShips) {
			for (EnemyShip ship : column) {
				if (ship != null && !ship.isDestroyed()) {
					ship.setColor(color);
				}
			}
		}
	}

	/**
	 * Applies color based on the current level number.
	 * @param level Level object.
	 */
	public void applyEnemyColorByLevel(final Level level) {
		if (level == null) return;
		final int lv = level.getLevel();
		applyEnemyColor(getColorForLevel(lv));
	}

	/**
	 * Returns a color corresponding to the level number.
	 * @param levelNumber Level index.
	 * @return Color object.
	 */
	private Color getColorForLevel(final int levelNumber) {
		switch (levelNumber) {
			case 1: return new Color(0x3DDC84); // green
			case 2: return new Color(0x00BCD4); // cyan
			case 3: return new Color(0xFF4081); // pink
			case 4: return new Color(0xFFC107); // amber
			case 5: return new Color(0x9C27B0); // purple
			case 6: return new Color(0xFF5722); // deep orange
			case 7: return new Color(0x8BC34A); // light green
			case 8: return new Color(0x03A9F4); // light blue
			case 9: return new Color(0xE91E63); // magenta
			case 10: return new Color(0x607D8B); // blue gray
			default: return Color.WHITE;
		}
	}
}