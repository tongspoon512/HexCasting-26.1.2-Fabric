package dev.emi.trinkets.api;

import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Stub of trinkets' Trinket interface.
 */
public interface Trinket {
    default Multimap<Holder<Attribute>, AttributeModifier> getModifiers(ItemStack stack, SlotReference slot,
                                                                       LivingEntity entity, UUID uuid) {
        return com.google.common.collect.ArrayListMultimap.create();
    }
}
