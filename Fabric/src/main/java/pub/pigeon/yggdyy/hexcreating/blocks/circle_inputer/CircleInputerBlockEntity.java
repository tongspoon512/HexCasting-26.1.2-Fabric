package pub.pigeon.yggdyy.hexcreating.blocks.circle_inputer;

import at.petrak.hexcasting.api.block.HexBlockEntity;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.api.casting.iota.ListIota;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IIotaTerminalHost;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaPack;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaTerminal;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlockEntities;

import java.util.ArrayList;
import java.util.List;

public class CircleInputerBlockEntity extends HexBlockEntity implements IIotaTerminalHost {
    private static final String TAG_IOTAS = "iotas";

    public List<Iota> iotas = List.of();
    public final IotaTerminal terminal = new IotaTerminal(this, 0);

    public CircleInputerBlockEntity(BlockPos pos, BlockState state) {
        super(HexCreatingBlockEntities.CIRCLE_INPUTER, pos, state);
    }

    @Override
    protected void saveModData(CompoundTag tag) {
        this.terminal.writeNbt(tag);
        CompoundTag list = new CompoundTag();
        list.putInt("length", this.iotas.size());
        for (int i = 0; i < this.iotas.size(); i++) {
            list.put("iota_" + i, IotaType.serialize(this.iotas.get(i)));
        }
        tag.put(TAG_IOTAS, list);
    }

    @Override
    protected void loadModData(CompoundTag tag) {
        this.terminal.readNbt(tag);
        List<Iota> loaded = new ArrayList<>();
        CompoundTag list = tag.getCompoundOrEmpty(TAG_IOTAS);
        int length = list.getIntOr("length", 0);
        for (int i = 0; i < length; i++) {
            if (this.level instanceof ServerLevel serverLevel) {
                var iotaTag = list.getCompound("iota_" + i);
                if (iotaTag.isPresent()) {
                    Iota iota = IotaType.deserialize(iotaTag.get(), serverLevel);
                    if (iota != null) {
                        loaded.add(iota);
                    }
                }
            }
        }
        this.iotas = loaded;
    }

    @Override
    public List<IotaTerminal> getTerminals() {
        return List.of(this.terminal);
    }

    @Override
    public boolean tryReceiveIotaPack(IotaPack pack, IotaTerminal from, IotaTerminal to) {
        if (!(this.level instanceof ServerLevel)) {
            return false;
        }
        CircleInputerBlock.InputType type = this.getBlockState().getValue(CircleInputerBlock.INPUT_TYPE);
        return switch (type) {
            case REJECT -> false;
            case ALWAYS_SINGLE -> {
                this.iotas = List.of(pack.iota);
                this.sync();
                yield true;
            }
            case TRY_MULTIPLE -> {
                if (pack.iota instanceof ListIota listIota) {
                    List<Iota> unpacked = new ArrayList<>();
                    listIota.subIotas().forEach(unpacked::add);
                    this.iotas = unpacked;
                } else {
                    this.iotas = List.of(pack.iota);
                }
                this.sync();
                yield true;
            }
        };
    }

    @Override
    public @Nullable IotaTerminal getDefaultTerminal(IotaTerminal from) {
        return this.terminal;
    }
}
