package pub.pigeon.yggdyy.hexcreating.blocks.train_gate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class TrainGateCoreBlock extends Block implements EntityBlock {
    public TrainGateCoreBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrainGateCoreBlockEntity(pos, state);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TrainGateCoreBlockEntity core) {
            core.tryDestroyGate();
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
