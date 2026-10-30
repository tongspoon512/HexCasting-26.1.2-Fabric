package dev.emi.emi.api.widget;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.Identifier;

import java.util.Random;
import java.util.function.Function;

/**
 * Stub of emi's WidgetHolder.
 */
public interface WidgetHolder {
    SlotWidget addSlot(EmiIngredient stack, int x, int y);

    SlotWidget add(Widget widget);

    SlotWidget addGeneratedSlot(Function<Random, EmiStack> supplier, int x, int y);

    SlotWidget addGeneratedSlot(Function<Random, EmiStack> supplier, int uniq, int x, int y);

    void addTexture(Identifier id, int x, int y, int width, int height,
                    int u, int v, int regionWidth, int regionHeight,
                    int textureWidth, int textureHeight);
}
