package game.droid.types;

import game.droid.Droid;
import game.droid.DroidType;
import game.util.SpriteLoader;

import java.awt.*;

/**
 * Phantom має власну анімацію атаки, тому не випускає червоні кулі.
 */
public class Phantom extends Droid {

    private static final String IMG = "src/game/ui/sprites/";
    private static final long APPEAR_TO_STRIKE_DELAY_MS = 420;

    public Phantom(int id, String name, int xPosition, int yPosition) {
        super(
                id,
                DroidType.PHANTOM,
                name,
                "Phantom",
                80,
                55,
                100,
                0.6,
                7.0,
                90,
                false,
                xPosition,
                yPosition,
                // Хід/стояння
                new Image[]{
                        SpriteLoader.loadScaled(IMG + "phantom.png", 150, 150),
                },
                // Удар: вступ-удар-відхід (3 кадри)
                new Image[]{
                        SpriteLoader.loadScaled(IMG + "phantom.png", 150, 150),
                        SpriteLoader.loadScaled(IMG + "phantom_attack.png", 150, 150),
                        SpriteLoader.loadScaled(IMG + "phantom.png", 150, 150),
                },
                new Image[]{
                        SpriteLoader.loadScaled(IMG + "phantom_defeat_1.png", 150, 150),
                        SpriteLoader.loadScaled(IMG + "phantom_defeat_2.png", 150, 150),
                }
        );
    }

    @Override
    public boolean hasAttackAnimation() {
        return true;
    }

    public long getAppearToStrikeDelayMs() {
        return APPEAR_TO_STRIKE_DELAY_MS;
    }

    public void stealthAttack(Droid target) {
        if (target == null) return;
        System.out.println(getName() + " атакує зі спини " + target.getName());
    }
}
