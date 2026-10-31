package ram.talia.moreiotas.api.casting.iota

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.utils.downcast
import com.mojang.datafixers.util.Either
import net.minecraft.ChatFormatting
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import ram.talia.moreiotas.common.lib.hex.MoreIotasIotaTypes

class ItemTypeIota private constructor(val either: Either<Item, Block>) : Iota(MoreIotasIotaTypes.ITEM_TYPE, either) {
    constructor(item: Item) : this(Either.left(item))

    constructor(block: Block) : this(Either.right(block))

    /** 若为方块形态（或方块物品）则返回对应方块，否则返回 null。 */
    val block: Block?
        get() = this.either.map({ item -> (item as? BlockItem)?.block }, { block -> block })

    /** 若方块没有物品形态则返回 Items.AIR。 */
    val item: Item
        get() = this.either.map({ item -> item }, { block -> block.asItem() })

    override fun toleratesOther(that: Iota): Boolean =
        Iota.typesMatch(this, that) && that is ItemTypeIota && this.item == that.item && this.block == that.block

    override fun isTruthy(): Boolean =
        this.either.map({ item -> item != Items.AIR }, { block -> !block.defaultBlockState().isAir })

    override fun serialize(): Tag {
        val tag = CompoundTag()
        this.either.map({ item ->
            tag.putString(TAG_ITEM, BuiltInRegistries.ITEM.getKey(item).toString())
            tag
        }, { block ->
            tag.putString(TAG_BLOCK, BuiltInRegistries.BLOCK.getKey(block).toString())
            tag
        })
        return tag
    }

    companion object {
        private const val TAG_ITEM = "item"
        private const val TAG_BLOCK = "block"

        @JvmField
        val TYPE: IotaType<ItemTypeIota> = object : IotaType<ItemTypeIota>() {
            override fun deserialize(tag: Tag, world: ServerLevel?): ItemTypeIota? {
                val ctag = tag.downcast(CompoundTag.TYPE)

                if (ctag.contains(TAG_ITEM)) {
                    val id = Identifier.tryParse(ctag.getStringOr(TAG_ITEM, "")) ?: return null
                    val item = BuiltInRegistries.ITEM.getOptional(id).orElse(null) ?: return null
                    return ItemTypeIota(item)
                } else if (ctag.contains(TAG_BLOCK)) {
                    val id = Identifier.tryParse(ctag.getStringOr(TAG_BLOCK, "")) ?: return null
                    val block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null) ?: return null
                    return ItemTypeIota(block)
                }

                return null
            }

            override fun display(tag: Tag): Component {
                if (tag !is CompoundTag) {
                    return Component.translatable("hexcasting.spelldata.unknown")
                }

                if (tag.contains(TAG_ITEM)) {
                    val id = Identifier.tryParse(tag.getStringOr(TAG_ITEM, ""))
                        ?: return Component.translatable("hexcasting.spelldata.unknown")
                    val item = BuiltInRegistries.ITEM.getOptional(id).orElse(null)
                        ?: return Component.translatable("hexcasting.spelldata.unknown")
                    return Component.translatable(item.descriptionId).withStyle(ChatFormatting.GOLD)
                } else if (tag.contains(TAG_BLOCK)) {
                    val id = Identifier.tryParse(tag.getStringOr(TAG_BLOCK, ""))
                        ?: return Component.translatable("hexcasting.spelldata.unknown")
                    val block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null)
                        ?: return Component.translatable("hexcasting.spelldata.unknown")
                    return block.name.withStyle(ChatFormatting.GOLD)
                }

                return Component.translatable("hexcasting.spelldata.unknown")
            }

            override fun color(): Int = 0xfffeaa01.toInt()
        }
    }
}
