package at.petrak.hexcasting.mixin.client;

import at.petrak.hexcasting.api.client.ClientRenderHelper;
import at.petrak.hexcasting.client.model.HexAvatarRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class MixinAvatarRenderer {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    public void hex$extractPlayer(Avatar entity, net.minecraft.client.renderer.entity.state.AvatarRenderState state,
                                  float partialTick, CallbackInfo ci) {
        if (entity instanceof Player player) {
            HexAvatarRenderState duck = (HexAvatarRenderState) (Object) state;
            duck.hex$setPlayer(player);
            duck.hex$setPartialTick(partialTick);
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"))
    public void hex$onSubmit(LivingEntityRenderState state, PoseStack ps, SubmitNodeCollector collector,
                             CameraRenderState cameraState, CallbackInfo ci) {
        HexAvatarRenderState duck = (HexAvatarRenderState) (Object) state;
        Player player = duck.hex$getPlayer();
        if (player != null) {
            ClientRenderHelper.renderCastingStack(ps, collector, player, duck.hex$getPartialTick());
        }
    }
}
