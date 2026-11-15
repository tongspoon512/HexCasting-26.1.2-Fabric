package pub.pigeon.yggdyy.hexcreating.client;

import com.zurrtum.create.client.ponder.api.scene.SceneBuilder;
import com.zurrtum.create.client.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import pub.pigeon.yggdyy.hexcreating.blocks.AnalyticalEngineBlock;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlocks;
import pub.pigeon.yggdyy.hexcreating.items.HexCreatingItems;

/**
 * Ponder scenes introducing the analytical engine. The scenes build the machine in place so that
 * the tutorial works with the empty base-plate structures shipped by HexCreating.
 */
public final class AnalyticalEngineScenes {
    private AnalyticalEngineScenes() {
    }

    /**
     * Reveals the area the machine is built in. Blocks set with {@code world().setBlock} are only
     * rendered once the section that contains them is shown.
     */
    private static void showArea(SceneBuilder scene, SceneBuildingUtil util,
                                 int x1, int y1, int z1, int x2, int y2, int z2) {
        scene.world().showSection(util.select().fromTo(x1, y1, z1, x2, y2, z2), Direction.DOWN);
    }

    private static BlockPos place(SceneBuilder scene, BlockPos pos, BlockState state) {
        scene.world().setBlock(pos, state, false);
        return pos;
    }

    public static void controller(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("analytical_engine_controller", "Analytical Engine Basics");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        showArea(scene, util, 1, 1, 1, 3, 1, 3);
        scene.idle(10);

        BlockPos controllerPos = place(scene, new BlockPos(2, 1, 2),
            HexCreatingBlocks.ANALYTICAL_ENGINE_CONTROLLER.defaultBlockState()
                .setValue(AnalyticalEngineBlock.FACING, Direction.NORTH));
        scene.overlay().showText(80)
            .attachKeyFrame()
            .text("The Controller executes the program and owns the stack, ports and media buffer.")
            .pointAt(util.vector().blockSurface(controllerPos, Direction.UP))
            .placeNearTarget();
        scene.idle(90);

        BlockPos aluPos = place(scene, new BlockPos(2, 1, 3),
            HexCreatingBlocks.ANALYTICAL_ENGINE_ALU.defaultBlockState());
        scene.overlay().showText(80)
            .attachKeyFrame()
            .text("Each adjacent ALU adds program length, stack depth and instructions per tick.")
            .pointAt(util.vector().blockSurface(aluPos, Direction.UP))
            .placeNearTarget();
        scene.idle(90);

        BlockPos interfacePos = place(scene, new BlockPos(2, 1, 1),
            HexCreatingBlocks.ANALYTICAL_ENGINE_KINETIC_INTERFACE.defaultBlockState());
        scene.overlay().showText(80)
            .attachKeyFrame()
            .text("The Kinetic Interface feeds rotational force in and turns it into media.")
            .pointAt(util.vector().blockSurface(interfacePos, Direction.UP))
            .placeNearTarget();
        scene.idle(90);
    }

    public static void program(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("analytical_engine_program", "Programs and Instructions");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        showArea(scene, util, 2, 1, 2, 2, 1, 2);
        scene.idle(10);

        BlockPos controllerPos = place(scene, new BlockPos(2, 1, 2),
            HexCreatingBlocks.ANALYTICAL_ENGINE_CONTROLLER.defaultBlockState());
        scene.overlay().showText(90)
            .attachKeyFrame()
            .text("Write a program onto a Paper Reel, then insert it into the Controller.")
            .pointAt(util.vector().blockSurface(controllerPos, Direction.UP))
            .placeNearTarget();
        scene.idle(40);

        scene.world().createItemEntity(new Vec3(2.5, 2.2, 2.5), Vec3.ZERO,
            new ItemStack(HexCreatingItems.PAPER_REEL));
        scene.idle(60);

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
        showArea(scene, util, 1, 1, 1, 3, 1, 3);
        scene.idle(10);

        BlockPos controllerPos = place(scene, new BlockPos(2, 1, 2),
            HexCreatingBlocks.ANALYTICAL_ENGINE_CONTROLLER.defaultBlockState());
        scene.overlay().showText(80)
            .attachKeyFrame()
            .text("Wrench a face to turn it into an Input, Output, Program or Redstone port.")
            .pointAt(util.vector().blockSurface(controllerPos, Direction.UP))
            .placeNearTarget();
        scene.idle(90);

        BlockPos connectorPos = place(scene, new BlockPos(2, 1, 3),
            HexCreatingBlocks.IOTA_WIRE_CONNECTOR.defaultBlockState());
        place(scene, new BlockPos(2, 1, 1), HexCreatingBlocks.IOTA_PACK_OBSERVER.defaultBlockState());
        scene.overlay().showText(80)
            .attachKeyFrame()
            .text("Adjacent iota connectors relay packs between machines; channels keep them apart.")
            .pointAt(util.vector().blockSurface(connectorPos, Direction.UP))
            .placeNearTarget();
        scene.idle(90);
    }
}
