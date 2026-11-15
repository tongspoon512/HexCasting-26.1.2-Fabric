package pub.pigeon.yggdyy.hexcreating.api.iotawire;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IIotaTerminalHost;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaNetProcessor;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaPack;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaTerminal;
import pub.pigeon.yggdyy.hexcreating.items.HexCreatingItems;

import java.util.ArrayList;
import java.util.List;

/**
 * One wire socket on a connector block. Holds the ids of all sockets it is wired to.
 */
public class IotaWireConnector {
    public record Link(BlockPos pos, int terminalId) {
    }

    @NotNull
    public final IotaTerminal terminal;
    public final AABB clickBox;
    public final List<Link> others = new ArrayList<>();

    public IotaWireConnector(@NotNull IotaTerminal terminal, AABB clickBox) {
        this.terminal = terminal;
        this.clickBox = clickBox;
    }

    public Vec3 getCenter() {
        return this.clickBox.getCenter();
    }

    public String getBaseNbtKey() {
        return "iota_connector_" + this.terminal.terminalId + "_";
    }

    public void writeNbt(CompoundTag tag) {
        this.terminal.writeNbt(tag);
        tag.putInt(getBaseNbtKey() + "others_length", this.others.size());
        for (int i = 0; i < this.others.size(); i++) {
            String posKey = getBaseNbtKey() + "others_" + i + "_pos_";
            BlockPos pos = this.others.get(i).pos();
            tag.putInt(posKey + "x", pos.getX());
            tag.putInt(posKey + "y", pos.getY());
            tag.putInt(posKey + "z", pos.getZ());
            tag.putInt(getBaseNbtKey() + "others_" + i + "_id", this.others.get(i).terminalId());
        }
    }

    public void readNbt(CompoundTag tag) {
        this.terminal.readNbt(tag);
        this.others.clear();
        int size = tag.getIntOr(getBaseNbtKey() + "others_length", 0);
        for (int i = 0; i < size; i++) {
            String posKey = getBaseNbtKey() + "others_" + i + "_pos_";
            BlockPos pos = new BlockPos(
                tag.getIntOr(posKey + "x", 0),
                tag.getIntOr(posKey + "y", 0),
                tag.getIntOr(posKey + "z", 0)
            );
            int id = tag.getIntOr(getBaseNbtKey() + "others_" + i + "_id", 0);
            this.others.add(new Link(pos, id));
        }
    }

    /**
     * Server side only: remove every wire attached to this socket and drop the wire items.
     */
    public void disConnectAll() {
        if (!(this.terminal.blockEntity.getLevel() instanceof ServerLevel level)) {
            return;
        }
        List<Link> copy = List.copyOf(this.others);
        for (Link link : copy) {
            // Make sure the other chunk is loaded before touching the other socket.
            level.getBlockEntity(link.pos());
            IotaWireProcessor.tryDisConnect2Connectors(level, this.terminal.pos(), this.terminal.terminalId,
                link.pos(), link.terminalId());
        }
        if (!copy.isEmpty()) {
            ItemStack stack = new ItemStack(HexCreatingItems.IOTA_WIRE);
            stack.setCount(copy.size());
            Vec3 center = this.terminal.pos().getCenter();
            level.addFreshEntity(new ItemEntity(level, center.x, center.y, center.z, stack));
        }
        this.terminal.blockEntity.setChanged();
    }

    public void sendIotaPackToAll(IotaPack pack) {
        if (!(this.terminal.blockEntity.getLevel() instanceof ServerLevel level)) {
            return;
        }
        for (Link link : List.copyOf(this.others)) {
            BlockEntity be = level.getBlockEntity(link.pos());
            if (!(be instanceof IIotaWireConnectorHost host)) {
                continue;
            }
            IotaWireConnector target = IotaWireProcessor.tryGetConnector(level, link.pos(), link.terminalId());
            if (target == null) {
                continue;
            }
            IotaNetProcessor.trySendIotaPack(pack.copyWithSameId(), this.terminal, host, target.terminal);
        }
    }
}
