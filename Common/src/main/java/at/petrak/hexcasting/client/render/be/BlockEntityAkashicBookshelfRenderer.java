package at.petrak.hexcasting.client.render.be;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.client.render.WorldlyPatternRenderHelpers;
import at.petrak.hexcasting.common.blocks.akashic.BlockEntityAkashicBookshelf;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class BlockEntityAkashicBookshelfRenderer
    implements BlockEntityRenderer<BlockEntityAkashicBookshelf, BlockEntityAkashicBookshelfRenderer.State> {
    public static class State extends BlockEntityRenderState {
        @Nullable
        public HexPattern pattern;
        public BlockState blockState;
    }

    public BlockEntityAkashicBookshelfRenderer(BlockEntityRendererProvider.Context ctx) {
        // NO-OP
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BlockEntityAkashicBookshelf tile, State state, float partialTick, Vec3 cameraPos,
        ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTick, cameraPos, crumbling);
        state.pattern = tile.getPattern();
        state.blockState = tile.getBlockState();
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.pattern == null) {
            return;
        }

        WorldlyPatternRenderHelpers.renderPatternForAkashicBookshelf(state.blockPos.hashCode(), state.pattern, ps,
            collector, state.lightCoords, state.blockState);
    }
}
