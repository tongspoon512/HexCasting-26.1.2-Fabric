package at.petrak.hexcasting.client.color;

import at.petrak.hexcasting.api.item.IotaHolderItem;
import at.petrak.hexcasting.api.utils.HexUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 26.1 item tints are data driven: {@code assets/<ns>/items/<id>.json} may attach a {@code tints}
 * array to a {@code minecraft:model} node. This source paints the stored iota's color onto the
 * overlay layer of foci, spellbooks and thought knots (previously an {@code ItemColor}).
 */
public final class IotaStorageTintSource implements ItemTintSource {
    public static final IotaStorageTintSource INSTANCE = new IotaStorageTintSource();
    public static final MapCodec<IotaStorageTintSource> MAP_CODEC = MapCodec.unit(INSTANCE);

    private IotaStorageTintSource() {
    }

    @Override
    public int calculate(ItemStack stack, ClientLevel level, LivingEntity entity) {
        if (stack.getItem() instanceof IotaHolderItem holder) {
            return holder.getColor(stack);
        }
        return HexUtils.ERROR_COLOR;
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
