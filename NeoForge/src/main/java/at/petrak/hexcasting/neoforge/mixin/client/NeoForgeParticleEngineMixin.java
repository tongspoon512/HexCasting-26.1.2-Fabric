package at.petrak.hexcasting.neoforge.mixin.client;

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

/**
 * 把咒术粒子加入 ParticleEngine 的渲染顺序表末尾（与 Fabric 侧一致）。
 */
@Mixin(ParticleEngine.class)
public class NeoForgeParticleEngineMixin {
    @Mutable
    @Final
    @Shadow
    private static List<ParticleRenderType> RENDER_ORDER;

    @Inject(at = @At("RETURN"), method = "<clinit>")
    private static void addTypes(CallbackInfo ci) {
        var order = new ArrayList<ParticleRenderType>(RENDER_ORDER);
        order.add(ConjureParticle.CONJURE_RENDER_TYPE);
        RENDER_ORDER = order;
    }
}
