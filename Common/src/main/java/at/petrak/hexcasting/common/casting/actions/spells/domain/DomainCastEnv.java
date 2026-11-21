package at.petrak.hexcasting.common.casting.actions.spells.domain;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.api.addldata.ADMediaHolder;
import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.PatternShapeMatch;
import at.petrak.hexcasting.api.casting.area.DomainMediaSource;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.MishapEnvironment;
import at.petrak.hexcasting.api.casting.eval.env.PlayerBasedMishapEnv;
import at.petrak.hexcasting.api.casting.mishaps.Mishap;
import at.petrak.hexcasting.api.casting.mishaps.MishapDisallowedSpell;
import at.petrak.hexcasting.api.pigment.FrozenPigment;
import at.petrak.hexcasting.api.utils.MediaHelper;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Casting environment for a domain's periodic spell releases.
 * <p>
 * Everything must happen inside the domain, the stack is limited to 128 iotas and at most 1000
 * patterns are executed per tick. Player-only and circle-only actions are rejected.
 */
public class DomainCastEnv extends CastingEnvironment {
    private final AreaField.DomainInstance domain;
    private final @Nullable ServerPlayer caster;
    private final MishapEnvironment mishapEnv;

    public DomainCastEnv(AreaField.DomainInstance domain) {
        super(domain.getLevel());
        this.domain = domain;
        this.caster = domain.getCaster();
        this.mishapEnv = this.caster != null
            ? new PlayerBasedMishapEnv(this.caster)
            : new EmptyMishapEnv(this.world);
    }

    @Override
    public void precheckAction(PatternShapeMatch match) throws Mishap {
        Identifier key = this.actionKey(match);
        if (key != null) {
            boolean playerOnly = key.equals(HexAPI.modLoc("get_caster"));
            boolean circleOnly = key.getPath().startsWith("circle/");
            if (playerOnly || circleOnly) {
                throw new MishapDisallowedSpell("disallowed_domain", key);
            }
        }
        super.precheckAction(match);
    }

    @Override
    public int maxOpCount() {
        return Math.min(super.maxOpCount(), AreaField.MAX_OPS_PER_RELEASE);
    }

    @Override
    public int maxStackSize() {
        return AreaField.MAX_STACK_SIZE;
    }

    @Override
    public @Nullable LivingEntity getCastingEntity() {
        return this.caster;
    }

    @Override
    public @Nullable ServerPlayer getCaster() {
        return this.caster;
    }

    @Override
    public MishapEnvironment getMishapEnvironment() {
        return this.mishapEnv;
    }

    @Override
    public Vec3 mishapSprayPos() {
        return this.domain.getShape().getCenter();
    }

    @Override
    protected long extractMediaEnvironment(long cost, boolean simulate) {
        DomainMediaSource source = this.domain.getMediaSource();
        if (source != null) {
            long taken = source.extractMedia(cost, simulate);
            return Math.max(cost - taken, 0L);
        }
        if (this.caster == null) {
            return cost;
        }
        long left = cost;
        for (ADMediaHolder holder : MediaHelper.scanPlayerForMediaStuff(this.caster)) {
            left -= MediaHelper.extractMedia(holder, left, false, simulate);
            if (left <= 0) {
                break;
            }
        }
        return Math.max(left, 0L);
    }

    @Override
    protected boolean isVecInRangeEnvironment(Vec3 vec) {
        return this.domain.getShape().contains(vec);
    }

    @Override
    protected boolean hasEditPermissionsAtEnvironment(BlockPos pos) {
        return this.caster != null
            && this.caster.gameMode.getGameModeForPlayer() != GameType.ADVENTURE
            && this.world.mayInteract(this.caster, pos);
    }

    @Override
    public InteractionHand getCastingHand() {
        return InteractionHand.MAIN_HAND;
    }

    @Override
    public List<ItemStack> getUsableStacks(StackDiscoveryMode mode) {
        return this.caster != null
            ? this.getUsableStacksForPlayer(mode, InteractionHand.MAIN_HAND, this.caster)
            : List.of();
    }

    @Override
    public List<HeldItemInfo> getPrimaryStacks() {
        if (this.caster == null) {
            return List.of(
                new HeldItemInfo(ItemStack.EMPTY, InteractionHand.MAIN_HAND),
                new HeldItemInfo(ItemStack.EMPTY, InteractionHand.OFF_HAND));
        }
        return this.getPrimaryStacksForPlayer(InteractionHand.MAIN_HAND, this.caster);
    }

    @Override
    public boolean replaceItem(Predicate<ItemStack> stackOk, ItemStack replaceWith,
                               @Nullable InteractionHand hand) {
        return this.caster != null && this.replaceItemForPlayer(stackOk, replaceWith, hand, this.caster);
    }

    @Override
    public FrozenPigment getPigment() {
        return this.caster != null
            ? IXplatAbstractions.INSTANCE.getPigment(this.caster)
            : FrozenPigment.DEFAULT.get();
    }

    @Override
    public @Nullable FrozenPigment setPigment(@Nullable FrozenPigment pigment) {
        return this.caster != null ? IXplatAbstractions.INSTANCE.setPigment(this.caster, pigment) : null;
    }

    @Override
    public void produceParticles(ParticleSpray particles, FrozenPigment colorizer) {
        particles.sprayParticles(this.world, colorizer);
    }

    @Override
    public void printMessage(net.minecraft.network.chat.Component message) {
        if (this.caster != null) {
            this.caster.sendSystemMessage(message);
        }
    }

    private static class EmptyMishapEnv extends MishapEnvironment {
        private EmptyMishapEnv(ServerLevel level) {
            super(level, null);
        }

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
    }
}
