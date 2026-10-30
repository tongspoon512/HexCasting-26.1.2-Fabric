package com.samsthenerd.inline.api;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * Stub of inline's InlineData.
 */
public interface InlineData<T extends InlineData<T>> {
    InlineDataType<T> getType();

    Identifier getRendererId();

    Style getExtraStyle();

    default Style asStyle(boolean withExtra) {
        return withExtra ? getExtraStyle() : Style.EMPTY;
    }

    Component asText(boolean withExtra);

    interface InlineDataType<T extends InlineData<T>> {
        Identifier getId();

        Codec<T> getCodec();
    }
}
