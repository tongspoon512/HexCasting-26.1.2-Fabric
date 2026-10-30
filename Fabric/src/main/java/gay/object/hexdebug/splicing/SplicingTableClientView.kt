package gay.`object`.hexdebug.splicing

import net.minecraft.nbt.CompoundTag

const val IOTA_BUTTONS = 9
const val VIEW_END_INDEX_OFFSET = IOTA_BUTTONS - 1

/**
 * The data the client needs in order to render the table. Sent over the network when the menu
 * opens (and whenever the table contents change).
 */
data class SplicingTableClientView(
    /** Serialized iotas, or null if the list slot does not contain a readable list. */
    val list: List<CompoundTag?>?,
    val clipboard: CompoundTag?,
    val isListWritable: Boolean,
    val isClipboardWritable: Boolean,
    val undoSize: Int,
    val undoIndex: Int,
) {
    val isListReadable get() = list != null

    val isClipboardReadable get() = clipboard != null

    val listSize get() = list?.size ?: 0

    val lastIndex get() = list?.lastIndex ?: -1

    fun isInRange(index: Int) = list?.let { index in it.indices } ?: false

    companion object {
        fun empty() = SplicingTableClientView(null, null, false, false, 0, -1)
    }
}
