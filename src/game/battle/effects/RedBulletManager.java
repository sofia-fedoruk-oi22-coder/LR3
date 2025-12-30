package game.battle.effects;

import game.droid.Droid;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class RedBulletManager {

    private RedBulletManager() {}

    private static final List<RedBullet> bullets = new ArrayList<>();

    public static void spawn(Droid attacker, Droid target, int damage) {
        if (attacker == null || target == null) return;
        Point p = attacker.getShootPoint();
        bullets.add(new RedBullet(p.x, p.y, target, damage));
    }

    public static void updateAll() {
        if (bullets.isEmpty()) return;

        Iterator<RedBullet> it = bullets.iterator();
        while (it.hasNext()) {
            RedBullet b = it.next();
            b.update();
            if (b.isFinished()) it.remove();
        }
    }

    public static void drawAll(Graphics2D g2) {
        if (bullets.isEmpty()) return;
        for (RedBullet b : bullets) {
            b.draw(g2);
        }
    }

    public static void clear() {
        bullets.clear();
    }
}