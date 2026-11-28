package at.petrak.hexcasting.neoforge.network

import at.petrak.hexcasting.common.msgs.IMessage
import at.petrak.hexcasting.neoforge.client.NeoForgeClientPacketHandler
import at.petrak.hexcasting.common.msgs.MsgBeepS2C
import at.petrak.hexcasting.common.msgs.MsgCastParticleS2C
import at.petrak.hexcasting.common.msgs.MsgClearSpiralPatternsS2C
import at.petrak.hexcasting.common.msgs.MsgNewSpellPatternC2S
import at.petrak.hexcasting.common.msgs.MsgNewSpellPatternS2C
import at.petrak.hexcasting.common.msgs.MsgNewSpiralPatternsS2C
import at.petrak.hexcasting.common.msgs.MsgNewWallScrollS2C
import at.petrak.hexcasting.common.msgs.MsgOpenSpellGuiS2C
import at.petrak.hexcasting.common.msgs.MsgPullClipboardS2C
import at.petrak.hexcasting.common.msgs.MsgPushClipboardC2S
import at.petrak.hexcasting.common.msgs.MsgRecalcWallScrollDisplayS2C
import at.petrak.hexcasting.common.msgs.MsgShiftScrollC2S
import gay.`object`.hexdebug.networking.MsgSplicingTableActionC2S
import gay.`object`.hexdebug.networking.MsgSplicingTableDrawPatternC2S
import gay.`object`.hexdebug.networking.MsgSplicingTableGetDataC2S
import gay.`object`.hexdebug.networking.MsgSplicingTableNewDataS2C
import gay.`object`.hexdebug.networking.MsgSplicingTableSelectC2S
import gay.`object`.hexdebug.networking.MsgSplicingTableViewC2S
import miyucomics.hexcassettes.network.MsgCassetteRemoveC2S
import miyucomics.hexcassettes.network.MsgCassetteSyncRequestC2S
import miyucomics.hexcassettes.network.MsgCassetteSyncS2C
import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.handling.IPayloadHandler

/**
 * NeoForge 网络层。
 *
 * 与 Fabric 侧一致：每条 [IMessage] 都以自身的 [IMessage.getFabricId] 作为 id，
 * 这里把它们统一塞进一条 `hexcasting:main` 通道，按 id 分派。
 */
object NeoForgePacketHandler {
    private val CHANNEL = Identifier.fromNamespaceAndPath("hexcasting", "main")
    val TYPE = CustomPacketPayload.Type<HexPayload>(CHANNEL)

    /** 一条通道 + 消息 id + 原始字节，避免为每条消息单独注册载荷类型。 */
    class HexPayload(val id: Identifier, val data: ByteArray) : CustomPacketPayload {
        override fun type(): CustomPacketPayload.Type<HexPayload> = TYPE
    }

    private val CODEC: StreamCodec<RegistryFriendlyByteBuf, HexPayload> =
        object : StreamCodec<RegistryFriendlyByteBuf, HexPayload> {
            override fun decode(buf: RegistryFriendlyByteBuf): HexPayload =
                HexPayload(buf.readIdentifier(), buf.readByteArray())

            override fun encode(buf: RegistryFriendlyByteBuf, value: HexPayload) {
                buf.writeIdentifier(value.id)
                buf.writeByteArray(value.data)
            }
        }

    private typealias Decoder = (FriendlyByteBuf) -> IMessage

    private val DECODERS: Map<Identifier, Decoder> = mapOf(
        MsgNewSpellPatternC2S.ID to { MsgNewSpellPatternC2S.deserialize(it) },
        MsgShiftScrollC2S.ID to { MsgShiftScrollC2S.deserialize(it) },
        MsgPushClipboardC2S.ID to { MsgPushClipboardC2S.deserialize(it) },
        MsgNewSpellPatternS2C.ID to { MsgNewSpellPatternS2C.deserialize(it) },
        MsgCastParticleS2C.ID to { MsgCastParticleS2C.deserialize(it) },
        MsgOpenSpellGuiS2C.ID to { MsgOpenSpellGuiS2C.deserialize(it) },
        MsgBeepS2C.ID to { MsgBeepS2C.deserialize(it) },
        MsgNewWallScrollS2C.ID to { MsgNewWallScrollS2C.deserialize(it) },
        MsgRecalcWallScrollDisplayS2C.ID to { MsgRecalcWallScrollDisplayS2C.deserialize(it) },
        MsgNewSpiralPatternsS2C.ID to { MsgNewSpiralPatternsS2C.deserialize(it) },
        MsgClearSpiralPatternsS2C.ID to { MsgClearSpiralPatternsS2C.deserialize(it) },
        MsgPullClipboardS2C.ID to { MsgPullClipboardS2C.deserialize(it) },
        MsgSplicingTableGetDataC2S.ID to { MsgSplicingTableGetDataC2S.deserialize(it) },
        MsgSplicingTableSelectC2S.ID to { MsgSplicingTableSelectC2S.deserialize(it) },
        MsgSplicingTableViewC2S.ID to { MsgSplicingTableViewC2S.deserialize(it) },
        MsgSplicingTableActionC2S.ID to { MsgSplicingTableActionC2S.deserialize(it) },
        MsgSplicingTableDrawPatternC2S.ID to { MsgSplicingTableDrawPatternC2S.deserialize(it) },
        MsgSplicingTableNewDataS2C.ID to { MsgSplicingTableNewDataS2C.deserialize(it) },
        MsgCassetteSyncS2C.ID to { MsgCassetteSyncS2C.deserialize(it) },
        MsgCassetteRemoveC2S.ID to { MsgCassetteRemoveC2S.deserialize(it) },
        MsgCassetteSyncRequestC2S.ID to { MsgCassetteSyncRequestC2S.deserialize(it) },
    )

