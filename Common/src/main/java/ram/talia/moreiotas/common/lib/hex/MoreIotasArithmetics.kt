package ram.talia.moreiotas.common.lib.hex

import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic
import net.minecraft.resources.Identifier
import ram.talia.moreiotas.api.MoreIotasAPI
import ram.talia.moreiotas.common.casting.arithmetic.ItemArithmetic
import ram.talia.moreiotas.common.casting.arithmetic.MatrixArithmetic
import ram.talia.moreiotas.common.casting.arithmetic.StringArithmetic
import java.util.LinkedHashMap
import java.util.function.BiConsumer

/**
 * MoreIotas 的算术注册表。
 * id 必须与上游保持一致（moreiotas:string / matrix / type），否则旧存档的图案无法解析。
 */
object MoreIotasArithmetics {
    private val ARITHMETICS = LinkedHashMap<Identifier, Arithmetic>()

    @JvmField
    val STRING: StringArithmetic = make("string", StringArithmetic)

    @JvmField
    val MATRIX: MatrixArithmetic = make("matrix", MatrixArithmetic)

    @JvmField
    val ITEM: ItemArithmetic = make("type", ItemArithmetic)

    @JvmStatic
    fun register(r: BiConsumer<Arithmetic, Identifier>) {
        for ((id, arithmetic) in ARITHMETICS) {
            r.accept(arithmetic, id)
        }
    }

    private fun <T : Arithmetic> make(name: String, arithmetic: T): T {
        val old = ARITHMETICS.put(MoreIotasAPI.modLoc(name), arithmetic)
        check(old == null) { "Typo? Duplicate id $name" }
        return arithmetic
    }
}
