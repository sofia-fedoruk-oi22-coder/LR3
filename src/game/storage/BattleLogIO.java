package game.storage;

import game.battle.BattleMode;
import game.battle.BattleSide;
import game.droid.DroidType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Запис / читання бою у текстовий файл.
 *
 * Формат (version=1):
 * META|version=1|mode=TEAM_VS_TEAM|createdAt=...
 * DROID|id=1|name=...|type=Crusher|side=LEFT
 * EVENT|t=1200|type=LASER_SHOT|attacker=1|target=2|...
 */
public final class BattleLogIO {
    private BattleLogIO() {
    }

    public static void write(BattleLog log, String filePath) throws IOException {
        write(log, Path.of(filePath));
    }

    public static void write(BattleLog log, Path path) throws IOException {
        if (log == null) throw new IllegalArgumentException("log is null");
        if (path == null) throw new IllegalArgumentException("path is null");

        StringBuilder sb = new StringBuilder();
        sb.append("META|version=").append(BattleLog.VERSION)
                .append("|mode=").append(log.getMode())
                .append("|createdAt=").append(log.getCreatedAt()).append('\n');

        for (DroidEntry p : log.getParticipants()) {
            sb.append("DROID|id=").append(p.getId())
                    .append("|name=").append(escape(p.getName()))
                    .append("|type=").append(p.getType().getDisplayName())
                    .append("|side=").append(p.getSide())
                    .append('\n');
        }

        for (BattleEvent e : log.getEvents()) {
            sb.append("EVENT|t=").append(e.getTimeMs())
                    .append("|type=").append(e.getType());
            for (Map.Entry<String, String> kv : e.getData().entrySet()) {
                sb.append('|').append(kv.getKey()).append('=').append(escape(kv.getValue()));
            }
            sb.append('\n');
        }

        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
    }

    public static BattleLog read(String filePath) throws IOException {
        return read(Path.of(filePath));
    }

    public static BattleLog read(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty()) throw new IOException("Порожній файл бою.");

        BattleLog log = null;

        for (String line : lines) {
            if (line == null) continue;
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            String[] parts = trimmed.split("\\|");
            if (parts.length == 0) continue;

            String tag = parts[0].trim();
            if ("META".equalsIgnoreCase(tag)) {
                Map<String, String> meta = parseKeyValues(parts, 1);
                String modeStr = meta.get("mode");
                BattleMode mode = modeStr != null ? BattleMode.valueOf(modeStr) : BattleMode.ONE_VS_ONE;
                log = new BattleLog(mode);
                // createdAt ми зараз не перезаписуємо (не критично для реплею)

            } else if ("DROID".equalsIgnoreCase(tag)) {
                if (log == null) throw new IOException("META не знайдено перед DROID");
                Map<String, String> kv = parseKeyValues(parts, 1);
                int id = Integer.parseInt(kv.getOrDefault("id", "0"));
                String name = unescape(kv.getOrDefault("name", ""));
                DroidType type = DroidType.fromDisplayName(kv.get("type"));
                BattleSide side = BattleSide.valueOf(kv.getOrDefault("side", "LEFT"));
                log.addParticipant(new DroidEntry(id, name, type, side));

            } else if ("EVENT".equalsIgnoreCase(tag)) {
                if (log == null) throw new IOException("META не знайдено перед EVENT");
                Map<String, String> kv = parseKeyValues(parts, 1);
                long t = Long.parseLong(kv.getOrDefault("t", "0"));
                String type = kv.getOrDefault("type", "UNKNOWN");
                kv.remove("t");
                kv.remove("type");
                // розекранизуємо значення
                Map<String, String> data = new HashMap<>();
                for (Map.Entry<String, String> e : kv.entrySet()) {
                    data.put(e.getKey(), unescape(e.getValue()));
                }
                log.addEvent(new BattleEvent(t, type, data));
            }
        }

        if (log == null) throw new IOException("Не вдалося прочитати META з файлу.");
        return log;
    }

    private static Map<String, String> parseKeyValues(String[] parts, int fromIndex) {
        Map<String, String> map = new HashMap<>();
        for (int i = fromIndex; i < parts.length; i++) {
            String p = parts[i];
            int eq = p.indexOf('=');
            if (eq <= 0) continue;
            String key = p.substring(0, eq).trim();
            String val = p.substring(eq + 1);
            map.put(key, val);
        }
        return map;
    }

    private static String escape(String s) {
        if (s == null) return "";
        // Мінімальне екранування для нашого формату
        return s.replace("\\", "\\\\")
                .replace("|", "/")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private static String unescape(String s) {
        if (s == null) return "";
        return s.replace("\\\\", "\\");
    }
}
