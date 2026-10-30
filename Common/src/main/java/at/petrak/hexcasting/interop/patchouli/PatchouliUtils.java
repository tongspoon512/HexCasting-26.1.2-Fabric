package at.petrak.hexcasting.interop.patchouli;

import at.petrak.hexcasting.api.misc.MediaConstants;
import at.petrak.hexcasting.common.items.magic.ItemMediaBattery;
import at.petrak.hexcasting.common.lib.HexItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import vazkii.patchouli.api.IVariable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * > no this is a "literally copy these files/parts of file into your mod"
 * > we should put this in patchy but lol
 * > lazy
 * -- Hubry Vazcord
 */
public class PatchouliUtils {
    /**
     * Placeholder for a slotted ingredient that the recipe doesn't actually use.
     */
    public static final Ingredient EMPTY = Ingredient.of(Stream.<ItemLike>empty());

    @SuppressWarnings("unchecked")
    public static <T extends Recipe<?>> T getRecipe(RecipeType<T> type, Identifier id) {
        // PageDoubleRecipeRegistry
        if (Minecraft.getInstance().level == null) {
            return null;
        }
        // 26.1 no longer ships the full recipe list to the client; only the integrated server's
        // RecipeManager can still be enumerated.
        if (!(Minecraft.getInstance().level.recipeAccess() instanceof RecipeManager manager)) {
            return null;
        }
        for (var holder : manager.getRecipes()) {
            if (holder.id().identifier().equals(id) && holder.value().getType().equals(type)) {
                return (T) holder.value();
            }
        }
        return null;
    }

    public static ItemStack getResultItem(CraftingRecipe recipe, Level level) {
        var displays = recipe.display();
        if (displays.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return displays.getFirst().result().resolveForFirstStack(SlotDisplayContext.fromLevel(level));
    }

    /**
     * Combines the ingredients, returning the first matching stack of each, then the second stack of each, etc.
     * looping back ingredients that run out of matched stacks, until the ingredients reach the length
     * of the longest ingredient in the recipe set.
     *
     * @param ingredients           List of ingredients in the specific slot
     * @param longestIngredientSize Longest ingredient in the entire recipe
     * @return Serialized Patchouli ingredient string
     */
    public static IVariable interweaveIngredients(List<Ingredient> ingredients, int longestIngredientSize) {
        if (ingredients.size() == 1) {
            return IVariable.wrapList(itemStacks(ingredients.get(0)).stream().map(IVariable::from).collect(
                Collectors.toList()));
        }

        ItemStack[] empty = {ItemStack.EMPTY};
        List<ItemStack[]> stacks = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            if (ingredient != null && !ingredient.isEmpty()) {
                stacks.add(itemStacks(ingredient).toArray(new ItemStack[0]));
            } else {
                stacks.add(empty);
            }
        }
        List<IVariable> list = new ArrayList<>(stacks.size() * longestIngredientSize);
        for (int i = 0; i < longestIngredientSize; i++) {
            for (ItemStack[] stack : stacks) {
                list.add(IVariable.from(stack[i % stack.length]));
            }
        }
        return IVariable.wrapList(list);
    }

    /**
     * Overload of the method above that uses the provided list's longest ingredient size.
     */
    public static IVariable interweaveIngredients(List<Ingredient> ingredients) {
        return interweaveIngredients(ingredients,
            ingredients.stream().mapToInt(ingr -> (int) ingr.items().count()).max().orElse(1));
    }

    private static List<ItemStack> itemStacks(Ingredient ingredient) {
        return ingredient.items().map(Holder::value).map(ItemStack::new).toList();
    }

    /**
     * @param mediaCost The amount of media to represent
     * @return A list of ItemStacks in dust, shards, and charged that equal the given amount of media. If the amount cannot be represented in an integer multiple of dust, falls back to a phial.
     */
    public static List<ItemStack> mediaItems(long mediaCost) {
        record ItemCost(Item item, int cost) {
            public boolean dividesEvenly (int dividend) {
                return dividend % cost == 0;
            }
        }
        ItemCost[] costs = {
            new ItemCost(HexItems.AMETHYST_DUST, (int)MediaConstants.DUST_UNIT),
            new ItemCost(Items.AMETHYST_SHARD, (int)MediaConstants.SHARD_UNIT),
            new ItemCost(HexItems.CHARGED_AMETHYST, (int)MediaConstants.CRYSTAL_UNIT),
        };

        // get evenly divisible ItemStacks
        List<ItemStack> validItemStacks = Arrays.stream(costs)
            .filter(itemCost -> itemCost.dividesEvenly((int)mediaCost))
            .map(validItemCost -> new ItemStack(validItemCost.item, (int)mediaCost / validItemCost.cost))
            .toList();

        if (!validItemStacks.isEmpty()) return validItemStacks;

        return List.of(ItemMediaBattery.withMedia(new ItemStack(HexItems.BATTERY), mediaCost, mediaCost));
    }
}
