package at.petrak.hexcasting.mixin.client;

import at.petrak.hexcasting.interop.inline.InlinePatternFont;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.network.chat.FontDescription;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Serves glyphs for the inline-pattern font descriptions.
 *
 * <p>Both text rendering ({@code Font.getGlyph}) and measuring ({@code StringSplitter}'s width provider)
 * resolve glyphs through this method, so intercepting it makes an inline pattern draw *and* measure at its
 * real width.</p>
 */
@Mixin(Font.class)
public abstract class MixinFontInline {
    @Inject(method = "getGlyphSource", at = @At("HEAD"), cancellable = true)
    private void hex$inlinePatternGlyphs(FontDescription description, CallbackInfoReturnable<GlyphSource> cir) {
        if (description instanceof FontDescription.Resource resource) {
            GlyphSource source = InlinePatternFont.sourceFor(resource.id());
            if (source != null) {
                cir.setReturnValue(source);
            }
        }
    }
}
