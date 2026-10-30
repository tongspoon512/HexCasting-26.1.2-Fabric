package dev.emi.emi.api.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Stub of emi's EmiTexture.
 */
public class EmiTexture implements EmiRenderable {
    public static final EmiTexture SLOT = new EmiTexture(
        Identifier.fromNamespaceAndPath("emi", "textures/gui/slot.png"), 0, 0, 18, 18, 18, 18, 0, 0);
    public static final EmiTexture LARGE_SLOT = new EmiTexture(
        Identifier.fromNamespaceAndPath("emi", "textures/gui/large_slot.png"), 0, 0, 26, 26, 26, 26, 0, 0);

    private final Identifier texture;
    private final int u, v, regionWidth, regionHeight, textureWidth, textureHeight, u2, v2;

    public EmiTexture(Identifier texture, int u, int v, int regionWidth, int regionHeight,
                      int textureWidth, int textureHeight, int u2, int v2) {
        this.texture = texture;
        this.u = u;
        this.v = v;
        this.regionWidth = regionWidth;
        this.regionHeight = regionHeight;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.u2 = u2;
        this.v2 = v2;
    }

    @Override
    public void render(GuiGraphicsExtractor draw, int x, int y, float delta) {
        EmiRender.drawTexture(draw, this.texture, x, y, this.u, this.v,
            this.regionWidth, this.regionHeight, this.textureWidth, this.textureHeight);
    }
}
