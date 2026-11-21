package at.petrak.hexcasting.common.casting.actions.spells

import at.petrak.hexcasting.api.casting.area.AreaShape
import at.petrak.hexcasting.common.casting.actions.spells.domain.SpellFieldSave
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
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
import java.util.IdentityHashMap
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/**
 * 三日凌空生成的“白昼场”：三颗太阳悬在火之领域的三个角上，
 * 整个领域内都会融雪、烘干、催生并灼烧敌对怪物。全部效果由服务端每秒结算，
 * 不注册状态效果，也不修改世界时间。
 */
object ThreeSunsAdriftField {
    const val DURATION_TICKS = 20 * 60 * 20
    const val MAX_AREA = 400.0
    private const val EFFECT_INTERVAL = 20
    private const val PARTICLE_INTERVAL = 10
    private const val RAIN_CHECK_INTERVAL = 100
    private const val RAIN_EXTINGUISH_CHANCE = 0.25f
    private const val CROP_GROW_CHANCE = 0.2f

    private class Sun(val pos: Vec3, var alive: Boolean = true)

    private class Field(
        val level: ServerLevel,
        val shape: AreaShape,
        val corners: List<Vec3>,
        val minY: Int,
        val maxY: Int,
        val suns: List<Sun>,
        var ticksLeft: Int = DURATION_TICKS
    )

    private val fields = ArrayList<Field>()
    private val levelSaves = IdentityHashMap<ServerLevel, SpellFieldSave>()
    private val loadedLevels = IdentityHashMap<ServerLevel, Boolean>()

    fun cast(level: ServerLevel, a: Vec3, b: Vec3, c: Vec3) {
        this.ensureLoaded(level)
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
        val suns = corners.map { corner -> Sun(corner) }
        fields += newField(level, corners, suns, DURATION_TICKS)
        this.sync(level)
        draw(level, suns, corners)
    }

