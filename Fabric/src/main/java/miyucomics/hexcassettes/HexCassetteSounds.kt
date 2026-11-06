package miyucomics.hexcassettes

import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import java.util.function.BiConsumer

/**
 * 卡带音效。id 使用 hexcassettes 命名空间，与 assets/hexcassettes/sounds.json 对应。
 */
object HexCassetteSounds {
    private val SOUNDS = LinkedHashMap<Identifier, SoundEvent>()

    @JvmField val CASSETTE_EJECT: SoundEvent = sound("cassette_eject")
    @JvmField val CASSETTE_FAIL: SoundEvent = sound("cassette_fail")
    @JvmField val CASSETTE_INSERT: SoundEvent = sound("cassette_insert")
    @JvmField val CASSETTE_LOOP: SoundEvent = sound("cassette_loop")

    @JvmStatic
    fun registerSounds(r: BiConsumer<SoundEvent, Identifier>) {
        for ((id, event) in SOUNDS) {
            r.accept(event, id)
        }
    }

    private fun sound(name: String): SoundEvent {
        val id = HexCassettes.id(name)
        val event = SoundEvent.createVariableRangeEvent(id)
        check(SOUNDS.put(id, event) == null) { "Typo? Duplicate id $name" }
        return event
    }
}
