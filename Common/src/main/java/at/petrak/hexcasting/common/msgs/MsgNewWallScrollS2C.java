package at.petrak.hexcasting.common.msgs;

import at.petrak.hexcasting.api.utils.HexUtils;
import at.petrak.hexcasting.client.ClientMessageHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

// https://github.com/VazkiiMods/Botania/blob/1.18.x/Xplat/src/main/java/vazkii/botania/network/clientbound/PacketSpawnDoppleganger.java
public record MsgNewWallScrollS2C(int entityId, UUID uuid, BlockPos pos, Direction dir, ItemStack scrollItem,
                                  boolean showsStrokeOrder, int blockSize) implements IMessage {
    public static final Identifier ID = modLoc("wallscr");

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void serialize(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeUUID(uuid);
        buf.writeBlockPos(pos);
        buf.writeByte(dir.ordinal());
        buf.writeNbt(HexUtils.serializeToNBT(scrollItem));
        buf.writeBoolean(showsStrokeOrder);
        buf.writeVarInt(blockSize);
    }

    public static MsgNewWallScrollS2C deserialize(FriendlyByteBuf buf) {
        var entityId = buf.readVarInt();
        var uuid = buf.readUUID();
        var pos = buf.readBlockPos();
        var dir = HexUtils.getSafe(Direction.values(), buf.readByte());
        CompoundTag scrollTag = buf.readNbt();
        var scroll = scrollTag == null ? ItemStack.EMPTY
            : ItemStack.CODEC.parse(NbtOps.INSTANCE, scrollTag).result().orElse(ItemStack.EMPTY);
        var strokeOrder = buf.readBoolean();
        var blockSize = buf.readVarInt();
        return new MsgNewWallScrollS2C(entityId, uuid, pos, dir, scroll, strokeOrder, blockSize);
    }

    public static void handle(MsgNewWallScrollS2C self) {
        ClientMessageHandlers.newWallScroll(self);
    }
}
