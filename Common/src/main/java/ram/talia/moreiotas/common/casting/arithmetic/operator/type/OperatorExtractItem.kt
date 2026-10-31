package ram.talia.moreiotas.common.casting.arithmetic.operator.type

import at.petrak.hexcasting.api.casting.arithmetic.operator.OperatorBasic
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaMultiPredicate
import at.petrak.hexcasting.api.casting.arithmetic.predicates.IotaPredicate
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.EntityIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.NullIota
import at.petrak.hexcasting.api.casting.iota.Vec3Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes.ENTITY
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes.VEC3
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Items
import ram.talia.moreiotas.api.asActionResult
import ram.talia.moreiotas.api.casting.iota.ItemStackIota
import ram.talia.moreiotas.common.lib.hex.MoreIotasIotaTypes.ITEM_STACK

object OperatorExtractItem : OperatorBasic(
    1, IotaMultiPredicate.all(
        IotaPredicate.any(
            IotaPredicate.ofType(VEC3), IotaPredicate.ofType(ENTITY), IotaPredicate.ofType(ITEM_STACK)
        )
    )
) {
    override fun apply(iotas: Iterable<Iota>, env: CastingEnvironment): Iterable<Iota> {
        val iota = iotas.first()

        return when (iota) {
            is Vec3Iota -> {
                if (!env.isVecInRange(iota.vec3)) {
                    listOf(NullIota())
                } else {
                    env.world.getBlockState(BlockPos.containing(iota.vec3)).block.asActionResult
                }
            }
            is EntityIota -> when (val entity = iota.entity) {
                is ItemEntity -> entity.item.item.asActionResult
                is ItemFrame -> entity.item.item.asActionResult
                else -> throw MishapInvalidIota.of(iota, 0, "blockitementityitemframeitem")
            }
            is ItemStackIota -> {
                if (iota.itemStack.isEmpty) Items.AIR.asActionResult
                else iota.itemStack.item.asActionResult
            }
            else -> error("iota argument to OperatorExtractItem must be one of Vec3, Entity, or ItemStack.")
        }
    }
}
