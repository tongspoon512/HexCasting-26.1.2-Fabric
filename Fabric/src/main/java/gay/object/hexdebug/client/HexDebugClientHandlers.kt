package gay.`object`.hexdebug.client

import gay.`object`.hexdebug.gui.splicing.SplicingTableScreen
import gay.`object`.hexdebug.networking.MsgSplicingTableNewDataS2C
import net.minecraft.client.Minecraft

/**
 * 客户端专用的 S2C 消息处理逻辑，避免消息类本身引用 `net.minecraft.client.*`。
 */
object HexDebugClientHandlers {
    @JvmStatic
    fun receiveSplicingTableData(msg: MsgSplicingTableNewDataS2C) {
        Minecraft.getInstance().execute {
            val screen = Minecraft.getInstance().screen
            if (screen is SplicingTableScreen) {
                screen.receiveData(msg)
            }
        }
    }
}
