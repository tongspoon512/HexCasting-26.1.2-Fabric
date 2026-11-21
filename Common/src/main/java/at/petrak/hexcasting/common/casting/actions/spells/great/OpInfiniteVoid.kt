package at.petrak.hexcasting.common.casting.actions.spells.great

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getLivingEntityButNotArmorStand
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapDisallowedSpell
import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.common.casting.actions.spells.domain.DomainCastEnv
import at.petrak.hexcasting.common.casting.actions.spells.domain.InfinityVoidField
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.LivingEntity

/**
 * 无量空处：只能由领域释放。让一个生物陷入无 AI 的封闭状态，
 * 玩家额外获得黑暗、失明与缓慢 V，并在 30 秒内无法施法。
 */
object OpInfiniteVoid : SpellAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        if (env !is DomainCastEnv) {
            throw MishapDisallowedSpell("not_domain", null)
        }
        val entity = args.getLivingEntityButNotArmorStand(0, this.argc)
        env.assertEntityInRange(entity)

        return SpellAction.Result(
            Spell(entity),
            20 * MediaConstants.DUST_UNIT,
            listOf(ParticleSpray.burst(entity.position(), 2.0, 40))
        )
    }

    private data class Spell(val entity: LivingEntity) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            val level = env.world as? ServerLevel ?: return
            if (!this.entity.isAlive) return
            InfinityVoidField.apply(level, this.entity)
        }
    }
}
