package ram.talia.moreiotas.common.casting.actions.strings

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import net.minecraft.world.entity.player.Player
import ram.talia.moreiotas.api.asActionResult
import ram.talia.moreiotas.xplat.IXplatAbstractions

/**
 * 返回最近一条聊天消息；allChat 为 true 时取全服最近一条，否则取施法者自己的。
 */
class OpChatString(private val allChat: Boolean) : ConstMediaAction {
    override val argc = 0

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> =
        IXplatAbstractions.INSTANCE.lastMessage(if (allChat) null else env.castingEntity as? Player)?.asActionResult
            ?: "".asActionResult
}
