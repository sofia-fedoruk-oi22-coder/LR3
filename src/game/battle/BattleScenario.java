package game.battle;

import game.droid.Droid;
import game.storage.BattleLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BattleScenario {
    private final BattleMode mode;
    private final List<Droid> leftTeam;
    private final List<Droid> rightTeam;

    // якщо це replay — тут буде log
    private final BattleLog replayLog;

    public BattleScenario(BattleMode mode, List<Droid> leftTeam, List<Droid> rightTeam) {
        this(mode, leftTeam, rightTeam, null);
    }

    public BattleScenario(BattleMode mode, List<Droid> leftTeam, List<Droid> rightTeam, BattleLog replayLog) {
        this.mode = mode;
        this.leftTeam = new ArrayList<>(leftTeam != null ? leftTeam : List.of());
        this.rightTeam = new ArrayList<>(rightTeam != null ? rightTeam : List.of());
        this.replayLog = replayLog;
    }

    public BattleMode getMode() {
        return mode;
    }

    public List<Droid> getLeftTeam() {
        return Collections.unmodifiableList(leftTeam);
    }

    public List<Droid> getRightTeam() {
        return Collections.unmodifiableList(rightTeam);
    }

    public BattleLog getReplayLog() {
        return replayLog;
    }
}
