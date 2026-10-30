package dev.emi.trinkets.api;

import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;

/**
 * Stub of trinkets' TrinketComponent.
 */
public interface TrinketComponent {
    List<Tuple<SlotReference, ItemStack>> getEquipped(Predicate<ItemStack> filter);
}
