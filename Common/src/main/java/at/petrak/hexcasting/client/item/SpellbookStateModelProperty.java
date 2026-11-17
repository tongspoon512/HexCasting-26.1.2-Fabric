package at.petrak.hexcasting.client.item;

import at.petrak.hexcasting.api.item.IotaHolderItem;
import at.petrak.hexcasting.api.utils.NBTHelper;
import at.petrak.hexcasting.common.items.storage.ItemSpellbook;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Selects a spellbook model based on the state of the page currently selected in the stack.
 * <p>
 * The legacy {@code overlay_layer} item property was removed in 26.1, and the page data lives in
 * {@code custom_data} keyed by page index, so the built-in component predicate cannot express it.
 */
public record SpellbookStateModelProperty(String state) implements ConditionalItemModelProperty {
    public static final String SEALED = "sealed";
    public static final String HAS_IOTA = "has_iota";

    public static final MapCodec<SpellbookStateModelProperty> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.fieldOf("state").forGetter(SpellbookStateModelProperty::state)
        ).apply(instance, SpellbookStateModelProperty::new));

    @Override
    public boolean get(ItemStack stack, ClientLevel level, LivingEntity entity, int seed,
        ItemDisplayContext context) {
        if (!(stack.getItem() instanceof ItemSpellbook spellbook)) {
            return false;
        }
        return switch (state) {
            case SEALED -> ItemSpellbook.isSealed(stack);
            case HAS_IOTA -> spellbook.readIotaTag(stack) != null
                || NBTHelper.hasString(stack, IotaHolderItem.TAG_OVERRIDE_VISUALLY);
            default -> false;
        };
    }

    @Override
    public MapCodec<? extends ConditionalItemModelProperty> type() {
        return MAP_CODEC;
    }
}
