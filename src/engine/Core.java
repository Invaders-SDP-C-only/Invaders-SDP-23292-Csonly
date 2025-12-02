package engine;

import audio.SoundManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;
import java.util.HashSet;

import entity.ShopItem;
import screen.GameScreen;
import screen.HighScoreScreen;
import screen.ScoreScreen;
import screen.Screen;
import screen.ShopScreen;
import screen.TitleScreen;
import screen.AchievementScreen;
import screen.SandboxScreen; // test code for sandbox-mode
import engine.level.LevelManager;
import screen.ShopScreen;
import screen.*;


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
	// [추가] 샌드박스 모드에서 깬 보스 목록을 임시 저장하는 변수
	private static Set<String> savedClearedBossRooms = new HashSet<>();


	/**
	 * Test implementation.
	 *
	 * @param args
	 *            Program args, ignored.
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
			// TODO handle exception
			e.printStackTrace();
		}

		frame = new Frame(WIDTH, HEIGHT);
		DrawManager.getInstance().setFrame(frame);
		int width = frame.getWidth();
		int height = frame.getHeight();

		levelManager = new LevelManager();
		GameState gameState = new GameState(1, 0, MAX_LIVES, MAX_LIVES, 0, 0,0);


        int returnCode = 1;
		do {
            // gameState = new GameState(1, 0, MAX_LIVES,MAX_LIVES, 0, 0,gameState.getCoin());
			switch (returnCode) {
                case 1:
                    // Main menu.
                    currentScreen = new TitleScreen(width, height, FPS);
					SoundManager.stopAll();
					SoundManager.playLoop("sfx/menu_music.wav");
                    LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
                            + " title screen at " + FPS + " fps.");
                    returnCode = frame.setScreen(currentScreen);
                    LOGGER.info("Closing title screen.");
                    break;
                case 2:
                    // Show player mode selection screen (1P / 2P)
                    ModeSelectScreen modeScreen = new ModeSelectScreen(width, height, FPS);
                    frame.setScreen(modeScreen);
                    modeScreen.run();
                    String selectedMode = modeScreen.getSelectedMode();
                    LOGGER.info("Selected Mode: " + selectedMode);

                    // If canceled, return to title
                    if ("CANCEL".equals(selectedMode)) {
                        LOGGER.info("Mode selection canceled, returning to title screen.");
                        currentScreen = new TitleScreen(width, height, FPS);
                        returnCode = frame.setScreen(currentScreen);
                        break;
                    }

                    boolean isTwoPlayer = "2P".equals(selectedMode);
                    int livesP1 = MAX_LIVES;
                    int livesP2 = isTwoPlayer ? MAX_LIVES : 0;

                    gameState = new GameState(1, 0, livesP1, livesP2, 0, 0, gameState.getCoin());

					returnCode = 9;
					break;
                case 3:
                    // High scores
                    currentScreen = new HighScoreScreen(width, height, FPS);
                    LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
                            + " high score screen at " + FPS + " fps.");
                    returnCode = frame.setScreen(currentScreen);
                    LOGGER.info("Closing high score screen.");
                    break;
                case 4:
                    // Shop opened manually from main menu

                    currentScreen = new ShopScreen(gameState, width, height, FPS, false);
                    LOGGER.info("Starting shop screen (menu) with " + gameState.getCoin() + " coins.");
                    returnCode = frame.setScreen(currentScreen);
                    LOGGER.info("Closing shop screen (menu).");
                    break;
                case 6:
                    // Achievements
                    currentScreen = new AchievementScreen(width, height, FPS);
                    LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
                            + " achievement screen at " + FPS + " fps.");
                    returnCode = frame.setScreen(currentScreen);
                    LOGGER.info("Closing achievement screen.");
                    break;
				case 8: // CreditScreen
					currentScreen = new CreditScreen(width, height, FPS);
					LOGGER.info("Starting " + currentScreen.getClass().getSimpleName() + " screen.");
					returnCode = frame.setScreen(currentScreen);
					break;
				case 9:
					// Mode Selection Screen (Classic / Sandbox)
					GameModeSelectScreen typeScreen = new GameModeSelectScreen(width, height, FPS);
					frame.setScreen(typeScreen);
					typeScreen.run();
					String gameType = typeScreen.getSelectedMode();

					if (GameModeSelectScreen.TYPE_CANCEL.equals(gameType)) {
						returnCode = 1; // 타이틀로 복귀
						break;
					}

					if (GameModeSelectScreen.TYPE_SANDBOX.equals(gameType)) {
						// 샌드박스 모드 시작 (returnCode 10로 이동)
						returnCode = 10;
					} else {
						// 클래식 모드 시작 (기존 루프 로직 진입)
						// 기존의 do-while 루프 로직을 별도 메서드로 빼거나 여기서 직접 처리
						// (코드 간결화를 위해 기존 로직 유지하되, 여기서 바로 진입)
						do {
							boolean isBonusLevel = gameState.getLevel() % EXTRA_LIFE_FRECUENCY == 0;
							boolean p1CanGain   = gameState.getLivesRemaining()    > 0 && gameState.getLivesRemaining()    < MAX_LIVES;
							boolean p2CanGain   = gameState.getLivesRemainingP2()  > 0 && gameState.getLivesRemainingP2()  < MAX_LIVES;
							boolean bonusLife   = isBonusLevel && (p1CanGain || p2CanGain);

							// Level music
							SoundManager.stopAll();
							SoundManager.playLoop("sfx/level" + gameState.getLevel() + ".wav");

							// Load level
							engine.level.Level currentLevel = levelManager.getLevel(gameState.getLevel());
							if (currentLevel == null) {
								// If no more levels are defined, exit to score
								break;
							}

							// Start level
							currentScreen = new GameScreen(
									gameState,
									currentLevel,
									bonusLife,
									MAX_LIVES,
									width,
									height,
									FPS
							);

							LOGGER.info("Starting " + WIDTH + "x" + HEIGHT + " game screen at " + FPS + " fps.");
							frame.setScreen(currentScreen);
							LOGGER.info("Closing game screen.");

							// Pull back the updated game state
							gameState = ((GameScreen) currentScreen).getGameState();

							// Between-level shop if anyone is still alive
							if (gameState.getLivesRemaining() > 0 || gameState.getLivesRemainingP2() > 0) {
								SoundManager.stopAll();
								SoundManager.play("sfx/levelup.wav");

								LOGGER.info("Opening shop screen with " + gameState.getCoin() + " coins.");
								currentScreen = new ShopScreen(gameState, width, height, FPS, true);
								frame.setScreen(currentScreen);

								// Prepare next level state
								gameState = new GameState(
										gameState.getLevel() + 1,
										gameState.getScore(),
										gameState.getLivesRemaining(),
										gameState.getLivesRemainingP2(),
										gameState.getBulletsShot(),
										gameState.getShipsDestroyed(),
										gameState.getCoin()
								);
							}
						} while (gameState.getLivesRemaining() > 0 || gameState.getLivesRemainingP2() > 0);

						// game이 끝났을 시
						SoundManager.stopAll();
						// SoundManager.play("sfx/gameover.wav");
						currentScreen = new ScoreScreen(width, height, FPS, gameState);
						returnCode = frame.setScreen(currentScreen);
					}
					break;
				case 10:
					// Sandbox Mode
					// 샌드박스 초기화 (1레벨부터 시작)
					gameState.setLevel(1);
					currentScreen = new SandboxScreen(gameState, width, height, FPS, savedClearedBossRooms);
					returnCode = frame.setScreen(currentScreen);
					// 샌드박스 종료 후 상태 업데이트
					gameState = ((SandboxScreen)currentScreen).getGameState();
					savedClearedBossRooms = ((SandboxScreen)currentScreen).getClearedBossRooms();

					// [수정 4] 샌드박스 결과 처리
					if (returnCode == 11) {
						// 상점 진입 (코드 11 유지)
					} else if (returnCode == 2) {
						// 게임 오버 또는 올 클리어 -> 스코어 화면으로
						SoundManager.stopAll();
						// SoundManager.play("sfx/gameover.wav"); // 스코어 화면에서 재생됨
						currentScreen = new ScoreScreen(width, height, FPS, gameState);
						returnCode = frame.setScreen(currentScreen);
					} else {
						// 그 외(ESC 등) -> 메인 메뉴
						returnCode = 1;
					}
					break;
				case 11: // Sandbox Mode Shop
					SoundManager.stopAll();
					SoundManager.playLoop("sfx/bgm_shop.wav"); // 상점 브금 (필요시)
					LOGGER.info("Opening Sandbox Shop.");
					// betweenLevels = true로 설정하여 "Back to Game" 표시
					currentScreen = new ShopScreen(gameState, width, height, FPS, true);
					frame.setScreen(currentScreen);

					// 상점 종료 후 다시 샌드박스(10)로 돌아감 -> 맵/적 리셋됨
					returnCode = 10;
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
	 * @return Application file manager.
	 */
	public static FileManager getFileManager() {
		return FileManager.getInstance();
	}

	/**
	 * Controls creation of new cooldowns.
	 *
	 * @param milliseconds
	 *            Duration of the cooldown.
	 * @return A new cooldown.
	 */
	public static Cooldown getCooldown(final int milliseconds) {
		return new Cooldown(milliseconds);
	}

	/**
	 * Controls creation of new cooldowns with variance.
	 *
	 * @param milliseconds
	 *            Duration of the cooldown.
	 * @param variance
	 *            Variation in the cooldown duration.
	 * @return A new cooldown with variance.
	 */
	public static Cooldown getVariableCooldown(final int milliseconds,
			final int variance) {
		return new Cooldown(milliseconds, variance);
	}
}