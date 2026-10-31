package at.petrak.hexcasting.common.casting.actions.spells

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getItemEntity
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadCaster
import at.petrak.hexcasting.api.misc.MediaConstants
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.item.ItemEntity

object OpPickupItem : SpellAction {
    override val argc = 1

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        if (env.castingEntity !is ServerPlayer)
            throw MishapBadCaster()

        val item = args.getItemEntity(0, argc)
        env.assertEntityInRange(item)

        return SpellAction.Result(
            Spell(item),
            MediaConstants.DUST_UNIT / 100,
            listOf(ParticleSpray.burst(item.position(), 0.5))
        )
    }

    private data class Spell(val item: ItemEntity) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            val player = env.castingEntity as? ServerPlayer ?: return
            if (!item.isAlive || item.level() != player.level()) return

            // 和原版拾取保持一致：交给 ItemEntity.playerTouch 合并/入包、统计与音效。
            // 图案本身即代表“立刻拾取”，因此忽略物品刚掉落的拾取延迟。
            if (item.hasPickUpDelay()) item.setNoPickUpDelay()
            item.playerTouch(player)
        }
    }
}
