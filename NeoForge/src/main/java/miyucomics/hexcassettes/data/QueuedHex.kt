package miyucomics.hexcassettes.data

import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import miyucomics.hexcassettes.CassetteCastEnv
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand

/**
 * 一盒正在等待施放的卡带。
 *
 * [depth] 记录该咒术是由第几层卡带排入的；直接由玩家排入时为 0。
 */
data class QueuedHex(
    val hex: CompoundTag,
    var delay: Int,
    val depth: Int,
) {
    fun cast(player: ServerPlayer, key: String) {
        // 与上游一致：主手有物品且副手为空时，视为从副手施放
        val hand = if (!player.mainHandItem.isEmpty && player.offhandItem.isEmpty) {
            InteractionHand.OFF_HAND
        } else {
            InteractionHand.MAIN_HAND
        }

        val vm = CastingVM.empty(CassetteCastEnv(player, hand, key, depth))
        val iota = IotaType.deserialize(hex, player.level())
        if (iota is ListIota) {
            vm.queueExecuteAndWrapIotas(iota.list.toList(), player.level())
        }
    }

    fun serialize(): CompoundTag {
        val tag = CompoundTag()
        tag.put("hex", hex)
        tag.putInt("delay", delay)
        tag.putInt("depth", depth)
        return tag
    }

    companion object {
        @JvmStatic
        fun deserialize(tag: CompoundTag): QueuedHex = QueuedHex(
            tag.getCompoundOrEmpty("hex"),
            tag.getIntOr("delay", 0),
            tag.getIntOr("depth", 0),
        )
    }
}
