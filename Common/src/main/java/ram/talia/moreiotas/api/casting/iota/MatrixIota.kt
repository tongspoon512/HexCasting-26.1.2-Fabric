package ram.talia.moreiotas.api.casting.iota

import at.petrak.hexcasting.api.casting.iota.DoubleIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.api.utils.downcast
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import org.ejml.simple.SimpleMatrix
import ram.talia.moreiotas.api.mod.MoreIotasConfig
import ram.talia.moreiotas.common.lib.hex.MoreIotasIotaTypes

class MatrixIota(val simpleMatrix: SimpleMatrix) : Iota(MoreIotasIotaTypes.MATRIX, simpleMatrix) {
    init {
        val max = MoreIotasConfig.maxMatrixSize
        if (simpleMatrix.numRows > max || simpleMatrix.numCols > max) {
            throw MishapInvalidIota.of(
                this, 0, "matrix.max_size", max, simpleMatrix.numRows, simpleMatrix.numCols
            )
        }
    }

    override fun toleratesOther(that: Iota): Boolean =
        that is MatrixIota && that.simpleMatrix.isIdentical(this.simpleMatrix, 0.001)

    override fun isTruthy(): Boolean =
        this.simpleMatrix.elementMaxAbs() > DoubleIota.TOLERANCE

    override fun serialize(): Tag {
        val mat = this.simpleMatrix
        val tag = CompoundTag()
        tag.putInt(TAG_ROWS, mat.numRows)
        tag.putInt(TAG_COLS, mat.numCols)

        val list = ListTag()
        for (i in 0 until mat.numRows) {
            val row = ListTag()
            for (j in 0 until mat.numCols) {
                row.add(DoubleTag.valueOf(mat.get(i, j)))
            }
            list.add(row)
        }
        tag.put(TAG_MAT, list)
        return tag
    }

    companion object {
        private const val TAG_ROWS = "rows"
        private const val TAG_COLS = "cols"
        private const val TAG_MAT = "mat"

        @JvmField
        val TYPE: IotaType<MatrixIota> = object : IotaType<MatrixIota>() {
            override fun deserialize(tag: Tag, world: ServerLevel?): MatrixIota {
                val ctag = tag.downcast(CompoundTag.TYPE)
                try {
                    return MatrixIota(deserialise(ctag))
                } catch (e: MishapInvalidIota) {
                    throw IllegalArgumentException(e)
                }
            }

            override fun display(tag: Tag): Component {
                if (tag !is CompoundTag) {
                    return Component.translatable("hexcasting.spelldata.unknown")
                }

                val mat = try {
                    deserialise(tag)
                } catch (e: IllegalArgumentException) {
                    return Component.translatable("hexcasting.spelldata.unknown")
                }

                val out = Component.empty()
                out.append("(${mat.numRows}, ${mat.numCols})")
                if (!(mat.numCols == 0 && mat.numRows == 0)) {
                    out.append(" | ")
                }

                for (r in 0 until mat.numRows) {
                    for (c in 0 until mat.numCols) {
                        out.append(
                            Component.literal(String.format("%.2f", mat.get(r, c))).withStyle(ChatFormatting.GREEN)
                        )
                        if (c < mat.numCols - 1) {
                            out.append(", ")
                        }
                    }
                    if (r < mat.numRows - 1) {
                        out.append("; ")
                    }
                }

                return Component.translatable("hexcasting.tooltip.list_contents", out)
                    .withStyle(ChatFormatting.AQUA)
            }

            override fun color(): Int = 0xff55ffff.toInt()

            private fun deserialise(ctag: CompoundTag): SimpleMatrix {
                if (!ctag.contains(TAG_ROWS) || !ctag.contains(TAG_COLS) || !ctag.contains(TAG_MAT)) {
                    throw IllegalArgumentException(
                        "expected tags \"rows\": int, \"cols\": int, and \"mat\": list(list(double))"
                    )
                }

                val rows = ctag.getIntOr(TAG_ROWS, 0)
                val cols = ctag.getIntOr(TAG_COLS, 0)
                if (rows <= 0 || cols <= 0) {
                    throw IllegalArgumentException("matrix dimensions must be positive")
                }

                val mat = SimpleMatrix(rows, cols)
                val list = ctag.getList(TAG_MAT).orElseThrow {
                    IllegalArgumentException("expected list tag \"mat\"")
                }

                for (i in 0 until rows) {
                    val row = list.getListOrEmpty(i)
                    for (j in 0 until cols) {
                        mat.set(i, j, row.getDoubleOr(j, 0.0))
                    }
                }

                return mat
            }
        }
    }
}
