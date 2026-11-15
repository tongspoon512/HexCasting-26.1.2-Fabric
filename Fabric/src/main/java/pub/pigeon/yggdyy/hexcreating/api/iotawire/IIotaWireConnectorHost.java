package pub.pigeon.yggdyy.hexcreating.api.iotawire;

import pub.pigeon.yggdyy.hexcreating.api.iotanet.IIotaTerminalHost;

import java.util.List;

/**
 * A block entity whose faces can host independent iota wire endpoints.
 */
public interface IIotaWireConnectorHost extends IIotaTerminalHost {
    List<IotaWireConnector> getIotaWireConnectors();

    void sync();
}
