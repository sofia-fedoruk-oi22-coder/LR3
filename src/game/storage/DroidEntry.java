package game.storage;

import game.battle.BattleSide;
import game.droid.DroidType;

public class DroidEntry {
    private final int id;
    private final String name;
    private final DroidType type;
    private final BattleSide side;

    public DroidEntry(int id, String name, DroidType type, BattleSide side) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.side = side;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public DroidType getType() {
        return type;
    }

    public BattleSide getSide() {
        return side;
    }
}
