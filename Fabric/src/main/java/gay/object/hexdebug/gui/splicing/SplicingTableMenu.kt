package gay.`object`.hexdebug.gui.splicing

import at.petrak.hexcasting.fabric.network.FabricPacketHandler
import gay.`object`.hexdebug.HexDebugRegistry
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity.Companion.CLIPBOARD_SLOT
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity.Companion.DATA_SIZE
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity.Companion.LIST_SLOT
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity.Companion.MEDIA_SLOT
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity.Companion.SLOT_COUNT
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity.Companion.SELECTION_FROM
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity.Companion.SELECTION_TO
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity.Companion.STAFF_SLOT
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity.Companion.VIEW_START_INDEX
import gay.`object`.hexdebug.gui.FilteredSlot
import gay.`object`.hexdebug.networking.MsgSplicingTableGetDataC2S
import gay.`object`.hexdebug.networking.MsgSplicingTableNewDataS2C
import gay.`object`.hexdebug.splicing.Selection
import gay.`object`.hexdebug.splicing.SplicingTableClientView
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.ContainerListener
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

class SplicingTableMenu private constructor(
    containerId: Int,
    inventory: Inventory,
    val table: SplicingTableBlockEntity?,
    private val data: ContainerData,
) : AbstractContainerMenu(HexDebugRegistry.SPLICING_TABLE_MENU, containerId) {

    /** Client-side constructor, used through the [net.minecraft.world.inventory.MenuType] supplier. */
    constructor(containerId: Int, inventory: Inventory) : this(
        containerId, inventory, null, SimpleContainerData(DATA_SIZE)
    )

    /** Server-side constructor, called from [SplicingTableBlockEntity.createMenu]. */
    internal constructor(
        containerId: Int,
        inventory: Inventory,
        table: SplicingTableBlockEntity,
    ) : this(containerId, inventory, table, table.containerData)

    private val container: Container = table ?: SimpleContainer(SLOT_COUNT)

    val selection: Selection?
        get() = Selection.fromRawIndices(data.get(SELECTION_FROM), data.get(SELECTION_TO))

    val viewStartIndex: Int get() = data.get(VIEW_START_INDEX)

    var clientView: SplicingTableClientView = SplicingTableClientView.empty()
        private set

    val staffSlot = FilteredSlot(
        container, STAFF_SLOT, -20, 169, SplicingTableBlockEntity::isValidStaff, maxStack = 1
    )

    init {
        addSlot(FilteredSlot(container, LIST_SLOT, 88, 68, SplicingTableBlockEntity::isValidList))
        addSlot(FilteredSlot(container, CLIPBOARD_SLOT, 7, 68, SplicingTableBlockEntity::isValidList))
        addSlot(FilteredSlot(container, MEDIA_SLOT, 205, 169, SplicingTableBlockEntity::isValidMedia))
        addSlot(staffSlot)

        for (y in 0 until 3) {
            for (x in 0 until 9) {
                addSlot(Slot(inventory, x + y * 9 + 9, 16 + x * 18, 111 + y * 18))
            }
        }
        for (x in 0 until 9) {
            addSlot(Slot(inventory, x, 16 + x * 18, 169))
        }

        addDataSlots(data)

        if (inventory.player.level().isClientSide) {
            // ask the server for the initial client view (the list contents aren't in the menu itself)
            FabricPacketHandler.sendToServer(MsgSplicingTableGetDataC2S())
        } else {
            addSlotListener(object : ContainerListener {
                override fun slotChanged(menu: AbstractContainerMenu, index: Int, stack: ItemStack) {
                    if (index == LIST_SLOT) {
                        val player = inventory.player as? ServerPlayer ?: return
                        table?.onListStackChanged()
                        sendData(player)
                    } else if (index == CLIPBOARD_SLOT) {
                        val player = inventory.player as? ServerPlayer ?: return
                        table?.onClipboardStackChanged()
                        sendData(player)
                    }
                }

                override fun dataChanged(menu: AbstractContainerMenu, index: Int, value: Int) {}
            })
        }
    }

    fun sendData(player: ServerPlayer) {
        table?.let {
            FabricPacketHandler.sendToPlayer(player, MsgSplicingTableNewDataS2C(it.getClientView()))
        }
    }

    fun receiveData(msg: MsgSplicingTableNewDataS2C) {
        clientView = msg.data
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        val slot = slots.getOrNull(index)?.takeIf { it.hasItem() } ?: return ItemStack.EMPTY
        val original = slot.item
        val copy = original.copy()

        if (index < SLOT_COUNT) {
            if (!moveItemStackTo(original, SLOT_COUNT, slots.size, true)) {
                return ItemStack.EMPTY
            }
        } else {
            if (!moveItemStackTo(original, 0, SLOT_COUNT, false)) {
                return ItemStack.EMPTY
            }
        }

        if (original.isEmpty) {
            slot.set(ItemStack.EMPTY)
        } else {
            slot.setChanged()
        }

        return copy
    }

    override fun stillValid(player: Player): Boolean = table?.stillValid(player) ?: true
}
