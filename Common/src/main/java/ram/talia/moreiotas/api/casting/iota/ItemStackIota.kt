package ram.talia.moreiotas.api.casting.iota

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.utils.downcast
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import net.minecraft.resources.RegistryOps
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import ram.talia.moreiotas.common.lib.hex.MoreIotasIotaTypes

/**
 * 物品栈 iota。
 *
 * 26.1 起物品不再有自由 NBT，序列化改为写入完整的 [ItemStack.OPTIONAL_CODEC] 结果：
 * `{ "moreiotas:stack": { id, count, components } }`。
 * 同时保留对旧格式（`moreiotas:stack_id` / `stack_count` / `stack_tag`）的读取兼容，
 * 旧存档至少能恢复物品与数量。
 */
class ItemStackIota private constructor(val itemStack: ItemStack) : Iota(MoreIotasIotaTypes.ITEM_STACK, itemStack) {
    override fun toleratesOther(that: Iota): Boolean =
        that is ItemStackIota && ItemStack.matches(this.itemStack, that.itemStack)

    override fun isTruthy(): Boolean = !this.itemStack.isEmpty

    override fun serialize(): Tag {
        val tag = CompoundTag()
        ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, this.itemStack)
            .resultOrPartial()
            .orElse(null)
            ?.let { tag.put(TAG_STACK, it) }
        return tag
    }

    companion object {
        private const val TAG_STACK = "moreiotas:stack"

        // 旧格式字段，仅用于读取兼容
        private const val TAG_STACK_ID = "moreiotas:stack_id"
        private const val TAG_STACK_COUNT = "moreiotas:stack_count"

        @JvmStatic
        fun createFiltered(originalStack: ItemStack): ItemStackIota {
            val stack = originalStack.copy()
            // 避免把嵌套容器/方块实体数据以及 iota 自身的数据带进序列化结果
            stack.remove(DataComponents.CUSTOM_DATA)
            stack.remove(DataComponents.CONTAINER)
            stack.remove(DataComponents.BUNDLE_CONTENTS)
            stack.remove(DataComponents.BLOCK_ENTITY_DATA)
            return ItemStackIota(stack)
        }

        @JvmField
        val TYPE: IotaType<ItemStackIota> = object : IotaType<ItemStackIota>() {
            override fun deserialize(tag: Tag, world: ServerLevel?): ItemStackIota {
                val ctag = tag.downcast(CompoundTag.TYPE)

                if (ctag.isEmpty) {
                    return ItemStackIota(ItemStack.EMPTY)
                }

                ctag.getCompound(TAG_STACK).orElse(null)?.let { stackTag ->
                    // 部分组件（如附魔）解码时依赖注册表，有世界时使用 RegistryOps
                    val ops = if (world != null) {
                        RegistryOps.create(NbtOps.INSTANCE, world.registryAccess())
                    } else {
                        NbtOps.INSTANCE
                    }
                    val stack = ItemStack.OPTIONAL_CODEC.parse(ops, stackTag)
                        .resultOrPartial()
                        .orElse(ItemStack.EMPTY)
                    return ItemStackIota(stack)
                }

                // 旧格式兼容
                val idStr = ctag.getString(TAG_STACK_ID).orElse(null)
                    ?: return ItemStackIota(ItemStack.EMPTY)
                val id = Identifier.tryParse(idStr) ?: return ItemStackIota(ItemStack.EMPTY)
                val item = BuiltInRegistries.ITEM.getOptional(id).orElse(null)
                    ?: return ItemStackIota(ItemStack.EMPTY)
                val count = ctag.getIntOr(TAG_STACK_COUNT, 0)

                return ItemStackIota(
                    if (item == Items.AIR || count <= 0) ItemStack.EMPTY else ItemStack(item, count)
                )
            }

            override fun display(tag: Tag): Component {
                if (tag !is CompoundTag) {
                    return Component.translatable("hexcasting.spelldata.unknown")
                }

                val style = Style.EMPTY.withColor(0x1E90FF)
                val stack = deserialize(tag, null).itemStack

                if (!stack.isEmpty) {
                    return Component.translatable("moreiotas.tooltip.stack.format", stack.count, stack.hoverName)
                        .withStyle(style)
                }

                // 无注册表时部分组件无法解码，退化为只显示物品 id 与数量
                val stackTag = tag.getCompound(TAG_STACK).orElse(null) as? CompoundTag
                val id = stackTag?.getString("id")?.orElse(null)
                val item = id?.let { Identifier.tryParse(it) }?.let { BuiltInRegistries.ITEM.getOptional(it).orElse(null) }
                val count = stackTag?.getIntOr("count", 0) ?: 0
                if (item != null && item != Items.AIR && count > 0) {
                    return Component.translatable(
                        "moreiotas.tooltip.stack.format", count, item.getName(ItemStack(item, 1))
                    ).withStyle(style)
                }

                return Component.translatable("moreiotas.tooltip.stack.empty").withStyle(style)
            }

            override fun color(): Int = 0xff1e90ff.toInt()
        }
    }
}
