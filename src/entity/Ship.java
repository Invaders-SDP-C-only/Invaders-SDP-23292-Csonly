package entity;
import audio.SoundManager;

import java.awt.Color;
import java.util.Set;

import engine.Cooldown;
import engine.Core;
import engine.DrawManager.SpriteType;

/**
 * Implements a ship, to be controlled by the player.
 * * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * */
public class Ship extends Entity {

	/** Time between shots. */
	private static final int SHOOTING_INTERVAL = 750;
	/** Speed of the bullets shot by the ship. */
	private static final int BULLET_SPEED = -6;
	/** Movement of the ship for each unit of time. */
	private static final int SPEED = 2;

	/** Minimum time between shots. */
	private Cooldown shootingCooldown;
	/** Time spent inactive between hits. */
	private Cooldown destructionCooldown;
	/** Cooldown for the invincibility shield. */
	private Cooldown shieldCooldown;
	/** Checks if the ship is invincible. */
	private boolean isInvincible;

	/** * Player Identifier.
	 * 1 for Player 1, 2 for Player 2.
	 * Default is 1 for single-player compatibility.
	 */
	private int playerId = 1;

	/** Flag for Melee Mode (Sword Only). */
	private boolean isMeleeMode = false;
	/** Flag for Hybrid Mode (Sword + Gun). */
	private boolean isHybridMode = false;
	/** Cooldown for parrying action. */
	private Cooldown parryCooldown;
	/** Flag indicating if the player is currently parrying. */
	private boolean isParrying = false;
	/** Visual effect entity for sword slash. */
	private Entity swordSlashEffect;
	/** Base color of the ship (used to restore color after effects). */
	private Color baseColor;
	/** Duration of the active parry window. */
	private Cooldown parryWindow;
	/** Flag for Cheat Mode (Rapid fire, etc.). */
	private boolean isCheatMode = false;

	/**
	 * Sets the player ID for this ship.
	 * @param pid Player ID (1 or 2).
	 */
	public void setPlayerId(int pid) { this.playerId = pid; }

	/**
	 * Returns the player ID.
	 * @return Player ID.
	 */
	public int getPlayerId() { return this.playerId; }

	/**
	 * Constructor, establishes the ship's properties.
	 * * @param positionX
	 * Initial position of the ship in the X axis.
	 * @param positionY
	 * Initial position of the ship in the Y axis.
	 * @param color
	 * Color of the ship.
	 */
	public Ship(final int positionX, final int positionY, final Color color) {
		super(positionX, positionY, 13 * 2, 8 * 2, color);

		this.spriteType = SpriteType.Ship;
		this.shootingCooldown = Core.getCooldown(ShopItem.getShootingInterval());
		this.destructionCooldown = Core.getCooldown(1000);
		this.shieldCooldown = Core.getCooldown(0);
		this.isInvincible = false;
		this.parryCooldown = Core.getCooldown(300);
		this.swordSlashEffect = new Entity(positionX, positionY, 16*2, 16*2, Color.WHITE);
		this.swordSlashEffect.spriteType = SpriteType.SwordSlashEffect;
		this.baseColor = color;
		this.parryWindow = Core.getCooldown(250);
	}

	/**
	 * Moves the ship speed units right, or until the right screen border is
	 * reached.
	 */
	public final void moveRight() {
		int shipspeed = ShopItem.getSHIPSpeedCOUNT();
		this.positionX += SPEED*(1+shipspeed/10);
	}

	/**
	 * Moves the ship speed units left, or until the left screen border is
	 * reached.
	 */
	public final void moveLeft() {
		int shipspeed = ShopItem.getSHIPSpeedCOUNT();
		this.positionX -= SPEED*(1+shipspeed/10);
	}

	/**
	 * Moves the ship speed units up, or until the SEPARATION_LINE_HEIGHT is
	 * reached.
	 */
	public final void moveUp() {
		int shipspeed = ShopItem.getSHIPSpeedCOUNT();
		this.positionY -= SPEED*(1+shipspeed/10);
	}

	/**
	 * Moves the ship speed units down, or until the down screen border is
	 * reached.
	 */
	public final void moveDown() {
		int shipspeed = ShopItem.getSHIPSpeedCOUNT();
		this.positionY += SPEED*(1+shipspeed/10);
	}

	/**
	 * Shoots a bullet upwards.
	 * * @param bullets
	 * List of bullets on screen, to add the new bullet.
	 * @return Checks if the bullet was shot correctly.
	 */
	public final boolean shoot(final Set<Bullet> bullets) {
		if (this.shootingCooldown.checkFinished()) {
			this.shootingCooldown.reset();

			// Check Player Mode
			if (this.isCheatMode) {
				// Cheat Mode: Extremely fast fire rate
				this.shootingCooldown = Core.getCooldown(50);
			}
			else if (this.isHybridMode) {
				// Hybrid Mode: Parry + Shoot
				this.isParrying = true;
				this.parryWindow.reset();
				SoundManager.play("sfx/melee.wav");

				fireBullets(bullets);
				return true;
			}
			else if (this.isMeleeMode) {
				// Melee Mode: Parry only
				this.isParrying = true;
				this.parryWindow.reset();
				SoundManager.play("sfx/melee.wav");
				return true;
			} else {
				// Normal Mode: Shoot only
				fireBullets(bullets);
				return true;
			}
		}
		return false;
	}

