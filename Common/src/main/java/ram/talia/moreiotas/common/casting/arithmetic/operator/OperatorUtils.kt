package ram.talia.moreiotas.common.casting.arithmetic.operator

import at.petrak.hexcasting.api.casting.iota.DoubleIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.Vec3Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import org.ejml.simple.SimpleMatrix
import ram.talia.moreiotas.api.asSimpleMatrix
import ram.talia.moreiotas.api.casting.iota.MatrixIota
import ram.talia.moreiotas.api.casting.iota.StringIota
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 数字 / 向量 / 矩阵的统一表示。
 * [scalar] 非空表示原始 iota 是 DoubleIota，此时 [matrix] 是它的 1x1 形式。
 */
class NumVecMat(val matrix: SimpleMatrix, val scalar: Double?) {
    companion object {
        @JvmStatic
        fun from(iota: Iota, idx: Int, argc: Int): NumVecMat = when (iota) {
            is DoubleIota -> NumVecMat(SimpleMatrix(1, 1, false, iota.double), iota.double)
            is Vec3Iota -> NumVecMat(iota.vec3.asSimpleMatrix, null)
            is MatrixIota -> NumVecMat(iota.simpleMatrix, null)
            else -> throw MishapInvalidIota.of(
                iota, if (argc == 0) idx else argc - (idx + 1), "numvecmat"
            )
        }
    }
}

fun Iterator<IndexedValue<Iota>>.nextNumVecMat(argc: Int = 0): NumVecMat {
    val (idx, x) = this.next()
    return NumVecMat.from(x, idx, argc)
}

fun Iterator<IndexedValue<Iota>>.nextString(argc: Int = 0): String {
    val (idx, x) = this.next()
    if (x is StringIota) {
        return x.string
    }
    throw MishapInvalidIota.of(x, if (argc == 0) idx else argc - (idx + 1), "string")
}

fun Iterator<IndexedValue<Iota>>.nextPositiveInt(argc: Int = 0): Int {
    val (idx, x) = this.next()
    if (x is DoubleIota) {
        val double = x.double
        val rounded = double.roundToInt()
        if (abs(double - rounded) <= DoubleIota.TOLERANCE && rounded >= 0) {
            return rounded
        }
    }
    throw MishapInvalidIota.of(x, if (argc == 0) idx else argc - (idx + 1), "int.positive")
}
