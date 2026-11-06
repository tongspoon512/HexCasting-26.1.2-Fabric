package at.petrak.hexcasting.client.guidebook

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.client.Minecraft
import java.nio.file.Files
import java.nio.file.Path

/** 页级书签：条目 id 加上该跨页左边的页码。 */
data class GuideBookmark(val entryId: String, val page: Int)

/** 退出前停留的位置。 */
data class GuideBookPosition(
    val view: String,
    val entryId: String?,
    val categoryId: String?,
    val spread: Int
)

/**
 * 指南书的本地状态（书签与上次阅读位置），存放在 `config/hexcasting_guidebook.json`，
 * 与图案别名一样按游戏实例持久化。
 */
object GuideBookState {
    private var loaded = false
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val savedBookmarks = ArrayList<GuideBookmark>()
    private var position: GuideBookPosition? = null

    private fun path(): Path =
        Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("hexcasting_guidebook.json")

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        val file = path()
        if (!Files.exists(file)) return
        runCatching {
            Files.newBufferedReader(file).use { reader ->
                val json = JsonParser.parseReader(reader).asJsonObject
                json.getAsJsonArray("bookmarks")?.forEach { element ->
                    if (!element.isJsonObject) return@forEach
                    val obj = element.asJsonObject
                    val entry = obj.get("entry")?.asString ?: return@forEach
                    if (entry.isBlank()) return@forEach
                    val page = runCatching { obj.get("page")?.asInt ?: 0 }.getOrDefault(0)
                    if (savedBookmarks.none { it.entryId == entry && it.page == page }) {
                        savedBookmarks += GuideBookmark(entry, page)
                    }
                }
                json.getAsJsonObject("last")?.let { last ->
                    val view = last.get("view")?.asString ?: return@let
                    position = GuideBookPosition(
                        view = view,
                        entryId = last.get("entry")?.asString?.ifBlank { null },
                        categoryId = last.get("category")?.asString?.ifBlank { null },
                        spread = runCatching { last.get("spread")?.asInt ?: 0 }.getOrDefault(0)
                    )
                }
            }
        }
    }

    fun bookmarks(): List<GuideBookmark> {
        ensureLoaded()
        return savedBookmarks
    }

    fun isBookmarked(entryId: String, page: Int): Boolean {
        ensureLoaded()
        return savedBookmarks.any { it.entryId == entryId && it.page == page }
    }

    /** 切换书签，返回切换后是否处于已收藏状态。 */
    fun toggleBookmark(entryId: String, page: Int): Boolean {
        ensureLoaded()
        val existing = savedBookmarks.firstOrNull { it.entryId == entryId && it.page == page }
        if (existing != null) savedBookmarks.remove(existing) else savedBookmarks += GuideBookmark(entryId, page)
        save()
        return existing == null
    }

    fun removeBookmark(entryId: String, page: Int) {
        ensureLoaded()
        if (savedBookmarks.removeAll { it.entryId == entryId && it.page == page }) save()
    }

    fun loadPosition(): GuideBookPosition? {
        ensureLoaded()
        return position
    }

    fun savePosition(view: String, entryId: String?, categoryId: String?, spread: Int) {
        ensureLoaded()
        position = GuideBookPosition(view, entryId, categoryId, spread)
        save()
    }

    private fun save() {
        runCatching {
            val file = path()
            Files.createDirectories(file.parent)
            val json = JsonObject()
            val array = JsonArray()
            for (bookmark in savedBookmarks) {
                val obj = JsonObject()
                obj.addProperty("entry", bookmark.entryId)
                obj.addProperty("page", bookmark.page)
                array.add(obj)
            }
            json.add("bookmarks", array)
            position?.let { pos ->
                val last = JsonObject()
                last.addProperty("view", pos.view)
                pos.entryId?.let { last.addProperty("entry", it) }
                pos.categoryId?.let { last.addProperty("category", it) }
                last.addProperty("spread", pos.spread)
                json.add("last", last)
            }
            Files.writeString(file, gson.toJson(json))
        }
    }
}
