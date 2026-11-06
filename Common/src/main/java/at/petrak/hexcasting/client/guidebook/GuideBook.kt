package at.petrak.hexcasting.client.guidebook

import at.petrak.hexcasting.api.HexAPI
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * 客户端侧的指南书数据。内容直接复用官方 Patchouli 书的 JSON（book/category/entry），
 * 因此不需要 Patchouli 运行时，也不会与 hexdoc 的官方文档流程分叉。
 */
object GuideBook {
    private const val LANG = "en_us"
    private const val ROOT = "patchouli_books/thehexbook"
    private const val BOOK_JSON = "/data/hexcasting/$ROOT/book.json"
    private const val LANG_JSON = "/assets/hexcasting/lang/en_us.json"
    private const val ACTION_PREFIX = "hexcasting.action."

    @Volatile
    private var cached: GuideBookData? = null

    fun get(): GuideBookData {
        cached?.let { return it }
        return load().also { cached = it }
    }

    fun invalidate() {
        cached = null
    }

    private fun load(): GuideBookData {
        val categories = ArrayList<GuideCategory>()
        val entries = ArrayList<GuideEntry>()

        val manager = Minecraft.getInstance().resourceManager
        val categoryPrefix = "$ROOT/$LANG/categories/"
        val categoryResources = manager.listResources("$ROOT/$LANG/categories") { path ->
            path.path.endsWith(".json")
        }
        for ((id, resource) in categoryResources) {
            val json = readObject(resource) ?: continue
            val localId = id.path.removePrefix(categoryPrefix).removeSuffix(".json")
            categories += GuideCategory(
                id = localId,
                nameKey = json.string("name", "hexcasting.category.$localId"),
                descKey = json.string("description", ""),
                icon = stackOf(json.string("icon", "minecraft:book")),
                iconTexture = textureOf(json.string("icon", "")),
                parentId = json.string("parent", "").ifBlank { null }?.substringAfter(':'),
                sortnum = json.int("sortnum", 1000)
            )
        }

        val entryPrefix = "$ROOT/$LANG/entries/"
        val entryResources = manager.listResources("$ROOT/$LANG/entries") { path ->
            path.path.endsWith(".json")
        }
        for ((id, resource) in entryResources) {
            val json = readObject(resource) ?: continue
            val localId = id.path.removePrefix(entryPrefix).removeSuffix(".json")
            val category = json.string("category", "").substringAfter(':')
            val pages = ArrayList<GuidePage>()
            json.getAsJsonArray("pages")?.forEach { element ->
                if (element.isJsonPrimitive) {
                    // Patchouli 允许用字符串简写一个纯文本页
                    val page = JsonObject()
                    page.addProperty("type", "patchouli:text")
                    page.addProperty("text", element.asString)
                    pages += GuidePage("patchouli:text", page)
                } else if (element.isJsonObject) {
                    val pageJson = element.asJsonObject
                    val type = pageJson.string("type", "patchouli:text")
                    pages += GuidePage(type, pageJson)
                }
            }
            entries += GuideEntry(
                id = localId,
                nameKey = json.string("name", "hexcasting.entry.$localId"),
                categoryId = category,
                icon = stackOf(json.string("icon", "minecraft:book")),
                iconTexture = textureOf(json.string("icon", "")),
                sortnum = json.int("sortnum", 1000),
                advancement = json.string("advancement", "").ifBlank { null },
                readByDefault = json.bool("read_by_default", false),
                pages = pages
            )
        }

        categories.sortWith(compareBy({ it.sortnum }, { it.id }))
        entries.sortWith(compareBy({ it.sortnum }, { it.id }))

        val bookJson = readClasspathObject(BOOK_JSON)
        val landing = bookJson?.string("landing_text", "hexcasting.landing") ?: "hexcasting.landing"
        val macros = LinkedHashMap<String, String>()
        bookJson?.getAsJsonObject("macros")?.entrySet()?.forEach { (key, value) ->
            if (value.isJsonPrimitive) macros[key] = value.asString
        }

        val (textKeys, actionKeys) = buildLangAliases()
        return GuideBookData(landing, categories, entries, macros, textKeys, actionKeys)
    }

    /**
     * 这本上游书的 JSON 里把语言键的斜杠后又插了一个点（如
     * `hexcasting.page.consts.const/true` 对应 lang 里的 `.../const/.true`），
     * 且不同键的点数不一致。这里用“去掉 / 与 : 后的多余点”做归一化索引。
     */
    private fun buildLangAliases(): Pair<Map<String, String>, Map<String, String>> {
        val text = HashMap<String, String>()
        val actions = HashMap<String, String>()
        val lang = readClasspathObject(LANG_JSON) ?: return text to actions
        for (key in lang.keySet()) {
            text[canonical(key)] = key
            if (!key.startsWith(ACTION_PREFIX)) continue
            val suffix = key.removePrefix(ACTION_PREFIX)
            val isBook = suffix.startsWith("book.")
            val id = canonical(if (isBook) suffix.removePrefix("book.") else suffix)
            if (isBook || !actions.containsKey(id)) actions[id] = key
        }
        return text to actions
    }

