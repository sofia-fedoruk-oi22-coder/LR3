package game.storage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BattleEvent {
    private final long timeMs;
    private final String type;
    private final Map<String, String> data;

    public BattleEvent(long timeMs, String type, Map<String, String> data) {
        this.timeMs = Math.max(0, timeMs);
        this.type = type;
        this.data = (data == null) ? new HashMap<>() : new HashMap<>(data);
    }

    public long getTimeMs() {
        return timeMs;
    }

    public String getType() {
        return type;
    }

    public Map<String, String> getData() {
        return Collections.unmodifiableMap(data);
    }

    public String get(String key) {
        return data.get(key);
    }
}
