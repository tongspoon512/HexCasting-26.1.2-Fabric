package at.petrak.hexcasting.common.casting.actions.spells

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getVec3
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.misc.MediaConstants
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * 三日凌空：三个坐标围成的三角形内，所有生物受到一次灼烧打击。
 * 三个坐标既是三角形的顶点，也是三颗太阳的位置来源；法术不读取施法者，
 * 因此分析机与法术环都可以施放。
 */
class OpThreeSunsAdrift : SpellAction {
    override val argc = 3

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        val corners = listOf(
            args.getVec3(0, argc),
            args.getVec3(1, argc),
            args.getVec3(2, argc)
        )
        for (corner in corners) env.assertVecInRange(corner)

        val center = Vec3(
            (corners[0].x + corners[1].x + corners[2].x) / 3.0,
            (corners[0].y + corners[1].y + corners[2].y) / 3.0,
            (corners[0].z + corners[1].z + corners[2].z) / 3.0
        )
        // 按三角形覆盖面积计费：每 3 格² 消耗 1 粉，最低 1 粉
        val area = abs(crossXZ(corners[0], corners[1], corners[2])) / 2.0
        val dust = max(1L, ceil(area / 3.0).toLong())
        return SpellAction.Result(
            Spell(corners[0], corners[1], corners[2]),
            MediaConstants.DUST_UNIT * dust,
            listOf(ParticleSpray.burst(center, 4.0, 60))
        )
    }

    private data class Spell(val a: Vec3, val b: Vec3, val c: Vec3) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            val level = env.world
            drawTriangle(level, a, b, c)

            // 退化三角形不造成伤害，但仍保留粒子表现
            if (abs(crossXZ(a, b, c)) < 1e-4) return

            val minY = minOf(a.y, b.y, c.y) - 2.0
            val maxY = maxOf(a.y, b.y, c.y) + 8.0
            val box = AABB(
                minOf(a.x, b.x, c.x), minY, minOf(a.z, b.z, c.z),
                maxOf(a.x, b.x, c.x), maxY, maxOf(a.z, b.z, c.z)
            )

            val source = level.damageSources().magic()
            for (entity in level.getEntities(null, box) { it is LivingEntity && it.isAlive && !it.isSpectator }) {
                val living = entity as LivingEntity
                val pos = living.position()
                if (pos.y !in minY..maxY) continue
                if (!insideTriangleXZ(pos, a, b, c)) continue
                living.hurtServer(level, source, 8.0f)
                living.igniteForSeconds(8.0f)
            }
        }
    }

    companion object {
        private fun crossXZ(o: Vec3, p: Vec3, q: Vec3): Double =
            (p.x - o.x) * (q.z - o.z) - (p.z - o.z) * (q.x - o.x)

        private fun insideTriangleXZ(p: Vec3, a: Vec3, b: Vec3, c: Vec3): Boolean {
            val d1 = crossXZ(a, b, p)
            val d2 = crossXZ(b, c, p)
            val d3 = crossXZ(c, a, p)
            val hasNeg = d1 < 0.0 || d2 < 0.0 || d3 < 0.0
            val hasPos = d1 > 0.0 || d2 > 0.0 || d3 > 0.0
            return !(hasNeg && hasPos)
        }

        private fun drawTriangle(level: ServerLevel, a: Vec3, b: Vec3, c: Vec3) {
            for (corner in listOf(a, b, c)) {
                val sky = corner.add(0.0, 10.0, 0.0)
                level.sendParticles(
                    ParticleTypes.FLAME,
                    sky.x, sky.y, sky.z,
                    40, 0.5, 0.5, 0.5, 0.01
                )
                for (step in 0..12) {
                    val t = step / 12.0
                    val p = sky.lerp(corner, t)
                    level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0)
                }
            }
            for ((from, to) in listOf(a to b, b to c, c to a)) {
                for (step in 0..16) {
                    val t = step / 16.0
                    val p = from.lerp(to, t).add(0.0, 0.1, 0.0)
                    level.sendParticles(ParticleTypes.SMALL_FLAME, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0)
                }
            }
        }
    }
}
