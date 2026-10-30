package dev.emi.emi.api.recipe;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Stub of emi's EmiRecipe.
 */
public interface EmiRecipe {
    EmiRecipeCategory getCategory();

    @Nullable
    Identifier getId();

    List<EmiIngredient> getInputs();

    List<EmiStack> getOutputs();

    int getDisplayWidth();

    int getDisplayHeight();

    void addWidgets(WidgetHolder widgets);

    default boolean supportsRecipeTree() {
        return true;
    }
}
