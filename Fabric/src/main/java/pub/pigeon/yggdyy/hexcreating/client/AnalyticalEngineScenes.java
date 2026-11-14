package pub.pigeon.yggdyy.hexcreating.client;

import com.zurrtum.create.client.ponder.api.scene.SceneBuilder;
import com.zurrtum.create.client.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import pub.pigeon.yggdyy.hexcreating.blocks.AnalyticalEngineBlock;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlocks;

/**
 * Ponder scenes introducing the analytical engine. The scenes build the machine in place so that
 * the tutorial works with the empty base-plate structures shipped by HexCreating.
 */
public final class AnalyticalEngineScenes {
    private AnalyticalEngineScenes() {
    }

    public static void controller(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("analytical_engine_controller", "Analytical Engine Basics");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos controllerPos = new BlockPos(2, 1, 2);
        scene.world().setBlock(controllerPos,
            HexCreatingBlocks.ANALYTICAL_ENGINE_CONTROLLER.defaultBlockState()
                .setValue(AnalyticalEngineBlock.FACING, Direction.NORTH), true);
        scene.overlay().showText(80)
            .attachKeyFrame()
            .text("The Controller executes the program and owns the stack, ports and media buffer.")
            .pointAt(util.vector().blockSurface(controllerPos, Direction.UP))
            .placeNearTarget();
        scene.idle(90);

        scene.world().setBlock(new BlockPos(2, 1, 3),
            HexCreatingBlocks.ANALYTICAL_ENGINE_ALU.defaultBlockState(), true);
        scene.overlay().showText(80)
            .attachKeyFrame()
            .text("Each adjacent ALU adds program length, stack depth and instructions per tick.")
            .pointAt(util.vector().blockSurface(new BlockPos(2, 1, 3), Direction.UP))
            .placeNearTarget();
        scene.idle(90);
    }

    public static void program(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("analytical_engine_program", "Programs and Instructions");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos controllerPos = new BlockPos(2, 1, 2);
        scene.world().setBlock(controllerPos,
            HexCreatingBlocks.ANALYTICAL_ENGINE_CONTROLLER.defaultBlockState(), true);
        scene.overlay().showText(90)
            .attachKeyFrame()
            .text("Write a program onto a Paper Reel, then insert it into the Controller.")
            .pointAt(util.vector().blockSurface(controllerPos, Direction.UP))
            .placeNearTarget();
        scene.idle(100);

        scene.overlay().showText(90)
            .attachKeyFrame()
            .text("Jump, Jump-if-zero, Wait, Read, Emit and Halt drive patterns and the iota network.")
            .pointAt(util.vector().blockSurface(controllerPos, Direction.UP))
            .placeNearTarget();
        scene.idle(100);
    }

    public static void network(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("analytical_engine_network", "Iota Networking");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos controllerPos = new BlockPos(2, 1, 2);
        scene.world().setBlock(controllerPos,
            HexCreatingBlocks.ANALYTICAL_ENGINE_CONTROLLER.defaultBlockState(), true);
        scene.overlay().showText(80)
            .attachKeyFrame()
            .text("Wrench a face to turn it into an Input, Output, Program or Redstone port.")
            .pointAt(util.vector().blockSurface(controllerPos, Direction.UP))
            .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(80)
            .attachKeyFrame()
            .text("Adjacent iota connectors relay packs between machines; channels keep them apart.")
            .pointAt(util.vector().blockSurface(controllerPos, Direction.EAST))
            .placeNearTarget();
        scene.idle(90);
    }
}
