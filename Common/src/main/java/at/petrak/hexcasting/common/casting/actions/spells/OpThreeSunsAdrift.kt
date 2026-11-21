package at.petrak.hexcasting.common.casting.actions.spells

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.area.AreaKind
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getArea
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadCaster
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.api.misc.MediaConstants
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil

/**
 * 三日凌空：在天顶法术的层级上，把一小片世界变成白昼。
 * 只接受火之领域，三颗太阳直接落在火之领域的三个角上，效果覆盖整个领域；
 * 被屋顶遮住或对应太阳熄灭的位置不受影响。法术需要施法者，不能由法术环或分析机施放。
 */
class OpThreeSunsAdrift : SpellAction {
    override val argc = 1

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        // 天顶法术需要“意识”来维持三颗太阳，解析机没有施法者
        env.castingEntity ?: throw MishapBadCaster()

        val areaIota = args.getArea(0, this.argc)
        if (areaIota.shape.kind != AreaKind.TRIANGLE) {
            throw MishapInvalidIota.of(args[0], 0, "area.fire")
        }
        val corners = areaIota.shape.points
        for (corner in corners) env.assertVecInRange(corner)

        val center = areaIota.shape.center
        val area = ThreeSunsAdriftField.areaXZ(corners[0], corners[1], corners[2])
        if (area < 1.0) throw MishapBadLocation(center, "degenerate")
        if (area > ThreeSunsAdriftField.MAX_AREA) throw MishapBadLocation(center, "too_large")

        val dust = ceil(area / 4.0).toLong() * MediaConstants.DUST_UNIT
        return SpellAction.Result(
            Spell(corners[0], corners[1], corners[2]),
            MediaConstants.CRYSTAL_UNIT + dust,
            corners.map { ParticleSpray.burst(it, 1.0, 30) }
        )
    }

    private data class Spell(val a: Vec3, val b: Vec3, val c: Vec3) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            ThreeSunsAdriftField.cast(env.world as ServerLevel, a, b, c)
        }
    }
}
