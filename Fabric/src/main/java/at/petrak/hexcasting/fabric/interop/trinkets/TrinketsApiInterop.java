package at.petrak.hexcasting.fabric.interop.trinkets;

import at.petrak.hexcasting.api.misc.DiscoveryHandlers;
import at.petrak.hexcasting.common.items.HexBaubleItem;
import at.petrak.hexcasting.common.items.magic.ItemCreativeUnlocker;
import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Trinkets 4 (trinkets_updated) integration. The old 3.x API was stubbed out for 1.21, which meant
 * the lens never contributed its attributes; this version drives the new callback/predicate API.
 */
public class TrinketsApiInterop {
    public static void init() {
        BuiltInRegistries.ITEM.stream().forEach(item -> {
            if (item instanceof HexBaubleItem bauble) {
                TrinketCallback.setCallback(item, new TrinketCallback() {
                    @Override
                    public void forEachTrinketModifier(ItemStack stack, TrinketSlotAccess slot,
                        LivingEntity entity, Identifier id,
                        BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
                        bauble.getHexBaubleAttrs(stack).forEach(consumer);
                    }

                    @Override
                    public boolean canEquip(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity) {
                        return isFaceSlot(slot);
                    }

                    @Override
                    public boolean canEquipFromUse(ItemStack stack, LivingEntity entity) {
                        return true;
                    }
                });
            }
        });

        DiscoveryHandlers.addExtraEquipmentDiscoverer(player -> {
            TrinketAttachment attachment = TrinketsApi.getAttachment(player);
            if (attachment == null) {
                return List.of();
            }
            return attachment.equipped(stack -> !stack.isEmpty(), true).stream()
                .map(TrinketSlotAccess::get)
                .toList();
        });

        DiscoveryHandlers.addDebugItemDiscoverer((player, type) -> {
            TrinketAttachment attachment = TrinketsApi.getAttachment(player);
            if (attachment != null) {
                return attachment.findFirst(
                    stack -> ItemCreativeUnlocker.isDebug(stack, type), true)
                    .map(TrinketSlotAccess::get)
                    .orElse(ItemStack.EMPTY);
            }
            return ItemStack.EMPTY;
        });
    }

    public static void clientInit() {
        // The lens is an eyepiece; the new API has no separate trinket-model registry requirement.
    }

    private static boolean isFaceSlot(TrinketSlotAccess slot) {
        return slot != null && "head".equals(slot.slotType().group()) && "face".equals(slot.slotType().name());
    }
}
