package gay.`object`.hexdebug.actions

import at.petrak.hexcasting.api.casting.ActionRegistryEntry
import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.media.HexDebugAttributes
import net.minecraft.resources.Identifier
import java.util.function.BiConsumer

/**
 * HexDebug patterns, registered into the shared action registry from
 * {@code FabricHexInitializer.initRegistries()}.
 */
object HexDebugActions {
    private val ACTIONS = LinkedHashMap<Identifier, ActionRegistryEntry>()

    init {
        wrap("get_personal_media", "qqaqqea", HexDir.EAST, OpGetPersonalMedia(HexDebugAttributes.PERSONAL_MEDIA))
        wrap("get_personal_media_max", "qqaqqqd", HexDir.EAST, OpGetPersonalMedia(HexDebugAttributes.PERSONAL_MEDIA_MAX))
        wrap("mind_env/schedule", "waawedaqqqqdeaqq", HexDir.SOUTH_WEST, OpScheduleCall)
        wrap("mind_env/running_code", "wqaqwweeeeeqdeaqq", HexDir.SOUTH_WEST, OpScheduledCode)
    }

    @JvmStatic
    fun register(reg: BiConsumer<ActionRegistryEntry, Identifier>) {
        for (e in ACTIONS.entries) {
            reg.accept(e.value, e.key)
        }
    }

    private fun wrap(name: String, signature: String, dir: HexDir, action: Action) {
        ACTIONS[HexDebug.id(name)] = ActionRegistryEntry(HexPattern.fromAngles(signature, dir), action)
    }
}
