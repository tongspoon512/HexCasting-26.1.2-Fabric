package ram.talia.moreiotas.common.casting.arithmetic.operator.matrix

import at.petrak.hexcasting.api.casting.arithmetic.operator.OperatorBasic
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaMultiPredicate
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaPredicate
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.DoubleIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes.DOUBLE
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes.VEC3
import org.ejml.simple.SimpleMatrix
import org.ejml.simple.SimpleOperations
import ram.talia.moreiotas.api.asActionResult
import ram.talia.moreiotas.api.casting.iota.MatrixIota
import ram.talia.moreiotas.api.matrices.MatrixExponentials
import ram.talia.moreiotas.api.matrixWrongSize
import ram.talia.moreiotas.common.casting.arithmetic.operator.nextNumVecMat
import ram.talia.moreiotas.common.lib.hex.MoreIotasIotaTypes.MATRIX
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt

private val NUM_VEC_MAT = IotaPredicate.any(
    IotaPredicate.ofType(DOUBLE), IotaPredicate.ofType(VEC3), IotaPredicate.ofType(MATRIX)
)

private val ADD_SUB_ACCEPTS = IotaMultiPredicate.any(
    IotaPredicate.ofType(MATRIX),
    IotaPredicate.or(IotaPredicate.ofType(DOUBLE), IotaPredicate.ofType(VEC3))
)

private val SAME_SIZE_ACCEPTS = IotaMultiPredicate.pair(NUM_VEC_MAT, NUM_VEC_MAT)

class OperatorMatrixAdd(private val subtract: Boolean) : OperatorBasic(2, ADD_SUB_ACCEPTS) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> {
        val it = iotas.iterator().withIndex()
        val mat0 = it.nextNumVecMat(arity).matrix
        val mat1 = it.nextNumVecMat(arity).matrix

        if (mat0.numRows != mat1.numRows || mat0.numCols != mat1.numCols) {
            throw MishapInvalidIota.matrixWrongSize(iotas.last(), 0, mat0.numRows, mat0.numCols)
        }
        return (if (subtract) mat0.minus(mat1) else mat0.plus(mat1)).asActionResult
    }
}

object OperatorMatrixMul : OperatorBasic(2, ADD_SUB_ACCEPTS) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> {
        val it = iotas.iterator().withIndex()
        val arg0 = it.nextNumVecMat(arity)
        val arg1 = it.nextNumVecMat(arity)

        arg0.scalar?.let { return arg1.matrix.scale(it).asActionResult }
        arg1.scalar?.let { return arg0.matrix.scale(it).asActionResult }

        val mat0 = arg0.matrix
        val mat1 = arg1.matrix
        if (mat0.numCols != mat1.numRows) {
            throw MishapInvalidIota.matrixWrongSize(iotas.last(), 0, mat0.numCols, null)
        }
        return mat0.mult(mat1).asActionResult
    }
}

object OperatorMatrixDiv : OperatorBasic(2, ADD_SUB_ACCEPTS) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> {
        val it = iotas.iterator().withIndex()
        val arg0 = it.nextNumVecMat(arity)
        val arg1 = it.nextNumVecMat(arity)

        arg0.scalar?.let { return arg1.matrix.invert().scale(it).asActionResult }
        arg1.scalar?.let { return arg0.matrix.scale(1 / it).asActionResult }

        val mat0 = arg0.matrix
        val mat1 = arg1.matrix
        if (mat0.numCols != mat1.numRows) {
            throw MishapInvalidIota.matrixWrongSize(iotas.last(), 0, mat0.numCols, null)
        }
        if (mat1.numCols != mat1.numRows) {
            throw MishapInvalidIota.matrixWrongSize(iotas.last(), 0, mat1.numRows, mat0.numRows)
        }
        return mat0.mult(mat1.invert()).asActionResult
    }
}

