package pub.pigeon.yggdyy.hexcreating.client;

import at.petrak.hexcasting.api.casting.iota.BooleanIota;
import at.petrak.hexcasting.api.casting.iota.DoubleIota;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.Vec3Iota;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import net.createmod.ponder.api.client.scene.SceneBuilder;
import net.createmod.ponder.api.client.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Port of the upstream PatternScenes1 to the Create 6 ponder API.
 * Every scene now draws the pattern it talks about, so no scene is left with a bare base plate.
 */
public final class PatternScenes1 {
    private static final Vec3 PATTERN_ORIGIN = new Vec3(3, 3, 0);
    private static final Vec3 PATTERN_X = new Vec3(-1, 0, 0);
    private static final Vec3 PATTERN_Y = new Vec3(0, 0.5 * Math.sqrt(2), 0.5 * Math.sqrt(2));

    private PatternScenes1() {
    }

    public static void introduction(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("pattern_introduction", "Introduction to draw patterns");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.rotateCameraY(35);
        scene.idle(20);
        HexPonderUtils.PatternDisplay patternTest = new HexPonderUtils.PatternDisplay(scene, util,
            PATTERN_ORIGIN, PATTERN_X, PATTERN_Y,
            HexPattern.fromAngles("qaq", HexDir.NORTH_EAST));
        patternTest.show(HexPonderUtils.A_LONG_TIME, 20);
    }

    public static void reset(SceneBuilder scene, SceneBuildingUtil util) {
        begin(scene, "pattern_introduction_reset", "How to reset the casting HUD");
        drawPattern(scene, util, "qaq", HexDir.NORTH_EAST);
        scene.overlay().showText(80)
            .attachKeyFrame()
            .text("Press the reset key to clear the pattern you are drawing.");
        scene.idle(90);
    }

    public static void locateSelf(SceneBuilder scene, SceneBuildingUtil util) {
        begin(scene, "pattern_locate_self", "How to get yourself and your location");
        drawPattern(scene, util, "qaq", HexDir.NORTH_EAST);
        scene.overlay().showText(80)
            .attachKeyFrame()
            .text("The get_caster pattern pushes yourself as an entity iota.");
        scene.idle(90);
    }

    public static void locateBlock(SceneBuilder scene, SceneBuildingUtil util) {
        begin(scene, "pattern_locate_block", "How to get location of block");
        BlockPos target = new BlockPos(2, 1, 2);
        scene.world().setBlock(target, Blocks.AMETHYST_BLOCK.defaultBlockState(), false);
        scene.world().showSection(util.select().position(target), Direction.DOWN);
        drawPattern(scene, util, "wqaawdd", HexDir.EAST);
        scene.overlay().showText(80)
            .attachKeyFrame()
            .text("The raycast pattern turns the block you are looking at into a vector.")
            .pointAt(util.vector().blockSurface(target, Direction.UP))
            .placeNearTarget();
        scene.idle(90);
    }

    public static void locateEntity(SceneBuilder scene, SceneBuildingUtil util) {
        begin(scene, "pattern_locate_entity", "How to get entity and its location");
        Vec3 villagerPos = new Vec3(1.5, 1, 1.5);
        scene.world().createEntity(world -> {
            Villager villager = new Villager(EntityType.VILLAGER, world);
            villager.setPos(villagerPos);
            return villager;
        });
        drawPattern(scene, util, "weaqa", HexDir.EAST);
        scene.overlay().showText(80)
            .attachKeyFrame()
            .text("The raycast/entity pattern gets the entity you are looking at.")
            .pointAt(villagerPos.add(0, 1, 0))
            .placeNearTarget();
        scene.idle(90);
    }

    public static void numberConstants(SceneBuilder scene, SceneBuildingUtil util) {
        begin(scene, "pattern_constants_number", "Number Constants");
        drawPattern(scene, util, "qdwdq", HexDir.NORTH_EAST);
        scene.overlay().showText(70)
            .attachKeyFrame()
            .text("Patterns such as pi and e push number iotas onto the stack.");
        scene.idle(50);
        showIota(scene, util, new BlockPos(1, 1, 2), new DoubleIota(Math.PI), 80);
        showIota(scene, util, new BlockPos(1, 1, 4), new DoubleIota(Math.E), 80);
        scene.idle(90);
    }

    public static void vectorConstants(SceneBuilder scene, SceneBuildingUtil util) {
        begin(scene, "pattern_constants_vector", "Vector Constants");
        drawPattern(scene, util, "qqqqqea", HexDir.NORTH_WEST);
        scene.overlay().showText(70)
            .attachKeyFrame()
            .text("Direction constant patterns push unit vectors such as (1, 0, 0).");
        scene.idle(50);
        showIota(scene, util, new BlockPos(1, 1, 1), new Vec3Iota(new Vec3(1, 0, 0)), 80);
        showIota(scene, util, new BlockPos(1, 1, 3), new Vec3Iota(new Vec3(0, 0, 1)), 80);
        scene.idle(90);
    }

