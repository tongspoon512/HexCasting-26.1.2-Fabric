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
import net.minecraft.world.entity.animal.allay.Allay
import kotlin.math.ceil

/**
 * 生命的节拍·兴奋：让一只悦灵持续跳舞若干秒。
 * 悦灵会自行检查点唱机，所以跳舞状态由 LifeEffectField 每 10 tick 维持一次。
 */
object OpLifeExcite : SpellAction {
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

        val allay = entity as? Allay
        if (allay == null) {
            throw MishapInvalidIota.of(args[0], argc - 1, "allay")
        }

        val cost = MediaConstants.DUST_UNIT * (1 + ceil(seconds / 10.0).toLong())
        return SpellAction.Result(
            Spell(allay, seconds),
            cost,
            listOf(ParticleSpray.burst(allay.position(), 1.0))
        )
    }

    private data class Spell(val allay: Allay, val seconds: Int) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            if (!allay.isAlive) return
            LifeEffectField.excite(env.world, allay, seconds * 20)
        }
    }
}
