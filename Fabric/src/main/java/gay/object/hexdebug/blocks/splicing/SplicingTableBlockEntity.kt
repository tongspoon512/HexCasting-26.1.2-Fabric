package gay.`object`.hexdebug.blocks.splicing

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.addldata.ADIotaHolder
import at.petrak.hexcasting.api.block.HexBlockEntity
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.HexDebugRegistry
import gay.`object`.hexdebug.gui.splicing.SplicingTableMenu
import gay.`object`.hexdebug.splicing.Selection
import gay.`object`.hexdebug.splicing.ReadWriteList
import gay.`object`.hexdebug.splicing.SplicingTableAction
import gay.`object`.hexdebug.splicing.SplicingTableClientView
import gay.`object`.hexdebug.splicing.SplicingTableData
import gay.`object`.hexdebug.splicing.UndoStack
import gay.`object`.hexdebug.splicing.VIEW_END_INDEX_OFFSET
import gay.`object`.hexdebug.splicing.Option
import gay.`object`.hexdebug.splicing.Option.Some
import net.minecraft.core.BlockPos
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import kotlin.math.max
import kotlin.math.min

class SplicingTableBlockEntity(pos: BlockPos, state: BlockState) :
    HexBlockEntity(HexDebugRegistry.SPLICING_TABLE_TILE, pos, state), Container, MenuProvider {

    private val stacks: NonNullList<ItemStack> = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY)
    val containerData = SimpleContainerData(DATA_SIZE)
    private val undoStack = UndoStack()

    val listHolder: ADIotaHolder? get() = IXplatAbstractions.INSTANCE.findDataHolder(getItem(LIST_SLOT))
    val clipboardHolder: ADIotaHolder? get() = IXplatAbstractions.INSTANCE.findDataHolder(getItem(CLIPBOARD_SLOT))

    val selection: Selection?
        get() = Selection.fromRawIndices(
            containerData.get(SELECTION_FROM),
            containerData.get(SELECTION_TO),
        )

    val viewStartIndex: Int get() = containerData.get(VIEW_START_INDEX)

    private fun setSelection(value: Selection?) {
        containerData.set(SELECTION_FROM, value?.from ?: -1)
        containerData.set(SELECTION_TO, value?.to ?: -1)
    }

    private fun setViewStartIndex(value: Int) {
        containerData.set(VIEW_START_INDEX, value)
    }

    // ---------------------------------------------------------------- client view

    fun getClientView(): SplicingTableClientView {
        val level = level as? ServerLevel ?: return SplicingTableClientView.empty()
        val list = (listHolder?.readIota(level) as? ListIota)?.list
            ?.map { iota -> serializeIota(iota) }
        return SplicingTableClientView(
            list = list,
            clipboard = clipboardHolder?.readIota(level)?.let { serializeIota(it) },
            isListWritable = listHolder?.writeable() == true,
            isClipboardWritable = clipboardHolder?.writeable() == true,
            undoSize = undoStack.size,
            undoIndex = undoStack.index,
        )
    }

    private fun serializeIota(iota: Iota): CompoundTag? = try {
        IotaType.serialize(iota)
    } catch (e: Exception) {
        HexDebug.LOGGER.warn("Failed to serialize iota for splicing table client view", e)
        null
    }

    // ---------------------------------------------------------------- interaction

    fun selectIndex(index: Int, hasShiftDown: Boolean, isIota: Boolean) {
        val list = getList() ?: return
        if (isIota) {
            if (index in list.indices) selectIota(list, index, hasShiftDown)
        } else {
            if (isEdgeInRange(list, index)) selectEdge(list, index, hasShiftDown)
        }
        clampView(list)
    }

    private fun selectIota(list: List<Iota>, index: Int, hasShiftDown: Boolean) {
        val current = selection
        setSelection(
            when {
                current != null && current.size == 1 && current.from == index -> null
                hasShiftDown && current != null -> {
                    if (current is Selection.Edge && index < current.from) {
                        Selection.of(current.from - 1, index)
                    } else {
                        Selection.of(current.from, index)
                    }
                }
                else -> Selection.withSize(index, 1)
            }
        )
    }

    private fun selectEdge(list: List<Iota>, index: Int, hasShiftDown: Boolean) {
        val current = selection
        setSelection(
            when {
                current is Selection.Edge && current.start == index -> null
                hasShiftDown && current != null -> {
                    if (current is Selection.Edge && index < current.from) {
                        Selection.of(current.from - 1, index)
                    } else if (index > current.from) {
                        Selection.of(current.from, index - 1)
                    } else {
                        Selection.of(current.from, index)
                    }
                }
                else -> Selection.edge(index)
            }
        )
    }

    fun scrollView(delta: Int) {
        val list = getList()
        val maxStart = if (list != null) max(0, list.lastIndex - VIEW_END_INDEX_OFFSET) else 0
        setViewStartIndex((viewStartIndex + delta).coerceIn(0, maxStart))
    }

    fun onListStackChanged() {
        if (getItem(LIST_SLOT).isEmpty) {
            undoStack.clear()
            setSelection(null)
            setViewStartIndex(0)
        } else {
            clampView(getList())
        }
    }

    fun onClipboardStackChanged() {
        if (getItem(CLIPBOARD_SLOT).isEmpty) {
            undoStack.stack.apply {
                val newStack = mapNotNull { entry ->
                    entry.copy(clipboard = Option.None()).takeIf { it.isNotEmpty }
                }
                clear()
                addAll(newStack)
                undoStack.index = min(undoStack.index, lastIndex)
            }
        }
    }

    // ---------------------------------------------------------------- actions

    fun runAction(action: SplicingTableAction, player: ServerPlayer?) {
        val data = getData(player) ?: return
        clampView(data.list)
        setupUndoStack(data)
        convertAndRun(action.value, data)
    }

    private fun getData(player: ServerPlayer?): SplicingTableData? {
        val level = level as? ServerLevel ?: return null
        return SplicingTableData(
            player = player,
            level = level,
            undoStack = undoStack,
            selection = selection,
            viewStartIndex = viewStartIndex,
            listHolder = listHolder,
            clipboardHolder = clipboardHolder,
        )
    }

    private fun setupUndoStack(data: SplicingTableData) {
        if (undoStack.size == 0) {
            data.pushUndoState(
                list = Some(data.list.orEmpty()),
                clipboard = Some(data.clipboard),
                selection = Some(selection),
            )
        }
    }

    private fun <T : SplicingTableData> convertAndRun(
        actionValue: SplicingTableAction.Value<T>,
        data: SplicingTableData,
    ) {
        val converted = actionValue.convert(data) ?: return
        actionValue.run(converted)
        postRunAction(converted)
    }

    private fun postRunAction(data: SplicingTableData) {
        setSelection(data.selection)
        setViewStartIndex(data.viewStartIndex)
        clampView(data.list)
    }

    /** Called when a pattern is drawn in the embedded staff grid; writes it at the current selection. */
    fun drawPattern(player: ServerPlayer?, pattern: HexPattern): ResolvedPatternType {
        val data = getData(player)?.let { ReadWriteList.convertOrNull(it) }
            ?: return ResolvedPatternType.ERRORED
        clampView(data.list)
        setupUndoStack(data)
        val result = drawPattern(pattern, data)
        postRunAction(data)
        return result
    }

    private fun drawPattern(pattern: HexPattern, data: ReadWriteList): ResolvedPatternType = data.run {
        typedSelection.mutableSubList(list).apply {
            clear()
            add(PatternIota(pattern))
        }
        if (writeList(list)) {
            selection = Selection.edge(typedSelection.start + 1)?.also { makeEdgeVisible(it) }
            pushUndoState(
                list = Some(list),
                selection = Some(selection),
            )
            ResolvedPatternType.ESCAPED
        } else {
            ResolvedPatternType.ERRORED
        }
    }

    private fun getList(): List<Iota>? {
        val level = level as? ServerLevel ?: return null
        return (listHolder?.readIota(level) as? ListIota)?.list?.toList()
    }

    private fun isEdgeInRange(list: List<Iota>, index: Int) =
        index in list.indices
            || (index - 1) in list.indices
            || (index == 0 && list.isEmpty())

    private fun clampView(list: List<Iota>?) {
        if (list != null) {
            val maxStart = max(0, list.lastIndex - VIEW_END_INDEX_OFFSET)
            when (val current = selection) {
                is Selection.Range -> if (current.end !in list.indices) setSelection(null)
                is Selection.Edge -> if (!isEdgeInRange(list, current.index)) setSelection(null)
                null -> {}
            }
            setViewStartIndex(viewStartIndex.coerceIn(0, maxStart))
        } else {
            setSelection(null)
            setViewStartIndex(0)
        }
    }

    // ---------------------------------------------------------------- Container

    override fun getContainerSize() = SLOT_COUNT

    override fun isEmpty() = stacks.all { it.isEmpty }

    override fun getItem(index: Int): ItemStack = stacks[index]

    override fun removeItem(index: Int, count: Int): ItemStack = ContainerHelper.removeItem(stacks, index, count)

    override fun removeItemNoUpdate(index: Int): ItemStack = ContainerHelper.takeItem(stacks, index)

    override fun setItem(index: Int, stack: ItemStack) {
        stacks[index] = stack
        if (stack.count > maxStackSize) {
            stack.count = maxStackSize
        }
        setChanged()
        if (index == LIST_SLOT) {
            onListStackChanged()
        }
    }

    override fun clearContent() {
        stacks.clear()
    }

    override fun canPlaceItem(index: Int, stack: ItemStack): Boolean = when (index) {
        LIST_SLOT, CLIPBOARD_SLOT -> isValidList(stack)
        MEDIA_SLOT -> isValidMedia(stack)
        STAFF_SLOT -> isValidStaff(stack)
        else -> true
    }

    override fun stillValid(player: Player) = Container.stillValidBlockEntity(this, player)

    // ---------------------------------------------------------------- MenuProvider

    override fun createMenu(containerId: Int, inventory: Inventory, player: Player) =
        SplicingTableMenu(containerId, inventory, this)

    override fun getDisplayName(): Component = Component.translatable(blockState.block.descriptionId)

    // ---------------------------------------------------------------- persistence

    override fun saveModData(tag: CompoundTag) {}

    override fun loadModData(tag: CompoundTag) {}

    override fun saveAdditional(output: ValueOutput) {
        super.saveAdditional(output)
        ContainerHelper.saveAllItems(output, stacks)
        output.putInt("selectionFrom", selection?.from ?: -1)
        output.putInt("selectionTo", selection?.to ?: -1)
        output.putInt("viewStartIndex", viewStartIndex)
    }

    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input)
        ContainerHelper.loadAllItems(input, stacks)
        setSelection(
            Selection.fromRawIndices(
                input.getIntOr("selectionFrom", -1),
                input.getIntOr("selectionTo", -1),
            )
        )
        setViewStartIndex(input.getIntOr("viewStartIndex", 0))
    }

    companion object {
        const val LIST_SLOT = 0
        const val CLIPBOARD_SLOT = 1
        const val MEDIA_SLOT = 2
        const val STAFF_SLOT = 3
        const val SLOT_COUNT = 4

        const val SELECTION_FROM = 0
        const val SELECTION_TO = 1
        const val VIEW_START_INDEX = 2
        const val DATA_SIZE = 3

        fun isValidList(stack: ItemStack): Boolean =
            !stack.isEmpty && IXplatAbstractions.INSTANCE.findDataHolder(stack) != null

        fun isValidMedia(stack: ItemStack): Boolean =
            !stack.isEmpty && HexAPI.instance().findMediaHolder(stack) != null

        fun isValidStaff(stack: ItemStack): Boolean =
            !stack.isEmpty && stack.`is`(HexTags.Items.STAVES)
    }
}
