package pub.pigeon.yggdyy.hexcreating.client;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.level.material.Fluid;
import pub.pigeon.yggdyy.hexcreating.HexCreatingCompat;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlockEntities;
import pub.pigeon.yggdyy.hexcreating.client.render.BoardRenderer;
import pub.pigeon.yggdyy.hexcreating.fluids.HexCreatingFluids;

/**
 * L1 client wiring. Only loaded after the Create presence gate in {@link HexCreatingClientCompat}.
 */
public final class HexCreatingClientRegistration {
    private HexCreatingClientRegistration() {
    }

    public static void register() {
        registerFluid(HexCreatingFluids.MEDIA, HexCreatingFluids.FLOWING_MEDIA,
            "block/media_still", "block/media_flowing");
        registerFluid(HexCreatingFluids.SOUL, HexCreatingFluids.FLOWING_SOUL,
            "block/soul_still", "block/soul_flowing");

        BlockEntityRendererRegistry.register(HexCreatingBlockEntities.BOARD, BoardRenderer::new);
    }

    private static void registerFluid(Fluid source, Fluid flowing, String still, String flowingTexture) {
        FluidRenderingRegistry.register(source, flowing, new FluidModel.Unbaked(
            new Material(HexCreatingCompat.id(still), true),
            new Material(HexCreatingCompat.id(flowingTexture), true),
            null, null));
    }
}
