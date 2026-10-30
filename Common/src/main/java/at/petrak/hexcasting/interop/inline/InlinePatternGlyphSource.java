package at.petrak.hexcasting.interop.inline;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.util.RandomSource;

/**
 * Glyph source for a single pattern. Every codepoint in that "font" resolves to the same glyph, so a single
 * placeholder character is enough to represent the whole pattern.
 */
public final class InlinePatternGlyphSource implements GlyphSource {
    private final InlinePatternBakedGlyph glyph;

    public InlinePatternGlyphSource(HexPattern pattern) {
        this.glyph = new InlinePatternBakedGlyph(pattern);
    }

    @Override
    public BakedGlyph getGlyph(int codepoint) {
        return this.glyph;
    }

    @Override
    public BakedGlyph getRandomGlyph(RandomSource random, int advance) {
        return this.glyph;
    }
}
