package lucid.util;

public class Clock implements Runnable {
    public static final int DEFAULT_TICKS_PER_SECOND = 100;
    public static final int DEFAULT_DELAY = 10;

    /** Current time of the game server */
    private long time = 0;

    private boolean started = false;

    /** The class containing the tick method */
    public Ticking ticking = null;

    /** The amount of times to call update() per second */
    public int ticksPerSecond = DEFAULT_TICKS_PER_SECOND;

    /** System timer delta */
    public int delay = DEFAULT_DELAY;

    public Clock(Ticking ticking, int ticksPerSecond) {
        this.ticking = ticking;
        this.ticksPerSecond = ticksPerSecond;
        this.delay = 1000 / ticksPerSecond;
    }

    public void start() {
        new Thread(this).start();
        started = true;
    }

    public void run() {
        while(started) {
            ticking.tick();

            time++;
            try {
                Thread.sleep(delay);
            } catch(InterruptedException e) {
                System.out.println("Clock interrupted");
            }
        }
    }

    public long getTime() {
        return time;
    }
}
