package at.petrak.hexcasting.common.msgs;

import at.petrak.hexcasting.client.ClientMessageHandlers;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

/**
 * 请求客户端把剪贴板内容发回服务端（由 /hexParse clipboard 使用）。
 */
public record MsgPullClipboardS2C(String rename, int mode) implements IMessage {
    public static final Identifier ID = modLoc("hexparse/clipboard_pull");

    public static final int MODE_DEFAULT = 0;
    public static final int MODE_ANGLES = 1;

    /** writeUtf 上限是 32767 字节，留一点余量 */
    public static final int MAX_CODE_LENGTH = 32000;

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    public static void handle(MsgPullClipboardS2C msg) {
        ClientMessageHandlers.pullClipboard(msg);
    }

    @Override
    public void serialize(FriendlyByteBuf buf) {
        buf.writeByte(this.mode);
        buf.writeBoolean(this.rename != null);
        if (this.rename != null) {
            buf.writeUtf(this.rename);
        }
    }

    public static MsgPullClipboardS2C deserialize(ByteBuf buffer) {
        var buf = new FriendlyByteBuf(buffer);
        var mode = buf.readByte();
        String rename = null;
        if (buf.readBoolean()) {
            rename = buf.readUtf();
        }
        return new MsgPullClipboardS2C(rename, mode);
    }
}
