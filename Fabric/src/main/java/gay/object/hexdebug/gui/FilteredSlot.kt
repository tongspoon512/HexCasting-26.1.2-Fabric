package gay.`object`.hexdebug.gui

import net.minecraft.world.Container
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

/** A slot that only accepts items matching [predicate]. */
class FilteredSlot(
    container: Container,
    index: Int,
    x: Int,
    y: Int,
    private val predicate: (ItemStack) -> Boolean,
    private val maxStack: Int = 64,
) : Slot(container, index, x, y) {
    override fun mayPlace(stack: ItemStack): Boolean = predicate(stack)

    override fun getMaxStackSize(): Int = maxStack

    override fun getMaxStackSize(stack: ItemStack): Int = maxStack
}
