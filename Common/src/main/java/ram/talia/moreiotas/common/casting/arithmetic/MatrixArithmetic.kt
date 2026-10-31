package ram.talia.moreiotas.common.casting.arithmetic

import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic.ABS
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic.ADD
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic.CEIL
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic.DIV
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic.FLOOR
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic.MUL
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic.POW
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic.REV
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic.SUB
import at.petrak.hexcasting.api.casting.arithmetic.engine.InvalidOperatorException
import at.petrak.hexcasting.api.casting.arithmetic.operator.Operator
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import ram.talia.moreiotas.common.casting.arithmetic.operator.matrix.OperatorMatrixAbs
import ram.talia.moreiotas.common.casting.arithmetic.operator.matrix.OperatorMatrixAdd
import ram.talia.moreiotas.common.casting.arithmetic.operator.matrix.OperatorMatrixAltDiv
import ram.talia.moreiotas.common.casting.arithmetic.operator.matrix.OperatorMatrixAltMul
import ram.talia.moreiotas.common.casting.arithmetic.operator.matrix.OperatorMatrixAltPow
import ram.talia.moreiotas.common.casting.arithmetic.operator.matrix.OperatorMatrixCeil
import ram.talia.moreiotas.common.casting.arithmetic.operator.matrix.OperatorMatrixDiv
import ram.talia.moreiotas.common.casting.arithmetic.operator.matrix.OperatorMatrixFloor
import ram.talia.moreiotas.common.casting.arithmetic.operator.matrix.OperatorMatrixMul
import ram.talia.moreiotas.common.casting.arithmetic.operator.matrix.OperatorMatrixPow
import ram.talia.moreiotas.common.casting.arithmetic.operator.matrix.OperatorMatrixTranspose

object MatrixArithmetic : Arithmetic {
    @JvmField
    val ALTADD: HexPattern = HexPattern.fromAngles("waawawaeawwaea", HexDir.EAST)
    @JvmField
    val ALTMUL: HexPattern = HexPattern.fromAngles("waqawawwaeaww", HexDir.SOUTH_EAST)
    @JvmField
    val ALTDIV: HexPattern = HexPattern.fromAngles("wdedwdwwdqdww", HexDir.NORTH_EAST)
    @JvmField
    val ALTPOW: HexPattern = HexPattern.fromAngles("wedewqawwawqwa", HexDir.NORTH_EAST)

    private val OPS = listOf(ADD, SUB, MUL, ALTMUL, DIV, ALTDIV, ABS, POW, ALTPOW, FLOOR, CEIL, REV)

    override fun arithName(): String = "matrix_maths"

    override fun opTypes(): Iterable<HexPattern> = OPS

    override fun getOperator(pattern: HexPattern): Operator = when (pattern) {
        ADD -> OperatorMatrixAdd(false)
        SUB -> OperatorMatrixAdd(true)
        MUL -> OperatorMatrixMul
        ALTMUL -> OperatorMatrixAltMul
        DIV -> OperatorMatrixDiv
        ALTDIV -> OperatorMatrixAltDiv
        ABS -> OperatorMatrixAbs
        POW -> OperatorMatrixPow
        ALTPOW -> OperatorMatrixAltPow
        FLOOR -> OperatorMatrixFloor
        CEIL -> OperatorMatrixCeil
        REV -> OperatorMatrixTranspose
        else -> throw InvalidOperatorException("$pattern is not a valid operator in Arithmetic $this.")
    }
}
