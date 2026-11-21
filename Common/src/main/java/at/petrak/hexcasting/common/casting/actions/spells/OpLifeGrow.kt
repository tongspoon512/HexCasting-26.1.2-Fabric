package at.petrak.hexcasting.common.casting.actions.spells

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getDouble
import at.petrak.hexcasting.api.casting.getEntity
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.api.misc.MediaConstants
import net.minecraft.world.entity.AgeableMob
import kotlin.math.ceil

/**
 * 生命的节拍·成长：把一只幼崽的成长计时向前推进若干秒。
 * 上限 60 秒并按 60 秒计费，避免一次施法直接刷出成年生物。
 */
object OpLifeGrow : SpellAction {
    override val argc = 2

    private const val MAX_SECONDS = 60

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val entity = args.getEntity(0, argc)
        env.assertEntityInRange(entity)

        val secondsRaw = args.getDouble(1, argc)
        val seconds = secondsRaw.toInt()
        if (secondsRaw != seconds.toDouble() || seconds < 1 || seconds > MAX_SECONDS) {
            throw MishapInvalidIota.of(args[1], 0, "seconds")
        }

        val mob = entity as? AgeableMob
        if (mob == null || !mob.isBaby) {
            throw MishapInvalidIota.of(args[0], argc - 1, "baby")
        }

        val cost = MediaConstants.DUST_UNIT * (1 + ceil(seconds / 10.0).toLong())
        return SpellAction.Result(
            Spell(mob, seconds),
            cost,
            listOf(ParticleSpray.burst(mob.position(), 1.0))
        )
    }

    private data class Spell(val mob: AgeableMob, val seconds: Int) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            if (!mob.isAlive || !mob.isBaby) return
            mob.ageUp(seconds * 20, true)
        }
    }
}
