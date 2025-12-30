package game.ui;

import game.droid.DroidRepository;

import javax.swing.*;
import java.awt.*;

public final class DroidListDialog {
    private DroidListDialog() {
    }

    public static void show(Component parent, DroidRepository repo) {
        String text = (repo == null) ? "Дроїдів немає." : repo.formatForView();

        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(450, 400));

        JOptionPane.showMessageDialog(parent, scroll, "Список дроїдів", JOptionPane.INFORMATION_MESSAGE);
    }
}
