package game.util;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

/**
 * Простий завантажувач ресурсів.
 *
 * Підтримує:
 * - абсолютні / відносні шляхи до файлів
 * - ресурси з classpath (якщо ви кладете картинки у resources)
 */
public final class Assets {
    private Assets() {
    }

    public static Image loadImage(String path) {
        if (path == null) {
            return placeholder(1, 1);
        }

        String trimmed = path.trim();
        if (trimmed.isEmpty()) {
            return placeholder(1, 1);
        }

        // 1) Спроба завантажити з файлової системи
        File file = new File(trimmed);
        if (file.exists()) {
            return new ImageIcon(file.getAbsolutePath()).getImage();
        }

        // 2) Спроба завантажити з classpath
        String cp = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
        URL url = Assets.class.getClassLoader().getResource(cp);
        if (url != null) {
            return new ImageIcon(url).getImage();
        }

        // 3) Плейсхолдер, щоб не падати з NPE
        return placeholder(1, 1);
    }

    public static boolean exists(String path) {
        if (path == null) return false;
        String trimmed = path.trim();
        if (trimmed.isEmpty()) return false;

        File file = new File(trimmed);
        if (file.exists()) return true;

        String cp = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
        return Assets.class.getClassLoader().getResource(cp) != null;
    }

    /**
     * Автоматично генерує шлях для hover-картинки.
     * play_button.png -> play_button_hover.png
     */
    public static String deriveHoverPath(String normalPath) {
        if (normalPath == null) return null;
        String p = normalPath.trim();
        if (p.isEmpty()) return null;
        if (!p.toLowerCase().endsWith(".png")) return null;
        if (p.toLowerCase().contains("_hover")) return p;

        int dot = p.lastIndexOf('.');
        return p.substring(0, dot) + "_hover" + p.substring(dot);
    }

    public static Image placeholder(int w, int h) {
        int ww = Math.max(1, w);
        int hh = Math.max(1, h);
        BufferedImage img = new BufferedImage(ww, hh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, ww, hh);
        g2d.dispose();
        return img;
    }
}
