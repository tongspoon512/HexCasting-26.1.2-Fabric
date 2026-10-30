package at.petrak.hexcasting.interop.emi;

import dev.emi.emi.api.render.EmiRender;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

public class EntitySlotWidget extends SlotWidget {

    private final float renderScale;

    public EntitySlotWidget(EmiIngredient stack, int x, int y, float renderScale) {
        super(stack, x, y);
        this.renderScale = renderScale;
    }

    private boolean useOffset = true;
    private float xShift = 0;
    private float yShift = 0;

    public EntitySlotWidget useOffset(boolean offset) {
        useOffset = offset;
        return this;
    }

    public EntitySlotWidget customShift(float xShift, float yShift) {
        this.xShift = xShift;
        this.yShift = yShift;
        return this;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int x, int y, float delta) {
        var poseStack = graphics.pose();
        Bounds bounds = this.getBounds();
        int width = bounds.width();
        int height = bounds.height();
        if (this.drawBack) {
            if (this.textureId != null) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureId, bounds.x(), bounds.y(),
                    (float) this.u, (float) this.v, width, height, 256, 256);
            } else {
                if (this.output) {
                    EmiTexture.LARGE_SLOT.render(graphics, bounds.x(), bounds.y(), delta);
                } else {
                    EmiTexture.SLOT.render(graphics, bounds.x(), bounds.y(), delta);
                }
            }
        }

        int xOff = useOffset ? (width - 16) / 2 : 0;
        int yOff = useOffset ? (height - 16) / 2 : 0;
        poseStack.pushMatrix();
        poseStack.translate(bounds.x() + xOff + xShift, bounds.y() + yOff + yShift);
        poseStack.scale(renderScale, renderScale);
        this.getStack().render(graphics, 0, 0, delta, EmiIngredient.RENDER_ICON);
        if (this.catalyst) {
            EmiRender.renderCatalystIcon(this.getStack(), graphics, 0, 0);
        }
        poseStack.popMatrix();
    }
}
