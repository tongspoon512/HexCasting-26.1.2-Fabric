package at.petrak.hexcasting.fabric.mixin.client;

import at.petrak.hexcasting.client.model.AltioraLayer;
import at.petrak.hexcasting.mixin.accessor.client.AccessorLivingEntityRenderer;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class FabricPlayerRendererMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void addAltiora(EntityRendererProvider.Context ctx, boolean slim, CallbackInfo ci) {
        @SuppressWarnings("unchecked")
        var parent = (RenderLayerParent<AvatarRenderState, PlayerModel>) (Object) this;
        ((AccessorLivingEntityRenderer) (Object) this).hex$addLayer(new AltioraLayer<>(parent, ctx.getModelSet()));
    }
}
