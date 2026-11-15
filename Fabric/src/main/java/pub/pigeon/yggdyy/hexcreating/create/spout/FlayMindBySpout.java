package pub.pigeon.yggdyy.hexcreating.create.spout;

import at.petrak.hexcasting.api.misc.MediaConstants;
import at.petrak.hexcasting.common.recipe.BrainsweepRecipe;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import pub.pigeon.yggdyy.hexcreating.fluids.HexCreatingFluids;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets a Create spout "flay the mind" out of a brainsweep-able block by pouring liquid soul on it,
 * mirroring the original hexcreating mechanic.
 */
public final class FlayMindBySpout {
    /** Create stores fluid amounts in droplets: 81 droplets make one millibucket. */
    private static final long DROPLETS_PER_DUST = 81L;

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

    public static boolean isItemFlayMindAble(ServerLevel level, ItemStack stack) {
        return !suitableRecipes(level, stack).isEmpty();
    }

    /**
     * @return the soul droplets required to flay the item, or -1 if this item/fluid combination is
     *     not handled here (the spout then falls back to its normal filling logic).
     */
    public static int getRequiredSoul(ServerLevel level, ItemStack stack, FluidStack fluid) {
        if (!isSoulFluid(fluid)) {
            return -1;
        }
        List<BrainsweepRecipe> recipes = suitableRecipes(level, stack);
        if (recipes.isEmpty()) {
            return -1;
        }
        long required = requiredDroplets(recipes);
        return required <= fluid.getAmount() ? (int) required : -1;
    }

    @Nullable
    public static ItemStack flayMind(ServerLevel level, int amount, ItemStack stack, FluidStack fluid) {
        if (!isSoulFluid(fluid)) {
            return null;
        }
        List<BrainsweepRecipe> recipes = suitableRecipes(level, stack);
        if (recipes.isEmpty()) {
            return null;
        }
        long required = requiredDroplets(recipes);
        if (required != amount) {
            return null;
        }
        BrainsweepRecipe recipe = recipes.get(level.getRandom().nextInt(recipes.size()));
        fluid.decrement(amount);
        stack.shrink(1);
        return recipe.result().getBlock().asItem().getDefaultInstance();
    }

    private static long requiredDroplets(List<BrainsweepRecipe> recipes) {
        long totalCost = 0L;
        for (BrainsweepRecipe recipe : recipes) {
            totalCost += recipe.mediaCost();
        }
        long droplets = totalCost / recipes.size() / MediaConstants.DUST_UNIT * DROPLETS_PER_DUST;
        return Math.max(droplets, DROPLETS_PER_DUST);
    }
}
