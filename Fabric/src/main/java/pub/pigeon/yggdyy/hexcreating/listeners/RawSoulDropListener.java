package pub.pigeon.yggdyy.hexcreating.listeners;

import at.petrak.hexcasting.xplat.IXplatAbstractions;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import pub.pigeon.yggdyy.hexcreating.items.RawSoulItem;

/**
 * Drops a raw soul when a mob dies while standing in soul fire.
 */
public class RawSoulDropListener implements ServerLivingEntityEvents.AfterDeath {
    @Override
    public void afterDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        if (!(entity instanceof Mob mob) || mob.level().isClientSide()) {
            return;
        }
        if (IXplatAbstractions.INSTANCE.isBrainswept(mob)) {
            return;
        }
        Vec3 dropPos = mob.position().add(0, 0.2, 0);
        BlockPos pos = BlockPos.containing(dropPos);
        if (!(mob.level() instanceof ServerLevel level) || !level.getBlockState(pos).is(Blocks.SOUL_FIRE)) {
            return;
        }
        int count = level.getRandom().nextFloat() < 0.25f ? 2 : 1;
        for (int i = 0; i < count; i++) {
            ItemEntity drop = new ItemEntity(level, dropPos.x, dropPos.y, dropPos.z,
                RawSoulItem.create(mob.getType()));
            level.addFreshEntity(drop);
        }
    }
}
