package game.droid.types;

import game.droid.Droid;
import game.droid.DroidType;
import game.util.SpriteLoader;

import java.awt.*;

/**
 * Легкий розвідник без анімації атаки – стріляє червоними кулями.
 */
public class Scoutling extends Droid {

    private static final String IMG = "src/game/ui/sprites/";

    public Scoutling(int id, String name, int xPosition, int yPosition) {
        super(
                id,
                DroidType.SCOUTLING,
                name,
                "Scoutling",
                60,
                8,
                80,
                0.5,
                6.0,
                85,
                false,
                xPosition,
                yPosition,
                new Image[]{
                        SpriteLoader.loadScaled(IMG + "scoutling_fly_1.png", 150, 150),
                        SpriteLoader.loadScaled(IMG + "scoutling_fly_2.png", 150, 150),
                        SpriteLoader.loadScaled(IMG + "scoutling_fly_3.png", 150, 150),
                        SpriteLoader.loadScaled(IMG + "scoutling_fly_2.png", 150, 150),
                },
                new Image[0],
                new Image[]{
                        SpriteLoader.loadScaled(IMG + "scoutling_defeat_1.png", 150, 150),
                        SpriteLoader.loadScaled(IMG + "scoutling_defeat_2.png", 150, 150),
                }
        );
        setAnimationFrameDuration(500); // максимально повільне перемикання кадрів
    }
}

