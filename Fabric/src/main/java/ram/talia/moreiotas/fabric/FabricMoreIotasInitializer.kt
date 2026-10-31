package ram.talia.moreiotas.fabric

import at.petrak.hexcasting.xplat.IXplatAbstractions
import net.minecraft.core.Registry
import net.minecraft.resources.Identifier
import ram.talia.moreiotas.common.lib.hex.MoreIotasActions
import ram.talia.moreiotas.common.lib.hex.MoreIotasArithmetics
import ram.talia.moreiotas.common.lib.hex.MoreIotasIotaTypes
import ram.talia.moreiotas.fabric.eventhandlers.ChatEventHandler
import ram.talia.moreiotas.fabric.xplat.FabricXplatImpl
import ram.talia.moreiotas.xplat.IXplatAbstractions as MoreIotasXplat
import java.util.function.BiConsumer

/**
 * MoreIotas 的 Fabric 侧注册入口，由 FabricHexInitializer 在 initRegistries() 中调用。
 *
 * 注册顺序必须早于 PatternRegistryManifest.processRegistry，否则图案与算术不会进入
 * 图案清单，法术也无法解析。
 */
object FabricMoreIotasInitializer {
    fun init() {
        MoreIotasXplat.INSTANCE = FabricXplatImpl
        ChatEventHandler.init()
    }

    fun initRegistries() {
        MoreIotasIotaTypes.registerTypes(bind(IXplatAbstractions.INSTANCE.iotaTypeRegistry))
        MoreIotasActions.register(bind(IXplatAbstractions.INSTANCE.actionRegistry))
        MoreIotasArithmetics.register(bind(IXplatAbstractions.INSTANCE.arithmeticRegistry))
    }

    private fun <T : Any> bind(registry: Registry<T>): BiConsumer<T, Identifier> =
        BiConsumer { t, id -> Registry.register(registry, id, t) }
}
