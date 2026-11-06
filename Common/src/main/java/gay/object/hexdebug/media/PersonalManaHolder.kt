package gay.`object`.hexdebug.media

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.addldata.ADMediaHolder
import at.petrak.hexcasting.api.mod.HexConfig
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.AdvancementProgress
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.WeakHashMap

/**
 * Per-player media pool stored in attributes. Ported from HexOverpowered's personal mana pool.
 *
 * <p>The pool is only used when {@link HexConfig.ServerConfigAccess#personalMediaPool()} is on and
 * the player is a real server player (optionally after becoming enlightened).</p>
 */
class PersonalManaHolder private constructor(player: Player) : ADMediaHolder {

    // The holder lives in a WeakHashMap keyed by the player; a strong reference here would keep
    // that key alive forever and leak every player instance (dimension changes, respawns, relogs).
    private val playerRef = WeakReference(player)
    private val player: Player?
        get() = playerRef.get()

    private val mediaAttribute = HexDebugAttributes.holder(HexDebugAttributes.PERSONAL_MEDIA)
    private val maxMediaAttribute = HexDebugAttributes.holder(HexDebugAttributes.PERSONAL_MEDIA_MAX)
    private val regenAttribute = HexDebugAttributes.holder(HexDebugAttributes.PERSONAL_MEDIA_REGEN)

    override fun getMedia(): Long {
        val player = player ?: return 0
        tryContinue()
        return player.getAttributeBaseValue(mediaAttribute).toLong()
    }

    override fun getMaxMedia(): Long {
        val player = player ?: return 0
        return player.getAttributeValue(maxMediaAttribute).toLong()
    }

    override fun setMedia(media: Long) {
        val player = player ?: return
        val target = media.coerceAtLeast(0L).toDouble()
        player.getAttribute(mediaAttribute)?.baseValue = target
        CACHED_MEDIA[player.uuid] = target
    }

    override fun canRecharge() = false

    override fun canProvide() = true

    override fun getConsumptionPriority() = Int.MAX_VALUE

    override fun canConstructBattery() = true

    fun getMediaRegenStep(): Long = player?.getAttributeValue(regenAttribute)?.toLong() ?: 0

    /**
     * Attributes are recreated from their defaults whenever a player instance is rebuilt
     * (dimension change, respawn...). The marker lets us restore the value cached for this session
     * instead of resetting the pool to zero.
     */
    fun tryContinue() {
        val player = player ?: return
        if (player !is ServerPlayer) return
        val attr = player.getAttribute(mediaAttribute) ?: return
        if (attr.baseValue != HexDebugAttributes.INIT_MEDIA_MARKER) return
        attr.baseValue = CACHED_MEDIA[player.uuid] ?: 0.0
    }

    companion object {
        private val HOLDERS = WeakHashMap<Player, PersonalManaHolder>()
        private val CACHED_MEDIA = HashMap<UUID, Double>()

        // The advancement object is registry data, so resolve it once instead of on every tick.
        private var cachedEnlightenment: AdvancementHolder? = null
        private val ENLIGHTENMENT_PROGRESS = WeakHashMap<Player, AdvancementProgress>()

        @JvmStatic
        fun get(player: Player?): PersonalManaHolder? {
            if (player == null) return null
            // The client and the integrated server each look up their own player from different
            // threads, so the weak map needs to be protected.
            return synchronized(HOLDERS) { HOLDERS.getOrPut(player) { PersonalManaHolder(player) } }
        }

        /** Drops registry-derived caches; holders/progress become stale after a data pack reload. */
        @JvmStatic
        fun clearCaches() {
            cachedEnlightenment = null
            ENLIGHTENMENT_PROGRESS.clear()
        }

        /** Drops the session cache for a leaving player so it cannot pile up per UUID. */
        @JvmStatic
        fun forget(player: Player) {
            CACHED_MEDIA.remove(player.uuid)
            synchronized(HOLDERS) { HOLDERS.remove(player) }
            ENLIGHTENMENT_PROGRESS.remove(player)
        }

        @JvmStatic
        fun enablesManaForPlayer(player: Player): Boolean {
            val server = (player as? ServerPlayer)?.level()?.server ?: return false
            if (!HexConfig.server().personalMediaPool()) return false
            if (!HexConfig.server().personalMediaAfterEnlightened()) return true

            val advancement = cachedEnlightenment
                ?: server.advancements.get(HexAPI.modLoc("enlightenment"))?.also { cachedEnlightenment = it }
                ?: return false
            val progress = ENLIGHTENMENT_PROGRESS.getOrPut(player) {
                player.advancements.getOrStartProgress(advancement)
            }
            return progress.isDone
        }
    }
}
