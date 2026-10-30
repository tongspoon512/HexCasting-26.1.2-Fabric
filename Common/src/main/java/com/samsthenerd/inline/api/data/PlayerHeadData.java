package com.samsthenerd.inline.api.data;

import com.samsthenerd.inline.api.InlineData;

/**
 * Stub of inline's PlayerHeadData.
 */
public class PlayerHeadData implements InlineData<PlayerHeadData> {
    @Override
    public InlineDataType<PlayerHeadData> getType() {
        return null;
    }

    @Override
    public net.minecraft.resources.Identifier getRendererId() {
        return null;
    }

    @Override
    public net.minecraft.network.chat.Style getExtraStyle() {
        return net.minecraft.network.chat.Style.EMPTY;
    }

    @Override
    public net.minecraft.network.chat.Component asText(boolean withExtra) {
        return net.minecraft.network.chat.Component.empty();
    }
}
