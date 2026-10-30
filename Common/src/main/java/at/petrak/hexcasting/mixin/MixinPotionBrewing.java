package at.petrak.hexcasting.mixin;

import at.petrak.hexcasting.common.lib.HexPotions;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.1 replaced the static {@code PotionBrewing.addMix} calls with {@code PotionBrewing.Builder},
 * so the hex potion recipes are appended while the vanilla mix list is being assembled.
 */
@Mixin(PotionBrewing.class)
public class MixinPotionBrewing {
    @Inject(method = "addVanillaMixes", at = @At("TAIL"))
    private static void hex$addHexMixes(PotionBrewing.Builder builder, CallbackInfo ci) {
        HexPotions.addRecipes(builder);
    }
}
