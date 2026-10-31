package ram.talia.moreiotas.common.casting.actions.strings

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import ram.talia.moreiotas.api.casting.iota.StringIota
import ram.talia.moreiotas.api.getString

object OpSplitString : ConstMediaAction {
    override val argc = 2

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val toSplit = args.getString(0, argc)
        val splitOn = args.getString(1, argc)
        return toSplit.split(splitOn).map { StringIota.make(it) }.asActionResult
    }
}
