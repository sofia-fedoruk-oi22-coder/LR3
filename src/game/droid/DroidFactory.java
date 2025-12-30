package game.droid;

import game.droid.types.Crusher;
import game.droid.types.Medicor;
import game.droid.types.Phantom;
import game.droid.types.Scoutling;

/**
 * Фабрика для створення конкретних підкласів дроїдів.
 */
public final class DroidFactory {
    private DroidFactory() {
    }

    public static Droid create(DroidType type, int id, String name, int x, int y) {
        if (type == null) throw new IllegalArgumentException("DroidType is null");
        return switch (type) {
            case CRUSHER -> new Crusher(id, name, x, y);
            case SCOUTLING -> new Scoutling(id, name, x, y);
            case MEDICOR -> new Medicor(id, name, x, y);
            case PHANTOM -> new Phantom(id, name, x, y);
        };
    }
}