    private fun canonical(key: String): String = key.replace("/.", "/").replace(":.", ":")

    private fun readObject(resource: net.minecraft.server.packs.resources.Resource?): JsonObject? {
        if (resource == null) return null
        return try {
            resource.openAsReader().use { JsonParser.parseReader(it) as? JsonObject }
        } catch (e: Exception) {
            HexAPI.LOGGER.error("Failed to read guidebook resource {}", resource.sourcePackId(), e)
            null
        }
    }

    private fun readClasspathObject(path: String): JsonObject? {
        return try {
            GuideBook::class.java.getResourceAsStream(path)?.use {
                it.reader().use { reader -> JsonParser.parseReader(reader) as? JsonObject }
            }
        } catch (e: Exception) {
            HexAPI.LOGGER.error("Failed to read guidebook classpath resource {}", path, e)
            null
        }
    }

    /** 把 Patchouli 的语言键解析成当前语言的原文；不存在则当作字面量。 */
    fun string(key: String): String {
        if (key.isBlank()) return ""
        val resolved = keyFor(key) ?: return key
        return I18n.get(resolved)
    }

    fun translate(key: String): Component {
        if (key.isBlank()) return Component.empty()
        val resolved = keyFor(key) ?: return Component.literal(key)
        return Component.translatable(resolved)
    }

    /**
     * 找到实际存在的语言键。书里有部分 name 直接写成动作 id（如
     * `hexcasting.action.hexcasting:create_lava`），但本项目 lang 里的动作键是
     * `hexcasting.action.hexcasting:.create_lava` 这种带点的形式，这里两种都试。
     */
    fun keyFor(key: String): String? {
        if (key.isBlank()) return null
        if (I18n.exists(key)) return key
        val data = get()
        if (key.startsWith(ACTION_PREFIX)) {
            val suffix = key.removePrefix(ACTION_PREFIX).removePrefix("book.")
            data.actionKeys[canonical(suffix)]?.let { if (I18n.exists(it)) return it }
        }
        data.textKeys[canonical(key)]?.let { if (I18n.exists(it)) return it }
        for (candidate in mangledActionKeys(key)) {
            if (I18n.exists(candidate)) return candidate
        }
        return null
    }

    private fun mangledActionKeys(key: String): List<String> {
        val prefix = "hexcasting.action."
        if (!key.startsWith(prefix)) return emptyList()
        val id = Identifier.tryParse(key.removePrefix(prefix)) ?: return emptyList()
        val mangled = "${id.namespace}:.${id.path.replace("/", "/.")}"
        return listOf(prefix + "book." + mangled, prefix + mangled)
    }

    fun stackOf(raw: String): ItemStack {
        val cleaned = raw.substringBefore('{').removePrefix("#")
        if (cleaned.isBlank() || cleaned.contains("/textures/")) return ItemStack.EMPTY
        val id = Identifier.tryParse(cleaned) ?: return ItemStack(Items.BOOK)
        val item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.BOOK)
        return ItemStack(item)
    }

    fun textureOf(raw: String): Identifier? {
        val cleaned = raw.substringBefore('{').removePrefix("#")
        return if (cleaned.contains("/textures/") && cleaned.endsWith(".png")) Identifier.tryParse(cleaned) else null
    }
}

