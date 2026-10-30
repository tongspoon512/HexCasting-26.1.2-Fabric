package at.petrak.hexcasting.fabric.mixin.client;

import at.petrak.hexcasting.fabric.event.MouseScrollCallback;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class FabricMouseHandlerMixin {
    /**
     * 26.1 reworked the body of onScroll (two-axis accumulation via ScrollWheelHandler), so the old
     * local capture no longer applies. Cancelling at HEAD is equivalent for our use: the listener
     * bails out on its own whenever a screen is open.
     */
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long winptr, double xOff, double yOff, CallbackInfo ci) {
        if (MouseScrollCallback.EVENT.invoker().interact(yOff)) {
            ci.cancel();
        }
    }
}
