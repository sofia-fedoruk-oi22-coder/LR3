package game.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public final class SpriteLoader {

    private SpriteLoader() {}

    public static Image load(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            System.err.println("[SpriteLoader] Не вдалося завантажити: " + path);
            return placeholder(96, 96, shortName(path));
        }
    }

    public static Image loadOrFallback(String path, String fallbackPath) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException ignored) {
            return load(fallbackPath);
        }
    }

    /**
     * Завантажує зображення та масштабує його під потрібний розмір.
     */
    public static Image loadScaled(String path, int targetW, int targetH) {
        Image img = load(path);
        int w = Math.max(1, targetW);
        int h = Math.max(1, targetH);

        if (img == null) {
            return placeholder(w, h, shortName(path));
        }

        Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = copy.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();
        return copy;
    }

    public static Image placeholder(int w, int h, String label) {
        int width = Math.max(16, w);
        int height = Math.max(16, h);
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setColor(new Color(30, 30, 30));
        g.fillRect(0, 0, width, height);

        g.setColor(Color.MAGENTA);
        g.drawRect(0, 0, width - 1, height - 1);

        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.setColor(Color.WHITE);
        String text = (label == null) ? "MISSING" : label;
        g.drawString(text, 6, Math.min(18, height - 6));

        g.dispose();
        return img;
    }

    private static String shortName(String path) {
        if (path == null) return "MISSING";
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return (slash >= 0 && slash + 1 < path.length()) ? path.substring(slash + 1) : path;
    }
}
