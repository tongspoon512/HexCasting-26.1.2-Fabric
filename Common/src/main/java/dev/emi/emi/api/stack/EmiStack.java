package dev.emi.emi.api.stack;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;

/**
 * Stub of emi's EmiStack.
 */
public class EmiStack implements EmiIngredient {
    public EmiStack() {
    }

    public static EmiStack of(ItemLike item) {
        return new EmiStack();
    }

    public static EmiStack of(ItemStack stack) {
        return new EmiStack();
    }

    @Override
    public List<EmiStack> getEmiStacks() {
        return List.of(this);
    }

    @Override
    public EmiStack setAmount(long amount) {
        return this;
    }

    @Override
    public long getAmount() {
        return 0;
    }
}
