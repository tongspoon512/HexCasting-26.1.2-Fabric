package pub.pigeon.yggdyy.hexcreating.cast

import at.petrak.hexcasting.api.casting.ActionRegistryEntry
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.getInt
import at.petrak.hexcasting.api.casting.iota.DoubleIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.NullIota
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.casting.mishaps.MishapBadBlock
import at.petrak.hexcasting.api.misc.MediaConstants
import com.zurrtum.create.AllBlocks
import com.zurrtum.create.content.equipment.armor.BacktankBlockEntity
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlock
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlockEntity
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlockEntity.FuelType
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import pub.pigeon.yggdyy.hexcreating.HexCreatingCompat
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlocks
import pub.pigeon.yggdyy.hexcreating.blocks.train_gate.TrainGateCoreBlockEntity
import java.util.function.BiConsumer
import kotlin.math.abs
import kotlin.math.min

/**
 * All HexCreating patterns. Ported from upstream; only the Create API package and the
 * 26.1 iota API have been adapted. Protected Blaze Burner fields are reached through
 * a tiny reflection helper instead of a mixin.
 */
object HexCreatingActions {
    private val ACTIONS = LinkedHashMap<Identifier, ActionRegistryEntry>()

    fun register(r: BiConsumer<ActionRegistryEntry, Identifier>) {
        ACTIONS.forEach { (id, entry) -> r.accept(entry, id) }
    }

    init {
        add("kelvin_heat_gambit", "qadedadd", HexDir.SOUTH_WEST, OpKelvinHeatGambit())
        add("kelvin_super_heat_gambit", "qadedade", HexDir.SOUTH_WEST, OpKelvinSuperHeatGambit())
        add("kelvin_time_purification", "qadedadw", HexDir.SOUTH_WEST, OpKelvinTimePurification())
        add("kelvin_type_purification", "qadedadq", HexDir.SOUTH_WEST, OpKelvinTypePurification())
        add("pascal_time_purification", "qadedaed", HexDir.SOUTH_WEST, OpPascalTimePurification())
        add("kinetic_rotation_purification", "qadedaqd", HexDir.SOUTH_WEST, OpKineticRotationPurification())
        add("kinetic_network_stress_all_purification", "qadedaqe", HexDir.SOUTH_WEST,
            OpKineticNetworkStressAllPurification())
        add("kinetic_network_stress_used_purification", "qadedaqw", HexDir.SOUTH_WEST,
            OpKineticNetworkStressUsedPurification())
        add("kinetic_stress_purification", "qadedaqq", HexDir.SOUTH_WEST, OpKineticStressPurification())
        add("train_gate_set_up_gambit", "aadqwqwqwqwqwqqqaq", HexDir.SOUTH_WEST, OpTrainGateSetUpGambit())
    }

    private fun add(name: String, stroke: String, dir: HexDir, action: Action) {
        ACTIONS[HexCreatingCompat.id(name)] =
            ActionRegistryEntry(HexPattern.fromAngles(stroke, dir), action)
    }
}

private fun blazeBurnerAt(env: CastingEnvironment, pos: BlockPos): BlazeBurnerBlockEntity? {
    if (!env.world.getBlockState(pos).`is`(AllBlocks.BLAZE_BURNER)) {
        return null
    }
    return env.world.getBlockEntity(pos) as? BlazeBurnerBlockEntity
}

private fun field(entity: Any, name: String): java.lang.reflect.Field =
    entity.javaClass.getDeclaredField(name).apply { isAccessible = true }

private fun getRemainingBurnTime(e: BlazeBurnerBlockEntity): Int =
    field(e, "remainingBurnTime").getInt(e)

private fun setRemainingBurnTime(e: BlazeBurnerBlockEntity, value: Int) {
    field(e, "remainingBurnTime").setInt(e, value)
}

private fun getActiveFuel(e: BlazeBurnerBlockEntity): FuelType =
    field(e, "activeFuel").get(e) as FuelType

