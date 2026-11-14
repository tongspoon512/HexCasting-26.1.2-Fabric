package pub.pigeon.yggdyy.hexcreating.blocks.analytical_engine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Breadth-first scan over the six-neighbour graph of engine parts.
 */
public final class AnalyticalEngineClusterScanner {
    private static final int MAX_PARTS = 64;

    private AnalyticalEngineClusterScanner() {
    }

    public static AnalyticalEngineCluster scan(Level level, BlockPos origin) {
        if (level == null || !(level.getBlockEntity(origin) instanceof AnalyticalEnginePart)) {
            return AnalyticalEngineCluster.EMPTY;
        }

        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(origin);
        queue.add(origin);

        BlockPos controller = null;
        int controllers = 0;
        int alu = 0;
        int kinetic = 0;
        float rpm = 0f;

        while (!queue.isEmpty() && seen.size() <= MAX_PARTS) {
            BlockPos pos = queue.removeFirst();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AnalyticalEngineControllerBlockEntity) {
                controllers++;
                if (controller == null) {
                    controller = pos;
                }
            } else if (be instanceof AnalyticalEngineAluBlockEntity) {
                alu++;
            } else if (be instanceof AnalyticalEngineKineticInterfaceBlockEntity kineticInterface) {
                kinetic++;
                float speed = Math.abs(kineticInterface.getSpeed());
                if (speed > rpm) {
                    rpm = speed;
                }
            } else {
                continue;
            }

            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (seen.add(next) && level.getBlockEntity(next) instanceof AnalyticalEnginePart) {
                    queue.add(next);
                }
            }
        }

        return new AnalyticalEngineCluster(controller, controllers, alu, kinetic, rpm);
    }
}
