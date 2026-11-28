package at.petrak.hexcasting.neoforge.registry

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.casting.ActionRegistryEntry
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic
import at.petrak.hexcasting.api.casting.castables.SpecialHandler
import at.petrak.hexcasting.api.casting.eval.sideeffects.EvalSound
import at.petrak.hexcasting.api.casting.eval.vm.ContinuationFrame
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.common.lib.HexRegistries
import net.minecraft.core.Registry
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.NewRegistryEvent
import net.neoforged.neoforge.registries.RegistryBuilder

/**
 * Hex 的六个自定义注册表。
 *
 * Common 里的 HexActions / HexIotaTypes 等会在类初始化时直接持有这些注册表，
 * 因此这里做懒创建：谁先访问谁创建，随后在 NewRegistryEvent 里注册给 NeoForge。
 */
object NeoForgeHexRegistries {
    private var action: Registry<ActionRegistryEntry>? = null
    private var specialHandler: Registry<SpecialHandler.Factory<*>>? = null
    private var iotaType: Registry<IotaType<*>>? = null
    private var arithmetic: Registry<Arithmetic>? = null
    private var continuationType: Registry<ContinuationFrame.Type<*>>? = null
    private var evalSound: Registry<EvalSound>? = null

    fun action(): Registry<ActionRegistryEntry> =
        action ?: RegistryBuilder<ActionRegistryEntry>(HexRegistries.ACTION)
            .create()
            .also { action = it }

    fun specialHandler(): Registry<SpecialHandler.Factory<*>> =
        specialHandler ?: RegistryBuilder<SpecialHandler.Factory<*>>(HexRegistries.SPECIAL_HANDLER)
            .create()
            .also { specialHandler = it }

    fun iotaType(): Registry<IotaType<*>> =
        iotaType ?: RegistryBuilder<IotaType<*>>(HexRegistries.IOTA_TYPE)
            .defaultKey(HexAPI.modLoc("null"))
            .create()
            .also { iotaType = it }

    fun arithmetic(): Registry<Arithmetic> =
        arithmetic ?: RegistryBuilder<Arithmetic>(HexRegistries.ARITHMETIC)
            .create()
            .also { arithmetic = it }

    fun continuationType(): Registry<ContinuationFrame.Type<*>> =
        continuationType ?: RegistryBuilder<ContinuationFrame.Type<*>>(HexRegistries.CONTINUATION_TYPE)
            .defaultKey(HexAPI.modLoc("end"))
            .create()
            .also { continuationType = it }

    fun evalSound(): Registry<EvalSound> =
        evalSound ?: RegistryBuilder<EvalSound>(HexRegistries.EVAL_SOUND)
            .defaultKey(HexAPI.modLoc("nothing"))
            .create()
            .also { evalSound = it }

    /** 在模组总线上登记，NewRegistryEvent 时把这些注册表交给 NeoForge。 */
    fun attach(modBus: IEventBus) {
        modBus.addListener(NewRegistryEvent::class.java) { event ->
            event.register(action())
            event.register(specialHandler())
            event.register(iotaType())
            event.register(arithmetic())
            event.register(continuationType())
            event.register(evalSound())
        }
    }
}
