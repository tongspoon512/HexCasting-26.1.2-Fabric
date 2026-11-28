package miyucomics.hexcassettes.network

import at.petrak.hexcasting.common.msgs.IMessage
import io.netty.buffer.ByteBuf
import miyucomics.hexcassettes.HexCassettes
import miyucomics.hexcassettes.cassetteState
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer

/**
 * 客户端请求移除给定标签的磁带。
 */
data class MsgCassetteRemoveC2S(val label: String) : IMessage {
    override fun getFabricId(): Identifier = ID

    override fun serialize(buf: FriendlyByteBuf) {
        buf.writeUtf(label)
    }

    companion object {
        @JvmField
        val ID: Identifier = HexCassettes.id("cassette_remove")

        @JvmStatic
        fun deserialize(buffer: ByteBuf): MsgCassetteRemoveC2S =
            MsgCassetteRemoveC2S(FriendlyByteBuf(buffer).readUtf())

        @JvmStatic
        fun handle(player: ServerPlayer, msg: MsgCassetteRemoveC2S) {
            val state = player.cassetteState
            state.hexes.remove(msg.label)
            state.sync(player)
        }
    }
}
