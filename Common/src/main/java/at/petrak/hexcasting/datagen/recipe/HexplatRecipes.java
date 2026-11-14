package at.petrak.hexcasting.datagen.recipe;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.api.misc.MediaConstants;
import at.petrak.hexcasting.api.mod.HexTags;
import at.petrak.hexcasting.common.blocks.decoration.BlockAkashicLog;
import at.petrak.hexcasting.common.items.ItemStaff;
import at.petrak.hexcasting.common.items.pigment.ItemPridePigment;
import at.petrak.hexcasting.common.lib.HexBlocks;
import at.petrak.hexcasting.common.lib.HexItems;
import at.petrak.hexcasting.common.recipe.SealThingsRecipe;
import at.petrak.hexcasting.common.recipe.ingredient.StateIngredientHelper;
import at.petrak.hexcasting.common.recipe.ingredient.brainsweep.EntityTypeIngredient;
import at.petrak.hexcasting.common.recipe.ingredient.brainsweep.VillagerIngredient;
import at.petrak.hexcasting.datagen.HexAdvancements;
import at.petrak.hexcasting.datagen.IXplatConditionsBuilder;
import at.petrak.hexcasting.datagen.IXplatIngredients;
import at.petrak.hexcasting.datagen.recipe.builders.BrainsweepRecipeBuilder;
import at.petrak.hexcasting.datagen.recipe.builders.CompatIngredientValue;
import at.petrak.hexcasting.datagen.recipe.builders.FarmersDelightCuttingRecipeBuilder;
import at.petrak.paucal.api.datagen.PaucalRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

// TODO: need to do a big refactor of this class cause it's giant and unwieldy, probably as part of #360
public class HexplatRecipes extends PaucalRecipeProvider {
    private final IXplatIngredients ingredients;
    private final Function<RecipeBuilder, IXplatConditionsBuilder> conditions;

    private final List<BlockAkashicLog> EDIFIED_LOGS = List.of(
        HexBlocks.EDIFIED_LOG, HexBlocks.EDIFIED_LOG_AMETHYST,
        HexBlocks.EDIFIED_LOG_AVENTURINE, HexBlocks.EDIFIED_LOG_CITRINE,
        HexBlocks.EDIFIED_LOG_PURPLE);

    private final Map<BlockAkashicLog, BlockAkashicLog> EDIFIED_LOG_TO_WOOD = Map.ofEntries(
        Map.entry(HexBlocks.EDIFIED_LOG, HexBlocks.EDIFIED_WOOD),
//      These don't exist, idk if they should
//        Map.entry(HexBlocks.EDIFIED_LOG_AMETHYST, HexBlocks.EDIFIED_WOOD_AMETHYST),
//        Map.entry(HexBlocks.EDIFIED_LOG_AVENTURINE, HexBlocks.EDIFIED_WOOD_AVENTURINE),
//        Map.entry(HexBlocks.EDIFIED_LOG_CITRINE, HexBlocks.EDIFIED_WOOD_CITRINE),
//        Map.entry(HexBlocks.EDIFIED_LOG_PURPLE, HexBlocks.EDIFIED_WOOD_PURPLE),
        Map.entry(HexBlocks.STRIPPED_EDIFIED_LOG, HexBlocks.STRIPPED_EDIFIED_WOOD)
    );

    public HexplatRecipes(HolderLookup.Provider registries, RecipeOutput output, IXplatIngredients ingredients,
                          Function<RecipeBuilder, IXplatConditionsBuilder> conditions) {
        super(registries, output, HexAPI.MOD_ID);
        this.ingredients = ingredients;
        this.conditions = conditions;
    }

    private static ResourceKey<Recipe<?>> recipeKey(Identifier id) {
        return ResourceKey.create(Registries.RECIPE, id);
    }

