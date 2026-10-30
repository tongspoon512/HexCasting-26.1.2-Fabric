package com.samsthenerd.inline.api.client.extrahooks;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Stub of inline's ItemOverlayRenderer.
 */
@FunctionalInterface
public interface ItemOverlayRenderer {
    void render(ItemStack stack, GuiGraphicsExtractor graphics);

    static void addRenderer(Item item, ItemOverlayRenderer renderer) {
        // no-op stub
    }
}
