package game.storage;

import game.battle.BattleMode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BattleLog {
    public static final int VERSION = 1;

    private final BattleMode mode;
    private final Instant createdAt;

    private final List<DroidEntry> participants = new ArrayList<>();
    private final List<BattleEvent> events = new ArrayList<>();

    public BattleLog(BattleMode mode) {
        this.mode = mode;
        this.createdAt = Instant.now();
    }

    public BattleMode getMode() {
        return mode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void addParticipant(DroidEntry entry) {
        participants.add(entry);
    }

    public List<DroidEntry> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    public void addEvent(BattleEvent event) {
        events.add(event);
    }

    public List<BattleEvent> getEvents() {
        // гарантуємо порядок по часу
        List<BattleEvent> copy = new ArrayList<>(events);
        copy.sort(Comparator.comparingLong(BattleEvent::getTimeMs));
        return copy;
    }
}
