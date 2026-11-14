package pub.pigeon.yggdyy.hexcreating.blocks.analytical_engine;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.MishapEnvironment;
import at.petrak.hexcasting.api.pigment.FrozenPigment;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Casting environment for an analytical engine. It deliberately exposes no caster, no inventory and
 * no held items, so patterns that need a player context fail as a mishap instead of silently
 * performing player-only behaviour.
 */
public class AnalyticalEngineCastEnv extends CastingEnvironment {
    public static final double RANGE = 32.0;

    private final AnalyticalEngineControllerBlockEntity controller;

    public AnalyticalEngineCastEnv(AnalyticalEngineControllerBlockEntity controller, ServerLevel level) {
        super(level);
        this.controller = controller;
    }

    @Override
    public @Nullable LivingEntity getCastingEntity() {
        return null;
    }

    @Override
    public MishapEnvironment getMishapEnvironment() {
        return new MishapEnvironment(this.world, null) {
            @Override
            public void yeetHeldItemsTowards(Vec3 targetPos) {
            }

            @Override
            public void dropHeldItems() {
            }

            @Override
            public void drown() {
            }

            @Override
            public void damage(float healthProportion) {
            }

            @Override
            public void removeXp(int amount) {
            }

            @Override
            public void blind(int ticks) {
            }
        };
    }

    @Override
    public Vec3 mishapSprayPos() {
        return Vec3.atCenterOf(this.controller.getBlockPos());
    }

    @Override
    protected long extractMediaEnvironment(long cost, boolean simulate) {
        long available = this.controller.getMediaBuffer();
        long taken = Math.min(Math.max(0L, cost), available);
        if (!simulate) {
            this.controller.setMediaBuffer(available - taken);
        }
        return cost - taken;
    }

    @Override
    protected boolean isVecInRangeEnvironment(Vec3 vec) {
        return vec.distanceToSqr(Vec3.atCenterOf(this.controller.getBlockPos())) <= RANGE * RANGE;
    }

    @Override
    protected boolean hasEditPermissionsAtEnvironment(BlockPos pos) {
        return true;
    }

    @Override
    public InteractionHand getCastingHand() {
        return InteractionHand.MAIN_HAND;
    }

    @Override
    public List<ItemStack> getUsableStacks(StackDiscoveryMode mode) {
        return List.of();
    }

    @Override
    public List<HeldItemInfo> getPrimaryStacks() {
        return List.of(new HeldItemInfo(ItemStack.EMPTY, InteractionHand.MAIN_HAND),
            new HeldItemInfo(ItemStack.EMPTY, InteractionHand.OFF_HAND));
    }

    @Override
    public boolean replaceItem(Predicate<ItemStack> stackOk, ItemStack replaceWith,
                               @Nullable InteractionHand hand) {
        return false;
    }

    @Override
    public FrozenPigment getPigment() {
        return FrozenPigment.DEFAULT.get();
    }

    @Override
    public @Nullable FrozenPigment setPigment(@Nullable FrozenPigment pigment) {
        return null;
    }

    @Override
    public void produceParticles(ParticleSpray particles, FrozenPigment colorizer) {
        // Machines do not spray pigment.
    }

    @Override
    public void printMessage(Component message) {
        // No chat spam from the machine.
    }
}
