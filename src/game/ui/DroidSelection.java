package game.ui;

import game.droid.Droid;
import game.droid.DroidRepository;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public final class DroidSelection {
    private DroidSelection() {
    }

    /**
     * Показує список дроїдів і просить ввести id. Повертає Droid або null (cancel).
     */
    public static Droid selectById(Component parent, DroidRepository repo, String title, Set<Integer> excluded) {
        if (repo == null || repo.size() == 0) {
            JOptionPane.showMessageDialog(parent, "Дроїдів немає.");
            return null;
        }

        Set<Integer> excl = (excluded != null) ? excluded : new HashSet<>();

        while (true) {
            String prompt = repo.formatForSelection();
            prompt += "\n\n" + title + "\n";
            if (!excl.isEmpty()) {
                prompt += "(Заборонені id: " + excl + ")\n";
            }
            String input = JOptionPane.showInputDialog(parent, prompt);
            if (input == null) return null;
            input = input.trim();
            if (input.isEmpty()) continue;

            int id;
            try {
                id = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(parent, "Некоректний id. Введіть число.");
                continue;
            }

            if (excl.contains(id)) {
                JOptionPane.showMessageDialog(parent, "Цей id заборонений для вибору.");
                continue;
            }

            Droid d = repo.findById(id);
            if (d == null) {
                JOptionPane.showMessageDialog(parent, "Дроїд з таким id не знайдений.");
                continue;
            }
            return d;
        }
    }
}
