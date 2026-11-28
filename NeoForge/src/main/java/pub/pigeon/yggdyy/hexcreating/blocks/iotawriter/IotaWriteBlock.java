package pub.pigeon.yggdyy.hexcreating.blocks.iotawriter;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlockEntities;

/**
 * 把 iota 网络中收到的 iota 写到传送带物品上的写入器。
 */
public class IotaWriteBlock extends HorizontalKineticBlock implements IBE<IotaWriteBlockEntity> {
    public IotaWriteBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public Class<IotaWriteBlockEntity> getBlockEntityClass() {
        return IotaWriteBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends IotaWriteBlockEntity> getBlockEntityType() {
        return HexCreatingBlockEntities.IOTA_WRITER;
    }
}
