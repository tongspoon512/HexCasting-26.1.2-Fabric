package pub.pigeon.yggdyy.hexcreating;

import com.zurrtum.create.api.stress.BlockStressValues;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlocks;
import pub.pigeon.yggdyy.hexcreating.create.HexCreatingDisplaySources;
import pub.pigeon.yggdyy.hexcreating.listeners.RawSoulDropListener;

/**
 * L2 Create-specific wiring. Referenced only from {@link HexCreatingRegistration#init()}, which is
 * itself only called when Create is loaded.
 */
public final class HexCreatingCreateIntegration {
    private HexCreatingCreateIntegration() {
    }

    public static void init() {
        BlockStressValues.IMPACTS.register(HexCreatingBlocks.CIRCLE_AMPLIFIER, () -> 4.0);
        BlockStressValues.IMPACTS.register(HexCreatingBlocks.IOTA_READER, () -> 2.0);
        BlockStressValues.IMPACTS.register(HexCreatingBlocks.IOTA_WRITER, () -> 2.0);
        BlockStressValues.IMPACTS.register(HexCreatingBlocks.IOTA_WIRE_CONNECTOR, () -> 0.0);
        BlockStressValues.IMPACTS.register(HexCreatingBlocks.ANALYTICAL_ENGINE_KINETIC_INTERFACE, () -> 8.0);
        HexCreatingDisplaySources.register();
        ServerLivingEntityEvents.AFTER_DEATH.register(new RawSoulDropListener());
    }
}
