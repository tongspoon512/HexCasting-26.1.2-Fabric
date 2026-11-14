package at.petrak.hexcasting.common.msgs;

import at.petrak.hexcasting.client.ClientMessageHandlers;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.UUID;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

public record MsgClearSpiralPatternsS2C(UUID playerUUID) implements IMessage {
    public static final Identifier ID = modLoc("clr_spi_pats_sc");

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    public static MsgClearSpiralPatternsS2C deserialize(ByteBuf buffer) {
        var buf = new FriendlyByteBuf(buffer);

        var player = buf.readUUID();

        return new MsgClearSpiralPatternsS2C(player);
    }

    @Override
    public void serialize(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
    }

    public static void handle(MsgClearSpiralPatternsS2C self) {
        ClientMessageHandlers.clearSpiralPatterns(self);
    }
}
