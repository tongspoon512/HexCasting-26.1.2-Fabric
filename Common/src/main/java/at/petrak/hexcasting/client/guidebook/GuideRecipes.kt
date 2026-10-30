package at.petrak.hexcasting.client.guidebook

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * 从 mod 自己的 classpath 读取配方 JSON。26.1 的客户端不再保留完整 RecipeManager，
 * 但这些配方是随模组分发的静态数据，直接读 jar 里的文件最稳。
 */
object GuideRecipes {
    data class Recipe(val result: ItemStack, val grid: List<ItemStack?>)

    private val cache = HashMap<String, Recipe?>()

    fun get(id: Identifier): Recipe? = cache.getOrPut(id.toString()) { load(id) }

    private fun load(id: Identifier): Recipe? {
        val json = readJson("/data/${id.namespace}/recipe/${id.path}.json")
            ?: readJson("/data/${id.namespace}/recipes/${id.path}.json")
            ?: return null

        val result = json.getAsJsonObject("result")?.let { readResult(it) } ?: return null

        val grid = ArrayList<ItemStack?>()
        val pattern = json.getAsJsonArray("pattern")
        if (pattern != null) {
            val key = json.getAsJsonObject("key")
            for (r in 0 until 3) {
                val row = if (r < pattern.size()) pattern[r].asString else ""
                for (c in 0 until 3) {
                    val ch = row.getOrNull(c)
                    if (ch == null || ch == ' ') {
                        grid += null
                    } else {
                        grid += readIngredient(key?.get(ch.toString()))
                    }
                }
            }
        } else {
            val ingredients = json.getAsJsonArray("ingredients")
            if (ingredients != null) {
                for (i in 0 until 9) {
                    grid += if (i < ingredients.size()) readIngredient(ingredients[i]) else null
                }
            }
        }
        return Recipe(result, grid)
    }

    private fun readResult(json: JsonObject): ItemStack? {
        val raw = when {
            json.has("id") -> json.get("id").asString
            json.has("name") -> json.get("name").asString
            json.has("item") -> json.get("item").asString
            else -> return null
        }
        val count = if (json.has("count")) json.get("count").asInt else 1
        val stack = GuideBook.stackOf(raw)
        return if (stack.isEmpty) null else stack.copyWithCount(count)
    }

    private fun readIngredient(element: JsonElement?): ItemStack? {
        if (element == null) return null
        return when {
            element.isJsonPrimitive -> stackOfName(element.asString)
            element.isJsonArray -> readIngredient(element.asJsonArray.firstOrNull())
            element.isJsonObject -> {
                val obj = element.asJsonObject
                when {
                    obj.has("item") -> stackOfName(obj.get("item").asString)
                    obj.has("tag") -> firstOfTag(obj.get("tag").asString)
                    obj.has("id") -> stackOfName(obj.get("id").asString)
                    else -> null
                }
            }
            else -> null
        }
    }

    private fun stackOfName(raw: String): ItemStack? {
        val stack = GuideBook.stackOf(raw)
        return if (stack.isEmpty) null else stack
    }

    private fun firstOfTag(raw: String): ItemStack? {
        val id = Identifier.tryParse(raw.removePrefix("#")) ?: return null
        val tag = TagKey.create(Registries.ITEM, id)
        return BuiltInRegistries.ITEM.getTagOrEmpty(tag).firstOrNull()?.let { ItemStack(it.value()) }
    }

    private fun readJson(path: String): JsonObject? {
        return try {
            GuideRecipes::class.java.getResourceAsStream(path)?.use {
                it.reader().use { reader -> JsonParser.parseReader(reader) as? JsonObject }
            }
        } catch (e: Exception) {
            null
        }
    }
}
