package at.petrak.hexcasting.interop.inline;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.client.render.Mesh;
import at.petrak.hexcasting.client.render.MeshVertex;
import at.petrak.hexcasting.client.render.PatternColors;
import at.petrak.hexcasting.client.render.PatternRenderer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Style;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws a {@link HexPattern} in the middle of a text line.
 *
 * <p>It reuses the same stroke geometry as the pattern previews ({@link PatternRenderer}), but the GUI's
 * glyph renderer hands us a raw {@link VertexConsumer} for a fixed {@code QUADS} index buffer, so triangles
 * are padded to four vertices and their winding is normalised exactly like
 * {@code GuiMeshRenderState} does.</p>
 */
public final class InlinePatternTextRenderable implements TextRenderable.Styled {
    private static final float PATTERN_HEIGHT = 9f;

    private final HexPattern pattern;
    private final float x;
    private final float y;
    private final int color;
    private final float width;
    private final Style style;

    private List<Mesh> meshes;

    public InlinePatternTextRenderable(HexPattern pattern, float x, float y, int color, float width,
        Style style) {
        this.pattern = pattern;
        this.x = x;
        this.y = y;
        this.color = color;
        this.width = width;
        this.style = style;
    }

    @Override
    public Style style() {
        return this.style;
    }

    private List<Mesh> meshes() {
        if (this.meshes == null) {
            List<Mesh> captured = new ArrayList<>(2);
            PoseStack pose = new PoseStack();
            pose.translate(this.x, this.y - 0.5f, 0f);
            PatternRenderer.renderPattern(this.pattern, pose, new PatternRenderer.WorldlyBits(captured::add),
                InlinePatternRenderer.INLINE_SETTINGS, PatternColors.singleStroke(this.color), 0,
                InlinePatternRenderer.INLINE_TEXTURE_RES);
            this.meshes = captured;
        }
        return this.meshes;
    }

    @Override
    public void render(Matrix4fc matrix, VertexConsumer vc, int light, boolean shadow) {
        for (Mesh mesh : this.meshes()) {
            Matrix4f transform = new Matrix4f(matrix).mul(mesh.transform);
            List<MeshVertex> vertices = mesh.vertices;
            int n = vertices.size();
            switch (mesh.mode) {
                case TRIANGLES -> {
                    for (int i = 0; i + 2 < n; i += 3) {
                        triangle(vc, transform, vertices.get(i), vertices.get(i + 1), vertices.get(i + 2));
                    }
                }
                case TRIANGLE_FAN -> {
                    if (n >= 3) {
                        MeshVertex centre = vertices.get(0);
                        for (int i = 1; i + 1 < n; i++) {
                            triangle(vc, transform, centre, vertices.get(i), vertices.get(i + 1));
                        }
                    }
                }
                default -> {
                    for (MeshVertex v : vertices) {
                        emit(vc, transform, v);
                    }
                }
            }
        }
    }

    private static void triangle(VertexConsumer vc, Matrix4fc transform, MeshVertex a, MeshVertex b, MeshVertex c) {
        float doubleArea = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
        if (doubleArea > 0f) {
            emit(vc, transform, a);
            emit(vc, transform, c);
            emit(vc, transform, b);
            emit(vc, transform, b);
        } else {
            emit(vc, transform, a);
            emit(vc, transform, b);
            emit(vc, transform, c);
            emit(vc, transform, c);
        }
    }

    private static void emit(VertexConsumer vc, Matrix4fc transform, MeshVertex v) {
        vc.addVertex(transform, v.x, v.y, v.z);
        vc.setColor(v.color);
    }

    @Override
    public RenderPipeline guiPipeline() {
        return RenderPipelines.GUI;
    }

    @Override
    @Nullable
    public GpuTextureView textureView() {
        return null;
    }

    @Override
    public RenderType renderType(Font.DisplayMode displayMode) {
        // Only ever inserted into GUI text, which goes through GlyphRenderState rather than a RenderType.
        return null;
    }

    @Override
    public float left() {
        return this.x;
    }

    @Override
    public float top() {
        return this.y - PATTERN_HEIGHT;
    }

    @Override
    public float right() {
        return this.x + this.width;
    }

    @Override
    public float bottom() {
        return this.y + 1f;
    }
}
