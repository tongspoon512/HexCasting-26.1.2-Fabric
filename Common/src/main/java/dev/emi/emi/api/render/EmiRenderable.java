package dev.emi.emi.api.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Stub of emi's EmiRenderable.
 */
public interface EmiRenderable {
    default void render(GuiGraphicsExtractor draw, int x, int y, float delta) {
    }
}
