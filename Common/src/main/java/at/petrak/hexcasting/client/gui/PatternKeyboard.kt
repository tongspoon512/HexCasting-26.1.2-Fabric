package at.petrak.hexcasting.client.gui

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.casting.math.HexAngle
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.common.casting.PatternRegistryManifest
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

    /** Printable text delivered by a character event (ASCII, IME commits, other scripts). */
    fun charOfTypedEvent(codepoint: Int): String? {
        if (codepoint < 0x20 || codepoint == 0x7f) return null
        return String(Character.toChars(codepoint))
    }

    fun removeLastWord(text: String): String =
        text.split(' ').dropLast(1).joinToString(" ")

    /**
     * JustEnoughCharacters compatibility. JEC patches Hexcessible's `fluffySearch` by injecting
     * `HexcessibleHooks.mergeScore(score, query, candidate)`; our search is a port of that same
     * algorithm, so invoke the same hook reflectively. When JEC is absent the lookup fails once
     * and the score is left untouched, keeping JEC purely optional.
     */
    private val jecMergeScoreMethod: java.lang.reflect.Method? by lazy {
        runCatching {
            Class.forName("me.towdium.jecharacters.utils.HexcessibleHooks")
                .getMethod("mergeScore", Int::class.javaPrimitiveType, String::class.java, String::class.java)
        }.getOrNull()
    }

    private fun jecMergeScore(score: Int, query: String, candidate: String): Int {
        val merge = jecMergeScoreMethod ?: return score
        return runCatching {
            (merge.invoke(null, score, query, candidate) as Number).toInt()
        }.getOrDefault(score)
    }

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

    /**
     * Name for a drawn pattern. Registered actions resolve through the action registry (so aliases
     * apply), while number/mask literals resolve through their special handler, which supplies the
     * computed value or mask fingerprint (e.g. "数字之精思：4.00").
     */
    fun nameOf(pattern: HexPattern): Component? {
        entryOf(pattern.angles)?.let { return it.displayName() }
        // There is no CastingEnvironment on the client; the built-in handlers accept null. A third
        // party handler that insists on an env must not be able to take the screen down with it.
        return runCatching {
            PatternRegistryManifest.matchPatternToSpecialHandler(pattern, null)?.first?.name
        }.getOrNull()
    }

    /**
     * Fuzzy search over localized names, aliases and ids. Locked patterns are dropped from
     * [PatternSearchResult.suggestions] but counted in [PatternSearchResult.lockedCount].
     */
    fun search(query: String): PatternSearchResult {
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
        return PatternSearchResult(unlocked, matches.size - unlocked.size)
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
        // Fall back to JEC's pinyin matcher (e.g. "shuzizhisi" for 数字之精思) when the plain
        // subsequence match finds nothing.
        return if (qi == q.length) score else jecMergeScore(0, query, candidate)
    }
}
