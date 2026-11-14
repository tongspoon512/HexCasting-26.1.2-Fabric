package pub.pigeon.yggdyy.hexcreating.client;

import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import com.zurrtum.create.client.ponder.api.scene.SceneBuilder;
import com.zurrtum.create.client.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.world.phys.Vec3;

/**
 * Port of the upstream PatternScenes1 to the Create 6 ponder API.
 * Scenes that were title-only upstream keep their titles and additionally show the base plate
 * so that every thought key displays a visible scene.
 */
public final class PatternScenes1 {
    private PatternScenes1() {
    }

    public static void introduction(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("pattern_introduction", "Introduction to draw patterns");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.rotateCameraY(35);
        scene.idle(20);
        HexPonderUtils.PatternDisplay patternTest = new HexPonderUtils.PatternDisplay(scene, util,
            new Vec3(3, 3, 0), new Vec3(-1, 0, 0),
            new Vec3(0, 0.5 * Math.sqrt(2), 0.5 * Math.sqrt(2)),
            HexPattern.fromAngles("qaq", HexDir.NORTH_EAST));
        patternTest.show(HexPonderUtils.A_LONG_TIME, 20);
    }

    public static void reset(SceneBuilder scene, SceneBuildingUtil util) {
        titleOnly(scene, "pattern_introduction_reset", "How to reset the casting HUD");
    }

    public static void locateSelf(SceneBuilder scene, SceneBuildingUtil util) {
        titleOnly(scene, "pattern_locate_self", "How to get yourself and your location");
    }

    public static void locateBlock(SceneBuilder scene, SceneBuildingUtil util) {
        titleOnly(scene, "pattern_locate_block", "How to get location of block");
    }

    public static void locateEntity(SceneBuilder scene, SceneBuildingUtil util) {
        titleOnly(scene, "pattern_locate_entity", "How to get entity and its location");
    }

    public static void numberConstants(SceneBuilder scene, SceneBuildingUtil util) {
        titleOnly(scene, "pattern_constants_number", "Number Constants");
    }

    public static void vectorConstants(SceneBuilder scene, SceneBuildingUtil util) {
        titleOnly(scene, "pattern_constants_vector", "Vector Constants");
    }

    public static void booleanConstants(SceneBuilder scene, SceneBuildingUtil util) {
        titleOnly(scene, "pattern_constants_boolean", "Boolean Constants");
    }

    public static void calculate(SceneBuilder scene, SceneBuildingUtil util) {
        titleOnly(scene, "pattern_calculate", "Calculate Iotas");
    }

    public static void edifyTreePattern(SceneBuilder scene, SceneBuildingUtil util) {
        titleOnly(scene, "pattern_spell_edifytree", "Pattern of EdifyTree");
    }

    public static void edifyTreePractice(SceneBuilder scene, SceneBuildingUtil util) {
        titleOnly(scene, "pattern_spell_edifytree_practice", "Practice of EdifyTree");
    }

    private static void titleOnly(SceneBuilder scene, String id, String title) {
        scene.title(id, title);
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(40);
    }
}
