package at.petrak.hexcasting.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Pushes collected geometry into the world renderer's submit queue.
 *
 * 26.1 has no immediate-mode drawing for custom shapes, so instead of handing back a VertexConsumer we
 * hand the finished batches to a SubmitNodeCollector.
 */
public final class WorldMeshSink implements MeshSink {
    private final SubmitNodeCollector collector;
    private final PoseStack ps;
    private final int light;
    private final Vec3 normal;

    public WorldMeshSink(SubmitNodeCollector collector, PoseStack ps, int light, @Nullable Vec3 normal) {
        this.collector = collector;
        this.ps = ps;
        this.light = light;
        this.normal = normal;
    }

    @Override
    public void submit(Mesh mesh) {
        RenderType layer = RenderTypes.entityTranslucent(mesh.texture);
        final float nx = this.normal != null ? (float) this.normal.x : 0f;
        final float ny = this.normal != null ? (float) this.normal.y : 0f;
        final float nz = this.normal != null ? (float) this.normal.z : 0f;
        this.collector.submitCustomGeometry(this.ps, layer, (pose, vc) -> {
            for (MeshVertex v : mesh.vertices) {
                // The mesh transform is the same PoseStack pose we hand to submitCustomGeometry, so applying
                // the pose from the callback is the whole transform -- the vertices are in local space.
                vc.addVertex(pose, v.x, v.y, v.z);
                vc.setColor(v.color);
                vc.setUv(v.u, v.v);
                vc.setOverlay(OverlayTexture.NO_OVERLAY);
                vc.setLight(this.light);
                vc.setNormal(nx, ny, nz);
            }
        });
    }
}
