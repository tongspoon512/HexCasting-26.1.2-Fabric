package pub.pigeon.yggdyy.hexcreating.client.render;

import at.petrak.hexcasting.api.casting.iota.PatternIota;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.api.item.IotaHolderItem;
import at.petrak.hexcasting.client.render.PatternColors;
import at.petrak.hexcasting.client.render.PatternRenderer;
import at.petrak.hexcasting.client.render.WorldMeshSink;
import at.petrak.hexcasting.client.render.WorldlyPatternRenderHelpers;
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pub.pigeon.yggdyy.hexcreating.blocks.board.BoardBlock;
import pub.pigeon.yggdyy.hexcreating.blocks.board.BoardBlockEntity;

/**
 * Draws the "movable type" (square) tiles slotted into a typesetting board and the pattern of each
 * of them. The 4x4 grid geometry mirrors {@link BoardBlock#slotFromHit}; the transform follows the
 * original hexcreating board renderer (corner-based slots, pattern scaled to a quarter block).
 */
public class BoardRenderer implements BlockEntityRenderer<BoardBlockEntity, BoardRenderer.State> {
    private static final float CELL = 0.25f;
    private static final float GRID_ROWS_AND_COLS = 4f;
    private static final float TILE_OFFSET = 0.125f;
    private static final float TILE_THICKNESS = 0.125f;
    private static final float PATTERN_OFFSET = 0.13f;
    // The movable type is a small quartz block (4x4x2 px), matching the original block/square model.
    private static final Identifier TILE_TEXTURE =
        Identifier.fromNamespaceAndPath("minecraft", "textures/block/quartz_block_side.png");
    private static final RenderType TILE_RENDER_TYPE = RenderTypes.entitySolid(TILE_TEXTURE);

    public static class State extends BlockEntityRenderState {
        public final HexPattern[] patterns = new HexPattern[BoardBlockEntity.SLOTS];
        public final boolean[] filled = new boolean[BoardBlockEntity.SLOTS];
        public Direction facing = Direction.NORTH;
    }

    public BoardRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BoardBlockEntity board, State state, float partialTick, Vec3 cameraPos,
        ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(board, state, partialTick, cameraPos, crumbling);
        state.facing = board.getBlockState().getValue(BoardBlock.FACING);
        for (int i = 0; i < BoardBlockEntity.SLOTS; i++) {
            state.patterns[i] = null;
            ItemStack stack = board.getSquare(i);
            state.filled[i] = !stack.isEmpty();
            if (stack.isEmpty() || !(stack.getItem() instanceof IotaHolderItem holder)) {
                continue;
            }
            CompoundTag tag = holder.readIotaTag(stack);
            if (tag == null) {
                continue;
            }
            Tag data = tag.get(HexIotaTypes.KEY_DATA);
            if (!(data instanceof CompoundTag compound)) {
                continue;
            }
            try {
                state.patterns[i] = PatternIota.deserialize(compound).getPattern();
            } catch (IllegalArgumentException ignored) {
                // Not a pattern iota (number, entity, ...): the board keeps it, but only patterns are drawn.
            }
        }
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        Direction facing = state.facing;
        Vec3 origin = switch (facing) {
            case NORTH -> new Vec3(1, 1, 0.75);
            case SOUTH -> new Vec3(0, 1, 0.25);
            case WEST -> new Vec3(0.75, 1, 0);
            case EAST -> new Vec3(0.25, 1, 1);
            default -> null;
        };
        if (origin == null) {
            return;
        }
        Vec3 x = switch (facing) {
            case NORTH -> new Vec3(-CELL, 0, 0);
            case SOUTH -> new Vec3(CELL, 0, 0);
            case WEST -> new Vec3(0, 0, CELL);
            case EAST -> new Vec3(0, 0, -CELL);
            default -> Vec3.ZERO;
        };
        Vec3 y = new Vec3(0, -CELL, 0);
        Vec3 f = switch (facing) {
            case NORTH -> new Vec3(0, 0, -1);
            case SOUTH -> new Vec3(0, 0, 1);
            case WEST -> new Vec3(-1, 0, 0);
            case EAST -> new Vec3(1, 0, 0);
            default -> Vec3.ZERO;
        };
        Quaternionf q = switch (facing) {
            case NORTH -> Axis.YP.rotationDegrees(0);
            case SOUTH -> Axis.YP.rotationDegrees(180);
            case WEST -> Axis.YP.rotationDegrees(-90);
            case EAST -> Axis.YP.rotationDegrees(90);
            default -> new Quaternionf();
        };
        q.mul(new Quaternionf().rotateAxis((float) Math.PI, (float) f.x, (float) f.y, (float) f.z));

