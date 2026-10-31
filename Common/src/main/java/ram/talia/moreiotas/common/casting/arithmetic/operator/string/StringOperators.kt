package ram.talia.moreiotas.common.casting.arithmetic.operator.string

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.arithmetic.operator.OperatorBasic
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaMultiPredicate
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaPredicate
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.DoubleIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.NullIota
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.common.casting.arithmetic.operator.nextPositiveIntUnderInclusive
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes.DOUBLE
import ram.talia.moreiotas.api.asActionResult
import ram.talia.moreiotas.api.casting.iota.StringIota
import ram.talia.moreiotas.common.casting.arithmetic.operator.nextString
import ram.talia.moreiotas.common.lib.hex.MoreIotasIotaTypes.STRING
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object OperatorStringMul : OperatorBasic(
    2, IotaMultiPredicate.either(
        IotaMultiPredicate.pair(IotaPredicate.ofType(STRING), IotaPredicate.ofType(DOUBLE)),
        IotaMultiPredicate.pair(IotaPredicate.ofType(DOUBLE), IotaPredicate.ofType(STRING))
    )
) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> {
        val it = iotas.iterator()
        val first = it.next()
        val second = it.next()

        val (num, str) = if (first is DoubleIota) {
            toRepeatCount(first, 1) to (second as StringIota).string
        } else {
            toRepeatCount(second as DoubleIota, 0) to (first as StringIota).string
        }

        return str.repeat(num).asActionResult
    }

    private fun toRepeatCount(iota: DoubleIota, reverseIdx: Int): Int {
        val rounded = iota.double.roundToInt()
        if (abs(iota.double - rounded) > DoubleIota.TOLERANCE) {
            throw MishapInvalidIota.of(iota, reverseIdx, "int")
        }
        if (rounded < 0) {
            throw MishapInvalidIota.of(iota, reverseIdx, "int.positive")
        }
        return rounded
    }
}

object OperatorStringIndex : OperatorBasic(
    2, IotaMultiPredicate.pair(IotaPredicate.ofType(STRING), IotaPredicate.ofType(DOUBLE))
) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> {
        val it = iotas.iterator()
        val str = (it.next() as StringIota).string
        val index = (it.next() as DoubleIota).double.roundToInt()
        return str.getOrNull(index)?.toString()?.asActionResult ?: listOf(NullIota())
    }
}

object OperatorStringSlice : OperatorBasic(
    3, IotaMultiPredicate.triple(
        IotaPredicate.ofType(STRING),
        IotaPredicate.ofType(DOUBLE),
        IotaPredicate.ofType(DOUBLE)
    )
) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> {
        val it = iotas.iterator().withIndex()
        val str = it.nextString(arity)
        val index0 = it.nextPositiveIntUnderInclusive(str.length, arity)
        val index1 = it.nextPositiveIntUnderInclusive(str.length, arity)

        if (index0 == index1) {
            return "".asActionResult
        }
        return str.substring(min(index0, index1), max(index0, index1)).asActionResult
    }
}

object OperatorStringIndexOf : OperatorBasic(
    2, IotaMultiPredicate.pair(IotaPredicate.ofType(STRING), IotaPredicate.ofType(STRING))
) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> {
        val it = iotas.iterator().withIndex()
        val toSearch = it.nextString(arity)
        val searchFor = it.nextString(arity)
        return toSearch.indexOf(searchFor).asActionResult
    }
}

object OperatorStringRemove : OperatorBasic(
    2, IotaMultiPredicate.pair(
        IotaPredicate.ofType(STRING),
        IotaPredicate.or(IotaPredicate.ofType(STRING), IotaPredicate.ofType(DOUBLE))
    )
) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> {
        val it = iotas.iterator().withIndex()
        val removeFrom = it.nextString(arity)

        val (_, x) = it.next()
        return if (x is StringIota) {
            removeFrom.replaceFirst(x.string, "")
        } else {
            val double = (x as DoubleIota).double
            val rounded = double.roundToInt()
            if (abs(double - rounded) > DoubleIota.TOLERANCE || rounded !in 0..removeFrom.length) {
                throw MishapInvalidIota.of(x, 0, "int.positive.less.equal", removeFrom.length)
            }
            if (rounded == removeFrom.length) {
                removeFrom
            } else {
                removeFrom.substring(0, rounded) + removeFrom.substring(rounded + 1)
            }
        }.asActionResult
    }
}

object OperatorStringReplace : OperatorBasic(
    3, IotaMultiPredicate.triple(
        IotaPredicate.ofType(STRING),
        IotaPredicate.or(IotaPredicate.ofType(DOUBLE), IotaPredicate.ofType(STRING)),
        IotaPredicate.ofType(STRING)
    )
) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> {
        val it = iotas.iterator().withIndex()
        val replaceIn = it.nextString(arity)
        val (_, x) = it.next()
        val replaceWith = it.nextString()

        return if (x is StringIota) {
            replaceIn.replaceFirst(x.string, replaceWith)
        } else {
            val double = (x as DoubleIota).double
            val rounded = double.roundToInt()
            if (abs(double - rounded) > DoubleIota.TOLERANCE || rounded !in 0..replaceIn.length) {
                throw MishapInvalidIota.of(x, 0, "int.positive.less.equal", replaceIn.length)
            }

            val replaceLength = min(replaceIn.length - rounded, replaceWith.length)
            replaceIn.substring(0, rounded) +
                replaceWith.substring(0, replaceLength) +
                replaceIn.substring(rounded + replaceLength)
        }.asActionResult
    }
}
