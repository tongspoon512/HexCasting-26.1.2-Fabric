package miyucomics.hexcassettes.network

import at.petrak.hexcasting.common.msgs.IMessage
import miyucomics.hexcassettes.HexCassettes
import miyucomics.hexcassettes.client.CassetteClientStorage
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import io.netty.buffer.ByteBuf
import net.minecraft.resources.Identifier

/**
 * 服务端把已拥有的槽位数与所有忙碌磁带的标签同步给客户端。
 */
data class MsgCassetteSyncS2C(val owned: Int, val keys: List<String>) : IMessage {
    override fun getFabricId(): Identifier = ID

    override fun serialize(buf: FriendlyByteBuf) {
        buf.writeInt(owned)
        buf.writeInt(keys.size)
        for (key in keys) {
            buf.writeUtf(key)
        }
    }

    companion object {
        @JvmField
        val ID: Identifier = HexCassettes.id("sync_cassettes")

        @JvmStatic
        fun deserialize(buffer: ByteBuf): MsgCassetteSyncS2C {
            val buf = FriendlyByteBuf(buffer)
            val owned = buf.readInt()
            val count = buf.readInt()
            val keys = ArrayList<String>(count)
            repeat(count) { keys.add(buf.readUtf()) }
            return MsgCassetteSyncS2C(owned, keys)
        }

        @JvmStatic
        fun handle(msg: MsgCassetteSyncS2C) {
            Minecraft.getInstance().execute {
                CassetteClientStorage.update(msg.owned, msg.keys)
            }
        }
    }
}
