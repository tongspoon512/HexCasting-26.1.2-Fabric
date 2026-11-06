package miyucomics.hexcassettes.actions

import at.petrak.hexcasting.api.casting.ActionRegistryEntry
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.OperationResult
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.getPlayer
import at.petrak.hexcasting.api.casting.iota.DoubleIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.NullIota
import at.petrak.hexcasting.api.casting.math.EulerPathFinder
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.casting.mishaps.Mishap
import at.petrak.hexcasting.api.casting.mishaps.MishapBadCaster
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.api.casting.mishaps.MishapNotEnoughArgs
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import miyucomics.hexcassettes.CassetteCastEnv
import miyucomics.hexcassettes.HexCassettes
import miyucomics.hexcassettes.cassetteState
import miyucomics.hexcassettes.data.CassetteState
import miyucomics.hexcassettes.data.QueuedHex
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.DyeColor
import ram.talia.moreiotas.api.casting.iota.StringIota
import java.util.function.BiConsumer
import kotlin.math.roundToInt

private const val RANDOM_LABEL_SIGNATURE = "qeqwqwqwqwqeqaweqqqqqwweeweweewqdwwewewwewweweww"

/**
 * 将任意标签 iota 解析为字符串；标签在本移植版中使用 MoreIotas 的 StringIota。
 */
private fun List<Iota>.getString(idx: Int, argc: Int): String {
    val iota = getOrElse(idx) { throw MishapNotEnoughArgs(idx + 1, size) }
    if (iota is StringIota) return iota.string
    throw MishapInvalidIota.ofType(iota, argc - 1 - idx, "string")
}

private fun CastingEnvironment.requirePlayer(): ServerPlayer {
    val caster = this.castingEntity
    if (caster !is ServerPlayer) throw MishapBadCaster()
    return caster
}

/**
 * 排入一个延迟施放的咒术。
 *
 * 栈：`list, number` 或 `list, number, string`（标签位于栈顶）。
 */
object OpEnqueue : Action {
    override fun operate(
        env: CastingEnvironment,
        image: CastingImage,
        continuation: SpellContinuation,
    ): OperationResult {
        val caster = env.requirePlayer()
        if (image.stack.size < 2) throw MishapNotEnoughArgs(2, image.stack.size)

        val cassetteState = caster.cassetteState
        val stack = image.stack.toMutableList()

        var key = if (env is CassetteCastEnv && !cassetteState.hexes.containsKey(env.key)) {
            env.key
        } else {
            EulerPathFinder.findAltDrawing(
                HexPattern.fromAngles(RANDOM_LABEL_SIGNATURE, HexDir.EAST),
                env.world.server.tickCount.toLong(),
            ).anglesSignature()
        }

        var labelled = 0
        if (stack.last() is StringIota) {
            key = (stack.removeAt(stack.lastIndex) as StringIota).string
            labelled++
        }

        val potentialDelay = stack.removeAt(stack.lastIndex)
        if (potentialDelay !is DoubleIota ||
            potentialDelay.double - potentialDelay.double.roundToInt() > DoubleIota.TOLERANCE ||
            potentialDelay.double.roundToInt() <= 0
        ) {
            throw MishapInvalidIota.of(potentialDelay, labelled, "int.positive")
        }

        val potentialHex = stack.removeAt(stack.lastIndex)
        if (potentialHex !is ListIota) {
            throw MishapInvalidIota.ofType(potentialHex, labelled + 1, "list")
        }

        if (!cassetteState.hexes.containsKey(key) && cassetteState.hexes.keys.size >= cassetteState.owned) {
            throw NoFreeCassettes()
        }

        var depth = 0
        if (env is CassetteCastEnv && env.depth < 10) {
            depth = env.depth + 1
        }
        cassetteState.hexes[key] = QueuedHex(
            IotaType.serialize(potentialHex),
            potentialDelay.double.toInt(),
            depth,
        )

        if (labelled == 0) {
            stack.add(StringIota.makeUnchecked(key))
        }
        return OperationResult(image.copy(stack = stack), listOf(), continuation, HexEvalSounds.SPELL)
    }
}

class NoFreeCassettes : Mishap() {
    override fun accentColor(env: CastingEnvironment, errorCtx: Context) = dyeColor(DyeColor.RED)

