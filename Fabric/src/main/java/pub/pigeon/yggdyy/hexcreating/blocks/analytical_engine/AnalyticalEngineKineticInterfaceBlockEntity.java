package pub.pigeon.yggdyy.hexcreating.blocks.analytical_engine;

import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlockEntities;

/**
 * Couples the cluster to Create's kinetic network. The 8.0 SU base is registered as the block's
 * impact; every ALU adds 4.0 SU on top of it.
 */
public class AnalyticalEngineKineticInterfaceBlockEntity extends KineticBlockEntity
    implements AnalyticalEnginePart {
    public static final float BASE_STRESS = 8.0f;
    public static final float STRESS_PER_ALU = 4.0f;

    public AnalyticalEngineKineticInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(HexCreatingBlockEntities.ANALYTICAL_ENGINE_KINETIC_INTERFACE, pos, state);
    }

    @Override
    public AnalyticalEngineCluster cluster() {
        return AnalyticalEngineClusterScanner.scan(this.level, this.worldPosition);
    }

    @Override
    public float calculateStressApplied() {
        return BASE_STRESS + STRESS_PER_ALU * this.cluster().clampedAluCount();
    }
}
