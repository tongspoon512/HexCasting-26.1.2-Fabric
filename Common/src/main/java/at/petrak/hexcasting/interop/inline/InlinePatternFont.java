package at.petrak.hexcasting.interop.inline;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Carries {@link HexPattern}s inside a vanilla {@link FontDescription}.
 *
 * <p>Origin uses Inline's style-component mechanism, which 26.1 no longer has ({@code Style} is a fixed-field
 * value class). A font description, however, is a normal part of {@code Style} and is serialised through
 * vanilla's codec, so encoding the pattern into a {@code FontDescription.Resource} id keeps components
 * serialisable (they can cross the network) while still letting the client pick a custom glyph source.</p>
 */
public final class InlinePatternFont {
    /** Private-use codepoint standing in for a whole pattern inside a text run. */
    public static final char PLACEHOLDER = '\uE000';

    private static final String PREFIX = "inline_pattern/";

    private static final Map<String, HexDir> DIRS_BY_NAME = new HashMap<>();
    private static final ConcurrentMap<Identifier, GlyphSource> SOURCES = new ConcurrentHashMap<>();

    static {
        for (HexDir dir : HexDir.values()) {
            DIRS_BY_NAME.put(dirKey(dir), dir);
        }
    }

    private InlinePatternFont() {
    }

    private static String dirKey(HexDir dir) {
        return dir.name().toLowerCase(Locale.ROOT).replace("_", "");
    }

    public static FontDescription descriptionFor(HexPattern pattern) {
        return new FontDescription.Resource(idFor(pattern));
    }

    public static Identifier idFor(HexPattern pattern) {
        String sig = pattern.anglesSignature();
        // an empty signature would leave a trailing empty path segment
        String path = PREFIX + dirKey(pattern.getStartDir()) + "/" + (sig.isEmpty() ? "_" : sig);
        return Identifier.fromNamespaceAndPath(HexAPI.MOD_ID, path);
    }

    @Nullable
    public static HexPattern patternFor(Identifier id) {
        if (!HexAPI.MOD_ID.equals(id.getNamespace()) || !id.getPath().startsWith(PREFIX)) {
            return null;
        }
        String rest = id.getPath().substring(PREFIX.length());
        int slash = rest.indexOf('/');
        if (slash < 0) {
            return null;
        }
        HexDir dir = DIRS_BY_NAME.get(rest.substring(0, slash));
        String sig = rest.substring(slash + 1);
        if ("_".equals(sig)) {
            sig = "";
        }
        if (dir == null) {
            return null;
        }
        try {
            return HexPattern.fromAnglesUnchecked(sig, dir);
        } catch (Exception e) {
            return null;
        }
    }

    /** Returns the glyph source for one of our font descriptions, or {@code null} for anything else. */
    @Nullable
    public static GlyphSource sourceFor(Identifier id) {
        HexPattern pattern = patternFor(id);
        if (pattern == null) {
            return null;
        }
        return SOURCES.computeIfAbsent(id, key -> new InlinePatternGlyphSource(pattern));
    }
}
