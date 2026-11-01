package at.petrak.hexcasting.common.lib.hexparse

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.casting.PatternShapeMatch
import at.petrak.hexcasting.api.casting.eval.env.StaffCastEnv
import at.petrak.hexcasting.api.casting.iota.BooleanIota
import at.petrak.hexcasting.api.casting.iota.DoubleIota
import at.petrak.hexcasting.api.casting.iota.EntityIota
import at.petrak.hexcasting.api.casting.iota.GarbageIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.NullIota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.iota.Vec3Iota
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.api.utils.isOfTag
import at.petrak.hexcasting.common.casting.PatternRegistryManifest
import at.petrak.hexcasting.common.casting.actions.math.SpecialHandlerNumberLiteral
import at.petrak.hexcasting.common.casting.actions.stack.SpecialHandlerMask
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes
import at.petrak.hexcasting.xplat.IXplatAbstractions
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.ejml.simple.SimpleMatrix
import ram.talia.moreiotas.api.casting.iota.EntityTypeIota
import ram.talia.moreiotas.api.casting.iota.IotaTypeIota
import ram.talia.moreiotas.api.casting.iota.ItemTypeIota
import ram.talia.moreiotas.api.casting.iota.MatrixIota
import ram.talia.moreiotas.api.casting.iota.StringIota
import java.util.ArrayDeque
import java.util.UUID

/**
 * 在 iota 列表与可读文本（HexParse 风格）之间互相转换。
 *
 * 文本格式与 https://github.com/YukkuriC/HexParseMod 一致，例如：
 * `[add, 1, vec_1_2, "hello"]`。列表用 `[` / `]`，元素用空白、`;` 或 `,` 分隔，
 * 行注释与块注释会被忽略。
 */
object IotaCode {
    private const val TYPE_LIST = "hexcasting:list"
    private const val TYPE_PATTERN = "hexcasting:pattern"
    private const val TYPE_DOUBLE = "hexcasting:double"
    private const val TYPE_VEC3 = "hexcasting:vec3"
    private const val TYPE_ENTITY = "hexcasting:entity"
    private const val TYPE_BOOLEAN = "hexcasting:boolean"
    private const val TYPE_NULL = "hexcasting:null"
    private const val TYPE_GARBAGE = "hexcasting:garbage"

    private const val TYPE_STRING = "moreiotas:string"
    private const val TYPE_MATRIX = "moreiotas:matrix"
    private const val TYPE_IOTA_TYPE = "moreiotas:iota_type"
    private const val TYPE_ENTITY_TYPE = "moreiotas:entity_type"
    private const val TYPE_ITEM_TYPE = "moreiotas:item_type"

    private val TOKEN = Regex("\\\\|\\(|\\)|\\{|\\}|\\[|]|[\\w./\\-:#\u0100-\uffff]+")
    private val NUMBER = Regex("[0-9.\\-]+(e[0-9.\\-]+)?")
    private val RAW_PATTERN = Regex("_[wedsaq]*")
    private val MASK = Regex("mask_[-v]+")

    private val SPECIAL_PATTERNS = mapOf("qqq" to "(", "eee" to ")", "qqqaw" to "\\")
    private val SPECIAL_PATTERN_IMPORT = mapOf(
        "(" to HexPattern.fromAnglesUnchecked("qqq", HexDir.WEST),
        ")" to HexPattern.fromAnglesUnchecked("eee", HexDir.EAST),
        "\\" to HexPattern.fromAnglesUnchecked("qqqaw", HexDir.WEST),
    )

