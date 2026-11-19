package at.petrak.hexcasting.common.casting.actions.spells

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getVec3
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadCaster
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation
import at.petrak.hexcasting.api.misc.MediaConstants
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil

/**
 * 三日凌空：在天顶法术的层级上，把一小片世界变成白昼。
 * 三个向量既是三角形的顶点，也是三颗太阳的锚点；每颗太阳只照亮
 * 离自己最近的三分之一区域。法术需要施法者，不能由法术环或分析机施放。
 */
class OpThreeSunsAdrift : SpellAction {
    override val argc = 3

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        // 天顶法术需要“意识”来维持三颗太阳，解析机没有施法者
        env.castingEntity ?: throw MishapBadCaster()

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
        val area = ThreeSunsAdriftField.areaXZ(corners[0], corners[1], corners[2])
        if (area < 1.0) throw MishapBadLocation(center, "degenerate")
        if (area > ThreeSunsAdriftField.MAX_AREA) throw MishapBadLocation(center, "too_large")

        val dust = ceil(area / 4.0).toLong() * MediaConstants.DUST_UNIT
        return SpellAction.Result(
            Spell(corners[0], corners[1], corners[2]),
            MediaConstants.CRYSTAL_UNIT + dust,
            corners.map { ParticleSpray.burst(it.add(0.0, 10.0, 0.0), 1.0, 30) }
        )
    }

    private data class Spell(val a: Vec3, val b: Vec3, val c: Vec3) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            ThreeSunsAdriftField.cast(env.world as ServerLevel, a, b, c)
        }
    }
}