    @Override
    public void buildRecipes() {
        specialRecipe(SealThingsRecipe.FOCUS_SERIALIZER);
        specialRecipe(SealThingsRecipe.SPELLBOOK_SERIALIZER);

        staffRecipe(HexItems.STAFF_OAK, Items.OAK_PLANKS);
        staffRecipe(HexItems.STAFF_BIRCH, Items.BIRCH_PLANKS);
        staffRecipe(HexItems.STAFF_SPRUCE, Items.SPRUCE_PLANKS);
        staffRecipe(HexItems.STAFF_JUNGLE, Items.JUNGLE_PLANKS);
        staffRecipe(HexItems.STAFF_DARK_OAK, Items.DARK_OAK_PLANKS);
        staffRecipe(HexItems.STAFF_ACACIA, Items.ACACIA_PLANKS);
        staffRecipe(HexItems.STAFF_CRIMSON, Items.CRIMSON_PLANKS);
        staffRecipe(HexItems.STAFF_WARPED, Items.WARPED_PLANKS);
        staffRecipe(HexItems.STAFF_MANGROVE, Items.MANGROVE_PLANKS);
        staffRecipe(HexItems.STAFF_CHERRY, Items.CHERRY_PLANKS);
        staffRecipe(HexItems.STAFF_BAMBOO, Items.BAMBOO_PLANKS);
        staffRecipe(HexItems.STAFF_EDIFIED, HexBlocks.EDIFIED_PLANKS.asItem());
        staffRecipe(HexItems.STAFF_QUENCHED, HexItems.QUENCHED_SHARD);
        staffRecipe(HexItems.STAFF_MINDSPLICE, tag(HexTags.Items.MINDFLAYED_CIRCLE_COMPONENTS));

        ShapelessRecipeBuilder.shapeless(itemLookup, RecipeCategory.TOOLS, HexItems.THOUGHT_KNOT)
            .requires(HexItems.AMETHYST_DUST)
            .requires(Items.STRING)
            .unlockedBy("has_item", hasItem(HexTags.Items.STAVES))
            .save(this.output);
        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.TOOLS, HexItems.FOCUS)
            .define('G', ingredients.glowstoneDust())
            .define('L', ingredients.leather())
            .define('P', Items.PAPER)
            .define('A', HexItems.CHARGED_AMETHYST)
            .pattern("GLG")
            .pattern("PAP")
            .pattern("GLG")
            .unlockedBy("has_item", hasItem(HexTags.Items.STAVES))
            .save(this.output);
        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.TOOLS, HexItems.FOCUS)
            .define('G', ingredients.glowstoneDust())
            .define('L', ingredients.leather())
            .define('P', Items.PAPER)
            .define('A', HexItems.CHARGED_AMETHYST)
            .pattern("GPG")
            .pattern("LAL")
            .pattern("GPG")
            .unlockedBy("has_item", hasItem(HexTags.Items.STAVES))
            .save(this.output, modLoc("focus_rotated").toString());

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.TOOLS, HexItems.SPELLBOOK)
            .define('N', ingredients.goldNugget())
            .define('B', Items.WRITABLE_BOOK)
            .define('A', HexItems.CHARGED_AMETHYST)
            .define('F', Items.CHORUS_FRUIT) // i wanna gate this behind the end SOMEHOW
            // hey look its my gender ^^
            .pattern("NBA")
            .pattern("NFA")
            .pattern("NBA")
            .unlockedBy("has_focus", hasItem(HexItems.FOCUS))
            .unlockedBy("has_chorus", hasItem(Items.CHORUS_FRUIT)).save(this.output);

        ringCornerless(RecipeCategory.TOOLS,
            HexItems.CYPHER, 1,
            ingredients.copperIngot(),
            Ingredient.of(HexItems.AMETHYST_DUST))
            .unlockedBy("has_item", hasItem(HexTags.Items.STAVES)).save(this.output);

        ringCornerless(RecipeCategory.TOOLS,
            HexItems.TRINKET, 1,
            ingredients.ironIngot(),
            Ingredient.of(Items.AMETHYST_SHARD))
            .unlockedBy("has_item", hasItem(HexTags.Items.STAVES)).save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.TOOLS, HexItems.ARTIFACT)
            .define('F', ingredients.goldIngot())
            .define('A', HexItems.CHARGED_AMETHYST)
            // 26.1 移除了 minecraft:music_discs，这里改用包含全部唱片的 hexcassettes 标签
            .define('D', TagKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath("hexcassettes", "music_discs")))
            .pattern(" F ")
            .pattern("FAF")
            .pattern(" D ")
            .unlockedBy("has_item", hasItem(HexTags.Items.STAVES)).save(this.output);

        ringCornerless(RecipeCategory.TOOLS, HexItems.SCRYING_LENS, 1, Items.GLASS, HexItems.AMETHYST_DUST)
            .unlockedBy("has_item", hasItem(HexTags.Items.STAVES)).save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.TOOLS, HexItems.LENS_ASSEMBLY)
            .define('i', ingredients.goldIngot())
            .define('z', HexItems.CHARGED_AMETHYST)
            .define('t', HexItems.SCRYING_LENS)
            .pattern(" i ")
            .pattern("tzt")
            .pattern(" i ")
            .unlockedBy("has_item", hasItem(HexTags.Items.STAVES)).save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.TOOLS, HexItems.ABACUS)
            .define('S', Items.STICK)
            .define('A', Items.AMETHYST_SHARD)
            .define('W', ItemTags.PLANKS)
            .pattern("WAW")
            .pattern("SAS")
            .pattern("WAW")
            .unlockedBy("has_item", hasItem(HexTags.Items.STAVES)).save(this.output);

        // Why am I like this
        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.FOOD, HexItems.SUBMARINE_SANDWICH)
            .define('S', Items.STICK)
            .define('A', Items.AMETHYST_SHARD)
            .define('C', Items.COOKED_BEEF)
            .define('B', Items.BREAD)
            .pattern(" SA")
            .pattern(" C ")
            .pattern(" B ")
            .unlockedBy("has_item", hasItem(Items.AMETHYST_SHARD)).save(this.output);

        for (var dye : DyeColor.values()) {
            var item = HexItems.DYE_PIGMENTS.get(dye);
            ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.MISC, item)
                .define('D', HexItems.AMETHYST_DUST)
                .define('C', ingredients.dyes().get(dye))
                .pattern(" D ")
                .pattern("DCD")
                .pattern(" D ")
                .unlockedBy("has_item", hasItem(HexItems.AMETHYST_DUST)).save(this.output);
        }

        gayRecipe(ItemPridePigment.Type.AGENDER, Ingredient.of(Items.GLASS));
        gayRecipe(ItemPridePigment.Type.AROACE, Ingredient.of(Items.WHEAT_SEEDS));
        gayRecipe(ItemPridePigment.Type.AROMANTIC, Ingredient.of(Items.ARROW));
        gayRecipe(ItemPridePigment.Type.ASEXUAL, Ingredient.of(Items.BREAD));
        gayRecipe(ItemPridePigment.Type.BISEXUAL, Ingredient.of(Items.WHEAT));
        gayRecipe(ItemPridePigment.Type.DEMIBOY, Ingredient.of(Items.RAW_IRON));
        gayRecipe(ItemPridePigment.Type.DEMIGIRL, Ingredient.of(Items.RAW_COPPER));
        gayRecipe(ItemPridePigment.Type.GAY, Ingredient.of(Items.STONE_BRICK_WALL));
        gayRecipe(ItemPridePigment.Type.GENDERFLUID, Ingredient.of(Items.WATER_BUCKET));
        gayRecipe(ItemPridePigment.Type.GENDERQUEER, Ingredient.of(Items.GLASS_BOTTLE));
        gayRecipe(ItemPridePigment.Type.INTERSEX, Ingredient.of(Items.AZALEA));
        gayRecipe(ItemPridePigment.Type.LESBIAN, Ingredient.of(Items.HONEYCOMB));
        gayRecipe(ItemPridePigment.Type.NONBINARY, Ingredient.of(Items.MOSS_BLOCK));
        gayRecipe(ItemPridePigment.Type.PANSEXUAL, Ingredient.of(Items.CARROT));
        gayRecipe(ItemPridePigment.Type.PLURAL, Ingredient.of(Items.REPEATER));
        gayRecipe(ItemPridePigment.Type.TRANSGENDER, Ingredient.of(Items.EGG));

        ring(RecipeCategory.MISC, HexItems.UUID_PIGMENT, 1, HexItems.AMETHYST_DUST, Items.AMETHYST_SHARD)
            .unlockedBy("has_item", hasItem(HexItems.AMETHYST_DUST)).save(this.output);
        ringCornerless(RecipeCategory.MISC, HexItems.DEFAULT_PIGMENT, 1, HexItems.AMETHYST_DUST, Items.AMETHYST_SHARD)
            .unlockedBy("has_item", hasItem(HexItems.AMETHYST_DUST)).save(this.output);
        ringCornerless(RecipeCategory.MISC, HexItems.ANCIENT_PIGMENT, 1, HexItems.AMETHYST_DUST, Items.COPPER_INGOT)
            .unlockedBy("has_item", hasItem(HexItems.AMETHYST_DUST)).save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.DECORATIONS, HexItems.SCROLL_SMOL)
            .define('P', Items.PAPER)
            .define('A', HexItems.AMETHYST_DUST)
            .pattern(" A")
            .pattern("P ")
            .unlockedBy("has_item", hasItem(HexTags.Items.STAVES)).save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.DECORATIONS, HexItems.SCROLL_MEDIUM)
            .define('P', Items.PAPER)
            .define('A', HexItems.AMETHYST_DUST)
            .pattern("  A")
            .pattern("PP ")
            .pattern("PP ")
            .unlockedBy("has_item", hasItem(HexTags.Items.STAVES)).save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.DECORATIONS, HexItems.SCROLL_LARGE)
            .define('P', Items.PAPER)
            .define('A', HexItems.AMETHYST_DUST)
            .pattern("PPA")
            .pattern("PPP")
            .pattern("PPP")
            .unlockedBy("has_item", hasItem(HexTags.Items.STAVES)).save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.DECORATIONS, HexItems.SLATE, 6)
            .define('S', Items.DEEPSLATE)
            .define('A', HexItems.AMETHYST_DUST)
            .pattern(" A ")
            .pattern("SSS")
            .unlockedBy("has_item", hasItem(HexItems.AMETHYST_DUST)).save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.TOOLS, HexItems.JEWELER_HAMMER)
            .define('I', ingredients.ironIngot())
            .define('N', ingredients.ironNugget())
            .define('A', Items.AMETHYST_SHARD)
            .define('S', ingredients.stick())
            .pattern("IAN")
            .pattern(" S ")
            .pattern(" S ")
            .unlockedBy("has_item", hasItem(Items.AMETHYST_SHARD)).save(this.output);

        ShapelessRecipeBuilder.shapeless(itemLookup, RecipeCategory.MISC, HexItems.AMETHYST_DUST,
                (int) (MediaConstants.QUENCHED_SHARD_UNIT / MediaConstants.DUST_UNIT) + 1)
            .requires(HexItems.QUENCHED_SHARD)
            .requires(HexItems.AMETHYST_DUST)
            .unlockedBy("has_item", hasItem(HexItems.QUENCHED_SHARD))
            .save(this.output, modLoc("decompose_quenched_shard/dust").toString());
        ShapelessRecipeBuilder.shapeless(itemLookup, RecipeCategory.MISC, Items.AMETHYST_SHARD,
                (int) (MediaConstants.QUENCHED_SHARD_UNIT / MediaConstants.SHARD_UNIT) + 1)
            .requires(HexItems.QUENCHED_SHARD)
            .requires(Items.AMETHYST_SHARD)
            .unlockedBy("has_item", hasItem(HexItems.QUENCHED_SHARD))
            .save(this.output, modLoc("decompose_quenched_shard/shard").toString());
        ShapelessRecipeBuilder.shapeless(itemLookup, RecipeCategory.MISC, HexItems.CHARGED_AMETHYST,
                (int) (MediaConstants.QUENCHED_SHARD_UNIT / MediaConstants.CRYSTAL_UNIT) + 1)
            .requires(HexItems.QUENCHED_SHARD)
            .requires(HexItems.CHARGED_AMETHYST)
            .unlockedBy("has_item", hasItem(HexItems.QUENCHED_SHARD))
            .save(this.output, modLoc("decompose_quenched_shard/charged").toString());

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.BUILDING_BLOCKS, HexBlocks.SLATE_BLOCK)
            .define('S', HexItems.SLATE)
            .pattern("S")
            .pattern("S")
            .unlockedBy("has_item", hasItem(HexItems.SLATE))
            .save(this.output, modLoc("slate_block_from_slates").toString());

        ringAll(RecipeCategory.BUILDING_BLOCKS, HexBlocks.SLATE_BLOCK, 8, Blocks.DEEPSLATE, HexItems.AMETHYST_DUST)
            .unlockedBy("has_item", hasItem(HexItems.SLATE)).save(this.output);

        packing(RecipeCategory.BUILDING_BLOCKS, HexItems.AMETHYST_DUST, HexBlocks.AMETHYST_DUST_BLOCK.asItem(), "amethyst_dust",
            false, null);

        ringAll(RecipeCategory.BUILDING_BLOCKS, HexBlocks.SCROLL_PAPER, 8, Items.PAPER, Items.AMETHYST_SHARD)
            .unlockedBy("has_item", hasItem(Items.AMETHYST_SHARD)).save(this.output);

        ShapelessRecipeBuilder.shapeless(itemLookup, RecipeCategory.BUILDING_BLOCKS, HexBlocks.ANCIENT_SCROLL_PAPER, 8)
            .requires(ingredients.dyes().get(DyeColor.BROWN))
            .requires(HexBlocks.SCROLL_PAPER, 8)
            .unlockedBy("has_item", hasItem(HexBlocks.SCROLL_PAPER)).save(this.output);

        stack(RecipeCategory.DECORATIONS, HexBlocks.SCROLL_PAPER_LANTERN, 1, HexBlocks.SCROLL_PAPER, Items.TORCH)
            .unlockedBy("has_item", hasItem(HexBlocks.SCROLL_PAPER)).save(this.output);

        stack(RecipeCategory.DECORATIONS, HexBlocks.ANCIENT_SCROLL_PAPER_LANTERN, 1, HexBlocks.ANCIENT_SCROLL_PAPER, Items.TORCH)
            .unlockedBy("has_item", hasItem(HexBlocks.ANCIENT_SCROLL_PAPER)).save(this.output);

        ShapelessRecipeBuilder.shapeless(itemLookup, RecipeCategory.DECORATIONS, HexBlocks.ANCIENT_SCROLL_PAPER_LANTERN, 8)
            .requires(ingredients.dyes().get(DyeColor.BROWN))
            .requires(HexBlocks.SCROLL_PAPER_LANTERN, 8)
            .unlockedBy("has_item", hasItem(HexBlocks.SCROLL_PAPER_LANTERN))
            .save(this.output, modLoc("ageing_scroll_paper_lantern").toString());

        stack(RecipeCategory.DECORATIONS, HexBlocks.SCONCE, 4,
            Ingredient.of(HexItems.CHARGED_AMETHYST),
            ingredients.copperIngot())
            .unlockedBy("has_item", hasItem(HexItems.CHARGED_AMETHYST)).save(this.output);

        ShapelessRecipeBuilder.shapeless(itemLookup, RecipeCategory.BUILDING_BLOCKS, HexBlocks.EDIFIED_PLANKS, 4)
            .requires(HexTags.Items.EDIFIED_LOGS)
            .unlockedBy("has_item", hasItem(HexTags.Items.EDIFIED_LOGS)).save(this.output);

        for (var entry : EDIFIED_LOG_TO_WOOD.entrySet()) {
            var log = entry.getKey();
            var wood = entry.getValue();
            ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.BUILDING_BLOCKS, wood, 3)
                    .define('W', log)
                    .pattern("WW")
                    .pattern("WW")
                    .unlockedBy("has_item", hasItem(log)).save(this.output);
        }

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.BUILDING_BLOCKS, HexBlocks.EDIFIED_PANEL, 9)
            .define('W', HexTags.Items.EDIFIED_PLANKS)
            .pattern("WWW")
            .pattern("WWW")
            .pattern("WWW")
            .unlockedBy("has_item", hasItem(HexTags.Items.EDIFIED_PLANKS)).save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.BUILDING_BLOCKS, HexBlocks.EDIFIED_TILE, 6)
            .define('W', HexTags.Items.EDIFIED_PLANKS)
            .pattern("WW ")
            .pattern("W W")
            .pattern(" WW")
            .unlockedBy("has_item", hasItem(HexTags.Items.EDIFIED_PLANKS)).save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.REDSTONE, HexBlocks.EDIFIED_DOOR, 3)
            .define('W', HexTags.Items.EDIFIED_PLANKS)
            .pattern("WW")
            .pattern("WW")
            .pattern("WW")
            .unlockedBy("has_item", hasItem(HexTags.Items.EDIFIED_PLANKS)).save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.REDSTONE, HexBlocks.EDIFIED_TRAPDOOR, 2)
            .define('W', HexTags.Items.EDIFIED_PLANKS)
            .pattern("WWW")
            .pattern("WWW")
            .unlockedBy("has_item", hasItem(HexTags.Items.EDIFIED_PLANKS)).save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.BUILDING_BLOCKS, HexBlocks.EDIFIED_STAIRS, 4)
            .define('W', HexTags.Items.EDIFIED_PLANKS)
            .pattern("W  ")
            .pattern("WW ")
            .pattern("WWW")
            .unlockedBy("has_item", hasItem(HexTags.Items.EDIFIED_PLANKS)).save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.BUILDING_BLOCKS, HexBlocks.EDIFIED_FENCE, 3)
                .define('W', HexTags.Items.EDIFIED_PLANKS)
                .define('S', Items.STICK)
                .pattern("WSW")
                .pattern("WSW")
                .unlockedBy("has_item", hasItem(HexTags.Items.EDIFIED_PLANKS)).save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.BUILDING_BLOCKS, HexBlocks.EDIFIED_FENCE_GATE, 1)
                .define('W', HexTags.Items.EDIFIED_PLANKS)
                .define('S', Items.STICK)
                .pattern("SWS")
                .pattern("SWS")
                .unlockedBy("has_item", hasItem(HexTags.Items.EDIFIED_PLANKS)).save(this.output);


        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.BUILDING_BLOCKS, HexBlocks.EDIFIED_SLAB, 6)
            .define('W', HexTags.Items.EDIFIED_PLANKS)
            .pattern("WWW")
            .unlockedBy("has_item", hasItem(HexTags.Items.EDIFIED_PLANKS)).save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.REDSTONE, HexBlocks.EDIFIED_PRESSURE_PLATE, 1)
            .define('W', HexTags.Items.EDIFIED_PLANKS)
            .pattern("WW")
            .unlockedBy("has_item", hasItem(HexTags.Items.EDIFIED_PLANKS)).save(this.output);

        ShapelessRecipeBuilder.shapeless(itemLookup, RecipeCategory.REDSTONE, HexBlocks.EDIFIED_BUTTON)
            .requires(HexTags.Items.EDIFIED_PLANKS)
            .unlockedBy("has_item", hasItem(HexTags.Items.EDIFIED_PLANKS)).save(this.output);

        var enlightenment = HexAdvancements.ENLIGHTEN;
        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.REDSTONE, HexBlocks.IMPETUS_EMPTY)
            .define('B', Items.IRON_BARS)
            .define('A', HexItems.CHARGED_AMETHYST)
            .define('S', HexBlocks.SLATE_BLOCK)
            .define('P', Items.PURPUR_BLOCK)
            .pattern("PSS")
            .pattern("BAB")
            .pattern("SSP")
            .unlockedBy("enlightenment", enlightenment).save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.REDSTONE, HexBlocks.EMPTY_DIRECTRIX)
            .define('C', Items.COMPARATOR)
            .define('O', Items.OBSERVER)
            .define('A', HexItems.CHARGED_AMETHYST)
            .define('S', HexBlocks.SLATE_BLOCK)
            .pattern("CSS")
            .pattern("OAO")
            .pattern("SSC")
            .unlockedBy("enlightenment", enlightenment).save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.REDSTONE, HexBlocks.AKASHIC_BOOKSHELF)
            .define('L', HexTags.Items.EDIFIED_LOGS)
            .define('P', HexTags.Items.EDIFIED_PLANKS)
            .define('C', Items.BOOK)
            /*this is the*/.pattern("LPL") // and what i have for you today is
            .pattern("CCC")
            .pattern("LPL")
            .unlockedBy("enlightenment", enlightenment).save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.REDSTONE, HexBlocks.AKASHIC_LIGATURE, 4)
            .define('L', HexTags.Items.EDIFIED_LOGS)
            .define('P', HexTags.Items.EDIFIED_PLANKS)
            .define('1', HexItems.AMETHYST_DUST)
            .define('2', Items.AMETHYST_SHARD)
            .define('3', HexItems.CHARGED_AMETHYST)
            .pattern("LPL")
            .pattern("123")
            .pattern("LPL")
            .unlockedBy("enlightenment", enlightenment).save(this.output);

        // Stone sets
        stoneSet(HexBlocks.SLATE_BLOCK.asItem(), HexBlocks.SLATE_BRICKS.asItem(), HexBlocks.SLATE_BRICKS_SMALL.asItem(), HexBlocks.SLATE_TILES.asItem(), HexBlocks.SLATE_PILLAR.asItem());
        stoneSet(Blocks.AMETHYST_BLOCK.asItem(), HexBlocks.AMETHYST_BRICKS.asItem(), HexBlocks.AMETHYST_BRICKS_SMALL.asItem(), HexBlocks.AMETHYST_TILES.asItem(), HexBlocks.AMETHYST_PILLAR.asItem());
        stoneSet(HexBlocks.QUENCHED_ALLAY.asItem(), HexBlocks.QUENCHED_ALLAY_BRICKS.asItem(), HexBlocks.QUENCHED_ALLAY_BRICKS_SMALL.asItem(), HexBlocks.QUENCHED_ALLAY_TILES.asItem(), null);

        // Stone sets in stonecutter
        stoneCutterFromTag(HexTags.Items.SLATE_BLOCKS, HexBlocks.SLATE_BRICKS.asItem(), HexBlocks.SLATE_BRICKS_SMALL.asItem(), HexBlocks.SLATE_TILES.asItem(), HexBlocks.SLATE_PILLAR.asItem());
        stoneCutterFromTag(HexTags.Items.AMETHYST_BLOCKS, HexBlocks.AMETHYST_BRICKS.asItem(), HexBlocks.AMETHYST_BRICKS_SMALL.asItem(), HexBlocks.AMETHYST_TILES.asItem(), HexBlocks.AMETHYST_PILLAR.asItem());
        stoneCutterFromTag(HexTags.Items.QUENCHED_ALLAY_BLOCKS, HexBlocks.QUENCHED_ALLAY_BRICKS.asItem(), HexBlocks.QUENCHED_ALLAY_BRICKS_SMALL.asItem(), HexBlocks.QUENCHED_ALLAY_TILES.asItem());

        // Slate & Amethyst block set
        ShapelessRecipeBuilder.shapeless(itemLookup, RecipeCategory.BUILDING_BLOCKS, HexBlocks.SLATE_AMETHYST_BRICKS.asItem(), 2)
                .requires(HexBlocks.SLATE_BRICKS)
                .requires(HexBlocks.AMETHYST_BRICKS)
                .unlockedBy("has_item", has(HexBlocks.SLATE)).save(this.output);

        ShapelessRecipeBuilder.shapeless(itemLookup, RecipeCategory.BUILDING_BLOCKS, HexBlocks.SLATE_AMETHYST_BRICKS_SMALL.asItem(), 2)
                .requires(HexBlocks.SLATE_BRICKS_SMALL)
                .requires(HexBlocks.AMETHYST_BRICKS_SMALL)
                .unlockedBy("has_item", has(HexBlocks.SLATE)).save(this.output);

        ShapelessRecipeBuilder.shapeless(itemLookup, RecipeCategory.BUILDING_BLOCKS, HexBlocks.SLATE_AMETHYST_TILES.asItem(), 2)
                .requires(HexBlocks.SLATE_TILES)
                .requires(HexBlocks.AMETHYST_TILES)
                .unlockedBy("has_item", has(HexBlocks.SLATE)).save(this.output);

        ShapelessRecipeBuilder.shapeless(itemLookup, RecipeCategory.BUILDING_BLOCKS, HexBlocks.SLATE_AMETHYST_PILLAR.asItem(), 2)
                .requires(HexBlocks.SLATE_PILLAR)
                .requires(HexBlocks.AMETHYST_PILLAR)
                .unlockedBy("has_item", has(HexBlocks.SLATE)).save(this.output);

        new BrainsweepRecipeBuilder(StateIngredientHelper.of(Blocks.AMETHYST_BLOCK),
            new VillagerIngredient(null, null, 3),
            Blocks.BUDDING_AMETHYST.defaultBlockState(), MediaConstants.CRYSTAL_UNIT * 10)
            .unlockedBy("enlightenment", enlightenment)
            .save(this.output, recipeKey(modLoc("brainsweep/budding_amethyst")));

        new BrainsweepRecipeBuilder(StateIngredientHelper.of(HexBlocks.IMPETUS_EMPTY),
            new VillagerIngredient(BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.TOOLSMITH), null, 2),
            HexBlocks.IMPETUS_RIGHTCLICK.defaultBlockState(), MediaConstants.CRYSTAL_UNIT * 10)
            .unlockedBy("enlightenment", enlightenment)
            .save(this.output, recipeKey(modLoc("brainsweep/impetus_rightclick")));

        new BrainsweepRecipeBuilder(StateIngredientHelper.of(HexBlocks.IMPETUS_EMPTY),
            new VillagerIngredient(BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.FLETCHER), null, 2),
            HexBlocks.IMPETUS_LOOK.defaultBlockState(), MediaConstants.CRYSTAL_UNIT * 10)
            .unlockedBy("enlightenment", enlightenment)
            .save(this.output, recipeKey(modLoc("brainsweep/impetus_look")));

        new BrainsweepRecipeBuilder(StateIngredientHelper.of(HexBlocks.IMPETUS_EMPTY),
            new VillagerIngredient(BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.CLERIC), null, 2),
            HexBlocks.IMPETUS_REDSTONE.defaultBlockState(), MediaConstants.CRYSTAL_UNIT * 10)
            .unlockedBy("enlightenment", enlightenment)
            .save(this.output, recipeKey(modLoc("brainsweep/impetus_storedplayer")));

        new BrainsweepRecipeBuilder(StateIngredientHelper.of(HexBlocks.EMPTY_DIRECTRIX),
            new VillagerIngredient(BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.MASON), null, 1),
            HexBlocks.DIRECTRIX_REDSTONE.defaultBlockState(), MediaConstants.CRYSTAL_UNIT * 10)
            .unlockedBy("enlightenment", enlightenment)
            .save(this.output, recipeKey(modLoc("brainsweep/directrix_redstone")));

        new BrainsweepRecipeBuilder(StateIngredientHelper.of(HexBlocks.EMPTY_DIRECTRIX),
                new VillagerIngredient(BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.SHEPHERD), null, 1),
                HexBlocks.DIRECTRIX_BOOLEAN.defaultBlockState(), MediaConstants.CRYSTAL_UNIT * 10)
                .unlockedBy("enlightenment", enlightenment)
                .save(this.output, recipeKey(modLoc("brainsweep/directrix_boolean")));

        new BrainsweepRecipeBuilder(StateIngredientHelper.of(HexBlocks.AKASHIC_LIGATURE),
            new VillagerIngredient(BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.LIBRARIAN), null, 5),
            HexBlocks.AKASHIC_RECORD.defaultBlockState(), MediaConstants.CRYSTAL_UNIT * 10)
            .unlockedBy("enlightenment", enlightenment)
            .save(this.output, recipeKey(modLoc("brainsweep/akashic_record")));

        // Temporary tests
        new BrainsweepRecipeBuilder(StateIngredientHelper.of(Blocks.AMETHYST_BLOCK),
            new EntityTypeIngredient(EntityType.ALLAY),
            HexBlocks.QUENCHED_ALLAY.defaultBlockState(), MediaConstants.CRYSTAL_UNIT)
            .unlockedBy("enlightenment", enlightenment)
            .save(this.output, recipeKey(modLoc("brainsweep/quench_allay")));
    }

    private void staffRecipe(ItemStaff staff, Item plank) {
        staffRecipe(staff, Ingredient.of(plank));
    }

    private void staffRecipe(ItemStaff staff, Ingredient plank) {
        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.TOOLS, staff)
            .define('W', plank)
            .define('S', Items.STICK)
            .define('A', HexItems.CHARGED_AMETHYST)
            .pattern(" SA")
            .pattern(" WS")
            .pattern("S  ")
            .unlockedBy("has_item", hasItem(HexItems.CHARGED_AMETHYST))
            .save(this.output);
    }

    private void gayRecipe(ItemPridePigment.Type type, Ingredient material) {
        var colorizer = HexItems.PRIDE_PIGMENTS.get(type);
        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.MISC, colorizer)
            .define('D', HexItems.AMETHYST_DUST)
            .define('C', material)
            .pattern(" D ")
            .pattern("DCD")
            .pattern(" D ")
            .unlockedBy("has_item", hasItem(HexItems.AMETHYST_DUST))
            .save(this.output);
    }

    private static String itemPath(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    private void specialRecipe(RecipeSerializer<?> serializer) {
        var name = BuiltInRegistries.RECIPE_SERIALIZER.getKey(serializer);
        Supplier<Recipe<?>> recipe = serializer == SealThingsRecipe.FOCUS_SERIALIZER
            ? () -> new SealThingsRecipe(CraftingBookCategory.MISC, SealThingsRecipe.Sealee.FOCUS)
            : () -> new SealThingsRecipe(CraftingBookCategory.MISC, SealThingsRecipe.Sealee.SPELLBOOK);
        SpecialRecipeBuilder.special(recipe).save(this.output, HexAPI.MOD_ID + ":dynamic" + name.getPath());
    }

    private void stoneSet(Item base, Item bricks, Item smallBricks, Item tiles, @Nullable Item pillar) {
        // Bricks from base block
        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.BUILDING_BLOCKS, bricks, 4)
                .define('#', base)
                .pattern("##")
                .pattern("##")
                .unlockedBy("has_item", hasItem(base))
                .save(this.output);

        // Bricks from small bricks
        ShapelessRecipeBuilder.shapeless(itemLookup, RecipeCategory.BUILDING_BLOCKS, bricks)
                .requires(smallBricks)
                .unlockedBy("has_item", hasItem(base))
                .save(this.output, modLoc(itemPath(bricks) + "_from_" + itemPath(smallBricks)).toString());

        // Small bricks from bricks
        ShapelessRecipeBuilder.shapeless(itemLookup, RecipeCategory.BUILDING_BLOCKS, smallBricks)
                .requires(bricks)
                .unlockedBy("has_item", hasItem(base))
                .save(this.output, modLoc(itemPath(smallBricks) + "_from_" + itemPath(bricks)).toString());

        // Tiles from bricks
        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.BUILDING_BLOCKS, tiles, 4)
                .define('#', bricks)
                .pattern("##")
                .pattern("##")
                .unlockedBy("has_item", hasItem(base))
                .save(this.output);

        // Pillar from base block
        if (pillar != null) {
            ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.BUILDING_BLOCKS, pillar, 2)
                    .define('#', base)
                    .pattern("#")
                    .pattern("#")
                    .unlockedBy("has_item", hasItem(base))
                    .save(this.output);
        }
    }

    private void stoneCutterFromTag(TagKey<Item> tagKey, Item ...results) {
        for (Item result : results) {
            SingleItemRecipeBuilder.stonecutting(tag(tagKey), RecipeCategory.BUILDING_BLOCKS, result, 1)
                    .unlockedBy("has_item", hasItem(tagKey))
                    .save(this.output, modLoc("stonecutting/" + itemPath(result)).toString());
        }
    }
}
