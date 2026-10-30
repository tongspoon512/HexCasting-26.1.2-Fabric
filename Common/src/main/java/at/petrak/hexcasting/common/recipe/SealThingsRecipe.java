package at.petrak.hexcasting.common.recipe;

import at.petrak.hexcasting.api.mod.HexTags;
import at.petrak.hexcasting.common.items.storage.ItemFocus;
import at.petrak.hexcasting.common.items.storage.ItemSpellbook;
import at.petrak.hexcasting.common.lib.HexItems;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class SealThingsRecipe extends CustomRecipe {
    public final Sealee sealee;
    private final CraftingBookCategory category;

    // 26.1 removed SimpleCraftingRecipeSerializer; serializers are MapCodec + StreamCodec records now.
    public static final RecipeSerializer<SealThingsRecipe> FOCUS_SERIALIZER = makeSerializer(Sealee.FOCUS);
    public static final RecipeSerializer<SealThingsRecipe> SPELLBOOK_SERIALIZER = makeSerializer(Sealee.SPELLBOOK);

    private static RecipeSerializer<SealThingsRecipe> makeSerializer(Sealee sealee) {
        MapCodec<SealThingsRecipe> mapCodec = RecordCodecBuilder.mapCodec(i -> i.group(
            CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC)
                .forGetter(SealThingsRecipe::category)
        ).apply(i, category -> new SealThingsRecipe(category, sealee)));
        StreamCodec<RegistryFriendlyByteBuf, SealThingsRecipe> streamCodec = StreamCodec.of(
            (buf, recipe) -> buf.writeEnum(recipe.category),
            buf -> new SealThingsRecipe(buf.readEnum(CraftingBookCategory.class), sealee));
        return new RecipeSerializer<>(mapCodec, streamCodec);
    }

    public SealThingsRecipe(CraftingBookCategory category, Sealee sealee) {
        super();
        this.category = category;
        this.sealee = sealee;
    }

    @Override
    public @NotNull CraftingBookCategory category() {
        return this.category;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean foundComb = false;
        boolean foundSealee = false;

        for (int i = 0; i < input.size(); i++) {
            var stack = input.getItem(i);
            if (this.sealee.isCorrectSealee(stack)) {
                if (foundSealee) return false;
                foundSealee = true;
            } else if (stack.is(HexTags.Items.SEAL_MATERIALS)) {
                if (foundComb) return false;
                foundComb = true;
            }
        }

        return foundComb && foundSealee;
    }

    @Override
    public @NotNull ItemStack assemble(CraftingInput input) {
        ItemStack sealee = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            var stack = input.getItem(i);
            if (this.sealee.isCorrectSealee(stack)) {
                sealee = stack.copy();
                break;
            }
        }

        if (!sealee.isEmpty()) {
            this.sealee.seal(sealee);
            sealee.setCount(1);
        }

        return sealee;
    }

    @Override
    public @NotNull RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return switch (this.sealee) {
            case FOCUS -> FOCUS_SERIALIZER;
            case SPELLBOOK -> SPELLBOOK_SERIALIZER;
        };
    }

    public enum Sealee implements StringRepresentable {
        FOCUS,
        SPELLBOOK;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public boolean isCorrectSealee(ItemStack stack) {
            return switch (this) {
                case FOCUS -> stack.is(HexItems.FOCUS)
                    && HexItems.FOCUS.readIotaTag(stack) != null
                    && !ItemFocus.isSealed(stack);
                case SPELLBOOK -> stack.is(HexItems.SPELLBOOK)
                    && HexItems.SPELLBOOK.readIotaTag(stack) != null
                    && !ItemSpellbook.isSealed(stack);
            };
        }

        public void seal(ItemStack stack) {
            switch (this) {
                case FOCUS -> {
                    ItemFocus.seal(stack);
                }
                case SPELLBOOK -> {
                    ItemSpellbook.setSealed(stack, true);
                }
            }
        }
    }
}
