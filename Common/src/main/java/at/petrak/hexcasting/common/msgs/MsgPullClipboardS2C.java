package at.petrak.hexcasting.common.msgs;

import at.petrak.hexcasting.xplat.IClientXplatAbstractions;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Pattern;

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

    private static final Pattern ANGLES = Pattern.compile("(?<=\")[wedsaq]*(?=\")");

    @Override
    public Identifier getFabricId() {
        return ID;
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

    public static void handle(MsgPullClipboardS2C msg) {
        var mc = Minecraft.getInstance();
        mc.execute(() -> {
            var code = mc.keyboardHandler.getClipboard();
            if (code == null || code.isBlank()) {
                return;
            }

            if (msg.mode() == MODE_ANGLES) {
                var parts = new ArrayList<String>();
                var matcher = ANGLES.matcher(code);
                while (matcher.find()) {
                    parts.add("_" + matcher.group());
                }
                if (parts.isEmpty()) {
                    return;
                }
                code = String.join(" ", parts);
            }

            var length = code.getBytes(StandardCharsets.UTF_8).length;
            if (length > MAX_CODE_LENGTH) {
                var player = mc.player;
                if (player != null) {
                    player.sendSystemMessage(Component.literal(
                        "Clipboard code is too long to send (" + length + " bytes, max " + MAX_CODE_LENGTH + ")"));
                }
                return;
            }

            IClientXplatAbstractions.INSTANCE.sendPacketToServer(
                new MsgPushClipboardC2S(code, msg.rename(), msg.mode()));
        });
    }
}
