package at.petrak.hexcasting.client.render.be;

import at.petrak.hexcasting.client.RegisterClientStuff;
import at.petrak.hexcasting.client.render.GaslightingTracker;
import at.petrak.hexcasting.common.blocks.BlockQuenchedAllay;
import at.petrak.hexcasting.common.blocks.entity.BlockEntityQuenchedAllay;
import at.petrak.hexcasting.xplat.IClientXplatAbstractions;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// TODO: this doesn't cover the block being *behind* something. Is it possible to cleanly do that?
// it would probably require some depth-texture bullshit that I don't want to worry about
public class BlockEntityQuenchedAllayRenderer
    implements BlockEntityRenderer<BlockEntityQuenchedAllay, BlockEntityQuenchedAllayRenderer.State> {
    public static class State extends BlockEntityRenderState {
        public final BlockModelRenderState model = new BlockModelRenderState();
        public boolean visible = true;
    }

    private final BlockEntityRendererProvider.Context ctx;

    public BlockEntityQuenchedAllayRenderer(BlockEntityRendererProvider.Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BlockEntityQuenchedAllay blockEntity, State state, float partialTick,
        Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, crumbling);

        // https://github.com/MinecraftForge/MinecraftForge/blob/79dfdb0ace9694f9dd0f1d9e8c5c24a0ae2d77f8/patches/minecraft/net/minecraft/client/renderer/LevelRenderer.java.patch#L68
        // Forge fixes BEs rendering offscreen; Fabric doesn't!
        // So we do a special check on Fabric only
        var pos = blockEntity.getBlockPos();
        var aabb = new AABB(pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX() + 1, pos.getY() + 1,
            pos.getZ() + 1);
        state.visible = IClientXplatAbstractions.INSTANCE.fabricAdditionalQuenchFrustumCheck(aabb);

        BlockState bs = blockEntity.getBlockState();
        var idx = Math.abs(GaslightingTracker.getGaslightingAmount() % BlockQuenchedAllay.VARIANTS);
        List<BlockModel> variants =
            RegisterClientStuff.QUENCHED_ALLAY_VARIANTS.get(BuiltInRegistries.BLOCK.getKey(bs.getBlock()));
        BlockModel model = variants != null && idx < variants.size() ? variants.get(idx) : null;
        if (model != null) {
            model.update(state.model, bs, BlockDisplayContext.create(), state.blockPos.hashCode());
        } else {
            this.ctx.blockModelResolver().update(state.model, bs, BlockDisplayContext.create());
        }
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.visible) {
            state.model.submit(ps, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return false;
    }
}
