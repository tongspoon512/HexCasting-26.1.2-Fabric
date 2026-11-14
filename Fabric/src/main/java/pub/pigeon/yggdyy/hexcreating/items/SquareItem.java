package pub.pigeon.yggdyy.hexcreating.items;

import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.api.casting.iota.PatternIota;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.api.item.IotaHolderItem;
import at.petrak.hexcasting.api.utils.NBTHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A board square. It only accepts pattern iotas, matching the upstream behaviour.
 */
public class SquareItem extends Item implements IotaHolderItem {
    public static final String TAG_IOTA = "iota";

    public SquareItem(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable CompoundTag readIotaTag(ItemStack stack) {
        return NBTHelper.getCompound(stack, TAG_IOTA);
    }

    @Override
    public boolean writeable(ItemStack stack) {
        return readIotaTag(stack) == null;
    }

    @Override
    public boolean canWrite(ItemStack stack, @Nullable Iota iota) {
        return iota != null && Iota.typesMatch(iota, new PatternIota(HexPattern.fromAngles("w", HexDir.EAST)));
    }

    @Override
    public void writeDatum(ItemStack stack, @Nullable Iota iota) {
        if (canWrite(stack, iota)) {
            NBTHelper.put(stack, TAG_IOTA, IotaType.serialize(iota));
        }
    }
}
