package miyucomics.hexcassettes.network

import at.petrak.hexcasting.common.msgs.IMessage
import io.netty.buffer.ByteBuf
import miyucomics.hexcassettes.HexCassettes
import miyucomics.hexcassettes.cassetteState
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer

/**
 * 客户端登录后请求一次磁带状态同步。
 */
class MsgCassetteSyncRequestC2S : IMessage {
    override fun getFabricId(): Identifier = ID

    override fun serialize(buf: FriendlyByteBuf) {
    }

    companion object {
        @JvmField
        val ID: Identifier = HexCassettes.id("sync_cassettes_request")

        @JvmStatic
        fun deserialize(buffer: ByteBuf): MsgCassetteSyncRequestC2S = MsgCassetteSyncRequestC2S()

        @JvmStatic
        fun handle(player: ServerPlayer) {
            player.cassetteState.sync(player)
        }
    }
}
