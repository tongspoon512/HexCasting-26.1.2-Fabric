package at.petrak.hexcasting.common.casting

import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult

/**
 * 26.1 中 UseOnContext 的“带自定义物品栈”构造是 protected：
 * Fabric 端靠 access widener 放宽，NeoForge 端则需要 access transformer。
 * 这里统一改用匿名子类访问，避免两个平台各自维护一份访问放宽配置。
 */
fun makeUseOnContext(
    level: Level,
    player: Player?,
    hand: InteractionHand,
    stack: ItemStack,
    hit: BlockHitResult
): UseOnContext = object : UseOnContext(level, player, hand, stack, hit) {}
