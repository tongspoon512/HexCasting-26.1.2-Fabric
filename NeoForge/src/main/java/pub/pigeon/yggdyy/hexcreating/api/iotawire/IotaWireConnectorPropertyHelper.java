package pub.pigeon.yggdyy.hexcreating.api.iotawire;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import pub.pigeon.yggdyy.hexcreating.blocks.IotaWireConnectorBlock;

import java.util.List;

/**
 * Maps the six per-face terminals of a connector to their blockstate property, box and id.
 * The id order is the one used by the upstream mod and by saved wires.
 */
public final class IotaWireConnectorPropertyHelper {
    private IotaWireConnectorPropertyHelper() {
    }

    public static BooleanProperty getPropertyByDir(Direction dir) {
        return switch (dir) {
            case UP -> IotaWireConnectorBlock.UP;
            case DOWN -> IotaWireConnectorBlock.DOWN;
            case NORTH -> IotaWireConnectorBlock.NORTH;
            case SOUTH -> IotaWireConnectorBlock.SOUTH;
            case WEST -> IotaWireConnectorBlock.WEST;
            case EAST -> IotaWireConnectorBlock.EAST;
        };
    }

    public static List<BooleanProperty> getAllProperty() {
        return List.of(IotaWireConnectorBlock.UP, IotaWireConnectorBlock.DOWN,
            IotaWireConnectorBlock.NORTH, IotaWireConnectorBlock.SOUTH,
            IotaWireConnectorBlock.WEST, IotaWireConnectorBlock.EAST);
    }

    public static AABB getBox(BooleanProperty property) {
        if (property == IotaWireConnectorBlock.UP) {
            return IotaWireConnectorBlock.UP_BOX;
        }
        if (property == IotaWireConnectorBlock.DOWN) {
            return IotaWireConnectorBlock.DOWN_BOX;
        }
        if (property == IotaWireConnectorBlock.NORTH) {
            return IotaWireConnectorBlock.NORTH_BOX;
        }
        if (property == IotaWireConnectorBlock.SOUTH) {
            return IotaWireConnectorBlock.SOUTH_BOX;
        }
        if (property == IotaWireConnectorBlock.WEST) {
            return IotaWireConnectorBlock.WEST_BOX;
        }
        return IotaWireConnectorBlock.EAST_BOX;
    }

    public static int getTerminalIdByProperty(BooleanProperty property) {
        if (property == IotaWireConnectorBlock.UP) {
            return 0;
        }
        if (property == IotaWireConnectorBlock.DOWN) {
            return 1;
        }
        if (property == IotaWireConnectorBlock.NORTH) {
            return 2;
        }
        if (property == IotaWireConnectorBlock.SOUTH) {
            return 3;
        }
        if (property == IotaWireConnectorBlock.WEST) {
            return 4;
        }
        return 5;
    }

    public static Direction getDirByProperty(BooleanProperty property) {
        if (property == IotaWireConnectorBlock.UP) {
            return Direction.UP;
        }
        if (property == IotaWireConnectorBlock.DOWN) {
            return Direction.DOWN;
        }
        if (property == IotaWireConnectorBlock.NORTH) {
            return Direction.NORTH;
        }
        if (property == IotaWireConnectorBlock.SOUTH) {
            return Direction.SOUTH;
        }
        if (property == IotaWireConnectorBlock.WEST) {
            return Direction.WEST;
        }
        return Direction.EAST;
    }
}
