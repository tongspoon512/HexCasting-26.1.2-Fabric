package ram.talia.moreiotas.common.casting.actions.types

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.NullIota
import net.minecraft.world.item.Items
import ram.talia.moreiotas.api.asActionResult

/**
 * 取施法者手中物品的类型；空手返回 Null。
 */
object OpTypeItemHeld : ConstMediaAction {
    override val argc = 0

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        return env.getHeldItemToOperateOn { it.item != Items.AIR }
            ?.stack?.item?.takeUnless { it == Items.AIR }?.asActionResult
            ?: listOf(NullIota())
    }
}
