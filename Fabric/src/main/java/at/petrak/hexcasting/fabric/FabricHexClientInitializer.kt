package at.petrak.hexcasting.fabric

import at.petrak.hexcasting.api.HexAPI

import at.petrak.hexcasting.client.ClientTickCounter
import at.petrak.hexcasting.client.Keybinds
import at.petrak.hexcasting.client.RegisterClientStuff
import at.petrak.hexcasting.client.ShiftScrollListener
import at.petrak.hexcasting.client.gui.PatternTooltipComponent
import at.petrak.hexcasting.client.model.HexModelLayers
import at.petrak.hexcasting.client.render.HexAdditionalRenderers
import at.petrak.hexcasting.common.casting.PatternRegistryManifest
import at.petrak.hexcasting.common.lib.HexParticles
import at.petrak.hexcasting.fabric.event.MouseScrollCallback
import at.petrak.hexcasting.fabric.network.FabricPacketHandler
import at.petrak.hexcasting.interop.HexInterop
import gay.`object`.hexdebug.HexDebugRegistry
import gay.`object`.hexdebug.gui.splicing.SplicingTableScreen
import miyucomics.hexcassettes.client.CassetteScreen
import miyucomics.hexcassettes.network.MsgCassetteSyncRequestC2S
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.*
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType

object FabricHexClientInitializer : ClientModInitializer {
    private val SCRYING_LENS_OVERLAY = HexAPI.modLoc("scrying_lens_overlay")
    private val PERSONAL_MEDIA_BAR = gay.`object`.hexdebug.HexDebug.id("personal_media_bar")

    override fun onInitializeClient() {
        FabricPacketHandler.initClient()

        MenuScreens.register(HexDebugRegistry.SPLICING_TABLE_MENU) { menu, inv, title ->
            SplicingTableScreen(menu, inv, title)
        }

        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register { ctx ->
            HexAdditionalRenderers.overlayLevel(ctx.poseStack(), ctx.submitNodeCollector())
        }
        HudElementRegistry.addLast(SCRYING_LENS_OVERLAY, HudElement { graphics, deltaTracker ->
            HexAdditionalRenderers.overlayGui(graphics, deltaTracker.getGameTimeDeltaPartialTick(false))
        })
        HudElementRegistry.addLast(PERSONAL_MEDIA_BAR, HudElement { graphics, _ ->
            gay.`object`.hexdebug.client.PersonalMediaHud.render(graphics)
        })
        LevelRenderEvents.START_MAIN.register {
            ClientTickCounter.renderTickStart(
                Minecraft.getInstance().deltaTracker.getGameTimeDeltaPartialTick(false)
            )
        }
        ClientTickEvents.END_CLIENT_TICK.register {
            ClientTickCounter.clientTickEnd()
            Keybinds.clientTickEnd()
            ShiftScrollListener.clientTickEnd()

            val minecraft = Minecraft.getInstance()
            while (Keybinds.openCassettes.consumeClick()) {
                if (minecraft.screen == null) {
                    minecraft.setScreen(CassetteScreen())
                }
            }
        }
        ClientTooltipComponentCallback.EVENT.register(PatternTooltipComponent::tryConvert)
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            if (!FabricHexInitializer.patternRegistryIsProcessed) {
                PatternRegistryManifest.processRegistry(null)
                FabricHexInitializer.patternRegistryIsProcessed = true
            }
            // 登录后向服务端请求一次磁带状态
            FabricPacketHandler.sendToServer(MsgCassetteSyncRequestC2S())
        }

        MouseScrollCallback.EVENT.register(ShiftScrollListener::onScrollInGameplay)

        Keybinds.ALL_BINDS.forEach(KeyMappingHelper::registerKeyMapping)

        RegisterClientStuff.init()
        HexModelLayers.init { loc, defn -> ModelLayerRegistry.registerModelLayer(loc, defn::get) }

        // 26.1: particle providers are registered from FabricParticleResourcesMixin.

        // how ergonomic
        RegisterClientStuff.registerBlockEntityRenderers(object :
            RegisterClientStuff.BlockEntityRendererRegisterererer {
            override fun <T : BlockEntity> registerBlockEntityRenderer(
                type: BlockEntityType<T>,
                berp: BlockEntityRendererProvider<in T, out BlockEntityRenderState>
            ) {
                BlockEntityRendererRegistry.register(type, berp)
            }
        })

        HexInterop.clientInit()
        // TODO(26.1): item/block tint sources moved into model definitions; the old ColorProviderRegistry no
        // longer exists. Re-register the focus/spellbook/akashic bookshelf tints through the new system.
        // RegisterClientStuff.registerColorProviders(...)
    }
}
