package game.main;

import game.battle.BattleEngine;
import game.battle.BattleParticipant;
import game.battle.BattleSide;
import game.battle.LaserProjectile;
import game.core.GameContext;
import game.storage.BattleLogIO;
import game.util.Assets;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import javax.swing.*;

public class GameWindow extends JFrame {
    private static final int WINDOW_WIDTH = 1000;
    private static final int WINDOW_HEIGHT = 800;

    private final GameContext context;

    private final MenuPanel menuPanel;
    private GamePanel gamePanel;

    public GameWindow(GameContext context) {
        this.context = context;

        setTitle("Droid Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        setResizable(true);

        menuPanel = new MenuPanel();
        add(menuPanel);
        setVisible(true);
    }

    public GameContext getContext() {
        return context;
    }

    public MenuPanel getMenuPanel() {
        return menuPanel;
    }

    public GamePanel getGamePanel() {
        return gamePanel;
    }

    public void switchToGame() {
        getContentPane().removeAll();
        gamePanel = new GamePanel(this);
        add(gamePanel);
        revalidate();
        repaint();
    }

    public void switchToMenu() {
        getContentPane().removeAll();
        add(menuPanel);
        menuPanel.showMainMenu();
        revalidate();
        repaint();
    }

    // -------- Menu Panel --------

    public static class MenuPanel extends JPanel {
        private final Image background;
        private final JPanel contentPanel;
        private MenuState currentState = MenuState.MAIN;

        // Кнопки (створюємо один раз, щоб не губити listeners при resize)
        private final ImageButton playButton;
        private final ImageButton settingsButton;
        private final ImageButton exitButton;

        private final ImageButton fightOneVsOneButton;
        private final ImageButton fightTeamVsTeamButton;

        private final ImageButton createDroidButton;
        private final ImageButton viewDroidsButton;
        private final ImageButton replayBattleButton;

        private final ImageButton backButton;

        // Текстові повідомлення (напр. "Дроїди відсутні")
        private String tempMessage;
        private Timer tempMessageTimer;

        private final String gameTitle = "DROID BATTLE";

        // Підставте свій шлях до картинок меню (або використовуйте ресурси)
        private static final String MENU_IMG_BASE = "src/game/ui/sprites/";

        public MenuPanel() {
            this.background = Assets.loadImage(MENU_IMG_BASE + "menu_background.png");

            setLayout(null);
            setOpaque(true);
            setFocusable(true);

            contentPanel = new JPanel(null);
            contentPanel.setOpaque(false);
            add(contentPanel);

            // Кнопки головного меню
            playButton = new ImageButton(MENU_IMG_BASE + "play_button.png");
            settingsButton = new ImageButton(MENU_IMG_BASE + "settings_button.png");
            exitButton = new ImageButton(MENU_IMG_BASE + "exit_button.png");

            // Кнопки Play submenu
            fightOneVsOneButton = new ImageButton(MENU_IMG_BASE + "fight_1vs1_button.png");
            fightTeamVsTeamButton = new ImageButton(MENU_IMG_BASE + "fight_team_button.png");

            // Кнопки Settings submenu
            createDroidButton = new ImageButton(MENU_IMG_BASE + "create_droid_button.png");
            viewDroidsButton = new ImageButton(MENU_IMG_BASE + "view_droids_button.png");
            replayBattleButton = new ImageButton(MENU_IMG_BASE + "replay_match_button.png");

            // Back
            backButton = new ImageButton(MENU_IMG_BASE + "back_button.png");

            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    updateLayout();
                }
            });

