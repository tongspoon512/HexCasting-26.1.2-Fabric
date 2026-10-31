package ram.talia.moreiotas.api.casting.iota

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.utils.downcast
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes
import net.minecraft.ChatFormatting
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import ram.talia.moreiotas.common.lib.hex.MoreIotasIotaTypes

class IotaTypeIota(val iotaType: IotaType<*>) : Iota(MoreIotasIotaTypes.IOTA_TYPE, iotaType) {
    override fun toleratesOther(that: Iota): Boolean =
        Iota.typesMatch(this, that) && that is IotaTypeIota && this.iotaType == that.iotaType

    override fun isTruthy(): Boolean = true

    override fun serialize(): Tag {
        val location = HexIotaTypes.REGISTRY.getKey(this.iotaType) ?: return StringTag.valueOf("")
        return StringTag.valueOf(location.toString())
    }

    companion object {
        @JvmField
        val TYPE: IotaType<IotaTypeIota> = object : IotaType<IotaTypeIota>() {
            override fun deserialize(tag: Tag, world: ServerLevel?): IotaTypeIota? {
                val stag = tag.downcast(StringTag.TYPE)
                val id = Identifier.tryParse(stag.asString().orElse("")) ?: return null
                val type = HexIotaTypes.REGISTRY.getOptional(id).orElse(null) ?: return null
                return IotaTypeIota(type)
            }

            override fun display(tag: Tag): Component {
                if (tag !is StringTag) {
                    return Component.translatable("hexcasting.spelldata.unknown")
                }
                val id = Identifier.tryParse(tag.asString().orElse("")) ?: return Component.translatable("hexcasting.spelldata.unknown")
                val type = HexIotaTypes.REGISTRY.getOptional(id).orElse(null)
                    ?: return Component.translatable("hexcasting.spelldata.unknown")
                val location = HexIotaTypes.REGISTRY.getKey(type)
                    ?: return Component.translatable("hexcasting.spelldata.unknown")

                return Component.translatable("hexcasting.iota.$location")
                    .withStyle(ChatFormatting.DARK_PURPLE)
            }

            override fun color(): Int = 0xff553355.toInt()
        }
    }
}
