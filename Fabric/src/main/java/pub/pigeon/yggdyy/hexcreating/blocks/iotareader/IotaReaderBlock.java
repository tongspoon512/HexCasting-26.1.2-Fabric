package pub.pigeon.yggdyy.hexcreating.blocks.iotareader;

import com.zurrtum.create.content.kinetics.base.HorizontalKineticBlock;
import com.zurrtum.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlockEntities;

import java.util.List;

/**
 * 把传送带上物品里的 iota 读出并发送到 iota 网络的读取器。
 */
public class IotaReaderBlock extends HorizontalKineticBlock implements IBE<IotaReaderBlockEntity> {
    public IotaReaderBlock(Properties properties) {
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
    public Class<IotaReaderBlockEntity> getBlockEntityClass() {
        return IotaReaderBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends IotaReaderBlockEntity> getBlockEntityType() {
        return HexCreatingBlockEntities.IOTA_READER;
    }

    /**
     * 读取完成后向这些位置发送 iota 包，避免直接灌回自己所在的传送带。
     */
    public static List<BlockPos> getCanSendIotaPackToPos(BlockState state, BlockPos pos) {
        Direction.Axis axis = state.getValue(HORIZONTAL_FACING).getAxis();
        if (axis == Direction.Axis.X) {
            return List.of(pos.above(), pos.north(), pos.south());
        }
        if (axis == Direction.Axis.Z) {
            return List.of(pos.above(), pos.east(), pos.west());
        }
        return List.of(pos.above());
    }
}
