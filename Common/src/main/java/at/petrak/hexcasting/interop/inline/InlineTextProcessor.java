package at.petrak.hexcasting.interop.inline;

import com.samsthenerd.inline.api.matching.InlineMatch;
import com.samsthenerd.inline.api.matching.RegexMatcher;
import com.samsthenerd.inline.impl.InlineClientAPIImpl;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Tuple;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Rewrites GUI text runs so that inline syntax such as {@code {west aqw}} is drawn as a hex pattern.
 *
 * <p>The original glyphs of a match are collapsed into one {@link InlinePatternFont#PLACEHOLDER} character
 * whose style carries a font description identifying the pattern. The pattern then renders through the same
 * glyph pipeline as normal text, and its advance (used for measuring) is the pattern's actual width.</p>
 */
public final class InlineTextProcessor {
    private InlineTextProcessor() {
    }

    public static FormattedCharSequence process(FormattedCharSequence text) {
        if (text == null) {
            return null;
        }
        List<RegexMatcher> matchers = InlineClientAPIImpl.matchers();
        if (matchers.isEmpty()) {
            return text;
        }

        List<Integer> codepoints = new ArrayList<>();
        List<Style> styles = new ArrayList<>();
        List<Integer> offsets = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        text.accept((index, style, codepoint) -> {
            offsets.add(plain.length());
            codepoints.add(codepoint);
            styles.add(style);
            plain.appendCodePoint(codepoint);
            return true;
        });

        String raw = plain.toString();
        if (!mightContainInlineSyntax(raw)) {
            return text;
        }

        List<Replacement> replacements = new ArrayList<>();
        for (RegexMatcher matcher : matchers) {
            Matcher regex = matcher.getRegex().matcher(raw);
            while (regex.find()) {
                if (overlaps(replacements, regex.start(), regex.end())) {
                    continue;
                }
                Tuple<InlineMatch, Integer> result;
                try {
                    result = matcher.getMatchAndGroup(regex.toMatchResult(), null);
                } catch (Exception e) {
                    continue;
                }
                if (result == null || !(result.getA() instanceof InlineMatch.DataMatch<?> dataMatch)) {
                    continue;
                }
                if (!(dataMatch.getData() instanceof InlinePatternData patternData)) {
                    continue;
                }
                if (patternData.pattern == null) {
                    continue;
                }
                Style base = styles.get(partIndexAt(offsets, regex.start()));
                Style merged = applyInlineStyle(patternData, dataMatch.getStyle(), base);
                replacements.add(new Replacement(regex.start(), regex.end(), merged));
            }
        }

        if (replacements.isEmpty()) {
            return text;
        }
        replacements.sort(Comparator.comparingInt(Replacement::start));

        List<Integer> outCodepoints = new ArrayList<>(codepoints.size() - replacements.size());
        List<Style> outStyles = new ArrayList<>(styles.size() - replacements.size());
        int part = 0;
        for (Replacement replacement : replacements) {
            while (part < codepoints.size() && offsets.get(part) < replacement.start()) {
                outCodepoints.add(codepoints.get(part));
                outStyles.add(styles.get(part));
                part++;
            }
            while (part < codepoints.size() && offsets.get(part) < replacement.end()) {
                part++;
            }
            outCodepoints.add((int) InlinePatternFont.PLACEHOLDER);
            outStyles.add(replacement.style());
        }
        while (part < codepoints.size()) {
            outCodepoints.add(codepoints.get(part));
            outStyles.add(styles.get(part));
            part++;
        }

        return sink -> {
            int index = 0;
            for (int i = 0; i < outCodepoints.size(); i++) {
                if (!sink.accept(index++, outStyles.get(i), outCodepoints.get(i))) {
                    return false;
                }
            }
            return true;
        };
    }

    private static Style applyInlineStyle(InlinePatternData data, Style extra, Style base) {
        Style style = extra == null ? Style.EMPTY : extra;
        style = style.applyTo(base);
        return style.withFont(InlinePatternFont.descriptionFor(data.pattern));
    }

    private static int partIndexAt(List<Integer> offsets, int stringIndex) {
        int low = 0;
        int high = offsets.size() - 1;
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (offsets.get(mid) <= stringIndex) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    private static boolean overlaps(List<Replacement> replacements, int start, int end) {
        for (Replacement replacement : replacements) {
            if (start < replacement.end() && end > replacement.start()) {
                return true;
            }
        }
        return false;
    }

    private static boolean mightContainInlineSyntax(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{' || c == '[' || c == '<' || c == '(') {
                return true;
            }
        }
        return false;
    }

    private record Replacement(int start, int end, Style style) {
    }
}
