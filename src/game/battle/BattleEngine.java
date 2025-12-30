package game.battle;

import game.droid.Droid;
import game.droid.DroidType;
import game.droid.types.Medicor;
import game.droid.types.Phantom;
import game.storage.BattleEvent;
import game.storage.BattleLog;
import game.storage.DroidEntry;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Основна логіка бою.
 *
 * - 1v1: автоматичний бій
 * - команда на команду: ліва сторона — під контролем миші (призначення цілей), права — AI
 * - replay: відтворення подій з BattleLog
 */
public class BattleEngine {
    private final BattleScenario scenario;
    private final Dimension panelSize;

    private final List<BattleParticipant> participants = new ArrayList<>();
    private final Map<Integer, BattleParticipant> byId = new HashMap<>();

    private final List<LaserProjectile> projectiles = new ArrayList<>();

    private final Map<Integer, Long> cooldownMs = new HashMap<>();
    private final Set<Integer> lockedActors = new HashSet<>();

    private final List<ScheduledAction> scheduled = new ArrayList<>();

    // Для team-vs-team: attackerId -> targetId
    private final Map<Integer, Integer> manualTargets = new HashMap<>();

    private final Random rnd = new Random();

    private long elapsedMs = 0;
    private boolean finished = false;
    private BattleResult result;

    private final BattleLog log;
    private final Deque<BattleEvent> replayEvents;

    public BattleEngine(BattleScenario scenario, Dimension panelSize) {
        this.scenario = scenario;
        this.panelSize = new Dimension(Math.max(1, panelSize.width), Math.max(1, panelSize.height));

        if (scenario.getMode() == BattleMode.REPLAY) {
            this.log = scenario.getReplayLog();
            if (this.log == null) throw new IllegalArgumentException("Replay mode requires BattleLog");
            this.replayEvents = new ArrayDeque<>(this.log.getEvents());
        } else {
            this.log = new BattleLog(scenario.getMode());
            this.replayEvents = new ArrayDeque<>();
        }

        setupParticipants();

        if (scenario.getMode() != BattleMode.REPLAY) {
            logEvent("START", Map.of());
        }
    }

    public BattleMode getMode() {
        return scenario.getMode();
    }

    public List<BattleParticipant> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    public List<LaserProjectile> getProjectiles() {
        return projectiles;
    }

    public boolean isFinished() {
        return finished;
    }

    public BattleResult getResult() {
        return result;
    }

