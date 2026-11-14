package gay.`object`.hexdebug.actions

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.NullIota
import at.petrak.hexcasting.api.casting.mishaps.MishapDisallowedSpell
import at.petrak.hexcasting.api.mod.HexConfig
import gay.`object`.hexdebug.HexDebug

/**
 * Returns the code currently waiting to run for this caster, ported from HexOverpowered's
 * {@code mind_env/running_code}.
 */
object OpScheduledCode : ConstMediaAction {
    override val argc = 0

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        if (!HexConfig.server().enablesMindEnvActions()) {
            throw MishapDisallowedSpell("disallowed", HexDebug.id("mind_env/running_code"))
        }
        val data = OpScheduleCall.QueryScheduledCode(env)
        return listOf(if (data == null) NullIota() else ListIota(data))
    }
}
