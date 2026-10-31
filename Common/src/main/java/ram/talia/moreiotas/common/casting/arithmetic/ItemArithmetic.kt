package ram.talia.moreiotas.common.casting.arithmetic

import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic.ABS
import at.petrak.hexcasting.api.casting.arithmetic.engine.InvalidOperatorException
import at.petrak.hexcasting.api.casting.arithmetic.operator.Operator
import at.petrak.hexcasting.api.casting.arithmetic.operator.OperatorBasic
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaMultiPredicate
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaPredicate
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.DoubleIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import ram.talia.moreiotas.api.casting.iota.ItemStackIota
import ram.talia.moreiotas.common.casting.arithmetic.operator.item.OperatorMakeItem
import ram.talia.moreiotas.common.casting.arithmetic.operator.type.OperatorExtractItem
import ram.talia.moreiotas.common.lib.hex.MoreIotasIotaTypes.ITEM_STACK

object ItemArithmetic : Arithmetic {
    @JvmField
    val EXTRACT_ITEM: HexPattern = HexPattern.fromAngles("qaqqaea", HexDir.EAST)

    @JvmField
    val MAKE_ITEM: HexPattern = HexPattern.fromAngles("adeeedew", HexDir.EAST)

    private val OPS = listOf(ABS, EXTRACT_ITEM, MAKE_ITEM)

    override fun arithName(): String = "item_ops"

    override fun opTypes(): Iterable<HexPattern> = OPS

    override fun getOperator(pattern: HexPattern): Operator = when (pattern) {
        ABS -> object : OperatorBasic(1, IotaMultiPredicate.all(IotaPredicate.ofType(ITEM_STACK))) {
            override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> {
                val stack = (iotas.first() as ItemStackIota).itemStack
                return listOf(DoubleIota(if (stack.isEmpty) 0.0 else stack.count.toDouble()))
            }
        }
        EXTRACT_ITEM -> OperatorExtractItem
        MAKE_ITEM -> OperatorMakeItem
        else -> throw InvalidOperatorException("$pattern is not a valid operator in Arithmetic $this.")
    }
}