    public static void booleanConstants(SceneBuilder scene, SceneBuildingUtil util) {
        begin(scene, "pattern_constants_boolean", "Boolean Constants");
        drawPattern(scene, util, "aqae", HexDir.SOUTH_EAST);
        scene.overlay().showText(70)
            .attachKeyFrame()
            .text("The true and false patterns push boolean iotas.");
        scene.idle(50);
        showIota(scene, util, new BlockPos(1, 1, 2), new BooleanIota(true), 80);
        showIota(scene, util, new BlockPos(1, 1, 4), new BooleanIota(false), 80);
        scene.idle(90);
    }

    public static void calculate(SceneBuilder scene, SceneBuildingUtil util) {
        begin(scene, "pattern_calculate", "Calculate Iotas");
        showIota(scene, util, new BlockPos(1, 1, 1), new DoubleIota(1), 60);
        showIota(scene, util, new BlockPos(1, 1, 3), new DoubleIota(2), 60);
        scene.idle(40);
        drawPattern(scene, util, "waaw", HexDir.NORTH_EAST);
        scene.overlay().showText(80)
            .attachKeyFrame()
            .text("The add pattern consumes two numbers and pushes their sum.");
        scene.idle(50);
        showIota(scene, util, new BlockPos(1, 1, 2), new DoubleIota(3), 80);
        scene.idle(90);
    }

    public static void edifyTreePattern(SceneBuilder scene, SceneBuildingUtil util) {
        begin(scene, "pattern_spell_edifytree", "Pattern of EdifyTree");
        drawPattern(scene, util, "wqaqwd", HexDir.NORTH_EAST);
        scene.overlay().showText(80)
            .attachKeyFrame()
            .text("This is the pattern of the Edify Sapling spell.");
        scene.idle(90);
    }

    public static void edifyTreePractice(SceneBuilder scene, SceneBuildingUtil util) {
        begin(scene, "pattern_spell_edifytree_practice", "Practice of EdifyTree");
        BlockPos saplingPos = new BlockPos(2, 1, 2);
        scene.world().setBlock(saplingPos, Blocks.OAK_SAPLING.defaultBlockState(), false);
        scene.world().showSection(util.select().position(saplingPos), Direction.DOWN);
        scene.idle(10);

        drawPattern(scene, util, "wqaqwd", HexDir.NORTH_EAST);
        scene.overlay().showText(80)
            .attachKeyFrame()
            .text("Draw the pattern on a sapling to grow a tree.")
            .pointAt(util.vector().blockSurface(saplingPos, Direction.UP))
            .placeNearTarget();
        scene.idle(60);

        scene.world().setBlock(new BlockPos(2, 2, 2), Blocks.OAK_LOG.defaultBlockState(), false);
        scene.world().setBlock(new BlockPos(2, 3, 2), Blocks.OAK_LOG.defaultBlockState(), false);
        scene.world().setBlock(new BlockPos(2, 4, 2), Blocks.OAK_LEAVES.defaultBlockState(), false);
        scene.world().setBlock(new BlockPos(1, 3, 2), Blocks.OAK_LEAVES.defaultBlockState(), false);
        scene.world().setBlock(new BlockPos(3, 3, 2), Blocks.OAK_LEAVES.defaultBlockState(), false);
        scene.world().setBlock(new BlockPos(2, 3, 1), Blocks.OAK_LEAVES.defaultBlockState(), false);
        scene.world().setBlock(new BlockPos(2, 3, 3), Blocks.OAK_LEAVES.defaultBlockState(), false);
        scene.world().showSection(util.select().fromTo(1, 2, 1, 3, 4, 3), Direction.UP);
        scene.idle(40);
    }

    private static void begin(SceneBuilder scene, String id, String title) {
        scene.title(id, title);
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);
    }

    private static void drawPattern(SceneBuilder scene, SceneBuildingUtil util, String angles, HexDir dir) {
        new HexPonderUtils.PatternDisplay(scene, util, PATTERN_ORIGIN, PATTERN_X, PATTERN_Y,
            HexPattern.fromAngles(angles, dir))
            .show(HexPonderUtils.A_LONG_TIME, 8);
    }

    private static void showIota(SceneBuilder scene, SceneBuildingUtil util, BlockPos pos, Iota iota, int duration) {
        HexPonderUtils.IotaDisplay display = new HexPonderUtils.IotaDisplay(scene, util, iota);
        display.setPos(pos);
        display.showIota(duration);
    }
}
