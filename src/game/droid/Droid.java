package game.droid;

import game.battle.effects.RedBulletManager;
import game.util.SpriteLoader;
import java.awt.*;

/**
 * Базовий клас для всіх дроїдів.
 * Phantom має власну анімацію атаки, тоді як інші моделі стріляють червоними кулями.
 */
public class Droid {

    // ----------- BASE PROPERTIES -----------
    private final int id;
    private final DroidType type;
    private final int maxHealth;
    private final int maxEnergy;

    private String name;
    private final String model;
    private int health;
    private final int damage;
    private int energyLevel;
    private final double regenerationHealth;
    private final double regenerationEnergy;
    private final int accuracy;
    private final boolean canHealAllies;

    private int xPosition;
    private int yPosition;
    private int dx;
    private int dy;

    // ----------- VISUALS / ANIMATION -----------
    private final Image[] walkFrames;
    private final Image[] attackFrames;
    private final Image[] defeatFrames;

    private int currentFrame = 0;
    private int frameCount;
    private String state = "idle";

    private long frameDurationMs = 120; // час між кадрами
    private long lastFrameSwitchMs = System.currentTimeMillis();

    private boolean mirrored = false;
    private Boolean mirrorOverride = null;

    // Тільки для non-phantom: тривалість стану "attack"
    private long attackStateUntilMs = 0;

    // Миготіння (поразка/смерть)
    private int blinkFramesLeft = 0;

    public Droid(
            int id,
            DroidType type,
            String name,
            String model,
            int health,
            int damage,
            int energyLevel,
            double regenerationHealth,
            double regenerationEnergy,
            int accuracy,
            boolean canHealAllies,
            int xPosition,
            int yPosition,
            Image[] walkFrames,
            Image[] attackFrames,
            Image[] defeatFrames
    ) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.model = model;
        this.maxHealth = Math.max(1, health);
        this.health = this.maxHealth;
        this.damage = damage;
        this.maxEnergy = Math.max(0, energyLevel);
        this.energyLevel = this.maxEnergy;
        this.regenerationHealth = regenerationHealth;
        this.regenerationEnergy = regenerationEnergy;
        this.accuracy = accuracy;
        this.canHealAllies = canHealAllies;
        this.xPosition = xPosition;
        this.yPosition = yPosition;

        this.walkFrames = (walkFrames != null && walkFrames.length > 0)
                ? walkFrames
                : new Image[]{SpriteLoader.placeholder(96, 96, "NO_WALK")};
        this.attackFrames = (attackFrames != null) ? attackFrames : new Image[0];
        this.defeatFrames = (defeatFrames != null && defeatFrames.length > 0)
                ? defeatFrames
                : new Image[]{this.walkFrames[0]};

