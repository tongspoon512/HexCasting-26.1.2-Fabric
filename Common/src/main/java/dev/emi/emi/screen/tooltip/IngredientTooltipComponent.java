package dev.emi.emi.screen.tooltip;

import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import java.util.List;

/**
 * Stub of emi's IngredientTooltipComponent.
 */
public class IngredientTooltipComponent implements ClientTooltipComponent {
    private final List<EmiStack> stacks;

    public IngredientTooltipComponent(List<EmiStack> stacks) {
        this.stacks = stacks;
    }

    @Override
    public int getHeight(Font font) {
        return 0;
    }

    @Override
    public int getWidth(Font font) {
        return 0;
    }
}
