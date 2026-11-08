package at.petrak.hexcasting.client.item;

import at.petrak.hexcasting.common.items.storage.ItemScroll;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * True when the stack's {@code minecraft:custom_data} contains the given tag, regardless of its value.
 * <p>
 * Used for the ancient scroll, which only stores an arbitrary {@code op_id} string; the built-in
 * component predicate can only compare exact values, so it cannot express "any string".
 */
public record CustomDataTagModelProperty(String tag) implements ConditionalItemModelProperty {
    public static final MapCodec<CustomDataTagModelProperty> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.fieldOf("tag").forGetter(CustomDataTagModelProperty::tag)
        ).apply(instance, CustomDataTagModelProperty::new));

    public static CustomDataTagModelProperty ancientScroll() {
        return new CustomDataTagModelProperty(ItemScroll.TAG_OP_ID);
    }

    @Override
    public boolean get(ItemStack stack, ClientLevel level, LivingEntity entity, int seed,
        ItemDisplayContext context) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().contains(tag);
    }

    @Override
    public MapCodec<? extends ConditionalItemModelProperty> type() {
        return MAP_CODEC;
    }
}
