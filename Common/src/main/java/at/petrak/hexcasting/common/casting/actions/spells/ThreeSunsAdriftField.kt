package at.petrak.hexcasting.common.casting.actions.spells

import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.BonemealableBlock
import net.minecraft.world.level.block.SnowLayerBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/**
 * 三日凌空生成的“白昼场”：三颗太阳分别负责离自己最近的顶点区域，
 * 在直射区域内融雪、烘干、催生并灼烧敌对怪物。全部效果由服务端每秒结算，
 * 不注册状态效果，也不修改世界时间。
 */
object ThreeSunsAdriftField {
    const val DURATION_TICKS = 20 * 60 * 20
    const val MAX_AREA = 400.0
    private const val SUN_HEIGHT = 10.0
    private const val EFFECT_INTERVAL = 20
    private const val PARTICLE_INTERVAL = 10
    private const val RAIN_CHECK_INTERVAL = 100
    private const val RAIN_EXTINGUISH_CHANCE = 0.25f
    private const val CROP_GROW_CHANCE = 0.2f

    private class Sun(val pos: Vec3, var alive: Boolean = true)

    private class Field(
        val level: ServerLevel,
        val a: Vec3,
        val b: Vec3,
        val c: Vec3,
        val corners: List<Vec3>,
        val minY: Int,
        val maxY: Int,
        val suns: List<Sun>,
        var ticksLeft: Int = DURATION_TICKS
    )

    private val fields = ArrayList<Field>()

    fun cast(level: ServerLevel, a: Vec3, b: Vec3, c: Vec3) {
        // 同一维度只保留最新的一片白昼
        val iter = fields.iterator()
        while (iter.hasNext()) {
            val old = iter.next()
            if (old.level == level) {
                end(old, particles = false)
                iter.remove()
            }
        }

        val corners = listOf(a, b, c)
        val suns = corners.map { corner -> Sun(corner.add(0.0, SUN_HEIGHT, 0.0)) }
        fields += Field(
            level, a, b, c, corners,
            floor(minOf(a.y, b.y, c.y) - 8.0).toInt(),
            ceil(maxOf(a.y, b.y, c.y) + 16.0).toInt(),
            suns
        )
        draw(level, suns, corners)
    }

    fun tick(level: ServerLevel) {
        val iter = fields.iterator()
        while (iter.hasNext()) {
            val field = iter.next()
            if (field.level != level) continue

            field.ticksLeft--
            if (field.ticksLeft <= 0) {
                end(field, particles = true)
                iter.remove()
                continue
            }

            if (field.ticksLeft % RAIN_CHECK_INTERVAL == 0) {
                checkRain(field)
            }
            if (field.ticksLeft % EFFECT_INTERVAL == 0) {
                checkBeams(field)
                applySunlight(field)
            }
            if (field.ticksLeft % PARTICLE_INTERVAL == 0) {
                draw(level, field.suns, field.corners)
            }
        }
    }

    fun clear() {
        for (field in fields) {
            end(field, particles = false)
        }
        fields.clear()
    }

    private fun checkRain(field: Field) {
        if (!field.level.isRaining) return
        // 只有暴露在雨中的太阳才会被雨水消磨，洞穴与屋顶下不受影响
        val alive = field.suns.filter {
            it.alive && field.level.isRainingAt(BlockPos.containing(it.pos))
        }
        if (alive.isEmpty()) return
        if (field.level.random.nextFloat() < RAIN_EXTINGUISH_CHANCE) {
            extinguish(field, alive[field.level.random.nextInt(alive.size)])
        }
    }

    private fun checkBeams(field: Field) {
        for (sun in field.suns) {
            if (!sun.alive) continue
            val hit = field.level.clip(
                ClipContext(
                    sun.pos,
                    Vec3(sun.pos.x, field.minY.toDouble(), sun.pos.z),
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.ANY,
                    CollisionContext.empty()
                )
            )
            if (hit.type == HitResult.Type.MISS) continue
            val state = field.level.getBlockState(hit.blockPos)
            if (state.block === Blocks.WATER || state.block === Blocks.POWDER_SNOW) {
                extinguish(field, sun)
            }
        }
    }

    private fun extinguish(field: Field, sun: Sun) {
        if (!sun.alive) return
        sun.alive = false
        field.level.sendParticles(
            ParticleTypes.LARGE_SMOKE,
            sun.pos.x, sun.pos.y, sun.pos.z,
            20, 0.4, 0.4, 0.4, 0.02
        )
    }