    public BattleLog getBattleLog() {
        return log;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public void setManualTarget(int attackerId, int targetId) {
        manualTargets.put(attackerId, targetId);
        if (scenario.getMode() != BattleMode.REPLAY) {
            logEvent("TARGET_SET", Map.of(
                    "attacker", String.valueOf(attackerId),
                    "target", String.valueOf(targetId)
            ));
        }
    }

    public Integer getManualTarget(int attackerId) {
        return manualTargets.get(attackerId);
    }

    public Map<Integer, Integer> getManualTargetsSnapshot() {
        return new HashMap<>(manualTargets);
    }

    public void update(long deltaMs) {
        if (finished) return;
        if (deltaMs < 0) deltaMs = 0;
        elapsedMs += deltaMs;

        // 1) Відтворення подій (replay)
        if (scenario.getMode() == BattleMode.REPLAY) {
            processReplayEvents();
        }

        // 2) Tick запланованих дій
        tickScheduled(deltaMs);

        // 3) Оновлення анімацій
        for (BattleParticipant _ : participants) {
            // Animation update is handled internally by Droid state changes
        }

        // 4) Оновлення снарядів
        updateProjectiles(deltaMs);

        // 5) Логіка бою (якщо не replay)
        if (scenario.getMode() != BattleMode.REPLAY) {
            tickAIAndActions(deltaMs);
        }

        // 6) Перевірка завершення бою
        checkFinishByDeathOrTimeout();
    }

    // --------- Setup ---------

    private void setupParticipants() {
        participants.clear();
        byId.clear();
        projectiles.clear();
        cooldownMs.clear();
        scheduled.clear();
        manualTargets.clear();
        lockedActors.clear();

        int approxW = approximateSpriteWidth();

        List<Droid> left = scenario.getLeftTeam();
        List<Droid> right = scenario.getRightTeam();

        // Reset дроїдів
        for (Droid d : left) d.resetForBattle();
        for (Droid d : right) d.resetForBattle();

        // Розставляємо позиції
        List<Point> leftPos = BattleLayout.positionsFor(BattleSide.LEFT, left.size(), panelSize, approxW);
        List<Point> rightPos = BattleLayout.positionsFor(BattleSide.RIGHT, right.size(), panelSize, approxW);

        for (int i = 0; i < left.size(); i++) {
            Droid d = left.get(i);
            Point pt = leftPos.get(i);
            d.setPosition(pt.x, pt.y);
            d.setState("idle");

            BattleParticipant bp = new BattleParticipant(d, BattleSide.LEFT, pt.x, pt.y);
            participants.add(bp);
            byId.put(d.getId(), bp);

            cooldownMs.put(d.getId(), initialCooldownFor(d));

            if (scenario.getMode() != BattleMode.REPLAY) {
                log.addParticipant(new DroidEntry(d.getId(), d.getName(), d.getType(), BattleSide.LEFT));
            }
        }

        for (int i = 0; i < right.size(); i++) {
            Droid d = right.get(i);
            Point pt = rightPos.get(i);
            d.setPosition(pt.x, pt.y);
            d.setState("idle");

            BattleParticipant bp = new BattleParticipant(d, BattleSide.RIGHT, pt.x, pt.y);
            participants.add(bp);
            byId.put(d.getId(), bp);

            cooldownMs.put(d.getId(), initialCooldownFor(d));

            if (scenario.getMode() != BattleMode.REPLAY) {
                log.addParticipant(new DroidEntry(d.getId(), d.getName(), d.getType(), BattleSide.RIGHT));
            }
        }
    }

    private int approximateSpriteWidth() {
        for (BattleParticipant p : participants) {
            Image img = p.getDroid().getCurrentFrameImage();
            if (img != null && img.getWidth(null) > 0) {
                return img.getWidth(null);
            }
        }
        // якщо participants ще порожній — подивимося у сценарії
        for (Droid d : scenario.getLeftTeam()) {
            Image img = d.getCurrentFrameImage();
            if (img != null && img.getWidth(null) > 0) return img.getWidth(null);
        }
        for (Droid d : scenario.getRightTeam()) {
            Image img = d.getCurrentFrameImage();
            if (img != null && img.getWidth(null) > 0) return img.getWidth(null);
        }
        return 140;
    }

    private long initialCooldownFor(@SuppressWarnings("unused") Droid d) {
        // щоб бій стартував не зовсім синхронно
        return 200 + rnd.nextInt(500);
    }

    // --------- Replay ---------

    private void processReplayEvents() {
        while (!replayEvents.isEmpty() && replayEvents.peekFirst().getTimeMs() <= elapsedMs) {
            BattleEvent e = replayEvents.removeFirst();
            applyReplayEvent(e);
        }
    }

    private void applyReplayEvent(BattleEvent e) {
        String type = e.getType();
        Map<String, String> d = e.getData();

        switch (type) {
            case "LASER_SHOT" -> {
                int attackerId = Integer.parseInt(d.getOrDefault("attacker", "0"));
                int targetId = Integer.parseInt(d.getOrDefault("target", "0"));
                boolean hit = Boolean.parseBoolean(d.getOrDefault("hit", "true"));
                int dmg = Integer.parseInt(d.getOrDefault("damage", "0"));
                fireLaser(attackerId, targetId, hit, dmg, true);
            }
            case "LASER_HIT" -> {
                int attackerId = Integer.parseInt(d.getOrDefault("attacker", "0"));
                int targetId = Integer.parseInt(d.getOrDefault("target", "0"));
                int dmg = Integer.parseInt(d.getOrDefault("damage", "0"));
                applyDirectDamage(attackerId, targetId, dmg, true);
                // прибʼємо перший активний снаряд attacker->target (щоб виглядало природно)
                for (LaserProjectile p : projectiles) {
                    if (p.isActive() && p.getAttackerId() == attackerId && p.getTargetId() == targetId) {
                        p.deactivate();
                        break;
                    }
                }
            }
            case "HEAL_START" -> {
                int healerId = Integer.parseInt(d.getOrDefault("healer", "0"));
                BattleParticipant healer = byId.get(healerId);
                if (healer != null) healer.getDroid().healState();
            }
            case "HEAL_APPLY" -> {
                int healerId = Integer.parseInt(d.getOrDefault("healer", "0"));
                int targetId = Integer.parseInt(d.getOrDefault("target", "0"));
                int amount = Integer.parseInt(d.getOrDefault("amount", "0"));
                BattleParticipant target = byId.get(targetId);
                if (target != null) {
                    target.getDroid().heal(amount);
                    target.getDroid().setState("idle");
                }
                BattleParticipant healer = byId.get(healerId);
                if (healer != null) healer.getDroid().setState("idle");
            }
            case "PHANTOM_APPEAR" -> {
                int phantomId = Integer.parseInt(d.getOrDefault("phantom", "0"));
                int targetId = Integer.parseInt(d.getOrDefault("target", "0"));
                teleportPhantomBehind(phantomId, targetId);
            }
            case "PHANTOM_STRIKE" -> {
                int phantomId = Integer.parseInt(d.getOrDefault("phantom", "0"));
                int targetId = Integer.parseInt(d.getOrDefault("target", "0"));
                int dmg = Integer.parseInt(d.getOrDefault("damage", "0"));
                BattleParticipant ph = byId.get(phantomId);
                if (ph != null) ph.getDroid().attack();
                applyDirectDamage(phantomId, targetId, dmg, true);
            }
            case "PHANTOM_RETURN" -> {
                int phantomId = Integer.parseInt(d.getOrDefault("phantom", "0"));
                returnPhantomToBase(phantomId);
            }
            case "DROID_DEFEATED" -> {
                int id = Integer.parseInt(d.getOrDefault("id", "0"));
                BattleParticipant bp = byId.get(id);
                if (bp != null) {
                    bp.getDroid().setHealth(0);
                    bp.getDroid().die();
                }
            }
            case "WIN" -> {
                String sideStr = d.getOrDefault("winner", "LEFT");
                BattleSide side = BattleSide.valueOf(sideStr);
                finish(side);
            }
            default -> {
                // ігноруємо невідомі
            }
        }
    }

    // --------- Core loop ---------

    private void tickScheduled(long deltaMs) {
        if (scheduled.isEmpty()) return;

        // Проходимо по знімку, щоб не ловити ConcurrentModification під час додавання нових задач із run()
        List<ScheduledAction> snapshot = new ArrayList<>(scheduled);
        for (ScheduledAction a : snapshot) {
            a.tick(deltaMs);
            if (a.isReady()) {
                a.run();
                scheduled.remove(a);
            }
        }
    }

    private void updateProjectiles(long deltaMs) {
        if (projectiles.isEmpty()) return;
        int w = panelSize.width;
        int h = panelSize.height;

        for (LaserProjectile p : projectiles) {
            if (!p.isActive()) continue;
            p.update(deltaMs);

            Rectangle b = p.getBounds();
            if (b.x < -100 || b.x > w + 100 || b.y < -100 || b.y > h + 100) {
                p.deactivate();
                continue;
            }

            if (scenario.getMode() == BattleMode.REPLAY) {
                // у реплеї шкода застосовується по EVENT-ам
                continue;
            }

            if (!p.willHit()) {
                // пролітає повз
                continue;
            }

            if (p.isApplied()) {
                continue;
            }

            BattleParticipant targetP = byId.get(p.getTargetId());
            if (targetP == null) continue;
            Droid target = targetP.getDroid();
            if (!target.isAlive()) {
                p.deactivate();
                continue;
            }

            if (p.getBounds().intersects(target.getBounds())) {
                p.markApplied();
                p.deactivate();
                applyDirectDamage(p.getAttackerId(), p.getTargetId(), p.getPlannedDamage(), false);

                logEvent("LASER_HIT", Map.of(
                        "attacker", String.valueOf(p.getAttackerId()),
                        "target", String.valueOf(p.getTargetId()),
                        "damage", String.valueOf(p.getPlannedDamage()),
                        "targetHealth", String.valueOf(target.getHealth())
                ));
            }
        }

        // чистимо неактивні
        projectiles.removeIf(p -> !p.isActive());
    }

    private void tickAIAndActions(long deltaMs) {
        for (BattleParticipant bp : participants) {
            Droid d = bp.getDroid();
            if (!d.isAlive()) continue;

            int id = d.getId();

            // блокування під час «довгих» дій
            if (lockedActors.contains(id)) continue;

            long cd = cooldownMs.getOrDefault(id, 0L);
            cd -= deltaMs;
            if (cd > 0) {
                cooldownMs.put(id, cd);
                continue;
            }

            if (null == d.getType()) {
                Droid target = selectTargetFor(bp);
                if (target != null) {
                    fireLaser(d.getId(), target.getId(), decideHit(d), computeDamage(d), false);
                }
                cooldownMs.put(id, cooldownAfter(d));
            } else // Вирішуємо дію
            switch (d.getType()) {
                case MEDICOR -> {
                    boolean healed = tryHeal((Medicor) d, bp.getSide());
                    if (!healed) {
                        Droid target = selectTargetFor(bp);
                        if (target != null) {
                            fireLaser(d.getId(), target.getId(), decideHit(d), computeDamage(d), false);
                        }
                    }   cooldownMs.put(id, cooldownAfter(d));
                }
                case PHANTOM ->                     {
                        Droid target = selectTargetFor(bp);
                        if (target != null) {
                            startPhantomSequence((Phantom) d, bp, target);
                        }       cooldownMs.put(id, cooldownAfter(d));
                    }
                default ->                     {
                        Droid target = selectTargetFor(bp);
                        if (target != null) {
                            fireLaser(d.getId(), target.getId(), decideHit(d), computeDamage(d), false);
                        }       cooldownMs.put(id, cooldownAfter(d));
                    }
            }
        }
    }

    private long cooldownAfter(Droid d) {
        return switch (d.getType()) {
            case SCOUTLING -> 650;
            case CRUSHER -> 1050;
            case MEDICOR -> 900;
            case PHANTOM -> 1700;
        };
    }

    private boolean decideHit(Droid attacker) {
        int roll = rnd.nextInt(100) + 1;
        return roll <= attacker.getAccuracy();
    }

    private int computeDamage(Droid attacker) {
        int base = attacker.getDamage();
        // невеликий розкид, щоб було динамічніше
        int spread = Math.max(1, base / 6);
        return Math.max(1, base - spread + rnd.nextInt(spread * 2 + 1));
    }

    private Droid selectTargetFor(BattleParticipant attackerP) {
        BattleSide attackerSide = attackerP.getSide();
        BattleSide enemySide = (attackerSide == BattleSide.LEFT) ? BattleSide.RIGHT : BattleSide.LEFT;

        // Team-vs-team: ліва сторона може мати ручні цілі
        if (scenario.getMode() == BattleMode.TEAM_VS_TEAM && attackerSide == BattleSide.LEFT) {
            Integer manual = manualTargets.get(attackerP.getDroid().getId());
            if (manual != null) {
                BattleParticipant targetP = byId.get(manual);
                if (targetP != null && targetP.getSide() == enemySide && targetP.getDroid().isAlive()) {
                    return targetP.getDroid();
                }
            }
        }

        // AI: обираємо найслабшого живого
        Droid best = null;
        double bestScore = Double.MAX_VALUE;
        for (BattleParticipant p : participants) {
            if (p.getSide() != enemySide) continue;
            Droid d = p.getDroid();
            if (!d.isAlive()) continue;
            double score = (double) d.getHealth() / (double) d.getMaxHealth();
            if (score < bestScore) {
                bestScore = score;
                best = d;
            }
        }
        return best;
    }

    private boolean tryHeal(Medicor medicor, BattleSide side) {
        if (!medicor.canHealAllies()) return false;
        if (medicor.getEnergyLevel() < medicor.getHealEnergyCost()) return false;

        // шукаємо союзника з найнижчим відсотком HP
        Droid target = null;
        double bestScore = 1.0;
        for (BattleParticipant p : participants) {
            if (p.getSide() != side) continue;
            Droid d = p.getDroid();
            if (!d.isAlive()) continue;
            double ratio = (double) d.getHealth() / (double) d.getMaxHealth();
            if (ratio < bestScore) {
                bestScore = ratio;
                target = d;
            }
        }

        if (target == null) return false;
        if (bestScore > 0.85) return false; // якщо всі майже full HP — не лікуємо

        // «підʼїхати та лікувати» — робимо heal state + delayed apply
        int healAmount = medicor.getHealAmount();
        int cost = medicor.getHealEnergyCost();

        // для лямбд робимо final посилання
        final Droid healTarget = target;
        final int healAmountFinal = healAmount;

        lockedActors.add(medicor.getId());
        medicor.setEnergyLevel(medicor.getEnergyLevel() - cost);
        medicor.healState();

        logEvent("HEAL_START", Map.of(
                "healer", String.valueOf(medicor.getId()),
                "target", String.valueOf(healTarget.getId())
        ));

        scheduled.add(new ScheduledAction(450, () -> {
            if (medicor.isAlive() && healTarget.isAlive()) {
                int healed = healTarget.heal(healAmountFinal);
                logEvent("HEAL_APPLY", Map.of(
                        "healer", String.valueOf(medicor.getId()),
                        "target", String.valueOf(healTarget.getId()),
                        "amount", String.valueOf(healed),
                        "targetHealth", String.valueOf(healTarget.getHealth())
                ));
                healTarget.setState("idle");
            }
        }));

        scheduled.add(new ScheduledAction(800, () -> {
            medicor.setState("idle");
            lockedActors.remove(medicor.getId());
        }));

        return true;
    }

    private void startPhantomSequence(Phantom phantom, @SuppressWarnings("unused") BattleParticipant phantomP, Droid target) {
        int pid = phantom.getId();
        if (lockedActors.contains(pid)) return;

        lockedActors.add(pid);

        logEvent("PHANTOM_APPEAR", Map.of(
                "phantom", String.valueOf(pid),
                "target", String.valueOf(target.getId())
        ));

        teleportPhantomBehind(pid, target.getId());

        long delay = phantom.getAppearToStrikeDelayMs();
        scheduled.add(new ScheduledAction(delay, () -> {
            if (!phantom.isAlive()) return;
            if (!target.isAlive()) {
                returnPhantomToBase(pid);
                lockedActors.remove(pid);
                return;
            }
            phantom.attack();
            int dmg = computeDamage(phantom);

            logEvent("PHANTOM_STRIKE", Map.of(
                    "phantom", String.valueOf(pid),
                    "target", String.valueOf(target.getId()),
                    "damage", String.valueOf(dmg)
            ));

            applyDirectDamage(pid, target.getId(), dmg, false);
        }));

        scheduled.add(new ScheduledAction(delay + 650, () -> {
            returnPhantomToBase(pid);
            phantom.setState("idle");
            lockedActors.remove(pid);

            logEvent("PHANTOM_RETURN", Map.of(
                    "phantom", String.valueOf(pid)
            ));
        }));
    }

    private void teleportPhantomBehind(int phantomId, int targetId) {
        BattleParticipant phP = byId.get(phantomId);
        BattleParticipant tP = byId.get(targetId);
        if (phP == null || tP == null) return;

        Droid phantom = phP.getDroid();
        Droid target = tP.getDroid();
        Rectangle tr = target.getBounds();

        // behind = позаду противника. Для лівого противника (дивиться вправо) — behind зліва.
        // Для правого противника (віддзеркалений, дивиться вліво) — behind справа.
        boolean targetIsRight = (tP.getSide() == BattleSide.RIGHT);

        Rectangle pr = phantom.getBounds();
        int phantomW = Math.max(60, pr.width);
        int phantomH = Math.max(60, pr.height);

        // Стаємо за спиною, але з невеликим відступом, щоб не накладатись
        int x;
        if (targetIsRight) {
            x = tr.x + tr.width - (int) (phantomW * 0.4);
            phantom.setMirrorOverride(true);
        } else {
            x = tr.x - (int) (phantomW * 0.6);
            phantom.setMirrorOverride(false);
        }

        // По вертикалі — вирівнюємо низом до цілі
        int y = tr.y + tr.height - phantomH;
        phantom.setPosition(x, y);
        phantom.setState("idle");
    }

    private void returnPhantomToBase(int phantomId) {
        BattleParticipant phP = byId.get(phantomId);
        if (phP == null) return;
        Droid phantom = phP.getDroid();
        phantom.setPosition(phP.getBaseX(), phP.getBaseY());
        phantom.setMirrorOverride(null);
    }

    private void fireLaser(int attackerId, int targetId, boolean hit, int damage, boolean fromReplay) {
        BattleParticipant attackerP = byId.get(attackerId);
        BattleParticipant targetP = byId.get(targetId);
        if (attackerP == null || targetP == null) return;

        Droid attacker = attackerP.getDroid();
        Droid target = targetP.getDroid();
        if (attacker.getType() == DroidType.PHANTOM) {
            return;
        }
        if (!attacker.isAlive() || !target.isAlive()) return;

        attacker.attack();

        // Старт пострілу з верхньої частини спрайту
        Rectangle ar = attacker.getBounds();
        Rectangle tr = target.getBounds();

        double startX;
        double startY = ar.y + Math.max(10, ar.height * 0.2);

        boolean attackerIsRightSide = (attackerP.getSide() == BattleSide.RIGHT);
        if (!attackerIsRightSide) {
            startX = ar.x + ar.width - 10;
        } else {
            startX = ar.x + 10;
        }

        double targetX = tr.getCenterX();
        double targetY = tr.getCenterY();

        double dx = targetX - startX;
        double dy = targetY - startY;
        double len = Math.max(1.0, Math.hypot(dx, dy));

        // Швидкість кулі
        double speed = 12.0;
        double vx = speed * dx / len;
        double vy = speed * dy / len;

        LaserProjectile p = new LaserProjectile(startX, startY, vx, vy, attackerId, targetId, hit, damage);
        projectiles.add(p);

        if (!fromReplay) {
            logEvent("LASER_SHOT", Map.of(
                    "attacker", String.valueOf(attackerId),
                    "target", String.valueOf(targetId),
                    "hit", String.valueOf(hit),
                    "damage", String.valueOf(damage)
            ));
        }

        // Повертаємо атакера в idle через короткий час
        scheduled.add(new ScheduledAction(420, () -> {
            if (attacker.isAlive()) attacker.setState("idle");
        }));

        // Якщо промах — просто не дамажимо, куля вилетить за екран
    }

    private void applyDirectDamage(int attackerId, int targetId, int damage, boolean fromReplay) {
        BattleParticipant targetP = byId.get(targetId);
        if (targetP == null) return;

        Droid target = targetP.getDroid();
        if (!target.isAlive()) return;

        int applied = target.applyDamage(damage);
        if (target.isAlive()) {
            target.hit();
            scheduled.add(new ScheduledAction(350, () -> {
                if (target.isAlive()) target.setState("idle");
            }));
        } else {
            target.die();
            if (!fromReplay) {
                logEvent("DROID_DEFEATED", Map.of(
                        "id", String.valueOf(targetId)
                ));
            }
        }

        if (!fromReplay) {
            logEvent("STATS", Map.of(
                    "target", String.valueOf(targetId),
                    "targetHealth", String.valueOf(target.getHealth()),
                    "attacker", String.valueOf(attackerId),
                    "damageApplied", String.valueOf(applied)
            ));
        }
    }

    private void checkFinishByDeathOrTimeout() {
        boolean leftAlive = false;
        boolean rightAlive = false;

        for (BattleParticipant p : participants) {
            if (!p.getDroid().isAlive()) continue;
            if (p.getSide() == BattleSide.LEFT) leftAlive = true;
            else rightAlive = true;
        }

        if (!leftAlive && rightAlive) {
            finish(BattleSide.RIGHT);
            return;
        }

        if (!rightAlive && leftAlive) {
            finish(BattleSide.LEFT);
            return;
        }

        // таймаут, щоб бій не затягувався
        if (elapsedMs > 60000) {
            int leftHp = sumHp(BattleSide.LEFT);
            int rightHp = sumHp(BattleSide.RIGHT);
            finish(leftHp >= rightHp ? BattleSide.LEFT : BattleSide.RIGHT);
        }
    }

    private int sumHp(BattleSide side) {
        int sum = 0;
        for (BattleParticipant p : participants) {
            if (p.getSide() == side) sum += p.getDroid().getHealth();
        }
        return sum;
    }

    private void finish(BattleSide winner) {
        if (finished) return;

        List<Droid> winners = new ArrayList<>();
        for (BattleParticipant p : participants) {
            if (p.getSide() == winner && p.getDroid().isAlive()) winners.add(p.getDroid());
        }

        this.result = new BattleResult(winner, winners);
        this.finished = true;

        if (scenario.getMode() != BattleMode.REPLAY) {
            logEvent("WIN", Map.of(
                    "winner", winner.toString()
            ));
        }
    }

    private void logEvent(String type, Map<String, String> data) {
        if (scenario.getMode() == BattleMode.REPLAY) return;
        BattleEvent e = new BattleEvent(elapsedMs, type, data);
        log.addEvent(e);

        // «в реальному часі кудись записувати» — пишемо і в консоль теж
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(elapsedMs).append("ms] ").append(type);
        if (data != null && !data.isEmpty()) {
            sb.append(" ").append(data);
        }
        System.out.println(sb);
    }
}
