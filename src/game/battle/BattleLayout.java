package game.battle;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Розставляє дроїдів у 2 ряди (до 4 на сторону).
 */
public final class BattleLayout {
    private BattleLayout() {
    }

    public static List<Point> positionsFor(BattleSide side, int teamSize, Dimension panelSize, int approxSpriteWidth) {
        int w = Math.max(1, panelSize.width);
        int h = Math.max(1, panelSize.height);

        // Використовуємо нижні 300px екрану під дроїдів, розкладаємо у кілька рядів без накладання
        int bottomHeight = Math.min(300, h);
        int marginX = 40;
        int stepX = Math.max(approxSpriteWidth + 30, 120);
        int halfWidth = w / 2;

        int startXLeft = marginX;
        int startXRight = halfWidth + marginX;

        int maxPerRowLeft = Math.max(1, (halfWidth - marginX * 2) / stepX);
        int maxPerRowRight = maxPerRowLeft; // симетрично

        int maxPerRow = (side == BattleSide.LEFT) ? maxPerRowLeft : maxPerRowRight;
        int rows = Math.max(1, (int) Math.ceil(teamSize / (double) maxPerRow));

        int stepY = Math.max(approxSpriteWidth + 10, bottomHeight / rows);
        int baseY = h - bottomHeight;

        List<Point> result = new ArrayList<>();
        for (int i = 0; i < teamSize; i++) {
            int row = i / maxPerRow;
            int col = i % maxPerRow;

            int x;
            int y = baseY + row * stepY;
            // Не виходимо за межі екрана
            y = Math.min(y, h - approxSpriteWidth - 5);

            if (side == BattleSide.LEFT) {
                x = startXLeft + col * stepX;
            } else {
                x = startXRight + col * stepX;
            }

            result.add(new Point(x, y));
        }
        return result;
    }
}
