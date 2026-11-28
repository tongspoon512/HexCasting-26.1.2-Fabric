package at.petrak.hexcasting.neoforge.client

import at.petrak.hexcasting.neoforge.network.NeoForgePacketHandler
import net.neoforged.neoforge.client.network.ClientPacketDistributor

/**
 * 客户端发送器。只在物理客户端加载，供 [NeoForgePacketHandler.sendToServer] 转发。
 */
object NeoForgeClientPacketSender {
    fun send(payload: NeoForgePacketHandler.HexPayload) {
        ClientPacketDistributor.sendToServer(payload)
    }
}
