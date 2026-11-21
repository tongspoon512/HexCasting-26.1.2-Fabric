package at.petrak.hexcasting.common.casting.actions.spells.domain

import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getArea
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation
import java.util.UUID

/**
 * 黑无常之策略：销毁领域。
 */
object OpHeiWuchangStrategy : SpellAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val area = args.getArea(0, this.argc)
        AreaField.get(area.domainId) ?: throw MishapBadLocation(area.shape.center, "domain_expired")
        return SpellAction.Result(Spell(area.domainId), 0, emptyList())
    }

    private data class Spell(val id: UUID) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            AreaField.remove(this.id)
        }
    }
}
