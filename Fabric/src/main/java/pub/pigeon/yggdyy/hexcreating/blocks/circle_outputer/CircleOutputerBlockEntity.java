package pub.pigeon.yggdyy.hexcreating.blocks.circle_outputer;

import at.petrak.hexcasting.api.block.HexBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlockEntities;

public class CircleOutputerBlockEntity extends HexBlockEntity {
    public CircleOutputerBlockEntity(BlockPos pos, BlockState state) {
        super(HexCreatingBlockEntities.CIRCLE_OUTPUTER, pos, state);
    }

    @Override
    protected void saveModData(CompoundTag tag) {
    }

    @Override
    protected void loadModData(CompoundTag tag) {
    }
}
