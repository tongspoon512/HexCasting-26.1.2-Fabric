package at.petrak.hexcasting.common.items;

import at.petrak.hexcasting.common.lib.HexItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ItemJewelerHammer extends Item {
    public ItemJewelerHammer(ToolMaterial tier, int damageMod, float attackSpeedMod, Properties props) {
        super(props.pickaxe(tier, damageMod, attackSpeedMod));
    }

    public static boolean shouldFailToBreak(Player player, BlockState state, BlockPos pos) {
        ItemStack stack = player.getMainHandItem();
        return stack.is(h -> h.value() == HexItems.JEWELER_HAMMER)
            && Block.isShapeFullBlock(state.getShape(player.level(), pos));
    }
}
