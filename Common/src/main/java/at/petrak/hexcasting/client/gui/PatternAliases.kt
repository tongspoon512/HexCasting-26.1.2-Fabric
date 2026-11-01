package at.petrak.hexcasting.client.gui

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.nio.file.Files
import java.nio.file.Path

/** Client-side pattern aliases, stored per game instance in `config/hexcasting_aliases.json`. */
internal object PatternAliases {
    private var loaded = false
    private val aliases = HashMap<String, String>()
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private fun path(): Path =
        Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("hexcasting_aliases.json")

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        val file = path()
        if (!Files.exists(file)) return
        runCatching {
            Files.newBufferedReader(file).use { reader ->
                val json = JsonParser.parseReader(reader).asJsonObject
                for ((id, value) in json.entrySet()) {
                    val alias = value.asString
                    if (alias.isNotBlank()) aliases[id] = alias
                }
            }
        }
    }

    fun get(id: String): String? {
        ensureLoaded()
        return aliases[id]
    }

    fun put(id: String, alias: String) {
        ensureLoaded()
        if (alias.isBlank()) aliases.remove(id) else aliases[id] = alias.trim()
        save()
    }

    fun nameOf(id: String, nameKey: String): Component {
        val alias = get(id)
        return if (alias != null) Component.literal(alias) else Component.translatable(nameKey)
    }

    private fun save() {
        runCatching {
            val file = path()
            Files.createDirectories(file.parent)
            val json = JsonObject()
            for ((id, alias) in aliases) json.addProperty(id, alias)
            Files.writeString(file, gson.toJson(json))
        }
    }
}
