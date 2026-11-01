package at.petrak.hexcasting.common.items;

import at.petrak.hexcasting.client.gui.GridSpacingScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * 探知透镜组：右键打开咒术网格间距调节界面，修改客户端的 gridSpacing 配置。
 */
public class ItemScryingLensAssembly extends Item {
    public ItemScryingLensAssembly(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        if (world.isClientSide()) {
            Minecraft.getInstance().setScreen(new GridSpacingScreen());
        }
        return InteractionResult.SUCCESS.heldItemTransformedTo(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
        Consumer<Component> tooltip, TooltipFlag advanced) {
        tooltip.accept(Component.translatable("item.hexcasting.lens_assembly.tooltip")
            .withStyle(ChatFormatting.GRAY));
    }
}
