package at.petrak.hexcasting.common.casting.actions.spells.domain

import at.petrak.hexcasting.api.casting.area.AreaKind
import at.petrak.hexcasting.api.casting.area.AreaShape
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Particle outlines that mark a domain's boundary.
 *
 * Lines are sampled every [LINE_SPACING] blocks and every sample emits a small
 * cluster of particles, so the boundary stays readable even for large domains.
 */
object DomainVisuals {
    private const val LINE_SPACING = 0.43
    private const val MAX_LINE_STEPS = 96

    fun draw(level: ServerLevel, shape: AreaShape) {
        when (shape.kind) {
            // 火之领域只画自己的边界，太阳特效要等三日凌空施放后才出现
            AreaKind.TRIANGLE -> drawPolygon(level, shape)

            AreaKind.PENTAGRAM -> drawPolygon(level, shape)

            AreaKind.SPHERE -> drawSphere(level, shape.center, shape.radius)

            AreaKind.CUBOID -> drawCuboid(level, shape.points[0], shape.points[1])
        }
    }

    private fun drawPolygon(level: ServerLevel, shape: AreaShape) {
        val points = shape.points
        for (i in points.indices) {
            line(level, points[i], points[(i + 1) % points.size])
        }
        drawPillars(level, shape)
    }

    private fun drawSphere(level: ServerLevel, center: Vec3, radius: Double) {
        val circles = 8
        val steps = 35
        for (row in 0 until circles) {
            val theta = Math.PI * (row + 1) / (circles + 1)
            val y = center.y + radius * cos(theta)
            val ringRadius = radius * sin(theta)
            for (step in 0 until steps) {
                val angle = Math.PI * 2.0 * step / steps
                val x = center.x + ringRadius * cos(angle)
                val z = center.z + ringRadius * sin(angle)
                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.02, 0.02, 0.02, 0.0)
            }
        }
        // 两极也点一下，避免球顶球底看起来是空的
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + radius, center.z, 1, 0.15, 0.0, 0.15, 0.0)
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y - radius, center.z, 1, 0.15, 0.0, 0.15, 0.0)
    }

    private fun drawCuboid(level: ServerLevel, cornerA: Vec3, cornerB: Vec3) {
        val minX = minOf(cornerA.x, cornerB.x)
        val minY = minOf(cornerA.y, cornerB.y)
        val minZ = minOf(cornerA.z, cornerB.z)
        val maxX = maxOf(cornerA.x, cornerB.x)
        val maxY = maxOf(cornerA.y, cornerB.y)
        val maxZ = maxOf(cornerA.z, cornerB.z)
        val corners = listOf(
            Vec3(minX, minY, minZ), Vec3(maxX, minY, minZ),
            Vec3(maxX, minY, maxZ), Vec3(minX, minY, maxZ),
            Vec3(minX, maxY, minZ), Vec3(maxX, maxY, minZ),
            Vec3(maxX, maxY, maxZ), Vec3(minX, maxY, maxZ),
        )
        val edges = listOf(
            0 to 1, 1 to 2, 2 to 3, 3 to 0,
            4 to 5, 5 to 6, 6 to 7, 7 to 4,
            0 to 4, 1 to 5, 2 to 6, 3 to 7,
        )
        for ((a, b) in edges) {
            line(level, corners[a], corners[b])
        }
        for (corner in corners) {
            level.sendParticles(ParticleTypes.END_ROD, corner.x, corner.y, corner.z, 1, 0.08, 0.08, 0.08, 0.0)
        }
    }

    private fun drawPillars(level: ServerLevel, shape: AreaShape) {
        val depth = shape.extrusionDirection.scale(AreaShape.POLYGON_DEPTH)
        if (depth.lengthSqr() < 1e-6) {
            return
        }
        val outline = shape.outline
        // 竖直棱：标出领域向下延伸的 10 格
        for (point in outline) {
            val bottom = point.add(depth)
            line(level, point, bottom)
            level.sendParticles(ParticleTypes.END_ROD, bottom.x, bottom.y, bottom.z, 1, 0.08, 0.08, 0.08, 0.0)
        }
        // 底面轮廓：把覆盖范围的底部也框出来
        for (i in outline.indices) {
            val bottom = outline[i].add(depth)
            val nextBottom = outline[(i + 1) % outline.size].add(depth)
            line(level, bottom, nextBottom)
        }
    }

    private fun line(level: ServerLevel, from: Vec3, to: Vec3) {
        val length = from.distanceTo(to)
        val steps = max(2, ceil(length / LINE_SPACING).toInt().coerceAtMost(MAX_LINE_STEPS))
        for (step in 0..steps) {
            val point = from.lerp(to, step.toDouble() / steps)
            level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0.0)
        }
    }
}