    fun init(modBus: IEventBus) {
        modBus.addListener(RegisterPayloadHandlersEvent::class.java) { event ->
            val registrar = event.registrar("1")
            val serverHandler = IPayloadHandler<HexPayload> { payload, ctx -> handleServerbound(payload, ctx) }
            if (FMLEnvironment.getDist().isClient) {
                // 客户端额外注册下行处理；专用服务端不会加载这个类。
                registrar.playBidirectional(TYPE, CODEC, serverHandler, NeoForgeClientPacketHandler.HANDLER)
            } else {
                registrar.playToServer(TYPE, CODEC, serverHandler)
            }
        }
    }

    private fun decode(payload: HexPayload): IMessage? {
        val decoder = DECODERS[payload.id] ?: return null
        return decoder(FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data)))
    }

    private fun handleServerbound(payload: HexPayload, ctx: IPayloadContext) {
        val msg = decode(payload) ?: return
        ctx.enqueueWork {
            val player = ctx.player() as? ServerPlayer ?: return@enqueueWork
            val server = player.level().server
            when (msg) {
                is MsgNewSpellPatternC2S -> msg.handle(server, player)
                is MsgShiftScrollC2S -> msg.handle(server, player)
                is MsgPushClipboardC2S -> MsgPushClipboardC2S.handle(player, msg)
                is MsgSplicingTableGetDataC2S -> MsgSplicingTableGetDataC2S.handle(player)
                is MsgSplicingTableSelectC2S -> MsgSplicingTableSelectC2S.handle(player, msg)
                is MsgSplicingTableViewC2S -> MsgSplicingTableViewC2S.handle(player, msg)
                is MsgSplicingTableActionC2S -> MsgSplicingTableActionC2S.handle(player, msg)
                is MsgSplicingTableDrawPatternC2S -> MsgSplicingTableDrawPatternC2S.handle(player, msg)
                is MsgCassetteRemoveC2S -> MsgCassetteRemoveC2S.handle(player, msg)
                is MsgCassetteSyncRequestC2S -> MsgCassetteSyncRequestC2S.handle(player)
            }
        }
    }

    /** 由客户端包处理器调用，触发下行消息的客户端逻辑。 */
    fun dispatchClientbound(msg: IMessage) {
        when (msg) {
            is MsgNewSpellPatternS2C -> MsgNewSpellPatternS2C.handle(msg)
            is MsgCastParticleS2C -> MsgCastParticleS2C.handle(msg)
            is MsgOpenSpellGuiS2C -> MsgOpenSpellGuiS2C.handle(msg)
            is MsgBeepS2C -> MsgBeepS2C.handle(msg)
            is MsgNewWallScrollS2C -> MsgNewWallScrollS2C.handle(msg)
            is MsgRecalcWallScrollDisplayS2C -> MsgRecalcWallScrollDisplayS2C.handle(msg)
            is MsgNewSpiralPatternsS2C -> MsgNewSpiralPatternsS2C.handle(msg)
            is MsgClearSpiralPatternsS2C -> MsgClearSpiralPatternsS2C.handle(msg)
            is MsgPullClipboardS2C -> MsgPullClipboardS2C.handle(msg)
            is MsgSplicingTableNewDataS2C -> MsgSplicingTableNewDataS2C.handle(msg)
            is MsgCassetteSyncS2C -> MsgCassetteSyncS2C.handle(msg)
        }
    }

    fun unwrap(payload: HexPayload): IMessage? = decode(payload)

    fun wrap(message: IMessage): HexPayload {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        try {
            message.serialize(buf)
            val bytes = ByteArray(buf.readableBytes())
            buf.readBytes(bytes)
            return HexPayload(message.getFabricId(), bytes)
        } finally {
            buf.release()
        }
    }

    fun sendToPlayer(player: ServerPlayer, message: IMessage) {
        PacketDistributor.sendToPlayer(player, wrap(message))
    }

    /** 仅在物理客户端调用；实际的客户端发送器按需加载，避免服务端解析客户端类。 */
    fun sendToServer(message: IMessage) {
        at.petrak.hexcasting.neoforge.client.NeoForgeClientPacketSender.send(wrap(message))
    }

    fun sendPacketNear(pos: Vec3, radius: Double, level: ServerLevel, message: IMessage) {
        val packet = wrap(message)
        val r2 = radius * radius
        for (player in level.players()) {
            if (player.distanceToSqr(pos) <= r2) {
                PacketDistributor.sendToPlayer(player, packet)
            }
        }
    }

    fun sendPacketTracking(entity: net.minecraft.world.entity.Entity, message: IMessage) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, wrap(message))
    }

    @Suppress("UNCHECKED_CAST")
    fun toVanillaClientboundPacket(message: IMessage): Packet<ClientGamePacketListener> =
        ClientboundCustomPayloadPacket(wrap(message)) as Packet<ClientGamePacketListener>
}
