package pub.pigeon.yggdyy.hexcreating.blocks.iotawriter;

import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.api.casting.iota.NullIota;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IIotaTerminalHost;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaPack;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaTerminal;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlockEntities;

import java.util.List;

/**
 * 写入器方块实体：既是 iota 网络端点，也是 Create 动力网络上的应力消耗者。
 */
public class IotaWriteBlockEntity extends KineticBlockEntity implements IIotaTerminalHost {
    private static final String TAG_KEY = "hexcreating_write";
    private static final String IOTA_KEY = "local_iota";

    private final IotaTerminal terminal = new IotaTerminal(this, 0);
    private Iota iota = new NullIota();
    @Nullable
    private IotaWriteBehaviour writeBehaviour;

    public IotaWriteBlockEntity(BlockPos pos, BlockState state) {
        super(HexCreatingBlockEntities.IOTA_WRITER, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
        super.addBehaviours(behaviours);
        this.writeBehaviour = new IotaWriteBehaviour(this);
        behaviours.add(this.writeBehaviour);
    }

    @Override
    public float calculateStressApplied() {
        this.lastStressApplied = 2f;
        return this.lastStressApplied;
    }

    public Iota getIota() {
        return this.iota;
    }

    public @Nullable IotaWriteBehaviour getWriteBehaviour() {
        return this.writeBehaviour;
    }

    @Override
    public List<IotaTerminal> getTerminals() {
        return List.of(this.terminal);
    }

    @Override
    public boolean tryReceiveIotaPack(IotaPack pack, IotaTerminal from, IotaTerminal to) {
        this.iota = pack.iota;
        this.sync();
        return true;
    }

    @Override
    public @Nullable IotaTerminal getDefaultTerminal(IotaTerminal from) {
        return this.terminal;
    }

    public void sync() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void write(ValueOutput output, boolean clientPacket) {
        super.write(output, clientPacket);
        CompoundTag tag = new CompoundTag();
        this.terminal.writeNbt(tag);
        tag.put(IOTA_KEY, IotaType.serialize(this.iota));
        output.store(TAG_KEY, CompoundTag.CODEC, tag);
    }

    @Override
    protected void read(ValueInput input, boolean clientPacket) {
        super.read(input, clientPacket);
        CompoundTag tag = input.read(TAG_KEY, CompoundTag.CODEC).orElseGet(CompoundTag::new);
        this.terminal.readNbt(tag);
        if (this.level instanceof ServerLevel serverLevel) {
            tag.getCompound(IOTA_KEY).ifPresent(saved -> {
                Iota read = IotaType.deserialize(saved, serverLevel);
                if (read != null) {
                    this.iota = read;
                }
            });
        }
    }
}
