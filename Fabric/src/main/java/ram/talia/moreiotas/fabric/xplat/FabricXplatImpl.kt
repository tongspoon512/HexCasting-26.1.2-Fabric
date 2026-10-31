package ram.talia.moreiotas.fabric.xplat

import net.minecraft.world.entity.player.Player
import ram.talia.moreiotas.fabric.cc.MoreIotasCardinalComponents
import ram.talia.moreiotas.fabric.eventhandlers.ChatEventHandler
import ram.talia.moreiotas.xplat.IXplatAbstractions

object FabricXplatImpl : IXplatAbstractions {
    override fun lastMessage(player: Player?): String? = ChatEventHandler.lastMessage(player)

    override fun getChatPrefix(player: Player?): String? =
        player?.let { MoreIotasCardinalComponents.CHAT_PREFIX_HOLDER.get(it).prefix }

    override fun setChatPrefix(player: Player, prefix: String?) {
        MoreIotasCardinalComponents.CHAT_PREFIX_HOLDER.get(player).setPrefix(prefix)
    }
}
