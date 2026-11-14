package at.petrak.hexcasting.common.msgs;

import at.petrak.hexcasting.client.ClientMessageHandlers;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.Vec3;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

public record MsgBeepS2C(Vec3 target, int note, NoteBlockInstrument instrument) implements IMessage {
    public static final Identifier ID = modLoc("beep");

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    public static MsgBeepS2C deserialize(ByteBuf buffer) {
        var buf = new FriendlyByteBuf(buffer);
        var x = buf.readDouble();
        var y = buf.readDouble();
        var z = buf.readDouble();
        var note = buf.readInt();
        var instrument = buf.readEnum(NoteBlockInstrument.class);
        return new MsgBeepS2C(new Vec3(x, y, z), note, instrument);
    }

    @Override
    public void serialize(FriendlyByteBuf buf) {
        buf.writeDouble(this.target.x);
        buf.writeDouble(this.target.y);
        buf.writeDouble(this.target.z);
        buf.writeInt(this.note);
        buf.writeEnum(instrument);
    }

    public static void handle(MsgBeepS2C msg) {
        ClientMessageHandlers.beep(msg);
    }
}
