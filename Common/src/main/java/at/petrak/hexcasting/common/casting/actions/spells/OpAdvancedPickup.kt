package at.petrak.hexcasting.common.casting.actions.spells

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.getItemEntity
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadBlock
import at.petrak.hexcasting.api.misc.MediaConstants
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.block.entity.HopperBlockEntity

object OpAdvancedPickup : SpellAction {
    override val argc: Int
        get() = 2

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        val item = args.getItemEntity(0, argc)
        val pos = args.getBlockPos(1, argc)

        env.assertEntityInRange(item)
        env.assertPosInRangeForEditing(pos)

        if (HopperBlockEntity.getContainerAt(env.world, pos) == null)
            throw MishapBadBlock.of(pos, "container")

        return SpellAction.Result(
            Spell(item, pos),
            MediaConstants.DUST_UNIT / 10,
            listOf(ParticleSpray.burst(item.position(), 1.0))
        )
    }

    private data class Spell(val item: ItemEntity, val pos: BlockPos) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            if (!item.isAlive || item.level() != env.world) return
            val container = HopperBlockEntity.getContainerAt(env.world, pos) ?: return

            // 与原版漏斗一致：按容器的槽位规则插入，放不下的部分留在掉落物实体上。
            HopperBlockEntity.addItem(container, item)
        }
    }
}
