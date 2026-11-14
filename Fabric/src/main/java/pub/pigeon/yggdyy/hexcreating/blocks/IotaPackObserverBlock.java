package pub.pigeon.yggdyy.hexcreating.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

public class IotaPackObserverBlock extends Block implements EntityBlock {
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<Direction> FACING =
        BlockStateProperties.HORIZONTAL_FACING;
    public IotaPackObserverBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(BlockStateProperties.POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BlockStateProperties.POWER);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(BlockStateProperties.POWER);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SimpleIotaBlock.BufferBlockEntity(pos, state, true);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
        net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide()) {
            return;
        }
        int power = 0;
        if (level.getBlockEntity(pos.relative(state.getValue(FACING).getOpposite())) instanceof SimpleIotaBlock.BufferBlockEntity buffer
            && buffer.getIota() != null) {
            power = 15;
        }
        if (state.getValue(BlockStateProperties.POWER) != power) {
            level.setBlock(pos, state.setValue(BlockStateProperties.POWER, power), 3);
        }
    }
}