    override fun errorMessage(env: CastingEnvironment, errorCtx: Context) =
        error(HexCassettes.MOD_ID + ":no_free_cassettes")

    override fun execute(env: CastingEnvironment, errorCtx: Context, stack: MutableList<Iota>) {}
}

/**
 * 取消指定标签的卡带。
 */
object OpDequeue : SpellAction {
    override val argc: Int = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val caster = env.requirePlayer()
        return SpellAction.Result(Spell(caster, args.getString(0, argc)), 0, listOf())
    }

    private data class Spell(val caster: ServerPlayer, val label: String) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            caster.cassetteState.hexes.remove(label)
        }
    }
}

/**
 * 清空所有卡带。
 */
object OpKillAll : SpellAction {
    override val argc: Int = 0

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val caster = env.requirePlayer()
        return SpellAction.Result(Spell(caster.cassetteState), 0, listOf())
    }

    private data class Spell(val state: CassetteState) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            state.hexes.clear()
        }
    }
}

/**
 * 返回已拥有的卡带槽位总数。
 */
object OpSpecs : ConstMediaAction {
    override val argc: Int = 0

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> =
        env.requirePlayer().cassetteState.owned.asActionResult
}

/**
 * 返回所有忙碌卡带的标签列表。
 */
object OpBusy : ConstMediaAction {
    override val argc: Int = 0

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> =
        env.requirePlayer().cassetteState.hexes.keys
            .map { StringIota.makeUnchecked(it) }
            .asActionResult
}

/**
 * 查询指定玩家是否持有指定标签的卡带。
 */
object OpInspect : ConstMediaAction {
    override val argc: Int = 2

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val player = args.getPlayer(0, argc)
        val label = args.getString(1, argc)
        return player.cassetteState.hexes.containsKey(label).asActionResult
    }
}

/**
 * 返回指定标签卡带的剩余延迟；不存在时返回 Null。
 */
object OpForetell : ConstMediaAction {
    override val argc: Int = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val queued = env.requirePlayer().cassetteState.hexes[args.getString(0, argc)]
        return queued?.delay?.asActionResult ?: listOf(NullIota())
    }
}

/**
 * 返回当前咒术所在的卡带标签；不在卡带中运行时返回 Null。
 */
object OpSelf : ConstMediaAction {
    override val argc: Int = 0

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> =
        if (env is CassetteCastEnv) {
            listOf(StringIota.makeUnchecked(env.key))
        } else {
            listOf(NullIota())
        }
}

/**
 * 图案注册表，保持上游 id 与笔画顺序。
 */
object HexCassetteActions {
    private val ACTIONS = LinkedHashMap<Identifier, ActionRegistryEntry>()

    @JvmStatic
    fun register(r: BiConsumer<ActionRegistryEntry, Identifier>) {
        for ((id, action) in ACTIONS) {
            r.accept(action, id)
        }
    }

    init {
        make("enqueue", "qeqwqwqwqwqeqaweqqqqqwweeweweewqdwwewewwewweweww", HexDir.EAST, OpEnqueue)
        make("dequeue", "eqeweweweweqedwqeeeeewwqqwqwqqweawwqwqwwqwwqwqww", HexDir.WEST, OpDequeue)
        make("killall", "eqeweweweweqedwqeeeeewwqqwqwqqw", HexDir.WEST, OpKillAll)
        make("specs", "qeqwqwqwqwqeqaweqqqqq", HexDir.EAST, OpSpecs)
        make("busy", "qeqwqwqwqwqeqaweqqqqqaww", HexDir.EAST, OpBusy)
        make("inspect", "eqeweweweweqedwqeeeee", HexDir.WEST, OpInspect)
        make("foretell", "eqeweweweweqedwqeeeeedww", HexDir.WEST, OpForetell)
        make("self", "qeqwqwqwqwqeqaweqqqqqwweeweweew", HexDir.EAST, OpSelf)
    }

    private fun make(name: String, signature: String, dir: HexDir, action: Action) {
        val id = HexCassettes.id(name)
        check(ACTIONS.put(id, ActionRegistryEntry(HexPattern.fromAngles(signature, dir), action)) == null) {
            "Typo? Duplicate id $name"
        }
    }
}