    private fun applySunlight(field: Field) {
        val level = field.level
        val minX = floor(minOf(field.a.x, field.b.x, field.c.x)).toInt()
        val maxX = ceil(maxOf(field.a.x, field.b.x, field.c.x)).toInt()
        val minZ = floor(minOf(field.a.z, field.b.z, field.c.z)).toInt()
        val maxZ = ceil(maxOf(field.a.z, field.b.z, field.c.z)).toInt()

        // 生物：只有直射阳光下的敌对怪物会着火
        val box = AABB(
            minX.toDouble(), field.minY.toDouble(), minZ.toDouble(),
            maxX + 1.0, field.maxY + 1.0, maxZ + 1.0
        )
        for (entity in level.getEntities(null, box) { it is LivingEntity && it.isAlive && !it.isSpectator }) {
            val living = entity as LivingEntity
            val pos = living.position()
            if (pos.y < field.minY || pos.y > field.maxY) continue
            if (!insideTriangleXZ(pos, field.a, field.b, field.c)) continue
            val sun = nearestSun(pos, field)
            if (sun == null || !sun.alive) continue
            if (isBlocked(level, living.getEyePosition(), sun.pos)) continue
            if (living is Enemy) {
                living.igniteForSeconds(3.0f)
                living.hurtServer(level, level.damageSources().magic(), 3.0f)
            }
        }

        // 方块：从太阳向下找到每一列的第一个非空气方块。
        // 屋顶、隧道和上层方块会先被命中，从而替下方区域挡住阳光。
        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                val columnCenter = Vec3(x + 0.5, field.a.y, z + 0.5)
                if (!insideTriangleXZ(columnCenter, field.a, field.b, field.c)) continue
                val sun = nearestSun(columnCenter, field) ?: continue
                if (!sun.alive) continue
                val top = minOf(field.maxY, floor(sun.pos.y).toInt() - 1)
                for (y in top downTo field.minY) {
                    val pos = BlockPos(x, y, z)
                    val state = level.getBlockState(pos)
                    if (state.isAir) continue
                    applyHeat(level, pos, state)
                    break
                }
            }
        }
    }

    private fun applyHeat(level: ServerLevel, pos: BlockPos, state: BlockState) {
        if (state.block is SnowLayerBlock) {
            val layers = state.getValue(SnowLayerBlock.LAYERS)
            level.setBlockAndUpdate(
                pos,
                if (layers > 1) state.setValue(SnowLayerBlock.LAYERS, layers - 1)
                else Blocks.AIR.defaultBlockState()
            )
            return
        }
        when (state.block) {
            Blocks.ICE, Blocks.FROSTED_ICE -> level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState())
            Blocks.POWDER_SNOW -> level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())
            Blocks.WET_SPONGE -> level.setBlockAndUpdate(pos, Blocks.SPONGE.defaultBlockState())
            else -> {
                if (state.block is BonemealableBlock && level.random.nextFloat() < CROP_GROW_CHANCE) {
                    state.randomTick(level, pos, level.random)
                }
            }
        }
    }

    private fun draw(level: ServerLevel, suns: List<Sun>, corners: List<Vec3>) {
        for ((index, sun) in suns.withIndex()) {
            if (!sun.alive) continue
            level.sendParticles(
                ParticleTypes.END_ROD,
                sun.pos.x, sun.pos.y, sun.pos.z,
                4, 0.3, 0.3, 0.3, 0.01
            )
            val corner = corners[index]
            for (step in 0..10) {
                val t = step / 10.0
                val p = sun.pos.lerp(corner, t)
                level.sendParticles(ParticleTypes.SMALL_FLAME, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0)
            }
        }
    }

    private fun end(field: Field, particles: Boolean) {
        for (sun in field.suns) {
            if (particles && sun.alive) {
                field.level.sendParticles(
                    ParticleTypes.END_ROD,
                    sun.pos.x, sun.pos.y, sun.pos.z,
                    30, 0.6, 0.6, 0.6, 0.05
                )
            }
        }
    }

    private fun nearestSun(pos: Vec3, field: Field): Sun? {
        var best: Sun? = null
        var bestDist = Double.MAX_VALUE
        for ((index, corner) in field.corners.withIndex()) {
            val dx = pos.x - corner.x
            val dz = pos.z - corner.z
            val dist = dx * dx + dz * dz
            if (dist < bestDist) {
                bestDist = dist
                best = field.suns[index]
            }
        }
        return best
    }

    private fun isBlocked(level: ServerLevel, from: Vec3, to: Vec3): Boolean {
        val hit = level.clip(
            ClipContext(
                from, to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()
            )
        )
        return hit.type != HitResult.Type.MISS
    }

    fun crossXZ(o: Vec3, p: Vec3, q: Vec3): Double =
        (p.x - o.x) * (q.z - o.z) - (p.z - o.z) * (q.x - o.x)

    fun areaXZ(a: Vec3, b: Vec3, c: Vec3): Double = abs(crossXZ(a, b, c)) / 2.0

    fun insideTriangleXZ(p: Vec3, a: Vec3, b: Vec3, c: Vec3): Boolean {
        val d1 = crossXZ(a, b, p)
        val d2 = crossXZ(b, c, p)
        val d3 = crossXZ(c, a, p)
        val hasNeg = d1 < 0.0 || d2 < 0.0 || d3 < 0.0
        val hasPos = d1 > 0.0 || d2 > 0.0 || d3 > 0.0
        return !(hasNeg && hasPos)
    }
}
