package game.droid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Зберігає створених користувачем дроїдів (список для меню, вибору бою тощо).
 */
public class DroidRepository {
    private final List<Droid> droids = new ArrayList<>();
    private int nextId = 1;

    public synchronized Droid createAndAdd(DroidType type, String name) {
        int id = nextId++;
        Droid droid = DroidFactory.create(type, id, name, 0, 0);
        droids.add(droid);
        return droid;
    }

    public synchronized void add(Droid droid) {
        if (droid == null) return;
        // якщо хтось додає зовні, підстрахуємося: оновимо nextId
        if (droid.getId() >= nextId) {
            nextId = droid.getId() + 1;
        }
        droids.add(droid);
    }

    public synchronized List<Droid> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(droids));
    }

    public synchronized int size() {
        return droids.size();
    }

    public synchronized Droid findById(int id) {
        for (Droid d : droids) {
            if (d.getId() == id) return d;
        }
        return null;
    }

    public synchronized boolean containsId(int id) {
        return findById(id) != null;
    }

    public synchronized String formatForSelection() {
        if (droids.isEmpty()) return "(порожньо)";
        StringBuilder sb = new StringBuilder();
        for (Droid d : droids) {
            sb.append("Id: ").append(d.getId()).append(" | ")
                    .append(d.getName()).append(" | ")
                    .append(d.getModel())
                    .append('\n');
        }
        return sb.toString();
    }

    public synchronized String formatForView() {
        if (droids.isEmpty()) return "Дроїдів немає.";
        StringBuilder sb = new StringBuilder();
        for (Droid d : droids) {
            sb.append("Id: ").append(d.getId()).append('\n');
            sb.append("Ім'я: ").append(d.getName()).append('\n');
            sb.append("Вид дроїда: ").append(d.getModel()).append("\n\n");
        }
        return sb.toString();
    }
}
