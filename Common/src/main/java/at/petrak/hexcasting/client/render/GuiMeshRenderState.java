package at.petrak.hexcasting.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4fc;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A GUI element built out of arbitrary coloured triangles.
 *
 * 26.1's GUI is a recorded render-state list rather than an immediate-mode canvas, so the only way to get
 * non-rectangular shapes on screen is to append one of these to the GuiRenderState.
 *
 * <p>Two quirks of the GUI renderer shape this class:</p>
 * <ul>
 *   <li>It always assembles meshes with the fixed sequential index buffer of {@code QUADS} -- every group of
 *   four vertices is drawn as (0,1,2) plus (0,2,3) -- so triangles have to be padded to four vertices.</li>
 *   <li>The {@code GUI} pipeline does not disable backface culling, and the GUI's Y-inverted projection makes
 *   clockwise (in screen space) the front facing side. Our stroke geometry is authored for a non-culling
 *   pipeline, so each triangle's winding is normalised on the way in.</li>
 * </ul>
 */
public final class GuiMeshRenderState implements GuiElementRenderState {
    private final TextureSetup textureSetup;
    private final Matrix3x2f pose;
    private final VertexFormat.Mode mode;
    private final List<MeshVertex> vertices;
    private final ScreenRectangle scissorArea;
    private final ScreenRectangle bounds;

    public GuiMeshRenderState(Matrix4fc transform, Matrix3x2fc guiPose, VertexFormat.Mode mode,
        List<MeshVertex> vertices, @Nullable ScreenRectangle scissorArea) {
        this.mode = mode;
        // Flatten the 4x4 down to the 2D affine transform the GUI can actually express. Every transform we
        // build for GUI patterns is translate/scale only, so the upper-left 2x2 plus the translation is enough.
        this.pose = new Matrix3x2f(guiPose).mul(new Matrix3x2f(
            transform.m00(), transform.m01(),
            transform.m10(), transform.m11(),
            transform.m30(), transform.m31()));
        this.vertices = vertices;
        this.scissorArea = scissorArea;
        this.bounds = this.computeBounds();
        this.textureSetup = TextureSetup.noTexture();
    }

    private ScreenRectangle computeBounds() {
        if (this.vertices.isEmpty()) {
            return ScreenRectangle.empty();
        }
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (MeshVertex v : this.vertices) {
            float x = this.pose.m00() * v.x + this.pose.m10() * v.y + this.pose.m20();
            float y = this.pose.m01() * v.x + this.pose.m11() * v.y + this.pose.m21();
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        return new ScreenRectangle(
            (int) Math.floor(minX), (int) Math.floor(minY),
            Math.max(1, (int) Math.ceil(maxX - minX)), Math.max(1, (int) Math.ceil(maxY - minY)));
    }

    @Override
    public void buildVertices(VertexConsumer vc) {
        int n = this.vertices.size();
        switch (this.mode) {
            case TRIANGLES -> {
                for (int i = 0; i + 2 < n; i += 3) {
                    this.triangle(vc, this.vertices.get(i), this.vertices.get(i + 1), this.vertices.get(i + 2));
                }
            }
            case TRIANGLE_FAN -> {
                if (n >= 3) {
                    MeshVertex centre = this.vertices.get(0);
                    for (int i = 1; i + 1 < n; i++) {
                        this.triangle(vc, centre, this.vertices.get(i), this.vertices.get(i + 1));
                    }
                }
            }
            default -> {
                for (MeshVertex v : this.vertices) {
                    this.emit(vc, v);
                }
            }
        }
    }

    /**
     * Emit one triangle as a four-vertex group (the trailing vertex duplicates the last one, so the second
     * triangle of the quad is degenerate), with the winding flipped if needed so backface culling keeps it.
     */
    private void triangle(VertexConsumer vc, MeshVertex a, MeshVertex b, MeshVertex c) {
        float doubleArea = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
        if (doubleArea > 0f) {
            this.emit(vc, a);
            this.emit(vc, c);
            this.emit(vc, b);
            this.emit(vc, b);
        } else {
            this.emit(vc, a);
            this.emit(vc, b);
            this.emit(vc, c);
            this.emit(vc, c);
        }
    }

    private void emit(VertexConsumer vc, MeshVertex v) {
        vc.addVertexWith2DPose(this.pose, v.x, v.y);
        vc.setColor(v.color);
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI;
    }

    @Override
    public TextureSetup textureSetup() {
        return this.textureSetup;
    }

    @Nullable
    @Override
    public ScreenRectangle scissorArea() {
        return this.scissorArea;
    }

    @Override
    public ScreenRectangle bounds() {
        return this.bounds;
    }
}
