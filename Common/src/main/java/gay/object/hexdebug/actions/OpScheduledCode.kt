package gay.`object`.hexdebug.actions

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.NullIota

/**
 * Returns the code currently waiting to run for this caster, ported from HexOverpowered's
 * {@code mind_env/running_code}.
 */
object OpScheduledCode : ConstMediaAction {
    override val argc = 0

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val data = OpScheduleCall.QueryScheduledCode(env)
        return listOf(if (data == null) NullIota() else ListIota(data))
    }
}
