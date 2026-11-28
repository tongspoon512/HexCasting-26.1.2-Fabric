package pub.pigeon.yggdyy.hexcreating.create.spout;

import at.petrak.hexcasting.api.misc.MediaConstants;
import at.petrak.hexcasting.common.recipe.BrainsweepRecipe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import pub.pigeon.yggdyy.hexcreating.fluids.HexCreatingFluids;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets a Create spout "flay the mind" out of a brainsweep-able block by pouring liquid soul on it,
 * mirroring the original hexcreating mechanic.
 */
public final class FlayMindBySpout {
    /** 1 紫水晶粉＝81 mB；Create（NeoForge）的流体量本身就是 mB。 */
    private static final int MB_PER_DUST = 81;

    private FlayMindBySpout() {
    }

    public static boolean isSoulFluid(FluidStack fluid) {
        return fluid.getFluid() == HexCreatingFluids.SOUL || fluid.getFluid() == HexCreatingFluids.FLOWING_SOUL;
    }

    public static List<BrainsweepRecipe> suitableRecipes(ServerLevel level, ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return List.of();
        }
        BlockState state = blockItem.getBlock().defaultBlockState();
        List<BrainsweepRecipe> recipes = new ArrayList<>();
        for (RecipeHolder<?> holder : level.recipeAccess().getRecipes()) {
            if (holder.value() instanceof BrainsweepRecipe recipe
                && recipe.blockIn().test(state)
                && !recipe.result().getBlock().asItem().equals(Items.AIR)) {
                recipes.add(recipe);
            }
        }
        return recipes;
    }

    private static List<BrainsweepRecipe> suitableRecipes(Level level, ItemStack stack) {
        return level instanceof ServerLevel serverLevel ? suitableRecipes(serverLevel, stack) : List.of();
    }

    public static boolean isItemFlayMindAble(Level level, ItemStack stack) {
        return !suitableRecipes(level, stack).isEmpty();
    }

    /**
     * @return the soul amount (mB) required to flay the item, or -1 if this item/fluid combination
     *     is not handled here (the spout then falls back to its normal filling logic).
     */
    public static int getRequiredSoul(Level level, ItemStack stack, FluidStack fluid) {
        if (!isSoulFluid(fluid)) {
            return -1;
        }
        List<BrainsweepRecipe> recipes = suitableRecipes(level, stack);
        if (recipes.isEmpty()) {
            return -1;
        }
        long required = requiredAmount(recipes);
        return required <= fluid.getAmount() ? (int) required : -1;
    }

    @Nullable
    public static ItemStack flayMind(Level level, int amount, ItemStack stack, FluidStack fluid) {
        if (!isSoulFluid(fluid)) {
            return null;
        }
        List<BrainsweepRecipe> recipes = suitableRecipes(level, stack);
        if (recipes.isEmpty()) {
            return null;
        }
        long required = requiredAmount(recipes);
        if (required != amount) {
            return null;
        }
        BrainsweepRecipe recipe = recipes.get(level.getRandom().nextInt(recipes.size()));
        fluid.shrink(amount);
        stack.shrink(1);
        return recipe.result().getBlock().asItem().getDefaultInstance();
    }

    private static long requiredAmount(List<BrainsweepRecipe> recipes) {
        long totalCost = 0L;
        for (BrainsweepRecipe recipe : recipes) {
            totalCost += recipe.mediaCost();
        }
        long amount = totalCost / recipes.size() / MediaConstants.DUST_UNIT * MB_PER_DUST;
        return Math.max(amount, MB_PER_DUST);
    }
}
