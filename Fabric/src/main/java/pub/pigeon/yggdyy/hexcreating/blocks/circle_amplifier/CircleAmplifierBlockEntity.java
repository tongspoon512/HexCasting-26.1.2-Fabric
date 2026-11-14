package pub.pigeon.yggdyy.hexcreating.blocks.circle_amplifier;

import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlockEntities;

public class CircleAmplifierBlockEntity extends KineticBlockEntity {
    public CircleAmplifierBlockEntity(BlockPos pos, BlockState state) {
        super(HexCreatingBlockEntities.CIRCLE_AMPLIFIER, pos, state);
    }
}
