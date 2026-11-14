package pub.pigeon.yggdyy.hexcreating;

import com.zurrtum.create.api.stress.BlockStressValues;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlocks;
import pub.pigeon.yggdyy.hexcreating.create.HexCreatingDisplaySources;

/**
 * L2 Create-specific wiring. Referenced only from {@link HexCreatingRegistration#init()}, which is
 * itself only called when Create is loaded.
 */
public final class HexCreatingCreateIntegration {
    private HexCreatingCreateIntegration() {
    }

    public static void init() {
        BlockStressValues.IMPACTS.register(HexCreatingBlocks.CIRCLE_AMPLIFIER, () -> 4.0);
        BlockStressValues.IMPACTS.register(HexCreatingBlocks.IOTA_READER, () -> 1.0);
        BlockStressValues.IMPACTS.register(HexCreatingBlocks.IOTA_WRITER, () -> 1.0);
        BlockStressValues.IMPACTS.register(HexCreatingBlocks.IOTA_WIRE_CONNECTOR, () -> 0.0);
        BlockStressValues.IMPACTS.register(HexCreatingBlocks.ANALYTICAL_ENGINE_KINETIC_INTERFACE, () -> 8.0);
        HexCreatingDisplaySources.register();
    }
}
