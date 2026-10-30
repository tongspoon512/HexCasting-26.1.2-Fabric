package gay.`object`.hexdebug.media

import at.petrak.hexcasting.api.mod.HexConfig
import net.minecraft.server.level.ServerLevel

/**
 * Regenerates the personal media pool every {@code personalMediaRegenInterval} ticks. Hooked to
 * {@code ServerTickEvents.END_LEVEL_TICK} instead of a {@code ServerPlayer.tick} mixin.
 */
object PersonalMediaRegen {

    @JvmStatic
    fun tick(level: ServerLevel) {
        val interval = HexConfig.server().personalMediaRegenInterval().coerceAtLeast(1)
        for (player in level.players()) {
            if (!PersonalManaHolder.enablesManaForPlayer(player)) continue
            if (player.tickCount % interval != 0) continue
            val holder = PersonalManaHolder.get(player) ?: continue
            holder.insertMedia(holder.getMediaRegenStep(), false)
        }
    }
}
