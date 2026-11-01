package at.petrak.hexcasting.common.msgs;

import at.petrak.hexcasting.common.lib.hexparse.HexParseCommand;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

/**
 * 客户端把剪贴板内容发回服务端（{@link MsgPullClipboardS2C} 的回程）。
 */
public record MsgPushClipboardC2S(String code, @Nullable String rename, int mode) implements IMessage {
    public static final Identifier ID = modLoc("hexparse/clipboard_push");

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void serialize(FriendlyByteBuf buf) {
        buf.writeUtf(this.code);
        buf.writeBoolean(this.rename != null);
        if (this.rename != null) {
            buf.writeUtf(this.rename);
        }
        buf.writeByte(this.mode);
    }

    public static MsgPushClipboardC2S deserialize(ByteBuf buffer) {
        var buf = new FriendlyByteBuf(buffer);
        var code = buf.readUtf();
        String rename = null;
        if (buf.readBoolean()) {
            rename = buf.readUtf();
        }
        var mode = buf.readByte();
        return new MsgPushClipboardC2S(code, rename, mode);
    }

    public static void handle(ServerPlayer sender, MsgPushClipboardC2S msg) {
        if (msg.mode() < 0 || msg.mode() > MsgPullClipboardS2C.MODE_ANGLES) {
            return;
        }
        HexParseCommand.applyCode(sender, msg.code(), msg.rename());
    }
}
