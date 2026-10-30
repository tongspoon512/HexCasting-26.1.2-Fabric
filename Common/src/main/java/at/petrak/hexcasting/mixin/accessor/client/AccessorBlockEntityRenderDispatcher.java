package at.petrak.hexcasting.mixin.accessor.client;

import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockEntityRenderDispatcher.class)
public interface AccessorBlockEntityRenderDispatcher {
    @Accessor("blockModelResolver")
    BlockModelResolver hex$getBlockModelResolver();
}
