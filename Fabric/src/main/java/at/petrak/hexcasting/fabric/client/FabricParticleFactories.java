package at.petrak.hexcasting.fabric.client;

import at.petrak.hexcasting.common.lib.HexParticles;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleResources;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

import java.util.function.Function;

/**
 * 26.1 removed Fabric's particle factory registry; providers now live in {@link ParticleResources}.
 */
public final class FabricParticleFactories {
    private FabricParticleFactories() {
    }

    public static void register(ParticleResources resources) {
        HexParticles.FactoryHandler.registerFactories(new HexParticles.FactoryHandler.Consumer() {
            @Override
            public <T extends ParticleOptions> void register(ParticleType<T> type,
                Function<SpriteSet, ParticleProvider<T>> constructor) {
                resources.register(type, constructor::apply);
            }
        });
    }
}
