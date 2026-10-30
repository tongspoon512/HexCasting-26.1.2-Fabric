package com.samsthenerd.inline.api.client;

import com.samsthenerd.inline.api.matching.RegexMatcher;
import com.samsthenerd.inline.impl.InlineClientAPIImpl;

/**
 * Stub of inline's InlineClientAPI, backed by a working registry so client-side text matching actually runs.
 */
public interface InlineClientAPI {
    InlineClientAPI INSTANCE = new InlineClientAPIImpl();

    void addMatcher(RegexMatcher matcher);

    void addRenderer(InlineRenderer<?> renderer);
}
