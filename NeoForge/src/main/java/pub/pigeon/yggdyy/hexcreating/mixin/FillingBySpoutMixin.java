package pub.pigeon.yggdyy.hexcreating.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pub.pigeon.yggdyy.hexcreating.create.spout.FlayMindBySpout;

/**
 * Teaches Create's spout to "flay the mind" out of brainsweep-able blocks when it pours liquid soul.
 * Pseudo-mixin so the game still starts without Create installed.
 */
@Pseudo
@Mixin(targets = "com.simibubi.create.content.fluids.spout.FillingBySpout")
public class FillingBySpoutMixin {
    @Inject(method = "canItemBeFilled(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Z",
        at = @At("HEAD"), cancellable = true, remap = false)
    private static void hexcreating$canFlayMind(Level level, ItemStack stack,
        CallbackInfoReturnable<Boolean> cir) {
        if (FlayMindBySpout.isItemFlayMindAble(level, stack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getRequiredAmountForItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/neoforged/neoforge/fluids/FluidStack;)I",
        at = @At("HEAD"), cancellable = true, remap = false)
    private static void hexcreating$requiredSoul(Level level, ItemStack stack, FluidStack fluid,
        CallbackInfoReturnable<Integer> cir) {
        int required = FlayMindBySpout.getRequiredSoul(level, stack, fluid);
        if (required > 0) {
            cir.setReturnValue(required);
        }
    }

    @Inject(method = "fillItem(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/item/ItemStack;Lnet/neoforged/neoforge/fluids/FluidStack;)Lnet/minecraft/world/item/ItemStack;",
        at = @At("HEAD"), cancellable = true, remap = false)
    private static void hexcreating$flayMind(Level level, int amount, ItemStack stack, FluidStack fluid,
        CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = FlayMindBySpout.flayMind(level, amount, stack, fluid);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
