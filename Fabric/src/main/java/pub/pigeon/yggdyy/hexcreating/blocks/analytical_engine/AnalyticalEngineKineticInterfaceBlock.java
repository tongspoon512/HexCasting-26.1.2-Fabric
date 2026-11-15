package pub.pigeon.yggdyy.hexcreating.blocks.analytical_engine;

import com.zurrtum.create.content.kinetics.base.DirectionalKineticBlock;
import com.zurrtum.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlockEntities;

/**
 * Couples the cluster to Create's kinetic network. Implementing {@link IBE} is what gives the
 * block a ticker; without it the kinetic block entity never attaches to the network and the
 * speed/stress readouts stay at zero.
 */
public class AnalyticalEngineKineticInterfaceBlock extends DirectionalKineticBlock
    implements IBE<AnalyticalEngineKineticInterfaceBlockEntity> {
    public AnalyticalEngineKineticInterfaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<AnalyticalEngineKineticInterfaceBlockEntity> getBlockEntityClass() {
        return AnalyticalEngineKineticInterfaceBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AnalyticalEngineKineticInterfaceBlockEntity> getBlockEntityType() {
        return HexCreatingBlockEntities.ANALYTICAL_ENGINE_KINETIC_INTERFACE;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        // 轴向两面都允许接轴，避免放置朝向导致“接不上动力网络”。
        return face.getAxis() == state.getValue(FACING).getAxis();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }
}
