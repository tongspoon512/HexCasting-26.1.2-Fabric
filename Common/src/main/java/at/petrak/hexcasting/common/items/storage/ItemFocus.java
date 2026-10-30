package at.petrak.hexcasting.common.items.storage;

import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.api.item.IotaHolderItem;
import at.petrak.hexcasting.api.item.VariantItem;
import at.petrak.hexcasting.api.utils.NBTHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

public class ItemFocus extends Item implements IotaHolderItem, VariantItem {
    // 0 = no overlay
    // 1 = unsealed
    // 2 = sealed
    public static final Identifier OVERLAY_PRED = modLoc("overlay_layer");
    public static final Identifier VARIANT_PRED = modLoc("variant");
    public static final int NUM_VARIANTS = 8;

    public static final String TAG_DATA = "data";
    public static final String TAG_SEALED = "sealed";

    public ItemFocus(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public @Nullable CompoundTag readIotaTag(ItemStack stack) {
        return NBTHelper.getCompound(stack, TAG_DATA);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId()
            + (NBTHelper.getBoolean(stack, TAG_SEALED) ? ".sealed" : ""));
    }

    @Override
    public boolean writeable(ItemStack stack) {
        return !isSealed(stack);
    }

    @Override
    public boolean canWrite(ItemStack stack, Iota datum) {
        return datum == null || !isSealed(stack);
    }

    @Override
    public void writeDatum(ItemStack stack, Iota datum) {
        if (datum == null) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.remove(TAG_DATA);
                tag.remove(TAG_SEALED);
            });
        } else if (!isSealed(stack)) {
            NBTHelper.put(stack, TAG_DATA, IotaType.serialize(datum));
        }
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, TooltipDisplay pDisplay,
        Consumer<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        var components = new java.util.ArrayList<Component>();
        IotaHolderItem.appendHoverText(this, pStack, components, pIsAdvanced);
        components.forEach(pTooltipComponents);
    }

    public static boolean isSealed(ItemStack stack) {
        return NBTHelper.getBoolean(stack, TAG_SEALED);
    }

    public static void seal(ItemStack stack) {
        NBTHelper.putBoolean(stack, TAG_SEALED, true);
    }

    @Override
    public int numVariants() {
        return NUM_VARIANTS;
    }

    @Override
    public void setVariant(ItemStack stack, int variant) {
        if (!isSealed(stack))
            NBTHelper.putInt(stack, TAG_VARIANT, clampVariant(variant));
    }
}
