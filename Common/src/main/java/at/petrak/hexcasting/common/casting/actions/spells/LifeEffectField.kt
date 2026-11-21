package at.petrak.hexcasting.common.casting.actions.spells

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.animal.allay.Allay
import java.util.UUID

/**
 * 生命的节拍·兴奋的临时状态：记录每只悦灵的跳舞到期时间。
 * 悦灵每 tick 都会检查点唱机并可能自行停止跳舞，所以要每 10 tick 重新置为跳舞。
 * 只改内存，不写存档；服务器停止或实体消失时立即恢复。
 */
object LifeEffectField {
    private const val RETRY_INTERVAL = 10

    private class Entry(val level: ServerLevel, var ticksLeft: Int)

    private val dancing = HashMap<UUID, Entry>()

    fun excite(level: ServerLevel, allay: Allay, durationTicks: Int) {
        allay.isDancing = true
        dancing[allay.uuid] = Entry(level, durationTicks)
    }

    fun tick(level: ServerLevel) {
        if (dancing.isEmpty()) return

        val iter = dancing.iterator()
        while (iter.hasNext()) {
            val (id, entry) = iter.next()
            if (entry.level != level) continue

            val allay = level.getEntityInAnyDimension(id) as? Allay
            if (allay == null || !allay.isAlive) {
                iter.remove()
                continue
            }

            entry.ticksLeft--
            if (entry.ticksLeft <= 0) {
                allay.isDancing = false
                iter.remove()
            } else if (entry.ticksLeft % RETRY_INTERVAL == 0) {
                allay.isDancing = true
            }
        }
    }

    fun clear() {
        for ((id, entry) in dancing) {
            (entry.level.getEntityInAnyDimension(id) as? Allay)?.isDancing = false
        }
        dancing.clear()
    }
}
