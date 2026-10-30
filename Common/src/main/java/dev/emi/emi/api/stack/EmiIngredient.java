package dev.emi.emi.api.stack;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import java.util.List;

/**
 * Stub of emi's EmiIngredient.
 */
public interface EmiIngredient {
    int RENDER_ICON = 1;
    int RENDER_AMOUNT = 2;
    int RENDER_REMAINDER = 4;
    int RENDER_INGREDIENT = 8;

    List<EmiStack> getEmiStacks();

    EmiIngredient setAmount(long amount);

    long getAmount();

    default boolean isEmpty() {
        return false;
    }

    default EmiIngredient copy() {
        return this;
    }

    default float getChance() {
        return 1;
    }

    default EmiIngredient setChance(float chance) {
        return this;
    }

    default CompoundTag getNbt() {
        return null;
    }

    default Object getKey() {
        return this;
    }

    default Identifier getId() {
        return null;
    }

    default List<Component> getTooltipText() {
        return List.of();
    }

    default Component getName() {
        return Component.empty();
    }

    default List<ClientTooltipComponent> getTooltip() {
        return List.of();
    }

    default void render(GuiGraphicsExtractor draw, int x, int y, float delta) {
    }

    default void render(GuiGraphicsExtractor draw, int x, int y, float delta, int flags) {
    }

    static EmiIngredient of(ItemLike item) {
        return EmiStack.of(item);
    }

    static EmiIngredient of(TagKey<Item> tag) {
        return new EmiStack();
    }

    static EmiIngredient of(List<EmiStack> stacks) {
        return new EmiStack();
    }
}
