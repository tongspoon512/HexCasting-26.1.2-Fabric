package miyucomics.hexcassettes

import at.petrak.hexcasting.api.mod.HexConfig
import miyucomics.hexcassettes.advancements.CassetteTrigger
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.component.Consumable
import net.minecraft.world.level.Level
import java.util.function.BiConsumer

/**
 * 可食用的卡带：每吃下一盒，就增加一个可用的卡带槽位（上限见配置 maxCassettes）。
 */
class CassetteItem(properties: Properties) : Item(properties) {
    override fun finishUsingItem(stack: ItemStack, level: Level, entity: LivingEntity): ItemStack {
        if (level.isClientSide) {
            level.playSound(entity, entity.x, entity.y, entity.z, HexCassetteSounds.CASSETTE_INSERT, SoundSource.PLAYERS, 1f, 1f)
        } else if (entity is ServerPlayer) {
            val state = entity.cassetteState
            val maxCassettes = HexConfig.server().maxCassettes()
            if (state.owned < maxCassettes) {
                CassetteTrigger.TAPE_WORM.trigger(entity)
                state.owned += 1
                if (state.owned == maxCassettes) {
                    CassetteTrigger.FULL_ARSENAL.trigger(entity)
                }
                state.sync(entity)
            }
        }
        return super.finishUsingItem(stack, level, entity)
    }
}

object HexCassetteItems {
    @JvmField
    val CASSETTE: Item = CassetteItem(
        Item.Properties()
            .stacksTo(1)
            .rarity(Rarity.UNCOMMON)
            .food(
                FoodProperties.Builder().alwaysEdible().build(),
                Consumable.builder()
                    .consumeSeconds(5.0f)
                    .animation(ItemUseAnimation.EAT)
                    .sound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(HexCassetteSounds.CASSETTE_LOOP))
                    .build(),
            )
            .setId(ResourceKey.create(Registries.ITEM, HexCassettes.id("cassette"))),
    )

    @JvmStatic
    fun registerItems(r: BiConsumer<Item, Identifier>) {
        r.accept(CASSETTE, HexCassettes.id("cassette"))
    }
}
