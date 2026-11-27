package at.petrak.hexcasting.common.casting.actions.spells

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getDouble
import at.petrak.hexcasting.api.casting.getVec3
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.api.misc.MediaConstants
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.world.phys.Vec3
import ram.talia.moreiotas.api.casting.iota.StringIota
import kotlin.math.abs

/**
 * 在指定位置附近生成一批粒子。粒子类型由字符串 iota 指定，
 * 例如 `flame`、`dust:#FF0000`、`block:minecraft:stone`。
 */
object OpSpawnParticles : SpellAction {
    override val argc = 4

    private const val MAX_PARTICLES = 256
    private const val MAX_SPREAD = 16.0

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val pos = args.getVec3(0, argc)
        val spread = args.getVec3(1, argc)
        val count = args.getDouble(2, argc)

        env.assertVecInRange(pos)

        val amount = count.toInt()
        if (count != amount.toDouble() || amount < 1 || amount > MAX_PARTICLES) {
            throw MishapInvalidIota.of(args[2], argc - 3, "particle_count")
        }
        if (abs(spread.x) > MAX_SPREAD || abs(spread.y) > MAX_SPREAD || abs(spread.z) > MAX_SPREAD) {
            throw MishapBadLocation(pos, "too_far")
        }

        val particleIota = args[3]
        if (particleIota !is StringIota) {
            throw MishapInvalidIota.ofType(particleIota, argc - 4, "string")
        }
        val particle = ParticleSpec.parse(particleIota, argc - 4)

        // 基础消耗 0.01 紫水晶粉，每多一个粒子再加 0.0001 紫水晶粉
        val cost = MediaConstants.DUST_UNIT / 100 + (amount - 1) * (MediaConstants.DUST_UNIT / 10_000)
        return SpellAction.Result(
            Spell(pos, spread, amount, particle),
            cost,
            listOf(ParticleSpray.burst(pos, 1.0))
        )
    }

    private data class Spell(
        val pos: Vec3,
        val spread: Vec3,
        val count: Int,
        val particle: ParticleOptions
    ) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            env.world.sendParticles(
                particle,
                pos.x, pos.y, pos.z,
                count,
                spread.x, spread.y, spread.z,
                0.0
            )
        }
    }
}
