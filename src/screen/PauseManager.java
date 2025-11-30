package screen;

import engine.InputManager;
import java.awt.event.KeyEvent;

public class PauseManager {

    private boolean paused = false;
    private int menuIndex = 0;

    public boolean wantQuit = false;
    public boolean wantReset = false;

    private boolean upLast = false;
    private boolean downLast = false;
    private boolean confirmLast = false;

    public boolean isPaused() {
        return paused;
    }

    public void togglePause() {
        paused = !paused;
    }

    public int getMenuIndex() {
        return menuIndex;
    }

    public void update(InputManager input) {

        // ↑
        boolean up = input.isKeyDown(KeyEvent.VK_UP);
        if (up && !upLast) {
            menuIndex = (menuIndex + 2) % 3;
        }
        upLast = up;

        // ↓
        boolean down = input.isKeyDown(KeyEvent.VK_DOWN);
        if (down && !downLast) {
            menuIndex = (menuIndex + 1) % 3;
        }
        downLast = down;

        // ENTER or SPACE
        boolean confirm = input.isKeyDown(KeyEvent.VK_ENTER) ||
                input.isKeyDown(KeyEvent.VK_SPACE);

        if (confirm && !confirmLast) {
            if (menuIndex == 0)      wantQuit = true;
            else if (menuIndex == 1) wantReset = true;
            else if (menuIndex == 2) paused = false;  // Return
        }
        confirmLast = confirm;
    }

    public void resetFlags() {
        wantQuit = false;
        wantReset = false;
    }
}