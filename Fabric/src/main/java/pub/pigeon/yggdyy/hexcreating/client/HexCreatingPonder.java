package pub.pigeon.yggdyy.hexcreating.client;

import com.zurrtum.create.client.ponder.api.PonderPalette;
import com.zurrtum.create.client.ponder.api.registration.PonderPlugin;
import com.zurrtum.create.client.ponder.api.registration.PonderSceneRegistrationHelper;
import com.zurrtum.create.client.ponder.api.scene.SceneBuilder;
import com.zurrtum.create.client.ponder.api.scene.SceneBuildingUtil;
import com.zurrtum.create.client.ponder.foundation.PonderIndex;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import pub.pigeon.yggdyy.hexcreating.HexCreatingCompat;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlocks;
import pub.pigeon.yggdyy.hexcreating.items.HexCreatingItems;

/**
 * Ponder integration for HexCreating. The four thought keys mirror the upstream scenes.
 */
public final class HexCreatingPonder {
    private HexCreatingPonder() {
    }

    public static void register() {
        PonderIndex.addPlugin(new Plugin());
    }

    private static final class Plugin implements PonderPlugin {
        @Override
        public String getModId() {
            return HexCreatingCompat.MOD_ID;
        }

        @Override
        public void registerScenes(PonderSceneRegistrationHelper<Identifier> helper) {
            PonderSceneRegistrationHelper<net.minecraft.world.item.Item> items =
                helper.<net.minecraft.world.item.Item>withKeyFunction(item ->
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item));
            items.addStoryBoard(HexCreatingItems.THOUGHT_KEY_HEXCASTING_INTRODUCTION,
                "hexcasting_introduction", HexCreatingPonder::introduction);
            items.addStoryBoard(HexCreatingItems.THOUGHT_KEY_BASIC_IOTA,
                "basic_iota", HexCreatingPonder::basicIota);
            items.addStoryBoard(HexCreatingItems.THOUGHT_KEY_VECTOR,
                "vector", HexCreatingPonder::vector);
            items.addStoryBoard(HexCreatingItems.THOUGHT_KEY_STACK,
                "stack", HexCreatingPonder::stack);
        }
    }

    private static void introduction(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("hexcasting_introduction", "HexCreating: Introduction");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        var board = util.grid().at(2, 1, 2);
        scene.world().setBlock(board, HexCreatingBlocks.BOARD.defaultBlockState(), false);
        scene.idle(10);
        scene.overlay().showText(70)
            .text("The Board holds pattern squares. Right-click it with a square to place one.")
            .pointAt(util.vector().centerOf(board))
            .placeNearTarget();
        scene.idle(80);
        scene.overlay().showControls(util.vector().topOf(board), com.zurrtum.create.catnip.math.Pointing.DOWN, 40)
            .rightClick();
        scene.idle(50);

        scene.overlay().showText(80)
            .text("Squares contain patterns. Combine them to design new spells.")
            .colored(PonderPalette.BLUE);
        scene.idle(90);
        scene.markAsFinished();
    }

    private static void basicIota(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("basic_iota", "HexCreating: Iota Basics");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        var board = util.grid().at(2, 1, 2);
        scene.world().setBlock(board, HexCreatingBlocks.BOARD.defaultBlockState(), false);
        scene.overlay().showOutlineWithText(util.select().position(board), 80)
            .text("An iota is a single piece of data: a number, vector, pattern, or list.")
            .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(80)
            .text("Printed Paper carries one iota and can be read by other HexCreating blocks.")
            .colored(PonderPalette.GREEN);
        scene.idle(90);
        scene.markAsFinished();
    }

    private static void vector(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("vector", "HexCreating: Vectors");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        var board = util.grid().at(2, 1, 2);
        scene.world().setBlock(board, HexCreatingBlocks.BOARD.defaultBlockState(), false);
        scene.overlay().showLine(PonderPalette.RED, util.vector().of(1.5, 1.5, 2.5), util.vector().of(3.5, 1.5, 2.5), 60);
        scene.overlay().showText(80)
            .text("Vectors are positions in the world. Purification patterns expose machine state as vectors.")
            .pointAt(util.vector().centerOf(board));
        scene.idle(90);
        scene.markAsFinished();
    }

    private static void stack(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("stack", "HexCreating: Stacks");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        var amplifier = util.grid().at(2, 1, 1);
        scene.world().setBlock(amplifier, HexCreatingBlocks.CIRCLE_AMPLIFIER.defaultBlockState()
            .setValue(pub.pigeon.yggdyy.hexcreating.blocks.circle_amplifier.CircleAmplifierBlock.FACING,
                Direction.UP), false);
        scene.overlay().showText(90)
            .text("The Circle Amplifier consumes rotation to widen a spell circle's working area.")
            .pointAt(util.vector().centerOf(amplifier))
            .placeNearTarget();
        scene.idle(100);

        scene.overlay().showText(80)
            .text("Higher speed means a larger area. Wrench it to change orientation.")
            .colored(PonderPalette.BLUE);
        scene.idle(90);
        scene.markAsFinished();
    }
}
