package gay.`object`.hexdebug.media

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.addldata.ADMediaHolder
import at.petrak.hexcasting.api.mod.HexConfig
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import java.util.UUID
import java.util.WeakHashMap

/**
 * Per-player media pool stored in attributes. Ported from HexOverpowered's personal mana pool.
 *
 * <p>The pool is only used when {@link HexConfig.ServerConfigAccess#personalMediaPool()} is on and
 * the player is a real server player (optionally after becoming enlightened).</p>
 */
class PersonalManaHolder private constructor(val player: Player) : ADMediaHolder {

    private val mediaAttribute = HexDebugAttributes.holder(HexDebugAttributes.PERSONAL_MEDIA)
    private val maxMediaAttribute = HexDebugAttributes.holder(HexDebugAttributes.PERSONAL_MEDIA_MAX)
    private val regenAttribute = HexDebugAttributes.holder(HexDebugAttributes.PERSONAL_MEDIA_REGEN)

    override fun getMedia(): Long {
        tryContinue()
        return player.getAttributeBaseValue(mediaAttribute).toLong()
    }

    override fun getMaxMedia(): Long = player.getAttributeValue(maxMediaAttribute).toLong()

    override fun setMedia(media: Long) {
        val target = media.coerceAtLeast(0L).toDouble()
        player.getAttribute(mediaAttribute)?.baseValue = target
        CACHED_MEDIA[player.uuid] = target
    }

    override fun canRecharge() = false

    override fun canProvide() = true

    override fun getConsumptionPriority() = Int.MAX_VALUE

    override fun canConstructBattery() = false

    fun getMediaRegenStep(): Long = player.getAttributeValue(regenAttribute).toLong()

    /**
     * Attributes are recreated from their defaults whenever a player instance is rebuilt
     * (dimension change, respawn...). The marker lets us restore the value cached for this session
     * instead of resetting the pool to zero.
     */
    fun tryContinue() {
        if (player !is ServerPlayer) return
        val attr = player.getAttribute(mediaAttribute) ?: return
        if (attr.baseValue != HexDebugAttributes.INIT_MEDIA_MARKER) return
        attr.baseValue = CACHED_MEDIA[player.uuid] ?: 0.0
    }

    companion object {
        private val HOLDERS = WeakHashMap<Player, PersonalManaHolder>()
        private val CACHED_MEDIA = HashMap<UUID, Double>()

        @JvmStatic
        fun get(player: Player?): PersonalManaHolder? =
            if (player == null) null
            else HOLDERS.getOrPut(player) { PersonalManaHolder(player) }

        @JvmStatic
        fun enablesManaForPlayer(player: Player): Boolean {
            if (!HexConfig.server().personalMediaPool()) return false
            if (player !is ServerPlayer) return false
            if (!HexConfig.server().personalMediaAfterEnlightened()) return true

            val advancement = player.level().server.advancements.get(HexAPI.modLoc("enlightenment")) ?: return false
            return player.advancements.getOrStartProgress(advancement).isDone
        }
    }
}
