package dev.emi.emi.api.render;

import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Stub of emi's EmiRender.
 */
public class EmiRender {
    public static void renderItem(GuiGraphicsExtractor draw, EmiIngredient stack, int x, int y) {
        stack.render(draw, x, y, 0f, EmiIngredient.RENDER_ICON);
    }

    public static void renderIngredientIcon(EmiIngredient stack, GuiGraphicsExtractor draw, int x, int y) {
        stack.render(draw, x, y, 0f, EmiIngredient.RENDER_ICON);
    }

    public static void renderCatalystIcon(EmiIngredient stack, GuiGraphicsExtractor draw, int x, int y) {
        stack.render(draw, x, y, 0f, EmiIngredient.RENDER_INGREDIENT);
    }

    public static void drawTexture(GuiGraphicsExtractor draw, net.minecraft.resources.Identifier id,
                                   int x, int y, int u, int v, int width, int height,
                                   int textureWidth, int textureHeight) {
        draw.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, id, x, y,
            (float) u, (float) v, width, height, textureWidth, textureHeight);
    }
}
