package game.battle;

import game.droid.Droid;

/**
 * Дроїд у бою разом зі стороною та базовою позицією.
 */
public class BattleParticipant {
    private final Droid droid;
    private final BattleSide side;
    private final int baseX;
    private final int baseY;

    public BattleParticipant(Droid droid, BattleSide side, int baseX, int baseY) {
        this.droid = droid;
        this.side = side;
        this.baseX = baseX;
        this.baseY = baseY;
    }

    public Droid getDroid() {
        return droid;
    }

    public BattleSide getSide() {
        return side;
    }

    public int getBaseX() {
        return baseX;
    }

    public int getBaseY() {
        return baseY;
    }
}
