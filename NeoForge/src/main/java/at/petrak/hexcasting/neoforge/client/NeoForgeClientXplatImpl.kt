package at.petrak.hexcasting.neoforge.client

import at.petrak.hexcasting.api.client.ClientCastingStack
import at.petrak.hexcasting.common.msgs.IMessage
import at.petrak.hexcasting.neoforge.network.NeoForgePacketHandler
import at.petrak.hexcasting.xplat.IClientXplatAbstractions
import at.petrak.hexcasting.xplat.ItemPropertyFunction
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.AABB
import java.util.WeakHashMap

/**
 * 客户端侧平台实现。
 *
 * 渲染器注册与施法栈同步依赖后续阶段（P4 附件 / P7 客户端事件），
 * 这里先提供可用的占位实现，避免 ServiceLoader 找不到实现而崩溃。
 */
// 同样由 ServiceLoader 实例化，必须是普通类而非 object。
class NeoForgeClientXplatImpl : IClientXplatAbstractions {
    private val clientStacks = WeakHashMap<Player, ClientCastingStack>()

    override fun sendPacketToServer(packet: IMessage) {
        NeoForgeClientPacketSender.send(NeoForgePacketHandler.wrap(packet))
    }

    override fun setRenderLayer(block: Block, type: RenderType) {
        // 26.1 通过方块模型/材质决定渲染层，无需运行时注册。
    }

    override fun initPlatformSpecific() {
        // P7：客户端事件与渲染器注册在此接入。
    }

    override fun <T : Entity> registerEntityRenderer(
        type: EntityType<out T>,
        renderer: EntityRendererProvider<T>
    ) {
        // P7：改用 EntityRenderersEvent.RegisterRenderers 注册。
    }

    override fun registerItemProperty(item: Item, id: Identifier, func: ItemPropertyFunction) {
        // 26.1 已移除旧的 ItemProperties 谓词系统。
    }

    override fun getClientCastingStack(player: Player): ClientCastingStack =
        clientStacks.getOrPut(player) { ClientCastingStack() }

    override fun fabricAdditionalQuenchFrustumCheck(aabb: AABB): Boolean = true
}
