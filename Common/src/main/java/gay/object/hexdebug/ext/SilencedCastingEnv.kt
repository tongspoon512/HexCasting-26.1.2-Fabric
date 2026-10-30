package gay.`object`.hexdebug.ext

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.env.PlayerBasedCastEnv
import at.petrak.hexcasting.api.casting.eval.sideeffects.OperatorSideEffect
import at.petrak.hexcasting.api.mod.HexConfig
import net.minecraft.network.chat.Component

/**
 * Casting environment used by delayed spells so they do not spam chat every tick. Ported from
 * HexOverpowered; 26.1's {@link PlayerBasedCastEnv} constructor already supplies the world.
 */
class SilencedCastingEnv(private val master: PlayerBasedCastEnv) :
    PlayerBasedCastEnv(master.caster, master.castingHand) {

    override fun extractMediaEnvironment(amount: Long, simulate: Boolean): Long =
        master.extractMedia(amount, simulate)

    override fun getCastingHand() = master.castingHand

    override fun getPigment() = master.pigment

    override fun printMessage(message: Component) {}

    override fun sendMishapMsgToPlayer(mishap: OperatorSideEffect.DoMishap) {}

    companion object {
        @JvmStatic
        fun from(env: CastingEnvironment): CastingEnvironment {
            if (HexConfig.server().bypassesSilencedEnvRestrictions()) return env
            if (env is SilencedCastingEnv) return env
            if (env is PlayerBasedCastEnv) return SilencedCastingEnv(env)
            return env
        }
    }
}