private fun setActiveFuel(e: BlazeBurnerBlockEntity, fuel: FuelType) {
    field(e, "activeFuel").set(e, fuel)
}

class OpKelvinHeatGambit : SpellAction {
    override val argc = 2

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val pos = args.getBlockPos(0, argc)
        val tick = abs(args.getInt(1, argc))
        val burner = blazeBurnerAt(env, pos)
            ?: throw MishapBadBlock(pos, Component.translatable("mishaps.need_blaze_burner"))
        val heat = BlazeBurnerBlock.getHeatLevelOf(env.world.getBlockState(pos))
        if (heat == BlazeBurnerBlock.HeatLevel.NONE || heat == BlazeBurnerBlock.HeatLevel.FADING) {
            throw MishapBadBlock(pos, Component.translatable("mishaps.need_blaze_burner_with_blaze"))
        }
        return SpellAction.Result(Spell(pos, tick), MediaConstants.DUST_UNIT / 20 * tick, listOf())
    }

    private data class Spell(val targetPos: BlockPos, val targetTick: Int) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            if (targetTick <= 0) return
            val e = env.world.getBlockEntity(targetPos) as? BlazeBurnerBlockEntity ?: return
            if (e.isCreative || getActiveFuel(e) == FuelType.SPECIAL) return
            setActiveFuel(e, FuelType.NORMAL)
            setRemainingBurnTime(e,
                min(BlazeBurnerBlockEntity.MAX_HEAT_CAPACITY, targetTick + getRemainingBurnTime(e)))
            e.updateBlockState()
        }
    }
}

class OpKelvinSuperHeatGambit : SpellAction {
    override val argc = 2

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val pos = args.getBlockPos(0, argc)
        val tick = abs(args.getInt(1, argc))
        blazeBurnerAt(env, pos)
            ?: throw MishapBadBlock(pos, Component.translatable("mishaps.need_blaze_burner"))
        val heat = BlazeBurnerBlock.getHeatLevelOf(env.world.getBlockState(pos))
        if (heat == BlazeBurnerBlock.HeatLevel.NONE || heat == BlazeBurnerBlock.HeatLevel.FADING) {
            throw MishapBadBlock(pos, Component.translatable("mishaps.need_blaze_burner_with_blaze"))
        }
        return SpellAction.Result(Spell(pos, tick), MediaConstants.DUST_UNIT / 20 * tick, listOf())
    }

    private data class Spell(val targetPos: BlockPos, val targetTick: Int) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            if (targetTick <= 0) return
            val e = env.world.getBlockEntity(targetPos) as? BlazeBurnerBlockEntity ?: return
            if (e.isCreative) return
            if (getActiveFuel(e) == FuelType.SPECIAL) {
                setRemainingBurnTime(e,
                    min(BlazeBurnerBlockEntity.MAX_HEAT_CAPACITY, getRemainingBurnTime(e) + targetTick))
            } else {
                setActiveFuel(e, FuelType.SPECIAL)
                setRemainingBurnTime(e, min(BlazeBurnerBlockEntity.MAX_HEAT_CAPACITY, targetTick))
            }
            e.updateBlockState()
        }
    }
}

class OpKelvinTimePurification : ConstMediaAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val pos = args.getBlockPos(0, argc)
        val e = blazeBurnerAt(env, pos) ?: return listOf(NullIota())
        return listOf(DoubleIota(getRemainingBurnTime(e) * 1.0))
    }
}

class OpKelvinTypePurification : ConstMediaAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val pos = args.getBlockPos(0, argc)
        val e = blazeBurnerAt(env, pos) ?: return listOf(NullIota())
        return listOf(DoubleIota(when (getActiveFuel(e)) {
            FuelType.SPECIAL -> 2.0
            FuelType.NORMAL -> 1.0
            else -> 0.0
        }))
    }
}

