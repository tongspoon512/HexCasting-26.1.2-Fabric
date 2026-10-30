package at.petrak.paucal.api.datagen;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.function.Consumer;

public abstract class PaucalRecipeProvider extends RecipeProvider {
    protected final String modid;
    protected final HolderGetter<Item> itemLookup;

    protected PaucalRecipeProvider(HolderLookup.Provider registries, RecipeOutput output, String modid) {
        super(registries, output);
        this.modid = modid;
        this.itemLookup = registries.lookupOrThrow(Registries.ITEM);
    }

    protected Identifier modLoc(String path) {
        return Identifier.fromNamespaceAndPath(modid, path);
    }

    protected Criterion<InventoryChangeTrigger.TriggerInstance> hasItem(ItemLike item) {
        return has(item);
    }

    protected Criterion<InventoryChangeTrigger.TriggerInstance> hasItem(TagKey<Item> tag) {
        return has(tag);
    }

    protected RecipeBuilder ring(RecipeCategory category, ItemLike result, int count, ItemLike outer, ItemLike center) {
        return ShapedRecipeBuilder.shaped(itemLookup, category, result, count)
            .define('O', outer).define('C', center)
            .pattern("OOO").pattern("OCO").pattern("OOO");
    }

    protected RecipeBuilder ringCornerless(RecipeCategory category, ItemLike result, int count, ItemLike outer, ItemLike center) {
        return ShapedRecipeBuilder.shaped(itemLookup, category, result, count)
            .define('O', outer).define('C', center)
            .pattern(" O ").pattern("OCO").pattern(" O ");
    }

    protected RecipeBuilder ringCornerless(RecipeCategory category, ItemLike result, int count, Ingredient outer, Ingredient center) {
        return ShapedRecipeBuilder.shaped(itemLookup, category, result, count)
            .define('O', outer).define('C', center)
            .pattern(" O ").pattern("OCO").pattern(" O ");
    }

    protected RecipeBuilder ringAll(RecipeCategory category, ItemLike result, int count, ItemLike outer, ItemLike center) {
        return ShapedRecipeBuilder.shaped(itemLookup, category, result, count)
            .define('O', outer)
            .pattern("OOO").pattern("OOO").pattern("OOO");
    }

    protected RecipeBuilder stack(RecipeCategory category, ItemLike result, int count, ItemLike outer, ItemLike center) {
        return ShapedRecipeBuilder.shaped(itemLookup, category, result, count)
            .define('O', outer).define('C', center)
            .pattern("O").pattern("C").pattern("O");
    }

    protected RecipeBuilder stack(RecipeCategory category, ItemLike result, int count, Ingredient outer, ItemLike center) {
        return ShapedRecipeBuilder.shaped(itemLookup, category, result, count)
            .define('O', outer).define('C', center)
            .pattern("O").pattern("C").pattern("O");
    }

    protected RecipeBuilder stack(RecipeCategory category, ItemLike result, int count, Ingredient outer, Ingredient center) {
        return ShapedRecipeBuilder.shaped(itemLookup, category, result, count)
            .define('O', outer).define('C', center)
            .pattern("O").pattern("C").pattern("O");
    }

    protected RecipeBuilder stick(RecipeCategory category, ItemLike result, int count, ItemLike outer) {
        return ShapedRecipeBuilder.shaped(itemLookup, category, result, count)
            .define('O', outer)
            .pattern("O").pattern("O");
    }

    protected void packing(RecipeCategory category, ItemLike input, ItemLike output, String name,
                           boolean reversible, Consumer<RecipeBuilder> extras) {
        var pack = ShapedRecipeBuilder.shaped(itemLookup, category, output)
            .define('I', input)
            .pattern("III").pattern("III").pattern("III");
        if (extras != null) extras.accept(pack);
        pack.unlockedBy("has_item", hasItem(input)).save(this.output, modLoc(name).toString());

        if (reversible) {
            var unpack = ShapelessRecipeBuilder.shapeless(itemLookup, category, input, 9)
                .requires(output);
            unpack.unlockedBy("has_item", hasItem(output)).save(this.output, modLoc(name + "_unpack").toString());
        }
    }
}