    private class PlayerContext(val player: ServerPlayer) {
        /** 图案名 -> 图案（action id、以及 hexcasting 命名空间下的短名） */
        val patterns: Map<String, HexPattern> by lazy {
            val out = HashMap<String, HexPattern>()
            val registry = IXplatAbstractions.INSTANCE.actionRegistry
            val overworld = player.level().server?.overworld()
            if (overworld != null) {
                for (key in registry.registryKeySet()) {
                    val entry = registry.getOptional(key).orElse(null) ?: continue
                    val pat = if (isOfTag(registry, key, HexTags.Actions.PER_WORLD_PATTERN))
                        PatternRegistryManifest.getCanonicalStrokesPerWorld(key, overworld)
                    else entry.prototype()
                    if (pat == null) continue
                    val id = key.identifier()
                    out[id.toString()] = pat
                    if (id.namespace == HexAPI.MOD_ID) out[id.path] = pat
                    else out.putIfAbsent(id.path, pat)
                }
            }
            out
        }
    }

    // ------------------------------------------------------------------ parse

    @JvmStatic
    fun splitCode(code: String): MutableList<String> {
        val out = ArrayList<String>()
        var rest = code
        while (rest.isNotEmpty()) {
            val c = rest[0]
            when {
                c == '"' -> {
                    var i = 1
                    var closed = false
                    while (i < rest.length) {
                        if (rest[i] == '\\') {
                            i += 2
                            continue
                        }
                        if (rest[i] == '"') {
                            closed = true
                            break
                        }
                        i++
                    }
                    if (!closed) throw IllegalArgumentException("unclosed string literal")
                    out.add(rest.substring(0, i + 1))
                    rest = rest.substring(i + 1)
                }

                c == '/' && rest.length > 1 && rest[1] == '/' -> {
                    val newline = rest.indexOf('\n')
                    rest = if (newline < 0) "" else rest.substring(newline)
                }

                c == '/' && rest.length > 1 && rest[1] == '*' -> {
                    val end = rest.indexOf("*/", 2)
                    if (end < 0) throw IllegalArgumentException("unclosed block comment")
                    rest = rest.substring(end + 2)
                }

                c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == ';' || c == ',' ->
                    rest = rest.substring(1)

                else -> {
                    val match = TOKEN.find(rest)
                    if (match != null && match.range.first == 0) {
                        out.add(match.value)
                        rest = rest.substring(match.value.length)
                    } else {
                        // 无法识别的字符直接丢弃，避免死循环
                        rest = rest.substring(1)
                    }
                }
            }
        }
        return out
    }

    @JvmStatic
    fun parseCode(code: String, player: ServerPlayer): Iota = parseNodes(splitCode(code), player)

    @JvmStatic
    fun parseNodes(nodes: List<String>, player: ServerPlayer): Iota {
        val ctx = PlayerContext(player)
        val stack = ArrayDeque<MutableList<Iota>>()
        stack.addLast(ArrayList())
        for (node in nodes) {
            when (node) {
                "[" -> stack.addLast(ArrayList())
                "]" -> {
                    if (stack.size <= 1) {
                        warn(player, "unmatched ']'")
                        continue
                    }
                    val inner = stack.removeLast()
                    stack.peekLast().add(ListIota(inner))
                }

                else -> {
                    try {
                        val parsed = parseNode(node, ctx)
                        if (parsed == null) {
                            warn(player, "unknown symbol: $node")
                        } else {
                            stack.peekLast().add(parsed)
                        }
                    } catch (e: Throwable) {
                        warn(player, "cannot parse '$node': ${e.message ?: e.javaClass.simpleName}")
                    }
                }
            }
        }
        while (stack.size > 1) {
            val inner = stack.removeLast()
            stack.peekLast().add(ListIota(inner))
        }
        return ListIota(stack.removeFirst())
    }

