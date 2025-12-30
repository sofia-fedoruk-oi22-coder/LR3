package game.droid.types;

import game.droid.Droid;
import game.droid.DroidType;
import game.util.SpriteLoader;

import java.awt.*;

/**
 * Важкий штурмовик без окремої анімації атаки – стріляє червоними кулями.
 */
public class Crusher extends Droid {

    private static final String IMG = "src/game/ui/sprites/";

    public Crusher(int id, String name, int xPosition, int yPosition) {
        super(
                id,
                DroidType.CRUSHER,
                name,
                "Crusher",
                220,
                45,
                60,
                1.0,
                3.0,
                70,
                false,
                xPosition,
                yPosition,
                new Image[]{
                        SpriteLoader.loadScaled(IMG + "crusher_move_1.png", 150, 150),
                        SpriteLoader.loadScaled(IMG + "crusher_move_2.png", 150, 150),
                },
                new Image[0],
                new Image[]{
                        SpriteLoader.loadScaled(IMG + "crusher_defeat_1.png", 150, 150),
                        SpriteLoader.loadScaled(IMG + "crusher_defeat_2.png", 150, 150),
                }
        );
    }
}