class GuideBookData(
    val landingTextKey: String,
    val categories: List<GuideCategory>,
    val entries: List<GuideEntry>,
    val macros: Map<String, String>,
    val textKeys: Map<String, String>,
    val actionKeys: Map<String, String>
) {
    private val byCategory = entries.groupBy { it.categoryId }

    fun entriesOf(category: GuideCategory): List<GuideEntry> =
        byCategory[category.id].orEmpty()

    fun childrenOf(category: GuideCategory): List<GuideCategory> =
        categories.filter { it.parentId == category.id }

    fun entry(id: String): GuideEntry? = entries.firstOrNull { it.id == id }

    fun category(id: String): GuideCategory? = categories.firstOrNull { it.id == id }

    /** 顶层分类先按 sortnum，再按 id；子分类跟随父分类排在后面。 */
    fun orderedCategories(): List<Pair<GuideCategory, Int>> {
        val out = ArrayList<Pair<GuideCategory, Int>>()
        for (root in categories.filter { it.parentId == null }.sortedWith(compareBy({ it.sortnum }, { it.id }))) {
            out += root to 0
            for (child in childrenOf(root).sortedWith(compareBy({ it.sortnum }, { it.id }))) {
                out += child to 1
            }
        }
        // 防御：父分类丢失的孤儿分类也要显示
        for (orphan in categories.filter { it.parentId != null && category(it.parentId!!) == null }) {
            out += orphan to 0
        }
        return out
    }

    // ------------------------------------------------------------- 搜索

    /** 一条搜索结果：命中的条目，若正文命中则带上页码（-1 表示仅名称命中）。 */
    data class SearchResult(val entry: GuideEntry, val page: Int, val nameMatch: Boolean)

    private class SearchDoc(val entry: GuideEntry, val name: String, val pages: List<String>)

    private var searchDocs: List<SearchDoc>? = null

    /**
     * 全库搜索：空格分隔的每个关键词都要命中（条目名/ID 或同一页正文）。
     * 结果按名称命中优先、书本顺序其次排序。
     */
    fun search(query: String): List<SearchResult> {
        val terms = query.trim().lowercase().split(' ').filter { it.isNotEmpty() }
        if (terms.isEmpty()) return emptyList()
        val docs = searchDocs ?: buildSearchDocs().also { searchDocs = it }
        val results = ArrayList<SearchResult>()
        for (doc in docs) {
            val name = doc.name.lowercase()
            val nameMatch = terms.all { name.contains(it) }
            var page = -1
            for ((index, text) in doc.pages.withIndex()) {
                if (terms.all { text.lowercase().contains(it) }) {
                    page = index
                    break
                }
            }
            if (nameMatch || page >= 0) results += SearchResult(doc.entry, page, nameMatch)
        }
        return results.sortedByDescending { if (it.nameMatch) 1 else 0 }
    }

    private fun buildSearchDocs(): List<SearchDoc> {
        return entries.map { entry ->
            SearchDoc(
                entry,
                GuideBook.translate(entry.nameKey).string + " " + entry.id,
                entry.pages.map { searchableText(it) }
            )
        }
    }

    private fun searchableText(page: GuidePage): String {
        val sb = StringBuilder()
        fun add(raw: String) {
            if (raw.isBlank()) return
            sb.append(GuideBook.string(raw)).append(' ')
        }
        add(page.string("text"))
        add(page.string("title"))
        add(page.string("link_text"))
        val opId = page.string("op_id", page.string("anchor"))
        if (opId.isNotBlank()) {
            sb.append(opId).append(' ')
            val translated = GuideBook.keyFor("hexcasting.action.$opId")
            if (translated != null) sb.append(I18n.get(translated)).append(' ')
        }
        page.json.getAsJsonArray("patterns")?.forEach { element ->
            if (element.isJsonObject) {
                element.asJsonObject.get("signature")?.asString?.let { sb.append(it).append(' ') }
            }
        }
        return sb.toString()
    }
}

class GuideCategory(
    val id: String,
    val nameKey: String,
    val descKey: String,
    val icon: ItemStack,
    val iconTexture: Identifier?,
    val parentId: String?,
    val sortnum: Int
)

class GuideEntry(
    val id: String,
    val nameKey: String,
    val categoryId: String,
    val icon: ItemStack,
    val iconTexture: Identifier?,
    val sortnum: Int,
    val advancement: String?,
    val readByDefault: Boolean,
    val pages: List<GuidePage>
) {
    /** 锚点可能来自 pattern 页的 op_id / anchor，或普通页的 anchor 字段。 */
    fun pageIndexForAnchor(anchor: String): Int =
        pages.indexOfFirst {
            it.string("anchor") == anchor || it.string("op_id") == anchor
        }
}

class GuidePage(val type: String, val json: JsonObject) {
    fun string(key: String, default: String = ""): String = json.string(key, default)
    fun int(key: String, default: Int = 0): Int = json.int(key, default)
    fun bool(key: String, default: Boolean = false): Boolean = json.bool(key, default)
}

private fun JsonObject.string(key: String, default: String): String {
    val element = this.get(key) ?: return default
    if (element.isJsonPrimitive) return element.asString
    return default
}

private fun JsonObject.int(key: String, default: Int): Int {
    val element = this.get(key) ?: return default
    return if (element.isJsonPrimitive) {
        try {
            element.asInt
        } catch (e: Exception) {
            default
        }
    } else default
}

private fun JsonObject.bool(key: String, default: Boolean): Boolean {
    val element = this.get(key) ?: return default
    return if (element.isJsonPrimitive) element.asBoolean else default
}
