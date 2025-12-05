package screen;

import java.awt.event.KeyEvent;
import java.util.Map;

import engine.GameSettingsManager;
import engine.Core;
import engine.Cooldown;

public class SettingsScreen extends Screen {

    private static final int SELECTION_TIME = 150;

    public enum EScreenState {
        MAIN_SELECTION,
        VOLUME_ADJUST,
        KEY_BINDINGS
    }

    private EScreenState screenState;

    private Cooldown selectionCooldown;
    private GameSettingsManager settingsManager;

    // Main menu
    private String[] mainMenuItems = {"Volume", "Player 1 Controls", "Player 2 Controls", "Back"};
    private int mainSelection;

    // Key bindings
    private String[] keyBindingActions = {"UP", "DOWN", "LEFT", "RIGHT", "SHOOT"};
    private int keyBindingSelection;
    private boolean isRebinding = false;
    private int currentPlayer; // 1 for P1, 2 for P2

    public SettingsScreen(final int width, final int height, final int fps) {
        super(width, height, fps);
        this.returnCode = 1; // Default return to title
        this.settingsManager = GameSettingsManager.getInstance();
        this.selectionCooldown = Core.getCooldown(SELECTION_TIME);
        this.screenState = EScreenState.MAIN_SELECTION;
        this.mainSelection = 0;
        this.keyBindingSelection = 0;
    }

    @Override
    public final int run() {
        super.run();
        return this.returnCode;
    }

    @Override
    protected final void update() {
        super.update();
        draw();

        if (this.selectionCooldown.checkFinished() && this.inputDelay.checkFinished()) {
            if (isRebinding) {
                handleRebindingInput();
            } else {
                switch (this.screenState) {
                    case MAIN_SELECTION:
                        handleMainSelectionInput();
                        break;
                    case VOLUME_ADJUST:
                        handleVolumeAdjustInput();
                        break;
                    case KEY_BINDINGS:
                        handleKeyBindingsInput();
                        break;
                }
            }
        }
    }

    private void handleMainSelectionInput() {
        if (inputManager.menuInput("UP")) {
            this.mainSelection = (this.mainSelection - 1 + mainMenuItems.length) % mainMenuItems.length;
            this.selectionCooldown.reset();
        } else if (inputManager.menuInput("DOWN")) {
            this.mainSelection = (this.mainSelection + 1) % mainMenuItems.length;
            this.selectionCooldown.reset();
        } else if (inputManager.menuInput("SHOOT")) {
            switch (this.mainSelection) {
                case 0: // Volume
                    this.screenState = EScreenState.VOLUME_ADJUST;
                    break;
                case 1: // Player 1
                    this.screenState = EScreenState.KEY_BINDINGS;
                    this.currentPlayer = 1;
                    this.keyBindingSelection = 0;
                    break;
                case 2: // Player 2
                    this.screenState = EScreenState.KEY_BINDINGS;
                    this.currentPlayer = 2;
                    this.keyBindingSelection = 0;
                    break;
                case 3: // Back
                    this.isRunning = false;
                    break;
            }
            this.selectionCooldown.reset();
        }
    }

    private void handleVolumeAdjustInput() {
        float volume = settingsManager.getVolume();
        if (inputManager.menuInput("LEFT")) {
            volume -= 0.05f;
            if (volume < 0) volume = 0;
            settingsManager.setVolume(volume);
            this.selectionCooldown.reset();
        } else if (inputManager.menuInput("RIGHT")) {
            volume += 0.05f;
            if (volume > 1.0f) volume = 1.0f;
            settingsManager.setVolume(volume);
            this.selectionCooldown.reset();
        } else if (inputManager.menuInput("SHOOT") || inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
            this.screenState = EScreenState.MAIN_SELECTION;
            this.selectionCooldown.reset();
        }
    }

    private void handleKeyBindingsInput() {
        if (inputManager.menuInput("UP")) {
            this.keyBindingSelection = (this.keyBindingSelection - 1 + keyBindingActions.length) % keyBindingActions.length;
            this.selectionCooldown.reset();
        } else if (inputManager.menuInput("DOWN")) {
            this.keyBindingSelection = (this.keyBindingSelection + 1) % keyBindingActions.length;
            this.selectionCooldown.reset();
        } else if (inputManager.menuInput("SHOOT")) {
            this.isRebinding = true;
            inputManager.clearLastKeyCode();
            this.selectionCooldown.reset();
        } else if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
            this.screenState = EScreenState.MAIN_SELECTION;
            this.selectionCooldown.reset();
        }
    }

    private void handleRebindingInput() {
        int keyCode = inputManager.getLastKeyCode();
        if (keyCode != -1) {
            rebindKey(keyCode);
            this.isRebinding = false;
            inputManager.clearLastKeyCode();
            this.selectionCooldown.reset();
        }
    }

    private void rebindKey(int keyCode) {
        String actionToRebind = keyBindingActions[keyBindingSelection];
        Map<String, Integer> currentBindings = (this.currentPlayer == 1) ? settingsManager.getKeyBindings() : settingsManager.getKeyBindingsP2();

        // Check for duplicates
        for (Map.Entry<String, Integer> entry : currentBindings.entrySet()) {
            if (entry.getValue() == keyCode && !entry.getKey().equals(actionToRebind)) {
                // Key is already in use. Maybe show a message? For now, just ignore.
                return;
            }
        }

        if (this.currentPlayer == 1) {
            settingsManager.setKey(actionToRebind, keyCode);
        } else {
            settingsManager.setKeyP2(actionToRebind, keyCode);
        }
    }

    private void draw() {
        drawManager.initDrawing(this);
        // This will be a new method with the modern UI
        drawManager.drawModernSettings(this, screenState, mainSelection, keyBindingSelection, settingsManager, isRebinding, currentPlayer);
        drawManager.completeDrawing(this);
    }

    // Getters for the DrawManager
    public String[] getMainMenuItems() { return mainMenuItems; }
    public String[] getKeyBindingActions() { return keyBindingActions; }
}
