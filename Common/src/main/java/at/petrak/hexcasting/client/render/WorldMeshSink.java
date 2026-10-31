package at.petrak.hexcasting.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
        // The render type's pipeline bakes in its own primitive topology (entity layers are QUADS), but the
        // pattern renderer emits TRIANGLES for stroked lines. Writing triangle vertices into a quad pipeline
        // makes every four vertices form one bogus quad, which is what shreds slates (and scrolls/bookshelves)
        // into stray triangles. Convert the vertex stream to the layer's own mode before submitting.
        final VertexFormat.Mode sourceMode = mesh.mode;
        final VertexFormat.Mode targetMode = layer.mode();
        this.collector.submitCustomGeometry(this.ps, layer, (pose, vc) -> {
            List<MeshVertex> vertices = mesh.vertices;
            if (sourceMode == targetMode) {
                for (MeshVertex v : vertices) {
                    emit(vc, pose, v, nx, ny, nz);
                }
            } else if (sourceMode == VertexFormat.Mode.TRIANGLES && targetMode == VertexFormat.Mode.QUADS) {
                // A quad with the last corner repeated rasterises as the original triangle: the fourth vertex
                // only produces a degenerate second triangle.
                int end = vertices.size() - vertices.size() % 3;
                for (int i = 0; i < end; i += 3) {
                    MeshVertex a = vertices.get(i);
                    MeshVertex b = vertices.get(i + 1);
                    MeshVertex c = vertices.get(i + 2);
                    emit(vc, pose, a, nx, ny, nz);
                    emit(vc, pose, b, nx, ny, nz);
                    emit(vc, pose, c, nx, ny, nz);
                    emit(vc, pose, c, nx, ny, nz);
                }
            } else if (sourceMode == VertexFormat.Mode.QUADS && targetMode == VertexFormat.Mode.TRIANGLES) {
                int end = vertices.size() - vertices.size() % 4;
                for (int i = 0; i < end; i += 4) {
                    MeshVertex a = vertices.get(i);
                    MeshVertex b = vertices.get(i + 1);
                    MeshVertex c = vertices.get(i + 2);
                    MeshVertex d = vertices.get(i + 3);
                    emit(vc, pose, a, nx, ny, nz);
                    emit(vc, pose, b, nx, ny, nz);
                    emit(vc, pose, c, nx, ny, nz);
                    emit(vc, pose, a, nx, ny, nz);
                    emit(vc, pose, c, nx, ny, nz);
                    emit(vc, pose, d, nx, ny, nz);
                }
            } else {
                for (MeshVertex v : vertices) {
                    emit(vc, pose, v, nx, ny, nz);
                }
            }
        });
    }

    private void emit(VertexConsumer vc, PoseStack.Pose pose, MeshVertex v, float nx, float ny, float nz) {
        // The mesh transform is the same PoseStack pose we hand to submitCustomGeometry, so applying
        // the pose from the callback is the whole transform -- the vertices are in local space.
        vc.addVertex(pose, v.x, v.y, v.z);
        vc.setColor(v.color);
        vc.setUv(v.u, v.v);
        vc.setOverlay(OverlayTexture.NO_OVERLAY);
        vc.setLight(this.light);
        vc.setNormal(nx, ny, nz);
    }
}
