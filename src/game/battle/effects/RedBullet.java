package game.battle.effects;

import game.droid.Droid;
import java.awt.*;

/**
 * "Червона куля" (лазерний постріл) — маленький овал "як тік-так".
 * Летить до target.
 */
public class RedBullet {

    private float x;
    private float y;
    private final Droid target;
    private final int damage;

    private final float speedPx = 16f;
    private boolean finished = false;

    public RedBullet(float startX, float startY, Droid target, int damage) {
        this.x = startX;
        this.y = startY;
        this.target = target;
        this.damage = Math.max(0, damage);
    }

    public void update() {
        if (finished) return;
        if (target == null || target.isDead()) {
            finished = true;
            return;
        }

        Point hit = target.getHitPoint();
        float tx = hit.x;
        float ty = hit.y;

        float dx = tx - x;
        float dy = ty - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist <= 18f) {
            if (damage > 0) {
                target.takeDamage(damage);
            }
            finished = true;
            return;
        }

        if (dist > 0.001f) {
            x += speedPx * (dx / dist);
            y += speedPx * (dy / dist);
        } else {
            finished = true;
        }
    }

    public void draw(Graphics2D g2) {
        if (finished) return;
        g2.setColor(Color.RED);
        g2.fillOval(Math.round(x), Math.round(y), 8, 12);
    }

    public boolean isFinished() {
        return finished;
    }
}