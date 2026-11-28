package at.petrak.hexcasting.neoforge.addldata

import at.petrak.hexcasting.api.addldata.ADHexHolder
import at.petrak.hexcasting.api.addldata.ADIotaHolder
import at.petrak.hexcasting.api.addldata.ADMediaHolder
import at.petrak.hexcasting.api.addldata.ADVariantItem
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.item.HexHolderItem
import at.petrak.hexcasting.api.item.IotaHolderItem
import at.petrak.hexcasting.api.item.MediaHolderItem
import at.petrak.hexcasting.api.item.VariantItem
import at.petrak.hexcasting.api.pigment.FrozenPigment
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import java.util.function.Function
import java.util.function.Supplier

/**
 * Fabric 侧 CCA adimpl 的 NeoForge 等价实现：这些只是对物品接口的薄包装，不涉及附件。
 */

class NeoItemMediaHolder(private val stack: ItemStack) : ADMediaHolder {
    private val mediaHolder: MediaHolderItem = stack.item as? MediaHolderItem
        ?: error("item is not a media holder: $stack")

    override fun getMedia(): Long = mediaHolder.getMedia(stack)
    override fun getMaxMedia(): Long = mediaHolder.getMaxMedia(stack)
    override fun setMedia(media: Long) = mediaHolder.setMedia(stack, media)
    override fun canRecharge(): Boolean = mediaHolder.canRecharge(stack)
    override fun canProvide(): Boolean = mediaHolder.canProvideMedia(stack)
    override fun getConsumptionPriority(): Int = mediaHolder.getConsumptionPriority(stack)
    override fun canConstructBattery(): Boolean = false
    override fun withdrawMedia(cost: Long, simulate: Boolean): Long =
        mediaHolder.withdrawMedia(stack, cost, simulate)
    override fun insertMedia(amount: Long, simulate: Boolean): Long =
        mediaHolder.insertMedia(stack, amount, simulate)
}

class NeoStaticMediaHolder(
    private val baseWorth: Supplier<Long>,
    private val consumptionPriority: Int,
    private val stack: ItemStack
) : ADMediaHolder {
    override fun getMedia(): Long = baseWorth.get() * stack.count
    override fun getMaxMedia(): Long = getMedia()
    override fun setMedia(media: Long) {}
    override fun canRecharge(): Boolean = false
    override fun canProvide(): Boolean = true
    override fun getConsumptionPriority(): Int = consumptionPriority
    override fun canConstructBattery(): Boolean = true

    override fun withdrawMedia(cost: Long, simulate: Boolean): Long {
        val worth = baseWorth.get()
        val realCost = if (cost < 0) worth * stack.count else cost
        val itemsUsed = minOf(Math.ceil(realCost / worth.toDouble()).toInt(), stack.count)
        if (!simulate) {
            stack.shrink(itemsUsed)
        }
        return itemsUsed * worth
    }
}

class NeoItemIotaHolder(private val stack: ItemStack) : ADIotaHolder {
    private val iotaHolder: IotaHolderItem = stack.item as? IotaHolderItem
        ?: error("item is not a data holder: $stack")

    override fun readIotaTag(): CompoundTag? = iotaHolder.readIotaTag(stack)
    override fun writeable(): Boolean = iotaHolder.writeable(stack)

    override fun writeIota(iota: Iota?, simulate: Boolean): Boolean {
        if (!iotaHolder.canWrite(stack, iota)) {
            return false
        }
        if (!simulate) {
            iotaHolder.writeDatum(stack, iota)
        }
        return true
    }
}

class NeoStaticIotaHolder(
    private val stack: ItemStack,
    private val provider: Function<ItemStack, Iota?>
) : ADIotaHolder {
    override fun readIotaTag(): CompoundTag? {
        val iota = provider.apply(stack) ?: return null
        return IotaType.serialize(iota)
    }
    override fun writeable(): Boolean = false
    override fun writeIota(iota: Iota?, simulate: Boolean): Boolean = false
}

class NeoHexHolder(private val stack: ItemStack) : ADHexHolder {
    private val hexHolder: HexHolderItem = stack.item as? HexHolderItem
        ?: error("item is not a hex holder: $stack")

    override fun canDrawMediaFromInventory(): Boolean = hexHolder.canDrawMediaFromInventory(stack)
    override fun hasHex(): Boolean = hexHolder.hasHex(stack)
    override fun getHex(level: ServerLevel): List<Iota>? = hexHolder.getHex(stack, level)
    override fun writeHex(patterns: List<Iota>, pigment: FrozenPigment?, media: Long) =
        hexHolder.writeHex(stack, patterns, pigment, media)
    override fun clearHex() = hexHolder.clearHex(stack)
    override fun getPigment(): FrozenPigment? = hexHolder.getPigment(stack)
}

class NeoVariantItem(private val stack: ItemStack) : ADVariantItem {
    private val variantItem: VariantItem = stack.item as? VariantItem
        ?: error("item is not a variant item: $stack")

    override fun numVariants(): Int = variantItem.numVariants()
    override fun getVariant(): Int = variantItem.getVariant(stack)
    override fun setVariant(variant: Int) = variantItem.setVariant(stack, variant)
}
