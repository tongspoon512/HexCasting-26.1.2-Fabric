package at.petrak.hexcasting.common.casting.actions.math

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getNumOrVec
import at.petrak.hexcasting.api.casting.iota.Iota
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.sign

object OpCoerceToAxial : ConstMediaAction {
    override val argc: Int
        get() = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val value = args.getNumOrVec(0, argc)
        return value.map({ num ->
            num.sign.asActionResult
        }, { vec ->
            if (vec == Vec3.ZERO)
                vec.asActionResult
            else {
                // 26.1 dropped the double overload of Direction.getNearest; pick the dominant axis directly.
                val ax = abs(vec.x)
                val ay = abs(vec.y)
                val az = abs(vec.z)
                val dir = when {
                    ax >= ay && ax >= az -> if (vec.x >= 0) Direction.EAST else Direction.WEST
                    ay >= az -> if (vec.y >= 0) Direction.UP else Direction.DOWN
                    else -> if (vec.z >= 0) Direction.SOUTH else Direction.NORTH
                }
                dir.unitVec3.asActionResult
            }
        })
    }
}
