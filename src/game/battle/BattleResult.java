package game.battle;

import game.droid.Droid;

import java.util.List;

public class BattleResult {
    private final BattleSide winnerSide;
    private final List<Droid> winners;

    public BattleResult(BattleSide winnerSide, List<Droid> winners) {
        this.winnerSide = winnerSide;
        this.winners = winners;
    }

    public BattleSide getWinnerSide() {
        return winnerSide;
    }

    public List<Droid> getWinners() {
        return winners;
    }
}
