package ram.talia.moreiotas.neoforge

import at.petrak.hexcasting.neoforge.attachment.NeoForgeHexAttachments
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.event.ServerChatEvent
import ram.talia.moreiotas.xplat.IXplatAbstractions
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * MoreIotas 的 NeoForge 平台实现：缓存最近一条聊天消息并处理聊天前缀。
 *
 * 开启前缀后，以该前缀开头的消息会被截断前缀并取消发送，剩余内容由 OpChatString 读取，
 * 与 Fabric 侧 ChatEventHandler 行为一致。
 */
object NeoForgeMoreIotasXplat : IXplatAbstractions {
    private val lastMessages = ConcurrentHashMap<UUID, String>()

    @Volatile
    private var lastMessage: String? = null

    fun receiveChat(event: ServerChatEvent) {
        val player = event.player
        val text = event.rawText
        val prefix = player.getData(NeoForgeHexAttachments.CHAT_PREFIX).prefix

        if (prefix == null) {
            lastMessages[player.uuid] = text
            lastMessage = text
            return
        }

        if (!text.startsWith(prefix)) {
            lastMessage = text
            return
        }

        lastMessages[player.uuid] = text.substring(prefix.length)
        event.isCanceled = true
    }

    override fun lastMessage(player: Player?): String? =
        if (player != null) lastMessages[player.uuid] else lastMessage

    override fun getChatPrefix(player: Player?): String? =
        player?.getData(NeoForgeHexAttachments.CHAT_PREFIX)?.prefix

    override fun setChatPrefix(player: Player, prefix: String?) {
        player.getData(NeoForgeHexAttachments.CHAT_PREFIX).prefix = prefix
    }
}
