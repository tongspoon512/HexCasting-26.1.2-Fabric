package com.samsthenerd.inline.api;

import net.minecraft.network.chat.Style;

/**
 * Stub of inline's InlineAPI for the 26.1.2 port.
 */
public interface InlineAPI {
    InlineAPI INSTANCE = new InlineAPI() {
        @Override
        public void addDataType(InlineData.InlineDataType<?> type) {
        }

        @Override
        public Style withSizeModifier(Style style, float size) {
            return style;
        }
    };

    void addDataType(InlineData.InlineDataType<?> type);

    Style withSizeModifier(Style style, float size);
}
