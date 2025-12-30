package game.droid;

/**
 * Перелік підтримуваних типів дроїдів.
 *
 * Тут же зберігаються базові характеристики, щоб можна було показувати
 * "короткі характеристики" під час створення.
 */
public enum DroidType {
    CRUSHER("Crusher", 220, 45, 60, 70, false,
            "Танк. Лазерна атака. Високе HP, великий DMG."),
    SCOUTLING("Scoutling", 60, 8, 80, 85, false,
            "Швидкий. Лазерна атака. Низьке HP, зате висока швидкість."),
    MEDICOR("Medicor", 120, 6, 140, 65, true,
            "Підтримка. Лікує союзників. Може атакувати слабко."),
    PHANTOM("Phantom", 80, 55, 100, 90, false,
            "Вбивця. Телепортується за спину та бʼє після паузи 2с.");

    private final String displayName;
    private final int baseHealth;
    private final int baseDamage;
    private final int baseEnergy;
    private final int accuracy;
    private final boolean canHealAllies;
    private final String description;

    DroidType(String displayName, int baseHealth, int baseDamage, int baseEnergy,
              int accuracy, boolean canHealAllies, String description) {
        this.displayName = displayName;
        this.baseHealth = baseHealth;
        this.baseDamage = baseDamage;
        this.baseEnergy = baseEnergy;
        this.accuracy = accuracy;
        this.canHealAllies = canHealAllies;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getBaseHealth() {
        return baseHealth;
    }

    public int getBaseDamage() {
        return baseDamage;
    }

    public int getBaseEnergy() {
        return baseEnergy;
    }

    public int getAccuracy() {
        return accuracy;
    }

    public boolean canHealAllies() {
        return canHealAllies;
    }

    public String getDescription() {
        return description;
    }

    public String shortInfo() {
        return displayName + " | HP:" + baseHealth + " | DMG:" + baseDamage + " | ACC:" + accuracy;
    }

    public static DroidType fromDisplayName(String displayName) {
        if (displayName == null) return null;
        for (DroidType t : values()) {
            if (t.displayName.equalsIgnoreCase(displayName.trim())) return t;
        }
        return null;
    }
}
