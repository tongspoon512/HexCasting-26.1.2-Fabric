package at.petrak.hexcasting.api.pigment;

import at.petrak.hexcasting.common.lib.HexItems;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * A snapshot of a pigment item and its owner.
 * <p>
 * Due to capabilities being really slow to query many times a tick on Forge, this returns a colorizer <i>supplier</i>.
 * Get it once, and then query it a lot.
 */
public record FrozenPigment(ItemStack item, UUID owner) {

    public static final String TAG_STACK = "stack";
    public static final String TAG_OWNER = "owner";

    public static final Supplier<FrozenPigment> DEFAULT =
        () -> new FrozenPigment(new ItemStack(HexItems.DEFAULT_PIGMENT), Util.NIL_UUID);

    public static final Supplier<FrozenPigment> ANCIENT =
        () -> new FrozenPigment(new ItemStack(HexItems.ANCIENT_PIGMENT), Util.NIL_UUID);

    public CompoundTag serializeToNBT() {
        var out = new CompoundTag();
        out.store(TAG_STACK, ItemStack.CODEC, this.item);
        out.store(TAG_OWNER, UUIDUtil.CODEC, this.owner);
        return out;
    }

    public static FrozenPigment fromNBT(CompoundTag tag) {
        if (tag.isEmpty()) {
            return FrozenPigment.DEFAULT.get();
        }
        var stack = tag.read(TAG_STACK, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        var uuid = tag.read(TAG_OWNER, UUIDUtil.CODEC).orElse(Util.NIL_UUID);
        return new FrozenPigment(stack, uuid);
    }

    public ColorProvider getColorProvider() {
        return IXplatAbstractions.INSTANCE.getColorProvider(this);
    }
}
