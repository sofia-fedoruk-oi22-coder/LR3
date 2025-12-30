package game.battle;

import java.awt.*;

/**
 * Проста «червона куля» (розміром тіктаку) для лазерної атаки.
 */
public class LaserProjectile {
    private double x;
    private double y;
    private final double vx;
    private final double vy;

    private final int width;
    private final int height;

    private boolean active = true;

    private final int attackerId;
    private final int targetId;

    /**
     * Чи має цей постріл попасти (для реплею/детермінованого відтворення).
     */
    private final boolean willHit;

    /**
     * Запланований урон (для реплею/детермінованого відтворення).
     */
    private final int plannedDamage;

    private boolean applied = false;

    public LaserProjectile(double x, double y, double vx, double vy, int attackerId, int targetId, boolean willHit, int plannedDamage) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.attackerId = attackerId;
        this.targetId = targetId;
        this.willHit = willHit;
        this.plannedDamage = Math.max(0, plannedDamage);

        // тіктак приблизно
        this.width = 18;
        this.height = 8;
    }

    public void update(long deltaMs) {
        if (!active) return;
        double dt = deltaMs / 16.0; // умовна нормалізація (16мс ~ 1 крок)
        x += vx * dt;
        y += vy * dt;
    }

    public void draw(Graphics g) {
        if (!active) return;
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.RED);
        g2d.fillRoundRect((int) x, (int) y, width, height, 6, 6);
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, width, height);
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean willHit() {
        return willHit;
    }

    public int getPlannedDamage() {
        return plannedDamage;
    }

    public boolean isApplied() {
        return applied;
    }

    public void markApplied() {
        this.applied = true;
    }

    public int getAttackerId() {
        return attackerId;
    }

    public int getTargetId() {
        return targetId;
    }
}
