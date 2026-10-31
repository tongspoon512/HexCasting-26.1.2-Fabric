package ram.talia.moreiotas.api.casting.iota

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.utils.downcast
import net.minecraft.ChatFormatting
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import ram.talia.moreiotas.common.lib.hex.MoreIotasIotaTypes

class EntityTypeIota(val entityType: EntityType<*>) : Iota(MoreIotasIotaTypes.ENTITY_TYPE, entityType) {
    override fun toleratesOther(that: Iota): Boolean =
        Iota.typesMatch(this, that) && that is EntityTypeIota && this.entityType == that.entityType

    override fun isTruthy(): Boolean = true

    override fun serialize(): Tag =
        StringTag.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(this.entityType).toString())

    companion object {
        @JvmField
        val TYPE: IotaType<EntityTypeIota> = object : IotaType<EntityTypeIota>() {
            override fun deserialize(tag: Tag, world: ServerLevel?): EntityTypeIota? {
                val stag = tag.downcast(StringTag.TYPE)
                val id = Identifier.tryParse(stag.asString().orElse("")) ?: return null
                val type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null) ?: return null
                return EntityTypeIota(type)
            }

            override fun display(tag: Tag): Component {
                if (tag !is StringTag) {
                    return Component.translatable("hexcasting.spelldata.unknown")
                }
                val id = Identifier.tryParse(tag.asString().orElse("")) ?: return Component.translatable("hexcasting.spelldata.unknown")
                val type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null)
                    ?: return Component.translatable("hexcasting.spelldata.unknown")

                return type.description.copy()
                    .append(" ")
                    .append(Component.translatable("moreiotas.spelldata.entity_type"))
                    .withStyle(ChatFormatting.DARK_AQUA)
            }

            override fun color(): Int = 0xff5555ff.toInt()
        }
    }
}
