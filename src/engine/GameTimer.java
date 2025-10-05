package engine;

public class GameTimer {

    private long startTime;
    private long stopTime;
    private boolean running;

    public GameTimer() {
        this.startTime = 0;
        this.stopTime = 0;
        this.running = false;
    }

    public void start() {
        this.startTime = System.currentTimeMillis();
        this.running = true;
    }

    public void stop() {
        if (this.running) {
            this.stopTime = System.currentTimeMillis();
            this.running = false;
        }
    }

    public long getElapsedTime() {
        if (running) {
            return System.currentTimeMillis() - this.startTime;
        }
        return this.stopTime - this.startTime;
    }

    public void reset() {
        this.startTime = 0;
        this.stopTime = 0;
        this.running = false;
    }
}