            updateLayout();
        }

        public void showMainMenu() {
            currentState = MenuState.MAIN;
            updateLayout();
        }

        public void showPlaySubmenu() {
            currentState = MenuState.PLAY_SUBMENU;
            updateLayout();
        }

        public void showSettingsSubmenu() {
            currentState = MenuState.SETTINGS_SUBMENU;
            updateLayout();
        }

        public void showTempMessage(String message, int millis, Runnable after) {
            this.tempMessage = message;
            repaint();

            if (tempMessageTimer != null) {
                tempMessageTimer.stop();
            }

            tempMessageTimer = new Timer(millis, e -> {
                tempMessage = null;
                repaint();
                if (after != null) after.run();
            });
            tempMessageTimer.setRepeats(false);
            tempMessageTimer.start();
        }

        private void updateLayout() {
            contentPanel.removeAll();

            int panelWidth = getWidth();
            int panelHeight = getHeight();

            if (panelWidth <= 0 || panelHeight <= 0) return;

            switch (currentState) {
                case MAIN -> layoutMainMenu(panelWidth, panelHeight);
                case PLAY_SUBMENU -> layoutPlaySubmenu(panelWidth, panelHeight);
                case SETTINGS_SUBMENU -> layoutSettingsSubmenu(panelWidth, panelHeight);
            }

            contentPanel.setBounds(0, 0, panelWidth, panelHeight);
            revalidate();
            repaint();
        }

        private void layoutMainMenu(int panelWidth, int panelHeight) {
            int buttonWidth = Math.min((int) (panelWidth * 0.25), 350);
            int buttonHeight = buttonWidth / 3;
            int spacing = buttonHeight / 2;

            int totalHeight = buttonHeight * 3 + spacing * 2;
            int startY = (panelHeight - totalHeight) / 2 + buttonHeight;
            int x = (panelWidth - buttonWidth) / 2;

            playButton.setBounds(x, startY, buttonWidth, buttonHeight);
            settingsButton.setBounds(x, startY + buttonHeight + spacing, buttonWidth, buttonHeight);
            exitButton.setBounds(x, startY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight);

            contentPanel.add(playButton);
            contentPanel.add(settingsButton);
            contentPanel.add(exitButton);
        }

        private void layoutPlaySubmenu(int panelWidth, int panelHeight) {
            int buttonWidth = Math.min((int) (panelWidth * 0.3), 400);
            int buttonHeight = buttonWidth / 3;
            int spacing = buttonHeight / 2;

            int totalHeight = buttonHeight * 2 + spacing;
            int startY = (panelHeight - totalHeight) / 2 + buttonHeight / 2;
            int x = (panelWidth - buttonWidth) / 2;

            fightOneVsOneButton.setBounds(x, startY, buttonWidth, buttonHeight);
            fightTeamVsTeamButton.setBounds(x, startY + buttonHeight + spacing, buttonWidth, buttonHeight);

            int backButtonWidth = Math.min((int) (panelWidth * 0.15), 150);
            int backButtonHeight = backButtonWidth / 2;
            backButton.setBounds(30, panelHeight - backButtonHeight - 30, backButtonWidth, backButtonHeight);

            contentPanel.add(fightOneVsOneButton);
            contentPanel.add(fightTeamVsTeamButton);
            contentPanel.add(backButton);
        }

        private void layoutSettingsSubmenu(int panelWidth, int panelHeight) {
            int buttonWidth = Math.min((int) (panelWidth * 0.3), 400);
            int buttonHeight = buttonWidth / 3;
            int spacing = buttonHeight / 2;

            int totalHeight = buttonHeight * 3 + spacing * 2;
            int startY = (panelHeight - totalHeight) / 2 + buttonHeight / 2;
            int x = (panelWidth - buttonWidth) / 2;

            createDroidButton.setBounds(x, startY, buttonWidth, buttonHeight);
            viewDroidsButton.setBounds(x, startY + buttonHeight + spacing, buttonWidth, buttonHeight);
            replayBattleButton.setBounds(x, startY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight);

            int backButtonWidth = Math.min((int) (panelWidth * 0.15), 150);
            int backButtonHeight = backButtonWidth / 2;
            backButton.setBounds(30, panelHeight - backButtonHeight - 30, backButtonWidth, backButtonHeight);

            contentPanel.add(createDroidButton);
            contentPanel.add(viewDroidsButton);
            contentPanel.add(replayBattleButton);
            contentPanel.add(backButton);
        }

        // Getters
        public ImageButton getPlayButton() {
            return playButton;
        }

        public ImageButton getSettingsButton() {
            return settingsButton;
        }

        public ImageButton getExitButton() {
            return exitButton;
        }

        public ImageButton getFightOneVsOneButton() {
            return fightOneVsOneButton;
        }

        public ImageButton getFightTeamVsTeamButton() {
            return fightTeamVsTeamButton;
        }

        public ImageButton getCreateDroidButton() {
            return createDroidButton;
        }

        public ImageButton getViewDroidsButton() {
            return viewDroidsButton;
        }

        public ImageButton getReplayBattleButton() {
            return replayBattleButton;
        }

        public ImageButton getBackButton() {
            return backButton;
        }

        public MenuState getCurrentState() {
            return currentState;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(background, 0, 0, getWidth(), getHeight(), this);

            // Назва гри
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int titleSize = Math.min(getWidth() / 15, 80);
            Font titleFont = new Font("Arial", Font.BOLD, titleSize);
            g2d.setFont(titleFont);

            // Тінь
            g2d.setColor(new Color(0, 0, 0, 150));
            FontMetrics fm = g2d.getFontMetrics();
            int titleWidth = fm.stringWidth(gameTitle);
            int titleX = (getWidth() - titleWidth) / 2;
            int titleY = getHeight() / 6;
            g2d.drawString(gameTitle, titleX + 3, titleY + 3);

            g2d.setColor(new Color(255, 215, 0));
            g2d.drawString(gameTitle, titleX, titleY);

            // Тимчасове повідомлення
            if (tempMessage != null) {
                g2d.setFont(new Font("Arial", Font.BOLD, Math.max(18, getWidth() / 35)));
                g2d.setColor(Color.WHITE);
                FontMetrics m = g2d.getFontMetrics();
                int w = m.stringWidth(tempMessage);
                int x = (getWidth() - w) / 2;
                int y = (int) (getHeight() * 0.85);
                g2d.drawString(tempMessage, x, y);
            }
        }
    }

    public enum MenuState {
        MAIN,
        PLAY_SUBMENU,
        SETTINGS_SUBMENU
    }

    // -------- Image Button --------

    public static class ImageButton extends JButton {
        private final Image normalImage;
        private final Image hoverImage;
        private boolean isHovered = false;

        public ImageButton(String normalImagePath) {
            this(normalImagePath, Assets.deriveHoverPath(normalImagePath));
        }

        public ImageButton(String normalImagePath, String hoverImagePath) {
            this.normalImage = Assets.loadImage(normalImagePath);
            if (hoverImagePath != null && Assets.exists(hoverImagePath)) {
                this.hoverImage = Assets.loadImage(hoverImagePath);
            } else {
                this.hoverImage = this.normalImage;
            }

            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    setCursor(new Cursor(Cursor.HAND_CURSOR));
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Image img = isHovered ? hoverImage : normalImage;
            g2d.drawImage(img, 0, 0, getWidth(), getHeight(), this);

            g2d.dispose();
        }
    }

    // -------- Game Panel (Battle) --------

    public static class GamePanel extends JPanel {
        private final GameWindow window;
        private final Image background;

        private BattleEngine engine;
        private Timer loopTimer;
        private long lastTickNs;

        private int selectedAttackerId = -1;
        private String overlayMessage;
        private Timer overlayTimer;

        private static final String GAME_IMG_BASE = "src/game/ui/sprites/";

        public GamePanel(GameWindow window) {
            this.window = window;
            this.background = Assets.loadImage(GAME_IMG_BASE + "game_background.png");
            setFocusable(true);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleClick(e.getX(), e.getY());
                }
            });
        }

        public void startBattle(BattleEngine engine) {
            this.engine = engine;
            this.selectedAttackerId = -1;
            this.overlayMessage = null;

            if (loopTimer != null) {
                loopTimer.stop();
            }

            lastTickNs = System.nanoTime();
            loopTimer = new Timer(16, e -> {
                try {
                    long now = System.nanoTime();
                    long deltaMs = (now - lastTickNs) / 1_000_000L;
                    lastTickNs = now;
                    if (deltaMs > 80) deltaMs = 80; // ?????? ??? ?????

                    if (engine != null) {
                        engine.update(deltaMs);
                    }

                    repaint();

                    if (engine != null && engine.isFinished()) {
                        loopTimer.stop();
                        onBattleFinished();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    if (loopTimer != null) loopTimer.stop();
                    String msg = ex.getMessage() != null ? ex.getMessage() : "???????? ???????";
                    showOverlay("??????? ? ???: " + msg, 2000, window::switchToMenu);
                }
            });
            loopTimer.start();
        }

        private void handleClick(int x, int y) {
            if (engine == null) return;
            if (engine.isFinished()) return;

            // Ручне управління тільки у team-vs-team
            if (engine.getMode() != game.battle.BattleMode.TEAM_VS_TEAM) return;

            // Знаходимо, чи клікнули по дроїду
            BattleParticipant clicked = null;
            for (BattleParticipant p : engine.getParticipants()) {
                if (p.getDroid().getBounds().contains(x, y)) {
                    clicked = p;
                    break;
                }
            }
            if (clicked == null) return;

            // якщо клік по своєму (LEFT) — вибираємо attacker
            if (clicked.getSide() == BattleSide.LEFT && clicked.getDroid().isAlive()) {
                selectedAttackerId = clicked.getDroid().getId();
                return;
            }

            // якщо клік по ворогу (RIGHT) і attacker вже обраний — назначаємо ціль
            if (clicked.getSide() == BattleSide.RIGHT && clicked.getDroid().isAlive() && selectedAttackerId != -1) {
                engine.setManualTarget(selectedAttackerId, clicked.getDroid().getId());
            }
        }

        private void onBattleFinished() {
            if (engine == null || engine.getResult() == null) {
                window.switchToMenu();
                return;
            }

            String winnerText;
            if (engine.getMode() == game.battle.BattleMode.ONE_VS_ONE) {
                winnerText = (engine.getResult().getWinnerSide() == BattleSide.LEFT)
                        ? "Переміг ЛІВИЙ дроїд!"
                        : "Переміг ПРАВИЙ дроїд!";
            } else {
                winnerText = (engine.getResult().getWinnerSide() == BattleSide.LEFT)
                        ? "Перемогла команда ЛІВОРУЧ!"
                        : "Перемогла команда ПРАВОРУЧ!";
            }

            showOverlay(winnerText, 1500, () -> {
                // Після показу переможця — запит на збереження
                maybeSaveBattle();
                // Повертаємо HP/energy, щоб список дроїдів не залишався «побитим»
                resetDroidsAfterBattle();
                window.switchToMenu();
            });
        }

        private void resetDroidsAfterBattle() {
            if (engine == null) return;
            for (BattleParticipant p : engine.getParticipants()) {
                p.getDroid().resetForBattle();
            }
        }

        private void showOverlay(String text, int millis, Runnable after) {
            this.overlayMessage = text;
            repaint();

            if (overlayTimer != null) overlayTimer.stop();

            overlayTimer = new Timer(millis, e -> {
                overlayMessage = null;
                repaint();
                if (after != null) after.run();
            });
            overlayTimer.setRepeats(false);
            overlayTimer.start();
        }

        private void maybeSaveBattle() {
            if (engine == null) return;
            if (engine.getMode() == game.battle.BattleMode.REPLAY) return; // реплей не перезаписуємо

            int res = JOptionPane.showConfirmDialog(
                    window,
                    "Зберегти проведений бій у файл?",
                    "Save battle",
                    JOptionPane.YES_NO_OPTION
            );
            if (res != JOptionPane.YES_OPTION) return;

            String path = JOptionPane.showInputDialog(window, "Введіть шлях до файлу (наприклад C:/tmp/battle.txt):");
            if (path == null || path.trim().isEmpty()) return;

            try {
                BattleLogIO.write(engine.getBattleLog(), path.trim());
                JOptionPane.showMessageDialog(window, "Бій збережено: " + path.trim());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(window, "Помилка збереження: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(background, 0, 0, getWidth(), getHeight(), this);

            if (engine == null) return;

            // Снаряди
            for (LaserProjectile p : engine.getProjectiles()) {
                p.draw(g);
            }

            // Дроїди
            for (BattleParticipant p : engine.getParticipants()) {
                boolean mirror = (p.getSide() == BattleSide.RIGHT);
                Boolean override = p.getDroid().getMirrorOverride();
                if (override != null) mirror = override;
                p.getDroid().draw(g, mirror);

                // обводка для selected attacker
                if (p.getSide() == BattleSide.LEFT && p.getDroid().getId() == selectedAttackerId) {
                    Rectangle r = p.getDroid().getBounds();
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setColor(new Color(0, 255, 255, 180));
                    g2d.setStroke(new BasicStroke(3));
                    g2d.drawRoundRect(r.x - 4, r.y - 4, r.width + 8, r.height + 8, 12, 12);
                }
            }

            // Характеристики зверху
            drawStatsOverlay(g);

            // Повідомлення посередині (переможець)
            if (overlayMessage != null) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setFont(new Font("Arial", Font.BOLD, Math.max(22, getWidth() / 30)));
                g2d.setColor(Color.WHITE);
                FontMetrics fm = g2d.getFontMetrics();
                int w = fm.stringWidth(overlayMessage);
                int x = (getWidth() - w) / 2;
                int y = (int) (getHeight() * 0.18);
                g2d.drawString(overlayMessage, x, y);
            }
        }

        private void drawStatsOverlay(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.PLAIN, Math.max(14, getWidth() / 60)));

            int leftX = 20;
            int rightX = getWidth() - 20;
            int y = 30;
            int line = 18;

            // Ліві
            int yLeft = y;
            for (BattleParticipant p : engine.getParticipants()) {
                if (p.getSide() != BattleSide.LEFT) continue;
                var d = p.getDroid();
                String txt = d.getName() + " (#" + d.getId() + ") " + d.getModel() + "  HP:" + d.getHealth() + "/" + d.getMaxHealth();
                g2d.drawString(txt, leftX, yLeft);
                yLeft += line;
            }

            // Праві
            int yRight = y;
            for (BattleParticipant p : engine.getParticipants()) {
                if (p.getSide() != BattleSide.RIGHT) continue;
                var d = p.getDroid();
                String txt = d.getName() + " (#" + d.getId() + ") " + d.getModel() + "  HP:" + d.getHealth() + "/" + d.getMaxHealth();
                int w = g2d.getFontMetrics().stringWidth(txt);
                g2d.drawString(txt, rightX - w, yRight);
                yRight += line;
            }
        }
    }
}


