package at.petrak.hexcasting.common.casting.actions.environment

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import net.minecraft.world.level.Level

/**
 * 「布尔位面之精思」：无输入，返回施法者所在维度是否为“非下界”。
 * 在下界返回 False，其他维度返回 True。
 */
object OpBooleanPlane : ConstMediaAction {
    override val argc = 0

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        return (env.world.dimension() != Level.NETHER).asActionResult
    }
}
