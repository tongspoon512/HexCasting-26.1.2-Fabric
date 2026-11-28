package pub.pigeon.yggdyy.hexcreating.fluids;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidType;
import pub.pigeon.yggdyy.hexcreating.HexCreatingCompat;

import java.util.function.BiConsumer;

/**
 * NeoForge 要求模组流体必须提供 FluidType（用于桶、流体交互与属性）。
 */
public final class HexCreatingFluidTypes {
    public static final FluidType MEDIA = new FluidType(FluidType.Properties.create()
        .descriptionId("fluid.hexcreating.media")
        .density(1000)
        .viscosity(1000)
        .canDrown(false)
        .canSwim(false)
        .canExtinguish(false));

    public static final FluidType SOUL = new FluidType(FluidType.Properties.create()
        .descriptionId("fluid.hexcreating.soul")
        .density(1200)
        .viscosity(1500)
        .lightLevel(7)
        .canDrown(false)
        .canSwim(false)
        .canExtinguish(false));

    private HexCreatingFluidTypes() {
    }

    public static void registerFluidTypes(BiConsumer<FluidType, Identifier> r) {
        r.accept(MEDIA, HexCreatingCompat.id("media"));
        r.accept(SOUL, HexCreatingCompat.id("soul"));
    }
}
