package at.petrak.hexcasting.common.msgs;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.client.ClientMessageHandlers;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

public record MsgNewSpiralPatternsS2C(UUID playerUUID, List<HexPattern> patterns, int lifetime) implements IMessage {
    public static final Identifier ID = modLoc("spi_pats_sc");

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    public static MsgNewSpiralPatternsS2C deserialize(ByteBuf buffer) {
        var buf = new FriendlyByteBuf(buffer);

        var player = buf.readUUID();
        var patterns = buf.readCollection(ArrayList::new, buff -> HexPattern.fromNBT(buf.readNbt()));
        var lifetime = buf.readInt();


        return new MsgNewSpiralPatternsS2C(player, patterns, lifetime);
    }

    @Override
    public void serialize(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeCollection(patterns, (buff, pattern) -> buff.writeNbt(pattern.serializeToNBT()));
        buf.writeInt(lifetime);
    }

    public static void handle(MsgNewSpiralPatternsS2C self) {
        ClientMessageHandlers.newSpiralPatterns(self);
    }
}
