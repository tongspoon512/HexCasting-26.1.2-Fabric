package at.petrak.hexcasting.mixin.accessor.client;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.ScrollWheelHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface AccessorMouseHandler {
    /**
     * 26.1 moved the scroll accumulation out of MouseHandler into ScrollWheelHandler.
     */
    @Accessor("scrollWheelHandler")
    ScrollWheelHandler hex$getScrollWheelHandler();
}
