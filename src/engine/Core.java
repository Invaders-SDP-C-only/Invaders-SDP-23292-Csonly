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

public final class Core {

	private static final int WIDTH = 448;
	private static final int HEIGHT = 520;
	private static final int FPS = 60;
	private static final int MAX_LIVES = 3;
	private static final int EXTRA_LIFE_FRECUENCY = 3;

	private static Frame frame;
	private static Screen currentScreen;
	private static LevelManager levelManager;
	private static final Logger LOGGER = Logger.getLogger(Core.class.getSimpleName());
	private static Handler fileHandler;
	private static ConsoleHandler consoleHandler;

	// 샌드박스 보스 클리어 기록 (상점 갔다와도 유지됨)
	private static Set<String> savedClearedBossRooms = new HashSet<>();

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
			// [중요] 여기서 gameState = new ... 를 하면 안됨! (1P/2P 선택 정보가 날아감)

			switch (returnCode) {
				case 1: // Main Menu
					currentScreen = new TitleScreen(width, height, FPS);
					SoundManager.stopAll();
					SoundManager.playLoop("sfx/menu_music.wav");
					LOGGER.info("Starting title screen.");
					returnCode = frame.setScreen(currentScreen);

					// 메인 메뉴로 돌아오면 샌드박스 기록 초기화
					savedClearedBossRooms.clear();
					break;

				case 2: // 1P vs 2P Selection
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

					// 유저 선택 반영하여 상태 생성
					gameState = new GameState(1, 0, livesP1, livesP2, 0, 0, gameState.getCoin());
					returnCode = 9; // 게임 모드 선택 화면으로 이동
					break;

				case 3: // High Scores
					currentScreen = new HighScoreScreen(width, height, FPS);
					returnCode = frame.setScreen(currentScreen);
					break;

				case 4: // Shop (Menu)
					currentScreen = new ShopScreen(gameState, width, height, FPS, false);
					returnCode = frame.setScreen(currentScreen);
					break;

				case 6: // Achievements
					currentScreen = new AchievementScreen(width, height, FPS);
					returnCode = frame.setScreen(currentScreen);
					break;

				case 8: // Credits
					currentScreen = new CreditScreen(width, height, FPS);
					returnCode = frame.setScreen(currentScreen);
					break;

				case 9: // [NEW] Game Type Selection (Classic / Sandbox)
					GameModeSelectScreen typeScreen = new GameModeSelectScreen(width, height, FPS);
					frame.setScreen(typeScreen);
					typeScreen.run();
					String gameType = typeScreen.getSelectedMode();

					if (GameModeSelectScreen.TYPE_CANCEL.equals(gameType)) {
						returnCode = 1;
						break;
					}

					if (GameModeSelectScreen.TYPE_SANDBOX.equals(gameType)) {
						returnCode = 10; // 샌드박스 모드로
					} else {
						// === [MERGE] CLASSIC MODE LOOP (from develop branch) ===
						boolean exitToTitle = false;
						do {
							boolean isBonusLevel = gameState.getLevel() % EXTRA_LIFE_FRECUENCY == 0;
							boolean p1CanGain = gameState.getLivesRemaining() > 0 && gameState.getLivesRemaining() < MAX_LIVES;
							boolean p2CanGain = gameState.getLivesRemainingP2() > 0 && gameState.getLivesRemainingP2() < MAX_LIVES;
							boolean bonusLife = isBonusLevel && (p1CanGain || p2CanGain);

							SoundManager.stopAll();
							SoundManager.playLoop("sfx/level" + gameState.getLevel() + ".wav");

							engine.level.Level currentLevel = levelManager.getLevel(gameState.getLevel());
							if (currentLevel == null) break;

							currentScreen = new GameScreen(gameState, currentLevel, bonusLife, MAX_LIVES, width, height, FPS);
							LOGGER.info("Starting Classic Game Level " + gameState.getLevel());

							// [중요] 일시정지 메뉴의 리턴값 처리 (1: Title, 2: Restart)
							int gameReturnCode = frame.setScreen(currentScreen);
							gameState = ((GameScreen) currentScreen).getGameState();

							if (gameReturnCode == 1) { // Quit to Title
								exitToTitle = true;
								break;
							} else if (gameReturnCode == 2) { // Restart
								gameState = new GameState(1, 0, MAX_LIVES, MAX_LIVES, 0, 0, 0); // Reset stats
								continue;
							}

							if (gameState.getLivesRemaining() > 0 || gameState.getLivesRemainingP2() > 0) {
								SoundManager.stopAll();
								SoundManager.play("sfx/levelup.wav");
								currentScreen = new ShopScreen(gameState, width, height, FPS, true);
								frame.setScreen(currentScreen);
								gameState = new GameState(gameState.getLevel() + 1, gameState.getScore(), gameState.getLivesRemaining(), gameState.getLivesRemainingP2(), gameState.getBulletsShot(), gameState.getShipsDestroyed(), gameState.getCoin());
							}
						} while (gameState.getLivesRemaining() > 0 || gameState.getLivesRemainingP2() > 0);

						if (exitToTitle) {
							returnCode = 1;
							break;
						}

						SoundManager.stopAll();
						SoundManager.play("sfx/gameover.wav");
						currentScreen = new ScoreScreen(width, height, FPS, gameState);
						returnCode = frame.setScreen(currentScreen);
					}
					break;

				case 10: // Sandbox Mode Game
					LOGGER.info("Starting Sandbox Mode.");
					gameState.setLevel(1);
					// 저장된 보스 목록을 전달하여 재생성 방지
					currentScreen = new SandboxScreen(gameState, width, height, FPS, savedClearedBossRooms);
					returnCode = frame.setScreen(currentScreen);

					// 게임 종료 후 상태 저장
					gameState = ((SandboxScreen)currentScreen).getGameState();
					savedClearedBossRooms = ((SandboxScreen)currentScreen).getClearedBossRooms();

					if (returnCode == 11) {
						// 상점 진입
					} else if (returnCode == 2) {
						// 게임 클리어/오버 -> 스코어 화면
						SoundManager.stopAll();
						currentScreen = new ScoreScreen(width, height, FPS, gameState);
						returnCode = frame.setScreen(currentScreen);
						savedClearedBossRooms.clear();
					} else {
						// 나가기 -> 타이틀
						returnCode = 1;
						savedClearedBossRooms.clear();
					}
					break;

				case 11: // Sandbox Shop
					SoundManager.stopAll();
					SoundManager.playLoop("sfx/bgm_shop.wav");
					currentScreen = new ShopScreen(gameState, width, height, FPS, true);
					frame.setScreen(currentScreen);
					returnCode = 10; // 다시 샌드박스로
					break;

				default:
					break;
			}
		} while (returnCode != 0);

		fileHandler.flush();
		fileHandler.close();
		System.exit(0);
	}

	private Core() {}
	public static Logger getLogger() { return LOGGER; }
	public static DrawManager getDrawManager() { return DrawManager.getInstance(); }
	public static InputManager getInputManager() { return InputManager.getInstance(); }
	public static FileManager getFileManager() { return FileManager.getInstance(); }
	public static Cooldown getCooldown(final int milliseconds) { return new Cooldown(milliseconds); }
	public static Cooldown getVariableCooldown(final int milliseconds, final int variance) { return new Cooldown(milliseconds, variance); }
}