        // After the rotation the tile's local +z may point away from or towards the board face,
        // depending on the facing. Extend the slab towards the board (i.e. along -f) either way.
        Vector3f localZ = q.transform(new Vector3f(0f, 0f, 1f));
        float depth = localZ.dot((float) f.x, (float) f.y, (float) f.z) > 0f
            ? -TILE_THICKNESS
            : TILE_THICKNESS;

        for (int i = 0; i < BoardBlockEntity.SLOTS; i++) {
            int row = (int) (i / GRID_ROWS_AND_COLS);
            int col = (int) (i % GRID_ROWS_AND_COLS);
            HexPattern pattern = state.patterns[i];
            if (state.filled[i]) {
                ps.pushPose();
                ps.translate(origin.x, origin.y, origin.z);
                ps.translate(x.x * col, x.y * col, x.z * col);
                ps.translate(y.x * row, y.y * row, y.z * row);
                ps.translate(f.x * TILE_OFFSET, f.y * TILE_OFFSET, f.z * TILE_OFFSET);
                ps.mulPose(q);
                submitTile(ps, collector, state.lightCoords, depth);
                ps.popPose();
            }
            if (pattern == null) {
                continue;
            }
            ps.pushPose();
            ps.translate(origin.x, origin.y, origin.z);
            ps.translate(x.x * col, x.y * col, x.z * col);
            ps.translate(y.x * row, y.y * row, y.z * row);
            ps.translate(f.x * PATTERN_OFFSET, f.y * PATTERN_OFFSET, f.z * PATTERN_OFFSET);
            ps.scale(1f / GRID_ROWS_AND_COLS, 1f / GRID_ROWS_AND_COLS, 1f / GRID_ROWS_AND_COLS);
            ps.mulPose(q);
            PatternRenderer.renderPattern(pattern, ps,
                new PatternRenderer.WorldlyBits(new WorldMeshSink(collector, ps, state.lightCoords, f)),
                WorldlyPatternRenderHelpers.READABLE_SCROLL_SETTINGS,
                PatternColors.glowyStroke(0xff_fecbe6), 0, 256);
            ps.popPose();
        }
    }

    private static void submitTile(PoseStack ps, SubmitNodeCollector collector, int light, float depth) {
        float zA = Math.min(0f, depth);
        float zB = Math.max(0f, depth);
        collector.submitCustomGeometry(ps, TILE_RENDER_TYPE, (pose, vc) -> {
            // Quartz slab filling one cell: x/y in [0, CELL], extruded from zA to zB towards the board.
            quad(vc, pose, light, 0f, 0f, 1f,
                0f, 0f, zB, 1f, 0f, zB, 1f, CELL, zB, 0f, CELL, zB);
            quad(vc, pose, light, 0f, 0f, -1f,
                CELL, 0f, zA, 0f, 0f, zA, 0f, CELL, zA, CELL, CELL, zA);
            quad(vc, pose, light, -1f, 0f, 0f,
                0f, 0f, zA, 0f, 0f, zB, 0f, CELL, zB, 0f, CELL, zA);
            quad(vc, pose, light, 1f, 0f, 0f,
                CELL, 0f, zB, CELL, 0f, zA, CELL, CELL, zA, CELL, CELL, zB);
            quad(vc, pose, light, 0f, 1f, 0f,
                0f, CELL, zB, CELL, CELL, zB, CELL, CELL, zA, 0f, CELL, zA);
            quad(vc, pose, light, 0f, -1f, 0f,
                0f, 0f, zA, CELL, 0f, zA, CELL, 0f, zB, 0f, 0f, zB);
        });
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, int light, float nx, float ny, float nz,
        float x0, float y0, float z0, float x1, float y1, float z1,
        float x2, float y2, float z2, float x3, float y3, float z3) {
        vertex(vc, pose, nx, ny, nz, light, x0, y0, z0, 0f, 0f);
        vertex(vc, pose, nx, ny, nz, light, x1, y1, z1, 1f, 0f);
        vertex(vc, pose, nx, ny, nz, light, x2, y2, z2, 1f, 1f);
        vertex(vc, pose, nx, ny, nz, light, x3, y3, z3, 0f, 1f);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float nx, float ny, float nz, int light,
        float x, float y, float z, float u, float v) {
        vc.addVertex(pose, x, y, z)
            .setColor(0xFFFFFFFF)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, nx, ny, nz);
    }
}
