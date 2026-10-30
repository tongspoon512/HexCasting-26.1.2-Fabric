package com.samsthenerd.inline.impl;

import com.samsthenerd.inline.api.client.InlineClientAPI;
import com.samsthenerd.inline.api.client.InlineRenderer;
import com.samsthenerd.inline.api.matching.RegexMatcher;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Minimal working implementation of Inline's client API: it just remembers the matchers/renderers that
 * mods register. 26.1 has no Inline build, so Hex Casting ships this small replacement.
 */
public final class InlineClientAPIImpl implements InlineClientAPI {
    private static final List<RegexMatcher> MATCHERS = new CopyOnWriteArrayList<>();
    private static final List<InlineRenderer<?>> RENDERERS = new CopyOnWriteArrayList<>();

    @Override
    public void addMatcher(RegexMatcher matcher) {
        MATCHERS.add(matcher);
    }

    @Override
    public void addRenderer(InlineRenderer<?> renderer) {
        RENDERERS.add(renderer);
    }

    public static List<RegexMatcher> matchers() {
        return MATCHERS;
    }

    public static List<InlineRenderer<?>> renderers() {
        return RENDERERS;
    }
}
