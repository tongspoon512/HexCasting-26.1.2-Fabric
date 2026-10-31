package ram.talia.moreiotas.fabric.eventhandlers

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.minecraft.network.chat.PlayerChatMessage
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import ram.talia.moreiotas.fabric.cc.MoreIotasCardinalComponents
import java.util.UUID

/**
 * 缓存玩家最近一条聊天消息，并处理聊天前缀。
 * 开启前缀后，以该前缀开头的消息会被截断前缀并取消发送，剩余内容由 OpChatString 读取。
 */
object ChatEventHandler {
    private val lastMessages = mutableMapOf<UUID, String>()
    private var lastMessage: String? = null

    fun init() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register { message, player, _ ->
            receiveChat(message, player)
        }
    }

    private fun receiveChat(message: PlayerChatMessage, player: ServerPlayer): Boolean {
        val text = message.signedContent()
        val prefix = MoreIotasCardinalComponents.CHAT_PREFIX_HOLDER.get(player).prefix

        if (prefix == null) {
            lastMessages[player.uuid] = text
            lastMessage = text
            return true
        }

        if (!text.startsWith(prefix)) {
            lastMessage = text
            return true
        }

        lastMessages[player.uuid] = text.substring(prefix.length)
        return false
    }

    @JvmStatic
    fun lastMessage(player: Player?): String? =
        if (player != null) lastMessages[player.uuid] else lastMessage
}
