package at.petrak.hexcasting.datagen.recipe.builders;

import net.minecraft.advancements.Criterion;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// Largely adapted from the following classes:
// https://github.com/Creators-of-Create/Create/blob/82be76d8934af03b4e52cad6a9f74a4175ba7b05/src/main/java/com/simibubi/create/content/contraptions/processing/ProcessingOutput.java
// https://github.com/Creators-of-Create/Create/blob/82be76d8934af03b4e52cad6a9f74a4175ba7b05/src/main/java/com/simibubi/create/foundation/data/recipe/ProcessingRecipeGen.java
// https://github.com/Creators-of-Create/Create/blob/82be76d8934af03b4e52cad6a9f74a4175ba7b05/src/main/java/com/simibubi/create/content/contraptions/processing/ProcessingRecipeBuilder.java
// https://github.com/Creators-of-Create/Create/blob/82be76d8934af03b4e52cad6a9f74a4175ba7b05/src/main/java/com/simibubi/create/content/contraptions/processing/ProcessingRecipeSerializer.java
public class CreateCrushingRecipeBuilder implements RecipeBuilder {
    private String group = "";
    private Ingredient input;
    private final List<ProcessingOutput> results = new ArrayList<>();
    private int processingTime = 100;

    @Override
    public @NotNull CreateCrushingRecipeBuilder unlockedBy(@NotNull String name, @NotNull Criterion<?> trigger) {
        return this;
    }

    @Override
    public @NotNull CreateCrushingRecipeBuilder group(@Nullable String name) {
        group = name;
        return this;
    }

    public CreateCrushingRecipeBuilder duration(int duration) {
        this.processingTime = duration;
        return this;
    }

    public CreateCrushingRecipeBuilder withInput(ItemLike item) {
        return withInput(Ingredient.of(item));
    }

    public CreateCrushingRecipeBuilder withInput(Ingredient ingredient) {
        this.input = ingredient;
        return this;
    }

    public CreateCrushingRecipeBuilder withOutput(ItemLike output) {
        return withOutput(1f, output, 1);
    }

    public CreateCrushingRecipeBuilder withOutput(float chance, ItemLike output) {
        return withOutput(chance, output, 1);
    }

    public CreateCrushingRecipeBuilder withOutput(ItemLike output, int count) {
        return withOutput(1f, output, count);
    }

    public CreateCrushingRecipeBuilder withOutput(float chance, ItemLike output, int count) {
        return withOutput(new ItemStack(output, count), chance);
    }

    public CreateCrushingRecipeBuilder withOutput(ItemStack output, float chance) {
        this.results.add(new ItemProcessingOutput(output, chance));
        return this;
    }

    public CreateCrushingRecipeBuilder withOutput(String name) {
        return withOutput(1f, name, 1);
    }

    public CreateCrushingRecipeBuilder withOutput(String name, int count) {
        return withOutput(1f, name, count);
    }

    public CreateCrushingRecipeBuilder withOutput(float chance, String name) {
        return withOutput(chance, name, 1);
    }

    public CreateCrushingRecipeBuilder withOutput(float chance, String name, int count) {
        this.results.add(new CompatProcessingOutput(name, count, chance));
        return this;
    }

    @Override
    public ResourceKey<Recipe<?>> defaultId() {
        return ResourceKey.create(Registries.RECIPE,
            Identifier.fromNamespaceAndPath("create", "crushing/compat"));
    }

    // 26.1 no longer has FinishedRecipe, so datagen can only emit vanilla Recipe objects. Create's
    // processing recipes are not representable that way, so these compat recipes are not written.
    @Override
    public void save(RecipeOutput output, ResourceKey<Recipe<?>> id) {
    }
}
