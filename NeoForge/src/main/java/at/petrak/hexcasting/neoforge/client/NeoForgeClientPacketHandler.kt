package at.petrak.hexcasting.neoforge.client

import at.petrak.hexcasting.neoforge.network.NeoForgePacketHandler
import net.neoforged.neoforge.network.handling.IPayloadHandler

/**
 * 仅在物理客户端加载：把下行载荷切回主线程后交给 Common 的客户端处理逻辑。
 */
object NeoForgeClientPacketHandler {
    val HANDLER = IPayloadHandler<NeoForgePacketHandler.HexPayload> { payload, ctx ->
        NeoForgePacketHandler.unwrap(payload)?.let { msg ->
            ctx.enqueueWork { NeoForgePacketHandler.dispatchClientbound(msg) }
        }
    }
}
