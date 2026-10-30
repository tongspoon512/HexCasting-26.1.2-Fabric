package gay.`object`.hexdebug.networking

import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.common.msgs.IMessage
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.gui.splicing.SplicingTableMenu
import gay.`object`.hexdebug.splicing.SplicingTableAction
import gay.`object`.hexdebug.splicing.SplicingTableClientView
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer

class MsgSplicingTableGetDataC2S : IMessage {
    override fun getFabricId(): Identifier = ID

    override fun serialize(buf: FriendlyByteBuf) {}

    companion object {
        @JvmField
        val ID: Identifier = HexDebug.id("splicing_table/get_data")

        @JvmStatic
        fun deserialize(@Suppress("UNUSED_PARAMETER") buffer: ByteBuf) = MsgSplicingTableGetDataC2S()

        @JvmStatic
        fun handle(player: ServerPlayer) {
            (player.containerMenu as? SplicingTableMenu)?.sendData(player)
        }
    }
}

class MsgSplicingTableSelectC2S(
    val index: Int,
    val hasShiftDown: Boolean,
    val isIota: Boolean,
) : IMessage {
    override fun getFabricId(): Identifier = ID

    override fun serialize(buf: FriendlyByteBuf) {
        buf.writeVarInt(index)
        buf.writeBoolean(hasShiftDown)
        buf.writeBoolean(isIota)
    }

    companion object {
        @JvmField
        val ID: Identifier = HexDebug.id("splicing_table/select")

        @JvmStatic
        fun deserialize(buffer: ByteBuf): MsgSplicingTableSelectC2S {
            val buf = FriendlyByteBuf(buffer)
            return MsgSplicingTableSelectC2S(buf.readVarInt(), buf.readBoolean(), buf.readBoolean())
        }

        @JvmStatic
        fun handle(player: ServerPlayer, msg: MsgSplicingTableSelectC2S) {
            (player.containerMenu as? SplicingTableMenu)?.table
                ?.selectIndex(msg.index, msg.hasShiftDown, msg.isIota)
        }
    }
}

class MsgSplicingTableViewC2S(val delta: Int) : IMessage {
    override fun getFabricId(): Identifier = ID

    override fun serialize(buf: FriendlyByteBuf) {
        buf.writeVarInt(delta)
    }

    companion object {
        @JvmField
        val ID: Identifier = HexDebug.id("splicing_table/view")

        @JvmStatic
        fun deserialize(buffer: ByteBuf): MsgSplicingTableViewC2S {
            val buf = FriendlyByteBuf(buffer)
            return MsgSplicingTableViewC2S(buf.readVarInt())
        }

        @JvmStatic
        fun handle(player: ServerPlayer, msg: MsgSplicingTableViewC2S) {
            (player.containerMenu as? SplicingTableMenu)?.table?.scrollView(msg.delta)
        }
    }
}

class MsgSplicingTableActionC2S(val action: SplicingTableAction) : IMessage {
    override fun getFabricId(): Identifier = ID

    override fun serialize(buf: FriendlyByteBuf) {
        buf.writeVarInt(action.ordinal)
    }

    companion object {
        @JvmField
        val ID: Identifier = HexDebug.id("splicing_table/action")

        @JvmStatic
        fun deserialize(buffer: ByteBuf): MsgSplicingTableActionC2S {
            val buf = FriendlyByteBuf(buffer)
            val ordinal = buf.readVarInt()
            return MsgSplicingTableActionC2S(
                SplicingTableAction.entries.getOrNull(ordinal) ?: SplicingTableAction.SELECT_NONE
            )
        }

        @JvmStatic
        fun handle(player: ServerPlayer, msg: MsgSplicingTableActionC2S) {
            val menu = player.containerMenu as? SplicingTableMenu ?: return
            menu.table?.runAction(msg.action, player)
            menu.sendData(player)
        }
    }
}

class MsgSplicingTableDrawPatternC2S(val pattern: HexPattern) : IMessage {
    override fun getFabricId(): Identifier = ID

    override fun serialize(buf: FriendlyByteBuf) {
        buf.writeNbt(pattern.serializeToNBT())
    }

    companion object {
        @JvmField
        val ID: Identifier = HexDebug.id("splicing_table/draw_pattern")

        @JvmStatic
        fun deserialize(buffer: ByteBuf): MsgSplicingTableDrawPatternC2S {
            val buf = FriendlyByteBuf(buffer)
            return MsgSplicingTableDrawPatternC2S(HexPattern.fromNBT(buf.readNbt()!!))
        }

        @JvmStatic
        fun handle(player: ServerPlayer, msg: MsgSplicingTableDrawPatternC2S) {
            val menu = player.containerMenu as? SplicingTableMenu ?: return
            menu.table?.drawPattern(player, msg.pattern)
            menu.sendData(player)
        }
    }
}

class MsgSplicingTableNewDataS2C(val data: SplicingTableClientView) : IMessage {
    override fun getFabricId(): Identifier = ID

    override fun serialize(buf: FriendlyByteBuf) {
        val list = data.list
        buf.writeBoolean(list != null)
        if (list != null) {
            buf.writeVarInt(list.size)
            for (tag in list) {
                buf.writeBoolean(tag != null)
                if (tag != null) {
                    buf.writeNbt(tag)
                }
            }
        }
        buf.writeBoolean(data.clipboard != null)
        data.clipboard?.let { buf.writeNbt(it) }
        buf.writeBoolean(data.isListWritable)
        buf.writeBoolean(data.isClipboardWritable)
        buf.writeVarInt(data.undoSize)
        buf.writeVarInt(data.undoIndex)
    }

    companion object {
        @JvmField
        val ID: Identifier = HexDebug.id("splicing_table/new_data")

        @JvmStatic
        fun deserialize(buffer: ByteBuf): MsgSplicingTableNewDataS2C {
            val buf = FriendlyByteBuf(buffer)
            val list: List<CompoundTag?>? = if (buf.readBoolean()) {
                List(buf.readVarInt()) {
                    if (buf.readBoolean()) buf.readNbt() else null
                }
            } else {
                null
            }
            val clipboard: CompoundTag? = if (buf.readBoolean()) buf.readNbt() else null
            val isListWritable = buf.readBoolean()
            val isClipboardWritable = buf.readBoolean()
            val undoSize = buf.readVarInt()
            val undoIndex = buf.readVarInt()
            return MsgSplicingTableNewDataS2C(
                SplicingTableClientView(
                    list = list,
                    clipboard = clipboard,
                    isListWritable = isListWritable,
                    isClipboardWritable = isClipboardWritable,
                    undoSize = undoSize,
                    undoIndex = undoIndex,
                )
            )
        }

        @JvmStatic
        fun handle(msg: MsgSplicingTableNewDataS2C) {
            Minecraft.getInstance().execute {
                val screen = Minecraft.getInstance().screen
                if (screen is gay.`object`.hexdebug.gui.splicing.SplicingTableScreen) {
                    screen.receiveData(msg)
                }
            }
        }
    }
}
