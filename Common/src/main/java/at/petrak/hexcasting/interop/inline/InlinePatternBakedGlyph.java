package at.petrak.hexcasting.interop.inline;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import com.mojang.blaze3d.font.GlyphInfo;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.network.chat.Style;

/**
 * A glyph whose advance is the pattern's width and whose renderable draws the pattern itself.
 */
public final class InlinePatternBakedGlyph implements BakedGlyph {
    private final HexPattern pattern;
    private final GlyphInfo info;

    public InlinePatternBakedGlyph(HexPattern pattern) {
        this.pattern = pattern;
        int advance = InlinePatternRenderer.INSTANCE.charWidth(
            new InlinePatternData(pattern), Style.EMPTY, InlinePatternFont.PLACEHOLDER);
        this.info = GlyphInfo.simple(Math.max(1, advance));
    }

    @Override
    public GlyphInfo info() {
        return this.info;
    }

    @Override
    public TextRenderable.Styled createGlyph(float x, float y, int color, int shadowColor, Style style,
        float boldOffset, float shadowOffset) {
        return new InlinePatternTextRenderable(this.pattern, x, y, color, this.info.getAdvance(), style);
    }
}
