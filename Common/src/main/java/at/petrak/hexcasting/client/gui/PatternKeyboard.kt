package at.petrak.hexcasting.client.gui

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.casting.math.HexAngle
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.common.lib.hex.HexActions
import net.minecraft.network.chat.Component

internal data class PatternSearchResult(
    val suggestions: List<PatternEntry>,
    val lockedCount: Int,
)

internal data class PatternEntry(
    val id: String,
    val startDir: HexDir,
    val angles: List<HexAngle>,
    private val nameKey: String,
) {
    val signature: String get() = PatternKeyboard.signature(this.angles)
    val signatureWithDir: String get() = "<${this.startDir.name.lowercase()}, $signature>"

    fun rawName(): Component = Component.translatable(this.nameKey)
    fun displayName(): Component = PatternAliases.nameOf(this.id, this.nameKey)
    fun displayNameString(): String = this.displayName().string
}

/**
 * Support code for the Hexcessible-style idle pattern tooltips, keyboard drawing, alias editing
 * and the fuzzy autocomplete.
 *
 * The key handling deliberately uses raw GLFW key codes (the same numbers
 * [net.minecraft.client.input.KeyEvent] reports) so nothing has to touch a character event.
 */
internal object PatternKeyboard {
    // GLFW key codes, named here to keep the imports minimal.
    const val KEY_BACKSPACE = 259
    const val KEY_TAB = 258
    const val KEY_ENTER = 257
    const val KEY_KP_ENTER = 335
    const val KEY_ESCAPE = 256
    const val KEY_SPACE = 32
    const val KEY_LEFT = 263
    const val KEY_RIGHT = 262
    const val KEY_UP = 265
    const val KEY_DOWN = 264
    const val KEY_H = 72
    const val KEY_J = 74
    const val KEY_K = 75
    const val KEY_L = 76
    const val KEY_Q = 81
    const val KEY_W = 87
    const val KEY_E = 69
    const val KEY_A = 65
    const val KEY_D = 68
    const val KEY_S = 83
    const val KEY_R = 82
    const val KEY_0 = 48
    const val KEY_9 = 57
    const val KEY_MINUS = 45

    /** GLFW_MOD_SHIFT */
    const val MOD_SHIFT = 1

    /** GLFW_MOD_CONTROL */
    const val MOD_CONTROL = 2

    /** Relative angle keys, mirroring Hexcessible's `q/w/e/a/d` scheme. */
    fun angleOfKey(key: Int): HexAngle? = when (key) {
        KEY_Q -> HexAngle.LEFT
        KEY_W -> HexAngle.FORWARD
        KEY_E -> HexAngle.RIGHT
        KEY_A -> HexAngle.LEFT_BACK
        KEY_D -> HexAngle.RIGHT_BACK
        else -> null
    }

    fun keyOfAngle(angle: HexAngle): Char = when (angle) {
        HexAngle.LEFT -> 'q'
        HexAngle.FORWARD -> 'w'
        HexAngle.RIGHT -> 'e'
        HexAngle.LEFT_BACK -> 'a'
        HexAngle.RIGHT_BACK -> 'd'
        HexAngle.BACK -> 's'
    }

    fun signature(angles: List<HexAngle>): String = buildString {
        for (angle in angles) append(keyOfAngle(angle))
    }

    /** Text a search box / alias editor should receive for the given key, if any. */
    fun charOfKey(key: Int, modifiers: Int): Char? {
        if (key in KEY_A..(KEY_A + 25)) {
            val lower = ('a' + (key - KEY_A))
            return if (modifiers and MOD_SHIFT != 0) lower.uppercaseChar() else lower
        }
        if (key in KEY_0..KEY_9) return ('0' + (key - KEY_0))
        if (key == KEY_SPACE) return ' '
        if (key == KEY_MINUS) return if (modifiers and MOD_SHIFT != 0) '_' else '-'
        return null
    }

    fun removeLastWord(text: String): String =
        text.split(' ').dropLast(1).joinToString(" ")

    private var entries: List<PatternEntry>? = null
    private var byAngles: Map<List<HexAngle>, PatternEntry>? = null

    private fun allEntries(): List<PatternEntry> {
        entries?.let { return it }
        val built = ArrayList<PatternEntry>()
        for (entry in HexActions.REGISTRY.entrySet()) {
            val prototype = entry.value.prototype
            built.add(
                PatternEntry(
                    entry.key.identifier().toString(),
                    prototype.startDir,
                    prototype.angles.toList(),
                    HexAPI.instance().getActionI18nKey(entry.key)
                )
            )
        }
        entries = built
        return built
    }

    private fun angleIndex(): Map<List<HexAngle>, PatternEntry> {
        byAngles?.let { return it }
        val built = HashMap<List<HexAngle>, PatternEntry>()
        for (entry in allEntries()) built.putIfAbsent(entry.angles, entry)
        byAngles = built
        return built
    }

    /** Entry matching an angle signature; like [HexPattern.sigsEqual] the start direction is ignored. */
    fun entryOf(angles: List<HexAngle>): PatternEntry? = angleIndex()[angles]

    fun nameOf(angles: List<HexAngle>): Component? = entryOf(angles)?.displayName()

    /**
     * Fuzzy search over localized names, aliases and ids. Locked patterns are dropped from
     * [PatternSearchResult.suggestions] but counted in [PatternSearchResult.lockedCount].
     */
    fun search(query: String, limit: Int): PatternSearchResult {
        val all = allEntries()
        val matches = if (query.isBlank()) {
            all.sortedBy { it.displayNameString().lowercase() }
        } else {
            val trimmed = query.trim()
            all
                .map { entry ->
                    val idText = entry.id.replace(':', ' ').replace('_', ' ').replace('/', ' ')
                    val score = fluffySearch(trimmed, entry.displayNameString()) * 3 +
                        fluffySearch(trimmed, idText)
                    entry to score
                }
                .filter { it.second > 0 }
                .sortedByDescending { it.second }
                .map { it.first }
        }

        val unlocked = matches.filterNot { PatternDocs.isLocked(it.id) }
        return PatternSearchResult(unlocked.take(limit), matches.size - unlocked.size)
    }

    private fun fluffySearch(query: String, candidate: String): Int {
        if (query.isEmpty()) return 0
        val q = query.lowercase()
        val c = candidate.lowercase()
        var score = 0
        var consecutive = 0
        var qi = 0
        for (ci in c.indices) {
            if (qi >= q.length) break
            if (q[qi] == c[ci]) {
                score += 10 + consecutive * 5
                consecutive++
                qi++
            } else {
                consecutive = 0
            }
        }
        if (c.startsWith(q)) score += 15
        return if (qi == q.length) score else 0
    }
}
