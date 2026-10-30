package at.petrak.hexcasting.mixin.accessor.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 26.1's GUI builds a recorded GuiRenderState instead of drawing immediately, and GuiGraphicsExtractor
 * keeps that state private with no getter. We need it to append arbitrary geometry (spell patterns),
 * so expose it here.
 */
@Mixin(GuiGraphicsExtractor.class)
public interface AccessorGuiGraphicsExtractor {
    @Accessor("guiRenderState")
    GuiRenderState hex$getGuiRenderState();
}