    private fun parseNode(node: String, ctx: PlayerContext): Iota? {
        SPECIAL_PATTERN_IMPORT[node]?.let { return PatternIota(it) }
        when (node.lowercase()) {
            "self", "myself" -> return EntityIota(ctx.player)
            "true" -> return BooleanIota(true)
            "false" -> return BooleanIota(false)
            "null" -> return NullIota()
            "garbage" -> return GarbageIota()
        }

        ctx.patterns[node]?.let { return PatternIota(it) }

        if (NUMBER.matches(node)) {
            return DoubleIota(node.toDoubleOrNull() ?: Double.NaN)
        }

        if (node.startsWith("vec")) {
            val frags = node.split("_")
            val axes = DoubleArray(3)
            for (i in 1..3) {
                axes[i - 1] = frags.getOrNull(i)?.toDoubleOrNull() ?: 0.0
            }
            return Vec3Iota(Vec3(axes[0], axes[1], axes[2]))
        }

        if (MASK.matches(node)) {
            return PatternIota(patternFromMask(node))
        }

        if (node.startsWith("num")) {
            val num = node.split("_").getOrNull(1)?.toDoubleOrNull() ?: 0.0
            return PatternIota(
                HexPattern.fromAnglesUnchecked(anglesFromNum(num), if (num < 0) HexDir.NORTH_EAST else HexDir.SOUTH_EAST)
            )
        }

        if (node.startsWith("entity_")) {
            val uuid = try {
                UUID.fromString(node.substring(7))
            } catch (e: IllegalArgumentException) {
                null
            }
            val level = ctx.player.level() as? ServerLevel
            val entity = if (uuid != null && level != null) level.getEntity(uuid) else null
            if (entity != null) {
                if (entity is Player && entity != ctx.player) {
                    throw IllegalArgumentException("refusing to reference another player")
                }
                return EntityIota(entity)
            }
            return NullIota()
        }

        if (RAW_PATTERN.matches(node)) {
            return PatternIota(HexPattern.fromAnglesUnchecked(node.substring(1), HexDir.EAST))
        }

        // MoreIotas
        if (node.startsWith("type/entity_")) {
            val id = resourceId(node, "minecraft:")
            val type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null)
                ?: throw IllegalArgumentException("unknown entity type: $id")
            return EntityTypeIota(type)
        }

        if (node.startsWith("type/item_")) {
            val id = resourceId(node, "minecraft:")
            val item = BuiltInRegistries.ITEM.getOptional(id).orElse(null)
                ?: throw IllegalArgumentException("unknown item: $id")
            return ItemTypeIota(item)
        }

