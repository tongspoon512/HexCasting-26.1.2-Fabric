package at.petrak.hexcasting.common.casting.actions.spells.domain

import at.petrak.hexcasting.api.casting.area.AreaIota
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getVec3
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation
import net.minecraft.server.level.ServerLevel

/**
 * 魔虚罗之纯化：向量 → 该向量所在领域的领域 iota。
 */
object OpMahoragaPurification : ConstMediaAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val point = args.getVec3(0, this.argc)
        val level = env.world as? ServerLevel
            ?: throw MishapBadLocation(point, "no_domain")
        val domain = AreaField.findContaining(level, point)
            ?: throw MishapBadLocation(point, "no_domain")
        return listOf(AreaIota(domain.shape, domain.id))
    }
}