class OpPascalTimePurification : ConstMediaAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val pos = args.getBlockPos(0, argc)
        val state = env.world.getBlockState(pos)
        if (!state.`is`(AllBlocks.COPPER_BACKTANK) && !state.`is`(AllBlocks.NETHERITE_BACKTANK)) {
            return listOf(NullIota())
        }
        val e = env.world.getBlockEntity(pos) as? BacktankBlockEntity ?: return listOf(NullIota())
        return listOf(DoubleIota(e.airLevel * 1.0))
    }
}

open class OpKineticBasePurification : ConstMediaAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val pos = args.getBlockPos(0, argc)
        env.world.getBlockEntity(pos) as? KineticBlockEntity ?: return listOf(NullIota())
        return run(pos, env)
    }

    open fun run(targetPos: BlockPos, env: CastingEnvironment): List<Iota> = listOf()
}

class OpKineticRotationPurification : OpKineticBasePurification() {
    override fun run(targetPos: BlockPos, env: CastingEnvironment): List<Iota> {
        val e = env.world.getBlockEntity(targetPos) as KineticBlockEntity
        return listOf(DoubleIota(abs(e.speed * 1.0)))
    }
}

class OpKineticNetworkStressAllPurification : OpKineticBasePurification() {
    override fun run(targetPos: BlockPos, env: CastingEnvironment): List<Iota> {
        val e = env.world.getBlockEntity(targetPos) as KineticBlockEntity
        val network = e.orCreateNetwork ?: return listOf(DoubleIota(0.0))
        return listOf(DoubleIota(network.calculateCapacity() * 1.0))
    }
}

class OpKineticNetworkStressUsedPurification : OpKineticBasePurification() {
    override fun run(targetPos: BlockPos, env: CastingEnvironment): List<Iota> {
        val e = env.world.getBlockEntity(targetPos) as KineticBlockEntity
        val network = e.orCreateNetwork ?: return listOf(DoubleIota(0.0))
        return listOf(DoubleIota(network.calculateStress() * 1.0))
    }
}

class OpKineticStressPurification : OpKineticBasePurification() {
    override fun run(targetPos: BlockPos, env: CastingEnvironment): List<Iota> {
        val e = env.world.getBlockEntity(targetPos) as KineticBlockEntity
        return listOf(DoubleIota(
            (e.calculateAddedStressCapacity() - e.calculateStressApplied()) * 1.0 * abs(e.speed)))
    }
}

class OpTrainGateSetUpGambit : SpellAction {
    override val argc = 2

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val pos1 = args.getBlockPos(0, argc)
        val pos2 = args.getBlockPos(1, argc)
        if (!env.world.getBlockState(pos1).`is`(HexCreatingBlocks.TRAIN_GATE_CORE)) {
            throw MishapBadBlock(pos1, Component.translatable("mishaps.need_train_gate_core"))
        }
        if (!env.world.getBlockState(pos2).`is`(HexCreatingBlocks.TRAIN_GATE_CORE)) {
            throw MishapBadBlock(pos2, Component.translatable("mishaps.need_train_gate_core"))
        }
        val distance = (abs(pos1.x - pos2.x) + abs(pos1.y - pos2.y) + abs(pos1.z - pos2.z)).toLong()
        return SpellAction.Result(Spell(pos1, pos2), MediaConstants.DUST_UNIT / 10L * distance, listOf())
    }

    private data class Spell(val pos1: BlockPos, val pos2: BlockPos) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            val world = env.world
            val core1 = world.getBlockEntity(pos1) as? TrainGateCoreBlockEntity ?: return
            val core2 = world.getBlockEntity(pos2) as? TrainGateCoreBlockEntity ?: return
            core1.tryDestroyGate()
            core2.tryDestroyGate()
            if (core1.canSetUpGate() && core2.canSetUpGate()) {
                core1.setUpGate(pos2)
                core2.setUpGate(pos1)
            }
        }
    }
}
