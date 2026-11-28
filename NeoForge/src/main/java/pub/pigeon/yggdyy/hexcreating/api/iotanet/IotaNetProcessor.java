package pub.pigeon.yggdyy.hexcreating.api.iotanet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class IotaNetProcessor {
    private IotaNetProcessor() {
    }

    public static boolean trySendIotaPack(IotaPack pack, IotaTerminal from, IIotaTerminalHost host,
        IotaTerminal to) {
        if (to.canAcceptIotaPack(pack) && host.tryReceiveIotaPack(pack, from, to)) {
            to.confirmAcceptIotaPack(pack);
            return true;
        }
        return false;
    }

    public static boolean trySendIotaPack(IotaPack pack, IotaTerminal from, BlockPos target, ServerLevel level) {
        if (level.getBlockEntity(target) instanceof IIotaTerminalHost host) {
            IotaTerminal to = host.getDefaultTerminal(from);
            return to != null && trySendIotaPack(pack, from, host, to);
        }
        return false;
    }
}
