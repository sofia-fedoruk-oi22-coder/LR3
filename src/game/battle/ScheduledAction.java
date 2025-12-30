package game.battle;

/**
 * Запланована дія всередині BattleEngine.
 */
public class ScheduledAction {
    private long remainingMs;
    private final Runnable action;

    public ScheduledAction(long delayMs, Runnable action) {
        this.remainingMs = Math.max(0, delayMs);
        this.action = action;
    }

    public void tick(long deltaMs) {
        remainingMs -= deltaMs;
    }

    public boolean isReady() {
        return remainingMs <= 0;
    }

    public void run() {
        if (action != null) action.run();
    }
}
