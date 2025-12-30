package game.main;

import game.battle.BattleEngine;
import game.battle.BattleMode;
import game.battle.BattleScenario;
import game.core.GameContext;
import game.droid.Droid;
import game.droid.DroidFactory;
import game.droid.DroidRepository;
import game.droid.DroidType;
import game.storage.BattleLog;
import game.storage.BattleLogIO;
import game.storage.DroidEntry;
import game.ui.CreateDroidDialog;
import game.ui.DroidListDialog;
import game.ui.DroidSelection;

import javax.swing.*;
import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        GameContext ctx = new GameContext();

        // Якщо потрібне саме консольне меню (вимога ЛР), запускайте:
        // java game.main.Main console
        if (args != null && args.length > 0 && "console".equalsIgnoreCase(args[0])) {
            new ConsoleMenu(ctx).run();
            return;
        }

        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow(ctx);
            bindMenuListeners(window, ctx);
            window.setVisible(true);
        });
    }

    private static void bindMenuListeners(GameWindow window, GameContext ctx) {
        GameWindow.MenuPanel menu = window.getMenuPanel();
        DroidRepository repo = ctx.getDroidRepository();

        // MAIN MENU
        menu.getPlayButton().addActionListener(e -> menu.showPlaySubmenu());
        menu.getSettingsButton().addActionListener(e -> menu.showSettingsSubmenu());
        menu.getExitButton().addActionListener(e -> {
            int res = JOptionPane.showConfirmDialog(window, "Вийти з програми?", "Exit", JOptionPane.YES_NO_OPTION);
            if (res == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        });

        // BACK (для обох підменю)
        menu.getBackButton().addActionListener(e -> menu.showMainMenu());

        // SETTINGS SUBMENU
        menu.getCreateDroidButton().addActionListener(e -> {
            CreateDroidDialog.Result r = CreateDroidDialog.show(window);
            if (r == null) return;

            Droid created = repo.createAndAdd(r.type, r.name);
            JOptionPane.showMessageDialog(window,
                    "Дроїда створено!\n\n" +
                            "Id: " + created.getId() + "\n" +
                            "Ім'я: " + created.getName() + "\n" +
                            "Вид дроїда: " + created.getModel(),
                    "Created",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        menu.getViewDroidsButton().addActionListener(e -> {
            if (repo.size() == 0) {
                menu.showTempMessage("Дроїди відсутні", 5000, menu::showMainMenu);
                return;
            }
            DroidListDialog.show(window, repo);
        });

        menu.getReplayBattleButton().addActionListener(e -> {
            String path = JOptionPane.showInputDialog(window, "Введіть шлях до текстового файлу бою:");
            if (path == null || path.trim().isEmpty()) return;

            try {
                BattleLog log = BattleLogIO.read(path.trim());
                startReplay(window, log);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(window, "Помилка читання: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // PLAY SUBMENU
        menu.getFightOneVsOneButton().addActionListener(e -> {
            if (repo.size() == 0) {
                menu.showTempMessage("Дроїди відсутні", 5000, menu::showMainMenu);
                return;
            }
            if (repo.size() < 2) {
                menu.showTempMessage("Недостатньо дроїдів для бою 1 на 1", 5000, menu::showMainMenu);
                return;
            }

            Droid first = DroidSelection.selectById(window, repo, "Введіть id першого бійця:", Collections.emptySet());
            if (first == null) return;

            Set<Integer> excluded = new HashSet<>();
            excluded.add(first.getId());
            Droid second = DroidSelection.selectById(window, repo, "Введіть id супротивника:", excluded);
            if (second == null) return;

            startOneVsOne(window, first, second);
        });

        menu.getFightTeamVsTeamButton().addActionListener(e -> {
            if (repo.size() == 0) {
                menu.showTempMessage("Дроїди відсутні", 5000, menu::showMainMenu);
                return;
            }
            if (repo.size() < 4) {
                menu.showTempMessage("Для командного бою потрібно мінімум 4 дроїди (2+2)", 5000, menu::showMainMenu);
                return;
            }

            startTeamVsTeam(window, repo);
        });
    }

    // -------- Battle starters --------

    private static void startOneVsOne(GameWindow window, Droid left, Droid right) {
        window.switchToGame();
        SwingUtilities.invokeLater(() -> {
            var gp = window.getGamePanel();
            var size = gp.getSize();
            if (size.width <= 0 || size.height <= 0) size = window.getSize();

            BattleScenario scenario = new BattleScenario(BattleMode.ONE_VS_ONE, List.of(left), List.of(right));
            BattleEngine engine = new BattleEngine(scenario, size);
            gp.startBattle(engine);
        });
    }

    private static void startTeamVsTeam(GameWindow window, DroidRepository repo) {
        int total = repo.size();

        int sizeA;
        int sizeB;
        while (true) {
            sizeA = askTeamSize(window, "Розмір команди 1 (2-4):");
            if (sizeA == -1) return;
            sizeB = askTeamSize(window, "Розмір команди 2 (2-4):");
            if (sizeB == -1) return;

            if (sizeA + sizeB > total) {
                JOptionPane.showMessageDialog(window,
                        "Недостатньо дроїдів. Потрібно " + (sizeA + sizeB) + ", а створено " + total + ".",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                continue;
            }
            break;
        }

        // Вибір дроїдів по id
        List<Droid> teamA = new ArrayList<>();
        List<Droid> teamB = new ArrayList<>();

        Set<Integer> used = new HashSet<>();

        for (int i = 0; i < sizeA; i++) {
            Droid d = DroidSelection.selectById(window, repo, "Команда 1: введіть id (" + (i + 1) + "/" + sizeA + "):", used);
            if (d == null) return;
            used.add(d.getId());
            teamA.add(d);
        }

        for (int i = 0; i < sizeB; i++) {
            Droid d = DroidSelection.selectById(window, repo, "Команда 2: введіть id (" + (i + 1) + "/" + sizeB + "):", used);
            if (d == null) return;
            used.add(d.getId());
            teamB.add(d);
        }

        window.switchToGame();
        SwingUtilities.invokeLater(() -> {
            var gp = window.getGamePanel();
            var size = gp.getSize();
            if (size.width <= 0 || size.height <= 0) size = window.getSize();

            BattleScenario scenario = new BattleScenario(BattleMode.TEAM_VS_TEAM, teamA, teamB);
            BattleEngine engine = new BattleEngine(scenario, size);
            gp.startBattle(engine);
        });
    }

    private static int askTeamSize(JFrame parent, String prompt) {
        while (true) {
            String input = JOptionPane.showInputDialog(parent, prompt);
            if (input == null) return -1;
            input = input.trim();
            if (input.isEmpty()) continue;

            int n;
            try {
                n = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(parent, "Введіть число 2..4");
                continue;
            }

            if (n < 2 || n > 4) {
                JOptionPane.showMessageDialog(parent, "Розмір команди має бути 2..4");
                continue;
            }
            return n;
        }
    }

    private static void startReplay(GameWindow window, BattleLog log) {
        // Створюємо дроїдів з файлу (окремі обʼєкти, щоб не конфліктувати з repo)
        List<Droid> left = new ArrayList<>();
        List<Droid> right = new ArrayList<>();

        for (DroidEntry e : log.getParticipants()) {
            DroidType type = e.getType();
            Droid d = DroidFactory.create(type, e.getId(), e.getName(), 0, 0);
            if (e.getSide() == game.battle.BattleSide.LEFT) left.add(d);
            else right.add(d);
        }

        window.switchToGame();
        SwingUtilities.invokeLater(() -> {
            var gp = window.getGamePanel();
            var size = gp.getSize();
            if (size.width <= 0 || size.height <= 0) size = window.getSize();

            BattleScenario scenario = new BattleScenario(BattleMode.REPLAY, left, right, log);
            BattleEngine engine = new BattleEngine(scenario, size);
            gp.startBattle(engine);
        });
    }
}
