package at.petrak.hexcasting.common.msgs;

import at.petrak.hexcasting.api.casting.eval.ExecutionClientView;
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType;
import at.petrak.hexcasting.client.ClientMessageHandlers;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.Optional;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

/**
 * Sent server->client when the player finishes casting a spell.
 */
public record MsgNewSpellPatternS2C(ExecutionClientView info, int index) implements IMessage {
    public static final Identifier ID = modLoc("pat_sc");

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    public static MsgNewSpellPatternS2C deserialize(ByteBuf buffer) {
        var buf = new FriendlyByteBuf(buffer);

        var isStackEmpty = buf.readBoolean();
        var resolutionType = buf.readEnum(ResolvedPatternType.class);
        var index = buf.readInt();

        var stack = buf.readList(b -> b.readNbt());
        var raven = buf.readOptional(b -> b.readNbt()).orElse(null);

        return new MsgNewSpellPatternS2C(
            new ExecutionClientView(isStackEmpty, resolutionType, stack, raven), index
        );
    }

    @Override
    public void serialize(FriendlyByteBuf buf) {
        buf.writeBoolean(this.info.isStackClear());
        buf.writeEnum(this.info.getResolutionType());
        buf.writeInt(this.index);

        buf.writeCollection(this.info.getStackDescs(), (b, t) -> b.writeNbt(t));
        buf.writeOptional(Optional.ofNullable(this.info.getRavenmind()), (b, t) -> b.writeNbt(t));
    }

    public static void handle(MsgNewSpellPatternS2C self) {
        ClientMessageHandlers.newSpellPattern(self);
    }
}
