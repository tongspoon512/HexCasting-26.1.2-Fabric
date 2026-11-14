package pub.pigeon.yggdyy.hexcreating.blocks.analytical_engine;

import at.petrak.hexcasting.api.block.HexBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlockEntities;

/**
 * Arithmetic/logic unit. Stateless: all of its contribution is derived from the cluster scan, so
 * it can be added or removed while the engine is running.
 */
public class AnalyticalEngineAluBlockEntity extends HexBlockEntity implements AnalyticalEnginePart {
    public AnalyticalEngineAluBlockEntity(BlockPos pos, BlockState state) {
        super(HexCreatingBlockEntities.ANALYTICAL_ENGINE_ALU, pos, state);
    }

    @Override
    public AnalyticalEngineCluster cluster() {
        return AnalyticalEngineClusterScanner.scan(this.level, this.worldPosition);
    }

    @Override
    protected void saveModData(CompoundTag tag) {
    }

    @Override
    protected void loadModData(CompoundTag tag) {
    }
}
