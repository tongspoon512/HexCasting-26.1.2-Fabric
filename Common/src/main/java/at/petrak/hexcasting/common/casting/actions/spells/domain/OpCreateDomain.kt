package at.petrak.hexcasting.common.casting.actions.spells.domain

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.area.AreaIota
import at.petrak.hexcasting.api.casting.area.AreaKind
import at.petrak.hexcasting.api.casting.area.AreaShape
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.getPositiveDouble
import at.petrak.hexcasting.api.casting.getVec3
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadCaster
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation
import at.petrak.hexcasting.api.misc.MediaConstants
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil

/**
 * 生得领域的四种生成图案。
 */
class OpCreateDomain(private val kind: AreaKind) : SpellAction {
    override val argc = when (this.kind) {
        AreaKind.PENTAGRAM -> 5
        AreaKind.TRIANGLE -> 3
        AreaKind.SPHERE -> 2
        AreaKind.CUBOID -> 2
    }

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        env.castingEntity as? ServerPlayer ?: throw MishapBadCaster()

        val rawShape = when (this.kind) {
            AreaKind.PENTAGRAM -> AreaShape.pentagram((0 until 5).map { args.getVec3(it, this.argc) })
            AreaKind.TRIANGLE -> AreaShape.triangle((0 until 3).map { args.getVec3(it, this.argc) })
            AreaKind.SPHERE -> {
                val center = args.getVec3(0, this.argc)
                val radius = args.getPositiveDouble(1, this.argc)
                if (radius > AreaShape.MAX_SPHERE_RADIUS) {
                    throw MishapBadLocation(center, "area_too_large")
                }
                AreaShape.sphere(center, radius)
            }
            AreaKind.CUBOID -> AreaShape.cuboid(args.getVec3(0, this.argc), args.getVec3(1, this.argc))
        }
        // 火/风领域向下延伸到固体方块，并取最高的延伸点作为统一底面
        val shape = when (this.kind) {
            AreaKind.PENTAGRAM, AreaKind.TRIANGLE -> rawShape.withDepth(groundDepth(env, rawShape))
            else -> rawShape
        }

        for (point in shape.points) {
            env.assertVecInRange(point)
        }
        if (shape.isDegenerate()) {
            throw MishapBadLocation(shape.center, "bad_shape")
        }
        // 新领域没有咒术列表，与已有非空领域重叠时必然被裁决抹除
        val level = env.world as? ServerLevel
        if (level != null && AreaField.overlapsLongerDomain(level, shape)) {
            throw MishapBadLocation(shape.center, "domain_overlapped")
        }

        val cost = 13L * MediaConstants.DUST_UNIT +
            ceil(shape.area() / 8.0).toLong() * MediaConstants.DUST_UNIT
        return SpellAction.Result(
            Spell(shape),
            cost,
            listOf(ParticleSpray.burst(shape.center, 1.5, 30))
        )
    }

    private class Spell(private val shape: AreaShape) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            // 领域只在 cast(env, image) 中创建，以便把领域 iota 压回栈顶
        }

        override fun cast(env: CastingEnvironment, image: CastingImage): CastingImage? {
            val caster = env.castingEntity as? ServerPlayer ?: return null
            val level = env.world as? ServerLevel ?: return null
            val id = AreaField.create(level, caster.uuid, this.shape)
            if (AreaField.get(id) == null) {
                // 极少数等长随机裁决中被抹除：不返回失效的领域 iota
                return image
            }
            DomainVisuals.draw(level, this.shape)
            return image.copy(stack = image.stack + AreaIota(this.shape, id))
        }
    }

    companion object {
        /** 向下探测固体方块的最大距离。 */
        private const val MAX_GROUND_DEPTH = 64.0

        /** 向下探测的步长。 */
        private const val GROUND_STEP = 0.25

        /**
         * 从每个轮廓顶点沿领域延伸方向向下探测固体方块；
         * 各顶点终点不在同一平面时，取最高的延伸点（即行进距离最短者）作为统一底面。
         */
        private fun groundDepth(env: CastingEnvironment, shape: AreaShape): Double {
            val level = env.world as? ServerLevel ?: return AreaShape.POLYGON_DEPTH
            val down = shape.extrusionDirection
            if (down.lengthSqr() < 1e-6) {
                return AreaShape.POLYGON_DEPTH
            }
            var best = Double.MAX_VALUE
            for (point in shape.outline) {
                var travelled = 0.0
                while (travelled <= MAX_GROUND_DEPTH) {
                    val sample = point.add(down.scale(travelled + GROUND_STEP))
                    val state = level.getBlockState(BlockPos.containing(sample.x, sample.y, sample.z))
                    if (!state.isAir && !state.liquid()) {
                        best = minOf(best, travelled)
                        break
                    }
                    travelled += GROUND_STEP
                }
            }
            return if (best == Double.MAX_VALUE) {
                AreaShape.POLYGON_DEPTH
            } else {
                best.coerceAtLeast(0.5)
            }
        }

        @JvmField
        val WIND = OpCreateDomain(AreaKind.PENTAGRAM)

        @JvmField
        val FIRE = OpCreateDomain(AreaKind.TRIANGLE)

        @JvmField
        val WATER = OpCreateDomain(AreaKind.SPHERE)

        @JvmField
        val EARTH = OpCreateDomain(AreaKind.CUBOID)
    }
}
