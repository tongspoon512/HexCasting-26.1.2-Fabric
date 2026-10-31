package ram.talia.moreiotas.api.casting.iota

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.api.utils.downcast
import net.minecraft.ChatFormatting
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import ram.talia.moreiotas.api.mod.MoreIotasConfig
import ram.talia.moreiotas.common.lib.hex.MoreIotasIotaTypes

class StringIota private constructor(val string: String) : Iota(MoreIotasIotaTypes.STRING, string) {
    override fun toleratesOther(that: Iota): Boolean =
        Iota.typesMatch(this, that) && that is StringIota && this.string == that.string

    override fun isTruthy(): Boolean = this.string.isNotEmpty()

    override fun serialize(): Tag = StringTag.valueOf(this.string)

    companion object {
        @JvmStatic
        fun make(string: String): StringIota {
            val max = MoreIotasConfig.maxStringLength
            if (string.length > max) {
                throw MishapInvalidIota.of(makeUnchecked(string), 0, "string.max_size", max, string.length)
            }
            return StringIota(string)
        }

        @JvmStatic
        fun makeUnchecked(string: String): StringIota = StringIota(string)

        @JvmField
        val TYPE: IotaType<StringIota> = object : IotaType<StringIota>() {
            override fun deserialize(tag: Tag, world: ServerLevel?): StringIota {
                val stag = tag.downcast(StringTag.TYPE)
                try {
                    return make(stag.asString().orElse(""))
                } catch (e: MishapInvalidIota) {
                    throw IllegalArgumentException(e)
                }
            }

            override fun display(tag: Tag): Component {
                if (tag !is StringTag) {
                    return Component.translatable("hexcasting.spelldata.unknown")
                }
                return Component.translatable("moreiotas.tooltip.string", tag.asString().orElse(""))
                    .withStyle(ChatFormatting.LIGHT_PURPLE)
            }

            override fun color(): Int = 0xffff55ff.toInt()
        }
    }
}