        this.frameCount = Math.max(1, this.walkFrames.length);
    }

    // -------------------- GETTERS --------------------
    public int getId() { return id; }
    public DroidType getType() { return type; }
    public String getName() { return name; }
    public String getModel() { return model; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getDamage() { return damage; }
    public int getEnergyLevel() { return energyLevel; }
    public double getRegenerationHealth() { return regenerationHealth; }
    public double getRegenerationEnergy() { return regenerationEnergy; }
    public int getAccuracy() { return accuracy; }
    public boolean canHealAllies() { return canHealAllies; }
    public int getXPosition() { return xPosition; }
    public int getYPosition() { return yPosition; }
    public boolean isMirrored() { return mirrored; }
    public Boolean getMirrorOverride() { return mirrorOverride; }

    // -------------------- SETTERS --------------------
    public void setName(String name) { this.name = name; }
    public void setHealth(int health) {
        this.health = Math.max(0, Math.min(maxHealth, health));
        if (this.health == 0) {
            die();
        }
    }
    public void setEnergyLevel(int energyLevel) {
        this.energyLevel = Math.max(0, Math.min(maxEnergy, energyLevel));
    }
    public void setPosition(int x, int y) {
        this.xPosition = x;
        this.yPosition = y;
    }
    public void setMirrored(boolean mirrored) {
        this.mirrored = mirrored;
    }
    public void setMirrorOverride(Boolean mirrorOverride) {
        this.mirrorOverride = mirrorOverride;
    }
    public void setAnimationFrameDuration(long millis) {
        this.frameDurationMs = Math.max(16, millis);
    }

    // -------------------- STATE / VISUAL --------------------
    public void setState(String state) {
        if (state == null) state = "idle";
        if (!this.state.equals(state)) {
            this.state = state;
            this.frameCount = switch (state) {
                case "walk", "idle" -> walkFrames.length;
                case "attack", "heal", "hit" -> hasAttackAnimation() ? attackFrames.length : walkFrames.length;
                case "defeat" -> defeatFrames.length;
                default -> 1;
            };
            if (this.frameCount <= 0) this.frameCount = 1;
            currentFrame = 0;
        }
    }

    public void update() {
        xPosition += dx;
        yPosition += dy;

        if (!hasAttackAnimation() && "attack".equals(state)) {
            if (System.currentTimeMillis() > attackStateUntilMs) {
                setState("idle");
            }
        }
    }

    public Image getCurrentFrameImage() {
        return selectFrame(false);
    }

    private Image selectFrame(boolean advanceFrame) {
        Image frame;
        if (blinkFramesLeft > 0) {
            boolean useDefeat = (blinkFramesLeft % 2 == 0);
            frame = useDefeat ? safeFrame(defeatFrames, currentFrame) : safeFrame(walkFrames, currentFrame);
        } else {
            switch (state) {
                case "attack", "heal" -> frame = hasAttackAnimation()
                        ? safeFrame(attackFrames, currentFrame)
                        : safeFrame(walkFrames, currentFrame);
                case "defeat" -> frame = safeFrame(defeatFrames, currentFrame);
                case "walk", "idle", "hit" -> frame = safeFrame(walkFrames, currentFrame);
                default -> frame = safeFrame(walkFrames, 0);
            }
        }

        if (advanceFrame) {
            long now = System.currentTimeMillis();
            if (now - lastFrameSwitchMs >= frameDurationMs) {
                currentFrame = (currentFrame + 1) % Math.max(1, frameCount);
                lastFrameSwitchMs = now;
            }
        }
        return frame;
    }

    public void draw(Graphics g) {
        draw(g, mirrored);
    }

    public void draw(Graphics g, boolean mirror) {
        this.mirrored = mirror;
        Image frame = selectFrame(true);
        int w = frame.getWidth(null);
        int h = frame.getHeight(null);
        if (w <= 0) w = 1;
        if (h <= 0) h = 1;

        if (!mirror) {
            g.drawImage(frame, xPosition, yPosition, null);
        } else {
            g.drawImage(frame, xPosition + w, yPosition, -w, h, null);
        }

        if (blinkFramesLeft > 0) {
            blinkFramesLeft--;
        }
    }

    private Image safeFrame(Image[] frames, int index) {
        if (frames == null || frames.length == 0) {
            return SpriteLoader.placeholder(96, 96, "NO_FRAME");
        }
        int i = Math.max(0, Math.min(index, frames.length - 1));
        return frames[i];
    }

    public Rectangle getBounds() {
        Image frame = getCurrentFrameImage();
        int w = Math.max(1, frame.getWidth(null));
        int h = Math.max(1, frame.getHeight(null));
        return new Rectangle(xPosition, yPosition, w, h);
    }

    // -------------------- MOVEMENT --------------------
    public void move(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
        setState("walk");
    }

    public void stop() {
        dx = 0;
        dy = 0;
        setState("idle");
    }

    // -------------------- ATTACK LOGIC --------------------
    public boolean hasAttackAnimation() {
        return attackFrames != null && attackFrames.length > 0;
    }

    public boolean usesRedBullets() {
        return !hasAttackAnimation();
    }

    public void attack() {
        setState("attack");

        if (!hasAttackAnimation()) {
            attackStateUntilMs = System.currentTimeMillis() + 200;
        }
    }

    public void attack(Droid target) {
        if (target == null) {
            attack();
            return;
        }

        setState("attack");

        if (usesRedBullets()) {
            RedBulletManager.spawn(this, target, damage);
            attackStateUntilMs = System.currentTimeMillis() + 200;
        }
    }

    public void takeDamage(int dmg) {
        if (dmg <= 0) return;
        setHealth(health - dmg);
    }

    public int applyDamage(int dmg) {
        if (dmg <= 0) return 0;
        int applied = Math.min(dmg, health);
        setHealth(health - applied);
        return applied;
    }

    public int heal(int amount) {
        if (amount <= 0 || !isAlive()) return 0;
        int before = health;
        setHealth(health + amount);
        return health - before;
    }

    public void healState() {
        setState("heal");
    }

    public void hit() {
        setState("hit");
        blinkFramesLeft = Math.max(blinkFramesLeft, 6); // мінімум 3 миготіння
    }

    public boolean isDead() {
        return health <= 0;
    }

    public boolean isAlive() {
        return health > 0;
    }

    public void die() {
        setState("defeat");
        blinkFramesLeft = Math.max(blinkFramesLeft, 6);
    }

    // -------------------- BULLET HELPERS --------------------
    public int getSpriteWidth() {
        Image img = safeFrame(walkFrames, 0);
        int w = img.getWidth(null);
        return (w > 0) ? w : 1;
    }

    public int getSpriteHeight() {
        Image img = safeFrame(walkFrames, 0);
        int h = img.getHeight(null);
        return (h > 0) ? h : 1;
    }

    public Point getShootPoint() {
        int w = getSpriteWidth();
        int sx = mirrored ? xPosition : (xPosition + w);
        int sy = yPosition + Math.max(10, getSpriteHeight() / 6);
        return new Point(sx, sy);
    }

    public Point getHitPoint() {
        int w = getSpriteWidth();
        int h = getSpriteHeight();
        return new Point(xPosition + w / 2, yPosition + h / 2);
    }

    public void printInfo() {
        System.out.println("Droid Name: " + name);
        System.out.println("Model: " + model);
        System.out.println("Health: " + health + "/" + maxHealth);
        System.out.println("Damage: " + damage);
        System.out.println("Energy Level: " + energyLevel + "/" + maxEnergy);
        System.out.println("Regeneration Health: " + regenerationHealth);
        System.out.println("Regeneration Energy: " + regenerationEnergy);
        System.out.println("Accuracy: " + accuracy);
        System.out.println("Can Heal Allies: " + canHealAllies);
        System.out.println("Has Attack Animation: " + hasAttackAnimation());
        System.out.println("Uses Red Bullets: " + usesRedBullets());
    }

    // -------------------- BATTLE LIFECYCLE --------------------
    public void resetForBattle() {
        health = maxHealth;
        energyLevel = maxEnergy;
        dx = 0;
        dy = 0;
        attackStateUntilMs = 0;
        mirrored = false;
        mirrorOverride = null;
        blinkFramesLeft = 0;
        lastFrameSwitchMs = System.currentTimeMillis();
        setState("idle");
        currentFrame = 0;
    }
}




