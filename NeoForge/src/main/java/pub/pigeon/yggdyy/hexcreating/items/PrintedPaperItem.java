package pub.pigeon.yggdyy.hexcreating.items;

import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.api.item.IotaHolderItem;
import at.petrak.hexcasting.api.utils.NBTHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A read-only iota carrier produced by the board.
 */
public class PrintedPaperItem extends Item implements IotaHolderItem {
    public static final String TAG_IOTA = "iota";

    public PrintedPaperItem(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable CompoundTag readIotaTag(ItemStack stack) {
        return NBTHelper.getCompound(stack, TAG_IOTA);
    }

    @Override
    public boolean writeable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canWrite(ItemStack stack, @Nullable Iota iota) {
        return false;
    }

    @Override
    public void writeDatum(ItemStack stack, @Nullable Iota iota) {
        if (iota != null) {
            NBTHelper.put(stack, TAG_IOTA, IotaType.serialize(iota));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
        Consumer<Component> tooltip, TooltipFlag flag) {
        var lines = new java.util.ArrayList<Component>();
        IotaHolderItem.appendHoverText(this, stack, lines, flag);
        lines.forEach(tooltip);
    }
}
