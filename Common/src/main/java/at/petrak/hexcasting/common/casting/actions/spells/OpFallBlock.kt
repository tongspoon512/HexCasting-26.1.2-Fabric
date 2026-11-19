package at.petrak.hexcasting.common.casting.actions.spells

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getVec3
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadBlock
import at.petrak.hexcasting.api.misc.MediaConstants
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.item.FallingBlockEntity
import net.minecraft.world.level.block.FallingBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3

/**
 * 让指定位置的方块变成下落方块实体。
 * 不接受容器与方块实体，避免复制内容或丢失物品。
 */
object OpFallBlock : SpellAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val pos = BlockPos.containing(args.getVec3(0, argc))
        env.assertPosInRangeForEditing(pos)

        if (!isFallable(env, pos)) {
            throw MishapBadBlock.of(pos, "fallable")
        }

        return SpellAction.Result(
            Spell(pos),
            MediaConstants.DUST_UNIT,
            listOf(ParticleSpray.burst(Vec3.atCenterOf(pos), 1.0))
        )
    }

    private fun isFallable(env: CastingEnvironment, pos: BlockPos): Boolean {
        val state = env.world.getBlockState(pos)
        return !state.isAir &&
            !state.liquid() &&
            !state.hasBlockEntity() &&
            state.getDestroySpeed(env.world, pos) >= 0.0f &&
            state.block !is FallingBlock
    }

    private data class Spell(val pos: BlockPos) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            if (!env.canEditBlockAt(pos)) return
            if (!isFallable(env, pos)) return

            val state: BlockState = env.world.getBlockState(pos)
            env.world.removeBlock(pos, false)
            FallingBlockEntity.fall(env.world, pos, state)
        }
    }
}
