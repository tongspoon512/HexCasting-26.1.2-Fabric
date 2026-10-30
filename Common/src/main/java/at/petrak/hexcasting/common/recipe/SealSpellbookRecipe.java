package at.petrak.hexcasting.common.recipe;

import at.petrak.hexcasting.api.item.IotaHolderItem;
import at.petrak.hexcasting.api.utils.NBTHelper;
import at.petrak.hexcasting.common.items.storage.ItemSpellbook;
import at.petrak.hexcasting.common.lib.HexItems;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SealSpellbookRecipe extends ShapelessRecipe {
    // 26.1 removed SimpleCraftingRecipeSerializer; serializers are MapCodec + StreamCodec records now.
    private static final MapCodec<SealSpellbookRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC)
            .forGetter(SealSpellbookRecipe::category)
    ).apply(i, SealSpellbookRecipe::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, SealSpellbookRecipe> STREAM_CODEC = StreamCodec.of(
        (buf, recipe) -> buf.writeEnum(recipe.category()),
        buf -> new SealSpellbookRecipe(buf.readEnum(CraftingBookCategory.class)));
    public static final RecipeSerializer<SealSpellbookRecipe> SERIALIZER =
        new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private static ItemStack getSealedStack() {
        ItemStack output = new ItemStack(HexItems.SPELLBOOK);
        ItemSpellbook.setSealed(output, true);
        NBTHelper.putString(output, IotaHolderItem.TAG_OVERRIDE_VISUALLY, "any");
        return output;
    }

    private static List<Ingredient> createIngredients() {
        return List.of(
            IXplatAbstractions.INSTANCE.getUnsealedIngredient(new ItemStack(HexItems.SPELLBOOK)),
            Ingredient.of(Items.HONEYCOMB));
    }

    public SealSpellbookRecipe(CraftingBookCategory category) {
        super(new Recipe.CommonInfo(true), new CraftingRecipe.CraftingBookInfo(category, ""),
            ItemStackTemplate.fromNonEmptyStack(getSealedStack()), createIngredients());
    }

    @Override
    public @NotNull ItemStack assemble(CraftingInput inv) {
        ItemStack out = ItemStack.EMPTY;

        for (int i = 0; i < inv.size(); i++) {
            var stack = inv.getItem(i);
            if (stack.is(HexItems.SPELLBOOK)) {
                out = stack.copy();
                break;
            }
        }

        if (!out.isEmpty()) {
            ItemSpellbook.setSealed(out, true);
            out.setCount(1);
        }

        return out;
    }

    @Override
    public @NotNull RecipeSerializer<ShapelessRecipe> getSerializer() {
        @SuppressWarnings("unchecked")
        RecipeSerializer<ShapelessRecipe> serializer =
            (RecipeSerializer<ShapelessRecipe>) (RecipeSerializer<?>) SERIALIZER;
        return serializer;
    }
}
