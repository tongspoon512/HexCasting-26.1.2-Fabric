package at.petrak.hexcasting.common.casting.actions.spells.domain

import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getArea
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation
import java.util.UUID

/**
 * 白无常之策略：暂停或恢复领域的释放。
 */
object OpBaiWuchangStrategy : SpellAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val area = args.getArea(0, this.argc)
        val domain = AreaField.get(area.domainId)
            ?: throw MishapBadLocation(area.shape.center, "domain_expired")
        return SpellAction.Result(Spell(domain.id, !domain.active), 0, emptyList())
    }

    private data class Spell(val id: UUID, val active: Boolean) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            AreaField.setActive(this.id, this.active)
        }
    }
}
