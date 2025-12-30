package game.core;

import game.droid.DroidRepository;

/**
 * Загальний стан програми (список створених дроїдів і т.д.).
 */
public class GameContext {
    private final DroidRepository droidRepository = new DroidRepository();

    public DroidRepository getDroidRepository() {
        return droidRepository;
    }
}
