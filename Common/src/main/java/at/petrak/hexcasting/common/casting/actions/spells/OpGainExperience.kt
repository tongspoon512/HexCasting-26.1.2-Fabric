package at.petrak.hexcasting.common.casting.actions.spells

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getEntity
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadCaster
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.api.misc.MediaConstants
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.ExperienceOrb

object OpGainExperience : SpellAction {
    override val argc: Int
        get() = 1

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        if (env.castingEntity !is ServerPlayer)
            throw MishapBadCaster()

        val entity = args.getEntity(0, argc)
        if (entity !is ExperienceOrb)
            throw MishapInvalidIota.ofType(args[0], 0, "entity.experience_orb")
        env.assertEntityInRange(entity)

        return SpellAction.Result(
            Spell(entity),
            MediaConstants.DUST_UNIT / 100,
            listOf(ParticleSpray.burst(entity.position(), 1.0))
        )
    }

    private data class Spell(val orb: ExperienceOrb) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            val player = env.castingEntity as? ServerPlayer ?: return
            if (!orb.isAlive || orb.level() != player.level()) return

            // 与原版拾取经验球一致：把该球的价值加到玩家身上，然后移除经验球。
            player.giveExperiencePoints(orb.value)
            orb.discard()
        }
    }
}
