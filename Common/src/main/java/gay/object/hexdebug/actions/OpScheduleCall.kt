package gay.`object`.hexdebug.actions

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.casting.SpellList
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.OperationResult
import at.petrak.hexcasting.api.casting.eval.env.CircleCastEnv
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.getInt
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.mishaps.Mishap
import at.petrak.hexcasting.api.casting.mishaps.MishapDisallowedSpell
import at.petrak.hexcasting.api.casting.mishaps.MishapEvalTooMuch
import at.petrak.hexcasting.api.casting.mishaps.MishapInternalException
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.api.mod.HexConfig
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.ext.SilencedCastingEnv
import net.minecraft.server.MinecraftServer
import java.util.ArrayDeque
import java.util.WeakHashMap

private const val USELESS_CALL_THRESHOLD = 10
private const val MAX_CALL_THRESHOLD = 100000

/**
 * Schedules a spell to run on a later tick, ported from HexOverpowered's {@code mind_env/schedule}.
 */
object OpScheduleCall : ConstMediaAction {

    private class SameTickCallCounter(private val limit: Int) {
        private var lastChangeAge = -1
        private var calledCount = 0

        fun update(now: Int) {
            if (now == lastChangeAge) {
                calledCount++
                if (calledCount >= limit) {
                    calledCount = 0
                    throw MishapEvalTooMuch()
                }
            } else {
                calledCount = 1
            }
            lastChangeAge = now
        }
    }

    private class TaskPool(val key: Any?) {
        val queue = ArrayDeque<Task>()

        fun cancel(): Boolean {
            val ret = queue.any { !it.executed }
            queue.clear()
            return ret
        }

        fun execute(tick: Int) {
            val peek = queue.peekFirst() ?: return
            if (peek.execute(tick)) {
                val newPeek = queue.peekFirst()
                if (peek === newPeek) queue.pollFirst()
            }
        }

        val counterNormal = SameTickCallCounter(MAX_CALL_THRESHOLD)
        val counterDumb = SameTickCallCounter(USELESS_CALL_THRESHOLD)
    }

    private class Task(
        val myAge: Int,
        val env: CastingEnvironment,
        val code: SpellList,
        val action: (now: Int) -> Unit,
    ) {
        var executed = false

        fun execute(tick: Int): Boolean {
            if (tick < myAge) return false
            try {
                action(tick)
            } catch (e: Exception) {
                val mishap = MishapInternalException(e)
                val msg = mishap.errorMessageWithName(env, Mishap.Context(null, null))
                if (env is CircleCastEnv) {
                    val impetus = env.impetus
                    if (msg != null && impetus != null) impetus.postPrint(msg)
                } else {
                    val caster = env.caster
                    if (msg != null && caster != null) caster.sendSystemMessage(msg)
                }
            }
            executed = true
            return true
        }
    }

    private val TaskMap = WeakHashMap<Any?, TaskPool>()

    // 26.1 no longer exposes the current CastingImage to execute(), so remember it in operate().
    private lateinit var myImage: CastingImage

    override val argc = 2

    override fun operate(
        env: CastingEnvironment,
        image: CastingImage,
        continuation: SpellContinuation,
    ): OperationResult {
        myImage = image
        return super.operate(env, image, continuation)
    }

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        if (!HexConfig.server().enablesMindEnvActions()) {
            throw MishapDisallowedSpell("disallowed", HexDebug.id("mind_env/schedule"))
        }

        val tickThisExec = env.world.server.tickCount
        val key = pickKeyFrom(env)
        val code = args[0].let { topIota ->
            if (topIota is ListIota) topIota.list
            else if (topIota.executable()) SpellList.LList(listOf(topIota))
            else throw MishapInvalidIota.of(topIota, 0, "evaluatable")
        }
        val delay = args.getInt(1)
        val ravenDataATM = myImage.userData.getCompound(HexAPI.RAVENMIND_USERDATA).orElse(null)
        val taskPool = TaskMap.computeIfAbsent(key, ::TaskPool)

        val action: (Int) -> Unit = { tick ->
            taskPool.counterNormal.update(tick)
            val vm = CastingVM.empty(SilencedCastingEnv.from(env))
            ravenDataATM?.let { vm.image.userData.put(HexAPI.RAVENMIND_USERDATA, it) }
            vm.queueExecuteAndWrapIotas(code.toList(), env.world)
        }

        // A newly scheduled call replaces whatever was pending for this caster.
        if (taskPool.cancel()) taskPool.counterDumb.update(tickThisExec)

        if (delay <= 0) {
            try {
                action.invoke(tickThisExec)
            } catch (e: StackOverflowError) {
                throw MishapEvalTooMuch()
            }
            return listOf()
        }

        taskPool.queue.addLast(Task(delay + tickThisExec, env, code, action))
        return listOf()
    }

    private fun pickKeyFrom(env: CastingEnvironment): Any? =
        if (env is CircleCastEnv) env.impetus else env.caster

    @JvmStatic
    fun ProcessQueue(server: MinecraftServer) {
        // Snapshot the pools: a scheduled spell may schedule more work (or clear its own pool) while
        // this loop is running.
        for (task in TaskMap.values.toList()) {
            task.execute(server.tickCount)
        }
    }

    @JvmStatic
    fun ResetQueue(server: MinecraftServer) {
        TaskMap.clear()
    }

    fun QueryScheduledCode(env: CastingEnvironment): SpellList? {
        val key = pickKeyFrom(env)
        val pool = TaskMap[key]
        return pool?.queue?.peekFirst()?.code
    }
}
