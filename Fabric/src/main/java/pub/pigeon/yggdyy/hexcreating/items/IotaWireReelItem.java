package pub.pigeon.yggdyy.hexcreating.items;

import at.petrak.hexcasting.api.utils.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import pub.pigeon.yggdyy.hexcreating.api.iotawire.IIotaWireConnectorHost;
import pub.pigeon.yggdyy.hexcreating.api.iotawire.IotaWireConnector;
import pub.pigeon.yggdyy.hexcreating.api.iotawire.IotaWireProcessor;

/**
 * Two-step wire placement: right-click one socket, right-click a second socket, then sneak-right-click
 * the air to actually connect them. Mirrors the upstream reel item, including its messages.
 */
public class IotaWireReelItem extends Item {
    private static final String TAG_CUR = "cur";
    private static final String[] POS0 = {"pos0_x", "pos0_y", "pos0_z"};
    private static final String[] POS1 = {"pos1_x", "pos1_y", "pos1_z"};
    private static final String TAG_ID0 = "id0";
    private static final String TAG_ID1 = "id1";
    private static final double MAX_DISTANCE = 16.0;

    public IotaWireReelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide() || player == null || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof IIotaWireConnectorHost host)) {
            return InteractionResult.PASS;
        }
        Vec3 relative = context.getClickLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        for (IotaWireConnector connector : host.getIotaWireConnectors()) {
            if (!connector.clickBox.inflate(0.005).contains(relative)) {
                continue;
            }
            ItemStack stack = context.getItemInHand();
            int id = connector.terminal.terminalId;
            if (NBTHelper.getInt(stack, TAG_CUR, 0) == 0) {
                NBTHelper.putInt(stack, POS0[0], pos.getX());
                NBTHelper.putInt(stack, POS0[1], pos.getY());
                NBTHelper.putInt(stack, POS0[2], pos.getZ());
                NBTHelper.putInt(stack, TAG_ID0, id);
                NBTHelper.putInt(stack, TAG_CUR, 1);
            } else {
                NBTHelper.putInt(stack, POS1[0], pos.getX());
                NBTHelper.putInt(stack, POS1[1], pos.getY());
                NBTHelper.putInt(stack, POS1[2], pos.getZ());
                NBTHelper.putInt(stack, TAG_ID1, id);
                NBTHelper.putInt(stack, TAG_CUR, 0);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!NBTHelper.contains(stack, TAG_ID0) || !NBTHelper.contains(stack, TAG_ID1)) {
            player.sendSystemMessage(Component.translatable("hexcreating.iota_wire_reel.unfinished"));
            return InteractionResult.PASS;
        }

        BlockPos pos0 = new BlockPos(NBTHelper.getInt(stack, POS0[0], 0),
            NBTHelper.getInt(stack, POS0[1], 0), NBTHelper.getInt(stack, POS0[2], 0));
        BlockPos pos1 = new BlockPos(NBTHelper.getInt(stack, POS1[0], 0),
            NBTHelper.getInt(stack, POS1[1], 0), NBTHelper.getInt(stack, POS1[2], 0));
        int id0 = NBTHelper.getInt(stack, TAG_ID0, 0);
        int id1 = NBTHelper.getInt(stack, TAG_ID1, 0);

        if (pos0.equals(pos1) && id0 == id1) {
            player.sendSystemMessage(Component.translatable("hexcreating.iota_wire_reel.same"));
            return InteractionResult.PASS;
        }
        if (pos0.getCenter().distanceTo(pos1.getCenter()) > MAX_DISTANCE) {
            player.sendSystemMessage(Component.translatable("hexcreating.iota_wire_reel.far"));
            return InteractionResult.PASS;
        }
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        if (!IotaWireProcessor.tryConnect2Connectors(serverLevel, pos0, id0, pos1, id1)) {
            player.sendSystemMessage(Component.translatable("hexcreating.iota_wire_reel.fail"));
            return InteractionResult.PASS;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            ItemStack empty = new ItemStack(HexCreatingItems.EMPTY_REEL);
            if (!player.getInventory().add(empty)) {
                player.drop(empty, false);
            }
        }
        player.sendSystemMessage(Component.translatable("hexcreating.iota_wire_reel.success"));
        return InteractionResult.CONSUME;
    }
}
