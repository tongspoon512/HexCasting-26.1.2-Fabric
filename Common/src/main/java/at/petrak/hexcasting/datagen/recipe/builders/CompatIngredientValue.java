package at.petrak.hexcasting.datagen.recipe.builders;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public final class CompatIngredientValue {
    public final String item;

    public CompatIngredientValue(String name) {
        this.item = name;
    }

    public static Ingredient of(String itemName) {
        // 26.1 ingredients have to resolve to registered item holders, so an arbitrary (modded) item id
        // can't be represented here without that mod being present in the registry.
        return Ingredient.of(Items.AIR);
    }
}
