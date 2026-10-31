package ram.talia.moreiotas.common.casting.arithmetic

import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic.ABS
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic.ADD
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic.INDEX
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic.INDEX_OF
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic.MUL
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic.REMOVE
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic.REPLACE
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic.REV
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic.SLICE
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic.UNIQUE
import at.petrak.hexcasting.api.casting.arithmetic.engine.InvalidOperatorException
import at.petrak.hexcasting.api.casting.arithmetic.operator.Operator
import at.petrak.hexcasting.api.casting.arithmetic.operator.OperatorBasic
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaMultiPredicate
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaPredicate
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.DoubleIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.math.HexPattern
import ram.talia.moreiotas.api.casting.iota.StringIota
import ram.talia.moreiotas.common.casting.arithmetic.operator.string.OperatorStringIndex
import ram.talia.moreiotas.common.casting.arithmetic.operator.string.OperatorStringIndexOf
import ram.talia.moreiotas.common.casting.arithmetic.operator.string.OperatorStringMul
import ram.talia.moreiotas.common.casting.arithmetic.operator.string.OperatorStringRemove
import ram.talia.moreiotas.common.casting.arithmetic.operator.string.OperatorStringReplace
import ram.talia.moreiotas.common.casting.arithmetic.operator.string.OperatorStringSlice
import ram.talia.moreiotas.common.lib.hex.MoreIotasIotaTypes.STRING

object StringArithmetic : Arithmetic {
    private val OPS = listOf(ADD, MUL, ABS, INDEX, SLICE, REV, INDEX_OF, REMOVE, REPLACE, UNIQUE)

    private val STRING_PREDICATE = IotaMultiPredicate.all(IotaPredicate.ofType(STRING))

    override fun arithName(): String = "string_ops"

    override fun opTypes(): Iterable<HexPattern> = OPS

    override fun getOperator(pattern: HexPattern): Operator = when (pattern) {
        ADD -> binary { a, b -> a + b }
        MUL -> OperatorStringMul
        ABS -> unaryDouble { it.length.toDouble() }
        INDEX -> OperatorStringIndex
        SLICE -> OperatorStringSlice
        REV -> unary { it.reversed() }
        INDEX_OF -> OperatorStringIndexOf
        REMOVE -> OperatorStringRemove
        REPLACE -> OperatorStringReplace
        UNIQUE -> unary { str ->
            str.fold(StringBuilder()) { builder, c ->
                if (!builder.contains(c)) builder.append(c)
                builder
            }.toString()
        }
        else -> throw InvalidOperatorException("$pattern is not a valid operator in Arithmetic $this.")
    }

    private fun unary(op: (String) -> String): OperatorBasic =
        object : OperatorBasic(1, STRING_PREDICATE) {
            override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> =
                listOf(StringIota.make(op((iotas.first() as StringIota).string)))
        }

    private fun unaryDouble(op: (String) -> Double): OperatorBasic =
        object : OperatorBasic(1, STRING_PREDICATE) {
            override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> =
                listOf(DoubleIota(op((iotas.first() as StringIota).string)))
        }

    private fun binary(op: (String, String) -> String): OperatorBasic =
        object : OperatorBasic(2, STRING_PREDICATE) {
            override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> {
                val it = iotas.iterator()
                return listOf(
                    StringIota.make(
                        op((it.next() as StringIota).string, (it.next() as StringIota).string)
                    )
                )
            }
        }
}
