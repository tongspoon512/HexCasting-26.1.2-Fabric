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
import kotlin.math.ceil

/**
 * 沿三次贝塞尔曲线生成粒子。
 * 四个控制点决定曲线，采样数决定点的密度。
 */
object OpSpawnBezierParticles : SpellAction {
    override val argc = 6

    private const val MAX_SAMPLES = 128
    private const val MAX_CONTROL_LENGTH = 64.0

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val p0 = args.getVec3(0, argc)
        val p1 = args.getVec3(1, argc)
        val p2 = args.getVec3(2, argc)
        val p3 = args.getVec3(3, argc)
        val samples = args.getDouble(4, argc)

        for (point in listOf(p0, p1, p2, p3)) env.assertVecInRange(point)

        val amount = samples.toInt()
        if (samples != amount.toDouble() || amount < 2 || amount > MAX_SAMPLES) {
            throw MishapInvalidIota.of(args[4], argc - 5, "particle_count")
        }

        val controlLength = p0.distanceTo(p1) + p1.distanceTo(p2) + p2.distanceTo(p3)
        if (controlLength > MAX_CONTROL_LENGTH) {
            throw MishapBadLocation(p0, "curve_too_long")
        }

        val particleIota = args[5]
        if (particleIota !is StringIota) {
            throw MishapInvalidIota.ofType(particleIota, argc - 6, "string")
        }
        val particle = ParticleSpec.parse(particleIota, argc - 6)

        val dust = ceil(amount / 8.0).toLong() * MediaConstants.DUST_UNIT
        return SpellAction.Result(
            Spell(p0, p1, p2, p3, amount, particle),
            MediaConstants.DUST_UNIT + dust,
            listOf(ParticleSpray.burst(p0, 1.0))
        )
    }

    private data class Spell(
        val p0: Vec3,
        val p1: Vec3,
        val p2: Vec3,
        val p3: Vec3,
        val samples: Int,
        val particle: ParticleOptions
    ) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            val last = samples - 1
            for (i in 0..last) {
                val t = i.toDouble() / last
                val s = 1.0 - t
                val point = p0.scale(s * s * s)
                    .add(p1.scale(3.0 * s * s * t))
                    .add(p2.scale(3.0 * s * t * t))
                    .add(p3.scale(t * t * t))
                env.world.sendParticles(
                    particle,
                    point.x, point.y, point.z,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
                )
            }
        }
    }
}
