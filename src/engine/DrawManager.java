package engine;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.awt.event.KeyEvent;

import entity.Entity;
import entity.FinalBoss;
import entity.FinalBoss_3;
import entity.Ship;
import engine.Achievement;
import screen.CreditScreen;
import screen.GameScreen;
import screen.Screen;
import screen.SettingsScreen;
import engine.Score;
import screen.TitleScreen;
import screen.TitleScreen.Star;
import screen.TitleScreen.ShootingStar;

/**
 * Manages screen drawing.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public final class DrawManager {

	/**
	 * Singleton instance of the class.
	 */
	private static DrawManager instance;
	/**
	 * Current frame.
	 */
	private static Frame frame;
	/**
	 * FileManager instance.
	 */
	private static FileManager fileManager;
	/**
	 * Application logger.
	 */
	private static final Logger logger = Core.getLogger();
	/**
	 * Graphics context.
	 */
	private static Graphics graphics;
	/**
	 * Buffer Graphics.
	 */
	private static Graphics backBufferGraphics;
	/**
	 * Buffer image.
	 */
	private static BufferedImage backBuffer;
	/**
	 * Normal sized font.
	 */
	private static Font fontRegular;
	/**
	 * Normal sized font properties.
	 */
	private static FontMetrics fontRegularMetrics;
	/**
	 * Big sized font.
	 */
	private static Font fontBig;
	/**
	 * Big sized font properties.
	 */
	private static FontMetrics fontBigMetrics;
	/**
	 * Small sized font for credits.
	 */
	private static Font fontSmall;
	/**
	 * Small sized font properties.
	 */
	private static FontMetrics fontSmallMetrics;
	/**
	 * Pause menu animation state.
	 */
	private static long lastPauseMenuDrawTime = 0L;
	private static float pauseMenuAnimProgress = 1f;

	/**
	 * Sprite types mapped to their images.
	 */
	private static Map<SpriteType, boolean[][]> spriteMap;

	/**
	 * Sprite types.
	 */
	public static enum SpriteType {
		Ship, ShipDestroyed, Bullet, EnemyBullet, EnemyShipA1, EnemyShipA2,
		EnemyShipB1, EnemyShipB2, EnemyShipC1, EnemyShipC2, EnemyShipSpecial,
		FinalBoss1, FinalBoss2, FinalBossBullet, FinalBossDeath, OmegaBoss1, OmegaBoss2, OmegaBossDeath, Explosion, SoundOn, SoundOff, Item_MultiShot,
		Item_Atkspeed, Item_Penetrate, Item_Explode, Item_Slow, Item_Stop,
		Item_Push, Item_Shield, Item_Heal,
		SamuraiNormal, SamuraiAttack, SamuraiBroken, SwordWave, DeathblowMarker,
		SwordSlashEffect, ParrySparkEffect
	}

	/**
	 * Private constructor.
	 */
	private DrawManager() {
		fileManager = Core.getFileManager();
		logger.info("Started loading resources.");

		try {
			spriteMap = new LinkedHashMap<SpriteType, boolean[][]>();
			spriteMap.put(SpriteType.Ship, new boolean[13][8]);
			spriteMap.put(SpriteType.ShipDestroyed, new boolean[13][8]);
			spriteMap.put(SpriteType.Bullet, new boolean[3][5]);
			spriteMap.put(SpriteType.EnemyBullet, new boolean[3][5]);
			spriteMap.put(SpriteType.EnemyShipA1, new boolean[12][8]);
			spriteMap.put(SpriteType.EnemyShipA2, new boolean[12][8]);
			spriteMap.put(SpriteType.EnemyShipB1, new boolean[12][8]);
			spriteMap.put(SpriteType.EnemyShipB2, new boolean[12][8]);
			spriteMap.put(SpriteType.EnemyShipC1, new boolean[12][8]);
			spriteMap.put(SpriteType.EnemyShipC2, new boolean[12][8]);
			spriteMap.put(SpriteType.EnemyShipSpecial, new boolean[16][7]);
			spriteMap.put(SpriteType.Explosion, new boolean[13][7]);
			spriteMap.put(SpriteType.SoundOn, new boolean[15][15]);
			spriteMap.put(SpriteType.SoundOff, new boolean[15][15]);
			spriteMap.put(SpriteType.Item_Explode, new boolean[5][5]);
			spriteMap.put(SpriteType.Item_Slow, new boolean[5][5]);
			spriteMap.put(SpriteType.Item_Stop, new boolean[5][5]);
			spriteMap.put(SpriteType.Item_Push, new boolean[5][5]);
			spriteMap.put(SpriteType.Item_Shield, new boolean[5][5]);
			spriteMap.put(SpriteType.Item_Heal, new boolean[5][5]);
			spriteMap.put(SpriteType.FinalBoss1, new boolean[50][40]);
			spriteMap.put(SpriteType.FinalBoss2, new boolean[50][40]);
			spriteMap.put(SpriteType.FinalBossBullet, new boolean[3][5]);
			spriteMap.put(SpriteType.FinalBossDeath, new boolean[50][40]);
			spriteMap.put(SpriteType.OmegaBoss1, new boolean[32][14]);
			spriteMap.put(SpriteType.OmegaBoss2, new boolean[32][14]);
			spriteMap.put(SpriteType.OmegaBossDeath, new boolean[16][16]);
			spriteMap.put(SpriteType.SamuraiNormal, new boolean[24][24]);
			spriteMap.put(SpriteType.SamuraiAttack, new boolean[24][24]);
			spriteMap.put(SpriteType.SamuraiBroken, new boolean[24][24]);
			spriteMap.put(SpriteType.SwordWave, new boolean[12][18]);
			spriteMap.put(SpriteType.DeathblowMarker, new boolean[4][4]);
			spriteMap.put(SpriteType.SwordSlashEffect, new boolean[16][16]);

			fileManager.loadSprite(spriteMap);
			logger.info("Finished loading the sprites.");

			fontRegular = fileManager.loadFont(14f);
			fontBig = fileManager.loadFont(24f);
			fontSmall = fileManager.loadFont(9f);
			logger.info("Finished loading the fonts.");

		} catch (IOException e) {
			logger.warning("Loading failed.");
		} catch (FontFormatException e) {
			logger.warning("Font formatting failed.");
		}
	}

	/**
	 * Returns shared instance of DrawManager.
	 */
	public static DrawManager getInstance() {
		if (instance == null)
			instance = new DrawManager();
		return instance;
	}

	public Graphics getBackBufferGraphics() {
		return backBufferGraphics;
	}

	/**
	 * Sets the frame to draw the image on.
	 */
	public void setFrame(final Frame currentFrame) {
		frame = currentFrame;
	}

	/**
	 * First part of the drawing process.
	 */
	public void initDrawing(final Screen screen) {
		backBuffer = new BufferedImage(screen.getWidth(), screen.getHeight(),
				BufferedImage.TYPE_INT_RGB);

		graphics = frame.getGraphics();
		backBufferGraphics = backBuffer.getGraphics();

		backBufferGraphics.setColor(Color.BLACK);
		backBufferGraphics.fillRect(0, 0, screen.getWidth(), screen.getHeight());

		fontRegularMetrics = backBufferGraphics.getFontMetrics(fontRegular);
		fontBigMetrics = backBufferGraphics.getFontMetrics(fontBig);
		fontSmallMetrics = backBufferGraphics.getFontMetrics(fontSmall);
	}

	/**
	 * Draws the completed drawing on screen.
	 */
	public void completeDrawing(final Screen screen) {
		graphics.drawImage(backBuffer, frame.getInsets().left,
				frame.getInsets().top, frame);
	}

	/**
	 * Draws an entity.
	 * Handles specific drawing logic for different entity types (e.g. Samurai Boss).
	 * * @param entity Entity to be drawn.
	 * @param positionX X coordinate.
	 * @param positionY Y coordinate.
	 */
	public void drawEntity(final Entity entity, final int positionX, final int positionY) {
		SpriteType type = entity.getSpriteType();
		if (type == SpriteType.FinalBossBullet && entity.getHeight() > 20) {
			backBufferGraphics.setColor(entity.getColor());
			backBufferGraphics.fillRect(positionX, positionY, entity.getWidth(), entity.getHeight());
			return;
		}

		// Special handling for Samurai Boss (uses Image sprite instead of boolean array)
		if (entity instanceof entity.SamuraiBoss) {
			entity.SamuraiBoss boss = (entity.SamuraiBoss) entity;
			if (boss.getSprite() != null) {
				backBufferGraphics.drawImage(
						boss.getSprite(),
						positionX,
						positionY,
						boss.getWidth(),
						boss.getHeight(),
						null
				);
			}
			return;
		}
		boolean[][] image = spriteMap.get(type);
		if (image == null) {
			logger.warning("SpriteType " + type + " is null in spriteMap.");
			backBufferGraphics.setColor(entity.getColor());
			backBufferGraphics.fillRect(positionX, positionY, entity.getWidth(), entity.getHeight());
			return;
		}

		if (type == null) {
			backBufferGraphics.setColor(entity.getColor());
			backBufferGraphics.fillRect(positionX, positionY, entity.getWidth(), entity.getHeight());
			return;
		}

		backBufferGraphics.setColor(entity.getColor());
		for (int i = 0; i < image.length; i++)
			for (int j = 0; j < image[i].length; j++)
				if (image[i][j])
					backBufferGraphics.drawRect(positionX + i * 2, positionY + j * 2, 1, 1);


	}

	/**
	 * Draws a warning line for laser attacks.
	 * (From feature/new-boss-3)
	 */
	public void drawLaserWarning(final Screen screen, final int x, final int startY) {
		int height = screen.getHeight() - startY;

		float pulse = (float) ((Math.sin(System.currentTimeMillis() / 150.0) + 1.0) / 2.0);
		int alpha = (int) (80 + pulse * 120);

		backBufferGraphics.setColor(new Color(0, 255, 255, alpha));
		backBufferGraphics.drawRect(x, startY, 4, height);
	}

	/**
	 * Draws Boss's health bar.
	 */
	public void drawBossHealthBar(final Screen screen, final int current, final int max) {
		int barWidth = (int) (screen.getWidth() * 0.6);
		int barHeight = 10;
		int x = (screen.getWidth() - barWidth) / 2;
		int y = GameScreen.getItemsSeparationLineHeight() + 8;

		float healthPercent = (float) current / max;

		backBufferGraphics.setColor(Color.DARK_GRAY);
		backBufferGraphics.fillRect(x, y, barWidth, barHeight);
		backBufferGraphics.setColor(Color.RED);
		backBufferGraphics.fillRect(x, y, (int) (barWidth * healthPercent), barHeight);
	}

	/**
	 * Draws a dim overlay for boss wave warning.
	 */
	public void drawBossWaveDim(final Screen screen, float alpha) {
		int a = (int) (Math.max(0f, Math.min(1f, alpha)) * 180);
		backBufferGraphics.setColor(new Color(0, 0, 0, a));
		backBufferGraphics.fillRect(0, 0, screen.getWidth(), screen.getHeight());
	}

	/**
	 * Draws a simple glow around a boss.
	 */
	public void drawBossGlow(int x, int y, int w, int h, float alpha) {
		Graphics2D g2 = (Graphics2D) backBufferGraphics;
		Stroke old = g2.getStroke();
		Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		float a = Math.max(0f, Math.min(1f, alpha));
		float pulse = (float) ((Math.sin(System.currentTimeMillis() / 200.0) + 1.0) / 2.0);
		float radiusGrow = 8f + pulse * 6f;

		int cx = x + w / 2;
		int cy = y + h / 2;
		int rx = (int) ((double)w / 2 + radiusGrow);
		int ry = (int) ((double)h / 2 + radiusGrow);

		g2.setColor(new Color(255, 180, 120, (int) (60 * a)));
		g2.setStroke(new BasicStroke(6f));
		g2.drawOval(cx - rx, cy - ry, rx * 2, ry * 2);

		g2.setColor(new Color(255, 240, 200, (int) (90 * a)));
		g2.setStroke(new BasicStroke(3f));
		g2.drawOval(cx - rx + 6, cy - ry + 6, (rx - 6) * 2, (ry - 6) * 2);

		g2.setStroke(old);
		if (oldAA != null) {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
		}
	}

	/**
	 * Draw Death marker.
	 */
	public void drawDeathblowMarker(final Screen screen, final int x, final int y) {
		int markerWidth = 4 * 2; // 4x2
		int markerHeight = 4 * 2; // 4x2
		int drawX = x - (markerWidth / 2);
		int drawY = y - (markerHeight / 2);

		Entity marker = new Entity(drawX, drawY, markerWidth, markerHeight, Color.RED);
		marker.spriteType = SpriteType.DeathblowMarker;
		drawEntity(marker, marker.getPositionX(), marker.getPositionY());
	}

	/**
	 * Draws Boss's posture bar.
	 */
	public void drawBossPostureBar(final Screen screen, final int current, final int max) {
		int barWidth = (int) (screen.getWidth() * 0.4);
		int barHeight = 8;
		int x = (screen.getWidth() - barWidth) / 2 - 8;
		int y = GameScreen.getItemsSeparationLineHeight() + 20;

		float posturePercent = (float) current / max;

		backBufferGraphics.setColor(Color.DARK_GRAY);
		backBufferGraphics.fillRect(x, y, barWidth, barHeight);
		backBufferGraphics.setColor(Color.ORANGE);
		backBufferGraphics.fillRect(x, y, (int) (barWidth * posturePercent), barHeight);
	}

	public void drawScore(final Screen screen, final int score) {
		backBufferGraphics.setFont(fontRegular);
		backBufferGraphics.setColor(Color.WHITE);
		String scoreString = String.format("P1:%04d", score);
		backBufferGraphics.drawString(scoreString, screen.getWidth() - 120, 25);
	}

	//  === [ADD] Draw P2's score on the line below P1's score ===
	public void drawScoreP2(final Screen screen, final int scoreP2) {
		backBufferGraphics.setFont(fontRegular);
		backBufferGraphics.setColor(Color.WHITE);
		String text = String.format("P2:%04d", scoreP2);
		//  Y coordinate is 15px lower than P1 score to avoid overlapping
		backBufferGraphics.drawString(text, screen.getWidth() - 120, 40);
	}

	/**
	 * Draws the elapsed time on screen.
	 */
	public void drawTime(final Screen screen, final long milliseconds) {
		backBufferGraphics.setFont(fontRegular);
		backBufferGraphics.setColor(Color.GRAY);
		long seconds = milliseconds / 1000;
		long minutes = seconds / 60;
		seconds %= 60;
		String timeString = String.format("Time: %02d:%02d", minutes, seconds);
		int x = 10;
		int y = screen.getHeight() - 20;
		backBufferGraphics.drawString(timeString, x, y);
	}

	/**
	 * Draws current coin on screen.
	 */
	public void drawCoin(final Screen screen, final int coin) {
		backBufferGraphics.setFont(fontRegular);
		backBufferGraphics.setColor(Color.WHITE);
		String coinString = String.format("%03d$", coin);
		int x = screen.getWidth() / 2 - fontRegularMetrics.stringWidth(coinString) / 2;
		int y = screen.getHeight() - 50;
		backBufferGraphics.drawString(coinString, x, y);
	}

	/**
	 * Draws number of remaining lives on screen.
	 */
	public void drawLives(final Screen screen, final int lives) {
		backBufferGraphics.setFont(fontRegular);
		backBufferGraphics.setColor(Color.WHITE);
		// backBufferGraphics.drawString("P1:" + Integer.toString(lives), 10, 25);
		backBufferGraphics.drawString("P1:", 15, 25);
		Ship dummyShip = new Ship(0, 0, Color.green);
		for (int i = 0; i < lives; i++)
			drawEntity(dummyShip, 40 + 35 * i, 10);
	}

	public void drawLivesP2(final Screen screen, final int lives) {
		backBufferGraphics.setFont(fontRegular);
		backBufferGraphics.setColor(Color.WHITE);
		// backBufferGraphics.drawString("P2:" + Integer.toString(lives), 10, 40);
		backBufferGraphics.drawString("P2:", 15, 40);

		Ship dummyShip = new Ship(0, 0, Color.pink);
		for (int i = 0; i < lives; i++) {
			drawEntity(dummyShip, 40 + 35 * i, 30);
		}
	}


	/**
	 * Draws the items HUD.
	 */
	public void drawItemsHUD(final Screen screen) {
		ItemHUDManager itemHUD = ItemHUDManager.getInstance();
		itemHUD.initialize(screen);
		itemHUD.drawItems(screen, backBufferGraphics);
	}

	/**
	 * Draws the current level on the bottom-left of the screen.
	 */
	public void drawLevel(final Screen screen, final String levelName) {
		final int paddingX = 20;
		final int paddingY = 50;
		backBufferGraphics.setFont(fontRegular);
		backBufferGraphics.setColor(Color.WHITE);
		int yPos = screen.getHeight() - paddingY;
		backBufferGraphics.drawString(levelName, paddingX, yPos);
	}

	/**
	 * Draws an achievement pop-up message on the screen.
	 */
	public void drawAchievementPopup(final Screen screen, final String text) {
		int popupWidth = 250;
		int popupHeight = 50;
		int x = screen.getWidth() / 2 - popupWidth / 2;
		int y = 80;
		backBufferGraphics.setColor(new Color(0, 0, 0, 200));
		backBufferGraphics.fillRoundRect(x, y, popupWidth, popupHeight, 15, 15);
		backBufferGraphics.setColor(Color.YELLOW);
		backBufferGraphics.drawRoundRect(x, y, popupWidth, popupHeight, 15, 15);
		backBufferGraphics.setFont(fontRegular);
		backBufferGraphics.setColor(Color.WHITE);
		drawCenteredRegularString(screen, text, y + popupHeight / 2 + 5);
	}

	/**
	 * Draws a notification popup for changes in health.
	 */
	public void drawHealthPopup(final Screen screen, final String text) {
		int popupWidth = 250;
		int popupHeight = 40;
		int x = screen.getWidth() / 2 - popupWidth / 2;
		int y = 100;
		backBufferGraphics.setColor(new Color(0, 0, 0, 200));
		backBufferGraphics.fillRoundRect(x, y, popupWidth, popupHeight, 15, 15);
		Color textColor;
		if (text.startsWith("+")) {
			textColor = new Color(50, 255, 50);
		} else {
			textColor = new Color(255, 50, 50);
		}
		backBufferGraphics.setColor(textColor);
		drawCenteredBigString(screen, text, y + popupHeight / 2 + 5);
	}

	/**
	 * Draws a thick line from side to side of the screen.
	 */
	public void drawHorizontalLine(final Screen screen, final int positionY) {
		backBufferGraphics.setColor(Color.GREEN);
		backBufferGraphics.drawLine(0, positionY, screen.getWidth(), positionY);
		backBufferGraphics.drawLine(0, positionY + 1, screen.getWidth(), positionY + 1);
	}

	/**
	 * Draws game title.
	 */
	public void drawTitle(final Screen screen) {
		String titleString = "Invaders";
		String instructionsString = "select with w+s / arrows, confirm with space";
		backBufferGraphics.setColor(Color.GRAY);
		drawCenteredRegularString(screen, instructionsString, screen.getHeight() / 2);
		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredBigString(screen, titleString, screen.getHeight() / 3);
	}

	/**
	 * Draws the True Final Boss
	 */
	public void drawTrueFinalBoss(screen.Screen screen, entity.TrueFinalBoss boss) {
		if (boss.getSprite() != null) {
			// Central alignment coordinate calculation
			int offsetX = (boss.getDrawWidth() - boss.getWidth()) / 2;
			int offsetY = (boss.getDrawHeight() - boss.getHeight()) / 2;

			int drawX = boss.getPositionX() - offsetX;
			int drawY = boss.getPositionY() - offsetY;

			// Draw image
			backBufferGraphics.drawImage(
					boss.getSprite(),
					drawX,
					drawY,
					boss.getDrawWidth(),
					boss.getDrawHeight(),
					null
			);
		} else {
			// Fallback if no sprite (Black rectangle)
			backBufferGraphics.setColor(Color.BLACK);
			backBufferGraphics.fillRect(boss.getPositionX(), boss.getPositionY(), boss.getWidth(), boss.getHeight());
		}

		// [Draw Laser Warning Lines]
		if (boss.isLaserWarningActive()) {
			for (float angle : boss.getWarningAngles()) {
				drawLaserWarningLine(boss.getWarningOriginX(), boss.getWarningOriginY(), angle, Color.RED);
			}
		}

		// [Draw UI] Health Bar & Posture Bar
		if (screen != null) {
			drawBossHealthBar(screen, boss.getHealPoint(), boss.getMaxHealth());
			// Posture Bar (Orange)
			drawBossPostureBar(screen, boss.getPosture(), boss.getMaxPosture());

			// Deathblow Marker
			if (boss.isPostureBroken()) {
				drawDeathblowMarker(screen, boss.getPositionX() + boss.getWidth()/2, boss.getPositionY() + boss.getHeight()/2);
			}
		}
	}

	/**
	 * Draws main menu.
	 */
	public void drawMenu(final Screen screen, final int option) {
		String playString = "Play";
		String highScoresString = "High scores";
		String achievementsString = "Achievements";
        String settingsString = "Settings";
		String shopString = "Shop";
		String exitString = "Exit";

		// Pulsing color for selected item
		float pulse = (float) ((Math.sin(System.currentTimeMillis() / 200.0) + 1.0) / 2.0);
		Color pulseColor = new Color(0, 0.5f + pulse * 0.5f, 0);

		if (option == 2) backBufferGraphics.setColor(pulseColor);
		else backBufferGraphics.setColor(Color.WHITE);
		drawCenteredRegularString(screen, playString, screen.getHeight() / 3 * 2);

		if (option == 3) backBufferGraphics.setColor(pulseColor);
		else backBufferGraphics.setColor(Color.WHITE);
		drawCenteredRegularString(screen, highScoresString, screen.getHeight() / 3 * 2 + fontRegularMetrics.getHeight() * 1);

		if (option == 6) backBufferGraphics.setColor(pulseColor);
		else backBufferGraphics.setColor(Color.WHITE);
		drawCenteredRegularString(screen, achievementsString, screen.getHeight() / 3 * 2 + fontRegularMetrics.getHeight() * 2);

        if (option == 7) backBufferGraphics.setColor(pulseColor);
        else backBufferGraphics.setColor(Color.WHITE);
        drawCenteredRegularString(screen, settingsString, screen.getHeight() / 3 * 2 + fontRegularMetrics.getHeight() * 3);

		if (option == 4) backBufferGraphics.setColor(pulseColor);
		else backBufferGraphics.setColor(Color.WHITE);
		drawCenteredRegularString(screen, shopString, screen.getHeight() / 3 * 2 + fontRegularMetrics.getHeight() * 4);

		if (option == 0) backBufferGraphics.setColor(pulseColor);
		else backBufferGraphics.setColor(Color.WHITE);
		drawCenteredRegularString(screen, exitString, screen.getHeight() / 3 * 2 + fontRegularMetrics.getHeight() * 5);
	}

	public void drawModernSettings(final SettingsScreen screen, final SettingsScreen.EScreenState screenState, final int mainSelection, final int keyBindingSelection, final GameSettingsManager settingsManager, final boolean isRebinding, final int currentPlayer) {
		// Enable anti-aliasing for smoother graphics
		Graphics2D g2d = (Graphics2D) backBufferGraphics;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	
		// Draw Title
		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredBigString(screen, "Settings", screen.getHeight() / 8);
	
		// Draw main menu items
		String[] mainMenuItems = screen.getMainMenuItems();
		int initialY = screen.getHeight() / 4;
		int yStep = 60; // Increased step for more space overall
		int extraSpaceAfterVolume = 30; // The specific gap
	
		for (int i = 0; i < mainMenuItems.length; i++) {
			int currentY = initialY + i * yStep;
			if (i >= 1) { // Apply space for items after "Volume"
				currentY += extraSpaceAfterVolume;
			}
			
			boolean isSelected = (i == mainSelection);
			Color color = isSelected ? Color.GREEN : Color.WHITE;
			
			if (isSelected && screenState == SettingsScreen.EScreenState.MAIN_SELECTION) {
				float pulse = (float) ((Math.sin(System.currentTimeMillis() / 200.0) + 1.0) / 2.0);
				color = new Color(0, 0.5f + pulse * 0.5f, 0);
			}
			
			backBufferGraphics.setColor(color);
			backBufferGraphics.setFont(fontBig);
			// Center align the text as requested
			drawCenteredBigString(screen, mainMenuItems[i], currentY);
	
			if (i == 0) { // Volume
				// The volume slider is now drawn below the text
				drawVolumeSlider(screen, settingsManager.getVolume(), currentY, isSelected || screenState == SettingsScreen.EScreenState.VOLUME_ADJUST);
			}
		}
	
		// Handle sub-menus (Key Bindings)
		if (screenState == SettingsScreen.EScreenState.KEY_BINDINGS) {
			drawDimOverlay(screen);
			drawKeyBindingPanel(screen, keyBindingSelection, settingsManager, isRebinding, currentPlayer, screen.getKeyBindingActions());
		}
	
		if (isRebinding) {
			drawRebindingOverlay(screen);
		}
		
		// Draw footer instructions
		drawFooterInstructions(screen, screenState);
	}
	
	private void drawVolumeSlider(final Screen screen, float volume, int y, boolean isActive) {
		int barWidth = 150;
		int barHeight = 8;
		int barX = screen.getWidth() / 2 - barWidth / 2; // Centered
		int barY = y + 30; // Positioned below the "Volume" text
		
		// Draw bar background
		backBufferGraphics.setColor(Color.DARK_GRAY);
		backBufferGraphics.fillRoundRect(barX, barY, barWidth, barHeight, 8, 8);
	
		// Draw filled portion of the bar
		backBufferGraphics.setColor(isActive ? Color.GREEN : new Color(0, 150, 0));
		backBufferGraphics.fillRoundRect(barX, barY, (int) (barWidth * volume), barHeight, 8, 8);
	
		// Draw knob
		int knobSize = 16;
		int knobX = barX + (int) (barWidth * volume) - knobSize / 2;
		int knobY = barY + barHeight / 2 - knobSize / 2;
		backBufferGraphics.setColor(isActive ? Color.WHITE : Color.LIGHT_GRAY);
		backBufferGraphics.fillOval(knobX, knobY, knobSize, knobSize);
	
		// Draw percentage
		backBufferGraphics.setFont(fontRegular);
		backBufferGraphics.setColor(isActive ? Color.WHITE : Color.GRAY);
		String volPercent = String.format("%d%%", (int) (volume * 100));
		// Position percentage above the bar
		backBufferGraphics.drawString(volPercent, barX + barWidth / 2 - fontRegularMetrics.stringWidth(volPercent) / 2, barY - 10);
	}
	
	private void drawKeyBindingPanel(final Screen screen, final int selection, final GameSettingsManager settingsManager, final boolean isRebinding, final int player, final String[] actions) {
		int panelWidth = 380;
		int panelHeight = 280;
		int panelX = (screen.getWidth() - panelWidth) / 2;
		int panelY = (screen.getHeight() - panelHeight) / 2;
	
		// Draw panel background
		backBufferGraphics.setColor(new Color(20, 20, 30, 240));
		backBufferGraphics.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 15, 15);
		backBufferGraphics.setColor(Color.GREEN);
		backBufferGraphics.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 15, 15);
	
		// Panel Title
		backBufferGraphics.setFont(fontBig);
		String title = "Player " + player + " Controls";
		int titleWidth = fontBigMetrics.stringWidth(title);
		backBufferGraphics.drawString(title, panelX + (panelWidth - titleWidth) / 2, panelY + 40);
	
		// Draw key bindings list
		int listY = panelY + 80;
		int yStep = 35;
		backBufferGraphics.setFont(fontRegular);
	
		for (int i = 0; i < actions.length; i++) {
			boolean isSelected = (i == selection);
			Color color = isSelected ? Color.GREEN : Color.WHITE;
			
			if (isSelected) {
				float pulse = (float) ((Math.sin(System.currentTimeMillis() / 200.0) + 1.0) / 2.0);
				color = new Color(0.5f, 0.8f + pulse * 0.2f, 0.5f);
			}
	
			backBufferGraphics.setColor(color);
			String action = actions[i];
			String key = (player == 1) ? KeyEvent.getKeyText(settingsManager.getKey(action)) : KeyEvent.getKeyText(settingsManager.getKeyP2(action));
			
			backBufferGraphics.drawString(action, panelX + 40, listY + i * yStep);
			backBufferGraphics.drawString(key, panelX + panelWidth - 100, listY + i * yStep);
		}
	}
	
	private void drawRebindingOverlay(final Screen screen) {
		drawDimOverlay(screen);
		backBufferGraphics.setFont(fontBig);
		backBufferGraphics.setColor(Color.YELLOW);
		drawCenteredBigString(screen, "Press any key to bind...", screen.getHeight() / 2);
	}
	
	private void drawDimOverlay(final Screen screen) {
		backBufferGraphics.setColor(new Color(0, 0, 0, 180));
		backBufferGraphics.fillRect(0, 0, screen.getWidth(), screen.getHeight()); 
	}
	
	private void drawFooterInstructions(final Screen screen, final screen.SettingsScreen.EScreenState state) {
		backBufferGraphics.setFont(fontRegular);
		backBufferGraphics.setColor(Color.GRAY);
		String instructions = "";
		switch (state) {
			case MAIN_SELECTION:
				instructions = "UP/DOWN: Navigate | SHOOT: Select";
				break;
			case VOLUME_ADJUST:
				instructions = "LEFT/RIGHT: Adjust Volume | SHOOT/ESC: Back";
				break;
			case KEY_BINDINGS:
				instructions = "UP/DOWN: Navigate | SHOOT: Rebind | ESC: Back";
				break;
		}
		drawCenteredRegularString(screen, instructions, screen.getHeight() - 30);
	}

    // Helper method to draw a centered string within a given width
    private void drawCenteredRegularString(final Screen screen, final String string, final int height, final int startX, final int width) {
        backBufferGraphics.setFont(fontRegular);
        backBufferGraphics.drawString(string, startX + width / 2 - fontRegularMetrics.stringWidth(string) / 2, height);
    }

	/**
	 * Draws game results.
	 */
	public void drawResults(final Screen screen, final int score, final int livesRemaining, final int shipsDestroyed, final float accuracy, final boolean isNewRecord) {
		String scoreString = String.format("score %04d", score);
		String livesRemainingString = "lives remaining " + livesRemaining;
		String shipsDestroyedString = "enemies destroyed " + shipsDestroyed;
		String accuracyString = String.format("accuracy %.2f%%", accuracy * 100);
		int height = isNewRecord ? 4 : 2;
		backBufferGraphics.setColor(Color.WHITE);
		drawCenteredRegularString(screen, scoreString, screen.getHeight() / height);
		drawCenteredRegularString(screen, livesRemainingString, screen.getHeight() / height + fontRegularMetrics.getHeight() * 2);
		drawCenteredRegularString(screen, shipsDestroyedString, screen.getHeight() / height + fontRegularMetrics.getHeight() * 4);
		drawCenteredRegularString(screen, accuracyString, screen.getHeight() / height + fontRegularMetrics.getHeight() * 6);
	}

	/**
	 * Draws interactive characters for name input.
	 */
	public void drawNameInput(final Screen screen, final char[] name, final int nameCharSelected) {
		String newRecordString = "New Record!";
		String introduceNameString = "Introduce name:";
		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredRegularString(screen, newRecordString, screen.getHeight() / 4 + fontRegularMetrics.getHeight() * 10);
		backBufferGraphics.setColor(Color.WHITE);
		drawCenteredRegularString(screen, introduceNameString, screen.getHeight() / 4 + fontRegularMetrics.getHeight() * 12);

		int positionX = screen.getWidth() / 2 - (fontRegularMetrics.getWidths()[name[0]] + fontRegularMetrics.getWidths()[name[1]] + fontRegularMetrics.getWidths()[name[2]] + fontRegularMetrics.getWidths()[' ']) / 2;

		for (int i = 0; i < 3; i++) {
			if (i == nameCharSelected) backBufferGraphics.setColor(Color.GREEN);
			else backBufferGraphics.setColor(Color.WHITE);
			positionX += fontRegularMetrics.getWidths()[name[i]] / 2;
			positionX = i == 0 ? positionX : positionX + (fontRegularMetrics.getWidths()[name[i - 1]] + fontRegularMetrics.getWidths()[' ']) / 2;
			backBufferGraphics.drawString(Character.toString(name[i]), positionX, screen.getHeight() / 4 + fontRegularMetrics.getHeight() * 14);
		}
	}

	/**
	 * Draws basic content of game over screen.
	 */
	public void drawGameOver(final Screen screen, final boolean acceptsInput, final boolean isNewRecord) {
		String gameOverString = "Game Over";
		String continueOrExitString = "Press Space to play again, Escape to exit";
		int height = isNewRecord ? 4 : 2;
		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredBigString(screen, gameOverString, screen.getHeight() / height - fontBigMetrics.getHeight() * 2);
		if (acceptsInput) backBufferGraphics.setColor(Color.GREEN);
		else backBufferGraphics.setColor(Color.GRAY);
		drawCenteredRegularString(screen, continueOrExitString, screen.getHeight() / 2 + fontRegularMetrics.getHeight() * 10);
	}

	/**
	 * Draws high score screen title and instructions.
	 */
	public void drawHighScoreMenu(final Screen screen) {
		String highScoreString = "High Scores";
		String instructionsString = "Press Space to return";
		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredBigString(screen, highScoreString, screen.getHeight() / 8);
		backBufferGraphics.setColor(Color.GRAY);
		drawCenteredRegularString(screen, instructionsString, screen.getHeight() / 5);
	}

	/**
	 * Draws high scores.
	 */
	public void drawHighScores(final Screen screen, final List<Score> highScores) {
		backBufferGraphics.setColor(Color.WHITE);
		int i = 0;
		String scoreString = "";
		for (Score score : highScores) {
			scoreString = String.format("%s        %04d", score.getName(), score.getScore());
			drawCenteredRegularString(screen, scoreString, screen.getHeight() / 4 + fontRegularMetrics.getHeight() * (i + 1) * 2);
			i++;
		}
	}

	public void drawAchievements(final Screen screen, final List<Achievement> achievements) {
		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredBigString(screen, "Achievements", screen.getHeight() / 8);
		int i = 0;
		for (Achievement achievement : achievements) {
			if (achievement.isUnlocked()) {
				backBufferGraphics.setColor(Color.GREEN);
			} else {
				backBufferGraphics.setColor(Color.WHITE);
			}
			drawCenteredRegularString(screen, achievement.getName() + " - " + achievement.getDescription(), screen.getHeight() / 5 + fontRegularMetrics.getHeight() * (i + 1) * 2);
			i++;
		}
		backBufferGraphics.setColor(Color.GRAY);
		drawCenteredRegularString(screen, "Press ESC to return", screen.getHeight() - 50);
	}

	/**
	 * Draws the credits screen title and instructions.
	 */
	public void drawCreditsMenu(final Screen screen) {
		String creditsString = "Credits";
		String instructionsString = "Press Space to return";
		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredBigString(screen, creditsString, screen.getHeight() / 8);
		backBufferGraphics.setColor(Color.GRAY);
		drawCenteredRegularString(screen, instructionsString, screen.getHeight() / 5);
	}

	/**
	 * Draws the list of credits on the screen.
	 */
	public void drawCredits(final Screen screen, final List<CreditScreen.Credit> creditList) {
		backBufferGraphics.setFont(fontSmall);
		int yPosition = screen.getHeight() / 4;
		final int xPosition = screen.getWidth() / 10;
		final int lineSpacing = fontSmallMetrics.getHeight() + 1;
		final int teamSpacing = lineSpacing + 5;
		for (CreditScreen.Credit credit : creditList) {
			backBufferGraphics.setColor(Color.GREEN);
			String teamInfo = String.format("%s - %s", credit.getTeamName(), credit.getRole());
			backBufferGraphics.drawString(teamInfo, xPosition, yPosition);
			yPosition += lineSpacing;
			yPosition += teamSpacing;
		}
	}

	/**
	 * Draws a centered string on regular font.
	 */
	public void drawCenteredRegularString(final Screen screen, final String string, final int height) {
		backBufferGraphics.setFont(fontRegular);
		backBufferGraphics.drawString(string, screen.getWidth() / 2 - fontRegularMetrics.stringWidth(string) / 2, height);
	}

	/**
	 * Draws a centered string on big font.
	 */
	public void drawCenteredBigString(final Screen screen, final String string, final int height) {
		backBufferGraphics.setFont(fontBig);
		backBufferGraphics.drawString(string, screen.getWidth() / 2 - fontBigMetrics.stringWidth(string) / 2, height);
	}

	/**
	 * Countdown to game start.
	 */
	public void drawCountDown(final Screen screen, final int level, final int number, final boolean bonusLife) {
		int rectWidth = screen.getWidth();
		int rectHeight = screen.getHeight() / 6;
		backBufferGraphics.setColor(Color.BLACK);
		backBufferGraphics.fillRect(0, screen.getHeight() / 2 - rectHeight / 2, rectWidth, rectHeight);
		backBufferGraphics.setColor(Color.GREEN);
		if (number >= 4) {
			if (!bonusLife) {
				drawCenteredBigString(screen, "Level " + level, screen.getHeight() / 2 + fontBigMetrics.getHeight() / 3);
			} else {
				drawCenteredBigString(screen, "Level " + level + " - Bonus life!", screen.getHeight() / 2 + fontBigMetrics.getHeight() / 3);
			}
		} else if (number != 0) {
			drawCenteredBigString(screen, Integer.toString(number), screen.getHeight() / 2 + fontBigMetrics.getHeight() / 3);
		} else {
			drawCenteredBigString(screen, "GO!", screen.getHeight() / 2 + fontBigMetrics.getHeight() / 3);
		}
	}

	/**
	 * Draws the complete shop screen with all items and levels.
	 */
	public void drawShopScreen(final Screen screen, final int coinBalance, final int selectedItem, final int selectionMode, final int selectedLevel, final int totalItems, final String[] itemNames, final String[] itemDescriptions, final int[][] itemPrices, final int[] maxLevels, final screen.ShopScreen shopScreen) {
		// Draw title
		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredBigString(screen, "SHOP", screen.getHeight() / 8);
		// Draw coin balance
		backBufferGraphics.setColor(Color.YELLOW);
		String balanceString = String.format("Your Balance: %d coins", coinBalance);
		drawCenteredRegularString(screen, balanceString, 120);
		// Draw instructions based on mode
		backBufferGraphics.setColor(Color.GRAY);
		String instructions = "";
		if (selectionMode == 0) {
			instructions = "W/S: Navigate | SPACE: Select | ESC: Exit";
		} else {
			instructions = "A/D: Change Level | SPACE: Buy | ESC: Back";
		}
		drawCenteredRegularString(screen, instructions, 145);

		int headerHeight = 165;
		int footerHeight = 50;
		int availableHeight = screen.getHeight() - headerHeight - footerHeight;

		int currentY = 170;
		int baseSpacing = 58;
		int expandedExtraSpace = 55;

		boolean hasExpandedItem = (selectionMode == 1);

		int totalRequiredHeight = totalItems * baseSpacing;
		if (hasExpandedItem) {
			totalRequiredHeight += expandedExtraSpace;
		}

		int adjustedSpacing = baseSpacing;
		if (totalRequiredHeight > availableHeight) {
			int overflow = totalRequiredHeight - availableHeight;
			adjustedSpacing = baseSpacing - (overflow / totalItems);
			if (adjustedSpacing < 48) {
				adjustedSpacing = 48;
			}
		}

		for (int i = 0; i < totalItems; i++) {
			boolean isSelected = (i == selectedItem) && (selectionMode == 0);
			boolean isLevelSelection = (i == selectedItem && selectionMode == 1);
			int currentLevel = shopScreen.getItemCurrentLevel(i);
			drawShopItem(screen, itemNames[i], itemDescriptions[i], itemPrices[i], maxLevels[i], currentLevel, currentY, isSelected, coinBalance, isLevelSelection, selectedLevel);
			if (isLevelSelection) {
				currentY += adjustedSpacing + expandedExtraSpace;
			} else {
				currentY += adjustedSpacing;
			}
		}

		int exitY = screen.getHeight() - 30;
		if (selectedItem == totalItems && selectionMode == 0) {
			backBufferGraphics.setColor(Color.GREEN);
		} else {
			backBufferGraphics.setColor(Color.WHITE);
		}

		if (shopScreen.betweenLevels) {
			drawCenteredRegularString(screen, "< Back to Game >", exitY);
		} else {
			drawCenteredRegularString(screen, "< Back to Main Menu >", exitY);
		}
	}

	/**
	 * Draws a single shop item with level indicators.
	 */
	public void drawShopItem(final Screen screen, final String itemName, final String description, final int[] prices, final int maxLevel, final int currentLevel, final int yPosition, final boolean isSelected, final int playerCoins, final boolean isLevelSelection, final int selectedLevel) {
		if (isSelected || isLevelSelection) {
			backBufferGraphics.setColor(Color.GREEN);
		} else {
			backBufferGraphics.setColor(Color.WHITE);
		}
		String levelInfo = currentLevel > 0 ? " [Lv." + currentLevel + "/" + maxLevel + "]" : " [Not Owned]";
		backBufferGraphics.setFont(fontRegular);
		backBufferGraphics.drawString(itemName + levelInfo, 30, yPosition);

		if (isSelected || isLevelSelection) {
			backBufferGraphics.setColor(Color.GRAY);
			backBufferGraphics.drawString(description, 30, yPosition + 15);
		}

		if (isLevelSelection) {
			int levelStartX = 30;
			int levelY = yPosition + 35;
			int maxWidth = screen.getWidth() - 60;
			int currX = levelStartX;
			int currY = levelY;
			int spaceBetween = 18;

			for (int lvl = 1; lvl <= maxLevel; lvl++) {
				int price = prices[lvl - 1];
				boolean canAfford = playerCoins >= price;
				boolean isOwned = currentLevel >= lvl;
				boolean isThisLevel = (lvl == selectedLevel);

				if (isOwned) {
					backBufferGraphics.setColor(Color.DARK_GRAY);
				} else if (isThisLevel) {
					backBufferGraphics.setColor(Color.GREEN);
				} else if (canAfford) {
					backBufferGraphics.setColor(Color.WHITE);
				} else {
					backBufferGraphics.setColor(Color.RED);
				}
				String levelText = "Lv." + lvl + (isOwned ? " [OWNED]" : " (" + price + "$)");
				int textWidth = fontRegularMetrics.stringWidth(levelText);

				if (currX + textWidth > levelStartX + maxWidth) {
					currX = levelStartX;
					currY += fontRegularMetrics.getHeight() + 3;
				}
				backBufferGraphics.drawString(levelText, currX, currY);
				currX += textWidth + spaceBetween;
			}
		}
	}

	/**
	 * Draws purchase feedback message.
	 */
	public void drawShopFeedback(final Screen screen, final String message) {
		int popupWidth = 300;
		int popupHeight = 50;
		int x = screen.getWidth() / 2 - popupWidth / 2;
		int y = 70;

		backBufferGraphics.setColor(new Color(0, 0, 0, 200));
		backBufferGraphics.fillRoundRect(x, y, popupWidth, popupHeight, 15, 15);

		if (message.contains("Purchased")) {
			backBufferGraphics.setColor(Color.GREEN);
		} else if (message.contains("Not enough") || message.contains("failed")) {
			backBufferGraphics.setColor(Color.RED);
		} else {
			backBufferGraphics.setColor(Color.YELLOW);
		}
		backBufferGraphics.drawRoundRect(x, y, popupWidth, popupHeight, 15, 15);

		backBufferGraphics.setFont(fontRegular);
		drawCenteredRegularString(screen, message, y + popupHeight / 2 + 5);
	}
	public void drawStars(final Screen screen, final List<Star> stars, final float angle) {
		final int centerX = screen.getWidth() / 2;
		final int centerY = screen.getHeight() / 2;
		final double angleRad = Math.toRadians(angle);
		final double cosAngle = Math.cos(angleRad);
		final double sinAngle = Math.sin(angleRad);

		for (Star star : stars) {
			float relX = star.baseX - centerX;
			float relY = star.baseY - centerY;

			double rotatedX = relX * cosAngle - relY * sinAngle;
			double rotatedY = relX  * sinAngle + relY * cosAngle;

			int screenX = (int) (rotatedX + centerX);
			int screenY = (int) (rotatedY + centerY);

			// Use star's brightness to set its color for twinkling effect
			float b = star.brightness;
			if (b < 0) b = 0;
			if (b > 1) b = 1;
			backBufferGraphics.setColor(new Color(b, b, b));
			backBufferGraphics.drawRect(screenX, screenY, 1, 1);
		}
	}

	public void drawShootingStars(final Screen screen, final List<ShootingStar> shootingStars, final float angle) {    }
	public void drawModeSelectMenu(final screen.Screen screen, final int selection) {
		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredBigString(screen, "SELECT  PLAYER  MODE", 160);

		float pulse = (float) ((Math.sin(System.currentTimeMillis() / 200.0) + 1.0) / 2.0);
		Color pulseColor = new Color(0, 0.5f + pulse * 0.5f, 0);

		if (selection == 0) backBufferGraphics.setColor(pulseColor);
		else backBufferGraphics.setColor(Color.WHITE);
		drawCenteredRegularString(screen, " 1 PLAYER ", 230);

		if (selection == 1) backBufferGraphics.setColor(pulseColor);
		else backBufferGraphics.setColor(Color.WHITE);
		drawCenteredRegularString(screen, " 2 PLAYER ", 270);

		if (selection == 2) backBufferGraphics.setColor(pulseColor);
		else backBufferGraphics.setColor(Color.WHITE);
		drawCenteredRegularString(screen, "< BACK TO MAIN MENU >", 310);

		int y1 = 230;
		int y2 = 270;
		int mid1 = y1 - fontRegularMetrics.getAscent() / 2;
		int mid2 = y2 - fontRegularMetrics.getAscent() / 2;

		int shipW = 13 * 2;
		int shipH = 8 * 2;
		int shipY1 = mid1 - shipH / 2;
		int shipY2 = mid2 - shipH / 2;

		int centerX = screen.getWidth() / 2;
		int textW1 = fontRegularMetrics.stringWidth(" 1 PLAYER ");
		int textW2 = fontRegularMetrics.stringWidth(" 2 PLAYER ");
		int gap = 10;
		int shipGap = 12;

		int textLeft1 = centerX - textW1 / 2;
		int rightEdge1 = textLeft1 - gap;
		int shipX1 = rightEdge1 - shipW;
		drawEntity(new Ship(0, 0, (selection == 0) ? new Color(0,200,0) : Color.DARK_GRAY), shipX1, shipY1);

		int textLeft2 = centerX - textW2 / 2;
		int rightEdge2 = textLeft2 - gap;
		int shipsTotalW = shipW * 2 + shipGap;
		int shipX2a = rightEdge2 - shipsTotalW;
		int shipX2b = shipX2a + shipW + shipGap;
		Color twoPColor = (selection == 1) ? new Color(0,200,0) : Color.DARK_GRAY;
		drawEntity(new Ship(0, 0, twoPColor), shipX2a, shipY2);
		drawEntity(new Ship(0, 0, twoPColor), shipX2b, shipY2);

		backBufferGraphics.setColor(Color.GRAY);
		drawCenteredRegularString(screen, "Press SPACE TO CONFIRM", 370);
	}
	public void drawLaserBeam(LaserBeam beam) {
		Graphics2D g2 = (Graphics2D) backBufferGraphics;

		float alpha = beam.getAlphaFactor();
		if (alpha <= 0f) return;

		// Save state
		Stroke oldStroke = g2.getStroke();
		Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Two-layer beam: soft glow + bright core
		Color glowColor = new Color(0f, 1f, 1f, Math.min(1f, alpha * 0.55f));
		Color coreColor = new Color(0.85f, 1f, 1f, Math.min(1f, alpha * 1.1f));

		float x1 = beam.getOriginX();
		float y1 = beam.getOriginY();

		float angle = beam.getAngle();
		float length = beam.getLength();
		float thickness = beam.getThickness();

		float x2 = (float)(x1 + Math.cos(angle) * length);
		float y2 = (float)(y1 + Math.sin(angle) * length);

		// Outer glow
		g2.setColor(glowColor);
		g2.setStroke(new BasicStroke(thickness * 1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2.draw(new java.awt.geom.Line2D.Float(x1, y1, x2, y2));

		// Inner bright core
		g2.setColor(coreColor);
		g2.setStroke(new BasicStroke(thickness * 0.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2.draw(new java.awt.geom.Line2D.Float(x1, y1, x2, y2));

		// simple glow ring at origin
		g2.setStroke(new BasicStroke(2.5f));
		g2.setColor(new Color(1f, 0.95f, 0.8f, Math.min(1f, alpha * 0.8f)));
		float ringR = thickness * 1.2f;
		g2.drawOval((int) (x1 - ringR), (int) (y1 - ringR), (int) (ringR * 2), (int) (ringR * 2));

		// Restore state
		g2.setStroke(oldStroke);
		if (oldAA != null) {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
		}
	}
	/**
	 * Draw a custom “laser satellite” boss without using the sprite sheet.
	 */
	public void drawLaserBoss(FinalBoss_3 boss) {
		Graphics2D g2 = (Graphics2D) backBufferGraphics;
		Stroke oldStroke = g2.getStroke();
		Object oldAA = enableAntialiasing(g2);

		int cx = boss.getPositionX() + boss.getWidth() / 2;
		int cy = boss.getPositionY() + boss.getHeight() / 2;

		int bodyW = boss.getWidth() - 14;
		int bodyH = boss.getHeight() - 8;
		int rx = bodyW / 2;
		int ry = bodyH / 2;

		drawBossBody(g2, cx, cy, bodyW, bodyH, rx, ry);
		drawBossCore(g2, boss, cx, cy, bodyW, bodyH);
		drawBossLenses(g2, boss, cx, cy, rx, ry);

		restoreGraphicsState(g2, oldStroke, oldAA);
	}

	// [Add] Since Boss4 uses Graphics directly, pass backBufferGraphics through DrawManager
	public void drawBoss4(final entity.Boss4 boss) {
		boss.draw(backBufferGraphics);
	}

	public void drawSamuraiBoss(screen.Screen screen, entity.SamuraiBoss boss) {
		if (boss.getSprite() != null) {

			// 1. Calculate offset for center alignment (hitbox vs image size)
			int offsetX = (boss.getDrawWidth() - boss.getWidth()) / 2;
			int offsetY = (boss.getDrawHeight() - boss.getHeight()) / 2;

			// 2. Calculate actual draw position
			int drawX = boss.getPositionX() - offsetX;
			int drawY = boss.getPositionY() - offsetY;

			// 3. [Fix] Use drawX, drawY instead of offsetX
			backBufferGraphics.drawImage(
					boss.getSprite(),
					drawX,  // <-- Fixed! (Follows boss position)
					drawY,  // <-- Fixed!
					boss.getDrawWidth(),
					boss.getDrawHeight(),
					null
			);
		}

		// 4. Draw Health & Posture bars
		// (Pass screen param to calculate width)
		if (screen != null) {
			drawBossHealthBar(screen, boss.getHealPoint(), boss.getMaxHealth());
			drawBossPostureBar(screen, boss.getPosture(), boss.getMaxPosture());

			if (boss.isPostureBroken()) {
				drawDeathblowMarker(screen,
						boss.getPositionX() + boss.getWidth()/2,
						boss.getPositionY() + boss.getHeight()/2
				);
			}
		}
	}

	private Object enableAntialiasing(Graphics2D g2) {
		Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		return oldAA;
	}

	private void restoreGraphicsState(Graphics2D g2, Stroke oldStroke, Object oldAA) {
		g2.setStroke(oldStroke);
		if (oldAA != null) {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
		}
	}

	private void drawBossBody(Graphics2D g2, int cx, int cy, int bodyW, int bodyH, int rx, int ry) {
		g2.setColor(new Color(28, 30, 40));
		g2.fillOval(cx - rx, cy - ry, bodyW, bodyH);

		g2.setColor(new Color(80, 90, 110));
		g2.setStroke(new BasicStroke(3f));
		g2.drawOval(cx - rx, cy - ry, bodyW, bodyH);

		drawInnerPulseRing(g2, cx, cy, bodyW, bodyH);
	}

	private void drawInnerPulseRing(Graphics2D g2, int cx, int cy, int bodyW, int bodyH) {
		float pulse = (float) ((Math.sin(System.currentTimeMillis() / 220.0) + 1.0) / 2.0);
		int innerW = (int) (bodyW * 0.62f + pulse * 3f);
		int innerH = (int) (bodyH * 0.62f + pulse * 3f);
		g2.setColor(new Color(60, 150, 220, 180));
		g2.setStroke(new BasicStroke(2f));
		g2.drawOval(cx - innerW / 2, cy - innerH / 2, innerW, innerH);
	}

	private void drawBossCore(Graphics2D g2, FinalBoss_3 boss, int cx, int cy, int bodyW, int bodyH) {
		Color coreColor = boss.isBossWaveActive() ? new Color(120, 220, 255) : new Color(90, 200, 240);
		int coreR = (int) (Math.min(bodyW, bodyH) * 0.18f);
		g2.setColor(coreColor);
		g2.fillOval(cx - coreR, cy - coreR, coreR * 2, coreR * 2);
	}

	private void drawBossLenses(Graphics2D g2, FinalBoss_3 boss, int cx, int cy, int rx, int ry) {
		float[] lensAnglesDeg = {0f, 90f, 180f, 270f};
		float lensRadius = Math.min(rx, ry) * 0.78f;
		int lensSize = 12;
		int lensHighlightSize = 16;

		boolean[] highlightLens = buildLensHighlights(boss, lensAnglesDeg);
		boolean forceHighlight = boss.isBossWaveActive();

		for (int i = 0; i < lensAnglesDeg.length; i++) {
			boolean on = forceHighlight || highlightLens[i];
			drawSingleLens(g2, cx, cy, lensAnglesDeg[i], on, lensRadius, lensSize, lensHighlightSize);
		}
	}

	private boolean[] buildLensHighlights(FinalBoss_3 boss, float[] lensAnglesDeg) {
		boolean[] highlightLens = new boolean[lensAnglesDeg.length];
		if (!boss.isLaserWarningActive()) {
			return highlightLens;
		}

		for (float warn : boss.getPendingWarningAngles()) {
			markNearbyLens(warn, lensAnglesDeg, highlightLens);
		}
		return highlightLens;
	}

	private void markNearbyLens(float warn, float[] lensAnglesDeg, boolean[] highlightLens) {
		for (int i = 0; i < lensAnglesDeg.length; i++) {
			if (isAngleWithinThreshold(warn, lensAnglesDeg[i], 30f)) {
				highlightLens[i] = true;
			}
		}
	}

	private boolean isAngleWithinThreshold(float angle, float target, float threshold) {
		float normalized = ((angle % 360) + 360) % 360;
		float diff = Math.abs(normalized - target);
		diff = Math.min(diff, 360 - diff);
		return diff <= threshold;
	}

	private void drawSingleLens(Graphics2D g2, int cx, int cy, float angleDeg, boolean highlight, float lensRadius, int lensSize, int lensHighlightSize) {
		double rad = Math.toRadians(angleDeg);
		int lx = (int) (cx + Math.cos(rad) * lensRadius);
		int ly = (int) (cy + Math.sin(rad) * lensRadius);

		Color lensCore = highlight ? new Color(140, 240, 255) : new Color(70, 150, 210);
		Color lensRing = highlight ? new Color(110, 200, 255) : new Color(60, 110, 160);

		int size = highlight ? lensHighlightSize : lensSize;
		g2.setColor(lensRing);
		g2.fillOval(lx - size / 2, ly - size / 2, size, size);

		g2.setColor(lensCore);
		g2.fillOval(lx - (size - 6) / 2, ly - (size - 6) / 2, size - 6, size - 6);
	}
	public void drawLaserWarningLine(float originX, float originY, float angle) {
		drawLaserWarningLine(originX, originY, angle, new Color(0, 255, 255, 180));
	}
	public void drawLaserWarningLine(float originX, float originY, float angle, Color color) {
		Graphics2D g2 = (Graphics2D) backBufferGraphics;

		float[] dashPattern = {6f, 6f};
		g2.setStroke(new BasicStroke(
				2f,
				BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND,
				10f,
				dashPattern,
				0f
		));

		g2.setColor(color);
		float length = 2000f;
		float rad = (float) Math.toRadians(angle);

		float x2 = (float) (originX + Math.cos(rad) * length);
		float y2 = (float) (originY + Math.sin(rad) * length);

		g2.drawLine((int)originX, (int)originY, (int)x2, (int)y2);
		// Stroke initialization
		g2.setStroke(new BasicStroke(1f));
	}
	/**
	 * Draws a simple colored rectangle (useful for UI, Minimap).
	 */
	public void drawRectangle(final int x, final int y, final int width, final int height, final Color color) {
		backBufferGraphics.setColor(color);
		backBufferGraphics.fillRect(x, y, width, height);
		// Uncomment below to draw border
		backBufferGraphics.setColor(Color.WHITE);
		backBufferGraphics.drawRect(x, y, width, height);
	}

	public void drawGameModeSelection(final screen.Screen screen, final int selection) {
		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredBigString(screen, "SELECT  GAME  MODE", 160);

		float pulse = (float) ((Math.sin(System.currentTimeMillis() / 200.0) + 1.0) / 2.0);
		Color pulseColor = new Color(0, 0.5f + pulse * 0.5f, 0);

		if (selection == 0) backBufferGraphics.setColor(pulseColor);
		else backBufferGraphics.setColor(Color.WHITE);
		drawCenteredRegularString(screen, " CLASSIC MODE ", 230);

		if (selection == 1) backBufferGraphics.setColor(pulseColor);
		else backBufferGraphics.setColor(Color.WHITE);
		drawCenteredRegularString(screen, " SANDBOX MODE ", 270);

		if (selection == 2) backBufferGraphics.setColor(pulseColor);
		else backBufferGraphics.setColor(Color.WHITE);
		drawCenteredRegularString(screen, "< BACK TO MAIN MENU >", 310);

		int y1 = 230;
		int y2 = 270;
		int mid1 = y1 - fontRegularMetrics.getAscent() / 2;
		int mid2 = y2 - fontRegularMetrics.getAscent() / 2;


		int centerX = screen.getWidth() / 2;
		int textW1 = fontRegularMetrics.stringWidth(" 1 PLAYER ");
		int textW2 = fontRegularMetrics.stringWidth(" 2 PLAYER ");
		int gap = 10;
		int shipGap = 12;

		int textLeft1 = centerX - textW1 / 2;

		int textLeft2 = centerX - textW2 / 2;

		backBufferGraphics.setColor(Color.GRAY);
		drawCenteredRegularString(screen, "Press SPACE TO CONFIRM", 370);
	}

	public void drawPauseOverlay(final screen.Screen screen) {
		backBufferGraphics.setColor(new java.awt.Color(0, 0, 0, 150));
		backBufferGraphics.fillRect(0, 0, screen.getWidth(), screen.getHeight());
	}
	public void drawPauseMenu(final screen.Screen screen, final int menuIndex) {
		String[] menu = { "Quit Game", "Restart", "Return" };

		backBufferGraphics.setFont(fontBig);

		// Position menu slightly above center.
		int centerX = screen.getWidth() / 2;
		int baseCenterY = screen.getHeight() / 2 - 40;

		// Simple drop-in animation: reset progress if menu has been closed for a bit.
		long now = System.currentTimeMillis();
		if (now - lastPauseMenuDrawTime > 300) {
			pauseMenuAnimProgress = 0f;
		}
		pauseMenuAnimProgress = Math.min(1f, pauseMenuAnimProgress + 0.12f);
		lastPauseMenuDrawTime = now;
		// Start higher and ease to the target.
		int centerY = baseCenterY + (int) ((1f - pauseMenuAnimProgress) * -40);

		// Measure widest text to size the container.
		int maxTextWidth = 0;
		for (String text : menu) {
			maxTextWidth = Math.max(maxTextWidth, fontBigMetrics.stringWidth(text));
		}

		int lineHeight = fontBigMetrics.getHeight();
		int itemSpacing = 40;
		int boxPadding = 24;
		int boxWidth = maxTextWidth + boxPadding * 2;
		int boxHeight = itemSpacing * menu.length + boxPadding;

		int boxX = centerX - boxWidth / 2;
		int boxY = centerY - boxHeight / 2;

		// Draw container with semi-transparent fill and border.
		backBufferGraphics.setColor(new java.awt.Color(0, 0, 0, 180));
		backBufferGraphics.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 16, 16);
		// Border uses game accent green.
		backBufferGraphics.setColor(new java.awt.Color(0, 200, 70, 200));
		backBufferGraphics.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 16, 16);

		for (int i = 0; i < menu.length; i++) {
			if (i == menuIndex)
				backBufferGraphics.setColor(java.awt.Color.YELLOW);
			else
				backBufferGraphics.setColor(java.awt.Color.WHITE);

			String text = menu[i];
			int width = fontBigMetrics.stringWidth(text);
			int y = boxY + boxPadding + (i * itemSpacing) + lineHeight / 2;

			backBufferGraphics.drawString(text, centerX - width / 2, y);
		}
	}

}