package pub.pigeon.yggdyy.hexcreating.api.iotawire;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class IotaWireProcessor {
    private IotaWireProcessor() {
    }

    @Nullable
    public static IotaWireConnector tryGetConnector(Level level, BlockPos pos, int terminalId) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IIotaWireConnectorHost host) {
            for (IotaWireConnector connector : host.getIotaWireConnectors()) {
                if (connector.terminal.terminalId == terminalId) {
                    return connector;
                }
            }
        }
        return null;
    }

    public static boolean tryConnect2Connectors(ServerLevel level, BlockPos pos1, int id1, BlockPos pos2, int id2) {
        if (pos1.equals(pos2) && id1 == id2) {
            return false;
        }
        IotaWireConnector c1 = tryGetConnector(level, pos1, id1);
        IotaWireConnector c2 = tryGetConnector(level, pos2, id2);
        if (c1 == null || c2 == null || isConnected(c1, c2)) {
            return false;
        }
        c1.others.add(new IotaWireConnector.Link(pos2, id2));
        c2.others.add(new IotaWireConnector.Link(pos1, id1));
        c1.terminal.blockEntity.setChanged();
        c2.terminal.blockEntity.setChanged();
        ((IIotaWireConnectorHost) c1.terminal.blockEntity).sync();
        ((IIotaWireConnectorHost) c2.terminal.blockEntity).sync();
        return true;
    }

    public static boolean isConnected(IotaWireConnector c1, IotaWireConnector c2) {
        for (IotaWireConnector.Link link : c1.others) {
            if (link.pos().equals(c2.terminal.pos()) && link.terminalId() == c2.terminal.terminalId) {
                return true;
            }
        }
        return false;
    }

    public static boolean tryDisConnect2Connectors(ServerLevel level, BlockPos pos1, int id1, BlockPos pos2, int id2) {
        IotaWireConnector c1 = tryGetConnector(level, pos1, id1);
        IotaWireConnector c2 = tryGetConnector(level, pos2, id2);
        if (c1 == null || c2 == null || !isConnected(c1, c2)) {
            return false;
        }
        c1.others.removeIf(link -> link.pos().equals(pos2) && link.terminalId() == id2);
        c2.others.removeIf(link -> link.pos().equals(pos1) && link.terminalId() == id1);
        c1.terminal.blockEntity.setChanged();
        c2.terminal.blockEntity.setChanged();
        ((IIotaWireConnectorHost) c1.terminal.blockEntity).sync();
        ((IIotaWireConnectorHost) c2.terminal.blockEntity).sync();
        return true;
    }
}
