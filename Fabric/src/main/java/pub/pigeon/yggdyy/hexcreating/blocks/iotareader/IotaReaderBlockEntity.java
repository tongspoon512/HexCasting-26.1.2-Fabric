package pub.pigeon.yggdyy.hexcreating.blocks.iotareader;

import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlockEntities;

import java.util.List;

/**
 * 读取器方块实体。继承 {@link KineticBlockEntity} 后才会接入 Create 的应力网络。
 */
public class IotaReaderBlockEntity extends KineticBlockEntity {
    @Nullable
    private IotaReadBehaviour readBehaviour;

    public IotaReaderBlockEntity(BlockPos pos, BlockState state) {
        super(HexCreatingBlockEntities.IOTA_READER, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
        super.addBehaviours(behaviours);
        this.readBehaviour = new IotaReadBehaviour(this);
        behaviours.add(this.readBehaviour);
    }

    @Override
    public float calculateStressApplied() {
        this.lastStressApplied = 2f;
        return this.lastStressApplied;
    }

    public List<BlockPos> getCanSendIotaPackToPos() {
        return IotaReaderBlock.getCanSendIotaPackToPos(this.getBlockState(), this.getBlockPos());
    }

    public @Nullable IotaReadBehaviour getReadBehaviour() {
        return this.readBehaviour;
    }

    public void sync() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }
}
