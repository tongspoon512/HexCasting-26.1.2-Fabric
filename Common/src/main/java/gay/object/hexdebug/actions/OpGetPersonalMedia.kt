package gay.`object`.hexdebug.actions

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.DoubleIota
import at.petrak.hexcasting.api.casting.iota.Iota
import gay.`object`.hexdebug.media.HexDebugAttributes
import net.minecraft.world.entity.ai.attributes.Attribute

/**
 * Reads the caster's personal media attribute. Ported from HexOverpowered's {@code get_personal_media}.
 */
class OpGetPersonalMedia(private val attr: Attribute) : ConstMediaAction {
    override val argc = 0

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val caster = env.caster
        return listOf(DoubleIota(caster?.getAttributeValue(HexDebugAttributes.holder(attr)) ?: 0.0))
    }
}
