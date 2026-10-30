package at.petrak.hexcasting.fabric.datagen;

import at.petrak.hexcasting.datagen.IXplatConditionsBuilder;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.fabric.impl.datagen.FabricDataGenHelper;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.Criterion;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class HexFabricConditionsBuilder implements IXplatConditionsBuilder {
    private final List<ResourceCondition> conditions = new ArrayList<>();
    private final RecipeBuilder parent;

    public HexFabricConditionsBuilder(RecipeBuilder parent) {
        this.parent = parent;
    }

    @Override
    public IXplatConditionsBuilder whenModLoaded(String modid) {
        conditions.add(ResourceConditions.allModsLoaded(modid));
        return this;
    }

    @Override
    public IXplatConditionsBuilder whenModMissing(String modid) {
        conditions.add(ResourceConditions.not(ResourceConditions.allModsLoaded(modid)));
        return this;
    }

    @Override
    public RecipeBuilder unlockedBy(@NotNull String string, @NotNull Criterion<?> criterion) {
        return parent.unlockedBy(string, criterion);
    }

    @Override
    public RecipeBuilder group(@Nullable String string) {
        return parent.group(string);
    }

    @Override
    public ResourceKey<Recipe<?>> defaultId() {
        return parent.defaultId();
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void save(@NotNull RecipeOutput output, @NotNull ResourceKey<Recipe<?>> id) {
        if (conditions.isEmpty()) {
            parent.save(output, id);
            return;
        }

        var conditional = new RecipeOutput() {
            @Override
            public void accept(@NotNull ResourceKey<Recipe<?>> key, @NotNull Recipe<?> recipe,
                @NotNull AdvancementHolder advancement) {
                FabricDataGenHelper.addConditions(recipe, conditions.toArray(new ResourceCondition[0]));
                output.accept(key, recipe, advancement);
            }

            @Override
            public Advancement.@NotNull Builder advancement() {
                return output.advancement();
            }

            @Override
            public void includeRootAdvancement() {
                // NO-OP
            }
        };

        parent.save(conditional, id);
    }
}
