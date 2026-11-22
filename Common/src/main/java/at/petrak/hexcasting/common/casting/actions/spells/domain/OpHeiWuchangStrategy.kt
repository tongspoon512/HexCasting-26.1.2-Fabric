package at.petrak.hexcasting.common.casting.actions.spells.domain

import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getArea
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadCaster
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation
import net.minecraft.server.level.ServerPlayer
import java.util.UUID

/**
 * 黑无常之策略：销毁领域。
 */
object OpHeiWuchangStrategy : SpellAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val area = args.getArea(0, this.argc)
        val domain = AreaField.get(area.domainId)
            ?: throw MishapBadLocation(area.shape.center, "domain_expired")
        val caster = env.castingEntity as? ServerPlayer ?: throw MishapBadCaster()
        // 只能销毁自己的领域；他人的领域只能通过重叠裁决抹除
        if (domain.ownerId != caster.uuid) {
            throw MishapBadLocation(domain.shape.center, "not_your_domain")
        }
        return SpellAction.Result(Spell(area.domainId), 0, emptyList())
    }

    private data class Spell(val id: UUID) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            AreaField.remove(this.id)
        }
    }
}
