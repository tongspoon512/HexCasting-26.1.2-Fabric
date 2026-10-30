package com.samsthenerd.inline.api.matching;

import net.minecraft.resources.Identifier;
import net.minecraft.util.Tuple;

import javax.annotation.Nullable;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * Stub of inline's RegexMatcher.
 */
public interface RegexMatcher {
    Pattern getRegex();

    Tuple<InlineMatch, Integer> getMatchAndGroup(MatchResult regexMatch, MatchContext ctx);

    @Nullable
    InlineMatch getMatch(MatchResult mr, MatchContext ctx);

    MatcherInfo getInfo();

    Identifier getId();
}
