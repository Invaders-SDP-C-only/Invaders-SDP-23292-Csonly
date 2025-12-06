package engine;

import audio.SoundManager;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;
import java.util.HashSet;

import screen.*;
import engine.level.LevelManager;

/**
 * Implements core game logic.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public final class Core {

	/** Width of current screen. */
	private static final int WIDTH = 448;
	/** Height of current screen. */
	private static final int HEIGHT = 520;
	/** Max fps of current screen. */
	private static final int FPS = 60;

	/** Max lives. */
	private static final int MAX_LIVES = 3;
	/** Levels between extra life. */
	private static final int EXTRA_LIFE_FRECUENCY = 3;

	/** Saved row position for Sandbox mode. */
	private static int savedSandboxRow = -1;
	/** Saved column position for Sandbox mode. */
	private static int savedSandboxCol = -1;

	/** Frame to draw the screen on. */
	private static Frame frame;
	/** Screen currently shown. */
	private static Screen currentScreen;
	/** Level manager for loading level settings. */
	private static LevelManager levelManager;
	/** Application logger. */
	private static final Logger LOGGER = Logger.getLogger(Core.class
			.getSimpleName());
	/** Logger handler for printing to disk. */
	private static Handler fileHandler;
	/** Logger handler for printing to console. */
	private static ConsoleHandler consoleHandler;

	/** Set of cleared boss rooms coordinates in Sandbox mode. */
	private static Set<String> savedClearedBossRooms = new HashSet<>();
	/** Set of cleared normal rooms coordinates in Sandbox mode. */
	private static Set<String> savedClearedRooms = new HashSet<>();

	/**
	 * Test implementation.
	 *
	 * @param args
	 * Program args, ignored.
	 */
	public static void main(final String[] args) {
		try {
			LOGGER.setUseParentHandlers(false);

			fileHandler = new FileHandler("log");
			fileHandler.setFormatter(new MinimalFormatter());

			consoleHandler = new ConsoleHandler();
			consoleHandler.setFormatter(new MinimalFormatter());

			LOGGER.addHandler(fileHandler);
			LOGGER.addHandler(consoleHandler);
			LOGGER.setLevel(Level.ALL);

		} catch (Exception e) {
			e.printStackTrace();
		}

		frame = new Frame(WIDTH, HEIGHT);
		DrawManager.getInstance().setFrame(frame);
		int width = frame.getWidth();
		int height = frame.getHeight();

		levelManager = new LevelManager();
		GameState gameState = new GameState(1, 0, MAX_LIVES, MAX_LIVES, 0, 0, 0);

		int returnCode = 1;
		do {
			// GameState is re-initialized inside case 2 based on player mode.

			switch (returnCode) {
				case 1:
					// Main menu.
					currentScreen = new TitleScreen(width, height, FPS);
					SoundManager.stopAll();
					SoundManager.playLoop("sfx/menu_music.wav");
					LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
							+ " title screen at " + FPS + " fps.");
					returnCode = frame.setScreen(currentScreen);

					// Reset Sandbox state when returning to title.
					savedClearedBossRooms.clear();
					savedClearedRooms.clear();
					savedSandboxRow = -1;
					savedSandboxCol = -1;
					LOGGER.info("Closing title screen.");
					break;

				case 2:
					// Player Mode Selection (1P / 2P).
					ModeSelectScreen modeScreen = new ModeSelectScreen(width, height, FPS);
					frame.setScreen(modeScreen);
					modeScreen.run();
					String selectedMode = modeScreen.getSelectedMode();

					if ("CANCEL".equals(selectedMode)) {
						returnCode = 1;
						break;
					}

					boolean isTwoPlayer = "2P".equals(selectedMode);
					int livesP1 = MAX_LIVES;
					int livesP2 = isTwoPlayer ? MAX_LIVES : 0;

					// Initialize GameState with selected player mode.
					gameState = new GameState(1, 0, livesP1, livesP2, 0, 0, gameState.getCoin());
					returnCode = 9; // Proceed to Game Type Selection.
					break;

				case 3:
					// High Scores.
					currentScreen = new HighScoreScreen(width, height, FPS);
					LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
							+ " high score screen at " + FPS + " fps.");
					returnCode = frame.setScreen(currentScreen);
					LOGGER.info("Closing high score screen.");
					break;

				case 4:
					// Shop (Menu).
					currentScreen = new ShopScreen(gameState, width, height, FPS, false);
					LOGGER.info("Starting shop screen (menu).");
					returnCode = frame.setScreen(currentScreen);
					LOGGER.info("Closing shop screen (menu).");
					break;

				case 6:
					// Achievements.
					currentScreen = new AchievementScreen(width, height, FPS);
					LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
							+ " achievement screen at " + FPS + " fps.");
					returnCode = frame.setScreen(currentScreen);
					LOGGER.info("Closing achievement screen.");
					break;

				case 8:
					// Credits.
					currentScreen = new CreditScreen(width, height, FPS);
					LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
							+ " credit screen at " + FPS + " fps.");
					returnCode = frame.setScreen(currentScreen);
					LOGGER.info("Closing credit screen.");
					break;

				case 9:
					// Game Mode Selection (Classic / Sandbox).
					GameModeSelectScreen typeScreen = new GameModeSelectScreen(width, height, FPS);
					frame.setScreen(typeScreen);
					typeScreen.run();
					String gameType = typeScreen.getSelectedMode();

					if (GameModeSelectScreen.TYPE_CANCEL.equals(gameType)) {
						returnCode = 1;
						break;
					}

					if (GameModeSelectScreen.TYPE_SANDBOX.equals(gameType)) {
						returnCode = 10; // Sandbox Mode.
					} else {
						// Classic Mode Loop.
						boolean isMultiplayer = gameState.getLivesRemainingP2() > 0;
						boolean exitToTitle = false;
						do {
							boolean isBonusLevel = gameState.getLevel() % EXTRA_LIFE_FRECUENCY == 0;
							boolean p1CanGain = gameState.getLivesRemaining() > 0
									&& gameState.getLivesRemaining() < MAX_LIVES;
							boolean p2CanGain = gameState.getLivesRemainingP2() > 0
									&& gameState.getLivesRemainingP2() < MAX_LIVES;
							boolean bonusLife = isBonusLevel && (p1CanGain || p2CanGain);

							SoundManager.stopAll();
							SoundManager.playLoop("sfx/level" + gameState.getLevel() + ".wav");

							engine.level.Level currentLevel = levelManager.getLevel(gameState.getLevel());
							if (currentLevel == null) break;

							currentScreen = new GameScreen(gameState, currentLevel, bonusLife, MAX_LIVES, width,
									height, FPS);
							LOGGER.info("Starting Classic Game Level " + gameState.getLevel());

							int gameReturnCode = frame.setScreen(currentScreen);
							gameState = ((GameScreen) currentScreen).getGameState();

							if (gameReturnCode == 1) { // Quit to Title.
								exitToTitle = true;
								break;
							} else if (gameReturnCode == 2) { // Restart Level.
								int initialLivesP2 = isMultiplayer ? MAX_LIVES : 0;
								gameState = new GameState(1, 0, MAX_LIVES, initialLivesP2, 0, 0, 0);
								continue;
							}

							if (gameState.getLivesRemaining() > 0 || gameState.getLivesRemainingP2() > 0) {
								SoundManager.stopAll();
								SoundManager.play("sfx/levelup.wav");
								currentScreen = new ShopScreen(gameState, width, height, FPS, true);
								frame.setScreen(currentScreen);
								gameState = new GameState(gameState.getLevel() + 1, gameState.getScore(),
										gameState.getLivesRemaining(), gameState.getLivesRemainingP2(),
										gameState.getBulletsShot(), gameState.getShipsDestroyed(), gameState.getCoin());
							}
						} while (gameState.getLivesRemaining() > 0 || gameState.getLivesRemainingP2() > 0);

						if (exitToTitle) {
							returnCode = 1;
							break;
						}

						SoundManager.stopAll();
						SoundManager.play("sfx/gameover.wav");
						LOGGER.info("Starting score screen.");
						currentScreen = new ScoreScreen(width, height, FPS, gameState);
						returnCode = frame.setScreen(currentScreen);
						LOGGER.info("Closing score screen.");
					}
					break;

				case 10:
					// Sandbox Mode Game.
					LOGGER.info("Starting Sandbox Mode.");
					gameState.setLevel(1);
					currentScreen = new SandboxScreen(gameState, width, height, FPS, savedClearedBossRooms,
							savedClearedRooms, savedSandboxRow, savedSandboxCol);
					returnCode = frame.setScreen(currentScreen);

					// Save state after session.
					gameState = ((SandboxScreen) currentScreen).getGameState();
					savedClearedBossRooms = ((SandboxScreen) currentScreen).getClearedBossRooms();
					savedClearedRooms = ((SandboxScreen) currentScreen).getClearedRooms();

					if (returnCode == 11) {
						// Enter Sandbox Shop.
						savedSandboxRow = ((SandboxScreen) currentScreen).getCurrentRow();
						savedSandboxCol = ((SandboxScreen) currentScreen).getCurrentCol();
					} else if (returnCode == 12) {
						// Restart Sandbox (Reset persistent data).
						savedClearedBossRooms.clear();
						savedClearedRooms.clear();
						savedSandboxRow = -1;
						savedSandboxCol = -1;
						returnCode = 10;
					} else if (returnCode == 2) {
						// Game Clear/Over -> Score Screen.
						savedSandboxRow = -1;
						savedSandboxCol = -1;
						SoundManager.stopAll();
						LOGGER.info("Starting score screen.");
						currentScreen = new ScoreScreen(width, height, FPS, gameState);
						returnCode = frame.setScreen(currentScreen);
						savedClearedBossRooms.clear();
						LOGGER.info("Closing score screen.");
					} else {
						// Quit -> Main Menu.
						savedSandboxRow = -1;
						savedSandboxCol = -1;
						savedClearedBossRooms.clear();
						savedClearedRooms.clear();
						if (returnCode != 2) returnCode = 1;
					}
					break;

				case 11:
					// Sandbox Shop.
					SoundManager.stopAll();
					SoundManager.playLoop("sfx/bgm_shop.wav");
					LOGGER.info("Starting shop screen (Sandbox).");
					currentScreen = new ShopScreen(gameState, width, height, FPS, true);
					frame.setScreen(currentScreen);
					LOGGER.info("Closing shop screen (Sandbox).");
					returnCode = 10; // Return to Sandbox.
					break;

				default:
					break;
			}
		} while (returnCode != 0);

		fileHandler.flush();
		fileHandler.close();
		System.exit(0);
	}

	/**
	 * Constructor, not called.
	 */
	private Core() {

	}

	/**
	 * Controls access to the logger.
	 *
	 * @return Application logger.
	 */
	public static Logger getLogger() {
		return LOGGER;
	}

	/**
	 * Controls access to the drawing manager.
	 *
	 * @return Application draw manager.
	 */
	public static DrawManager getDrawManager() {
		return DrawManager.getInstance();
	}

	/**
	 * Controls access to the input manager.
	 *
	 * @return Application input manager.
	 */
	public static InputManager getInputManager() {
		return InputManager.getInstance();
	}

	/**
	 * Controls access to the file manager.
	 *
	 * @return Application file manager.
	 */
	public static FileManager getFileManager() {
		return FileManager.getInstance();
	}

	/**
	 * Controls creation of new cooldowns.
	 *
	 * @param milliseconds
	 * Duration of the cooldown.
	 * @return A new cooldown.
	 */
	public static Cooldown getCooldown(final int milliseconds) {
		return new Cooldown(milliseconds);
	}

	/**
	 * Controls creation of new cooldowns with variance.
	 *
	 * @param milliseconds
	 * Duration of the cooldown.
	 * @param variance
	 * Variation in the cooldown duration.
	 * @return A new cooldown with variance.
	 */
	public static Cooldown getVariableCooldown(final int milliseconds,
											   final int variance) {
		return new Cooldown(milliseconds, variance);
	}
}