package ram.talia.moreiotas.common.lib.hex

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import net.minecraft.resources.Identifier
import ram.talia.moreiotas.api.MoreIotasAPI
import ram.talia.moreiotas.api.casting.iota.EntityTypeIota
import ram.talia.moreiotas.api.casting.iota.IotaTypeIota
import ram.talia.moreiotas.api.casting.iota.ItemStackIota
import ram.talia.moreiotas.api.casting.iota.ItemTypeIota
import ram.talia.moreiotas.api.casting.iota.MatrixIota
import ram.talia.moreiotas.api.casting.iota.StringIota
import java.util.LinkedHashMap
import java.util.function.BiConsumer

/**
 * MoreIotas 的 iota 类型注册表。
 * id 必须与上游保持一致（moreiotas:string 等），否则旧存档无法解析。
 */
object MoreIotasIotaTypes {
    private val TYPES = LinkedHashMap<Identifier, IotaType<*>>()

    @JvmStatic
    fun registerTypes(r: BiConsumer<IotaType<*>, Identifier>) {
        for ((id, type) in TYPES) {
            r.accept(type, id)
        }
    }

    @JvmField
    val STRING: IotaType<StringIota> = type("string", StringIota.TYPE)

    @JvmField
    val MATRIX: IotaType<MatrixIota> = type("matrix", MatrixIota.TYPE)

    @JvmField
    val IOTA_TYPE: IotaType<IotaTypeIota> = type("iota_type", IotaTypeIota.TYPE)

    @JvmField
    val ENTITY_TYPE: IotaType<EntityTypeIota> = type("entity_type", EntityTypeIota.TYPE)

    @JvmField
    val ITEM_TYPE: IotaType<ItemTypeIota> = type("item_type", ItemTypeIota.TYPE)

    @JvmField
    val ITEM_STACK: IotaType<ItemStackIota> = type("item_stack", ItemStackIota.TYPE)

    private fun <U : Iota, T : IotaType<U>> type(name: String, type: T): T {
        val old = TYPES.put(MoreIotasAPI.modLoc(name), type)
        check(old == null) { "Typo? Duplicate id $name" }
        return type
    }
}
