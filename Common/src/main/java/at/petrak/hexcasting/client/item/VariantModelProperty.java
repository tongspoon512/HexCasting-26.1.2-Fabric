package at.petrak.hexcasting.client.item;

import at.petrak.hexcasting.api.item.VariantItem;
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
 * Selects a model based on {@link VariantItem#getVariant(ItemStack)}.
 * <p>
 * The built-in {@code minecraft:component} predicate reads its NBT through {@code JsonOps}, which turns
 * small JSON numbers into {@code ByteTag}s, while variants are written as {@code IntTag}s -- so the
 * predicate never matched and every variant fell back to index 0.
 */
public record VariantModelProperty(int variant) implements ConditionalItemModelProperty {
    public static final MapCodec<VariantModelProperty> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.INT.fieldOf("variant").forGetter(VariantModelProperty::variant)
        ).apply(instance, VariantModelProperty::new));

    @Override
    public boolean get(ItemStack stack, ClientLevel level, LivingEntity entity, int seed,
        ItemDisplayContext context) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getIntOr(VariantItem.TAG_VARIANT, 0) == variant;
    }

    @Override
    public MapCodec<? extends ConditionalItemModelProperty> type() {
        return MAP_CODEC;
    }
}
