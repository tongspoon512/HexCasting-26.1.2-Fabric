package at.petrak.hexcasting.client.render;

import at.petrak.hexcasting.mixin.accessor.client.AccessorGuiGraphicsExtractor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.joml.Matrix3x2f;

import javax.annotation.Nullable;

/**
 * Bridge for pushing [MeshCollector] geometry into the GUI.
 */
public final class HexGuiRender {
    private HexGuiRender() {
    }

    public static MeshSink guiSink(GuiGraphicsExtractor graphics, @Nullable ScreenRectangle scissorArea) {
        GuiRenderState state = ((AccessorGuiGraphicsExtractor) graphics).hex$getGuiRenderState();
        Matrix3x2f guiPose = new Matrix3x2f(graphics.pose());
        return mesh -> state.addGuiElement(
            new GuiMeshRenderState(mesh.transform, guiPose, mesh.mode, mesh.vertices, scissorArea));
    }

    public static MeshSink guiSink(GuiGraphicsExtractor graphics) {
        return guiSink(graphics, null);
    }

    public static void submitMesh(GuiGraphicsExtractor graphics, MeshCollector mesh,
        @Nullable ScreenRectangle scissorArea) {
        if (mesh.isEmpty()) {
            return;
        }
        mesh.submitTo(guiSink(graphics, scissorArea));
    }

    public static void submitMesh(GuiGraphicsExtractor graphics, MeshCollector mesh) {
        submitMesh(graphics, mesh, null);
    }
}
