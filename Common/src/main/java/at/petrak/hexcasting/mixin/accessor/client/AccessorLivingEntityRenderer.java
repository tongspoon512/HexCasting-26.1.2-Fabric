package at.petrak.hexcasting.mixin.accessor.client;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * {@code addLayer} lives on LivingEntityRenderer (protected final) and is not redeclared by
 * AvatarRenderer, so the invoker has to target the declaring class.
 */
@Mixin(LivingEntityRenderer.class)
public interface AccessorLivingEntityRenderer {
    @Invoker("addLayer")
    boolean hex$addLayer(RenderLayer<?, ?> layer);
}
