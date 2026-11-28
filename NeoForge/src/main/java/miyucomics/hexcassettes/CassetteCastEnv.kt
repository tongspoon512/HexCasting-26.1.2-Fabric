package miyucomics.hexcassettes

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.eval.env.PlayerBasedCastEnv
import at.petrak.hexcasting.api.pigment.FrozenPigment
import at.petrak.hexcasting.xplat.IXplatAbstractions
import miyucomics.hexcassettes.advancements.CassetteTrigger
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand

/**
 * 卡带施法环境：视同玩家以 [hand] 施法，从物品栏而非个人媒质池抽取媒质。
 *
 * [key] 是当前卡带的标签，供 self/busy 等图案识别；[depth] 是嵌套层数。
 */
class CassetteCastEnv(
    caster: ServerPlayer,
    private val hand: InteractionHand,
    val key: String,
    val depth: Int,
) : PlayerBasedCastEnv(caster, hand) {
    init {
        // 嵌套层数达到 9 时视为触发了守护进程
        if (depth >= 9) {
            CassetteTrigger.QUINIO.trigger(caster)
        }
    }

    override fun getCastingHand(): InteractionHand = hand

    override fun getPigment(): FrozenPigment = IXplatAbstractions.INSTANCE.getPigment(caster)

    override fun extractMediaEnvironment(costLeft: Long, simulate: Boolean): Long =
        if (caster.isCreative) 0 else extractMediaFromInventory(costLeft, true, simulate)

    override fun produceParticles(particles: ParticleSpray, colorizer: FrozenPigment) {}
}
