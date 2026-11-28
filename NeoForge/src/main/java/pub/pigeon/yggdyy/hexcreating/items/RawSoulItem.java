package pub.pigeon.yggdyy.hexcreating.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Soul left behind when a mob dies in soul fire. Keeps the id of the mob it came from so the
 * JEI description and tooltips can say whose soul it is.
 */
public class RawSoulItem extends Item {
    private static final String ENTITY_ID_KEY = "hexcreating:entity_id";
    private static final String SOUL_AMOUNT_KEY = "hexcreating:soul_amount";
    public static final int DEFAULT_SOUL_AMOUNT = 25;

    public RawSoulItem(Properties props) {
        super(props);
    }

    public static ItemStack create(EntityType<?> type) {
        ItemStack stack = new ItemStack(HexCreatingItems.RAW_SOUL);
        setEntityId(stack, EntityType.getKey(type));
        setSoulAmount(stack, DEFAULT_SOUL_AMOUNT);
        return stack;
    }

    public static void setEntityId(ItemStack stack, Identifier entityId) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
            tag -> tag.putString(ENTITY_ID_KEY, entityId.toString()));
    }

    public static void setSoulAmount(ItemStack stack, int amount) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(SOUL_AMOUNT_KEY, amount));
    }

    @Nullable
    public static Identifier getEntityId(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) {
            return null;
        }
        String id = data.copyTag().getStringOr(ENTITY_ID_KEY, "");
        return id.isEmpty() ? null : Identifier.tryParse(id);
    }

    public static int getSoulAmount(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) {
            return DEFAULT_SOUL_AMOUNT;
        }
        return data.copyTag().getIntOr(SOUL_AMOUNT_KEY, DEFAULT_SOUL_AMOUNT);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
        Consumer<Component> tooltip, TooltipFlag flag) {
        Identifier entityId = getEntityId(stack);
        if (entityId != null) {
            tooltip.accept(Component.translatable(entityId.toLanguageKey("entity"))
                .withStyle(ChatFormatting.DARK_RED));
        }
        tooltip.accept(Component.translatable("hexcreating.tooltip.raw_soul").withStyle(ChatFormatting.GRAY));
    }
}
