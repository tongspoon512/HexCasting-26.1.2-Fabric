package at.petrak.hexcasting.interop.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Renders an entity into the GUI through 26.1's render-state pipeline.
 */
public final class EntityDrawingUtil {
    private EntityDrawingUtil() {
    }

    public static void drawEntity(GuiGraphicsExtractor graphics, Entity entity, float x, float y, float scale) {
        if (entity == null) {
            return;
        }

        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        var state = dispatcher.extractEntity(entity, 1f);

        // Same framing as vanilla's inventory player preview: rotate to face the camera, then centre the
        // render state vertically in a box around the requested point.
        var translation = new Vector3f(0f, state.boundingBoxHeight / 2f, 0f);
        var rotation = new Quaternionf().rotateZ((float) Math.PI);

        graphics.entity(state, scale, translation, rotation, new Quaternionf(),
            (int) (x - 32f), (int) (y - 64f), (int) (x + 32f), (int) (y + 16f));
    }
}
