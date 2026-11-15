package pub.pigeon.yggdyy.hexcreating.client.render;

import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.PatternIota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pub.pigeon.yggdyy.hexcreating.api.iotawire.IotaWireConnector;
import pub.pigeon.yggdyy.hexcreating.api.iotawire.IotaWireProcessor;
import pub.pigeon.yggdyy.hexcreating.blocks.IotaWireConnectorBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the invisible connector block: one small copper nub per active socket plus the purple
 * wires that leave it. Geometry is derived from the same boxes the block uses for selection, so
 * clicks, collisions and visuals always line up.
 */
public class IotaWireConnectorRenderer
    implements BlockEntityRenderer<IotaWireConnectorBlock.ConnectorBlockEntity, IotaWireConnectorRenderer.State> {

    private static final Identifier NUB_TEXTURE =
        Identifier.fromNamespaceAndPath("minecraft", "textures/block/copper_block.png");
    private static final RenderType NUB_TYPE = RenderTypes.entitySolid(NUB_TEXTURE);
    private static final Identifier WIRE_TEXTURE =
        Identifier.fromNamespaceAndPath("minecraft", "textures/block/white_concrete.png");
    private static final RenderType WIRE_TYPE = RenderTypes.entitySolid(WIRE_TEXTURE);
    private static final int WIRE_COLOR = 0xFFC890F0;
    private static final double WIRE_WIDTH = 0.012;

    public static class State extends BlockEntityRenderState {
        public final boolean[] active = new boolean[6];
        /** Each entry is {start, end} in block-local space. */
        public final List<Vec3[]> wires = new ArrayList<>();
    }

    public IotaWireConnectorRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(IotaWireConnectorBlock.ConnectorBlockEntity connector, State state,
        float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(connector, state, partialTick, cameraPos, crumbling);
        BlockState bs = connector.getBlockState();
        state.active[0] = bs.getValue(IotaWireConnectorBlock.UP);
        state.active[1] = bs.getValue(IotaWireConnectorBlock.DOWN);
        state.active[2] = bs.getValue(IotaWireConnectorBlock.NORTH);
        state.active[3] = bs.getValue(IotaWireConnectorBlock.SOUTH);
        state.active[4] = bs.getValue(IotaWireConnectorBlock.WEST);
        state.active[5] = bs.getValue(IotaWireConnectorBlock.EAST);
        state.wires.clear();

        if (connector.getLevel() == null) {
            return;
        }
        for (IotaWireConnector socket : connector.getIotaWireConnectors()) {
            Vec3 from = socket.clickBox.getCenter();
            for (IotaWireConnector.Link link : socket.others) {
                IotaWireConnector other = IotaWireProcessor.tryGetConnector(connector.getLevel(),
                    link.pos(), link.terminalId());
                if (other == null) {
                    continue;
                }
                Vec3 target = Vec3.atLowerCornerOf(link.pos()).add(other.clickBox.getCenter());
                state.wires.add(new Vec3[]{from,
                    target.subtract(Vec3.atLowerCornerOf(connector.getBlockPos()))});
            }
        }
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int id = 0; id < 6; id++) {
            if (state.active[id]) {
                AABB box = boxFor(id);
                submitBox(ps, collector, state.lightCoords, box);
            }
        }
        for (Vec3[] wire : state.wires) {
            submitWire(ps, collector, state.lightCoords, wire[0], wire[1]);
        }
    }

    private static AABB boxFor(int id) {
        return switch (id) {
            case 0 -> IotaWireConnectorBlock.UP_BOX;
            case 1 -> IotaWireConnectorBlock.DOWN_BOX;
            case 2 -> IotaWireConnectorBlock.NORTH_BOX;
            case 3 -> IotaWireConnectorBlock.SOUTH_BOX;
            case 4 -> IotaWireConnectorBlock.WEST_BOX;
            default -> IotaWireConnectorBlock.EAST_BOX;
        };
    }

    private static void submitBox(PoseStack ps, SubmitNodeCollector collector, int light, AABB box) {
        float x0 = (float) box.minX, y0 = (float) box.minY, z0 = (float) box.minZ;
        float x1 = (float) box.maxX, y1 = (float) box.maxY, z1 = (float) box.maxZ;
        collector.submitCustomGeometry(ps, NUB_TYPE, (pose, vc) -> {
            quad(vc, pose, light, 0, 0, -1, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0);
            quad(vc, pose, light, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
            quad(vc, pose, light, -1, 0, 0, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0);
            quad(vc, pose, light, 1, 0, 0, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1);
            quad(vc, pose, light, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
            quad(vc, pose, light, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        });
    }

    private static void submitWire(PoseStack ps, SubmitNodeCollector collector, int light, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.0e-4) {
            return;
        }
        Vec3 dir = delta.scale(1.0 / length);
        Vec3 up = Math.abs(dir.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 sideA = dir.cross(up).normalize().scale(WIRE_WIDTH);
        Vec3 sideB = dir.cross(sideA).normalize().scale(WIRE_WIDTH);
        collector.submitCustomGeometry(ps, WIRE_TYPE, (pose, vc) -> {
            ribbon(vc, pose, light, from, to, sideA);
            ribbon(vc, pose, light, from, to, sideB);
        });
    }

    private static void ribbon(VertexConsumer vc, PoseStack.Pose pose, int light, Vec3 from, Vec3 to, Vec3 side) {
        Vec3 a0 = from.subtract(side);
        Vec3 a1 = from.add(side);
        Vec3 b0 = to.subtract(side);
        Vec3 b1 = to.add(side);
        float nx = (float) side.x;
        float ny = (float) side.y;
        float nz = (float) side.z;
        // Both windings so the ribbon stays visible from either side.
        quad(vc, pose, light, nx, ny, nz, a0, a1, b1, b0, WIRE_COLOR);
        quad(vc, pose, light, -nx, -ny, -nz, b0, b1, a1, a0, WIRE_COLOR);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, int light,
        float nx, float ny, float nz, float x0, float y0, float z0, float x1, float y1, float z1,
        float x2, float y2, float z2, float x3, float y3, float z3) {
        quad(vc, pose, light, nx, ny, nz,
            new Vec3(x0, y0, z0), new Vec3(x1, y1, z1), new Vec3(x2, y2, z2), new Vec3(x3, y3, z3),
            0xFFFFFFFF);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, int light, float nx, float ny, float nz,
        Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int color) {
        vertex(vc, pose, nx, ny, nz, light, p0, 0f, 0f, color);
        vertex(vc, pose, nx, ny, nz, light, p1, 1f, 0f, color);
        vertex(vc, pose, nx, ny, nz, light, p2, 1f, 1f, color);
        vertex(vc, pose, nx, ny, nz, light, p3, 0f, 1f, color);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float nx, float ny, float nz, int light,
        Vec3 p, float u, float v, int color) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z)
            .setColor(color)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, nx, ny, nz);
    }
}
