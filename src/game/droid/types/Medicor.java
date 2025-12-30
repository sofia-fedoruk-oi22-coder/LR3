package game.droid.types;

import game.droid.Droid;
import game.droid.DroidType;
import game.util.SpriteLoader;

import java.awt.*;

/**
 * Медик зцілює союзників і стріляє червоними кулями, коли нема кого лікувати.
 */
public class Medicor extends Droid {

    private static final String IMG = "src/game/ui/sprites/";
    private static final int HEAL_AMOUNT = 30;
    private static final int HEAL_ENERGY_COST = 20;

    public Medicor(int id, String name, int xPosition, int yPosition) {
        super(
                id,
                DroidType.MEDICOR,
                name,
                "Medicor",
                120,
                6,
                140,
                2.5,
                8.0,
                65,
                true,
                xPosition,
                yPosition,
                new Image[]{
                        SpriteLoader.loadScaled(IMG + "medicor_move.png", 150, 150),
                },
                new Image[0],
                new Image[]{
                        SpriteLoader.loadScaled(IMG + "medicor_defeat_1.png", 150, 150),
                        SpriteLoader.loadScaled(IMG + "medicor_defeat_2.png", 150, 150),
                }
        );
    }

    public int getHealAmount() {
        return HEAL_AMOUNT;
    }

    public int getHealEnergyCost() {
        return HEAL_ENERGY_COST;
    }

    public void healAlly(Droid ally) {
        if (ally == null) return;

        if (canHealAllies() && getEnergyLevel() >= HEAL_ENERGY_COST && !ally.isDead()) {
            int healed = ally.heal(HEAL_AMOUNT);
            setEnergyLevel(getEnergyLevel() - HEAL_ENERGY_COST);
            System.out.println(getName() + " вилікував " + ally.getName() + " на " + healed + " HP.");
        }
    }
}