	/**
	 * Toggles Cheat Mode.
	 * @param active True to enable cheat mode.
	 */
	public void setCheatMode(boolean active) {
		this.isCheatMode = active;
		if (active) {
			// Rapid fire in Cheat Mode (50ms interval)
			this.shootingCooldown = Core.getCooldown(50);
		} else {
			// Restore normal fire rate
			restoreCooldown();
		}
	}

	/**
	 * Helper method to restore Cooldown of fire.
	 */
	private void restoreCooldown() {
		int shopSpeed = ShopItem.getShootingInterval();
		if (this.isHybridMode) {
			this.shootingCooldown = Core.getCooldown(Math.min(150, shopSpeed));
		} else if (this.isMeleeMode) {
			this.shootingCooldown = Core.getCooldown(Math.min(300, shopSpeed));
		} else {
			this.shootingCooldown = Core.getCooldown(shopSpeed);
		}
	}

	/**
	 * Helper method to fire bullets.
	 * Handles spread shot logic based on Shop upgrades.
	 * @param bullets The set of bullets to add to.
	 */
	private void fireBullets(Set<Bullet> bullets){
		// Get Spread Shot information from ShopItem
		int bulletCount = ShopItem.getMultiShotBulletCount();
		int spacing = ShopItem.getMultiShotSpacing();

		int centerX = positionX + this.width / 2;
		int centerY = positionY;

		if (bulletCount == 1) {
			// Single Shot
			Bullet b = BulletPool.getBullet(centerX, centerY, BULLET_SPEED);
			SoundManager.stop("sfx/laser.wav");
			SoundManager.play("sfx/laser.wav");
			b.setOwnerId(this.playerId); // Assign owner

			bullets.add(b);
		} else {
			// Spread Shot
			int startOffset = -(bulletCount / 2) * spacing;

			for (int i = 0; i < bulletCount; i++) {
				int offsetX = startOffset + (i * spacing);
				Bullet b = BulletPool.getBullet(centerX + offsetX, centerY, BULLET_SPEED);
				b.setOwnerId(this.playerId); // Assign owner

				bullets.add(b);

				SoundManager.stop("sfx/laser.wav");
				SoundManager.play("sfx/laser.wav");
			}
		}
	}

	/**
	 * Updates status of the ship.
	 */
	public final void update() {
		if (this.isInvincible && this.shieldCooldown.checkFinished()) {
			this.isInvincible = false;
			this.setColor(baseColor);
		}

		if (!this.destructionCooldown.checkFinished())
			this.spriteType = SpriteType.ShipDestroyed;
		else
			this.spriteType = SpriteType.Ship;

		if (this.isParrying && this.parryWindow.checkFinished()) {
			this.isParrying = false;
		}

		// Update Sword Slash Effect position to follow ship
		this.swordSlashEffect.setPositionX(this.positionX + (this.width / 2) - (this.swordSlashEffect.getWidth() / 2));
		this.swordSlashEffect.setPositionY(this.positionY - this.swordSlashEffect.getHeight() + 10);
	}

	/**
	 * Switches the ship to its destroyed state.
	 */
	public final void destroy() {
		if (!this.isInvincible) {
			SoundManager.stop("sfx/impact.wav");
			SoundManager.play("sfx/impact.wav");
			this.destructionCooldown.reset();
		}
	}

	/**
	 * Checks if the ship is destroyed.
	 * * @return True if the ship is currently destroyed.
	 */
	public final boolean isDestroyed() {
		return !this.destructionCooldown.checkFinished();
	}

	/**
	 * Getter for the ship's speed.
	 * * @return Speed of the ship.
	 */
	public final int getSpeed() {
		return SPEED;
	}

	/**
	 * Getter for the ship's invincibility state.
	 *
	 * @return True if the ship is currently invincible.
	 */
	public final boolean isInvincible() {
		return this.isInvincible;
	}

	/**
	 * Sets the Melee Mode (Sword Only).
	 * @param mode True to enable Melee Mode.
	 */
	public final void setMeleeMode(boolean mode) {
		this.isMeleeMode = mode;
		this.isHybridMode = false;
		if (mode) {
			int currentSpeed = ShopItem.getShootingInterval();
			this.shootingCooldown = Core.getCooldown(Math.min(300, currentSpeed));
		} else {
			this.shootingCooldown = Core.getCooldown(ShopItem.getShootingInterval());
		}
	}

	/**
	 * Sets the Hybrid Mode (Sword + Gun).
	 * @param mode True to enable Hybrid Mode.
	 */
	public final void setHybridMode(boolean mode) {
		this.isHybridMode = mode;
		this.isMeleeMode = false;
		if (mode) {
			int currentSpeed = ShopItem.getShootingInterval();
			this.shootingCooldown = Core.getCooldown(Math.min(150, currentSpeed));
		} else {
			this.shootingCooldown = Core.getCooldown(ShopItem.getShootingInterval());
		}
	}

	/**
	 * Checks if the player is currently parrying.
	 *
	 * @return True if parrying.
	 */
	public final boolean isParrying() { return this.isParrying; }

	/**
	 * Activates the ship's invincibility shield for a given duration.
	 *
	 * @param duration
	 * Duration of the invincibility in milliseconds.
	 */
	public final void activateInvincibility(final int duration) {
		this.isInvincible = true;
		this.shieldCooldown.setMilliseconds(duration);
		this.shieldCooldown.reset();
		this.setColor(Color.BLUE);
	}

	/**
	 * Returns the visual effect entity for melee attacks.
	 * @return Sword slash effect entity.
	 */
	public final Entity getSwordSlashEffect() {
		return this.swordSlashEffect;
	}
}