        if (node.startsWith("type/block_")) {
            val id = resourceId(node, "minecraft:")
            val block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null)
                ?: throw IllegalArgumentException("unknown block: $id")
            return ItemTypeIota(block)
        }

        if (node.startsWith("type_") || node.startsWith("type/iota_")) {
            var id = node.substring(node.indexOf('_') + 1)
            if (!id.contains(':')) id = "hexcasting:$id"
            val loc = Identifier.tryParse(id) ?: throw IllegalArgumentException("invalid iota type: $id")
            val type = HexIotaTypes.REGISTRY.getOptional(loc).orElse(null)
                ?: throw IllegalArgumentException("unknown iota type: $loc")
            return IotaTypeIota(type)
        }

        if (node.startsWith("str_")) {
            return StringIota.makeUnchecked(node.substring(node.indexOf('_') + 1))
        }

        if (node.startsWith("\"") && node.endsWith("\"") && node.length >= 2) {
            return StringIota.makeUnchecked(unescape(node.substring(1, node.length - 1)))
        }

        if (node.startsWith("mat_") || node.startsWith("matrix_")) {
            return matrixFromCode(node)
        }

        return null
    }

    private fun resourceId(node: String, defaultNamespace: String): Identifier {
        var id = node.substring(node.indexOf('_') + 1)
        if (!id.contains(':')) id = defaultNamespace + id
        return Identifier.tryParse(id) ?: throw IllegalArgumentException("invalid id: $id")
    }

    private fun patternFromMask(node: String): HexPattern {
        val seq = StringBuilder()
        var line = true
        var start = HexDir.EAST
        if (node[5] == 'v') {
            line = false
            seq.append('a')
            start = HexDir.SOUTH_EAST
        }
        for (c in node.substring(6)) {
            if (c == '-') {
                seq.append(if (line) 'w' else 'e')
                line = true
            } else {
                seq.append(if (line) "ea" else "da")
                line = false
            }
        }
        return HexPattern.fromAnglesUnchecked(seq.toString(), start)
    }

    private fun matrixFromCode(node: String): Iota {
        val parts = node.split("_")
        val rows = parts.getOrNull(1)?.toIntOrNull()
            ?: throw IllegalArgumentException("matrix rows missing")
        val cols = parts.getOrNull(2)?.toIntOrNull()
            ?: throw IllegalArgumentException("matrix cols missing")
        if (rows <= 0 || cols <= 0) throw IllegalArgumentException("matrix dimensions must be positive")
        val values = parts.drop(3).map { it.toDoubleOrNull() ?: 0.0 }
        val mat = SimpleMatrix(rows, cols)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val idx = i * cols + j
                mat.set(i, j, values.getOrElse(idx) { 0.0 })
            }
        }
        return MatrixIota(mat)
    }

    // ----------------------------------------------------------------- output

    @JvmStatic
    fun iotaToCode(root: Iota, player: ServerPlayer, signaturesOnly: Boolean): String {
        return if (root is ListIota) {
            root.list.joinToString(",") { encode(it, player, signaturesOnly) }
        } else {
            encode(root, player, signaturesOnly)
        }
    }

    private fun encode(iota: Iota, player: ServerPlayer, signaturesOnly: Boolean): String {
        val typeId = HexIotaTypes.REGISTRY.getKey(iota.getType())?.toString() ?: return "garbage"
        return when (typeId) {
            TYPE_LIST -> {
                val list = iota as ListIota
                "[" + list.list.joinToString(",") { encode(it, player, signaturesOnly) } + "]"
            }

            TYPE_PATTERN -> patternToCode((iota as PatternIota).pattern, player, signaturesOnly)
            TYPE_DOUBLE -> displayMinimal((iota as DoubleIota).getDouble())
            TYPE_BOOLEAN -> if ((iota as BooleanIota).getBool()) "true" else "false"
            TYPE_NULL -> "null"
            TYPE_GARBAGE -> "garbage"
            TYPE_VEC3 -> vecToCode((iota as Vec3Iota).getVec3())
            TYPE_ENTITY -> {
                val uuid = (iota as EntityIota).entity.uuid
                if (uuid == player.uuid) "self" else "entity_$uuid"
            }

            TYPE_STRING -> "\"" + escape((iota as StringIota).string) + "\""
            TYPE_MATRIX -> matrixToCode((iota as MatrixIota).simpleMatrix)
            TYPE_IOTA_TYPE -> {
                val inner = (iota as IotaTypeIota).iotaType
                val id = HexIotaTypes.REGISTRY.getKey(inner)
                if (id == null) "garbage" else "type_" + omitPrefix(id.toString(), "hexcasting:")
            }

            TYPE_ENTITY_TYPE -> {
                val id = BuiltInRegistries.ENTITY_TYPE.getKey((iota as EntityTypeIota).entityType)
                if (id == null) "garbage" else "type/entity_" + omitPrefix(id.toString(), "minecraft:")
            }

            TYPE_ITEM_TYPE -> {
                val itemType = iota as ItemTypeIota
                val item = itemType.either.left().orElse(null)
                val block = itemType.either.right().orElse(null)
                val id = when {
                    item != null -> BuiltInRegistries.ITEM.getKey(item)
                    block != null -> BuiltInRegistries.BLOCK.getKey(block)
                    else -> null
                }
                when {
                    id == null -> "garbage"
                    item != null -> "type/item_" + omitPrefix(id.toString(), "minecraft:")
                    else -> "type/block_" + omitPrefix(id.toString(), "minecraft:")
                }
            }

            else -> "garbage"
        }
    }

    private fun vecToCode(vec: Vec3): String {
        val axes = mutableListOf(vec.x, vec.y, vec.z)
        while (axes.isNotEmpty() && axes.last() == 0.0) axes.removeAt(axes.size - 1)
        return (listOf("vec") + axes.map { displayMinimal(it) }).joinToString("_")
    }

    private fun matrixToCode(mat: SimpleMatrix): String {
        val out = ArrayList<String>()
        out.add("mat")
        out.add(mat.numRows.toString())
        out.add(mat.numCols.toString())
        for (i in 0 until mat.numRows) {
            for (j in 0 until mat.numCols) {
                out.add(displayMinimal(mat.get(i, j)))
            }
        }
        return out.joinToString("_")
    }

    private fun patternToCode(pat: HexPattern, player: ServerPlayer, signaturesOnly: Boolean): String {
        val sig = pat.anglesSignature()
        if (signaturesOnly) return "_$sig"
        SPECIAL_PATTERNS[sig]?.let { return it }

        val match = PatternRegistryManifest.matchPattern(pat, StaffCastEnv(player, InteractionHand.MAIN_HAND))
        val key = when (match) {
            is PatternShapeMatch.Normal -> match.key
            is PatternShapeMatch.PerWorld -> match.key
            is PatternShapeMatch.Special -> {
                val handler = match.handler
                if (handler is SpecialHandlerMask) {
                    return "mask_" + handler.mask.map { if (it) '-' else 'v' }.joinToString("")
                }
                if (handler is SpecialHandlerNumberLiteral) {
                    return "num_" + displayMinimal(handler.x)
                }
                null
            }

            else -> null
        }
        if (key != null) {
            val id = key.identifier()
            return if (id.namespace == HexAPI.MOD_ID) id.path else id.toString()
        }
        return "_$sig"
    }

    // -------------------------------------------------------------- utilities

    @JvmStatic
    fun displayMinimal(raw: Double): String {
        val mid = String.format("%.4f", raw)
        var ptr = mid.length
        while (ptr > 0) {
            val c = mid[ptr - 1]
            if (c == '0') {
                ptr--
            } else {
                if (c == '.') ptr--
                break
            }
        }
        return mid.substring(0, ptr)
    }

    @JvmStatic
    fun escape(string: String): String = string
        .replace("\\", "\\\\")
        .replace("\r", "\\r")
        .replace("\n", "\\n")
        .replace("\"", "\\\"")

    @JvmStatic
    fun unescape(string: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < string.length) {
            val c = string[i]
            if (c == '\\') {
                if (i + 1 >= string.length) throw IllegalArgumentException("trailing backslash")
                out.append(
                    when (string[i + 1]) {
                        'n' -> '\n'
                        'r' -> '\r'
                        '\\' -> '\\'
                        '"' -> '"'
                        else -> throw IllegalArgumentException("invalid escape sequence")
                    }
                )
                i += 2
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }

    private fun omitPrefix(string: String, prefix: String): String =
        if (string.startsWith(prefix)) string.substring(prefix.length) else string

    private fun warn(player: ServerPlayer, message: String) {
        player.sendSystemMessage(Component.literal(message))
    }

    /**
     * 数字 -> 图案。与 HexParseMod 的 NumEvaluatorBrute 相同：先反复乘 2 消掉小数，
     * 再按位分解，最后补上符号前缀。
     */
    @JvmStatic
    fun anglesFromNum(targetIn: Double): String {
        var target = if (targetIn.isNaN()) 0.0 else targetIn
        val neg = target < 0
        target = Math.min(Math.abs(target), Long.MAX_VALUE.toDouble())
        var tolerance = DoubleIota.TOLERANCE / 10
        val seq = ArrayList<String>()

        for (i in 0 until 100) {
            if (target % 1 < tolerance) break
            target *= 2
            tolerance *= 2
            seq.add("d")
        }

        var n = Math.round(target)
        while (n != 0L) {
            when {
                (n and 10L) == 10L -> {
                    seq.add("e")
                    n = n xor 10L
                }

                (n and 5L) == 5L -> {
                    seq.add("q")
                    n = n xor 5L
                }

                (n and 1L) == 1L -> {
                    seq.add("w")
                    n = n xor 1L
                }

                else -> {
                    seq.add("a")
                    n = n ushr 1
                }
            }
        }
        seq.add(if (neg) "dedd" else "aqaa")
        seq.reverse()
        return seq.joinToString("")
    }
}
