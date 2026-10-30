package at.petrak.hexcasting.datagen.recipe.builders;

import at.petrak.hexcasting.common.recipe.BrainsweepRecipe;
import at.petrak.hexcasting.common.recipe.ingredient.StateIngredient;
import at.petrak.hexcasting.common.recipe.ingredient.brainsweep.BrainsweepeeIngredient;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeUnlockAdvancementBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BrainsweepRecipeBuilder implements RecipeBuilder {
	private final StateIngredient blockIn;
	private final BrainsweepeeIngredient entityIn;
	private final long mediaCost;
	private final BlockState result;

	private final RecipeUnlockAdvancementBuilder advancementBuilder = new RecipeUnlockAdvancementBuilder();

	public BrainsweepRecipeBuilder(StateIngredient blockIn, BrainsweepeeIngredient entityIn, BlockState result,
		long mediaCost) {
		this.blockIn = blockIn;
		this.entityIn = entityIn;
		this.result = result;
		this.mediaCost = mediaCost;
	}

	@Override
	public BrainsweepRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
		this.advancementBuilder.unlockedBy(name, criterion);
		return this;
	}

	@Override
	public BrainsweepRecipeBuilder group(@Nullable String groupName) {
		return this;
	}

	@Override
	public ResourceKey<Recipe<?>> defaultId() {
		return ResourceKey.create(Registries.RECIPE,
			Identifier.fromNamespaceAndPath("hexcasting", "brainsweep/default"));
	}

	@Override
	public void save(RecipeOutput output, ResourceKey<Recipe<?>> id) {
		var advancement = this.advancementBuilder.build(output, id, RecipeCategory.MISC);
		output.accept(id, new BrainsweepRecipe(this.blockIn, this.entityIn, this.mediaCost, this.result), advancement);
	}
}
