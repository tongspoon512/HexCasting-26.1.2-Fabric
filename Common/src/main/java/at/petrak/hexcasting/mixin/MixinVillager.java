package at.petrak.hexcasting.mixin;

import at.petrak.hexcasting.xplat.IXplatAbstractions;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Prevents the villager from any of its brain goals
@Mixin(Villager.class)
public class MixinVillager {
    @Inject(method = "canBreed", at = @At("HEAD"), cancellable = true)
    private void preventBreeding(CallbackInfoReturnable<Boolean> cir) {
        var self = (Villager) (Object) this;
        if (IXplatAbstractions.INSTANCE.isBrainswept(self)) {
            cir.setReturnValue(false);
        }
    }

    // 26.1 removed Villager#setUnhappy; the angry-villager particles are now only broadcast through event 13.
    @Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
    private void preventUnhappiness(byte id, CallbackInfo ci) {
        if (id == 13) {
            var self = (Villager) (Object) this;
            if (IXplatAbstractions.INSTANCE.isBrainswept(self)) {
                ci.cancel();
            }
        }
    }
}
