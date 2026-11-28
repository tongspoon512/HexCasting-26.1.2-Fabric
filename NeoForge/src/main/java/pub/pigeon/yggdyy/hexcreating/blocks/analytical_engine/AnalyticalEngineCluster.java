package pub.pigeon.yggdyy.hexcreating.blocks.analytical_engine;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable snapshot of the engine parts connected to a given block.
 * <p>
 * Parts join a cluster when they touch on one of the six block faces; the shape is free-form and
 * there is no multiblock controller.
 */
public record AnalyticalEngineCluster(@Nullable BlockPos controllerPos, int controllerCount,
                                      int aluCount, int kineticCount, float rpm) {
    public static final AnalyticalEngineCluster EMPTY =
        new AnalyticalEngineCluster(null, 0, 0, 0, 0f);

    public boolean hasController() {
        return this.controllerCount == 1 && this.controllerPos != null;
    }

    public boolean hasConflict() {
        return this.controllerCount > 1;
    }

    public int clampedAluCount() {
        return Math.max(0, Math.min(7, this.aluCount));
    }

    public int programLength() {
        return 128 + 128 * this.clampedAluCount();
    }

    public int stackDepth() {
        return 64 + 16 * this.clampedAluCount();
    }

    /**
     * Instructions per tick. Speed only scales the clock; a sub-32 RPM engine runs fractionally.
     */
    public int opBudget() {
        int multiplier = (int) (Math.abs(this.rpm) / 32f);
        multiplier = Math.max(0, Math.min(8, multiplier));
        return Math.min((1 + this.clampedAluCount()) * multiplier, 32);
    }
}
