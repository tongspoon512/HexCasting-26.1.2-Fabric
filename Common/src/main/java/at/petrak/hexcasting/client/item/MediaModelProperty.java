package at.petrak.hexcasting.client.item;

import at.petrak.hexcasting.api.item.MediaHolderItem;
import at.petrak.hexcasting.api.misc.MediaConstants;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;

/**
 * Numeric item model property backing the media battery's {@code range_dispatch} models.
 * <p>
 * {@code max = false} exposes the fill fraction (0..1) so the bottle knows which liquid level to draw;
 * {@code max = true} maps the capacity onto the 0..4 phial sizes with the same log curve the old
 * {@code hexcasting:max_media} predicate used.
 */
public record MediaModelProperty(boolean max) implements RangeSelectItemModelProperty {
    public static final MapCodec<MediaModelProperty> MEDIA_CODEC = MapCodec.unit(new MediaModelProperty(false));
    public static final MapCodec<MediaModelProperty> MAX_CODEC = MapCodec.unit(new MediaModelProperty(true));

    @Override
    public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
        if (!(stack.getItem() instanceof MediaHolderItem holder)) {
            return 0;
        }
        if (!max) {
            return holder.getMediaFullness(stack);
        }
        var maxMedia = holder.getMaxMedia(stack);
        return 1.049658f * (float) Math.log((float) maxMedia / MediaConstants.CRYSTAL_UNIT + 9.06152f) - 2.1436f;
    }

    @Override
    public MapCodec<? extends RangeSelectItemModelProperty> type() {
        return max ? MAX_CODEC : MEDIA_CODEC;
    }
}
