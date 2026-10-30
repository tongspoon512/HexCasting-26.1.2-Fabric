package at.petrak.hexcasting.xplat;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 26.1 removed {@code net.minecraft.client.renderer.item.ItemPropertyFunction} along with the old
 * item-model predicate system, but the platform implementations still need a SAM type for the
 * item property registrations in {@code RegisterClientStuff}.
 */
@FunctionalInterface
public interface ItemPropertyFunction {
    float apply(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed);
}
