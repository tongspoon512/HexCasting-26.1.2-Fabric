package at.petrak.hexcasting.client.gui

import at.petrak.hexcasting.client.guidebook.GuideBook
import net.minecraft.advancements.AdvancementProgress
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientAdvancements
import net.minecraft.resources.Identifier
import java.lang.reflect.Field

/** One guidebook page describing a pattern: its stack signature and prose. */
internal data class PatternDoc(val args: String, val desc: String)

/**
 * Index of the guidebook's `hexcasting:pattern` pages, keyed by action id, plus the
 * advancement that unlocks each action. The autocomplete list uses this to show the
 * implementation args/description and to hide patterns the player has not unlocked yet.
 */
internal object PatternDocs {
    private var docs: Map<String, List<PatternDoc>>? = null
    private var locks: Map<String, String>? = null

    /**
     * `ClientAdvancements` exposes no progress accessor, so grab its progress map directly.
     * If the field cannot be found the lock filter is simply disabled.
     */
    private val progressField: Field? by lazy {
        runCatching {
            ClientAdvancements::class.java.declaredFields
                .firstOrNull { Map::class.java.isAssignableFrom(it.type) && !it.isSynthetic }
                ?.apply { isAccessible = true }
        }.getOrNull()
    }

    private fun ensureIndexed() {
        if (docs != null) return
        val docMap = HashMap<String, MutableList<PatternDoc>>()
        val lockMap = HashMap<String, String>()
        for (entry in GuideBook.get().entries) {
            for (page in entry.pages) {
                if (page.type != "hexcasting:pattern") continue
                val opId = page.string("op_id", page.string("anchor"))
                if (opId.isBlank()) continue
                docMap.getOrPut(opId) { ArrayList() }
                    .add(PatternDoc(formatArgs(page.string("input"), page.string("output")),
                        cleanText(GuideBook.string(page.string("text")))))
                entry.advancement?.let { lockMap.putIfAbsent(opId, it) }
            }
        }
        docs = docMap
        locks = lockMap
    }

    fun docsOf(actionId: String): List<PatternDoc> {
        ensureIndexed()
        return docs?.get(actionId).orEmpty()
    }

    /** Whether the guidebook entry that documents [actionId] is still locked by an advancement. */
    fun isLocked(actionId: String): Boolean {
        ensureIndexed()
        val advancementId = locks?.get(actionId) ?: return false
        val field = progressField ?: return false
        val connection = Minecraft.getInstance().connection ?: return false
        // Fail open: if the client cannot tell us about the advancement, don't hide the pattern.
        val holder = connection.advancements.get(Identifier.tryParse(advancementId) ?: return false)
            ?: return false
        val progress = runCatching { field.get(connection.advancements) }.getOrNull()
        val entry = (progress as? Map<*, *>)?.get(holder) as? AdvancementProgress ?: return false
        return !entry.isDone
    }

    private fun formatArgs(input: String, output: String): String = when {
        input.isBlank() && output.isBlank() -> ""
        input.isBlank() -> "\u2192 $output"
        output.isBlank() -> "$input \u2192"
        else -> "$input \u2192 $output"
    }

    /** Strip the guidebook's `$(...)` markup and mask the underscores it uses for emphasis. */
    private fun cleanText(text: String): String =
        text.replace(Regex("\\$\\([^)]*\\)|/\\$"), "")
            .replace(Regex("[\\s^]_"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
