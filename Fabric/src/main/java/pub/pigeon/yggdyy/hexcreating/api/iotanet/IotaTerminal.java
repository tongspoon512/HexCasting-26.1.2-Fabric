package pub.pigeon.yggdyy.hexcreating.api.iotanet;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class IotaTerminal {
    public int terminalId;
    public long lastAcceptTime = -1L;
    public List<Long> receivedId = new ArrayList<>();
    public final BlockEntity blockEntity;

    public IotaTerminal(BlockEntity blockEntity, int terminalId) {
        this.blockEntity = blockEntity;
        this.terminalId = terminalId;
    }

    public void readNbt(CompoundTag tag) {
        this.lastAcceptTime = tag.getLongOr(baseKey() + "last_accept_time", -1L);
        this.receivedId = new ArrayList<>();
        int length = tag.getIntOr(baseKey() + "received_id_length", 0);
        for (int i = 0; i < length; i++) {
            this.receivedId.add(tag.getLongOr(baseKey() + "received_id_" + i, 0L));
        }
    }

    public void writeNbt(CompoundTag tag) {
        tag.putLong(baseKey() + "last_accept_time", lastAcceptTime);
        tag.putInt(baseKey() + "received_id_length", receivedId.size());
        for (int i = 0; i < receivedId.size(); i++) {
            tag.putLong(baseKey() + "received_id_" + i, receivedId.get(i));
        }
    }

    public String baseKey() {
        return "iota_terminal_" + terminalId + "_";
    }

    public boolean canAcceptIotaPack(IotaPack pack) {
        if (this.blockEntity.getLevel() == null) {
            return false;
        }
        if (this.blockEntity.getLevel().getGameTime() == this.lastAcceptTime) {
            return !this.receivedId.contains(pack.id);
        }
        return true;
    }

    public void confirmAcceptIotaPack(IotaPack pack) {
        if (this.blockEntity.getLevel() == null) {
            return;
        }
        long now = this.blockEntity.getLevel().getGameTime();
        if (this.lastAcceptTime != now) {
            this.lastAcceptTime = now;
            this.receivedId = new ArrayList<>(List.of(pack.id));
        } else {
            this.receivedId.add(pack.id);
        }
        this.blockEntity.setChanged();
    }

    public BlockPos pos() {
        return this.blockEntity.getBlockPos();
    }
}
