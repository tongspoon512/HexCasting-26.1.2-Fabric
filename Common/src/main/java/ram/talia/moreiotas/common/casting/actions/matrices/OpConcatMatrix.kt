package ram.talia.moreiotas.common.casting.actions.matrices

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import ram.talia.moreiotas.api.asActionResult
import ram.talia.moreiotas.api.getNumOrVecOrSimpleMatrix
import ram.talia.moreiotas.api.matrixWrongSize
import ram.talia.moreiotas.api.mod.MoreIotasConfig

/**
 * 纵向（[concatVertically] 为 true）或横向拼接两个矩阵。
 */
class OpConcatMatrix(private val concatVertically: Boolean) : ConstMediaAction {
    override val argc = 2

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val mat0 = args.getNumOrVecOrSimpleMatrix(0, argc)
        val mat1 = args.getNumOrVecOrSimpleMatrix(1, argc)
        val maxSize = MoreIotasConfig.maxMatrixSize

        if (concatVertically) {
            if (mat0.numCols != mat1.numCols)
                throw MishapInvalidIota.matrixWrongSize(args[1], 0, null, mat0.numCols)
            if (mat0.numRows + mat1.numRows > maxSize)
                throw MishapInvalidIota.of(
                    args[1], 0, "matrix.max_size", maxSize, mat0.numRows + mat1.numRows, mat0.numCols
                )
        } else {
            if (mat0.numRows != mat1.numRows)
                throw MishapInvalidIota.matrixWrongSize(args[1], 0, mat0.numRows, null)
            if (mat0.numCols + mat1.numCols > maxSize)
                throw MishapInvalidIota.of(
                    args[1], 0, "matrix.max_size", maxSize, mat0.numRows, mat0.numCols + mat1.numCols
                )
        }

        return if (concatVertically)
            mat0.concatRows(mat1).asActionResult
        else
            mat0.concatColumns(mat1).asActionResult
    }
}
