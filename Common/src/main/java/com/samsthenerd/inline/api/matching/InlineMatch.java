package com.samsthenerd.inline.api.matching;

import com.samsthenerd.inline.api.InlineData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stub of inline's InlineMatch, carrying the data/style a matcher produced for a span of text.
 */
public abstract class InlineMatch {
    public static class TextMatch extends InlineMatch {
        private final Component component;

        public TextMatch(Component component) {
            this.component = component;
        }

        public Component getComponent() {
            return this.component;
        }
    }

    public static class DataMatch<T extends InlineData<T>> extends InlineMatch {
        private final T data;
        @Nullable
        private final Style style;

        public DataMatch(@NotNull T data, @Nullable Style style) {
            this.data = data;
            this.style = style;
        }

        public T getData() {
            return this.data;
        }

        @Nullable
        public Style getStyle() {
            return this.style;
        }
    }
}
