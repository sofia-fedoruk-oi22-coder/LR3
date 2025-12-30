package game.main;

import game.battle.BattleEngine;
import game.battle.BattleMode;
import game.battle.BattleScenario;
import game.battle.BattleSide;
import game.core.GameContext;
import game.droid.Droid;
import game.droid.DroidFactory;
import game.droid.DroidRepository;
import game.droid.DroidType;
import game.storage.BattleLog;
import game.storage.BattleLogIO;
import game.storage.DroidEntry;

import java.awt.Dimension;
import java.io.IOException;
import java.util.*;

/**
 * Мінімальне консольне меню (якщо потрібно для вимоги ЛР).
 *
 * Запуск: java game.main.Main console
 */
public class ConsoleMenu {

    private final DroidRepository repo;

    public ConsoleMenu(GameContext ctx) {
        this.repo = ctx.getDroidRepository();
    }

    public void run() {
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n==== Droid Battle (Console Menu) ====");
            System.out.println("1) Створити дроїда");
            System.out.println("2) Показати список дроїдів");
            System.out.println("3) Запустити бій 1 на 1");
            System.out.println("4) Запустити бій команда на команду");
            System.out.println("5) Відтворити бій з файлу");
            System.out.println("6) Вийти");
            System.out.print("Ваш вибір: ");

            String cmd = sc.nextLine().trim();

            switch (cmd) {
                case "1" -> createDroid(sc);
                case "2" -> listDroids();
                case "3" -> fightOneVsOne(sc);
                case "4" -> fightTeamVsTeam(sc);
                case "5" -> replay(sc);
                case "6" -> {
                    System.out.println("Вихід...");
                    return;
                }
                default -> System.out.println("Невідома команда.");
            }
        }
    }

    private void createDroid(Scanner sc) {
        System.out.println("Оберіть тип:");
        int i = 1;
        for (DroidType t : DroidType.values()) {
            System.out.println(i + ") " + t.shortInfo());
            i++;
        }

        System.out.print("Номер типу: ");
        String s = sc.nextLine().trim();
        int idx;
        try {
            idx = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            System.out.println("Некоректно.");
            return;
        }
        if (idx < 1 || idx > DroidType.values().length) {
            System.out.println("Некоректно.");
            return;
        }

        DroidType type = DroidType.values()[idx - 1];
        System.out.print("Імʼя дроїда: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Порожнє імʼя.");
            return;
        }

        Droid d = repo.createAndAdd(type, name);
        System.out.println("Створено: Id=" + d.getId() + ", Name=" + d.getName() + ", Type=" + d.getModel());
    }

    private void listDroids() {
        if (repo.size() == 0) {
            System.out.println("Дроїдів немає.");
            return;
        }
        System.out.println(repo.formatForView());
    }

    private void fightOneVsOne(Scanner sc) {
        if (repo.size() < 2) {
            System.out.println("Недостатньо дроїдів для 1v1.");
            return;
        }
        listDroids();

        Droid a = askDroidById(sc, "Введіть id першого бійця: ", Set.of());
        if (a == null) return;

        Droid b = askDroidById(sc, "Введіть id супротивника: ", Set.of(a.getId()));
        if (b == null) return;

        BattleScenario scenario = new BattleScenario(BattleMode.ONE_VS_ONE, java.util.List.of(a), java.util.List.of(b));
        BattleEngine engine = new BattleEngine(scenario, new Dimension(1000, 700));
        runEngineToFinish(engine);
        afterBattleSavePrompt(sc, engine);
    }

    private void fightTeamVsTeam(Scanner sc) {
        if (repo.size() < 4) {
            System.out.println("Для командного бою потрібно мінімум 4 дроїди.");
            return;
        }

        int sizeA = askTeamSize(sc, "Розмір команди 1 (2-4): ");
        if (sizeA == -1) return;
        int sizeB = askTeamSize(sc, "Розмір команди 2 (2-4): ");
        if (sizeB == -1) return;

        if (sizeA + sizeB > repo.size()) {
            System.out.println("Недостатньо дроїдів для таких команд.");
            return;
        }

        listDroids();

        List<Droid> teamA = new ArrayList<>();
        List<Droid> teamB = new ArrayList<>();
        Set<Integer> used = new HashSet<>();

        for (int i = 0; i < sizeA; i++) {
            Droid d = askDroidById(sc, "Команда 1: id (" + (i + 1) + "/" + sizeA + "): ", used);
            if (d == null) return;
            used.add(d.getId());
            teamA.add(d);
        }
        for (int i = 0; i < sizeB; i++) {
            Droid d = askDroidById(sc, "Команда 2: id (" + (i + 1) + "/" + sizeB + "): ", used);
            if (d == null) return;
            used.add(d.getId());
            teamB.add(d);
        }

        BattleScenario scenario = new BattleScenario(BattleMode.TEAM_VS_TEAM, teamA, teamB);
        BattleEngine engine = new BattleEngine(scenario, new Dimension(1000, 700));
        runEngineToFinish(engine);
        afterBattleSavePrompt(sc, engine);
    }

    private void replay(Scanner sc) {
        System.out.print("Шлях до файлу: ");
        String path = sc.nextLine().trim();
        if (path.isEmpty()) return;

        try {
            BattleLog log = BattleLogIO.read(path);

            List<Droid> left = new ArrayList<>();
            List<Droid> right = new ArrayList<>();
            for (DroidEntry e : log.getParticipants()) {
                Droid d = DroidFactory.create(e.getType(), e.getId(), e.getName(), 0, 0);
                if (e.getSide() == BattleSide.LEFT) left.add(d);
                else right.add(d);
            }

            BattleScenario scenario = new BattleScenario(BattleMode.REPLAY, left, right, log);
            BattleEngine engine = new BattleEngine(scenario, new Dimension(1000, 700));
            runEngineToFinish(engine);

        } catch (IOException e) {
            System.out.println("Помилка читання: " + e.getMessage());
        }
    }

    private Droid askDroidById(Scanner sc, String prompt, Set<Integer> excluded) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            if (s.isEmpty()) return null;

            int id;
            try {
                id = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("Введіть число.");
                continue;
            }

            if (excluded.contains(id)) {
                System.out.println("Цей id заборонений.");
                continue;
            }

            Droid d = repo.findById(id);
            if (d == null) {
                System.out.println("Дроїда не знайдено.");
                continue;
            }
            return d;
        }
    }

    private int askTeamSize(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            if (s.isEmpty()) return -1;
            int n;
            try {
                n = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("Введіть число 2..4");
                continue;
            }
            if (n < 2 || n > 4) {
                System.out.println("Має бути 2..4");
                continue;
            }
            return n;
        }
    }

    private void runEngineToFinish(BattleEngine engine) {
        long step = 50;
        while (!engine.isFinished()) {
            engine.update(step);
        }
        System.out.println("\n=== Результат ===");
        System.out.println("Переміг: " + engine.getResult().getWinnerSide());
    }

    private void afterBattleSavePrompt(Scanner sc, BattleEngine engine) {
        System.out.print("Зберегти бій у файл? (y/n): ");
        String ans = sc.nextLine().trim();
        if (!ans.equalsIgnoreCase("y")) return;

        System.out.print("Шлях для збереження: ");
        String path = sc.nextLine().trim();
        if (path.isEmpty()) return;

        try {
            BattleLogIO.write(engine.getBattleLog(), path);
            System.out.println("Збережено у: " + path);
        } catch (IOException e) {
            System.out.println("Помилка запису: " + e.getMessage());
        }
    }
}
