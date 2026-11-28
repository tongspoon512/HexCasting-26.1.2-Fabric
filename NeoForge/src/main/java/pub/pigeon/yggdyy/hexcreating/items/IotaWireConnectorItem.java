package pub.pigeon.yggdyy.hexcreating.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import pub.pigeon.yggdyy.hexcreating.api.iotawire.IotaWireConnectorPropertyHelper;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlocks;

/**
 * Places a single socket on the face that was clicked. Additional sockets are added to an
 * existing connector block by clicking another face of it.
 */
public class IotaWireConnectorItem extends Item {
    public IotaWireConnectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Direction clicked = context.getClickedFace();
        BlockPos targetPos = context.getClickedPos().relative(clicked);
        var property = IotaWireConnectorPropertyHelper.getPropertyByDir(clicked.getOpposite());
        BlockState state = level.getBlockState(targetPos);
        ItemStack stack = context.getItemInHand();

        if (state.isAir()) {
            BlockState placed = HexCreatingBlocks.IOTA_WIRE_CONNECTOR.defaultBlockState()
                .setValue(property, true);
            level.setBlock(targetPos, placed, 3);
            level.playSound(null, targetPos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
            stack.shrink(1);
            return InteractionResult.SUCCESS;
        }

        if (state.is(HexCreatingBlocks.IOTA_WIRE_CONNECTOR) && !state.getValue(property)) {
            level.setBlock(targetPos, state.setValue(property, true), 3);
            level.playSound(null, targetPos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
            stack.shrink(1);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
