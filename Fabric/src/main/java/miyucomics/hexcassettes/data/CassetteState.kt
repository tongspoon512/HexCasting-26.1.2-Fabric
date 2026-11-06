package miyucomics.hexcassettes.data

import at.petrak.hexcasting.fabric.network.FabricPacketHandler
import miyucomics.hexcassettes.network.MsgCassetteSyncS2C
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer

/**
 * 玩家拥有的卡带状态。
 *
 * [owned] 是已食用的卡带数量（可用槽位数），[hexes] 是标签到排队咒术的映射。
 */
class CassetteState {
    var owned: Int = 0
    val hexes: MutableMap<String, QueuedHex> = LinkedHashMap()

    /** 上一次同步给客户端的忙碌槽位集合，用于检测变化。 */
    private var previousActiveSlots: Set<String> = emptySet()

    /**
     * 把当前槽位与标签同步给客户端。
     */
    fun sync(player: ServerPlayer) {
        FabricPacketHandler.sendToPlayer(player, MsgCassetteSyncS2C(owned, hexes.keys.toList()))
    }

    /**
     * 每 tick 递减所有排队项的延迟；归零的立即施放并释放槽位。
     */
    fun tick(player: ServerPlayer) {
        if (hexes.isNotEmpty()) {
            for (queued in hexes.values) {
                queued.delay -= 1
            }

            val expired = hexes.entries
                .filter { it.value.delay <= 0 }
                .map { it.key to it.value }

            for ((key, queued) in expired) {
                // 咒术可能在同一 tick 重新排入同一个标签，因此先移除再施放
                if (hexes[key] === queued) {
                    hexes.remove(key)
                }
                queued.cast(player, key)
            }
        }

        val activeSlots = hexes.keys.toSet()
        if (previousActiveSlots != activeSlots) {
            previousActiveSlots = activeSlots
            sync(player)
        }
    }

    fun serialize(): CompoundTag {
        val tag = CompoundTag()
        tag.putInt("owned", owned)

        val queuedTag = CompoundTag()
        for ((key, queued) in hexes) {
            queuedTag.put(key, queued.serialize())
        }
        tag.put("hexes", queuedTag)
        return tag
    }

    fun load(tag: CompoundTag) {
        owned = tag.getIntOr("owned", 0)
        hexes.clear()

        val queuedTag = tag.getCompoundOrEmpty("hexes")
        for (key in queuedTag.keySet()) {
            hexes[key] = QueuedHex.deserialize(queuedTag.getCompoundOrEmpty(key))
        }
    }
}
