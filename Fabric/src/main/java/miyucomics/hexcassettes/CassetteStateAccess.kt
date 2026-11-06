package miyucomics.hexcassettes

import at.petrak.hexcasting.fabric.cc.HexCardinalComponents
import miyucomics.hexcassettes.data.CassetteState
import net.minecraft.server.level.ServerPlayer

/**
 * 读取玩家身上的卡带状态组件。
 */
val ServerPlayer.cassetteState: CassetteState
    get() = HexCardinalComponents.CASSETTES.get(this).state
