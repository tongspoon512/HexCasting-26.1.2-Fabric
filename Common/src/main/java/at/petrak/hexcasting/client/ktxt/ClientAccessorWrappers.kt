@file:JvmName("ClientAccessorWrappers")
package at.petrak.hexcasting.client.ktxt

import at.petrak.hexcasting.mixin.accessor.client.AccessorMouseHandler
import at.petrak.hexcasting.mixin.accessor.client.AccessorScrollWheelHandler
import net.minecraft.client.MouseHandler

/**
 * 26.1 keeps the accumulated scroll on the ScrollWheelHandler owned by MouseHandler.
 */
var MouseHandler.accumulatedScroll: Double
    get() = ((this as AccessorMouseHandler).`hex$getScrollWheelHandler`() as AccessorScrollWheelHandler)
        .`hex$getAccumulatedScrollY`()
    set(value) = ((this as AccessorMouseHandler).`hex$getScrollWheelHandler`() as AccessorScrollWheelHandler)
        .`hex$setAccumulatedScrollY`(value)
