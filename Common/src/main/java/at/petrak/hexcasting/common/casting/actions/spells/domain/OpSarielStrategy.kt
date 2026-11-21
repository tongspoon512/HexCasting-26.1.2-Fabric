package at.petrak.hexcasting.common.casting.actions.spells.domain

import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getArea
import at.petrak.hexcasting.api.casting.getList
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation
import java.util.UUID

/**
 * 沙利叶之策略：领域 + 咒术列表 → 开始释放。
 */
object OpSarielStrategy : SpellAction {
    override val argc = 2

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val area = args.getArea(0, this.argc)
        val spells = args.getList(1, this.argc).toList()
        val domain = AreaField.get(area.domainId)
            ?: throw MishapBadLocation(area.shape.center, "domain_expired")
        return SpellAction.Result(Spell(domain.id, spells), 0, emptyList())
    }

    private data class Spell(val id: UUID, val spells: List<Iota>) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            if (AreaField.setSpells(this.id, this.spells)) {
                AreaField.setActive(this.id, true)
            }
        }
    }
}
