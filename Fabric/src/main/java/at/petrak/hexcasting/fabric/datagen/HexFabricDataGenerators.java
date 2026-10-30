package at.petrak.hexcasting.fabric.datagen;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.datagen.HexAdvancements;
import at.petrak.hexcasting.datagen.HexLootTables;
import at.petrak.hexcasting.datagen.IXplatIngredients;
import at.petrak.hexcasting.datagen.recipe.HexplatRecipes;
import at.petrak.hexcasting.datagen.recipe.builders.FarmersDelightToolIngredient;
import at.petrak.hexcasting.datagen.tag.HexActionTagProvider;
import at.petrak.hexcasting.datagen.tag.HexBlockTagProvider;
import at.petrak.hexcasting.datagen.tag.HexItemTagProvider;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.EnumMap;
import java.util.List;
import java.util.Set;

public class HexFabricDataGenerators implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator gen) {
        HexAPI.LOGGER.info("Starting Fabric-specific datagen");

        var pack = gen.createPack();
        var xtags = IXplatAbstractions.INSTANCE.tags();

        pack.addProvider((FabricDataGenerator.Pack.RegistryDependentFactory<FabricRecipeProvider>)
            (output, registries) -> new FabricRecipeProvider(output, registries) {
                @Override
                protected RecipeProvider createRecipeProvider(HolderLookup.Provider provider, RecipeOutput recipeOutput) {
                    return new HexplatRecipes(provider, recipeOutput, INGREDIENTS, HexFabricConditionsBuilder::new);
                }

                @Override
                public String getName() {
                    return "Hexcasting Recipes";
                }
            });

        var btagProviderWrapper = new BlockTagProviderWrapper(); // CURSED
        pack.addProvider((output, lookup) -> {
            btagProviderWrapper.provider = new HexBlockTagProvider(output, lookup, xtags);
            return btagProviderWrapper.provider;
        });
        pack.addProvider((output, lookup) -> new HexItemTagProvider(output, lookup, btagProviderWrapper.provider, xtags));

        pack.addProvider(HexActionTagProvider::new);
        pack.addProvider((FabricDataGenerator.Pack.Factory<HexItemModelDefinitionProvider>)
            HexItemModelDefinitionProvider::new);

        pack.addProvider((output, lookup) ->
            new AdvancementProvider(output, lookup, List.of(new HexAdvancements())));

        pack.addProvider((FabricDataGenerator.Pack.RegistryDependentFactory<LootTableProvider>) (output, registries) ->
            new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(HexLootTables::new, LootContextParamSets.ALL_PARAMS)),
                registries));
    }

    private static class BlockTagProviderWrapper {
        HexBlockTagProvider provider;
    }

    private static final IXplatIngredients INGREDIENTS = new IXplatIngredients() {
        // 26.1: Ingredient is a final HolderSet-backed type, so the old item+tag unions collapse to the vanilla item.
        @Override
        public Ingredient glowstoneDust() {
            return Ingredient.of(Items.GLOWSTONE_DUST);
        }

        @Override
        public Ingredient leather() {
            // apparently c:leather also includes rabbit hide
            return Ingredient.of(Items.LEATHER);
        }

        @Override
        public Ingredient ironNugget() {
            return Ingredient.of(Items.IRON_NUGGET);
        }

        @Override
        public Ingredient goldNugget() {
            return Ingredient.of(Items.GOLD_NUGGET);
        }

        @Override
        public Ingredient copperIngot() {
            return Ingredient.of(Items.COPPER_INGOT);
        }

        @Override
        public Ingredient ironIngot() {
            return Ingredient.of(Items.IRON_INGOT);
        }

        @Override
        public Ingredient goldIngot() {
            return Ingredient.of(Items.GOLD_INGOT);
        }

        @Override
        public EnumMap<DyeColor, Ingredient> dyes() {
            var out = new EnumMap<DyeColor, Ingredient>(DyeColor.class);
            for (var col : DyeColor.values()) {
                out.put(col, Ingredient.of(dyeItem(col)));
            }
            return out;
        }

        @Override
        public Ingredient stick() {
            return Ingredient.of(Items.STICK);
        }

        @Override
        public Ingredient whenModIngredient(Ingredient defaultIngredient, String modid, Ingredient modIngredient) {
            return IXplatAbstractions.INSTANCE.isModPresent(modid) ? modIngredient : defaultIngredient;
        }

        private final FarmersDelightToolIngredient AXE_INGREDIENT = () -> {
            JsonObject object = new JsonObject();
            object.addProperty("type", "farmersdelight:tool");
            object.addProperty("tag", "c:tools/axes");
            return object;
        };

        @Override
        public FarmersDelightToolIngredient axeStrip() {
            return AXE_INGREDIENT;
        }

        @Override
        public FarmersDelightToolIngredient axeDig() {
            return AXE_INGREDIENT;
        }
    };

    private static Item dyeItem(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.WHITE_DYE;
            case ORANGE -> Items.ORANGE_DYE;
            case MAGENTA -> Items.MAGENTA_DYE;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_DYE;
            case YELLOW -> Items.YELLOW_DYE;
            case LIME -> Items.LIME_DYE;
            case PINK -> Items.PINK_DYE;
            case GRAY -> Items.GRAY_DYE;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_DYE;
            case CYAN -> Items.CYAN_DYE;
            case PURPLE -> Items.PURPLE_DYE;
            case BLUE -> Items.BLUE_DYE;
            case BROWN -> Items.BROWN_DYE;
            case GREEN -> Items.GREEN_DYE;
            case RED -> Items.RED_DYE;
            case BLACK -> Items.BLACK_DYE;
        };
    }
}