object OperatorMatrixPow : OperatorBasic(
    2, IotaMultiPredicate.either(
        IotaMultiPredicate.pair(IotaPredicate.ofType(MATRIX), IotaPredicate.ofType(DOUBLE)),
        IotaMultiPredicate.pair(IotaPredicate.ofType(DOUBLE), IotaPredicate.ofType(MATRIX))
    )
) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> {
        val it = iotas.iterator().withIndex()
        val arg0 = it.nextNumVecMat(arity)
        val arg1 = it.nextNumVecMat(arity)

        // 标量 ^ 矩阵 -> exp(ln(标量) * 矩阵)
        arg0.scalar?.let { base ->
            val mat = arg1.matrix
            if (mat.numRows != mat.numCols) {
                throw MishapInvalidIota.matrixWrongSize(iotas.last(), 0, mat.numRows, mat.numRows)
            }
            return MatrixExponentials.matrixExponential(mat.scale(ln(base))).asActionResult
        }

        val mat = arg0.matrix
        if (mat.numRows != mat.numCols) {
            throw MishapInvalidIota.matrixWrongSize(iotas.last(), 0, mat.numRows, mat.numRows)
        }
        val power = arg1.scalar ?: throw MishapInvalidIota.of(iotas.last(), 0, "double")
        return fractionalPowerMatrix(mat, power).asActionResult
    }

    private fun fractionalPowerMatrix(matrix: SimpleMatrix, power: Double): SimpleMatrix {
        if (power < 0) {
            return fractionalPowerMatrix(matrix.pseudoInverse(), -power)
        }

        val rounded = power.roundToInt()
        if (abs(power - rounded) <= DoubleIota.TOLERANCE) {
            return intMatrixPower(matrix, rounded)
        }

        throw MishapInvalidIota.of(DoubleIota(power), 0, "int")
    }

    private fun intMatrixPower(matrix: SimpleMatrix, power: Int): SimpleMatrix {
        if (power == 0) return SimpleMatrix.identity(matrix.numRows)

        var base: SimpleMatrix? = null
        var result: SimpleMatrix? = null
        var currPower = power

        while (currPower > 0) {
            base = base?.mult(base) ?: matrix
            if (currPower and 1 == 1) {
                result = result?.mult(base) ?: base
            }
            currPower = currPower shr 1
        }

        return result!!
    }
}

object OperatorMatrixAltMul : OperatorBasic(2, SAME_SIZE_ACCEPTS) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> =
        elementwise(iotas, 2) { a, b -> a.elementMult(b) }
}

object OperatorMatrixAltDiv : OperatorBasic(2, SAME_SIZE_ACCEPTS) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> =
        elementwise(iotas, 2) { a, b -> a.elementDiv(b) }
}

object OperatorMatrixAltPow : OperatorBasic(2, SAME_SIZE_ACCEPTS) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> =
        elementwise(iotas, 2) { a, b -> a.elementPower(b) }
}

private fun elementwise(
    iotas: Iterable<Iota>,
    arity: Int,
    op: (SimpleMatrix, SimpleMatrix) -> SimpleMatrix
): Iterable<Iota> {
    val it = iotas.iterator().withIndex()
    val mat0 = it.nextNumVecMat(arity).matrix
    val mat1 = it.nextNumVecMat(arity).matrix
    if (mat0.numRows != mat1.numRows || mat0.numCols != mat1.numCols) {
        throw MishapInvalidIota.matrixWrongSize(iotas.last(), 0, mat0.numRows, mat0.numCols)
    }
    return op(mat0, mat1).asActionResult
}

object OperatorMatrixAbs : OperatorBasic(1, IotaMultiPredicate.all(IotaPredicate.ofType(MATRIX))) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> =
        listOf(DoubleIota((iotas.first() as MatrixIota).simpleMatrix.normF()))
}

object OperatorMatrixFloor : OperatorBasic(1, IotaMultiPredicate.all(IotaPredicate.ofType(MATRIX))) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> =
        unaryMatrixApply(iotas) { mat ->
            mat.elementOp(SimpleOperations.ElementOpReal { _, _, d -> floor(d) })
        }
}

object OperatorMatrixCeil : OperatorBasic(1, IotaMultiPredicate.all(IotaPredicate.ofType(MATRIX))) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> =
        unaryMatrixApply(iotas) { mat ->
            mat.elementOp(SimpleOperations.ElementOpReal { _, _, d -> ceil(d) })
        }
}

object OperatorMatrixTranspose : OperatorBasic(1, IotaMultiPredicate.all(IotaPredicate.ofType(MATRIX))) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> =
        unaryMatrixApply(iotas) { mat -> mat.transpose() }
}

private inline fun unaryMatrixApply(
    iotas: Iterable<Iota>,
    op: (SimpleMatrix) -> SimpleMatrix
): Iterable<Iota> = listOf(MatrixIota(op((iotas.first() as MatrixIota).simpleMatrix)))
