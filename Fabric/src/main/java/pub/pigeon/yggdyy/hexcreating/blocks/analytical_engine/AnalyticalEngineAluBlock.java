package pub.pigeon.yggdyy.hexcreating.blocks.analytical_engine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import pub.pigeon.yggdyy.hexcreating.blocks.AnalyticalEngineBlock;

public class AnalyticalEngineAluBlock extends AnalyticalEngineBlock implements EntityBlock {
    public AnalyticalEngineAluBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnalyticalEngineAluBlockEntity(pos, state);
    }
}
