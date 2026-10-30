package at.petrak.hexcasting.client.entity;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.client.render.WorldlyPatternRenderHelpers;
import at.petrak.hexcasting.common.entities.EntityWallScroll;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

public class WallScrollRenderer extends EntityRenderer<EntityWallScroll, WallScrollRenderer.State> {
    private static final Identifier PRISTINE_BG_LARGE = modLoc("textures/entity/scroll_large.png");
    private static final Identifier PRISTINE_BG_MEDIUM = modLoc("textures/entity/scroll_medium.png");
    private static final Identifier PRISTINE_BG_SMOL = modLoc("textures/block/scroll_paper.png");
    private static final Identifier ANCIENT_BG_LARGE = modLoc("textures/entity/scroll_ancient_large.png");
    private static final Identifier ANCIENT_BG_MEDIUM = modLoc("textures/entity/scroll_ancient_medium.png");
    private static final Identifier ANCIENT_BG_SMOL = modLoc("textures/block/ancient_scroll_paper.png");

    public static class State extends EntityRenderState {
        public Direction direction = Direction.SOUTH;
        @Nullable
        public HexPattern pattern;
        public int blockSize;
        public boolean showsStrokeOrder;
        public boolean ancient;
        public int seed;
    }

    public WallScrollRenderer(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityWallScroll wallScroll, State state, float partialTicks) {
        super.extractRenderState(wallScroll, state, partialTicks);
        state.direction = wallScroll.getDirection();
        state.pattern = wallScroll.pattern;
        state.blockSize = wallScroll.blockSize;
        state.showsStrokeOrder = wallScroll.getShowsStrokeOrder();
        state.ancient = wallScroll.isAncient;
        state.seed = wallScroll.getPos().hashCode();
        state.lightCoords = this.getPackedLightCoords(wallScroll, partialTicks);
    }

    // I do as the PaintingRenderer guides
    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose();

        ps.mulPose(Axis.YP.rotationDegrees(180f - state.direction.get2DDataValue() * 90f));
        ps.mulPose(Axis.ZP.rotationDegrees(180f));

        int light = state.lightCoords;
        int blockSize = state.blockSize;

        {
            ps.pushPose();
            // X is right, Y is down, Z is *in*
            // Our origin will be the lower-left corner of the scroll touching the wall
            // (so it has "negative" thickness)
            ps.translate(-blockSize / 2f, -blockSize / 2f, 1f / 32f);

            float dx = blockSize, dy = blockSize, dz = -1f / 16f;
            float margin = 1f / 48f;

            RenderType layer = RenderTypes.entityCutout(this.getTextureLocation(state));

            // Remember: CCW
            collector.submitCustomGeometry(ps, layer, (pose, verts) -> {
                // Front face
                vertex(pose, verts, light, 0, 0, dz, 0, 0, 0, 0, -1);
                vertex(pose, verts, light, 0, dy, dz, 0, 1, 0, 0, -1);
                vertex(pose, verts, light, dx, dy, dz, 1, 1, 0, 0, -1);
                vertex(pose, verts, light, dx, 0, dz, 1, 0, 0, 0, -1);
                // Back face
                vertex(pose, verts, light, 0, 0, 0, 0, 0, 0, 0, 1);
                vertex(pose, verts, light, dx, 0, 0, 1, 0, 0, 0, 1);
                vertex(pose, verts, light, dx, dy, 0, 1, 1, 0, 0, 1);
                vertex(pose, verts, light, 0, dy, 0, 0, 1, 0, 0, 1);
                // Top face
                vertex(pose, verts, light, 0, 0, 0, 0, 0, 0, -1, 0);
                vertex(pose, verts, light, 0, 0, dz, 0, margin, 0, -1, 0);
                vertex(pose, verts, light, dx, 0, dz, 1, margin, 0, -1, 0);
                vertex(pose, verts, light, dx, 0, 0, 1, 0, 0, -1, 0);
                // Left face
                vertex(pose, verts, light, 0, 0, 0, 0, 0, -1, 0, 0);
                vertex(pose, verts, light, 0, dy, 0, 0, 1, -1, 0, 0);
                vertex(pose, verts, light, 0, dy, dz, margin, 1, -1, 0, 0);
                vertex(pose, verts, light, 0, 0, dz, margin, 0, -1, 0, 0);
                // Right face
                vertex(pose, verts, light, dx, 0, dz, 1 - margin, 0, 1, 0, 0);
                vertex(pose, verts, light, dx, dy, dz, 1 - margin, 1, 1, 0, 0);
                vertex(pose, verts, light, dx, dy, 0, 1, 1, 1, 0, 0);
                vertex(pose, verts, light, dx, 0, 0, 1, 0, 1, 0, 0);
                // Bottom face
                vertex(pose, verts, light, 0, dy, dz, 0, 1 - margin, 0, 1, 0);
                vertex(pose, verts, light, 0, dy, 0, 0, 1, 0, 1, 0);
                vertex(pose, verts, light, dx, dy, 0, 1, 1, 0, 1, 0);
                vertex(pose, verts, light, dx, dy, dz, 1, 1 - margin, 0, 1, 0);
            });

            ps.popPose();

            if (state.pattern != null) {
                WorldlyPatternRenderHelpers.renderPatternForScroll(state.pattern, state.seed, ps, collector, light,
                    blockSize, state.showsStrokeOrder);
            }
        }

        ps.popPose();
        super.submit(state, ps, collector, camera);
    }

    private Identifier getTextureLocation(State state) {
        if (state.ancient) {
            if (state.blockSize <= 1) {
                return ANCIENT_BG_SMOL;
            } else if (state.blockSize == 2) {
                return ANCIENT_BG_MEDIUM;
            } else {
                return ANCIENT_BG_LARGE;
            }
        } else {
            if (state.blockSize <= 1) {
                return PRISTINE_BG_SMOL;
            } else if (state.blockSize == 2) {
                return PRISTINE_BG_MEDIUM;
            } else {
                return PRISTINE_BG_LARGE;
            }
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer verts, int light, float x, float y, float z,
        float u, float v, float nx, float ny, float nz) {
        verts.addVertex(pose, x, y, z)
            .setColor(0xffffffff)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, nx, ny, nz);
    }
}
