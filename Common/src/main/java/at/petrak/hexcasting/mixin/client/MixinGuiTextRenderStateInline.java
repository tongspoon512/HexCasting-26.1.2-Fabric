package at.petrak.hexcasting.mixin.client;

import at.petrak.hexcasting.interop.inline.InlineTextProcessor;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Chat and other widgets build {@link GuiTextRenderState} directly instead of going through
 * {@code GuiGraphicsExtractor.text}, so this is the one place every piece of GUI text passes through.
 */
@Mixin(GuiTextRenderState.class)
public abstract class MixinGuiTextRenderStateInline {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static FormattedCharSequence hex$processInlineText(FormattedCharSequence text) {
        return InlineTextProcessor.process(text);
    }
}
