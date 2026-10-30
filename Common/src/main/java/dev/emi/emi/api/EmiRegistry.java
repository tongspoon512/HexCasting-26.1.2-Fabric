package dev.emi.emi.api;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.world.item.crafting.RecipeManager;

/**
 * Stub of emi's EmiRegistry.
 */
public interface EmiRegistry {
    void addCategory(EmiRecipeCategory category);

    void addWorkstation(EmiRecipeCategory category, EmiIngredient workstation);

    void addRecipe(EmiRecipe recipe);

    RecipeManager getRecipeManager();
}