    fun tick(level: ServerLevel) {
        this.ensureLoaded(level)
        val iter = fields.iterator()
        while (iter.hasNext()) {
            val field = iter.next()
            if (field.level != level) continue

            field.ticksLeft--
            if (field.ticksLeft <= 0) {
                end(field, particles = true)
                iter.remove()
                this.sync(level)
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
        // 每秒同步一次剩余时间与太阳存活状态，退出世界后可以接着燃烧
        if (level.gameTime % 20L == 0L && fields.any { it.level === level }) {
            this.sync(level)
        }
    }

    fun clear() {
        for (level in this.fields.map { it.level }.distinct()) {
            this.sync(level)
        }
        for (field in fields) {
            end(field, particles = false)
        }
        fields.clear()
        this.levelSaves.clear()
        this.loadedLevels.clear()
    }

    private fun newField(level: ServerLevel, corners: List<Vec3>, suns: List<Sun>, ticksLeft: Int): Field {
        val depth = AreaShape.POLYGON_DEPTH
        val bottomY = corners.minOf { it.y } - depth
        return Field(
            level,
            AreaShape.triangle(corners),
            corners,
            floor(bottomY).toInt() - 1,
            ceil(corners.maxOf { it.y }).toInt() + 1,
            suns,
            ticksLeft
        )
    }

    private fun ensureLoaded(level: ServerLevel) {
        if (this.loadedLevels.containsKey(level)) {
            return
        }
        this.loadedLevels[level] = true
        val data = level.dataStorage.get(SpellFieldSave.TYPE) ?: return
        this.levelSaves[level] = data
        for (raw in data.suns) {
            val tag = raw as? CompoundTag ?: continue
            val field = this.loadField(level, tag) ?: continue
            this.fields += field
        }
    }

    private fun loadField(level: ServerLevel, tag: CompoundTag): Field? {
        val ticks = tag.getIntOr("ticks", 0)
        if (ticks <= 0) {
            return null
        }
        val a = readVec(tag, "a")
        val b = readVec(tag, "b")
        val c = readVec(tag, "c")
        val corners = listOf(a, b, c)
        val suns = corners.mapIndexed { index, corner ->
            Sun(corner, tag.getBooleanOr("sun$index", true))
        }
        return newField(level, corners, suns, ticks)
    }

    /** Writes this level's daylight fields into its saved data. */
    private fun sync(level: ServerLevel) {
        val data = this.levelSaves[level] ?: run {
            val storage = level.dataStorage
            val existing = storage.get(SpellFieldSave.TYPE) ?: SpellFieldSave()
            storage.set(SpellFieldSave.TYPE, existing)
            this.levelSaves[level] = existing
            existing
        }
        val list = ListTag()
        for (field in this.fields) {
            if (field.level !== level) {
                continue
            }
            val tag = CompoundTag()
            tag.put("a", vecTag(field.corners[0]))
            tag.put("b", vecTag(field.corners[1]))
            tag.put("c", vecTag(field.corners[2]))
            tag.putInt("ticks", field.ticksLeft)
            for ((index, sun) in field.suns.withIndex()) {
                tag.putBoolean("sun$index", sun.alive)
            }
            list.add(tag)
        }
        data.suns = list
        data.setDirty()
    }

    private fun vecTag(vec: Vec3): CompoundTag {
        val tag = CompoundTag()
        tag.putDouble("x", vec.x)
        tag.putDouble("y", vec.y)
        tag.putDouble("z", vec.z)
        return tag
    }

    private fun readVec(tag: CompoundTag, key: String): Vec3 {
        val inner = tag.getCompoundOrEmpty(key)
        return Vec3(
            inner.getDoubleOr("x", 0.0),
            inner.getDoubleOr("y", 0.0),
            inner.getDoubleOr("z", 0.0)
        )
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
        val minX = floor(field.corners.minOf { it.x }).toInt()
        val maxX = ceil(field.corners.maxOf { it.x }).toInt()
        val minZ = floor(field.corners.minOf { it.z }).toInt()
        val maxZ = ceil(field.corners.maxOf { it.z }).toInt()

        // 生物：整个领域内都会被灼烧，只有被屋顶挡住的才幸免
        val box = AABB(
            minX.toDouble(), field.minY.toDouble(), minZ.toDouble(),
            maxX + 1.0, field.maxY + 1.0, maxZ + 1.0
        )
        for (entity in level.getEntities(null, box) { it is LivingEntity && it.isAlive && !it.isSpectator }) {
            val living = entity as LivingEntity
            val pos = living.position()
            if (!field.shape.contains(pos)) continue
            val sun = nearestSun(pos, field)
            if (sun == null || !sun.alive) continue
            if (isBlocked(level, living.getEyePosition(), sun.pos)) continue
            if (living is Enemy) {
                living.igniteForSeconds(3.0f)
                living.hurtServer(level, level.damageSources().magic(), 6.0f)
            }
        }

        // 方块：从太阳向下找到每一列的第一个非空气方块。
        // 屋顶、隧道和上层方块会先被命中，从而替下方区域挡住阳光。
        val probeY = (field.minY + field.maxY) / 2.0
        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                if (!field.shape.contains(Vec3(x + 0.5, probeY, z + 0.5))) continue
                val columnCenter = Vec3(x + 0.5, probeY, z + 0.5)
                val sun = nearestSun(columnCenter, field) ?: continue
                if (!sun.alive) continue
                val top = minOf(field.maxY, ceil(sun.pos.y).toInt())
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
        val depth = AreaShape.POLYGON_DEPTH
        for ((index, sun) in suns.withIndex()) {
            if (!sun.alive) continue
            level.sendParticles(
                ParticleTypes.END_ROD,
                sun.pos.x, sun.pos.y, sun.pos.z,
                4, 0.3, 0.3, 0.3, 0.01
            )
            // 太阳就在角上，光柱沿领域深度向下打
            val bottom = corners[index].add(0.0, -depth, 0.0)
            for (step in 0..10) {
                val t = step / 10.0
                val p = sun.pos.lerp(bottom, t)
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
}
