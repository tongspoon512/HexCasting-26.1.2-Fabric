package at.petrak.hexcasting.mixin.accessor;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractArrow.class)
public interface AccessorAbstractArrow {
    /**
     * 26.1 replaced the inGround field with a synched-data backed isInGround() accessor.
     */
    @Invoker("isInGround")
    boolean hex$isInGround();
}
