package at.petrak.hexcasting.common.items.storage;

import at.petrak.hexcasting.api.casting.iota.DoubleIota;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.api.item.IotaHolderItem;
import at.petrak.hexcasting.api.utils.NBTHelper;
import at.petrak.hexcasting.common.lib.HexSounds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class ItemAbacus extends Item implements IotaHolderItem {
    public static final String TAG_VALUE = "value";

    public ItemAbacus(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public @Nullable
    CompoundTag readIotaTag(ItemStack stack) {
        var datum = new DoubleIota(NBTHelper.getDouble(stack, TAG_VALUE));
        return IotaType.serialize(datum);
    }

    @Override
    public boolean writeable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canWrite(ItemStack stack, Iota datum) {
        return false;
    }

    @Override
    public void writeDatum(ItemStack stack, Iota datum) {
        // nope
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            double oldNum = NBTHelper.getDouble(stack, TAG_VALUE);
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(TAG_VALUE));

            player.playSound(HexSounds.ABACUS_SHAKE, 1f, 1f);

            var key = "hexcasting.tooltip.abacus.reset";
            if (oldNum == 69) {
                key += ".nice";
            }
            player.sendOverlayMessage(Component.translatable(key));

            return world.isClientSide() ? InteractionResult.SUCCESS.heldItemTransformedTo(stack)
                : InteractionResult.CONSUME.heldItemTransformedTo(stack);
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, TooltipDisplay pDisplay,
        Consumer<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        var components = new java.util.ArrayList<Component>();
        IotaHolderItem.appendHoverText(this, pStack, components, pIsAdvanced);
        components.forEach(pTooltipComponents);
    }
}
