package at.petrak.hexcasting.fabric.mixin.client;

import at.petrak.hexcasting.client.particles.ConjureParticle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ParticleEngine.class)
public class FabricParticleEngineMixin {
    @Mutable
    @Final
    @Shadow
    private static List<ParticleRenderType> RENDER_ORDER;

    @Inject(at = @At("RETURN"), method = "<clinit>")
    private static void addTypes(CallbackInfo ci) {
        // Fabric API turns RENDER_ORDER into a mutable ArrayList at the same injection point and
        // sorts it in place later, so keep the list mutable instead of wrapping it in an
        // ImmutableList.
        var order = new ArrayList<ParticleRenderType>(RENDER_ORDER);
        order.add(ConjureParticle.CONJURE_RENDER_TYPE);
        RENDER_ORDER = order;
    }
}
