package ram.talia.moreiotas.api

import at.petrak.hexcasting.api.casting.iota.BooleanIota
import at.petrak.hexcasting.api.casting.iota.DoubleIota
import at.petrak.hexcasting.api.casting.iota.EntityIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.NullIota
import at.petrak.hexcasting.api.casting.iota.Vec3Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.api.casting.mishaps.MishapNotEnoughArgs
import com.mojang.datafixers.util.Either
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.Vec3
import org.ejml.simple.SimpleMatrix
import ram.talia.moreiotas.api.casting.iota.EntityTypeIota
import ram.talia.moreiotas.api.casting.iota.IotaTypeIota
import ram.talia.moreiotas.api.casting.iota.ItemStackIota
import ram.talia.moreiotas.api.casting.iota.ItemTypeIota
import ram.talia.moreiotas.api.casting.iota.MatrixIota
import ram.talia.moreiotas.api.casting.iota.StringIota

inline val Vec3.asSimpleMatrix get() = SimpleMatrix(3, 1, false, this.x, this.y, this.z)
inline val BlockPos.asSimpleMatrix get() = SimpleMatrix(1, 3, false, this.x.toDouble(), this.y.toDouble(), this.z.toDouble())
inline val SimpleMatrix.asVec3 get() = Vec3(this[0], this[1], this[2])
inline val List<Vec3>.asSimpleMatrix get(): SimpleMatrix {
    val matrix = SimpleMatrix(this.size, 3)
    this.forEachIndexed { i, vec ->
        matrix.set(i, 0, vec.x)
        matrix.set(i, 1, vec.y)
        matrix.set(i, 2, vec.z)
    }
    return matrix
}

inline val SimpleMatrix.asActionResult get() = listOf(MatrixIota(this))
inline val String.asActionResult get() = listOf(StringIota.make(this))
inline val IotaType<*>.asActionResult get() = listOf(IotaTypeIota(this))
inline val Block.asActionResult get() = listOf(ItemTypeIota(this))
inline val EntityType<*>.asActionResult get() = listOf(EntityTypeIota(this))
inline val Item.asActionResult get() = listOf(ItemTypeIota(this))
inline val ItemStack.asActionResult get() = listOf(ItemStackIota.createFiltered(this))

/**
 * 抛出矩阵尺寸不匹配的 mishap。expectedRows / expectedColumns 至少有一个非 null。
 */
fun MishapInvalidIota.Companion.matrixWrongSize(
    perpetrator: Iota,
    reverseIdx: Int,
    expectedRows: Int?,
    expectedColumns: Int?
): MishapInvalidIota {
    require(expectedRows != null || expectedColumns != null) {
        "Need at least one of expectedRows and expectedColumns non-null."
    }

    return when {
        expectedRows == null -> of(perpetrator, reverseIdx, "matrix.wrong_size", "n", expectedColumns!!)
        expectedColumns == null -> of(perpetrator, reverseIdx, "matrix.wrong_size", expectedRows, "n")
        else -> of(perpetrator, reverseIdx, "matrix.wrong_size", expectedRows, expectedColumns)
    }
}

fun List<Iota>.getBoolOrNull(idx: Int, argc: Int = 0): Boolean? {
    val x = this.getOrElse(idx) { throw MishapNotEnoughArgs(idx + 1, this.size) }
    return when (x) {
        is BooleanIota -> x.bool
        is NullIota -> null
        else -> throw MishapInvalidIota.ofType(x, if (argc == 0) idx else argc - (idx + 1), "booleannull")
    }
}

fun List<Iota>.getString(idx: Int, argc: Int = 0): String {
    val x = this.getOrElse(idx) { throw MishapNotEnoughArgs(idx + 1, this.size) }
    if (x is StringIota) {
        return x.string
    }
    throw MishapInvalidIota.ofType(x, if (argc == 0) idx else argc - (idx + 1), "string")
}

fun List<Iota>.getStringOrNull(idx: Int, argc: Int = 0): String? {
    val x = this.getOrElse(idx) { throw MishapNotEnoughArgs(idx + 1, this.size) }
    return when (x) {
        is StringIota -> x.string
        is NullIota -> null
        else -> throw MishapInvalidIota.ofType(x, if (argc == 0) idx else argc - (idx + 1), "string")
    }
}

fun List<Iota>.getStringOrList(idx: Int, argc: Int = 0): Either<String, List<String>> {
    val x = this.getOrElse(idx) { throw MishapNotEnoughArgs(idx + 1, this.size) }
    return when (x) {
        is StringIota -> Either.left(x.string)
        is ListIota -> {
            val list = mutableListOf<String>()
            for (iota in x.list) {
                if (iota !is StringIota) {
                    throw MishapInvalidIota.ofType(x, if (argc == 0) idx else argc - (idx + 1), "stringstringlist")
                }
                list.add(iota.string)
            }
            Either.right(list)
        }
        else -> throw MishapInvalidIota.ofType(x, if (argc == 0) idx else argc - (idx + 1), "stringstringlist")
    }
}

fun List<Iota>.getSimpleMatrix(idx: Int, argc: Int = 0): SimpleMatrix {
    val x = this.getOrElse(idx) { throw MishapNotEnoughArgs(idx + 1, this.size) }
    if (x is MatrixIota) {
        return x.simpleMatrix
    }
    throw MishapInvalidIota.ofType(x, if (argc == 0) idx else argc - (idx + 1), "matrix")
}

/**
 * 接受数字 / 向量 / 矩阵，统一转换成 [SimpleMatrix]。
 */
fun List<Iota>.getNumOrVecOrSimpleMatrix(idx: Int, argc: Int = 0): SimpleMatrix {
    val x = this.getOrElse(idx) { throw MishapNotEnoughArgs(idx + 1, this.size) }
    return when (x) {
        is DoubleIota -> SimpleMatrix(1, 1, false, x.double)
        is Vec3Iota -> x.vec3.asSimpleMatrix
        is MatrixIota -> x.simpleMatrix
        else -> throw MishapInvalidIota.ofType(x, if (argc == 0) idx else argc - (idx + 1), "numvecmat")
    }
}

fun List<Iota>.getEntityType(idx: Int, argc: Int = 0): EntityType<*> {
    val x = this.getOrElse(idx) { throw MishapNotEnoughArgs(idx + 1, this.size) }
    if (x is EntityTypeIota) {
        return x.entityType
    }
    if (x is EntityIota) {
        return x.entity.type
    }
    throw MishapInvalidIota.ofType(x, if (argc == 0) idx else argc - (idx + 1), "type.entity")
}

fun List<Iota>.getItemType(idx: Int, argc: Int = 0): Item {
    val x = this.getOrElse(idx) { throw MishapNotEnoughArgs(idx + 1, this.size) }
    if (x is ItemTypeIota) {
        return x.item
    }
    throw MishapInvalidIota.ofType(x, if (argc == 0) idx else argc - (idx + 1), "type.item")
}

fun List<Iota>.getItemStack(index: Int, argc: Int = 0): ItemStack {
    val x = this.getOrElse(index) { throw MishapNotEnoughArgs(index + 1, this.size) }
    return (x as? ItemStackIota)?.itemStack
        ?: throw MishapInvalidIota.of(x, if (argc == 0) index else argc - (index + 1), "item_stack")
}
