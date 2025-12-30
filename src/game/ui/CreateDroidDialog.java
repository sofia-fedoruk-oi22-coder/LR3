package game.ui;

import game.droid.DroidType;

import javax.swing.*;
import java.awt.*;

public final class CreateDroidDialog {
    private CreateDroidDialog() {
    }

    public static Result show(Component parent) {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.setPreferredSize(new Dimension(460, 420));

        JTextArea info = new JTextArea();
        info.setEditable(false);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setOpaque(false);

        StringBuilder sb = new StringBuilder();
        for (DroidType t : DroidType.values()) {
            sb.append(t.getDisplayName())
                    .append(" | HP: ").append(t.getBaseHealth())
                    .append(" | DMG: ").append(t.getBaseDamage())
                    .append("\n")
                    .append("  ").append(t.getDescription())
                    .append("\n\n");
        }
        info.setText(sb.toString());
        root.add(info, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));

        JComboBox<DroidType> typeCombo = new JComboBox<>(DroidType.values());
        JTextField nameField = new JTextField(20);

        form.add(new JLabel("Вид дроїда:"));
        form.add(typeCombo);
        form.add(new JLabel("Ім'я:"));
        form.add(nameField);

        root.add(form, BorderLayout.CENTER);

        int res = JOptionPane.showConfirmDialog(parent, root, "Створити дроїда", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return null;

        DroidType type = (DroidType) typeCombo.getSelectedItem();
        String name = nameField.getText() != null ? nameField.getText().trim() : "";

        if (type == null) {
            JOptionPane.showMessageDialog(parent, "Оберіть вид дроїда.");
            return null;
        }
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Вкажіть ім'я дроїда.");
            return null;
        }

        return new Result(type, name);
    }

    public static class Result {
        public final DroidType type;
        public final String name;

        public Result(DroidType type, String name) {
            this.type = type;
            this.name = name;
        }
    }
}
