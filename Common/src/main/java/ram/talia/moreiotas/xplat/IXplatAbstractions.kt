package ram.talia.moreiotas.xplat

import net.minecraft.world.entity.player.Player

/**
 * MoreIotas 需要的平台能力。单 jar 架构下由 Fabric 侧在初始化时替换 [INSTANCE]。
 */
interface IXplatAbstractions {
    fun lastMessage(player: Player?): String?

    fun getChatPrefix(player: Player?): String?

    fun setChatPrefix(player: Player, prefix: String?)

    companion object {
        @Volatile
        var INSTANCE: IXplatAbstractions = Noop
    }
}

private object Noop : IXplatAbstractions {
    override fun lastMessage(player: Player?): String? = null
    override fun getChatPrefix(player: Player?): String? = null
    override fun setChatPrefix(player: Player, prefix: String?) {}
}
