package at.petrak.hexcasting.fabric.mixin.client;

import at.petrak.hexcasting.fabric.client.FabricParticleFactories;
import net.minecraft.client.particle.ParticleResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleResources.class)
public class FabricParticleResourcesMixin {
    @Inject(method = "registerProviders", at = @At("RETURN"))
    private void hexcasting$registerFactories(CallbackInfo ci) {
        FabricParticleFactories.register((ParticleResources) (Object) this);
    }
}
