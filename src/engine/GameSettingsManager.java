package engine;

import java.awt.event.KeyEvent;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import audio.SoundManager;

public class GameSettingsManager {

    private static GameSettingsManager instance;
    private static final String SETTINGS_FILE = "settings.properties";

    private float volume;
    private Map<String, Integer> keyBindings;
    private Map<String, Integer> keyBindingsP2;

    private GameSettingsManager() {
        loadSettings();
    }

    public static synchronized GameSettingsManager getInstance() {
        if (instance == null) {
            instance = new GameSettingsManager();
        }
        return instance;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
        SoundManager.setVolume(this.volume);
        saveSettings();
    }

    public int getKey(String action) {
        return keyBindings.getOrDefault(action, -1);
    }

    public int getKeyP2(String action) {
        return keyBindingsP2.getOrDefault(action, -1);
    }

    public void setKey(String action, int keyCode) {
        keyBindings.put(action, keyCode);
        saveSettings();
    }

    public void setKeyP2(String action, int keyCode) {
        keyBindingsP2.put(action, keyCode);
        saveSettings();
    }

    public Map<String, Integer> getKeyBindings() {
        return keyBindings;
    }

    public Map<String, Integer> getKeyBindingsP2() {
        return keyBindingsP2;
    }

    private void loadSettings() {
        Properties props = new Properties();
        try (FileReader reader = new FileReader(SETTINGS_FILE)) {
            props.load(reader);
            this.volume = Float.parseFloat(props.getProperty("volume", "0.5"));
            this.keyBindings = new HashMap<>();
            this.keyBindings.put("UP", Integer.parseInt(props.getProperty("P1_UP", String.valueOf(KeyEvent.VK_W))));
            this.keyBindings.put("DOWN", Integer.parseInt(props.getProperty("P1_DOWN", String.valueOf(KeyEvent.VK_S))));
            this.keyBindings.put("LEFT", Integer.parseInt(props.getProperty("P1_LEFT", String.valueOf(KeyEvent.VK_A))));
            this.keyBindings.put("RIGHT", Integer.parseInt(props.getProperty("P1_RIGHT", String.valueOf(KeyEvent.VK_D))));
            this.keyBindings.put("SHOOT", Integer.parseInt(props.getProperty("P1_SHOOT", String.valueOf(KeyEvent.VK_SPACE))));
            this.keyBindingsP2 = new HashMap<>();
            this.keyBindingsP2.put("UP", Integer.parseInt(props.getProperty("P2_UP", String.valueOf(KeyEvent.VK_UP))));
            this.keyBindingsP2.put("DOWN", Integer.parseInt(props.getProperty("P2_DOWN", String.valueOf(KeyEvent.VK_DOWN))));
            this.keyBindingsP2.put("LEFT", Integer.parseInt(props.getProperty("P2_LEFT", String.valueOf(KeyEvent.VK_LEFT))));
            this.keyBindingsP2.put("RIGHT", Integer.parseInt(props.getProperty("P2_RIGHT", String.valueOf(KeyEvent.VK_RIGHT))));
            this.keyBindingsP2.put("SHOOT", Integer.parseInt(props.getProperty("P2_SHOOT", String.valueOf(KeyEvent.VK_ENTER))));
        } catch (IOException e) {
            // File not found, use default settings
            setDefaultSettings();
        }
        SoundManager.setVolume(this.volume);
    }

    private void saveSettings() {
        Properties props = new Properties();
        props.setProperty("volume", String.valueOf(this.volume));
        props.setProperty("P1_UP", String.valueOf(this.keyBindings.get("UP")));
        props.setProperty("P1_DOWN", String.valueOf(this.keyBindings.get("DOWN")));
        props.setProperty("P1_LEFT", String.valueOf(this.keyBindings.get("LEFT")));
        props.setProperty("P1_RIGHT", String.valueOf(this.keyBindings.get("RIGHT")));
        props.setProperty("P1_SHOOT", String.valueOf(this.keyBindings.get("SHOOT")));
        props.setProperty("P2_UP", String.valueOf(this.keyBindingsP2.get("UP")));
        props.setProperty("P2_DOWN", String.valueOf(this.keyBindingsP2.get("DOWN")));
        props.setProperty("P2_LEFT", String.valueOf(this.keyBindingsP2.get("LEFT")));
        props.setProperty("P2_RIGHT", String.valueOf(this.keyBindingsP2.get("RIGHT")));
        props.setProperty("P2_SHOOT", String.valueOf(this.keyBindingsP2.get("SHOOT")));

        try (FileWriter writer = new FileWriter(SETTINGS_FILE)) {
            props.store(writer, "Game Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setDefaultSettings() {
        this.volume = 0.5f;
        this.keyBindings = new HashMap<>();
        this.keyBindings.put("UP", KeyEvent.VK_W);
        this.keyBindings.put("DOWN", KeyEvent.VK_S);
        this.keyBindings.put("LEFT", KeyEvent.VK_A);
        this.keyBindings.put("RIGHT", KeyEvent.VK_D);
        this.keyBindings.put("SHOOT", KeyEvent.VK_SPACE);
        this.keyBindingsP2 = new HashMap<>();
        this.keyBindingsP2.put("UP", KeyEvent.VK_UP);
        this.keyBindingsP2.put("DOWN", KeyEvent.VK_DOWN);
        this.keyBindingsP2.put("LEFT", KeyEvent.VK_LEFT);
        this.keyBindingsP2.put("RIGHT", KeyEvent.VK_RIGHT);
        this.keyBindingsP2.put("SHOOT", KeyEvent.VK_ENTER);
    }
}
