@file:JvmName("MediaHelper")

package at.petrak.hexcasting.api.utils

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.addldata.ADMediaHolder
import at.petrak.hexcasting.api.misc.DiscoveryHandlers
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.media.PersonalManaHolder
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.ARGB
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemStack
import kotlin.math.roundToInt

fun isMediaItem(stack: ItemStack): Boolean {
    val mediaHolder = IXplatAbstractions.INSTANCE.findMediaHolder(stack) ?: return false
    if (!mediaHolder.canProvide())
        return false
    return mediaHolder.withdrawMedia(-1, true) > 0
}

/**
 * Extract [cost] media from [stack]. If [cost] is less than zero, extract all media instead.
 * This may mutate [stack] (and may consume it) unless [simulate] is set.
 *
 * If [drainForBatteries] is true, this will only consider forms of media that can be used to make new batteries.
 *
 * Return the amount of media extracted. This may be over [cost] if media is wasted.
 */
@JvmOverloads
fun extractMedia(
    stack: ItemStack,
    cost: Long = -1,
    drainForBatteries: Boolean = false,
    simulate: Boolean = false
): Long {
    val mediaHolder = IXplatAbstractions.INSTANCE.findMediaHolder(stack) ?: return 0

    return extractMedia(mediaHolder, cost, drainForBatteries, simulate)
}

/**
 * Extract [cost] media from [holder]. If [cost] is less than zero, extract all media instead.
 * This may mutate the stack underlying [holder] (and may consume it) unless [simulate] is set.
 *
 * If [drainForBatteries] is true, this will only consider forms of media that can be used to make new batteries.
 *
 * Return the amount of media extracted. This may be over [cost] if media is wasted.
 */
fun extractMedia(
    holder: ADMediaHolder,
    cost: Long = -1,
    drainForBatteries: Boolean = false,
    simulate: Boolean = false
): Long {
    if (drainForBatteries && !holder.canConstructBattery())
        return 0

    return holder.withdrawMedia(cost, simulate)
}

/**
 * Convenience function to scan the player's inventory, curios, etc for media sources,
 * and then sorts them
 */
fun scanPlayerForMediaStuff(player: ServerPlayer): List<ADMediaHolder> {
    val sources = mutableListOf<ADMediaHolder>()

    // 26.1's Inventory is a Container without public item lists, so scan every slot.
    val inv = player.inventory
    ((0 until inv.containerSize).map { inv.getItem(it) } + DiscoveryHandlers.collectExtraEquipments(player)).forEach {
        val holder = HexAPI.instance().findMediaHolder(it)
        if (holder?.canProvide() == true) {
            sources.add(holder)
        }
    }

    // HexDebug's personal pool always has the highest consumption priority, so it belongs in the
    // list before sorting; the subsequent sort + reverse puts it first.
    if (PersonalManaHolder.enablesManaForPlayer(player)) {
        PersonalManaHolder.get(player)?.let { sources.add(it) }
    }

    sources.sortWith(::compareMediaItem)
    sources.reverse()
    return sources
}

/**
 * Sorted from least important to most important
 */
fun compareMediaItem(aMedia: ADMediaHolder, bMedia: ADMediaHolder): Int {
    val priority = aMedia.consumptionPriority - bMedia.consumptionPriority
    if (priority != 0)
        return priority

    return (aMedia.withdrawMedia(-1, true) - bMedia.withdrawMedia(-1, true))
            .coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
}

fun mediaBarColor(media: Long, maxMedia: Long): Int {
    val amt = if (maxMedia == 0L) {
        0f
    } else {
        media.toFloat() / maxMedia.toFloat()
    }

    val r = Mth.lerp(amt, 84f, 254f)
    val g = Mth.lerp(amt, 57f, 203f)
    val b = Mth.lerp(amt, 138f, 230f)
    return ARGB.color(255, r.roundToInt(), g.roundToInt(), b.roundToInt())
}

fun mediaBarWidth(media: Long, maxMedia: Long): Int {
    val amt = if (maxMedia == 0L) {
        0f
    } else {
        media.toFloat() / maxMedia.toFloat()
    }
    return (13f * amt).roundToInt()
}
