package at.petrak.hexcasting.interop.patchouli;

import at.petrak.hexcasting.common.recipe.BrainsweepRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jetbrains.annotations.Nullable;
import vazkii.patchouli.api.IComponentProcessor;
import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.api.IVariableProvider;

import java.util.List;

public class BrainsweepProcessor implements IComponentProcessor {
	private BrainsweepRecipe recipe;
	@Nullable
	private List<IVariable> exampleEntityList;

	@Override
	public void setup(Level level, IVariableProvider vars) {
		var id = Identifier.withDefaultNamespace(vars.get("recipe").asString());

		if (level.recipeAccess() instanceof RecipeManager recman) {
			for (var holder : recman.getRecipes()) {
				if (holder.value() instanceof BrainsweepRecipe recipe && holder.id().identifier().equals(id)) {
					this.recipe = recipe;
					break;
				}
			}
		}
	}

	@Override
	public IVariable process(Level level, String key) {
		if (this.recipe == null) {
			return null;
		}

		switch (key) {
			case "header" -> {
				return IVariable.from(this.recipe.result().getBlock().getName());
			}
			case "input" -> {
				var inputStacks = this.recipe.blockIn().getDisplayedStacks();
				return IVariable.from(inputStacks.toArray(new ItemStack[0]));
			}
			case "result" -> {
				return IVariable.from(new ItemStack(this.recipe.result().getBlock()));
			}

			case "entity" -> {
				if (this.exampleEntityList == null) {
					var entities = this.recipe.entityIn().exampleEntities(Minecraft.getInstance().level);
					if (entities.isEmpty()) {
						// oh dear
						return null;
					}

					this.exampleEntityList = entities.stream().map(entity -> {
						var bob = new StringBuilder();
						bob.append(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));

						var tag = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
						entity.save(tag);
						bob.append(tag.buildResult().toString());
						return IVariable.wrap(bob.toString());
					}).toList();
				}

				//return IVariable.wrapList(this.exampleEntityList);
				// patchouli does not currently support cycling entity displays
				// https://github.com/VazkiiMods/Patchouli/issues/852
				return this.exampleEntityList.get(0);
			}
			case "entityTooltip" -> {
				Minecraft mc = Minecraft.getInstance();
				return IVariable.wrapList(this.recipe.entityIn()
					.getTooltip(mc.options.advancedItemTooltips)
					.stream()
					.map(IVariable::from)
					.toList());
			}
			case "mediaCost" -> {
				List<ItemStack> stacks = PatchouliUtils.mediaItems(this.recipe.mediaCost());
				return IVariable.wrapList(stacks.stream().map(IVariable::from).toList());
			}
			default -> {
				return null;
			}
		}
	}
}
