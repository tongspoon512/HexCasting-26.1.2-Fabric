package at.petrak.hexcasting.common.casting.actions.spells

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getEntity
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.api.misc.MediaConstants
import net.minecraft.world.entity.animal.Animal

/**
 * 生命的节拍·繁殖：让一只成年动物进入恋爱状态，效果等同于喂食繁殖物品。
 * 不绑定施法者，分析机也可以施放。
 */
object OpLifeBreed : SpellAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val entity = args.getEntity(0, argc)
        env.assertEntityInRange(entity)

        val animal = entity as? Animal
        if (animal == null || animal.isBaby) {
            throw MishapInvalidIota.of(args[0], 0, "animal")
        }

        return SpellAction.Result(
            Spell(animal),
            MediaConstants.DUST_UNIT * 2,
            listOf(ParticleSpray.burst(animal.position(), 1.0))
        )
    }

    private data class Spell(val animal: Animal) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            if (!animal.isAlive || animal.isBaby) return
            // 已经在恋爱中就不叠加，避免连续施法无限延长
            if (animal.inLoveTime > 0) return
            animal.inLoveTime = 600
        }
    }
}
