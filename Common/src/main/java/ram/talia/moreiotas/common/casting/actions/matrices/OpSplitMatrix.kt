package ram.talia.moreiotas.common.casting.actions.matrices

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getPositiveIntUnderInclusive
import at.petrak.hexcasting.api.casting.iota.Iota
import ram.talia.moreiotas.api.casting.iota.MatrixIota
import ram.talia.moreiotas.api.getNumOrVecOrSimpleMatrix

/**
 * 纵向（[splitVertically] 为 true）或横向把矩阵切成两个。
 */
class OpSplitMatrix(private val splitVertically: Boolean) : ConstMediaAction {
    override val argc = 2

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val mat = args.getNumOrVecOrSimpleMatrix(0, argc)
        val split = args.getPositiveIntUnderInclusive(
            1, if (splitVertically) mat.numRows else mat.numCols, argc
        )

        val splitNum = if (splitVertically) mat.numRows else mat.numCols
        val out0 = if (splitVertically) mat.rows(0, split) else mat.cols(0, split)
        val out1 = if (splitVertically) mat.rows(split, splitNum) else mat.cols(split, splitNum)

        return listOf(MatrixIota(out0), MatrixIota(out1))
    }
}
