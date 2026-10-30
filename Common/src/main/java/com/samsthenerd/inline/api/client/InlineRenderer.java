package com.samsthenerd.inline.api.client;

import com.samsthenerd.inline.api.InlineData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * Stub of inline's InlineRenderer.
 */
public interface InlineRenderer<T extends InlineData<T>> {
    Identifier getId();

    GlowHandling getGlowPreference(T data);

    int render(T data, GuiGraphicsExtractor drawContext, int index, Style style, int codepoint, TextRenderingContext trContext);

    int charWidth(T data, Style style, int codepoint);
}
